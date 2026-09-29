/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

/**
 * The articulation of a link on its parent: a revolute joint, turning about an
 * axis of the link's own frame, within limits and resting at a default angle
 * (radians). As it is kept in the .kine file.
 *
 * <pre>
 *   type      revolute (the only one so far)
 *   axis      {x, y, z} in the frame of the link, before it turns (z by default)
 *   min, max  the angles it may take (rad); none for no limit
 *   def       the angle it rests at (0)
 *   velocity  how fast it may turn (rad/s); 0 for no limit
 * </pre>
 */
public class KineJoint
{
	static public final String		REVOLUTE	= "revolute";

	public String					type		= REVOLUTE;
	public double[]					axis		= { 0.0, 0.0, 1.0 };
	public Double					min, max;
	public double					def;
	public double					velocity;

	public KineJoint ()									{ }

	public KineJoint (double ax, double ay, double az, double min, double max, double def)
	{
		this.axis	= new double[] { ax, ay, az };
		this.min	= Double.valueOf (min);
		this.max	= Double.valueOf (max);
		this.def	= def;
	}

	/** An angle brought within the limits, when there are any. */
	public double clamp (double a)
	{
		if ((min != null) && (a < min.doubleValue ()))		a = min.doubleValue ();
		if ((max != null) && (a > max.doubleValue ()))		a = max.doubleValue ();
		return a;
	}

	/** The turn of the joint by an angle, as a frame. */
	public Frame turn (double angle)
	{
		return Frame.rotation (axis[0], axis[1], axis[2], angle);
	}
}
