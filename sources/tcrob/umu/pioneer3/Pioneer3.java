/*
 * (c) 2004 Humberto Martinez
 */

package tcrob.umu.pioneer3;

import java.util.*;

import devices.drivers.laser.*;

import tc.vrobot.*;
import tc.vrobot.models.*;
import tc.shared.linda.*;

public class Pioneer3 extends VirtualRobot
{	
	class Pioneer3Laser extends Thread
	{
		protected Pioneer3					robot;
		protected RobotDesc					rdesc;
		protected RobotData					data;
		
		protected boolean					updated;
		protected boolean					running;
		
		// Constructors
		public Pioneer3Laser (Pioneer3 robot, RobotDesc rdesc)
		{
			this.robot		= robot;
			this.rdesc		= rdesc;
			running			= true;
			data				= new RobotData (rdesc);
			updated			= false;
			setName ("TC-Thread-ROB-Pioneer3Laser");
		}
		
		public void process_laser ()
		{
			int				i;
			long 			stime, ctime = 0;
			
			// Update laser range sensors
			for (i = 0; i < rdesc.MAXLRF; i++)
			{
				data.lrfs_flg[i]	= false;
				data.lrfs[i]	= null;
				
				if (robot.lasers[i] != null)
				{
					try
					{
						stime = System.currentTimeMillis();
						data.lrfs[i]	= robot.lasers[i].getLaserData ();
						ctime = System.currentTimeMillis() - stime;
					} catch (Exception e) { e.printStackTrace (); }
					
					if (data.lrfs[i] != null)
					{
						updated = true;
						data.lrfs_flg[i]	= true;
						if (robot.debug ())
							System.out.println ("  [Pioneer3Laser] Laser acquisition time="+ctime+" ms");
					}
				}		
			}
		}		
		
		public void run () 
		{
			System.out.println ("  [Pioneer3Laser] Starting laser processing thread");		
			
			while (running)
			{
				Thread.yield ();
				while (updated)
					try { Thread.sleep (25); } catch(Exception e) { }
				process_laser();
			}
			
			for (int i = 0; i < rdesc.MAXLRF; i++)
				robot.lasers[i].close();
		}
		
		public void close ()
		{
			running = false;
		}
	}
	
	// Devices and data
	protected Pioneer3Driver				driver;
	protected Pioneer3Laser				thlaser;
	protected DifferentialDrive			model;
	protected Laser[]					lasers;
	
	// Motor control
	protected double						speed;
	protected double						turn;
	protected int						ctrlmode;
	
	// Other local stuff
	protected boolean					debug		= false;
	
	// Constructors
	public Pioneer3 (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Accessors
	protected boolean				debug ()			{ return debug; }
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		int			i;
		String		port;
		
		super.initialise (props);
		
		// Initialise the real robot
		model		= (DifferentialDrive) rdesc.model;
		
		// Configure laser range sensors
		System.out.println ("  [Pioneer3] Initialising " + rdesc.MAXLRF + " laser range sensors ...");
		lasers = new Laser[rdesc.MAXLRF];
		for (i = 0; i < rdesc.MAXLRF; i++)
		{
			port = rprops.getProperty ("LRF" + i);
			try { lasers[i] = Laser.getLaser (port); } catch (Exception e) { System.out.println ("--[Pioneer3] Error opening LASER_RANGE" + i + "=" + e.toString ()); }
		}	
		
		reset ();
	}
	
	public void reset ()
	{
		System.out.println ("  [Pioneer3] Resetting robot");		
		
		// Stop previous action
		running		= false;
		
		// Open and reset serial driver
		if (driver == null)
			driver	= new Pioneer3Driver (rprops);
		
		if (thlaser == null){
			thlaser	= new Pioneer3Laser (this, rdesc);
			thlaser.start ();
		}
		
		// Initialise hardware parameters
		driver.setMaxVelocity (model.Vmax);
		driver.setEnableMotors (true);
		driver.setMotors (0.0, 0.0);
		
		// Set default odometry-based position
		data.odom_x	= 0.0;		
		data.odom_y	= 0.0;		
		data.odom_a	= 0.0;		
	}
	
