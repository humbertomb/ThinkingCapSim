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
import tcapps.tcsimulator.simulator.objects.SimObject;
import tcrob.umu.quaky2.gui.SoccerRefereeWindow;

/**
 * The referee of a simulated soccer match: it looks at where the simulator
 * really has the ball and the robots and decides on them. The ball leaving the
 * field (the zone FIELD) is a fault; the ball getting inside a net (the zones
 * NET1, NET2, Net1Inside and Net2Inside) is a goal for the team that attacks
 * that net, the red team owning the red net (Net1) and the blue team the blue
 * one (Net2). A robot in the area of a net (AREA1, AREA2, Net1Area and
 * Net2Area) is a fault unless it is the one robot that defends that net: the
 * keeper of the team that owns it. Each is decided once, when it happens, and
 * not again until the ball or the robot has left where it was.
 *
 * Which team a robot is on is said by name (ROBOTS1, ROBOTS2) or, failing
 * that, taken from the half of the field it is first seen in: the one with
 * the red net is the red team's.
 *
 * It needs the simulator ({@link Simulated}), which the execution gives it when
 * the architecture runs in one; the world it reads the zones from is the
 * simulator's. With local graphics it opens the referee's window
 * ({@link SoccerRefereeWindow}): the score, the clock of the match and the
 * decisions as they are made.
 *
 * <pre>
 *   BALL        the animated object that is the ball (ball)
 *   FIELD       the zone of the field (Field)
 *   NET1, NET2  the zones inside the nets, where a ball is a goal (Net1Inside, Net2Inside)
 *   AREA1, AREA2  the areas of the nets, where only the keeper may be (Net1Area, Net2Area)
 *   TEAM1, TEAM2  what the teams are called (Red, Blue)
 *   ROBOTS1, ROBOTS2  the robots of each team, by name, comma separated (by default, by the half they start in)
 *   DURATION    the match, in seconds (600)
 * </pre>
 */
public class SoccerSupervisorSimul extends Supervisor implements Simulated
{
	static public final Color		C_TEAM1		= new Color (200, 30, 30);
	static public final Color		C_TEAM2		= new Color (30, 70, 200);

	protected Simulator				sim;
	protected String				ballName;
	protected String				fieldName;
	protected String[]				netNames	= new String[2];
	protected String[]				areaNames	= new String[2];
	protected String[]				teamNames	= new String[2];
	protected String[][]			teamRobots	= new String[2][];		// by name, when the settings say; null for by the half they start in

	// What is decided and kept
	protected final int[]			score		= new int[2];
	protected int					faults;

	// Where the ball was last seen, to decide once on each thing that happens
	protected Boolean				inField;				// null: not looked at yet
	protected Boolean[]				inNet		= new Boolean[2];
	protected int[]					teamOf		= new int[Simulator.MAX_ROBOTS];		// the team of each robot, -1 while not known
	protected Boolean[][]			inArea		= new Boolean[Simulator.MAX_ROBOTS][2];	// which robots are in which areas

	protected SoccerRefereeWindow	win;

	public SoccerSupervisorSimul (ModuleConfig config, Linda linda)
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
		java.util.Arrays.fill (teamOf, -1);

		if (localgfx)
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;
					win	= new SoccerRefereeWindow (hostFrame (), SoccerSupervisorSimul.this);
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
	/** How many times the ball has left the field. */
	public int						faults ()			{ return faults; }
	public String					teamName (int team)	{ return teamNames[team]; }
	public Color					teamColor (int team){ return (team == 0) ? C_TEAM1 : C_TEAM2; }

	protected void restart ()
	{
		score[0]	= 0;
		score[1]	= 0;
		faults		= 0;
		inField		= null;
		inNet[0]	= null;
		inNet[1]	= null;
		for (Boolean[] a : inArea)		{ a[0] = null;	a[1] = null; }
	}

	/** The team of a robot (0 or 1), or -1 while it is not known. */
	public int teamOf (int robot)
	{
		return ((robot >= 0) && (robot < teamOf.length)) ? teamOf[robot] : -1;
	}

	/* ------------------------------------------------------------------ */
	/* Refereeing                                                          */
	/* ------------------------------------------------------------------ */

	protected void supervise (long ctime)
	{
		robots ();

		SimObject	ball = ball ();

		if (ball == null)							return;

		double		x = ball.odesc.pos.x (), y = ball.odesc.pos.y ();

		// out of the field: a fault, once, when it leaves -- a ball inside a net has
		// not left the field, it has got into the net
		Boolean		field = in (fieldName, x, y);

		if (field != null)
		{
			for (int net = 0; net < 2; net++)
			{
				Boolean	n = in (netNames[net], x, y);

				if ((n != null) && n.booleanValue ())		field = Boolean.TRUE;
			}
			if ((inField != null) && inField.booleanValue () && !field.booleanValue ())
			{
				faults++;
				announce ("FAULT: ball out of the field at (" + fmt (x) + ", " + fmt (y) + ")  --  faults: " + faults);
			}
			else if ((inField != null) && !inField.booleanValue () && field.booleanValue ())
				announce ("Ball back in the field");
			inField	= field;
		}

		// in a net: a goal for the team that attacks it, once, when it gets in
		for (int net = 0; net < 2; net++)
		{
			Boolean		in = in (netNames[net], x, y);

			if (in == null)							continue;
			if ((inNet[net] != null) && !inNet[net].booleanValue () && in.booleanValue ())
			{
				int		team = 1 - net;				// the red team owns the red net (Net1): a ball in it is the blue team's goal

				score[team]++;
				announce ("GOAL for " + teamNames[team] + ": ball in " + netNames[net] + " (" + teamNames[net] + " net)"
						  + "  --  " + teamNames[0] + " " + score[0] + " - " + score[1] + " " + teamNames[1]);
				changed ();
			}
			inNet[net]	= in;
		}
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
					String	who = s.robotName (r) + ((teamOf[r] >= 0) ? (" (" + teamNames[teamOf[r]] + ")") : "");

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

		WMZone		z0 = (world != null) ? world.zones ().at (netNames[0]) : null;
		WMZone		z1 = (world != null) ? world.zones ().at (netNames[1]) : null;

		if ((z0 == null) || (z1 == null))			return -1;

		double		d0 = Math.hypot (x - z0.area.getCenterX (), y - z0.area.getCenterY ());
		double		d1 = Math.hypot (x - z1.area.getCenterX (), y - z1.area.getCenterY ());

		return (d0 <= d1) ? 0 : 1;
	}

	/** Names given comma separated, or null when none were. */
	static private String[] names (String list)
	{
		if ((list == null) || (list.trim ().length () == 0))		return null;

		String[]	n = list.trim ().split ("[,;\\s]+");

		return (n.length > 0) ? n : null;
	}

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

	/** Whether a point is in a zone of the world, or null when the world has no such zone. */
	protected Boolean in (String zone, double x, double y)
	{
		WMZone		z = (world != null) ? world.zones ().at (zone) : null;

		if (z == null)								return null;
		return Boolean.valueOf (z.area.contains (x, y));
	}

	static private String fmt (double v)			{ return String.format ("%.2f", v); }
}
