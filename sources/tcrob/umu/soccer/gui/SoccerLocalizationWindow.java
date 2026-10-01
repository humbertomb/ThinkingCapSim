/*
 * (c) 2026 Humberto Martinez
 */

package tcrob.umu.soccer.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import tclib.utils.pos.Position;
import tcrob.umu.soccer.SoccerLocalization;
import tcrob.umu.soccer.gm.Localisation;
import static tcrob.umu.soccer.gm.Localisation.*;
import tcrob.umu.soccer.gm.data.*;
import wucore.utils.math.Angles;
import wucore.widgets.*;

/**
 * The window of the localisation of a soccer robot (SoccerLocalization), after
 * the simulator of ChaosManager: the current position (the global state the
 * method estimates, and the true one of the simulation), the reduced LPS the
 * method is fed with (a row per object), and the inside of the method (its
 * drawing: the grid, the particles...) over the field of the RoboCup 2007, with
 * what was just used of each object, the estimated robot (its uncertainty) and
 * the true one, and the path of both (View, Show robot path). The green of the
 * field can be left out (View, Show green field). Under the LPS, the method of
 * the module can be changed (Methods): the one chosen is made anew with the
 * initial parameters shown under it, which are the static variables of
 * SoccerLocalization (and Restart makes it anew with them as they are).
 *
 * {@link #update} is called from the thread of the module with the method as it
 * is, and the window draws it on the event thread, holding the method (the
 * module updates it holding it too) while it reads it.
 */
public class SoccerLocalizationWindow extends JFrame
{
	private static final long			serialVersionUID = 1L;

	static public final int				WIN_WIDTH		= 800;
	static public final int				WIN_HEIGHT		= 760;
	static public final int				PATH_SIZE		= 2000;			// the positions kept of each path

	/** The objects of the reduced LPS, by their index there (LocLps). */
	static public final String[]		NAMES			= { "Ball", "Landmark1", "Landmark2", "Net1", "Net2" };
	static public final Color[]			COLORS			= { Color.ORANGE, Color.YELLOW, Color.CYAN, Color.YELLOW, Color.CYAN };
	static public final Color			NET1			= Color.YELLOW;		// the net at +y, as the world has it
	static public final Color			NET2			= Color.CYAN;
	static public final Color			GS_COLOR		= Color.BLUE;
	static public final Color			GT_COLOR		= Color.CYAN;

	protected JTextArea					positionArea;
	protected LpsTableModel				lpsModel;
	protected JTable					lpsTable;
	protected LocalisationView			locView;
	protected boolean					showPaths		= true;		// View menu: the paths of the robots
	protected boolean					showGreen		= true;		// View menu: the green of the field

	protected SoccerLocalization		module;				// the module whose method is shown (null: none to change)
	protected JComboBox<String>			methodBox;
	protected JPanel					paramCards;			// the initial parameters of each method, one card each
	protected CardLayout				cards;

	protected LocLps					lps;				// what is shown
	protected Localisation				loc;

	public SoccerLocalizationWindow (JFrame frame, String title)
	{
		this (frame, title, null);
	}

