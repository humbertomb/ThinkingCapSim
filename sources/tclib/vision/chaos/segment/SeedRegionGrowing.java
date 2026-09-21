/**
 * Created on 04-ago-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import tclib.vision.chaos.channels.*;

public class SeedRegionGrowing extends Thresholding
{
	static protected final String[]	DMODES		= { "Box", "Euclidean" };

	static public final int 			M_BOX		= 0;
	static public final int 			M_EUCLIDEAN	= 1;

	static protected int				MODE			= M_EUCLIDEAN;

	private int[]					xTable;
	private int[]					yTable;
	private boolean[]				selected;

	public void process (BufferedImage input, LUT lut, Channels chs)
	{		
		int 				yuv;
		int				xx, yy, ch;
		int				pixel, size;
				
		this.input 		= input;
		this.channels	= chs;

		// Perform tables initialisation
		size = input.getHeight () * input.getWidth ();
		if ((segmented == null) || (segmented.length != size))
		{
			xTable			= new int [size];
			yTable			= new int [size];
			segmented		= new int [size];
			selected			= new boolean [size];
		}

		// Perform segmentation
		for (int i = 0; i < size; i++)
			selected[i] = false;
		for (yy = 0; yy < input.getHeight(); yy++)
			for (xx = 0; xx < input.getWidth(); xx++)
			{
				pixel = (yy * input.getWidth()) + xx;				
				if (selected[pixel])		continue;
				selected[pixel]	= true;
				
				yuv	= input.getRGB(xx,yy);			
				ch	= lut.lookup (yuv);

				if (ch >= 0)
					labelPixel (xx, yy, chs.at(ch));
				else
					segmented[pixel] = Channels.NO_COLOR;
			}
	}
	
	protected void labelPixel (int xo, int yo, Channel channel)
	{
		int pos, newpos;
		int xx, yy;
		int endTable, startTable;
		int y, u, v;
			
		xx = xo;
		yy = yo;	
		endTable = 0;
		startTable = -1;
		
		// look for neighbours, until the queue is full
		while (startTable != endTable)
		{
			int			yuv;
			
			yuv		= input.getRGB (xx, yy);				
			y		= Pixel.getY (yuv);
			u		= Pixel.getCr (yuv);
			v		= Pixel.getCb (yuv);

			pos		= yy * input.getWidth () + xx;			
			segmented[pos] = channel.id;
			
			// check left cell
			newpos = pos - 1;
			if (	(newpos > -1) && (newpos < segmented.length) && (xx > 0) && 
				!selected[newpos] && needLabelPixel(y, u, v, newpos, channel))
			{
				selected[newpos] = true;
				xTable[endTable] = xx-1; yTable[endTable] = yy;
				endTable++;
			}
			
			// check right cell
			newpos = pos + 1;
			if (	(newpos > -1) && (newpos < segmented.length) && (xx < (input.getWidth () - 1)) &&
				!selected[newpos] && needLabelPixel(y, u, v, newpos, channel))
			{
				selected[newpos] = true;
			   	xTable[endTable] = xx+1; yTable[endTable] = yy;
			   	endTable++;
		    }

			// check upper cell
			newpos = pos - input.getWidth ();
			if (	(newpos > -1) && (newpos < segmented.length) && (yy > 0) &&
				!selected[newpos] && needLabelPixel(y, u, v, newpos, channel))
			{
				selected[newpos] = true;
			   	xTable[endTable] = xx; yTable[endTable] = yy-1; 
			   	endTable++;
		    }
		    
		    // check lower cell
			newpos = pos + input.getWidth ();
			if (	(newpos > -1) && (newpos < segmented.length) && (yy < (input.getHeight() - 1)) &&
				!selected[newpos] && needLabelPixel(y, u, v, newpos, channel))
			{
				selected[newpos] = true;
				xTable[endTable] = xx; yTable[endTable] = yy+1;
				endTable++;
			}
		     
		    startTable++;
		    
		    // take next position
		    xx = xTable[startTable];
		    yy = yTable[startTable];
		}
	}
	
	protected boolean needLabelPixel (int y, int u, int v, int pos, Channel channel)
	{
		int			ny, nu, nv;
		int			yuv;
		int			xx, yy;
		int			thresh;
		
		yy		= pos / input.getWidth();
		xx		= pos % input.getWidth();
		
		yuv		= input.getRGB (xx, yy);				
		ny		= Pixel.getY (yuv);
		nu		= Pixel.getCr (yuv);
		nv		= Pixel.getCb (yuv);
		
		thresh	= channel.threshold;
		
		switch (MODE)
		{
		case M_BOX:
			if (Math.abs (y - ny) > thresh)		return false;
			if (Math.abs (u - nu) > thresh)		return false;
			if (Math.abs (v - nv) > thresh)		return false;
			break;
			
		case M_EUCLIDEAN:
			if (Math.sqrt ((y-ny)*(y-ny)+(u-nu)*(u-nu)+(v-nv)*(v-nv)) > thresh)		return false;
			break;
		}
		
		return true;
	}
	
	public boolean configurable ()
	{
		return true;
	}
	
	public void configureDialog ()
	{
		new SeedRegionGrowingDialog ();
	}
	
	class SeedRegionGrowingDialog extends JDialog
	{
		protected JComboBox<String> dmodes = new JComboBox<String> (DMODES);
		
		public SeedRegionGrowingDialog ()
		{
			JPanel panel;
			
			panel = new JPanel ();
			panel.setLayout (new GridLayout (1, 2));
			
			panel.add (new JLabel ("Growing Mode"));
			panel.add (dmodes);
			
			dmodes.setSelectedIndex (MODE);
			
			setTitle ("SeedRegionGrowing Configuration");
			setModal (true);
			setLocation (new Point (50, 50));
			setSize (new Dimension (300, 80));
			getContentPane().setLayout (new GridLayout (1, 1));
			getContentPane().add (panel);
	
			// event handling
			addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {
					thisWindowClosing(e);
				}
			});
			
			setVisible (true);
		}
		
		protected void thisWindowClosing (java.awt.event.WindowEvent e) 
		{
			MODE		= dmodes.getSelectedIndex ();			
			
			setVisible (false);
			dispose ();
		}
	}
}
