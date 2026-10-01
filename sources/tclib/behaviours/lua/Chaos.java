/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua;

import java.util.HashMap;
import java.util.Map;

import tclib.utils.pos.Position;

import tc.shared.linda.ItemBehNeeds.ScanTypes;
import tcrob.umu.soccer.linda.ItemReferee;
import tc.shared.lps.LPS;
import tc.shared.lps.lpo.LPO;

import tclib.behaviours.lua.interpreter.Lua;
import tclib.behaviours.lua.interpreter.LuaFunction;
import tclib.behaviours.lua.interpreter.LuaTable;

import wucore.utils.math.Angles;

/**
 * The bridge between the scripts of a machine and the robot: the table the
 * scripts know as <code>chaos</code>. What a script asks for it reads from the
 * LPS, and what a script commands it keeps, for the controller to carry out at
 * the end of the cycle.
 *
 * The scripts come from the Chaos robots, which said distances in millimetres
 * and asked for speeds in millimetres a second and turn rates in degrees a
 * second. Angles are in degrees here as well, so that everything a script reads
 * and writes about an angle is in the same unit: what ThinkingCap keeps in
 * metres and radians is turned on this bridge and nowhere else.
 */
public class Chaos implements LuaBridge
{
	/** How many millimetres a metre has: what the scripts count distances in. */
	static public final double		MM			= 1000.0;

	/**
	 * Every angle the scripts are given or give is in degrees, as their turn rates
	 * always were: what ThinkingCap keeps in radians is turned here and nowhere
	 * else, and an angle is brought into -180 .. 180.
	 */
	static public double degrees (double radians)
	{
		if (!Double.isFinite (radians))			return radians;

		double		d = Math.toDegrees (radians) % 360.0;

		if (d > 180.0)							d -= 360.0;
		else if (d <= -180.0)					d += 360.0;
		return d;
	}

	/** An angle of the scripts (degrees) as ThinkingCap keeps it (radians). */
	static public double radians (double degrees)
	{
		return Double.isFinite (degrees) ? Math.toRadians (degrees) : degrees;
	}

	/** The objects of the LPS the scripts ask for by number (chaos.getLpo). */
	static public final String[]	LPOS		= { "Ball", "Net1", "Net2", "Align", "Looka", "Landmark1", "Landmark2" };

	/**
	 * The kinds of scan of the camera a script may ask for (chaos.setScanType), as
	 * constants of the table: SCAN_NONE, SCAN_LOW, SCAN_MID, SCAN_HIGH and
	 * SCAN_FULL, each worth its place in {@link ScanTypes}, which is what the vision
	 * is told.
	 */
	static public final ScanTypes[]	SCANS		= ScanTypes.values ();

	/**
	 * The states of the game the referee may say (chaos.getGameState), as
	 * constants of the table: REFEREE_INITIAL, REFEREE_READY, REFEREE_SET,
	 * REFEREE_PLAYING, REFEREE_PENALIZED and REFEREE_FINISHED, each worth its place
	 * in {@link ItemReferee.GameStates}.
	 */
	static public final ItemReferee.GameStates[]	STATES	= ItemReferee.GameStates.values ();
	static public final String		REFEREE_	= "REFEREE_";

	// What the robot knows
	protected LPS					lps;
	protected String[]				lpos		= LPOS;
	protected Position				pose		= new Position ();		// where the robot is (m, rad)
	protected Position				desired		= new Position ();		// where it has been told to go
	protected Position				start		= new Position ();		// where it starts (m, rad): its position at a kick-off
	protected Position				ballvel		= new Position ();		// how fast the ball goes (m/s)
	protected String				role		= "player";
	protected volatile ItemReferee	referee;							// the last thing the referee said, null while nothing
	protected volatile ItemReferee.GameStates	gstate;					// the state of the game for this robot (null: the one the referee last said)

	// What the scripts commanded
	protected double				vlin;								// mm/s
	protected double				vrot;								// deg/s
	protected double				vlat;								// mm/s
	protected String				behaviour;							// the behaviour the state chose
	protected boolean				behaviournew;						// ... and whether it has just been chosen
	protected long					behaviourtime;						// when it was chosen (ms)
	protected ScanTypes				scan		= ScanTypes.SCAN_NONE;	// the scan of the camera the script asked for this cycle
	protected boolean				kick;
	protected boolean				synchrokick;
	protected boolean				booked;

