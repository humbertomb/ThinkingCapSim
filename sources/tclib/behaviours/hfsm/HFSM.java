/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tclib.behaviours.lua.LuaBridge;
import tclib.behaviours.lua.interpreter.Lua;
import tclib.behaviours.lua.interpreter.LuaError;
import tclib.behaviours.lua.interpreter.LuaScript;
import tclib.behaviours.lua.interpreter.LuaState;

/**
 * A machine of hierarchical states, loaded from a <code>.xas</code> file and run
 * a cycle at a time.
 *
 * On every cycle the machine, from the innermost state it is in outwards, tries
 * the transitions of that state in the order of their priority: the first one
 * whose test says yes is taken, its own script is run, and the machine is left
 * in the state it arrives at (entering a meta state leaves it at its initial
 * state). Then the script of the state it has ended in is run, which is what
 * chooses the behaviour that drives the robot.
 *
 * Every script runs in the same interpreter, so what one leaves in a global
 * another one finds, as the Chaos robots had it. The constants the file declares
 * (<code>privatevariable</code>) are globals from the start.
 *
 * What the scripts speak to the robot through is a bridge ({@link LuaBridge}),
 * given by whoever runs the machine and put in the interpreter under its name:
 * the machine knows nothing of what is in it, it only clears it before every
 * cycle and asks it which behaviour the scripts chose. A machine may run with
 * no bridge at all, its scripts then having the language alone.
 */
public class HFSM
{
	// The machine
	protected MetaState				root;
	protected File					file;
	protected List<XMLParser.PrivateVar>	vars = new ArrayList<XMLParser.PrivateVar> ();
	protected List<String>			problems = new ArrayList<String> ();

	// Where it is: the state of each level, outermost first
	protected List<State>			active = new java.util.concurrent.CopyOnWriteArrayList<State> ();	// the monitor reads it from another thread

	// How it is run
	protected LuaState				lua;
	protected LuaBridge				bridge;
	protected String				behaviours;					// where the behaviours the states name are: what the file says, else the folder of the file
	protected Map<String, LuaScript>	library = new HashMap<String, LuaScript> ();
	protected List<String>			missing = new ArrayList<String> ();

	protected boolean				debug;
	protected String				lastTransition;
	protected long					steps;

	/** Loads the machine a file holds, with no bridge: its scripts have the language alone. */
	public HFSM (String path) throws Exception
	{
		this (new File (path), null);
	}

	/**
	 * Loads a machine, either the one file of it (<code>.hfsm</code>, which holds
	 * everything) or the <code>.xas</code> of the Chaos editor, whose scripts are
	 * the <code>.acc</code> files beside it.
	 */
	public HFSM (File file, LuaBridge bridge) throws Exception
	{
		this.file	= file;
		this.bridge	= bridge;

		if (file.getName ().toLowerCase ().endsWith (HFSMJson.SUFFIX))
		{
			HFSMJson.Machine	machine = HFSMJson.read (file);

			this.root		= machine.root;
			this.vars		= machine.vars;
			this.problems	= machine.problems;
			this.behaviours	= machine.behpath;
		}
		else
		{
			XMLParser	parser = XMLParser.parse (file);

			this.root		= parser.root ();
			this.vars		= parser.privateVars ();
			this.problems	= parser.problems ();
			this.root.loadCode (file.getParent ());
		}
		// a machine that does not say where its behaviours are has them beside it
		if (this.behaviours == null)
			this.behaviours	= (file.getAbsoluteFile ().getParent () != null) ? file.getAbsoluteFile ().getParent () : ".";
		this.root.sortAll ();
		this.root.compileAll (this.problems);

		this.lua	= new LuaState ();
		if (bridge != null)			this.lua.set (bridge.name (), bridge.table ());
		declare ();
		reset ();
	}

	/* ------------------------------------------------------------------ */
	/* What was loaded                                                     */
	/* ------------------------------------------------------------------ */

