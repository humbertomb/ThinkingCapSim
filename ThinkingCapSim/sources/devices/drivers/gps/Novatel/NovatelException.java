package devices.drivers.gps.Novatel;

import devices.drivers.gps.GPSException;

/**
 * Title: NovatelException
 * Description: Excepcion producida por un GPS Novatel Millenium
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

public class NovatelException extends GPSException
{

  public NovatelException()
  {
    super ("Novatel Millenium Exception.");
  }

  public NovatelException(String message)
  {
    super ("Novatel Millenium Exception: "+message);
  }
}