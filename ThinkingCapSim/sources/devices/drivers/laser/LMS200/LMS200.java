/**
 * @author Humberto Martinez Barbera
 */

package devices.drivers.laser.LMS200;

import devices.drivers.laser.*;
import devices.drivers.laser.PLS.*;

import javax.comm.*;

public class LMS200 extends PLS
{
	public LMS200 () throws LaserException 
	{
		super ();
	}
	
	public void initialise (String param) throws LaserException
	{
		openLaserPort (param, "LMS200", SerialPort.PARITY_NONE);
	}	
}
