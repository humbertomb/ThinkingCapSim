/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

public class ItemMotion extends Item
{
	// Traction and steering control mode
	public static final int		 	CTRL_NONE		= 0;
	public static final int		 	CTRL_AUTO		= 1;
	public static final int		 	CTRL_MANUAL		= 2;	

	// Speed and steering angle control
	public double					speed;				// Longitudinal speed
	public double					turn;				// Turning ratio
	public boolean					brake;				// Activate brake
	public int						ctrlmode;			// Auto/manual mode
		
	// Constructors
	public ItemMotion () 
	{
		this.set (0.0, 0.0, 0);
	}	
	
	public ItemMotion (double speed, double turn, long tstamp) 
	{
		this.set (speed, turn, tstamp);
	}	
	
	// Initialisers
	public void set (long tstamp)
	{
		super.set (tstamp);
		
		this.speed		= 0.0;
		this.turn		= 0.0;
		this.brake		= false;
		this.ctrlmode	= CTRL_NONE;
	}

	public void set (double speed, double turn, long tstamp)
	{
		set (tstamp);
		
		this.speed		= speed;
		this.turn		= turn;
		this.ctrlmode	= CTRL_AUTO;
	}

	public void set (double speed, double turn, boolean brake, long tstamp)
	{
		set (tstamp);
		
		this.speed		= speed;
		this.turn		= turn;
		this.brake		= brake;
		this.ctrlmode	= CTRL_AUTO;
	}

	public void set (int ctrlmode, double speed, double turn, long tstamp)
	{
		set (tstamp);
		
		this.speed		= speed;
		this.turn		= turn;
		this.ctrlmode	= ctrlmode;
	}
	
	public void set (boolean brake, long tstamp)
	{
		set (tstamp);
		
		this.brake		= brake;
	}
	
	public String toString ()
	{
		return "speed=" + speed + ", turn=" + turn + ", brake=" + brake;
	}	
}
