/*
 * (c) 2004-2026 Humberto Martinez Barbera
 */
 
package tcapps.tcsimulator.simulator.objects;

import java.lang.reflect.Constructor;

import tc.shared.world.WMAObject;

import wucore.utils.geom.Line2;

/**
 * An object handled by the simulator: an animated object of the world
 * ({@link WMAObject}) plus its simulation state. The plain SimObject does not
 * move by itself; the subclasses implement the dynamic behaviours
 * ({@link SimMobileObject}, {@link SimCargo}), and the world object names the
 * one to use in its <code>dynamics</code> attribute.
 */
public class SimObject
{
	public WMAObject			odesc;			// The world object (its pose is updated by the simulation)
	public double				radius;			// Bounding radius used for collisions and picking (m)
	public int 					idsimul;		// Index in the visualisation

	// Constructors
	public SimObject (WMAObject odesc)
	{
		this.odesc	= odesc;
		radius		= (odesc.radius > 0.0) ? odesc.radius : iconRadius (odesc);
	}

	/**
	 * Creates the simulation object for a world object: an instance of the
	 * class named in <code>odesc.dynamics</code> (a SimObject subclass with a
	 * constructor taking a WMAObject), or a static SimObject when there is no
	 * dynamics or the class cannot be used.
	 */
	static public SimObject create (WMAObject odesc)
	{
		if (odesc.dynamics == null)			return new SimObject (odesc);
		try
		{
			Class<?>		tclass = Class.forName (odesc.dynamics);
			Constructor<?>	cons = tclass.getConstructor (WMAObject.class);
			return (SimObject) cons.newInstance (odesc);
		} catch (Exception e)
		{
			System.out.println ("--[SimObject] Cannot create dynamics <" + odesc.dynamics + "> for object <" + odesc.label + ">: " + e + ". Using a static object");
			return new SimObject (odesc);
		}
	}

	/** Bounding radius of the local icon (fallback when the object has no radius). */
	static protected double iconRadius (WMAObject odesc)
	{
		double		r = 0.0;
		for (Line2 l : odesc.getLocalIcon ())
		{
			r = Math.max (r, Math.hypot (l.orig ().x (), l.orig ().y ()));
			r = Math.max (r, Math.hypot (l.dest ().x (), l.dest ().y ()));
		}
		return r;
	}

	public String toString ()
	{
		return "odesc=" + odesc.toRawString () + " radius=" + radius + " idsimul=" + idsimul;
	}
}