	public final MetaState			root ()				{ return root; }
	public final File				file ()				{ return file; }
	public final LuaState			lua ()				{ return lua; }
	/** What the scripts speak to the robot through, or null when they have the language alone. */
	public final LuaBridge			bridge ()			{ return bridge; }
	public final List<XMLParser.PrivateVar>	vars ()		{ return vars; }
	/** What the file said that could not be made sense of. */
	public final List<String>		problems ()			{ return problems; }
	/** The behaviours the machine named and were nowhere to be found. */
	public final List<String>		missing ()			{ return missing; }
	public final long				steps ()			{ return steps; }

	public void debug (boolean d)						{ debug = d; }

	/** Where the behaviours the states name are looked for: what the module was told (BEH), else what the file says (behpath), else the folder of the file. */
	public void behaviours (String path)				{ if ((path != null) && (path.trim ().length () > 0))	{ behaviours = path.trim ();	library.clear ();	missing.clear (); } }
	public String behaviours ()							{ return behaviours; }

	/** The constants the file declares, as globals of the interpreter. */
	protected void declare ()
	{
		for (XMLParser.PrivateVar v : vars)
		{
			Double	d = Lua.tonumber (v.initValue);

			lua.set (v.name, (d != null) ? (Object) d : (Object) v.initValue);
		}
	}

	/* ------------------------------------------------------------------ */
	/* Where the machine is                                                */
	/* ------------------------------------------------------------------ */

	/** Leaves the machine at the initial state of every level. */
	public void reset ()
	{
		active.clear ();
		enter (root);
		steps			= 0;
		lastTransition	= null;
	}

	/** Goes into a state: a meta state is entered at its initial state, and so on. */
	protected void enter (State s)
	{
		while (s instanceof MetaState)
		{
			MetaState	m = (MetaState) s;
			State		init = m.getInitialState ();

			active.add (m);
			if (init == null)												// a meta state with no initial state stops here
			{
				System.out.println ("  [HFSM] Meta state <" + m.getName () + "> has no initial state: the machine does nothing there");
				return;
			}
			s	= init;
		}
		active.add (s);
	}

	/**
	 * Where the machine is, as the state of every level, outermost first: the last
	 * one is the state it is really in, and the ones before it the meta states that
	 * hold it. It is a copy, safe to look at while the machine runs.
	 */
	public List<State> active ()
	{
		return new ArrayList<State> (active);
	}

	/** The state the machine is in, the innermost one. */
	public State state ()
	{
		Object[]	now = active.toArray ();							// one picture of it, whatever the machine does meanwhile

		return (now.length == 0) ? null : (State) now[now.length - 1];
	}

	/** Where the machine is, as <code>root.meta.state</code>. */
	public String where ()
	{
		StringBuffer	sb = new StringBuffer ();

		for (State s : active)												// a snapshot: the machine may move meanwhile
			sb.append ((sb.length () > 0) ? "." : "").append (s.getName ());
		return sb.toString ();
	}

	/**
	 * Why the machine does nothing, when it does nothing: it is in a meta state
	 * that has no initial state (deleted in the editor, or named in the file by a
	 * name no state has), so there is no state to run. Null when it runs.
	 */
	public String stuck ()
	{
		State		s = state ();

		if (s == null)							return "the machine has no state to be in";
		if (s instanceof MetaState)				return "meta state '" + s.getName () + "' has no initial state, so the machine does nothing there";
		return null;
	}

	/** The last transition taken, or null when none has been. */
	public String lastTransition ()						{ return lastTransition; }

	/* ------------------------------------------------------------------ */
	/* Running it                                                          */
	/* ------------------------------------------------------------------ */

