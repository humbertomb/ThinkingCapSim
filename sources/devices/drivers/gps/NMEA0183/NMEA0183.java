/*
 * (c) 2000-2001 Juan Pedro Canovas
 * (c) 2004 Humberto Martinez
 */

package devices.drivers.gps.NMEA0183;

import javax.comm.*;

import java.io.*;
import java.util.*;

import devices.data.*;
import devices.drivers.gps.*;

import devices.pos.*;
import wucore.utils.math.*;
import wucore.utils.geom.*;

public class NMEA0183 extends GPS
{
	static public final int			SER_TIMEOUT		= 1000;
	static public final int			EOL				= 10;
	
	// Protocol frames
	static public final int			ERROR			= 0;
	static public final int			GPGGA			= 1;
	static public final int			GPRMC			= 2;
	static public final int			GPVTG			= 3;
	static public final int			GPGSA			= 4;
	static public final int			GPGSV			= 5;
	static public final int			GPGLL			= 6;
	
	// ------------------------------
	// Interpreter and communications
	// ------------------------------
	
	// Runtime and state data
	protected int[]				packet;
	protected GPSData			data;
	private NMEA0183Parser		parser;
	private SerialPort			serial;
	private InputStream			input;
	private OutputStream		output;
	
	// Protocol features
	protected LLAPos			pos;
	protected double			height;
	protected Point3			vel;
	protected int				nsats;
	protected int				fix;
	
	private DOP dop;
	private double Hdop;
	private double Vdop;
	private double Pdop;
	private LinkedList sats;
	private int numsats;
	private int satsremain;
	
	// Constructors
	public NMEA0183 ()
	{
		parser	= new NMEA0183Parser ();
		data	= new GPSData ("WGS-84");
		pos		= new LLAPos ();
		vel		= new Point3 ();
	}
	
	public NMEA0183 (String port) throws NMEA0183Exception
	{
		this ();
		
		setPort (port);
		initialise ();		
	}
	
	// Class methods
	static public int parseCode (String code)
	{
		String			text;
		
		text	= code.toUpperCase ();
		if (text.equals ("$GPGGA"))
			return GPGGA;
		else if (text.equals ("$GPRMC"))
			return GPRMC;
		else if (text.equals ("$GPVTG"))
			return GPVTG;
		else if (text.equals ("$GPGSA"))
			return GPGSA;
		else if (text.equals ("$GPGSV"))
			return GPGSV;
		else if (text.equals ("$GPGLL"))
			return GPGLL;
		
		return ERROR;
	}
	
	// Internal classes
	class NMEA0183Parser implements Runnable
	{
		boolean			running;
		
