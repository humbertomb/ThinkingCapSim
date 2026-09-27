/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import tclib.behaviours.lua.LuaBridge;
import tclib.behaviours.lua.interpreter.Lua;
import tclib.behaviours.lua.interpreter.LuaFunction;
import tclib.behaviours.lua.interpreter.LuaState;
import tclib.behaviours.lua.interpreter.LuaTable;

/**
 * The variables of the Lua scripts an interpreter runs, in a table: every
 * variable in use, what it is worth and where it lives. It is what the Lua
 * monitor shows, and what the HFSM monitor shows under the diagram: the scripts
 * of the states of a machine run in an interpreter of the same kind.
 *
 * Four kinds of variable are told apart in the Scope column:
 * <pre>
 *   local &lt;script&gt;   a local of that script, as its last run left it
 *   global            a global of the interpreter, which every script shares
 *   chaos global      what the scripts left for one another through the bridge (chaos.setGlobal), named after it
 *   command           what the scripts asked the robot for on this cycle
 * </pre>
 * The locals shown are those of the scripts that are current (see
 * {@link Current}): the program being run, the state the machine is in. The
 * rest, and what the library and the bridge put in the globals, come out when
 * "Show all variables" is ticked. A row that has just changed is highlighted; a
 * local declared from a call that could not answer (chaos.getLpo of a constant
 * there is not) is red, and says why when pointed at.
 *
 * It only reads: whoever owns it calls {@link #refresh()} as often as it wants
 * the table looked at again.
 */
public class LuaVarsPanel extends JPanel
{
	private static final long		serialVersionUID = 1L;

	/* The scopes, as they are written in the table */
	static public final String		S_LOCAL		= "local";
	static public final String		S_GLOBAL	= "global";
	static public final String		S_CHAOS		= "chaos global";	// for a bridge called chaos: it is named after the bridge
	static public final String		S_COMMAND	= "command";

	/** How many fields of a table are written out one by one. */
	static public final int			FIELDS		= 16;

	static private final Color		C_CHANGED	= new Color (255, 246, 200);	// what has just changed
	static private final Color		C_SCOPE		= new Color (100, 100, 100);
	static private final Color		C_WRONG		= new Color (180, 0, 0);		// what is the matter with the scripts

	/** The names the library and the bridge take up, which are nobody's variables. */
	static private final String[]	LIBRARY		= { "math", "io", "string", "table", "os", "_VERSION" };

	/** Which scripts are the ones being run, by the name the interpreter knows them by. */
	public interface Current
	{
		public boolean isCurrent (String chunk);
	}

	/** How a script is named in the Scope column, when it is not by the name the interpreter knows it by. */
	public interface Labels
	{
		public String label (String chunk);
	}

	/** One row of the table. */
	static public class Var
	{
		public String				name;
		public String				value;
		public String				type;
		public String				scope;
		public boolean				changed;
		public String				wrong;				// why it is not what the script meant (a nil of chaos.getLpo given no object), or null

		Var (String name, String value, String type, String scope)
		{
			this.name	= name;
			this.value	= value;
			this.type	= type;
			this.scope	= scope;
		}

		String key ()				{ return scope + "/" + name; }
	}

	protected LuaState				lua;
	protected LuaBridge				chaos;						// the bridge the scripts speak through, or null
	protected Current				current;
	protected Labels				labels;						// null: the scripts are shown by the names the interpreter knows them by
	protected volatile String		wrong;						// what is the matter with the scripts, null for nothing
	protected String				what;						// what is being run, for the status bar (the program, the machine), or null

	protected Vars					vars;
	protected JTable				table;
	protected JLabel				status;
	protected JCheckBox				library;					// "Show all variables": the other scripts' locals and the library's globals too

