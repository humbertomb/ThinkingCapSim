/**
 * Created on 24-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.gui.ctables.colspace;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.gui.images.*;

public class SpaceCylindricalProjection extends SpaceOrtoProjection
{	
	protected BufferedImageDrawing	dwg = new BufferedImageDrawing ();

	public SpaceCylindricalProjection (Channels chs, Color color)
	{
		super (chs, color);
	}
	
	public BufferedImage project (BufferedImage iproj, BufferedImage irgb, int mode, boolean dchans)
	{
		int				x, y;
		int				pix;
		int				co0, co1, co2;
		double			rho, theta;
		double			offset;
		int				xx, yy;
		int				black;
		BufferedImage	output;
		
		output = new BufferedImage (SIZE, SIZE, BufferedImage.TYPE_INT_RGB);		
		for (y = 0; y < SIZE; y++)
			for (x = 0; x < SIZE; x++)
				output.setRGB (x, y, bkg);
			
		offset	= SIZE * 0.5;
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
					rho		= (double) co2 / 255.0 * SIZE * 0.5;
					theta	= ((double) co1 / 255.0) * Math.PI * 2.0;
					break;				
					
				case PROJ02:
					rho		= (double) co2 / 255.0 * SIZE * 0.5;
					theta	= ((double) co0 / 255.0) * Math.PI * 2.0;
					break;			
					
				case PROJ01:
				default:
					rho		= (double) co1 / 255.0 * SIZE * 0.5;
					theta	= ((double) co0 / 255.0) * Math.PI * 2.0;
					break;
				}
				xx		= (int) (offset + rho * Math.cos (theta));
				yy		= (int) (offset + rho * Math.sin (theta));
				if ((xx >= 0) && (xx < SIZE) && (yy >= 0) && (yy < SIZE))
					output.setRGB (xx, yy, irgb.getRGB (x, y));
			}

		
		black	= Color.BLACK.getRGB ();
		for (y = 0; y < SIZE; y++)
			output.setRGB ((int) offset, y, black);		
		for (x = 0; x < SIZE; x++)
			output.setRGB (x, (int) offset, black);
		
		if (dchans)
			drawChannels (output, mode);
		
		return output;
	}
	
	protected void drawChannels (BufferedImage image, int mode)
	{
		int				offset;
		double			rhmin, rhmax;
		double			thmin, thmax;

		dwg.updateImage (image);
		for (int i = 0; i < chs.getNumChannels(); i++)
		{
			int			color;

			Channel ch = chs.getChannelID (i);
			if (!ch.segmented) continue;
							
			color	= ch.color.getRGB();
			offset	= SIZE / 2;
			if (ch.getCluster() instanceof ColorPrism)
			{
				ColorPrism	prism;
				
				prism = (ColorPrism) ch.getCluster ();
				switch (mode)
				{
				case PROJ12:
					rhmin	= (double) prism.getMin2 () / 255.0 * SIZE * 0.5;
					thmin	= ((double) prism.getMin1 () / 255.0) * Math.PI * 2.0;
					rhmax	= (double) prism.getMax2 () / 255.0 * SIZE * 0.5;
					thmax	= ((double) prism.getMax1 () / 255.0) * Math.PI * 2.0;
					break;
					
				case PROJ02:
					rhmin	= (double) prism.getMin2 () / 255.0 * SIZE * 0.5;
					thmin	= ((double) prism.getMin0 () / 255.0) * Math.PI * 2.0;
					rhmax	= (double) prism.getMax2 () / 255.0 * SIZE * 0.5;
					thmax	= ((double) prism.getMax0 () / 255.0) * Math.PI * 2.0;
					break;
					
				case PROJ01:
				default:
					rhmin	= (double) prism.getMin1 () / 255.0 * SIZE * 0.5;
					thmin	= ((double) prism.getMin0 () / 255.0) * Math.PI * 2.0;
					rhmax	= (double) prism.getMax1 () / 255.0 * SIZE * 0.5;
					thmax	= ((double) prism.getMax0 () / 255.0) * Math.PI * 2.0;
					break;
				}
				dwg.drawSector (offset, offset, (int) rhmin, thmin, (int) rhmax, thmax, color);
			}
		}	
	}
}
