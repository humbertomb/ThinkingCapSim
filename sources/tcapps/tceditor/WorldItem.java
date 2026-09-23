/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

/**
 * Reference to one editable element of a World: a kind (walls, zones, ...) plus
 * its index inside the corresponding collection. The START point and the
 * default settings use index 0.
 */
public class WorldItem
{
	// Element kinds (order is also the drawing order, background first)
	static public final int		ZONE		= 0;
	static public final int		FAREA		= 1;
	static public final int		PATH		= 2;
	static public final int		MARKING		= 3;		// lines drawn on the floor (visual guides)
	static public final int		WALL		= 4;
	static public final int		OBJECT		= 5;
	static public final int		AOBJECT		= 6;		// animated objects
	static public final int		CONNECTOR		= 7;
	static public final int		BEACON		= 8;
	static public final int		CBEACON		= 9;
	static public final int		WAYPOINT	= 10;
	static public final int		DOCK		= 11;
	static public final int		START		= 12;
	static public final int		ICON		= 13;		// icon definitions (local coordinates, no position in the world)
	static public final int		GEOMETRY	= 14;		// default sizes and textures of the geometric elements
	static public final int		BEHAVIOUR	= 15;		// settings of the world as used by the simulation

	static public final int		NKINDS		= 16;

	/** First of the setting kinds: the kinds below this one are drawable elements. */
	static public final int		DEFAULTS	= GEOMETRY;

	static public final String[]	NAMES	= {
		"Zone", "Forbidden area", "Path point", "Marking", "Wall", "Object", "Animated object", "Connector",
		"Strip beacon", "Cylindrical beacon", "Waypoint", "Dock", "Start point", "Icon",
		"Default values", "Default values"
	};

	static public final String[]	PLURALS	= {
		"Zones", "Forbidden areas", "Path points", "Markings", "Walls", "Objects", "Animated objects", "Connectors",
		"Strip beacons", "Cylindrical beacons", "Waypoints", "Docks", "Start points", "Icons",
		"Default values", "Behaviours"
	};

	/** True for the kinds that hold settings of the world, not drawable elements. */
	static public boolean isSettings (int kind)		{ return (kind == GEOMETRY) || (kind == BEHAVIOUR); }

	/** True for the kinds that are objects (static or animated). */
	static public boolean isObject (int kind)		{ return (kind == OBJECT) || (kind == AOBJECT); }

	public int			kind;
	public int			index;

	public WorldItem (int kind, int index)
	{
		this.kind	= kind;
		this.index	= index;
	}

	public boolean equals (Object o)
	{
		if (!(o instanceof WorldItem))			return false;
		WorldItem	it = (WorldItem) o;
		return (it.kind == kind) && (it.index == index);
	}

	public int hashCode ()
	{
		return kind * 100003 + index;
	}

	public String toString ()
	{
		return NAMES[kind] + " " + index;
	}
}
