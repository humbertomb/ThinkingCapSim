/*
 * (c) 2003-2004 Humberto Martinez
 */

package devices.drivers.gps.Garmin;

import javax.comm.*;
import java.awt.*;

import devices.data.*;
import devices.drivers.gps.*;

import devices.pos.*;
import wucore.utils.math.*;

public class Garmin extends GPS
{
	// ------------------------
	// Timeouts and sizes
	// ------------------------

	static public final int	SER_TIMEOUT			= 500; 		// Timeout for serial communication (ms)
	static public final int 	WAIT_TIMEOUT 		= 250; 		// Timeout for active waits (ms)
	static public final int 	MAX_TRKLOG	 		= 100; 		// Maximum number of track logs
	static public final int 	MAX_SATS	 			= 12; 		// Maximum number of tracked satellites
	
	// ------------------------
	// Garmin IOP protocol keys (names taken from the manual)
	// ------------------------

	// Packet Boundaries.
	public static final int 	DLE 					= 16; 		// Data link escape. Packet boundary.
	public static final int 	ETX 					= 3;			// End of text. Packet boundary.
	
	// L000 protocol IDs.
	public static final int 	Pid_Ack_Byte 		= 6;
	public static final int 	Pid_Nak_Byte 		= 21;
	public static final int 	Pid_Protocol_Array 	= 253;
	public static final int 	Pid_Product_Rqst 	= 254;
	public static final int 	Pid_Product_Data 	= 255;
	
	// L001 protocol IDs.
	public static final int 	Pid_Command_Data 	= 10;
	public static final int 	Pid_Xfer_Cmplt 		= 12;
	public static final int 	Pid_Date_Time_Data 	= 14;
	public static final int 	Pid_Position_Data 	= 17;
	public static final int 	Pid_Prx_Wpt_Data 	= 19;
	public static final int 	Pid_Records 			= 27;
	public static final int 	Pid_Rte_Hdr 			= 29;
	public static final int 	Pid_Rte_Wpt_Data 	= 30;
	public static final int 	Pid_Almanac_Data 	= 31;
	public static final int 	Pid_Trk_Data 		= 34;
	public static final int 	Pid_Wpt_Data 		= 35;
	public static final int 	Pid_Pvt_Data 		= 51;
	public static final int 	Pid_Rte_Link_Data 	= 98;
	public static final int 	Pid_Trk_Hdr 			= 99;

	// L00* protocol IDs. - undocumented -
	public static final int 	Pid_Sat_Data 		= 26;
	public static final int 	Pid_Event_Data 		= 28;
	public static final int 	Pid_Unknown 			= 114;

	// A010 - Device Command Protocol.
	public static final int 	Cmnd_Abort_Transfer = 0;		// Abort current transfer. 
	public static final int 	Cmnd_Transfer_Alm 	= 1;		// Transfer almanac. 
	public static final int 	Cmnd_Transfer_Posn 	= 2;		// Transfer position. 
	public static final int 	Cmnd_Transfer_Prx 	= 3;		// Transfer proximity waypoints. 
	public static final int 	Cmnd_Transfer_Rte	= 4;		// Transfer routes. 
	public static final int 	Cmnd_Transfer_Time 	= 5;		// Transfer time. 
	public static final int 	Cmnd_Transfer_Trk	= 6;		// Transfer track log. 
	public static final int 	Cmnd_Transfer_Wpt	= 7;		// Transfer waypoints. 
	public static final int 	Cmnd_Turn_Off_Pwr 	= 8;		// Turn off power. 
	public static final int 	Cmnd_Start_Pvt_Data = 49;		// Start transmitting PVT (Position, velocity, time) Data.
	public static final int		Cmnd_Stop_Pvt_Data	= 50;		// Stop transmitting PVT (Position, velocity, time) Data. 