		public void run ()
		{
			int			i;
			byte		buffer[] = new byte[2048];
			String		code, text;
			int			abyte;
			boolean		newdata;
			
			Thread.currentThread ().setName ("NMEAParser");	
			System.out.println ("  [NMEA] NMEA-0183 protocol parser (listener thread) ready");
			
			running		= true;
			while (running)
			{
				newdata		= false;
				i			= 0;
				
				while (!newdata)
				{
					try
					{
						abyte	= input.read ();
						
						if (abyte < 0)		continue;
						
						buffer[i]	= (byte) abyte;
						i++;
						
						if (abyte == EOL)		newdata	= true;
					}
					catch (Exception e)
					{
						e.printStackTrace ();
					}
				}
				
				code	= new String (buffer, 0, 6);  
				text	= new String (buffer, 0, i-1);  
//				System.out.print ("  [NMEA] Parsing packet <"+code+"> <"+text+">");
				switch (parseCode (code))
				{
				case GPGGA:
					decodeGPGGA (text);
					break;
					
				case GPRMC:
					decodeGPRMC (text);
					break;
					
				case GPGLL:
					decodeGPGLL (text);
					break;
					
				case GPVTG: 
					decodeGPVTG (text);				 
					break;
					
				case GPGSA: 
					decodeGPGSA (text);
					break;
					
				case GPGSV: 
					decodeGPGSV (text);
					break;
					
				default: 
//					System.out.println ("--[NMEA] Unhandled data packet <"+code+">");
				}
				
				Thread.yield ();
			}		
		}
		/*
		 * 
		 Supported
		 >  [NMEA] Parsing packet type <$GPGGA> <$GPGGA,133848,3801.2368,N,00110.2585,W,8,11,2.0,95.5,M,48.9,M,,*6D
		 >  [NMEA] Parsing packet type <$GPGLL> <$GPGLL,3801.2368,N,00110.2585,W,133848,V,S*5A
		 >  [NMEA] Parsing packet type <$GPRMC> <$GPRMC,133848,V,3801.2368,N,00110.2585,W,0.0,0.0,270104,1.6,W,S*13
		 >  [NMEA] Parsing packet type <$GPVTG> <$GPVTG,0.0,T,1.6,M,0.0,N,0.0,K*49
		 
		 Unsupported
		 >  [NMEA] Parsing packet type <$PGRMM> <$PGRMM,WGS 84*06
		 >  [NMEA] Parsing packet type <$HCHDG> <$HCHDG,140.2,,,1.6,W*3B
		 >  [NMEA] Parsing packet type <$GPRMB> <$GPRMB,V,1.96,R,,PERDIGUERA,3742.0000,N,00048.2000,W,25.969,137.7,,V,S*43
		 >  [NMEA] Parsing packet type <$GPBOD> <$GPBOD,133.4,T,135.0,M,PERDIGUERA,*4B
		 >  [NMEA] Parsing packet type <$GPBWC> <$GPBWC,133848,3742.0000,N,00048.2000,W,137.7,T,139.3,M,25.969,N,PERDIGUERA,S*52
		 >  [NMEA] Parsing packet type <$GPXTE> <$GPXTE,V,V,1.96,R,N,S*01
		 >  [NMEA] Parsing packet type <$PGRME> <$PGRME,15.0,M,22.5,M,27.0,M*1A
		 >  [NMEA] Parsing packet type <$PGRMZ> <$PGRMZ,368,f*09
		 
		 */
		
		private double parseLatLon (String coord, String suffix)
		{
			double		sgn;
			double		deg, min;
			double		value;
			
			if (suffix.equalsIgnoreCase ("N") || suffix.equalsIgnoreCase ("E"))
				sgn		= 1.0;
			else
				sgn		= -1.0;
			
			value	= Double.parseDouble (coord);
			deg		= Math.floor (value/100.0);
			value	= value/100.0-deg;
			min		= value*100.0/60.0;
			
			return sgn*(deg+min);
		}
		
		private void decodeGPGGA (String str)
		{
			StringTokenizer	st;
			String 			token;
			double 			lat;
			double 			lon;
			
			try
			{
				st = new StringTokenizer (str,",");
				
				if (st.countTokens() < 12)
				{
					System.out.println ("--[NMEA] Packet $GPGGA too short <" + st.countTokens() + ">");
					return;					
				}
				
				st.nextToken(); 							// $GPGGA
				st.nextToken(); 							// Time of fix (UTC)
				
				token	= st.nextToken(); 					// Latitude
				lat		= parseLatLon (token, st.nextToken());	
				
				token	= st.nextToken(); 					// Longitude
				lon		= parseLatLon (token, st.nextToken());	
				
				token	= st.nextToken(); 					// GPS quality
				fix		= Integer.parseInt(token);
				
				token	= st.nextToken();					// Number of satellites
				nsats	= Integer.parseInt(token);
				
				st.nextToken(); 							// HDOP
				
				token	= st.nextToken();					// Altitude
				height	= Double.parseDouble (token);
				
				pos.setLatLon (lat, lon);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
			}
		}
		
