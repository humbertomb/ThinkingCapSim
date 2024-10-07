package devices.drivers.captors.UDP;

import devices.drivers.captors.*;

/**
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

public class UDPCaptorsException extends CaptorsException
{

  public UDPCaptorsException()
  {
    super ("UDP Captors Exception.");
  }

  public UDPCaptorsException (String message)
  {
    super("UDP Captors Exception: "+message);
  }
}