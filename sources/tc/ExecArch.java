/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 * (c) 2003 Humberto Martinez
 */

package tc;

import java.io.*;
import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.linda.net.*;
import tcapps.tcsimulator.simulator.Simulator;
import tcapps.tcsimulator.simulator.objects.SimRobot;
import wucore.utils.geom.Point3;

public class ExecArch extends Thread
{
	static public final int 		MAX_THS		= 20;	// Maximum number of modules
	
	// Linda Spaces parameters
	public LindaDesc				lldesc;				// Local Linda Space description
	public LindaDesc				gldesc;				// Global Linda Space description
	
	// Actual Linda Spaces
	protected LindaServer			linda_loc;
	protected LindaServer			linda_glob;

	// Architecture modules and parameters
	public int					num;					// Number of modules
	public ThreadDesc[]			thdesc;				// Actual standard modules
	public ThreadDesc			vrdesc;				// Actual VirtualRobot module
	public RouterDesc			lrdesc;				// Actual LindaRouter module
		
	// Additional execution variables
	protected boolean			initialised	= false;
	protected volatile boolean	running		= false;
	protected Properties			props;
	protected String				robotid;

	// Simulation: when a Simulator is given the virtual robot module is replaced by a SimRobot bound to it
	protected Simulator			sim;
	protected Point3			start;				// Optional initial pose of the simulated robot

	// Source file (when loaded from / saved to an ADF), kept to preserve comments and layout on save
	protected File				file;
	protected List<String>		source;				// Original text lines (null when built from Properties only)
	protected Properties			original;			// Properties as read from the source text

	/* Constructors */
	protected ExecArch ()
	{
	}

	public ExecArch (String robotid, String name)
	{
		this (robotid, name, (Properties) null);
	}

	public ExecArch (String robotid, String name, Properties pdefs)
	{
		Properties		props;

		// Read properties from file
		props			= new Properties ();
		try
		{
			readSource (new File (name));
			props.load (new StringReader (joinLines (source)));
		} catch (Exception e) { e.printStackTrace (); }
		original		= (Properties) props.clone ();
		
		initialise (robotid, props, pdefs);
	}

	/**
	 * Loads an ADF for inspection/editing (no robot identifier is injected into
	 * the properties). Throws if the file cannot be read.
	 */
	public static ExecArch load (File f) throws IOException
	{
		ExecArch	arch = new ExecArch ();
		arch.readSource (f);
		Properties	props = new Properties ();
		props.load (new StringReader (joinLines (arch.source)));
		arch.original	= (Properties) props.clone ();
		arch.initialise (null, props, null);
		return arch;
	}

	/** Creates an empty architecture (single virtual robot module, no world) from the built-in template. */
	public static ExecArch create ()
	{
		ExecArch	arch = new ExecArch ();
		arch.source		= new ArrayList<String> (Arrays.asList (TEMPLATE.split ("\n")));
		Properties	props = new Properties ();
		try { props.load (new StringReader (TEMPLATE)); } catch (IOException e) { }
		arch.original	= (Properties) props.clone ();
		arch.initialise (null, props, null);
		return arch;
	}

	public ExecArch (String robotid, Properties props)
	{
		
		initialise (robotid, props, null);
	}

	public ExecArch (String robotid, Properties props, Properties pdefs)
	{
		initialise (robotid, props, pdefs);
	}

	/**
	 * Simulated execution (former ExecArchSim): the architecture is read from
	 * <code>name</code> and its virtual robot runs as a {@link SimRobot} inside
	 * <code>sim</code>. If the simulator has no world yet, the architecture's
	 * world is loaded into it; otherwise the simulator's world is imposed.
	 */
	public ExecArch (String robotid, String name, Properties pdefs, Simulator sim)
	{
		this (robotid, name, pdefs);
		simulate (sim);
	}

	/** Simulated execution of an architecture given as properties (see the file-based constructor). */
	public ExecArch (String robotid, Properties props, Properties pdefs, Simulator sim)
	{
		this (robotid, props, pdefs);
		simulate (sim);
	}

