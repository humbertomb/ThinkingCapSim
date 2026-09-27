/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.quaky2;

import java.awt.Color;

import javax.swing.SwingUtilities;

import tc.modules.Supervisor;
import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.Linda;
import tc.shared.world.WMZone;
import tcapps.tcsimulator.simulator.Simulated;
import tcapps.tcsimulator.simulator.Simulator;
import tcapps.tcsimulator.simulator.objects.SimMobileObject;
import tcapps.tcsimulator.simulator.objects.SimObject;
import tcrob.umu.quaky2.gui.SoccerRefereeWindow;

/**
 * The referee of a simulated soccer match, after the rules of the RoboCup
 * Four-Legged League of 2007: it looks at where the simulator really has the
 * ball and the robots, and at which robot touched the ball last, and decides.
 *
 * <ul>
 * <li>A goal is the whole ball inside a net (the zones NET1, NET2), scored by
 *     the team that attacks it: the red team owns the red net (Net1) and the
 *     blue team the blue one (Net2). A kick-off shot -- a ball that no robot
 *     has touched outside the centre circle since the kick-off, so it was shot
 *     from the kick-off itself -- is no goal.
 * <li>After a goal (or a kick-off shot) the play restarts: the ball still at
 *     the centre and every robot back at its start position.
 * <li>The whole ball out of the field (the zone FIELD) is a fault, and the ball
 *     is put back still where the rules say: out over a side line, on the
 *     throw-in line at the point it went out, one metre back towards the goal
 *     of the team that touched it last, never nearer than one metre to the
 *     ends; out over an end line, at the corner kick point when the defending
 *     team touched it last, on the halfway line (same side) when the attacking
 *     team did, one metre in from the end line when nobody knows.
 * <li>A robot in the area of a net (AREA1, AREA2) is a fault unless it is the
 *     one robot that defends that net, the keeper of the team that owns it.
 * </ul>
 * Each is decided once, when it happens, and not again until the ball or the
 * robot has left where it was. The robot that touched the ball last is named
 * in every decision about the ball.
 *
 * Which team a robot is on is said by name (ROBOTS1, ROBOTS2) or, failing
 * that, taken from the half of the field it is first seen in: the one with
 * the red net is the red team's.
 *
 * It needs the simulator ({@link Simulated}), which the execution gives it when
 * the architecture runs in one; the world it reads the zones from is the
 * simulator's. With local graphics it opens the referee's window
 * ({@link SoccerRefereeWindow}).
 *
 * <pre>
 *   BALL        the animated object that is the ball (ball)
 *   FIELD       the zone of the field (Field)
 *   NET1, NET2  the zones inside the nets, where a ball is a goal (Net1Inside, Net2Inside)
 *   AREA1, AREA2  the areas of the nets, where only the keeper may be (Net1Area, Net2Area)
 *   TEAM1, TEAM2  what the teams are called (Red, Blue)
 *   ROBOTS1, ROBOTS2  the robots of each team, by name, comma separated (by default, by the half they start in)
 *   THROWIN     how far in from the side line the throw-in line is [m] (0.5)
 *   CORNER      how far in from the end line the corner kick point is, on the throw-in line [m] (0.65)
 *   CIRCLE      the radius of the centre circle [m] (0.18)
 *   DURATION    the match, in seconds (600)
 * </pre>
 */
public class SoccerRefereeSimul extends Supervisor implements Simulated
{
	static public final Color		C_TEAM1		= new Color (200, 30, 30);
	static public final Color		C_TEAM2		= new Color (30, 70, 200);

	/** The rules: one metre back from where the ball went out, and never nearer than one metre to the ends. */
	static public final double		BACK		= 1.0;
	static public final double		END_MARGIN	= 1.0;

	protected Simulator				sim;
	protected String				ballName;
	protected String				fieldName;
	protected String[]				netNames	= new String[2];
	protected String[]				areaNames	= new String[2];
	protected String[]				teamNames	= new String[2];
	protected String[][]			teamRobots	= new String[2][];		// by name, when the settings say; null for by the half they start in
	protected double				throwIn;				// the throw-in line, in from the side line [m]
	protected double				corner;					// the corner kick point, in from the end line [m]
	protected double				circle;					// the centre circle [m]

