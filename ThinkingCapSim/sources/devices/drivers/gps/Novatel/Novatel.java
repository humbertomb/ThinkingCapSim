/**
 * Title: Novatel
 * Description: Clase para obtener datos de un GPS Novatel
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */
package devices.drivers.gps.Novatel;

import javax.comm.*;
import java.io.*;
import java.util.StringTokenizer;

import devices.drivers.gps.GPS;
import devices.data.*;

import devices.pos.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;

public class Novatel extends GPS
{
	private static int timeout = 1000;
	
	private SerialPort serie;
	private InputStream is;
	private OutputStream os;
	
	protected GPSData data;
	protected double height;
	protected int fix;
	
	public Novatel ()
	{
		super ();
		data	= new GPSData ("WGS-84");
	}
	
	
	private void sendCommand (String command) throws NovatelException
	{
		byte bytes[];
		
		cleanInput();
		bytes = (command.concat ("\r")).getBytes();
		
		//System.out.println ("sendCommand: envío="+command);
		try
		{
			os.write(bytes);
		} catch (IOException ioe)
		{
			throw (new NovatelException ("Error al acceder al puerto."));
		}
	}
	
	
	private String getResponse () throws NovatelException
	{
		byte buff[] = new byte[1024];
		int unbyte;
		int i;
		
		try
		{
			i = 0;
			while ((unbyte = is.read()) != '$');
			do
			{
				buff[i++] = (byte)unbyte;
				unbyte = is.read();
				if (unbyte < 0) throw (new NovatelException ("Error al acceder al puerto."));
			} while (unbyte != 10);
		} catch (IOException ioe)
		{
			throw (new NovatelException ("Error al acceder al puerto."));
		}
		
		//System.out.println("recibo: "+new String (buff,0,i));
		
		return (new String (buff,0,i));
	}
	
	private void cleanInput ()
	{
		try
		{
			while (is.available() > 0) is.read();
		} catch (IOException ioe)
		{
			return;
		}
	}
	
	protected void initialise () throws NovatelException
	{
		CommPortIdentifier id;
		
		try
		{
			id = CommPortIdentifier.getPortIdentifier (port);
		} catch (NoSuchPortException npe)
		{
			throw (new NovatelException ("El puerto especificado ("+port+") no existe."));
		}
		try
		{
			serie = (SerialPort) id.open ("Novatel Millenium GPS",10000);
		} catch (PortInUseException pie)
		{
			throw (new NovatelException ("El puerto especificado ("+port+") esta en uso."));
		}
		
		try
		{
			serie.setSerialPortParams(57600,8,1,SerialPort.PARITY_NONE);
			serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serie.enableReceiveTimeout(timeout);
		} catch (UnsupportedCommOperationException ucoe)
		{
			throw (new NovatelException ("No se pudo configurar el puerto "+port+"."));
		}
		
		try
		{
			is = serie.getInputStream();
			os = serie.getOutputStream();
		} catch (IOException ioe)
		{
			throw (new NovatelException ("No se pudo obtener los Streams del puerto."));
		}
		
		initNovatel();
	}
	
	public void close () throws NovatelException
	{
		try
		{
			is.close ();
			os.close ();
			serie.close ();
		} catch (Exception e) { throw new NovatelException (e.getMessage()); }
	}
	
	
	private void initNovatel () throws NovatelException
	{
		String r;
		byte buff[] = new byte[512];
		int i;
		int unbyte;
		
		sendCommand ("unlogall com1");//eliminamos log binarios de la configuración
		cleanInput ();
		//System.out.println ("Envio peticion de version");
		sendCommand ("version");
		i=0;
		try
		{
			while (is.read() != 10);
			do
			{
				unbyte = is.read();
				if (unbyte < 0) throw (new NovatelException ("No se obtuvo respuesta."));
				//System.out.print (Integer.toHexString(unbyte)+",");
				buff[i++] = (byte)unbyte;
			} while (unbyte != 10);
		} catch (IOException ioe)
		{
			throw (new NovatelException ("Error al acceder al puerto."));
		}
		//r = getResponse();
		r = new String(buff,0,i);
		//System.out.println("r="+r);
		if (r.startsWith("OEM-3 MILL"))
		{
			try
			{
				System.out.println ("\tNovatel Millenium GPS in port "+
						(CommPortIdentifier.getPortIdentifier(serie)).getName());
				System.out.println ("\tVersion: "+r);
			} catch (NoSuchPortException nspe)
			{
				throw (new NovatelException ("Problemas con el puerto"));
			}
			try
			{
				do { unbyte = is.read(); } while (unbyte != 10);
			} catch (IOException ioe)
			{
				throw (new NovatelException ("Problemas con el puerto"));
			}
			cleanInput();
		}
		else throw (new NovatelException ("No se encontró un GPS Novatel Millenium."));
	}
	