	// A??? - Event Command Protocol. - undocumented -
	public static final int 	Mask_Disable	 	= 0x0000;	// Disable sending events
	public static final int 	Mask_Enable_Sat 	= 0x8000;	// Enable satellite events
	public static final int 	Mask_Enable_All 	= 0xffff;	// Enable all events

	// A100 - Waypoint Protocol Types.								IMPLEMENTED
	public static final int		WP_D100				= 100;		// *
	public static final int		WP_D101				= 101;
	public static final int		WP_D102				= 102;
	public static final int		WP_D103				= 103;
	public static final int		WP_D104				= 104;
	public static final int		WP_D105				= 105;
	public static final int		WP_D106				= 106;
	public static final int		WP_D107				= 107;
	public static final int		WP_D108				= 108;		// *
	public static final int		WP_D109				= 109;
	public static final int		WP_D150				= 150;
	public static final int		WP_D151				= 151;
	public static final int		WP_D152				= 152;
	public static final int		WP_D154				= 154;
	public static final int		WP_D155				= 155;

	// A300, A301 - Track Log Protocol Types.
	public static final int		TRK_D300				= 300;		// *
	public static final int		TRK_D301				= 301;		// *

	// ------------------------
	// Protocol parser states
	// ------------------------
	public static final int		ST_WAIT				= 0;		// Waiting for answer
	public static final int		ST_POS				= 1;		// Position data received
	public static final int		ST_RECORD			= 2;		// Record data completed
	public static final int		ST_PRODUCT			= 3;		// Product info received
	public static final int		ST_TIME				= 4;		// Time data received
	
	// ---------------------------
	// Protocol type for records
	// --------------------------
	public static final int		REC_WP				= 0;		// Waypoint requested
	public static final int		REC_TRACK			= 1;		// Track requested
	public static final int		REC_ROUTE			= 2;		// Route requested

	// ----------------------------------
	// Protocol data and state variables
	// ----------------------------------

	// Common data types
	protected Color[]		tcolor				= { Color.BLACK, Color.RED.darker(), Color.GREEN.darker(), 
														Color.YELLOW.darker(), Color.BLUE.darker(), Color.MAGENTA.darker(),
														Color.CYAN.darker(), Color.GRAY.darker(), Color.GRAY.darker(),
														Color.RED, Color.GREEN, Color.YELLOW,
														Color.BLUE, Color.MAGENTA, Color.CYAN,
														Color.WHITE
														};
	
	// D600 Date and Time type
	protected String			dtime;							// Current date and time
	
	// D700 Position type
	protected LLAPos			pos;
	
	// D800 PVT Data type
	protected LLAPos			pvt_pos;						// Latitude/Longitude of location
	protected double			pvt_alt;						// Altitude of location
	protected float				pvt_epe; 						// Estimated position error
	protected float				pvt_eph; 						// Horizontal epe
	protected float				pvt_epv; 						// Vertical epe
	protected int				pvt_fix;				 		// Position fix
	protected double			pvt_tow;		 				// Time of week (seconds).
	protected float				pvt_veast; 		 				// Velocity east.
	protected float				pvt_vnorth;				 		// Velocity north.
	protected float				pvt_vup;						// Velocity up.
	protected float				pvt_mslh;						// Mean sea level height
	protected int				pvt_leaps;						// Time difference between GPS and GMT (UTC)
	protected long				pvt_wnd;		 				// Week number days. 

	// D??? - Event Data types - undocumented -
	protected SatelliteData[]	sats;							// Tracked satellites	
	protected int				nsats;							// Number of tracked satellites
	
	// D1** Waypoint type	
	protected int				wpformat			= WP_D108;
	protected WaypointData[]	wpdata;
	
	// D300, D301, D310 Track type
	protected int				trkformat			= TRK_D301;
	protected TracklogData[]	trkdata;
	protected int				trkndx;
	protected boolean			trkfirst;
	
	// Common data
	protected int				status;							// Status of reception protocol
	protected int				record;							// Protocol type for record
	protected boolean			newack;							// An ACK has been received
	protected boolean			newnack;						// A NACK has been received
	
