/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.arch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tcapps.tcsimulator.DeployArch;
import tcapps.tcsimulator.DeployArch.Event;
import tcapps.tcsimulator.DeployArch.Linda;
import tcapps.tcsimulator.DeployArch.Module;
import tcapps.tcsimulator.DeployArch.Robot;

/**
 * Editor view of a {@link DeployArch} as blocks: the global Linda space and,
 * for each robot, its local Linda space, optional router, modules and
 * virtual robot. Blocks are addressed by kind, robot index and (for the
 * modules) position; the model translates property keys (the ADF suffixes
 * the runtime reads) to the fields of the deployment.
 */
public class ArchModel
{
	/** Block kinds. */
	static public final int		GLOBAL_LINDA	= 0;
	static public final int		LOCAL_LINDA		= 1;
	static public final int		ROUTER			= 2;
	static public final int		MODULE			= 3;
	static public final int		VROBOT			= 4;
	static public final int		ROBOT			= 5;		// the robot container itself

	static public final String[]	KIND_NAMES	= { "Global Linda Space", "Local Linda Space", "Router", "Module", "Virtual Robot", "Robot" };


	/* --- property presentation ---------------------------------------- */

	/** Editor types of a property. */
	static public final int		P_TEXT		= 0;
	static public final int		P_BOOLEAN	= 1;
	static public final int		P_CHOICE	= 2;
	static public final int		P_FILE		= 3;

	/** Execution modes of a module (ThreadDesc.parse_mode) and protocols towards the global Linda (RouterDesc GMODE). */
	static public final String[]	MODES		= { "shared", "udp", "tcp" };
	static public final String[]	PROTOCOLS	= { "tcp", "udp", "shared" };

	/** How a property (suffix) of a block is shown and edited. */
	static public class Property
	{
		public String	key;			// suffix of the ADF key (INFO, CLASS, ...)
		public String	label;			// name shown to the user
		public int		type;			// P_TEXT, P_BOOLEAN, P_CHOICE, P_FILE
		public String[]	choices;		// P_CHOICE values
		public String	fileDir;		// P_FILE: default directory
		public String	fileDesc;		// P_FILE: filter description
		public String[]	fileExts;		// P_FILE: filter extensions

		public Property (String key, String label)								{ this (key, label, P_TEXT); }
		public Property (String key, String label, int type)						{ this.key = key; this.label = label; this.type = type; }
		public Property (String key, String label, String[] choices)				{ this (key, label, P_CHOICE); this.choices = choices; }
		public Property (String key, String label, String dir, String desc, String... exts)	{ this (key, label, P_FILE); fileDir = dir; fileDesc = desc; fileExts = exts; }
		public String toString ()	{ return label; }
	}

	static public final Property[]	ROBOT_PROPS	=
	{
		new Property ("START",	"Start pose (x, y, deg)"),
	};
	static public final Property[]	LINDA_PROPS	=
	{
		new Property ("ADDR",	"Address"),
		new Property ("PORT",	"Port"),
		new Property ("CREATE",	"Instantiate",	P_BOOLEAN),
	};
	static public final Property[]	ROUTER_PROPS	=
	{
		new Property ("INFO",	"Name"),
		new Property ("CLASS",	"Class"),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("GMODE",	"Protocol",		PROTOCOLS),
		new Property ("GFX",	"Internal Representation",	P_BOOLEAN),
	};
	static public final Property[]	MODULE_PROPS	=
	{
		new Property ("INFO",	"Name"),
		new Property ("CLASS",	"Class"),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("PASSIVE","Passive",		P_BOOLEAN),
		new Property ("QUEUED",	"Queued",		P_BOOLEAN),
		new Property ("POLLED",	"Polled",		P_BOOLEAN),
		new Property ("EXTIME",	"Exec. time (ms)"),
		new Property ("GFX",	"Internal Representation",	P_BOOLEAN),
	};
	static public final Property[]	VROBOT_PROPS	=
	{
		new Property ("INFO",	"Name"),
		new Property ("CLASS",	"Class"),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("PASSIVE","Passive",		P_BOOLEAN),
		new Property ("EXTIME",	"Exec. time (ms)"),
		new Property ("GFX",	"Internal Representation",	P_BOOLEAN),
		new Property ("DESC",	"DESC",			"./conf/robots",	"Robot descriptions (*.robot)",	"robot"),
		new Property ("CUST",	"CUST",			"./conf/robots",	"Robot customisations (*.cust)",	"cust"),
		new Property ("WORLD",	"WORLD",		"./conf/maps",		"World maps (*.world)",			"world"),
		new Property ("TOPOL",	"TOPOL",		"./conf/maps",		"Topological maps (*.topol)",	"topol"),
		new Property ("APW",	"APW",			P_BOOLEAN),
		new Property ("RADDR",	"RADDR"),
		new Property ("RPORT",	"RPORT"),
		new Property ("LPORT",	"LPORT"),
	};

