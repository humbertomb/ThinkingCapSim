package tclib.navigation.localisation.outdoor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import devices.data.GPSData;
import devices.data.InsData;

import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.vrobot.*;

import devices.pos.*;

/**
 * 
 * @author Miguel Julia Cristobal
 * 
 */

public class Simulation //extends Perception
{
		// Temporal referentes
		private long				tk;
		private long				tk1;
		private double				prevx;
		private double				prevy;
		private double				prevz;
		private long				GPSref;
		
		// Localisation related structures
		protected Position			pos;				// Current position (GPS corrected)
		protected Pose				pose;				// Current pose (INS corrected)
		protected FilterFactory	kloc;			// Kalman based position correction filter
		protected boolean			kfilter	= true;	// Activate Kalman filter?
		
		// De perception
		protected RobotData			data			= null;
		protected RobotDesc			rdesc;			// Robot description
		protected LPS				lps;			// Local Perceptual Space
		protected BufferedReader	reader;
		protected BufferedWriter 		output;	
		protected File				file;
		
		// Constructors
		public Simulation () // (Properties props, Linda linda)
		{
			//super (props, linda);
		}
		
		// Instance methods
		protected void initialise (Properties props)
		{
			String		fclass;
			
			fclass		= props.getProperty ("FILTER");
			if (fclass == null)	
				fclass = "tclib.navigation.localisation.outdoor.TripleKalmanFilter";
			
			// Initialise positioning filter
			kloc		= new FilterFactory (fclass + " log");
			pos			= new Position ();
			pose		= new Pose ();
			
			// Set initial pose
			pose.set_spd (0.0, 0.0, 0.0);
			pose.set_ang (0.0, 0.0, 0.0);
			pose.set_rate (0.0, 0.0, 0.0);

			try
			{
				reader	= new BufferedReader (new FileReader ("logs/insgps-6.log"));		
			} catch (Exception e) { e.printStackTrace(); }
			try	{
				output	= new BufferedWriter(new FileWriter ( "logs/kalman.log"));
			} catch (Exception e) { e.printStackTrace (); }

		}
		
		/**
		* This method calculates the position in (m, m, rad) coordinates using GPS 
		* and compass data and in INS (rad, rad, rad) coordinates using GPS data
		*/
		protected void position_correction ()
		{
			
			double			x, y, z, a;
			double			dt;
			
			x	= 0.0;
			y	= 0.0;
			z	= 0.0;
			a	= 0.0;

			// Data from log file:

			int				i;
			StringTokenizer	st;
			String			sensor;
			double[]		inslog = new double[InsData.DATALOG];
			double[]		gpslog = new double[GPSData.DATALOG];

			if (reader == null)			return;
			//data.compass[0] = new CompassData();
			int index=0;
			try
			{
				while (reader.ready ())
				{
					st	= new StringTokenizer (reader.readLine (), " ");
					// Wait for time
					tk = Integer.parseInt (st.nextToken ());
					if (tk == tk1) tk+=1; // to avoid dt = NaN
					dt	= (double) (tk - tk1) / 1000.0;
					
					// Parse data
					sensor = st.nextToken ();
					if (sensor.equals ("INS"))
					{
						for (i = 0; i < InsData.DATALOG; i++)
							inslog[i] = Double.parseDouble (st.nextToken ());
						
						if (data.ins[0] == null)			data.ins[0] = new InsData ();
						data.ins[0].fromDatalog (inslog);
						data.gps[0]=null;
					}
					else if (sensor.equals ("GPS"))
					{
						index++; 
							for (i = 0; i < GPSData.DATALOG; i++)
							gpslog[i] = Double.parseDouble (st.nextToken ());
						
						if (data.gps[0] == null)			data.gps[0] = new GPSData ();
						data.gps[0].fromDatalog (gpslog);

						//if (index > 100) break;
						//if (index > 2)
						//	data.gps[0]=null;
						data.ins[0]=null;
						//System.out.print((tk)/1000-120);
					}
					else
						System.out.println ("--ParseFile Unrecognised sensor type <"+sensor+">");
						
					// Use GPS position
					if ((data.gps.length > 0) && (data.gps[0] != null))
					{
						x	= data.gps[0].getPos ().getEast ();
						y	= data.gps[0].getPos ().getNorth ();
						z	= data.gps[0].getAltitude ();
						GPSref++;
						lps.qlty		= data.gps[0].getNumSat ();
					}
		
					// Use compass heading
					if ((data.compass.length > 0) && (data.compass[0] != null))
					{
						a	= data.compass[0].getHeading ();		
						pose.set_ang (data.compass[0].getRoll (), data.compass[0].getPitch (), a);
					}
					
					// Use INS data
					if ((data.ins.length > 0) && (data.ins[0] != null))
					{
						pose.set_ang (data.ins[0].getRoll (), data.ins[0].getPitch (), data.ins[0].getYaw ());
						pose.set_rate (data.ins[0].getRollRate (), data.ins[0].getPitchRate (), data.ins[0].getYawRate ());
					}
					
					// Compute ground speed
					pose.set_spd ((prevx - x) / dt, (prevy - y) / dt, (prevz - z) / dt);
					pos.set (x, y, z, a);
					
					// Perform EKF correction
					if (kfilter)
					{
						kloc.filter (data.gps[0], data.compass[0], data.ins[0], tk);
						pos.set (kloc.getPosition ());
					}
					
					if (GPSref > 0)
					output.write(tk + "," + pos.x() + "," + pos.y() + "," + pos.y() + "," + pose.roll()+","+pose.pitch()+","+pose.yaw()+"\r\n");
					
					// Update low-level perception & LPS datan
					lps.update (data, null, pos, pos, pose);
					
					// Store step information
					tk1		= tk;
					prevx	= x;
					prevy	= y;
					prevz	= z;
				}
				reader.close ();
				output.close ();
			} catch (Exception e) { e.printStackTrace(); }		
		}
		

		/**
		* This method updates current robot location and write it in the Linda space
		*/
		public void step (long ctime)
		{
			//if ((state != RUN) || (data == null))		return;
			
			// Update current robot location
			position_correction ();
			
			// Write perception data to the Linda space
			//lstore.set (lps, ctime);
			//linda.write (ltuple);
			
			// Finish processing by clearing flags
			data		= null;
		}

		public void notify_config (ItemConfig item)
		{
			if (item.props_robot != null)
			{
				rdesc	= new RobotDesc (item.props_robot);
				data    = new RobotData (rdesc);
				lps		= new LPS (rdesc, null);
				
				//state	= RUN;
			}
		}
		public static void main(String[] args)
		{
			Simulation sim = new Simulation();
			Properties cprops = new Properties ();
			try
			{
				File file 				= new File ("conf/robots/iboat.robot");
				FileInputStream stream 	= new FileInputStream (file);
				cprops.load (stream);
				stream.close ();
			} catch (Exception e) { e.printStackTrace (); }

			sim.initialise(cprops);
			sim.notify_config(new ItemConfig (cprops, null, null, 0));
			sim.position_correction();
		}
}

