/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * An animated object of the world: a {@link WMObject} (icon, pose, colour,
 * 3D shape) identified by a label, whose pose evolves during the simulation
 * according to a dynamic behaviour, implemented by the class named in
 * {@link #dynamics}, and to its physical parameters (virtual radius, movement
 * type, speed, acceleration, mass and collision coefficients). More
 * parameters will be added as the dynamics are defined.
 *
 * JSON: {label, icon, x, y, z, orientation, radius, color [, shape, usecolor]
 * [, dynamics], movement, speed, acceleration, mass, coef_res, coef_fric}
 */
public class WMAObject extends WMObject
{
	/** How the object moves: STATIC = it does not move by itself, ACCELERATED = speed / acceleration model. */
	public enum Movement			{ STATIC, ACCELERATED }

	static public final Movement	DEFAULT_MOVEMENT	= Movement.STATIC;
	static public final double		DEF_RADIUS			= 0.5;		// virtual radius (m)
	static public final double		DEF_COEF_RES		= 0.75;		// restitution coefficient
	static public final double		DEF_COEF_FRIC		= 0.001;	// friction coefficient

	/** Decimals kept of the friction, which is a small number worked out to millionths. */
	static public final int			FRIC_DECIMALS		= 6;

	public String				dynamics;						// Class implementing the dynamic behaviour (may be null)
	public double				radius		= DEF_RADIUS;		// Virtual radius for the simulation (m)
	public Movement				movement	= DEFAULT_MOVEMENT;
	public double				speed;							// Simulated speed (m/s)
	public double				acceleration;					// Simulated acceleration (m/s2)
	public double				mass;							// Mass (kg)
	public double				coef_res	= DEF_COEF_RES;		// Restitution coefficient
	public double				coef_fric	= DEF_COEF_FRIC;	// Friction coefficient

	/* Constructors */

	public WMAObject ()
	{
		super ();
	}

	public WMAObject (JsonObject o, List<WMIcon> icons)
	{
		super (o, icons);
		label			= World.getString (o, "label", "aobject");
		dynamics		= World.getString (o, "dynamics", null);
		if ((dynamics != null) && (dynamics.trim ().length () == 0))		dynamics = null;
		radius			= World.getDouble (o, "radius", DEF_RADIUS);
		movement		= parseMovement (World.getString (o, "movement", null));
		speed			= World.getDouble (o, "speed", 0.0);
		acceleration	= World.getDouble (o, "acceleration", 0.0);
		mass			= World.getDouble (o, "mass", 0.0);
		coef_res		= World.getDouble (o, "coef_res", DEF_COEF_RES);
		coef_fric		= World.getDouble (o, "coef_fric", DEF_COEF_FRIC);
	}

	/* Accessors */

	/** True when the object has a dynamic behaviour assigned. */
	public boolean isAnimated ()			{ return dynamics != null; }

	/** True when the object moves by itself (its speed, acceleration, mass and coefficients apply). */
	public boolean isMoving ()				{ return movement != Movement.STATIC; }

	/** Parses a movement type name (case-insensitive); unknown names give the default. */
	static public Movement parseMovement (String name)
	{
		if (name == null)				return DEFAULT_MOVEMENT;
		try { return Movement.valueOf (name.trim ().toUpperCase ()); } catch (IllegalArgumentException e) { return DEFAULT_MOVEMENT; }
	}

	/* Persistence */

	public JsonObject toJson ()
	{
		JsonObject	base = super.toJson ();
		JsonObject	o = new JsonObject ();
		o.addProperty ("label", label);
		// the object fields, with radius right after orientation
		for (String key : base.keySet ())
		{
			o.add (key, base.get (key));
			if (key.equals ("orientation"))		o.addProperty ("radius", World.num (radius));
		}
		if (dynamics != null)		o.addProperty ("dynamics", dynamics);
		o.addProperty ("movement", movement.name ());
		o.addProperty ("speed", World.num (speed));
		o.addProperty ("acceleration", World.num (acceleration));
		o.addProperty ("mass", World.num (mass));
		o.addProperty ("coef_res", World.num (coef_res));
		o.addProperty ("coef_fric", World.num (coef_fric, FRIC_DECIMALS));
		return o;
	}
}
