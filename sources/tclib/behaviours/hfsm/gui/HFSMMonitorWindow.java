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

/**
 * A look at a machine of states while it runs: the diagram of one level, with
 * the state the machine is in, and the meta states that hold it, drawn in red.
 *
 * Nothing of the machine can be changed here: a double click on a meta state
 * goes into it and a double click on the background comes back out, which is
 * all there is to do. Where the machine is is read on its own, every
 * {@link #PERIOD} milliseconds, so the module that runs the machine has nothing
 * to tell it.
 */
public class HFSMMonitorWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** How often where the machine is is looked at [ms]. */
	static public final int			PERIOD		= 100;

	static private final Color		C_LIVE		= new Color (200, 40, 40);

	protected HFSM					machine;
	protected HFSMCanvas			canvas;
	protected JLabel				where;
	protected JLabel				level;
	protected Timer					timer;
	protected boolean				fitted;						// the diagram was put in view once the window had a size

	public HFSMMonitorWindow (HFSM machine)
	{
		this (machine, null);
	}

	/** Watches a machine, the window named after the robot that runs it. */
	public HFSMMonitorWindow (HFSM machine, String robot)
	{
		super ("HFSM Monitor" + ((robot != null) ? (" [" + robot + "]") : "")
			   + ((machine != null) ? (": " + machine.root ().getName ()) : ""));

		this.machine	= machine;

		canvas	= new HFSMCanvas ((machine != null) ? machine.root () : new MetaState ("nothing", 0));
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

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (bar, BorderLayout.SOUTH);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);
		setSize (new Dimension (760, 640));

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

	/** Where the machine is now, straight onto the diagram. */
	public void refresh ()
	{
		if (machine == null)					return;

		List<State>		live = machine.active ();

		canvas.setLive (live);
		said ();
	}

	/** What the bar at the bottom says. */
	protected void said ()
	{
		if (machine == null)					return;

		String			last = machine.lastTransition ();

		where.setText ("At " + machine.where () + ((last != null) ? ("   (" + last + ")") : ""));
		level.setText ("Showing " + canvas.levelPath () + (canvas.canGoUp () ? "   (double click on the background to go up)" : ""));
	}

	/** Stops looking at the machine and goes away. */
	public void close ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				if (timer != null)			timer.stop ();
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
		final HFSMMonitorWindow[]	w = new HFSMMonitorWindow[1];

		try
		{
			SwingUtilities.invokeAndWait (new Runnable ()
			{
				public void run ()
				{
					w[0]	= new HFSMMonitorWindow (machine, robot);
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
