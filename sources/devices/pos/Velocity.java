/* ----------------------------------------
	(c) 2001 Humberto Martinez Barbera
   ---------------------------------------- */

package devices.pos;

public class Velocity extends Object
{	
	protected double				v;				// Linear speed
	protected double				w;				// Rotational speed

	// Constructors
	public Velocity ()
	{
		this.set (0.0, 0.0);
	}
	
	public Velocity (double v, double w)
	{
		this.set (v, w);
	}
	
	// Accessors
	public final double 			v ()			{ return v; }
	public final double 			w ()			{ return w; }

	// Instance methods 
	public void set (double v, double w)
	{
		this.v		= v;
		this.w		= w;
	}
	
	public void set (Velocity vel)
	{
		this.v		= vel.v;
		this.w		= vel.w;
	}
}
