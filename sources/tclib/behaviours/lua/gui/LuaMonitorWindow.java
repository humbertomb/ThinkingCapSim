/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import tclib.behaviours.lua.Chaos;
import tclib.behaviours.lua.interpreter.Lua;
import tclib.behaviours.lua.interpreter.LuaFunction;
import tclib.behaviours.lua.interpreter.LuaState;
import tclib.behaviours.lua.interpreter.LuaTable;

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
 * What the library puts in the globals (math, string, io, table, os) and the
 * bridge itself are left out unless they are asked for. A variable whose value
 * is not what it was on the cycle before is marked, so that what moves can be
 * seen at a glance.
 *
 * It only reads, and reads on its own every {@link #PERIOD} milliseconds, so the
 * module that runs the program has nothing to tell it.
 */
public class LuaMonitorWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** How often the variables are looked at [ms]. */
	static public final int			PERIOD		= 200;

	/* The scopes, as they are written in the table */
	static public final String		S_LOCAL		= "local";
	static public final String		S_GLOBAL	= "global";
	static public final String		S_CHAOS		= "chaos global";
	static public final String		S_COMMAND	= "command";

	static private final Color		C_CHANGED	= new Color (255, 246, 200);	// what has just changed
	static private final Color		C_SCOPE		= new Color (100, 100, 100);

	/** The names the library and the bridge take up, which are nobody's variables. */
	static private final String[]	LIBRARY		= { "math", "io", "string", "table", "os", "chaos", "_VERSION" };

	protected LuaState				lua;
	protected Chaos					chaos;
	protected String				program;

	protected Vars					vars;
	protected JTable				table;
	protected JLabel				status;
	protected JCheckBox			library;
	protected Timer					timer;

	/** One row of the table. */
	static public class Var
	{
		public String				name;
		public String				value;
		public String				type;
		public String				scope;
		public boolean				changed;

		Var (String name, String value, String type, String scope)
		{
			this.name	= name;
			this.value	= value;
			this.type	= type;
			this.scope	= scope;
		}

		String key ()				{ return scope + "/" + name; }
	}

	public LuaMonitorWindow (LuaState lua, Chaos chaos, String program)
	{
		this (lua, chaos, program, null);
	}

	/** Watches the variables of a program, the window named after the robot that runs it. */
	public LuaMonitorWindow (LuaState lua, Chaos chaos, String program, String robot)
	{
		super ("Lua Monitor" + ((robot != null) ? (" [" + robot + "]") : "")
			   + ((program != null) ? (": " + program) : ""));

		this.lua		= lua;
		this.chaos		= chaos;
		this.program	= program;

		if (lua != null)			lua.interpreter ().watch (true);		// the locals are only kept track of when asked for

		vars	= new Vars ();
		table	= new JTable (vars)
		{
			private static final long	serialVersionUID = 1L;

			private final DefaultTableCellRenderer	cells = new DefaultTableCellRenderer ()
			{
				private static final long	serialVersionUID = 1L;

				public Component getTableCellRendererComponent (JTable t, Object value, boolean sel, boolean focus, int row, int col)
				{
					super.getTableCellRendererComponent (t, value, sel, focus, row, col);

					Var		v = vars.at (row);

					if (!sel)			setBackground ((v != null) && v.changed ? C_CHANGED : Color.WHITE);
					setForeground ((col == 3) ? C_SCOPE : Color.BLACK);
					setFont (getFont ().deriveFont ((col == 0) ? Font.BOLD : Font.PLAIN));
					return this;
				}
			};

			public TableCellRenderer getCellRenderer (int row, int column)		{ return cells; }
		};
		table.setRowHeight (20);
		table.setShowGrid (false);
		table.setFont (new Font (Font.MONOSPACED, Font.PLAIN, 12));
		table.getColumnModel ().getColumn (0).setPreferredWidth (170);
		table.getColumnModel ().getColumn (1).setPreferredWidth (230);
		table.getColumnModel ().getColumn (2).setPreferredWidth (80);
		table.getColumnModel ().getColumn (3).setPreferredWidth (150);

		status	= new JLabel (" ");
		library	= new JCheckBox ("What the library puts in", false);
		library.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ refresh (); }
		});

		JPanel			bar = new JPanel (new BorderLayout (8, 0));

		bar.setBorder (BorderFactory.createEmptyBorder (3, 6, 3, 6));
		bar.add (status, BorderLayout.WEST);
		bar.add (library, BorderLayout.EAST);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (new JScrollPane (table), BorderLayout.CENTER);
		getContentPane ().add (bar, BorderLayout.SOUTH);
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

	public final JTable				getTable ()			{ return table; }
	/** The variables as the last look at them found them. */
	public List<Var>				getVars ()			{ return vars.rows (); }

	/* ------------------------------------------------------------------ */
	/* Reading the variables                                              */
	/* ------------------------------------------------------------------ */

	/** Looks at the variables again, and says what has changed since the last look. */
	public void refresh ()
	{
		List<Var>		now = read ();

		vars.set (now);
		status.setText (said ());
	}

	protected String said ()
	{
		String			beh = (chaos != null) ? chaos.behaviour () : null;

		return vars.getRowCount () + " variables"
			   + ((program != null) ? ("   program " + program) : "")
			   + ((beh != null) ? ("   behaviour " + beh) : "");
	}

	/** Every variable in use, in the order they are shown: locals, globals, the bridge's. */
	protected List<Var> read ()
	{
		List<Var>		rows = new ArrayList<Var> ();

		if (lua != null)
		{
			// the locals of every script, as its last run left them
			for (Map.Entry<String, Map<String, Object>> e : lua.interpreter ().locals ().entrySet ())
				for (Map.Entry<String, Object> v : e.getValue ().entrySet ())
					add (rows, v.getKey (), v.getValue (), S_LOCAL + " " + e.getKey ());

			// the globals every script shares
			LuaTable	g = lua.globals ();
			List<Object>	names = g.keys ();

			java.util.Collections.sort (names, new java.util.Comparator<Object> ()
			{
				public int compare (Object a, Object b)		{ return Lua.tostring (a).compareTo (Lua.tostring (b)); }
			});
			for (Object k : names)
			{
				String	name = Lua.tostring (k);
				Object	value = g.get (k);

				if (!library.isSelected () && isLibrary (name, value))		continue;
				add (rows, name, value, S_GLOBAL);
			}
		}
		if (chaos != null)
		{
			for (Map.Entry<String, Object> e : chaos.globals ().entrySet ())
				rows.add (var (e.getKey (), e.getValue (), S_CHAOS));

			// and what the program is asking the robot for right now
			rows.add (var ("vlin", Double.valueOf (chaos.vlin ()), S_COMMAND));
			rows.add (var ("vlat", Double.valueOf (chaos.vlat ()), S_COMMAND));
			rows.add (var ("vrot", Double.valueOf (chaos.vrot ()), S_COMMAND));
			rows.add (var ("behaviour", (chaos.behaviour () != null) ? chaos.behaviour () : null, S_COMMAND));
		}
		return rows;
	}

	/**
	 * A variable, and, when it is a table of its own (the object of the LPS a script
	 * asked for, what a behaviour was told about itself), what it holds, one field
	 * to a row under it: a table of many fields is only counted, as it is a lot of
	 * rows and little to read.
	 */
	protected void add (List<Var> rows, String name, Object value, String scope)
	{
		rows.add (var (name, value, scope));
		if (!(value instanceof LuaTable))		return;

		LuaTable		t = (LuaTable) value;

		if (t.keys ().size () > FIELDS)			return;
		for (Object k : t.keys ())
		{
			Object	f = t.get (k);

			if (f instanceof LuaFunction)		continue;					// a table of functions is a library, not data
			rows.add (var (name + "." + Lua.tostring (k), f, scope));
		}
	}

	/** How many fields of a table are written out one by one. */
	static public final int			FIELDS		= 16;

	/** Whether a global is the library's or the bridge itself, and so nobody's variable. */
	static protected boolean isLibrary (String name, Object value)
	{
		for (String s : LIBRARY)
			if (s.equals (name))				return true;
		return value instanceof LuaFunction;
	}

	/** One row: what a value is worth and what it is, as Lua has it. */
	static protected Var var (String name, Object value, String scope)
	{
		return new Var (name, text (value), Lua.type (value), scope);
	}

	/** A value as it is written in the table: a table says how much it holds. */
	static protected String text (Object value)
	{
		if (value == null)						return "nil";
		if (value instanceof LuaTable)			return "table (" + ((LuaTable) value).keys ().size () + " fields)";
		if (value instanceof LuaFunction)		return "function " + ((LuaFunction) value).name ();
		if (value instanceof Double)			return Lua.number (((Double) value).doubleValue ());
		if (value instanceof String)			return "\"" + value + "\"";
		return Lua.tostring (value);
	}

	/* ------------------------------------------------------------------ */
	/* The table                                                           */
	/* ------------------------------------------------------------------ */

	/** The rows of the table, which say which of them have just changed. */
	protected class Vars extends AbstractTableModel
	{
		private static final long	serialVersionUID = 1L;

		static private final String[]	COLUMNS = { "Variable", "Value", "Type", "Scope" };

		protected List<Var>			list = new ArrayList<Var> ();
		protected Map<String, String>	was = new HashMap<String, String> ();

		void set (List<Var> now)
		{
			for (Var v : now)
			{
				String	old = was.get (v.key ());

				v.changed	= (old != null) && !old.equals (v.value);
				was.put (v.key (), v.value);
			}
			list	= now;
			fireTableDataChanged ();
		}

		Var at (int r)						{ return ((r >= 0) && (r < list.size ())) ? list.get (r) : null; }
		List<Var> rows ()					{ return new ArrayList<Var> (list); }

		public int getRowCount ()			{ return list.size (); }
		public int getColumnCount ()		{ return COLUMNS.length; }
		public String getColumnName (int c)	{ return COLUMNS[c]; }
		public boolean isCellEditable (int r, int c)	{ return false; }

		public Object getValueAt (int r, int c)
		{
			Var		v = at (r);

			if (v == null)					return "";
			switch (c)
			{
			case 0:		return v.name;
			case 1:		return v.value;
			case 2:		return v.type;
			default:	return v.scope;
			}
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
	static public LuaMonitorWindow open (final LuaState lua, final Chaos chaos, final String program, final String robot)
	{
		final LuaMonitorWindow[]	w = new LuaMonitorWindow[1];

		try
		{
			SwingUtilities.invokeAndWait (new Runnable ()
			{
				public void run ()
				{
					w[0]	= new LuaMonitorWindow (lua, chaos, program, robot);
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
