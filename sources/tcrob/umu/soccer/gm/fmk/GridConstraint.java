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
	protected float			dist;		// Distance to Object
	protected float			angle;		// Angle to Object

	public GridConstraint ()
	{
		dist		= 0.0f;
		angle	= 0.0f;
	}
	
	public float getAngle ()			{ return angle; }
	public float getDistance ()		{ return dist; }
	
	public void set (float dist, float angle)
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
		dist		= (float) Math.sqrt ((distY*distY) + (distX*distX));
		
		if ((distY == 0.0) && (distX == 0.0))
			angle = 0.0f;
		else
			angle = (float) Math.atan2 (distY, distX);
			// angle = (float)atan2(distY, distX) - (PIh);
	}
}
