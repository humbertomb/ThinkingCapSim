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
import tcrob.umu.soccer.gm.hybrid.*;
import tcrob.umu.soccer.gm.kalman.*;
import tcrob.umu.soccer.gm.NKFMK.*;
import tcrob.umu.soccer.gm.particle.*;
import tcrob.umu.soccer.gm.srl.*;
import tcrob.umu.soccer.gui.SoccerLocalizationWindow;
import wucore.utils.math.Angles;

/**
 * The localisation of a soccer robot on the field, with the methods of
 * ChaosManager (tcrob.umu.soccer.gm), from the LPS of the robot.
 * The field is the one of the RoboCup of 2007 (the constants of Localisation).
 *
 * The method is one of {@link #METHODS} ({@link #METHOD} to start with), made
 * with the initial parameters of the static variables of this class (see
 * {@link #PARAMS}); it can be changed while it runs ({@link #method}).
 *
 * With local graphics, a window shows the position, the reduced LPS, the method
 * and the field (SoccerLocalizationWindow), updated with every LPS, and lets the
 * method and its initial parameters be chosen.
 */
public class SoccerLocalization extends Navigation
{
	/** The localisation methods there are (all those of tcrob.umu.soccer.gm). */
	static public final String[]	METHODS			= { "GridFMarkov", "Kalman", "KFMarkov", "NKFMK", "Particles", "SensorResetting" };
	/** The method the module starts with. */
	static public String			METHOD			= "Particles";

	/* ---- the initial parameters of each method ---- */

	// GridFMarkov (fuzzy Markov grid)
	static public int				GFMK_CELL		= 100;			// side of a cell of the grid (mm)
	static public double			GFMK_BLUR_POS	= 0.08;			// blurring of the position at every motion
	static public double			GFMK_BLUR_ANGLE	= 1.0;			// blurring of the heading at every motion (deg)

	// Kalman (extended Kalman filter)
	static public int				EKF_TOLERANCE	= 0;			// to observations far from the estimate: deactivated, low, medium, high
	static public double			EKF_ODO_LIN		= 0.3;			// noise of the odometry, linear (fraction of the displacement)
	static public double			EKF_ODO_ROT		= 0.3;			// noise of the odometry, rotational (fraction of the turn)
	static public double			EKF_OBS_DIST	= 1000.0;		// noise of the distance of what is seen (mm)
	static public double			EKF_OBS_ANGLE	= 20.0;			// noise of the bearing of what is seen (deg)

	// KFMarkov (fuzzy Markov grid and extended Kalman filter)
	static public int				KFMK_CELL		= 100;
	static public double			KFMK_BLUR_POS	= 0.08;
	static public double			KFMK_BLUR_ANGLE	= 1.0;
	static public int				KFMK_TOLERANCE	= 0;
	static public double			KFMK_ODO_LIN	= 0.3;
	static public double			KFMK_ODO_ROT	= 0.3;
	static public double			KFMK_OBS_DIST	= 1000.0;
	static public double			KFMK_OBS_ANGLE	= 20.0;

	// NKFMK (fuzzy Markov grid and several extended Kalman filters)
	static public int				NKFMK_CELL		= 100;
	static public double			NKFMK_BLUR_POS	= 0.08;
	static public double			NKFMK_BLUR_ANGLE= 1.0;
	static public int				NKFMK_TOLERANCE	= 0;
	static public double			NKFMK_ODO_LIN	= 0.3;
	static public double			NKFMK_ODO_ROT	= 0.3;
	static public double			NKFMK_OBS_DIST	= 1000.0;
	static public double			NKFMK_OBS_ANGLE	= 20.0;
	static public int				NKFMK_EKFS		= 10;			// how many Kalman filters
	static public int				NKFMK_MIN_AGE	= 5;
	static public double			NKFMK_CHI		= 500.0;		// threshold of the chi-square test
	static public int				NKFMK_POSITION	= 0;			// how the position is decided: FMK probability, FMK-CHI, mean of the EKFs
	static public int				NKFMK_RESET_EKF	= 30;			// on resetting, the share put at the last position (%)
	static public int				NKFMK_RESET_FMK	= 30;			// on resetting, the share put where the grid says (%)

