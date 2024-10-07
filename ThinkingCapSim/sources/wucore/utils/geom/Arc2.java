/*
 * (c) 1997-2002 Humberto Martinez
 */
 
package wucore.utils.geom;

public class Arc2 extends Object
{
	protected Point2			center;					// Center
	protected double			rad;					// Radius
	protected double			s_ang;					// Start angle
	protected double			e_ang;					// End angle

	/* Constructors */
	public Arc2 ()
	{
		create ();
	}
	
	public Arc2 (double x, double y, double r, double sa, double ea)
	{
		create ();
		set (x, y, r, sa, ea);
	}
	
	/* Accessor methods */
	public final Point2 	center () 					{ return center; }
	public final double 	radius () 					{ return rad; }

	/* Instance methods */
	protected void create ()
	{
		center	= new Point2 ();
		rad		= 0.0;
		s_ang	= 0.0;
		e_ang	= 0.0;
	}
			
	public void set (double x, double y, double r, double sa, double ea)
	{
		center.set (x, y);
		rad		= r;
		s_ang	= sa;
		e_ang	= ea;
	}
	
	public void set (Point2 p1, Point2 p2, Point2 p3)
	{
		double			m1, n1, m2, n2;
		
		m1	= (p2.x - p1.x) / (p2.y - p1.y);
		n1	= p1.x - m1 * p1.y;
		m2	= (p2.x - p3.x) / (p2.y - p3.y);
		n2	= p3.x - m2 * p3.y;
		
		center.x	= (n2 - n1) / (m1 - m2);
		center.y	= (n2*m1 - n1*m2) / (m1 - m2);
		rad			= center.distance (p2);
		s_ang		= Math.atan2 (p1.y - center.y, p1.x - center.x);
		e_ang		= Math.atan2 (p3.y - center.y, p3.x - center.x);
	}
	
	public String toString ()
	{
		return ("[" + center + ", " + rad + ", " + s_ang + ", " + e_ang + "]");
	}
}
