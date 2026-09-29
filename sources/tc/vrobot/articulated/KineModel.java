/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The kinematic model of an articulated robot: a tree of links
 * ({@link KineNode}) rooted at its body, each placed on its parent and turning
 * about its joint, with the solids each is drawn with. Read from and written
 * to a .kine file ({@link KineJson}). The angles of the joints are set here
 * ({@link #setAngle}) and the frame of every link on the body follows
 * ({@link #forward}): the forward kinematics of the robot, which is what a
 * visualisation places its parts with.
 *
 * The frame of the body is the frame of the robot: x forward, y to its left,
 * z up, metres and radians, as the rest of the simulator has it.
 *
 * <pre>
 *   name         the robot
 *   description  a line about it (optional)
 *   root         the body, and everything that hangs from it
 * </pre>
 */
public class KineModel
{
	public String					name;
	public String					description;
	public KineNode					root;

	transient private List<KineNode>			nodes;			// every link, parents before children
	transient private Map<String, KineNode>		byName;

	public KineModel ()									{ }

	public KineModel (String name, KineNode root)
	{
		this.name	= name;
		this.root	= root;
		link ();
	}

	/**
	 * Puts the tree in order after it was read or built: parents known to their
	 * children, every link findable by name, every joint at its default angle,
	 * and the frames computed.
	 */
	public void link ()
	{
		nodes	= new ArrayList<KineNode> ();
		byName	= new LinkedHashMap<String, KineNode> ();
		if (root != null)				link (root, null);
		reset ();
	}

	private void link (KineNode n, KineNode parent)
	{
		n.parent	= parent;
		if (n.shapes == null)			n.shapes = new ArrayList<KineShape> ();
		if (n.children == null)			n.children = new ArrayList<KineNode> ();
		nodes.add (n);
		if (n.name != null)				byName.put (n.name, n);
		for (KineNode c : n.children)	link (c, n);
	}

	/** Every link, parents before their children. */
	public List<KineNode> nodes ()
	{
		if (nodes == null)				link ();
		return nodes;
	}

	/** The links that turn, in the same order. */
	public List<KineNode> joints ()
	{
		List<KineNode>	js = new ArrayList<KineNode> ();

		for (KineNode n : nodes ())		if (n.joint != null)		js.add (n);
		return js;
	}

	public KineNode node (String name)
	{
		if (byName == null)				link ();
		return byName.get (name);
	}

	/** Every joint back to its default angle, and the frames with them. */
	public void reset ()
	{
		for (KineNode n : nodes ())		n.angle = (n.joint != null) ? n.joint.def : 0.0;
		forward ();
	}

	/** The angle of a joint (rad), brought within its limits; nothing for a link that is not one, or is not there. Whether it was set. */
	public boolean setAngle (String joint, double angle)
	{
		KineNode	n = node (joint);

		if ((n == null) || (n.joint == null))		return false;
		n.angle	= n.joint.clamp (angle);
		return true;
	}

	/** The angle of a joint (rad), 0 for one that is not there. */
	public double angle (String joint)
	{
		KineNode	n = node (joint);

		return (n != null) ? n.angle : 0.0;
	}

	/** Several joints at once, by name, then the frames. */
	public void setAngles (Map<String, Double> angles)
	{
		if (angles != null)
			for (Map.Entry<String, Double> e : angles.entrySet ())		setAngle (e.getKey (), e.getValue ().doubleValue ());
		forward ();
	}

	/** The angles of every joint, by name. */
	public Map<String, Double> angles ()
	{
		Map<String, Double>	a = new LinkedHashMap<String, Double> ();

		for (KineNode n : joints ())		a.put (n.name, Double.valueOf (n.angle));
		return a;
	}

	/**
	 * The forward kinematics: the frame of every link on the body, from the
	 * angles the joints are at now. What each link's {@link KineNode#world}
	 * says afterwards.
	 */
	public void forward ()
	{
		for (KineNode n : nodes ())
		{
			Frame	local = n.place ();

			n.world	= (n.parent != null) ? n.parent.world.times (local) : local;
		}
	}

	/** Where the origin of a link is on the body (m), after {@link #forward}; null for a link that is not there. */
	public double[] position (String link)
	{
		KineNode	n = node (link);

		return ((n != null) && (n.world != null)) ? n.world.origin () : null;
	}

	/** The lowest point of any link's origin on the body (m): how far the body is over the feet, roughly, with the joints as they are. */
	public double lowest ()
	{
		double	z = 0.0;

		for (KineNode n : nodes ())		if (n.world != null)	z = Math.min (z, n.world.m[11]);
		return z;
	}

	public String toString ()
	{
		StringBuilder	sb = new StringBuilder (name + ": " + nodes ().size () + " links, " + joints ().size () + " joints\n");

		for (KineNode n : nodes ())
		{
			int		depth = 0;

			for (KineNode p = n.parent; p != null; p = p.parent)		depth++;
			for (int i = 0; i < depth; i++)		sb.append ("  ");
			sb.append (n.name);
			if (n.joint != null)		sb.append (String.format ("  [%.1f deg]", Math.toDegrees (n.angle)));
			if (n.world != null)
			{
				double[]	o = n.world.origin ();

				sb.append (String.format ("  at (%.3f, %.3f, %.3f)", o[0], o[1], o[2]));
			}
			sb.append ("\n");
		}
		return sb.toString ();
	}

	/**
	 * Whether the robot can be drawn from its parts: a folder given, and in it
	 * the file of every link that names one (a single one missing, and the whole
	 * robot falls back to its solids, so that it is never half one thing and half
	 * the other). False when no link names a part.
	 */
	public boolean partsAvailable (String folder)
	{
		if ((folder == null) || (folder.trim ().length () == 0))		return false;

		java.io.File	dir = new java.io.File (folder.trim ());
		int				n = 0;

		if (!dir.isDirectory ())			return false;
		for (KineNode k : nodes ())
		{
			if ((k.mesh == null) || (k.mesh.trim ().length () == 0))	continue;
			if (!new java.io.File (dir, k.mesh.trim ()).isFile ())		return false;
			n++;
		}
		return n > 0;
	}

	/** The file of the part of a link, in a folder; null when the link names none. */
	static public java.io.File part (String folder, KineNode k)
	{
		if ((folder == null) || (k == null) || (k.mesh == null) || (k.mesh.trim ().length () == 0))		return null;
		return new java.io.File (folder.trim (), k.mesh.trim ());
	}
}
