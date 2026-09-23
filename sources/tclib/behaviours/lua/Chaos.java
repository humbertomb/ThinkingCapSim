/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua;

import java.util.HashMap;
import java.util.Map;

import devices.pos.Position;

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
 * and angles in radians, and asked for speeds in millimetres a second and turn
 * rates in degrees a second. Everything that crosses this bridge is turned into
 * what ThinkingCap uses (metres, radians a second) and back, so that the scripts
 * need no changing.
 */
public class Chaos
{
	/** How many millimetres a metre has: what the scripts count distances in. */
	static public final double		MM			= 1000.0;

	/** The objects of the LPS the scripts ask for by number (chaos.getLpo). */
	static public final String[]	LPOS		= { "Ball", "Net1", "Net2", "Align", "Looka" };

	// What the robot knows
	protected LPS					lps;
	protected String[]				lpos		= LPOS;
	protected Position				pose		= new Position ();		// where the robot is (m, rad)
	protected Position				desired		= new Position ();		// where it has been told to go
	protected Position				ballvel		= new Position ();		// how fast the ball goes (m/s)
	protected String				role		= "player";

	// What the scripts commanded
	protected double				vlin;								// mm/s
	protected double				vrot;								// deg/s
	protected double				vlat;								// mm/s
	protected String				behaviour;							// the behaviour the state chose
	protected boolean				behaviournew;						// ... and whether it has just been chosen
	protected long					behaviourtime;						// when it was chosen (ms)
	protected boolean				kick;
	protected boolean				synchrokick;
	protected boolean				surround;
	protected boolean				booked;

	// What the scripts left for one another
	protected Map<String, Object>	globals		= new HashMap<String, Object> ();
	protected Map<Integer, Double>	needed		= new HashMap<Integer, Double> ();

	protected LuaTable				table;
	protected java.util.Set<String>	warned		= new java.util.HashSet<String> ();

	public Chaos ()
	{
		table	= build ();
	}

	/* ------------------------------------------------------------------ */
	/* What the controller puts in and takes out                           */
	/* ------------------------------------------------------------------ */

	/** The table the scripts see as <code>chaos</code>. */
	public final LuaTable			table ()					{ return table; }

	public void lps (LPS lps)									{ this.lps = lps; }
	public LPS lps ()											{ return lps; }

	/** The objects of the LPS the scripts ask for by number. */
	public void lpoNames (String[] names)						{ this.lpos = (names != null) ? names : LPOS; }
	public String[] lpoNames ()									{ return lpos; }

	public void pose (Position p)								{ if (p != null) pose.set (p); }
	public Position pose ()										{ return pose; }

	public void ballVelocity (double vx, double vy)				{ ballvel.set (vx, vy); }
	public void role (String r)									{ role = r; }

	/** What the scripts asked for, in what ThinkingCap uses: metres a second. */
	public double speed ()										{ return vlin / MM; }
	/** What the scripts asked for, in radians a second. */
	public double turn ()										{ return vrot * Angles.DTOR; }
	public double lateral ()									{ return vlat / MM; }

	public double vlin ()										{ return vlin; }
	public double vrot ()										{ return vrot; }
	public double vlat ()										{ return vlat; }

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

	/** Whether the behaviour was chosen in this very cycle, which a behaviour asks to set itself up. */
	public boolean behaviourIsNew ()							{ return behaviournew; }

	public boolean kicking ()									{ return kick; }
	public boolean surrounding ()								{ return surround; }
	public Position desired ()									{ return desired; }

	/** How much the machine says it needs each object of the LPS to be seen. */
	public Map<Integer, Double> needed ()						{ return needed; }

	/** Forgets what the scripts commanded, before a new cycle. */
	public void clear ()
	{
		behaviournew	= false;
		vlin	= 0.0;
		vrot	= 0.0;
		vlat	= 0.0;
		kick	= false;
		surround	= false;
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

	/* ------------------------------------------------------------------ */
	/* The table of functions                                             */
	/* ------------------------------------------------------------------ */

	/** An object of the LPS as the scripts read it: rho in mm, theta in rad. */
	protected LuaTable lpo (int index)
	{
		LuaTable		t = new LuaTable ();
		LPO				o = ((lps != null) && (index >= 0) && (index < lpos.length)) ? lps.find (lpos[index]) : null;

		t.set ("index", Double.valueOf (index));
		t.set ("name", ((index >= 0) && (index < lpos.length)) ? lpos[index] : "?");
		t.set ("rho", Double.valueOf ((o != null) ? (o.rho () * MM) : 0.0));
		t.set ("theta", Double.valueOf ((o != null) ? o.theta () : 0.0));
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
		t.set ("theta", Double.valueOf (theta));
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
			public Object call (Object[] args)		{ return lpo ((int) num (args, 0, 0.0)); }
		});