	/** Suffixes that exist in the ADF but are not shown in the editor, per kind. */
	static public final String[]	LINDA_HIDDEN	= { "CLASS" };
	static public final String[]	ROUTER_HIDDEN	= { "PRI", "CONNECT" };
	static public final String[]	MODULE_HIDDEN	= { "PRI", "CONNECT" };		// CONNECT is edited in the events table
	static public final String[]	VROBOT_HIDDEN	= { "PRI", "CONNECT" };

	/** A block of the deployment: kind, robot (-1 for the global Linda) and, for modules, position in the robot. */
	static public class Block
	{
		public int		kind;
		public int		robot;			// index of the robot the block belongs to (-1: global)
		public int		index;			// MODULE: position in the module list; -1 otherwise

		public Block (int kind, int robot)				{ this (kind, robot, -1); }
		public Block (int kind, int robot, int index)	{ this.kind = kind; this.robot = robot; this.index = index; }

		public boolean equals (Object o)
		{
			if (!(o instanceof Block))		return false;
			Block	b = (Block) o;
			return (b.kind == kind) && (b.robot == robot) && (b.index == index);
		}
		public int hashCode ()		{ return (kind * 31 + robot) * 31 + index; }
		public String toString ()	{ return KIND_NAMES[kind] + " R" + robot + ((index >= 0) ? "#" + index : ""); }
	}

	protected DeployArch		deploy;

	public ArchModel (DeployArch deploy)		{ this.deploy = deploy; }

	public DeployArch	getDeploy ()				{ return deploy; }

	/* ------------------------------------------------------------------ */
	/* Robots                                                              */
	/* ------------------------------------------------------------------ */

	/** Indices of the robots, in order. */
	public List<Integer> robots ()
	{
		List<Integer>	l = new ArrayList<Integer> ();
		for (int i = 0; i < deploy.robots.size (); i++)		l.add (i);
		return l;
	}

	protected Robot robot (int r)					{ return deploy.robots.get (r); }

	public String	getRobotId (int r)				{ return robot (r).name; }
	public String	getRobotId ()					{ return deploy.robots.isEmpty () ? DeployArch.DEFAULT_ROBOT : getRobotId (0); }
	public void		setRobotName (int r, String n)	{ if ((n != null) && (n.trim ().length () > 0)) robot (r).name = n.trim (); }

	/* ------------------------------------------------------------------ */
	/* Queries                                                             */
	/* ------------------------------------------------------------------ */

	public boolean hasGlobalLinda ()				{ return deploy.globalLinda != null; }
	public boolean hasRobot ()						{ return deploy.robots.size () > 0; }
	public boolean hasRobot (int r)					{ return (r >= 0) && (r < deploy.robots.size ()); }
	public boolean hasLocalLinda (int r)			{ return hasRobot (r); }
	public boolean hasRouter (int r)				{ return hasRobot (r) && (robot (r).router != null); }
	public boolean hasVRobot (int r)				{ return hasRobot (r); }
	public int	   moduleCount (int r)				{ return hasRobot (r) ? robot (r).modules.size () : 0; }

