/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.recognize.*;
import tcrob.umu.quaky2.gui.images.BufferedImageDrawing;

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

		public String toString ()
		{
			return "(" + x + "," + y + ") [" + xmin + ".." + xmax + " x " + ymin + ".." + ymax + "] " + pixels + " px"
					+ String.format (" circle (%.0f,%.0f) r=%.0f%s", cx, cy, radius, round ? "" : " (box)");
		}
	}

	// What was recognised in the last frame (null: not seen): the largest blob taken for each object
	public Detection				ball;
	public Detection				net1;
	public Detection				net2;

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
		Blobs				pinks;
		
		output	= new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		output.setData (input.getData ());
		dwg.updateImage (output);
		
		ball	= null;
		net1	= null;
		net2	= null;

		computeFovea (output, dwg);
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
		
		pinks	= has (channels, blobs, params.lm_channel) ? blobs[params.lm_channel] : NO_BLOBS;
		for (int n : new int[] { params.net1_channel, params.net2_channel })
		{
			NetFitting	net = new NetFitting ();
			int			color;

			if (!has (channels, blobs, n) || !has (channels, blobs, params.carpet_channel))		continue;
			color	= channels.at (n).color.getRGB ();					// the box of a net in the colour of its channel
			for (int i = 0; i < blobs[n].getBlobNumber (); i++)
			{
				Blob		blob = blobs[n].getBlob (i);
				if (!testPinkOverlap (blob, pinks) && testValidNet (blob, config, horizon, dwg, color))
				{
					net.doFitting (output, segmented, blobs[n], channels.at (n), channels.at (params.carpet_channel));
					if (n == params.net1_channel)
					{
						if ((net1 == null) || (blob.getNumPixels () > net1.pixels))		net1 = new Detection (blob);
					}
					else if ((net2 == null) || (blob.getNumPixels () > net2.pixels))	net2 = new Detection (blob);
				}
				else
					testValidLandmark (blob, pinks, config, horizon, dwg, color);
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
	
	protected boolean testValidLandmark (Blob blob, Blobs pinks, SoccerVisionConfig config, VisualHorizon horizon, BufferedImageDrawing dwg, int color)
	{
		Blob			lmark;
		
		if ((blob.getSizeX() < params.lm_sx_min) || (blob.getSizeY() < params.lm_sy_min))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > params.lm_density)
			return false;
		if (!horizon.isAboveHorizont (blob, params.lm_horiz_hgt))
			return false;
	
		for (int i = 0; i < pinks.getBlobNumber(); ++i)
		{
			Blob			pink;
			
			pink		= pinks.getBlob (i);
			lmark	= checkBlobPair (blob, pink, config);
			if (lmark != null)
			{
				dwg.drawBox (lmark.getXMin(), lmark.getYMin(), lmark.getXMax(), lmark.getYMax(), Color.PINK.getRGB ());
				dwg.drawLine (lmark.getXMin(), lmark.getY(), lmark.getXMax(), lmark.getY(), color);
				return true;
			}
		}
		
		return false;
	}
	
	protected boolean testPinkOverlap (Blob blob, Blobs pinks)
	{
		for (int i = 0; i < pinks.getBlobNumber(); ++i)
		{
			Blob			pink;
			int			xoverlap, temp;
			int			minsizx, relation;
			
			pink		= pinks.getBlob (i);
			minsizx	= Math.min (blob.getSizeX(), pink.getSizeX());
			xoverlap	= blob.getXMax() - pink.getXMin();
			temp		= pink.getXMax() - blob.getXMin();
			
			if (pink.getSizeY() == 0)					continue;
			relation	= blob.getSizeY() / pink.getSizeY();
			
			
			if (temp < xoverlap)		xoverlap = temp;
			
			if (minsizx < xoverlap)
				xoverlap = minsizx;
			else if (xoverlap < 0)
				xoverlap = 0;
			
			if ((xoverlap != 0) && (relation < 3))		return true;
		}
		
		return false;
	}

	protected Blob checkBlobPair (Blob blob_color, Blob blob_pink, SoccerVisionConfig config)
	{
		// If blob is TOO SMALL for this object, reject it
		if (((blob_pink.getSizeX()) < params.lm_sx_min) || ((blob_pink.getSizeY()) < params.lm_sy_min ))
		{
			return null;
		}
		
		// If density is too small for this object, reject it
		if (blob_pink.getArea () / blob_pink.getNumPixels () > params.lm_density)
		{
			return null;
		}
		
		// Calculate differences between blobs
		int xsizediff, ysizediff, xoverlap, xgap, yoverlap, ygap, temp;
		int min_sizex = Math.min(blob_color.getSizeX(), blob_pink.getSizeX());
		int min_sizey = Math.min(blob_color.getSizeY(), blob_pink.getSizeY());
				
		xsizediff = (blob_color.getSizeX())-(blob_pink.getSizeX());
		if (xsizediff < 0) xsizediff = -xsizediff;
		
		ysizediff = (blob_color.getSizeY())-(blob_pink.getSizeY());
		if (ysizediff < 0) ysizediff = -ysizediff;
		
		xoverlap	= (blob_color.getXMax()) - (blob_pink.getXMin());
		temp		= (blob_pink.getXMax()) - (blob_color.getXMin());
		if (temp < xoverlap)			xoverlap = temp;
		
		if (min_sizex < xoverlap)
			xoverlap = min_sizex;
		else if (xoverlap < 0)
			xoverlap = 0;
		
		yoverlap	= (blob_color.getYMax()) - (blob_pink.getYMin());
		temp		= (blob_pink.getYMax()) - (blob_color.getYMin());
		if (temp < yoverlap)			yoverlap = temp;
		
		if (min_sizey < yoverlap)
			yoverlap = min_sizey;
		else if (yoverlap < 0)
			yoverlap = 0;
		
		xgap = (blob_color.getXMin()) - (blob_pink.getXMax());
		temp = (blob_pink.getXMin()) - (blob_color.getXMax());
		if (temp > xgap)				xgap = temp;
		if (xgap < 0)				xgap = 0;
		
		ygap = (blob_color.getYMin()) - (blob_pink.getYMax());
		temp = (blob_pink.getYMin()) - (blob_color.getYMax());
		if (temp > ygap)				ygap = temp;
		if (ygap < 0)				ygap = 0;
				
		// Size of two blobs should be similar
		if((xsizediff*100) > (XSIZEDIFF_MAX * min_sizex))
		{
			return null;
		}
		
		if((ysizediff*100) > (YSIZEDIFF_MAX * min_sizey))
		{
			return null;
		}
		
		// Check for gap or lack of overlap in x direction
		if ((xoverlap*100) < (XOVERLAP_MIN*min_sizex))
		{
			return null;
		}
		
		if ((xgap*100) > (XGAP_MAX*min_sizex))
		{
			return null;
		}
		
		// Check for gap or overlap in y direction
		if ((yoverlap*100) > (YOVERLAP_MAX*min_sizey))
		{
			return null;
		}
		
		if (ygap > YGAP_MAX)
		{
			return null;
		}

		// Ok. It is possible to make new blob of wanted LM.
		Blob		blob;
		Blob		blob_up, blob_down;
		int		mymin, mymax;
		
		if (blob_color.getY () < blob_pink.getY ())
		{
			blob_up	= blob_color;
			blob_down = blob_pink;
		}
		else
		{
			blob_up	= blob_pink;
			blob_down = blob_color;
		}
		
		if (blob_down.getSizeY() < blob_up.getSizeY())
		{
			mymin = blob_up.getYMin();
			mymax = mymin + 2 * blob_up.getSizeY();
		}
		else
		{
			mymax = blob_down.getYMax();
			mymin = mymax - 2 * blob_down.getSizeY();
		}
		
		blob = new Blob ();
		blob.setXMin (Math.min (blob_down.getXMin(), blob_up.getXMin()));
		blob.setXMax (Math.max (blob_down.getXMax(), blob_up.getXMax()));	
		blob.setYMin (mymin);
		blob.setYMax (mymax);
		blob.setSizeY (blob.getYMax()-blob.getYMin());
		blob.setSizeX (blob.getXMax()-blob.getXMin());
		blob.setX ((blob_down.getX() + blob_up.getX()) >> 1);
		blob.setY ((blob_down.getY() + blob_up.getY()) >> 1);
		
		return blob;
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
