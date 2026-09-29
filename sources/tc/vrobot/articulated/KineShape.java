/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

/**
 * One solid a link of an articulated robot is drawn with, in the frame of the
 * link: a box, a cylinder, a sphere or a capsule (a cylinder with a sphere on
 * each end), where it sits and how it is turned in that frame, and its colour.
 * As it is kept in the .kine file (metres, radians, colours 0 to 1).
 *
 * <pre>
 *   type         box | cylinder | sphere | capsule
 *   size         box: {sx, sy, sz}, the full sides
 *   radius       cylinder, sphere, capsule
 *   height       cylinder, capsule: the length along its axis (a capsule's, without its caps)
 *   axis         cylinder, capsule: "x", "y" or "z", the axis it is drawn along (z by default)
 *   translation  {x, y, z} of its centre in the frame of the link (0 0 0)
 *   rotation     {ax, ay, az, angle}, how it is turned about its centre (none)
 *   color        {r, g, b} (a light grey)
 * </pre>
 */
public class KineShape
{
	static public final String		BOX			= "box";
	static public final String		CYLINDER	= "cylinder";
	static public final String		SPHERE		= "sphere";
	static public final String		CAPSULE		= "capsule";

	public String					type		= BOX;
	public double[]					size;
	public double					radius;
	public double					height;
	public String					axis;
	public double[]					translation;
	public double[]					rotation;
	public double[]					color;

	public KineShape ()									{ }

	static public KineShape box (double sx, double sy, double sz)
	{
		KineShape	s = new KineShape ();

		s.type	= BOX;
		s.size	= new double[] { sx, sy, sz };
		return s;
	}

	static public KineShape cylinder (double radius, double height, String axis)
	{
		KineShape	s = new KineShape ();

		s.type		= CYLINDER;
		s.radius	= radius;
		s.height	= height;
		s.axis		= axis;
		return s;
	}

	static public KineShape sphere (double radius)
	{
		KineShape	s = new KineShape ();

		s.type		= SPHERE;
		s.radius	= radius;
		return s;
	}

	static public KineShape capsule (double radius, double height, String axis)
	{
		KineShape	s = new KineShape ();

		s.type		= CAPSULE;
		s.radius	= radius;
		s.height	= height;
		s.axis		= axis;
		return s;
	}

	public KineShape at (double x, double y, double z)	{ translation = new double[] { x, y, z };	return this; }
	public KineShape turned (double ax, double ay, double az, double angle)	{ rotation = new double[] { ax, ay, az, angle };	return this; }
	public KineShape colored (double r, double g, double b)	{ color = new double[] { r, g, b };	return this; }

	/** Where it sits in the frame of its link, as a frame. */
	public Frame frame ()
	{
		return Frame.translation (translation).times (Frame.rotation (rotation));
	}

	/** The axis a cylinder or a capsule is drawn along: 'x', 'y' or 'z'. */
	public char along ()
	{
		return ((axis != null) && (axis.length () > 0)) ? Character.toLowerCase (axis.charAt (0)) : 'z';
	}

	/** The colour, with a light grey for none. */
	public double[] rgb ()
	{
		return ((color != null) && (color.length >= 3)) ? color : new double[] { 0.8, 0.8, 0.8 };
	}
}
