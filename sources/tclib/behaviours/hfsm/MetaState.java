/*
 * (c) 2005 Daniel Garcia Nebot, Elad Rodriguez Alvaro, Miguel Cazorla
 * (c) 2026 Humberto Martinez Barbera (ported to ThinkingCap)
 */

package tclib.behaviours.hfsm;

import java.util.ArrayList;
import java.util.List;

/**
 * A state that is a machine of its own: it holds states (and other meta states)
 * and says which of them it starts at. A meta state may also be an extern one,
 * that is, a machine kept in a file of its own.
 *
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @version 0.2
 *
 * @see State
 */
public class MetaState extends State
{
	// Contents
	protected List<State>			states;
	protected State					initialState;

	// A machine kept in a file of its own
	protected boolean				extern;
	protected String				pathExtern;

	// What the editor names the next state, meta state and transition it creates
	public int						metaStateCountToName;
	public int						stateCountToName;
	public int						transitionCountToName;

	// Verification
	protected List<Object>			unreachableStatesList;

	// Constructors
	public MetaState (int i)
	{
		this (null, i, 0, 0);
	}

	public MetaState (String n, int i)
	{
		this (n, i, 0, 0);
	}

	public MetaState (String n, int i, int x, int y)
	{
		super (n, i, x, y);

		this.states			= new ArrayList<State> ();
		this.initialState	= null;
		this.verified		= false;
	}

	/* ---------------- contents ---------------- */

	public void setInitialState (State a)		{ this.initialState = a;	this.verified = false; }
	public State getInitialState ()				{ return this.initialState; }

	public void addState (State s)				{ this.states.add (s); }
	public int getStatesSize ()					{ return this.states.size (); }
	public State getState (int i)				{ return this.states.get (i); }
	public List<State> getStatesList ()			{ return this.states; }

	public void removeState (State s)
	{
		if (s == this.initialState)				this.initialState = null;
		this.states.remove (s);
	}

	public void resetStates ()
	{
		this.states			= new ArrayList<State> ();
		this.initialState	= null;
	}

	/** The state (or meta state) of this one with the given name, or null. */
	public State findState (String name)
	{
		for (State s : this.states)
			if (s.getName ().equals (name))		return s;
		return null;
	}

	/** The state (or meta state) of this one with the given id, or null. */
	public State findState (int id)
	{
		for (State s : this.states)
			if (s.getId () == id)				return s;
		return null;
	}

	/* ---------------- how many of each ---------------- */

	public int getMetaStateCount ()
	{
		int			n = 0;

		for (State s : this.states)
			if (s instanceof MetaState)			n++;
		return n;
	}

	public int getStateCount ()					{ return this.states.size () - this.getMetaStateCount (); }

	public int getTransitionCount ()
	{
		int			n = 0;

		for (State s : this.states)
			n	+= s.getTransitionsSize ();
		return n;
	}

	/* ---------------- extern machines ---------------- */

	public boolean isExtern ()					{ return this.extern; }
	public void setExtern (boolean extern)		{ this.extern = extern; }
	public String getPathExtern ()				{ return this.pathExtern; }
	public void setPathExtern (String path)		{ this.pathExtern = path; }

	/* ---------------- the scripts of the whole machine ---------------- */

	/** Reads the scripts of every state and transition under this one. */
	public void loadCode (String path)
	{
		super.loadCode (path);
		for (State s : this.states)
		{
			if (s instanceof MetaState)			((MetaState) s).loadCode (path);
			else								s.loadCode (path);
			for (Transition t : s.getTransitions ())
				t.loadCode (path);
		}
		for (Transition t : this.getTransitions ())
			t.loadCode (path);
	}

	/** Reads every script under this one into its tree, and answers how many were read. */
	public int compileAll ()
	{
		return compileAll (null);
	}

	/**
	 * Reads every script under this one into its tree, and answers how many were
	 * read. The scripts that could not be read are added to <code>problems</code>,
	 * when one is given.
	 */
	public int compileAll (List<String> problems)
	{
		int			n = 0;

		this.compile ();
		if (this.getCodeError () != null)		note (problems, this.getCodeError ());
		if (this.getScript () != null)			n++;
		for (State s : this.states)
		{
			if (s instanceof MetaState)			n += ((MetaState) s).compileAll (problems);
			else
			{
				s.compile ();
				if (s.getScript () != null)		n++;
				if (s.getCodeError () != null)	note (problems, s.getCodeError ());
			}
			for (Transition t : s.getTransitions ())
				n	+= compile (t, problems);
		}
		for (Transition t : this.getTransitions ())
			n	+= compile (t, problems);
		return n;
	}

