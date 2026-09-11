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
 * (GLIN, LLIN, COO, NAV, ROB, ...). Only one robot is supported for now.
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

	static public final String[]	KIND_NAMES	= { "Global Linda", "Local Linda", "Router", "Module", "Virtual Robot", "Robot" };

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

	/** A block of the architecture: kind + property prefix. */
	static public class Block
	{
		public int		kind;
		public String	prefix;			// null for the robot container

		public Block (int kind, String prefix)		{ this.kind = kind; this.prefix = prefix; }

		public boolean equals (Object o)
		{
			if (!(o instanceof Block))		return false;
			Block	b = (Block) o;
			return (b.kind == kind) && ((prefix == null) ? (b.prefix == null) : prefix.equals (b.prefix));
		}
		public int hashCode ()		{ return kind * 31 + ((prefix == null) ? 0 : prefix.hashCode ()); }
		public String toString ()	{ return KIND_NAMES[kind] + ((prefix != null) ? " (" + prefix + ")" : ""); }
	}

	protected Properties		props;
	protected String			robotId;

	public ArchModel (Properties props, String robotId)
	{
		this.props		= props;
		this.robotId	= robotId;
	}

	static public final String	DEFAULT_ROBOT_NAME	= "Unnamed";

	public Properties	getProperties ()		{ return props; }

	/** Name of the robot: the NAME property when present, otherwise the default given to the constructor. */
	public String getRobotId ()
	{
		String	n = props.getProperty ("NAME");
		return ((n != null) && (n.trim ().length () > 0)) ? n.trim () : robotId;
	}

	public void setRobotName (String name)
	{
		if ((name == null) || (name.trim ().length () == 0))		props.remove ("NAME");
		else														props.setProperty ("NAME", name.trim ());
	}

	/* ------------------------------------------------------------------ */
	/* Queries                                                             */
	/* ------------------------------------------------------------------ */

	public boolean hasGlobalLinda ()		{ return hasPrefix ("GLIN"); }
	public boolean hasLocalLinda ()			{ return hasPrefix ("LLIN"); }
	public boolean hasRouter ()				{ return routerPrefix () != null; }
	public boolean hasVRobot ()				{ return vrobotPrefix () != null; }

	/** The robot exists when it has a local Linda, a router, modules or a virtual robot. */
	public boolean hasRobot ()				{ return hasLocalLinda () || hasRouter () || hasVRobot () || (modulePrefixes ().size () > 0); }

	public String routerPrefix ()
	{
		String	p = props.getProperty ("ROUTER");
		return ((p == null) || (p.trim ().length () == 0)) ? null : p.trim ();
	}

	public String vrobotPrefix ()
	{
		String	p = props.getProperty ("VROBOT");
		return ((p == null) || (p.trim ().length () == 0)) ? null : p.trim ();
	}

	public List<String> modulePrefixes ()
	{
		List<String>	l = new ArrayList<String> ();
		String			m = props.getProperty ("MODULES");
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

	protected boolean hasPrefix (String prefix)
	{
		for (String k : props.stringPropertyNames ())
			if (!isGlobalKey (k) && k.startsWith (prefix) && (k.length () > prefix.length ()) && Character.isUpperCase (k.charAt (prefix.length ())))
				return true;
		return false;
	}

	/** Blocks of the robot in display order: local Linda, router, modules, virtual robot. */
	public List<Block> robotBlocks ()
	{
		List<Block>	l = new ArrayList<Block> ();
		if (hasLocalLinda ())		l.add (new Block (LOCAL_LINDA, "LLIN"));
		if (hasRouter ())			l.add (new Block (ROUTER, routerPrefix ()));
		for (String p : modulePrefixes ())	l.add (new Block (MODULE, p));
		if (hasVRobot ())			l.add (new Block (VROBOT, vrobotPrefix ()));
		return l;
	}

	/** Display label of a block: its INFO property, or the kind name. */
	public String labelOf (Block b)
	{
		if (b.kind == ROBOT)			return "ROBOT " + getRobotId ();
		if (b.kind == GLOBAL_LINDA)		return "Multi-Robot Linda Space";
		if (b.kind == LOCAL_LINDA)		return "Local Linda Space";
		String	info = props.getProperty (b.prefix + "INFO");
		return ((info != null) && (info.trim ().length () > 0)) ? info.trim () : b.prefix;
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
		for (String k : props.stringPropertyNames ())
			if (!isGlobalKey (k) && k.startsWith (b.prefix) && (k.length () > b.prefix.length ()) && Character.isUpperCase (k.charAt (b.prefix.length ())))
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
		String			c = props.getProperty (b.prefix + "CONNECT");
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
		for (Block b : robotBlocks ())
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
		String	v = props.getProperty (b.prefix + key);
		return (v == null) ? "" : v;
	}

	public void set (Block b, String key, String value)
	{
		if ((value == null) || (value.trim ().length () == 0))		props.remove (b.prefix + key);
		else															props.setProperty (b.prefix + key, value.trim ());
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
		return new Block (GLOBAL_LINDA, "GLIN");
	}

	/** Creates the robot ("Unnamed" unless it already has a name): its local Linda space and the virtual robot section when missing. */
	public Block addRobot ()
	{
		if (props.getProperty ("NAME") == null)		props.setProperty ("NAME", DEFAULT_ROBOT_NAME);
		if (!hasLocalLinda ())		addLocalLinda ();
		if (!hasVRobot ())
		{
			props.setProperty ("VROBOT", "ROB");
			props.setProperty ("ROBINFO", "Virtual Robot");
			props.setProperty ("ROBMODE", "shared");
			props.setProperty ("ROBCLASS", "tc.vrobot.VirtualRobot");
			props.setProperty ("ROBPASSIVE", "false");
			props.setProperty ("ROBEXTIME", "100");
			props.setProperty ("ROBGFX", "true");
		}
		return new Block (ROBOT, null);
	}

	public Block addLocalLinda ()
	{
		if (!hasLocalLinda ())
		{
			props.setProperty ("LLINADDR", "localhost");
			props.setProperty ("LLINPORT", "3000");
			props.setProperty ("LLINCREATE", "true");
		}
		return new Block (LOCAL_LINDA, "LLIN");
	}

	public Block addRouter ()
	{
		String	p = routerPrefix ();
		if (p == null)
		{
			p = uniquePrefix ("COO");
			props.setProperty ("ROUTER", p);
			props.setProperty (p + "INFO", "Linda Router");
			props.setProperty (p + "MODE", "shared");
			props.setProperty (p + "CLASS", "tc.coord.LindaRouter");
			props.setProperty (p + "GMODE", "tcp");
			props.setProperty (p + "GFX", "false");
		}
		return new Block (ROUTER, p);
	}

	public Block addModule ()
	{
		String			p = numberedPrefix ("MOD");					// MOD1, MOD2, ...
		List<String>	mods = modulePrefixes ();
		mods.add (p);
		props.setProperty ("MODULES", join (mods));
		props.setProperty (p + "INFO", "Module");
		props.setProperty (p + "MODE", "shared");
		props.setProperty (p + "CLASS", "tc.runtime.thread.StdThread");
		props.setProperty (p + "PASSIVE", "true");
		props.setProperty (p + "GFX", "false");
		return new Block (MODULE, p);
	}

	/** Removes a block and all its properties (the robot container removes everything local). */
	public void remove (Block b)
	{
		switch (b.kind)
		{
		case GLOBAL_LINDA:	removePrefix ("GLIN");		break;
		case LOCAL_LINDA:	removePrefix ("LLIN");		break;
		case ROUTER:		removePrefix (b.prefix);	props.remove ("ROUTER");	break;
		case VROBOT:		removePrefix (b.prefix);	props.remove ("VROBOT");	break;
		case MODULE:
		{
			List<String>	mods = modulePrefixes ();
			mods.remove (b.prefix);
			if (mods.size () > 0)		props.setProperty ("MODULES", join (mods));
			else						props.remove ("MODULES");
			removePrefix (b.prefix);
			break;
		}
		case ROBOT:
			for (Block rb : robotBlocks ())		remove (rb);
			break;
		}
	}

	/** Renames the prefix of a router, module or virtual robot (all its properties follow). */
	public Block rename (Block b, String newPrefix)
	{
		newPrefix = newPrefix.trim ().toUpperCase ();
		if ((newPrefix.length () == 0) || newPrefix.equals (b.prefix) || (b.prefix == null))		return b;
		if (hasPrefix (newPrefix) || newPrefix.equals ("GLIN") || newPrefix.equals ("LLIN"))		return b;
		for (String k : new ArrayList<String> (props.stringPropertyNames ()))
			if (!isGlobalKey (k) && k.startsWith (b.prefix) && (k.length () > b.prefix.length ()) && Character.isUpperCase (k.charAt (b.prefix.length ())))
			{
				props.setProperty (newPrefix + k.substring (b.prefix.length ()), props.getProperty (k));
				props.remove (k);
			}
		if (b.kind == ROUTER)		props.setProperty ("ROUTER", newPrefix);
		if (b.kind == VROBOT)		props.setProperty ("VROBOT", newPrefix);
		if (b.kind == MODULE)
		{
			List<String>	mods = modulePrefixes ();
			int				i = mods.indexOf (b.prefix);
			if (i >= 0)		mods.set (i, newPrefix);
			props.setProperty ("MODULES", join (mods));
		}
		return new Block (b.kind, newPrefix);
	}

	protected void removePrefix (String prefix)
	{
		for (String k : new ArrayList<String> (props.stringPropertyNames ()))
			if (!isGlobalKey (k) && k.startsWith (prefix) && (k.length () > prefix.length ()) && Character.isUpperCase (k.charAt (prefix.length ())))
				props.remove (k);
	}

	protected String uniquePrefix (String base)
	{
		if (!hasPrefix (base) && !isReserved (base))		return base;
		for (int i = 1; ; i++)
			if (!hasPrefix (base + i) && !isReserved (base + i))		return base + i;
	}

	/** Three upper-case letters plus a number, the first one free: MOD1, MOD2, ... */
	protected String numberedPrefix (String base)
	{
		for (int i = 1; ; i++)
			if (!hasPrefix (base + i) && !isReserved (base + i))		return base + i;
	}

	protected boolean isReserved (String p)
	{
		return p.equals (routerPrefix ()) || p.equals (vrobotPrefix ()) || modulePrefixes ().contains (p);
	}

	static protected String join (List<String> l)
	{
		StringBuilder	sb = new StringBuilder ();
		for (String s : l)		sb.append ((sb.length () > 0) ? ", " : "").append (s);
		return sb.toString ();
	}
}
