/* ----------------------------------------
	(c) 2002-2003 Humberto Martinez Barbera
   ---------------------------------------- */

package devices.pos;

import java.io.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class Pose extends Object implements Serializable
{	
	protected Point3				ang;				// Orientation 3D coordinates (rad)
	protected Point3				spd;				// Linear velocities 3D magnitudes (m/s)
	protected Point3				rate;			// Angular velocities 3D magnitudes (rad/s)

	protected boolean			valid	= false;

	// Constructors
	public Pose ()
	{
		ang		= new Point3 ();
		spd		= new Point3 ();
		rate		= new Point3 ();
	}
	
	// Accessors
	public final double 		roll ()					{ return ang.x (); }
	public final double 		pitch ()					{ return ang.y (); }
	public final double 		yaw ()					{ return ang.z (); }

	public final double 		rollRate ()				{ return rate.x (); }
	public final double 		pitchRate ()				{ return rate.y (); }
	public final double 		yawRate ()				{ return rate.z (); }

	public final double 		velx ()					{ return spd.x (); }
	public final double 		vely ()					{ return spd.y (); }
	public final double 		velz ()					{ return spd.z (); }

	public final void	 	valid (boolean valid)	{ this.valid = valid; }
	public final boolean	 	valid ()					{ return valid; }

	// Instance methods 
	public void set (Pose pose)
	{
		set_ang (pose.ang.x (), pose.ang.y (), pose.ang.z ());
		set_spd (pose.spd.x (), pose.spd.y (), pose.spd.z ());
		set_rate (pose.rate.x (), pose.rate.y (), pose.rate.z ());
		
		valid (pose.valid);
	}
	
	// Angular position
	public void set_ang (double roll, double pitch, double yaw)
	{
		ang.set (Angles.radnorm_180 (roll), Angles.radnorm_180 (pitch), Angles.radnorm_180 (yaw));
	}
	
	public void set_ang (double roll, double pitch, double yaw, boolean valid)
	{
		set_ang (roll, pitch, yaw);
		
		valid (valid);
	}
	
	// Linear velocities
	public void set_spd (double velx, double vely, double velz)
	{
		spd.set (velx, vely, velz);
	}
	
	public void set_spd (double velx, double vely, double velz, boolean valid)
	{
		set_spd (velx, vely, velz);
		
		valid (valid);
	}
	
	// Angular velocities
	public void set_rate (double ratex, double ratey, double ratez)
	{
		rate.set (ratex, ratey, ratez);
	}
	
	public void set_rate (double ratex, double ratey, double ratez, boolean valid)
	{
		set_rate (ratex, ratey, ratez);
		
		valid (valid);
	}
	
	public String toString ()
	{
		return ("[" + (int) (ang.x () * Angles.RTOD) + ", " + (int) (ang.y () * Angles.RTOD) + ", " + (int) (ang.z () * Angles.RTOD) + "]");
	}
}

