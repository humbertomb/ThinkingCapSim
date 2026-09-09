/**
 * Created on 25-oct-2007
 *
 * @author Humberto Martinez Barbera
 */
package wucore.utils.geom;

public class Line3 
{
	public Point3				orig = new Point3 ();
	public Point3				dest = new Point3 ();

	protected Point3			u = new Point3 ();
	protected Point3			v = new Point3 ();
	protected Point3			w = new Point3 ();
	protected Point3			pb = new Point3 ();

	public Line3 ()
	{

	}

	public Line3 (Point3 orig, Point3 dest)
	{
		set (orig, dest);
	}

	public final double					length ()			{ return orig.distance (dest); }

	public final void set (Line3 other)
	{
		this.orig.set (other.orig);
		this.dest.set (other.dest);
	}

	public final void set (Point3 orig, Point3 dest)
	{
		this.orig.set (orig);
		this.dest.set (dest);
	}

	public boolean contains (Line3 other, double tol)
	{
		return (distance (other.orig) < tol) && (distance (other.dest) < tol);
	}

	public boolean parallel (Line3 other, double tol)
	{
		u.sub (dest, orig);
		v.sub (other.dest, other.orig);

		return (Math.abs (u.x/v.x - u.y/v.y) < tol) && (Math.abs (u.y/v.y - u.z/v.z) < tol);
	}

	static protected boolean parallel (Point3 u, Point3 v, double tol)
	{
		return (Math.abs (u.x/v.x - u.y/v.y) < tol) && (Math.abs (u.y/v.y - u.z/v.z) < tol);
	}

	public final double distanceToBounds (Line3 other)
	{
		double			d1, d2, d3, d4;

		d1	= orig.distance (other.orig);
		d2	= orig.distance (other.dest);
		d3	= dest.distance (other.orig);
		d4	= dest.distance (other.dest);		

		return Math.min (Math.min (d1, d2), Math.min (d3, d4));
	}

	public final double distanceToBounds (Point3 point)
	{
		return Math.min (orig.distance (point), dest.distance (point));
	}

	/**
	 * Returns the Euclidean distance between a point and the segment
	 */
	public double distance (Point3 point)
	{
		double		c1, c2, b;

		v.sub (dest, orig);
		w.sub (point, orig);

		c1	= w.dot (v);
		if (c1 <= 0.0)
			return point.distance (orig);

		c2	= v.dot (v);
		if (c2 <= c1)
			return point.distance (dest);

		b = c1 / c2;
		pb.set (orig);
		pb.scaleAdd (v, b);

		return point.distance (pb);
	}

	public Line3 intersection (Line3 other, double tol)
	{
		double			du, dv;

		u.sub (dest, orig);
		v.sub (other.dest, other.orig);
		if (!parallel (u, v, tol))								return null;	// They are NOT parallel

		w.sub (orig, other.orig);
		if (!parallel (u, w, tol) || !parallel (v, w, tol))		return null;	// They are NOT collinear

		// Check if they are degenerate points
		du = u.dot (u);
		dv = v.dot (v);
		if ((du == 0.0) || (dv == 0.0))							return null;     // Both segments are points

		// Collinear segments - Get overlap (or not)
		double			t0, t1;                   // endpoints of S1 in eqn for S2
		Point3			w2 = new Point3 ();
		Line3			line;

		w2.sub (dest, other.orig);
		if (v.x != 0.0) 
		{
			t0 = w.x / v.x;
			t1 = w2.x / v.x;
		}
		else if (v.y != 0.0) 
		{
			t0 = w.y / v.y;
			t1 = w2.y / v.y;
		}
		else /* if (v.y != 0.0) */
		{
			t0 = w.z / v.z;
			t1 = w2.z / v.z;
		}
		if (t0 > t1) 
		{                  // must have t0 smaller than t1
			double t=t0; t0=t1; t1=t;    // swap if not
		}
		if (t0 > 1 || t1 < 0)							return null;    // NO overlap

		t0 = t0<0? 0 : t0;              // clip to min 0
		t1 = t1>1? 1 : t1;              // clip to max 1

		if (t0 == t1)									return null;	// intersect is a point

		// They overlap in a valid subsegment
		line	= new Line3 ();

		line.orig.set (other.orig);
		line.orig.scaleAdd (v, t0);
		line.dest.set (other.orig);
		line.dest.scaleAdd (v, t1);

		return line;
	}

	/** Returns the intersection point of two segments. If it does not
	 * exists, it returns the closest one. 
	 */
	public Point3 intersection (Line3 other)
	{
		u.sub (dest, orig);
		v.sub (other.dest, other.orig);
		w.sub (orig, other.orig);

		double    a = u.dot (u);        // always >= 0
		double    b = u.dot (v);
		double    c = v.dot (v);        // always >= 0
		double    d = u.dot (w);
		double    e = v.dot (w);
		double    D = a*c - b*b;       // always >= 0
		double    sc, sN, sD = D;      // sc = sN / sD, default sD = D >= 0
		double    tN, tD = D;      // tc = tN / tD, default tD = D >= 0

		// compute the line parameters of the two closest points
		if (D < 1.0e-6) { // the lines are almost parallel
			sN = 0.0;        // force using point P0 on segment S1
			sD = 1.0;        // to prevent possible division by 0.0 later
			tN = e;
			tD = c;
		}
		else {                // get the closest points on the infinite lines
			sN = (b*e - c*d);
			tN = (a*e - b*d);
			if (sN < 0.0) {       // sc < 0 => the s=0 edge is visible
				sN = 0.0;
				tN = e;
				tD = c;
			}
			else if (sN > sD) {  // sc > 1 => the s=1 edge is visible
				sN = sD;
				tN = e + b;
				tD = c;
			}
		}

		if (tN < 0.0) {           // tc < 0 => the t=0 edge is visible
			tN = 0.0;
			// recompute sc for this edge
			if (-d < 0.0)
				sN = 0.0;
			else if (-d > a)
				sN = sD;
			else {
				sN = -d;
				sD = a;
			}
		}
		else if (tN > tD) {      // tc > 1 => the t=1 edge is visible
			tN = tD;
			// recompute sc for this edge
			if ((-d + b) < 0.0)
				sN = 0;
			else if ((-d + b) > a)
				sN = sD;
			else {
				sN = (-d + b);
				sD = a;
			}
		}
		// finally do the division to get sc and tc
		sc = (Math.abs (sN) < 1.0e-6 ? 0.0 : sN / sD);
		//tc = (Math.abs (tN) < 1.0e-6 ? 0.0 : tN / tD);

		pb.set (orig);
		pb.scaleAdd (u, sc);
//		pb.set (other.orig);
//		pb.scaleAdd (v, tc);

		return pb; 
	}

	public String toString ()
	{
		return "LINE[O="+orig+", D="+dest+"]";
	}
}
