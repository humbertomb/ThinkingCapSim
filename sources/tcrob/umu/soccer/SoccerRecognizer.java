/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.recognize.*;
import tcrob.umu.soccer.gui.images.BufferedImageDrawing;

public class SoccerRecognizer
{
	static public final double		FOVEA_XSIZE		= 0.3;
	static public final double		FOVEA_YSIZE		= 0.3;

	static public final int 		XSIZEDIFF_MAX	= 160;
	static public final int 		YSIZEDIFF_MAX	= 180;
	static public final int 		XOVERLAP_MIN	= 30;
	static public final int 		YGAP_MAX		= 4;	
	static public final int 		YOVERLAP_MAX	= 40;
	static public final int 		XGAP_MAX		= 70;

	static private int				fovea_xmin;
	static private int				fovea_xmax;
	static private int				fovea_ymin;
	static private int				fovea_ymax;

	// What the objects are looked for in, and what their blobs must be like (the ones of the configuration)
	public SoccerVisionConfig.RecognizerParams	params;

	static private final Blobs		NO_BLOBS	= new Blobs ();		// the blobs of a channel that is not there

	/** Where an object was seen in the last frame: the box of its blob and its centre (pixels). */
	static public class Detection
	{
		public int		x, y;						// centre of the blob
		public int		xmin, xmax, ymin, ymax;		// its box
		public int		pixels;						// how many pixels it has
		public double	cx, cy, radius;				// the circle of a ball (its centre may be out of the frame)
		public boolean	round;						// whether that circle was fitted to its edge (or is its box)
		public int		band = -1;					// a landmark: the row where its top band meets the one below (-1: none)

		Detection (Blob b)
		{
			x		= b.getX ();		y		= b.getY ();
			xmin	= b.getXMin ();		xmax	= b.getXMax ();
			ymin	= b.getYMin ();		ymax	= b.getYMax ();
			pixels	= b.getNumPixels ();
			cx		= x;
			cy		= y;
			radius	= (b.getSizeX () + b.getSizeY ()) / 4.0;
		}

		/** The same, with the circle fitted to its edge. */
		Detection (Blob b, CircleFitting c)
		{
			this (b);
			cx		= c.cx;
			cy		= c.cy;
			radius	= c.radius;
			round	= c.fitted;
		}

		/** A landmark: the box of its two bands, the row where they meet, and the pixels of both. */
		Detection (Blob top, Blob below)
		{
			xmin	= Math.min (top.getXMin (), below.getXMin ());		xmax	= Math.max (top.getXMax (), below.getXMax ());
			ymin	= Math.min (top.getYMin (), below.getYMin ());		ymax	= Math.max (top.getYMax (), below.getYMax ());
			x		= (xmin + xmax) / 2;
			y		= (ymin + ymax) / 2;
			pixels	= top.getNumPixels () + below.getNumPixels ();
			band	= (top.getYMax () + below.getYMin ()) / 2;
			cx		= x;
			cy		= band;
			radius	= (xmax - xmin) / 2.0;
		}

		public String toString ()
		{
			return "(" + x + "," + y + ") [" + xmin + ".." + xmax + " x " + ymin + ".." + ymax + "] " + pixels + " px"
					+ ((band >= 0) ? " band at " + band : String.format (" circle (%.0f,%.0f) r=%.0f%s", cx, cy, radius, round ? "" : " (box)"));
		}
	}

	// What was recognised in the last frame (null: not seen): the largest blob taken for each object
	public Detection				ball;
	public Detection				net1;
	public Detection				net2;
	public Detection				landmark1;			// the landmark with the colour of lm1_channel on top (that of lm2_channel below it)
	public Detection				landmark2;			// ... and the one the other way up

	private BufferedImageDrawing	dwg = new BufferedImageDrawing ();

	/** A recognizer that works with some parameters (the ones of a configuration, changed as they change). */
	public SoccerRecognizer (SoccerVisionConfig.RecognizerParams params)
	{
		this.params	= params;
	}