	/** The module behind a block (router, module or virtual robot), or null. */
	public Module moduleOf (Block b)
	{
		if (!hasRobot (b.robot))		return null;
		Robot	r = robot (b.robot);
		switch (b.kind)
		{
		case ROUTER:	return r.router;
		case VROBOT:	return r.virtualRobot;
		case MODULE:	return ((b.index >= 0) && (b.index < r.modules.size ())) ? r.modules.get (b.index) : null;
		default:		return null;
		}
	}

	/** The Linda space behind a block (global or local), or null. */
	public Linda lindaOf (Block b)
	{
		if (b.kind == GLOBAL_LINDA)		return deploy.globalLinda;
		if ((b.kind == LOCAL_LINDA) && hasRobot (b.robot))		return robot (b.robot).linda;
		return null;
	}

	/** Blocks of a robot in display order: local Linda, router, modules, virtual robot. */
	public List<Block> robotBlocks (int r)
	{
		List<Block>	l = new ArrayList<Block> ();
		if (!hasRobot (r))		return l;
		l.add (new Block (LOCAL_LINDA, r));
		if (hasRouter (r))		l.add (new Block (ROUTER, r));
		for (int i = 0; i < robot (r).modules.size (); i++)		l.add (new Block (MODULE, r, i));
		l.add (new Block (VROBOT, r));
		return l;
	}

	/** Blocks of every robot. */
	public List<Block> allRobotBlocks ()
	{
		List<Block>	l = new ArrayList<Block> ();
		for (int r : robots ())		l.addAll (robotBlocks (r));
		return l;
	}

	/** True when the block is part of the model. */
	public boolean exists (Block b)
	{
		if (b.kind == GLOBAL_LINDA)		return hasGlobalLinda ();
		if (b.kind == ROBOT)			return hasRobot (b.robot);
		return robotBlocks (b.robot).contains (b);
	}

	/** Display label of a block: the module name, or the kind name. */
	public String labelOf (Block b)
	{
		if (b.kind == ROBOT)			return "ROBOT " + getRobotId (b.robot);
		if (b.kind == GLOBAL_LINDA)		return "Global Linda Space";
		if (b.kind == LOCAL_LINDA)		return "Local Linda Space";
		Module	m = moduleOf (b);
		return (m != null) ? m.name : KIND_NAMES[b.kind];
	}

	/** True when the block can be deleted: everything but the local Linda space and the virtual robot, which every robot must have. */
	public boolean isRemovable (Block b)
	{
		return (b.kind != LOCAL_LINDA) && (b.kind != VROBOT);
	}

	/** True when the name of the block can be edited (robot name or module name). */
	public boolean isRenameable (Block b)
	{
		return (b.kind == ROBOT) || (b.kind == ROUTER) || (b.kind == MODULE) || (b.kind == VROBOT);
	}

	/** Editable name of a block: the robot name, or the module name. */
	public String nameOf (Block b)
	{
		return (b.kind == ROBOT) ? getRobotId (b.robot) : labelOf (b);
	}

	public void setName (Block b, String name)
	{
		if ((name == null) || (name.trim ().length () == 0))		return;
		if (b.kind == ROBOT)		setRobotName (b.robot, name);
		else
		{
			Module	m = moduleOf (b);
			if (m != null)		m.name = name.trim ();
		}
	}

