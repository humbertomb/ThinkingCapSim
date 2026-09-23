/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.util.ArrayList;
import java.util.List;

import tclib.behaviours.hfsm.MetaState;
import tclib.behaviours.hfsm.State;
import tclib.behaviours.hfsm.Transition;

/**
 * What the editor does to a machine of states: adding, removing and joining
 * states and transitions, and the housekeeping that goes with it -- the ids are
 * unique in the whole machine, as the file refers to them by number, and a
 * transition never arrives at a state that is no longer there.
 */
public class HFSMEdit
{
	private HFSMEdit ()						{ }

	/* ------------------------------------------------------------------ */
	/* Looking around                                                      */
	/* ------------------------------------------------------------------ */

	/** Every state and meta state of the machine, the root itself included. */
	static public List<State> all (MetaState root)
	{
		List<State>		out = new ArrayList<State> ();

		out.add (root);
		gather (root, out);
		return out;
	}

	static private void gather (MetaState m, List<State> out)
	{
		for (State s : m.getStatesList ())
		{
			out.add (s);
			if (s instanceof MetaState)			gather ((MetaState) s, out);
		}
	}

	/** Every transition of the machine. */
	static public List<Transition> transitions (MetaState root)
	{
		List<Transition>	out = new ArrayList<Transition> ();

		for (State s : all (root))
			out.addAll (s.getTransitions ());
		return out;
	}

	/** The meta state that holds a state, or null for the root. */
	static public MetaState parent (MetaState root, State s)
	{
		for (State o : all (root))
			if ((o instanceof MetaState) && ((MetaState) o).getStatesList ().contains (s))
				return (MetaState) o;
		return null;
	}

	/** The state a transition leaves, or null when no state has it. */
	static public State origin (MetaState root, Transition t)
	{
		for (State s : all (root))
			if (s.getTransitions ().contains (t))		return s;
		return null;
	}

	/** An id no state and no transition of the machine uses. */
	static public int nextId (MetaState root)
	{
		int				id = 0;

		for (State s : all (root))				id = Math.max (id, s.getId ());
		for (Transition t : transitions (root))	id = Math.max (id, t.getId ());
		return id + 1;
	}

	/** A name no state and no transition of the machine uses: prefix, prefix2, prefix3 ... */
	static public String uniqueName (MetaState root, String prefix)
	{
		for (int i = 1; ; i++)
		{
			String		name = (i == 1) ? prefix : (prefix + i);

			if (!named (root, name))			return name;
		}
	}

	static private boolean named (MetaState root, String name)
	{
		for (State s : all (root))
			if (name.equals (s.getName ()))		return true;
		for (Transition t : transitions (root))
			if (name.equals (t.getName ()))		return true;
		return false;
	}

	/* ------------------------------------------------------------------ */
	/* Adding                                                              */
	/* ------------------------------------------------------------------ */

	/** A new state in a meta state, the first one of it being its initial state. */
	static public State addState (MetaState root, MetaState parent, int x, int y)
	{
		State			s = new State (uniqueName (root, "State"), nextId (root), x, y);

		parent.addState (s);
		if (parent.getInitialState () == null)		parent.setInitialState (s);
		parent.stateCountToName++;
		return s;
	}

	/** A new meta state in a meta state, empty for now. */
	static public MetaState addMetaState (MetaState root, MetaState parent, int x, int y)
	{
		MetaState		ms = new MetaState (uniqueName (root, "Meta"), nextId (root), x, y);

		parent.addState (ms);
		if (parent.getInitialState () == null)		parent.setInitialState (ms);
		parent.metaStateCountToName++;
		return ms;
	}

	/** A new transition from one state to another, drawn halfway between them. */
	static public Transition addTransition (MetaState root, MetaState level, State from, State to)
	{
		int				x = (from.getX () + ((to != null) ? to.getX () : from.getX () + 160)) / 2;
		int				y = (from.getY () + ((to != null) ? to.getY () : from.getY ())) / 2;

		return addTransition (root, level, from, to, x, y);
	}

	static public Transition addTransition (MetaState root, MetaState level, State from, State to, int x, int y)
	{
		Transition		t = new Transition (to, uniqueName (root, "Trans"), nextId (root), x, y);

		if (from != null)						from.addTransition (t);
		if (level != null)						level.transitionCountToName++;
		return t;
	}

	/* ------------------------------------------------------------------ */
	/* Changing                                                            */
	/* ------------------------------------------------------------------ */

	/** Makes a transition leave another state. */
	static public void setOrigin (MetaState root, Transition t, State from)
	{
		State			old = origin (root, t);

		if (old == from)						return;
		if (old != null)						old.removeTransition (t);
		if (from != null)						from.addTransition (t);
	}

	/**
	 * Removes a state, and with it what it holds and every transition that leaves
	 * it or arrives at it.
	 */
	static public void remove (MetaState root, State s)
	{
		MetaState		parent = parent (root, s);

		if (parent == null)						return;						// the root itself is not removed
		parent.removeState (s);
		for (Transition t : transitions (root))
			if ((t.getArrivalState () == s) || inside (s, t.getArrivalState ()))
				remove (root, t);
		if (parent.getInitialState () == null)
			for (State o : parent.getStatesList ())	{ parent.setInitialState (o); break; }
	}

	/** Whether a state is the one given or somewhere under it. */
	static private boolean inside (State s, State target)
	{
		if ((target == null) || !(s instanceof MetaState))		return false;
		for (State o : all ((MetaState) s))
			if (o == target)					return true;
		return false;
	}

	/** Removes a transition from wherever it leaves. */
	static public void remove (MetaState root, Transition t)
	{
		State			from = origin (root, t);

		if (from != null)						from.removeTransition (t);
	}

	/** Makes a state the one its meta state starts at. */
	static public void setInitial (MetaState root, State s)
	{
		MetaState		parent = parent (root, s);

		if (parent != null)						parent.setInitialState (s);
	}
}
