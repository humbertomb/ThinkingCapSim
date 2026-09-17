/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

/**
 * Reference to one editable element of a robot description: a kind, the index
 * inside its collection and, for a sensor, the family it belongs to. The
 * platform, the kinematics and the other properties use index 0.
 */
public class RobotItem
{
	static public final int		PLATFORM	= 0;		// name, radius, drawings
	static public final int		KINEMATICS	= 1;		// drive model and its parameters
	static public final int		LINE		= 2;		// a segment of the robot drawing
	static public final int		BUMPER		= 3;
	static public final int		SENSOR		= 4;		// one sensor of a family
	static public final int		FAMILY		= 5;		// the parameters shared by a family of sensors
	static public final int		EXTRA		= 6;		// the properties the model does not describe
	static public final int		WHEEL		= 7;		// one wheel of the drive train

	static public final int		NKINDS		= 8;

	static public final String[]	NAMES	= { "Platform", "Kinematics", "Drawing line", "Bumper", "Sensor", "Sensors",
												"Other properties", "Wheel" };

	public int			kind;
	public int			index;
	public String		family;						// only for SENSOR and FAMILY

	public RobotItem (int kind, int index)					{ this (kind, index, null); }

	public RobotItem (int kind, int index, String family)
	{
		this.kind	= kind;
		this.index	= index;
		this.family	= family;
	}

	public boolean equals (Object o)
	{
		if (!(o instanceof RobotItem))			return false;
		RobotItem	it = (RobotItem) o;
		return (it.kind == kind) && (it.index == index)
				&& ((it.family == null) ? (family == null) : it.family.equals (family));
	}

	public int hashCode ()
	{
		return kind * 100003 + index * 31 + ((family != null) ? family.hashCode () : 0);
	}

	public String toString ()
	{
		return NAMES[kind] + ((family != null) ? " " + family : "") + " " + index;
	}
}
