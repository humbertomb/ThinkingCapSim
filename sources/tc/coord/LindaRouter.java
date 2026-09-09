/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 */
 
package tc.coord;

import tc.modules.*;
import tc.shared.linda.*;

public class LindaRouter implements LindaListener
{
	protected MonitorData			mdata;
	protected Tuple					mtuple;
	protected ItemMonitor			mitem;
	
	protected String 				robotid;
	protected Linda					lindalocal;				// Local Linda for this robot.
	protected Linda					lindaglobal;			// Global Linda for multirrobot.
	
	protected long					ltime;					// Time ot last update of the global linda
	protected long					timeout		= 1500;		// Timeout for updating the global linda
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
		// Initialise Linda structures
		mdata		= new MonitorData ();
		mitem		= new ItemMonitor ();
		mtuple		= new Tuple (Tuple.MONITOR, mitem);
		
		// Register LOCAL linda listeners
		lindalocal.register (new Tuple (Tuple.CONFIG), listener);
		lindalocal.register (new Tuple (Tuple.LPS), listener);
		lindalocal.register (new Tuple (Tuple.STATUS), listener);
		lindalocal.register (new Tuple (Tuple.GOAL), listener);
		lindalocal.register (new Tuple (Tuple.BEHINFO), listener);
		lindalocal.register (new Tuple (Tuple.GUISVC), listener);		
		lindalocal.register (new Tuple (Tuple.GUIDATA), listener);		
		
		// Register GLOBAL linda listeners
		lindaglobal.register (new Tuple (robotid, Tuple.DEBUG, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.PLAN, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.MOTION, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.LINDACTRL, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHRULES, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHNAME, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.BEHDEBUG, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.GUICTRL, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.DELROBOT, null), listener);
		lindaglobal.register (new Tuple (robotid, Tuple.PALLETCTRL, null), listener);
	}
	
	public void notify (Tuple tuple)
    {
    	if ((tuple.key == null) || (tuple.value == null))		return;
			
    	if (debug) System.out.print ("  [Router] <="+tuple);
    	
		// Tuples for Linda control
		if (tuple.key.equals (Tuple.LINDACTRL))
			process_linda (tuple);			
		// Tuples from local to global (polled)
		else if (tuple.key.equals (Tuple.LPS))
		{
			if (System.currentTimeMillis () - ltime < timeout)
				return;
				
			ltime	= System.currentTimeMillis ();
			
			mdata.update (((ItemLPS) tuple.value).lps);
			mitem.set (mdata, null, ltime);		
			mtuple.space	= robotid;
			lindaglobal.write (mtuple);
			if (debug) System.out.println (" G=>"+tuple);
		}			
		// Tuples from local to global
		else if (tuple.key.equals (Tuple.CONFIG) || tuple.key.equals (Tuple.STATUS) 
				|| tuple.key.equals (Tuple.GOAL) || tuple.key.equals (Tuple.BEHINFO) 
				|| tuple.key.equals (Tuple.GUISVC) || tuple.key.equals (Tuple.GUIDATA))
		{
			tuple.space	= robotid;
			lindaglobal.write (tuple);
			if (debug) System.out.println (" G=>"+tuple);
		}			
		// Tuples from global to local (broadcast)
		else if (tuple.key.equals (Tuple.DEBUG) || tuple.key.equals(Tuple.DELROBOT) 
				|| tuple.key.equals(Tuple.PALLETCTRL) )
		{	
			lindalocal.write (tuple);
			if (debug) System.out.println (" L=>"+tuple);
		}
		// Tuples from global to local (filtered)
		else if (tuple.key.equals (Tuple.PLAN) || tuple.key.equals (Tuple.MOTION) 
				|| tuple.key.equals (Tuple.BEHNAME) || tuple.key.equals (Tuple.BEHRULES) 
				|| tuple.key.equals (Tuple.BEHDEBUG) || tuple.key.equals (Tuple.GUICTRL))
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

	protected void process_linda (Tuple tuple)
	{
		ItemLindaCtrl		lctrl;
		
		if (debug) System.out.println (" <Processing Linda Event>");
		
		lctrl	= (ItemLindaCtrl) tuple.value;
		switch (lctrl.cmd)
		{
		case ItemLindaCtrl.TIMEOUT:
			timeout	= lctrl.param;
			break;
			
		case ItemLindaCtrl.DUMPREG:
		case ItemLindaCtrl.DUMPSPC:
		case ItemLindaCtrl.DELETE:
		default:
			lindalocal.write (tuple);
		}
	}
}

