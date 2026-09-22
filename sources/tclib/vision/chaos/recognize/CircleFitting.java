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

/**
 * The circle a ball makes in the image, from the edge of its blob: its centre
 * and radius, also when part of the ball is out of the frame (a ball close to
 * the robot is cut by the bottom of the image), which is what the centre of the
 * blob gets wrong.
 *
 * The edge is the first and the last pixel of the channel in each row of the
 * blob, leaving out the ones against the sides of the frame (where the blob is
 * cut, not round). The circle is the algebraic least squares one (Kasa):
 * x^2 + y^2 + D x + E y + F = 0, solved in doubles about the mean of the points
 * (the sums of cubes of pixel coordinates do not fit in an int, which is what
 * made the old fit give circles of any size).
 */
public class CircleFitting 
{
	static public final int		BORDER		= 3;			// pixels from a side of the frame that are not the edge of the ball
	static public final int		MINPOINTS	= 6;			// fewer points than this are no circle
	
	// Segmented source image and sizes
	protected Channel		channel;
	protected int[]			segmented;
	protected int			width, height;
	
	// The edge of the blob
	protected int[]			px		= new int[0];
	protected int[]			py		= new int[0];
	protected int			npts;

	// The circle: centre and radius (pixels), and whether it could be fitted
	public double			cx, cy, radius;
	public boolean			fitted;
	
	public CircleFitting ()
	{
	}
	
	/** The edge of a blob: the first and the last pixel of the channel in each of its rows, off the sides of the frame. */
	protected void perimeter (Blob b)
	{
		int			n = 2 * (b.getYMax () - b.getYMin () + 1);

		if (px.length < n)		{ px = new int[n]; py = new int[n]; }
		npts	= 0;
		for (int y = Math.max (0, b.getYMin ()); y <= Math.min (height - 1, b.getYMax ()); y++)
		{
			int		left = -1, right = -1;

			for (int x = Math.max (0, b.getXMin ()); x <= Math.min (width - 1, b.getXMax ()); x++)
				if (segmented[(y * width) + x] == channel.id)		{ left = x; break; }
			if (left < 0)		continue;
			for (int x = Math.min (width - 1, b.getXMax ()); x >= left; x--)
				if (segmented[(y * width) + x] == channel.id)		{ right = x; break; }

			if (left >= BORDER)							{ px[npts] = left;	py[npts] = y;	npts++; }
			if ((right != left) && (right < width - BORDER))	{ px[npts] = right;	py[npts] = y;	npts++; }
		}
	}

	/** The least squares circle through the edge; false when there is none (too few points, all in a line). */
	protected boolean fit ()
	{
		double		mx = 0.0, my = 0.0;
		double		suu = 0, svv = 0, suv = 0, suuu = 0, svvv = 0, suvv = 0, svuu = 0;
		double		det, uc, vc;

		if (npts < MINPOINTS)		return false;
		for (int i = 0; i < npts; i++)		{ mx += px[i]; my += py[i]; }
		mx	/= npts;
		my	/= npts;

		// about the mean, the centre (uc, vc) solves the two equations of the least squares circle
		for (int i = 0; i < npts; i++)
		{
			double	u = px[i] - mx, v = py[i] - my;
			suu += u * u;		svv += v * v;		suv += u * v;
			suuu += u * u * u;	svvv += v * v * v;	suvv += u * v * v;	svuu += v * u * u;
		}
		det		= suu * svv - suv * suv;
		if (Math.abs (det) < 1e-9)		return false;
		uc		= 0.5 * ((suuu + suvv) * svv - (svvv + svuu) * suv) / det;
		vc		= 0.5 * ((svvv + svuu) * suu - (suuu + suvv) * suv) / det;

		cx		= uc + mx;
		cy		= vc + my;
		radius	= Math.sqrt (uc * uc + vc * vc + (suu + svv) / npts);
		return true;
	}
		
	public void doFitting (BufferedImage input, int[] segmented, Blob blob, Channel channel)
	{
		if (blob == null)			return;
		
		this.segmented	= segmented;
		this.channel	= channel;
		width			= input.getWidth();
		height			= input.getHeight();

		perimeter (blob);
		fitted	= fit ();
		// a circle much larger than the blob is not the ball's (a few points nearly in a line)
		if (fitted && (radius > 2.0 * Math.max (blob.getSizeX (), blob.getSizeY ())))
			fitted	= false;
		if (!fitted)
		{
			cx		= blob.getX ();
			cy		= blob.getY ();
			radius	= (blob.getSizeX () + blob.getSizeY ()) / 4.0;
		}

		// Put circle fitting information into image
		BufferedImageDrawing	dwg;
		int						xx, yy;

		xx	= (int) Math.round (cx);
		yy	= (int) Math.round (cy);
		dwg	= new BufferedImageDrawing ();
		dwg.updateImage (input);
		for (int i = 0; i < npts; i++)
			dwg.drawPoint (px[i], py[i], Color.white.getRGB());
		dwg.setThickness (BufferedImageDrawing.MARK);			// the circle of the ball, thick
		dwg.drawCircle (xx, yy, (int) Math.round (radius), Color.black.getRGB());	
		dwg.drawCross (xx, yy, Color.white.getRGB());
	}
}
