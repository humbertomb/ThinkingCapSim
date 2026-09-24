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
import wucore.utils.logs.LogPlot;

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
	
	/**
	 * Opens the plot of what the controller commands, in the units it is commanded
	 * in: the two velocities against the left scale in metres a second and the turn
	 * rate against the right one in degrees a second, each as far as this platform
	 * goes that way. It is the very same command the virtual robot plots, and it is
	 * read off the picture the same way, without anybody having to know what the
	 * most the platform does is to make sense of a share of it.
	 *
	 * The ranges come from the platform, so this is opened once its description has
	 * arrived (notify_config), which is where a controller opens its windows.
	 *
	 * @param plot		the plot to open
	 * @param labels	what the lines are called, vlin, vlat and vrot in that order
	 */
	protected void openMotionPlot (LogPlot plot, String[] labels)
	{
		if (plot == null)					return;

		mplot	= plot;
		plot.setRightAxis (2, "deg/s");
		scaleMotionPlot ();
		plot.open (labels);
	}

	/**
	 * What the scales of that plot cover: as far as this platform goes each way. A
	 * controller opens its windows before the description of the platform has
	 * arrived, so until it does the scales are left to the values themselves, and
	 * they are put right the moment it comes (notify_config).
	 */
	protected void scaleMotionPlot ()
	{
		if (mplot == null)					return;
		if ((rdesc == null) || (rdesc.model == null))
		{
			mplot.rescale (0.0, 0.0, 0.0, 0.0);
			return;
		}

		double		vmax = Math.max (rdesc.model.Vmax, rdesc.model.Umax);
		double		rmax = Math.toDegrees (rdesc.model.Rmax);

		mplot.rescale (-vmax, vmax, -rmax, rmax);
	}

	/** One cycle of that plot: the command as it was given, the turn rate in degrees. */
	protected void motionValues (double[] buffer, double vlin, double vlat, double vrot)
	{
		if ((buffer == null) || (buffer.length < 3))		return;

		buffer[0]	= vlin;										// [m/s]
		buffer[1]	= vlat;										// [m/s]
		buffer[2]	= Math.toDegrees (vrot);					// [deg/s]
	}

	/** How many times a command that is not a number is said out loud before it is only counted. */
	static protected final int		INSANE_SAID		= 5;
	
	protected int					insane;					// how many such commands there have been
	protected LogPlot				mplot;					// the plot of what is commanded, if anybody asked for one
		
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
			scaleMotionPlot ();							// now it is known how far the platform goes
		    
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

