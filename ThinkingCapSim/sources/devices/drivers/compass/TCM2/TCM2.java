package devices.drivers.compass.TCM2;

import javax.comm.*;
import java.io.*;

import wucore.utils.math.*;

/**
 * Title: TCM2
 * Description: Clase para acceder a un compas TCM2
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */

public class TCM2 extends devices.drivers.compass.Compass
{
	
	private static int timeout = 3000;
	
	private SerialPort serie;
	private InputStream is;
	private OutputStream os;
	
	public TCM2 ()
	{
		super ();
	}
	
	public void init(String puerto) throws TCM2Exception
	{
		CommPortIdentifier id;
		
		try
		{
			id = CommPortIdentifier.getPortIdentifier (puerto);
		} catch (NoSuchPortException npe)
		{
			throw (new TCM2Exception ("El puerto especificado ("+puerto+") no existe."));
		}
		try
		{
			serie = (SerialPort) id.open ("TCM2 Compass",10000);
		} catch (PortInUseException pie)
		{
			throw (new TCM2Exception ("El puerto especificado ("+puerto+") esta en uso."));
		}
		
		try
		{
			serie.setSerialPortParams(9600,8,1,SerialPort.PARITY_NONE);
			serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serie.enableReceiveTimeout(timeout);
		} catch (UnsupportedCommOperationException ucoe)
		{
			throw (new TCM2Exception ("No se pudo configurar el puerto "+puerto+"."));
		}
		
		try
		{
			is = serie.getInputStream();
			os = serie.getOutputStream();
		} catch (IOException ioe)
		{
			throw (new TCM2Exception ("No se pudo obtener los Streams del puerto."));
		}
		
		initTCM2();
	}
	
	
	private void initTCM2 () throws TCM2Exception
	{
		String r;
		
		sendCommand ("ax");
		getResponse();
		r = getResponse();
		if (r.charAt (0) != 'T' || r.charAt (1) != 'C' ||
				r.charAt (2) != 'M' || r.charAt (3) != '2')
			throw (new TCM2Exception ("El compas no es del tipo TCM2."));
		
		
		try
		{
			System.out.println ("\tTCM2 compass in port "+
					(CommPortIdentifier.getPortIdentifier(serie)).getName());
		} catch (NoSuchPortException nspe)
		{
			throw (new TCM2Exception ("Problemas con el puerto."));
		}
		System.out.print ("\t"+r);
		System.out.print ("\t"+getResponse());
		getResponse();
		//System.out.println ();
		
		sendCommand ("ec=e");
		getResponse();
		
		sendCommand ("ep=e");
		getResponse();
		
		sendCommand ("er=e");
		getResponse();
		
		sendCommand ("em=e");
		getResponse();
		
		sendCommand ("et=e");
		getResponse();
		
		sendCommand ("ed=d");	// Quitar notificaciones de error (campo magnetico propio)
		getResponse();
		
		sendCommand ("ut=c");
		getResponse();
		
		sendCommand ("uc=d");
		getResponse();
		
		sendCommand ("ui=d");
		getResponse();
		
		sendCommand ("sdo=t");
		getResponse();
		
	}
	
	private void sendCommand (String command) throws TCM2Exception
	{
		byte bytes[];
		
		//command.concat ("\n\r");
		bytes = (command.concat ("\r")).getBytes();
		
		try
		{
			os.write(bytes);
		} catch (IOException ioe)
		{
			throw (new TCM2Exception ("sendCommand: Error al acceder al puerto."));
		}
	}
	
	private String getResponse () throws TCM2Exception
	{
		byte buff[] = new byte[256];
		int unbyte;
		int i;
		
		try
		{
			i = 0;
			do
			{
				unbyte = is.read();
				if (unbyte < 0) throw (new TCM2Exception ("getResponse: Error al acceder al puerto."));
				buff[i++] = (byte)unbyte;
			} while (unbyte != 10);
		} catch (IOException ioe)
		{
			throw (new TCM2Exception ("getResponse: Error al acceder al puerto."));
		}
		
		return (new String (buff,0,i));
	}
	
	
	public void resetDefaults () throws TCM2Exception
	{
		initTCM2 ();
	}
	
	public double getHeading () throws TCM2Exception
	{
		String resp;
		int i,i2;
		double val;
		
		sendCommand ("c?");
		resp = getResponse ();
		
		i = 0;
		while (resp.charAt (i) != 'C') i++;
		i++;
		i2 = i;
		while (resp.charAt (i2) != '*') i2++;
		val = 90.0 - Double.parseDouble (resp.substring (i,i2)) + offset;
		
		while ((val < -180.0) || (val > 180.0))
		{
			if (val > 180.0)		val -= 360.0;
			if (val < -180.0)	val += 360.0;
		}
		
		resp = getResponse ();
		if (resp.charAt (0) == ':' && resp.charAt(1) != 'E')
			return val * Angles.DTOR;
		else
			return Double.NaN;
	}
	
//	Roll and pitch angles seem to be REVERSED !!!!!
	public double getRoll () throws TCM2Exception
	{
		String resp;
		int i,i2;
		double val;
		
		sendCommand ("i?");
		resp = getResponse ();
		
		i = 0;
		while (resp.charAt (i) != 'P') i++;
		i++;
		i2 = i;
		while (resp.charAt (i2) != 'R') i2++;
		val = Double.parseDouble (resp.substring (i,i2));
		
		resp = getResponse ();
		if (resp.charAt (0) == ':' && resp.charAt(1) != 'E')
			return val * Angles.DTOR;
		else 
			return Double.NaN;
	}
	
	public double getPitch () throws TCM2Exception
	{
		String resp;
		int i,i2;
		double val;
		
		sendCommand ("i?");
		resp = getResponse ();
		
		i = 0;
		while (resp.charAt (i) != 'R') i++;
		i++;
		i2 = i;
		while (resp.charAt (i2) != '*') i2++;
		val = Double.parseDouble (resp.substring (i,i2));
		
		resp = getResponse ();
		if (resp.charAt (0) == ':' && resp.charAt(1) != 'E')
			return val * Angles.DTOR;
		else 
			return Double.NaN;
	}
	
	public double getTemp () throws TCM2Exception
	{
		String resp;
		int i,i2;
		double val;
		
		sendCommand ("t?");
		resp = getResponse ();
		
		i = 0;
		while (resp.charAt (i) != 'T') i++;
		i++;
		i2 = i;
		while (resp.charAt (i2) != '*') i2++;
		val = Double.parseDouble (resp.substring (i,i2));
		
		resp = getResponse ();
		if (resp.charAt (0) == ':' && resp.charAt(1) != 'E')
			return val;
		else 
			return Double.NaN;
	}
}