	// What the scripts left for one another
	protected Map<String, Object>	globals		= new HashMap<String, Object> ();
	// The clock of the execution (ms): when this bridge was made, from which the timers of the scripts count
	protected long					created		= System.currentTimeMillis ();
	protected Map<Integer, Double>	needed		= new HashMap<Integer, Double> ();

	protected LuaTable				table;
	protected java.util.Set<String>	warned		= new java.util.HashSet<String> ();
	protected java.util.List<String>	named	= new java.util.ArrayList<String> ();	// the constants of the objects of the LPS

	public Chaos ()
	{
		table	= build ();
		constants ();
	}

	/* ------------------------------------------------------------------ */
	/* What the controller puts in and takes out                           */
	/* ------------------------------------------------------------------ */

	/** The table the scripts see as <code>chaos</code>. */
	public final LuaTable			table ()					{ return table; }
	public final String				name ()						{ return "chaos"; }

	/** What the scripts asked of the robot on this cycle, as the monitors show it. */
	public Map<String, Object> commands ()
	{
		Map<String, Object>		c = new java.util.LinkedHashMap<String, Object> ();

		c.put ("vlin", Double.valueOf (vlin));
		c.put ("vlat", Double.valueOf (vlat));
		c.put ("vrot", Double.valueOf (vrot));
		c.put ("behaviour", behaviour);
		c.put ("scan", scan.name ());
		return c;
	}

	/**
	 * What the scripts left for one another through
	 * <code>setGlobal</code>/<code>getGlobal</code>, for whoever looks at a program
	 * while it runs: one value to a name.
	 */
	public Map<String, Object> globals ()
	{
		return new java.util.TreeMap<String, Object> (globals);
	}

	public void lps (LPS lps)									{ this.lps = lps; }
	public LPS lps ()											{ return lps; }

	/** The objects of the LPS the scripts ask for by number. */
	public void lpoNames (String[] names)
	{
		this.lpos	= (names != null) ? names : LPOS;
		constants ();
	}

	public String[] lpoNames ()									{ return lpos; }

	/**
	 * The number of every object of the LPS as a constant of the table, so that a
	 * script says <code>chaos.getLpo (chaos.BALL_LPO)</code> and never a number of
	 * its own: <code>BALL_LPO</code>, <code>NET1_LPO</code> and so on, the name of
	 * the object in capitals with <code>_LPO</code> after it.
	 *
	 * They are put in again on every cycle ({@link #clear}), so a script that wrote
	 * over one does not leave it changed for the next cycle or for another script.
	 */
	protected void constants ()
	{
		if (table == null)						return;					// while the table is being built

		for (String name : named)											// the ones of the objects there were
			table.set (name, null);
		named.clear ();
		for (int i = 0; i < lpos.length; i++)
		{
			named.add (constant (lpos[i]));
			table.set (constant (lpos[i]), Double.valueOf (i));
		}
		for (int i = 0; i < SCANS.length; i++)						// and the kinds of scan, by the name of the enum
			table.set (SCANS[i].name (), Double.valueOf (i));
		for (int i = 0; i < STATES.length; i++)					// and the states of the game: REFEREE_PLAYING and so on
			table.set (REFEREE_ + STATES[i].name (), Double.valueOf (i));
	}

	/** What the constant of an object of the LPS is called: Net1 is NET1_LPO. */
	static public String constant (String name)
	{
		return ((name != null) ? name.toUpperCase () : "?") + "_LPO";
	}

	/** Where the robot is, with how sure it is of it (quality) and its uncertainty, which the scripts read with chaos.getCurrentPos. */
	public void pose (Position p)								{ if (p != null) synchronized (pose) { pose.set (p); } }
	/** Where the robot starts, which the scripts read with chaos.getStartPos. */
	public void start (double x, double y, double alpha)		{ start.set (x, y, alpha); }

	/**
	 * The same, from the properties of the robot the simulator sends with CONFIG
	 * (START_X, START_Y in m, START_A in rad); nothing changes when they are not
	 * there. Every controller with a chaos calls it from notify_config.
	 */
	public void start (java.util.Properties props)
	{
		if (props == null)					return;
		try
		{
			String	x = props.getProperty ("START_X"), y = props.getProperty ("START_Y"), a = props.getProperty ("START_A");

			if ((x != null) && (y != null) && (a != null))
				start (Double.parseDouble (x), Double.parseDouble (y), Double.parseDouble (a));
		}
		catch (NumberFormatException e)		{ }
	}
	public Position start ()									{ return start; }
	public Position pose ()										{ return pose; }