	// ------------------------------
	// Interpreter and communications
	// ------------------------------

	// Runtime and state data
	protected int[]				packet;
	protected GPSData			data;
	private GarminParser		parser;
	private SerialPort			serial;
	private GarminInputStream	input;
	private GarminOutputStream	output;
	protected boolean			debug		= false;

	// Constructor 
	public Garmin () 
	{
		int				i;
		
		parser		= new GarminParser ();
		data		= new GPSData ("WGS-84");
		sats		= new SatelliteData[MAX_SATS];
		
		for (i = 0; i < MAX_SATS; i++)
			sats[i]		= new SatelliteData ();
		
		// Initialise data types
		pos			= new LLAPos (0.0, 0.0);
		pvt_pos		= new LLAPos (0.0, 0.0);
	}

	public Garmin (String port) throws GarminException
	{
		this ();
		
		setPort (port);
		initialise ();		
	}

	class GarminParser implements Runnable
	{
		boolean			running;
		int				pcount;
		boolean			newdata;
	
		public void run ()
		{
			double			lat, lon, alt;

			Thread.currentThread ().setName ("GarminParser");	
			System.out.println ("  [Garmin] Garmin IOP protocol parser (listener thread) ready");
      
			running		= true;
			while (running)
			{
				// Receive data
				newdata	= false;
				try
				{
					packet	= input.readPacket ();
					
//					if (isLegal () != -1)	// TODO the check seems not to be valid
//						throw new GarminException ("--[Garmin] Error reading byte no. " + isLegal ());

					newdata	= true;
				}
				catch (GarminException ge)
				{
					System.err.println (ge.toString ());
				}
				catch (Exception e)
				{
					e.printStackTrace ();
				}
				
				// Parse current packet
				if (newdata)
					switch (getID ())
					{
					case Pid_Ack_Byte:
						newack		= true;
						break;
							
					case Pid_Nak_Byte:
						newnack		= true;
						System.out.println ("--[Garmin] NACK received");
						break;
							
					case Pid_Product_Data:				// A000
						int				prod, vers;
						String			desc;
						
						prod	= readWord (3);
						vers	= readWord (5);
						desc	= readNullTerminatedString (7);
	
						System.out.println ("\tGARMIN GPS initialised");
						System.out.println ("\t" + desc);
						System.out.println ("\tSoftware version: " + vers);
						System.out.println ("\tProduct ID: " + prod);	
			
						status	= ST_PRODUCT;						
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Protocol_Array:			// A001
						System.out.println ("  [Garmin] Discarding A001 packet (Protocol Capabilities)");	
		
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Pvt_Data:					// D800
						if (getDataLength () != 64)
							System.out.println ("--[Garmin] D800 packet too short <" + getDataLength () + ">");

						alt			= (double) readFloat(3);
						pvt_epe 	= readFloat(7);
						pvt_eph 	= readFloat(11);
						pvt_epv 	= readFloat(15);
						pvt_fix 	= readWord(19);
						pvt_tow 	= readDouble(21);
						lat 		= readDouble(29) * Angles.RTOD;
						lon 		= readDouble(37) * Angles.RTOD;
						pvt_veast 	= readFloat(45);
						pvt_vnorth 	= readFloat(49);
						pvt_vup 	= readFloat(53);
						pvt_mslh 	= readFloat(57);
						pvt_leaps	= readWord(61);
						pvt_wnd		= readLong(63);
		
						pvt_pos		= new LLAPos (lat, lon);
						pvt_alt		= alt+pvt_mslh;
						break;

					case Pid_Position_Data:				// D700						
						if (getDataLength () != 16)
							System.out.println ("--[Garmin] D700 packet too short <" + getDataLength () + ">");
	
						lat 	= readDouble (3) * Angles.RTOD;
						lon 	= readDouble (11) * Angles.RTOD;
						pos.setLatLon (lat, lon);
						pvt_alt = 0.0;
						
						status	= ST_POS;	
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
							
					case Pid_Date_Time_Data:			// D600
						int			month, day, year;
						int			hour, minute, second;
						
						if (getDataLength () != 8)
							System.out.println ("--[Garmin] D600 packet too short <" + getDataLength () + ">");
	
						month 	= readByte(3);
						day 	= readByte(4);
						year 	= readWord(5);
						hour 	= readWord(7);
						minute 	= readByte(9);
						second 	= readByte(10);
						
						dtime	= day+"/"+month+"/"+year + " " + hour+":"+minute+":"+second;

						status	= ST_TIME;
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Sat_Data:					// D???
						parseSats ();
						break;
											
					case Pid_Unknown:
//System.out.println ("nuevo loquesea len="+getDataLength ());
						
						// sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Records:					// RECORD
						int			len;

						if (getDataLength () != 2)
							System.out.println ("--[Garmin] RECORD packet too short <" + getDataLength () + ">");
	
						len	= readWord (3);					
						switch (record)
						{
						case REC_WP:
							wpdata		= new WaypointData[len];
							break;
						
						case REC_TRACK:
							trkdata		= new TracklogData[MAX_TRKLOG];
							trkdata[0]	= new TracklogData ();
							trkndx		= 0;
							trkfirst	= true;
							break;
							
						case REC_ROUTE:
							break;
							
						default:
							System.out.println ("--[Garmin] RECORD type <" + record + "> not supported");							
						}
						
						if (progress != null)		progress.setTotalSteps (len);
						pcount	= 0;

						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
				
					case Pid_Xfer_Cmplt:				// RECORD
//						packet	= readWord (3);

						status	= ST_RECORD;
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Wpt_Data:					// A100 - <D0>
						if (progress != null)		progress.incrementSteps (1);
						
						parseWaypoint ();					
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Trk_Data:					// A300,A301 - <D0>,<D1>
						if (progress != null)		progress.incrementSteps (1);
						
						parseTrack ();					
						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					case Pid_Trk_Hdr:					// D310
						boolean			dsply;
						String			id;
						int				col;

						if (!trkfirst)
						{
							trkndx ++;
							trkdata[trkndx]	= new TracklogData ();						
						}
						trkfirst	= false;
						
						dsply	= (readByte (3) > 0);
						col 	= readByte (4);
						id		= readNullTerminatedString (5);

						trkdata[trkndx].set (id, dsply);
						if (col != 0xFF)		
							trkdata[trkndx].color	= tcolor[col];

						sendAckPacket (Pid_Ack_Byte, getID ());
						break;
						
					default:
						System.out.println ("--[Garmin] Unhandled data packet type <" + idToString (getID ()) + " " + getID () + ">");
					}
				
				Thread.yield ();
			}
		}
		
