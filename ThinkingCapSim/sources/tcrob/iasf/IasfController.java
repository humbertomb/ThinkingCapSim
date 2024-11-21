/*
 * (c) 2002 Humberto Martinez
 * (c) 2024 Humberto Martinez
 */
 
package tcrob.iasf;

import java.util.*;

import tc.shared.lps.lpo.*;
import tc.shared.linda.*;

import tclib.behaviours.bg.*;
import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

import tcrob.umu.indoor.linda.*;

import devices.pos.*;
import wucore.utils.math.*;
		
public class IasfController extends BGController
{	
	static public final double			WORLD_SIZE		= 8.0;					// World extent (m)
	static public final double			CELL_SIZE		= 0.15;					// Cell size (m)
	static public final double			DEF_DIL			= 1.5;					// Default dilation constant				
	static public final int				LOOKA_DIST		= 2;					// Look-ahead distance (cells)
	
	// Navigation structures
	protected Grid						grid;
	protected GridPath					gpath;

	// Linda data structures
	protected Tuple						gmtuple;
	protected ItemGridMap				gmitem;

	// Relevant objects (LPOs)
	protected Position					robot;
	protected Position					goal;
	
	// Constructors
	public IasfController (Properties props, Linda linda) 
	{
		super (props, linda);
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		super.initialise (props);

		// Initialise Linda related structures
		gmitem		= new ItemGridMap ();
		gmtuple		= new Tuple ("GRIDMAP", gmitem);
		
		// Local variables
		robot		= new Position (0.0, 0.0, 0.0);
		goal		= new Position (5.0, 5.0, 0.0);
	}

	protected void controller () 
	{
		int					i;
		int					result;
		LPOSensorRange		virtual;
		LPOSensorGroup		group;
		LPO					l_looka;
		double				speed, turn;

		// Set current goal point
		goal.set (plan.tpos);
		
		/* ------------------ */
		/* COMPUTE LOCAL GRID */
		/* ------------------ */
		
		grid.reset ();
		grid.location (pos);
		
		gpath.goal (goal);
		gpath.curve (GridPath.POLYLINE, GridPath.GRID);
		gpath.replan (robot);
		while (!gpath.newPath ())
			gpath.replan (robot);
		path = gpath.path ();

		gmitem.set (grid, gpath, robot, System.currentTimeMillis ());
		linda.write (gmtuple);
		
		/* ---------- */
		/* LOOK-AHEAD */
		/* ---------- */
		
		// Compute look-ahead point
		looka.valid (false);
		path.check_lookahead (robot, LOOKA_DIST);
		if (path.lookahead () != null)
		{
			path_dst = path.distance ();
			looka.set (path.lookahead ());
			looka.valid (true);
		}

		/* ----------- */
		/* INTERPRETER */
		/* ----------- */
				
		// Update LPS
		l_looka		= lps.find ("Looka");
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha ());
			l_looka.active (looka.valid ());
		}

		// Put perception percept into BG interpreter
		virtual	= (LPOSensorRange) lps.find ("Virtual");
		for (i = 0; i < fdesc.MAXVIRTU; i++)
			interp.access ("virtu"+i, virtual.range[i]);

		group	= (LPOSensorGroup) lps.find ("Group");
		for (i = 0; i < fdesc.MAXGROUP; i++)
			interp.access ("group"+i, group.range[i]);
		
		interp.access ("x", pos.x ());
		interp.access ("y", pos.y ());
		interp.access ("alpha", pos.alpha ());
		interp.access ("heading", Math.atan2 ((looka.y () - pos.y ()), (looka.x () - pos.x ())));

		// Run the whole BG program		
		interp.agents (program);

		// Read the specified action from BG interpreter
		speed	= interp.access ("speed");
		turn	= interp.access ("turn") * Angles.DTOR;

		/* ------------ */
		/* SEND RESULTS */
		/* ------------ */
		
		// Set action
		result	= inGoal ();
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			speed 	= 0.0;
			turn	= 0.0;
			
			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;
			
		case ItemBehResult.T_FAILED:
			speed 	= 0.0;
			turn	= 0.0;
			
			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);			// or ItemBehResult.F_SIDE
			break;
			
		case ItemBehResult.T_NOTYET:
		default:
			if (!looka.valid ())
			{
				speed 	= 0.0;
				turn	= 0.0;
			}
		}
		setMotion (speed, turn);

		// Plot current control commands
		if (debug)
		{
			c_buffer[0] 	= Math.max (Math.min (speed / rdesc.model.Vmax, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (turn / rdesc.model.Rmax, 1.0), -1.0);

			if (localgfx)
				c_plot.draw (c_buffer);	
			else
				c_dump.write (c_buffer);
		}
	}

	public void notify_config (String space, ItemConfig item)
	{
		int				w, h;
		int				dil;
		
		super.notify_config (space, item);
	    
		// Create data structures
		w 	= (int) Math.round (WORLD_SIZE / CELL_SIZE) + 4;
		h 	= (int) Math.round (WORLD_SIZE / CELL_SIZE) + 4;
		dil = (int) (Math.round (rdesc.RADIUS * DEF_DIL / CELL_SIZE));

		grid 	= new FGrid (fdesc, rdesc, w, h, CELL_SIZE);		
		grid.setMode (FGrid.SAFE_MOTION);
		grid.setRangeSON (1.5);
		grid.setOffsets (-WORLD_SIZE * 0.5, -WORLD_SIZE * 0.5);	
		
		gpath	= new FGridPathA (grid);
		gpath.setDilation (dil);
		gpath.setTimeStep (300);
	}

	public void notify_goal (String space, ItemGoal goal)
	{
		super.notify_goal (space, goal);
		
		// Reset current interpreter state
		interp.reset ();
	}
}

