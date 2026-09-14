/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Deployment architecture: everything the Deployment Architecture editor
 * works with, persisted as JSON in a <code>.deploy</code> file. An optional
 * global Linda space and a list of robots, each one with its local Linda
 * space, an optional Linda router, its modules and its virtual robot. Module
 * properties keep the names the runtime reads (CLASS, MODE, PASSIVE, ...,
 * see tc.runtime.thread.ThreadDesc), so {@link ExecArch} executes a robot
 * straight from this model. A legacy
 * <code>.arch</code> file can still be imported with
 * {@link #fromProperties(Properties, String)}.
 */
public class DeployArch
{
	static public final String		EXTENSION		= "deploy";
	static public final String		DEFAULT_ROBOT	= "Unnamed";

	/** A Linda space (global or local to a robot). */
	static public class Linda
	{
		public String	address		= "localhost";
		public int		port		= 3000;
		public boolean	instantiate	= true;			// create the server (CREATE) or connect to an existing one

		public Linda ()										{ }
		public Linda (String address, int port, boolean instantiate)	{ this.address = address; this.port = port; this.instantiate = instantiate; }
		public Linda copy ()								{ return new Linda (address, port, instantiate); }
	}

	/** An event a module registers in the Linda space: symbol, item class and callback method. */
	static public class Event
	{
		public String	symbol		= "";
		public String	itemClass	= "";
		public String	method		= "";

		public Event ()										{ }
		public Event (String symbol, String itemClass, String method)	{ this.symbol = symbol; this.itemClass = itemClass; this.method = method; }
		public Event copy ()								{ return new Event (symbol, itemClass, method); }
	}

	/**
	 * A module of a robot (also the router and the virtual robot). The name is
	 * the INFO of the ADF; <code>properties</code> holds the rest with the ADF
	 * suffixes (CLASS, MODE, PASSIVE, QUEUED, POLLED, EXTIME, PRI, GFX, GMODE,
	 * DESC, ...); the events are
	 * the CONNECT entries. ADF prefixes are generated when an ADF is rebuilt.
	 */
	static public class Module
	{
		public String				name		= "Module";
		public Map<String, String>	properties	= new LinkedHashMap<String, String> ();
		public List<Event>			events		= new ArrayList<Event> ();

		public Module ()									{ }
		public Module (String name)							{ this.name = name; }

		public String	get (String key)					{ return properties.get (key); }
		public void		set (String key, String value)
		{
			if ((value == null) || (value.trim ().length () == 0))		properties.remove (key);
			else															properties.put (key, value.trim ());
		}

		public Module copy ()
		{
			Module	m = new Module (name);
			m.properties.putAll (properties);
			for (Event e : events)		m.events.add (e.copy ());
			return m;
		}
	}

	/**
	 * A robot: name, local Linda space, optional router, modules and virtual
	 * robot. <code>properties</code> are robot-wide ADF entries that belong to
	 * no module (e.g. WHCOORD, read by IForkPlanner from the whole set).
	 */
	static public class Robot
	{
		public String				name			= DEFAULT_ROBOT;
		public Linda				linda			= new Linda ();
		public Module				router;											// null when the robot has no router (see validate)
		public List<Module>			modules			= new ArrayList<Module> ();
		public Module				virtualRobot	= newVirtualRobot ();
		public Map<String, String>	properties		= new LinkedHashMap<String, String> ();
		public String				start;											// start point of the world ("START_2"); null: the i-th one, by order

		public Robot ()										{ }
		/** A new robot: local Linda space and virtual robot, without router (one is added when the deployment has a global Linda space). */
		public Robot (String name)							{ this.name = name; }

		public Robot copy ()
		{
			Robot	r = new Robot (name);
			r.linda			= linda.copy ();
			r.router		= (router == null) ? null : router.copy ();
			for (Module m : modules)		r.modules.add (m.copy ());
			r.virtualRobot	= virtualRobot.copy ();
			r.properties.putAll (properties);
			r.start			= start;
			return r;
		}
	}

	/* ------------------------------------------------------------------ */

	public String				world;											// world map used by the simulation (all robots); see getWorldFile
	public Linda				globalLinda;									// null when there is none
	public List<Robot>			robots		= new ArrayList<Robot> ();

	protected transient File	file;											// where it was loaded from / saved to
	protected transient String	original;										// JSON as loaded or saved (to detect changes)

	static protected Gson gson ()
	{
		return new GsonBuilder ().setPrettyPrinting ().disableHtmlEscaping ().create ();
	}

	/** A new deployment: no global Linda space and one robot named "Unnamed". */
	static public DeployArch create ()
	{
		DeployArch	d = new DeployArch ();
		d.robots.add (new Robot (DEFAULT_ROBOT));
		d.original	= d.toJson ();
		return d;
	}

	static public DeployArch load (File f) throws IOException
	{
		Reader	in = new FileReader (f, java.nio.charset.StandardCharsets.UTF_8);
		try
		{
			DeployArch	d = gson ().fromJson (in, DeployArch.class);
			if (d == null)		throw new IOException ("Empty deployment file");
			d.normalise ();
			d.file		= f;
			d.original	= d.toJson ();
			return d;
		}
		finally { in.close (); }
	}

	public void save (File f) throws IOException
	{
		String	json = toJson ();
		Writer	out = new FileWriter (f, java.nio.charset.StandardCharsets.UTF_8);
		try { out.write (json); out.write ('\n'); }
		finally { out.close (); }
		file		= f;
		original	= json;
	}

	public String toJson ()							{ return gson ().toJson (this); }

	/** Deep copy (the editor works on a copy). */
	public DeployArch copy ()
	{
		DeployArch	d = new DeployArch ();
		d.world			= world;
		d.globalLinda	= (globalLinda == null) ? null : globalLinda.copy ();
		for (Robot r : robots)		d.robots.add (r.copy ());
		d.file			= file;
		d.original		= original;
		return d;
	}

	/** Takes the content of another deployment (after editing a copy), keeping the file. */
	public void replaceWith (DeployArch d)
	{
		world		= d.world;
		globalLinda	= (d.globalLinda == null) ? null : d.globalLinda.copy ();
		robots.clear ();
		for (Robot r : d.robots)	robots.add (r.copy ());
	}

	/** Fills what Gson may have left null in a hand-written or older file. */
	protected void normalise ()
	{
		if (robots == null)		robots = new ArrayList<Robot> ();
		if ((world != null) && (world.trim ().length () == 0))		world = null;
		for (Robot r : robots)
		{
			if (r.name == null)			r.name = DEFAULT_ROBOT;
			if (r.linda == null)		r.linda = new Linda ();
			if (r.modules == null)		r.modules = new ArrayList<Module> ();
			if (r.virtualRobot == null)	r.virtualRobot = newVirtualRobot ();
			if (r.properties == null)	r.properties = new LinkedHashMap<String, String> ();
			if ((r.start != null) && (r.start.trim ().length () == 0))	r.start = null;
			if (r.virtualRobot.properties != null)
			{
				// the world is one for the whole deployment, and the a priori knowledge a property of the world itself
				if ((world == null) || (world.trim ().length () == 0))		setWorldFile (r.virtualRobot.get ("WORLD"));
				r.virtualRobot.properties.remove ("WORLD");
				r.virtualRobot.properties.remove ("APW");
				r.virtualRobot.properties.remove ("RADDR");		// connection of the virtual robot to a robot driver:
				r.virtualRobot.properties.remove ("RPORT");		// never used, the drivers take their address
				r.virtualRobot.properties.remove ("LPORT");		// from the robot description
			}
			List<Module>	all = new ArrayList<Module> (r.modules);
			all.add (r.virtualRobot);
			if (r.router != null)		all.add (r.router);
			for (Module m : all)
			{
				if (m.name == null)			m.name = "Module";
				if (m.properties == null)	m.properties = new LinkedHashMap<String, String> ();
				if (m.events == null)		m.events = new ArrayList<Event> ();
			}
		}
		getWorldFile ();														// leaves it null when it is empty
	}

	public File		getFile ()						{ return file; }
	public boolean	isModified ()					{ return (original == null) || !original.equals (toJson ()); }

	/**
	 * World map of the deployment (attribute <code>world</code>), or null. It is
	 * one for every robot: the virtual robots do not carry one of their own,
	 * they are given this one when they are executed.
	 */
	public String getWorldFile ()
	{
		if ((world != null) && (world.trim ().length () == 0))		world = null;
		return world;
	}

	public void setWorldFile (String path)
	{
		world = ((path == null) || (path.trim ().length () == 0)) ? null : path.trim ();
	}

	/** Index (0-based) of the start point named "START_k", or -1 when the name is not of that form. */
	static public int startIndex (String name)
	{
		if ((name == null) || !name.startsWith ("START_"))		return -1;
		try { return Integer.parseInt (name.substring (6).trim ()) - 1; } catch (Exception e) { return -1; }
	}

	/** Name of the i-th (0-based) start point of a world: START_1, START_2, ... */
	static public String startName (int i)			{ return "START_" + (i + 1); }

	/* ------------------------------------------------------------------ */
	/* Defaults                                                            */
	/* ------------------------------------------------------------------ */

	static public Module newVirtualRobot ()
	{
		Module	m = new Module ("Virtual Robot");
		m.set ("CLASS", "tc.vrobot.VirtualRobot");
		m.set ("MODE", "shared");
		m.set ("PASSIVE", "false");
		m.set ("EXTIME", "100");
		m.set ("GFX", "true");
		return m;
	}

	static public Module newRouter ()
	{
		Module	m = new Module ("Linda Router");
		m.set ("CLASS", "tc.coord.LindaRouter");
		m.set ("MODE", "shared");
		m.set ("GMODE", "tcp");
		m.set ("GFX", "false");
		return m;
	}

	static public Module newModule (String name)
	{
		Module	m = new Module (name);
		m.set ("CLASS", "tc.runtime.thread.StdThread");
		m.set ("MODE", "shared");
		m.set ("PASSIVE", "true");
		m.set ("GFX", "false");
		return m;
	}

	static public Linda newGlobalLinda ()			{ return new Linda ("localhost", 5500, false); }

	/**
	 * Problems that prevent the deployment from being executed, as messages
	 * for the user (empty when it can run). One robot runs on its local Linda
	 * space alone; two or more need a global Linda space and a Linda router in
	 * every robot, which is how their modules exchange coordination tuples.
	 */
	public List<String> validate ()
	{
		List<String>	problems = new ArrayList<String> ();
		if (robots.isEmpty ())
			problems.add ("The deployment has no robots to execute.");
		if (robots.size () > 1)
		{
			if (globalLinda == null)
				problems.add ("A deployment with several robots needs a global Linda space (the robots coordinate through it).");
			List<String>	without = new ArrayList<String> ();
			for (Robot r : robots)	if (r.router == null)	without.add (r.name);
			if (!without.isEmpty ())
				problems.add ("Every robot of a multi-robot deployment needs a Linda router; missing in: " + String.join (", ", without) + ".");
		}
		return problems;
	}

	/* ------------------------------------------------------------------ */
	/* ADF (Properties) conversion                                         */
	/* ------------------------------------------------------------------ */

	/**
	 * Imports a legacy architecture definition file (.arch) as a deployment
	 * with one robot, named after the file (IFORK-1) unless the ADF has a NAME.
	 */
	static public DeployArch importArch (File f) throws IOException
	{
		String	n = f.getName ();
		int		dot = n.lastIndexOf ('.');
		if (dot > 0)		n = n.substring (0, dot);
		Properties	props = new Properties ();
		InputStream	in = new FileInputStream (f);
		try { props.load (in); } finally { in.close (); }
		DeployArch	d = fromProperties (props, n.toUpperCase () + "-1");
		d.normalise ();								// drops the keys the runtime no longer reads (APW, ...)
		return d;
	}

	/**
	 * Imports a legacy architecture definition (the Properties of a .arch) as
	 * a deployment with one robot; <code>robotName</code> is used when the
	 * ADF has no NAME property.
	 */
	static public DeployArch fromProperties (Properties props, String robotName)
	{
		DeployArch	d = new DeployArch ();
		if (hasPrefix (props, "GLIN"))
			d.globalLinda	= readLinda (props, "GLIN", 5500, false);

		String	name = props.getProperty ("NAME");
		Robot	r = new Robot (((name != null) && (name.trim ().length () > 0)) ? name.trim () : robotName);
		r.linda	= readLinda (props, "LLIN", 3000, true);
		List<String>	prefixes = new ArrayList<String> ();						// blocks read, to spot the loose entries
		prefixes.add ("GLIN"); prefixes.add ("LLIN");
		String	mods = props.getProperty ("MODULES");
		if (mods != null)
		{
			StringTokenizer	st = new StringTokenizer (mods, ", \t");
			while (st.hasMoreTokens ())
			{
				String	pre = st.nextToken ();
				prefixes.add (pre);
				r.modules.add (readModule (props, pre));
			}
		}
		String	router = props.getProperty ("ROUTER");
		if ((router != null) && (router.trim ().length () > 0))		{ prefixes.add (router.trim ()); r.router = readModule (props, router.trim ()); }
		String	vrobot = props.getProperty ("VROBOT");
		if ((vrobot != null) && (vrobot.trim ().length () > 0))		{ prefixes.add (vrobot.trim ()); r.virtualRobot = readModule (props, vrobot.trim ()); }
		// entries that belong to no block: robot-wide properties
		List<String>	keys = new ArrayList<String> (props.stringPropertyNames ());
		java.util.Collections.sort (keys);
		for (String k : keys)
		{
			boolean	owned = isGlobalKey (k);
			for (String pre : prefixes)
				if ((pre != null) && k.startsWith (pre) && (k.length () > pre.length ()) && Character.isUpperCase (k.charAt (pre.length ())))		owned = true;
			if (!owned)		r.properties.put (k, props.getProperty (k).trim ());
		}
		d.robots.add (r);
		d.getWorldFile ();													// adopt the robot's world as the deployment world
		d.original	= null;												// imported: counts as modified until saved
		return d;
	}

	static private boolean hasPrefix (Properties props, String prefix)
	{
		for (String k : props.stringPropertyNames ())
			if (k.startsWith (prefix) && (k.length () > prefix.length ()) && Character.isUpperCase (k.charAt (prefix.length ())))		return true;
		return false;
	}

	static private Linda readLinda (Properties props, String prefix, int defPort, boolean defCreate)
	{
		Linda	l = new Linda ();
		l.address	= props.getProperty (prefix + "ADDR", "localhost").trim ();
		try { l.port = Integer.parseInt (props.getProperty (prefix + "PORT", "").trim ()); } catch (Exception e) { l.port = defPort; }
		String	c = props.getProperty (prefix + "CREATE");
		l.instantiate	= (c == null) ? defCreate : Boolean.parseBoolean (c.trim ());
		return l;
	}

	static private final String[]	GLOBAL_KEYS	= { "MODULES", "ROUTER", "VROBOT", "ROBNAME", "NAME" };

	static private boolean isGlobalKey (String k)
	{
		for (String g : GLOBAL_KEYS)		if (g.equals (k))	return true;
		return false;
	}

	static private Module readModule (Properties props, String prefix)
	{
		Module	m = new Module (prefix);
		List<String>	keys = new ArrayList<String> (props.stringPropertyNames ());
		java.util.Collections.sort (keys);
		for (String k : keys)
		{
			if (isGlobalKey (k) || !k.startsWith (prefix) || (k.length () <= prefix.length ()) || !Character.isUpperCase (k.charAt (prefix.length ())))		continue;
			String	suffix = k.substring (prefix.length ());
			String	value = props.getProperty (k).trim ();
			if (suffix.equals ("INFO"))				m.name = value;
			else if (suffix.equals ("CONNECT"))
			{
				StringTokenizer	st = new StringTokenizer (value, ",");
				while (st.hasMoreTokens ())
				{
					StringTokenizer	tk = new StringTokenizer (st.nextToken (), " \t");
					if (!tk.hasMoreTokens ())		continue;
					String[]	e = { "", "", "" };
					for (int i = 0; (i < 3) && tk.hasMoreTokens (); i++)		e[i] = tk.nextToken ();
					m.events.add (new Event (e[0], e[1], e[2]));
				}
			}
			else									m.set (suffix, value);
		}
		return m;
	}
}
