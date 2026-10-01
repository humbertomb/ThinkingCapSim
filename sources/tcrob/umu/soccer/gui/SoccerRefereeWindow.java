/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.soccer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import tc.modules.Supervisor;
import tcrob.umu.soccer.SoccerRefereeSimul;

/**
 * The referee's window of a simulated soccer match, as a stadium shows it:
 * the score at the top, in big figures under the flags of the two teams (the
 * red team's all red, the blue team's all blue, as their nets are), the clock
 * of the match in the middle, counting down from what the match lasts and
 * running from START, and at the bottom the decisions as they are made, one
 * line each with the time of the match, as a ticker.
 *
 * It only reads: the referee ({@link SoccerRefereeSimul}) says when it
 * decides something, and the clock is looked at on its own every
 * {@link #PERIOD} milliseconds.
 */
public class SoccerRefereeWindow extends JFrame implements Supervisor.Listener
{
	private static final long		serialVersionUID = 1L;

	/** How often the clock is looked at [ms]. */
	static public final int			PERIOD		= 200;

	static private final Color		C_BOARD		= new Color (24, 28, 36);		// the board, dark as the ones of the stadiums
	static private final Color		C_DIGITS	= new Color (250, 250, 240);
	static private final Color		C_CLOCK		= new Color (255, 214, 60);
	static private final Color		C_CLOCK_OVER	= new Color (230, 70, 70);
	static private final Color		C_TICKER	= new Color (12, 14, 18);
	static private final Color		C_TICKER_TEXT	= new Color (120, 230, 120);
	static private final Color		C_DASH		= new Color (150, 150, 150);

	protected SoccerRefereeSimul	referee;
	protected Scoreboard			board;
	protected JLabel				clock;
	protected JLabel				state;						// the state of the game, over the clock
	protected JLabel				countdown;					// the seconds left of INITIAL, READY and SET, at the right of the clock
	protected JTextArea				ticker;
	protected Timer					timer;
	protected int					shown;						// how many decisions the ticker has

