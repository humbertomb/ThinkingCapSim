/*
 * Created on 11-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.test;

import java.util.*;

import tc.vrobot.*;
import tc.runtime.thread.*;
import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaTestConsumer extends StdThread
{
	protected RobotData					data;
	protected long						ltime;
	public double						tavg;
	public int							count	= 0;
	
	public LindaTestConsumer (Properties props, Linda linda)
	{
		super (props, linda);
	}

	// Instance methods
	protected void initialise (Properties props)
	{
		ltime	= System.currentTimeMillis ();
	}
	
	public void step (long ctime) 
	{ 
	}
	
	public void notify_data (String space, ItemData item)
	{
		long			time, dtime;
		
		// Process data
		data		= item.data;
		count ++;
		
		// Compute time increment
		time		= System.currentTimeMillis ();
		dtime	= time - ltime;
		ltime	= time;
		tavg		+= (double) dtime;

//		System.out.println ("CONSUMER dtime="+dtime);
	}
	
	public void notify_config (String space, ItemConfig item)
	{
	}
}
