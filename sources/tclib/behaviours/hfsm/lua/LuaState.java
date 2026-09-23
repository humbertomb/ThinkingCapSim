/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A Lua interpreter: one table of globals and everything that runs in it, which
 * is what a machine of states has one of. The scripts of its states and
 * transitions are read once ({@link #load}) and run on every cycle
 * ({@link #run}), so that what one of them leaves in a global another one finds.
 *
 * The bridge to the robot is put in with {@link #set}, under the name the
 * scripts use for it (<code>chaos</code>).
 */
public class LuaState
{
	protected LuaTable				globals;
	protected LuaInterp				interp;

	public LuaState ()
	{
		globals	= new LuaTable ();
		interp	= new LuaInterp (globals);
		LuaLib.open (globals);
	}

	public final LuaTable			globals ()					{ return globals; }
	public final LuaInterp			interpreter ()				{ return interp; }

	/** Puts a value (a table, a function, a number) where the scripts can see it. */
	public void set (String name, Object value)					{ globals.set (name, value); }
	public Object get (String name)								{ return globals.get (name); }

	/** Reads a script, ready to be run as many times as needed. */
	public LuaScript load (String source, String name)			{ return new LuaScript (source, name); }

	/** Reads a script from a file. */
	public LuaScript loadFile (File f) throws java.io.IOException
	{
		return new LuaScript (new String (Files.readAllBytes (f.toPath ()), StandardCharsets.UTF_8), f.getName ());
	}

	/** Runs a script, and answers what it returns (nil when it returns nothing). */
	public Object run (LuaScript script)
	{
		if (script == null)					return null;
		return interp.run (script.block (), script.name ());
	}

	/** Runs a script and answers whether what it returned is true, as a test of a transition is read. */
	public boolean test (LuaScript script)
	{
		return Lua.truth (run (script));
	}

	/** Reads and runs a piece of text in one go (handy for the console and the tests). */
	public Object eval (String source)
	{
		return run (load (source, "=(load)"));
	}
}
