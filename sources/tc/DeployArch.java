/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Deployment architecture: everything the Deployment Architecture editor
 * works with, persisted as JSON in a <code>.deploy</code> file. An optional
 * global Linda space and a list of robots, each one with its local Linda
 * space, an optional Linda router, its modules and its virtual robot. Module
 * properties keep the names the runtime reads (CLASS, MODE, PASSIVE, ...,
 * see tc.runtime.thread.ThreadDesc), so {@link ExecArch} executes a robot
 * straight from this model.
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
			// a robot, simulated or physical, is never passive: it runs on its own cycle
			r.virtualRobot.set ("PASSIVE", "false");
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
		// no class: VirtualRobot is abstract, so which one this robot is has to be
		// said, and the editor offers the ones the development has
		m.set ("CLASS", "");
		m.set ("MODE", "shared");
		m.set ("PASSIVE", "false");
		m.set ("EXTIME", "100");
		m.set ("GFX", "true");
		return m;
	}

	static public Module newRouter ()
	{
		Module	m = new Module ("Linda Router");
		m.set ("CLASS", "tc.modules.LindaRouter");
		m.set ("MODE", "shared");
		m.set ("GMODE", "tcp");
		m.set ("GFX", "false");
		return m;
	}

	static public Module newModule (String name)				{ return newModule (name, null); }

	/**
	 * A module of a kind ("Controller", "Navigation", "Perception", "Planner"):
	 * the kind is kept with it (<code>TYPE</code>), which is what says the class
	 * it may be given -- one deriving from the class of that kind in
	 * <code>tc.modules</code>.
	 *
	 * It is created with no class: the one it used to be given was StdThread
	 * itself, which is abstract and could never run, so which module this is has
	 * to be said.
	 */
	static public Module newModule (String name, String type)
	{
		Module	m = new Module (name);
		if ((type != null) && (type.trim ().length () > 0))		m.set ("TYPE", type.trim ());
		m.set ("CLASS", "");
		m.set ("MODE", "shared");
		m.set ("PASSIVE", "true");
		m.set ("GFX", "false");
		// a controller waits to be told to start unless it is said otherwise
		if ("Controller".equalsIgnoreCase (m.get ("TYPE")))		m.set ("AUTO", "false");
		return m;
	}

	/** A global Linda space, which is created rather than connected to: there is none to connect to yet. */
	static public Linda newGlobalLinda ()			{ return new Linda ("localhost", 5500, true); }

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
}
