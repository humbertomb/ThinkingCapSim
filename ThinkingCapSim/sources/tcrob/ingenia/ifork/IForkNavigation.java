/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.ingenia.ifork;

import java.util.*;


import tc.shared.linda.*;

import tclib.navigation.mapbuilding.Grid;
import tclib.planning.htopol.*;
import tclib.planning.htopol.gui.*;

import tcrob.ingenia.ifork.linda.*;
import tcrob.umu.indoor.IndoorNavigation;
import devices.pos.*;
import wucore.utils.geom.Point2;

public class IForkNavigation extends IndoorNavigation
{	
	protected String				my_id;
	protected Hashtable				agvinfo;

	protected ItemIForkZone			zitem;	
	protected Tuple					ztuple;

	protected HTopolMap				topol;							// Hierarchical Topological Map
	protected HTopolWindow			win;
	protected String				czone;							// Current working zone
	protected String				lzone;							// Previous working zone
	
	protected Position				g_pos;							// Current goal position
	protected int					g_mode;
	protected int					g_src;
	protected Point2				iconMax;
	protected Point2				iconMin;
	protected Hashtable				agv_runtime;

	// Constructors
	public IForkNavigation (Properties props, Linda linda)
	{
		super (props, linda);
		
		my_id 		= props.getProperty ("ROBNAME");
		agvinfo		= new Hashtable ();
		g_pos		= new Position ();
		
		// Linda data
		zitem 	= new ItemIForkZone ();
		ztuple 	= new Tuple (IForkTuple.ZONE, zitem); 
		agv_runtime  = new Hashtable ();
	}
		
	// Instance methods
	protected void navigation ()
	{
		if (!czone.equals (lzone))
		{
			GNodeFL		node;
			
			node		= (GNodeFL) topol.getNode (czone);
			if(node==null){
				System.out.println("czone="+czone+" lzone="+lzone);
				topol.print();
			}
			else{
				grid	= node.getGrid ();
				gpath	= node.getPath ();
				gpath.reset (pos);
				gpath.goal (g_pos);
				gpath.curve (g_mode, g_src);
			
				// Propagate zone change to other modules
				changeZone (czone);
				if (win != null) 	win.changeZone (czone);
			}
		}
		
		super.navigation ();
				
		lzone	= czone;
		czone	= world.zones ().inZone (pos.x (), pos.y ());
		if(czone.equals("Unknown"))
			System.out.println("Zona desconocida pos="+pos);
		if (win != null) 	win.updateGrid (pos);
	}
	
	protected void changeZone (String zone)
	{
		zitem.set (zone);
		linda.write (ztuple);
	}
	
	public void notify_goal (String space, ItemGoal item)
	{		
		super.notify_goal (space, item);
		
		g_pos.set (item.task.tpos);
		g_mode	= item.task.path_mode;
		g_src	= item.task.path_src;
	}
		
	public synchronized void notify_config (String space, ItemConfig item)
	{
		GNodeFL			node;
		double[]		limits;
		
		// Initialise other local stuff
		allow_fmap	= false;
		cell_size	= 0.5;
		dilation		= DEF_DIL / cell_size;

		super.notify_config (space, item);

		if ((world == null) || (item.props_topol  == null))
		{
			System.err.println ("--[iFrkNav] World and topological maps MUST be specified. Aborting module.");
			
			initialised = false;
			return;
		}
		
		// Get robot dimension
		limits = rdesc.getIconLimits();
		iconMin = new Point2(limits[0], limits[1]);
		iconMax = new Point2(limits[2], limits[3]);
		
		// Initialise hierarchical topological map
		topol	= new HTopolMap (world, item.props_topol);
		topol.createMaps (fdesc, rdesc);
		
		// Initialise zone state
		node		= (GNodeFL) topol.getNode (0);
		grid		= node.getGrid ();
		gpath	= node.getPath ();
		czone	= node.getLabel ();
		lzone	= "null";
		
		if (localgfx) 
			win		= new HTopolWindow (space, topol, rdesc);
	}

	public synchronized void notify_coord (String space, ItemCoordination coord)
	{
		if(agv_runtime.contains(space)){
			if((System.currentTimeMillis() - ((Long)agv_runtime.get(space)).longValue())<20000){
				//System.out.println("  [IForkNavigation]: notify_coord Recibido coord de "+space+" estando borrado.");
				return;
			}else{
				//System.out.println("  [IForkNavigation]: notify_coord "+space+" esta eliminado mas de 20 seg. Borrar de agv_runtime");
				agv_runtime.remove(space);
			}
		}
		if (my_id.equalsIgnoreCase(space) || space.equalsIgnoreCase(LindaEntryFilter.ANY))				return;
				
		if (grid != null)
		{
			if(iconMin != null && iconMax != null)
			    grid.robotOccupied (coord.position, iconMax, iconMin, space);
		}
		agvinfo.put (space, coord.position);	
	}
	
	public synchronized void notify_delrobot(String space,ItemDelRobot item){
		
		if(item.cmd==ItemDelRobot.INFO){
//			System.out.println("  [IForkNavigation] Recibido tuple INFO "+space+" item="+item);
		}
		else if(item.cmd==ItemDelRobot.DELETE){
			//System.out.println("  [IForkNavigation] Recibido tuple delrobot "+item+" space="+space+" my_id="+my_id);
			
			agv_runtime.put(item.robotid,new Long(System.currentTimeMillis()));
			
			if(my_id.equalsIgnoreCase(item.robotid)){
				System.out.println("  [IForkNavigation] Stop robot "+my_id);
				stop();
				return;
			}
			if(agvinfo!=null && agvinfo.contains(item.robotid))
				agvinfo.remove(item.robotid);
			
			if(topol != null){
				GNodeFL node;
				Grid zonegrid;
				// Se elimina el robot de todos los grids
				for(int i = 0; i<topol.numNodes(); i++){
					node = (GNodeFL)topol.getNode(i);
					if(node != null){
						System.out.println("  [IForkNavigation] Robot "+my_id+": eliminando "+item.robotid+" del Grid "+node.getLabel());
						zonegrid = node.getGrid();
						if(zonegrid != null)
							zonegrid.restartChanges(item.robotid);
					}
				}
			}
			
		}
	}

}