		private void decodeGPRMC (String str)
		{
			StringTokenizer	st;
			String 			token;
			double 			lat;
			double 			lon;
			
			try
			{
				st = new StringTokenizer (str,",");
				
				if (st.countTokens() < 8)
				{
					System.out.println ("--[NMEA] Packet $GPRMC too short <" + st.countTokens() + ">");
					return;					
				}
				
				st.nextToken(); 							// $GPRMC
				st.nextToken(); 							// Time of fix (UTC)
				token	= st.nextToken(); 					// Data status (A=OK, V=Warning)
				if (token.equalsIgnoreCase ("A"))
					fix		= GPSData.FIX_VALID;
				else
					fix		= GPSData.FIX_INVALID;
				
				token	= st.nextToken(); 					// Latitude
				lat		= parseLatLon (token, st.nextToken());	
				
				token	= st.nextToken(); 					// Longitude
				lon		= parseLatLon (token, st.nextToken());	
				
				st.nextToken(); 							// Speed over ground (knots)
				st.nextToken(); 							// Course made good (deg TRUE)
				st.nextToken(); 							// Date of fix
				st.nextToken(); 							// Magnetic variation
				
				pos.setLatLon (lat, lon);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
			}
		}
		
		private void decodeGPGLL (String str)
		{
			StringTokenizer	st;
			String 			token;
			double 			lat;
			double 			lon;
			
			try
			{
				st = new StringTokenizer (str,",");
				
				if (st.countTokens() < 8)
				{
					System.out.println ("--[NMEA] Packet $GPGLL too short <" + st.countTokens() + ">");
					return;					
				}
				
				st.nextToken(); 							// $GPGLL
				
				token	= st.nextToken(); 					// Latitude
				lat		= parseLatLon (token, st.nextToken());	
				
				token	= st.nextToken(); 					// Longitude
				lon		= parseLatLon (token, st.nextToken());	
				
				st.nextToken(); 							// Time of fix (UTC)
				token	= st.nextToken(); 					// Data status (A=OK, V=Warning)
				if (token.equalsIgnoreCase ("A"))
					fix		= GPSData.FIX_VALID;
				else
					fix		= GPSData.FIX_INVALID;
				
				pos.setLatLon (lat, lon);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
			}
		}
		
		private void decodeGPVTG (String str)
		{
			StringTokenizer	st;
			String 			token;
			double			spd, dir;
			double			vx, vy;
			
			try
			{
				st = new StringTokenizer (str,",");
				
				if (st.countTokens() < 7)
				{
					System.out.println ("--[NMEA] Packet $GPVTG too short <" + st.countTokens() + ">");
					return;					
				}
				
				st.nextToken(); 							// $GPVTG
				
				token	= st.nextToken();					// Course made good (deg TRUE)
				dir		= Double.parseDouble (token); 
				
				st.nextToken();								// T
				st.nextToken();								// Course made good (deg MAG)
				st.nextToken();								// M
				st.nextToken();								// Speed made good (knots)
				st.nextToken();								// N
				
				token	= st.nextToken();					// Speed made good (km/h)
				spd		= Double.parseDouble(token) / 3.6;
				st.nextToken();								// K
				
				vx		= spd * Math.cos (dir * Angles.DTOR);
				vy		= spd * Math.sin (dir * Angles.DTOR);
				vel.set (vx, vy, 0.0);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
			}
		}
		
		private void decodeGPGSA (String str)
		{
			StringTokenizer st;
			String token;
			int i;
			int nsats;
			
			if (dop == null) dop = new DOP();
			try
			{
				st = new StringTokenizer (str,",");
				
				st.nextToken(); //$GPGSA
				st.nextToken(); //mode MA
				
				if(st.nextToken().compareTo("1") != 0) //mode 123
				{
					
					nsats = st.countTokens() - 3; //hemos eliminado ya 3 tokens
					for (i=0; i < nsats; i++)
						st.nextToken(); //PRNs
					
					token = st.nextToken(); //pdop
					dop.setPDOP(Double.parseDouble(token));
					
					token = st.nextToken(); //hdop
					dop.setHDOP(Double.parseDouble(token));
					
					token = st.nextToken(); //vdop
					dop.setGDOP(Double.parseDouble(token));
				}
				//else System.out.println ("GSA no analizable");
			} catch (NumberFormatException ne)
			{
				dop = new DOP();
			}
			catch (java.util.NoSuchElementException nse)
			{
				System.out.print ("Error decodificando: "+str);
			}
		}
		