		private void parseWaypoint ()
		{
			String			id = null;
			double			lat, lon, alt;
			int				col, cls, smbl;
			WaypointData	wp;
					
			wp	= new WaypointData ();
			switch (wpformat)
			{
			case WP_D100:
				id		= readFixedString (3, 6);
				lat 	= (double) readLong (9) * GPS.SEMIC_UNIT;
				lon 	= (double) readLong (13) * GPS.SEMIC_UNIT;
							
				wp.set (id, lat, lon, 0.0);
				break;
				
			case WP_D108:
				cls 	= readByte (3);
				col 	= readByte (4);
				smbl	= readWord (7);
				lat 	= (double) readLong (27) * GPS.SEMIC_UNIT;
				lon 	= (double) readLong (31) * GPS.SEMIC_UNIT;
				alt 	= (double) readLong (35);
				id		= readNullTerminatedString (55);
							
				wp.set (id, lat, lon, alt);
				wp.cls		= cls;
				wp.symbol	= smbl;				
				if (col != 0xFF)		wp.color	= tcolor[col];
				break;
				
			default:
				System.out.println ("--[Garmin] Waypoint type <D" + wpformat + "> not implemented");
			}

			wpdata[pcount]	= wp;
			pcount ++;
		}
		
		private void parseTrack ()
		{
			double			lat, lon, alt;
			double			depth;
			long			time;
			boolean			newtrk = false;
			TracklogData	trk;

			trk		= trkdata[trkndx];
			switch (trkformat)
			{
			case TRK_D300:
				lat 	= (double) readLong (3) * GPS.SEMIC_UNIT;
				lon 	= (double) readLong (7) * GPS.SEMIC_UNIT;
				time 	= readLong (11);
				newtrk	= (readByte (15) > 0);

				trk.add (lat, lon, 0.0, time, newtrk);
				break;
				
			case TRK_D301:
				lat 	= (double) readLong (3) * GPS.SEMIC_UNIT;
				lon 	= (double) readLong (7) * GPS.SEMIC_UNIT;
				time 	= readLong (11);
				alt 	= (double) readFloat (15);
				depth 	= (double) readFloat (19);
				newtrk	= (readByte (23) > 0);
	
				trk.add (lat, lon, alt, time, newtrk);
				trk.pts[trk.npts-1].depth	= depth;
				break;
				
			default:
				System.out.println ("--[Garmin] Track Log type <D" + trkformat + "> not implemented");
			}

			pcount ++;
		}
		
