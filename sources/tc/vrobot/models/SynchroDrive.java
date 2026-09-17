/*
 * Created on 04-sep-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;
import wucore.utils.math.Angles;

/**
 * @author Humberto Martinez Barbera
 *
 * A synchro drive platform: every wheel is driven at the same speed and every
 * wheel is steered together to the same angle, by two motors of their own -- one
 * for the driving and one for the steering.
 *
 * That is what sets it apart from a differential drive, which turns by running
 * one side faster than the other and therefore pays for turning with speed: here
 * the two motors are independent, so the platform can go flat out and turn flat
 * out at the same time, and it can turn on the spot without moving. None of it
 * depends on where the wheels are: a synchro drive has no track and no wheel base
 * to speak of, which is why this model asks for no geometry at all.
 *
 * The platform drives where its wheels point and turns with them, so how fast it
 * can turn is how fast its steering moves (SAMAX) and not, as in every other
 * model, something its track or its wheel base works out. And, as its wheels
 * cannot jump to a new direction, the turn it makes in one cycle is limited to
 * what the steering covers in that cycle, the way a tricycle limits its steering
 * wheel.
 */
public class SynchroDrive extends RobotModel
{
	// Kynematics outputs
	public transient double		vm, del;			// Synchro model (wheel speed, and the turn its wheels make this cycle)

	// Dynamics simulation
	protected transient double	lvm;				// Synchro model (previously issued speed)

	/*
	 * Maximum values of kynematics parameters.
	 *
	 * None of them is given a value here on purpose: the constructor of
	 * RobotModel reads the properties, so update () runs before the fields of
	 * this class would be initialised, and anything set here would be written
	 * over what was read.  They are defaulted at the head of update () instead.
	 */
	private double		SAmax;							// Maximum steering angular velocity (rad/s)
	private double		LAmax;							// Maximum linear acceleration (m/s2)
	private double		LDmax;							// Maximum linear decceleration (m/s2)

	// Constructors
	public SynchroDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public SynchroDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);

		// Initialise simulation parameters
		lvm		= 0.0;
	}

	// Instance methods
	public void update (Properties props)
	{
		super.update (props);

		SAmax	= 0.0;
		LAmax	= Double.MAX_VALUE;
		LDmax	= Double.MAX_VALUE;
		try { SAmax	 = Double.valueOf (props.getProperty ("SAMAX")).doubleValue () * Angles.DTOR; }	catch (Exception e)		{ }
		try { LAmax	 = Double.valueOf (props.getProperty ("LAMAX")).doubleValue (); }				catch (Exception e)		{ }
		try { LDmax	 = Double.valueOf (props.getProperty ("LDMAX")).doubleValue (); }				catch (Exception e)		{ }

		// Nothing is asked of the drive train: how fast it turns is how fast its
		// steering moves, and neither that nor its speed depends on its geometry
		if (SAmax <= 0.0)		SAmax = Rmax;			// it says nothing: as fast as the platform may turn
	}

	public void kynematics_direct (double vm, double del)
	{
		double			dt;
		double			sdmax;
		double			amax, dmax;

		this.vm		= vm;
		this.del		= del;

		dt			= rdesc.DTIME / 1000.0;

		// Check for steering dynamics constraints: the wheels turn no faster than they can
		sdmax		= (SAmax * dt);
		if (del > sdmax)
			del		= sdmax;
		else if (del < -sdmax)
			del		= -sdmax;

		// Check for acceleration/decceleration constraints
		amax			= (LAmax * dt);
		dmax			= (LDmax * dt);
		if( Math.abs(vm) > Math.abs(lvm) )
		{
			if(Math.abs(vm-lvm) > amax)
			{
				if(vm > 0)
					vm = lvm + amax;
				else
					vm = lvm - amax;
			}
		}
		else
		{
			if(Math.abs(vm-lvm) > dmax)
			{
				if(lvm > 0)
					vm = lvm - dmax;
				else
					vm = lvm + dmax;
			}
		}

		// Compute direct kynematics: it goes exactly as fast as its wheels, since
		// they all point the same way, and it turns with them
		vr		= vm;
		wr		= (dt > 0.0) ? (del / dt) : 0.0;

		this.del	= del;
		lvm		= vm;
	}

	public void kynematics_inverse (double speed, double turn)
	{
		double			dt = rdesc.DTIME / 1000.0;

		// Set motion commands into the correct range
		vr	= Math.min (Math.max (speed, -Vmax), Vmax);
		wr	= Math.min (Math.max (turn, -Rmax), Rmax);

		// Compute robot command (inverse kynematics): the driving motor answers the
		// speed and the steering motor the turn, neither costing the other anything
		vm	= vr;
		del	= wr * dt;

		// Check for kynematics constraints
		vm	= Math.min (Math.max (vm, -Vmax), Vmax);
		del	= Math.min (Math.max (del, -SAmax * dt), SAmax * dt);
	}

	public void kynematics_simulation (double speed, double turn)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (speed, turn);

		// Add some noise to control commands, as a share of what was asked for, so
		// that a platform standing still does not wander off on its own
		del += rnd.nextGaussian() * del * 0.10;
		vm  += rnd.nextGaussian() * vm * 0.10;

		// Compute robot displacement (direct kynematics)
		kynematics_direct (vm, del);
	}
}
