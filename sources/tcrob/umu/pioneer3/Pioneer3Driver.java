/*
 * (c) 2004 Humberto Martinez
 */
 
package tcrob.umu.pioneer3;
 
import java.io.*;
import java.util.*;

import javax.comm.*;

public class Pioneer3Driver extends Object
{
	// ------------------------
	// Timeouts and sizes
	// ------------------------

	static public final int			SER_TIMEOUT		= 500; 		// Timeout for serial communication (ms)
	static public final int			AROS_BUFFER		= 206; 		// Maximum AROS data packet
	static public final int			AROS_HEADER		= 5; 		// Number of AROS header bytes

	// ------------------------
	// Properties constants
	// ------------------------

	static public final String		SER_PORT		= "SERPORT";	// Serial port id 
	static public final String		SER_BRATE		= "SERBRATE"; 	// Serial port baud rate

	// ------------------------
	// Protocol constants
	// ------------------------

	static public final int			HEADER1			= 0xFA;
	static public final int			HEADER2			= 0xFB;

	// ------------------------
	// AROS command set
	// ------------------------

	// AROS SIP codes
	static public final int			SYNC0			= 0;		// Synchronization header 0
	static public final int			SYNC1			= 1;		// Synchronization header 1
	static public final int			SYNC2			= 2;		// Synchronization header 2
	static public final int			SIP0			= 50;		// Standard SIP packet (stopped)
	static public final int			SIP1			= 51;		// Standard SIP packet (moving)
	
	// AROS command codes
	static public final int			PULSE			= 0;		// AROS keep-alive packet
	static public final int			OPEN			= 1;		// Start AROS services (opening a connection)
	static public final int			CLOSE			= 2;		// Stop AROS services (closing a connection)
	static public final int			ENABLE			= 4;		// Enable/disable the motors
	static public final int			SETV			= 6;		// Set maximum velocity
	static public final int			ENCODER			= 19;		// Request encoder data (single or continuous)
	static public final int			STOP			= 29;		// Stop the robot (do not disable motors)
	static public final int			VEL2			= 32;		// Independent wheel velocities

	// AROS data types
	static public final int			INT				= 0x3B;		// Positive integer (two bytes)
	static public final int			SINT			= 0x1B;		// Signed integer (two bytes)
	static public final int			STR				= 0x2B;		// String

	// ------------------------
	// SIP register fields
	// ------------------------
	static public final int			LVEL			= 7;		// Signed integer (two bytes)
	static public final int			RVEL			= 9;		// Signed integer (two bytes)
	static public final int			SON_COUNT		= 19;		// Byte
	static public final int			SON_NUMBER		= 20;		// Byte
	static public final int			SON_RANGE		= 21;		// Positive integer (two bytes)
	
	// ------------------------
	// Hardware constants
	// ------------------------

	static public final double		KVEL			= 1000.0;	// Velocity conversion (m/s -> mm/s)
	static public final double		KMOT			= 50.0;		// Motor speed conversion (m/s -> 20 mm/s)
	static public final int			KMOTMAX			= 120;		// Maximum motor speed value (20 mm/s)

	static public final int			SON_NUM			= 16;		// Maximum number of sonar sensors	
	static public final double		SON_NULL		= -100.0;	// Wrong sonar sensor value	
	static public final double		KSON			= 0.001;	// Sonar range conversion (mm -> m)
		
	// ------------------------------
	// Interpreter and communications
	// ------------------------------

	// Runtime and state data
	private Pioneer3DriverParser	parser;
	private SerialPort				serial;
	private InputStream				input;
	private OutputStream			output;
	private Integer					synchro;					// AROS initial synchronization lock
	private int[]					packet;						// Serial data buffer
	protected int[]					data;						// AROS commands data buffer
	protected boolean				debug		= false;
	
	// ------------------------
	// Sensor and hardware values
	// ------------------------

	// Variables for storing input data
  	public double 					odom_motL;
	public double 					odom_motR;
	public double[]					sons;
	public boolean[]				bumpers;
  
  	// Variables for storing state flags
  	public boolean					odom_flg	= false;
  	
	// Constructors
	public Pioneer3Driver (Properties props)
	{
		initialiseComms (props);
		initialiseHW ();
		initialiseAROS ();
	}

	// Class methods
	static public String SIPToString (int command)
	{
		switch (command)
		{
		case SYNC0:			return "SYNC0";
		case SYNC1:			return "SYNC1";
		case SYNC2:			return "SYNC2";
		case SIP0:			return "SIP(STOPPED)";
		case SIP1:			return "SIP(MOVING)";
		default:			return "N/A="+command;
		}
	}

	static public String cmdToString (int command)
	{
		switch (command)
		{
		case PULSE:			return "PULSE";
		case OPEN:			return "OPEN";
		case CLOSE:			return "CLOSE";
		case ENABLE:		return "ENABLE";
		case SETV:			return "SETV";
		case ENCODER:		return "ENCODER";
		case STOP:			return "STOP";
		case VEL2:			return "VEL2";
		default:			return "N/A="+command;
		}
	}