	public void ballVelocity (double vx, double vy)				{ ballvel.set (vx, vy); }
	public void role (String r)									{ role = r; }

	/*
	 * What the scripts asked for, the three velocities of the control action, in what
	 * ThinkingCap works in: the scripts say millimetres a second and degrees a
	 * second, and these are metres a second and radians a second, which is what the
	 * controller commands and the kinematics carry out.
	 */
	/** How fast the scripts asked to go forward [m/s]. */
	public double linear ()										{ return vlin / MM; }
	/** How fast the scripts asked to go sideways, to the left of the robot [m/s]. */
	public double lateral ()									{ return vlat / MM; }
	/** How fast the scripts asked to turn [rad/s]. */
	public double rotation ()									{ return vrot * Angles.DTOR; }

	/* The same three as the scripts themselves said them: mm/s, mm/s and deg/s. */
	public double vlin ()										{ return vlin; }
	public double vlat ()										{ return vlat; }
	public double vrot ()										{ return vrot; }

	/** The behaviour the machine chose, or null when it has chosen none. */
	public String behaviour ()									{ return behaviour; }

	public void behaviour (String b)
	{
		if ((b != null) && !b.equals (behaviour))
		{
			behaviournew	= true;
			behaviourtime	= System.currentTimeMillis ();
		}
		behaviour	= b;
	}

	/** Another state: no behaviour until its script chooses one. */
	public void entered ()
	{
		behaviour		= null;
		behaviournew	= false;
	}

	/** Whether the behaviour was chosen in this very cycle, which a behaviour asks to set itself up. */
	public boolean behaviourIsNew ()							{ return behaviournew; }

	/** What the referee last said (REFEREE), for the scripts to read (chaos.getGameState); null while nothing. */
	public void referee (ItemReferee item)						{ referee = item; }
	public ItemReferee referee ()								{ return referee; }

	/**
	 * The state of the game as it stands for this robot, which is not always what
	 * the referee last said: a robot penalised stays PENALIZED while the tuples
	 * about the others go by. Null to take the last tuple's.
	 */
	public void gameState (ItemReferee.GameStates s)			{ gstate = s; }
	public ItemReferee.GameStates gameState ()
	{
		ItemReferee.GameStates	s = gstate;
		ItemReferee				r = referee;

		return (s != null) ? s : ((r != null) ? r.state : ItemReferee.GameStates.INITIAL);
	}

	/** The scan of the camera the scripts asked for on this cycle: none unless one said so (chaos.setScanType). */
	public ScanTypes scanType ()								{ return scan; }

	public boolean kicking ()									{ return kick; }
	public Position desired ()									{ return desired; }

	/** How much the machine says it needs each object of the LPS to be seen. */
	public Map<Integer, Double> needed ()						{ return needed; }

	/** Forgets what the scripts commanded, before a new cycle. */
	public void clear ()
	{
		constants ();									// what a script wrote over is put back
		behaviournew	= false;
		scan	= ScanTypes.SCAN_NONE;					// a scan is asked for on every cycle, as a speed is
		vlin	= 0.0;
		vrot	= 0.0;
		vlat	= 0.0;
		kick	= false;
		needed.clear ();
	}

	/**
	 * A value a script commanded, or the one it had when the script did not command
	 * a number at all: a script that divides by zero (which they do, dividing an
	 * angle by its own size to take its sign) asks for a speed that is not a number,
	 * and a robot commanded with one loses its pose for good.
	 */
	protected double sane (String what, double value, double old)
	{
		if (Double.isFinite (value))			return value;

		if (warned.add (what))
			System.out.println ("  [CHAOS] " + what + " was given " + value + " and ignored");
		return old;
	}

	/**
	 * The object of the LPS a script asks for, as the number of it, or -1 when the
	 * script asked for no such object: nothing at all (a constant that does not exist,
	 * <code>chaos.BALL_NET2</code> say, is nil in Lua), something that is not a
	 * number, or a number of no object. It used to be read as 0, and the script went
	 * on with the ball in place of what it had misspelt and nobody knew; it is said
	 * once now on the console, with the constants there are, so that the script can
	 * be put right -- and the function is told every time ({@link LuaFunction#complain}),
	 * so that the monitor of the program marks the variable that got the nil.
	 */
	protected int index (LuaFunction f, Object[] args)
	{
		Object		a = LuaFunction.arg (args, 0);
		Double		d = Lua.tonumber (a);
		int			i = (d != null) ? d.intValue () : -1;

		if ((d != null) && (i >= 0) && (i < lpos.length))		return i;

		StringBuilder	sb = new StringBuilder ();

		for (int k = 0; k < lpos.length; k++)
			sb.append ((k > 0) ? ", " : "").append ("chaos.").append (constant (lpos[k])).append (" (").append (k).append (")");

		String		what = f.name () + " was given " + Lua.tostring (a) + " and ignored: it wants one of " + sb;

		f.complain (what);
		if (warned.add (f.name () + "/" + Lua.tostring (a)))
			System.out.println ("  [CHAOS] " + what);
		return -1;
	}

