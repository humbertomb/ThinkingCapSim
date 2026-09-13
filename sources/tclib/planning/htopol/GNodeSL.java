/*
 * (c) 2003 Juan Pedro Canovas Quiñonero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import com.google.gson.JsonObject;

import tclib.utils.graphs.*;

import wucore.utils.geom.*;

/**
 * Node of a second (or deeper) level graph: a labelled place of the world
 * (waypoint, dock, door) inside the zone of its father node. A node may own a
 * graph of its own ({@link #getGraph}), which adds a further level to the
 * hierarchy; the planner only uses the first two levels.
 */
public class GNodeSL extends GNode
{
	protected String				father;
	protected Point3				pos;
	protected int				type;
	protected Graph				sub;				// optional deeper level (null when none)

	public GNodeSL (String label, String father)
	{
		super (label);

		this.father	= father;
		pos			= new Point3 ();
	}

	// Accessors
	public void 		setPosition (Point3 npos)		{ pos.set (npos); }
	public Point3	getPosition ()				{ return pos; }

	public void		setFather (String father)		{ this.father = father; }
	public String	getFather () 				{ return father; }
	public void		setType (int type)			{ this.type = type; }
	public int		getType ()					{ return type; }

	/** The graph of the level below this node, or null when the node has none. */
	public Graph		getGraph ()					{ return sub; }
	public boolean	hasGraph ()					{ return (sub != null) && (sub.numNodes () > 0); }
	/** The graph of the level below, created empty if needed. */
	public Graph		createGraph ()				{ if (sub == null) sub = new Graph (); return sub; }
	public void		setGraph (Graph g)			{ sub = g; }

	/* JSON: {label, arcs: [{to, weight}], graph?: {nodes: [...]}} */

	public JsonObject toJson (Graph owner)
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("label", getLabel ());
		o.add ("arcs", HTopolMap.arcsToJson (this, owner));
		if (hasGraph ())		o.add ("graph", HTopolMap.graphToJson (sub));
		return o;
	}
}