		c.set ("setNeeded", new LuaFunction ("chaos.setNeeded")
		{
			public Object call (Object[] args)
			{
				needed.put (Integer.valueOf ((int) num (args, 0, 0.0)), Double.valueOf (num (args, 1, 1.0)));
				return null;
			}
		});

		c.set ("gsGetMyPos", new LuaFunction ("chaos.gsGetMyPos")
		{
			public Object call (Object[] args)		{ return point (pose.x (), pose.y (), pose.alpha ()); }
		});

		c.set ("getMyPos", new LuaFunction ("chaos.getMyPos")
		{
			public Object call (Object[] args)		{ return point (pose.x (), pose.y (), pose.alpha ()); }
		});

		c.set ("getBallVel", new LuaFunction ("chaos.getBallVel")
		{
			public Object call (Object[] args)		{ return point (ballvel.x (), ballvel.y (), 0.0); }
		});

		c.set ("lps_getAstray", new LuaFunction ("chaos.lps_getAstray")
		{
			public Object call (Object[] args)
			{
				LuaTable	t = new LuaTable ();

				t.set ("astray", Double.valueOf (0.0));						// the simulation never loses itself
				return t;
			}
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

		c.set ("setVelocities", new LuaFunction ("chaos.setVelocities")
		{
			public Object call (Object[] args)
			{
				vlin	= sane (name, num (args, 0, 0.0), vlin);
				vrot	= sane (name, num (args, 1, 0.0), vrot);
				vlat	= sane (name, num (args, 2, 0.0), vlat);
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

		c.set ("setDesiredPos", new LuaFunction ("chaos.setDesiredPos")
		{
			public Object call (Object[] args)
			{
				double		x = sane (name, num (args, 0, 0.0), desired.x () * MM);
				double		y = sane (name, num (args, 1, 0.0), desired.y () * MM);
				double		a = sane (name, num (args, 2, 0.0), desired.alpha ());

				desired.set (x / MM, y / MM, a);
				return null;
			}
		});

		c.set ("setTargetPos", (LuaFunction) c.get ("setDesiredPos"));

		c.set ("getDesiredPos", new LuaFunction ("chaos.getDesiredPos")
		{
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha ()); }
		});

		c.set ("setKick", new LuaFunction ("chaos.setKick")
		{
			public Object call (Object[] args)		{ kick = true;	return null; }
		});

		c.set ("setSynchroKick", new LuaFunction ("chaos.setSynchroKick")
		{
			public Object call (Object[] args)		{ synchrokick = Lua.truth (arg (args, 0));	kick = synchrokick;	return null; }
		});

		c.set ("setSurround", new LuaFunction ("chaos.setSurround")
		{
			public Object call (Object[] args)		{ surround = true;	return null; }
		});

		c.set ("trackLandMarks", new LuaFunction ("chaos.trackLandMarks")
		{
			public Object call (Object[] args)		{ return null; }			// the camera of the simulation looks everywhere
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
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha ()); }
		});

		c.set ("getDefPose", new LuaFunction ("chaos.getDefPose")
		{
			public Object call (Object[] args)		{ return point (desired.x (), desired.y (), desired.alpha ()); }
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

		// the scripts say setGlobal (name, index, value) and getGlobal (name [, index]):
		// the value is the last argument, and the index a slot of that name
		c.set ("setGlobal", new LuaFunction ("chaos.setGlobal")
		{
			public Object call (Object[] args)
			{
				String		name = str (args, 0);
				int			n = (args != null) ? args.length : 0;

				if (n >= 3)
				{
					globals.put (name + "#" + Lua.tostring (arg (args, 1)), arg (args, 2));
					globals.put (name, arg (args, 2));
				}
				else
					globals.put (name, arg (args, 1));
				return null;
			}
		});

		c.set ("getGlobal", new LuaFunction ("chaos.getGlobal")
		{
			public Object call (Object[] args)
			{
				String		name = str (args, 0);

				if ((args != null) && (args.length > 1) && (args[1] != null))
				{
					Object	v = globals.get (name + "#" + Lua.tostring (args[1]));

					if (v != null)					return v;
				}
				return globals.get (name);
			}
		});

		return c;
	}

	public String toString ()
	{
		return "chaos [vlin " + Lua.number (vlin) + " mm/s, vrot " + Lua.number (vrot) + " deg/s, vlat " + Lua.number (vlat)
			   + " mm/s, behaviour " + ((behaviour != null) ? behaviour : "-") + "]";
	}
}
