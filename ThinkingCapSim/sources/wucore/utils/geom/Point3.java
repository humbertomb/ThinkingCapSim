/*
 * (c) 1997-2002 Humberto Martinez
 */

package wucore.utils.geom;

import java.io.*;

import wucore.utils.math.*;

public class Point3 extends Point2 implements Serializable
{
	static public final int			PROJ_XY 	= 0;
	static public final int			PROJ_XZ 	= 1;
	static public final int			PROJ_YZ 	= 2;
	
	// Constants for axes defition
	static public final Point3		XUP			= new Point3 (1.0, 0.0, 0.0);
	static public final Point3		YUP			= new Point3 (0.0, 1.0, 0.0);
	static public final Point3		ZUP			= new Point3 (0.0, 0.0, 1.0);

	// Constants for axis symmetries
	static public final Point3		XSYM		= new Point3 (-1.0, 1.0, 1.0);
	static public final Point3		YSYM		= new Point3 (1.0, -1.0, 1.0);
	static public final Point3		ZSYM		= new Point3 (1.0, 1.0, -1.0);

	static public final double		VERYLARGE	= 1E10;
	
	public double					z;

	/* Constructors */
	public Point3 ()
	{
		set (0.0, 0.0, 0.0);
	}

	public Point3 (Point3 other)
	{
		set (other);
	}

	public Point3 (Point2 other)
	{
		set (other.x, other.y, 0.0);
	}

	public Point3 (double x, double y, double z)
	{
		set (x, y, z);
	}

	/* Accessor methods */
	public final double 	z () 				{ return z; }
	public final void 		z (double z) 		{ this.z = z;  }

	/* Instance methods */
	public void zero ()
	{
		x	= 0.0;
		y	= 0.0;
		z	= 0.0;
	}
	
	public void set (double xx, double yy, double zz)
	{
		x	= xx;
		y	= yy;
		z	= zz;
	}

	public void set (Point3 pt)
	{
		x	= pt.x;
		y	= pt.y;
		z	= pt.z;
	}

	public void set (PointH pt)
	{
		x	= pt.x / pt.w;
		y	= pt.y / pt.w;
		z	= pt.z / pt.w;
	}

	/**
	 * Set the value for the specified coordinate of the point.
	 *
	 * @param c coordinate index
	 * @param v value to set
	 */
	public void set(int c, double v) 
	{
		switch(c) 
		{
		case 0:
			x = v;
			break;
		case 1:
			y = v;
			break;
		case 2:
			z = v;
			break;
		default:
			throw new ArrayIndexOutOfBoundsException(c);
		}
	}

	/**
	 * Get the value for the specified coordinate of the point.
	 *
	 * @param c coordinate index
	 * @return value of coordinate
	 */	   
	public double get (int c) 
	{
		switch(c) 
		{
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		default:
			throw new ArrayIndexOutOfBoundsException(c);
		}
	}

	public void swap (Point3 pt)
	{
		double		xx, yy, zz;

		xx		= x;
		yy		= y;
		zz		= z;

		x		= pt.x;
		y		= pt.y;
		z		= pt.z;

		pt.x	= xx;
		pt.y	= yy;
		pt.z	= zz;
	}

	public void minimum (Point3 pt)
	{
		if (!Double.isInfinite (pt.x) && (pt.x > -VERYLARGE) && (pt.x < x))		x = pt.x;
		if (!Double.isInfinite (pt.y) && (pt.y > -VERYLARGE) && (pt.y < y))		y = pt.y;
		if (!Double.isInfinite (pt.z) && (pt.z > -VERYLARGE) && (pt.z < z))		z = pt.z;
	}

	public void maximum (Point3 pt)
	{
		if (!Double.isInfinite (pt.x) && (pt.x < VERYLARGE) && (pt.x > x))		x = pt.x;
		if (!Double.isInfinite (pt.y) && (pt.y < VERYLARGE) && (pt.y > y))		y = pt.y;
		if (!Double.isInfinite (pt.z) && (pt.z < VERYLARGE) && (pt.z > z))		z = pt.z;
	}

	/**
	 * Returns the Euclidean distance between two points.
	 *  
	 *	- The equation is as follows:
	 * 
	 *			L = sqrt ((XB-XA)^2 + (YB-YA)^2 + (ZB-ZA)^2)
	 */
	public double distance (Point3 pt)
	{
		return Math.sqrt ((pt.x - x) * (pt.x - x) + (pt.y - y) * (pt.y - y) + (pt.z - z) * (pt.z - z));
	}

