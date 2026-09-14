/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.runtime.thread;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The configuration of one module of a robot: the values the module declares
 * in the deployment (CLASS, MODE, EXTIME, ... plus whatever the module reads),
 * the properties shared by every module of its robot, and the name of the
 * robot itself.
 *
 * A module only sees its own values, so it no longer depends on the prefix it
 * happens to run under (MOD1, COO, ROB, ...): what an architecture file wrote
 * as <code>MOD1CELL</code> is simply the value <code>CELL</code> here. Keys not
 * found in the module fall back to the properties of the robot.
 */
public class ModuleConfig
{
	protected String						robot;			// Robot the module belongs to (may be null)
	protected String						name;			// Name of the module (its INFO)
	protected Map<String, String>			values;			// Values of the module (a copy: the runtime may change them)
	protected Map<String, String>			shared;			// Properties common to the robot (read only)

	/* Constructors */

	public ModuleConfig (String robot, String name, Map<String, String> values, Map<String, String> shared)
	{
		this.robot	= robot;
		this.name	= (name != null) ? name : "Module";
		this.values	= (values != null) ? new LinkedHashMap<String, String> (values) : new LinkedHashMap<String, String> ();
		this.shared	= (shared != null) ? shared : new LinkedHashMap<String, String> ();
	}

	public ModuleConfig (String robot, String name, Map<String, String> values)
	{
		this (robot, name, values, null);
	}

	/**
	 * Configuration of a module taken from a flat set of properties (a legacy
	 * architecture file, a robot description): the keys that start with
	 * <code>preffix</code>, without it. A null or empty prefix takes them all.
	 */
	static public ModuleConfig fromProperties (String robot, String name, String preffix, java.util.Properties props)
	{
		Map<String, String>		values = new LinkedHashMap<String, String> ();
		String					pre = (preffix != null) ? preffix : "";

		for (String key : props.stringPropertyNames ())
			if (key.startsWith (pre))		values.put (key.substring (pre.length ()), props.getProperty (key));

		return new ModuleConfig (robot, name, values);
	}

	/* Accessors */

	/** Name of the robot this module belongs to (the ROBNAME of the old ADF). */
	public String	robot ()					{ return robot; }
	/** Name of the module, as the deployment calls it. */
	public String	name ()						{ return name; }
	public boolean	has (String key)			{ return get (key) != null; }
	public Set<String> keys ()					{ return values.keySet (); }

	/** Value of a key of this module, or of the robot when the module does not declare it; null when neither does. */
	public String get (String key)
	{
		String		value = values.get (key);
		return (value != null) ? value : shared.get (key);
	}

	public String get (String key, String def)
	{
		String		value = get (key);
		return (value != null) ? value : def;
	}

	public int getInt (String key, int def)
	{
		try { return Integer.parseInt (get (key).trim ()); } catch (Exception e) { return def; }
	}

	public long getLong (String key, long def)
	{
		try { return Long.parseLong (get (key).trim ()); } catch (Exception e) { return def; }
	}

	public double getDouble (String key, double def)
	{
		try { return Double.parseDouble (get (key).trim ()); } catch (Exception e) { return def; }
	}

	public boolean getBoolean (String key, boolean def)
	{
		String		value = get (key);
		if (value == null)				return def;
		value = value.trim ();
		if (value.equalsIgnoreCase ("true"))		return true;
		if (value.equalsIgnoreCase ("false"))		return false;
		return def;
	}

	/** Changes a value of the module at runtime (the world imposed by the simulator, for instance). */
	public void set (String key, String value)
	{
		if (value == null)		values.remove (key);
		else					values.put (key, value);
	}

	public String toString ()
	{
		return name + ((robot != null) ? "@" + robot : "") + " " + values;
	}
}