	/**
	 * One cycle of the machine: takes a transition if one is due, runs the script
	 * of the state it is in and then the behaviour that state chose. What the
	 * scripts commanded is left in the bridge.
	 */
	public void step ()
	{
		if (active.isEmpty ())					reset ();

		if (bridge != null)			bridge.clear ();
		lastTransition	= null;
		steps++;

		// a transition of the state it is in, or of the meta state that holds it, and so outwards
		for (int level = active.size () - 1; level >= 0; level--)
		{
			State		s = active.get (level);
			Transition	taken = fired (s);

			if (taken == null)					continue;

			run (taken.getDoScript ());
			while (active.size () > level)		active.remove (active.size () - 1);
			enter (taken.getArrivalState ());
			lastTransition	= taken.getName ();
			if (debug)		System.out.println ("  [HFSM] " + s.getName () + " -- " + taken.getName () + " --> " + where ());
			break;
		}

		// what the robot does while it is there
		State			now = state ();

		if (now != null)						run (now.getScript ());

		// and the behaviour that was chosen, which is what moves it
		if (bridge != null)			behaviour (bridge.behaviour ());
	}

	/** The first transition of a state whose test says yes, or null. */
	protected Transition fired (State s)
	{
		for (Transition t : s.getTransitions ())
		{
			if ((t.getTestScript () == null) || (t.getArrivalState () == null))		continue;
			if (Lua.truth (run (t.getTestScript ())))								return t;
		}
		return null;
	}

	/** Runs the behaviour of the library the machine asked for, if it is there. */
	protected void behaviour (String name)
	{
		if ((name == null) || (name.length () == 0))		return;

		LuaScript		script = library.get (name);

		if (script == null)
		{
			if (missing.contains (name))		return;

			File		f = new File (behaviours, name + ".lua");

			if (!f.exists ())
			{
				missing.add (name);
				System.out.println ("  [HFSM] Behaviour <" + name + "> not found in " + behaviours);
				return;
			}
			try { script = lua.loadFile (f); }
			catch (Exception e)
			{
				missing.add (name);
				System.out.println ("  [HFSM] Behaviour <" + name + "> cannot be read: " + e.getMessage ());
				return;
			}
			library.put (name, script);
			if (debug)		System.out.println ("  [HFSM] Behaviour <" + name + "> read from " + f);
		}
		run (script);
	}

	/**
	 * Runs one script. A script that fails does not stop the machine: it is said
	 * out loud and the cycle carries on, as a robot that stops is worse than a
	 * robot that misses a cycle.
	 */
	protected Object run (LuaScript script)
	{
		if (script == null)						return null;
		try { return lua.run (script); }
		catch (LuaError e)
		{
			System.out.println ("  [HFSM] " + e.getMessage ());
			return null;
		}
		catch (RuntimeException e)
		{
			System.out.println ("  [HFSM] " + script.name () + ": " + e);
			return null;
		}
	}

	/* ------------------------------------------------------------------ */
	/* A look at it                                                        */
	/* ------------------------------------------------------------------ */

	/** How many states, meta states, transitions and scripts the machine holds. */
	public String summary ()
	{
		int[]		n = new int[4];

		count (root, n);
		return n[0] + " states, " + n[1] + " meta states, " + n[2] + " transitions, " + n[3] + " scripts";
	}

	static private void count (MetaState m, int[] n)
	{
		for (State s : m.getStatesList ())
		{
			if (s instanceof MetaState)			{ n[1]++;	count ((MetaState) s, n); }
			else								n[0]++;
			if (s.getScript () != null)			n[3]++;
			for (Transition t : s.getTransitions ())
			{
				n[2]++;
				if (t.getTestScript () != null)	n[3]++;
				if (t.getDoScript () != null)	n[3]++;
			}
		}
		for (Transition t : m.getTransitions ())
		{
			n[2]++;
			if (t.getTestScript () != null)		n[3]++;
			if (t.getDoScript () != null)		n[3]++;
		}
	}

	public String toString ()
	{
		return "HFSM " + root.getName () + " (" + summary () + ") at " + where ();
	}
}
