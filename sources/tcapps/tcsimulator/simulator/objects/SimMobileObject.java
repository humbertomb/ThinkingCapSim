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

	public double COL_COEF = 0.20; // Collision coefficient: the part of the speed lost when it meets a wall, a robot or another object

	public double FRIC_COEF = 0.001; // Friction coeficient

	/** The last robot that touched it (its number in the simulator), -1 for none yet, and when [ms]. */
	public int				touchedBy	= -1;
	public long				touchedAt;

	/** A robot touched it: it is the last one to have done so. */
	public void touched (int robot, long when)
	{
		touchedBy	= robot;
		touchedAt	= when;
	}

	/** The movement parameters come from the world object (movement, speed, acceleration, mass, coef_col, coef_fric). */
	public SimMobileObject (WMAObject odesc)
	{
		super (odesc);

		m_type		= (odesc.movement == WMAObject.Movement.ACCELERATED) ? ACCELERATED_MOVE : 0;
		SPEED		= odesc.speed;
		ACC			= odesc.acceleration;
		MASS		= odesc.mass;
		COL_COEF	= odesc.coef_col;
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
		SPEED=(mobj*SPEED+MASS*Math.abs(vobj)-mobj*kept ()*(Math.abs(vobj)-SPEED))/(MASS+mobj);
	}
	
	/**
	 * A robot, a disc of radius rradius at (rx, ry) moving at (rvx, rvy) m/s,
	 * against the object: when they overlap, the object is put out of it (just
	 * touching it, along the line from its centre) and, if they were getting
	 * closer, it bounces on it as on a moving wall of infinite mass (losing
	 * COL_COEF of the speed they met with, plus the one the robot pushes with).
	 * Returns whether they touched.
	 */
	/**
	 * The object against a robot of some outline (its segments where the robot is
	 * now): an object that overlaps it is put out of it, the shortest way, and
	 * bounces off it if they were getting closer. One whose centre got inside the
	 * outline goes out through the nearest of its edges. Returns
	 * whether they touched.
	 */
	public boolean robot_collision (wucore.utils.geom.Line2[] outline, double rx, double ry, double rvx, double rvy)
	{
		double		ox = odesc.pos.x (), oy = odesc.pos.y ();
		double		best = Double.MAX_VALUE, px = 0.0, py = 0.0;
		double		nx, ny, d, vx, vy, rel;
		int			cross = 0;

		if ((outline == null) || (outline.length == 0))		return false;
		for (wucore.utils.geom.Line2 l : outline)
		{
			if (l == null)		continue;

			double	x0 = l.orig ().x (), y0 = l.orig ().y (), ex = l.dest ().x () - x0, ey = l.dest ().y () - y0;
			double	len2 = ex * ex + ey * ey;
			double	t = (len2 > 0.0) ? Math.max (0.0, Math.min (1.0, ((ox - x0) * ex + (oy - y0) * ey) / len2)) : 0.0;
			double	cx = x0 + t * ex, cy = y0 + t * ey;
			double	dd = Math.hypot (ox - cx, oy - cy);

			if (dd < best)		{ best = dd;	px = cx;	py = cy; }
			// a ray to the right of the centre, for whether it is inside the outline
			if (((y0 > oy) != (y0 + ey > oy)) && (ox < x0 + (oy - y0) * ex / ey))		cross++;
		}
		boolean		inside = (cross % 2) == 1;

		if (!inside && (best >= radius))		return false;

		if (inside)
		{
			// in: out through the nearest of its edges, the way to it
			nx	= px - ox;		ny = py - oy;
			d	= Math.hypot (nx, ny);
			if (d > 1e-9)		{ nx /= d;	ny /= d; }
			else				{ nx = ox - rx;	ny = oy - ry;	d = Math.hypot (nx, ny);	nx = (d > 0.0) ? nx / d : 1.0;	ny = (d > 0.0) ? ny / d : 0.0; }
			odesc.pos.x (px + nx * (radius + 0.005));
			odesc.pos.y (py + ny * (radius + 0.005));
		}
		else
		{
			nx	= ox - px;		ny = oy - py;
			d	= Math.hypot (nx, ny);
			if (d > 1e-9)		{ nx /= d;	ny /= d; }
			else				{ nx = 1.0;	ny = 0.0; }
			odesc.pos.x (px + nx * (radius + 0.005));
			odesc.pos.y (py + ny * (radius + 0.005));
		}

		// the bounce, if they were getting closer
		vx		= SPEED * Math.cos (odesc.a);
		vy		= SPEED * Math.sin (odesc.a);
		rel		= (vx - rvx) * nx + (vy - rvy) * ny;
		if (rel < 0.0)
		{
			vx		-= (1.0 + kept ()) * rel * nx;
			vy		-= (1.0 + kept ()) * rel * ny;
			SPEED	= Math.sqrt (vx * vx + vy * vy);
			if (SPEED > 0.0)		odesc.a = Math.atan2 (vy, vx);
		}
		return true;
	}

	/** The object against a robot that is a disc of some radius at (rx, ry). */
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
			vx		-= (1.0 + kept ()) * rel * nx;
			vy		-= (1.0 + kept ()) * rel * ny;
			SPEED	= Math.sqrt (vx * vx + vy * vy);
			if (SPEED > 0.0)		odesc.a = Math.atan2 (vy, vx);
		}
		return true;
	}

	/** What a bounce keeps of the speed of incidence (never below nothing, nor above all of it). */
	protected double kept ()
	{
		return Math.min (1.0, Math.max (0.0, 1.0 - COL_COEF));
	}

	/** Indicates odesc.a wall collision: the object bounces off it, losing COL_COEF of the speed it came with. */
	public void wall_collision (Line2 wall)
	{
		SPEED=SPEED*kept ();
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
