/**
 * Created on 03-jul-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.data;

public class Velocity 
{
	static public final int		VxMaxForward     = 400;			// forward (mm/sec)
	static public final int		VxMaxBackward    = 300;			// backward (mm/sec)
	static public final int		VyMaxLeft        = 300;			// lateral  (mm/sec)
	static public final int		VyMaxRight       = VyMaxLeft;
	static public final float		VthMaxLeft       = 90.0f;			// rotational (deg/sec)
	static public final float		VthMaxRight      = VthMaxLeft;

	static public final int		VxMin			 = 5;
	static public final int		VyMin			 = 4;
	static public final float		VthMin			 = 2.0f;
	
	public int					vlin;
	public int					vlat;
	public float					vrot;

	public void set (int vx, int vy, float vtheta)
	{
		// Check upper limits
		if (vx > VxMaxForward)
			vx = VxMaxForward;
		else if (vx < -VxMaxBackward)
			vx = -VxMaxBackward;

		if (vy > VyMaxLeft)
			vy = VyMaxLeft;
		else if (vy < -VyMaxRight)
			vy = -VyMaxRight;

		if (vtheta > VthMaxLeft)
			vtheta = VthMaxLeft;
		else if (vtheta < -VthMaxRight)
			vtheta = -VthMaxRight;

		// Check lower limits
		if (Math.abs (vx) < VxMin)
			vx = 0;

		if (Math.abs (vy) < VyMin)
			vy = 0;

		if (Math.abs (vtheta) < VthMin)
			vtheta = 0.0f;

		// Update velocities
		vlin		= vx;
		vlat		= vy;
		vrot		= vtheta;
	}

	public String toString ()
	{
		return vlin + " " + vlat + " " + (int) vrot;
	}
}