	static private int compile (Transition t, List<String> problems)
	{
		int			n = 0;

		t.compile ();
		if (t.getTestScript () != null)			n++;
		if (t.getDoScript () != null)			n++;
		if (t.getCodeError () != null)			note (problems, t.getCodeError ());
		return n;
	}

	static private void note (List<String> problems, String what)
	{
		if (problems != null)					problems.add (what);
	}

	/** Leaves the transitions of every state in the order they are tried. */
	public void sortAll ()
	{
		this.sortTransitions ();
		for (State s : this.states)
		{
			s.sortTransitions ();
			if (s instanceof MetaState)			((MetaState) s).sortAll ();
		}
	}

	/* ---------------- verification ---------------- */

	/**
	 * Whether the machine is correct: every meta state holds something, starts
	 * somewhere, and has no state nothing arrives at. The errors found are left in
	 * the error of each meta state.
	 */
	public boolean isCorrect ()
	{
		boolean		r;

		this.error					= "";
		this.unreachableStatesList	= new ArrayList<Object> (this.states);
		r	= this.verificateMetaState ();

		List<Transition>	all = new ArrayList<Transition> ();

		for (State s : this.states)
			if (!s.isCorrect (this.name, this.unreachableStatesList, all))
			{
				this.error	+= s.getError ();
				r	= false;
			}
		if (!this.unreachableStatesManager ())	r = false;
		for (State s : this.states)
			if ((s instanceof MetaState) && !((MetaState) s).isCorrect ())
			{
				this.error	+= s.getError ();
				r	= false;
			}
		this.verified	= true;
		this.correct	= r;
		return r;
	}

	protected boolean verificateMetaState ()
	{
		boolean		r = true;

		if (this.states.size () == 0)
		{
			this.error	+= "ERROR in Meta State '" + this.name + "' : Meta State is empty.\n";
			r	= false;
		}
		if (this.initialState == null)
		{
			this.error	+= "ERROR in Meta State '" + this.name + "' : Meta State has no initial state.\n";
			r	= false;
		}
		return r;
	}

	protected boolean unreachableStatesManager ()
	{
		boolean		r = true;

		for (Object temp : this.unreachableStatesList)
			if (temp != this.getInitialState ())
			{
				this.error	+= "ERROR in Meta State '" + this.name + "' : Unreachable "
							   + (State.isState (temp) ? "state '" : "meta state '") + ((State) temp).getName () + "'.\n";
				r	= false;
			}
		return r;
	}

	static public boolean isMetaState (Object o)			{ return o instanceof MetaState; }

	/** The whole machine as text, one line per state, for a look at what was loaded. */
	public String dump ()
	{
		StringBuffer	sb = new StringBuffer ();

		dump (sb, this, "");
		return sb.toString ();
	}

	static private void dump (StringBuffer sb, MetaState m, String indent)
	{
		sb.append (indent).append ("+ ").append (m.getName ()).append (" [").append (m.getId ()).append ("]")
		  .append (m.isExtern () ? " extern " + m.getPathExtern () : "").append ("\n");
		for (State s : m.getStatesList ())
		{
			if (s instanceof MetaState)
				dump (sb, (MetaState) s, indent + "    ");
			else
				sb.append (indent).append ("    - ").append (s.getName ()).append (" [").append (s.getId ()).append ("]")
				  .append ((s == m.getInitialState ()) ? " (initial)" : "")
				  .append ((s.getCode ().trim ().length () > 0) ? "" : " (no code)").append ("\n");
			for (Transition t : s.getTransitions ())
				sb.append (indent).append ("        -> ").append (t.getName ()).append (" [").append (t.getId ()).append ("] to ")
				  .append ((t.getArrivalState () != null) ? t.getArrivalState ().getName () : "?")
				  .append (" prio ").append (t.getPriority ())
				  .append ((t.getTestCode ().trim ().length () > 0) ? "" : " (no test)").append ("\n");
		}
		for (Transition t : m.getTransitions ())
			sb.append (indent).append ("    => ").append (t.getName ()).append (" to ")
			  .append ((t.getArrivalState () != null) ? t.getArrivalState ().getName () : "?").append ("\n");
	}
}
