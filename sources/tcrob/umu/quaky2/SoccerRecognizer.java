/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

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

	// The segmented channels each object is looked for in (their index in the channels of the configuration)
	public int						CARPET_CHANNEL	= 3;				// the floor, which the horizon is found from
	public int						BALL_CHANNEL	= 0;
	public int						NET1_CHANNEL	= 1;
	public int						NET2_CHANNEL	= 2;
	public int						LM_CHANNEL		= 4;				// the pink of the landmarks

	public int 						BALL_SX_MIN		= 2;				// Minimum reliable size in image (pix)
	public int 						BALL_SY_MIN		= 2;				// was 5 --AS 020618
	public int 						BALL_HORIZ_HGT	= 20;
	public int 						BALL_DENSITY	= 2;
	public int 						BALL_XDISP		= -8;
	public int						BALL_YDISP		= -18;
	public int 						NET_SX_MIN		= 16;				// Minimum reliable size in image (pix)
	public int 						NET_SY_MIN		= 10;
	public int 						NET_HORIZ_HGT	= 40;
	public int 						NET_DENSITY		= 2;
	public int 						NET_IN_MINX		= 130;				// we are inside net if we see blobs this big
	public int 						NET_IN_MINY		= 110;				// ...
	public int 						NET_IN_MINA		= 120;				// all around us at this angle
	public int 						NET_IN_MEMO		= 2000;				// during this time	
	public int 						LM_SX_MIN		= 3;				// Minimum reliable size in image (pix)  //-- ZW
	public int 						LM_SY_MIN		= 3;				// resolution: 10pix~=10cm, 15pix~=5cm
	public int 						LM_HORIZ_HGT	= -20;
	public int 						LM_DENSITY		= 10;

	static private final Blobs		NO_BLOBS	= new Blobs ();		// the blobs of a channel that is not there

	private BufferedImageDrawing	dwg = new BufferedImageDrawing ();

	public SoccerRecognizer ()
	{
		
	}

	public BufferedImage process (BufferedImage input, int[] segmented, Blobs[] blobs, Channels channels, SoccerVisionConfig config)
	{
		BufferedImage		output;
		VisualHorizon		horizon;
		Blobs				pinks;
		
		output	= new BufferedImage (input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		output.setData (input.getData ());
		dwg.updateImage (output);
		
		computeFovea (output, dwg);
		horizon = new VisualHorizon ();
		// a channel that is not there (fewer channels than the one chosen) is not looked for
		if (has (channels, blobs, CARPET_CHANNEL) && has (channels, blobs, BALL_CHANNEL))
			horizon.findHorizon (output, segmented, channels.at (CARPET_CHANNEL), channels.at (BALL_CHANNEL));

		if (has (channels, blobs, BALL_CHANNEL))
		{
			CircleFitting	ellipse = new CircleFitting ();

			for (int i = 0; i < blobs[BALL_CHANNEL].getBlobNumber (); i++)
			{
				Blob		blob = blobs[BALL_CHANNEL].getBlob (i);
				if (testValidBall (blob, config, horizon, dwg))
					ellipse.doFitting (output, segmented, blob, channels.at (BALL_CHANNEL));
			}
		}
		
		pinks	= has (channels, blobs, LM_CHANNEL) ? blobs[LM_CHANNEL] : NO_BLOBS;
		for (int n : new int[] { NET1_CHANNEL, NET2_CHANNEL })
		{
			NetFitting	net = new NetFitting ();
			int			color;

			if (!has (channels, blobs, n) || !has (channels, blobs, CARPET_CHANNEL))		continue;
			color	= channels.at (n).color.getRGB ();					// the box of a net in the colour of its channel
			for (int i = 0; i < blobs[n].getBlobNumber (); i++)
			{
				Blob		blob = blobs[n].getBlob (i);
				if (!testPinkOverlap (blob, pinks) && testValidNet (blob, config, horizon, dwg, color))
					net.doFitting (output, segmented, blobs[n], channels.at (n), channels.at (CARPET_CHANNEL));
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
		if ((blob.getSizeX() < BALL_SX_MIN) || (blob.getSizeY() < BALL_SY_MIN))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > BALL_DENSITY)
			return false;
		if (!horizon.isBelowHorizont (blob, BALL_HORIZ_HGT))
			return false;
//		if (!checkFovea (blob))
//			return false;
		
		dwg.drawBox (blob.getXMin(), blob.getYMin(), blob.getXMax(), blob.getYMax(), Color.ORANGE.getRGB ());
		return true;
	}
	
	protected boolean testValidNet (Blob blob, SoccerVisionConfig config, VisualHorizon horizon, BufferedImageDrawing dwg, int color)
	{
		if ((blob.getSizeX() < NET_SX_MIN) || (blob.getSizeY() < NET_SY_MIN))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > NET_DENSITY)
			return false;
		if (!horizon.isAboveHorizont (blob, NET_HORIZ_HGT))
			return false;
//		if (!checkFovea (blob))
//			return false;
	
		dwg.drawBox (blob.getXMin(), blob.getYMin(), blob.getXMax(), blob.getYMax(), color);
		return true;
	}
	
	protected boolean testValidLandmark (Blob blob, Blobs pinks, SoccerVisionConfig config, VisualHorizon horizon, BufferedImageDrawing dwg, int color)
	{
		Blob			lmark;
		
		if ((blob.getSizeX() < LM_SX_MIN) || (blob.getSizeY() < LM_SY_MIN))
			return false;
		if (	blob.getArea () / blob.getNumPixels () > LM_DENSITY)
			return false;
		if (!horizon.isAboveHorizont (blob, LM_HORIZ_HGT))
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
		if (((blob_pink.getSizeX()) < LM_SX_MIN) || ((blob_pink.getSizeY()) < LM_SY_MIN ))
		{
			return null;
		}
		
		// If density is too small for this object, reject it
		if (blob_pink.getArea () / blob_pink.getNumPixels () > LM_DENSITY)
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
	
	public void fromFile (String name)
	{
		try
		{
			FileInputStream fd = new FileInputStream (name);
			Properties p = new Properties();
			
			p.load(fd);
						
			CARPET_CHANNEL	= Integer.valueOf (p.getProperty ("CARPET_CHANNEL", Integer.toString (CARPET_CHANNEL))).intValue ();
			BALL_CHANNEL	= Integer.valueOf (p.getProperty ("BALL_CHANNEL", Integer.toString (BALL_CHANNEL))).intValue ();
			NET1_CHANNEL	= Integer.valueOf (p.getProperty ("NET1_CHANNEL", Integer.toString (NET1_CHANNEL))).intValue ();
			NET2_CHANNEL	= Integer.valueOf (p.getProperty ("NET2_CHANNEL", Integer.toString (NET2_CHANNEL))).intValue ();
			LM_CHANNEL		= Integer.valueOf (p.getProperty ("LM_CHANNEL", Integer.toString (LM_CHANNEL))).intValue ();

			BALL_SX_MIN		= Integer.valueOf (p.getProperty ("BALL_SX_MIN", Integer.toString (BALL_SX_MIN))).intValue ();
			BALL_SY_MIN		= Integer.valueOf (p.getProperty ("BALL_SY_MIN", Integer.toString (BALL_SY_MIN))).intValue ();
			BALL_HORIZ_HGT	= Integer.valueOf (p.getProperty ("BALL_HORIZ_HGT", Integer.toString (BALL_HORIZ_HGT))).intValue ();
			BALL_DENSITY	= Integer.valueOf (p.getProperty ("BALL_DENSITY", Integer.toString (BALL_DENSITY))).intValue ();
			BALL_XDISP		= Integer.valueOf (p.getProperty ("BALL_XDISP", Integer.toString (BALL_XDISP))).intValue ();
			BALL_YDISP		= Integer.valueOf (p.getProperty ("BALL_YDISP", Integer.toString (BALL_YDISP))).intValue ();

			NET_SX_MIN		= Integer.valueOf (p.getProperty ("NET_SX_MIN", Integer.toString (NET_SX_MIN))).intValue ();
			NET_SY_MIN		= Integer.valueOf (p.getProperty ("NET_SY_MIN", Integer.toString (NET_SY_MIN))).intValue ();
			NET_HORIZ_HGT	= Integer.valueOf (p.getProperty ("NET_HORIZ_HGT", Integer.toString (NET_HORIZ_HGT))).intValue ();
			NET_DENSITY		= Integer.valueOf (p.getProperty ("NET_DENSITY", Integer.toString (NET_DENSITY))).intValue ();
			NET_IN_MINX		= Integer.valueOf (p.getProperty ("NET_IN_MINX", Integer.toString (NET_IN_MINX))).intValue ();
			NET_IN_MINY		= Integer.valueOf (p.getProperty ("NET_IN_MINY", Integer.toString (NET_IN_MINY))).intValue ();
			NET_IN_MINA		= Integer.valueOf (p.getProperty ("NET_IN_MINA", Integer.toString (NET_IN_MINA))).intValue ();
			NET_IN_MEMO		= Integer.valueOf (p.getProperty ("NET_IN_MEMO", Integer.toString (NET_IN_MEMO))).intValue ();

			LM_SX_MIN		= Integer.valueOf (p.getProperty ("LM_SX_MIN", Integer.toString (LM_SX_MIN))).intValue ();
			LM_SY_MIN		= Integer.valueOf (p.getProperty ("LM_SY_MIN", Integer.toString (LM_SY_MIN))).intValue ();
			LM_HORIZ_HGT	= Integer.valueOf (p.getProperty ("LM_HORIZ_HGT", Integer.toString (LM_HORIZ_HGT))).intValue ();
			LM_DENSITY		= Integer.valueOf (p.getProperty ("LM_DENSITY", Integer.toString (LM_DENSITY))).intValue ();

			fd.close();
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	public void toFile (String name)
	{
		try
		{
			FileOutputStream fd = new FileOutputStream(new File (name), true);
			
			String aux = "CARPET_CHANNEL = " + CARPET_CHANNEL + "\n";
			aux = aux + "BALL_CHANNEL = " + BALL_CHANNEL + "\n";
			aux = aux + "NET1_CHANNEL = " + NET1_CHANNEL + "\n";
			aux = aux + "NET2_CHANNEL = " + NET2_CHANNEL + "\n";
			aux = aux + "LM_CHANNEL = " + LM_CHANNEL + "\n";

			aux = aux + "BALL_SX_MIN = " + Integer.valueOf (BALL_SX_MIN).toString () + "\n";
			aux = aux + "BALL_SY_MIN = " + Integer.valueOf (BALL_SY_MIN).toString () + "\n";
			aux = aux + "BALL_HORIZ_HGT = " +Integer.valueOf (BALL_HORIZ_HGT).toString () + "\n";
			aux = aux + "BALL_DENSITY = " + Integer.valueOf (BALL_DENSITY).toString () + "\n";
			aux = aux + "BALL_XDISP = " + Integer.valueOf (BALL_XDISP).toString () + "\n";
			aux = aux + "BALL_YDISP = " + Integer.valueOf (BALL_YDISP).toString () + "\n";

			aux = aux + "NET_SX_MIN = " + Integer.valueOf (NET_SX_MIN).toString () + "\n";
			aux = aux + "NET_SY_MIN = " + Integer.valueOf (NET_SY_MIN).toString () + "\n";
			aux = aux + "NET_HORIZ_HGT = " + Integer.valueOf (NET_HORIZ_HGT).toString () + "\n";
			aux = aux + "NET_DENSITY = " + Integer.valueOf (NET_DENSITY).toString () + "\n";
			aux = aux + "NET_IN_MINX = " + Integer.valueOf (NET_IN_MINX).toString () + "\n";
			aux = aux + "NET_IN_MINY = " + Integer.valueOf (NET_IN_MINY).toString () + "\n";
			aux = aux + "NET_IN_MINA = " + Integer.valueOf (NET_IN_MINA).toString () + "\n";
			aux = aux + "NET_IN_MEMO = " + Integer.valueOf (NET_IN_MEMO).toString () + "\n";

			aux = aux + "LM_SX_MIN = " + Integer.valueOf (LM_SX_MIN).toString () + "\n";
			aux = aux + "LM_SY_MIN = " + Integer.valueOf (LM_SY_MIN).toString () + "\n";
			aux = aux + "LM_HORIZ_HGT = " + Integer.valueOf (LM_HORIZ_HGT).toString () + "\n";
			aux = aux + "LM_DENSITY = " + Integer.valueOf (LM_DENSITY).toString () + "\n";

			aux = aux + "\n";
						
			fd.write (aux.getBytes());
			fd.close ();
		}
		catch (Exception e) { e.printStackTrace (); }
	}
}