	/* ------------------------------------------------------------------ */
	/* The table of functions                                             */
	/* ------------------------------------------------------------------ */

	/** An object of the LPS as the scripts read it: rho in mm, theta in degrees. */
	protected LuaTable lpo (int index)
	{
		LuaTable		t = new LuaTable ();
		LPO				o = ((lps != null) && (index >= 0) && (index < lpos.length)) ? lps.find (lpos[index]) : null;

		t.set ("index", Double.valueOf (index));
		t.set ("name", ((index >= 0) && (index < lpos.length)) ? lpos[index] : "?");
		t.set ("rho", Double.valueOf ((o != null) ? (o.rho () * MM) : 0.0));
		t.set ("theta", Double.valueOf ((o != null) ? degrees (o.theta ()) : 0.0));
		t.set ("anchored", Double.valueOf ((o != null) ? o.anchor () : 0.0));
		t.set ("quality", Double.valueOf ((o != null) ? o.anchor () : 0.0));
		t.set ("active", Boolean.valueOf ((o != null) && o.active ()));
		if (o != null)
		{
			t.set ("x", Double.valueOf (o.rho () * Math.cos (o.theta ()) * MM));
			t.set ("y", Double.valueOf (o.rho () * Math.sin (o.theta ()) * MM));
		}
		else
		{
			t.set ("x", Double.valueOf (0.0));
			t.set ("y", Double.valueOf (0.0));
		}
		return t;
	}

	/**
	 * A pose as the scripts read it: x and y in mm, theta in rad, and how sure the
	 * robot is of it, which in a simulation it always is.
	 */
	static protected LuaTable point (double x, double y, double theta)
	{
		LuaTable		t = new LuaTable ();

		t.set ("x", Double.valueOf (x * MM));
		t.set ("y", Double.valueOf (y * MM));
		t.set ("theta", Double.valueOf (degrees (theta)));
		t.set ("quality", Double.valueOf (1.0));
		t.set ("anchored", Double.valueOf (1.0));
		return t;
	}

