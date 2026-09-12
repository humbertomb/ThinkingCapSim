/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import tc.coord.RobotList;
import tc.gui.monitor.EventList;
import tc.gui.monitor.EventListRenderer;
import tc.modules.MonitorData;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemGoal;
import tc.shared.linda.ItemLPS;
import tc.shared.linda.ItemStatus;
import tc.shared.linda.Linda;
import tc.shared.linda.LindaListener;
import tc.shared.linda.Tuple;
import tc.shared.world.World;
import tc.vrobot.RobotDesc;
import tclib.utils.fusion.FusionDesc;

/**
 * "Robots" and "Events" tables of the monitor (tc.gui.monitor.MultiRobotPanel),
 * fed directly from the local Linda space of the running architecture: CONFIG
 * registers the robot, LPS updates its position (as the Linda router does for
 * the global monitor), GOAL its destination and STATUS its state and the event
 * log. Same table models as the monitor: {@link RobotList} and {@link EventList}.
 */
public class RobotMonitorPanel extends JTabbedPane
{
	private static final long		serialVersionUID = 1L;

	static public final long		LPS_PERIOD	= 500;		// min. ms between position updates of a robot
	static public final int			VISIBLE_ROWS	= 6;	// preferred rows of the tables

	protected RobotList				robots;
	protected EventList				events;
	protected JTable				robotTB, eventTB;
	protected JScrollPane			robotSP, eventSP;

	protected World					world;					// to name the zone of the robot position
	protected java.util.Map<String, MonitorData>	mdatas	= new java.util.HashMap<String, MonitorData> ();	// per robot
	protected java.util.Map<String, Long>			ltimes	= new java.util.HashMap<String, Long> ();

	public RobotMonitorPanel ()
	{
		super (SwingConstants.LEFT);
		robots	= new RobotList (RobotList.MAXROBOTS);
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

	public RobotList	getRobotList ()		{ return robots; }
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
			linda.register (new Tuple (Tuple.GOAL), this);
		}

		void unregister ()
		{
			try
			{
				linda.unregister (new Tuple (Tuple.CONFIG), this);
				linda.unregister (new Tuple (Tuple.LPS), this);
				linda.unregister (new Tuple (Tuple.STATUS), this);
				linda.unregister (new Tuple (Tuple.GOAL), this);
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
		final String[]	ids = robots.getIDs ();
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				for (String id : ids)		robots.delete (id);
				events.clear ();
				mdatas.clear ();
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
			final ItemConfig	item = (ItemConfig) tuple.value;
			if (item.props_robot == null)		return;
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()		{ robots.add (id, new RobotDesc (item.props_robot), new FusionDesc (item.props_robot)); }
			});
		}
		else if (tuple.key.equals (Tuple.LPS))
		{
			// same throttling as LindaRouter when it builds the MONITOR tuple for the global monitor
			long	now = System.currentTimeMillis ();
			Long	last = ltimes.get (id);
			if ((last != null) && (now - last < LPS_PERIOD))		return;
			ltimes.put (id, now);
			final ItemLPS	item = (ItemLPS) tuple.value;
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					MonitorData	mdata = mdatas.get (id);
					if (mdata == null)		mdatas.put (id, mdata = new MonitorData ());
					mdata.update (item.lps);
					String	posmsg = (world != null) ? world.toString (mdata.cur.x (), mdata.cur.y ()) : "unknown";
					robots.update (id, mdata, new tc.shared.lps.lpo.LPO[0], posmsg);
				}
			});
		}
		else if (tuple.key.equals (Tuple.GOAL))
		{
			final ItemGoal	item = (ItemGoal) tuple.value;
			if ((item.task == null) || (item.task.tpos == null))		return;
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()		{ robots.update (id, item.task.tpos); }
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
					robots.update (id, ItemStatus.typeToString (item.type) + ". " + item.message);
				}
			});
		}
	}
}
