/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.arch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Structural view of an architecture definition (the Properties of an ADF)
 * as blocks: the global Linda space, and one robot made of its local Linda
 * space, an optional router, the modules of MODULES and the virtual robot
 * (VROBOT). Every block is identified by the prefix of its properties
 * (GLIN, LLIN, COO, NAV, ROB, ...).
 * <p>
 * Several robots: the ADF only describes one robot, so the first one (index
 * 0) uses the plain keys and every additional robot <i>i</i> keeps the same
 * keys under the namespace <code>R<i>i</i>.</code> (R1.LLINADDR, R1.MODULES,
 * R1.NAME, ...). The executor ignores those keys; they are a stop-gap until
 * the format migrates to JSON. Each robot has a NAME property.
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

	/** Property suffixes edited for each kind (existing ones with the prefix are shown too). */
	static public final String[]	LINDA_KEYS	= { "ADDR", "PORT", "CREATE", "CLASS" };
	static public final String[]	ROUTER_KEYS	= { "INFO", "CLASS", "MODE", "GMODE", "GFX", "PRI", "CONNECT" };
	static public final String[]	MODULE_KEYS	= { "INFO", "CLASS", "MODE", "PASSIVE", "QUEUED", "POLLED", "EXTIME", "PRI", "GFX", "CONNECT" };
	static public final String[]	VROBOT_KEYS	= { "INFO", "CLASS", "MODE", "PASSIVE", "EXTIME", "PRI", "GFX", "DESC", "CUST", "WORLD", "TOPOL", "APW", "RADDR", "RPORT", "LPORT", "CONNECT" };

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

	/** A block of the architecture: kind + property prefix + robot (-1 for the global Linda). */
	static public class Block
	{
		public int		kind;
		public String	prefix;			// null for the robot container
		public int		robot;			// index of the robot the block belongs to (-1: global)

		public Block (int kind, String prefix, int robot)	{ this.kind = kind; this.prefix = prefix; this.robot = robot; }
		public Block (int kind, String prefix)				{ this (kind, prefix, (kind == GLOBAL_LINDA) ? -1 : 0); }

		public boolean equals (Object o)
		{
			if (!(o instanceof Block))		return false;
			Block	b = (Block) o;
			return (b.kind == kind) && (b.robot == robot) && ((prefix == null) ? (b.prefix == null) : prefix.equals (b.prefix));
		}
		public int hashCode ()		{ return (kind * 31 + robot) * 31 + ((prefix == null) ? 0 : prefix.hashCode ()); }
		public String toString ()	{ return KIND_NAMES[kind] + ((prefix != null) ? " (" + prefix + ")" : "") + ((robot > 0) ? " R" + robot : ""); }
	}

	protected Properties		props;
	protected String			robotId;			// default name of robot 0 when it has no NAME

	public ArchModel (Properties props, String robotId)
	{
		this.props		= props;
		this.robotId	= robotId;
	}

	static public final String	DEFAULT_ROBOT_NAME	= "Unnamed";

	public Properties	getProperties ()		{ return props; }

	/* ------------------------------------------------------------------ */
	/* Namespaces                                                          */
	/* ------------------------------------------------------------------ */

	/** Key namespace of a robot: "" for the first one, "R<i>." for the others. */
	static public String ns (int robot)			{ return (robot <= 0) ? "" : "R" + robot + "."; }

	/** Index of the namespace of a key ("R2.LLINADDR" gives 2), 0 for plain keys. */
	static protected int robotOf (String key)
	{
		if ((key.length () < 3) || (key.charAt (0) != 'R'))		return 0;
		int	dot = key.indexOf ('.');
		if (dot < 2)		return 0;
		for (int i = 1; i < dot; i++)		if (!Character.isDigit (key.charAt (i)))		return 0;
		return Integer.parseInt (key.substring (1, dot));
	}

	/** Keys of a robot without their namespace (robot 0: the plain keys). */
	protected List<String> localKeys (int robot)
	{
		List<String>	l = new ArrayList<String> ();
		String			ns = ns (robot);
		for (String k : props.stringPropertyNames ())
			if (robotOf (k) == robot)		l.add (k.substring (ns.length ()));
		return l;
	}

	protected String	getp (int robot, String key)				{ return props.getProperty (ns (robot) + key); }
	protected void		setp (int robot, String key, String v)		{ props.setProperty (ns (robot) + key, v); }
	protected void		remp (int robot, String key)				{ props.remove (ns (robot) + key); }

	/* ------------------------------------------------------------------ */
	/* Robots                                                              */
	/* ------------------------------------------------------------------ */

	/** Indices of the existing robots, in order. */
	public List<Integer> robots ()
	{
		java.util.TreeSet<Integer>	set = new java.util.TreeSet<Integer> ();
		for (String k : props.stringPropertyNames ())
		{
			int	r = robotOf (k);
			if (r > 0)		set.add (r);
		}
		List<Integer>	l = new ArrayList<Integer> ();
		if (hasRobot (0))		l.add (0);
		l.addAll (set);
		return l;
	}

	/** Name of a robot: its NAME property, or the default name given to the constructor (robot 0). */
	public String getRobotId (int robot)
	{
		String	n = getp (robot, "NAME");
		if ((n != null) && (n.trim ().length () > 0))		return n.trim ();
		return (robot == 0) ? robotId : "Robot " + robot;
	}

	/** Name of the first robot (the one the executor runs). */
	public String getRobotId ()						{ return getRobotId (0); }

	public void setRobotName (int robot, String name)
	{
		if ((name == null) || (name.trim ().length () == 0))		remp (robot, "NAME");
		else														setp (robot, "NAME", name.trim ());
	}

	/* ------------------------------------------------------------------ */
	/* Queries                                                             */
	/* ------------------------------------------------------------------ */

	public boolean hasGlobalLinda ()				{ return hasPrefix (0, "GLIN"); }
	public boolean hasLocalLinda (int robot)		{ return hasPrefix (robot, "LLIN"); }
	public boolean hasRouter (int robot)			{ return routerPrefix (robot) != null; }
	public boolean hasVRobot (int robot)			{ return vrobotPrefix (robot) != null; }

	/** A robot exists when it has a name, a local Linda, a router, modules or a virtual robot. */
	public boolean hasRobot (int robot)
	{
		return (getp (robot, "NAME") != null) || hasLocalLinda (robot) || hasRouter (robot) || hasVRobot (robot) || (modulePrefixes (robot).size () > 0);
	}

	/** True when there is at least one robot. */
	public boolean hasRobot ()						{ return robots ().size () > 0; }

	public String routerPrefix (int robot)
	{
		String	p = getp (robot, "ROUTER");
		return ((p == null) || (p.trim ().length () == 0)) ? null : p.trim ();
	}

	public String vrobotPrefix (int robot)
	{
		String	p = getp (robot, "VROBOT");
		return ((p == null) || (p.trim ().length () == 0)) ? null : p.trim ();
	}

	public List<String> modulePrefixes (int robot)
	{
		List<String>	l = new ArrayList<String> ();
		String			m = getp (robot, "MODULES");
		if (m != null)
		{
			StringTokenizer	st = new StringTokenizer (m, ", \t");
			while (st.hasMoreTokens ())		l.add (st.nextToken ());
		}
		return l;
	}

	/** Top-level keys of the ADF that are not "prefix + suffix" of a block. */
	static public final String[]	GLOBAL_KEYS	= { "MODULES", "ROUTER", "VROBOT", "ROBNAME", "NAME" };

	static protected boolean isGlobalKey (String k)
	{
		for (String g : GLOBAL_KEYS)		if (g.equals (k))		return true;
		return false;
	}

	static protected boolean startsWithPrefix (String k, String prefix)
	{
		return !isGlobalKey (k) && k.startsWith (prefix) && (k.length () > prefix.length ()) && Character.isUpperCase (k.charAt (prefix.length ()));
	}

	protected boolean hasPrefix (int robot, String prefix)
	{
		for (String k : localKeys (robot))
			if (startsWithPrefix (k, prefix))		return true;
		return false;
	}

	/** Blocks of a robot in display order: local Linda, router, modules, virtual robot. */
	public List<Block> robotBlocks (int robot)
	{
		List<Block>	l = new ArrayList<Block> ();
		if (hasLocalLinda (robot))		l.add (new Block (LOCAL_LINDA, "LLIN", robot));
		if (hasRouter (robot))			l.add (new Block (ROUTER, routerPrefix (robot), robot));
		for (String p : modulePrefixes (robot))	l.add (new Block (MODULE, p, robot));
		if (hasVRobot (robot))			l.add (new Block (VROBOT, vrobotPrefix (robot), robot));
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

	/** Display label of a block: its INFO property, or the kind name. */
	public String labelOf (Block b)
	{
		if (b.kind == ROBOT)			return "ROBOT " + getRobotId (b.robot);
		if (b.kind == GLOBAL_LINDA)		return "Global Linda Space";
		if (b.kind == LOCAL_LINDA)		return "Local Linda Space";
		String	info = getp (b.robot, b.prefix + "INFO");
		return ((info != null) && (info.trim ().length () > 0)) ? info.trim () : b.prefix;
	}

	/** True when the block can be deleted: everything but the local Linda space and the virtual robot, which every robot must have. */
	public boolean isRemovable (Block b)
	{
		return (b.kind != LOCAL_LINDA) && (b.kind != VROBOT);
	}

	/** True when the name of the block can be edited (robot name or INFO of a module). */
	public boolean isRenameable (Block b)
	{
		return (b.kind == ROBOT) || (b.kind == ROUTER) || (b.kind == MODULE) || (b.kind == VROBOT);
	}

	/** Editable name of a block: the robot name, or the INFO of a module. */
	public String nameOf (Block b)
	{
		return (b.kind == ROBOT) ? getRobotId (b.robot) : labelOf (b);
	}

	public void setName (Block b, String name)
	{
		if (b.kind == ROBOT)		setRobotName (b.robot, name);
		else						set (b, "INFO", name);
	}

	/** Property suffixes shown for a block: the standard ones of its kind plus any other existing with its prefix. */
	public List<String> keysOf (Block b)
	{
		String[]		std;
		switch (b.kind)
		{
		case GLOBAL_LINDA:
		case LOCAL_LINDA:	std = LINDA_KEYS;	break;
		case ROUTER:		std = ROUTER_KEYS;	break;
		case MODULE:		std = MODULE_KEYS;	break;
		case VROBOT:		std = VROBOT_KEYS;	break;
		default:			return new ArrayList<String> ();
		}
		List<String>	keys = new ArrayList<String> ();
		for (String k : std)		keys.add (k);
		List<String>	extra = new ArrayList<String> ();
		for (String k : localKeys (Math.max (0, b.robot)))
			if (startsWithPrefix (k, b.prefix))
			{
				String	suffix = k.substring (b.prefix.length ());
				if (!keys.contains (suffix))		extra.add (suffix);
			}
		Collections.sort (extra);
		keys.addAll (extra);
		return keys;
	}

	/**
	 * Properties shown in the editor for a block: the standard ones of its
	 * kind (with their labels and editors), then any other existing with the
	 * prefix as plain text, minus the hidden ones.
	 */
	public List<Property> propertiesOf (Block b)
	{
		Property[]		std;
		String[]		hidden;
		switch (b.kind)
		{
		case GLOBAL_LINDA:
		case LOCAL_LINDA:	std = LINDA_PROPS;	hidden = LINDA_HIDDEN;	break;
		case ROUTER:		std = ROUTER_PROPS;	hidden = ROUTER_HIDDEN;	break;
		case MODULE:		std = MODULE_PROPS;	hidden = MODULE_HIDDEN;	break;
		case VROBOT:		std = VROBOT_PROPS;	hidden = VROBOT_HIDDEN;	break;
		default:			return new ArrayList<Property> ();
		}
		List<Property>	props = new ArrayList<Property> ();
		List<String>	known = new ArrayList<String> ();
		for (Property p : std)		{ props.add (p); known.add (p.key); }
		for (String h : hidden)		known.add (h);
		for (String k : keysOf (b))
			if (!known.contains (k))		props.add (new Property (k, k));
		return props;
	}

	/** True when the block registers events (CONNECT property): modules and the virtual robot. */
	public boolean hasEvents (Block b)
	{
		return (b != null) && ((b.kind == MODULE) || (b.kind == VROBOT));
	}

	/**
	 * Events of a module, from its CONNECT property: entries separated by
	 * commas, each one "symbol class method" (the format of EventDesc).
	 * Incomplete entries are padded with empty strings.
	 */
	public List<String[]> events (Block b)
	{
		List<String[]>	l = new ArrayList<String[]> ();
		String			c = getp (b.robot, b.prefix + "CONNECT");
		if (c == null)		return l;
		StringTokenizer	st = new StringTokenizer (c, ",");
		while (st.hasMoreTokens ())
		{
			StringTokenizer	tk = new StringTokenizer (st.nextToken (), " \t");
			if (!tk.hasMoreTokens ())		continue;
			String[]	e = { "", "", "" };
			for (int i = 0; (i < 3) && tk.hasMoreTokens (); i++)		e[i] = tk.nextToken ();
			l.add (e);
		}
		return l;
	}

	/**
	 * Symbols an event can register: the keys defined in {@link tc.shared.linda.Tuple}
	 * (public static String constants) plus any other symbol already used in
	 * the CONNECT properties of this architecture (COORD, ZONE, ...), sorted.
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

	/** Writes the events back to the CONNECT property (removed when empty). */
	public void setEvents (Block b, List<String[]> events)
	{
		StringBuilder	sb = new StringBuilder ();
		for (String[] e : events)
		{
			if ((e[0].trim ().length () == 0) && (e[1].trim ().length () == 0) && (e[2].trim ().length () == 0))		continue;
			if (sb.length () > 0)		sb.append (", ");
			sb.append (e[0].trim ()).append ('\t').append (e[1].trim ()).append ('\t').append (e[2].trim ());
		}
		set (b, "CONNECT", sb.toString ());
	}

	public String get (Block b, String key)
	{
		String	v = getp (Math.max (0, b.robot), b.prefix + key);
		return (v == null) ? "" : v;
	}

	public void set (Block b, String key, String value)
	{
		int	r = Math.max (0, b.robot);
		if ((value == null) || (value.trim ().length () == 0))		remp (r, b.prefix + key);
		else															setp (r, b.prefix + key, value.trim ());
	}

	/* ------------------------------------------------------------------ */
	/* Edition                                                             */
	/* ------------------------------------------------------------------ */

	public Block addGlobalLinda ()
	{
		if (!hasGlobalLinda ())
		{
			props.setProperty ("GLINADDR", "localhost");
			props.setProperty ("GLINPORT", "5500");
			props.setProperty ("GLINCREATE", "false");
		}
		return new Block (GLOBAL_LINDA, "GLIN", -1);
	}

	/**
	 * Adds a robot (index 0 when there is none, otherwise the first free
	 * namespace) named "Unnamed" (or "Unnamed2", ...), with its local Linda
	 * space and virtual robot.
	 */
	public Block addRobot ()
	{
		int		r = 0;
		if (hasRobot (0))
			for (r = 1; robots ().contains (r); r++);
		return addRobot (r);
	}

	/** Completes a robot: name, local Linda space and virtual robot section when missing. */
	public Block addRobot (int r)
	{
		if (getp (r, "NAME") == null)
		{
			String	name = DEFAULT_ROBOT_NAME;
			for (int i = 2; robotNames ().contains (name); i++)		name = DEFAULT_ROBOT_NAME + i;
			setp (r, "NAME", name);
		}
		if (!hasLocalLinda (r))		addLocalLinda (r);
		if (!hasVRobot (r))
		{
			setp (r, "VROBOT", "ROB");
			setp (r, "ROBINFO", "Virtual Robot");
			setp (r, "ROBMODE", "shared");
			setp (r, "ROBCLASS", "tc.vrobot.VirtualRobot");
			setp (r, "ROBPASSIVE", "false");
			setp (r, "ROBEXTIME", "100");
			setp (r, "ROBGFX", "true");
		}
		return new Block (ROBOT, null, r);
	}

	/** "Module 1", "Module 2", ...: the first name not used by another block of the robot. */
	protected String uniqueModuleName (int r, String base)
	{
		List<String>	names = new ArrayList<String> ();
		for (Block b : robotBlocks (r))		names.add (labelOf (b));
		for (int i = 1; ; i++)
			if (!names.contains (base + " " + i))		return base + " " + i;
	}

	protected List<String> robotNames ()
	{
		List<String>	l = new ArrayList<String> ();
		for (int r : robots ())		l.add (getRobotId (r));
		return l;
	}

	public Block addLocalLinda (int r)
	{
		if (!hasLocalLinda (r))
		{
			setp (r, "LLINADDR", "localhost");
			setp (r, "LLINPORT", "3000");
			setp (r, "LLINCREATE", "true");
		}
		return new Block (LOCAL_LINDA, "LLIN", r);
	}

	public Block addRouter (int r)
	{
		String	p = routerPrefix (r);
		if (p == null)
		{
			p = uniquePrefix (r, "COO");
			setp (r, "ROUTER", p);
			setp (r, p + "INFO", "Linda Router");
			setp (r, p + "MODE", "shared");
			setp (r, p + "CLASS", "tc.coord.LindaRouter");
			setp (r, p + "GMODE", "tcp");
			setp (r, p + "GFX", "false");
		}
		return new Block (ROUTER, p, r);
	}

	public Block addModule (int r)
	{
		String			p = numberedPrefix (r, "MOD");					// MOD1, MOD2, ...
		List<String>	mods = modulePrefixes (r);
		mods.add (p);
		setp (r, "MODULES", join (mods));
		setp (r, p + "INFO", uniqueModuleName (r, "Module"));
		setp (r, p + "MODE", "shared");
		setp (r, p + "CLASS", "tc.runtime.thread.StdThread");
		setp (r, p + "PASSIVE", "true");
		setp (r, p + "GFX", "false");
		return new Block (MODULE, p, r);
	}

	/** Removes a block and all its properties (the robot container removes everything of that robot). */
	public void remove (Block b)
	{
		int	r = Math.max (0, b.robot);
		switch (b.kind)
		{
		case GLOBAL_LINDA:	removePrefix (0, "GLIN");		break;
		case LOCAL_LINDA:	removePrefix (r, "LLIN");		break;
		case ROUTER:		removePrefix (r, b.prefix);	remp (r, "ROUTER");	break;
		case VROBOT:		removePrefix (r, b.prefix);	remp (r, "VROBOT");	break;
		case MODULE:
		{
			List<String>	mods = modulePrefixes (r);
			mods.remove (b.prefix);
			if (mods.size () > 0)		setp (r, "MODULES", join (mods));
			else						remp (r, "MODULES");
			removePrefix (r, b.prefix);
			break;
		}
		case ROBOT:
			for (Block rb : robotBlocks (r))		remove (rb);
			remp (r, "NAME");
			remp (r, "MODULES");
			break;
		}
	}

	/** Renames the prefix of a router, module or virtual robot (all its properties follow). */
	public Block rename (Block b, String newPrefix)
	{
		int	r = Math.max (0, b.robot);
		newPrefix = newPrefix.trim ().toUpperCase ();
		if ((newPrefix.length () == 0) || newPrefix.equals (b.prefix) || (b.prefix == null))		return b;
		if (hasPrefix (r, newPrefix) || newPrefix.equals ("GLIN") || newPrefix.equals ("LLIN"))		return b;
		for (String k : localKeys (r))
			if (startsWithPrefix (k, b.prefix))
			{
				setp (r, newPrefix + k.substring (b.prefix.length ()), getp (r, k));
				remp (r, k);
			}
		if (b.kind == ROUTER)		setp (r, "ROUTER", newPrefix);
		if (b.kind == VROBOT)		setp (r, "VROBOT", newPrefix);
		if (b.kind == MODULE)
		{
			List<String>	mods = modulePrefixes (r);
			int				i = mods.indexOf (b.prefix);
			if (i >= 0)		mods.set (i, newPrefix);
			setp (r, "MODULES", join (mods));
		}
		return new Block (b.kind, newPrefix, r);
	}

	protected void removePrefix (int r, String prefix)
	{
		for (String k : localKeys (r))
			if (startsWithPrefix (k, prefix))		remp (r, k);
	}

	protected String uniquePrefix (int r, String base)
	{
		if (!hasPrefix (r, base) && !isReserved (r, base))		return base;
		for (int i = 1; ; i++)
			if (!hasPrefix (r, base + i) && !isReserved (r, base + i))		return base + i;
	}

	/** Three upper-case letters plus a number, the first one free: MOD1, MOD2, ... */
	protected String numberedPrefix (int r, String base)
	{
		for (int i = 1; ; i++)
			if (!hasPrefix (r, base + i) && !isReserved (r, base + i))		return base + i;
	}

	protected boolean isReserved (int r, String p)
	{
		return p.equals (routerPrefix (r)) || p.equals (vrobotPrefix (r)) || modulePrefixes (r).contains (p);
	}

	static protected String join (List<String> l)
	{
		StringBuilder	sb = new StringBuilder ();
		for (String s : l)		sb.append ((sb.length () > 0) ? ", " : "").append (s);
		return sb.toString ();
	}
}