	static public String cmdPacketToString (int[] data, int datainit, int datalen)
	{
		int				i;
		String			out;
		
		out		= "<" + cmdToString(data[datainit]) + ">";
		for (i = 1; i < datalen; i++)
			out		+= " " + data[datainit+i];

		return out;
	}
	
	static public String SIPPacketToString (int[] data, int datainit, int datalen)
	{
		int				i;
		String			out;
		
		out		= "<" + SIPToString(data[datainit]) + ">";
		for (i = 1; i < datalen; i++)
			out		+= " " + data[datainit+i];

		return out;
	}
	
	static public int bytesToInt (byte hi, byte lo)
	{
		int			out;
		
		out		= (int) ((int) ((hi << 8) & 0xFF00) | (int) (lo & 0xFF));
		if (hi < 0)
		{
			out --;
			out		= out ^ 0xFFFF;
			out		= -out;
		}
	  		
	  	return out;			
	}

	static public int intToByte (int val)
	{
		int			out;
		int			uval;
		
		if (val < 0)
		{
			uval	= 0xFF - Math.abs (val) + 1;
			out		= uval % 0x100;
		}
		else
			out		= val % 0x100;
			
		return out;
	}
	
	class Pioneer3DriverParser implements Runnable
	{
		static public final int		ST_INIT				= 0;
		static public final int		ST_HEADER1			= 1;
		static public final int		ST_HEADER2			= 2;
		static public final int		ST_SIZE				= 3;
		static public final int		ST_DATA				= 4;
		
		protected Thread			thread;
		protected boolean			running;
		
		public Pioneer3DriverParser ()
		{
			thread	= new Thread (this);
			thread.start ();
		}
		
		public void run ()
		{
			int				state;
			int				inbyte, incount;
			int[]			indata;
			int				size;
			
			Thread.currentThread ().setName ("TC-Pioneer3Drv-Parser");	
			System.out.println ("  [Pioneer3Drv] Piooner3 AROS protocol parser (listener thread) ready");
      
			running		= true;
			indata		= new int[AROS_BUFFER];
			incount		= 0;
			size		= 0;
			state		= ST_HEADER1;
			while (running)
			{
				Thread.yield ();
				
				// Receive data
				inbyte	= -1;
				try
				{
					inbyte	= input.read ();
				}
				catch (Exception e) { /* e.printStackTrace (); */ }
				
				if (inbyte < 0)			continue;

				// Parse current packet
				switch (state)
				{
				case ST_HEADER1:
					if (inbyte == HEADER1)
						state	= ST_HEADER2;
					break;

				case ST_HEADER2:
					if (inbyte == HEADER2)
						state	= ST_SIZE;
					else
						state	= ST_HEADER1;
					break;

				case ST_SIZE:
					size	= inbyte;
					incount	= 0;
					state	= ST_DATA;
					break;
					
				case ST_DATA:
					indata[incount]	= inbyte;
					incount ++;
					
					if (incount == size)
					{
						state	= ST_HEADER1;
						parseSIP (indata, size);						
					}
					break;
				}
			}
		}
		
		protected void parseSIP (int[] data, int datalen)
		{			
			int				chksum;
			
			if (debug) System.out.println ("  [Pioneer3Drv] RECEIVED = "+SIPPacketToString (data, 0, datalen));		
			
			// Compute checksum
			chksum		= calcChecksum (data, datalen-2);
			if ((data[datalen-2] != chksum / 0x100) || (data[datalen-1] != chksum % 0x100))
			{
				System.out.println ("--[Pioneer3Drv] Wrong SIP packet checksum");
				
				return;
			}

			switch (data[0])
			{
			case SYNC0:			
			case SYNC1:			
			case SYNC2:			
				synchronized (synchro)
				{
					synchro.notify ();
				}
				break;
				
			case SIP0:
			case SIP1:
				int			i;
				int			num, val;
				int			count;

				// Clean previous sonar values
				for (i = 0; i < SON_NUM; i++)
					sons[i]	= SON_NULL;
				
				// Update new sonar sensors
				count	= data[SON_COUNT];
				for (i = 0; i < count; i++)
				{
					num			= data[SON_NUMBER+i*3];
					val			= bytesToInt ((byte) data[SON_RANGE+i*3+1], (byte) data[SON_RANGE+i*3]);
					
					sons[num]	= (double) val * KSON;												// [m]
				}
				
				// Update odometry information
				odom_flg	= true;
				odom_motL	= (double) bytesToInt ((byte) data[LVEL+1], (byte) data[LVEL]) / KVEL;	// [m/s] 
				odom_motR	= (double) bytesToInt ((byte) data[RVEL+1], (byte) data[RVEL]) / KVEL;	// [m/s] 
				break;
				
			default:
				System.out.println ("--[Pioneer3Drv] Wrong SIP packet received <"+data[0]+">");
			}
		}
		
		public void stop ()
		{
			running		= false;
		}
	}
	
