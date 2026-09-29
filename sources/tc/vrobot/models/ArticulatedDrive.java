/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;

/**
 * A legged platform that goes where it is told, and shows how: as
 * {@link LeggedOmniDrive}, it is a point that carries out the three velocities
 * of the control action as they are given (vlin, vlat, vrot), none of them
 * costing the others anything -- what its legs do to bring that about is not
 * seen from outside the body. What this one adds is that it says how it is to
 * be seen walking: the platform names a kinematic model of its links and
 * joints (a .kine file, the articulated shape of the platform) and a walking
 * model (a class, the one of the Aibo for now) that turns the velocities and
 * the pan and tilt of the camera into the angles of the joints, and the 3D view
 * of the simulator articulates the model with them while the robot moves.
 *
 * It asks for nothing but how fast it goes each way, which is what limits it:
 *
 * <pre>
 *   VMAX       how fast it goes forward (m/s)
 *   UMAX       how fast it goes sideways (m/s)
 *   RMAX       how fast it turns (deg/s in the description, radians a second here)
 *   WALKMODEL  the class of the walking model (tcrob.umu.soccer.walking.AiboWalking)
 *   KINEFILE   the .kine file of the platform (the kinematics model, beside the drive type)
 *   V3DPARTS   the folder of the 3D models of its parts (the robot parts, in the platform's group)
 * </pre>
 *
 * The kinematics itself is that of {@link LeggedOmniDrive}: nothing is worked
 * out of any geometry, and nothing is asked of the wheels, because it has none.
 */
public class ArticulatedDrive extends RobotModel
{
	// Constructors
	public ArticulatedDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public ArticulatedDrive (RobotDesc rdesc, Properties props)
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