	/**
	 * Properties shown in the editor for a block: the standard ones of its
	 * kind (with their labels and editors), then any other existing in the
	 * module as plain text, minus the hidden ones.
	 */
	public List<Property> propertiesOf (Block b)
	{
		Property[]		std;
		String[]		hidden;
		switch (b.kind)
		{
		case ROBOT:			return java.util.Arrays.asList (ROBOT_PROPS);
		case GLOBAL_LINDA:
		case LOCAL_LINDA:	return java.util.Arrays.asList (LINDA_PROPS);
		case ROUTER:		std = ROUTER_PROPS;	hidden = ROUTER_HIDDEN;	break;
		case MODULE:		std = MODULE_PROPS;	hidden = MODULE_HIDDEN;	break;
		case VROBOT:		std = VROBOT_PROPS;	hidden = VROBOT_HIDDEN;	break;
		default:			return new ArrayList<Property> ();
		}
		List<Property>	props = new ArrayList<Property> ();
		List<String>	known = new ArrayList<String> ();
		for (Property p : std)		{ props.add (p); known.add (p.key); }
		for (String h : hidden)		known.add (h);
		Module	m = moduleOf (b);
		if (m != null)
		{
			List<String>	extra = new ArrayList<String> ();
			for (String k : m.properties.keySet ())
				if (!known.contains (k))		extra.add (k);
			Collections.sort (extra);
			for (String k : extra)		props.add (new Property (k, k));
		}
		return props;
	}

	/** Value of a property of a block ("" when unset). Linda blocks: ADDR, PORT, CREATE; modules: INFO (name) or a property. */
	public String get (Block b, String key)
	{
		if ((b.kind == ROBOT) && key.equals ("START"))
		{
			double[]	st = hasRobot (b.robot) ? robot (b.robot).start : null;
			return (st == null) ? "" : String.format (java.util.Locale.ROOT, "%.3f, %.3f, %.1f", st[0], st[1], st[2]);
		}
		Linda	l = lindaOf (b);
		if (l != null)
		{
			if (key.equals ("ADDR"))		return l.address;
			if (key.equals ("PORT"))		return String.valueOf (l.port);
			if (key.equals ("CREATE"))		return String.valueOf (l.instantiate);
			return "";
		}
		Module	m = moduleOf (b);
		if (m == null)					return "";
		if (key.equals ("INFO"))		return m.name;
		String	v = m.get (key);
		return (v == null) ? "" : v;
	}

	public void set (Block b, String key, String value)
	{
		String	v = (value == null) ? "" : value.trim ();
		if ((b.kind == ROBOT) && key.equals ("START"))
		{
			if (!hasRobot (b.robot))		return;
			if (v.length () == 0)		{ robot (b.robot).start = null; return; }
			try
			{
				String[]	t = v.split ("[,\\s]+");
				robot (b.robot).start = new double[] { Double.parseDouble (t[0]), Double.parseDouble (t[1]), (t.length > 2) ? Double.parseDouble (t[2]) : 0.0 };
			} catch (Exception e) { }									// malformed: unchanged
			return;
		}
		Linda	l = lindaOf (b);
		if (l != null)
		{
			if (key.equals ("ADDR"))		l.address = (v.length () > 0) ? v : "localhost";
			else if (key.equals ("PORT"))	{ try { l.port = Integer.parseInt (v); } catch (Exception e) { } }
			else if (key.equals ("CREATE"))	l.instantiate = Boolean.parseBoolean (v);
			return;
		}
		Module	m = moduleOf (b);
		if (m == null)					return;
		if (key.equals ("INFO"))		{ if (v.length () > 0) m.name = v; }
		else							m.set (key, v);
	}

	/* --- events --- */

	/** True when the block registers events: modules and the virtual robot. */
	public boolean hasEvents (Block b)
	{
		return (b != null) && ((b.kind == MODULE) || (b.kind == VROBOT));
	}

	/** Events of a module as { symbol, class, method } rows. */
	public List<String[]> events (Block b)
	{
		List<String[]>	l = new ArrayList<String[]> ();
		Module			m = moduleOf (b);
		if (m != null)
			for (Event e : m.events)		l.add (new String[] { e.symbol, e.itemClass, e.method });
		return l;
	}

