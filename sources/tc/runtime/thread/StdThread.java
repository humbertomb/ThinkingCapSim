/*
 * (c) 2002 Humberto Martinez
 */

package tc.runtime.thread;

import java.util.*;

import tc.runtime.event.*;
import tc.shared.linda.*;

public abstract class StdThread implements Runnable, LindaListener
{
	// General constants
	static public final int		MAX_EVTS	= 100;		// Maximum number of events (for registration)
	
	// Execution status
	static public final int		INIT		= 0;		// Initialisation phase
	static public final int		CONFIG		= 1;		// Configuration phase
	static public final int		RUN			= 2;		// Execution phase
	
	// Linda related variables
	protected Thread			thread;					// Asynchronous execution thread
	private volatile boolean	thlock		= false;	// Asynchronous thread execution lock
	public ThreadDesc			tdesc;					// Parameters for the current thread
	protected Linda				linda; 					// Reference to distributed blackboard
	protected LindaQueue		queue;					// Queue for receiving Linda events
	
	// Linda registration variables
	protected Hashtable<String,EventDesc>	recvs;					// Description of Linda receivers
	
	// Debug and control parameters
	protected int				state;
	protected boolean			running		= false;
	private volatile boolean	configured	= false;	// whether it has taken in its configuration (CONFIG), whatever it made of it
	protected boolean			step		= false;
	protected boolean			debug		= false;
	protected boolean			auto		= true;
	protected boolean			localgfx	= false;
	
	protected ModuleConfig		config;					// Configuration of this module in the deployment

	// The window of the application running the modules (the simulator), which their own windows are placed by
	static private volatile javax.swing.JFrame	hostFrame;

	/** Says which window runs the modules (the simulator does, when it is built); null for none. */
	static public void setHostFrame (javax.swing.JFrame frame)		{ hostFrame = frame; }
	/** The window that runs the modules, for a module to place its own windows by it; null when there is none. */
	static public javax.swing.JFrame hostFrame ()					{ return hostFrame; }
	
	// Constructors
	public StdThread (ModuleConfig config, Linda linda)
	{
		this.linda	= linda;
		this.queue	= new LindaQueue ();
		this.state	= INIT;
		
		this.recvs	= new Hashtable<String,EventDesc> (MAX_EVTS);
		
		this.config	= config;
	}
	
	// Instance methods
	public void setTDesc (ThreadDesc tdesc)
	{ 
		String			event;
		EventDesc		edesc;
		ConfigDesc		cdesc;
		ExecutionDesc	execdesc;
		StringTokenizer	st;
		
		this.tdesc	= tdesc; 
		
		// Initialise user-defined event receivers
		if (tdesc.connects != null)
		{
			st = new StringTokenizer (tdesc.connects, ",");
			while (st.hasMoreTokens ())
			{
				event	= st.nextToken ();
				// an event that cannot be registered is left out, not the module
				try { edesc = new EventDesc (this, linda, event); }
				catch (IllegalArgumentException e)
				{
					System.out.println ("--[Thread] " + e.getMessage () + ". Ignored.");
					continue;
				}
				
				System.out.println ("\t>> Registering event " + edesc);
				recvs.put (edesc.key, edesc);
			}
		}
		
		// Initialise standard event receivers (the ConfigDesc MUST always be the last one)
		execdesc	= new ExecutionDesc (this, linda);
		recvs.put (execdesc.key, execdesc);
		cdesc		= new ConfigDesc (this, linda);
		recvs.put (cdesc.key, cdesc);
		
		// Initialise other processing options
		localgfx	= tdesc.cangfx;
		state 		= CONFIG; 

		initialise (config);
	}
	
	/**
	 * Whether the module has taken in its configuration (CONFIG, which the robot
	 * sends when it starts): until then it is still setting itself up -- a vision
	 * computing its tables, a controller reading its machine -- and is not ready
	 * to be told to run.
	 */
	public boolean isConfigured ()			{ return configured; }

	public void start ()
	{
		if (state == INIT)					return;
		if (tdesc.queued || tdesc.polled)		tdesc.passive = false;		
		if (tdesc.passive)					return;
		
		thread = new Thread (this);
		thread.setName ("TC-Thread-" + tdesc.preffix);
		if (tdesc.priority != -1)		
			thread.setPriority (tdesc.priority);
		thread.start ();
	}
	
