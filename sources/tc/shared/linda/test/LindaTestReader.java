/*
 * Created on 28-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.test;

import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaTestReader extends StdThread
{
	static public final int				DTIME	= 200;
	
	protected Tuple						tdata;
	public int							count	= 0;
	
	public LindaTestReader (Properties props, Linda linda)
	{
		super (props, linda);
	}

	// Instance methods
	protected void initialise (Properties props)
	{
		// Prepare Linda data structures
		tdata		= new Tuple (Tuple.DATA, null);
	}
	
	public void step (long ctime) 
	{ 
		Tuple		tuple;
		
		// Read sensor data from the Linda space
		tuple	= linda.read (tdata);
		if (tuple != null)
			count ++;
		
		// Wait for control cycle to finish
		try { Thread.sleep (DTIME); } catch (Exception e) { }	
	}
	
	public void notify_config (String space, ItemConfig item)
	{
	}
}