	// Instance methods
	private void initialiseComms (Properties props)
	{
		CommPortIdentifier		id;
		String					port;
		int						brate;
		
		// Parse properties
		port			= props.getProperty (SER_PORT);
		try { brate	 	= new Integer (props.getProperty (SER_BRATE)).intValue (); } 		catch (Exception e) 		{ brate		= 9600; }

		// Create communication data buffers
		packet			= new int[AROS_BUFFER];
		data			= new int[AROS_BUFFER];
		
		// Open serial driver
		try
		{
			id 		= CommPortIdentifier.getPortIdentifier (port);
			serial	= (SerialPort) id.open ("Pioneer3", 10000);
			serial.setSerialPortParams (brate, 8, 1, SerialPort.PARITY_NONE);
			serial.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serial.enableReceiveTimeout (SER_TIMEOUT);
			
			input	= serial.getInputStream ();
			output	= serial.getOutputStream ();

		} catch (Exception e) { e.printStackTrace (); }		
	}
	
	private void shutdownComms ()
	{
		try
		{
			input.close ();
			output.close ();
			serial.close ();
		} catch (Exception e) { }		
	}
	
	private void initialiseAROS ()
	{
		debug		= true;
		
		// Create AROS data structures
		synchro		= new Integer (0);
		parser		= new Pioneer3DriverParser ();
		
		System.out.println ("  [Pioneer3Drv] Synchronizing AROS server");
		
		sendCommand (SYNC0);
		synchronized (synchro)
		{
			try { synchro.wait (); } catch (Exception e) { }
		}
		
		sendCommand (SYNC1);
		synchronized (synchro)
		{
			try { synchro.wait (); } catch (Exception e) { }
		}
		
		sendCommand (SYNC2);
		synchronized (synchro)
		{
			try { synchro.wait (); } catch (Exception e) { }
		}
		
		sendCommand (OPEN);

		debug		= false;
		System.out.println ("  [Pioneer3Drv] AROS connection established");
	}

	private void shutdownAROS ()
	{
		if (parser != null)		parser.stop ();
		parser	= null;
		
		sendCommand (STOP);
		setEnableMotors (false);
		sendCommand (CLOSE);

		System.out.println ("  [Pioneer3Drv] AROS connection shutted down");
	}

	private void initialiseHW ()
	{
		int			i;

		// Create data structures
		sons	= new double[SON_NUM];
		bumpers	= new boolean[4];
		
		// Initialise sonars
		for (i = 0; i < SON_NUM; i++)
			sons[i]	= SON_NULL;
	}
	
	public void stop ()
	{
		shutdownComms ();
		shutdownAROS ();
	}
	
	protected void sendCommand (int command)
	{
		data[0]		= command;
		
		sendCommand (data, 1);
	}

	protected void sendCommand (int[] data, int datalen)
	{
		int			i;
		int			chksum;
		
		chksum		= calcChecksum (data, datalen);
		
		packet[0] 	= HEADER1;
		packet[1] 	= HEADER2;
		packet[2] 	= datalen + 2;
		for (i = 0; i < datalen; i++)
			packet[3+i]	= data[i];
		packet[3+i]	= chksum / 0x100;
		packet[4+i]	= chksum % 0x100;
		
		try
		{
			for (i = 0; i < datalen + AROS_HEADER; i++)
				output.write (packet[i]);
			output.flush ();
		}
		catch (Exception e) { e.printStackTrace (); }
		
		if (debug) System.out.println ("  [Pioneer3Drv] SENT = "+cmdPacketToString (packet, 3, datalen + 2));
	}
	
	protected int calcChecksum (int[] data, int datalen)
	{
		int			i, n;
		int			chk;
		
		chk		= 0;
		for (i = 0, n = datalen; n > 1; n -= 2, i += 2)
		{
			chk		+= bytesToInt ((byte) data[i], (byte) data[i+1]);
			chk		= chk & 0xFFFF;
		}
		
		if (n > 0)
			chk		= chk ^ data[i];
		
		return chk;
	}

	public void setEnableMotors (boolean enable)
	{
		data[0]		= ENABLE;
		data[1]		= INT;
		if (enable)
			data[2]		= 0x01;
		else
			data[2]		= 0x00;
		data[3]		= 0x00;
		
		sendCommand (data, 4);
	}

	public void setMotors (double motL, double motR)
	{
		int				ileft, iright;
		
		// Convert commands to driver specific format
		ileft	= (int) Math.round (motL * KMOT);							// [20 mm/s] 
		iright	= (int) Math.round (motR * KMOT);							// [20 mm/s] 

		// Check that values are inside the limits
		ileft	= Math.min (Math.max (ileft, -KMOTMAX), KMOTMAX);
		iright	= Math.min (Math.max (iright, -KMOTMAX), KMOTMAX);
		
		data[0]	= VEL2;
		data[1]	= INT;
		data[2]	= intToByte (iright);
		data[3]	= intToByte (ileft);
		
		sendCommand (data, 4);
	}
	
	public void setMaxVelocity (double mvel)
	{
		int				imvel;
		
		imvel	= (int) Math.round (mvel * KVEL);							// [mm/s] 
		
		data[0]	= SETV;
		data[1]	= INT;
		data[2]	= imvel % 0x100;
		data[3]	= imvel / 0x100;

		sendCommand (data, 4); 
	}
}