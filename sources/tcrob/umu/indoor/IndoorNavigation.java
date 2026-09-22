/*
 * (c) 2001 Humberto Martinez
 */
 
package tcrob.umu.indoor;

import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.*;
import tc.shared.lps.lpo.*;
import tc.modules.*;

import tclib.navigation.localisation.*;
import tclib.navigation.mapbuilding.*;
import tclib.navigation.mapbuilding.gui.*;
import tclib.navigation.mapbuilding.lpo.*;
import tclib.navigation.pathplanning.*;


import devices.pos.*;

public class IndoorNavigation extends Navigation
{
	static public final int			MAX_PATH		= 15000;				
	static public final double		DEF_CELL		= 0.075;	// Default cell size (m)			
	static public final int			MAX_CELLS		= 2000000;	// Largest grid built from a world (cells); the cell grows beyond DEF_CELL to keep it
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
	public IndoorNavigation (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
		
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		// Initialise Linda related structures
		nitem		= new ItemNavigation ();
		ntuple		= new Tuple (Tuple.NAVIGATION, nitem);
	
		pitem		= new ItemPath ();
		ptuple		= new Tuple (Tuple.PATH, pitem);
	
		// Initialise other local stuff
		pos			= new Position ();
		dpos		= new Position ();
		
		cell_size	= DEF_CELL;
		String		cprop = cfg.get ("CELL");			// module property CELL (m) overrides the default
		if (cprop != null)
			try { cell_size = Double.parseDouble (cprop.trim ()); } catch (Exception e) { System.out.println ("--[Nav] Invalid CELL <" + cprop + ">, using " + cell_size); }
		dilation	= DEF_DIL / cell_size;
	}
	
	protected void close_gfx ()
	{
		if (gwin != null)		gwin.close ();
		if (fwin != null)		fwin.close ();
		gwin		= null;
		fwin		= null;
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
			double[]	b = world.bounds ();
			// the cell is enlarged for worlds too big to fit MAX_CELLS at the configured size (a
			// 250 x 250 m map at 7.5 cm would need 11 M cells and exhausts the heap)
			double		area = (b[2] - b[0]) * (b[3] - b[1]);
			if (area / (cell_size * cell_size) > MAX_CELLS)
			{
				cell_size = Math.sqrt (area / MAX_CELLS);
				System.out.println ("  [Nav] World too large for the default grid cell: using " + String.format (java.util.Locale.US, "%.3f", cell_size) + " m cells");
			}
			dilation	= DEF_DIL / cell_size;
			w = (int) Math.ceil ((b[2] - b[0]) / cell_size);
			h = (int) Math.ceil ((b[3] - b[1]) / cell_size);
		}
		dil = (int) (Math.round (rdesc.RADIUS * dilation));
				
		// Create data structures
		grid = new FGrid (fdesc, rdesc, w, h, cell_size);		
		grid.setRangeSON (1.5);
		grid.setRangeLRF (15.0);
		if (world != null)
		{
			System.out.println ("  [Nav] Loading world into grid map");
			double[]	b = world.bounds ();
			grid.setOffsets (b[0], b[1]);	
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


