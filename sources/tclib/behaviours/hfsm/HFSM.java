/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tclib.behaviours.hfsm.lua.Lua;
import tclib.behaviours.hfsm.lua.LuaError;
import tclib.behaviours.hfsm.lua.LuaScript;
import tclib.behaviours.hfsm.lua.LuaState;

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
 */
public class HFSM
{
	/** Where the behaviours the states name are looked for, when nothing else is said. */
	static public final String		BEHAVIOURS	= "./conf/programs/lua";

	// The machine
	protected MetaState				root;
	protected File					file;
	protected List<XMLParser.PrivateVar>	vars = new ArrayList<XMLParser.PrivateVar> ();
	protected List<String>			problems = new ArrayList<String> ();

	// Where it is: the state of each level, outermost first
	protected List<State>			active = new ArrayList<State> ();

	// How it is run
	protected LuaState				lua;
	protected Chaos					chaos;
	protected String				behaviours = BEHAVIOURS;
	protected Map<String, LuaScript>	library = new HashMap<String, LuaScript> ();
	protected List<String>			missing = new ArrayList<String> ();

	protected boolean				debug;
	protected String				lastTransition;
	protected long					steps;

	/** Loads the machine a file holds, with a bridge of its own. */
	public HFSM (String path) throws Exception
	{
		this (new File (path), new Chaos ());
	}

	/**
	 * Loads a machine, either the one file of it (<code>.hfsm</code>, which holds
	 * everything) or the <code>.xas</code> of the Chaos editor, whose scripts are
	 * the <code>.acc</code> files beside it.
	 */
	public HFSM (File file, Chaos chaos) throws Exception
	{
		this.file	= file;
		this.chaos	= (chaos != null) ? chaos : new Chaos ();

		if (file.getName ().toLowerCase ().endsWith (HFSMJson.SUFFIX))
		{
			HFSMJson.Machine	machine = HFSMJson.read (file);

			this.root		= machine.root;
			this.vars		= machine.vars;
			this.problems	= machine.problems;
		}
		else
		{
			XMLParser	parser = XMLParser.parse (file);

			this.root		= parser.root ();
			this.vars		= parser.privateVars ();
			this.problems	= parser.problems ();
			this.root.loadCode (file.getParent ());
		}
		this.root.sortAll ();
		this.root.compileAll (this.problems);

		this.lua	= new LuaState ();
		this.lua.set ("chaos", this.chaos.table ());
		declare ();
		reset ();
	}

	/* ------------------------------------------------------------------ */
	/* What was loaded                                                     */
	/* ------------------------------------------------------------------ */

	public final MetaState			root ()				{ return root; }
	public final File				file ()				{ return file; }
	public final LuaState			lua ()				{ return lua; }
	public final Chaos				chaos ()			{ return chaos; }
	public final List<XMLParser.PrivateVar>	vars ()		{ return vars; }
	/** What the file said that could not be made sense of. */
	public final List<String>		problems ()			{ return problems; }
	/** The behaviours the machine named and were nowhere to be found. */
	public final List<String>		missing ()			{ return missing; }
	public final long				steps ()			{ return steps; }

	public void debug (boolean d)						{ debug = d; }

	/** Where the behaviours the states name are looked for. */
	public void behaviours (String path)				{ behaviours = path;	library.clear (); }
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
				if (debug)	System.out.println ("  [HFSM] Meta state <" + m.getName () + "> has no initial state");
				return;
			}
			s	= init;
		}
		active.add (s);
	}

	/** The state the machine is in, the innermost one. */
	public State state ()
	{
		return active.isEmpty () ? null : active.get (active.size () - 1);
	}

	/** Where the machine is, as <code>root.meta.state</code>. */
	public String where ()
	{
		StringBuffer	sb = new StringBuffer ();

		for (int i = 0; i < active.size (); i++)
			sb.append ((i > 0) ? "." : "").append (active.get (i).getName ());
		return sb.toString ();
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

		chaos.clear ();
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
		behaviour (chaos.behaviour ());
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
