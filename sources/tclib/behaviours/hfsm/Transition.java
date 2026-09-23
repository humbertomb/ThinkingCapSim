/*
 * (c) 2005 Daniel Garcia Nebot, Elad Rodriguez Alvaro, Miguel Cazorla
 * (c) 2026 Humberto Martinez Barbera (ported to ThinkingCap)
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import tclib.behaviours.lua.interpreter.LuaScript;

/**
 * One transition of a hierarchical machine: when it is taken (its test script,
 * <code>trans&lt;id&gt;_Test.acc</code>, which returns true or false), what is
 * done as it is taken (<code>trans&lt;id&gt;_Do.acc</code>, which may be
 * missing) and where it arrives.
 *
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @version 0.2
 */
public class Transition
{
	/** How the files of the scripts of a transition are named. */
	static public final String		TEST_SUFFIX	= "_Test.acc";
	static public final String		DO_SUFFIX	= "_Do.acc";

	// Identity
	protected String				name;
	protected int					id;
	protected int					priority;					// the lower the number, the sooner it is tried

	// Where it arrives (a State or a MetaState)
	protected State					arrivalState;

	// Where it is drawn (pixels, as the editor placed it)
	protected int					x;
	protected int					y;

	// What decides it, and what it does when it is taken
	protected String				testCode;
	protected String				doCode;
	protected LuaScript				testScript;
	protected LuaScript				doScript;
	protected String				codeError;					// why a script could not be read, if it could not

	// Constructors
	public Transition (int i)
	{
		this (null, null, i, 0, 0);
	}

	public Transition (State a, String n, int i)
	{
		this (a, n, i, 0, 0);
	}

	public Transition (State a, String n, int i, int x, int y)
	{
		this.id				= i;
		this.arrivalState	= a;
		this.name			= (n != null) ? n : "";
		this.priority		= 1;
		this.x				= x;
		this.y				= y;
		this.testCode		= "";
		this.doCode			= "";
	}

	/* ---------------- identity ---------------- */

	public void setName (String n)				{ this.name = n; }
	public String getName ()					{ return this.name; }
	public int getId ()							{ return this.id; }
	public void setId (int i)					{ this.id = i; }

	public void setPriority (int n)				{ this.priority = n; }
	public int getPriority ()					{ return this.priority; }

	public void setArrivalState (State a)		{ this.arrivalState = a; }
	public State getArrivalState ()				{ return this.arrivalState; }

	public int getX ()							{ return this.x; }
	public int getY ()							{ return this.y; }
	public void setPosition (int x, int y)		{ this.x = x; this.y = y; }

	/* ---------------- the names the old C++ generator used ---------------- */

	public String getTestfunctionName ()		{ return "transition_" + this.name + "_test"; }
	public String getDofunctionName ()			{ return "transition_" + this.name + "_do"; }

	/* ---------------- the scripts of the transition ---------------- */

	public String getPathTestCode (String path)	{ return new File (path, "trans" + this.id + TEST_SUFFIX).getPath (); }
	public String getPathDoCode (String path)	{ return new File (path, "trans" + this.id + DO_SUFFIX).getPath (); }

	public String getTestCode ()				{ return this.testCode; }
	public String getDoCode ()					{ return this.doCode; }
	public void setTestCode (String c)			{ this.testCode = (c != null) ? c : "";	this.testScript = null; }
	public void setDoCode (String c)			{ this.doCode = (c != null) ? c : "";	this.doScript = null; }

	public LuaScript getTestScript ()			{ return this.testScript; }
	public LuaScript getDoScript ()				{ return this.doScript; }

	/** Reads both scripts from the folder of the machine; a missing Do is no error. */
	public void loadCode (String path)
	{
		this.testCode	= read (getPathTestCode (path));
		this.doCode		= read (getPathDoCode (path));
		this.testScript	= null;
		this.doScript	= null;
	}

	static protected String read (String path)
	{
		File		f = new File (path);

		if (!f.exists ())						return "";
		try { return new String (Files.readAllBytes (f.toPath ()), StandardCharsets.UTF_8); }
		catch (Exception e) { System.out.println ("  [HFSM] Cannot read <" + f + ">: " + e.getMessage ()); }
		return "";
	}

	/**
	 * Reads both scripts into their trees. A transition with no test is never taken,
	 * and one whose script is not Lua at all keeps the reason instead of stopping the
	 * machine.
	 */
	public void compile ()
	{
		this.testScript	= null;
		this.doScript	= null;
		this.codeError	= null;
		if ((this.testCode != null) && (this.testCode.trim ().length () > 0))
			try { this.testScript = new LuaScript (this.testCode, "transition " + this.name + " (test)"); }
			catch (RuntimeException e) { this.codeError = e.getMessage (); }
		if ((this.doCode != null) && (this.doCode.trim ().length () > 0))
			try { this.doScript = new LuaScript (this.doCode, "transition " + this.name + " (do)"); }
			catch (RuntimeException e) { this.codeError = ((this.codeError != null) ? (this.codeError + "; ") : "") + e.getMessage (); }
	}

	/** Why a script of this transition could not be read, or null when both could. */
	public String getCodeError ()				{ return this.codeError; }

	static public boolean isTransition (Object o)			{ return o instanceof Transition; }

	public String toString ()					{ return this.name; }
}