	/**
	 * Returns the Euclidean distance between a point and the segment (pt0..pt1)
	 */
	public double distance (Point3 pt0, Point3 pt1)
	{
		Point3		v, w, pb;
		double		c1, c2, b;
		
		v = new Point3 ();
		w = new Point3 ();
		pb = new Point3 ();
		
		v.sub (pt1, pt0);
		w.sub (this, pt0);
		
		c1	= w.dot (v);
		if (c1 <= 0.0)
			return distance (pt0);
		
		c2	= v.dot (v);
		if (c2 <= c1)
			return distance (pt1);
		
		b = c1 / c2;
		pb.set (pt0);
		pb.scaleAdd (v, b);
		
		return distance (pb);
	}

	public void normalize ()
	{
		double		w;

		if ((w = norm ()) != 0.0) 
			set (x / w, y / w, z / w);
	}  

	public boolean is_null ()
	{
		return (equal (x, 0.0) && equal (y, 0.0) && equal (z, 0.0));
	} 

	public double norm ()
	{
		return Math.sqrt (x * x + y * y + z * z);
	} 

	public double norm2 ()
	{
		return x * x + y * y + z * z;
	} 

	public double angle (Point3 pt)
	{
		return Math.acos (dot (pt) / (norm () * pt.norm ()));
	}

	public void scale (double scale)
	{
		x	*= scale;
		y	*= scale;
		z	*= scale;
	}  

	public void scale (Point3 pt)
	{
		x	*= pt.x;
		y	*= pt.y;
		z	*= pt.z;
	}  

	public void divide (double scale)
	{
		x	/= scale;
		y	/= scale;
		z	/= scale;
	}  

	public void divide (Point3 pt)
	{
		x	/= pt.x;
		y	/= pt.y;
		z	/= pt.z;
	}  

	public void cross (Point3 pt)
	{
		double			tx, ty, tz;

		tx	= y * pt.z - z * pt.y;
		ty	= z * pt.x - x * pt.z;
		tz	= x * pt.y - y * pt.x;

		set (tx, ty, tz);
	}  

	public void cross (Point3 pt1, Point3 pt2)
	{
		x	= pt1.y * pt2.z - pt1.z * pt2.y;
		y	= pt1.z * pt2.x - pt1.x * pt2.z;
		z	= pt1.x * pt2.y - pt1.y * pt2.x;
	}  

	public double dot (Point3 pt)
	{		
		return x * pt.x + y * pt.y + z * pt.z;
	}  

	public double triple (Point3 pt1, Point3 pt2)
	{
		double		xx, yy, zz;
		
		xx	= pt1.y * pt2.z - pt1.z * pt2.y;
		yy	= pt1.z * pt2.x - pt1.x * pt2.z;
		zz	= pt1.x * pt2.y - pt1.y * pt2.x;

		return x * xx + y * yy + z * zz;
	}  

	public void add (double dx, double dy, double dz)
	{
		x	+= dx;
		y	+= dy;
		z	+= dz;
	}  

	public void add (Point3 pt)
	{
		x	+= pt.x;
		y	+= pt.y;
		z	+= pt.z;
	}

	public void add (PointH pt)
	{
		x	+= pt.x / pt.w;
		y	+= pt.y / pt.w;
		z	+= pt.z / pt.w;
	}

	public void add (Point3 pt1, Point3 pt2)
	{
		x	= pt1.x + pt2.x;
		y	= pt1.y + pt2.y;
		z	= pt1.z + pt2.z;
	}

	public void sub (Point3 pt)
	{
		x	-= pt.x;
		y	-= pt.y;
		z	-= pt.z;
	}

	public void sub (PointH pt)
	{
		x	-= pt.x / pt.w;
		y	-= pt.y / pt.w;
		z	-= pt.z / pt.w;
	}

	public void sub (Point3 pt1, Point3 pt2)
	{
		x	= pt1.x - pt2.x;
		y	= pt1.y - pt2.y;
		z	= pt1.z - pt2.z;
	}

	public void scaleSet (Point3 pt, double scale)
	{
		x	= scale * pt.x;
		y	= scale * pt.y;
		z	= scale * pt.z;
	}
	
	public void scaleAdd (Point3 pt, double scale)
	{
		x	+= scale * pt.x;
		y	+= scale * pt.y;
		z	+= scale * pt.z;
	}

