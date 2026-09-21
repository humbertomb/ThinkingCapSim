/*
 * Created on 29-jul-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.gui.images;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

import tclib.vision.chaos.channels.Channel;
import tclib.vision.chaos.segment.Segmentation;

public class CPZoomCanvas extends JPanel
{
	static public final int		WINDOW = 10;
	static public final int		WIDTH = 2*WINDOW+1;
	static public final int		HEIGHT = 2*WINDOW+1;
	
	public CPScalableImageCanvas	image;
	private BufferedImage		zoom;
	private BufferedImage		normal;
	private Channel				seeds;
	private int					gray, black, white, sgray;
	private int					lx = -1;
	private int					ly = -1;

	public CPZoomCanvas ()
	{
		int			i, j;
		
		zoom		= new BufferedImage (WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		gray		= Color.gray.getRGB ();
		sgray	= Color.gray.brighter ().getRGB ();
		black	= Color.black.getRGB ();
		white	= Color.white.getRGB ();
		for (i = 0; i < WIDTH; i++)
			for (j = 0; j < HEIGHT; j++)
				zoom.setRGB (i, j, gray);
	
		image	= new CPScalableImageCanvas ();
		image.setImage (zoom);

		setLayout (new GridLayout (1, 1));
		setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Zoom", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		setPreferredSize (new Dimension (100, 100));
		add (image);

		setVisible (true);
	}
	
	public void updateBufferedImage (BufferedImage bimage)
	{
		normal	= bimage;
	}
	
	public void updateSeeds (Channel seeds)
	{
		int			x, y;
		
		this.seeds = seeds;
		x = lx;
		y = ly;
		lx = -1;
		ly = -1;
		pixel (x, y, 0);
	}

	public void pixel (int x, int y, int value)
	{
		int			i, j;
		int			zx, zy;
		int			hsv;
		
		if (normal == null)				return;
		if ((x == lx) && (y == ly))		return;
		
		for (i = x-WINDOW, zx = 0; i <= x+WINDOW; i++, zx++)
			for (j = y-WINDOW, zy = 0; j <= y+WINDOW; j++, zy++)
			{
				if (((i != x) || (j != y)) && ((i == x) || (j == y)))
					zoom.setRGB (zx, zy, white);
				else if ((i < 0) || (j < 0) || (i > normal.getWidth()-1) || (j > normal.getHeight()-1))
					zoom.setRGB (zx, zy, gray);
				else
				{
					hsv = Segmentation.rgbToHsv (normal.getRGB (i, j));
					if ((seeds != null) && seeds.isSeed (hsv))
						zoom.setRGB (zx, zy, sgray);
					else if ((seeds != null) && seeds.insideChannel (hsv))
						zoom.setRGB (zx, zy, black);
					else
						zoom.setRGB (zx, zy, normal.getRGB (i, j));	
				}
			}
		image.updateImage (zoom);
		
		lx = x;
		ly = y;
	}
}
