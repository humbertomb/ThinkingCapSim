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

	static public final String[]	ACTIONS	= { "load", "unload", "goto", "stay" };

	protected String[]				robots;
	protected JComboBox<String>		robotCB;				// null with one robot or none
	protected JComboBox<String>		placeCB;
	protected JComboBox<String>		actionCB;
	protected JTable				table;
	protected TaskModel				model;
	protected JButton				addBT, upBT, downBT, removeBT, sendBT, cancelBT;
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
		super (owner, "Tasks", true);
		this.robots	= robots;
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
		String[]	p = new String[w.docks ().n ()];
		for (int i = 0; i < p.length; i++)		p[i] = w.docks ().at (i).label;
		return p;
	}

	private void buildGUI (String[] places)
	{
		// --- first line: place, action, add
		placeCB		= new JComboBox<String> (places);
		actionCB	= new JComboBox<String> (ACTIONS);
		addBT		= new JButton ("Add Task");
		addBT.setEnabled (places.length > 0);
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
		top.add (new JLabel ("Place"));
		top.add (placeCB);
		top.add (Box.createHorizontalStrut (8));
		top.add (new JLabel ("Action"));
		top.add (actionCB);
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
		for (JButton b : new JButton[] { upBT, downBT, removeBT })
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
