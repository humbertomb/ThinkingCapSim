/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

/**
 * A Lua script already read: the tree it was read into, kept so that it is read
 * once and run on every cycle of the machine.
 */
public class LuaScript
{
	protected String				name;						// what it is called in the messages
	protected String				source;
	protected LuaAst.Block			block;

	public LuaScript (String source, String name)
	{
		this.name	= (name != null) ? name : "script";
		this.source	= (source != null) ? source : "";
		this.block	= LuaParser.parse (this.source, this.name);
	}

	public final String				name ()			{ return name; }
	public final String				source ()		{ return source; }
	public final LuaAst.Block		block ()		{ return block; }

	/** Whether the script does nothing at all (empty or only comments). */
	public final boolean			isEmpty ()		{ return block.stats.isEmpty (); }

	public String toString ()						{ return name + " (" + block.stats.size () + " statements)"; }
}