		private void decodeGPGSV (String str)
		{
			StringTokenizer st;
			String token;
			int i;
			int tprn;
			double telev;
			double tazim;
			int tsnr;
			
			st = new StringTokenizer (str,",*");
			
			try
			{
				st.nextToken(); //$GPGSV
				st.nextToken(); //# msg
				if (st.nextToken().compareTo("1") == 0) //msg #
				{
					token = st.nextToken(); //# sats
					numsats = satsremain = Integer.parseInt(token);
					sats = new LinkedList();
				}
				else
				{
					token =  st.nextToken(); //# sats
					numsats = Integer.parseInt(token);
				}
				if (numsats == 0)
					i=0;
				else
					i=(st.countTokens()-5)/4;
				
				for (; i > 0; i--)
				{
					token = st.nextToken(); //prn
					tprn = Integer.parseInt (token);
					
					token = st.nextToken(); //elev
					telev = Double.parseDouble(token);
					
					token = st.nextToken(); //azimuth
					tazim = Double.parseDouble(token);
					
					token = st.nextToken(); //snr
					tsnr = Integer.parseInt(token);
					
					sats.add (new SatelliteData (tprn,telev,tazim,tsnr));
					satsremain--;
				}
			} catch (java.util.NoSuchElementException nse)
			{
				System.out.print ("Error decodificando: "+str);
			}
		}
	}
	
	/* ---------------------------- */
	/*        INITIALISATION        */
	/* ---------------------------- */
	protected void initialise () throws NMEA0183Exception
	{
		CommPortIdentifier		id;
		
		try
		{
			id 		= CommPortIdentifier.getPortIdentifier (port);
			serial	= (SerialPort) id.open ("NMEA 0183", 10000);
			serial.setSerialPortParams (4800, 8, 1, SerialPort.PARITY_NONE);
			serial.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serial.enableReceiveTimeout (SER_TIMEOUT);
			
			input	= serial.getInputStream ();
			output	= serial.getOutputStream ();
			
			initDevice ();
		} catch (Exception e) 
		{
			throw new NMEA0183Exception (e.toString ());
		}
	}
	
	public void close () throws NMEA0183Exception
	{
		try
		{
			input.close ();
			output.close ();
			serial.close ();
		} catch (Exception e) { throw new NMEA0183Exception (e.getMessage()); }
	}
	
	protected void initDevice () throws NMEA0183Exception 
	{
		Thread			thread;
		
		thread	= new Thread (parser);
		thread.start (); 
		
		askServices ("GSV");
		askServices ("GSA");
	}
	
	/* ---------------------------- */
	/*       REQUEST PROTOCOL       */
	/* ---------------------------- */
	protected void askServices (String code) throws NMEA0183Exception
	{
		String			cmd;
		
		cmd	= "$PMOTG,"+code+",0001\015\012";
		
		try
		{
			output.write (cmd.getBytes ());			
		}
		catch (Exception e)
		{
			throw new NMEA0183Exception (e.toString ());
		}
	}
	
	/* ---------------------------- */
	/*         GPS INTERFACE        */
	/* ---------------------------- */
	public GPSData getData ()
	{
		data.setPos (pos);		
		data.setAltitude (height);
		data.setVel (vel);
		data.setWeekSeconds (0);
		data.setFix (fix);
		data.setNumSat (nsats);			
		
		return data;
	}
	
	public SatelliteData[] getSatellites ()
	{	
		int					i;
		SatelliteData[]		asats;
		
		if (sats == null)			return null;
		
		asats = new SatelliteData[sats.size()];
		for (i = 0; i < sats.size(); i++)
			asats[i] = (SatelliteData) sats.get(i);
		
		return asats;
	}
}



