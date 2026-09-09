/**
 * Created on 30-sep-2007
 *
 * @author Humberto Martinez Barbera
 */
package wucore.utils.geom;

public class Plane3 
{
	public Point3					origin;			// Origin of plane
	public Point3					normal;			// Normal to plane
	
	private double					a;				// Plane equation coefficients
	private double					b;
	private double					c;
	private double					d;

	private Point3					u = new Point3 ();
	private Point3					w = new Point3 ();
	private Point3					pb = new Point3 ();
	
	public Plane3 (Point3 origin, Point3 normal)
	{
		this.origin		= origin;
		this.normal		= normal;
		
		// Compute plane coefficients
		a		= normal.x;
		b		= normal.y;
		c		= normal.z;
		d		= -normal.dot (origin);		
	}
	
	public double distanceToPlane (Point3 point)
	{
		return Math.abs (a * point.x + b * point.y + c * point.z + d);
	}

	/** Returns the distance between two points
	 * projected on a plane defined by a normal.
	 * 
	 * REMARK: The operations may be totally wrong. 
	 * They do work for the axes definitions XUP,
	 * YUP and ZUP.
	 */
	public double distanceOnPlane (Point3 pt1, Point3 pt2)
	{
		double		dx, dy, dz;
		
		dx	= (pt1.x - pt2.x) * (1.0 - normal.x);
		dy	= (pt1.y - pt2.y) * (1.0 - normal.y);
		dz	= (pt1.z - pt2.z) * (1.0 - normal.z);

		return Math.sqrt (dx * dx + dy * dy + dz * dz);
	}

	public Point3 intersection (Line3 s)
	{
		double		d, n, si;
		
		u.sub (s.dest, s.orig);
		w.sub (s.orig, origin);
		
		d	= normal.dot (u);
		n	= -normal.dot (w);

	    if (Math.abs (d) < 1.0e-9)         		// segment is parallel to plane
	    {
	        if (n == 0.0)                     	// segment lies in plane
	            return s.orig;
	        else
	            return null;                   	// no intersection
	    }
	    
	    // they are not parallel
	    // compute intersect param
	    si	= n / d;
	    if ((si < 0.0) || (si > 1.0))
	        return null;                       // no intersection

	    pb.set (s.orig);
	    pb.scaleAdd (u, si);
	    
	    return pb;
	}
	
	public String toString ()
	{
		return "PLANE[O="+origin+"/N="+normal+"]";
	}
}
