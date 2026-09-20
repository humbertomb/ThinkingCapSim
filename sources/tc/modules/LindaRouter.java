/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 */
 
package tc.modules;

import tc.shared.linda.*;

public class LindaRouter implements LindaListener
{
	protected String 				robotid;
	protected Linda					lindalocal;				// Local Linda for this robot.
	protected Linda					lindaglobal;			// Global Linda for multirrobot.
	
	protected boolean				debug		= false;
	
	// Constructors
	public LindaRouter (String robotid, Linda lindalocal, Linda lindaglobal)
	{
		this.robotid		= robotid;
		this.lindalocal		= lindalocal;
		this.lindaglobal	= lindaglobal;
		
		initialise (this);
	}

	// Instance methods
	protected void initialise (LindaListener listener)
	{		
		// Register LOCAL linda listeners
		lindalocal.register (new Tuple (Tuple.CONFIG), listener);
		lindalocal.register (new Tuple (Tuple.STATUS), listener);
		lindalocal.register (new Tuple (Tuple.GOAL), listener);
		lindalocal.register (new Tuple (Tuple.BEHINFO), listener);
		
		// Register GLOBAL linda listeners
		lindaglobal.register (new Tuple (robotid, Tuple.EXECUTION, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.PLAN, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.MOTION, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHRULES, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHNAME, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHDEBUG, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.DELROBOT, null), listener);
	}
	
	public void notify (Tuple tuple)
    {
    	if ((tuple.key == null) || (tuple.value == null))		return;
			
    	if (debug) System.out.print ("  [Router] <="+tuple);
    	
		// Tuples from local to global
		if (tuple.key.equals (Tuple.CONFIG) || tuple.key.equals (Tuple.STATUS) 
				|| tuple.key.equals (Tuple.GOAL) || tuple.key.equals (Tuple.BEHINFO))
		{
			tuple.space	= robotid;
			lindaglobal.write (tuple);
			if (debug) System.out.println (" G=>"+tuple);
		}			
		// Tuples from global to local (broadcast)
		else if (tuple.key.equals (Tuple.EXECUTION) || tuple.key.equals(Tuple.DELROBOT))
		{	
			lindalocal.write (tuple);
			if (debug) System.out.println (" L=>"+tuple);
		}
		// Tuples from global to local (filtered)
		else if (tuple.key.equals (Tuple.PLAN) || tuple.key.equals (Tuple.MOTION) 
				|| tuple.key.equals (Tuple.BEHNAME) || tuple.key.equals (Tuple.BEHRULES) 
				|| tuple.key.equals (Tuple.BEHDEBUG))
		{			
			if (tuple.space.equals (robotid))
			{	
				lindalocal.write (tuple);
				if (debug) System.out.println (" L=>"+tuple);
			}
			else
				if (debug) System.out.println (" <Discarded>");
		}
		else
			System.out.println ("--[Router] Un-requested tuple KEY received");	
	}	  
}

