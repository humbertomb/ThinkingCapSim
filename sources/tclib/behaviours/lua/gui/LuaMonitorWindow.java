/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import tclib.behaviours.lua.LuaBridge;
import tclib.behaviours.lua.interpreter.LuaState;

/**
 * A look at a Lua program while it runs: every variable in use, what it is
 * worth and where it lives.
 *
 * Four kinds of variable are told apart in the Scope column:
 * <pre>
 *   local &lt;script&gt;   a local of that script, as its last run left it
 *   global            a global of the interpreter, which every script shares
 *   chaos global      what the scripts left for one another (chaos.setGlobal)
 *   command           what the program asked the robot for on this cycle
 * </pre>
 *
 * Of the locals, only the ones of the program being run are shown: the scripts
 * of the behaviours and the programs run before leave locals of their own, and
 * under the same names they would be read as the program's. Those, and what the
 * library puts in the globals (math, string, io, table, os) and the bridge
 * itself, are shown when all the variables are asked for ("Show all variables").
 * A variable whose value is not what it was on the cycle before is marked, so
 * that what moves can be seen at a glance. A local that got a nil from the bridge
 * because it asked for what there is not (<code>chaos.getLpo</code> of a constant
 * that does not exist) is written in red, and pointed at says why.
 *
 * It only reads, and reads on its own every {@link #PERIOD} milliseconds, so the
 * module that runs the program has nothing to tell it.
 */