	/* ---------------------------- */
	/*         GPS INTERFACE        */
	/* ---------------------------- */
	public GPSData getData ()
	{
		try
		{
			data.setPos (getPos ());		
			data.setAltitude (height);
			data.setVel (getVel ());
			data.setWeekSeconds ((int) Math.round (getWeekSeconds()));
			data.setFix (fix);
			data.setNumSat (getNumSat());			
		} catch (Exception e)
		{
			new NovatelException (e.toString ()).printStackTrace ();
		}
		
		return data;
	}
	
	public SatelliteData[] getSatellites ()
	{		
		SatelliteData[]			sats = null;
		try
		{
			sats = getSV ();		
		} catch (Exception e)
		{
			new NovatelException (e.toString ()).printStackTrace ();
		}
		
		return sats;
	}		
	
	protected LLAPos getPos () throws NovatelException
	{
		String resp;
		String tst;
		StringTokenizer st;
		double tlat;
		double tlong;
		
		sendCommand ("log prtka once");
		resp = getResponse();
		if (resp.indexOf("$PRTKA") == -1)
		{
			System.out.println ("Paquete con identificador incorrecto: "+resp);
			return null;
		}
		
		st = new StringTokenizer (resp,",*");
		if (st.countTokens() < 18) return (new LLAPos (Double.NaN,Double.NaN));
		st.nextToken(); //$PRTKA
		st.nextToken(); //week
		st.nextToken(); //sec
		st.nextToken(); //lag
		st.nextToken(); //#sv
		st.nextToken(); //#high
		st.nextToken(); //L1L2 #high
		tst = st.nextToken(); //lat
		tlat = Double.parseDouble(tst);
		tst = st.nextToken(); //long
		tlong = Double.parseDouble(tst);
		tst = st.nextToken(); //hgt
		height = Double.parseDouble(tst);
		st.nextToken(); //undulation
		st.nextToken(); //datum ID
		st.nextToken(); //lat dev.
		st.nextToken(); //lon dev.
		st.nextToken(); //hgt dev.
		st.nextToken(); //soln status
		st.nextToken(); //rtk status
		
		tst = st.nextToken(); //posn type
		switch (Integer.parseInt(tst))
		{
		case 0: fix = GPSData.FIX_INVALID;
		break;
		
		case 1: fix = GPSData.FIX_3D;
		break;
		
		case 2:
		case 3:
		case 4: fix = GPSData.FIX_3D;			// DGPS fix
		break;
		
		case 5: fix = GPSData.FIX_WAAS_3D;
		break;
		
		default: fix = GPSData.FIX_ERROR;
		}
		
		return new LLAPos (tlat,tlong);
	}
	
	protected Point3 getVel () throws NovatelException
	//Usar log SPHA
	{
		String resp;
		String tst;
		StringTokenizer st;
		double spd, dir;
		Point3 vel;
		
		sendCommand ("log spha once");
		resp = getResponse();
		if (resp.indexOf("$SPHA") == -1)
		{
			System.out.println ("Paquete con identificador incorrecto: "+resp);
			return null;
		}
		
		st = new StringTokenizer (resp,",*");
		if (st.countTokens() < 6) return null;
		
		vel	= new Point3 ();
		st.nextToken(); //$SPHA
		st.nextToken(); //week
		st.nextToken(); //seconds
		tst = st.nextToken(); //hor spd
		spd = Double.parseDouble(tst); //en m/s
		
		tst = st.nextToken(); //trk gnd
		dir = Double.parseDouble(tst) * Angles.DTOR;
		
		vel.x (spd * Math.cos (dir));
		vel.y (spd * Math.sin (dir));
		
		tst = st.nextToken(); //vert spd
		vel.z (Double.parseDouble(tst));
		
		return vel;
	}
	