		private void parseSats ()
		{
			int				i;
			int				prn, snr;
			int				elev, azim;
			boolean			valid;
							
			nsats	= 0;
			for (i = 0; i < MAX_SATS; i++)
			{
				azim 	= readWord(i*8+3);		// Frac phase ???
				snr 	= readWord(i*8+5);
				elev 	= readByte(i*8+7);
				prn 	= readByte(i*8+8)+1;
				valid	= (readByte (i*8+9) > 2);
				
				if (valid)		nsats++;
				sats[i].set (prn, (double) elev, (double) azim, snr, valid);
			}
		}
	}

	// Class methods
	
	/* ---------------------------- */
	/*     UTILITY FUNCTIONS        */
	/* ---------------------------- */

	/**
	* Method that translates a packet-id into a human-readable string. 
	*/	
	public static String idToString (int id) 
	{
		switch (id) {
			case Pid_Ack_Byte :	
				return "Acknowledge packet";
			case Pid_Command_Data :
				return "Command packet";
			case Pid_Date_Time_Data :
				return "Date and time data";
			case Pid_Nak_Byte :
				return "Not acknowledged packet";
			case Pid_Product_Data :
				return "Product data.";
			case Pid_Product_Rqst :
				return "Product request";
			case Pid_Protocol_Array :
				return "Protocol array packet";
			case Pid_Position_Data :
				return "Position data";
			case Pid_Pvt_Data :
				return "PVT data";
			case Pid_Records :
				return "Start of record transfer";
			case Pid_Wpt_Data :
				return "Waypoint data";
			default :
				return "Unknown data";
		}
	}
	
	// Instance methods
	
	/* ---------------------------- */
	/*     DATA PACKETS FORMAT      */
	/* ---------------------------- */
	/**
	* Calculates the checksum for the packet.
	* Does <b> not </b> insert it into the correct position of the int[] packet array. <br/>
	* The method assumes that the packet is a valid Garmin-packet with all values containing their final
	* values.
	*/	
	public int calcChecksum () 
	{
		int sum = 0;
		for (int i = 1 ; i <= packet.length - 4 ; i++) 
		{
			sum += packet[i];		
		}		
		
		sum = sum % 256;
		sum = sum ^ 255;						
		sum += 1;
		return sum;	
	}

	/**
	* Returns the ID (ie. type) of the packet.
	*/ 
	public int getID() {
		return packet[1];
	}
	