	protected LuaTable build ()
	{
		LuaTable		c = new LuaTable ();

		/* ---- what the robot sees ---- */

		c.set ("getLpo", new LuaFunction ("chaos.getLpo")
		{
			public Object call (Object[] args)
			{
				int		i = index (this, args);

				return (i >= 0) ? lpo (i) : null;
			}
		});

		// the scan of the camera: setScanType (chaos.SCAN_LOW) and so on; a number of
		// no scan, or none at all, is said once and ignored, as an object of no number is
		c.set ("setScanType", new LuaFunction ("chaos.setScanType")
		{
			public Object call (Object[] args)
			{
				Object		a = LuaFunction.arg (args, 0);
				Double		d = Lua.tonumber (a);
				int			i = (d != null) ? d.intValue () : -1;

				if ((d != null) && (i >= 0) && (i < SCANS.length))
				{
					scan	= SCANS[i];
					return null;
				}

				StringBuilder	sb = new StringBuilder ();

				for (int k = 0; k < SCANS.length; k++)
					sb.append ((k > 0) ? ", " : "").append ("chaos.").append (SCANS[k].name ()).append (" (").append (k).append (")");

				String		what = name + " was given " + Lua.tostring (a) + " and ignored: it wants one of " + sb;

				complain (what);
				if (warned.add (name + "/" + Lua.tostring (a)))		System.out.println ("  [CHAOS] " + what);
				return null;
			}
		});

		c.set ("setNeeded", new LuaFunction ("chaos.setNeeded")
		{
			public Object call (Object[] args)
			{
				int		i = index (this, args);

				if (i >= 0)			needed.put (Integer.valueOf (i), Double.valueOf (num (args, 1, 1.0)));
				return null;
			}
		});

		// where the robot is now (it was getMyPos, and gsGetMyPos)
		c.set ("getCurrentPos", new LuaFunction ("chaos.getCurrentPos")
		{
			public Object call (Object[] args)
			{
				LuaTable	t;
				double[][]	u;

				synchronized (pose)
				{
					t	= point (pose.x (), pose.y (), pose.alpha);
					u	= pose.uncert;
					t.set ("quality", Double.valueOf (pose.quality));
					t.set ("anchored", Double.valueOf (pose.valid ? 1.0 : 0.0));
					if ((u != null) && (u.length > 2))			// the standard deviations (mm, mm, degrees)
					{
						t.set ("dx", Double.valueOf (Math.sqrt (Math.max (0.0, u[0][0])) * MM));
						t.set ("dy", Double.valueOf (Math.sqrt (Math.max (0.0, u[1][1])) * MM));
						t.set ("dtheta", Double.valueOf (degrees (Math.sqrt (Math.max (0.0, u[2][2])))));
					}
				}
				return t;
			}
		});

		// where the robot starts: the position it is put at for a kick-off
		c.set ("getStartPos", new LuaFunction ("chaos.getStartPos")
		{
			public Object call (Object[] args)		{ return point (start.x (), start.y (), start.alpha); }
		});

		c.set ("getBallVel", new LuaFunction ("chaos.getBallVel")
		{
			public Object call (Object[] args)		{ return point (ballvel.x (), ballvel.y (), 0.0); }
		});

		/* ---- what the robot is asked to do ---- */

		c.set ("setVlin", new LuaFunction ("chaos.setVlin")
		{
			public Object call (Object[] args)		{ vlin = sane (name, num (args, 0, 0.0), vlin);	return null; }
		});

		c.set ("setVrot", new LuaFunction ("chaos.setVrot")
		{
			public Object call (Object[] args)		{ vrot = sane (name, num (args, 0, 0.0), vrot);	return null; }
		});

		c.set ("setVlat", new LuaFunction ("chaos.setVlat")
		{
			public Object call (Object[] args)		{ vlat = sane (name, num (args, 0, 0.0), vlat);	return null; }
		});

		// the three velocities at once, in the order the scripts write them:
		// along, across and around (vlin, vlat, vrot), as x, y and heading
		c.set ("setVelocities", new LuaFunction ("chaos.setVelocities")
		{
			public Object call (Object[] args)
			{
				vlin	= sane (name, num (args, 0, 0.0), vlin);
				vlat	= sane (name, num (args, 1, 0.0), vlat);
				vrot	= sane (name, num (args, 2, 0.0), vrot);
				return null;
			}
		});

		c.set ("setBehavior", new LuaFunction ("chaos.setBehavior")
		{
			public Object call (Object[] args)		{ behaviour (str (args, 0));	return null; }
		});

		c.set ("getBehaviorInfo", new LuaFunction ("chaos.getBehaviorInfo")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = new LuaTable ();

				t.set ("name", (behaviour != null) ? behaviour : "");
				t.set ("isNew", Double.valueOf (behaviournew ? 1.0 : 0.0));
				// how long the behaviour has been running (ms), which the scripts call both ways
				t.set ("timer", Double.valueOf ((behaviourtime > 0) ? (System.currentTimeMillis () - behaviourtime) : 0.0));
				t.set ("time", t.get ("timer"));
				t.set ("finished", Double.valueOf (0.0));
				t.set ("failed", Double.valueOf (0.0));
				return t;
			}
		});

		// what the referee says: the state of the game as one of the REFEREE_ constants
		// (REFEREE_INITIAL while it has said nothing) and the last decision
		c.set ("getGameState", new LuaFunction ("chaos.getGameState")
		{
			public Object call (Object[] args)
			{
				ItemReferee				r = referee;
				ItemReferee.GameStates	s = gameState ();
				LuaTable				t = new LuaTable ();

				t.set ("state", Double.valueOf (s.ordinal ()));
				t.set ("name", s.name ());
				t.set ("player", Double.valueOf ((r != null) ? r.player : -1));
				t.set ("event", (r != null) ? r.event.name () : "");
				t.set ("team", Double.valueOf ((r != null) ? r.team : -1));
				t.set ("robot", ((r != null) && (r.robot != null)) ? r.robot : "");
				t.set ("text", (r != null) ? r.text : "");
				t.set ("score1", Double.valueOf ((r != null) ? r.score1 : 0));
				t.set ("score2", Double.valueOf ((r != null) ? r.score2 : 0));
				t.set ("time", Double.valueOf ((r != null) ? r.time : 0));
				return t;
			}
		});

		c.set ("setDesiredPos", new LuaFunction ("chaos.setDesiredPos")
		{
			public Object call (Object[] args)
			{
				double		x = sane (name, num (args, 0, 0.0), desired.x () * MM);
				double		y = sane (name, num (args, 1, 0.0), desired.y () * MM);
				double		a = sane (name, num (args, 2, 0.0), degrees (desired.alpha));

				desired.set (x / MM, y / MM, radians (a));
				return null;
			}
		});

		c.set ("getDesiredPos", new LuaFunction ("chaos.getDesiredPos")
		{
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha); }
		});

		c.set ("setKick", new LuaFunction ("chaos.setKick")
		{
			public Object call (Object[] args)		{ kick = true;	return null; }
		});

		c.set ("setSynchroKick", new LuaFunction ("chaos.setSynchroKick")
		{
			public Object call (Object[] args)		{ synchrokick = Lua.truth (arg (args, 0));	kick = synchrokick;	return null; }
		});

		/* ---- the team ---- */

		c.set ("getRole", new LuaFunction ("chaos.getRole")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = new LuaTable ();

				t.set ("role", (role != null) ? role : "");
				return t;
			}
		});

		c.set ("getOptimalPose", new LuaFunction ("chaos.getOptimalPose")
		{
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha); }
		});

		c.set ("getDefPose", new LuaFunction ("chaos.getDefPose")
		{
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha); }
		});

		c.set ("bookBall", new LuaFunction ("chaos.bookBall")
		{
			public Object call (Object[] args)		{ booked = true;	return Boolean.TRUE; }
		});

		c.set ("haveBookedBall", new LuaFunction ("chaos.haveBookedBall")
		{
			public Object call (Object[] args)		{ return Boolean.valueOf (booked); }
		});

		c.set ("releaseBookedBall", new LuaFunction ("chaos.releaseBookedBall")
		{
			public Object call (Object[] args)		{ booked = false;	return null; }
		});

		/* ---- what the scripts leave for one another ---- */

		// the scripts say setGlobal (name, value) and getGlobal (name): one value to a
		// name, and nothing else -- the index of before made a second entry (name#index)
		// of every value, which the monitor then showed twice
		c.set ("setGlobal", new LuaFunction ("chaos.setGlobal")
		{
			public Object call (Object[] args)
			{
				globals.put (str (args, 0), arg (args, 1));
				return null;
			}
		});

		c.set ("getGlobal", new LuaFunction ("chaos.getGlobal")
		{
			public Object call (Object[] args)
			{
				return globals.get (str (args, 0));
			}
		});

		/* ---- timers, kept as globals of the bridge ---- */

		// initializeTimer (name) keeps the clock of the execution (ms) in the global of
		// that name; getTimer (name) is how long it is since then (ms). A timer that
		// was never initialised (or a global that is not a number) reads 0, and it is said.
		c.set ("initializeTimer", new LuaFunction ("chaos.initializeTimer")
		{
			public Object call (Object[] args)
			{
				String	n = str (args, 0);

				if (n == null)		{ complain (name + " wants the name of a global");	return null; }
				globals.put (n, Double.valueOf ((double) executionTime ()));
				return null;
			}
		});

		c.set ("getTimer", new LuaFunction ("chaos.getTimer")
		{
			public Object call (Object[] args)
			{
				String	n = str (args, 0);
				Double	t0 = (n != null) ? Lua.tonumber (globals.get (n)) : null;

				if (t0 == null)
				{
					String	what = name + " (" + Lua.tostring (arg (args, 0)) + "): no such timer, initializeTimer it first; reads 0";

					complain (what);
					if (warned.add (name + "/" + n))		System.out.println ("  [CHAOS] " + what);
					return Double.valueOf (0.0);
				}
				return Double.valueOf ((double) executionTime () - t0.doubleValue ());
			}
		});

		return c;
	}

	/** The clock of the execution (ms): how long this bridge has been alive, what the timers of the scripts count from. */
	public long executionTime ()						{ return System.currentTimeMillis () - created; }

	public String toString ()
	{
		return "chaos [vlin " + Lua.number (vlin) + " mm/s, vrot " + Lua.number (vrot) + " deg/s, vlat " + Lua.number (vlat)
			   + " mm/s, behaviour " + ((behaviour != null) ? behaviour : "-") + "]";
	}
}
