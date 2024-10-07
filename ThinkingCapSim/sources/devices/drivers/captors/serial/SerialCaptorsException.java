package devices.drivers.captors.serial;

import devices.drivers.captors.*;

/**
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */
 
public class SerialCaptorsException extends CaptorsException
{
  public SerialCaptorsException()
  {
    super ("Serial Captors Exception.");
  }

  public SerialCaptorsException (String message)
  {
    super("Serial Captors Exception: "+message);
  }
}