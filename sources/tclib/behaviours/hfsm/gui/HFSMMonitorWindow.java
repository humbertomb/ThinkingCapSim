/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import tclib.behaviours.hfsm.HFSM;
import tclib.behaviours.hfsm.MetaState;
import tclib.behaviours.hfsm.State;
import tclib.behaviours.lua.gui.LuaVarsPanel;

/**
 * A look at a machine of states while it runs: the diagram of one level, with
 * the state the machine is in, and the meta states that hold it, drawn in red.
 *
 * Nothing of the machine can be changed on the diagram: a double click on a
 * meta state goes into it and a double click on the background comes back out.
 * The tool bar is where things are done: the button opens the editor on the
 * machine (what is saved there is read again by the robot), and the selector
 * runs another of the machines kept beside it. Under the diagram, the variables
 * of the Lua scripts of the machine ({@link LuaVarsPanel}), as in the Lua
 * monitor: by default the locals of the state the machine is in and of the
 * behaviour it chose, and every variable when asked for. Where the machine is,
 * and what its variables are worth, is read on its own, every {@link #PERIOD}
 * milliseconds, so the module that runs the machine has nothing to tell it.
 */
public class HFSMMonitorWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** The share of the window the diagram takes, the variables having the rest. */
	static public final double		SPLIT		= 0.40;

	/** How often where the machine is is looked at [ms]. */
	static public final int			PERIOD		= 100;

	static private final Color		C_LIVE		= new Color (200, 40, 40);
	static private final Color		C_WRONG		= new Color (180, 0, 0);		// what is the matter with the machine

	/** What the monitor asks of whoever runs the machine. */
	public interface Reload
	{
		/** Read the machine again: its file was written. */
		public void reload ();
		/** Run this machine instead, from the next cycle on. */
		public void load (java.io.File machine);
	}

	protected HFSM					machine;
	protected String				robot;
	protected Reload				reload;
	protected HFSMCanvas			canvas;
	protected LuaVarsPanel			vars;						// the variables of the scripts, under the diagram
	protected javax.swing.JSplitPane	split;
	protected JLabel				where;
	protected JLabel				level;
	protected Timer					timer;
	protected boolean				fitted;						// the diagram was put in view once the window had a size
	protected MetaState				holder;						// the meta state the machine was last seen in, whose level the diagram follows
	protected boolean				divided;
	protected boolean				complained;					// the table failed once and it was said					// the divider was put at its share once the window had a size

	protected HFSEditorMWindow		editor;						// the one editor of the machine, while it is open
	protected javax.swing.JButton	edit;
	protected javax.swing.JComboBox<String>	machines;			// the machines of the folder of the one running
	protected boolean				choosing;					// the selector is being filled in, which is nobody's choice

	public HFSMMonitorWindow (HFSM machine)
	{
		this (machine, null, null);
	}

	public HFSMMonitorWindow (HFSM machine, String robot)
	{
		this (machine, robot, null);
	}

	/**
	 * Watches a machine, the window named after the robot that runs it.
	 *
	 * @param reload  whoever runs the machine, to have it read again or another run (null: the tool bar does nothing)
	 */
	public HFSMMonitorWindow (HFSM machine, String robot, Reload reload)
	{
		super ("");

		this.machine	= machine;
		this.robot		= robot;
		this.reload		= reload;
		setTitle (title ());

		canvas	= new HFSMCanvas ((machine != null) ? machine.root () : new MetaState ("nothing", 0));
		canvas.setFile ((machine != null) ? machine.file () : null);
		canvas.setWatching (true);

		where	= new JLabel (" ");
		where.setFont (where.getFont ().deriveFont (Font.BOLD));
		where.setForeground (C_LIVE);
		level	= new JLabel (" ");
		level.setForeground (new Color (90, 90, 90));

		JPanel		bar = new JPanel (new BorderLayout (8, 0));

		bar.setBorder (BorderFactory.createEmptyBorder (3, 6, 3, 6));
		bar.add (where, BorderLayout.WEST);
		bar.add (level, BorderLayout.EAST);

		canvas.setListener (new HFSMCanvas.Listener ()
		{
			public void selectionChanged (Object selection)	{ }
			public void levelChanged (MetaState l)			{ said (); }
			public void machineChanged (String what)		{ }
			public void statusChanged (String text)			{ }
			public void usageChanged (String text)			{ }
			public void toolFinished ()						{ }
		});

		canvas.addComponentListener (new java.awt.event.ComponentAdapter ()
		{															// the diagram is fitted once there is a view to fit it in
			public void componentResized (java.awt.event.ComponentEvent e)
			{
				if (fitted)			return;
				fitted	= true;
				canvas.zoomToFit ();
			}
		});

		// the diagram, with its bar, over the variables of the scripts
		JPanel		diagram = new JPanel (new BorderLayout ());

		diagram.add (canvas, BorderLayout.CENTER);
		diagram.add (bar, BorderLayout.SOUTH);
		diagram.setMinimumSize (new Dimension (100, 80));

		vars	= new LuaVarsPanel ((machine != null) ? machine.lua () : null, (machine != null) ? machine.bridge () : null,
									new LuaVarsPanel.Current ()
		{
			public boolean isCurrent (String chunk)		{ return HFSMMonitorWindow.this.isCurrent (chunk); }
		});
		vars.what (title ().replaceFirst ("^HFSM Monitor[^:]*: ", "machine "));
		vars.labels (new LuaVarsPanel.Labels ()
		{															// the scripts are shown by name alone, without the number that tells two apart
			public String label (String chunk)		{ return chunk.replaceAll ("#\\d+", ""); }
		});
		vars.setMinimumSize (new Dimension (100, 80));

		split	= new javax.swing.JSplitPane (javax.swing.JSplitPane.VERTICAL_SPLIT, true, diagram, vars);
		split.setResizeWeight (SPLIT);								// the diagram keeps its share when the window grows
		split.setBorder (null);
		split.setOneTouchExpandable (true);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.NORTH);
		getContentPane ().add (split, BorderLayout.CENTER);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
		setSize (new Dimension (760, 760));
		split.addComponentListener (new java.awt.event.ComponentAdapter ()
		{															// the divider is put at its share once there is a height to share
			public void componentResized (java.awt.event.ComponentEvent e)
			{
				if (divided || (split.getHeight () <= 0))		return;
				divided	= true;
				split.setDividerLocation (SPLIT);
			}
		});

		timer	= new Timer (PERIOD, new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ refresh (); }
		});
		timer.setRepeats (true);
		timer.start ();

		refresh ();
	}

	public final HFSM				machine ()			{ return machine; }
	public final HFSMCanvas			getCanvas ()		{ return canvas; }
	/** The table of the variables of the scripts, under the diagram. */
	public final LuaVarsPanel		getVars ()			{ return vars; }
	public final javax.swing.JSplitPane	getSplit ()		{ return split; }
	/** The editor of the machine while it is open, null when it is not. */
	public final HFSEditorMWindow	getEditor ()		{ return editor; }
	/** The file of the machine being watched, or null. */
	public final java.io.File		getFile ()			{ return (machine != null) ? machine.file () : null; }

	/** What the title bar says. */
	protected String title ()
	{
		return "HFSM Monitor" + ((robot != null) ? (" [" + robot + "]") : "")
			   + ((machine != null) ? (": " + machine.root ().getName ()) : "");
	}

	/**
	 * Another machine is being run (read again, or another file): the diagram is
	 * the one of it from now on. Whoever runs the machine says so once it has it.
	 */
	public void setMachine (final HFSM m)
	{
		Runnable	r = new Runnable ()
		{
			public void run ()
			{
				machine	= m;
				holder	= null;										// the diagram finds where the new one is
				canvas.setMachine ((m != null) ? m.root () : new MetaState ("nothing", 0));
				canvas.setFile ((m != null) ? m.file () : null);
				vars.source ((m != null) ? m.lua () : null, (m != null) ? m.bridge () : null);
				setTitle (title ());
				vars.what ((m != null) ? ("machine " + m.root ().getName ()) : null);
				fillMachines ();
				refresh ();
			}
		};

		if (SwingUtilities.isEventDispatchThread ())	r.run ();
		else											SwingUtilities.invokeLater (r);
	}

	/* ------------------------------------------------------------------ */
	/* The tool bar                                                        */
	/* ------------------------------------------------------------------ */

	/**
	 * What there is to do from here: edit the machine that is running, and run
	 * another of the ones that are kept beside it.
	 */
	protected javax.swing.JToolBar buildToolBar ()
	{
		javax.swing.JToolBar	tb = new javax.swing.JToolBar ();

		tb.setFloatable (false);
		tb.setBorder (BorderFactory.createEmptyBorder (2, 4, 2, 4));
		tb.add (editButton ());
		tb.add (javax.swing.Box.createHorizontalGlue ());		// the selector goes on the right
		tb.add (new JLabel ("State Machine "));
		tb.add (machinesBox ());
		return tb;
	}

	/** The button that edits the machine: the icon alone, with no border around it. */
	protected javax.swing.JButton editButton ()
	{
		edit	= new javax.swing.JButton (new tclib.behaviours.lua.gui.LuaMonitorWindow.EditorIcon ());
		edit.setToolTipText ("Edit the state machine (it is read again when saved)");
		edit.setEnabled (getFile () != null);
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

	/** The machines of the folder the one running is kept in, to run another of them. */
	protected javax.swing.JComboBox<String> machinesBox ()
	{
		machines	= new javax.swing.JComboBox<String> ();
		machines.setToolTipText ("The state machines beside this one: choosing another runs it");
		machines.setMaximumSize (new Dimension (220, 24));
		machines.setPreferredSize (new Dimension (200, 24));
		machines.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				if (choosing)						return;
				chose ((String) machines.getSelectedItem ());
			}
		});
		fillMachines ();
		return machines;
	}

	/** Every .hfsm of the folder of the machine being run, the one running selected. */
	protected void fillMachines ()
	{
		if (machines == null)						return;

		java.io.File		file = getFile ();
		java.io.File		dir = (file != null) ? file.getAbsoluteFile ().getParentFile () : null;
		String[]			names = (dir != null) ? dir.list (new java.io.FilenameFilter ()
		{
			public boolean accept (java.io.File d, String name)		{ return name.toLowerCase ().endsWith (tclib.behaviours.hfsm.HFSMJson.SUFFIX); }
		}) : null;

		if (names == null)							names = new String[0];
		java.util.Arrays.sort (names, String.CASE_INSENSITIVE_ORDER);

		choosing	= true;
		machines.setModel (new javax.swing.DefaultComboBoxModel<String> (names));
		if (file != null)							machines.setSelectedItem (file.getName ());
		machines.setEnabled ((file != null) && (reload != null));
		if (edit != null)							edit.setEnabled (file != null);
		choosing	= false;
	}

	/**
	 * Another machine of the folder was chosen: it is the one the robot runs from
	 * now on, and the one the editor is on, if it is open (what was being written
	 * there is offered to be saved first).
	 */
	protected void chose (String name)
	{
		java.io.File		file = getFile ();

		if ((name == null) || (file == null) || (reload == null))		return;

		java.io.File		chosen = new java.io.File (file.getAbsoluteFile ().getParentFile (), name);

		if (chosen.getAbsolutePath ().equals (file.getAbsolutePath ()))		return;
		if ((editor != null) && editor.isDisplayable () && !editor.confirmDiscard ())
		{
			fillMachines ();						// it was not to be: the selector says what is running
			return;
		}
		reload.load (chosen);						// whoever runs it says so with setMachine, and the diagram follows
		if ((editor != null) && editor.isDisplayable ())
		{
			editor.load (chosen);
			editor.toFront ();
		}
	}

	/**
	 * Opens the editor on the machine being run, beside this window (its top left
	 * against the top right of this one), and has whoever runs the machine read it
	 * again every time it is saved. There is one editor: asking again brings the one
	 * that is open to the front.
	 */
	public HFSEditorMWindow edit ()
	{
		final java.io.File	file = getFile ();

		if (file == null)						return null;
		if ((editor != null) && editor.isDisplayable ())
		{
			editor.toFront ();
			editor.requestFocus ();
			return editor;
		}

		final HFSEditorMWindow	w = new HFSEditorMWindow (file, false);

		w.getEditor ().setOneFile (true);			// the machine that is running, and no other
		w.getEditor ().setOnSave (new HFSMPanel.Saved ()
		{
			public void saved (java.io.File f)
			{
				java.io.File	now = getFile ();
				boolean			same = (now != null) && now.getAbsolutePath ().equals (f.getAbsolutePath ());

				if (same && (reload != null))		reload.reload ();	// what was just written is what runs
				else if (reload != null)			reload.load (f);	// saved as another: that is the machine now
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

	/** Where the machine is now, straight onto the diagram, and what its variables are worth. */
	public void refresh ()
	{
		if (machine == null)					return;

		List<State>		live = machine.active ();

		follow (live);
		canvas.setLive (live);
		said ();
		try { vars.refresh (); }
		catch (RuntimeException e)											// the table is no reason for the diagram to stop
		{
			if (!complained)			{ complained = true;	System.out.println ("  [HFSM] The monitor cannot read the variables: " + e); }
		}
	}

	/**
	 * The diagram follows the machine into and out of the meta states: when the
	 * state it is in is held by another meta state than the last time, that is the
	 * level shown, going in when the machine enters one and out when it leaves it.
	 * Between such moves the level can be browsed by hand.
	 */
	protected void follow (List<State> live)
	{
		MetaState		in = (live.size () >= 2) && (live.get (live.size () - 2) instanceof MetaState)
							 ? (MetaState) live.get (live.size () - 2) : machine.root ();

		if (in == holder)						return;
		holder	= in;
		if (canvas.getLevel () != in)			canvas.setLevel (in);
	}

	/**
	 * Whether a script is one of those being run: the script of the state the
	 * machine is in, the tests and actions of the transitions out of it and of the
	 * meta states that hold it (named "state X#n", "transition T#n (test)",
	 * "transition T#n (do)" by the machine, n being the number that tells apart two
	 * called the same) and the behaviour it chose (its file).
	 */
	protected boolean isCurrent (String chunk)
	{
		HFSM		m = machine;

		if ((m == null) || (chunk == null))		return false;

		List<State>		live = m.active ();

		for (State s : live)
		{
			if (chunk.equals ("state " + s.ref ()))								return true;
			for (tclib.behaviours.hfsm.Transition t : s.getTransitions ())
				if (chunk.startsWith ("transition " + t.ref () + " ("))			return true;
		}

		String		beh = (m.bridge () != null) ? m.bridge ().behaviour () : null;

		return (beh != null) && (chunk.equals (beh + ".lua") || chunk.equals (beh));
	}

	/** What the bar at the bottom says. */
	protected void said ()
	{
		if (machine == null)					return;

		String			last = machine.lastTransition ();
		String			stuck = machine.stuck ();								// a meta state with nothing to start at: said in red, here and under the table
		String			problems = problems ();

		where.setText ("At " + machine.where () + ((stuck != null) ? ("   -- " + stuck) : (last != null) ? ("   (" + last + ")") : ""));
		where.setForeground ((stuck != null) ? C_WRONG : C_LIVE);
		vars.problem ((stuck == null) ? problems : (problems == null) ? stuck : (stuck + " (" + problems + ")"));
		level.setText ("Showing " + canvas.levelPath () + (canvas.canGoUp () ? "   (double click on the background to go up)" : ""));
	}

	/** What the machine had the matter with it when it was read, in one line, or null for nothing. */
	protected String problems ()
	{
		HFSM		m = machine;

		if ((m == null) || m.problems ().isEmpty ())		return null;

		StringBuilder	sb = new StringBuilder ();

		for (String p : m.problems ())
			sb.append ((sb.length () > 0) ? "; " : "").append (p);
		return sb.toString ();
	}

	/** Stops looking at the machine and goes away. */
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
		super.dispose ();
	}

	/** Watches a machine of states run, as a window of its own. */
	static public HFSMMonitorWindow open (final HFSM machine, final String robot)
	{
		return open (machine, robot, null);
	}

	static public HFSMMonitorWindow open (final HFSM machine, final String robot, final Reload reload)
	{
		final HFSMMonitorWindow[]	w = new HFSMMonitorWindow[1];

		try
		{
			SwingUtilities.invokeAndWait (new Runnable ()
			{
				public void run ()
				{
					w[0]	= new HFSMMonitorWindow (machine, robot, reload);
					w[0].setVisible (true);
				}
			});
		}
		catch (Exception e)
		{
			System.out.println ("  [HFSM] Cannot open the monitor: " + e);
		}
		return w[0];
	}
}
