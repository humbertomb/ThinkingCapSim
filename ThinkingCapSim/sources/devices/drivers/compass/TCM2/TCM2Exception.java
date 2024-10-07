package devices.drivers.compass.TCM2;

/**
 * Title: TCM2Exception
 * Description: Excepción en un compas TCM2.
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */

public class TCM2Exception extends devices.drivers.compass.CompassException {

  public TCM2Exception()
  {
    super("TCM2 Compass exception.");
  }

  public TCM2Exception (String message)
  {
    super ("TCM2 Compass exception: "+message);
  }
}