	public void scaleAdd (double scale, Point3 pt1, Point3 pt2)
	{
		x	= scale * pt1.x + pt2.x;
		y	= scale * pt1.y + pt2.y;
		z	= scale * pt1.z + pt2.z;
	}

	public void scaleSub (Point3 pt, double scale)
	{
		x	-= scale * pt.x;
		y	-= scale * pt.y;
		z	-= scale * pt.z;
	}

	/**
	 * Rotate the point about a given axis.
	 *
	 * @param angle in radians
	 * @param axis axis about which to rotate (X_AXIS, Y_AXIS, or Z_AXIS)
	 */

	public void rotate (double angle, int axis) 
	{
		double xt, yt, zt;

		switch(axis) 
		{
		case AXIS_X:
			yt = y;
			zt = z;
			y = yt * Math.cos(angle) - zt * Math.sin(angle);
			z = yt * Math.sin(angle) + zt * Math.cos(angle);
			break;
			
		case AXIS_Y:
			xt = x;
			zt = z;
			x = xt * Math.cos(angle) - zt * Math.sin(angle);
			z = xt * Math.sin(angle) + zt * Math.cos(angle);
			break;
			
		case AXIS_Z:
			xt = x;
			yt = y;
			x = xt * Math.cos(angle) - yt * Math.sin(angle);
			y = xt * Math.sin(angle) + yt * Math.cos(angle);
		}
	}

	/**
     * Apply a rotation transformation to this Point3.
     * @param xaxis the X component of the rotation axis
     * @param yaxis the Y component of the rotation axis
     * @param zaxis the Z component of the rotation axis
     * @param angle the angle of the rotation in radians
     */
    public void rotate (double xaxis, double yaxis, double zaxis, double angle)
    {
    	double cos = Math.cos(angle);
    	double sin = Math.sin(angle);
    	double xx = x * ( xaxis*xaxis  +  cos*(1f - xaxis*xaxis)) +
	    	y * ( zaxis*sin    +  xaxis*yaxis*(1f - cos)) +
	    	z * ( yaxis*sin    +  xaxis*zaxis*(1f - cos));
    	double yy = x * ( zaxis*sin    +  xaxis*yaxis*(1f - cos)) +
	    	y * ( yaxis*yaxis  +  cos*(1f - yaxis*yaxis)) +
	    	z * (-xaxis*sin    +  yaxis*zaxis*(1f - cos));
    	double zz = x * (-yaxis*sin    +  xaxis*zaxis*(1f - cos)) +
	    	y * ( xaxis*sin    +  yaxis*zaxis*(1f - cos)) +
	    	z * ( zaxis*zaxis  +  cos*(1f - zaxis*zaxis));
    	
    	x = xx;
    	y = yy;
    	z = zz;
    }

	public void transform (Matrix3D matrix)
	{
		double			tx, ty, tz;
		double			w;

		tx	= matrix.mat[0][0] * x + matrix.mat[0][1] * y + matrix.mat[0][2] * z + matrix.mat[0][3];
		ty	= matrix.mat[1][0] * x + matrix.mat[1][1] * y + matrix.mat[1][2] * z + matrix.mat[1][3];
		tz	= matrix.mat[2][0] * x + matrix.mat[2][1] * y + matrix.mat[2][2] * z + matrix.mat[2][3];
		w 	= matrix.mat[3][0] * x + matrix.mat[3][1] * y + matrix.mat[3][2] * z + matrix.mat[3][3];

		if (w == 0.0)
			zero ();
		else		
			set (tx/w, ty/w, tz/w);
	}

	public void projectXY (int proj, double x, double y)
	{
		projectXY (proj, x, y, 0.0);
	}

	public void projectXY (int proj, double x, double y, double def)
	{
		switch (proj)
		{
		case PROJ_XY:	set (x, y, def); break;
		case PROJ_XZ:	set (x, def, y); break;
		case PROJ_YZ:	set (def, x, y); break;
		}
	}

	public void mirror (int axis, Point3 min, Point3 max)
	{
		switch (axis)
		{
		case AXIS_X:	x = max.x - (x - min.x); break;			
		case AXIS_Y:	y = max.y - (y - min.y); break;
		case AXIS_Z:	z = max.z - (z - min.z); break;
		}
	}
	
	public String toString ()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}

	public String toRawString ()
	{
		return Double.toString(x)+", "+ Double.toString (y)+", "+ Double.toString (z);
	}
}
