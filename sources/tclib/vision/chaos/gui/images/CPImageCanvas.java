/**
 * @author David Herrero Perez
 * @author Humberto Martinez Barbera
 * 
 * Copyright (c) 2004 University of Murcia (Spain) and Team Chaos, Sweden and Spain.
 * All Rights Reserved.
 * 
 */

package tclib.vision.chaos.gui.images;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import tclib.vision.chaos.gui.*;
import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.*;

public class CPImageCanvas extends JPanel
{		
	static protected Font				FSMALL = new Font ("Courier", Font.PLAIN, 12);
	
	protected BufferedImage				imageTake;
	protected BufferedImage				imagePaint;	
	protected Channel					seeds;
	
	private boolean						showres = false;
	private boolean						showid = false;
	private Color						rescolor = Color.WHITE;
	private String						id = null;
		
	public CPImageCanvas ()
	{
		this (false, Color.WHITE, false, null);
	}
							
	public CPImageCanvas (boolean showres, Color rescolor, boolean showid, String id)
	{
		this.showres	= showres;
		this.showid		= showid;
		this.rescolor	= rescolor;
		this.id			= id;
		
		setMinimumSize (new Dimension (ChaosVisionWindow.GUI_IMAGE_WIDTH, ChaosVisionWindow.GUI_IMAGE_HEIGHT));
		setPreferredSize (new Dimension (ChaosVisionWindow.GUI_IMAGE_WIDTH, ChaosVisionWindow.GUI_IMAGE_HEIGHT));

		initialiseImages ();
		
		setCursor (new Cursor(Cursor.CROSSHAIR_CURSOR));
	}
							
	public void updateSeeds (Channel seeds)
	{
		this.seeds = seeds;
	}
		
	private void initialiseImages ()
	{
		imageTake = new BufferedImage (ChaosVisionWindow.GUI_IMAGE_WIDTH, ChaosVisionWindow.GUI_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		imagePaint = new BufferedImage (ChaosVisionWindow.GUI_IMAGE_WIDTH, ChaosVisionWindow.GUI_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		for (int y = 0; y < imageTake.getHeight(); y++)
			for (int x = 0; x < imageTake.getWidth(); x++)
			{
				imageTake.setRGB (x, y, Color.GRAY.getRGB());
				imagePaint.setRGB (x, y, Color.GRAY.getRGB());
			}
	}
				
	public void updateBufferedImage (BufferedImage image)
	{
		if (image != null)
		{
			imageTake	= image;	
			imagePaint	= new BufferedImage (image.getWidth(), image.getHeight (), BufferedImage.TYPE_INT_RGB);
			imagePaint.setData (imageTake.getData ());
		}
		else
		{
			imageTake	= null;
			imagePaint	= null;
		}
		
//		if ((clistener != null) && clistener.isSelectedViewSeeds() /*&& (imageformat == CPImageFormat.MODE_RGB)*/)
//			putSeedsInImage();
		
		repaint ();
	}
		
	protected void putSeedsInImage()
	{
		int		x, y;
		int		hsv;
		
		if (seeds == null)			return;
		
		for (y = 0; y < imagePaint.getHeight(); y++)
			for (x = 0; x < imagePaint.getWidth(); x++)
			{
				hsv = Segmentation.rgbToHsv(imageTake.getRGB(x,y));
							
				if (seeds.insideChannel (hsv))
					imagePaint.setRGB(x, y, Color.BLACK.getRGB());
			}
	}
	
	public void repaint ()
	{
		if (imagePaint == null)
			initialiseImages ();

		super.repaint ();
	}
	
	public void paint (Graphics g)
	{
		int				sizex, sizey;
		int				imgx, imgy;
		int				offx, offy;
		Dimension		dim;
		BufferedImage	image;
		
		// Compute size constants
		image	= imagePaint;
		dim		= getSize ();
		sizex	= (int) dim.getWidth ();
		sizey	= (int) dim.getHeight ();
		imgx	= image.getWidth ();
		imgy	= image.getHeight ();
		offx	= Math.max ((sizex - imgx) / 2, 0);
		offy	= Math.max ((sizey - imgy) / 2, 0);
		
		// Erase previous frame
		g.setColor (getBackground ());
		g.fillRect (0, 0, sizex, sizey);
		
		// Scale if the image is larger than the viewing area
		if ((imgx > sizex) || (imgy > sizey))
		{	
			image	= CPScalableImageCanvas.scaleImage (image, sizex, sizey);
			imgx	= image.getWidth ();
			imgy	= image.getHeight ();
		}
		
		// Draw image centered in the viewing area
		offx	= Math.max ((sizex - imgx) / 2, 0);
		offy	= Math.max ((sizey - imgy) / 2, 0);
		((Graphics2D) g).drawImage (image, offx, offy, imgx, imgy, (ImageObserver) this);		
		
		// Show image resolution
		if (showres)
		{
			g.setColor (rescolor);
			g.setFont (FSMALL);
			g.drawString ((showid ? id+" " : "")+imagePaint.getWidth ()+"x"+imagePaint.getHeight (), offx+10, offy+10);
		}
	}	
}
