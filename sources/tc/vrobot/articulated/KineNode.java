/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

import java.util.ArrayList;
import java.util.List;

/**
 * A link of an articulated robot: where its frame sits on its parent's (a
 * translation and a fixed turn), the joint it turns about, if any (a link with
 * none is rigidly fixed to its parent), the solids it is drawn with, and the
 * links that hang from it. The tree of them is the robot ({@link KineModel}).
 *
 * The frame of a link is placed on its parent by <code>translation</code> and
 * <code>rotation</code>, and then turned by its joint; what it holds (its
 * shapes and its children) moves with the turn. A segment of a chain is thus
 * a node whose child sits <code>translation</code> away from it: the length
 * of the segment is the distance between the two.
 *
 * <pre>
 *   name         the link, unique in the robot (the joints are named after their link)
 *   translation  {x, y, z} of its frame on its parent's (0 0 0)
 *   rotation     {ax, ay, az, angle}, a fixed turn of its frame on its parent's (none)
 *   joint        how it turns (none: fixed)
 *   mesh         the 3D Studio file it is drawn with when the robot has its parts (a file name,
 *                in the folder of the parts of the robot; the model is in the frame of the link,
 *                Z up as the robot), or nothing
 *   shapes       the solids it is drawn with otherwise (or when a part is missing), in its frame
 *   children     the links that hang from it
 * </pre>
 */
public class KineNode
{
	public String					name;
	public double[]					translation;
	public double[]					rotation;
	public KineJoint				joint;
	public String					mesh;
	public List<KineShape>			shapes		= new ArrayList<KineShape> ();
	public List<KineNode>			children	= new ArrayList<KineNode> ();

	/* At run time: not kept in the file */
	transient KineNode				parent;
	transient double				angle;					// where the joint is now (rad)
	transient Frame					local;					// its frame on its parent's, with the joint as it is
	transient Frame					world;					// its frame on the robot's root

	public KineNode ()									{ }

	public KineNode (String name, double x, double y, double z)
	{
		this.name			= name;
		this.translation	= new double[] { x, y, z };
	}

	public KineNode turned (double ax, double ay, double az, double angle)	{ rotation = new double[] { ax, ay, az, angle };	return this; }
	public KineNode jointed (KineJoint j)				{ joint = j;	return this; }
	public KineNode shape (KineShape s)					{ shapes.add (s);	return this; }
	public KineNode child (KineNode n)					{ children.add (n);	return this; }

	public KineNode parent ()							{ return parent; }
	public boolean isJoint ()							{ return joint != null; }
	public double angle ()								{ return angle; }

	/** Its frame on its parent's, with the joint at the angle it is now. */
	public Frame local ()								{ return local; }

	/** Its frame on the robot's root, as last computed ({@link KineModel#forward}). */
	public Frame world ()								{ return world; }

	/** The length of the segment from its parent to it (m): how far its frame is from its parent's. */
	public double length ()
	{
		if (translation == null)		return 0.0;
		return Math.sqrt (translation[0] * translation[0] + translation[1] * translation[1] + translation[2] * translation[2]);
	}

	/** Its frame on its parent's, from its placing and the turn of its joint. */
	protected Frame place ()
	{
		Frame	f = Frame.translation (translation).times (Frame.rotation (rotation));

		if (joint != null)				f = f.times (joint.turn (angle));
		local	= f;
		return f;
	}
}
