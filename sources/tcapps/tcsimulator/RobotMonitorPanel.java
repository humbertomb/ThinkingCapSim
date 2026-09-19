/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import tc.gui.monitor.EventList;
import tc.gui.monitor.EventListRenderer;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemLPS;
import tc.shared.linda.ItemStatus;
import tc.shared.linda.Linda;
import tc.shared.linda.LindaListener;
import tc.shared.linda.Tuple;
import tc.shared.world.World;

/**
 * "Robots" and "Events" tables of a running architecture, fed directly from
 * the local Linda space of each robot: CONFIG registers the robot, LPS says
 * where it is and STATUS its state and the event log.
 */
public class RobotMonitorPanel extends JTabbedPane
{
	private static final long		serialVersionUID = 1L;

	static public final long		LPS_PERIOD	= 500;		// min. ms between position updates of a robot
	static public final int			VISIBLE_ROWS	= 6;	// preferred rows of the tables

	protected Robots				robots;
	protected EventList				events;
	protected JTable				robotTB, eventTB;
	protected JScrollPane			robotSP, eventSP;

	protected World					world;					// to name the zone of the robot position
	protected Map<String, Long>		ltimes	= new HashMap<String, Long> ();

	/**
	 * The Robots table: a robot, where it is and how it is, which is all of a
	 * robot this says. A robot appears when it announces itself and stays until
	 * an execution ends.
	 */
	protected class Robots extends AbstractTableModel
	{
		private static final long		serialVersionUID = 1L;

		private final String[]			COLUMNS = { "Robot", "Position", "Status" };
		private final List<String>		ids = new ArrayList<String> ();		// in the order they announced themselves
		private final Map<String, String>	where = new HashMap<String, String> ();
		private final Map<String, String>	how = new HashMap<String, String> ();

		/** A robot that has announced itself; one that had already done so is left as it is. */
		void add (String id)
		{
			if (ids.contains (id))		return;
			ids.add (id);
			fireTableDataChanged ();
		}

		void position (String id, String zone)
		{
			if (!ids.contains (id))		return;
			where.put (id, zone);
			fireTableDataChanged ();
		}

		void status (String id, String status)
		{
			if (!ids.contains (id))		return;
			how.put (id, status);
			fireTableDataChanged ();
		}

		void clear ()
		{
			ids.clear ();
			where.clear ();
			how.clear ();
			fireTableDataChanged ();
		}

		public int		getRowCount ()				{ return ids.size (); }
		public int		getColumnCount ()			{ return COLUMNS.length; }
		public String	getColumnName (int col)		{ return COLUMNS[col]; }

		public Object getValueAt (int row, int col)
		{
			String	id = ids.get (row);
			String	value;

			switch (col)
			{
			case 0:		value = id;					break;
			case 1:		value = where.get (id);		break;
			default:	value = how.get (id);		break;
			}
			return (value == null) ? "N/A" : value;
		}
	}

	public RobotMonitorPanel ()
	{
		super (SwingConstants.LEFT);
		robots	= new Robots ();
		events	= new EventList ();

		robotTB	= new JTable (robots);
		robotTB.setRowHeight (20);
		robotTB.setPreferredScrollableViewportSize (new Dimension (500, VISIBLE_ROWS * 20));
		robotTB.setGridColor (Color.lightGray);
		robotTB.setShowGrid (true);
		robotTB.setShowHorizontalLines (true);
		robotTB.setShowVerticalLines (false);
		robotTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		robotSP	= new JScrollPane (robotTB);

		eventTB	= new JTable (events);
		eventTB.setRowHeight (20);
		eventTB.setPreferredScrollableViewportSize (new Dimension (500, VISIBLE_ROWS * 20));
		eventTB.setDefaultRenderer (Object.class, new EventListRenderer (eventTB));
		eventTB.setGridColor (Color.lightGray);
		eventTB.setShowGrid (true);
		eventTB.setShowHorizontalLines (true);
		eventTB.setShowVerticalLines (false);
		eventTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		eventSP	= new JScrollPane (eventTB);

		insertTab ("Robots", null, robotSP, null, 0);
		insertTab ("Events", null, eventSP, null, 1);
		setMinimumSize (new Dimension (300, 60));
	}

