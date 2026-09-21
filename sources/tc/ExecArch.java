/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 * (c) 2003 Humberto Martinez
 */

package tc;

import java.io.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.linda.net.*;
import tcapps.tcsimulator.simulator.Simulator;
import tcapps.tcsimulator.simulator.objects.SimRobot;
import tc.shared.world.WMStart;
import tc.shared.world.World;
import wucore.utils.geom.Point3;

public class ExecArch extends Thread
{
	static public final int 		MAX_THS		= 20;	// Maximum number of modules
	
	// Linda Spaces parameters
	public LindaDesc				lldesc;				// Local Linda Space description
	public LindaDesc				gldesc;				// Global Linda Space description
	
	// Actual Linda Spaces
	protected LindaServer			linda_loc;
	protected LindaServer			linda_glob;

	// Architecture modules and parameters
	public int					num;					// Number of modules
	public ThreadDesc[]			thdesc;				// Actual standard modules
	public ThreadDesc			vrdesc;				// Actual VirtualRobot module
	public RouterDesc			lrdesc;				// Actual LindaRouter module
		
	// Additional execution variables
	protected boolean			initialised	= false;
	protected volatile boolean	running		= false;
	protected DeployArch			deploy;				// Deployment this robot belongs to
	protected String				robotid;

	// Simulation: when a Simulator is given the virtual robot module is replaced by a SimRobot bound to it
	protected Simulator			sim;
	protected Point3			start;				// Optional initial pose of the simulated robot

	/* Constructors */
	/**
	 * Executes one robot of a deployment: its modules, its Linda spaces and its
	 * virtual robot, taken from the deployment itself. The first robot is the
	 * one that instantiates the global Linda space, so the robots must be
	 * started in order.
	 */
	public ExecArch (DeployArch deploy, int robot)
	{
		initialise (deploy, robot);
	}

	/**
	 * Simulated execution of one robot of a deployment: its virtual robot runs
	 * as a {@link SimRobot} inside <code>sim</code>, placed at the start point
	 * chosen for it in the deployment. If the simulator has no world yet, the
	 * world of the deployment is loaded into it; otherwise the simulator's
	 * world is imposed.
	 */
	public ExecArch (DeployArch deploy, int robot, Simulator sim)
	{
		this (deploy, robot);
		simulate (sim);
		startPoint (deploy.robots.get (robot), (sim != null) ? sim.getWorld () : null);
	}

	/** Places the simulated robot at the start point the deployment chose for it. */
	protected void startPoint (DeployArch.Robot rob, World world)
	{
		int		si = DeployArch.startIndex (rob.start);

		if ((world == null) || (si < 0) || (si >= world.n_starts ()))		return;

		WMStart	st = world.start (si);
		setStart (new Point3 (st.x (), st.y (), st.orientation));
	}

	protected void simulate (Simulator sim)
	{
		this.sim	= sim;
		if ((sim == null) || (vrdesc == null))		return;

		// Load a world description if none available
		if (sim.getWorld () == null)
		{
			try  { sim.setWorld (vrdesc.config.get ("WORLD")); }
			catch (Exception e)
			{
				System.out.println ("[ExecArch]: Exception loading world: " + e);
				return;
			}
		}
		if (sim.getWorldName () != null)
			vrdesc.config.set ("WORLD", sim.getWorldName ());

		// The simulated robot replaces the real one
		vrdesc.classn = SimRobot.class.getName ();
	}

	public Simulator	getSimulator ()					{ return sim; }
	public void			setStart (Point3 start)			{ this.start = start; }

	// Class methods
	public static void main (String[] argv)
	{
		if (argv.length >= 1)
		{
			try
			{
				DeployArch	deploy = DeployArch.load (new File (argv[0]));
				int			robot = (argv.length > 1) ? Integer.parseInt (argv[1]) : 0;
				new ExecArch (deploy, robot).start ();
			} catch (Exception e) { e.printStackTrace (); }
		}
		else
		{
			System.out.println ("ERROR: wrong number of arguments.");
			System.out.println ("\tUsage: ExecArch <deployment.deploy> [robot] to execute one robot of a deployment");
		}
	}

    
	/* Instance methods */

