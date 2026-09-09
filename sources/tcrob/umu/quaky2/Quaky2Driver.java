/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;
 
import java.util.*;

import devices.drivers.can.*;

public class Quaky2Driver extends CanDriver
{
	static public int			S_BOARDS		= 4;				// Number of sensor boards
	static public int			S_SENS			= 4;				// Number of sensor pairs per board
	static public int			SON_NULL		= 0xFFFF;			// Wrong sonar sensor value	
	static public int			IR_NULL			= 0xFF;				// Wrong sonar sensor value	
	
	// Variables for storing input data
  	public int 					odom_motL		= 0;
	public int 					odom_motR		= 0;
	public int[]				irs;
	public int[]				sons;
	public boolean[]			bumpers;
  
  	// Variables for storing state flags
  	public boolean				odom_flg		= false;
  	
	// CAN identifiers
	protected int				m_set_canid;  						// Steering/Traction motors setpoint
	protected int				m_cfg_canid;  						// Steering/Traction motors PID constants
	protected int				m_odom_canid;						// Motors counters
	protected int				s_fire_canid;  						// Fire sensors with given mask
	protected int				s_son_canid;  						// Sonar sensor values
	protected int				s_ir_canid;							// Infrared sensor values
	protected int				s_bum_canid;						// Bumper sensors values

	// Constructors
	public Quaky2Driver (Properties prop)
	{
		super (prop);
	}

	// Instance abstract methods
	protected void initialise ()
	{
		sons	= new int[S_BOARDS * S_SENS];
		irs		= new int[S_BOARDS * S_SENS];
		bumpers	= new boolean[S_SENS];
		
		cleanSensors ();
	}
	
	protected void parseIdentifiers (Properties p)
	{
		try
		{
			m_set_canid		= Integer.parseInt (p.getProperty ("CAN_M_SETID"));
			m_cfg_canid		= Integer.parseInt (p.getProperty ("CAN_M_CFGID"));
			m_odom_canid	= Integer.parseInt (p.getProperty ("CAN_M_ODOMID"));
			s_fire_canid	= Integer.parseInt (p.getProperty ("CAN_S_FIREID"));
			s_son_canid		= Integer.parseInt (p.getProperty ("CAN_S_SONID"));
			s_ir_canid		= Integer.parseInt (p.getProperty ("CAN_S_IRID"));
			s_bum_canid		= Integer.parseInt (p.getProperty ("CAN_S_BUMID"));
		} catch (Exception e) { e.printStackTrace (); }
		
		System.out.println ("  [Quaky2Drv] CAN identifiers:");
		System.out.print ("\tM_SETP: "+m_set_canid);
		System.out.print ("\tM_CFG: "+m_cfg_canid);
		System.out.println ("\tM_ODOM: "+m_odom_canid);
		System.out.print ("\tS_FIRE: "+s_fire_canid);
		System.out.print ("\tS_SON: "+s_son_canid);
		System.out.println ("\tS_IR: "+s_ir_canid);
		System.out.print ("\tS_BUMPER: "+s_bum_canid);
		System.out.println ();
	}