	public EventList	getEventList ()		{ return events; }
	public void			setWorld (World w)	{ world = w; }

	/* ------------------------------------------------------------------ */
	/* Linda                                                               */
	/* ------------------------------------------------------------------ */

	/** A local Linda space being listened to, with the robot it belongs to. */
	protected class Attachment implements LindaListener
	{
		Linda	linda;
		String	robotid;

		Attachment (Linda linda, String robotid)	{ this.linda = linda; this.robotid = robotid; }

		public void notify (Tuple tuple)			{ RobotMonitorPanel.this.notify (tuple, robotid); }

		void register ()
		{
			linda.register (new Tuple (Tuple.CONFIG), this);
			linda.register (new Tuple (Tuple.LPS), this);
			linda.register (new Tuple (Tuple.STATUS), this);
		}

		void unregister ()
		{
			try
			{
				linda.unregister (new Tuple (Tuple.CONFIG), this);
				linda.unregister (new Tuple (Tuple.LPS), this);
				linda.unregister (new Tuple (Tuple.STATUS), this);
			} catch (Exception e) { }
		}
	}

	protected java.util.List<Attachment>	attachments	= new java.util.ArrayList<Attachment> ();

	/**
	 * Starts listening to the robot of a local Linda space (several robots
	 * can be attached, one Linda space each). Inside the robot the modules
	 * write with space "any"; the router relabels the tuples with the robot id
	 * before forwarding them to the global monitor, so <code>robotid</code>
	 * plays that role here.
	 */
	public void attach (Linda linda, String robotid)
	{
		if (linda == null)		return;
		Attachment	a = new Attachment (linda, robotid);
		synchronized (attachments) { attachments.add (a); }
		a.register ();
	}

	/** Stops listening to every robot. */
	public void detach ()
	{
		java.util.List<Attachment>	l;
		synchronized (attachments) { l = new java.util.ArrayList<Attachment> (attachments); attachments.clear (); }
		for (Attachment a : l)		a.unregister ();
	}

	/** Removes every robot and event (a new execution starts). */
	public void clear ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				robots.clear ();
				events.clear ();
				ltimes.clear ();
			}
		});
	}

	/** Linda callback (module threads) of one robot: dispatched to the event thread. */
	protected void notify (final Tuple tuple, String robotid)
	{
		if ((tuple.key == null) || (tuple.value == null))		return;
		final String	id = ((tuple.space == null) || tuple.space.equals (tc.shared.linda.LindaEntryFilter.ANY)) ? robotid : tuple.space;

		if (tuple.key.equals (Tuple.CONFIG))
		{
			if (((ItemConfig) tuple.value).props_robot == null)		return;		// not a robot announcing itself
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()		{ robots.add (id); }
			});
		}
		else if (tuple.key.equals (Tuple.LPS))
		{
			// a robot moves faster than a table is worth redrawing
			long	now = System.currentTimeMillis ();
			Long	last = ltimes.get (id);
			if ((last != null) && (now - last < LPS_PERIOD))		return;
			ltimes.put (id, now);
			final ItemLPS	item = (ItemLPS) tuple.value;
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					// where it says it is: the zone of the world it is in, and no more
					robots.position (id, (world != null) ? world.toString (item.lps.cur.x (), item.lps.cur.y ()) : "unknown");
				}
			});
		}
		else if (tuple.key.equals (Tuple.STATUS))
		{
			final ItemStatus	item = (ItemStatus) tuple.value;
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (ItemStatus.typeIsReport (item.type))
					{
						events.addRow (id, item.timestamp.longValue (), item.type, item.message);
						eventTB.setRowSelectionInterval (events.getRowCount () - 1, events.getRowCount () - 1);
						JScrollBar	scroll = eventSP.getVerticalScrollBar ();
						scroll.setValue (scroll.getMaximum ());
					}
					robots.status (id, ItemStatus.typeToString (item.type) + ". " + item.message);
				}
			});
		}
	}
}
