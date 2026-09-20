/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import tc.shared.world.WMDock;
import tc.shared.world.World;
import tclib.planning.sequence.Sequence;

/**
 * Task set editor (the monitor's "Task" tab, simplified): a place (dock of
 * the world) and an action are added as a task; the task set can be
 * reordered and trimmed before sending it to the robot as a plan
 * ({@link Sequence}).
 */
public class TaskDialog extends JDialog
{
	private static final long		serialVersionUID = 1L;

	/**
	 * The actions a planner understands, as the planner class itself declares them
	 * in an <code>ACTIONS</code> of its own ({@link tc.modules.Planner#ACTIONS}),
	 * looked up the ancestry so that a planner of a development inherits the ones
	 * of the planner it is built on. A class the development does not hold, or one
	 * that declares none, understands nothing: nothing is offered for it.
	 */
	static public String[] actionsOf (String cls)
	{
		if ((cls == null) || (cls.trim ().length () == 0))		return new String[0];
		try
		{
			for (Class<?> c = Class.forName (cls.trim ()); c != null; c = c.getSuperclass ())
				try
				{
					java.lang.reflect.Field		f = c.getDeclaredField ("ACTIONS");
					if (!java.lang.reflect.Modifier.isStatic (f.getModifiers ()))		continue;
					f.setAccessible (true);
					String[]	a = (String[]) f.get (null);
					if ((a != null) && (a.length > 0))		return a;
				} catch (NoSuchFieldException e)			{ }
		} catch (Throwable t)		{ }
		return new String[0];
	}

	protected World					world;					// docks with their flow type (null: no filtering)
	protected String[]				places;					// all the places offered
	protected String[]				robots;
	protected java.util.Map<String, String>	planners;		// robot -> the planner class it runs (empty: none known)
	protected JComboBox<String>		robotCB;				// null with one robot or none
	protected JComboBox<String>		placeCB;
	protected JComboBox<String>		actionCB;
	protected JTable				table;
	protected TaskModel				model;
	protected JButton				addBT, upBT, downBT, removeBT, clearBT, sendBT, cancelBT;
	protected Sequence				result;					// null when cancelled

	/** Rows of the task table. */
	protected class TaskModel extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;
		List<String[]>	tasks = new ArrayList<String[]> ();

		public int getRowCount ()				{ return tasks.size (); }
		public int getColumnCount ()			{ return 2; }
		public String getColumnName (int c)		{ return (c == 0) ? "Place" : "Action"; }
		public Object getValueAt (int r, int c)	{ return tasks.get (r)[c]; }

