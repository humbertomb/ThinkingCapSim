/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.oru.rasmus;

import java.util.*;

import tc.shared.linda.*;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

import tcrob.umu.indoor.IndoorNavigation;

public class RasmusModel extends IndoorNavigation
{	
	static public final double		IF_CELL			= 0.25;				// Cell size (m)
	static public final double		IF_DIL			= 1.75;				// Default dilation constant				

	static public final double		LRF_BUF_DIST	= 15.0;				// Maximum laser distance for binary grid map	
	static public final double		SON_BUF_DIST	= 1.5;				// Maximum sonar distance for grid maps
	
	// Constructors
	public RasmusModel (Properties props, Linda linda)
	{
		super (props, linda);
	}
		
	// Instance methods
	public synchronized void notify_config (String space, ItemConfig item)
	{
		int				w = 200, h = 200;	
		int				dil;
		
		// Initialise other local stuff
		allow_fmap	= true;
		cell_size	= IF_CELL;
		dilation	= IF_DIL / cell_size;

		super.notify_config (space, item);

		// Initialise size dependent variables
		if (world != null)
		{
			w = (int) Math.round ((world.walls ().maxx () - world.walls ().minx ()) / cell_size) + 4;
			h = (int) Math.round ((world.walls ().maxy () - world.walls ().miny ()) / cell_size) + 4;
		}
		dil = (int) (Math.round (rdesc.RADIUS * dilation));

		// Modify inherited structures' behaviours
		grid 	= new FGrid (fdesc, rdesc, w, h, cell_size);		
		grid.setRange (SON_BUF_DIST, LRF_BUF_DIST);
		grid.setMode (FGrid.SAFE_MOTION);
		if (world != null)
		{
			grid.setOffsets (world.walls ().minx () - cell_size, world.walls ().miny () - cell_size);	
			grid.fromWorld (world);
		}
		
		// Change path planner
		gpath	= new FGridPathD (grid);
		((FGridPathD) gpath).method (GridPathD.D_STAR_MI);		// D* focused minimum init
		gpath.setDilation (dil);
		gpath.setTimeStep (30);
	}
}

