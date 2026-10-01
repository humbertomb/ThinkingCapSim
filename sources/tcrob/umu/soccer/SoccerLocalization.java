/*
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import tclib.utils.pos.Position;
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
 * The field is the one of the RoboCup of 2007 (the constants of Localisation).
 *
 * With local graphics, a window shows the position, the reduced LPS, the method
 * and the field (SoccerLocalizationWindow), updated with every LPS.
 */
public class SoccerLocalization extends Navigation
{
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
		
		if (!initialised || (item == null) || (item.lps == null))		return;
		
		synchronized (loc)												
		{
			loclps.updateFromLps (item.lps);
			odom.setOdometry (item.lps.odom);
			loc.updateMotionAndSensors (odom, loclps);
			setPosition (loc.getGs ());
		}

		// what the method makes of it, to the window (with where the robot really is, the ground truth of the simulation)
		if (win != null)
			win.update (loc, loclps, (item.lps.real != null) ? new Position (item.lps.real) : null);

		// where the robot is, to the rest of the architecture
		nitem.set (pos, (item.timestamp != null) ? item.timestamp.longValue () : System.currentTimeMillis ());
		linda.write (ntuple);
	}	

	/**
	 * The position of the robot from the estimate of the method (mm, rad), in m and
	 * rad. Its quality is the one of the method [0..1], and its uncertainty a
	 * covariance (m, m, rad) with half of the box of the estimate as the standard
	 * deviation of x and y, and its angular spread as the one of the heading.
	 */
	protected void setPosition (Gs gs)
	{
		double		sx, sy, sa;

		sx		= gs.getDX () * 0.5 / 1000.0;
		sy		= gs.getDY () * 0.5 / 1000.0;
		sa		= gs.getDTheta ();

		pos.set (gs.getX () / 1000.0, gs.getY () / 1000.0, gs.getTheta (), true);
		pos.set (new double[][] { { sx * sx, 0.0, 0.0 }, { 0.0, sy * sy, 0.0 }, { 0.0, 0.0, sa * sa } });
		pos.quality	= Math.max (0.0, Math.min (1.0, gs.getQuality ()));
	}
		
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
								
		loc	= new GridFMarkov (100, 0.08f, (float) (1.0*Angles.DTOR));
		odom.restart ();												// the displacements of the new method start from the next LPS

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

