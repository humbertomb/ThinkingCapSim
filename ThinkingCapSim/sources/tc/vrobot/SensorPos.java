/* ----------------------------------------
	(c) 2000-2002 Humberto Martinez Barbera
   ---------------------------------------- */

package tc.vrobot;

import java.io.*;

public class SensorPos extends Object implements Serializable
{
	// Sensor's euclidean position
	protected double					x;		// X-position of the sensor (m)
	protected double					y;		// Y-position of the sensor (m)
	protected double					z;		// Z-position of the sensor (m)
	
	// Sensor's polar position
	protected double					rho;		// Distance to the sensor (m)
	protected double					phi;		// Angle to the sensor (rad)
	
	protected double					alpha;	// Orientation of the sensor (rad)
    protected int                   	mode;	// Sensor mode for sensor fusion
	protected int					step;	// Cycle at which the sensor is fired (firing pattern)
	
	/* Constructors */
	public SensorPos ()
	{
		x		= 0.0;
		y		= 0.0;
		z		= 0.0;
		rho		= 0.0;
		phi		= 0.0;
		alpha	= 0.0;
		mode		= -1;
		step		= 1;
	}
	
	/* Accessor methods */
	public final double	 	x () 			{ return x; }
	public final double	 	y () 			{ return y; }
	public final double	 	z () 			{ return z; }
	public final double	 	rho () 			{ return rho; }
	public final double	 	phi () 			{ return phi; }
	public final double	 	alpha () 		{ return alpha; }
	
	public final void	 	mode (int mode) 	{ this.mode = mode; }
	public final int	 		mode () 			{ return mode; }
	public final void	 	step (int step) 	{ this.step = step; }
	public final int	 		step () 			{ return step; }

	/* Instance methods */
	public void set_xy (double x, double y, double alpha)
	{
		this.alpha	= alpha;
		this.x		= x;
		this.y		= y;
		this.rho		= Math.sqrt (x * x + y * y);
		this.phi		= Math.atan2 (y, x);
	}
	
	public void set_polar (double rho, double phi, double alpha)
	{
		this.alpha	= alpha;
		this.rho		= rho;
		this.phi		= phi;
		this.x		= rho * Math.cos (phi);
		this.y		= rho * Math.sin (phi);
	}
	
	public void set_height (double z)
	{
		this.z		= z;
	}
	
	public double distance (SensorPos sen)
	{
	    return Math.sqrt ((sen.x - x) * (sen.x - x) + (sen.y - y) * (sen.y - y));
	}
} 