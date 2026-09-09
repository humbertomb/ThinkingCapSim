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
public class LindaTestProducer extends StdThread
{
	//A partir de DTIME>250, no pierde paquetes TCP
	static public final int				DTIME	= 100;
	
	protected Tuple						tdata;
	protected ItemData					sdata;
	protected RobotData					data;	
	protected long						ltime;
	public int							count	= 0;
	
	public LindaTestProducer (Properties props, Linda linda)
	{
		super (props, linda);
	}

	// Instance methods
	protected void initialise (Properties props)
	{
		// Prepare Linda data structures
		sdata	= new ItemData ();
		tdata	= new Tuple (Tuple.DATA, sdata);
		data		= new RobotData (new RobotDesc (new Properties ()));

		ltime	= System.currentTimeMillis ();
	}
	
	public void step (long time) 
	{ 
//		long			dtime;
		
		// Compute time increment
//		dtime	= time - ltime;
		ltime	= time;
		// System.out.println ("PRODUCER dtime="+dtime);
		
		// Write sensor data to the Linda space
		sdata.set (data, time);
		if (linda.write (tdata))
			count ++;
		
		// Wait for control cycle to finish
		try { Thread.sleep (DTIME); } catch (Exception e) { }
	}
	
	public void notify_config (String space, ItemConfig item)
	{
	}
}
