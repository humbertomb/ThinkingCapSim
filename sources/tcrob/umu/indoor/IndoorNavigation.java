/*
 * (c) 2001 Humberto Martinez
 */
 
package tcrob.umu.indoor;

import java.util.*;
import tc.shared.linda.*;
import tc.shared.lps.lpo.*;
import tc.modules.*;

import tclib.navigation.localisation.*;
import tclib.navigation.mapbuilding.*;
import tclib.navigation.mapbuilding.gui.*;
import tclib.navigation.mapbuilding.lpo.*;
import tclib.navigation.pathplanning.*;

import tcrob.umu.indoor.linda.*;

import devices.pos.*;

public class IndoorNavigation extends Navigation
{
	static public final int			MAX_PATH		= 15000;				
	static public final double		DEF_CELL		= 0.075;	// Default cell size (m)			
	static public final double		DEF_DIL			= 0.75;		// Default dilation constant				

	// Navigation structures
	protected Grid					grid;
	protected GridPath				gpath;
	protected FSegMap				fmap;
	protected KFSegLoc				kloc;
	
	protected Position				pos;
	protected Position				dpos;
	protected Paths					paths;
	
	// Linda data structures
	protected Tuple					ntuple;
	protected ItemNavigation		nitem;

	protected Tuple					ptuple;
	protected ItemPath				pitem;

	protected Tuple					gmtuple;
	protected ItemGridMap			gmitem;

	protected Tuple					fmtuple;
	protected ItemFSegMap			fmitem;

	// Additional local variables
	protected boolean				initialised		= false;
	protected double				cell_size;
	protected double				dilation;
	
	// Algorithm execution control
	protected boolean				allow_fmap		= true;
	protected boolean				allow_gmap		= true;
	protected boolean				allow_path		= true;

	protected GridWindow			gwin;
	protected FSegWindow			fwin;

	// Constructors
	public IndoorNavigation (Properties props, Linda linda)
	{
		super (props, linda);
	}
		
	// Instance methods
	protected void initialise (Properties props)
	{		
		// Initialise Linda related structures
		nitem		= new ItemNavigation ();
		ntuple		= new Tuple (Tuple.NAVIGATION, nitem);
	
		pitem		= new ItemPath ();
		ptuple		= new Tuple (Tuple.PATH, pitem);
	
		gmitem		= new ItemGridMap ();
		gmtuple		= new Tuple ("GRIDMAP", gmitem);
	
		fmitem		= new ItemFSegMap ();
		fmtuple		= new Tuple ("FSEGMAP", fmitem);
	
		// Initialise other local stuff
		pos			= new Position ();
		dpos		= new Position ();
		
		cell_size	= DEF_CELL;
		dilation	= DEF_DIL / cell_size;
	}
	
	public void step (long ctime)
	{
		if (!initialised)		return;

		// Perform navigation calcs
		navigation ();
	    
		// Write the path to the Linda space
		if (gpath.newPath ())
		{
			pitem.set (gpath.path (), time_upd);
			linda.write (ptuple);
		}
	}
	
