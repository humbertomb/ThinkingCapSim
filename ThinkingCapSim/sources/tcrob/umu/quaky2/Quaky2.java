/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.util.*;

import tc.vrobot.*;
import tc.vrobot.models.*;
import tc.shared.linda.*;

import devices.pos.*;

import devices.data.*;
import devices.drivers.vision.*;
import devices.drivers.lcd.*;

public class Quaky2 extends VirtualRobot
{
	static public final String				PROP_DEBUG 		= "tc.quaky.debug";

	static protected final byte 			icon[][] 		= {	{ (byte)0x7C,(byte)0xF2,(byte)0xFB,(byte)0xFF,(byte)0xFF,(byte)0xFC,(byte)0xFF,(byte)0xFF,(byte)0xFE,(byte)0x7C },
																{ (byte)0x7C,(byte)0xF2,(byte)0xFB,(byte)0xFF,(byte)0xFC,(byte)0xF8,(byte)0xFC,(byte)0xFF,(byte)0xFE,(byte)0x7C },
																{ (byte)0x7C,(byte)0xF3,(byte)0xFA,(byte)0xFC,(byte)0xF0,(byte)0xE0,(byte)0xF0,(byte)0xFC,(byte)0xFF,(byte)0x7C }, 
																{ (byte)0x7C,(byte)0xF2,(byte)0xFB,(byte)0xFF,(byte)0xFC,(byte)0xF8,(byte)0xFC,(byte)0xFF,(byte)0xFE,(byte)0x7C } };
	static protected final byte 			eraser[] 		= { (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00 };

	static public final int					MAX_OBJS		= 10;					// Maximum number of objects

	// Sensor masks
	public static final int					MASK_SON_ZERO	= 0xA0;					// Enable sonar phase zero	(1010 0000)b
	public static final int					MASK_SON_ONE	= 0x50;					// Enable sonar phase one	(0101 0000)b
	public static final int					MASK_IR_ZERO	= 0x0A;					// Enable ir phase zero 	(0000 1010)b
	public static final int					MASK_IR_ONE		= 0x05;					// Enable ir phase one  	(0000 0101)b
	
	// Motor constants (PID controller)
	static public final int					Kq0 			= 17;					// Proportional gain
	static public final int					Kq1 			= 22;					// Integral gain
	static public final int					Kq2 			= 6;					// Derivative gain
	static public final int					Kdiv 			= 16;					// Divider
	static public final double				KMOT 			= 4083.59;				// Motor constant (for 1 m/s speed)
	static public final int					KOUTMAX 		= 1717;					// Maximum motor values
	
	// Sensor calibration
	public double[][] 						ir_calib 		= 
	{
		//dist(cm)	A0(0)	A0(1)	A0(2)	A0(3)	A1(0)	A1(1)	A1(2)	A1(3)	A2(0)	A2(1)	A2(2)	A2(3)	A3(0)	A3(1)	A3(2)	A3(3)
			{8		,242	,239	,242	,243	,233	,244	,234	,232	,232	,236	,244	,234	,236	,234	,232	,236},
			{12		,194	,194	,200	,211	,174	,216	,177	,171	,171	,187	,196	,180	,187	,180	,173	,184},
			{16		,165	,166	,172	,184	,144	,189	,147	,141	,141	,159	,168	,151	,158	,150	,143	,155},
			{20		,147	,149	,153	,165	,125	,169	,128	,123	,122	,141	,150	,133	,139	,132	,124	,135},
			{24		,134	,137	,141	,152	,112	,157	,115	,110	,109	,128	,136	,120	,126	,119	,111	,123},
			{28		,125	,128	,131	,144	,103	,149	,106	,101	,100	,118	,127	,111	,117	,110	,103	,114},
			{32		,119	,121	,125	,137	,96		,142	,99		,94		,93		,111	,121	,104	,110	,102	,96		,107},
			{36		,113	,116	,119	,131	,91		,136	,94		,89		,88		,105	,115	,99		,105	,98		,91		,102},
			{40		,109	,112	,115	,127	,86		,131	,89		,84		,83		,101	,111	,94		,100	,92		,86		,98},
			{44		,105	,109	,111	,121	,80		,126	,83		,78		,78		,97		,107	,90		,96		,87		,81		,94},
			{48		,102	,106	,108	,119	,78		,124	,81		,76		,75		,95		,104	,87		,94		,84		,77		,91},
			{52		,100	,103	,106	,117	,77		,122	,80		,75		,74		,92		,101	,85		,91		,82		,76		,89},
			{56		,98		,101	,104	,116	,75		,121	,78		,73		,72		,90		,99		,83		,89		,79		,74		,87},
			{60		,96		,99		,102	,115	,74		,119	,76		,72		,71		,88		,97		,82		,87		,78		,73		,85},
			{64		,94		,97		,101	,114	,72		,118	,75		,70		,69		,86		,95		,79		,85		,76		,71		,83},
			{68		,92		,96		,99		,112	,71		,117	,74		,69		,68		,84		,93		,78		,83		,75		,69		,82},
			{72		,91		,94		,98		,110	,70		,115	,73		,68		,67		,83		,92		,77		,82		,73		,68		,81},
			{76		,90		,93		,96		,109	,69		,114	,72		,67		,66		,81		,91		,76		,81		,72		,67		,80},
			{80		,88		,93		,95		,108	,68		,113	,71		,65		,65		,80		,89		,75		,80		,71		,66		,79},
			{84		,86		,92		,94		,108	,67		,113	,70		,64		,64		,79		,87		,74		,79		,70		,65		,78},
			{88		,85		,91		,93		,107	,66		,112	,69		,63		,63		,78		,86		,73		,78		,69		,64		,77},
			{90		,84		,90		,92		,106	,65		,111	,68		,62		,62		,77		,85		,72		,77		,68		,63		,76}
	};

	// Devices and data
	protected Quaky2Driver					driver;
	protected Vision[]						vision;
	protected VisionData[]					obdata;
	protected Position						vpos;									// Position reference for vision
	protected Position						cpos;									// Current odometry based location
	protected GLC24064						lcd;
	protected String							lcd_port;
	
	// Motor control
	protected DifferentialDrive				model;
	protected double							speed;
	protected double							turn;
	protected int							ctrlmode;
    	
	// Other local stuff
	private int								xoffset			= 0;					// LCD x position for icon
	private int								icon_n			= 0;					// Current icon number
	protected boolean						debug			= false;
	
	// Constructors
	public Quaky2 (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Accessors
	protected boolean				debug ()			{ return debug; }

	// Instance methods
	protected void initialise (Properties props)
	{		
		int				i;
		String			sprop;
		String			params;
		
		super.initialise (props);
		
		// Load system parameters
		sprop = System.getProperty (PROP_DEBUG);
		if ((sprop != null) && (sprop.equals ("true")))
			debug = true;
		
		// Initialise other structures
		model		= (DifferentialDrive) rdesc.model;
		vpos			= new Position ();
		cpos			= new Position ();
		obdata			= new VisionData[MAX_OBJS];
		for (i = 0; i < MAX_OBJS; i++)
			obdata[i]		= new VisionData ();

		// Configure vision based sensors
		System.out.println ("  [Quaky2] Initialising " + rdesc.MAXVISION + " vision based sensors ...");
		vision = new Vision[rdesc.MAXVISION];
		for (i = 0; i < rdesc.MAXVISION; i++)
		{
			params = rprops.getProperty ("VISION" + i);
			try
			{ 
				vision[i] = Vision.getVision (params); 
				vision[i].setDebug (debug);
				vision[i].start ();
			} catch (Exception e) { System.out.println ("--[Quaky2] Error opening VISION" + i + "=" + e.toString ()); }
		}
		
		// Configure default LCD serial port
		lcd_port	= rprops.getProperty ("LCD");
		System.out.println ("  [Quaky2] Initialising LCD in port " + lcd_port);

		// Initialise the real robot
		reset ();
	}
	
	public void reset ()
	{
		if (debug) 				System.out.println ("  [Quaky2] Resetting robot");		
		
		// Stop previous action
		running		= false;
		
    	// Open and clear LCD
    	try
    	{
	    	if (lcd == null)
	    		lcd		= new GLC24064 (lcd_port);
			lcd.clrlcd ();
			lcd.saveContrast ((byte) 170);
			
			lcd.setFont (GLC24064.FUTURA);
			lcd.writeTextAt ("NaveTech Inc. Quaky IIb Soccer", 1, 1);	
						
			lcd.setFont (GLC24064.SMALL);
			lcd.writeTextAt ("Sonar    [                ]", 1, 4);			
			lcd.writeTextAt ("Infrared [                ]", 1, 5);			
		} catch (Exception e) { e.printStackTrace (); }
		
    	// Open and reset CAN bus driver
    	if (driver == null)
	    	driver	= new Quaky2Driver (rprops);
	    driver.setMotorsCfg (Kq0, Kq1, Kq2, Kdiv);
	    
		// Stop motors
		set_motors (0.0, 0.0);

		// Set default odometry-based position
		data.odom_x	= 0.0;		
		data.odom_y	= 0.0;		
		data.odom_a	= 0.0;		
		vpos.set (data.odom_x, data.odom_y, data.odom_a);
	}

	// Transform raw Sharp GP2D02 IR sensor data to metres
	protected double gp2d02 (int sensor, double value)
	{
		int				i;
		double			cm;
		
		if (value >= ir_calib[0][sensor + 1])	
			cm	= 8.0;
		else
		{
			cm	= 90.0;
			for (i = 1; i < 22; i++)
			{	
				if (value >= ir_calib[i][sensor+1])
				{
					cm	= ir_calib[i-1][0] + 4.0 * (value - ir_calib[i-1][sensor+1]) / (ir_calib[i][sensor+1] - ir_calib[i-1][sensor+1]);
					break;
				}
			}
		}

		return cm / 100.0;
	}

	// Transform raw sonar data to metres
	protected double polaroid (int sensor, double value)
	{
		return (0.01376 * value - 4.9375) / 100.0;
	}

	protected void set_motors (double speed, double turn)
	{
    	int			ileft, iright;
		
		// Compute desired target wheels speed (m/s, m/s)
		model.kynematics_inverse (speed, turn);
		
		// Convert commands to driver specific format
		ileft	= (int) Math.round (model.dVl * KMOT);							// [pulses/s] 
		iright	= (int) Math.round (model.dVr * KMOT);							// [pulses/s] 

		// Check that values are inside the limits
		ileft	= Math.min (Math.max (ileft, -KOUTMAX), KOUTMAX);
		iright	= Math.min (Math.max (iright, -KOUTMAX), KOUTMAX);
		
		if (debug)	System.out.println ("  [Quaky2] AUTO motL="+ileft+" ("+model.dVl+" m/s), motR="+iright+" ("+model.dVr+" m/s)");

		if (driver != null)				driver.setMotors (ileft, iright);
	}

	protected void set_motors_raw (double speed, double turn)
	{
		double		tspeed, tturn;
    	int			ileft, iright;
		
		// Scale joystick command to maximum velocities
		tspeed	= speed * model.Vmax;											// [m/s]
		tturn	= turn * model.Rmax;											// [rad/s]

		// Compute desired target wheels speed (m/s, m/s)
		model.kynematics_inverse (tspeed, tturn);
		
		// Adapt commands to driver specific format 
		ileft	= (int) Math.round (model.dVl * KMOT);							// [pulses/s] 
		iright	= (int) Math.round (model.dVr * KMOT);							// [pulses/s] 
				
		// Check that values are inside the limits
		ileft	= Math.min (Math.max (ileft, -KOUTMAX), KOUTMAX);
		iright	= Math.min (Math.max (iright, -KOUTMAX), KOUTMAX);
		
		if (debug)	System.out.println ("  [Quaky2] MANUAL <speed="+tspeed+" m/s, turn="+tturn+" rad/s> motL="+ileft+", motR="+iright);

		if (driver != null)				driver.setMotors (ileft, iright);
	}

	public void process_sensors (long dtime) 
	{
		int				i, j, k;
		int				mask;
		long			ct;
    	double			odomL, odomR;		
    	String			stext, itext;
    	VisionData[]	vdata;
    	    	 
		if (driver == null)			return;
								
		if (debug)		System.out.println ("  [Quaky2] Entering SENSOR processing module");
		
		/* ---------- */
		/* SET MOTORS */
		/* ---------- */
		
		if (debug)		System.out.println ("  [Quaky2] Executing phase MOTORS");

		// Clean odometry flag
		driver.odom_flg		= false;

		// Movement commands
    	if (ctrlmode != ItemMotion.CTRL_NONE)
    	{
 			switch (ctrlmode)
			{
			case ItemMotion.CTRL_MANUAL:
				set_motors_raw (speed, turn);
				break;
			case ItemMotion.CTRL_AUTO:
				set_motors (speed, turn);
				break;
			default:
				System.out.println ("--[Quaky2] Unrecognised control-mode command");
			}
		}

		/* ------------ */
		/* READ SENSORS */
		/* ------------ */
		
		if (debug)		System.out.println ("  [Quaky2] Executing phase READ");

		// Read sonar sensors (discard erroneous readings)
		stext = "[";
		for (i = 0; i < rdesc.MAXSONAR; i++)
			if (driver.sons[i] == Quaky2Driver.SON_NULL)
			{
				data.sonars_flg[i]	= false;
				stext 				+= "_";
			}
			else
			{
				data.sonars[i] 		= polaroid (i, (double) driver.sons[i]);
				if (data.sonars[i] < 0.2)
					data.sonars_flg[i]	= false;
				else
					data.sonars_flg[i]	= true;
				driver.sons[i]		= Quaky2Driver.SON_NULL;
				if (data.sonars[i] < 0.2)
					stext 				+= "M";
				else
					stext 				+= "X";
			}	
		stext += "]";

		// Read ir sensors (discard erroneous readings)
		itext = "[";
		for (i = 0; i < rdesc.MAXIR; i++)
			if ((driver.irs[i] == Quaky2Driver.IR_NULL) || (i == 4))
			{
				data.irs_flg[i]		= false;
				itext 				+= "_";
			}
			else
			{
				data.irs[i] 		= gp2d02 (i, (double) driver.irs[i]);
				data.irs_flg[i]		= true;
				driver.irs[i]		= Quaky2Driver.IR_NULL;
				if (data.irs[i] < 0.2)
					itext 				+= "M";
				else
					itext 				+= "X";
			}	
		itext += "]";
	
		// Read bumper sensors
//		btext = "[";
//		for (i = 0; i < rdesc.MAXBUMPER; i++)
//		{
//			data.bumpers[i] 	= driver.bumpers[i];
//			driver.bumpers[i]	= false;
//			if (data.bumpers[i])
//				btext 				+= "X";
//			else
//				btext 				+= "_";
//		}	
//		btext += "]";
	
		/* ------------ */
		/* FIRE SENSORS */
		/* ------------ */
		
		if (debug)		System.out.println ("  [Quaky2] Executing phase FIRE");

    	mask	= 0;
    	
    	if (data_ctrl.ir)									// Fire infrareds
     	{
 			if (rdesc.irfeat[0].step () == cycir)
 				mask += MASK_IR_ZERO;
  			if (rdesc.irfeat[1].step () == cycir)
 				mask += MASK_IR_ONE;
 		}
    		
    	if (data_ctrl.sonar)								// Fire sonars
    	{
 			if (rdesc.sonfeat[0].step () == cycson)
 				mask += MASK_SON_ZERO;
  			if (rdesc.sonfeat[1].step () == cycson)
 				mask += MASK_SON_ONE;
 		}
 		
 		if (mask != 0)
 		{
			if (debug)		System.out.println ("  [Quaky2] Fire sensors with mask=" + mask);

			driver.fireSensors (mask);
		}
 
		/* -------- */
		/* DO DEBUG */
		/* -------- */
		
		// Draw LCD stuff
		try 
		{
			// Draw moving icon (I-Am-Alive live test)
			lcd.drawBitmap (1+xoffset, 50, 8+xoffset, 59, eraser);
			xoffset = (xoffset+8) % 240;						
			lcd.drawBitmap (1+xoffset, 50, 8+xoffset, 59, icon[icon_n]);
			icon_n = (icon_n + 1) % 4;
			
			// Draw reception flags
			lcd.writeTextAt (stext, 10, 4);										// Sonar flags
			lcd.writeTextAt (itext, 10, 5);										// Infrared flags
		}  catch (Exception e) { }

		/* ------------- */
		/* READ ODOMETRY */
		/* ------------- */
		
		if (debug)		System.out.println ("  [Quaky2] Executing phase ODOMETRY");

		// Wait until previous odometry data has been received	
		ct	= System.currentTimeMillis ();
		while (!driver.odom_flg && (System.currentTimeMillis () - ct < 50))
			Thread.yield ();
			
		if (System.currentTimeMillis () - ct > 50)
			System.out.println ("--[Quaky2] Odometry timeout");
				
		// Compute local translation from encoders
		odomL	= (double) driver.odom_motL * model.Cm;							// [m] 
		odomR	= (double) driver.odom_motR * model.Cm;							// [m]

		if (debug)		System.out.println ("  [Quaky2] => [ENC] motL="+driver.odom_motL+", motR="+driver.odom_motR+" [ODOM] dVl="+odomL+" m, dVr="+odomR+" m");

		// Apply kynematics model
		model.odometry (data, odomL, odomR);	
		
		// Reset odometry data
		driver.odom_motL	= 0;
		driver.odom_motR	= 0;
		
		/* -------------- */
		/* PROCESS VISION */
		/* -------------- */
		
		if (debug)		System.out.println ("  [Quaky2] Executing phase VISION");

		// Read and process vision data
		odata	= null;
		k		= 0;
		
		for (i = 0; i < MAX_OBJS; i++)
			obdata[i].valid = false;
			
		for (i = 0; i < rdesc.MAXVISION; i++)
			if (vision[i].isUpdated ())
			{				
				// Store current position
				cpos.set (data.odom_x, data.odom_y, data.odom_a);
				
				// Get vision objects
				vdata	= vision[i].getData ();
				
				// Update objects buffer
				for (j = 0; j < vdata.length; j++)
					if (vdata[j].valid && (k < MAX_OBJS))
					{
						obdata[k].set (vdata[j]);
						obdata[k].sensor_pos (rdesc.visfeat[i].x (), rdesc.visfeat[i].y (), rdesc.visfeat[i].alpha ());
						obdata[k].capture_pos (vpos, cpos);
						obdata[k].set_dev (i);
						k ++;
					}

				odata	= obdata;
				
				// Clear update flag
				vision[i].setUpdated (false);
			}
			
		// Fire vision processing
    	if (data_ctrl.vision)						
    		for (i = 0; i < rdesc.MAXVISION; i++)
 				if (rdesc.visfeat[i].step () == cycvis)
 				{
					vpos.set (data.odom_x, data.odom_y, data.odom_a);
					vision[i].acquire_frame ();
					vision[i].setUpdated (false);
				}
 	
		if (debug)		System.out.println ("  [Quaky2] Leaving SENSOR processing module\n");
	}	

	protected String format (int val, int len)
	{
		String			str;
		int				i, n;
		
		str	= Integer.toString (val);
		n	= len - str.length ();
		for (i = 0; i < n; i++)
			str += " ";
		
		return str;
	}
	
	public void notify_motion (String space, ItemMotion item)
	{
    	super.notify_motion (space, item);
    	
		speed		= item.speed;
		turn		= item.turn;
		ctrlmode	= item.ctrlmode;		
	}
}