	public BufferedImage process (BufferedImage input, int[] segmented, Blobs[] blobs, Channels channels, SoccerVisionConfig config)
	{
		BufferedImage		output;
		VisualHorizon		horizon;
		
		output	= new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		output.setData (input.getData ());
		dwg.updateImage (output);
		
		ball		= null;
		net1		= null;
		net2		= null;
		landmark1	= null;
		landmark2	= null;

		computeFovea (output, dwg);
		dwg.setThickness (BufferedImageDrawing.MARK);			// what is recognised is marked thick, to be seen at a glance
		horizon = new VisualHorizon ();
		// a channel that is not there (fewer channels than the one chosen) is not looked for
		if (has (channels, blobs, params.carpet_channel) && has (channels, blobs, params.ball_channel))
			horizon.findHorizon (output, segmented, channels.at (params.carpet_channel), channels.at (params.ball_channel));

		if (has (channels, blobs, params.ball_channel))
		{
			CircleFitting	ellipse = new CircleFitting ();

			for (int i = 0; i < blobs[params.ball_channel].getBlobNumber (); i++)
			{
				Blob		blob = blobs[params.ball_channel].getBlob (i);
				if (testValidBall (blob, config, horizon, dwg))
				{
					ellipse.doFitting (output, segmented, blob, channels.at (params.ball_channel));
					if ((ball == null) || (blob.getNumPixels () > ball.pixels))		ball = new Detection (blob, ellipse);
				}
			}
		}
		
		// the landmarks first: a blob of each of their two channels, one on top of the
		// other, is one of them -- and those blobs are not nets, though they are of
		// the colours of the nets
		java.util.List<Blob>	parts = landmarks (blobs, channels, horizon);

		for (int n : new int[] { params.net1_channel, params.net2_channel })
		{
			NetFitting	net = new NetFitting ();
			int			color;

			if (!has (channels, blobs, n) || !has (channels, blobs, params.carpet_channel))		continue;
			color	= channels.at (n).color.getRGB ();					// the box of a net in the colour of its channel
			for (int i = 0; i < blobs[n].getBlobNumber (); i++)
			{
				Blob		blob = blobs[n].getBlob (i);
				if (!partOf (blob, parts) && testValidNet (blob, config, horizon, dwg, color))
				{
					net.doFitting (output, segmented, blobs[n], channels.at (n), channels.at (params.carpet_channel));
					if (n == params.net1_channel)
					{
						if ((net1 == null) || (blob.getNumPixels () > net1.pixels))		net1 = new Detection (blob);
					}
					else if ((net2 == null) || (blob.getNumPixels () > net2.pixels))	net2 = new Detection (blob);
				}
			}
		}
				
		return output;
	}

	/** The same as {@link #process}. */
	public BufferedImage recognize (BufferedImage input, int[] segmented, Blobs[] blobs, Channels channels, SoccerVisionConfig config)
	{
		return process (input, segmented, blobs, channels, config);
	}

	/** Whether a channel is there, with the blobs of its own. */
	static protected boolean has (Channels channels, Blobs[] blobs, int ch)
	{
		return (ch >= 0) && (ch < channels.size ()) && (blobs != null) && (ch < blobs.length) && (blobs[ch] != null);
	}
		
	protected boolean testValidBall (Blob blob, SoccerVisionConfig config, VisualHorizon horizon, BufferedImageDrawing dwg)
	{
		if ((blob.getSizeX() < params.ball_sx_min) || (blob.getSizeY() < params.ball_sy_min))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > params.ball_density)
			return false;
		if (!horizon.isBelowHorizont (blob, params.ball_horiz_hgt))
			return false;
//		if (!checkFovea (blob))
//			return false;
		
		dwg.drawBox (blob.getXMin(), blob.getYMin(), blob.getXMax(), blob.getYMax(), Color.ORANGE.getRGB ());
		return true;
	}
	
	protected boolean testValidNet (Blob blob, SoccerVisionConfig config, VisualHorizon horizon, BufferedImageDrawing dwg, int color)
	{
		if ((blob.getSizeX() < params.net_sx_min) || (blob.getSizeY() < params.net_sy_min))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > params.net_density)
			return false;
		if (!horizon.isAboveHorizont (blob, params.net_horiz_hgt))
			return false;
