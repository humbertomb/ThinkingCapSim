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
 * really has the ball and decides on it. The ball leaving the field (the zone
 * FIELD) is a fault; the ball getting into the area of a net (the zones NET1,
 * NET2) is a goal for the team that attacks that net, the red team owning the
 * red net (Net1) and the blue team the blue one (Net2). Each is decided once,
 * when it happens, and not again until the ball has left where it was.
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
 *   NET1, NET2  the zones of the nets (Net1Area, Net2Area)
 *   TEAM1, TEAM2  what the teams are called (Red, Blue)
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
	protected String[]				teamNames	= new String[2];

	// What is decided and kept
	protected final int[]			score		= new int[2];
	protected int					faults;

	// Where the ball was last seen, to decide once on each thing that happens
	protected Boolean				inField;				// null: not looked at yet
	protected Boolean[]				inNet		= new Boolean[2];

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
		netNames[0]		= cfg.get ("NET1", "Net1Area");
		netNames[1]		= cfg.get ("NET2", "Net2Area");
		teamNames[0]	= cfg.get ("TEAM1", "Red");
		teamNames[1]	= cfg.get ("TEAM2", "Blue");

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
	}

	/* ------------------------------------------------------------------ */
	/* Refereeing                                                          */
	/* ------------------------------------------------------------------ */

	protected void supervise (long ctime)
	{
		SimObject	ball = ball ();

		if (ball == null)							return;

		double		x = ball.odesc.pos.x (), y = ball.odesc.pos.y ();

		// out of the field: a fault, once, when it leaves
		Boolean		field = in (fieldName, x, y);

		if (field != null)
		{
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