	// Transform raw sonar data to metres
	public void process_sensors (long dtime) 
	{
		int				i;
		long				ct;
		double			dt;
		double			odomL, odomR;		
		
		if (driver == null)			return;
		
		if (debug)		System.out.println ("  [Pioneer3] Entering SENSOR processing module");
		
		
		/* ------------ */
		/* READ SENSORS */
		/* ------------ */
		
		if (debug)		System.out.println ("  [Pioneer3] Executing phase READ");
		
		// Read sonar sensors (discard erroneous readings)
		for (i = 0; i < rdesc.MAXSONAR; i++)
			if (driver.sons[i] <= Pioneer3Driver.SON_NULL)
				data.sonars_flg[i]	= false;
			else
			{
				data.sonars[i] 		= driver.sons[i];
				if (data.sonars[i] < 0.2)
					data.sonars_flg[i]	= false;
				else
					data.sonars_flg[i]	= true;
				driver.sons[i]		= Pioneer3Driver.SON_NULL;
			}	
		
		// Update laser data (if readings are available)
//		thlaser.process_laser();
		if (thlaser.updated)
		{
			// Update laser range sensors
			for (i = 0; i < rdesc.MAXLRF; i++)
			{
				data.lrfs[i]		= thlaser.data.lrfs[i];
				data.lrfs_flg[i]	= thlaser.data.lrfs_flg[i];
			}
			// Clear update flag
			thlaser.updated	= false;
		}
		else
		{		           
			// Disable laser range sensors readings
			for (i = 0; i < rdesc.MAXLRF; i++)
				data.lrfs_flg[i]	= false;
		}
		
		/* ------------- */
		/* READ ODOMETRY */
		/* ------------- */
		
		if (debug)		System.out.println ("  [Pioneer3] Executing phase ODOMETRY");
		
		// Wait until previous odometry data has been received	
		ct	= System.currentTimeMillis ();
		while (!driver.odom_flg && (System.currentTimeMillis () - ct < 50))
			Thread.yield ();
		
		if (System.currentTimeMillis () - ct > 50)
			System.out.println ("--[Pioneer3] Odometry timeout");
		
		// Compute local translation from encoders
		dt		= 0.100;
		odomL	= driver.odom_motL * dt;									// [m] 
		odomR	= driver.odom_motR * dt;									// [m]
		
		if (debug)		System.out.println ("  [Pioneer3] => [ENC] motL="+driver.odom_motL+", motR="+driver.odom_motR+" [ODOM] dVl="+odomL+" m, dVr="+odomR+" m");
		
		// Apply kynematics model
		model.odometry (data, odomL, odomR);	
		
		// Reset odometry data
		driver.odom_motL	= 0;
		driver.odom_motR	= 0;
		
		/* ---------- */
		/* SET MOTORS */
		/* ---------- */
		
		if (debug)		System.out.println ("  [Pioneer3] Executing phase MOTORS");
		
		// Clean odometry flag
		driver.odom_flg		= false;
		
		// Movement commands
		if (ctrlmode != ItemMotion.CTRL_NONE)
		{
			switch (ctrlmode)
			{
			case ItemMotion.CTRL_MANUAL:
				double		tspeed, tturn;
				
				// Scale joystick command to maximum velocities
				tspeed	= speed * model.Vmax;								// [m/s]
				tturn	= turn * model.Rmax * 0.2;						// [rad/s]
				
				// Compute desired target wheels speed (m/s, m/s)
				model.kynematics_inverse (tspeed, tturn);	
				driver.setMotors (model.dVl, model.dVr);
				break;
				
			case ItemMotion.CTRL_AUTO:
				// Compute desired target wheels speed (m/s, m/s)
				model.kynematics_inverse (speed, turn);
				driver.setMotors (model.dVl, model.dVr);
				break;
				
			default:
				System.out.println ("--[Pioneer3] Unrecognised control-mode command");
			driver.setMotors (0.0, 0.0);
			}
		}
		
		if (debug)		System.out.println ("  [Pioneer3] Leaving SENSOR processing module\n");
	}	
	
	public void notify_motion (String space, ItemMotion item)
	{
		super.notify_motion (space, item);
		
		speed		= item.speed;
		turn		= item.turn;
		ctrlmode	= item.ctrlmode;		
	}
}