	protected void navigation ()
	{
		int					i;
		long				st;
		LPOSensorRange		virtual;
		LPOSensorScanner	scan;
		LPOSensorFSeg		lsegs;

		if (gpath != null) 
		    gpath.debug = debug;
		if ((pos == null) || (lps == null))		return;

		// Read sensor data from LPS
		virtual	= (LPOSensorRange) lps.find ("Virtual");
		scan	= (LPOSensorScanner) lps.find ("Scanner");
		lsegs	= (LPOSensorFSeg) lps.find ("FSegs");

		// WARNING: this localization schema lacks the ability of effectively using
		// the current segments to update the local or the global map (TODO list)
		//		NOTE: The kalman lozalisation and the fuzzy map update are
		//				decouple here, something which should not be. The idea is
		//				to combine steps A and B in a single process.
		
		// Perform Kalman based localization (step A)
		pos.set (lps.cur);
		if (allow_fmap)
		{
			// Write robot position to the Linda space
			nitem.set (pos, time_upd);
			linda.write (ntuple);
		}

/*
		if (allow_fmap)
		{
			dpos.set (pos);
	//		kloc.debug (debug);
			kloc.do_localisation (pos, time_upd, fmap, virtual);
			pos.set (kloc.current ());
			dpos.delta (pos);
			
			// Write robot position to the Linda space
			nitem.set (pos, time_upd);
			linda.write (ntuple);
		}
*/
		
		// Update the Fuzzy Segments map (step B)
		if (allow_fmap && (lsegs != null) && (lsegs.active ()))
		{
			fmap.update_inview (lsegs.segs (), pos);
			fmap.update_maps (pos);
		}

		// Update the path (odometry plus corrected)
		if (paths != null)
		{	
			paths.odometry ().add (lps.odom);
			paths.corrected ().add (pos);
			paths.correction (dpos);
		}

		st	= System.currentTimeMillis ();

		// Grid map related operations
		if (allow_gmap)
		{
			// Update grid map (sonar + irs)
			for (i = 0; i < fdesc.MAXVIRTU; i++)
				if (virtual.valid[i])
					grid.update (pos, i, virtual.range[i]);

			// Update grid map (laser)
			if (scan.active ())
				grid.update (pos, scan);

			//grid.location (pos);
			grid.add_time ((double) (System.currentTimeMillis () - st));
		}

	    // Update path planner
	    if (allow_path)
	    {
		    if (debug)			System.out.println ("  [Nav] Replanning ... ");

		    gpath.replan (pos);

//			gpath.goal (goal);
//			gpath.curve (GridPath.POLYLINE, GridPath.GRID);
//			gpath.replan (robot);
//			while (!gpath.newPath ())
//				gpath.replan (robot);
//			path = gpath.path ();

			if (debug)			System.out.println ("  [Nav] End Replanning. newpath=" + gpath.newPath ());
		}

		// Update the Linda Space
		gmitem.set (grid, gpath, pos, System.currentTimeMillis ());
		linda.write (gmtuple);

		fmitem.set (fmap, paths, pos, System.currentTimeMillis ());
		linda.write (fmtuple);
		
		
		if (gwin != null) 	gwin.updateGrid (gpath, pos);
		if (fwin != null) 	fwin.updateMap (paths, pos);
	}

	public void notify_goal (String space, ItemGoal item)
	{		
		gpath.goal (item.task.tpos);
		gpath.curve (item.task.path_mode, item.task.path_src);
	}
		
	public void notify_config (String space, ItemConfig item)
	{
		int			w = 200, h = 200;	
		int			dil;	
		
		super.notify_config (space, item);

		// Initialize size-dependent variables
		if (world != null)
		{
			w = (int) Math.ceil ((world.walls ().maxx () - world.walls ().minx ()) / cell_size);
			h = (int) Math.ceil ((world.walls ().maxy () - world.walls ().miny ()) / cell_size);
		}
		dil = (int) (Math.round (rdesc.RADIUS * dilation));
				
		// Create data structures
		grid = new FGrid (fdesc, rdesc, w, h, cell_size);		
		grid.setRangeSON (1.5);
		grid.setRangeLRF (15.0);
		if (world != null)
		{
			System.out.println ("  [Nav] Loading world into grid map");
			grid.setOffsets (world.walls ().minx (), world.walls ().miny ());	
//			grid.fromWorld (world);
		}
		
		gpath = new FGridPathA (grid);
		gpath.setDilation (dil);
		
		fmap	= new FSegMap ();
		kloc	= new KFSegLoc (fdesc);
		paths	= new Paths (MAX_PATH);

		time_upd	= 0;
		initialised = true;
		
		if (localgfx) 
		{
			gwin = new GridWindow (space, grid, rdesc);
			fwin = new FSegWindow (space, fmap, rdesc);
		}
	}
}