	/**
	 * Builds the descriptors of the robot straight from the deployment: its two
	 * Linda spaces, its modules (each one with its own configuration), its
	 * router and its virtual robot. Only the first robot creates the global
	 * space; the others connect to it.
	 */
	protected void initialise (DeployArch deploy, int robot)
	{
		DeployArch.Robot	rob = deploy.robots.get (robot);
		DeployArch.Linda	glin = deploy.globalLinda;
		String				world = deploy.getWorldFile ();
		int					i;

		this.robotid	= rob.name;
		this.deploy		= deploy;

		// Prepare data structures
		num				= 0;
		thdesc			= new ThreadDesc[MAX_THS];

		// Linda spaces of the robot
		lldesc		= new LindaDesc ("LLIN", LindaDesc.L_LOCAL, rob.linda.address, rob.linda.port, rob.linda.instantiate);
		gldesc		= ((glin != null)
					? new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, glin.address, glin.port, glin.instantiate && (robot == 0))
						: null);
//					: new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, null, 0, false);

		// Modules of the robot
		num			= Math.min (rob.modules.size (), MAX_THS);
		for (i = 0; i < num; i++)
			thdesc[i]	= new ThreadDesc ("MOD" + (i + 1), config (rob, rob.modules.get (i)));

		// The router only makes sense when there is a global space to route to
		if ((rob.router != null) && (glin != null))
			lrdesc		= new RouterDesc ("COO", config (rob, rob.router));

		// Virtual robot (the world of the deployment applies to every robot)
		if (rob.virtualRobot != null)
		{
			vrdesc		= new ThreadDesc ("ROB", config (rob, rob.virtualRobot));
			vrdesc.passive	= false;					// a robot runs on its own cycle, whatever it was told
			if (world != null)		vrdesc.config.set ("WORLD", world);
		}

