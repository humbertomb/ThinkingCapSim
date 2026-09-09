/*
 * (c) 2002 Humberto Martinez Barbera
 */

package wucore.utils.math;

public class Angles extends Object
{
	public static final double		DTOR	= Math.PI / 180.0;
	public static final double		RTOD	= 180.0 / Math.PI;

	public static final double		PI		= Math.PI;
	public static final double		PI2		= Math.PI * 2.0;
	public static final double		PI15	= Math.PI * 1.5;
	public static final double		PI05	= Math.PI * 0.5;
	public static final double		PI025	= Math.PI * 0.25;
	public static final double		PI015	= Math.PI * 0.15;

	// Class methods
	
	// RADIANS section
	static public final double radnorm_360b (double rad)
	{
		while ((rad < 0) || (rad >= PI2))
		{
			if (rad >= PI2)		rad -= PI2;
			if (rad < 0)		rad += PI2;
		}
		return rad;
	}

	static public final double radnorm_360 (double rad)
	{
		while ((rad < -PI2) || (rad > PI2))
		{
			if (rad > PI2)		rad -= PI2;
			if (rad < -PI2)		rad += PI2;
		}
		return rad;
	}	 
	
	static public final double radnorm_180 (double rad)
	{
		while ((rad < -PI) || (rad > PI))
		{
			if (rad > PI)		rad -= PI2;
			if (rad < -PI)		rad += PI2;
		}
		return rad;
	}

	static public final double radnorm_90 (double rad)
	{
		while ((rad < -PI05) || (rad > PI05))
		{
			if (rad > PI05)		rad -= PI;
			if (rad < -PI05)	rad += PI;
		}
		return rad;
	}
	
	// DEGREES section
	public static final double degnorm_360b (double deg)
	{
		while ((deg < 0.0) || (deg >= 360.0))
		{
			if (deg >= 360.0)	deg -= 360.0;
			if (deg < 0)		deg += 360.0;
		}
		return deg;
	}

	public static final double degnorm_360 (double deg)
	{
		while ((deg < -360.0) || (deg > 360.0))
		{
			if (deg > 360.0)	deg -= 360.0;
			if (deg < -360.0)	deg += 360.0;
		}
		return deg;
	}

	public static final double degnorm_180 (double deg)
	{
		while ((deg < -180.0) || (deg > 180.0))
		{
			if (deg > 180.0)	deg -= 360.0;
			if (deg < -180.0)	deg += 360.0;
		}
		return deg;
	}

	public static final double degnorm_90 (double deg)
	{
		while ((deg < -90.0) || (deg > 90.0))
		{
			if (deg > 90.0)		deg -= 180.0;
			if (deg < -90.0)	deg += 180.0;
		}
		return deg;
	}
}