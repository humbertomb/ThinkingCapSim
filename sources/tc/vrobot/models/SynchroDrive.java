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
 *
 * It is also the one model so far that can be driven sideways: as every wheel is
 * steered together and none of them has to point where the platform does, it
 * carries out a velocity of its own choosing -- forward and sideways at once
 * (vlin, vlat) -- and the two of them together are what its driving motor has to
 * cover, which is what limits them. Going sideways costs it nothing in turning
 * and nothing in speed but what the other direction takes.
 *
 * How fast it goes sideways is how fast it goes forward, and not a figure of its
 * own: it is one motor driving its wheels, and they are pointed wherever the
 * platform is to go.
 */
public class SynchroDrive extends RobotModel
{
	// Kynematics outputs
	public transient double		vm, del;			// Synchro model (wheel speed, and the turn its wheels make this cycle)
	public transient double		um;					// Synchro model (wheel speed sideways, to the left of the platform)

	// Dynamics simulation
	protected transient double	lvm;				// Synchro model (previously issued speed)
	protected transient double	lum;				// Synchro model (previously issued speed sideways)

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
		lum		= 0.0;
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

		// It goes sideways as fast as it goes forward: the same motor drives the same
		// wheels, whichever way they have been pointed
		Umax	= Vmax;
	}

	public void kynematics_direct (double vm, double del)
	{
		kynematics_direct (vm, 0.0, del);
	}

	/**
	 * What the platform does with wheels driven at a speed of their own, forward and
	 * sideways, and steered by a turn: the same as the two-argument one, which is
	 * this with nothing sideways.
	 */
	public void kynematics_direct (double vm, double um, double del)
	{
		double			dt;
		double			sdmax;
		double			amax, dmax;

		this.vm		= vm;
		this.um		= um;
		this.del		= del;

		dt			= rdesc.DTIME / 1000.0;

		// Check for steering dynamics constraints: the wheels turn no faster than they can
		sdmax		= (SAmax * dt);
		if (del > sdmax)
			del		= sdmax;
		else if (del < -sdmax)
			del		= -sdmax;

		// Check for acceleration/decceleration constraints, of the speed it is
		// driven at and of the speed it is taken sideways at, which are the same
		// motor and the same limits
		amax			= (LAmax * dt);
		dmax			= (LDmax * dt);
		vm			= reachable (vm, lvm, amax, dmax);
		um			= reachable (um, lum, amax, dmax);

		// Compute direct kynematics: it goes exactly as fast as its wheels, since
		// they all point the same way, and it turns with them
		vr		= vm;
		ur		= um;
		wr		= (dt > 0.0) ? (del / dt) : 0.0;

		this.del	= del;
		lvm		= vm;
		lum		= um;
	}

	/**
	 * A speed the platform can be at by the end of this cycle, given the one it was
	 * at and how much it may gain (amax) or lose (dmax) in a cycle: what it was
	 * asked for when it can, and as near to it as the motor goes when it cannot.
	 */
	protected double reachable (double v, double lv, double amax, double dmax)
	{
		if( Math.abs(v) > Math.abs(lv) )
		{
			if(Math.abs(v-lv) > amax)
			{
				if(v > 0)
					v = lv + amax;
				else
					v = lv - amax;
			}
		}
		else
		{
			if(Math.abs(v-lv) > dmax)
			{
				if(lv > 0)
					v = lv - dmax;
				else
					v = lv + dmax;
			}
		}
		return v;
	}

	/** A synchro drive steers every wheel together, so it can be driven sideways. */
	public boolean lateral ()							{ return true; }

	public void kynematics_inverse (double vlin, double vlat, double vrot)
	{
		double			dt = rdesc.DTIME / 1000.0;
		double			v;

		// Set motion commands into the correct range, each way as fast as it goes
		// that way
		vr	= Math.min (Math.max (vlin, -Vmax), Vmax);
		ur	= Math.min (Math.max (vlat, -Umax), Umax);
		wr	= Math.min (Math.max (vrot, -Rmax), Rmax);

		// The two directions are one velocity of the wheels, and it is that one the
		// platform has to cover: asked for more than it goes, it keeps the direction
		// it was asked to go in and goes as fast as it can that way. What it may do
		// of each is what it is measured against, so the whole of Vmax forward and
		// the whole of Umax sideways are each of them all it has
		v	= Math.hypot (share (vr, Vmax), share (ur, Umax));
		if (v > 1.0)
		{
			vr	/= v;
			ur	/= v;
		}

		// Compute robot command (inverse kynematics): the driving motor answers the
		// speed and the steering motor the turn, neither costing the other anything
		vm	= vr;
		um	= ur;
		del	= wr * dt;

		// Check for kynematics constraints
		vm	= Math.min (Math.max (vm, -Vmax), Vmax);
		um	= Math.min (Math.max (um, -Umax), Umax);
		del	= Math.min (Math.max (del, -SAmax * dt), SAmax * dt);
	}

	public void kynematics_simulation (double vlin, double vlat, double vrot)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (vlin, vlat, vrot);

		// Add some noise to control commands, as a share of what was asked for, so
		// that a platform standing still does not wander off on its own. A platform
		// that is not going sideways draws no random number for it, so that what it
		// does is the same to the last decimal as it was before it could
		del += rnd.nextGaussian() * del * 0.10;
		vm  += rnd.nextGaussian() * vm * 0.10;
		if (um != 0.0)		um += rnd.nextGaussian() * um * 0.10;

		// Compute robot displacement (direct kynematics)
		kynematics_direct (vm, um, del);
	}
}
