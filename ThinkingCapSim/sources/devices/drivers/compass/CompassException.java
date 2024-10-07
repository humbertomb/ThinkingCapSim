package devices.drivers.compass;

/**
 * Title: CompassException
 * Description: Excepci�n en un compas
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro C�novas Qui�onero
 * @version 1.0
 */

public class CompassException extends Exception
{
  public CompassException()
  {
    super();
  }

  public CompassException (String message)
  {
    super(message);
  }
}