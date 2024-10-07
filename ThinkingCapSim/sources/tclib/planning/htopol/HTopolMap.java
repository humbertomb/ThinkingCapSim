/*
 * (c) 2003 Juan Pedro Canovas Qui–onero
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol;

import java.util.*;
import java.io.*;

import tclib.utils.fusion.*;
import tclib.utils.graphs.*;

import tc.shared.world.*;
import tc.vrobot.*;

import wucore.utils.geom.*;

public class HTopolMap extends Graph
{
	protected World 				world;	
	
	public HTopolMap (World world, String name)
	{
		super ();
		
		this.world	= world;
		
		fromFile (name);
	}
	
	public HTopolMap (World world, Properties props)
	{
		super ();
		
		this.world	= world;
				
		fromProps (props);
	}
	
	// Implemented to override fromFile from class Graph
	protected void fromFile (String name)
	{
		try
		{
			FileInputStream finput = new FileInputStream (name);
			Properties descprop = new Properties ();
			descprop.load (finput);			
			fromProps (descprop);
		}
		catch (Exception e) 
		{ 
			System.out.println ("--[Topol] Exception loading graph:");
			e.printStackTrace ();	
		}	
	}
	
	protected void fromProps (Properties props)
	{
		int 				i, num;
		GNodeFL			aux[];
		String 			prop;
		StringTokenizer	st;
		
		num = Integer.parseInt (props.getProperty ("FSTNODES","0"));
		aux = new GNodeFL[num];
		
		// Read in FirstLevel nodes
		for (i = 0; i < num; i++)
		{
			String		label;
			
			prop		= props.getProperty (("FSTNODE_"+i),"");
			st 		= new StringTokenizer (prop,"\t, ");
			label	= st.nextToken();

			aux[i]	= new GNodeFL (label, i, props);
			aux[i].setCellSize (Double.parseDouble (st.nextToken()));
			aux[i].setDilation (Double.parseDouble (st.nextToken()));
			
			insNode (aux[i]);
		}
		
		// Read in arcs and insert nodes in graph
		// This MUST be done after all nodes have been created
		int 		orig, dest;
		for (i = 0; i < num; i++)
		{
			prop		= props.getProperty (("FSTARCS"),"");	
			st		= new StringTokenizer (prop,"\t,-/ ");
			while (st.hasMoreTokens ())
			{
				orig = Integer.parseInt (st.nextToken()); 			// Origin of arc
				dest = Integer.parseInt (st.nextToken()); 			// Destination of arc
				join (aux[orig], aux[dest], st.nextToken());		// Connection through door label
			}
		}
	}
	
	public void join (GNodeFL a, GNodeFL b, String door)
	{
		insNode (a);
		insNode (b);
		a.addNode (b, door);
	}
	
	public void createMaps (FusionDesc fdesc, RobotDesc rdesc)
	{
		int			i;

		for (i = 0; i < numNodes (); i++)
			((GNodeFL) getNode (i)).createMaps (world, fdesc, rdesc);
	}
						
	public Vector getInNodesSL (String label)
	{
		int			i, j;
		String		zone;
		GNodeFL		fnode;
		GNodeSL		snode, pnode;
		Graph		graph;
		Vector		in;
		
		zone		= world.zones ().inZone (world.getPos (label));
		fnode	= (GNodeFL) getNode (zone);
		graph	= fnode.getGraph ();
		in		= new Vector ();
		
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
	
	public Vector getOutNodesSL (String label)
	{
		int			i, j;
		String		zone;
		GNodeFL		fnode;
		GNodeSL		snode;
		Graph		graph;
		Vector		out;
		
		zone		= world.zones ().inZone (world.getPos (label));
		fnode	= (GNodeFL) getNode (zone);
		graph	= fnode.getGraph ();
		out		= new Vector ();
		
		for (i = 0; i < graph.numNodes (); i++)
		{
			snode	= (GNodeSL) graph.getNode (i);
			if (label.equals (snode.getLabel ()))
				for (j = 0; j < snode.nList (); j++)				
					out.add ((GNodeSL) graph.getNode (snode.getList (j)));
		}
	
		return out;
	}
	
	public Vector calcPath (Point2 orig, String destlabel)
	{
		String zo, zd;
		int	i,ind1, ind2;
		GNodeFL fst_node1;
		Graph sndgraph;
		Vector fst_path;
		Vector doors;
		Vector ret_path;
	
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
		
		doors = new Vector ();
		for (i=0; (i+1) < fst_path.size(); i++)
		{
			fst_node1 = (GNodeFL)fst_path.get (i);
			doors.add (fst_node1.getDoor (((GNode) fst_path.get (i+1)).getLabel ()));
		}
		
		if (debug)
			for (i=0; i < doors.size (); i++)
				System.out.println (((GNodeFL)fst_path.get (i)).getLabel()+" to "+
									((GNodeFL)fst_path.get (i+1)).getLabel()+" through "+(String)doors.get(i));
		
		ret_path = new Vector ();
		
		
		i = 0;
		fst_node1 = (GNodeFL)fst_path.get (0);
		while (!fst_node1.getLabel ().equals (zd))
		{
		
			ret_path.add (fst_node1.getGraph ().getNode ((String)doors.get(i)));
			i++;
			fst_node1 = (GNodeFL)fst_path.get (i);			
		}
		
		// Calculate path from last door to dest node and add it to final path
		
		//Is it necessary to find last node????? ***NO***
		//String destlabel = worldmap.wpat_label (worldmap.wp_nearest (dest)); //We only search in wait points
		if (destlabel != null && doors.size () > 0)
		{
			sndgraph = fst_node1.getGraph ();
			ind1 = sndgraph.indNode ((String)doors.lastElement());
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