	// Particles (particle filter)
	static public int				PF_SAMPLES		= 100;
	static public double			PF_SPREAD		= 500.0;		// how far the particles are spread round where they start (mm)
	static public double			PF_SPREAD_ANGLE	= 40.0;			// and their headings (deg)

	// SensorResetting (sensor resetting localisation)
	static public int				SRL_SAMPLES		= 500;

	/**
	 * The initial parameters of each method, as the window shows them: the method,
	 * the static variable of this class, its label and, for those that are a
	 * choice, the options (separated by commas) the value is the index of.
	 */
	static public final String[][]	PARAMS			=
	{
		{ "GridFMarkov",		"GFMK_CELL",		"Cell size (mm)" },
		{ "GridFMarkov",		"GFMK_BLUR_POS",	"Position blurring" },
		{ "GridFMarkov",		"GFMK_BLUR_ANGLE",	"Angle blurring (deg)" },

		{ "Kalman",				"EKF_TOLERANCE",	"Tolerance",					"Deactivated,Low,Medium,High" },
		{ "Kalman",				"EKF_ODO_LIN",		"Linear odometry noise" },
		{ "Kalman",				"EKF_ODO_ROT",		"Rotation odometry noise" },
		{ "Kalman",				"EKF_OBS_DIST",		"Distance noise (mm)" },
		{ "Kalman",				"EKF_OBS_ANGLE",	"Bearing noise (deg)" },

		{ "KFMarkov",			"KFMK_CELL",		"Cell size (mm)" },
		{ "KFMarkov",			"KFMK_BLUR_POS",	"Position blurring" },
		{ "KFMarkov",			"KFMK_BLUR_ANGLE",	"Angle blurring (deg)" },
		{ "KFMarkov",			"KFMK_TOLERANCE",	"Tolerance",					"Deactivated,Low,Medium,High" },
		{ "KFMarkov",			"KFMK_ODO_LIN",		"Linear odometry noise" },
		{ "KFMarkov",			"KFMK_ODO_ROT",		"Rotation odometry noise" },
		{ "KFMarkov",			"KFMK_OBS_DIST",	"Distance noise (mm)" },
		{ "KFMarkov",			"KFMK_OBS_ANGLE",	"Bearing noise (deg)" },

		{ "NKFMK",				"NKFMK_CELL",		"Cell size (mm)" },
		{ "NKFMK",				"NKFMK_BLUR_POS",	"Position blurring" },
		{ "NKFMK",				"NKFMK_BLUR_ANGLE",	"Angle blurring (deg)" },
		{ "NKFMK",				"NKFMK_TOLERANCE",	"Tolerance",					"Deactivated,Low,Medium,High" },
		{ "NKFMK",				"NKFMK_ODO_LIN",	"Linear odometry noise" },
		{ "NKFMK",				"NKFMK_ODO_ROT",	"Rotation odometry noise" },
		{ "NKFMK",				"NKFMK_OBS_DIST",	"Distance noise (mm)" },
		{ "NKFMK",				"NKFMK_OBS_ANGLE",	"Bearing noise (deg)" },
		{ "NKFMK",				"NKFMK_EKFS",		"Number of EKFs" },
		{ "NKFMK",				"NKFMK_MIN_AGE",	"Min age" },
		{ "NKFMK",				"NKFMK_CHI",		"Chi threshold" },
		{ "NKFMK",				"NKFMK_POSITION",	"Position strategy",			"FMK prob,FMK-CHI,EKFs mean" },
		{ "NKFMK",				"NKFMK_RESET_EKF",	"Reset at last position (%)" },
		{ "NKFMK",				"NKFMK_RESET_FMK",	"Reset by the grid (%)" },

		{ "Particles",			"PF_SAMPLES",		"Number of samples" },
		{ "Particles",			"PF_SPREAD",		"Initial spread (mm)" },
		{ "Particles",			"PF_SPREAD_ANGLE",	"Initial angle spread (deg)" },

		{ "SensorResetting",	"SRL_SAMPLES",		"Number of samples" },
	};

