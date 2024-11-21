/*
 * (c) 2003 Juan Pedro Canovas Qui�onero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import java.util.*;

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
	protected Hashtable<String,Vector<String>>		doors;
	
	// Constructors
	public GNodeFL (String label, int index, Properties props)
	{
		super (label);
		
		fromProps (props, index);

		doors = new Hashtable<String,Vector<String>> ();
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
	
	// Instance methods
	protected void fromProps (Properties props, int index)
	{
		int			i, numnodes;
		GNodeSL		aux[];
		
		sgraph		= new Graph();
		numnodes		= Integer.parseInt (props.getProperty (("SND_"+index+"_NODES"),"0"));
		
		// Parse and create nodes
		aux = new GNodeSL[numnodes];
		for (i=0; i < numnodes; i++)
		{
			String			aprops;
			StringTokenizer	st1;

			aprops	= props.getProperty ("SND_"+index+"_NODE_"+i);	
			st1		= new StringTokenizer (aprops,"\t, ");

			// Create node
			aux[i] = new GNodeSL (st1.nextToken(), getLabel ());
			sgraph.insNode (aux[i]);
		}
		
		// Parse and create output links
		for (i=0; i < numnodes; i++)
		{
			String			aprops;
			StringTokenizer	st1, st2;
			int				dest, weight;

			aprops	= props.getProperty ("SND_"+index+"_NODE_"+i);	
			st1		= new StringTokenizer (aprops,"\t, ");
			st1.nextToken();		// Skip node label

			while (st1.hasMoreTokens ())
			{
				st2		= new StringTokenizer (st1.nextToken(), "/");

				dest		= Integer.parseInt (st2.nextToken());
				weight	= Integer.parseInt (st2.nextToken());
				
				sgraph.join (aux[i], aux[dest], weight);
			}
		}
	}

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
		Vector<String>			vector;
		
		addNode (a);
		
		zone		= a.getLabel ();
		if (!doors.containsKey (zone))
		{		
			vector	= new Vector<String> ();
			vector.add (door);
			doors.put (zone, vector);
		}
		else
		{
			vector	= doors.get (zone);
			if (vector.indexOf (door) == -1)
				vector.addElement (door);
		}
	}
	
	public String getDoor (String zone)
	{
		Vector		vector;

		vector	= doors.get (zone);
		return (String) vector.firstElement ();
	}
	
	public Vector<String> getDoors (String zone)
	{
		return doors.get (zone);
	}
	
	public void printDoors ()
	{
		int				i;
		Enumeration		keys;
		String			zone;
		Vector<String>	vector;
		
		System.out.println ("Door list for zone <"+getLabel ()+">");
		keys		= doors.keys ();
		while (keys.hasMoreElements ())
		{
			zone		= (String) keys.nextElement ();
			vector	= doors.get (zone);
			
			System.out.print ("\tTo zone <"+zone+"> through [");
			for (i = 0; i < vector.size (); i++)
				System.out.print ("<"+vector.elementAt (i)+">");
			System.out.println ("]");
		}
	}
}