	/** Replaces the events of a module (blank rows are dropped). */
	public void setEvents (Block b, List<String[]> rows)
	{
		Module	m = moduleOf (b);
		if (m == null)		return;
		m.events.clear ();
		for (String[] e : rows)
		{
			if ((e[0].trim ().length () == 0) && (e[1].trim ().length () == 0) && (e[2].trim ().length () == 0))		continue;
			m.events.add (new Event (e[0].trim (), e[1].trim (), e[2].trim ()));
		}
	}

	/**
	 * Symbols an event can register: the keys defined in {@link tc.shared.linda.Tuple}
	 * (public static String constants) plus any other symbol already used in
	 * the events of this deployment (COORD, ZONE, ...), sorted.
	 */
	public List<String> symbols ()
	{
		java.util.TreeSet<String>	set = new java.util.TreeSet<String> ();
		for (java.lang.reflect.Field f : tc.shared.linda.Tuple.class.getFields ())
			if (java.lang.reflect.Modifier.isStatic (f.getModifiers ()) && (f.getType () == String.class))
				try { set.add ((String) f.get (null)); } catch (Exception e) { }
		for (Block b : allRobotBlocks ())
			if (hasEvents (b))
				for (String[] e : events (b))
					if (e[0].length () > 0)		set.add (e[0]);
		return new ArrayList<String> (set);
	}

	/* ------------------------------------------------------------------ */
	/* Edition                                                             */
	/* ------------------------------------------------------------------ */

	public Block addGlobalLinda ()
	{
		if (deploy.globalLinda == null)		deploy.globalLinda = DeployArch.newGlobalLinda ();
		return new Block (GLOBAL_LINDA, -1);
	}

	/** Adds a robot named "Unnamed" (or "Unnamed2", ...) with its local Linda space and virtual robot. */
	public Block addRobot ()
	{
		String	name = DeployArch.DEFAULT_ROBOT;
		for (int i = 2; robotNames ().contains (name); i++)		name = DeployArch.DEFAULT_ROBOT + i;
		deploy.robots.add (new Robot (name));
		return new Block (ROBOT, deploy.robots.size () - 1);
	}

	/** Adds an existing robot (imported), renaming it if the name is already in use. */
	public Block addRobot (Robot r)
	{
		String	base = r.name, name = base;
		for (int i = 2; robotNames ().contains (name); i++)		name = base + i;
		r.name	= name;
		deploy.robots.add (r);
		return new Block (ROBOT, deploy.robots.size () - 1);
	}

	protected List<String> robotNames ()
	{
		List<String>	l = new ArrayList<String> ();
		for (Robot r : deploy.robots)		l.add (r.name);
		return l;
	}

	public Block addRouter (int r)
	{
		if (!hasRobot (r))				return null;
		if (robot (r).router == null)	robot (r).router = DeployArch.newRouter ();
		return new Block (ROUTER, r);
	}

	/** Adds a module named "Module 1", "Module 2", ... (first name not used in the robot). */
	public Block addModule (int r)
	{
		if (!hasRobot (r))				return null;
		robot (r).modules.add (DeployArch.newModule (uniqueModuleName (r, "Module")));
		return new Block (MODULE, r, robot (r).modules.size () - 1);
	}

	protected String uniqueModuleName (int r, String base)
	{
		List<String>	names = new ArrayList<String> ();
		for (Block b : robotBlocks (r))		names.add (labelOf (b));
		for (int i = 1; ; i++)
			if (!names.contains (base + " " + i))		return base + " " + i;
	}

	/** Removes a block (the robot container removes the whole robot; local Linda and virtual robot cannot be removed). */
	public void remove (Block b)
	{
		switch (b.kind)
		{
		case GLOBAL_LINDA:	deploy.globalLinda = null;						break;
		case ROUTER:		if (hasRobot (b.robot))		robot (b.robot).router = null;	break;
		case MODULE:		if (moduleOf (b) != null)	robot (b.robot).modules.remove (b.index);	break;
		case ROBOT:			if (hasRobot (b.robot))		deploy.robots.remove (b.robot);	break;
		default:			break;
		}
	}
}
