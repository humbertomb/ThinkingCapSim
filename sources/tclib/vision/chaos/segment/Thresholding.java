/**
 * Created on 04-ago-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import java.awt.image.*;

import tclib.vision.chaos.channels.*;

public class Thresholding extends Segmentation
{
	public void process (BufferedImage input, LUT lut, Channels chs)
	{		
		int 				rgb;
		int				xx, yy, ch;
		int				pixel, size;
				
		this.input 		= input;
		this.channels	= chs;
	
		// Perform tables initialisation
		size = input.getHeight () * input.getWidth ();
		if ((segmented == null) || (segmented.length != size))
			segmented = new int [size];

		// Perform segmentation
		for (yy = 0; yy < input.getHeight(); yy++)
			for (xx = 0; xx < input.getWidth(); xx++)
			{
				pixel = (yy * input.getWidth()) + xx;				
				rgb	= input.getRGB (xx, yy);			
				ch	= lut.lookup (rgb);
				
				if (ch >= 0)
					segmented[pixel] = chs.at(ch).id;
				else
					segmented[pixel] = Channels.NO_COLOR;
			}
	}
		
	public boolean configurable ()
	{
		return false;
	}
}
