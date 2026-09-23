/*
 * (c) 2005 Daniel Garcia Nebot, Elad Rodriguez Alvaro, Miguel Cazorla
 * (c) 2026 Humberto Martinez Barbera (ported to ThinkingCap)
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tclib.behaviours.lua.interpreter.LuaScript;

/**
 * One state of a hierarchical machine: what the robot does while it is there
 * (its Lua script, the <code>state&lt;id&gt;.acc</code> file) and the
 * transitions that take it somewhere else.
 *
 * Where it is drawn (x, y) is kept as the editor left it, so that the file
 * comes back the same.
 *
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @author Miguel Cazorla
 * @version 0.3
 */
public class State
{
	/** How the file of the script of a state is named. */
	static public final String		SUFFIX		= ".acc";

	// Verification
	protected boolean				verified;
	protected boolean				correct;
	protected String				error;

	// Identity
	protected String				name;
	protected int					id;

	// Where it is drawn (pixels, as the editor placed it)
	protected int					x;
	protected int					y;

	// What leaves this state
	protected List<Transition>		transitions;

	// What the robot does while it is here
	protected String				code;						// the text of the script
	protected LuaScript				script;						// the script, once read
	protected String				codeError;					// why it could not be read, if it could not

	/** Compares two transitions by priority, the most urgent one first. */
	static public final Comparator<Transition>	BY_PRIORITY = new Comparator<Transition> ()
	{
		public int compare (Transition a, Transition b)			{ return a.getPriority () - b.getPriority (); }
	};

	// Constructors
	public State (int i)
	{
		this (null, i, 0, 0);
	}

	public State (String n, int i)
	{
		this (n, i, 0, 0);
	}

	public State (String n, int i, int x, int y)
	{
		this.id				= i;
		this.name			= (n != null) ? n : "";
		this.error			= "";
		this.x				= x;
		this.y				= y;
		this.transitions	= new ArrayList<Transition> ();
		this.code			= "";
	}

	/* ---------------- identity ---------------- */

	public void setName (String n)				{ this.name = n; }
	public String getName ()					{ return this.name; }
	public int getId ()							{ return this.id; }
	public void setId (int i)					{ this.id = i; }

	public void setVerified (boolean v)			{ this.verified = v; }
	public boolean isVerified ()				{ return this.verified; }
	public String getError ()					{ return this.error; }

	/* ---------------- where it is drawn ---------------- */

	public int getX ()							{ return this.x; }
	public int getY ()							{ return this.y; }
	public void setPosition (int x, int y)		{ this.x = x; this.y = y; }

	/* ---------------- the names the old C++ generator used ---------------- */

	public String getWhatToDofunctionName ()	{ return "whatToDo_in_state_" + this.name; }
	public String getConstName ()				{ return "state_" + this.name; }
	public String getObjName ()					{ return "obj_" + this.name; }

	/* ---------------- transitions ---------------- */

	public int getTransitionsSize ()			{ return (this.transitions == null) ? 0 : this.transitions.size (); }
	public Transition getTransition (int i)		{ return this.transitions.get (i); }
	public List<Transition> getTransitions ()	{ return this.transitions; }
	public void addTransition (Transition t)	{ this.transitions.add (t); }
	public void removeTransition (Transition t)	{ this.transitions.remove (t); }
	public void cloneTransitions (List<Transition> t)	{ this.transitions = new ArrayList<Transition> (t); }

	/** Leaves the transitions in the order they are tried: the most urgent one first. */
	public void sortTransitions ()
	{
		Collections.sort (this.transitions, BY_PRIORITY);
	}

	/* ---------------- the script of the state ---------------- */

	/** Where the script of this state is, in the folder of its machine. */
	public String getPathCode (String path)
	{
		return new File (path, "state" + this.id + SUFFIX).getPath ();
	}

	public String getCode ()					{ return this.code; }
	public void setCode (String c)				{ this.code = (c != null) ? c : "";	this.script = null; }

	public LuaScript getScript ()				{ return this.script; }

	/**
	 * Reads the script of this state from the folder of the machine. A state with
	 * no file of its own simply does nothing.
	 */
	public void loadCode (String path)
	{
		File		f = new File (getPathCode (path));

		this.code	= "";
		this.script	= null;
		if (!f.exists ())						return;
		try { this.code = new String (Files.readAllBytes (f.toPath ()), StandardCharsets.UTF_8); }
		catch (Exception e) { System.out.println ("  [HFSM] Cannot read <" + f + ">: " + e.getMessage ()); }
	}

	/**
	 * Reads the script into its tree, so that running it costs nothing but running
	 * it. A script that is not Lua at all (the machines of the old C++ robots carry
	 * some) is left out and the reason kept, rather than stopping the whole machine.
	 */
	public void compile ()
	{
		this.script		= null;
		this.codeError	= null;
		if ((this.code == null) || (this.code.trim ().length () == 0))		return;
		try { this.script = new LuaScript (this.code, "state " + this.name); }
		catch (RuntimeException e) { this.codeError = e.getMessage (); }
	}

	/** Why the script of this state could not be read, or null when it could. */
	public String getCodeError ()				{ return this.codeError; }

	/* ---------------- verification ---------------- */

	/**
	 * Whether the state is correct: every transition of it arrives somewhere and no
	 * two of them are called the same.
	 */
	public boolean isCorrect (String meta, List<Object> unreachableStatesList, List<Transition> totalTransList)
	{
		this.error		= "";
		this.correct	= this.verificateTransitions (meta, unreachableStatesList, totalTransList);
		this.verified	= true;
		return this.correct;
	}

	protected boolean verificateTransitions (String meta, List<Object> unreachableStatesList, List<Transition> totalTransList)
	{
		boolean			r = true;

		for (int i = 0; i < this.transitions.size (); i++)
		{
			Transition	temp = this.transitions.get (i);

			if (temp.getArrivalState () == null)
			{
				this.error	+= "ERROR in Meta State '" + meta + "' : Transition '" + temp.getName () + "' has no arrival state.\n";
				r	= false;
			}
			else
				unreachableStatesList.remove (temp.getArrivalState ());

			for (int j = i; j < totalTransList.size (); j++)
				if ((temp != totalTransList.get (j)) && temp.getName ().equals (totalTransList.get (j).getName ()))
				{
					this.error	+= "ERROR in Meta State '" + meta + "' : Transition name repeated: '" + temp.getName () + "'.\n";
					r	= false;
					break;
				}
			totalTransList.add (temp);
		}
		return r;
	}

	/* ---------------- kind ---------------- */

	/** Whether something is a plain state and not a meta state. */
	static public boolean isState (Object o)
	{
		return (o instanceof State) && !(o instanceof MetaState);
	}

	public String toString ()					{ return this.name; }
}
