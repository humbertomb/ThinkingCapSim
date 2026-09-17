/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;

/**
 * A skid-steer platform: its driving wheels sit on two axles and none of them is
 * steered, so it can only turn by dragging them sideways.
 *
 * What it does is what a differential drive does -- the two sides run at their
 * own speed and the platform turns about the point between them -- but not about
 * the track its wheels are built at: while they slide, the centre each wheel
 * turns about moves away from where it touches the floor, and the platform turns
 * about a wider track than it measures. It therefore turns more slowly than the
 * geometry alone says, and by how much is a matter of the ground it is on, not
 * of the drawing: it is measured, not worked out.
 *
 * That is all this model adds to {@link DifferentialDrive}: the skid factor
 * (<code>SKID</code>), the effective track over the geometric one. A Pioneer
 * 3-AT, 0.381 m of track, turns as if it were 0.58 -- a factor of about 1.5,
 * which is the usual range for four wheels on a hard floor. At 1.0 the platform
 * is taken to turn about its own track, which is what a differential drive with
 * a single axle does.
 */
public class SkidSteerDrive extends DifferentialDrive
{
	/** A platform that says nothing turns about its own track, as a differential drive does. */
	static public final double		SKID_NONE	= 1.0;

	/**
	 * The effective track over the geometric one.
	 *
	 * It is deliberately left without an initial value: the constructor of
	 * {@link RobotModel} reads the properties, so {@link #update(Properties)}
	 * runs before the fields of this class would be initialised, and anything
	 * set here would be written over what was read.
	 */
	public transient double			skid;

	// Constructors
	public SkidSteerDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public SkidSteerDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);
	}

	// Instance methods
	public void update (Properties props)
	{
		super.update (props);

		skid	= SKID_NONE;
		try { skid = Double.valueOf (props.getProperty ("SKID")).doubleValue (); }	catch (Exception e)		{ }
		if (skid <= 0.0)		skid = SKID_NONE;

		// what it turns about is the effective track, so every formula it inherits uses that
		b	*= skid;
	}
}
