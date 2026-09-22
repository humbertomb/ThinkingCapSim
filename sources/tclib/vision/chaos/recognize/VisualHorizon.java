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

public class VisualHorizon 
{
	static public final int	MAXPOINTS	= 1000;
	static public final int	GRID			= 5;
	
	// Segmented source image and sizes
	protected Channel		carpet;
	protected Channel		ball;
	protected int[]			segmented;
	protected int			width, height;
	
	// Ellipse fitting computation
	protected Point[]		pts;
	protected int			npts;
	LineParam				top;
	
	public VisualHorizon ()
	{
		int			i;
		
		npts		= 0;		
		pts		= new Point[MAXPOINTS];
		for (i = 0; i < MAXPOINTS; i++)
			pts[i] = new Point ();
	}
	
	/********************************************************************/
	/***						Net perimeter estimation				  ***/
	/********************************************************************/
	protected void boundary ()
	{
		int			x, y;
		int			xmin, xmax, ymin, ymax;
		int			offset;
		
		xmin = 0;
		xmax = width;
		ymin = 0;
		ymax = height;
		
		offset	= (xmax - xmin) * 2 / 10;
		xmin += offset;
		xmax -= offset;
		
		npts = 0;
		for (x = xmin; x < xmax; x += GRID)
		{
			for (y = ymin; y < ymax-1; y++)
				if (((segmented[(y * width) + x] == carpet.id)
					&& (segmented[((y+1) * width) + x] == carpet.id))
					|| ((segmented[(y * width) + x] == ball.id)
					&& (segmented[((y+1) * width) + x] == ball.id)))
				{
					pts[npts].x = x;
					pts[npts].y = y;
					npts ++;
					break;
				}
			if (y >= ymax-1)
			{
				pts[npts].x = x;
				pts[npts].y = ymax-1;
				npts ++;
			}
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
	
	public void findHorizon (BufferedImage input, int[] segmented, Channel carpet, Channel ball)
	{
		this.segmented	= segmented;
		this.carpet		= carpet;
		this.ball		= ball;
		width			= input.getWidth();
		height			= input.getHeight();
		
		boundary ();
		if (npts > 0)
		{
			top		= linearFitting (pts, npts);

			// Put line fitting information into image
			int			i;
			int			x1, y1, x2, y2;
			BufferedImageDrawing	dwg;
			
			dwg	= new BufferedImageDrawing ();
			dwg.updateImage (input);
			for (i = 0; i < npts; i++)
				dwg.drawPoint (pts[i].x, pts[i].y, Color.WHITE.getRGB());
			
			dwg.setThickness (BufferedImageDrawing.MARK);		// the horizon, thick
			x1	= 0;
			x2	= width;
			y1	= (int) Math.round (top.m * x1 + top.n);
			y2	= (int) Math.round (top.m * x2 + top.n);
			dwg.drawLine (x1, y1, x2, y2, Color.GREEN.getRGB());
		}
	}
	
	public boolean isBelowHorizont (Blob blob, int k)
	{
		int y;
		y = (int) (top.m * blob.getX () + top.n) - k;
		return (blob.getY () > y);
	}

	public boolean isAboveHorizont (Blob blob, int k)
	{
		int y;
		y = (int) (top.m * blob.getX () + top.n) + k;
		return (blob.getY () < y);
	}
}
