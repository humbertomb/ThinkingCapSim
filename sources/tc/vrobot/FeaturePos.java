/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tc.vrobot;

import wucore.utils.math.*;

/** A sensor of an area: where it sits and looks (a SensorPos), and the arc it covers. */
public class FeaturePos extends SensorPos
{
	protected double					cone;
	protected double					range;
    
	/* Constructors */
	public FeaturePos ()
	{
		super ();
	}
	
	/* Accessor methods */
	public final double		 	cone () 			{ return cone; }
	public final double		 	range () 			{ return range; }

	/** The arc the sensor of an area covers: its aperture (rad) and how far it reaches (m). */
	public void set_shape (double cone, double range)
	{
		this.cone	= cone;
		this.range	= range;
	}
	
	public String toString ()
	{
		return "sensor " + (orientation * Angles.RTOD) + " deg, cone " + (cone * Angles.RTOD) + " deg, range " + range + " m";
	}
}
