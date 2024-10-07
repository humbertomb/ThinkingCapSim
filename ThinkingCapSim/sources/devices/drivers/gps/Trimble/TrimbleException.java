package devices.drivers.gps.Trimble;

import devices.drivers.gps.GPSException;

/**
 * Title: TrimbleException
 * Description: Excepci�n provocada por un receptor Trimble ACE III
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro C�novas Qui�onero
 * @version 1.0
 */

public class TrimbleException extends GPSException
{

  public TrimbleException()
  {
    super ("Trimble TSIP Exception.");
  }

  public TrimbleException (String message)
  {
    super ("Trimble TSIP: "+message);
  }

}