package devices.drivers.gps.NMEA0183;

import devices.drivers.gps.GPSException;

/**
 * Title: NMEA0183Exception
 * Description: Excepcion provocada por un GPS tipo NMEA0183
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

public class NMEA0183Exception extends GPSException {

  public NMEA0183Exception()
  {
    super ("NMEA0183 GPS Exception");
  }

  public NMEA0183Exception(String message)
  {
    super ("NMEA0183 GPS Exception: "+message);
  }

}