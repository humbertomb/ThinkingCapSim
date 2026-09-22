/*
 * (c) 2001-2002 Humberto Martinez
 */
 
package tc.modules;

import java.util.HashMap;
import java.util.Map;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.world.*;
import tc.vrobot.*;

import tclib.utils.fusion.*;

/**
 * A perception module of a robot. Each one has an LPS, but the modules of a
 * robot running in the same program can work on one and the same: the one of
 * its owner (a module that is not a guest, and that keeps and publishes it).
 * A guest ({@link #lps_guest}) works on the LPS of the owner of its robot as
 * soon as there is one ({@link #lps_current}), and on its own until then.
 * Whoever touches a shared LPS does it holding its lock.
 */
public abstract class Perception extends StdThread
{
	// The LPS of the owner of each robot (robot id -> LPS)
	static private final Map<String, LPS>	SHARED	= new HashMap<String, LPS> ();

	protected World				world;			// A priori world model
	protected RobotDesc			rdesc;			// Robot description
	protected FusionDesc			fdesc;			// Fusion method description
	protected Fusion				fusion;			// Sensor fusion method
	protected LPS				lps;				// Local Perceptual Space

	// Linda related variables
	protected String				robotid;
	protected Tuple				ltuple;
	protected ItemLPS			lstore;
	protected long				stime 		= 0;
		
	// Robot and perception related variables
	protected RobotData			data			= null;

	// Constructors
	protected Perception (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
		
		robotid 		= cfg.robot ();
	}

	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{
		// Initialise local structures
		stime			= System.currentTimeMillis ();
		
		// Prepare Linda data structures
		lstore		= new ItemLPS ();
		ltuple		= new Tuple (Tuple.LPS, lstore);
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		// A new robot configuration is available
		if (item.props_robot != null)
		{
			rdesc	= new RobotDesc (item.props_robot);
			fdesc	= new FusionDesc (item.props_robot);
			fusion	= new Fusion (rdesc, fdesc);
			lps		= new LPS (rdesc, fdesc);
			if (!lps_guest ())
				synchronized (SHARED)		{ SHARED.put (robotid, lps); }
			
			state	= RUN;
		}
		
		// A new world model is available
		if (item.world != null)
			world	= World.fromJsonText (item.world);
	}
	
	/** Whether this module works on the LPS of the owner of its robot (true) or keeps its own (false, the default). */
	protected boolean lps_guest ()							{ return false; }

	/** The LPS the owner of a robot keeps; null if there is none. */
	static public LPS shared_lps (String robotid)
	{
		synchronized (SHARED)		{ return SHARED.get (robotid); }
	}

	/**
	 * The LPS to work on: for a guest, the one of the owner of its robot when
	 * there is one (it may change, with a new configuration); otherwise its own.
	 */
	protected LPS lps_current ()
	{
		LPS		shared;

		if (lps_guest () && ((shared = shared_lps (robotid)) != null))
			return shared;
		return lps;
	}

	public void notify_execution (String space, ItemExecution item) 
	{
		super.notify_execution (space, item);
		
		switch (item.operation)
		{
		case ItemExecution.DEBUG:
			debug	= item.dbg_perception;
			break;
		case ItemExecution.COMMAND:
		default:
		}	
	}	

	public void notify_sensors (String space, ItemSensors item) 
	{
		if (state != RUN)				return;
		
		if (data != null)		
		{
			System.out.println ("--[Per]: Discarding robot data. Unprocessed event");
			return;
		}
		
		data = item.data;
		
		try
		{
			if (tdesc.passive)				step (System.currentTimeMillis ());
		} catch (Exception e) { e.printStackTrace (); data = null;}
	}	
}