	protected void parseFrames (CanFrame[] frames)
	{
		int				i, j, k;
		int				id;
		int				ibum;
		byte[]			rcvd;
		boolean			valid;

		for (i = 0; i < frames.length; i++)
		{
			id		= frames[i].getID ();
			rcvd	= frames[i].getData ();		
			valid	= false;			
		
			// Odometry frame
			if (id == m_odom_canid)
			{
				if (rcvd.length < 8)
				{
					System.out.println ("--[Quaky2Drv] Odometry frame with len=" + rcvd.length);
					continue;
				}
				
				valid		= true;			
				odom_flg	= true;
				odom_motL 	= bytesToInt (rcvd[0], rcvd[1], rcvd[2], rcvd[3]);	  					
				odom_motR 	= -bytesToInt (rcvd[4], rcvd[5], rcvd[6], rcvd[7]);

				if (messageDebug)		System.out.println ("  [Quaky2Drv] Odometry motL= "+odom_motL+", motR="+odom_motR);
			}		
			// bumpers frame
			else if (id == s_bum_canid)
			{
				if (rcvd.length < 3)
				{
					System.out.println ("--[Quaky2Drv] Bumpers frame with len=" + rcvd.length);
					continue;
				}
				
				valid		= true;			
				ibum 		= bytesToUInt (rcvd[2]);	  					
				
				bumpers[0]	= ((ibum & 0x80) > 0);
				bumpers[1]	= ((ibum & 0x40) > 0);
				bumpers[2]	= ((ibum & 0x20) > 0);
				bumpers[3]	= ((ibum & 0x10) > 0);

				if (messageDebug)		System.out.println ("  [Quaky2Drv] Bumpers= "+ibum);
			}		
			// Sonar sensors frame
			for (j = 0; j < S_BOARDS; j++)
				if (id == (s_son_canid + j))
				{
					if (rcvd.length < 8)
					{
						System.out.println ("--[Quaky2Drv] Sonar frame with len=" + rcvd.length);
						continue;
					}
				
					valid		= true;			
					for (k = 0; k < S_SENS; k++)
						sons[j * S_BOARDS + k]	= bytesToUInt (rcvd[k*2], rcvd[k*2+1]);
  					
  					if (messageDebug)
  					{
  						System.out.print ("  [Quaky2Drv] Sonars("+j+") = ");
						for (k = 0; k < S_SENS; k++)
							System.out.print (sons[j * S_BOARDS + k]+", ");
  						System.out.println ();	  
  					}						
				}		
			// Ir sensors frame
			for (j = 0; j < S_BOARDS; j++)
				if (id == (s_ir_canid + j))
				{
					if (rcvd.length < 4)
					{
						System.out.println ("--[Quaky2Drv] Infrared frame with len=" + rcvd.length);
						continue;
					}
				
					valid		= true;			
					for (k = 0; k < S_SENS; k++)
						irs[j * S_BOARDS + k]	= bytesToUInt (rcvd[k]);
  					
  					if (messageDebug)
  					{
  						System.out.print ("  [Quaky2Drv] Irs("+j+") = ");
						for (k = 0; k < S_SENS; k++)
							System.out.print (irs[j * S_BOARDS + k]+", ");
  						System.out.println ();	  
  					}						
				}		
			// Unrecognised frame							
			if (!valid && messageDebug)
				 System.out.println ("--[Quaky2Drv] Unrecognised CAN frame <ID="+id+">");
		}
	}
	
	// Instance user methods
	protected void cleanSensors ()
	{
		int			i;
		
		for (i = 0; i < S_BOARDS * S_SENS; i++)
		{
			irs[i]	= IR_NULL;
			sons[i]	= SON_NULL;
		}
	}

	public void setMotors (int motL, int motR)
	{
	    byte[]			mensaje = new byte[4];

		mensaje[0] = intToByteHi (motL);
		mensaje[1] = intToByteLo (motL);
		mensaje[2] = intToByteHi (motR);
		mensaje[3] = intToByteLo (motR);
		
		frame.setID (m_set_canid);
		frame.setData (mensaje);
		frame.setFI (false,false);		
		
		if (canDebug) frame.dumpFrame ();

		try
		{
			can0.sendFrame (frame);
		} catch (Exception e) { System.err.println ("--[Quaky2Drv] setMotors: exception "+e); }
	}

	public void setMotorsCfg (int k0, int k1, int k2, int kdiv)
	{
	    byte[]			mensaje = new byte[4];

		mensaje[0] = intToByte (k0);
		mensaje[1] = intToByte (k1);
		mensaje[2] = intToByte (k2);
		mensaje[3] = intToByte (kdiv);
		
		frame.setID (m_cfg_canid);
		frame.setData (mensaje);
		frame.setFI (false,false);		
		
		if (canDebug) frame.dumpFrame ();

		try
		{
			can0.sendFrame (frame);
		} catch (Exception e) { System.err.println ("--[Quaky2Drv] setMotorsCfg: exception "+e); }
	}

	public void fireSensors (int mask)
	{
		int				i;
	    byte[]			mensaje = new byte[1];

		// Clear previous values
		cleanSensors ();	
			
		mensaje[0] = intToByte (mask);
		
		frame.setID (s_fire_canid);
		frame.setData (mensaje);
		frame.setFI (false, false);		
		
		if (canDebug) frame.dumpFrame ();

		try
		{
			for (i = 0; i < S_BOARDS; i++)
			{
				frame.setID (s_fire_canid + i);	
				can0.sendFrame (frame);
			}
		} catch (Exception e) { System.err.println ("--[Quaky2Drv] fireSensors: exception "+e); }
	}
}