	/**
	* Returns the amount of bytes in the data-field of this packet.	
	*/ 
	public int getDataLength() {
		return packet[2];
	}
		
	/**
	* Returns the length of the entire packet in bytes.
	*/	
	protected int getLength() {
		return packet.length;
	}
	
	/**
	* Method that reads a Garmin-word in the packet and returns it as an int.
	* This method can be used to read both int and word from a Garmin-packet.
	*/	
	protected int readWord(int packet_index) {
		int sum = packet[packet_index++];
		sum += packet[packet_index++] << 8;
		return sum;
	}

	/**  
	* Method that reads a Garmin-long in the packet and returns it as an int.
	*/ 	
	protected int readLong(int packet_index) {
		int res = packet[packet_index++];
		res += packet[packet_index++] << 8;
		res += packet[packet_index++] << 16;
		res += packet[packet_index++] << 24;
		
		return res;
	}

	/**
	* Method that reads a null-terminated string.
	*/
	protected String readNullTerminatedString(int packet_index) 
	{
		StringBuffer	res = new StringBuffer(20);
		while ((packet[packet_index] != 0) && (packet_index != packet.length )) 
			res.append ((char) packet[packet_index++]);			
			
		return res.toString();
	}
	
	/**
	* Method that reads a fixed length string.
	*/
	protected String readFixedString(int packet_index, int length) 
	{
		int				i;
		StringBuffer	res = new StringBuffer(length);
		
		for (i = 0; i < length; i++) 
			res.append ((char) packet[packet_index+i]);			

		return res.toString();
	}
	
	/**
	* Checks if the packet is valid with regards to header, footer,data-field-length and checksum.
	* Returns the index of the illegal byte. If packet is ok, -1 is returned.
	*/
	protected int isLegal() 
	{
		if (packet[0] != DLE)
			return 0;
			
		int size = packet[2];
		
		if (size + 6 != packet.length)
			return 2;
			
		if ( packet[packet.length  - 3] != calcChecksum() )
			return packet.length  - 3;
			
		if ( packet[packet.length  - 2] != DLE )
			return packet.length  - 2;
		
		if ( packet[packet.length  - 1] != ETX )
			return packet.length  - 1;
			
		return -1;
	}
	
	/**
	* Method that reads a Garmin-byte in the packet and returns it as a short.
	*/	
	protected short readByte(int packet_index) {
		return (short) packet[packet_index];
	}
	
	/**
	* Method that reads a Garmin-double in the packet and returns it as a double.
	*/
	protected double readDouble(int packet_index) {								
		long res = 0; 		
		
		res += ( (long) packet[packet_index++] );
		res += ( (long) packet[packet_index++] ) << 8;
		res += ( (long) packet[packet_index++] ) << 16;
		res += ( (long) packet[packet_index++] ) << 24;
		res += ( (long) packet[packet_index++] ) << 32;
		res += ( (long) packet[packet_index++] ) << 40;
		res += ( (long) packet[packet_index++] ) << 48;
		res += ( (long) packet[packet_index++] ) << 56;		
				
		return Double.longBitsToDouble(res);
	} 

	/**
	* Method that reads a Garmin-float in the packet and returns it as a float.
	*/	
	protected float readFloat(int packet_index) {
		int res = 0;
		res += packet[packet_index++];
		res += packet[packet_index++] << 8;
		res += packet[packet_index++] << 16;
		res += packet[packet_index++] << 24;

		return Float.intBitsToFloat(res);		
	}

	/* ---------------------------- */
	/*         LINK PROTOCOL        */
	/* ---------------------------- */
	protected void sendBasicPacket (int id, int[] data) throws GarminException
	{
		int			t = data.length;
		
		newnack		= false;
		newack		= false;
		packet 		= new int[t + 6];
		
		packet[0] 	= DLE;
		packet[1] 	= id;
		packet[2] 	= data.length;
		
		System.arraycopy (data, 0, packet, 3, data.length);
		
		packet[t-3]	= calcChecksum ();
		packet[t-2]	= DLE;
		packet[t-1]	= ETX;

		if (isLegal () != -1)
			throw new GarminException ("[sendBasicPacket] error in byte " + isLegal ());

		try
		{
			output.write (packet);			
		}
		catch (Exception e)
		{
			throw new GarminException (e.toString ());
		}
	}