	/** The window of a module, whose method can be changed in it (Methods). */
	public SoccerLocalizationWindow (JFrame frame, String title, SoccerLocalization module)
	{
		JPanel		pane = (JPanel) getContentPane ();

		this.module		= module;
		JPanel		left, table, local;

		pane.setLayout (new BorderLayout ());

		// Current position
		positionArea	= new JTextArea (5, 30);
		positionArea.setEditable (false);
		positionArea.setFont (new Font (Font.MONOSPACED, Font.PLAIN, 12));

		// The reduced LPS and the localisation method
		lpsModel	= new LpsTableModel ();
		lpsTable	= new JTable (lpsModel);
		lpsTable.setDefaultRenderer (Object.class, new LpsRenderer ());
		lpsTable.setGridColor (Color.LIGHT_GRAY);
		lpsTable.setFillsViewportHeight (true);
		table		= titled ("LPS", new JScrollPane (lpsTable));
		table.setPreferredSize (new Dimension (360, 150));

		locView		= new LocalisationView ();
		local		= titled ("Localisation", locView);

		// on the left the position and the LPS, one over the other; the rest, the localisation
		left		= new JPanel (new BorderLayout ());
		left.add (titled ("Current Position", positionArea), BorderLayout.NORTH);
		left.add (table, BorderLayout.CENTER);
		left.add (methodsPanel (), BorderLayout.SOUTH);
		left.setPreferredSize (new Dimension (360, WIN_HEIGHT));

		JPanel		column = new JPanel (new BorderLayout ());
		column.add (left, BorderLayout.CENTER);

		pane.add (column, BorderLayout.WEST);
		pane.add (local, BorderLayout.CENTER);
		setJMenuBar (menuBar ());

		setTitle (title);
		setSize (new Dimension (WIN_WIDTH, WIN_HEIGHT));
		setLocationRelativeTo (frame);
		// below the window it goes with, level with its left side (and on the screen)
		if (frame != null)
		{
			Rectangle	screen = (frame.getGraphicsConfiguration () != null) ? frame.getGraphicsConfiguration ().getBounds ()
									: new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
			int			x = frame.getX (), y = frame.getY () + 40;

			x	= Math.max (screen.x, Math.min (x, screen.x + screen.width - getWidth ()));
			y	= Math.max (screen.y, Math.min (y, screen.y + screen.height - getHeight ()));
			setLocation (x, y);
		}

		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ setVisible (false); }
		});
	}

	/** The View menu: whether the paths of the robots are drawn (and forgetting them), and the green of the field. */
	protected JMenuBar menuBar ()
	{
		JMenuBar			mb = new JMenuBar ();
		JMenu				view = new JMenu ("View");
		JCheckBoxMenuItem	paths = new JCheckBoxMenuItem ("Show robot path", showPaths);
		JMenuItem			clear = new JMenuItem ("Clear robot path");
		JCheckBoxMenuItem	green = new JCheckBoxMenuItem ("Show green field", showGreen);

		paths.addActionListener (_ -> { showPaths = paths.isSelected ();	locView.repaintAll (); });
		clear.addActionListener (_ -> { locView.clearPaths ();	locView.repaintAll (); });
		green.addActionListener (_ -> { showGreen = green.isSelected ();	locView.repaintAll (); });
		view.add (paths);
		view.add (clear);
		view.addSeparator ();
		view.add (green);
		mb.add (view);
		return mb;
	}

	/**
	 * The methods of localisation, to choose the one of the module from (it is
	 * made anew when the choice changes), and under them the initial parameters
	 * of the one chosen, which can be changed: they are used the next time it is
	 * made (Restart makes it now).
	 */
	protected JPanel methodsPanel ()
	{
		JPanel		p = new JPanel (new BorderLayout (0, 4));
		JPanel		row = new JPanel (new BorderLayout (4, 0));
		JButton		restart = new JButton ("Restart");
		String		current = ((module != null) && (module.method () != null)) ? module.method () : SoccerLocalization.METHOD;

		methodBox	= new JComboBox<> (SoccerLocalization.METHODS);
		methodBox.setSelectedItem (current);
		cards		= new CardLayout ();
		paramCards	= new WidthPanel (cards);
		for (String m : SoccerLocalization.METHODS)
			paramCards.add (paramsPanel (m), m);
		cards.show (paramCards, current);

		methodBox.addActionListener (_ -> { cards.show (paramCards, (String) methodBox.getSelectedItem ());	paramCards.revalidate ();	remake (); });
		restart.addActionListener (_ -> remake ());
		restart.setToolTipText ("The method made anew with the parameters as they are");
		restart.setEnabled (module != null);
		methodBox.setEnabled (module != null);

		row.add (methodBox, BorderLayout.CENTER);
		row.add (restart, BorderLayout.EAST);

		JScrollPane	scroll = new JScrollPane (paramCards, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		scroll.setBorder (BorderFactory.createEmptyBorder ());
		scroll.setPreferredSize (new Dimension (340, 300));
		scroll.getVerticalScrollBar ().setUnitIncrement (16);
		p.add (row, BorderLayout.NORTH);
		p.add (scroll, BorderLayout.CENTER);
		p.setBorder (BorderFactory.createEmptyBorder (2, 4, 4, 4));
		return titled ("Methods", p);
	}

	/** A panel as wide as the scroll pane it is in, which scrolls it only up and down. */
	static protected class WidthPanel extends JPanel implements Scrollable
	{
		private static final long	serialVersionUID = 1L;

		WidthPanel (LayoutManager layout)									{ super (layout); }

		public Dimension getPreferredScrollableViewportSize ()				{ return getPreferredSize (); }
		public int getScrollableUnitIncrement (Rectangle r, int o, int d)	{ return 16; }
		public int getScrollableBlockIncrement (Rectangle r, int o, int d)	{ return r.height; }
		public boolean getScrollableTracksViewportWidth ()					{ return true; }
		public boolean getScrollableTracksViewportHeight ()					{ return false; }

		/** As tall as the card shown (a card layout would have it as tall as the tallest one). */
		public Dimension getPreferredSize ()
		{
			for (Component c : getComponents ())
				if (c.isVisible ())		return c.getPreferredSize ();
			return super.getPreferredSize ();
		}
	}

	/** The method chosen made anew in the module, and the paths drawn so far forgotten. */
	protected void remake ()
	{
		if (module == null)						return;
		module.method ((String) methodBox.getSelectedItem ());
		locView.clearPaths ();
		locView.repaintAll ();
	}

	/** The initial parameters of a method (SoccerLocalization.PARAMS), each with what it is and its value, to be changed. */
	protected JPanel paramsPanel (String method)
	{
		JPanel				p = new JPanel (new GridBagLayout ());
		GridBagConstraints	gc = new GridBagConstraints ();
		int					y = 0;

		gc.insets	= new Insets (1, 2, 1, 2);
		gc.anchor	= GridBagConstraints.WEST;
		for (String[] d : SoccerLocalization.PARAMS)
		{
			if (!d[0].equals (method))			continue;

			java.lang.reflect.Field		f;

			try		{ f = SoccerLocalization.class.getField (d[1]); }
			catch (Exception e)		{ continue; }

			gc.gridy	= y++;
			gc.gridx	= 0;	gc.weightx = 1.0;	gc.fill = GridBagConstraints.HORIZONTAL;
			p.add (new JLabel (d[2]), gc);
			gc.gridx	= 1;	gc.weightx = 0.0;
			p.add ((d.length > 3) ? choice (f, d[3].split (",")) : field (f), gc);
		}
		gc.gridy	= y;	gc.gridx = 0;	gc.weighty = 1.0;
		p.add (Box.createVerticalGlue (), gc);								// the parameters at the top
		return p;
	}

	/** A parameter that is a number: what is typed in is taken when it is one (Enter, or leaving the field), and put back as it was when not. */
	static protected JTextField field (final java.lang.reflect.Field f)
	{
		final JTextField	t = new JTextField (value (f), 7);
		Runnable			take = () ->
		{
			try
			{
				String		s = t.getText ().trim ();

				if (f.getType () == int.class)		f.setInt (null, Integer.parseInt (s));
				else								f.setDouble (null, Double.parseDouble (s));
			} catch (Exception e) { }
			t.setText (value (f));
		};

		t.setHorizontalAlignment (JTextField.RIGHT);
		t.setMinimumSize (t.getPreferredSize ());
		t.addActionListener (_ -> take.run ());
		t.addFocusListener (new FocusAdapter ()
		{
			public void focusLost (FocusEvent e)		{ take.run (); }
		});
		return t;
	}

	/** A parameter that is one of some options: its value is the index of the one chosen. */
	static protected JComboBox<String> choice (final java.lang.reflect.Field f, String[] options)
	{
		final JComboBox<String>	c = new JComboBox<> (options);

		try		{ c.setSelectedIndex (Math.max (0, Math.min (options.length - 1, f.getInt (null)))); }
		catch (Exception e) { }
		c.addActionListener (_ -> { try { f.setInt (null, c.getSelectedIndex ()); } catch (Exception e) { } });
		return c;
	}

	/** The value of a parameter, as text. */
	static protected String value (java.lang.reflect.Field f)
	{
		try		{ return String.valueOf (f.get (null)); }
		catch (Exception e)		{ return "?"; }
	}

	static protected JPanel titled (String title, Component c)
	{
		JPanel		p = new JPanel (new BorderLayout ());

		p.setBorder (new javax.swing.plaf.BorderUIResource.TitledBorderUIResource (new javax.swing.border.LineBorder (new Color (153, 153, 153), 1, false), title, 4, 2, new Font ("Application", Font.BOLD, 12), new Color (102, 102, 153)));
		p.add (c, BorderLayout.CENTER);
		return p;
	}

	/**
	 * What there is to show now: the method (null: none yet), the reduced LPS it
	 * was fed with, and where the robot really is (m, m, rad; null: not known).
	 * Called from any thread: the window is redrawn on the event thread.
	 */
	public void update (final Localisation loc, final LocLps lps, final Position real)
	{
		final Position		gt = (real != null) ? new Position (real.x () * 1000.0, real.y () * 1000.0, real.alpha) : null;

		SwingUtilities.invokeLater (() -> show (loc, lps, gt));
	}

	protected void show (Localisation loc, LocLps lps, Position gt)
	{
		Gs			gs = null;

		this.loc	= loc;
		this.lps	= lps;
		if (loc != null)
			synchronized (loc)
			{
				gs	= loc.getGs ();
				locView.track (gs, gt);
				locView.redraw (loc, lps);
			}
		lpsModel.fireTableDataChanged ();
		positionArea.setText (position (gs, gt));
	}

	/** The estimate (mm, deg, and how sure) and the ground truth, as text. */
	static protected String position (Gs gs, Position gt)
	{
		StringBuilder	sb = new StringBuilder ();

		if (gs != null)
		{
			sb.append (String.format ("GS  x %6d  y %6d mm  %4.0f deg%n", gs.getX (), gs.getY (), gs.getTheta () * Angles.RTOD));
			sb.append (String.format (" +- x %6d  y %6d mm  %4.0f deg%n", gs.getDX (), gs.getDY (), gs.getDTheta () * Angles.RTOD));
			sb.append (String.format ("    quality %.2f%n", gs.getQuality ()));
		}
		else
			sb.append ("GS  -\n\n\n");
		if (gt != null)
			sb.append (String.format ("GT  x %6.0f  y %6.0f mm  %4.0f deg", gt.x (), gt.y (), gt.alpha * Angles.RTOD));
		else
			sb.append ("GT  -");
		return sb.toString ();
	}

	/* ------------------------------------------------------------------ */
	/* The reduced LPS                                                     */
	/* ------------------------------------------------------------------ */

	protected class LpsTableModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		protected String[]			columns	= { "Object", "Rho (mm)", "Theta (deg)", "Anchor", "Last" };

		public String	getColumnName (int col)				{ return columns[col]; }
		public int		getRowCount ()						{ return (lps == null) ? 0 : LocLps.LPS_SIZE; }
		public int		getColumnCount ()					{ return columns.length; }
		public boolean	isCellEditable (int row, int col)	{ return false; }

		public Object getValueAt (int row, int col)
		{
			LocLpo		o = lps.getLpo (row);

			switch (col)
			{
			case 0:		return (row < NAMES.length) ? NAMES[row] : Integer.toString (row);
			case 1:		return String.format ("%.0f", o.rho);
			case 2:		return String.format ("%.0f", o.theta * Angles.RTOD);
			case 3:		return String.format ("%.2f", o.anchored);
			case 4:		return Integer.toString (o.last_anchored);
			default:	return "";
			}
		}
	}

	/** The name of each object on its colour, and an object not anchored greyed. */
	protected class LpsRenderer extends DefaultTableCellRenderer
	{
		private static final long	serialVersionUID = 1L;

		public Component getTableCellRendererComponent (JTable table, Object value, boolean sel, boolean focus, int row, int col)
		{
			super.getTableCellRendererComponent (table, value, sel, focus, row, col);
			setHorizontalAlignment ((col == 0) ? LEFT : RIGHT);
			if ((col == 0) && (row < COLORS.length))
			{
				setBackground (COLORS[row]);
				setForeground (Color.BLACK);
			}
			else
			{
				setBackground (sel ? table.getSelectionBackground () : table.getBackground ());
				setForeground (((lps != null) && (lps.getLpo (row).anchored > 0.0)) ? Color.BLACK : Color.GRAY);
			}
			return this;
		}
	}

	/* ------------------------------------------------------------------ */
	/* The drawings                                                        */
	/* ------------------------------------------------------------------ */

	/** The lines of the field, the nets and the landmarks (mm), as ChaosManager draws them. */
	static protected void drawField (Model2D model)
	{
		double			xborder, yborder, xgoal, ygoal, xnet, ynet;

		xborder	= FIELD_X_SIZE * 0.5;
		yborder	= FIELD_Y_SIZE * 0.5;
		xgoal	= AREA_WIDTH * 0.5;
		ygoal	= yborder - AREA_HEIGHT;

		model.addRawLine (xborder, yborder, -xborder, yborder, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xborder, yborder, -xborder, -yborder, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xborder, -yborder, xborder, -yborder, Model2D.THICK, Color.WHITE);
		model.addRawLine (xborder, -yborder, xborder, yborder, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xborder, 0, xborder, 0, Model2D.THICK, Color.WHITE);
		model.addRawCircle (0.0, 0.0, CIRCLE_RADIUS, Model2D.THICK, Color.WHITE);

		model.addRawLine (xgoal, yborder, xgoal, ygoal, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xgoal, yborder, -xgoal, ygoal, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xgoal, ygoal, xgoal, ygoal, Model2D.THICK, Color.WHITE);
		model.addRawLine (xgoal, -yborder, xgoal, -ygoal, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xgoal, -yborder, -xgoal, -ygoal, Model2D.THICK, Color.WHITE);
		model.addRawLine (-xgoal, -ygoal, xgoal, -ygoal, Model2D.THICK, Color.WHITE);

		xnet	= NET_WIDTH * 0.5;
		ynet	= yborder + NET_DEPTH;
		model.addRawLine (xnet, yborder, xnet, ynet, Model2D.THICK, NET1);
		model.addRawLine (-xnet, yborder, -xnet, ynet, Model2D.THICK, NET1);
		model.addRawLine (-xnet, ynet, xnet, ynet, Model2D.THICK, NET1);
		model.addRawLine (xnet, -yborder, xnet, -ynet, Model2D.THICK, NET2);
		model.addRawLine (-xnet, -yborder, -xnet, -ynet, Model2D.THICK, NET2);
		model.addRawLine (-xnet, -ynet, xnet, -ynet, Model2D.THICK, NET2);

		// the landmarks: the colour of their top band ringed by the one below (LM0 is Landmark1)
		for (int i = 0; i < NUM_LANDMARKS; i++)
		{
			Color			top = COLORS[LocLps.INIT_LMS + (i % 2)], below = COLORS[LocLps.INIT_LMS + ((i + 1) % 2)];

			model.addRawCircle (LM_X[i], LM_Y[i], LM_RADIUS, Model2D.FILLED, below);
			model.addRawCircle (LM_X[i], LM_Y[i], LM_RADIUS * 0.6, Model2D.FILLED, top);
		}
	}

	/** A robot: a disc with a line where it heads (mm, rad). */
	static protected void drawRobot (Model2D model, double x, double y, double a, double r, Color color)
	{
		model.addRawLine (x, y, x + 2.0 * r * Math.cos (a), y + 2.0 * r * Math.sin (a), Model2D.THICK, color);
		model.addRawCircle (x, y, r, Model2D.FILLED, color);
	}

	/**
	 * The inside of the localisation method (what it draws of itself: its grid,
	 * its particles, its ellipse...) over the field, and, round each landmark and
	 * net it has just taken in, a circle as far from it as the robot saw it; over
	 * all of it, the path of the robot where the method has it (blue) and where it
	 * really is (cyan), when the View menu says so, and the two robots: the
	 * estimated one with its uncertainty (an orange box, and ringed in white when
	 * the method is sure of it) and the true one.
	 */
	protected class LocalisationView extends Component2D
	{
		private static final long	serialVersionUID = 1L;

		protected Localisation		loc;
		protected LocLps			lps;
		protected Gs				gs;
		protected Position			gt;
		protected java.util.ArrayDeque<double[]>	gspath = new java.util.ArrayDeque<double[]> ();	// {x, y, a} (mm, rad), oldest first
		protected java.util.ArrayDeque<double[]>	gtpath = new java.util.ArrayDeque<double[]> ();

		LocalisationView ()
		{
			setClipping (false);
			setBackground (Color.GRAY);
			setOpaque (true);
			setPreferredSize (new Dimension (420, 560));
		}

		void clearPaths ()				{ gspath.clear ();	gtpath.clear (); }

		/** Where the robots are now: the estimate (a copy of it) and the true pose (mm, rad), onto the end of their paths. */
		void track (Gs gs, Position gt)
		{
			if (gs != null)		{ this.gs = new Gs (gs.getX (), gs.getY (), gs.getTheta (), gs.getDX (), gs.getDY (), gs.getDTheta (), gs.getQuality ());	add (gspath, gs.getX (), gs.getY (), gs.getTheta ()); }
			if (gt != null)		{ this.gt = new Position (gt);	add (gtpath, gt.x (), gt.y (), gt.alpha); }
		}

		/** A position onto the end of a path, when it has moved from the last one (the oldest go when it is full). */
		protected void add (java.util.ArrayDeque<double[]> path, double x, double y, double a)
		{
			double[]	last = path.peekLast ();

			if ((last != null) && (Math.hypot (x - last[0], y - last[1]) < 10.0) && (Math.abs (Angles.radnorm_180 (a - last[2])) < 0.05))		return;
			if (path.size () >= PATH_SIZE)		path.pollFirst ();
			path.addLast (new double[] { x, y, a });
		}

		/** The method as it is now and the reduced LPS it was fed with (the caller holds the method). */
		void redraw (Localisation loc, LocLps lps)
		{
			double			xb = TOTAL_X_SIZE * 0.5, yb = TOTAL_Y_SIZE * 0.5, r = DOG_RADIUS;

			this.loc	= loc;
			this.lps	= lps;
			model.clearView ();
			if (showGreen)
				model.addRawBox (-xb, -yb, xb, yb, Model2D.FILLED, Color.GREEN.darker ());
			loc.drawElements (model);
			drawField (model);
			if (lps != null)
			{
				for (int i = 0; i < LocLps.NUM_LMS; i++)
					if (loc.getLastUpdated (LocLps.INIT_LMS + i))
						model.addRawCircle (LM_X[i], LM_Y[i], lps.getLpo (LocLps.INIT_LMS + i).rho, Model2D.THICK, Color.ORANGE);
				for (int i = 0; i < LocLps.NUM_NETS; i++)
					if (loc.getLastUpdated (LocLps.INIT_NETS + i))
						model.addRawCircle (NET_X[i], NET_Y[i], lps.getLpo (LocLps.INIT_NETS + i).rho, Model2D.THICK, Color.ORANGE.darker ());
			}
			if (showPaths)
			{
				drawPath (gtpath, GT_COLOR);
				drawPath (gspath, GS_COLOR);
			}
			if (gt != null)
				drawRobot (model, gt.x (), gt.y (), gt.alpha, r, GT_COLOR);
			if (gs != null)
			{
				double		w2 = gs.getDX () * 0.5, h2 = gs.getDY () * 0.5;

				model.addRawBox (gs.getX () - w2, gs.getY () - h2, gs.getX () + w2, gs.getY () + h2, Model2D.THICK, Color.ORANGE);
				drawRobot (model, gs.getX (), gs.getY (), gs.getTheta (), r, GS_COLOR);
				if (gs.getQuality () > 0.7)
					model.addRawCircle (gs.getX (), gs.getY (), r, Model2D.THICK, Color.WHITE);
			}
			model.setBB (-xb, -yb, xb, yb);
			repaint ();
		}

		/** Everything drawn again as it was last (the View menu changed): the method is held while it is read. */
		void repaintAll ()
		{
			Localisation	l = loc;

			if (l == null)		return;
			synchronized (l)	{ redraw (l, lps); }
		}

		protected void drawPath (java.util.ArrayDeque<double[]> path, Color color)
		{
			double[]	last = null;

			for (double[] p : path)
			{
				if (last != null)		model.addRawLine (last[0], last[1], p[0], p[1], Model2D.PLAIN, color);
				last	= p;
			}
		}
	}
}
