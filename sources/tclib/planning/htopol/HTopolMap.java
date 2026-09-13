/*
 * (c) 2003 Juan Pedro Canovas Quiñonero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tclib.utils.fusion.*;
import tclib.utils.graphs.*;

import tc.shared.world.*;
import tc.vrobot.*;

import wucore.utils.geom.*;

/**
 * Hierarchical topological map of a world: the first level nodes are the
 * zones ({@link GNodeFL}, connected through doors), each one owning a graph
 * of the places inside it ({@link GNodeSL}: waypoints, docks and doors),
 * which may in turn own deeper graphs. It is persisted as part of the world
 * (JSON key "topology").
 */
public class HTopolMap extends Graph
{
	protected World 				world;	
	
	/** An empty map of a world. */
	public HTopolMap (World world)
	{
		super ();
		
		this.world	= world;
	}
	
	public HTopolMap (World world, JsonObject json)
	{
		super ();
		
		this.world	= world;
		fromJson (json);
	}
	
	public void join (GNodeFL a, GNodeFL b, String door)
	{
		insNode (a);
		insNode (b);
		a.addNode (b, door);
	}

	public World		getWorld ()					{ return world; }
	public void		setWorld (World world)		{ this.world = world; }

	/** Removes a first level node and the doors the others kept to it. */
	public GNode removeNode (int index)
	{
		GNode	removed = super.removeNode (index);
		if (removed != null)
			for (int i = 0; i < numNodes (); i++)
				((GNodeFL) getNode (i)).removeDoors (removed.getLabel ());
		return removed;
	}

	/* ---- JSON ---- */

