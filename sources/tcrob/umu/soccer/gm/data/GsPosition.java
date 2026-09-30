/**
 * Created on 16-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.data;

import wucore.utils.math.Angles;

public class GsPosition
{
	public int		x;
	public int		y;
	public double		theta;
	public int		dx;
	public int		dy;
	public double		dtheta;
	
	public void set (GsPosition other)
	{
		x		= other.x;
		y		= other.y;
		theta	= other.theta;
		dx		= other.dx;
		dy		= other.dy;
		dtheta	= other.dtheta;
	}
	
	public void translate (Odometry odo)
	{
		double xx, yy, tt;
		double rho, phi;
				
		xx		= (double) x;
		yy		= (double) y;
		tt		= (double) theta;
		
		// Convert local displacement to polar coordinates (Cartesian translation can not be applied)
		rho		= Math.sqrt (odo.dlin * odo.dlin + odo.dlat * odo.dlat);
		phi		= Math.atan2 (odo.dlat, odo.dlin); // (y, x)

		xx		= xx + rho * Math.cos (tt + phi);
		yy		= yy + rho * Math.sin (tt + phi);
		tt		= tt + odo.drot;
		tt		= Angles.radnorm_180 (tt);
		
		x		= (int) xx;
		y		= (int) yy;
		theta	= (double) tt;
	}
}
