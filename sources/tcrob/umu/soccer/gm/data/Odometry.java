/**
 * Created on 16-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.data;

import tclib.utils.pos.Position;
import wucore.utils.math.*;

public class Odometry
{
	public double dlin;			// mm
	public double dlat;			// mm
	public double drot;			// rad
	public double elin;
	public double elat;
	public double erot;
	
	public void reset ()
	{
		dlin	= 0.0;
		dlat	= 0.0;
		drot	= 0.0;
		elin	= 0.0;
		elat	= 0.0;
		erot	= 0.0;
	}
	
	// Last odometric pose (m, m, rad), to get the displacement from one to the next
	protected boolean		started	= false;
	protected double		lastx, lasty, lasta;

	/**
	 * The displacement from the last odometric pose to this one (the odometry of
	 * the LPS, accumulated since the start: m, m, rad), in the frame of the robot
	 * at the last one: dlin forwards and dlat to the left (mm), drot (rad). The
	 * first time there is no last pose, so there is no displacement.
	 */
	public void setOdometry (Position odom)
	{
		double		dx, dy, ca, sa;

		dlin	= 0.0;
		dlat	= 0.0;
		drot	= 0.0;
		elin	= 0.0;
		elat	= 0.0;
		erot	= 0.0;

		if (started)
		{
			dx		= odom.x () - lastx;
			dy		= odom.y () - lasty;
			ca		= Math.cos (lasta);
			sa		= Math.sin (lasta);

			dlin	= ( ca * dx + sa * dy) * 1000.0;
			dlat	= (-sa * dx + ca * dy) * 1000.0;
			drot	= Angles.radnorm_180 (odom.alpha - lasta);
		}

		lastx	= odom.x ();
		lasty	= odom.y ();
		lasta	= odom.alpha;
		started	= true;
	}

	/** Forgets the last odometric pose: the next one gives no displacement. */
	public void restart ()
	{
		started	= false;
	}
	
	public void translate (Odometry odo)
	{
		double rho, phi;
		
		// Update position
		rho		= Math.sqrt (odo.dlin * odo.dlin + odo.dlat * odo.dlat);
		phi		= Math.atan2 (odo.dlat, odo.dlin) + 0.5 * odo.drot;
		
		dlin	= dlin + (rho * Math.cos (drot + phi));
		dlat	= dlat + (rho * Math.sin (drot + phi));
		drot	= drot + odo.drot;
		drot	= Angles.radnorm_180 (drot);
		
		// Update uncertainty
		rho		= Math.sqrt (odo.elin * odo.elin + odo.elat * odo.elat);
		phi		= Math.atan2 (odo.elat, odo.elin) + 0.5 * odo.erot;
		
		elin	= elin + (rho * Math.cos (erot + phi));
		elat	= elat + (rho * Math.sin (erot + phi));
		erot	= erot + odo.erot;
		erot	= Angles.radnorm_180 (erot);
	}
		
	public String toString ()
	{
		return "[" + dlin + ", " + dlat + ", " + (int) (drot * Angles.RTOD) + "]";
	}
}
