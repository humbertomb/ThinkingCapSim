/*
 * (c) 2002 Juan Pedro Canovas, Humberto Martinez Barbera
 * (c) 2003 Bernardo Canovas Segura
 * (c) 2004 Humberto Martinez Barbera
 */
 
package tcapps.tcsimulator.simulator.objects;

import tc.shared.world.WMAObject;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class SimMobileObject extends SimObject
{
	public final static int	CONSTANT_MOVE		= 121;
	public final static int	ACCELERATED_MOVE 	= 122;

	public double v;
	
	//MOVEMENT VALUES
	protected int	 m_type;
	
	public double SPEED; // Speed (metres per second)
	public double ACC;   // Acceleration (metres per square second)
	public double MASS = 0.4;  // Mass (Kilograms)

	public double RES_COEF = 0.75; // Restitution coeficient

	public double FRIC_COEF = 0.001; // Friction coeficient

	/** The movement parameters come from the world object (movement, speed, acceleration, mass, coef_res, coef_fric). */
	public SimMobileObject (WMAObject odesc)
	{
		super (odesc);

		m_type		= (odesc.movement == WMAObject.Movement.ACCELERATED) ? ACCELERATED_MOVE : 0;
		SPEED		= odesc.speed;
		ACC			= odesc.acceleration;
		MASS		= odesc.mass;
		RES_COEF	= odesc.coef_res;
		FRIC_COEF	= odesc.coef_fric;
	}
	
	/** Moves the object. Returns false if the object hasn't been moved */
	public boolean move (double time)
	{
		double ds, dv;

		switch (m_type)
		{
			case CONSTANT_MOVE:	
			case ACCELERATED_MOVE:  if (!((SPEED == 0.0) && (ACC == 0.0)))
 									{
										ds = SPEED*time;
										dv = (ACC*time) - (FRIC_COEF*9.8*time);
										SPEED = SPEED + dv;
										if (SPEED < 0.0)
											SPEED = 0.0;
										odesc.pos.x (odesc.pos.x () + (ds*Math.cos (odesc.a)));
										odesc.pos.y (odesc.pos.y () + (ds*Math.sin (odesc.a)));

										return (true);
									}
			
			default: return (false);
		}
	}
	
	/** Indicates odesc.a puntual collision of another object to this one
	 * @param edge : object's edge that has collided with this object
	 * @param xobj : horizontal position of the center of the collided object
	 * @param yobj : vertical position of the center of the collided object 
	 * @param aobj : angle of the collided object
	 * @param vobj : velocity of the collided object
	 * @param mobj : mass of the collided object
	 */
	public void object_collision (Line2 edge, double xobj, double yobj, double aobj, double vobj, double mobj)
	{
		recalc_angle(edge);
		SPEED=(mobj*SPEED+MASS*Math.abs(vobj)-mobj*RES_COEF*(Math.abs(vobj)-SPEED))/(MASS+mobj);
	}
	
	/**
	 * A robot, a disc of radius rradius at (rx, ry) moving at (rvx, rvy) m/s,
	 * against the object: when they overlap, the object is put out of it (just
	 * touching it, along the line from its centre) and, if they were getting
	 * closer, it bounces on it as on a moving wall of infinite mass (keeping
	 * RES_COEF of the speed they met with, plus the one the robot pushes with).
	 * Returns whether they touched.
	 */
	public boolean robot_collision (double rx, double ry, double rradius, double rvx, double rvy)
	{
		double		dx = odesc.pos.x () - rx, dy = odesc.pos.y () - ry;
		double		d = Math.sqrt (dx * dx + dy * dy);
		double		touch = rradius + radius;
		double		nx, ny;
		double		vx, vy, rel;

		if (d >= touch)		return false;

		// the way out: from the centre of the robot (ahead of it, if the centres are on each other)
		if (d > 1e-6)		{ nx = dx / d; ny = dy / d; }
		else				{ double n = Math.sqrt (rvx * rvx + rvy * rvy); nx = (n > 0.0) ? rvx / n : 1.0; ny = (n > 0.0) ? rvy / n : 0.0; }
		odesc.pos.x (rx + nx * (touch + 0.005));
		odesc.pos.y (ry + ny * (touch + 0.005));

		// the bounce, if they were getting closer
		vx		= SPEED * Math.cos (odesc.a);
		vy		= SPEED * Math.sin (odesc.a);
		rel		= (vx - rvx) * nx + (vy - rvy) * ny;
		if (rel < 0.0)
		{
			vx		-= (1.0 + RES_COEF) * rel * nx;
			vy		-= (1.0 + RES_COEF) * rel * ny;
			SPEED	= Math.sqrt (vx * vx + vy * vy);
			if (SPEED > 0.0)		odesc.a = Math.atan2 (vy, vx);
		}
		return true;
	}

	/** Indicates odesc.a wall collision */
	public void wall_collision (Line2 wall)
	{
		SPEED=SPEED*RES_COEF;
		recalc_angle(wall);		
		// Avoid that the ball pass off the wall
		if (wall.distance(odesc.pos.x(),odesc.pos.y())<= radius)
		{
			double dist;
			dist = (radius-wall.distance(odesc.pos.x(),odesc.pos.y()))+0.02; // Added odesc.a 0.02 offset to avoid aproximation errors

			odesc.pos.x (odesc.pos.x()+dist*Math.cos(odesc.a));
			odesc.pos.y (odesc.pos.y()+dist*Math.sin(odesc.a));
		}
	}
	
	/** Recalculates angle after odesc.a collision with odesc.a surface */
	protected void recalc_angle(Line2 wall)
	{
		double Ntan = (wall.dest().x()-wall.orig().x())/(wall.orig().y()-wall.dest().y());
		double Nang = Math.atan (Ntan);
					
		if (Nang < 0) Nang += Angles.PI2;
		
		double alpha = Math.PI - (Nang - odesc.a);
		if (alpha < 0) alpha += Angles.PI2;
					
		odesc.a = Nang - alpha;
		if (odesc.a < 0) odesc.a += Angles.PI2;
	}

}
