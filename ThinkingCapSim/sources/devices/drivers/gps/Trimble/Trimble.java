/**
 * Title: Trimble
 * Description: Clase para acceder a un receptor Trimble por protocolo TSIP
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.drivers.gps.Trimble;

import java.io.*;
import javax.comm.*;

import devices.drivers.gps.GPS;
import devices.data.*;

public class Trimble extends GPS
{
	
	private static int timeout = 1000;
	
	private SerialPort serie;
	private InputStream is;
	private OutputStream os;
	
	private TrimbleInterpreter ti;
	protected GPSData data;
	
	public Trimble()
	{
		super();
		
		data		= new GPSData ("WGS-84");
	}
	
	protected void initialise () throws TrimbleException
	{
		CommPortIdentifier id;
		
		try
		{
			id = CommPortIdentifier.getPortIdentifier (port);
		} catch (NoSuchPortException npe)
		{
			throw (new TrimbleException ("El puerto especificado ("+port+") no existe."));
		}
		try
		{
			serie = (SerialPort) id.open ("Trimble ACE III GPS",10000);
		} catch (PortInUseException pie)
		{
			throw (new TrimbleException ("El puerto especificado ("+port+") esta en uso."));
		}
		
		try
		{
			serie.setSerialPortParams(38400,8,1,SerialPort.PARITY_NONE);
			serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serie.enableReceiveTimeout(timeout);
		} catch (UnsupportedCommOperationException ucoe)
		{
			throw (new TrimbleException ("No se pudo configurar el puerto "+port+"."));
		}
		
		
		ti = null;
		try
		{
			is = serie.getInputStream();
			os = serie.getOutputStream();
		} catch (IOException ioe)
		{
			throw (new TrimbleException ("No se pudo obtener los Streams del puerto."));
		}
		
		try
		{
			ti = new TrimbleInterpreter (is,os);
			if (ti != null) ti.initTrimble();
		} catch (IOException ioe)
		{
			throw (new TrimbleException ("Problemas al inicializar el GPS: "+ioe));
		}
	}
	
	public void close () throws TrimbleException
	{
		try
		{
			is.close ();
			os.close ();
			serie.close ();
		} catch (Exception e) { throw new TrimbleException (e.getMessage()); }
	}
	
	/* ---------------------------- */
	/*         GPS INTERFACE        */
	/* ---------------------------- */
	public GPSData getData ()
	{
		try
		{
			data.setPos (ti.getPos ());		
			data.setAltitude (ti.height);
			data.setVel (ti.getVel ());
			data.setWeekSeconds ((int) Math.round (ti.getWeekSeconds()));
			data.setFix (GPSData.FIX_VALID);
			data.setNumSat (ti.getNumSat());			
		} catch (Exception e)
		{
			new TrimbleException (e.toString ()).printStackTrace ();
		}
		
		return data;
	}
	
	public SatelliteData[] getSatellites ()
	{		
		SatelliteData[]			sats = null;
		try
		{
			sats = ti.getSV ();		
		} catch (Exception e)
		{
			new TrimbleException (e.toString ()).printStackTrace ();
		}
		
		return sats;
	}		
}