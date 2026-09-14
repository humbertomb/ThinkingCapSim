/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 * (c) 2003 Humberto Martinez
 */

package tc;

import java.io.*;
import java.util.*;

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
	protected Properties			props;
	protected String				robotid;

	// Simulation: when a Simulator is given the virtual robot module is replaced by a SimRobot bound to it
	protected Simulator			sim;
	protected Point3			start;				// Optional initial pose of the simulated robot

	/* Constructors */
	protected ExecArch ()
	{
	}

	/**
	 * Executes one robot of a deployment: the modules, the Linda spaces and the
	 * virtual robot described by {@link DeployArch#toProperties(int)}. The first
	 * robot of the deployment is the one that instantiates the global Linda
	 * space, so the robots must be started in order.
	 */
	public ExecArch (DeployArch deploy, int robot)
	{
		initialise (deploy.robots.get (robot).name, deploy.toProperties (robot), null);
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

	/** Executes an architecture given directly as the properties of an ADF. */
	public ExecArch (String robotid, Properties props)
	{
		initialise (robotid, props, null);
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
			try  { sim.setWorld (props.getProperty (vrdesc.preffix + "WORLD")); }
			catch (Exception e)
			{
				System.out.println ("[ExecArch]: Exception loading world: " + e);
				return;
			}
		}
		if (sim.getWorldName () != null)
			props.setProperty (vrdesc.preffix + "WORLD", sim.getWorldName ());

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
	protected void initialise (String robotid, Properties props, Properties pdefs)
	{
		String			modules, vrmodule, lrmodule;
		String			preffix;
		StringTokenizer	st;
		Enumeration<?>	enu;
		String			pname;
		
		// Setup private local variables
		this.props		= props;
		this.robotid	= robotid;
				
		if (robotid != null)		props.setProperty ("ROBNAME", robotid);
		
		// Prepare data structures
		num				= 0;
		thdesc			= new ThreadDesc[MAX_THS];

		// Overwrite default properties
		if (pdefs != null)
		{
			enu	= pdefs.propertyNames ();
			while (enu.hasMoreElements ())
			{
				pname	= (String) enu.nextElement ();
				
				props.setProperty (pname, pdefs.getProperty (pname));
			}
		}

		// Load and parse Linda servers properties
		lldesc		= new LindaDesc ("LLIN", LindaDesc.L_LOCAL, props);			
		gldesc		= new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, props);			

		// Load and parse architecture global properties
		modules		= props.getProperty ("MODULES");
		vrmodule		= props.getProperty ("VROBOT");
		lrmodule		= props.getProperty ("ROUTER");
		if (modules != null)
		{
			st			= new StringTokenizer (modules, ", \t");
			for (num = 0; st.hasMoreTokens (); num++)
			{
				preffix		= st.nextToken ();
				thdesc[num]	= new ThreadDesc (preffix, props);
			}
		}
		if (lrmodule != null)
			lrdesc		= new RouterDesc (lrmodule, props);			
		if (vrmodule != null)
			vrdesc		= new ThreadDesc (vrmodule, props);			
			
		initialised		= true;	
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
			if (lldesc.create)			linda_loc	= lldesc.start_server ();
			if (gldesc.create)			linda_glob	= gldesc.start_server ();
			
			// Execute LindaRouter if needed
			if (lrdesc != null)
				lrdesc.start_thread (robotid, props, lldesc, linda_loc, gldesc, linda_glob);

			// Execute required standard modules
			for (i = 0; i < num; i++)
				thdesc[i].start_thread (robotid, props, lldesc, linda_loc);
			
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
	 * Sends an execution command (ItemDebug.START, STOP, STEP, ...) to every
	 * module of the robot through the local Linda space, as the monitor's
	 * execution control does. Returns false when there is no local space.
	 */
	public boolean sendCommand (int command)
	{
		if (linda_loc == null)			return false;
		ItemDebug	item = new ItemDebug ();
		item.command (command, System.currentTimeMillis ());
		Tuple		tuple = new Tuple (Tuple.DEBUG, item);
		tuple.space	= robotid;
		linda_loc.write (tuple);
		return true;
	}
	
	public Properties getProperties ()		{ return props; }
	public String getRobotId ()				{ return robotid; }

	protected void virtual_robot ()
	{
		if (vrdesc == null)				return;
		if (sim == null)
		{
			vrdesc.start_thread (robotid, props, lldesc, linda_loc);
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

		SimRobot	thread = new SimRobot (robotid, props, linda, sim);
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
		
		str += "\t" + gldesc + "\n";									// Global Linda Space
		str += "\t" + lldesc + "\n";									// Local Linda Space
		if (lrdesc != null)		str += "\t" + lrdesc + "\n";			// Linda Router
		
		for (i = 0; i < num; i++)
			str += "\t" + thdesc[i] + "\n";								// Architecture modules
			
		if (vrdesc != null)		str += "\t" + vrdesc + "\n";			// Virtual Robot

		return str;
	}
}

