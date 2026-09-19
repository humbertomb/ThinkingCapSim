/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.arch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tc.DeployArch;
import tc.DeployArch.Event;
import tc.DeployArch.Linda;
import tc.DeployArch.Module;
import tc.DeployArch.Robot;

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
	static public final int		P_CLASS		= 4;		// chosen among the classes of the development deriving from one

	/** The class the modules and the robots of an architecture are run as threads of. */
	static public final String	THREAD_BASE	= "tc.runtime.thread.StdThread";
	/** The class the robot of an architecture is. */
	static public final String	VROBOT_BASE	= "tc.vrobot.VirtualRobot";
	/** The class a router of the coordination layer is. */
	static public final String	ROUTER_BASE	= "tc.coord.LindaRouter";
	/** The class a monitor of a robot is: a thread of the runtime, but not a module of the architecture. */
	static public final String	MONITOR_BASE	= "tc.modules.Monitor";
	/** What a module is not, though it is a thread of the runtime as they are. */
	static public final String[]	MODULE_NOT	= { VROBOT_BASE, MONITOR_BASE };
	/**
	 * What a robot of a deployment is not, though it is a virtual robot: the robot
	 * of the simulator itself, which stands for a real robot and is only used when
	 * running on one.
	 */
	static public final String		SIM_ROBOT	= "tcapps.tcsimulator.simulator.objects.SimRobot";
	static public final String[]	VROBOT_NOT	= { SIM_ROBOT };

	/**
	 * The kinds of module an architecture is made of, which are the classes of
	 * <code>tc.modules</code> they derive from: the kind a module is created as is
	 * kept with it (TYPE) and is what names it and says which classes it may be.
	 */
	static public final String[]	MODULE_TYPES	= { "Controller", "Navigation", "Perception", "Planner" };

	/** The kind of module that runs a program of its own. */
	static public final String		CONTROLLER		= "Controller";

	/**
	 * The program a controller runs (PRG). Any file will do: whether it is one the
	 * controller can read is for the controller to say when it loads it.
	 */
	static public final Property	PRG_PROP		= new Property ("PRG", "Program", "./conf/programs", "Programs");

	/**
	 * Whether a controller starts running of its own accord (AUTO), instead of
	 * waiting to be told to start. It is what the module is given in its
	 * configuration, and nothing is taken to be no.
	 */
	static public final Property	AUTO_PROP		= new Property ("AUTO", "Autostart", P_BOOLEAN);

	/** The class the modules of a kind derive from. */
	static public String moduleTypeBase (String type)
	{
		if (type == null)						return null;
		type	= type.trim ();
		for (String t : MODULE_TYPES)			if (t.equalsIgnoreCase (type))		return "tc.modules." + t;
		return null;
	}

	/** Execution modes of a module (ThreadDesc.parse_mode) and protocols towards the global Linda (RouterDesc GMODE). */
	static public final String[]	MODES		= { "shared", "udp", "tcp" };
	static public final String[]	PROTOCOLS	= { "tcp", "udp", "shared" };

	/** How a property (suffix) of a block is shown and edited. */
	static public class Property
	{
		public String	key;			// suffix of the ADF key (INFO, CLASS, ...)
		public String	label;			// name shown to the user
		public int		type;			// P_TEXT, P_BOOLEAN, P_CHOICE, P_FILE, P_CLASS
		public String[]	choices;		// P_CHOICE values
		public String	classBase;		// P_CLASS: the class the ones offered derive from
		public String[]	classNot;		// P_CLASS: and what they must not derive from
		public boolean	classBlank;		// P_CLASS: whether it may be left with no class at all
		public String	fileDir;		// P_FILE: default directory
		public String	fileDesc;		// P_FILE: filter description
		public String[]	fileExts;		// P_FILE: filter extensions

		public Property (String key, String label)								{ this (key, label, P_TEXT); }
		public Property (String key, String label, int type)						{ this.key = key; this.label = label; this.type = type; }
		public Property (String key, String label, String[] choices)				{ this (key, label, P_CHOICE); this.choices = choices; }
		public Property (String key, String label, String dir, String desc, String... exts)	{ this (key, label, P_FILE); fileDir = dir; fileDesc = desc; fileExts = exts; }
		/** A property naming a class: what is offered are the ones of the development deriving from <code>base</code>. */
		static public Property ofClass (String key, String label, String base)		{ return ofClass (key, label, base, (String[]) null); }

		static public Property ofClass (String key, String label, String base, String... not)
		{
			Property	p = new Property (key, label, P_CLASS);
			p.classBase	= base;
			p.classNot	= not;
			return p;
		}

		/** The same, which may also be left blank: the first thing offered is no class at all. */
		public Property orNone ()						{ classBlank = true; return this; }
		public String toString ()	{ return label; }
	}

	/** Names of the start points of the world (START_1, ...), offered for the robots; set by the editor. */
	protected List<String>		startNames	= new ArrayList<String> ();

	public void setStartNames (List<String> names)	{ startNames = new ArrayList<String> (names); }

	/** Robot block: the start point of the world it departs from ("" = the i-th one, by order). */
	protected Property[] robotProps ()
	{
		String[]	choices = new String[startNames.size () + 1];
		choices[0]	= "";
		for (int i = 0; i < startNames.size (); i++)		choices[i + 1] = startNames.get (i);
		return new Property[] { new Property ("START", "Start Position", choices) };
	}
	static public final Property[]	LINDA_PROPS	=
	{
		new Property ("ADDR",	"Address"),
		new Property ("PORT",	"Port"),
		new Property ("CREATE",	"Instantiate",	P_BOOLEAN),
	};
	static public final Property[]	ROUTER_PROPS	=
	{
		new Property ("INFO",	"Name"),
		Property.ofClass ("CLASS", "Class", ROUTER_BASE),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("GMODE",	"Protocol",		PROTOCOLS),
		new Property ("GFX",	"Graphics",	P_BOOLEAN),
	};
	static public final Property[]	MODULE_PROPS	=
	{
		new Property ("INFO",	"Name"),
		Property.ofClass ("CLASS", "Class", THREAD_BASE, MODULE_NOT).orNone (),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("PASSIVE","Passive",		P_BOOLEAN),
		new Property ("QUEUED",	"Queued",		P_BOOLEAN),
		new Property ("POLLED",	"Polled",		P_BOOLEAN),
		new Property ("EXTIME",	"Exec. time (ms)"),
		new Property ("GFX",	"Graphics",	P_BOOLEAN),
	};
	static public final Property[]	VROBOT_PROPS	=
	{
		new Property ("INFO",	"Name"),
		// the description comes right after the name: it is what says which robot this is
		new Property ("DESC",	"Robot Definition",	"./conf/robots",	"Robot descriptions (*.robot)",	"robot"),
		Property.ofClass ("CLASS", "Class", VROBOT_BASE, VROBOT_NOT).orNone (),
		new Property ("MODE",	"Mode",			MODES),
		new Property ("PASSIVE","Passive",		P_BOOLEAN),
		new Property ("EXTIME",	"Exec. time (ms)"),
		new Property ("GFX",	"Graphics",	P_BOOLEAN),
	};

	/** Suffixes that exist in the ADF but are not shown in the editor, per kind. */
	static public final String[]	LINDA_HIDDEN	= { "CLASS" };
	static public final String[]	ROUTER_HIDDEN	= { "PRI", "CONNECT" };
	static public final String[]	MODULE_HIDDEN	= { "PRI", "CONNECT", "TYPE" };		// CONNECT is edited in the events table, TYPE is what the module was created as
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
		case ROBOT:			return java.util.Arrays.asList (robotProps ());
		case GLOBAL_LINDA:
		case LOCAL_LINDA:	return java.util.Arrays.asList (LINDA_PROPS);
		case ROUTER:		std = ROUTER_PROPS;	hidden = ROUTER_HIDDEN;	break;
		case MODULE:		std = MODULE_PROPS;	hidden = MODULE_HIDDEN;	break;
		case VROBOT:		std = VROBOT_PROPS;	hidden = VROBOT_HIDDEN;	break;
		default:			return new ArrayList<Property> ();
		}
		List<Property>	props = new ArrayList<Property> ();
		List<String>	known = new ArrayList<String> ();
		for (Property p : std)
		{
			props.add (p);
			known.add (p.key);
			// only a controller runs a program, and it is of a piece with its class
			if ((b.kind == MODULE) && p.key.equals ("CLASS") && CONTROLLER.equalsIgnoreCase (get (b, "TYPE")))
			{
				props.add (PRG_PROP);
				known.add (PRG_PROP.key);
				// and whether it starts on its own, which goes with the program it runs
				props.add (AUTO_PROP);
				known.add (AUTO_PROP.key);
			}
		}
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
			String	st = hasRobot (b.robot) ? robot (b.robot).start : null;
			return (st == null) ? "" : st;
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
		// a controller that says nothing of starting on its own does not, which is
		// what a description written before it was asked for says
		if ((v == null) && key.equals (AUTO_PROP.key) && CONTROLLER.equalsIgnoreCase (m.get ("TYPE")))
			return "false";
		return (v == null) ? "" : v;
	}

	public void set (Block b, String key, String value)
	{
		String	v = (value == null) ? "" : value.trim ();
		if ((b.kind == ROBOT) && key.equals ("START"))
		{
			if (!hasRobot (b.robot))		return;
			robot (b.robot).start = (v.length () == 0) ? null : v;
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

	/**
	 * True when the symbols of a block are part of the drawing of the
	 * architecture: the modules and the virtual robot, which say in the
	 * deployment what they are given, and the router, which says it in its own
	 * code.
	 */
	public boolean hasSymbols (Block b)
	{
		return (b != null) && ((b.kind == MODULE) || (b.kind == VROBOT) || (b.kind == ROUTER));
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

	/**
	 * Symbols a class names but does not produce: it builds the tuple to read
	 * them, not to write them, and read off a class file there is no telling the
	 * two apart. The few there are said here, by the class they are found in.
	 */
	static private final String[][]	NOT_PRODUCED	= { { "tc.modules.Planner", "LPS" } };		// the planner polls the LPS

	/**
	 * Symbols a router writes without being registered for them: read off a class
	 * file the traffic of a router is one lot of names, and what it makes up
	 * itself is not told apart from what it passes on. The few there are said
	 * here.
	 */
	static private final String[]	ROUTER_WRITES	= { "MONITOR" };			// the router sums up the LPS for the global space

	/**
	 * The class a block runs, as it is read: what a description names when the
	 * development builds it, and what stands for it when it does not -- the robot
	 * of the simulator, and the plain router of the coordination layer, which is
	 * the case of the classes left out of the development for the hardware they
	 * need.
	 */
	private String classOf (Block b)
	{
		String		cls = get (b, "CLASS");

		if (tcapps.tceditor.DriverClasses.exists (cls))		return cls;
		if (b.kind == VROBOT)								return SIM_ROBOT;
		if (b.kind == ROUTER)								return ROUTER_BASE;
		return cls;
	}

	/** The symbols the class of a block, or one of its ancestors, names. */
	private java.util.Map<String, List<String>> named (Block b)
	{
		return tcapps.tceditor.DriverClasses.symbolsOf (classOf (b), symbols ());
	}

	/**
	 * Symbols a block is given: the ones its events register, and, for a router,
	 * the ones its code registers for -- which is its whole traffic but what it
	 * writes of its own accord.
	 */
	public List<String> inputs (Block b)
	{
		List<String>	in = new ArrayList<String> ();

		if ((b == null) || !hasSymbols (b))					return in;
		if (b.kind == ROUTER)
		{
			for (List<String> syms : named (b).values ())
				for (String sym : syms)
					if (!in.contains (sym) && !writesOnly (sym))		in.add (sym);
			return in;
		}
		for (String[] e : events (b))
		{
			String	sym = (e[0] != null) ? e[0].trim () : "";
			if ((sym.length () > 0) && !in.contains (sym))			in.add (sym);
		}
		return in;
	}

	static private boolean writesOnly (String sym)
	{
		for (String s : ROUTER_WRITES)
			if (s.equals (sym))		return true;
		return false;
	}

	/**
	 * Symbols a block produces: the ones the class it runs, or one of its
	 * ancestors, names -- which is how a module says what it writes, since a
	 * deployment only says what it is given -- less the ones it is given and less
	 * the ones a class is known to read rather than write.
	 *
	 * A router is the exception to the first of those: it routes what it is
	 * registered for, so what comes into it is what goes out of it, and taking
	 * the one from the other would leave it writing nothing.
	 */
	public List<String> produces (Block b)
	{
		List<String>				out = new ArrayList<String> ();
		List<String>				in;

		if ((b == null) || !hasSymbols (b))					return out;
		in		= inputs (b);
		for (java.util.Map.Entry<String, List<String>> e : named (b).entrySet ())
			for (String sym : e.getValue ())
			{
				boolean		reads = false;
				for (String[] no : NOT_PRODUCED)
					if (no[0].equals (e.getKey ()) && no[1].equals (sym))		reads = true;
				if (reads || out.contains (sym))								continue;
				if ((b.kind != ROUTER) && in.contains (sym))					continue;
				out.add (sym);
			}
		return out;
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

	/** Adds a robot named "Unnamed" (or "Unnamed2", ...) with its local Linda space and virtual robot, plus a Linda router when the deployment has a global Linda space. */
	public Block addRobot ()
	{
		String	name = DeployArch.DEFAULT_ROBOT;
		for (int i = 2; robotNames ().contains (name); i++)		name = DeployArch.DEFAULT_ROBOT + i;
		Robot	r = new Robot (name);
		if (deploy.globalLinda != null)		r.router = DeployArch.newRouter ();		// only with a global space to route to
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

	/**
	 * Adds a module of a kind ({@link #MODULE_TYPES}), named after it: "Perception"
	 * and, when the robot already has one, "Perception 2".
	 */
	public Block addModule (int r, String type)
	{
		if (!hasRobot (r))				return null;
		if (moduleTypeBase (type) == null)		return addModule (r);
		type	= type.trim ();
		robot (r).modules.add (DeployArch.newModule (uniqueName (r, type), type));
		return new Block (MODULE, r, robot (r).modules.size () - 1);
	}

	protected String uniqueModuleName (int r, String base)
	{
		List<String>	names = new ArrayList<String> ();
		for (Block b : robotBlocks (r))		names.add (labelOf (b));
		for (int i = 1; ; i++)
			if (!names.contains (base + " " + i))		return base + " " + i;
	}

	/** The name itself when the robot has nothing by it, and the name with a numeral when it has. */
	protected String uniqueName (int r, String base)
	{
		List<String>	names = new ArrayList<String> ();

		for (Block b : robotBlocks (r))		names.add (labelOf (b));
		if (!names.contains (base))			return base;
		for (int i = 2; ; i++)
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
