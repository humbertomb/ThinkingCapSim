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
	protected int				mode;
	protected boolean			running		= false;
	protected boolean			step		= false;
	protected boolean			debug		= false;
	protected boolean			auto		= true;
	protected boolean			localgfx	= false;
	
	private Properties props;
	
	// Constructors
	public StdThread (Properties props, Linda linda)
	{
		this.linda	= linda;
		this.queue	= new LindaQueue ();
		this.state	= INIT;
		
		this.recvs	= new Hashtable<String,EventDesc> (MAX_EVTS);
		
		this.props	= props;
//		initialise (props);
	}
	
	// Instance methods
	public void setTDesc (ThreadDesc tdesc)
	{ 
		String			event;
		EventDesc		edesc;
		ConfigDesc		cdesc;
		DebugDesc		ddesc;
		StringTokenizer	st;
		
		this.tdesc	= tdesc; 
		
		// Initialise user-defined event receivers
		if (tdesc.connects != null)
		{
			st = new StringTokenizer (tdesc.connects, ",");
			while (st.hasMoreTokens ())
			{
				event	= st.nextToken ();
				edesc	= new EventDesc (this, linda, event);
				
				System.out.println ("\t>> Registering event " + edesc);
				recvs.put (edesc.key, edesc);
			}
		}
		
		// Initialise standard event receivers (the ConfigDesc MUST always be the last one)
		ddesc		= new DebugDesc (this, linda);
		recvs.put (ddesc.key, ddesc);
		cdesc		= new ConfigDesc (this, linda);
		recvs.put (cdesc.key, cdesc);
		
		// Initialise other processing options
		localgfx	= tdesc.cangfx;
		state 		= CONFIG; 

		initialise (props);
	}
	
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
		Enumeration		enu;
		EventDesc		edesc;
		
		System.out.println (">> Stopping module [" + tdesc.preffix + "@" + tdesc.robotid + "]");
		
		// Unregister events
		enu		= recvs.keys ();
		while (enu.hasMoreElements ())
		{
			edesc	= (EventDesc) recvs.get ((String) enu.nextElement ());
			
			System.out.println ("\t>> Unregistering event " + edesc);
			edesc.unregister (linda);
		}
		
		thlock		= false;
		
		if (linda != null)
		{
			linda.stop ();
			linda		= null;
		}
		
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
		
		Thread.yield ();
	}	  
	
	public void notify_debug (String space, ItemDebug item) 
	{
		switch (item.operation)
		{
		case ItemDebug.COMMAND:
			switch (item.command)
			{
			case ItemDebug.START:
				running	= true;
				step	= false;
				break;
			case ItemDebug.STOP:
			case ItemDebug.RESET:
				running	= false;
				step	= false;
				break;
			case ItemDebug.STEP:
				running	= true;
				step	= true;
				break;
			case ItemDebug.MANUAL:
				auto	= false;
				break;
			case ItemDebug.AUTO:
				auto	= true;
				break;
			default:
			}	
			break;
		case ItemDebug.DEBUG:
		case ItemDebug.MODE:
		default:
		}	
	}	
	
	// Template instance methods. Subclasses COULD implement
	public void poll ()										{ }
	
	// Abstract instance methods. Subclasses MUST implement
	public abstract void step (long ctime);
	protected abstract void initialise (Properties props);
}

