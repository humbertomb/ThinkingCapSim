/*
 * (c) 2003 Humberto Martinez
 */

package devices.drivers.gps.Garmin;

import devices.drivers.gps.*;

public class GarminException extends GPSException
{
	public GarminException ()
	{
		super ("Garmin IOP Exception");
	}

	public GarminException (String message)
	{
		super ("Garmin IOP " + message);
	}

	public GarminException (Exception e)
	{
		super (e.toString ());
	}
}