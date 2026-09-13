/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * An animated object of the world: a {@link WMObject} (icon, pose, colour,
 * 3D shape) whose pose evolves during the simulation according to a dynamic
 * behaviour, implemented by the class named in {@link #dynamics}. The
 * parameters of that behaviour will be added as the dynamics are defined.
 *
 * JSON: the fields of an object plus <code>dynamics</code> (class name).
 */
public class WMAObject extends WMObject
{
	public String				dynamics;		// Class implementing the dynamic behaviour (may be null: static)

	/* Constructors */

	public WMAObject ()
	{
		super ();
	}

	public WMAObject (JsonObject o, List<WMIcon> icons)
	{
		super (o, icons);
		dynamics	= World.getString (o, "dynamics", null);
		if ((dynamics != null) && (dynamics.trim ().length () == 0))		dynamics = null;
	}

	/* Accessors */

	/** True when the object has a dynamic behaviour assigned. */
	public boolean isAnimated ()			{ return dynamics != null; }

	/* Persistence */

	public JsonObject toJson ()
	{
		JsonObject	o = super.toJson ();
		if (dynamics != null)		o.addProperty ("dynamics", dynamics);
		return o;
	}
}
