/*
 * (c) 2001-2002 Humberto Martinez
 */
 
package tc.modules;

import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.world.*;
import tc.vrobot.*;

import tclib.utils.fusion.*;

public abstract class Perception extends StdThread
{
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
	protected Perception (Properties props, Linda linda)
	{
		super (props, linda);
		
		robotid 		= props.getProperty ("ROBNAME");
	}

	// Instance methods
	protected void initialise (Properties props)
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
			
			state	= RUN;
		}
		
		// A new world model is available
		if (item.props_world != null)
			world	= new World (item.props_world);
	}
	
	public void notify_debug (String space, ItemDebug item) 
	{
		super.notify_debug (space, item);
		
		switch (item.operation)
		{
		case ItemDebug.DEBUG:
			debug	= item.dbg_perception;
			break;
		case ItemDebug.MODE:
			mode	= item.mode_perception;
			break;
		case ItemDebug.COMMAND:
		default:
		}	
	}	

	public void notify_data (String space, ItemData item) 
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