	public void stop ()
	{
		Enumeration<String>	enu;
		EventDesc		edesc;
		
		System.out.println (">> Stopping module [" + tdesc.preffix + "@" + tdesc.robotid + "]");
		
		// Unregister events
		enu		= recvs.keys ();
		while (enu.hasMoreElements ())
		{
			edesc	= recvs.get (enu.nextElement ());
			
			System.out.println ("\t>> Unregistering event " + edesc);
			edesc.unregister (linda);
		}
		
		thlock		= false;

		if (linda != null)
		{
			linda.stop ();
			linda		= null;
		}

		// Close the graphical windows opened by the module
		close_gfx_safe ();
		
		// Shut down threads and connections
		if (thread != null)			
		{
			try {
				thread.join(1000);
				//System.out.println("Terminado "+thread);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread		= null;
		}
	}
	
	public void run ()
	{
		boolean			do_step;
		long				tk, tk1, dt;
		
		if (state == INIT)						return;
		
		thlock		= true;
		tk			= System.currentTimeMillis () - tdesc.exectime;
		while (thlock)
		{
			// Measure current time
			tk1		= tk;
			tk		= System.currentTimeMillis ();
			dt		= tk - tk1;
			
			do_step	= false;
			if (tdesc.queued)
			{
				while (!queue.isEmpty ())
				{
					do_step = true;				
					unqueue ();
				}
			}
			else
				do_step = true;
			
			// Check Debug & Control status
			if (!running)			do_step = false;
			if (step)				running = false;
			
			// Perform execution
			if (do_step)			
			{
				long			ct;
				
				if (tdesc.polled)	poll ();
				
				step (tk);
				ct		= System.currentTimeMillis () - tk;
				if (debug)		System.out.println ("  ["+tdesc.preffix+"] Cycle time=" + dt + "ms \tCPU time=" + ct + "ms");		
			}
			
			// Measure the amount of time consumed during execution
			dt		= System.currentTimeMillis () - tk;
			if(dt<tdesc.exectime){
				try { Thread.sleep (tdesc.exectime - dt); } catch (Exception e) {
					System.out.println("StdThread: tdesc.exectime="+tdesc.exectime+" dt="+dt+" tk="+tk);
					e.printStackTrace(); }
			}
			Thread.yield ();
		}
	}
	
	public void notify (Tuple tuple)
	{
		if (state == INIT)					return;
		
		if (tdesc.queued)
			queue.add (tuple);
		else
			dispatch_notification (tuple);
	}	  
	
	public void unqueue ()
	{
		Tuple			tuple;
		
		tuple	= queue.extractFIFO ();
		if (tuple == null)					return;
		
		dispatch_notification (tuple);
	}	  
	
	public void dispatch_notification (Tuple tuple)
	{
		EventDesc		edesc;
		
		if (tuple.value == null)
		{
			System.out.println ("--[StdThd] Received null tuple (possibly, packet larger than 8Kb)");
			return;
		}
		
		edesc	= (EventDesc) recvs.get (tuple.key);
		if (edesc != null)
			edesc.notify (tuple.space, tuple.value);
		if (Tuple.CONFIG.equals (tuple.key))		configured = true;		// done with it, however it went
		
		Thread.yield ();
	}	  
	
	public void notify_execution (String space, ItemExecution item) 
	{
		switch (item.operation)
		{
		case ItemExecution.COMMAND:
			switch (item.command)
			{
			case ItemExecution.START:
				running	= true;
				step	= false;
				break;
			case ItemExecution.STOP:
				running	= false;
				step	= false;
				break;
			case ItemExecution.RESET:
				running	= false;
				step	= false;
				reset ();									// and whatever the module was in the middle of
				break;
			case ItemExecution.STEP:
				running	= true;
				step	= true;
				break;
			case ItemExecution.MANUAL:
				auto	= false;
				break;
			case ItemExecution.AUTO:
				auto	= true;
				break;
			default:
			}	
			break;
		case ItemExecution.DEBUG:
		default:
		}	
	}	
	
	// Template instance methods. Subclasses COULD implement
	public void poll ()										{ }

	/**
	 * Starts the module afresh, without it being taken out of the execution: it is
	 * stopped, whatever it was in the middle of is forgotten, and it goes on being
	 * there, registered for its events and ready to be started again (RESET).
	 *
	 * Whoever has something to forget says how; the default is that there is nothing.
	 */
	protected void reset ()									{ }

	/**
	 * Closes the windows the module opened when running with local graphics
	 * ({@link #localgfx}). Called from {@link #stop} on the event dispatch
	 * thread; the default implementation does nothing. Subclasses that open
	 * windows in {@link #initialise} should dispose them here, so that the
	 * windows do not survive the termination of the execution.
	 */
	protected void close_gfx ()								{ }

	/**
	 * Opens the windows the module shows when it runs with local graphics, as it
	 * opens them when it is configured: the other half of {@link #close_gfx}, so
	 * that they can be closed and opened again while it runs ({@link #graphics}).
	 * The default implementation opens nothing.
	 */
	protected void open_gfx ()								{ }

	/** Whether the module shows its windows (local graphics). */
	public final boolean graphics ()						{ return localgfx; }

	/**
	 * Shows or hides the windows of the module while it runs: they are opened
	 * ({@link #open_gfx}) or closed ({@link #close_gfx}), and the module draws on
	 * them or not ({@link #localgfx}). A module that has not been configured yet
	 * opens them when it is, as it does with local graphics.
	 */
	public void graphics (boolean on)
	{
		if (on == localgfx)						return;
		if (!on)
		{
			localgfx	= false;					// nothing more is drawn on them
			close_gfx_safe ();
			return;
		}
		if (!isConfigured ())					{ localgfx = true;	return; }

		// opened as the module opens them, away from the event thread (some of them
		// wait for it), and drawn on once they are there
		Thread		t = new Thread (new Runnable ()
		{
			public void run ()
			{
				try { open_gfx (); } catch (Exception e) { e.printStackTrace (); }
				localgfx	= true;
			}
		}, "TC-Gfx-" + ((tdesc != null) ? tdesc.preffix : "module"));
		t.setDaemon (true);
		t.start ();
	}

	/** Runs {@link #close_gfx} on the event dispatch thread, ignoring failures. */
	protected void close_gfx_safe ()
	{
		Runnable		close = new Runnable ()
		{
			public void run ()
			{
				try { close_gfx (); } catch (Exception e) { e.printStackTrace (); }
			}
		};
		try
		{
			if (javax.swing.SwingUtilities.isEventDispatchThread ())
				close.run ();
			else
				javax.swing.SwingUtilities.invokeAndWait (close);
		}
		catch (Exception e) { e.printStackTrace (); }
	}

	/** Disposes a window if it exists (helper for {@link #close_gfx}). */
	protected static void dispose_window (java.awt.Window win)
	{
		if (win != null)		win.dispose ();
	}

	// Abstract instance methods. Subclasses MUST implement
	public abstract void step (long ctime);
	protected abstract void initialise (ModuleConfig config);

	/** Configuration of this module in the deployment. */
	public ModuleConfig getConfig ()		{ return config; }
}

