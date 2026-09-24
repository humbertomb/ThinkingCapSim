/*
 * (c) 2001 Humberto Martinez
 */
 
package tclib.behaviours.bg;

import tc.runtime.thread.ModuleConfig;

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
	protected double[]				c_buffer;
	protected String[]				c_labels;
	
	// Behaviour fusion debug
	private LogPlot					b_plot;
	private LogFile					b_dump;
	private double[]				b_buffer;
	
	// Goal and task related variables
	protected boolean				autostart;					// AUTO: it runs with no plan of anybody's
	protected boolean				has_goal;					// Is any goal available?
	protected boolean				has_plan;					// Has anybody said where to go? (a plan was received)
	protected boolean				new_goal;					// New goal received
	protected long					new_id;						// New task ID received
	protected Task					new_plan;					// New task received
	
	protected Task					plan;						// Current goal task
	protected long					idtask;						// Current task ID

	// Look-ahead related variables
	protected Path					path;						// Desired robot path
	protected Position				pos;						// Current robot location
	protected boolean				need_looka;					// Do we need a look-ahead point?
	protected Position				looka;						// Current look-ahead point
	protected int					looka_pts;					// Current look-ahead distance (points)
	protected double				path_dst;					// Current robot to desired path distance (m)
		
	// Constructors
	public BGController (ModuleConfig cfg, Linda linda) 
	{
		super (cfg, linda);
	}
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);
		
		// Initialize local structures
		looka		= new Position ();
		pos			= new Position ();
		plan		= new Task ();
		
		has_goal	= false;
		has_plan	= false;
		new_plan	= new Task ();
		new_goal	= false;
		new_id		= 0;
		need_looka	= true;
		
		idtask		= 0;
		looka_pts	= 15;

		// Create the BG interpreter
		interp	= new Interpreter ();	
		
		// Initialize debug modules
		c_buffer	= new double[3];
		c_labels	= new String[3];			// the three velocities of the control action
		c_labels[0]	= "vlin";
		c_labels[1]	= "vlat";
		c_labels[2]	= "vrot";
		c_plot		= new LogPlot ("Controller Output", "step", "values");
		
		b_dump		= new LogFile (PREFFIX, ".beh");
		b_plot		= new LogPlot ("Behaviour Fusion", "step", "DoA");
		b_plot.setImpulses (true);
		b_plot.setValues ("Behaviour Values");
		b_plot.setYRange (0.0, 1.0);
		
		// Parse BG file
		parse (cfg);		
		
		// Autostart the controller without a plan: it runs its program from the
		// first cycle, and until somebody says where to go there is no goal to have
		// arrived at (see inGoal)
		autostart	= cfg.getBoolean ("AUTO", false);
		if (autostart)
		{
			has_goal	= true;
			has_plan	= false;
			need_looka	= false;
		}
	}
	
	protected void parse (ModuleConfig cfg)
	{		
		String			name = null;
	
		b_dump.close ();

		// Load and parse a BG program
		name = cfg.get ("PRG");
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
			}
		}	
	}
	
	/**
	 * Whether the robot has arrived where it was told to go: reached, failed, or
	 * not yet.
	 *
	 * Nobody having said where to go is not arriving: a controller started on its
	 * own (AUTO) runs its program with no plan, and the task it carries is then
	 * the one a Task is born with -- the origin of the world, within a quarter of a
	 * metre and any heading at all. Checked against that, a robot standing at the
	 * start of its world reported the task finished on every cycle, and a finished
	 * task zeroes what the behaviours asked for: it ran and did not move.
	 */
	protected int inGoal ()
	{
		double			dx, dy;
		double			dist, delta;

		if (!has_plan)												return ItemBehResult.T_NOTYET;

		// Check if goal position has been reached
		dx		= plan.tpos.x () - pos.x ();
		dy		= plan.tpos.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);										// [m]
		delta	= Math.abs (Angles.radnorm_180 (plan.tpos.alpha () - pos.alpha ()));	// [rad]	

		if ((dist < plan.tol_pos) && (delta < plan.tol_head))		
			return ItemBehResult.T_FINISHED;
		
		return ItemBehResult.T_NOTYET;
	}
	
	/**
	 * The program starts afresh: the interpreter forgets what it had worked out and
	 * there is no goal, no plan and no path any more (RESET). Whether it runs from
	 * the first cycle again is what AUTO says, as when the module was set up.
	 */
	protected void reset ()
	{
		super.reset ();

		if ((interp != null) && (BGParser.isparsed ()))		interp.initialize (BGParser.program ());
		has_goal	= autostart;
		has_plan	= false;
		new_goal	= false;
		path		= null;
		idtask		= 0;
		new_id		= 0;
	}

	protected void controller () 
	{
		int					result;
		LPOSensorRange		virtual;
		LPOSensorGroup		group;
		LPO					l_looka;
		double				vlin, vlat, vrot;

		if (!has_goal)
		{
			setMotion (0.0, 0.0, 0.0);
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
		vlin	= interp.access ("speed");
		vlat	= 0.0;										// a BG program commands no lateral velocity
		vrot	= interp.access ("turn") * Angles.DTOR;

		// Set action
		result	= inGoal ();
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			vlin 	= 0.0;
			vlat	= 0.0;
			vrot	= 0.0;
			
			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;
			
		case ItemBehResult.T_FAILED:
			vlin 	= 0.0;
			vlat	= 0.0;
			vrot	= 0.0;
			
			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);			// or ItemBehResult.F_SIDE
			break;
			
		case ItemBehResult.T_NOTYET:
		default:
			if (need_looka && !looka.valid ())
			{
				vlin 	= 0.0;
				vlat	= 0.0;
				vrot	= 0.0;
			}
		}
		setMotion (vlin, vlat, vrot);

		// Plot current control commands
		if (localgfx)
		{
			c_buffer[0] 	= tc.vrobot.RobotModel.share (vlin, rdesc.model.Vmax);
			c_buffer[1] 	= tc.vrobot.RobotModel.share (vlat, rdesc.model.Umax);
			c_buffer[2] 	= tc.vrobot.RobotModel.share (vrot, rdesc.model.Rmax);
			c_plot.draw (c_buffer);	
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
	
	protected void close_gfx ()
	{
		if (c_plot != null)		c_plot.close ();
		if (b_plot != null)		b_plot.close ();
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

		if ((b_buffer != null) && localgfx)
		{
			interp.fusion (program, b_buffer);			
			b_plot.draw (b_buffer);	
		}
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
	    
	    // TODO This could be the RIGHT place to receive the full BG program
	    // parse (name);
	}
	
	public void notify_execution (String space, ItemExecution item)
	{
		super.notify_execution (space, item);
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
		has_plan	= true;										// now there is somewhere to arrive at
	}

	public void notify_path (String space, ItemPath item)
	{
		path		= item.path;
		
		new_goal	= false;
	}
}

