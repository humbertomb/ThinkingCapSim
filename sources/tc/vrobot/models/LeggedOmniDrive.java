/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;

/**
 * A platform that goes where it is told: a point that carries out the three
 * velocities of the control action as they are given, each of them on its own
 * and none of them costing the others anything (vlin, vlat, vrot).
 *
 * That is what a legged platform does, and why it has no drive train here: how
 * it walks is a matter of its legs, of how they take turns and where they are
 * put down, and none of that is seen from outside -- what is seen is a body that
 * goes forward, goes sideways and turns, all at once if that is what it was
 * asked for. A wheeled platform is the other way about: what its wheels are and
 * where they are built is what says what the body can do, which is what every
 * other model here works out.
 *
 * So it asks for nothing but how fast it goes each way, which is what limits it:
 *
 * <pre>
 *   VMAX   how fast it goes forward (m/s)
 *   UMAX   how fast it goes sideways (m/s)
 *   RMAX   how fast it turns (deg/s in the description, radians a second here)
 * </pre>
 *
 * Nothing is worked out of any geometry, and nothing is asked of the wheels,
 * because it has none. More can be asked of it later -- how fast it may gain
 * speed, what a step costs it, how well it keeps a heading -- and a platform that
 * says nothing of them will go on doing what it does now.
 */
public class LeggedOmniDrive extends RobotModel
{
	// Constructors
	public LeggedOmniDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public LeggedOmniDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);
	}

	// Instance methods
	/** It is a point with legs under it: it goes sideways as readily as forward. */
	public boolean lateral ()							{ return true; }

	/**
	 * What the platform does with what it is built with: it is built with nothing
	 * that has a say, so this is the control action itself, carried out as it is
	 * given. The two-argument one is the three-argument one with nothing sideways,
	 * as every other model has it.
	 */
	public void kynematics_direct (double vlin, double vrot)
	{
		kynematics_direct (vlin, 0.0, vrot);
	}

	public void kynematics_direct (double vlin, double vlat, double vrot)
	{
		vr	= vlin;
		ur	= vlat;
		wr	= vrot;
	}

	public void kynematics_inverse (double vlin, double vlat, double vrot)
	{
		// Set motion commands into the correct range: each way as fast as it goes
		// that way, and no way costing another anything
		vr	= Math.min (Math.max (vlin, -Vmax), Vmax);
		ur	= Math.min (Math.max (vlat, -Umax), Umax);
		wr	= Math.min (Math.max (vrot, -Rmax), Rmax);
	}

	public void kynematics_simulation (double vlin, double vlat, double vrot)
	{
		// Compute robot command (inverse kynematics): there is nothing between what
		// it is asked for and what it does, so there is nothing else to work out
		kynematics_inverse (vlin, vlat, vrot);
	}
}