		void add (String place, String action)	{ tasks.add (new String[] { place, action }); fireTableRowsInserted (tasks.size () - 1, tasks.size () - 1); }
		void remove (int r)						{ tasks.remove (r); fireTableRowsDeleted (r, r); }
		void move (int from, int to)			{ String[] t = tasks.remove (from); tasks.add (to, t); fireTableDataChanged (); }
		void clear ()							{ tasks.clear (); fireTableDataChanged (); }
	}

	/**
	 * @param places   destinations offered in the Place selector (usually the dock labels of the world)
	 * @param initial  task set to start from (may be null)
	 */
	public TaskDialog (Frame owner, String[] places, Sequence initial)
	{
		this (owner, places, initial, new String[0]);
	}

	/** @param robots  robots the task set can be sent to (a selector is shown when there is more than one) */
	public TaskDialog (Frame owner, String[] places, Sequence initial, String[] robots)
	{
		this (owner, null, places, initial, robots, null);
	}

	/**
	 * Task editor over the docks of a world: the Place selector only offers the
	 * docks whose material flow ({@link WMDock.FlowType}) admits the selected
	 * action (load: OUT or INOUT; unload: IN or INOUT; others: all).
	 */
	public TaskDialog (Frame owner, World world, Sequence initial, String[] robots)
	{
		this (owner, world, placesOf (world), initial, robots, null);
	}

	/**
	 * Task editor for the robots of a running deployment: every one of them with
	 * the planner it runs, so that the Action selector offers what that planner
	 * understands and nothing else. The robots are the keys of the map, in the
	 * order it holds them.
	 */
	public TaskDialog (Frame owner, World world, Sequence initial, java.util.Map<String, String> planners)
	{
		this (owner, world, placesOf (world), initial,
			  planners.keySet ().toArray (new String[0]), planners);
	}

	protected TaskDialog (Frame owner, World world, String[] places, Sequence initial, String[] robots,
						  java.util.Map<String, String> planners)
	{
		super (owner, "Tasks", true);
		this.world		= world;
		this.places		= places;
		this.robots		= robots;
		this.planners	= (planners != null) ? planners : new java.util.LinkedHashMap<String, String> ();
		buildGUI (places);
		if (initial != null)
			for (int i = 0; i < initial.size (); i++)		model.add (initial.place[i], initial.action[i]);
		updateButtons ();
		pack ();
		setMinimumSize (new Dimension (480, 340));
		setLocationRelativeTo (owner);
	}

	/** Dock labels of a world, in order (the places a plan can refer to). */
	static public String[] placesOf (World w)
	{
		String[]	p = new String[w.docks ().size ()];
		for (int i = 0; i < p.length; i++)		p[i] = w.docks ().get (i).label;
		return p;
	}

	/**
	 * Places admitting the given action (all of them without a world): load and
	 * unload offer the docks whose flow accepts them; the other actions (goto,
	 * stay) offer every dock, waypoint and zone of the world.
	 */
	protected String[] placesFor (String action)
	{
		if (world == null)				return places;
		boolean			material = (action != null) && (action.equalsIgnoreCase ("load") || action.equalsIgnoreCase ("unload"));
		List<String>	sel = new ArrayList<String> ();
		for (int i = 0; i < world.docks ().size (); i++)
		{
			WMDock	d = world.docks ().get (i);
			if (!material || d.accepts (action))		sel.add (d.label);
		}
		if (!material)
		{
			for (int i = 0; i < world.wps ().size (); i++)			sel.add (world.wps ().get (i).label);
			for (int i = 0; i < world.zones ().n (); i++)		sel.add (world.zones ().at (i).label);
		}
		return sel.toArray (new String[sel.size ()]);
	}

	/** Refills the Place selector for the current action, keeping the selected place when still offered. */
	protected void updatePlaces ()
	{
		String		current = (String) placeCB.getSelectedItem ();
		String[]	offered = placesFor ((String) actionCB.getSelectedItem ());
		placeCB.setModel (new javax.swing.DefaultComboBoxModel<String> (offered));
		if (current != null)
			for (String p : offered)		if (p.equals (current))		{ placeCB.setSelectedItem (current); break; }
		// nowhere to go, or nothing its planner would make of it: nothing to add
		addBT.setEnabled ((offered.length > 0) && (actionCB.getItemCount () > 0));
	}

	/** The actions the planner of the robot the tasks are addressed to understands. */
	protected String[] actions ()
	{
		return actionsOf (planners.get (getRobot ()));
	}

	/**
	 * Refills the Action selector for the robot the tasks are addressed to,
	 * keeping the selected action when its planner understands it too, and the
	 * Place selector after it.
	 */
	protected void updateActions ()
	{
		String		current = (String) actionCB.getSelectedItem ();
		String[]	offered = actions ();

		actionCB.setModel (new javax.swing.DefaultComboBoxModel<String> (offered));
		if (current != null)
			for (String a : offered)		if (a.equals (current))		{ actionCB.setSelectedItem (current); break; }
		updatePlaces ();
	}

	private void buildGUI (String[] places)
	{
		// --- first line: action, place, add
		actionCB	= new JComboBox<String> (actions ());
		placeCB		= new JComboBox<String> (places);
		addBT		= new JButton ("Add Task");
		actionCB.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ updatePlaces (); }
		});
		updatePlaces ();
		addBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				model.add ((String) placeCB.getSelectedItem (), (String) actionCB.getSelectedItem ());
				table.setRowSelectionInterval (model.getRowCount () - 1, model.getRowCount () - 1);
				updateButtons ();
			}
		});
		JPanel		top = new JPanel (new FlowLayout (FlowLayout.LEFT, 6, 4));
		top.add (new JLabel ("Action"));
		top.add (actionCB);
		top.add (Box.createHorizontalStrut (8));
		top.add (new JLabel ("Place"));
		top.add (placeCB);
		top.add (Box.createHorizontalStrut (8));
		top.add (addBT);

		// --- second line: task table with reorder / remove controls
		model	= new TaskModel ();
		table	= new JTable (model);
		table.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight (22);
		table.getSelectionModel ().addListSelectionListener (new ListSelectionListener ()
		{
			public void valueChanged (ListSelectionEvent e)		{ updateButtons (); }
		});
		JScrollPane	scroll = new JScrollPane (table);
		scroll.setPreferredSize (new Dimension (380, 180));

		upBT		= new JButton ("Up");
		downBT		= new JButton ("Down");
		removeBT	= new JButton ("Remove");
		clearBT		= new JButton ("Clear");
		clearBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ model.clear (); updateButtons (); }
		});
		upBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ move (-1); }
		});
		downBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ move (1); }
		});
		removeBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ removeSelected (); }
		});
		JPanel		side = new JPanel ();
		side.setLayout (new BoxLayout (side, BoxLayout.Y_AXIS));
		side.setBorder (BorderFactory.createEmptyBorder (0, 6, 0, 0));
		for (JButton b : new JButton[] { upBT, downBT, removeBT, clearBT })
		{
			b.setAlignmentX (0f);
			b.setMaximumSize (new Dimension (Integer.MAX_VALUE, b.getPreferredSize ().height));
			side.add (b);
			side.add (Box.createVerticalStrut (4));
		}
		JPanel		center = new JPanel (new BorderLayout ());
		center.setBorder (BorderFactory.createTitledBorder ("Task set"));
		center.add (scroll, BorderLayout.CENTER);
		center.add (side, BorderLayout.EAST);

		// --- bottom: cancel / send
		cancelBT	= new JButton ("Cancel");
		sendBT		= new JButton ("Send");
		cancelBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ result = null; dispose (); }
		});
		sendBT.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ result = sequence (); dispose (); }
		});
		JPanel		bottom = new JPanel (new BorderLayout ());
		JPanel		buttons = new JPanel (new FlowLayout (FlowLayout.RIGHT, 6, 6));
		buttons.add (cancelBT);
		buttons.add (sendBT);
		bottom.add (buttons, BorderLayout.EAST);
		if (robots.length > 1)
		{
			robotCB	= new JComboBox<String> (robots);
			robotCB.addActionListener (new ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ updateActions (); }		// another robot, another planner
			});
			JPanel	rp = new JPanel (new FlowLayout (FlowLayout.LEFT, 6, 6));
			rp.add (new JLabel ("Send to robot"));
			rp.add (robotCB);
			bottom.add (rp, BorderLayout.WEST);
		}
		getRootPane ().setDefaultButton (sendBT);
		getRootPane ().getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_ESCAPE, 0), "cancel");
		getRootPane ().getActionMap ().put ("cancel", new javax.swing.AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ result = null; dispose (); }
		});
		// Delete key removes the selected task
		table.getInputMap (JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put (KeyStroke.getKeyStroke (KeyEvent.VK_DELETE, 0), "remove");
		table.getInputMap (JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put (KeyStroke.getKeyStroke (KeyEvent.VK_BACK_SPACE, 0), "remove");
		table.getActionMap ().put ("remove", new javax.swing.AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ removeSelected (); }
		});

		JPanel		content = new JPanel (new BorderLayout ());
		content.setBorder (BorderFactory.createEmptyBorder (6, 8, 4, 8));
		content.add (top, BorderLayout.NORTH);
		content.add (center, BorderLayout.CENTER);
		content.add (bottom, BorderLayout.SOUTH);
		setContentPane (content);
	}

	private void move (int delta)
	{
		int	r = table.getSelectedRow ();
		int	to = r + delta;
		if ((r < 0) || (to < 0) || (to >= model.getRowCount ()))		return;
		model.move (r, to);
		table.setRowSelectionInterval (to, to);
		updateButtons ();
	}

	private void removeSelected ()
	{
		int	r = table.getSelectedRow ();
		if (r < 0)				return;
		model.remove (r);
		if (model.getRowCount () > 0)
		{
			int	sel = Math.min (r, model.getRowCount () - 1);
			table.setRowSelectionInterval (sel, sel);
		}
		updateButtons ();
	}

	private void updateButtons ()
	{
		int		r = table.getSelectedRow ();
		int		n = model.getRowCount ();
		upBT.setEnabled (r > 0);
		downBT.setEnabled ((r >= 0) && (r < n - 1));
		removeBT.setEnabled (r >= 0);
		clearBT.setEnabled (n > 0);
		sendBT.setEnabled (n > 0);								// Send only with at least one task
	}

	/** Robot the task set is addressed to (the only one when there is no selector). */
	public String getRobot ()
	{
		if (robotCB != null)		return (String) robotCB.getSelectedItem ();
		return (robots.length > 0) ? robots[0] : null;
	}

	/** The task set as a plan. */
	public Sequence sequence ()
	{
		Sequence	seq = new Sequence (model.getRowCount ());
		for (int i = 0; i < model.getRowCount (); i++)
			seq.set (i, model.tasks.get (i)[0], model.tasks.get (i)[1]);
		return seq;
	}

	/** Shows the dialog; returns the task set to send, or null when cancelled. */
	public Sequence showDialog ()
	{
		result = null;
		setVisible (true);
		return result;
	}
}
