/*
 * (c) 1997-2002 Humberto Martinez
 */

package wucore.utils.geom;
 
public class Ellipse2 extends Object
{
	protected Point2			center;					// Center
	protected double			horiz;					// Horizontal size
	protected double			vert;					// vertical size

	/* Constructors */
	public Ellipse2 ()
	{
		this.create ();
	}
	
	public Ellipse2 (double x, double y, double h, double v)
	{
		this.create ();
		this.set (x, y, h, v);
	}
	
	/* Accessor methods */
	public final Point2 	center () 					{ return center; }
	public final double 	horiz () 					{ return horiz; }
	public final double 	vert () 					{ return vert; }

	/* Instance methods */
	protected void create ()
	{
		this.center		= new Point2 (0.0, 0.0);
		this.horiz		= 0.0;
		this.vert		= 0.0;
	}
			
	public void set (double x, double y, double h, double v)
	{
		center.set (x, y);
		this.horiz		= h;
		this.vert		= v;
	}
	
	public String toString ()
	{
		return ("[" + center.toString () + ", " + horiz + ", " + vert + "]");
	}
}