	protected void sendBasicPacket (int id) throws GarminException
	{
		newnack		= false;
		newack		= false;
		packet 		= new int[6];
		
		packet[0] 	= DLE;
		packet[1] 	= id;
		packet[2] 	= 0;
		packet[3]	= calcChecksum ();
		packet[4]	= DLE;
		packet[5]	= ETX;

		if (isLegal () != -1)
			throw new GarminException ("[sendBasicPacket] error in byte " + isLegal ());

		try
		{
			output.write (packet);			
		}
		catch (Exception e)
		{
			throw new GarminException (e.toString ());
		}
	}

	protected void sendCommandPacket (int cmd) throws GarminException
	{
		newnack		= false;
		newack		= false;
		packet 		= new int[8];
		
		packet[0] 	= DLE;
		packet[1] 	= Pid_Command_Data;
		packet[2] 	= 2;
		packet[3] 	= cmd;
		packet[4] 	= 0;
		packet[5]	= calcChecksum ();
		packet[6]	= DLE;
		packet[7]	= ETX;

		if (isLegal () != -1)
			throw new GarminException ("[sendCommandPacket] error in byte " + isLegal ());

		try
		{
			output.write (packet);			
		}
		catch (Exception e)
		{
			throw new GarminException (e.toString ());
		}
	}

	protected void sendEventPacket (int mask) throws GarminException
	{
		newnack		= false;
		newack		= false;
		packet 		= new int[8];
		
		packet[0] 	= DLE;
		packet[1] 	= Pid_Event_Data;
		packet[2] 	= 2;
		packet[3] 	= (byte) (mask / 255);
		packet[4] 	= (byte) (mask % 255);
		packet[5]	= calcChecksum ();
		packet[6]	= DLE;
		packet[7]	= ETX;

		if (isLegal () != -1)
			throw new GarminException ("[sendEventPacket] error in byte " + isLegal ());

		try
		{
			output.write (packet);			
		}
		catch (Exception e)
		{
			throw new GarminException (e.toString ());
		}
	}

	protected void sendAckPacket (int ack, int id)
	{
		packet 		= new int[7];
		
		packet[0] 	= DLE;
		packet[1] 	= ack;
		packet[2] 	= 1;
		packet[3] 	= id;
		packet[4]	= calcChecksum ();
		packet[5]	= DLE;
		packet[6]	= ETX;

		if (isLegal () != -1)
			System.out.println ("--[Garmin] Sending ACK error in byte " + isLegal ());

		try
		{
			output.write (packet);			
		}
		catch (Exception e)
		{
			System.out.println ("--[Garmin] Sending ACK: " + e.toString ());
		}
	}

	protected void startAsync () throws GarminException
	{
		sendEventPacket (Mask_Enable_Sat);		

		while (!newack && !newnack)
			Thread.yield ();

		sendCommandPacket (Cmnd_Start_Pvt_Data);		
	}
	
	protected void stopAsync () throws GarminException
	{
		sendCommandPacket (Cmnd_Stop_Pvt_Data);		

		while (!newack && !newnack)
			Thread.yield ();

		sendEventPacket (Mask_Disable);		
	}
		
	/* ---------------------------- */
	/*        INITIALISATION        */
	/* ---------------------------- */
	protected void initialise () throws GarminException
	{
		CommPortIdentifier		id;

		try
		{
			id 		= CommPortIdentifier.getPortIdentifier (port);
			serial	= (SerialPort) id.open ("Garmin", 10000);
			serial.setSerialPortParams (9600, 8, 1, SerialPort.PARITY_NONE);
			serial.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
			serial.enableReceiveTimeout (SER_TIMEOUT);
			
			input	= new GarminInputStream (serial.getInputStream ());
			output	= new GarminOutputStream (serial.getOutputStream ());

			initDevice ();
		} catch (Exception e) 
		{
			throw new GarminException (e);
		}
	}