	public LuaVarsPanel (LuaState lua, LuaBridge chaos, Current current)
	{
		super (new BorderLayout ());

		this.current	= current;

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

					boolean	wrong = (v != null) && (v.wrong != null);

					if (!sel)			setBackground ((v != null) && v.changed ? C_CHANGED : Color.WHITE);
					setForeground (wrong ? C_WRONG : (col == 3) ? C_SCOPE : Color.BLACK);
					setFont (getFont ().deriveFont ((col == 0) ? Font.BOLD : Font.PLAIN));
					setToolTipText (wrong ? v.wrong : null);
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
		library	= new JCheckBox ("Show all variables", false);
		library.setToolTipText ("The locals of the other scripts (behaviours, programs run before, other states) and what the library and the bridge put in the globals");
		library.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ refresh (); }
		});

		JPanel			bar = new JPanel (new BorderLayout (8, 0));

		bar.setBorder (BorderFactory.createEmptyBorder (3, 6, 3, 6));
		bar.add (status, BorderLayout.WEST);
		bar.add (library, BorderLayout.EAST);

		add (new JScrollPane (table), BorderLayout.CENTER);
		add (bar, BorderLayout.SOUTH);

		source (lua, chaos);
	}

	/** Reads another interpreter and bridge from now on (another program, another machine). */
	public void source (LuaState lua, LuaBridge chaos)
	{
		this.lua		= lua;
		this.chaos		= chaos;
		if (lua != null)			lua.interpreter ().watch (true);		// the locals are only kept track of when asked for
	}

	/** Which scripts are the current ones from now on. */
	public void current (Current c)					{ current = c; }
	/** How the scripts are named in the table from now on (null: as the interpreter knows them). */
	public void labels (Labels l)					{ labels = l; }

	/** A script as it is named in the table. */
	protected String label (String chunk)
	{
		return (labels != null) ? labels.label (chunk) : chunk;
	}
	/** What is being run, as the status bar names it (the program, the machine), or null. */
	public void what (String w)						{ what = w; }

	public final JTable				getTable ()			{ return table; }
	/** The variables as the last look at them found them. */
	public List<Var>				getVars ()			{ return vars.rows (); }
	/** Whether every variable is being shown, or only those of the current scripts. */
	public final boolean			showingAll ()		{ return library.isSelected (); }
	public void						showAll (boolean b)	{ library.setSelected (b); }

	/* ------------------------------------------------------------------ */
	/* Reading the variables                                              */
	/* ------------------------------------------------------------------ */

	/** Looks at the variables again, and says what has changed since the last look. */
	public void refresh ()
	{
		List<Var>		now;

		// the scripts run in another thread and may be writing while this reads:
		// this look at them is then given up, and the next one will do
		try { now = read (); }
		catch (java.util.ConcurrentModificationException e)		{ return; }
		catch (NullPointerException e)								{ return; }

		vars.set (now);
		status.setText (said ());
		status.setForeground ((wrong != null) ? C_WRONG : Color.BLACK);
	}

	/**
	 * What is the matter with the scripts, when they are not being run at all:
	 * whoever tried to read them says so here, and the status bar has it instead
	 * of what a script that runs is doing. Null for nothing the matter.
	 */
	public void problem (String text)
	{
		wrong	= ((text != null) && (text.trim ().length () > 0)) ? text.trim () : null;
	}

	/** What is the matter with the scripts, null when there is nothing. */
	public final String				problem ()			{ return wrong; }

	/** Something to say in the status bar until the next look at the variables. */
	public void notice (String text)				{ status.setText (text); }

	protected String said ()
	{
		String			beh = (chaos != null) ? chaos.behaviour () : null;

		// the error of a script says which one and where already
		if (wrong != null)
			return ((what == null) || wrong.startsWith (what)) ? wrong : (what + ": " + wrong);

		return vars.getRowCount () + " variables"
			   + ((what != null) ? ("   " + what) : "")
			   + ((beh != null) ? ("   behaviour " + beh) : "");
	}

	/** Every variable in use, in the order they are shown: locals, globals, the bridge's. */
	protected List<Var> read ()
	{
		List<Var>		rows = new ArrayList<Var> ();

		if (lua != null)
		{
			// the locals of the scripts being run, as their last run left them -- and of
			// every other script only when all the variables are asked for: a behaviour
			// or a program run before has locals of the same names, and side by side
			// they would not be told apart
			// A local declared from a call that could not answer (chaos.getLpo of a
			// constant there is not) is nil and to blame for what follows: its row is red,
			// and says why when pointed at.
			// A table several scripts hold under the same name with the same in it (the
			// ball, asked of chaos.getLpo by a state and by the transitions out of it) is
			// one row, its scope naming every script, rather than the same fields over
			// and over.
			Map<String, Map<String, String>>	wrongs = lua.interpreter ().complaints ();
			List<Var>							shared = new ArrayList<Var> ();		// the tables, one row per distinct one
			List<LuaTable>						tables = new ArrayList<LuaTable> ();	// and the table each of them is

			for (Map.Entry<String, Map<String, Object>> e : lua.interpreter ().locals ().entrySet ())
			{
				if (!library.isSelected () && !isCurrent (e.getKey ()))		continue;

				Map<String, String>		bad = wrongs.get (e.getKey ());

				for (Map.Entry<String, Object> v : e.getValue ().entrySet ())
				{
					String	name = v.getKey ();
					Object	value = v.getValue ();
					String	wrong = ((bad != null) && bad.containsKey (name)) ? bad.get (name) : null;

					if ((value instanceof LuaTable) && (wrong == null))
					{
						int		k = sameTable (shared, tables, name, (LuaTable) value);

						if (k >= 0)											// seen already: one more script holds it
						{
							shared.get (k).scope	+= ", " + label (e.getKey ());
							continue;
						}
						Var		row = var (name, value, S_LOCAL + " " + label (e.getKey ()));

						shared.add (row);
						tables.add ((LuaTable) value);
						continue;
					}

					int		at = rows.size ();

					add (rows, name, value, S_LOCAL + " " + label (e.getKey ()));
					if ((wrong != null) && (rows.size () > at))
						rows.get (at).wrong	= wrong;
				}
			}
			for (int k = 0; k < shared.size (); k++)						// the tables, with their fields, after the plain locals
				add (rows, shared.get (k), tables.get (k));

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

				if (!library.isSelected () && (isLibrary (name, value) || ((chaos != null) && name.equals (chaos.name ()))))		continue;
				add (rows, name, value, S_GLOBAL);
			}
		}
		if (chaos != null)
		{
			String	scope = chaos.name () + " global";

			for (Map.Entry<String, Object> e : chaos.globals ().entrySet ())
				rows.add (var (e.getKey (), e.getValue (), scope));

			// and what the scripts are asking the robot for right now
			for (Map.Entry<String, Object> e : chaos.commands ().entrySet ())
				rows.add (var (e.getKey (), e.getValue (), S_COMMAND));
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
		Var		row = var (name, value, scope);

		if (value instanceof LuaTable)			add (rows, row, (LuaTable) value);
		else									rows.add (row);
	}

	/** A table, as a row of its own and one under it per field, all with the scope of the table's row. */
	protected void add (List<Var> rows, Var row, LuaTable t)
	{
		rows.add (row);
		if (t.keys ().size () > FIELDS)			return;
		for (Object k : t.keys ())
		{
			Object	f = t.get (k);

			if (f instanceof LuaFunction)		continue;					// a table of functions is a library, not data
			rows.add (var (row.name + "." + Lua.tostring (k), f, row.scope));
		}
	}

	/**
	 * Which of the tables gathered so far is the same as this one, by name and by
	 * what it holds: the very object, or one with the same fields worth the same.
	 * -1 for none.
	 */
	static protected int sameTable (List<Var> rows, List<LuaTable> tables, String name, LuaTable t)
	{
		for (int i = 0; i < rows.size (); i++)
		{
			if (!rows.get (i).name.equals (name))		continue;

			LuaTable	o = tables.get (i);

			if ((o == t) || sameContent (o, t))			return i;
		}
		return -1;
	}

	/** Whether two tables hold the same fields, each worth the same (as written in the table). */
	static protected boolean sameContent (LuaTable a, LuaTable b)
	{
		List<Object>	ka = a.keys ();

		if (ka.size () != b.keys ().size ())			return false;
		for (Object k : ka)
		{
			Object	fa = a.get (k), fb = b.get (k);

			if ((fa instanceof LuaTable) && (fb instanceof LuaTable))
			{
				if (!sameContent ((LuaTable) fa, (LuaTable) fb))	return false;
			}
			else if (!text (fa).equals (text (fb)))		return false;
		}
		return true;
	}

	/** Whether a script is one of those being run, by the name the interpreter knows it by. */
	protected boolean isCurrent (String chunk)
	{
		return (current != null) && current.isCurrent (chunk);
	}

	/** Whether a global is the library's or the bridge itself, and so nobody's variable. */
	static public boolean isLibrary (String name, Object value)
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
	static public String text (Object value)
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
}
