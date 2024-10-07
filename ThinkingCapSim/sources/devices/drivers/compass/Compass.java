package devices.drivers.compass;

import devices.data.CompassData;

/**
 * Title: Compass
 * Description: Clase genérica para un compás
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */

public class Compass
{
    protected double 			offset;

    private String 				clase;
    private String 				port;

     public static Compass getCompass (String compassprop, double offset) throws CompassException
    {
      Compass tc;

      try
      {
		tc = getCompass (compassprop);
		tc.setOffset (offset);
      } catch (Exception e) { throw (new CompassException (e.toString())); }
      
      return tc;
    }

    public static Compass getCompass (String compassprop) throws CompassException
    {
      Compass tc;
      String _clase;
      String _port;

      try
      {
        _clase = compassprop.substring (0,compassprop.indexOf("|"));
        _port = compassprop.substring (compassprop.indexOf("|")+1, compassprop.length());
        Class compassclass = Class.forName(_clase);

        tc=(Compass)compassclass.newInstance();
        tc.init(_port);
        tc.setType (_clase);
        tc.setPort (_port);

      } catch (Exception e)
      {
        throw (new CompassException (e.toString()));
      }
      return (tc);
    }

   public void init (String port) throws CompassException
    {
      return;
    }

    public void setOffset (double o)
    {
      offset = o;
    }

    public void setType (String c)
    {
      clase = c;
    }

    public void setPort (String p)
    {
      port = p;
    }

    public String getType ()
    {
      return (clase);
    }

    public String getPort ()
    {
      return (port);
    }


    public void resetDefaults () throws CompassException
    {
      return;
    }

    public double getHeading () throws CompassException
    {
      return Double.NaN;
    }

    public double getPitch () throws CompassException
    {
      return Double.NaN;
    }

    public double getRoll () throws CompassException
    {
      return Double.NaN;
    }

    public double getTemp () throws CompassException
    {
      return Double.NaN;
    }

    public CompassData getData () throws CompassException
    {
      CompassData data = new CompassData();

      data.setHeading (this.getHeading());
      data.setPitch (this.getPitch());
      data.setRoll (this.getRoll());
      data.setTemp (this.getTemp());

      return data;
    }
}