public class LuaMonitorWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** How often the variables are looked at [ms]. */
	static public final int			PERIOD		= 200;

	/* The scopes, as they are written in the table (those of the panel, kept here by name) */
	static public final String		S_LOCAL		= LuaVarsPanel.S_LOCAL;
	static public final String		S_GLOBAL	= LuaVarsPanel.S_GLOBAL;
	static public final String		S_CHAOS		= LuaVarsPanel.S_CHAOS;
	static public final String		S_COMMAND	= LuaVarsPanel.S_COMMAND;

	/** What the monitor asks of whoever runs the program. */
	public interface Reload
	{
		/** Read the program again: its file was written. */
		public void reload ();
		/** Run this program instead, from the next cycle on. */
		public void load (java.io.File program);
	}

	protected LuaState				lua;
	protected LuaBridge				chaos;
	protected String				program;
	protected java.io.File			file;						// the program being run, to write in
	protected Reload				reload;
	protected String				robot;
	protected LuaEditorWindow		editor;						// the one editor of it, while it is open

	protected LuaVarsPanel			vars;						// the table of the variables
	protected javax.swing.JComboBox<String>	programs;			// the programs of the folder of the one running
	protected boolean				choosing;					// the selector is being filled in, which is nobody's choice
	protected Timer					timer;

	public LuaMonitorWindow (LuaState lua, LuaBridge chaos, String program)
	{
		this (lua, chaos, program, null);
	}

	public LuaMonitorWindow (LuaState lua, LuaBridge chaos, String program, String robot)
	{
		this (lua, chaos, (program != null) ? new java.io.File (program) : null, robot, null);
	}

	/**
	 * Watches the variables of a program, the window named after the robot that runs
	 * it. The file is the program being run, which can be written from here, and
	 * whoever runs it is told to read it again once it was.
	 */
	public LuaMonitorWindow (LuaState lua, LuaBridge chaos, java.io.File file, String robot, Reload reload)
	{
		super ("Lua Monitor" + ((robot != null) ? (" [" + robot + "]") : "")
			   + ((file != null) ? (": " + file.getName ()) : ""));

		this.lua		= lua;
		this.chaos		= chaos;
		this.file		= file;
		this.program	= (file != null) ? file.getName () : null;
		this.reload		= reload;
		this.robot		= robot;

		// the table: the locals it shows by default are those of the program being run
		vars	= new LuaVarsPanel (lua, chaos, new LuaVarsPanel.Current ()
		{
			public boolean isCurrent (String chunk)		{ return (program != null) && program.equals (chunk); }
		});
		vars.what ((program != null) ? ("program " + program) : null);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.NORTH);
		getContentPane ().add (vars, BorderLayout.CENTER);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
		setSize (new Dimension (680, 560));

		timer	= new Timer (PERIOD, new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ refresh (); }
		});
		timer.setRepeats (true);
		timer.start ();

		refresh ();
	}

	public final JTable				getTable ()			{ return vars.getTable (); }
	/** The variables as the last look at them found them. */
	public List<LuaVarsPanel.Var>	getVars ()			{ return vars.getVars (); }
	/** The table of the variables, to put it elsewhere too. */
	public final LuaVarsPanel		getVarsPanel ()		{ return vars; }
	/** The program being watched, or null when it is not known where it is kept. */
	public final java.io.File		getFile ()			{ return file; }
	/** The editor of the program while it is open, null when it is not. */
	public final LuaEditorWindow	getEditor ()		{ return editor; }

	/* ------------------------------------------------------------------ */
	/* The tool bar                                                        */
	/* ------------------------------------------------------------------ */

	/**
	 * What there is to do from here: write the program that is running, and run
	 * another of the ones that are kept beside it.
	 */
	protected javax.swing.JToolBar buildToolBar ()
	{
		javax.swing.JToolBar	tb = new javax.swing.JToolBar ();

		tb.setFloatable (false);
		tb.setBorder (BorderFactory.createEmptyBorder (2, 4, 2, 4));
		tb.add (editButton ());
		tb.add (javax.swing.Box.createHorizontalGlue ());		// the selector goes on the right
		tb.add (new JLabel ("Program "));
		tb.add (programsBox ());
		return tb;
	}

	/** The button that writes the program: the icon alone, with no border around it. */
	protected javax.swing.JButton editButton ()
	{
		javax.swing.JButton		edit = new javax.swing.JButton (new EditorIcon ());

		edit.setToolTipText ((file != null) ? ("Write " + file.getName () + " (it is read again when saved)")
										   : "Write the program");
		edit.setEnabled (file != null);
		edit.setFocusable (false);
		edit.setBorderPainted (false);							// the icon says it all
		edit.setContentAreaFilled (false);
		edit.setFocusPainted (false);
		edit.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
		edit.setMargin (new java.awt.Insets (0, 0, 0, 0));
		edit.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ edit (); }
		});
		return edit;
	}

	/** The programs of the folder the one running is kept in, to run another of them. */
	protected javax.swing.JComboBox<String> programsBox ()
	{
		programs	= new javax.swing.JComboBox<String> ();
		programs.setToolTipText ("The programs beside this one: choosing another runs it");
		programs.setMaximumSize (new Dimension (220, 24));
		programs.setPreferredSize (new Dimension (200, 24));
		programs.setEnabled (file != null);
		programs.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				if (choosing)						return;
				chose ((String) programs.getSelectedItem ());
			}
		});
		fillPrograms ();
		return programs;
	}

	/** Every .lua of the folder of the program being run, the one running selected. */
	protected void fillPrograms ()
	{
		if (programs == null)						return;

		java.io.File		dir = (file != null) ? file.getAbsoluteFile ().getParentFile () : null;
		String[]			names = (dir != null) ? dir.list (new java.io.FilenameFilter ()
		{
			public boolean accept (java.io.File d, String name)		{ return name.toLowerCase ().endsWith (".lua"); }
		}) : null;

		if (names == null)							names = new String[0];
		java.util.Arrays.sort (names, String.CASE_INSENSITIVE_ORDER);

		choosing	= true;
		programs.setModel (new javax.swing.DefaultComboBoxModel<String> (names));
		if (file != null)							programs.setSelectedItem (file.getName ());
		choosing	= false;
	}

	/**
	 * Another program of the folder was chosen: it is the one the robot runs from
	 * now on, and the one the editor is on, if it is open (what was being written
	 * there is offered to be saved first).
	 */
	protected void chose (String name)
	{
		if ((name == null) || (file == null))		return;

		java.io.File		chosen = new java.io.File (file.getAbsoluteFile ().getParentFile (), name);

		if (chosen.getAbsolutePath ().equals (file.getAbsolutePath ()))		return;
		if ((editor != null) && editor.isDisplayable () && !editor.confirmDiscard ())
		{
			fillPrograms ();						// it was not to be: the selector says what is running
			return;
		}

		file		= chosen;
		program		= chosen.getName ();
		vars.what ("program " + program);
		setTitle (title ());
		if (reload != null)							reload.load (chosen);
		if ((editor != null) && editor.isDisplayable ())
		{
			editor.load (chosen);
			editor.toFront ();
		}
		vars.notice (chosen.getName () + " is now the program");
		fillPrograms ();
		refresh ();
	}

	/** What the title bar says. */
	protected String title ()
	{
		return "Lua Monitor" + ((robot != null) ? (" [" + robot + "]") : "")
			   + ((file != null) ? (": " + file.getName ()) : "");
	}

	/**
	 * Opens the editor on the program being run, beside this window (its top left
	 * against the top right of this one), and has whoever runs the program read it
	 * again every time it is saved. There is one editor: asking again brings the one
	 * that is open to the front.
	 */
	public LuaEditorWindow edit ()
	{
		if (file == null)						return null;
		if ((editor != null) && editor.isDisplayable ())
		{
			editor.toFront ();
			editor.requestFocus ();
			return editor;
		}

		final LuaEditorWindow	w = new LuaEditorWindow (file);

		w.setOneFile (true);						// the program that is running, and no other
		w.setOnSave (new LuaEditorWindow.Saved ()
		{
			public void saved (java.io.File f)
			{
				boolean		same = (file != null) && file.getAbsolutePath ().equals (f.getAbsolutePath ());

				if (same && (reload != null))		reload.reload ();	// what was just written is what runs
				vars.notice (f.getName () + (same ? " saved and read again" : " saved (a copy: the robot goes on with "
																				 + file.getName () + ")"));
			}
		});
		beside (w);
		w.setVisible (true);
		editor	= w;
		return w;
	}

	/** Puts a window against the top right side of this one, on the screen if it fits. */
	protected void beside (JFrame w)
	{
		java.awt.Rectangle	screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment ().getMaximumWindowBounds ();
		int					x = getX () + getWidth ();
		int					y = getY ();

		if ((x + w.getWidth ()) > (screen.x + screen.width))			// no room on the right: as far right as it goes
			x	= Math.max (screen.x, screen.x + screen.width - w.getWidth ());
		w.setLocation (x, y);
	}

	/* ------------------------------------------------------------------ */
	/* Reading the variables                                              */
	/* ------------------------------------------------------------------ */

	/** Looks at the variables again, and says what has changed since the last look. */
	public void refresh ()
	{
		vars.refresh ();
	}

	/**
	 * What is the matter with the program, when it is not being run at all: whoever
	 * tried to read it says so here, and the status bar has it instead of what a
	 * program that runs is doing. Null for nothing the matter.
	 */
	public void problem (String text)
	{
		vars.problem (text);
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ refresh (); }
		});
	}

	/** What is the matter with the program, null when there is nothing. */
	public final String				problem ()			{ return vars.problem (); }


	/**
	 * The icon of the button: a sheet of paper with lines of text on it and a pencil
	 * across its corner, drawn rather than read from a file, as the icons of the
	 * editors of the simulator are.
	 */
	static public class EditorIcon implements javax.swing.Icon
	{
		static public final int			SIZE		= 20;

		static private final Color		C_PAPER		= Color.WHITE;
		static private final Color		C_EDGE		= new Color (70, 70, 70);
		static private final Color		C_TEXT		= new Color (120, 120, 120);
		static private final Color		C_PENCIL	= new Color (230, 170, 40);
		static private final Color		C_LEAD		= new Color (60, 60, 60);

		public int getIconWidth ()				{ return SIZE; }
		public int getIconHeight ()				{ return SIZE; }

		public void paintIcon (Component c, java.awt.Graphics g0, int x, int y)
		{
			java.awt.Graphics2D		g = (java.awt.Graphics2D) g0.create ();

			g.setRenderingHint (java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.translate (x, y);

			// the sheet, with a folded corner
			java.awt.geom.Path2D.Double	sheet = new java.awt.geom.Path2D.Double ();

			sheet.moveTo (3, 1);
			sheet.lineTo (12, 1);
			sheet.lineTo (16, 5);
			sheet.lineTo (16, 18);
			sheet.lineTo (3, 18);
			sheet.closePath ();
			g.setColor (C_PAPER);
			g.fill (sheet);
			g.setColor (C_EDGE);
			g.setStroke (new java.awt.BasicStroke (1.2f));
			g.draw (sheet);
			g.draw (new java.awt.geom.Line2D.Double (12, 1, 12, 5));
			g.draw (new java.awt.geom.Line2D.Double (12, 5, 16, 5));

			// the lines of text
			g.setColor (C_TEXT);
			g.setStroke (new java.awt.BasicStroke (1f));
			for (int i = 0; i < 4; i++)
				g.draw (new java.awt.geom.Line2D.Double (5.5, 8 + i * 2.6, (i == 3) ? 11 : 13.5, 8 + i * 2.6));

			// and the pencil across the corner
			g.setColor (C_PENCIL);
			g.setStroke (new java.awt.BasicStroke (3f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_ROUND));
			g.draw (new java.awt.geom.Line2D.Double (9.5, 17.5, 17.5, 9.5));
			g.setColor (C_LEAD);
			g.setStroke (new java.awt.BasicStroke (3f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_ROUND));
			g.draw (new java.awt.geom.Line2D.Double (17.8, 9.2, 18.6, 8.4));
			g.dispose ();
		}
	}

	/** Stops looking at the program and goes away. */
	public void close ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				if (timer != null)			timer.stop ();
				if (editor != null)			{ editor.dispose ();	editor = null; }
				setVisible (false);
				dispose ();
			}
		});
	}

	public void dispose ()
	{
		if (timer != null)						timer.stop ();
		if (lua != null)						lua.interpreter ().watch (false);
		super.dispose ();
	}

	/** Watches the variables of a program, as a window of its own. */
	static public LuaMonitorWindow open (final LuaState lua, final LuaBridge chaos, final java.io.File file,
										 final String robot, final Reload reload)
	{
		final LuaMonitorWindow[]	w = new LuaMonitorWindow[1];

		try
		{
			SwingUtilities.invokeAndWait (new Runnable ()
			{
				public void run ()
				{
					w[0]	= new LuaMonitorWindow (lua, chaos, file, robot, reload);
					w[0].setVisible (true);
				}
			});
		}
		catch (Exception e)
		{
			System.out.println ("  [LUA] Cannot open the monitor: " + e);
		}
		return w[0];
	}
}
