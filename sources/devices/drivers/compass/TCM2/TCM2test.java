package devices.drivers.compass.TCM2;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:
 * @author
 * @version 1.0
 */

public class TCM2test
{

  public static void main (String args[])
  {
    TCM2 compas;

    /*java.util.Enumeration enum = CommPortIdentifier.getPortIdentifiers ();
    System.out.println ("Puertos encontrados: ");
    while (enum.hasMoreElements ())
    {
      CommPortIdentifier id = (CommPortIdentifier) enum.nextElement();
      System.out.println(id.getName());
    }*/

    try
    {
      compas = new TCM2 ();
      compas.init("/dev/ttyS0");
      System.out.println("HEADING: "+compas.getHeading ());
      System.out.println ("TEMP: "+compas.getTemp ());
      System.out.println ("PITCH: "+compas.getPitch ());
      System.out.println ("ROLL: "+compas.getRoll ());
    } catch (TCM2Exception TCM2e)
    {
      System.err.println ("ERROR: "+TCM2e);
      System.exit(-1);
    }
  }
}