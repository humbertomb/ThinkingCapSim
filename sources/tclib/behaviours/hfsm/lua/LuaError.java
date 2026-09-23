/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

/** What went wrong while a Lua script was read or run, and where. */
public class LuaError extends RuntimeException
{
	private static final long		serialVersionUID = 1L;

	protected String				chunk;						// which script
	protected int					line;						// which line of it (0 when not known)

	public LuaError (String message)
	{
		this (null, 0, message);
	}

	public LuaError (String chunk, int line, String message)
	{
		super (((chunk != null) ? chunk : "?") + ((line > 0) ? (":" + line) : "") + ": " + message);

		this.chunk	= chunk;
		this.line	= line;
	}

	public final String		chunk ()			{ return chunk; }
	public final int		line ()				{ return line; }
}
