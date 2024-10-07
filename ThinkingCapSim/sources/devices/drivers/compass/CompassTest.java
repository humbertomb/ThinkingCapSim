package devices.drivers.compass;

import java.util.Properties;
import java.io.FileInputStream;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:
 * @author
 * @version 1.0
 */

public class CompassTest
{
  public static void main(String[] args)
  {
    Compass compas;
    Properties props;
    FileInputStream fprops;
    String routetoprops = "/home/juanpe/jbproject/Mimics/src/mimics.properties";
    String st;

    try
    {
      fprops = new FileInputStream (routetoprops);
      props = new Properties();
      props.load (fprops);

      st = props.getProperty("COMPASS0");
      compas = Compass.getCompass (st);

      System.out.println ("Compás del tipo "+compas.getType()+" encontrado");
      System.out.println("HEADING: "+compas.getHeading ());
      System.out.println ("TEMP: "+compas.getTemp ());
      System.out.println ("PITCH: "+compas.getPitch ());
      System.out.println ("ROLL: "+compas.getRoll ());
    } catch (Exception e)
    {
      System.err.println ("ERROR: "+e);
      System.exit(-1);
    }

  }
}