	protected void simulate (Simulator sim)
	{
		this.sim	= sim;
		if ((sim == null) || (vrdesc == null))		return;

		// Load a world description if none available
		if (sim.getWorld () == null)
		{
			try  { sim.setWorld (props.getProperty (vrdesc.preffix + "WORLD")); }
			catch (Exception e)
			{
				System.out.println ("[ExecArch]: Exception loading world: " + e);
				return;
			}
		}
		if (sim.getWorldName () != null)
			props.setProperty (vrdesc.preffix + "WORLD", sim.getWorldName ());

		// The simulated robot replaces the real one
		vrdesc.classn = SimRobot.class.getName ();
	}

	public Simulator	getSimulator ()					{ return sim; }
	public void			setStart (Point3 start)			{ this.start = start; }

	// Class methods
	public static void main (String[] argv)
	{
		if (argv.length == 2)
				new ExecArch (argv[0], argv[1]).start ();
		else
		{
			System.out.println ("ERROR: wrong number of arguments.");
			System.out.println ("\tUsage: ExecArch <robot_id> <arch> to execute an ADF");
		}
	}
    
	/* Instance methods */
	protected void initialise (String robotid, Properties props, Properties pdefs)
	{
		String			modules, vrmodule, lrmodule;
		String			preffix;
		StringTokenizer	st;
		Enumeration<?>	enu;
		String			pname;
		
		// Setup private local variables
		this.props		= props;
		this.robotid	= robotid;
				
		if (robotid != null)		props.setProperty ("ROBNAME", robotid);
		
		// Prepare data structures
		num				= 0;
		thdesc			= new ThreadDesc[MAX_THS];

		// Overwrite default properties
		if (pdefs != null)
		{
			enu	= pdefs.propertyNames ();
			while (enu.hasMoreElements ())
			{
				pname	= (String) enu.nextElement ();
				
				props.setProperty (pname, pdefs.getProperty (pname));
			}
		}

		// Load and parse Linda servers properties
		lldesc		= new LindaDesc ("LLIN", LindaDesc.L_LOCAL, props);			
		gldesc		= new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, props);			

		// Load and parse architecture global properties
		modules		= props.getProperty ("MODULES");
		vrmodule		= props.getProperty ("VROBOT");
		lrmodule		= props.getProperty ("ROUTER");
		if (modules != null)
		{
			st			= new StringTokenizer (modules, ", \t");
			for (num = 0; st.hasMoreTokens (); num++)
			{
				preffix		= st.nextToken ();
				thdesc[num]	= new ThreadDesc (preffix, props);
			}
		}
		if (lrmodule != null)
			lrdesc		= new RouterDesc (lrmodule, props);			
		if (vrmodule != null)
			vrdesc		= new ThreadDesc (vrmodule, props);			
			