//		if (!checkFovea (blob))
//			return false;
	
		dwg.drawBox (blob.getXMin(), blob.getYMin(), blob.getXMax(), blob.getYMax(), color);
		return true;
	}
	
	/**
	 * The landmarks in the frame: every blob of one of their two channels (lm1,
	 * lm2) that could be a band of one ({@link #testValidLandmark}) is paired
	 * with every one of the other channel that could, and a pair of bands one
	 * right on top of the other, about as wide ({@link #checkBlobPair}), is a
	 * landmark -- the first one when the lm1 colour is on top, the second when
	 * the lm2 one is. The largest of each is the one taken. Returns the blobs
	 * that made up a landmark (whichever was taken), which are not nets.
	 */
	protected java.util.List<Blob> landmarks (Blobs[] blobs, Channels channels, VisualHorizon horizon)
	{
		java.util.List<Blob>	parts = new java.util.ArrayList<Blob> ();
		int						c1 = params.lm1_channel, c2 = params.lm2_channel;

		if ((c1 == c2) || !has (channels, blobs, c1) || !has (channels, blobs, c2))		return parts;
		for (int i = 0; i < blobs[c1].getBlobNumber (); i++)
		{
			Blob	a = blobs[c1].getBlob (i);

			if (!testValidLandmark (a, horizon))		continue;
			for (int k = 0; k < blobs[c2].getBlobNumber (); k++)
			{
				Blob	b = blobs[c2].getBlob (k);

				if (!testValidLandmark (b, horizon) || !checkBlobPair (a, b))		continue;

				boolean		first = a.getY () < b.getY ();					// the lm1 colour on top: landmark 1
				Blob		top = first ? a : b, below = first ? b : a;
				Detection	d = new Detection (top, below);

				parts.add (a);
				parts.add (b);
				dwg.drawBox (d.xmin, d.ymin, d.xmax, d.ymax, channels.at (first ? c1 : c2).color.getRGB ());
				dwg.drawLine (d.xmin, d.band, d.xmax, d.band, channels.at (first ? c2 : c1).color.getRGB ());
				if (first)
				{
					if ((landmark1 == null) || (d.pixels > landmark1.pixels))		landmark1 = d;
				}
				else if ((landmark2 == null) || (d.pixels > landmark2.pixels))		landmark2 = d;
			}
		}
		return parts;
	}

	/** Whether a blob is one of some (the same one, not an equal one). */
	static protected boolean partOf (Blob blob, java.util.List<Blob> parts)
	{
		for (Blob p : parts)		if (p == blob)		return true;
		return false;
	}

	/** Whether a blob can be a band of a landmark: big enough, dense enough, and above the horizon (by so much). */
	protected boolean testValidLandmark (Blob blob, VisualHorizon horizon)
	{
		if ((blob.getSizeX() < params.lm_sx_min) || (blob.getSizeY() < params.lm_sy_min))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > params.lm_density)
			return false;
		if (!horizon.isAboveHorizont (blob, params.lm_horiz_hgt))
			return false;
		return true;
	}

	/**
	 * Whether two blobs are the two bands of a landmark: about as wide and as
	 * high as each other, overlapping enough across, and one right on top of the
	 * other (a small gap between them, or a small overlap).
	 */
	protected boolean checkBlobPair (Blob a, Blob b)
	{
		int		xsizediff, ysizediff, xoverlap, xgap, yoverlap, ygap, temp;
		int		min_sizex = Math.min (a.getSizeX (), b.getSizeX ());
		int		min_sizey = Math.min (a.getSizeY (), b.getSizeY ());

		if ((min_sizex <= 0) || (min_sizey <= 0))		return false;

		xsizediff	= Math.abs (a.getSizeX () - b.getSizeX ());
		ysizediff	= Math.abs (a.getSizeY () - b.getSizeY ());

		xoverlap	= a.getXMax () - b.getXMin ();
		temp		= b.getXMax () - a.getXMin ();
		if (temp < xoverlap)			xoverlap = temp;
		if (min_sizex < xoverlap)		xoverlap = min_sizex;
		else if (xoverlap < 0)			xoverlap = 0;

		yoverlap	= a.getYMax () - b.getYMin ();
		temp		= b.getYMax () - a.getYMin ();
		if (temp < yoverlap)			yoverlap = temp;
		if (min_sizey < yoverlap)		yoverlap = min_sizey;
		else if (yoverlap < 0)			yoverlap = 0;

		xgap		= Math.max (a.getXMin () - b.getXMax (), b.getXMin () - a.getXMax ());
		if (xgap < 0)					xgap = 0;
		ygap		= Math.max (a.getYMin () - b.getYMax (), b.getYMin () - a.getYMax ());
		if (ygap < 0)					ygap = 0;

		if ((xsizediff * 100) > (XSIZEDIFF_MAX * min_sizex))		return false;		// the two about as big
		if ((ysizediff * 100) > (YSIZEDIFF_MAX * min_sizey))		return false;
		if ((xoverlap * 100) < (XOVERLAP_MIN * min_sizex))			return false;		// one over the other
		if ((xgap * 100) > (XGAP_MAX * min_sizex))					return false;
		if ((yoverlap * 100) > (YOVERLAP_MAX * min_sizey))			return false;		// on top, not across each other
		if (ygap > YGAP_MAX)										return false;
		return true;
	}
	
	protected void computeFovea (BufferedImage output, BufferedImageDrawing dwg)
	{
		int		width, height;
		int		with_pixel, height_pixel;
		
		// Calculate fovea area (pixels), the most accurate camera info
		width		= output.getWidth();
		height		= output.getHeight();
		with_pixel	= (int) (((double) width) * FOVEA_XSIZE);
		height_pixel	= (int) (((double) height) * FOVEA_YSIZE);
	
		fovea_xmin	= (width >> 1) - with_pixel;
		fovea_xmax	= (width >> 1) + with_pixel;
		fovea_ymin	= (height >> 1) - height_pixel;
		fovea_ymax	= (height >> 1) + height_pixel;

		dwg.drawBox (fovea_xmin, fovea_ymin, fovea_xmax, fovea_ymax, Color.WHITE.getRGB ());
	}
	
	protected boolean checkFovea (Blob blob)
	{
	 	return (blob.getX() >= fovea_xmin) && (blob.getX() <= fovea_xmax) && (blob.getY() >= fovea_ymin) && (blob.getY() <= fovea_ymax);
	}

}