	/** {nodes: [{label, cellSize, dilation, arcs: [{to, door}], graph: {...}}]} */
	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonArray	nodes = new JsonArray ();
		for (int i = 0; i < numNodes (); i++)		nodes.add (((GNodeFL) getNode (i)).toJson (this));
		o.add ("nodes", nodes);
		return o;
	}

	public void fromJson (JsonObject o)
	{
		JsonArray	nodes = arrayOf (o, "nodes");
		GNodeFL[]	aux = new GNodeFL[nodes.size ()];

		// nodes first, arcs afterwards (they refer to nodes by label)
		for (int i = 0; i < aux.length; i++)
		{
			JsonObject	n = nodes.get (i).getAsJsonObject ();
			aux[i]	= new GNodeFL (stringOf (n, "label", "Zone" + i));
			aux[i].setCellSize (doubleOf (n, "cellSize", GNodeFL.IF_CELL));
			aux[i].setDilation (doubleOf (n, "dilation", GNodeFL.IF_DIL));
			insNode (aux[i]);
			graphFromJson (aux[i].getGraph (), objectOf (n, "graph"), aux[i].getLabel ());
		}
		for (int i = 0; i < aux.length; i++)
		{
			JsonObject	n = nodes.get (i).getAsJsonObject ();
			for (JsonElement e : arrayOf (n, "arcs"))
			{
				JsonObject	a = e.getAsJsonObject ();
				GNode		dest = getNode (stringOf (a, "to", ""));
				if (dest == null)		continue;
				aux[i].addNode (dest, 1);
				String		door = stringOf (a, "door", null);
				if (door != null)		aux[i].setDoor (dest.getLabel (), door);
			}
		}
	}

	/** Graph of {@link GNodeSL} nodes: {nodes: [{label, arcs: [{to, weight}], graph?}]} */
	static public JsonObject graphToJson (Graph g)
	{
		JsonObject	o = new JsonObject ();
		JsonArray	nodes = new JsonArray ();
		if (g != null)
			for (int i = 0; i < g.numNodes (); i++)		nodes.add (((GNodeSL) g.getNode (i)).toJson (g));
		o.add ("nodes", nodes);
		return o;
	}

	/** Fills a graph with the nodes and arcs of its JSON form (recursively). */
	static public void graphFromJson (Graph g, JsonObject o, String father)
	{
		JsonArray	nodes = arrayOf (o, "nodes");
		GNodeSL[]	aux = new GNodeSL[nodes.size ()];
		for (int i = 0; i < aux.length; i++)
		{
			JsonObject	n = nodes.get (i).getAsJsonObject ();
			aux[i]	= new GNodeSL (stringOf (n, "label", "node" + i), father);
			g.insNode (aux[i]);
			if (n.has ("graph"))		graphFromJson (aux[i].createGraph (), objectOf (n, "graph"), aux[i].getLabel ());
		}
		for (int i = 0; i < aux.length; i++)
		{
			JsonObject	n = nodes.get (i).getAsJsonObject ();
			for (JsonElement e : arrayOf (n, "arcs"))
			{
				JsonObject	a = e.getAsJsonObject ();
				GNode		dest = g.getNode (stringOf (a, "to", ""));
				if (dest != null)		aux[i].addNode (dest, (int) doubleOf (a, "weight", 0));
			}
		}
	}

	/** Arcs of a node as [{to, weight}]. */
	static JsonArray arcsToJson (GNode node, Graph owner)
	{
		JsonArray	arcs = new JsonArray ();
		for (int i = 0; i < node.nList (); i++)
		{
			JsonObject	a = new JsonObject ();
			a.addProperty ("to", owner.getNode (node.getList (i)).getLabel ());
			a.addProperty ("weight", node.getPeso (i));
			arcs.add (a);
		}
		return arcs;
	}

	static private JsonArray arrayOf (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
	}

	static private JsonObject objectOf (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
	}

	static private String stringOf (JsonObject o, String key, String def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsString () : def;
	}

	static private double doubleOf (JsonObject o, String key, double def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsDouble () : def;
	}
	
	public void createMaps (FusionDesc fdesc, RobotDesc rdesc)
	{
		int			i;

		for (i = 0; i < numNodes (); i++)
			((GNodeFL) getNode (i)).createMaps (world, fdesc, rdesc);
	}
						
	public ArrayList<GNodeSL> getInNodesSL (String label)
	{
		int			i, j;
		String		zone;
		GNodeFL		fnode;
		GNodeSL		snode, pnode;
		Graph		graph;
		ArrayList<GNodeSL>	in;
		
		zone		= world.zones ().inZone (world.getPos (label));
		fnode	= (GNodeFL) getNode (zone);
		graph	= fnode.getGraph ();
		in		= new ArrayList<GNodeSL> ();
		
		for (i = 0; i < graph.numNodes (); i++)
		{
			snode	= (GNodeSL) graph.getNode (i);
			for (j = 0; j < snode.nList (); j++)		
			{
				pnode	= (GNodeSL) graph.getNode (snode.getList (j));
				if (label.equals (pnode.getLabel ()))
					in.add (snode);
			}
		}
	
		return in;
	}
	
	public ArrayList<GNodeSL> getOutNodesSL (String label)
	{
		int			i, j;
		String		zone;
		GNodeFL		fnode;
		GNodeSL		snode;
		Graph		graph;
		ArrayList<GNodeSL>	out;
		
		zone		= world.zones ().inZone (world.getPos (label));
		fnode	= (GNodeFL) getNode (zone);
		graph	= fnode.getGraph ();
		out		= new ArrayList<GNodeSL> ();
		
		for (i = 0; i < graph.numNodes (); i++)
		{
			snode	= (GNodeSL) graph.getNode (i);
			if (label.equals (snode.getLabel ()))
				for (j = 0; j < snode.nList (); j++)				
					out.add ((GNodeSL) graph.getNode (snode.getList (j)));
		}
	
		return out;
	}
	
	public ArrayList<GNode> calcPath (Point2 orig, String destlabel)
	{
		String zo, zd;
		int	i,ind1, ind2;
		GNodeFL fst_node1;
		Graph sndgraph;
		ArrayList<GNode> fst_path;
		ArrayList<String> doors;
		ArrayList<GNode> ret_path;
	
		zo = world.zones ().inZone (orig);		
		zd = world.zones ().inZone (world.getPos (destlabel));
		
		if (zo.equals ("Unknown"))
		{
			System.out.println ("--[Topol] calc_path: point "+orig+" isn't in world map");
			return null;
		}
		else if (zd.equals ("Unknown"))
		{
			System.out.println ("--[Topol] calc_path: point "+destlabel+" isn't in world map");
			return null;
		}	
			
		//System.out.println ("TopolMap.calc_path: from "+zo+" to "+zd);
		
		fst_path = calcPath (indNode (zo),indNode (zd));				
		
		doors = new ArrayList<String> ();
		for (i=0; (i+1) < fst_path.size(); i++)
		{
			fst_node1 = (GNodeFL)fst_path.get (i);
			doors.add (fst_node1.getDoor (fst_path.get (i+1).getLabel ()));
		}
		
		if (debug)
			for (i=0; i < doors.size (); i++)
				System.out.println (((GNodeFL)fst_path.get (i)).getLabel()+" to "+
									((GNodeFL)fst_path.get (i+1)).getLabel()+" through "+doors.get(i));
		
		ret_path = new ArrayList<GNode> ();
		
		
		i = 0;
		fst_node1 = (GNodeFL)fst_path.get (0);
		while (!fst_node1.getLabel ().equals (zd))
		{
		
			ret_path.add (fst_node1.getGraph ().getNode (doors.get(i)));
			i++;
			fst_node1 = (GNodeFL)fst_path.get (i);			
		}
		
		// Calculate path from last door to dest node and add it to final path
		
		//Is it necessary to find last node????? ***NO***
		//String destlabel = worldmap.wpat_label (worldmap.wp_nearest (dest)); //We only search in wait points
		if (destlabel != null && doors.size () > 0)
		{
			sndgraph = fst_node1.getGraph ();
			ind1 = sndgraph.indNode (doors.get (doors.size () - 1));
			ind2 = sndgraph.indNode(destlabel);
			ret_path.addAll (sndgraph.calcPath (ind1,ind2));
		}
		else if (destlabel != null)
		{
			sndgraph = fst_node1.getGraph ();
			ret_path.add (sndgraph.getNode(destlabel)); // orig and dest in the same room
		}
		
		return ret_path;	
	}	
}