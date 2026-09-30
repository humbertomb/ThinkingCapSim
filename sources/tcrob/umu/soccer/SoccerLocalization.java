/*
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import devices.pos.Position;
import tc.modules.Navigation;
import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemLPS;
import tc.shared.linda.ItemNavigation;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tcrob.umu.soccer.gm.*;
import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.fmk.*;
import tcrob.umu.soccer.gui.SoccerLocalizationWindow;
import wucore.utils.math.Angles;

/**
 * The localisation of a soccer robot on the field, with the methods of
 * ChaosManager (tcrob.umu.soccer.gm), from the LPS of the robot.
 *
 * <pre>
 *   FIELD   the world model of the field the methods work on (./conf/fields/robocup2007.ini)
 * </pre>
 *
 * With local graphics, a window shows the position, the reduced LPS, the method
 * and the field (SoccerLocalizationWindow), updated with every LPS.
 */
public class SoccerLocalization extends Navigation
{
	static public final String		FIELD			= "./conf/fields/robocup2007.ini";

	// Navigation structures
	protected Localisation			loc;
	protected Odometry				odom = new Odometry ();
	protected LocLps				loclps = new LocLps ();
	
	protected Position				pos;
	
	// Linda data structures
	protected Tuple					ntuple;
	protected ItemNavigation		nitem;

	// Additional local variables
	protected boolean				initialised		= false;
	protected String				field			= FIELD;

	// Local graphics
	protected SoccerLocalizationWindow	win;

	// Constructors
	public SoccerLocalization (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
		
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		// Initialise Linda related structures
		nitem		= new ItemNavigation ();
		ntuple		= new Tuple (Tuple.NAVIGATION, nitem);
		
		// Initialise other local stuff
		pos			= new Position ();
		if (cfg.get ("FIELD") != null)		field = cfg.get ("FIELD");
	}
	
	/** The window of the localisation goes away with the module. */
	protected void close_gfx ()
	{
		dispose_window (win);
		win		= null;
	}

	public void step (long ctime)
	{
		if (!initialised)		return;

	}
	
	public void notify_lps (String space, ItemLPS item) 
	{ 
		super.notify_lps (space, item);
		if (!initialised || (item == null) || (item.lps == null))		return;		// no method of localisation yet (it comes with the configuration)
		
		synchronized (loc)												// the window reads it on another thread
		{
			loclps.updateFromLps (item.lps);
			loc.updateMotionAndSensors (odom, loclps);
		}

		// what the method makes of it, to the window (with where the robot really is, the ground truth of the simulation)
		final SoccerLocalizationWindow	shown = win;

		if (shown != null)
			shown.update (loc, loclps, (item.lps.real != null) ? new Position (item.lps.real) : null);
		
		// TODO
		// Update pos variable
		// Send ItemNavigation
	}	
		
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
								
		loc	= new GridFMarkov (field, 500, 0.08f, (float) (1.0*Angles.DTOR));

		// with local graphics, the window of the localisation
		if (localgfx && (win == null))
			javax.swing.SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;

					win = new SoccerLocalizationWindow (hostFrame (), "Chaos Localisation [" + getConfig ().robot () + "]");
					win.setVisible (true);
				}
			});

		initialised = true;
	}
}

