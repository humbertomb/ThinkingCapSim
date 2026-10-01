/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.soccer;

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
import tcrob.umu.soccer.gui.SoccerRefereeWindow;
import tcrob.umu.soccer.gui.SoccerSounds;
import tcrob.umu.soccer.linda.ItemReferee;
import tcrob.umu.soccer.linda.ItemReferee.Events;
import tcrob.umu.soccer.linda.ItemReferee.GameStates;
import tcrob.umu.soccer.linda.SoccerTuple;
import tc.shared.linda.Tuple;

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
 * <li>After a goal (or a kick-off shot) the play restarts: the game READY, in
 *     which the robots walk back to their start positions on their own
 *     (chaos.getStartPos), as the rules have it, and the ball put still at the
 *     centre on SET, once they are there (in READY they would knock it away).
 * <li>The whole ball out of the field (the zone FIELD) is a fault, and the ball
 *     is put back still where the rules say: out over a side line, on the
 *     throw-in line at the point it went out, one metre back towards the goal
 *     of the team that touched it last, never nearer than one metre to the
 *     ends; out over an end line, at the corner kick point when the defending
 *     team touched it last, on the halfway line (same side) when the attacking
 *     team did, one metre in from the end line when nobody knows.
 * <li>A robot wholly inside the area of a net (AREA1, AREA2) for more than
 *     {@link #AREA_TIME} is a fault unless it is the one robot that defends
 *     that net, the keeper of the team that owns it.
 * <li>A robot that is not in its own half of the field when the game is to be
 *     SET (its centre on the other side of the halfway line) is put back in its
 *     half, where it was across the field and {@link #OFFSIDE_BACK} of the way
 *     from the halfway line to its end line, and told it is PENALIZED for it;
 *     but it is not out of the game for {@link #PENALTY_TIME}: the SET that
 *     follows puts it back in, and it plays from there.
 * </ul>
 * Each is decided once, when it happens, and not again until the ball or the
 * robot has left where it was. The robot that touched the ball last is named
 * in every decision about the ball.
 *
 * Which team a robot is on is said by name (ROBOTS1, ROBOTS2) or, failing
 * that, taken from the half of the field it is first seen in: the one with
 * the red net is the red team's.
 *
 * It runs the game as the game controller of the league does, through the
 * states of {@link GameStates}: INITIAL the moment it starts, and then, once the
 * execution runs, READY after {@link #WAIT_INITIAL}, SET after {@link #WAIT_READY}
 * (or as soon as all the robots stand still in their own halves, see {@link #settled})
 * and PLAYING after {@link #WAIT_SET}; after a goal (or a kick-off shot) READY
 * again, and SET and PLAYING after it. The
 * clock of the match stops with READY and goes on with PLAYING; the execution's
 * START and STOP run and hold it only while the game is PLAYING. When the clock
 * runs out the game is FINISHED.
 *
 * Every state and every decision is written to the Linda space as an
 * {@link ItemReferee} (tuple REFEREE), which is what the controllers of the
 * robots read (chaos.getGameState).
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

	/** How long the game stays in INITIAL before READY [ms]: the robots get going. */
	static public final long		WAIT_INITIAL	= 3000;
	/** How long it stays in READY before SET at most [ms]: the robots walk to their start positions (30 s, as the rules have it). */
	static public final long		WAIT_READY		= 30000;
	/** How far a robot may wander and still be standing on a position (m): it may turn, and shuffle that much. */
	static public final double		STILL_DIST		= 0.10;
	/** How long it has to stay so to be standing (ms). */
	static public final long		STILL_TIME		= 2000;
	/** How long it stays in SET before PLAYING [ms]. */
	static public final long		WAIT_SET		= 1000;

	/** The rules: one metre back from where the ball went out, and never nearer than one metre to the ends. */
	static public final double		BACK		= 1.0;
	static public final double		END_MARGIN	= 1.0;

	protected Simulator				sim;
	protected String				ballName;
	protected String				fieldName;
	protected String[]				netNames	= new String[2];
	protected String[]				areaNames	= new String[2];
	protected String[]				teamNames	= new String[2];
	/** How many start points past its own a penalised robot is sent to (START_1 -> START_5). */
	static public final int			PENALTY_START	= 4;
	/** How far back into its half a robot out of it on SET is put: this part of the way from the halfway line to its end line. */
	static public final double		OFFSIDE_BACK	= 3.0 / 5.0;
	/** How long a penalised robot is out of the game [ms] (the standard penalty of the 2007 rules). */
	static public final long		PENALTY_TIME	= 30000;

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
	protected long[][]				inArea		= new long[Simulator.MAX_ROBOTS][2];		// since when each robot is wholly in each area [ms of the system]; 0 when it is not
	protected boolean[][]			areaFault	= new boolean[Simulator.MAX_ROBOTS][2];	// whether its stay there was called already
	protected boolean[][]			robotInNet	= new boolean[Simulator.MAX_ROBOTS][2];	// whether each robot touches the inside of each net (called at once, once per entry)
	/** How long a robot may be wholly inside the area of a net it has no business in before it is a fault [ms]. */
	static public final long		AREA_TIME	= 3000;
	protected long[]				penalty		= new long[Simulator.MAX_ROBOTS];		// when the penalty of each robot ends [ms of the system], 0 for none
	protected double[][]			still		= new double[Simulator.MAX_ROBOTS][];	// where each robot has been standing in READY {x, y, since [ms of the system]}, null: not yet looked at

	protected SoccerRefereeWindow	win;

	// The game, as the game controller runs it
	protected volatile GameStates	state		= GameStates.INITIAL;
	protected long					stateSince;				// when the state was entered [ms of the system]
	protected Tuple					rtuple;					// what the referee says (REFEREE)

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
		rtuple			= new Tuple (SoccerTuple.REFEREE, null);

		// the game starts the moment the referee does, and nothing else for a while
		enter (GameStates.INITIAL, -1);

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

	/** The state of the game, as the game controller has it. */
	public GameStates				state ()			{ return state; }

	/** How long is left [ms] before INITIAL, READY or SET gives way to the next state on its own; -1 in the others. */
	public long stateLeft ()
	{
		long		wait;

		switch (state)
		{
		case INITIAL:		wait = WAIT_INITIAL;	break;
		case READY:			wait = WAIT_READY;		break;
		case SET:			wait = WAIT_SET;		break;
		default:			return -1;
		}
		return Math.max (0L, wait - (System.currentTimeMillis () - stateSince));
	}

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
		enter (GameStates.INITIAL, -1);
	}

	/* ------------------------------------------------------------------ */
	/* The game                                                            */
	/* ------------------------------------------------------------------ */

	/**
	 * The game goes from one state to the next: READY stops the clock of the
	 * match, PLAYING sets it going again (if the execution runs), FINISHED stops
	 * it for good. Whoever listens is told (REFEREE).
	 */
	protected void enter (GameStates s, int player)
	{
		state		= s;
		stateSince	= System.currentTimeMillis ();
		switch (s)
		{
		case SET:			pause ();		centreBall ();		offside ();		break;		// the robots are in place: the ball to the centre, and those out of their half off it
		case READY:			pause ();		java.util.Arrays.fill (still, null);		break;		// where they stand is looked at afresh
		case INITIAL:
		case FINISHED:		pause ();		break;
		case PLAYING:		if (running)	super.resume ();		break;
		default:
		}
		decide (Events.STATE, -1, null, player, "State " + s.name ());
		changed ();
	}

	/** The clock of the match runs only while the game is PLAYING, whatever the execution says. */
	protected void resume ()
	{
		if (state == GameStates.PLAYING)		super.resume ();
	}

	/** What the game does on its own: INITIAL, READY and SET each give way to the next after its wait. */
	protected void game ()
	{
		long	in = System.currentTimeMillis () - stateSince;

		switch (state)
		{
		case INITIAL:		if (in >= WAIT_INITIAL)		enter (GameStates.READY, -1);		break;
		case READY:			if ((in >= WAIT_READY) || settled ())		enter (GameStates.SET, -1);			break;
		case SET:			if (in >= WAIT_SET)			enter (GameStates.PLAYING, -1);		break;
		default:
		}
	}

	/**
	 * Whether all the robots are ready in READY, so the game need not wait for the
	 * whole of it to be SET: each in its own half of the field and standing on a
	 * position -- it may turn, and shuffle within {@link #STILL_DIST} of where it
	 * stopped -- for {@link #STILL_TIME}. What it is looked at is where each robot
	 * stands now, against where it stopped, every time this is asked.
	 */
	protected boolean settled ()
	{
		Simulator	s = sim;
		WMZone		field = zone (fieldName);
		long		now = System.currentTimeMillis ();
		boolean		all = true;
		int			seen = 0;

		if ((s == null) || (field == null))			return false;

		boolean		along = alongY (field);

		for (int r = 0; r < s.numrobots; r++)
		{
			if (s.MODEL[r] == null)					continue;

			double		x = s.MODEL[r].real_x, y = s.MODEL[r].real_y;
			double[]	at = still[r];

			seen++;
			if ((at == null) || (Math.hypot (x - at[0], y - at[1]) > STILL_DIST))
				still[r]	= at = new double[] { x, y, now };			// it moved: it stands here from now on, if it does
			if (now - at[2] < STILL_TIME)			all = false;		// not standing long enough yet

			if (teamOf[r] < 0)						teamOf[r] = team (r, x, y);
			if (teamOf[r] < 0)						{ all = false;	continue; }

			double		u = along ? (y - field.area.getCenterY ()) : (x - field.area.getCenterX ());

			if (u * goalSide (teamOf[r], field) < 0.0)		all = false;	// not in its own half
		}
		return all && (seen > 0);
	}

	/** The clock ran out: the game is over. */
	protected void timeUp ()
	{
		decide (Events.TIME_UP, -1, null, -1, "Time is up");
		enter (GameStates.FINISHED, -1);
	}

	/**
	 * Something decided: said as every decision is ({@link #announce}) and written
	 * to the Linda space with the state of the game, whom it concerns and the
	 * score, for the robots to read.
	 */
	protected void decide (Events event, int team, int robot, String text)
	{
		decide (event, team, (robot >= 0) ? robot (robot) : null, robot, text);
	}

	protected void decide (Events event, int team, String robot, int player, String text)
	{
		decide (event, team, robot, player, text, state);
	}

	/**
	 * The same, saying a state other than the game's: what one player is in
	 * (PENALIZED, and the game's own when it comes back), for that player alone.
	 */
	protected void decide (Events event, int team, String robot, int player, String text, GameStates st)
	{
		ItemReferee		item = new ItemReferee ();

		announce (text, cue (event, st, player));
		item.setState (st, player);
		item.setEvent (event, team, robot, text);
		item.setScore (score[0], score[1], elapsed ());
		item.set (System.currentTimeMillis ());
		if (linda != null)
		{
			rtuple.value	= item;					// a new item every time: a shared space hands the reader the very object
			linda.write (rtuple);
		}
	}

	/** Nothing is remembered of where the ball and the robots were: the play starts afresh, at a kick-off. */
	protected void forget ()
	{
		inField		= null;
		inNet[0]	= null;
		inNet[1]	= null;
		kickoff		= true;
		for (long[] a : inArea)			{ a[0] = 0;	a[1] = 0; }
		for (boolean[] a : areaFault)	{ a[0] = false;	a[1] = false; }
		for (boolean[] a : robotInNet)	{ a[0] = false;	a[1] = false; }
		java.util.Arrays.fill (penalty, 0L);						// a kick-off puts everyone back in the game
	}

	/* ------------------------------------------------------------------ */
	/* Refereeing                                                          */
	/* ------------------------------------------------------------------ */

	protected void supervise (long ctime)
	{
		teams ();
		game ();
		if (state != GameStates.PLAYING)			return;		// nothing is judged until the game is on

		penalties ();
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
					decide (Events.KICKOFF_SHOT, lastTeam (ball), lastRobot (ball),
							"NO GOAL: kick-off shot into " + netNames[net] + " (" + teamNames[net] + " net), the ball was not touched outside the centre circle" + toucher (ball));
				else
				{
					score[team]++;
					decide (Events.GOAL, team, lastRobot (ball),
							"GOAL for " + teamNames[team] + ": ball in " + netNames[net] + " (" + teamNames[net] + " net)" + toucher (ball)
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
			decide (Events.BALL_OUT, lastTeam (ball), lastRobot (ball),
					"FAULT: ball out over the " + (sideOut (field, x, y) ? "side" : "end") + " line at (" + fmt (x) + ", " + fmt (y) + ")"
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

	/**
	 * A goal or a kick-off shot: the kick-off on, and the game READY (the clock
	 * stops), to be SET and PLAYING again after the waits. The robots are not
	 * moved: READY is theirs to walk back to their start positions in, as the
	 * rules have it; the ball goes to the centre on SET (see {@link #centreBall}).
	 */
	protected void kickOff (SimObject ball, WMZone field)
	{
		forget ();
		decide (Events.KICKOFF, -1, null, -1, "Kick-off: robots to their start positions, the ball to the centre when they are set");
		enter (GameStates.READY, -1);
	}

	/**
	 * The ball to the centre of the field, still: done on entering SET, once the
	 * robots have walked to their start positions, and not before -- in READY
	 * they are still on the move and would knock it away.
	 */
	protected void centreBall ()
	{
		SimObject	ball = ball ();
		WMZone		field = zone (fieldName);

		if ((ball != null) && (field != null))		place (ball, field.area.getCenterX (), field.area.getCenterY ());
	}

	/** Puts the ball somewhere, still. */
	protected void place (SimObject ball, double x, double y)
	{
		int		i = index (ball);

		if ((sim != null) && (i >= 0))		sim.placeObject (i, x, y, ball.odesc.a);
	}

	/**
	 * The robots: each is put on a team the first time it is seen, and one wholly
	 * inside the area of a net (the disc of its radius, all of it) for more than
	 * {@link #AREA_TIME} is a fault unless it is the keeper of the team that owns
	 * the net -- the one robot of that team allowed there, the first one in; a
	 * second one of the same team is a fault as well. A robot with a foot over
	 * the line is not in, and one that goes through and out again in time is
	 * left alone: a match with faults at every touch of the area is no match.
	 * The inside of the net is another matter: a robot that gets into it at all
	 * (any part of its disc over the zone), keeper or not, is a fault at once --
	 * otherwise one could cross the area in less than the time allowed and end
	 * up in the goal.
	 */
	protected void robots ()
	{
		Simulator	s = sim;

		if (s == null)								return;
		for (int r = 0; r < s.numrobots; r++)
		{
			if (s.MODEL[r] == null)					continue;

			double		x = s.MODEL[r].real_x, y = s.MODEL[r].real_y;
			double		rr = (s.RDESC[r] != null) ? s.RDESC[r].RADIUS : 0.0;

			if (teamOf[r] < 0)						teamOf[r] = team (r, x, y);
			for (int net = 0; net < 2; net++)
			{
				Boolean		in = wholeIn (areaNames[net], x, y, rr);
				Boolean		net_ = touches (netNames[net], x, y, rr);
				long		now = System.currentTimeMillis ();

				// into the net itself, however little: a fault at once, whoever it is
				if ((net_ != null) && net_.booleanValue ())
				{
					if (!robotInNet[r][net])
					{
						robotInNet[r][net]	= true;
						faults++;
						decide (Events.ILLEGAL_DEFENDER, teamOf[r], r, "FAULT: robot " + robot (r) + " inside the " + teamNames[net] + " net (" + netNames[net] + "): no robot may enter it"
								+ penalize (r) + "  --  faults: " + faults);
						expel (r);
						inArea[r][net]	= 0;
						areaFault[r][net]	= false;
						continue;
					}
				}
				else								robotInNet[r][net] = false;
				if (in == null)						continue;
				if (!in.booleanValue ())			{ inArea[r][net] = 0;	areaFault[r][net] = false;	continue; }
				if (inArea[r][net] == 0)			inArea[r][net] = now;						// just in: the clock of its stay starts
				if (!areaFault[r][net] && (now - inArea[r][net] >= AREA_TIME))
				{
					String	who = robot (r);

					areaFault[r][net]	= true;										// called once per stay

					if (teamOf[r] != net)			// not of the team that owns the net: it has no business there
					{
						faults++;
						decide (Events.ILLEGAL_DEFENDER, teamOf[r], r, "FAULT: robot " + who + " over " + (AREA_TIME / 1000) + " s in " + areaNames[net] + " (" + teamNames[net] + " net): only the "
								+ teamNames[net] + " keeper may be there" + penalize (r) + "  --  faults: " + faults);
						expel (r);
					}
					else if (keeperIn (net, r))		// of the team, but the keeper is in already
					{
						faults++;
						decide (Events.ILLEGAL_DEFENDER, teamOf[r], r, "FAULT: robot " + who + " over " + (AREA_TIME / 1000) + " s in " + areaNames[net] + " (" + teamNames[net] + " net) with the keeper already there: only one may be"
								+ penalize (r) + "  --  faults: " + faults);
						expel (r);
					}
				}
			}
		}
	}

	/**
	 * What a decision sounds like (SoccerSounds): the whistle of a kick-off when
	 * the game goes PLAYING, a short one at a fault of a player or the ball out,
	 * a whistle and applause at a goal, the whistles of the end and the applause
	 * when the time is up; nothing else.
	 */
	protected String cue (Events event, GameStates st, int player)
	{
		switch (event)
		{
		case STATE:				return ((st == GameStates.PLAYING) && (player < 0)) ? SoccerSounds.START : null;
		case GOAL:				return SoccerSounds.GOAL;
		case ILLEGAL_DEFENDER:
		case BALL_OUT:
		case KICKOFF_SHOT:		return SoccerSounds.FAULT;
		case TIME_UP:			return SoccerSounds.END;
		default:				return null;
		}
	}

	/**
	 * A robot penalised is taken off the pitch: to the start point of the world
	 * {@link #PENALTY_START} places past its own (the robot that starts at START_1
	 * is put on START_5), standing as that point says. What was done, for the
	 * message of the fault; nothing when the world has no such point.
	 */
	protected String penalize (int r)
	{
		if (!toPenaltyPoint (r))		return "";
		penalty[r]	= System.currentTimeMillis () + PENALTY_TIME;
		return ", penalised: sent to START_" + (r + PENALTY_START + 1) + " for " + (PENALTY_TIME / 1000) + " s";
	}

	/** A robot to where the penalised ones go (see {@link #penalize}); false when the world has no such point. */
	protected boolean toPenaltyPoint (int r)
	{
		Simulator	s = sim;
		double[]	p = (s != null) ? s.worldStart (r + PENALTY_START) : null;

		if (p == null)					return false;
		s.placeRobot (r, p[0], p[1], p[2]);
		return true;
	}

	/** The team of each robot not known yet, from where it is first seen (where it starts, before it moves). */
	protected void teams ()
	{
		Simulator	s = sim;

		if (s == null)								return;
		for (int r = 0; r < s.numrobots; r++)
			if ((s.MODEL[r] != null) && (teamOf[r] < 0))
				teamOf[r] = team (r, s.MODEL[r].real_x, s.MODEL[r].real_y);
	}

	/**
	 * When the game is to be SET, the robots that are not in their own half of the
	 * field (their centre past the halfway line, towards the net of the other team)
	 * are put back in their half -- where they were across the field, and
	 * {@link #OFFSIDE_BACK} of the way from the halfway line to their end line,
	 * heading as they were -- and told they are PENALIZED for it, but without the
	 * time out of a penalty: the SET said right after (see {@link #enter}) puts
	 * them back in the game, and they play from there.
	 */
	protected void offside ()
	{
		Simulator	s = sim;
		WMZone		field = zone (fieldName);

		if ((s == null) || (field == null))			return;

		boolean		along = alongY (field);

		for (int r = 0; r < s.numrobots; r++)
		{
			if (s.MODEL[r] == null)					continue;

			double		x = s.MODEL[r].real_x, y = s.MODEL[r].real_y;

			if (teamOf[r] < 0)						teamOf[r] = team (r, x, y);
			if (teamOf[r] < 0)						continue;

			double		cu = along ? field.area.getCenterY () : field.area.getCenterX ();
			double		halfU = (along ? field.area.getHeight () : field.area.getWidth ()) / 2.0;
			double		u = (along ? y : x) - cu;
			double		side = goalSide (teamOf[r], field);			// towards its own net

			if (u * side >= 0.0)					continue;		// in its own half (or on the line)

			double		nu = cu + side * OFFSIDE_BACK * halfU;

			if (along)		y = nu;
			else			x = nu;
			s.placeRobot (r, x, y, s.MODEL[r].real_a);
			decide (Events.STATE, teamOf[r], name (r), r, "State PENALIZED for robot " + robot (r) + ": not in its half on SET, put back in it at ("
					+ String.format (java.util.Locale.US, "%.2f, %.2f", x, y) + ") (back in the game with the SET)", GameStates.PENALIZED);
		}
	}

	/**
	 * Tells a robot it is out (PENALIZED, for it alone: its machine of states
	 * stops), after the fault has been told. Its team mates and the other team
	 * read the same tuple, and take it for what it is: a state of that player.
	 */
	protected void expel (int r)
	{
		if (penalty[r] == 0)			return;
		decide (Events.STATE, teamOf[r], name (r), r, "State PENALIZED for robot " + robot (r), GameStates.PENALIZED);
	}

	/** The penalties that are over: the robot is told the game is on for it again, where it is. */
	protected void penalties ()
	{
		long	now = System.currentTimeMillis ();

		for (int r = 0; r < penalty.length; r++)
		{
			if ((penalty[r] == 0) || (now < penalty[r]))		continue;
			penalty[r]	= 0;
			decide (Events.STATE, teamOf[r], name (r), r, "Robot " + robot (r) + " back in the game", state);
		}
	}

	/** Whether a robot of the team that owns a net, other than this one, is in its area. */
	protected boolean keeperIn (int net, int robot)
	{
		for (int r = 0; r < inArea.length; r++)
			if ((r != robot) && (teamOf[r] == net) && (inArea[r][net] != 0))		return true;
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
	/** The bare name of a robot, as its modules know it (the robot of a tuple about one player). */
	protected String name (int r)
	{
		return (sim != null) ? sim.robotName (r) : ("robot " + r);
	}

	/** A robot with its team, for the messages. */
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

	/** Whether any of a disc (centre, radius) is over a zone (its bounding square, near enough); null when there is no such zone. */
	protected Boolean touches (String zone, double x, double y, double r)
	{
		WMZone		z = zone (zone);

		if (z == null)								return null;
		return Boolean.valueOf (z.area.intersects (x - r, y - r, 2 * r, 2 * r));
	}

	/** Whether the whole of a disc (centre, radius) is inside a zone; null when there is no such zone. */
	protected Boolean wholeIn (String zone, double x, double y, double r)
	{
		WMZone		z = zone (zone);

		if (z == null)								return null;
		return Boolean.valueOf (z.area.contains (x - r, y - r, 2 * r, 2 * r));
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
