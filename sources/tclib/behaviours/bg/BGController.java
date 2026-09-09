/*
 * (c) 2001 Humberto Martinez
 */
 
package tclib.behaviours.bg;

import java.util.*;

import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tclib.behaviours.bg.interpreter.*;
import tclib.planning.sequence.*;

import devices.pos.*;
import wucore.utils.logs.*;
import wucore.utils.math.*;

public class BGController extends Controller
{
	static public final String		PREFFIX			= "CNTL_";
	
	// BG Interpreter related stuff
	protected Interpreter			interp;
	protected Program				program;
	
	// Controller debug
	protected LogPlot				c_plot;
	protected LogFile				c_dump;
	protected double[]				c_buffer;
	protected String[]				c_labels;
	
	// Behaviour fusion debug
	private LogPlot					b_plot;
	private LogFile					b_dump;
	private double[]				b_buffer;
	
	// Goal and task related variables
	protected boolean				has_goal;					// Is any goal available?
	protected boolean				new_goal;					// New goal received
	protected long					new_id;						// New task ID received
	protected Task					new_plan;					// New task received
	
	protected Task					plan;						// Current goal task
	protected long					idtask;						// Current task ID

	// Look-ahead related variables
	protected Path					path;						// Desired robot path
	protected Position				pos;						// Current robot location
	protected Position				looka;						// Current look-ahead point
	protected int					looka_pts;					// Current look-ahead distance (points)
	protected double				path_dst;					// Current robot to desired path distance (m)
	
	protected boolean				dump;
	
	// Constructors
	public BGController (Properties props, Linda linda) 
	{
		super (props, linda);
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		super.initialise (props);
		
		// Initialize local structures
		looka		= new Position ();
		pos			= new Position ();
		plan		= new Task ();
		
		has_goal	= false;
		new_plan	= new Task ();
		new_goal	= false;
		new_id		= 0;
		
		idtask		= 0;
		looka_pts	= 15;

		// Create the BG interpreter
		interp	= new Interpreter ();	
		
		// Initialize debug modules
		c_buffer	= new double[2];
		c_labels	= new String[4];
		c_labels[0]	= "speed";
		c_labels[1]	= "turn";
		c_dump		= new LogFile (PREFFIX, ".log");
		c_plot		= new LogPlot ("Controller Output", "step", "values");
		
		b_dump		= new LogFile (PREFFIX, ".beh");
		b_plot		= new LogPlot ("Behaviour Fusion", "step", "DoA");
		b_plot.setImpulses (true);
		b_plot.setYRange (0.0, 1.0);
		
		dump		= false;

		// Parse BG file
		parse (props);		
	}
	
	protected void parse (Properties props)
	{		
		String			name = null;
	
		b_dump.close ();
		c_dump.close ();

		// Load and parse a BG program
		name = props.getProperty ("CONPRG");
		if (name != null)
		{
			BGParser.parse (name, false);
			if (BGParser.isparsed ())
			{
				// Initialize BG interpreter
				program	= BGParser.program ();
				interp.initialize (program);
				
				// Create additional perceptual structures
				b_buffer = new double[program.behcount ()];
				
				if (localgfx)
				{
					b_plot.open (program.behlabels ());
					c_plot.open (c_labels);
				}
				if (dump)
				{
					b_dump.open (program.behlabels ());
					c_dump.open (c_labels);
				}
			}
		}
	}
	
	protected int inGoal ()
	{
		double			dx, dy;
		double			dist, delta;

		// Check if goal position has been reached
		dx		= plan.tpos.x () - pos.x ();
		dy		= plan.tpos.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);										// [m]
		delta	= Math.abs (Angles.radnorm_180 (plan.tpos.alpha () - pos.alpha ()));	// [rad]	

		if ((dist < plan.tol_pos) && (delta < plan.tol_head))		
			return ItemBehResult.T_FINISHED;
		
		return ItemBehResult.T_NOTYET;
	}
	
	protected void controller () 
	{
		int					result;
		LPOSensorRange		virtual;
		LPOSensorGroup		group;
		LPO					l_looka;
		double				speed, turn;

		if (!has_goal)
		{
			setMotion (0.0, 0.0);
			return;
		}
		
		/* ---------- */
		/* LOOK-AHEAD */
		/* ---------- */
		
		// Compute look-ahead point
		pos.set (lps.cur);
		looka.set (pos);
		looka.valid (false);
		if (!new_goal && (path != null))
		{
			path.check_lookahead (pos, looka_pts);
			if (path.lookahead () != null)
			{
				path_dst = path.distance ();
				looka.set (path.lookahead ());
				looka.valid (true);
			}
		}

		// Update LPS
		l_looka = lps.find ("Looka");
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha ());
			l_looka.active (looka.valid ());
		}

		/* ----------- */
		/* INTERPRETER */
		/* ----------- */
				
		// Put perception percepts into BG interpreter
		virtual	= (LPOSensorRange) lps.find ("Virtual");
		for (int i = 0; i < fdesc.MAXVIRTU; i++)
			interp.access ("virtu"+i, virtual.range[i]);

		group = (LPOSensorGroup) lps.find ("Group");
		for (int i = 0; i < fdesc.MAXGROUP; i++)
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
		if (localgfx || dump)
		{
			c_buffer[0] 	= Math.max (Math.min (speed / rdesc.model.Vmax, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (turn / rdesc.model.Rmax, 1.0), -1.0);

			if (localgfx)
				c_plot.draw (c_buffer);	
			if (dump)
				c_dump.write (c_buffer);
		}
	}
	
	protected void checkplan ()
	{
		if (idtask != new_id)
		{
			idtask = new_id;
			plan.set (new_plan);
				
			if (debug)		System.out.println ("  [BG] Working with task [" + plan + "] and ID: " + idtask);
		}
	}
	
	protected void newplan (ItemGoal goal)
	{
		new_plan.set (goal.task);		
		new_id = goal.timestamp.longValue ();
	}
	
	public void step (long ctime) 
	{
		if (state != RUN)												return;
		
		if (!auto || (program == null) || (lps == null))				return;
		
		// Set last goal received as the current one
		checkplan ();
		
		// Run BG program
		controller ();

		// Update behavior fusion information 
		if (debug)
			System.out.println ("  [BG] Control cycle: " + (System.currentTimeMillis () - ctime) + " ms");

		if ((b_buffer != null) && (localgfx || dump))
		{
			interp.fusion (program, b_buffer);
			
			if (localgfx)
				b_plot.draw (b_buffer);	
			if (dump)
				b_dump.write (b_buffer);
		}
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
	    
	    // TODO This could be the RIGHT place to receive the full BG program
	    // parse (name);
	}
	
	public void notify_debug (String space, ItemDebug item)
	{
		super.notify_debug (space, item);
	}
	
	public void notify_goal (String space, ItemGoal goal)
	{
		LPOPoint			l_goal;
	
		// Update LPS
		l_goal = (LPOPoint) lps.find ("Goal");
		if (l_goal != null)
		{
			l_goal.update (pos, goal.task.tpos);
			l_goal.active (true);
		}
				
		newplan (goal);
		
		new_goal	= true;
		has_goal	= true;
	}

	public void notify_path (String space, ItemPath item)
	{
		path		= item.path;
		
		new_goal	= false;
	}
}

