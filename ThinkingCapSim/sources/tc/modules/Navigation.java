/*
 * (c) 2001 Humberto Martinez
 */
 
package tc.modules;

import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.world.*;
import tc.vrobot.*;

import tclib.utils.fusion.*;

public abstract class Navigation extends StdThread
{
	protected World				world;					// A priori world model
	protected RobotDesc			rdesc;					// Robot description
	protected FusionDesc			fdesc;					// Fusion method description
	protected LPS				lps;						// Local Perceptual Space

	protected long				time_upd;

	// Constructors
	public Navigation (Properties props, Linda linda)
	{
		super (props, linda);
	}

	// Instance methods
	public void notify_config (String space, ItemConfig item)
	{
		if (item.props_robot != null)
		{
			rdesc		= new RobotDesc (item.props_robot);
			fdesc		= new FusionDesc (item.props_robot);
			
			state		= RUN;
		}
		
		if (item.props_world != null)
			world		= new World (item.props_world);
	}
	
	public void notify_debug (String space, ItemDebug item) 
	{
	
		super.notify_debug (space, item);
		
		switch (item.operation)
		{
		case ItemDebug.DEBUG:
			debug	= item.dbg_navigation;
			break;
		case ItemDebug.MODE:
			mode	= item.mode_navigation;
			break;
		case ItemDebug.COMMAND:
		default:
		}	
	}	
	
	public void notify_lps (String space, ItemLPS item) 
	{ 
		if (state != RUN)				return;
		
		lps			= item.lps;
		time_upd		= item.timestamp.longValue ();
		
		try
		{
			if (tdesc.passive)				step (System.currentTimeMillis ());
		} catch (Exception e) { e.printStackTrace (); }
	}	
}

