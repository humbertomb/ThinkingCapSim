/*
 * (c) 2003 Juan Pedro Canovas Quiñonero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import java.util.*;

import com.google.gson.JsonObject;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;
import tclib.utils.fusion.*;
import tclib.utils.graphs.*;


import tc.shared.world.*;
import tc.vrobot.*;


public class GNodeFL extends GNode
{
	static public final double						IF_CELL		= 0.55;				// Default cell size (m)			
	static public final double						IF_DIL		= 1.75;				// Default dilation constant				

	// Node parameters
	protected double 								size			= IF_CELL;
	protected double 								dilation		= IF_DIL;
	
	// Node data structures
	protected Grid									grid;
	protected GridPath								gpath;
	protected Graph 								sgraph;
	protected boolean								realized		= false;
	
	// Connectivity
	protected Hashtable<String,ArrayList<String>>		doors;
	
	// Constructors
	/** A first level node (a zone of the world) with an empty second level graph. */
	public GNodeFL (String label)
	{
		super (label);

		sgraph	= new Graph ();
		doors	= new Hashtable<String,ArrayList<String>> ();
	}
	
	// Accessors
	public void 		setCellSize (double size)		{ this.size = size; }
	public void 		setDilation (double dil)		{ dilation = dil; }
	
	public Grid		getGrid () 					{ return grid; }	
	public GridPath	getPath () 					{ return gpath; }	
	public Graph		getGraph () 					{ return sgraph; }
	public double	getCellSize () 				{ return size; }
	public double	getDilation () 				{ return dilation; }
	public boolean	isRealized () 				{ return realized; }
	

	public void createMaps (World world, FusionDesc fdesc, RobotDesc rdesc)
	{
		int			w, h;	
		int			dil;
		WMZone		zone;

		// Initialise size dependent variables
		zone		= world.zones ().at (getLabel ());
		w		= (int) Math.round (zone.width () / size) + 4;
		h		= (int) Math.round (zone.height () / size) + 4;
		dil		= (int) (Math.round (rdesc.RADIUS * (dilation / size)));

		// Create grid map for the current zone
//		grid 	= new DFGrid (fdesc, rdesc, w, h, size);		
		grid 	= new FGrid (fdesc, rdesc, w, h, size);		
		grid.setRangeLRF (30.0);
		grid.setMode (FGrid.SAFE_MOTION);
		grid.setOffsets (zone.minx () - size, zone.miny () - size);	
		grid.fromWorld (world);
		
		// Create path planner for the current zone
		gpath	= new FGridPathD (grid);
		((FGridPathD) gpath).method (GridPathD.D_STAR_MI);		// D* focused minimum init
		gpath.setDilation (dil);
		gpath.setTimeStep (30);
		gpath.setExtension (5.0);
		gpath.setHeuristic (GridPath.FOLLOW_GN);
		
		// Attach map features to second level nodes
		int			i;
		String		label;
		GNodeSL		node;
		
		for (i = 0; i < sgraph.numNodes (); i++)
		{
			node		= (GNodeSL) sgraph.getNode (i);
			label	= node.getLabel ();
			node.setPosition (world.getPos (label, getLabel ()));			
			node.setType (world.getType (label));
		}
		
		realized = true;
	}
	
	public void addNode (GNode a, String door)
	{
		String			zone;
		ArrayList<String>			vector;
		
		addNode (a);
		
		zone		= a.getLabel ();
		if (!doors.containsKey (zone))
		{		
			vector	= new ArrayList<String> ();
			vector.add (door);
			doors.put (zone, vector);
		}
		else
		{
			vector	= doors.get (zone);
			if (vector.indexOf (door) == -1)
				vector.add (door);
		}
	}
	
	public String getDoor (String zone)
	{
		ArrayList<String>	vector;

		vector	= doors.get (zone);
		return ((vector == null) || vector.isEmpty ()) ? null : vector.get (0);
	}

	/** Makes the given door the (only) connection to a zone. */
	public void setDoor (String zone, String door)
	{
		ArrayList<String>	vector = new ArrayList<String> ();
		if (door != null)		vector.add (door);
		doors.put (zone, vector);
	}

	/** Forgets the doors to a zone (when the arc to it is removed). */
	public void removeDoors (String zone)		{ doors.remove (zone); }

	/** Forgets the doors to a zone (when the arc to it is removed). */
	public void renameZone (String from, String to)
	{
		ArrayList<String>	vector = doors.remove (from);
		if (vector != null)		doors.put (to, vector);
	}

	/* JSON: {label, cellSize, dilation, arcs: [{to, door}], graph: {nodes: [...]}} */

	public JsonObject toJson (Graph owner)
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("label", getLabel ());
		o.addProperty ("cellSize", size);
		o.addProperty ("dilation", dilation);
		com.google.gson.JsonArray	arcs = new com.google.gson.JsonArray ();
		for (int i = 0; i < nList (); i++)
		{
			GNode		dest = owner.getNode (getList (i));
			JsonObject	a = new JsonObject ();
			a.addProperty ("to", dest.getLabel ());
			String		door = getDoor (dest.getLabel ());
			if (door != null)		a.addProperty ("door", door);
			arcs.add (a);
		}
		o.add ("arcs", arcs);
		o.add ("graph", HTopolMap.graphToJson (sgraph));
		return o;
	}
	
	public ArrayList<String> getDoors (String zone)
	{
		return doors.get (zone);
	}
	
	public void printDoors ()
	{
		int				i;
		Enumeration<String>	keys;
		String			zone;
		ArrayList<String>	vector;
		
		System.out.println ("Door list for zone <"+getLabel ()+">");
		keys		= doors.keys ();
		while (keys.hasMoreElements ())
		{
			zone		= keys.nextElement ();
			vector	= doors.get (zone);
			
			System.out.print ("\tTo zone <"+zone+"> through [");
			for (i = 0; i < vector.size (); i++)
				System.out.print ("<"+vector.get (i)+">");
			System.out.println ("]");
		}
	}
}