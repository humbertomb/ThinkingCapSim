package devices.drivers.gps;

/**
 * Title: GPSException
 * Description: Excepcion provocada por un dispositivo GPS
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

public class GPSException extends Exception
{

  public GPSException()
  {
    super ();
  }

  public GPSException (String message)
  {
    super (message);
  }
}