/*
	Title:			Thinking Cap
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

/**
 * The control action: what a platform is asked to do, in the units its
 * kinematics work in.
 *
 * It is three velocities of the platform itself, and not of anything it is built
 * with: how fast it goes forward, how fast it goes sideways and how fast it
 * turns. A platform that cannot be driven sideways is given a lateral velocity
 * of zero and makes nothing of any other, which is a matter of its kinematics
 * model and not of whoever commands it (a synchro drive can, a differential
 * drive cannot).
 */
public class ItemMotion extends Item
{
	// Traction and steering control mode
	public static final int		 	CTRL_NONE		= 0;
	public static final int		 	CTRL_AUTO		= 1;
	public static final int		 	CTRL_MANUAL		= 2;

	// The velocities commanded
	public double					vlin;				// Longitudinal velocity (m/s)
	public double					vlat;				// Lateral velocity (m/s), to the left of the platform
	public double					vrot;				// Rotational velocity (rad/s)
	public boolean					brake;				// Activate brake
	public int						ctrlmode;			// Auto/manual mode

	// Constructors
	public ItemMotion ()
	{
		this.set (0.0, 0.0, 0.0, 0);
	}

	public ItemMotion (double vlin, double vlat, double vrot, long tstamp)
	{
		this.set (vlin, vlat, vrot, tstamp);
	}

	// Initialisers
	public void set (long tstamp)
	{
		super.set (tstamp);

		this.vlin		= 0.0;
		this.vlat		= 0.0;
		this.vrot		= 0.0;
		this.brake		= false;
		this.ctrlmode	= CTRL_NONE;
	}

	public void set (double vlin, double vlat, double vrot, long tstamp)
	{
		set (tstamp);

		this.vlin		= vlin;
		this.vlat		= vlat;
		this.vrot		= vrot;
		this.ctrlmode	= CTRL_AUTO;
	}

	public void set (double vlin, double vlat, double vrot, boolean brake, long tstamp)
	{
		set (tstamp);

		this.vlin		= vlin;
		this.vlat		= vlat;
		this.vrot		= vrot;
		this.brake		= brake;
		this.ctrlmode	= CTRL_AUTO;
	}

	public void set (int ctrlmode, double vlin, double vlat, double vrot, long tstamp)
	{
		set (tstamp);

		this.vlin		= vlin;
		this.vlat		= vlat;
		this.vrot		= vrot;
		this.ctrlmode	= ctrlmode;
	}

	public void set (boolean brake, long tstamp)
	{
		set (tstamp);

		this.brake		= brake;
	}

	public String toString ()
	{
		return "vlin=" + vlin + ", vlat=" + vlat + ", vrot=" + vrot + ", brake=" + brake;
	}
}