	protected int getNumSat () throws NovatelException
	{
		String resp;
		String tst;
		StringTokenizer st;
		
		
		sendCommand ("log sata once");
		resp = getResponse();
		if (resp.indexOf("$SATA") == -1)
		{
			System.out.println ("Paquete con identificador incorrecto: "+resp);
			return (0);
		}
		
		st = new StringTokenizer (resp,",*");
		if (st.countTokens() < 5) return (0);
		st.nextToken (); //$SATA
		st.nextToken (); //week
		st.nextToken (); //seconds
		st.nextToken (); //sol status
		tst = st.nextToken(); //# obs
		
		return Integer.parseInt(tst);
	}
	
	protected SatelliteData[] getSV () throws NovatelException
	//Usar log SATA
	{
		String resp;
		String tst;
		StringTokenizer st;
		int i;
		int tprn;
		double telev;
		double tazim;
		SatelliteData[] tlista;
		
		sendCommand ("log sata once");
		resp = getResponse();
		if (resp.indexOf("$SATA") == -1)
		{
			System.out.println ("Paquete con identificador incorrecto: "+resp);
			return (new SatelliteData[0]);
		}
		st = new StringTokenizer (resp,",*");
		
		//System.out.println ("Novatel getSV: "+resp);
		
		if (st.countTokens() < 5) return (new SatelliteData[0]);
		st.nextToken (); //$SATA
		st.nextToken (); //week
		st.nextToken (); //seconds
		st.nextToken (); //sol status
		tst = st.nextToken(); //# obs
		i = Integer.parseInt(tst);
		if (i > 0)
		{
			tlista = new SatelliteData[i];
			for (; i > 0; i--)
			{
				tst = st.nextToken(); //prn
				tprn = Integer.parseInt (tst);
				tst = st.nextToken(); //azimuth
				tazim = Double.parseDouble(tst);
				tst = st.nextToken();//elevation
				telev = Double.parseDouble(tst);
				st.nextToken();//residual
				st.nextToken();//reject code
				
				tlista[i-1] = new SatelliteData (tprn,telev, tazim, -666);
			}
			return (tlista);
		}
		return (null);
	}
	
	protected DOP getDOP () throws NovatelException
	{
		String resp;
		String tst;
		StringTokenizer st;
		double tg, tp, tht, th, tt;
		int tsat;
		
		sendCommand ("log dopa once");
		resp = getResponse();
		st = new StringTokenizer (resp,",*");
		//System.out.println ("\ngetDOP:"+resp);
		if (st.countTokens() < 9) return (new DOP (0.0,0.0,0.0,0.0,0.0,0));
		st.nextToken();//$DOPA
		st.nextToken();//week
		st.nextToken();//seconds
		tst = st.nextToken();//gdop
		tg = Double.parseDouble (tst);
		tst = st.nextToken();//pdop
		tp = Double.parseDouble(tst);
		tst = st.nextToken();//htdop
		tht = Double.parseDouble(tst);
		tst = st.nextToken();//hdop
		th = Double.parseDouble(tst);
		tst = st.nextToken();//tdop
		tt = Double.parseDouble(tst);
		
		tst = st.nextToken(); //# sats
		tsat = Integer.parseInt(tst);
		
		
		return (new DOP (tg,tp,tht,th,tt,tsat));
	}
	
	protected float getWeekSeconds () throws NovatelException
	{
		String resp;
		String tst;
		StringTokenizer st;
		float secs, offset;
		int cmstatus;
		
		sendCommand ("log tm1a once");
		resp = getResponse();
		st = new StringTokenizer (resp,",*");
		//System.out.println ("\ngetWeekSeconds: "+resp);
		//if (st.countTokens() < 8) return (new DOP (0.0,0.0,0.0,0.0,0.0,0));
		st.nextToken();//$TM1A
		st.nextToken();//week
		tst = st.nextToken();//seconds
		secs = Float.parseFloat(tst);
		tst = st.nextToken();//offset
		offset = Float.parseFloat(tst);
		st.nextToken(); //offset std
		st.nextToken(); //utc offset
		tst = st.nextToken(); //cm status
		cmstatus = Integer.parseInt(tst);
		if (cmstatus == 0) return (secs - offset);
		else return (Float.NaN);
	}
	
}