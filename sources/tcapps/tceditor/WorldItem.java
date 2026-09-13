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
	static public final int		WALL		= 3;
	static public final int		OBJECT		= 4;
	static public final int		AOBJECT		= 5;		// animated objects
	static public final int		CONNECTOR		= 6;
	static public final int		BEACON		= 7;
	static public final int		CBEACON		= 8;
	static public final int		WAYPOINT	= 9;
	static public final int		DOCK		= 10;
	static public final int		START		= 11;
	static public final int		ICON		= 12;		// icon definitions (local coordinates, no position in the world)
	static public final int		DEFAULTS	= 13;

	static public final int		NKINDS		= 14;

	static public final String[]	NAMES	= {
		"Zone", "Forbidden area", "Path point", "Wall", "Object", "Animated object", "Connector",
		"Strip beacon", "Cylindrical beacon", "Waypoint", "Dock", "Start point", "Icon", "Defaults"
	};

	static public final String[]	PLURALS	= {
		"Zones", "Forbidden areas", "Path points", "Walls", "Objects", "Animated objects", "Connectors",
		"Strip beacons", "Cylindrical beacons", "Waypoints", "Docks", "Start points", "Icons", "Defaults"
	};

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
