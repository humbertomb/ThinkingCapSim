/*
 * (c) 2004 Humberto Martinez Barbera
 */
 
package tclib.navigation.localisation.outdoor;

import java.util.*;

import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.vrobot.*;
import tc.modules.*;

import devices.pos.*;

/**
* Outdoor Positioning Module Implementation
*/
public class OutdoorPositioning extends Perception
{
	// Temporal referentes
	private long					tk;
	private long					tk1;
	private double				prevx;
	private double				prevy;
	private double				prevz;
	
	// Localisation related structures
	protected Position			pos;					// Current position (GPS corrected)
	protected Pose				pose;				// Current pose (INS corrected)
	protected FilterFactory		kloc;				// Kalman based position correction filter
	protected boolean			kfilter	= false;		// Activate Kalman filter?

	// Constructors
	public OutdoorPositioning (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{
		String		fclass;
		
		super.initialise (props);
		
		fclass		= props.getProperty ("FILTER");
		if (fclass == null)		fclass = "tclib.navigation.localisation.outdoor.KalmanSimple";
		
		// Initialise positioning filter
		kloc		= new FilterFactory (fclass);
		pos		= new Position ();
		pose		= new Pose ();
		
		// Set initial pose
		pose.set_spd (0.0, 0.0, 0.0);
		pose.set_ang (0.0, 0.0, 0.0);
		pose.set_rate (0.0, 0.0, 0.0);
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
		
		tk	= System.currentTimeMillis ();
		dt	= (double) (tk - tk1) / 1000.0;
		
		// Use GPS position
		if ((data.gps.length > 0) && (data.gps[0] != null))
		{
			x	= data.gps[0].getPos ().getEast ();
			y	= data.gps[0].getPos ().getNorth ();
			z	= data.gps[0].getAltitude ();

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
			pose.set_ang (data.ins[0].getRoll (), data.ins[0].getPitch (), a);
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
			
		// Update low-level perception & LPS data
		lps.update (data, null, pos, pos, pose);
		
		// Store step information
		tk1		= tk;
		prevx	= x;
		prevy	= y;
		prevz	= z;
	}

	/**
	* This method updates current robot location and write it in the Linda space
	*/
	public void step (long ctime)
	{
		if ((state != RUN) || (data == null))		return;
		
		// Update current robot location
		position_correction ();
			
		// Write perception data to the Linda space
		lstore.set (lps, ctime);		
		linda.write (ltuple);
		
		// Finish processing by clearing flags
		data		= null;
	}

	public void notify_config (String space, ItemConfig item)
	{
		if (item.props_robot != null)
		{
			rdesc	= new RobotDesc (item.props_robot);
			lps		= new LPS (rdesc, null);
			
			state	= RUN;
		}
	}
}