		initialised		= true;	
	}

	/** Configuration a module is given: its own values, the properties of its robot and the robot name. */
	protected ModuleConfig config (DeployArch.Robot rob, DeployArch.Module m)
	{
		ModuleConfig	cfg = new ModuleConfig (rob.name, m.name, m.properties, rob.properties);
		StringBuilder	sb = new StringBuilder ();

		// the events the module registers, in the form StdThread reads them
		for (DeployArch.Event ev : m.events)
		{
			if (sb.length () > 0)		sb.append (", ");
			sb.append (ev.symbol).append ('\t').append (ev.itemClass).append ('\t').append (ev.method);
		}
		if (sb.length () > 0)		cfg.set ("CONNECT", sb.toString ());

		return cfg;
	}

	public void run ()
	{
		int				i;
		
		while (!initialised);
		
		this.setName ("TC-Arch-Executor ("+robotid+")");

		System.out.println ("\nExecArch: Executing a TC-II ADF (Architecture Definition File)");
		System.out.println (this);
		System.out.println ();		
		
		try
		{
			// Crate Linda Spaces if required
			if (lldesc.create)							linda_loc	= lldesc.start_server ();
			if ((gldesc != null) && gldesc.create)		linda_glob	= gldesc.start_server ();
			
			// Execute LindaRouter if needed
			if (lrdesc != null)
				lrdesc.start_thread (robotid, lldesc, linda_loc, gldesc, linda_glob);

			// Execute required standard modules
			for (i = 0; i < num; i++)
				thdesc[i].start_thread (robotid, lldesc, linda_loc);
			
			// Execute VirtualRobot if needed
			virtual_robot ();
		}
		catch (Exception e) { e.printStackTrace (); }
		
		running	= true;
		try { Thread.currentThread ().join (); } catch (InterruptedException e) { /* terminate () */ }
		running	= false;
		
		System.out.println ("Program finished.");
	}

	/* ------------------------------------------------------------------ */
	/* Execution control (used by the simulator GUI)                        */
	/* ------------------------------------------------------------------ */

	/** True between the start of the modules and {@link #terminate}. */
	public boolean isRunning ()			{ return running; }

	/**
	 * Stops every module thread and the Linda servers created by this
	 * architecture and finishes the executor thread.
	 */
	public void terminate ()
	{
		int		i;

		System.out.println ("ExecArch: terminating [" + robotid + "]");
		try
		{
			if ((vrdesc != null) && (vrdesc.thread != null))		vrdesc.thread.stop ();
			for (i = 0; i < num; i++)
				if (thdesc[i].thread != null)						thdesc[i].thread.stop ();
			if (linda_loc != null)									linda_loc.stop ();
			if (linda_glob != null)									linda_glob.stop ();
		} catch (Exception e) { e.printStackTrace (); }
		running	= false;
		interrupt ();
	}

	/**
	 * Sends a task sequence (plan) to the robot through the local Linda space,
	 * as the monitor's Task panel does. Returns false when there is no local space.
	 */
	public boolean sendPlan (tclib.planning.sequence.Sequence seq)
	{
		if (linda_loc == null)			return false;
		ItemPlan	item = new ItemPlan ();
		item.set (seq, System.currentTimeMillis ());
		Tuple		tuple = new Tuple (Tuple.PLAN, item);
		tuple.space	= robotid;
		linda_loc.write (tuple);
		return true;
	}

	/** Local Linda space of the running architecture (null in remote modes or before start). */
	public Linda getLocalLinda ()		{ return linda_loc; }

	/**
	 * Sends an execution command (ItemExecution.START, STOP, STEP, ...) to every
	 * module of the robot through the local Linda space, as the monitor's
	 * execution control does. Returns false when there is no local space.
	 */
	public boolean sendCommand (int command)
	{
		if (linda_loc == null)			return false;
		ItemExecution	item = new ItemExecution ();
		item.command (command, System.currentTimeMillis ());
		Tuple		tuple = new Tuple (Tuple.EXECUTION, item);
		tuple.space	= robotid;
		linda_loc.write (tuple);
		return true;
	}
	
	public DeployArch getDeployment ()		{ return deploy; }
	public String getRobotId ()				{ return robotid; }

	protected void virtual_robot ()
	{
		if (vrdesc == null)				return;
		if (sim == null)
		{
			vrdesc.start_thread (robotid, lldesc, linda_loc);
			return;
		}

		// Simulated robot (former ExecArchSim.virtual_robot)
		System.out.println (">> Starting Simulated Robot [" + vrdesc.preffix + "@" + robotid + "]");
		Linda	linda = null;
		try
		{
			if (linda_loc != null)
				linda = new LindaSharedClient (linda_loc, robotid);			// own tuples delivered with the robot id as space
			else if (vrdesc.mode == ThreadDesc.M_UDP)
				linda = new LindaNetClient (LindaNet.UDP, null, lldesc.addr, lldesc.port);
			else if (vrdesc.mode == ThreadDesc.M_TCP)
				linda = new LindaNetClient (LindaNet.TCP, null, lldesc.addr, lldesc.port);
		} catch (Exception e)
		{
			e.printStackTrace ();
			linda = null;
		}
		if (linda == null)
		{
			System.out.println ("--[ExecArch] Can not create Linda client. Aborting simulated robot [" + vrdesc.preffix + "@" + robotid + "]");
			return;
		}

		SimRobot	thread = new SimRobot (robotid, vrdesc.config, linda, sim);
		vrdesc.robotid	= robotid;
		thread.setTDesc (vrdesc);
		if (start != null)		thread.reset (start);
		vrdesc.thread	= thread;
		thread.start ();
	}
	
	public String toString ()
	{
		int				i;
		String			str;
		
		str		= "Architecture Description\n";
		
		if (gldesc != null) 	str += "\t" + gldesc + "\n";			// Global Linda Space
		str += "\t" + lldesc + "\n";									// Local Linda Space
		if (lrdesc != null)		str += "\t" + lrdesc + "\n";			// Linda Router
		
		for (i = 0; i < num; i++)
			str += "\t" + thdesc[i] + "\n";								// Architecture modules
			
		if (vrdesc != null)		str += "\t" + vrdesc + "\n";			// Virtual Robot

		return str;
	}
}

