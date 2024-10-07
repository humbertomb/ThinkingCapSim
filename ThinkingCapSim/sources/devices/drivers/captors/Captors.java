package devices.drivers.captors;

/**
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */

public class Captors 
{
	private String clase;
	private String connect;
	
	public static Captors getCaptors (String Capprop) throws CaptorsException
	{
	  Captors tcp;
      String cl;
      String param;

     try
      {
      	
        cl = Capprop.substring (0,Capprop.indexOf("|"));
        param = Capprop.substring (Capprop.indexOf("|")+1,Capprop.length());
        Class captorsclass = Class.forName(cl);
        tcp=(Captors)captorsclass.newInstance();
        System.out.println ("Captors: inicializando "+cl+" en "+param+".");
        tcp.init(param);
        tcp.setType (cl);
        tcp.setConnection (param);
        
      } catch (Exception e)
      {
        //e.printStackTrace();
        throw (new CaptorsException ("(getCaptors) "+e.toString()));
      }
      return (tcp);	
	}
	
	public void init (String p) throws CaptorsException
	{
		return;
	}
	
	public void setType (String c)
	{
		clase = c;					
	}
	
	public void setConnection (String con)
	{
		connect = con;
	}
	
	public String getType ()
	{
		return(clase);					
	}
	
	public String getConnection ()
	{
		return(connect);
	}
	
	public int[] getValues () throws CaptorsException
	{
		return (null);
	}
}