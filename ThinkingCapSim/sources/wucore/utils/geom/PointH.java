/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package wucore.utils.geom;

import wucore.utils.math.*;

public class PointH extends Point3
{
	public double					w;	

	// Constructors
	public PointH ()
	{
		zero ();
	}
	
	public PointH (PointH other)
	{
		set (other);
	}
	
	public PointH (double x, double y, double z, double w)
	{
		set (x, y, z, w);
	}
	
	// Instance methods
	public void zero ()
	{
		x	= 0.0;
		y	= 0.0;
		z	= 0.0;
		w	= 0.0;
	}
	
	public void set (PointH s)
	{
		x	= s.x;
		y	= s.y;
		z	= s.z;
		w	= s.w;
	}
	
	public void set (Point3 s)
	{
		x	= s.x;
		y	= s.y;
		z	= s.z;
		w	= 1.0;
	}
	
	public void set (Point2 s)
	{
		x	= s.x;
		y	= s.y;
		z	= 0.0;
		w	= 1.0;
	}
	
	public void set (double xx, double yy, double zz, double ww)
	{
		x	= xx;
		y	= yy;
		z	= zz;
		w	= ww;
	}
	
	public void set (double xx, double yy, double zz)
	{
		x	= xx;
		y	= yy;
		z	= zz;
		w	= 1.0;
	}
	
	public void set (double xx, double yy)
	{
		x	= xx;
		y	= yy;
		z	= 0.0;
		w	= 1.0;
	}
	
	/**
     * Scale this Point4 uniformly.
     * @param scale the uniform scale factor
     */
    public void scale (double scale)
    {
        x	= x * scale;
        y	= y * scale;
        z	= z * scale;
		w	= w * scale;
    }

    /**
     * Scale this Point4 non-uniformly.
     * @param xscale the x scale factor
     * @param yscale the y scale factor
     * @param zscale the z scale factor
     * @param wscale the w scale factor
     */
    public void scale (double xscale, double yscale, double zscale, double wscale) 
    {
        x	= x * xscale;
        y	= y * yscale;
        z	= z * zscale;
        w	= w * wscale;
   }

	public void add (PointH s)
	{
		x	+= s.x;
		y	+= s.y;
		z	+= s.z;
		w	+= s.w;
	}

	public void add (PointH s1, PointH s2)
	{
		x	= s1.x + s2.x;
		y	= s1.y + s2.y;
		z	= s1.z + s2.z;
		w	= s1.w + s2.w;
	}

	public void sub (PointH s)
	{
		x	-= s.x;
		y	-= s.y;
		z	-= s.z;
		w	-= s.w;
	}

	public void sub (PointH s1, PointH s2)
	{
		x	= s1.x - s2.x;
		y	= s1.y - s2.y;
		z	= s1.z - s2.z;
		w	= s1.w - s2.w;
	}

	public void scaleSet (PointH pt, double scale)
	{
		x	= scale * pt.x;
		y	= scale * pt.y;
		z	= scale * pt.z;
		w	= scale * pt.w;
	}

	public void scaleAdd (PointH pt, double scale)
	{
		x	+= scale * pt.x;
		y	+= scale * pt.y;
		z	+= scale * pt.z;
		w	+= scale * pt.w;
	}

	public void scaleSub (PointH pt, double scale)
	{
		x	-= scale * pt.x;
		y	-= scale * pt.y;
		z	-= scale * pt.z;
		w	-= scale * pt.w;
	}

   /**
     * Translate this Point4.
     * @param x the X translation component
     * @param y the Y translation component
     * @param z the Z translation component
     */
    public void translate (double xx, double yy, double zz)
    {
        x	+= w * xx;
        y	+= w * yy;
        z	+= w * zz;
    }

	public void transform (Matrix3D matrix)
	{
		double			tx, ty, tz, tw;

		tx	= matrix.mat[0][0] * x + matrix.mat[0][1] * y + matrix.mat[0][2] * z + matrix.mat[0][3] * w;
		ty	= matrix.mat[1][0] * x + matrix.mat[1][1] * y + matrix.mat[1][2] * z + matrix.mat[1][3] * w;
		tz	= matrix.mat[2][0] * x + matrix.mat[2][1] * y + matrix.mat[2][2] * z + matrix.mat[2][3] * w;
		tw 	= matrix.mat[3][0] * x + matrix.mat[3][1] * y + matrix.mat[3][2] * z + matrix.mat[3][3] * w;

		set (tx, ty, tz, tw);
	}

    public double norm ()
	{
	    return Math.sqrt (x * x + y * y + z * z + w * w);
	}

    public double norm2 ()
	{
	    return x * x + y * y + z * z + w * w;
	}

	public double distance (PointH s)
	{
	    return Math.sqrt ((x/w-s.x/s.w) * (x/w-s.x/s.w) + (y/w-s.y/s.w) * (y/w-s.y/s.w) + (z/w-s.z/s.w) * (z/w-s.z/s.w));
	}
	
	public double distance (Point3 s)
	{
	    return Math.sqrt ((x/w-s.x) * (x/w-s.x) + (y/w-s.y) * (y/w-s.y) + (z/w-s.z) * (z/w-s.z));
	}

    /**
     * Converts from homoheneous 3D coordinates to standard 3D coordinates
     * 
     * Divides each coordinate by the weight and sets the coordinate to the
     * newly calculated ones.
     */
    public void unweight () 
    {
        x	/= w;
        y	/= w;
        z	/= w;
    }

	public void unweight (Point3 s)
	{
		s.set (x / w, y / w, z / w);
	}                 

	public void projectXY (int proj, double x, double y, double w)
	{
		projectXY (proj, x, y, 0.0, w);
	}

	public void projectXY (int proj, double x, double y, double def, double w)
	{
		switch (proj)
		{
		case PROJ_XY:	set (x, y, def, w); break;
		case PROJ_XZ:	set (x, def, y, w); break;
		case PROJ_YZ:	set (def, x, y, w); break;
		}
	}

	public void mirror (int axis, Point3 min, Point3 max)
	{
		switch (axis)
		{
		case AXIS_X:	x = (max.x - (x/w - min.x)) * w; break;			
		case AXIS_Y:	y = (max.y - (y/w - min.y)) * w; break;
		case AXIS_Z:	z = (max.z - (z/w - min.z)) * w; break;
		}
	}
	
	public String toString ()
	{
		return "(" + x + ", " + y + ", " + z + ", " + w + ")";
	}
	
	public String toRawString ()
	{
		return Double.toString(x)+", "+ Double.toString (y)+", "+ Double.toString (z)+", "+ Double.toString (w);
	}
}