	public void close () throws GarminException
	{
		stopAsync ();
		parser.running = false;
		
		try
		{
			input.close ();
			output.close ();
			serial.close ();
		} catch (Exception e) { throw new GarminException (e.getMessage()); }
	}

	protected void initDevice () throws GarminException 
	{
		long			ct;
		Thread		thread;
		
		status	= ST_WAIT;
		
		thread	= new Thread (parser);
		thread.start (); 
		
		sendBasicPacket (Pid_Product_Rqst);

		ct	= System.currentTimeMillis ();
		while ((status != ST_PRODUCT) && (!newnack) && (System.currentTimeMillis ()-ct < 5000))
			Thread.yield ();

		startAsync ();
	}
		
	/* ---------------------------- */
	/*         GPS INTERFACE        */
	/* ---------------------------- */
	public GPSData getData ()
	{
		data.setPos (pvt_pos);		
		data.setAltitude (pvt_alt);
		data.setVel (pvt_veast, pvt_vnorth, pvt_vup);
		data.setWeekSeconds ((int) Math.round (pvt_tow));
		data.setFix (pvt_fix);
		data.setNumSat (nsats);
		
		return data;
	}

	public SatelliteData[] getSatellites ()
	{		
		return sats;
	}		

	public WaypointData[] getWaypoints () throws GarminException
	{
		status	= ST_WAIT;
		record	= REC_WP;
	
		stopAsync ();
		sendCommandPacket (Cmnd_Transfer_Wpt);

		while ((status != ST_RECORD) && (!newnack))
			Thread.yield ();
	
		startAsync ();
		if ((status != ST_RECORD) || newnack)		return null;
		
		return wpdata;
	}		
	
	public TracklogData[] getTracklog () throws GarminException
	{
		int				i;
		TracklogData[]	tmptrk;
		
		status	= ST_WAIT;
		record	= REC_TRACK;
	
		stopAsync ();
		sendCommandPacket (Cmnd_Transfer_Trk);

		while ((status != ST_RECORD) && (!newnack))
			Thread.yield ();
	
		startAsync ();
		if ((status != ST_RECORD) || newnack)		return null;

		tmptrk	= new TracklogData[trkndx+1];
		for (i = 0; i < trkndx+1; i++)
			tmptrk[i]	= trkdata[i];
		
		return tmptrk;
	}		

	/* ---------------------------- */
	/*       GARMIN INTERFACE       */
	/* ---------------------------- */
	public LLAPos getPosition () throws GarminException
	{
		long			ct;
		
		status	= ST_WAIT;
		
		stopAsync ();
		sendCommandPacket (Cmnd_Transfer_Posn);

		ct	= System.currentTimeMillis ();
		while ((status != ST_POS) && (!newnack) && (System.currentTimeMillis ()-ct < WAIT_TIMEOUT))
			Thread.yield ();
				
		startAsync ();
		return pos;
	}

	public String getDateTime () throws GarminException
	{
		long			ct;
		
		status	= ST_WAIT;
		
		stopAsync ();
		sendCommandPacket (Cmnd_Transfer_Time);

		ct	= System.currentTimeMillis ();
		while ((status != ST_TIME) && (!newnack) && (System.currentTimeMillis ()-ct < WAIT_TIMEOUT))
			Thread.yield ();

		startAsync ();
		if (status != ST_TIME)			return null;
		
		return dtime;
	}
		
	public void shutdown () throws GarminException
	{
		stopAsync ();
		sendCommandPacket (Cmnd_Turn_Off_Pwr);
	}
}
