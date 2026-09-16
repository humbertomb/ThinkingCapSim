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
	protected double					theta;	// Angle to the sensor (rad)
	
	protected double					orientation;	// Orientation of the sensor over the horizontal plane (rad)
	protected double					elevation;	// Orientation of the sensor over the vertical plane (rad)
    protected int                   	mode;	// Sensor mode for sensor fusion
	protected int					step;	// Cycle at which the sensor is fired (firing pattern)
	
	/* Constructors */
	public SensorPos ()
	{
		x		= 0.0;
		y		= 0.0;
		z		= 0.0;
		rho			= 0.0;
		theta		= 0.0;
		orientation	= 0.0;
		elevation	= 0.0;
		mode		= -1;
		step		= 1;
	}
	
	/* Accessor methods */
	public final double	 	x () 			{ return x; }
	public final double	 	y () 			{ return y; }
	public final double	 	z () 			{ return z; }
	public final double	 	rho () 			{ return rho; }
	public final double	 	theta () 		{ return theta; }
	public final double	 	orientation () 	{ return orientation; }
	public final double	 	elevation () 	{ return elevation; }
	public final void	 	elevation (double elevation)	{ this.elevation = elevation; }
	
	public final void	 	mode (int mode) 	{ this.mode = mode; }
	public final int	 		mode () 			{ return mode; }
	public final void	 	step (int step) 	{ this.step = step; }
	public final int	 		step () 			{ return step; }

	/* Instance methods */
	public void set_xy (double x, double y, double orientation)
	{
		this.orientation	= orientation;
		this.x				= x;
		this.y				= y;
		this.rho			= Math.sqrt (x * x + y * y);
		this.theta			= Math.atan2 (y, x);
	}
	
	public void set_polar (double rho, double theta, double orientation)
	{
		this.orientation	= orientation;
		this.rho			= rho;
		this.theta			= theta;
		this.x				= rho * Math.cos (theta);
		this.y				= rho * Math.sin (theta);
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