/*
 * (c) 2001 Humberto Martinez
 */
 
package tc.modules;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.world.World;
import tc.vrobot.*;
import tclib.utils.fusion.*;

public abstract class Controller extends StdThread
{
	protected World					world;					// A priori world model
	protected RobotDesc				rdesc;					// Robot description
	protected FusionDesc			fdesc;					// Fusion method description
	protected LPS					lps;						// Local Perceptual Space
	
	// Linda related variables
	protected Tuple					mtuple;
	protected ItemMotion			mitem;
	protected Tuple					btuple;
	protected ItemBehResult			bitem;

	// Constructors
	protected Controller (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		// Setup local stuff
		mitem	= new ItemMotion ();
		mtuple	= new Tuple (Tuple.MOTION, mitem);
		bitem	= new ItemBehResult ();
		btuple	= new Tuple (Tuple.BEHRESULT, bitem);
	}

	/**
	 * Commands the platform: how fast it is to go forward, how fast sideways and how
	 * fast it is to turn ([m/s], [m/s], [rad/s]). A platform that cannot be driven
	 * sideways makes nothing of the lateral velocity, which is a matter of its
	 * kinematics model.
	 */
	public void setMotion (double vlin, double vlat, double vrot)
	{
		if (mitem == null)		return;
		if (!sane (vlin, vlat, vrot))	{ vlin = 0.0;	vlat = 0.0;	vrot = 0.0; }
		
		mitem.set (vlin, vlat, vrot, System.currentTimeMillis ());
		linda.write (mtuple);
	}
	
	public void setMotion (int mode, double vlin, double vlat, double vrot)
	{
		if (mitem == null)		return;
		if (!sane (vlin, vlat, vrot))	{ vlin = 0.0;	vlat = 0.0;	vrot = 0.0; }
		
		mitem.set (mode, vlin, vlat, vrot, System.currentTimeMillis ());
		linda.write (mtuple);
	}
	
	/**
	 * Whether a command can be carried out at all: a controller that has divided by
	 * zero somewhere asks for a velocity that is not a number, and a robot commanded
	 * with one has no pose from then on, which stops the whole simulation (the
	 * camera of a robot is the first thing to refuse it). Such a command is dropped
	 * and the robot stands still, said out loud the first few times.
	 */
	protected boolean sane (double vlin, double vlat, double vrot)
	{
		if (Double.isFinite (vlin) && Double.isFinite (vlat) && Double.isFinite (vrot))		return true;
		
		if (insane < INSANE_SAID)
			System.out.println ("  [CNTL] " + getClass ().getSimpleName () + " asked for vlin " + vlin + ", vlat " + vlat
								+ " and vrot " + vrot + ": no motion commanded");
		insane++;
		return false;
	}
	
	/** How many times a command that is not a number is said out loud before it is only counted. */
	static protected final int		INSANE_SAID		= 5;
	
	protected int					insane;					// how many such commands there have been
		
	public void setResult (int result, int reason, long serial)
	{
		if (bitem == null)		return;
		
		bitem.set (result, reason, serial, System.currentTimeMillis ());
		linda.write (btuple);
	}

	/** Nothing is commanded and nothing has been complained about yet (RESET). */
	protected void reset ()
	{
		insane	= 0;
		setMotion (0.0, 0.0, 0.0);
	}

	public void notify_config (String space, ItemConfig item)
	{
		if (item.props_robot != null)
		{
			rdesc		= new RobotDesc (item.props_robot);
			fdesc		= new FusionDesc (item.props_robot);
		    
		    state 		= RUN;
	    }
		
		if (item.world != null)
			world = World.fromJsonText (item.world);
	}
	
	public void notify_execution (String space, ItemExecution item) 
	{		
		super.notify_execution (space, item);
		
		switch (item.operation)
		{
		case ItemExecution.DEBUG:
			debug	= item.dbg_controller;
			break;
		case ItemExecution.COMMAND:
		default:
		}	
	}	
	
	public void notify_lps (String space, ItemLPS item) 
	{ 
		if (state != RUN)				return;
		
		lps		= item.lps;
		
		try
		{
			if (tdesc.passive)				step (System.currentTimeMillis ());
		} catch (Exception e) { e.printStackTrace (); }
	}	
}