	// What is decided and kept
	protected final int[]			score		= new int[2];
	protected int					faults;

	// Where the ball and the robots were last seen, to decide once on each thing that happens
	protected Boolean				inField;				// null: not looked at yet
	protected Boolean[]				inNet		= new Boolean[2];
	protected boolean				kickoff		= true;		// since the last kick-off no robot has touched the ball outside the centre circle
	protected long					touchSeen;				// the last touch of the ball looked at (its time), to see the new ones
	protected int[]					teamOf		= new int[Simulator.MAX_ROBOTS];		// the team of each robot, -1 while not known
	protected Boolean[][]			inArea		= new Boolean[Simulator.MAX_ROBOTS][2];	// which robots are in which areas

	protected SoccerRefereeWindow	win;

	public SoccerRefereeSimul (ModuleConfig config, Linda linda)
	{
		super (config, linda);
	}

	protected void initialise (ModuleConfig cfg)
	{
		super.initialise (cfg);

		ballName		= cfg.get ("BALL", "ball");
		fieldName		= cfg.get ("FIELD", "Field");
		netNames[0]		= cfg.get ("NET1", "Net1Inside");
		netNames[1]		= cfg.get ("NET2", "Net2Inside");
		areaNames[0]	= cfg.get ("AREA1", "Net1Area");
		areaNames[1]	= cfg.get ("AREA2", "Net2Area");
		teamNames[0]	= cfg.get ("TEAM1", "Red");
		teamNames[1]	= cfg.get ("TEAM2", "Blue");
		teamRobots[0]	= names (cfg.get ("ROBOTS1"));
		teamRobots[1]	= names (cfg.get ("ROBOTS2"));
		throwIn			= cfg.getDouble ("THROWIN", 0.5);
		corner			= cfg.getDouble ("CORNER", 0.65);
		circle			= cfg.getDouble ("CIRCLE", 0.18);
		java.util.Arrays.fill (teamOf, -1);

		if (localgfx)
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;
					win	= new SoccerRefereeWindow (hostFrame (), SoccerRefereeSimul.this);
					win.setVisible (true);
				}
			});
	}

	/** The simulator this referee looks at; its world is the one the zones are read from. */
	public void simulator (Simulator sim)
	{
		this.sim	= sim;
		if ((world == null) && (sim != null))		world = sim.getWorld ();
	}

	protected void close_gfx ()
	{
		dispose_window (win);
		win	= null;
	}

	/* ------------------------------------------------------------------ */
	/* What is kept                                                        */
	/* ------------------------------------------------------------------ */

	/** The goals of a team (0 or 1). */
	public int						score (int team)	{ return score[team]; }
	/** How many faults there have been. */
	public int						faults ()			{ return faults; }
	public String					teamName (int team)	{ return teamNames[team]; }
	public Color					teamColor (int team){ return (team == 0) ? C_TEAM1 : C_TEAM2; }

	/** The team of a robot (0 or 1), or -1 while it is not known. */
	public int teamOf (int robot)
	{
		return ((robot >= 0) && (robot < teamOf.length)) ? teamOf[robot] : -1;
	}

	protected void restart ()
	{
		score[0]	= 0;
		score[1]	= 0;
		faults		= 0;
		forget ();
	}

	/** Nothing is remembered of where the ball and the robots were: the play starts afresh, at a kick-off. */
	protected void forget ()
	{
		inField		= null;
		inNet[0]	= null;
		inNet[1]	= null;
		kickoff		= true;
		for (Boolean[] a : inArea)		{ a[0] = null;	a[1] = null; }
	}

	/* ------------------------------------------------------------------ */
	/* Refereeing                                                          */
	/* ------------------------------------------------------------------ */

	protected void supervise (long ctime)
	{
		robots ();

		SimObject	ball = ball ();
		WMZone		field = zone (fieldName);

		if ((ball == null) || (field == null))		return;

		double		x = ball.odesc.pos.x (), y = ball.odesc.pos.y (), r = ball.radius;

		// the kick-off is over once a robot touches the ball with the whole of it
		// outside the centre circle: what is shot from inside it is a kick-off shot
		long		touch = (ball instanceof SimMobileObject) ? ((SimMobileObject) ball).touchedAt : 0;

		if (touch != touchSeen)
		{
			touchSeen	= touch;
			if (kickoff && (Math.hypot (x - field.area.getCenterX (), y - field.area.getCenterY ()) > circle + r))
				kickoff	= false;
		}

		// in a net, the whole of it: a goal for the team that attacks it, once, when it gets in
		for (int net = 0; net < 2; net++)
		{
			WMZone		z = zone (netNames[net]);

			if (z == null)							continue;

			Boolean		in = Boolean.valueOf (z.area.contains (x - r, y - r, 2 * r, 2 * r));

			if ((inNet[net] != null) && !inNet[net].booleanValue () && in.booleanValue ())
			{
				int		team = 1 - net;				// the red team owns the red net (Net1): a ball in it is the blue team's goal

				if (kickoff)
					announce ("NO GOAL: kick-off shot into " + netNames[net] + " (" + teamNames[net] + " net), the ball was not touched outside the centre circle" + toucher (ball));
				else
				{
					score[team]++;
					announce ("GOAL for " + teamNames[team] + ": ball in " + netNames[net] + " (" + teamNames[net] + " net)" + toucher (ball)
							  + "  --  " + teamNames[0] + " " + score[0] + " - " + score[1] + " " + teamNames[1]);
					changed ();
				}
				kickOff (ball, field);
				return;
			}
			inNet[net]	= in;
		}

		// the whole ball out of the field: a fault, once, when it leaves, and the ball
		// put back still where the rules say
		Boolean		on = Boolean.valueOf (field.area.intersects (x - r, y - r, 2 * r, 2 * r));

		if ((inField != null) && inField.booleanValue () && !on.booleanValue ())
		{
			double[]	back = putBack (ball, field, x, y);

			faults++;
			place (ball, back[0], back[1]);
			announce ("FAULT: ball out over the " + (sideOut (field, x, y) ? "side" : "end") + " line at (" + fmt (x) + ", " + fmt (y) + ")"
					  + toucher (ball) + ", put back at (" + fmt (back[0]) + ", " + fmt (back[1]) + ")  --  faults: " + faults);
			on	= Boolean.TRUE;								// it is in the field again, and nothing to say about it
		}
		inField	= on;
	}

	/**
	 * Where a ball that went out is put back, after the rules. Out over a side
	 * line: on the throw-in line, at the point it went out moved one metre back
	 * towards the goal of the team that touched it last, and never nearer than a
	 * metre to the ends. Out over an end line: at the corner kick point when the
	 * team that defends that end touched it last, on the halfway line when the
	 * other team did, and a metre in from the end when nobody knows; on the side
	 * the ball went out either way.
	 */
	protected double[] putBack (SimObject ball, WMZone field, double x, double y)
	{
		boolean		alongY = alongY (field);					// the nets are at the ends of the y axis (or of the x axis)
		double		cu = alongY ? field.area.getCenterY () : field.area.getCenterX ();	// along the field, towards the nets
		double		cv = alongY ? field.area.getCenterX () : field.area.getCenterY ();	// across it
		double		halfU = (alongY ? field.area.getHeight () : field.area.getWidth ()) / 2.0;
		double		halfV = (alongY ? field.area.getWidth () : field.area.getHeight ()) / 2.0;
		double		u = (alongY ? y : x) - cu, v = (alongY ? x : y) - cv;
		int			last = lastTeam (ball);
		double		pu, pv;

		pv	= Math.signum (v == 0.0 ? 1.0 : v) * (halfV - throwIn);		// the throw-in line, on the side the ball went out
		if (sideOut (field, x, y))
		{
			pu	= u;
			if (last >= 0)		pu += BACK * goalSide (last, field);	// back towards the goal of the team that touched it last
			pu	= Math.max (-(halfU - END_MARGIN), Math.min (halfU - END_MARGIN, pu));
		}
		else
		{
			double	end = Math.signum (u);							// which end it went out over
			int		defender = (end == goalSide (0, field)) ? 0 : 1;	// the team whose net is at that end

			if (last < 0)					pu = end * (halfU - END_MARGIN);
			else if (last == defender)		pu = end * (halfU - corner);
			else							pu = 0.0;
		}
		return alongY ? new double[] { cv + pv, cu + pu } : new double[] { cu + pu, cv + pv };
	}

	/** Whether a ball at (x, y) is out over a side line (as against an end line, past the nets). */
	protected boolean sideOut (WMZone field, double x, double y)
	{
		boolean		alongY = alongY (field);
		double		du = alongY ? Math.abs (y - field.area.getCenterY ()) - field.area.getHeight () / 2.0
								: Math.abs (x - field.area.getCenterX ()) - field.area.getWidth () / 2.0;
		double		dv = alongY ? Math.abs (x - field.area.getCenterX ()) - field.area.getWidth () / 2.0
								: Math.abs (y - field.area.getCenterY ()) - field.area.getHeight () / 2.0;

		return dv > du;											// further out across the field than along it
	}

	/** Whether the nets are at the ends of the y axis of the field (else of the x axis). */
	protected boolean alongY (WMZone field)
	{
		WMZone		n = zone (netNames[0]);

		if (n == null)		return field.area.getHeight () >= field.area.getWidth ();
		return Math.abs (n.area.getCenterY () - field.area.getCenterY ()) >= Math.abs (n.area.getCenterX () - field.area.getCenterX ());
	}

	/** Which end of the field (+1 or -1, along it) the net of a team is at. */
	protected double goalSide (int team, WMZone field)
	{
		WMZone		n = zone (netNames[team]);

		if (n == null)		return (team == 0) ? 1.0 : -1.0;
		return alongY (field) ? Math.signum (n.area.getCenterY () - field.area.getCenterY ())
							  : Math.signum (n.area.getCenterX () - field.area.getCenterX ());
	}

	/** A goal or a kick-off shot: the ball still at the centre, every robot back where it starts, and the kick-off on. */
	protected void kickOff (SimObject ball, WMZone field)
	{
		Simulator	s = sim;

		place (ball, field.area.getCenterX (), field.area.getCenterY ());
		if (s != null)
			for (int r = 0; r < s.numrobots; r++)		s.restartRobot (r);
		forget ();
		announce ("Kick-off: ball at the centre, robots at their start positions");
	}

	/** Puts the ball somewhere, still. */
	protected void place (SimObject ball, double x, double y)
	{
		int		i = index (ball);

		if ((sim != null) && (i >= 0))		sim.placeObject (i, x, y, ball.odesc.a);
	}

	/**
	 * The robots: each is put on a team the first time it is seen, and one in the
	 * area of a net is a fault unless it is the keeper of the team that owns the
	 * net -- the one robot of that team allowed there, the first one in; a second
	 * one of the same team is a fault as well.
	 */
	protected void robots ()
	{
		Simulator	s = sim;

		if (s == null)								return;
		for (int r = 0; r < s.numrobots; r++)
		{
			if (s.MODEL[r] == null)					continue;

			double		x = s.MODEL[r].real_x, y = s.MODEL[r].real_y;

			if (teamOf[r] < 0)						teamOf[r] = team (r, x, y);
			for (int net = 0; net < 2; net++)
			{
				Boolean		in = in (areaNames[net], x, y);

				if (in == null)						continue;
				if ((inArea[r][net] != null) && !inArea[r][net].booleanValue () && in.booleanValue ())
				{
					String	who = robot (r);

					if (teamOf[r] != net)			// not of the team that owns the net: it has no business there
					{
						faults++;
						announce ("FAULT: robot " + who + " in " + areaNames[net] + " (" + teamNames[net] + " net): only the "
								  + teamNames[net] + " keeper may be there  --  faults: " + faults);
					}
					else if (keeperIn (net, r))		// of the team, but the keeper is in already
					{
						faults++;
						announce ("FAULT: robot " + who + " in " + areaNames[net] + " (" + teamNames[net] + " net) with the keeper already there: only one may be  --  faults: " + faults);
					}
				}
				inArea[r][net]	= in;
			}
		}
	}

	/** Whether a robot of the team that owns a net, other than this one, is in its area. */
	protected boolean keeperIn (int net, int robot)
	{
		for (int r = 0; r < inArea.length; r++)
			if ((r != robot) && (teamOf[r] == net) && (inArea[r][net] != null) && inArea[r][net].booleanValue ())		return true;
		return false;
	}

	/**
	 * The team of a robot: the one the settings name it in, or the one whose net
	 * is nearer to where it is first seen (the half of the field it starts in).
	 */
	protected int team (int robot, double x, double y)
	{
		String		name = (sim != null) ? sim.robotName (robot) : null;

		for (int t = 0; t < 2; t++)
			if ((teamRobots[t] != null) && (name != null))
				for (String n : teamRobots[t])
					if (n.equalsIgnoreCase (name))	return t;

		WMZone		z0 = zone (netNames[0]);
		WMZone		z1 = zone (netNames[1]);

		if ((z0 == null) || (z1 == null))			return -1;

		double		d0 = Math.hypot (x - z0.area.getCenterX (), y - z0.area.getCenterY ());
		double		d1 = Math.hypot (x - z1.area.getCenterX (), y - z1.area.getCenterY ());

		return (d0 <= d1) ? 0 : 1;
	}

	/* ------------------------------------------------------------------ */
	/* What is looked at                                                   */
	/* ------------------------------------------------------------------ */

	/** The simulated ball, or null while there is none to look at. */
	protected SimObject ball ()
	{
		Simulator	s = sim;

		if ((s == null) || (s.objects == null))		return null;
		for (int i = 0; i < s.objects.numobjects; i++)
		{
			SimObject	o = s.objects.OBJS[i];

			if ((o != null) && (o.odesc != null) && ballName.equalsIgnoreCase (o.odesc.label))		return o;
		}
		return null;
	}

	/** Which of the objects of the simulator one is, or -1. */
	protected int index (SimObject o)
	{
		Simulator	s = sim;

		if ((s == null) || (s.objects == null))		return -1;
		for (int i = 0; i < s.objects.numobjects; i++)
			if (s.objects.OBJS[i] == o)				return i;
		return -1;
	}

	/** The robot that touched the ball last (its number in the simulator), or -1 for none yet. */
	protected int lastRobot (SimObject ball)
	{
		return (ball instanceof SimMobileObject) ? ((SimMobileObject) ball).touchedBy : -1;
	}

	/** The team of the robot that touched the ball last, or -1 when there is none or it is not known. */
	protected int lastTeam (SimObject ball)
	{
		int		r = lastRobot (ball);

		return (r >= 0) ? teamOf (r) : -1;
	}

	/** ", last touched by <robot> (<team>)", or nothing when no robot has touched the ball. */
	protected String toucher (SimObject ball)
	{
		int		r = lastRobot (ball);

		return (r >= 0) ? (", last touched by " + robot (r)) : "";
	}

	/** A robot as it is named in the decisions: its name and its team. */
	protected String robot (int r)
	{
		int		t = teamOf (r);

		return ((sim != null) ? sim.robotName (r) : ("robot " + r)) + ((t >= 0) ? (" (" + teamNames[t] + ")") : "");
	}

	/** A zone of the world, or null when it has no such zone. */
	protected WMZone zone (String name)
	{
		return (world != null) ? world.zones ().at (name) : null;
	}

	/** Whether a point is in a zone of the world, or null when the world has no such zone. */
	protected Boolean in (String zone, double x, double y)
	{
		WMZone		z = zone (zone);

		if (z == null)								return null;
		return Boolean.valueOf (z.area.contains (x, y));
	}

	/** Names given comma separated, or null when none were. */
	static private String[] names (String list)
	{
		if ((list == null) || (list.trim ().length () == 0))		return null;

		String[]	n = list.trim ().split ("[,;\\s]+");

		return (n.length > 0) ? n : null;
	}

	static private String fmt (double v)			{ return String.format ("%.2f", v); }
}
