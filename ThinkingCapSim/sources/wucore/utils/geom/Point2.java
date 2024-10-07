/*
 * (c) 1997-2001 Humberto Martinez
 */

package wucore.utils.geom;

import java.io.*;

import wucore.utils.dxf.*;
import wucore.utils.math.*;

public class Point2 extends Object implements Serializable
{
	static public final double		EPSILON		= 1.0e-10;

	static public final int 		AXIS_X 		= 0;
	static public final int 		AXIS_Y 		= 1;
	static public final int 		AXIS_Z 		= 2;

	public double					x;
	public double					y;

	/* Constructors */
	public Point2()
	{
		set (0.0, 0.0);
	}

	public Point2 (Point2 point)
	{
		set (point);
	}

	public Point2 (double x, double y)
	{
		set (x, y);
	}

	// Class methods
	static public boolean equal (double a, double b)
	{
		return (Math.abs (a - b) <= EPSILON);
	}

	static public double sgn (double a)
	{
		if (a < 0.0)
			return	-1.0;
		else
			return	1.0;
	}

	/* Accessor methods */
	public final double 		x () 					{ return x; }
	public final void 			x (double x) 			{ this.x = x; }
	public final double 		y () 					{ return y; }
	public final void 			y (double y) 			{ this.y = y;  }

	/* Instance methods */
	public void zero ()
	{
		x	= 0.0;
		y	= 0.0;
	}
	
	public void set (double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public void set (Point2 pt)
	{
		x = pt.x;
		y = pt.y;
	}

	public void add (double dx, double dy)
	{
		x += dx;
		y += dy;
	}  

	public void scale (double scale)
	{
		x *= scale;
		y *= scale;
	}  

	public void rotate (double phi)
	{
		double		nx, ny;

		nx = x*Math.cos(phi) + y*Math.sin(phi);
		ny = y*Math.cos(phi) - x*Math.sin(phi);

		set (nx, ny);
	}

	/**
	 * Rotate the point about a given axis.
	 *
	 * @param angle in radians
	 * @param axis axis about which to rotate (X_AXIS, Y_AXIS, or Z_AXIS)
	 */

	public void rotate (double angle, int axis) 
	{
		double xt, yt;

		switch(axis) 
		{
		case AXIS_X:
			yt = y;
			y = yt * Math.cos(angle);
		case AXIS_Y:
			xt = x;
			x = xt * Math.cos(angle);
		case AXIS_Z:
			xt = x;
			yt = y;
			x = xt * Math.cos(angle) - yt * Math.sin(angle);
			y = xt * Math.sin(angle) + yt * Math.cos(angle);
		}
	}

	/** 
	 * Transform the point by given transformation matrix.
	 *
	 * @param matrix	transformation matrix
	 */   
	public void transform (Matrix3D matrix)
	{
		double			tx, ty;

		tx	= matrix.mat[0][0] * x + matrix.mat[0][1] * y + matrix.mat[0][3];
		ty	= matrix.mat[1][0] * x + matrix.mat[1][1] * y + matrix.mat[1][3];

		set (tx, ty);
	}

	public double distance (Point2 pt)
	{
		return Math.sqrt ((pt.x - x) * (pt.x - x) + (pt.y - y) * (pt.y - y));
	}

	public double distance (double x1, double y1)
	{
		return Math.sqrt ((x1 - x) * (x1 - x) + (y1 - y) * (y1 - y));
	}

	public double angle (Point2 pt)
	{
		return Math.atan2 (pt.y - y, pt.x - x);
	}

	public String toString ()
	{
		return "(" + x + ", " + y + ")";
	}

	public String toRawString ()
	{
		return Double.toString(x)+", "+ Double.toString (y);
	}

	public String toRoundString ()
	{
		return DoubleFormat.format(x)+", "+ DoubleFormat.format(y);
	}
}
