/**
 * Created on 15-jun-2006
 *
 * @author Humberto Martinez Barbera (2006)
 * @author David Herrero Perez (2004)
 * @author Alessandro Saffiotti (2002)
 */

package tcrob.umu.soccer.gm.fmk;

public class GridConstraint
{
	protected double			dist;		// Distance to Object
	protected double			angle;		// Angle to Object

	public GridConstraint ()
	{
		dist		= 0.0;
		angle	= 0.0;
	}
	
	public double getAngle ()			{ return angle; }
	public double getDistance ()		{ return dist; }
	
	public void set (double dist, double angle)
	{
		this.dist = dist;
		this.angle = angle;
	}

	public void setConstraint (int gx, int gy, int aobjx, int aobjy, int gsize)
	{
		double distX;
		double distY;
		
		distX	= (double) ((aobjx - gx) * gsize - (gsize >> 1));
		distY	= (double) ((aobjy - gy) * gsize - (gsize >> 1));
		dist		= (double) Math.sqrt ((distY*distY) + (distX*distX));
		
		if ((distY == 0.0) && (distX == 0.0))
			angle = 0.0;
		else
			angle = (double) Math.atan2 (distY, distX);
			// angle = (double)atan2(distY, distX) - (PIh);
	}
}