		initialised		= true;	
	}
	
	public void run ()
	{
		int				i;
		
		while (!initialised);
		
		this.setName ("TC-Arch-Executor ("+robotid+")");

		System.out.println ("\nExecArch: Executing a TC-II ADF (Architecture Definition File)");
		System.out.println (this);
		System.out.println ();		
		
		try
		{
			// Crate Linda Spaces if required
			if (lldesc.create)			linda_loc	= lldesc.start_server ();
			if (gldesc.create)			linda_glob	= gldesc.start_server ();
			
			// Execute LindaRouter if needed
			if (lrdesc != null)
				lrdesc.start_thread (robotid, props, lldesc, linda_loc, gldesc, linda_glob);

			// Execute required standard modules
			for (i = 0; i < num; i++)
				thdesc[i].start_thread (robotid, props, lldesc, linda_loc);
			
			// Execute VirtualRobot if needed
			virtual_robot ();
		}
		catch (Exception e) { e.printStackTrace (); }
		
		running	= true;
		try { Thread.currentThread ().join (); } catch (InterruptedException e) { /* terminate () */ }
		running	= false;
		
		System.out.println ("Program finished.");
	}

	/* ------------------------------------------------------------------ */
	/* Execution control (used by the simulator GUI)                        */
	/* ------------------------------------------------------------------ */

	/** True between the start of the modules and {@link #terminate}. */
	public boolean isRunning ()			{ return running; }

	/** Executable copy of this description (a Thread can only be started once). */
	public ExecArch runner (String robotid)
	{
		return new ExecArch (robotid, (Properties) props.clone ());
	}

	/** Executable copy of this description whose virtual robot is simulated in <code>sim</code>. */
	public ExecArch runner (String robotid, Simulator sim)
	{
		return new ExecArch (robotid, (Properties) props.clone (), null, sim);
	}

	/**
	 * Stops every module thread and the Linda servers created by this
	 * architecture and finishes the executor thread.
	 */
	public void terminate ()
	{
		int		i;

		System.out.println ("ExecArch: terminating [" + robotid + "]");
		try
		{
			if ((vrdesc != null) && (vrdesc.thread != null))		vrdesc.thread.stop ();
			for (i = 0; i < num; i++)
				if (thdesc[i].thread != null)						thdesc[i].thread.stop ();
			if (linda_loc != null)									linda_loc.stop ();
			if (linda_glob != null)									linda_glob.stop ();
		} catch (Exception e) { e.printStackTrace (); }
		running	= false;
		interrupt ();
	}

	/**
	 * Sends a task sequence (plan) to the robot through the local Linda space,
	 * as the monitor's Task panel does. Returns false when there is no local space.
	 */
	public boolean sendPlan (tclib.planning.sequence.Sequence seq)
	{
		if (linda_loc == null)			return false;
		ItemPlan	item = new ItemPlan ();
		item.set (seq, System.currentTimeMillis ());
		Tuple		tuple = new Tuple (Tuple.PLAN, item);
		tuple.space	= robotid;
		linda_loc.write (tuple);
		return true;
	}

	/** Local Linda space of the running architecture (null in remote modes or before start). */
	public Linda getLocalLinda ()		{ return linda_loc; }

	/**
	 * Sends an execution command (ItemDebug.START, STOP, STEP, ...) to every
	 * module of the robot through the local Linda space, as the monitor's
	 * execution control does. Returns false when there is no local space.
	 */
	public boolean sendCommand (int command)
	{
		if (linda_loc == null)			return false;
		ItemDebug	item = new ItemDebug ();
		item.command (command, System.currentTimeMillis ());
		Tuple		tuple = new Tuple (Tuple.DEBUG, item);
		tuple.space	= robotid;
		linda_loc.write (tuple);
		return true;
	}
	
	/* ------------------------------------------------------------------ */
	/* ADF file access (used by the simulator GUI)                          */
	/* ------------------------------------------------------------------ */

	static public final String		TEMPLATE =
		"#---------------------------------------------------------------------\n" +
		"# Architecture definition file (ThinkingCap-II ADF)\n" +
		"#---------------------------------------------------------------------\n" +
		"\n" +
		"#---------------------------------------------------------------------\n" +
		"# Robot (no global Linda server: add it explicitly when needed)\n" +
		"#---------------------------------------------------------------------\n" +
		"NAME\t\t= Unnamed\n" +
		"\n" +
		"#---------------------------------------------------------------------\n" +
		"# Linda intra-architecture server\n" +
		"#---------------------------------------------------------------------\n" +
		"LLINADDR\t\t= localhost\n" +
		"LLINPORT\t\t= 3000\n" +
		"LLINCREATE\t= true\n" +
		"\n" +
		"#---------------------------------------------------------------------\n" +
		"# Active modules and TC-II execution parameters\n" +
		"#---------------------------------------------------------------------\n" +
		"MODULES\t\t= \n" +
		"VROBOT\t\t= ROB\n" +
		"\n" +
		"#---------------------------------------------------------------------\n" +
		"# Virtual robot section\n" +
		"#---------------------------------------------------------------------\n" +
		"ROBINFO\t\t= Virtual Robot\n" +
		"ROBMODE\t\t= shared\n" +
		"ROBCLASS\t= tc.vrobot.VirtualRobot\n" +
		"ROBPASSIVE\t= false\n" +
		"ROBEXTIME\t= 100\n" +
		"ROBDESC\t\t= \n" +
		"ROBWORLD\t= \n" +
		"ROBGFX\t\t= true\n";

	public File getFile ()					{ return file; }
	public Properties getProperties ()		{ return props; }
	public String getRobotId ()				{ return robotid; }

	/** Prefix of the virtual robot module (VROBOT property, e.g. "ROB"). */
	public String getVRobotPrefix ()
	{
		String	p = props.getProperty ("VROBOT");
		return (p == null) ? "ROB" : p.trim ();
	}

	/** World map used by the virtual robot (property VROBOT+"WORLD"), or null. */
	public String getWorldFile ()
	{
		String	w = props.getProperty (getVRobotPrefix () + "WORLD");
		return ((w == null) || (w.trim ().length () == 0)) ? null : w.trim ();
	}

	public void setWorldFile (String path)
	{
		props.setProperty (getVRobotPrefix () + "WORLD", (path == null) ? "" : path);
	}

	/**
	 * Replaces the whole set of properties (the architecture was edited) and
	 * rebuilds the module descriptors. The source text is kept, so a later
	 * {@link #save(File)} preserves the layout of the unchanged lines. Not
	 * allowed while the architecture is running.
	 */
	public void replaceProperties (Properties p)
	{
		if (isRunning ())			throw new IllegalStateException ("The architecture is running");
		initialised		= false;
		lrdesc			= null;
		vrdesc			= null;
		initialise (robotid, p, null);
	}

	/** True when some property differs from the ones read from the source file. */
	public boolean isModified ()
	{
		if (original == null)			return true;
		for (String key : props.stringPropertyNames ())
		{
			if (key.equals ("ROBNAME") && !original.containsKey ("ROBNAME"))		continue;		// injected at runtime
			if (!props.getProperty (key).equals (original.getProperty (key)))		return true;
		}
		for (String key : original.stringPropertyNames ())
			if (!props.containsKey (key))		return true;
		return false;
	}

	/**
	 * Writes the architecture to <code>f</code>, preserving the comments and
	 * layout of the source file: only the entries whose value changed are
	 * rewritten (as a single line), new properties are appended at the end.
	 */
	public void save (File f) throws IOException
	{
		List<String>	out = new ArrayList<String> ();
		Set<String>		written = new HashSet<String> ();
		List<String>	src = (source != null) ? source : new ArrayList<String> ();

		for (int i = 0; i < src.size (); i++)
		{
			String	line = src.get (i);
			String	t = line.trim ();
			if ((t.length () == 0) || t.startsWith ("#") || t.startsWith ("!"))
			{
				out.add (line);
				continue;
			}
			// logical entry: this line plus its continuation lines
			int		start = i;
			while (continues (src.get (i)) && (i + 1 < src.size ()))		i++;
			String	key = keyOf (t);
			String	value = props.getProperty (key);
			if ((value == null) || (key.equals ("ROBNAME") && !original.containsKey ("ROBNAME")))
				continue;												// property removed: drop the entry
			written.add (key);
			if (value.equals (original.getProperty (key)))
				for (int j = start; j <= i; j++)		out.add (src.get (j));		// unchanged: keep the original text
			else
				out.add (prefixOf (src.get (start)) + value);
		}
		// new properties
		List<String>	added = new ArrayList<String> ();
		for (String key : props.stringPropertyNames ())
			if (!written.contains (key) && !(key.equals ("ROBNAME") && !original.containsKey ("ROBNAME")))
				added.add (key);
		if (added.size () > 0)
		{
			Collections.sort (added);
			if ((out.size () > 0) && (out.get (out.size () - 1).trim ().length () > 0))		out.add ("");
			for (String key : added)		out.add (key + "\t= " + props.getProperty (key));
		}

		Writer	w = new OutputStreamWriter (new FileOutputStream (f), "ISO-8859-1");
		try
		{
			for (String line : out)		{ w.write (line); w.write ("\n"); }
		} finally { w.close (); }

		file		= f;
		source		= out;
		original	= (Properties) props.clone ();
		if (!written.contains ("ROBNAME") && !added.contains ("ROBNAME"))		original.remove ("ROBNAME");
	}

	protected void readSource (File f) throws IOException
	{
		BufferedReader	in = new BufferedReader (new InputStreamReader (new FileInputStream (f), "ISO-8859-1"));
		List<String>	lines = new ArrayList<String> ();
		try
		{
			String	line;
			while ((line = in.readLine ()) != null)		lines.add (line);
		} finally { in.close (); }
		file	= f;
		source	= lines;
	}

	static protected String joinLines (List<String> lines)
	{
		StringBuilder	sb = new StringBuilder ();
		for (String l : lines)		sb.append (l).append ('\n');
		return sb.toString ();
	}

	/** True when a properties line ends with an odd number of backslashes (continues on the next line). */
	static protected boolean continues (String line)
	{
		int	n = 0;
		for (int i = line.length () - 1; (i >= 0) && (line.charAt (i) == '\\'); i--)		n++;
		return (n % 2) == 1;
	}

	/** Key of a (trimmed) properties line: text up to the first unescaped '=', ':' or blank. */
	static protected String keyOf (String t)
	{
		int	i = 0;
		while (i < t.length ())
		{
			char	c = t.charAt (i);
			if (c == '\\')									{ i += 2; continue; }
			if ((c == '=') || (c == ':') || Character.isWhitespace (c))		break;
			i++;
		}
		return t.substring (0, Math.min (i, t.length ()));
	}

	/** Text of a properties line up to (and including) the separator, i.e. everything before the value. */
	static protected String prefixOf (String line)
	{
		String	t = line.trim ();
		int		k = line.indexOf (t) + keyOf (t).length ();
		int		i = k;
		while ((i < line.length ()) && Character.isWhitespace (line.charAt (i)))		i++;
		if ((i < line.length ()) && ((line.charAt (i) == '=') || (line.charAt (i) == ':')))		i++;
		while ((i < line.length ()) && Character.isWhitespace (line.charAt (i)))		i++;
		String	prefix = line.substring (0, i);
		return (prefix.indexOf ('=') >= 0 || prefix.indexOf (':') >= 0) ? prefix : prefix + "= ";
	}

	protected void virtual_robot ()
	{
		if (vrdesc == null)				return;
		if (sim == null)
		{
			vrdesc.start_thread (robotid, props, lldesc, linda_loc);
			return;
		}

		// Simulated robot (former ExecArchSim.virtual_robot)
		System.out.println (">> Starting Simulated Robot [" + vrdesc.preffix + "@" + robotid + "]");
		Linda	linda = null;
		try
		{
			if (linda_loc != null)
				linda = linda_loc;
			else if (vrdesc.mode == ThreadDesc.M_UDP)
				linda = new LindaNetClient (LindaNet.UDP, null, lldesc.addr, lldesc.port);
			else if (vrdesc.mode == ThreadDesc.M_TCP)
				linda = new LindaNetClient (LindaNet.TCP, null, lldesc.addr, lldesc.port);
		} catch (Exception e)
		{
			e.printStackTrace ();
			linda = null;
		}
		if (linda == null)
		{
			System.out.println ("--[ExecArch] Can not create Linda client. Aborting simulated robot [" + vrdesc.preffix + "@" + robotid + "]");
			return;
		}

		SimRobot	thread = new SimRobot (robotid, props, linda, sim);
		thread.setTDesc (vrdesc);
		if (start != null)		thread.reset (start);
		vrdesc.thread	= thread;
		thread.start ();
	}
	
	public String toString ()
	{
		int				i;
		String			str;
		
		str		= "Architecture Description\n";
		
		str += "\t" + gldesc + "\n";									// Global Linda Space
		str += "\t" + lldesc + "\n";									// Local Linda Space
		if (gldesc != null)		str += "\t" + lrdesc + "\n";			// Linda Router
		
		for (i = 0; i < num; i++)
			str += "\t" + thdesc[i] + "\n";								// Architecture modules
			
		if (gldesc != null)		str += "\t" + vrdesc + "\n";			// Virtual Robot

		return str;
	}
}

