/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua;

import java.util.Map;

import tclib.behaviours.lua.interpreter.LuaTable;

/**
 * What the scripts of a machine or a program speak to the robot through: a
 * table the interpreter knows by a name (<code>chaos</code>, for the soccer
 * robots), filled in by the controller before every cycle and read out of
 * afterwards. A machine of states ({@link tclib.behaviours.hfsm.HFSM}) runs
 * with any bridge, or with none, and what the table holds is the bridge's
 * business: this is only what the machine and the monitors need of it.
 */
public interface LuaBridge
{
	/** The global the scripts see the table as. */
	public String name ();

	/** The table itself. */
	public LuaTable table ();

	/** Forgets what the scripts commanded, before a new cycle. */
	public void clear ();

	/** The behaviour a script chose on this cycle, or null when none did. */
	public String behaviour ();

	/**
	 * The machine went into another state: what the state it left had chosen for
	 * as long as it lasted (its behaviour) is forgotten, so that a state that
	 * chooses none runs none.
	 */
	public void entered ();

	/** What the scripts left for one another, by name, for whoever looks at them while they run. */
	public Map<String, Object> globals ();

	/** What the scripts asked of the robot on this cycle, by name and in order, for whoever looks at them while they run. */
	public Map<String, Object> commands ();
}
