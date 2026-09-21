/**
 * Created on 24-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui.ctables.colspace;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.gui.images.*;
import tcrob.umu.quaky2.gui.images.BufferedImageDrawing;

public class SpaceOrtoProjection
{
	static public final int			PROJ12	 = 0;
	static public final int			PROJ02	 = 1;
	static public final int			PROJ01	 = 2;

	static public final int			SIZE	 	= 256;
	
	protected Channels				chs;
	protected int					bkg;
	
	protected BufferedImageDrawing	dwg = new BufferedImageDrawing ();
	
	public SpaceOrtoProjection (Channels chs, Color color)
	{
		this.chs		= chs;
		this.bkg		= color.getRGB ();
	}
	
	public BufferedImage project (BufferedImage iproj, BufferedImage irgb, int mode, boolean dchans)
	{
		int				x, y;
		int				pix;
		int				co0, co1, co2;
		BufferedImage	output;
		
		output = new BufferedImage (SIZE, SIZE, BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < SIZE; y++)
			for (x = 0; x < SIZE; x++)
				output.setRGB (x, y, bkg);
			
		for (y = 0; y < iproj.getHeight(); y++)
			for (x = 0; x < iproj.getWidth(); x++)
			{
				pix	= iproj.getRGB (x,y);
				co0	= Pixel.getComponent0 (pix);
				co1	= Pixel.getComponent1 (pix);
				co2	= Pixel.getComponent2 (pix);
				
				switch (mode)
				{
				case PROJ12:
					output.setRGB (co1, co2, irgb.getRGB (x, y));
					break;				
				case PROJ02:
					output.setRGB (co0, co2, irgb.getRGB (x, y));
					break;				
				case PROJ01:
					output.setRGB (co0, co1, irgb.getRGB (x, y));
					break;
				}
			}
		
		if (dchans)
			drawChannels (output, mode);
		
		return output;
	}
	
	protected void drawChannels (BufferedImage image, int mode)
	{
		dwg.updateImage (image);
		for (int i = 0; i < chs.getNumChannels(); i++)
		{
			int			color;

			Channel ch = chs.getChannelID (i);
			if (!ch.segmented) continue;
							
			color	= ch.color.getRGB();
			if (ch.getCluster() instanceof ColorPrism)
			{
				ColorPrism	prism;
				
				prism = (ColorPrism) ch.getCluster ();
				switch (mode)
				{
				case PROJ12:
					dwg.drawBox (prism.getMin1 (), prism.getMin2 (), prism.getMax1 (), prism.getMax2 (), color);
					break;					
				case PROJ02:
					dwg.drawBox (prism.getMin0 (), prism.getMin2 (), prism.getMax0 (), prism.getMax2 (), color);
					break;				
				case PROJ01:
					dwg.drawBox (prism.getMin0 (), prism.getMin1 (), prism.getMax0 (), prism.getMax1 (), color);
					break;
				}
			}
		}	
	}
}
