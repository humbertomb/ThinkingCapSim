/**
 * Created on 08-may-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.gui.images;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

public class CPScalableImageCanvas extends JPanel 
{
	protected BufferedImage image;
	
	static public BufferedImage scaleImage (BufferedImage input, int width, int height) 
	{
		int			type;
		double		scale;
		double		facx, facy;
		
		facx		= (double) width / (double) input.getWidth ();
		facy		= (double) height / (double) input.getHeight ();
		scale	= Math.min (facx, facy);
		type		= (scale > 1.0 ? AffineTransformOp.TYPE_NEAREST_NEIGHBOR : AffineTransformOp.TYPE_BILINEAR);

		AffineTransform tx = new AffineTransform();
		tx.scale (scale, scale);		
		AffineTransformOp op = new AffineTransformOp(tx, type);
		return op.filter (input, null);
	}

	public void setImage (BufferedImage image)
	{
		this.image	= image;
		setSize (new Dimension (image.getWidth(), image.getHeight()));

		repaint ();
	}
	
	public void updateImage (BufferedImage image)
	{
		this.image	= image;

		repaint ();
	}
	
	public void clearImage ()
	{
		image	= null;
		repaint ();
	}

	public void paint (Graphics g)
	{
		int				width, height;
		
		width	= (int) getSize ().getWidth ();
		height	= (int) getSize ().getHeight ();
		
		if (image == null)
		{
			g.setColor (getBackground ());
			g.fillRect (0, 0, width, height);
		}
		else
		{
			int				iw, ih;
			int				ox = 0, oy = 0;
			BufferedImage	output;
			
			output	= scaleImage (image, width, height);
			iw		= output.getWidth ();
			ih		= output.getHeight ();
			
			if (iw < width)
				ox	= (width - iw) / 2;
			if (ih < height)
				oy	= (height - ih) / 2;
			
			((Graphics2D) g).drawImage (output, null, ox, oy);
		}
	}
}