	/** A new method by its name (one of {@link #METHODS}), with the initial parameters it has now; null if there is none so called. */
	static public Localisation create (String method)
	{
		if (method == null)								return null;
		switch (method)
		{
		case "GridFMarkov":			return new GridFMarkov (GFMK_CELL, GFMK_BLUR_POS, GFMK_BLUR_ANGLE * Angles.DTOR);
		case "Kalman":				return new Kalman (EKF_TOLERANCE, EKF_ODO_LIN, EKF_ODO_ROT, EKF_OBS_DIST, EKF_OBS_ANGLE * Angles.DTOR);
		case "KFMarkov":			return new KFMarkov (KFMK_CELL, KFMK_BLUR_POS, KFMK_BLUR_ANGLE * Angles.DTOR,
											KFMK_TOLERANCE, KFMK_ODO_LIN, KFMK_ODO_ROT, KFMK_OBS_DIST, KFMK_OBS_ANGLE * Angles.DTOR);
		case "NKFMK":				return new NKFMK (NKFMK_CELL, NKFMK_BLUR_POS, NKFMK_BLUR_ANGLE * Angles.DTOR,
											NKFMK_TOLERANCE, NKFMK_ODO_LIN, NKFMK_ODO_ROT, NKFMK_OBS_DIST, NKFMK_OBS_ANGLE * Angles.DTOR,
											NKFMK_EKFS, NKFMK_MIN_AGE, NKFMK_CHI, NKFMK_POSITION, NKFMK_RESET_EKF, NKFMK_RESET_FMK);
		case "Particles":			return new Particles (PF_SAMPLES, PF_SPREAD, PF_SPREAD_ANGLE * Angles.DTOR);
		case "SensorResetting":		return new SensorResetting (SRL_SAMPLES);
		default:					return null;
		}
	}

	// Navigation structures
	protected volatile Localisation	loc;
	protected volatile String		method;			// the name of the method in use
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

	/** The name of the method in use. */
	public String method ()										{ return method; }

	/**
	 * The method to use from now on, by its name (one of {@link #METHODS}), made anew
	 * with the initial parameters it has now: it starts afresh, and so do the
	 * displacements of the odometry. Nothing changes if there is no such method.
	 */
	public synchronized void method (String name)
	{
		Localisation	l = create (name);

		if (l == null)						return;
		synchronized (odom)
		{
			odom.restart ();										// the displacements of the new method start from the next LPS
			method	= name;
			loc		= l;
		}
	}
	
	public void notify_lps (String space, ItemLPS item) 
	{ 
		super.notify_lps (space, item);
		
		Localisation	l = loc;

		if (!initialised || (l == null) || (item == null) || (item.lps == null))		return;
		
		synchronized (l)												
		{
			loclps.updateFromLps (item.lps);
			synchronized (odom)		{ odom.setOdometry (item.lps.odom); }
			l.updateMotionAndSensors (odom, loclps);
			setPosition (l.getGs ());
		}

		// what the method makes of it, to the window (with where the robot really is, the ground truth of the simulation)
		if (win != null)
			win.update (l, loclps, (item.lps.real != null) ? new Position (item.lps.real) : null);

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
								
		method (METHOD);

		// with local graphics, the window of the localisation
		if (localgfx && (win == null))
			javax.swing.SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;

					win = new SoccerLocalizationWindow (hostFrame (), "Chaos Localisation [" + getConfig ().robot () + "]", SoccerLocalization.this);
					win.setVisible (true);
				}
			});

		initialised = true;
	}
}
