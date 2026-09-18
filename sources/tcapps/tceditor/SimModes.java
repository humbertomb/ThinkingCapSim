/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tcapps.tcsimulator.simulator.Simulator;

/**
 * How the simulator works the readings of a family of sensors out: the ways it
 * knows, by name, so that one is chosen from what there is instead of a number
 * being typed in.
 *
 * What each name stands for is the constant of {@link Simulator} the property
 * of the description (MODESON, MODEIR, MODELRF, MODELSB) is read as, so a
 * description written by hand with a number the simulator does not know shows
 * that number rather than losing it.
 */
public class SimModes
{
	static private final Map<String, Map<Integer, String>>	MODES = modes ();

	static private Map<String, Map<Integer, String>> modes ()
	{
		Map<String, Map<Integer, String>>	m = new LinkedHashMap<String, Map<Integer, String>> ();

		// a sonar: the cone, the lobes of Gallardo (Watt & Watt), or the exact distance
		m.put ("son", of (Simulator.S_GEOM, "Geometric", Simulator.S_GALLARDO, "Gallardo", Simulator.S_EXACT, "Exact"));
		// an infrared: the cone, the exact distance, or the curve of a Sharp GP2D02
		m.put ("ir", of (Simulator.I_GEOM, "Geometric", Simulator.I_EXACT, "Exact", Simulator.I_SHARP, "Sharp GP2D02"));
		// a laser: the cone, the exact distance, or the exact one with gaussian noise
		m.put ("lrf", of (Simulator.LRF_GEOM, "Geometric", Simulator.LRF_EXACT, "Exact", Simulator.LRF_GAUSS, "Gaussian"));
		m.put ("lsb", of (Simulator.LSB_GEOM, "Geometric", Simulator.LSB_EXACT, "Exact", Simulator.LSB_GAUSS, "Gaussian"));
		return m;
	}

	static private Map<Integer, String> of (int a, String na, int b, String nb, int c, String nc)
	{
		Map<Integer, String>	m = new LinkedHashMap<Integer, String> ();

		m.put (Integer.valueOf (a), na);
		m.put (Integer.valueOf (b), nb);
		m.put (Integer.valueOf (c), nc);
		return m;
	}

	/** True when the simulator works the readings of this family out in a way that can be chosen. */
	static public boolean has (String fam)						{ return MODES.containsKey (fam); }

	/** The ways it knows for a family, by name and in the order they are offered. */
	static public List<String> names (String fam)
	{
		Map<Integer, String>	m = MODES.get (fam);

		return (m == null) ? new ArrayList<String> () : new ArrayList<String> (m.values ());
	}

	/** The name of one way, or the number itself when it is not one the simulator knows. */
	static public String name (String fam, int mode)
	{
		Map<Integer, String>	m = MODES.get (fam);
		String					name = (m != null) ? m.get (Integer.valueOf (mode)) : null;

		return (name != null) ? name : String.valueOf (mode);
	}

	/** The way a name stands for, or what the text says when it names none. */
	static public int mode (String fam, String name)
	{
		Map<Integer, String>	m = MODES.get (fam);

		if ((m != null) && (name != null))
			for (Map.Entry<Integer, String> e : m.entrySet ())
				if (e.getValue ().equalsIgnoreCase (name.trim ()))		return e.getKey ().intValue ();
		try { return Integer.parseInt (name.trim ()); }	catch (Exception e)		{ return 0; }
	}
}
