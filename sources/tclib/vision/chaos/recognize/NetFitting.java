/**
 * Created on 08-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.recognize;

import java.awt.*;
import java.awt.image.*;

import tclib.vision.chaos.blobs.*;
import tclib.vision.chaos.channels.*;
import tcrob.umu.quaky2.gui.images.BufferedImageDrawing;

public class NetFitting 
{
	static public final int	MAXPOINTS	= 1000;
	
	// Segmented source image and sizes
	protected Channel		net;
	protected Channel		carpet;
	protected int[]			segmented;
	protected int			width, height;
	
	// Ellipse fitting computation
	protected Point[]		toppts;
	protected int			ntoppts;
	protected Point[]		downpts;
	protected int			ndownpts;
	
	public NetFitting ()
	{
		int			i;
		
		ntoppts		= 0;		
		toppts		= new Point[MAXPOINTS];
		for (i = 0; i < MAXPOINTS; i++)
			toppts[i] = new Point ();
		
		ndownpts		= 0;		
		downpts		= new Point[MAXPOINTS];
		for (i = 0; i < MAXPOINTS; i++)
			downpts[i] = new Point ();
	}
	
	/********************************************************************/
	/***						Net perimeter estimation				  ***/
	/********************************************************************/
	protected void perimeter (Blob b)
	{
		int			x, y;
		int			xmin, xmax, ymin, ymax;
		int			halfh, offset;
		
		xmin = b.getXMin();
		xmax = b.getXMax();
		ymin = b.getYMin();
		ymax = b.getYMax();
		
		halfh	= (ymax - ymin) / 3;
		offset	= (xmax - xmin) * 2 / 10;
		xmin += offset;
		xmax -= offset;
		
		ntoppts = 0;
		for (x = xmin; x < xmax; x++)
			for (y = ymin; y < ymin+halfh; y++)	// From top downto first net pixel (only upper part)
				if (segmented[(y * width) + x] == net.id)
				{
					toppts[ntoppts].x = x;
					toppts[ntoppts].y = y;
					ntoppts ++;
					break;
				}
		
		ndownpts = 0;
		for (x = xmin; x < xmax; x++)
			for (y = ymax; y >= ymax-halfh; y--)	// From bottom upto first net pixel (only lower part)
				if (segmented[(y * width) + x] != carpet.id)
				{
					downpts[ndownpts].x = x;
					downpts[ndownpts].y = y;
					ndownpts ++;
					break;
				}
	}
	
	/**
	 *  Use the Least Squares fit method for fitting a
	 *  straight line to 2-D data for measurements
	 *  y[i] vs. dependent variable x[i]. This fit assumes
	 *  there are errors only on the y measuresments as
	 *  given by the sigma_y array.
	 *  See, e.g. Press et al., "Numerical Recipes..." for details
	 *  of the algorithm.
	 **/
	protected LineParam linearFitting (Point[] pts, int num_points)
	{
		
		double		s=0.0,sx=0.0,sy=0.0,sxx=0.0,sxy=0.0;
		double		del;
		LineParam	line;
		
		s = num_points; // x.length;
		for (int i=0; i < num_points; i++)
		{
			sx  += pts[i].x;
			sy  += pts[i].y;
			sxx += pts[i].x*pts[i].x;
			sxy += pts[i].x*pts[i].y;
		}		
		del		= s*sxx - sx*sx;
		
		line		= new LineParam ();	
		line.n	= (sxx*sy -sx*sxy)/del;
		line.m	= (s*sxy -sx*sy)/del;
		line.en	= sxx/del;
		line.em	= s/del;
		
		return line;
	}
	
	public void doFitting (BufferedImage input, int[] segmented, Blobs blobs, Channel net, Channel carpet)
	{
		Blob				blob;
		
		blob		= blobs.getBlob (0);
		if (blob == null)		return;
		
		this.segmented	= segmented;
		this.net			= net;
		this.carpet		= carpet;
		width			= input.getWidth();
		height			= input.getHeight();
		
		perimeter (blob);
		if ((ntoppts > 0) && (ndownpts > 0))
		{
			LineParam		top, down;
			
			top		= linearFitting (toppts, ntoppts);
			down		= linearFitting (downpts, ndownpts);

			// Put line fitting information into image
			int			i;
			int			x1, yu1, yd1, x2, yu2, yd2;
			BufferedImageDrawing	dwg;
			
			dwg	= new BufferedImageDrawing ();
			dwg.updateImage (input);
			for (i = 0; i < ntoppts; i++)
				dwg.drawPoint (toppts[i].x, toppts[i].y, Color.white.getRGB());
			for (i = 0; i < ndownpts; i++)
				dwg.drawPoint (downpts[i].x, downpts[i].y, Color.white.getRGB());
			
			dwg.setThickness (BufferedImageDrawing.MARK);		// the lines fitted to the net, thick
			x1	= blob.getXMin ();
			x2	= blob.getXMax ();
			yu1	= (int) Math.round (top.m * x1 + top.n);
			yu2	= (int) Math.round (top.m * x2 + top.n);
			dwg.drawLine (x1, yu1, x2, yu2, Color.black.getRGB());
			yd1	= (int) Math.round (down.m * x1 + down.n);
			yd2	= (int) Math.round (down.m * x2 + down.n);
			dwg.drawLine (x1, yd1, x2, yd2, Color.black.getRGB());
			
//			int			h;
//			h = (yd1 - yu1 + yd2 - yu2) / 2;			
//			System.out.println ("Blob height="+blob.getSizeY()+", fitting="+h);
		}
	}
}
