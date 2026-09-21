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

public class CircleFitting 
{
	static public final int	MAXPOINTS	= 1000;
	static public final int	BORDER		= 10;
	
	// Segmented source image and sizes
	protected Channel		channel;
	protected int[]			segmented;
	protected int			width, height;
	
	// Ellipse fitting computation
	protected int[][]		A			= new int[3][4];
	protected double[][]		B			= new double[2][4];
	protected double[]		res			= new double[3];
	protected Point[]		pts;
	protected int			npts;
	
	public CircleFitting ()
	{
		int			i;
		
		npts		= 0;		
		pts		= new Point[MAXPOINTS];
		for (i = 0; i < MAXPOINTS; i++)
			pts[i] = new Point ();
	}
	
	/*******************************************************************/
	/***							Linear equation solver, 3x3		 ***/
	/*******************************************************************/
	protected void linearEquation ()
	{
		int i;
		double cte0, cte1;
		double x = -1.0, y = -1.0, z = -1.0;
		
		
		if ((A[1][1] != 0) && (A[2][1] != 0) && (A[1][0] != 0) && (A[2][0] != 0))
		{
			/*** X ***/
			cte0 = (A[0][1] / (double) A[1][1]);				// solve X variable by Y and Z equations subtraction
			cte1 = (A[1][1] / (double) A[2][1]);
			
			for (i = 0; i < 4; i++)
			{
				B[0][i] = A[0][i] - (A[1][i] * cte0);
				B[1][i] = A[1][i] - (A[2][i] * cte1);
			}
			
			if (B[1][2] != 0.0)
			{
				cte0 = B[0][2] / B[1][2];
				for (i = 0; i < 4; i++)
					B[0][i] = B[0][i] - (B[1][i] * cte0);
				
				if (B[0][0] == 0.0) x = -1;
				else x = B[0][3] / B[0][0];
			}
			
			/*** Y ***/
			cte0 = (A[0][0] / (double) A[1][0]);				// solve Y variable by X and Z equations	subtraction
			cte1 = (A[1][0] / (double) A[2][0]);
			
			for (i = 0; i < 4; i++)
			{
				B[0][i] = A[0][i] - (A[1][i] * cte0);
				B[1][i] = A[1][i] - (A[2][i] * cte1);
			}
			
			if (B[1][2] != 0.0)
			{
				cte0 = B[0][2] / B[1][2];
				for (i = 0; i < 4; i++)
					B[0][i] = B[0][i] - (B[1][i] * cte0);
				
				if (B[0][1] == 0.0) y = -1;
				else y = B[0][3] / B[0][1];
			}
			
			/*** Z ***/
			cte0 = (A[0][0] / (double) A[1][0]);				// solve Z variable by X and Y equations subtraction
			cte1 = (A[1][0] / (double) A[2][0]);	
			
			for (i = 0; i < 4; i++)
			{
				B[0][i] = A[0][i] - (A[1][i] * cte0);
				B[1][i] = A[1][i] - (A[2][i] * cte1);
			}
			
			if (B[1][1] != 0.0)		
			{
				cte0 = B[0][1] / B[1][1];
				for (i = 0; i < 4; i++)
					B[0][i] = B[0][i] - (B[1][i] * cte0);
				
				if (B[0][2] == 0.0) z = -1;
				else z = B[0][3] / B[0][2];
			}
		}
		
		res[0] = x;
		res[1] = y;
		res[2] = z;
	}
	
