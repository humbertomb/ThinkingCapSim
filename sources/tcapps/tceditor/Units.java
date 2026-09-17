/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.util.HashMap;
import java.util.Map;

/**
 * The unit a property is said in, so that an editor can show it beside the
 * value: a distance in metres, an angle in degrees, and nothing at all for the
 * properties that are a name, a file, a count or a plain factor.
 *
 * A property is named the same way in every editor (the x of a wall and the x
 * of a beacon are both metres), so one table serves them all.
 */
public class Units
{
	static public final String				NONE	= "";

	static private final Map<String, String>	UNITS = new HashMap<String, String> ();

	static
	{
		// where something is and how big it is
		metres ("radius", "diameter", "width", "height", "length", "base", "rwheel", "wheel diameter",
				"x", "y", "z", "x1", "y1", "z1", "x2", "y2", "z2",
				"xi", "yi", "xf", "yf",
				"path x1", "path y1", "path z1", "path x2", "path y2", "path z2",
				"wall width", "wall height", "connector width", "connector height");
		// where something looks at and how wide it sees
		degrees ("theta", "orientation", "elevation", "cone", "hfov", "vfov", "reflect");
		// what a sensor reaches
		metres ("rho", "range max", "range min");

		// how fast it goes
		put ("m/s", "vmax", "speed");
		put ("deg/s", "rmax", "samax");
		put ("m/s2", "lamax", "ldmax", "acceleration");

		// the rest
		put ("ms", "dtime");
		put ("m/s", "odom et");					// the odometry errors of the simulation, as standard deviations
		put ("deg/s", "odom er");
		put ("kg", "mass");
		put ("rpm", "max rpm");
		degrees ("max steering");
		put ("deg/s", "max turning");
	}

	/** The unit of a property, or an empty string when it has none. */
	static public String of (String name)
	{
		String		u;

		if (name == null)					return NONE;
		u	= UNITS.get (name.trim ());
		return (u != null) ? u : NONE;
	}

	static private void metres (String... names)				{ put ("m", names); }
	static private void degrees (String... names)				{ put ("deg", names); }

	static private void put (String unit, String... names)
	{
		for (String name : names)		UNITS.put (name, unit);
	}
}