	public SoccerRefereeWindow (JFrame host, SoccerRefereeSimul referee)
	{
		super ("Soccer Referee" + ((referee.robot () != null) ? (" [" + referee.robot () + "]") : ""));

		this.referee	= referee;

		// the score, under the flags
		board	= new Scoreboard ();

		// the clock of the match
		clock	= new JLabel (Supervisor.clock (referee.remaining ()), SwingConstants.CENTER);
		clock.setFont (new Font (Font.MONOSPACED, Font.BOLD, 84));
		clock.setForeground (C_CLOCK);
		clock.setOpaque (true);
		clock.setBackground (C_BOARD);
		clock.setBorder (BorderFactory.createEmptyBorder (6, 12, 12, 12));

		// the decisions, as a ticker
		ticker	= new JTextArea (8, 60);
		ticker.setEditable (false);
		ticker.setLineWrap (true);
		ticker.setWrapStyleWord (true);
		ticker.setFont (new Font (Font.MONOSPACED, Font.PLAIN, 14));
		ticker.setBackground (C_TICKER);
		ticker.setForeground (C_TICKER_TEXT);
		ticker.setCaretColor (C_TICKER_TEXT);
		ticker.setBorder (BorderFactory.createEmptyBorder (6, 8, 6, 8));
		for (Supervisor.Decision d : referee.decisions ())		line (d);

		// the state of the game, small, over the clock
		state	= new JLabel (referee.state ().name (), SwingConstants.CENTER);
		state.setFont (new Font (Font.SANS_SERIF, Font.BOLD, 18));
		state.setForeground (C_DASH);
		state.setOpaque (true);
		state.setBackground (C_BOARD);

		// the seconds left of INITIAL, READY and SET, small, at the right of the clock and on its baseline
		countdown	= new JLabel ("", SwingConstants.LEFT);
		countdown.setFont (new Font (Font.MONOSPACED, Font.BOLD, 36));
		countdown.setForeground (Color.WHITE);

		FontMetrics	cm = countdown.getFontMetrics (countdown.getFont ());
		Dimension	room = new Dimension (cm.stringWidth ("888") + 12, cm.getHeight ());		// for up to three digits, also while it says nothing
		JLabel		filler = new JLabel ("");							// as wide as the room at the right of the clock, so the clock stays centred

		countdown.setPreferredSize (room);
		countdown.setHorizontalAlignment (SwingConstants.CENTER);
		filler.setPreferredSize (new Dimension (room.width, 1));

		JPanel		row = new JPanel (new GridBagLayout ());
		GridBagConstraints	gc = new GridBagConstraints ();

		row.setBackground (C_BOARD);
		// the clock in the middle, and the countdown half way between it and the right side of the panel:
		// the two sides share what room is left over alike, and the countdown sits in the middle of its own
		gc.anchor	= GridBagConstraints.BASELINE;
		gc.weightx	= 1.0;
		gc.gridx	= 0;		row.add (filler, gc);
		gc.weightx	= 0.0;
		gc.gridx	= 1;		row.add (clock, gc);
		gc.weightx	= 1.0;
		gc.gridx	= 2;		row.add (countdown, gc);

		JPanel		clocks = new JPanel (new BorderLayout ());

		clocks.setBackground (C_BOARD);
		clocks.add (state, BorderLayout.NORTH);
		clocks.add (row, BorderLayout.CENTER);

		JPanel		top = new JPanel (new BorderLayout ());

		top.setBackground (C_BOARD);
		top.add (board, BorderLayout.CENTER);
		top.add (clocks, BorderLayout.SOUTH);

		JScrollPane	scroll = new JScrollPane (ticker);

		scroll.setBorder (BorderFactory.createEmptyBorder ());
		scroll.setPreferredSize (new Dimension (640, 190));

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (top, BorderLayout.CENTER);
		getContentPane ().add (scroll, BorderLayout.SOUTH);
		getContentPane ().setBackground (C_BOARD);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
		pack ();
		setSize (new Dimension (Math.max (640, getWidth ()), 560));
		if (host != null)			beside (host);
		else						setLocationRelativeTo (null);

		referee.addListener (this);
		timer	= new Timer (PERIOD, new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ refresh (); }
		});
		timer.setRepeats (true);
		timer.start ();
	}

	/** Against the bottom left of the window that runs the simulation, on the screen if it fits. */
	protected void beside (JFrame host)
	{
		java.awt.Rectangle	screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment ().getMaximumWindowBounds ();
		int					x = host.getX ();
		int					y = host.getY () + host.getHeight ();

		if ((y + getHeight ()) > (screen.y + screen.height))			y = Math.max (screen.y, screen.y + screen.height - getHeight ());
		if ((x + getWidth ()) > (screen.x + screen.width))				x = Math.max (screen.x, screen.x + screen.width - getWidth ());
		setLocation (x, y);
	}

	/* ------------------------------------------------------------------ */
	/* What the referee says                                               */
	/* ------------------------------------------------------------------ */

	public void decided (final Supervisor.Decision d)
	{
		SoccerSounds.play (d.cue);									// the whistle, when the decision has one
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ line (d);	board.repaint (); }
		});
	}

	public void changed ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ refresh (); }
		});
	}

	/** One more line of the ticker, and the ticker on it. */
	protected void line (Supervisor.Decision d)
	{
		ticker.append ("[" + d.when () + "]  " + d.text + "\n");
		ticker.setCaretPosition (ticker.getDocument ().getLength ());
		shown++;
	}

	/** The clock and the score as they are now; a ticker with more than the referee remembers (RESET) starts over. */
	public void refresh ()
	{
		long		left = referee.remaining ();
		java.util.List<Supervisor.Decision>	all = referee.decisions ();

		if (all.size () < shown)
		{
			ticker.setText ("");
			shown	= 0;
			for (Supervisor.Decision d : all)		line (d);
		}

		clock.setText ((left >= 0) ? Supervisor.clock (left) : Supervisor.clock (referee.elapsed ()));
		clock.setForeground (referee.isOver () ? C_CLOCK_OVER : C_CLOCK);
		state.setText (referee.state ().name ());

		long		wait = referee.stateLeft ();

		countdown.setText ((wait >= 0) ? Long.toString ((wait + 999) / 1000) : "");
		board.repaint ();
	}

	public void dispose ()
	{
		if (timer != null)			timer.stop ();
		referee.removeListener (this);
		super.dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* The scoreboard                                                      */
	/* ------------------------------------------------------------------ */

	/** The two flags, the goals of each team in big figures under them and the dash between. */
	protected class Scoreboard extends JPanel
	{
		private static final long	serialVersionUID = 1L;

		Scoreboard ()
		{
			setBackground (C_BOARD);
			setPreferredSize (new Dimension (640, 210));
		}

		protected void paintComponent (Graphics g0)
		{
			super.paintComponent (g0);

			Graphics2D	g = (Graphics2D) g0;

			g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int			w = getWidth (), h = getHeight ();
			int			half = w / 2;
			int			flagW = Math.min (150, half - 40), flagH = flagW * 2 / 3;
			int			top = 18;

			for (int team = 0; team < 2; team++)
			{
				int		cx = (team == 0) ? (half / 2) : (half + half / 2);

				// the flag, all of the colour of the team, with a thin light edge
				g.setColor (referee.teamColor (team));
				g.fillRoundRect (cx - flagW / 2, top, flagW, flagH, 8, 8);
				g.setColor (new Color (255, 255, 255, 90));
				g.drawRoundRect (cx - flagW / 2, top, flagW, flagH, 8, 8);

				// the name under it
				g.setFont (new Font (Font.SANS_SERIF, Font.BOLD, 16));
				g.setColor (C_DASH);
				centred (g, referee.teamName (team), cx, top + flagH + 20);

				// and the goals, in big figures
				g.setFont (new Font (Font.SANS_SERIF, Font.BOLD, 96));
				g.setColor (C_DIGITS);
				centred (g, String.valueOf (referee.score (team)), cx, h - 12);
			}

			// the dash between the two
			g.setFont (new Font (Font.SANS_SERIF, Font.BOLD, 64));
			g.setColor (C_DASH);
			centred (g, "-", half, h - 24);
		}

		private void centred (Graphics2D g, String s, int cx, int baseline)
		{
			FontMetrics	fm = g.getFontMetrics ();

			g.drawString (s, cx - fm.stringWidth (s) / 2, baseline);
		}
	}
}