	/********************************************************************/
	/***						Ball perimeter estimation				  ***/
	/********************************************************************/
	protected void perimeter (Blob b, int imgminx, int imgmaxx)
	{
		int			x, y;
		int			xmin, xmax, ymin, ymax;
		boolean		edge;
		
		xmin = b.getXMin();
		xmax = b.getXMax();
		ymin = b.getYMin();
		ymax = b.getYMax();
		
		// Do not consider pixels which are close to the horizontal
		// borders of the image. This usually happens when the ball
		// is close and do not want to include the boundary of the
		// shadow into the fitting algorithm.
		npts = 0;
		edge = false;
		for (y = ymin; (y < ymax) && !edge; y++)	// From left to first ORANGE (ball) pixel
			for (x = xmin; x < xmax; x++)
				if (segmented[(y * width) + x] == channel.id)
				{
					pts[npts].x = x;
					pts[npts].y = y;
					npts ++;
					
					if ((x < imgminx) || (x > imgmaxx))	edge = true;
					break;
				}
		
		edge = false;
		for (y = ymin; (y < ymax) && !edge; y++)	// From left to first ORANGE (ball) pixel
			for (x = xmax; x >= xmin; x--)
				if (segmented[(y * width) + x] == channel.id)
				{
					pts[npts].x = x;
					pts[npts].y = y;
					npts ++;
					
					if ((x < imgminx) || (x > imgmaxx))	edge = true;
					break;
				}
	}

	/********************************************************************/
	/***								Moment estimation			  ***/
	/********************************************************************/
	protected void computeMoments ()
	{
		int i;	
		int accX = 0, accY = 0, accZ = 0;
		int accXX = 0, accYY = 0, accXY = 0;
		int accXZ = 0, accYZ = 0;
		
		for (i = 0; i < npts; i++)
		{
			accXX += (pts[i].x * pts[i].x);
			accYY += (pts[i].y * pts[i].y);
			accXY += (pts[i].x * pts[i].y);
			accX += pts[i].x;
			accY += pts[i].y;
			accXZ += (pts[i].x * ((pts[i].x * pts[i].x) + (pts[i].y * pts[i].y)));
			accYZ += (pts[i].y * ((pts[i].x * pts[i].x) + (pts[i].y * pts[i].y)));
			accZ += ((pts[i].x * pts[i].x) + (pts[i].y * pts[i].y));
		}
		
		A[0][0] = accXX;
		A[0][1] = accXY;
		A[0][2] = accX;
		A[0][3] = -accXZ;
		
		A[1][0] = accXY;
		A[1][1] = accYY;
		A[1][2] = accY;
		A[1][3] = -accYZ;
		
		A[2][0] = accX;
		A[2][1] = accY;
		A[2][2] = npts;
		A[2][3] = -accZ;
	}
		
	public void doFitting (BufferedImage input, int[] segmented, Blob blob, Channel channel)
	{
		double			cx = 0.0, cy = 0.0;
		double			radius = 0.0;
		boolean			fitted;
	
		if (blob == null)			return;
		
		this.segmented	= segmented;
		this.channel		= channel;
		width			= input.getWidth();
		height			= input.getHeight();

		fitted = false;
		perimeter (blob, BORDER, width-BORDER);
		if (npts > 0)
		{
			computeMoments ();
			linearEquation ();
			
			if ((res[0] != -1.0) && (res[1] != -1.0) && (res[2] != -1.0))
			{
				double		aux;
				
				cx = (res[0] * 0.5);		// a / -2
				cy = (res[1] * 0.5);		// b / -2
				
				aux = ((cx * cx) + (cy * cy) - res[2]);
				if (aux < 0) aux *= -1;
				
				radius	= Math.sqrt (aux);	// x2+y2-d		
				fitted	= true;
			}
		}

		if (!fitted)
		{
			cx		= -blob.getX();
			cy		= -blob.getY();
			radius	= (blob.getSizeX() + blob.getSizeY()) / 4;
		}

		// Put ellipse fitting information into image
		int			i;
		int			xx, yy;
		BufferedImageDrawing	dwg;

		xx	= -(int) Math.round (cx);
		yy	= -(int) Math.round (cy);
		dwg	= new BufferedImageDrawing ();
		dwg.updateImage (input);
		for (i = 0; i < npts; i++)
			dwg.drawPoint (pts[i].x, pts[i].y, Color.white.getRGB());
		dwg.drawCircle (xx, yy, (int) Math.round (radius), Color.black.getRGB());	
		dwg.drawCross (xx, yy, Color.white.getRGB());
	}
}
