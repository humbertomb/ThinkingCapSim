/**
 * Created on 30-mar-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.navigation.localisation.outdoor.gui;

import java.io.*;
import java.util.*;

import tc.fleet.*;
import tc.runtime.thread.*;
import tc.shared.linda.*;

import devices.data.*;

import devices.pos.*;

public class PositioningProducer extends StdThread
{
	protected VehicleData			data;				// Vehicle data
	protected PayloadData			payload;				// Vehicle's payload data
	protected Position			pos;
	protected Pose				pose;
	private double				prevx;
	private double				prevy;
	private double				prevz;
	
	protected Tuple				vtuple;
	protected ItemVehData			vitem;
	protected boolean			newdata = false;
	
	public PositioningProducer (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Class methods
	static public PositioningProducer createProducer (String addr, String port)
	{
		LindaServer			linda = null;
		
		// Set up Linda server
		LindaDesc			gldesc;
		Properties			sprops;
		
		sprops	= new Properties ();
		sprops.setProperty ("LINADDR", addr);
		sprops.setProperty ("LINPORT", port);
		gldesc	= new LindaDesc ("LIN", LindaDesc.L_GLOBAL, sprops);			
		try
		{
			linda	= gldesc.start_server ();
		} catch (Exception e) { e.printStackTrace(); }
		
		// Set up Linda producer
		ThreadDesc			ptdesc;
		Properties			pprops;
		
		pprops	= new Properties ();
		pprops.setProperty ("POSCLASS", "tclib.positioning.gui.PositioningProducer");
		pprops.setProperty ("POSMODE", "tcp");
		ptdesc	= new ThreadDesc ("POS", pprops);
		ptdesc.start_thread ("GPSINS Logger", pprops, gldesc, linda);
		
		return (PositioningProducer) ptdesc.thread;
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{
		Tuple				ctuple;
		Properties			cprops;
		File					file;
		FileInputStream		stream;
		
		// Setup vehicle description and data structures		
		data			= new VehicleData ();
		payload		= new PayloadData (0); // MAX_PAYLOAD
		
		// Initialise vehicle state
		pos			= new Position ();
		pose			= new Pose ();
		
		// Set initial pose
		pose.set_spd (0.0, 0.0, 0.0);
		pose.set_ang (0.0, 0.0, 0.0);
		pose.set_rate (0.0, 0.0, 0.0);
		
		// Prepare Linda data structures
		vitem		= new ItemVehData ();
		vtuple		= new Tuple ("VEHDATA", vitem);
		
		// Send CONFIG data
		cprops = new Properties ();
		try
		{
			file 		= new File ("../XFleet/conf/vehicles/garmin.vehicle");
			stream 		= new FileInputStream (file);
			cprops.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }
		ctuple	= new Tuple (Tuple.CONFIG, new ItemConfig (cprops, null, null, 0));
		linda.write (ctuple);
	}
	
	public void step (long time) 
	{
		if (newdata)
		{
			linda.write (vtuple);
			newdata = false;
		}
		
		try { Thread.sleep (500); } catch (Exception e) { e.printStackTrace(); }
		Thread.yield ();
	}
	
	public void setPositionData (GPSData gpsdata, InsData insdata, int delta)
	{
		int				qlty;
		double			x, y, z;
		double			dt;
		boolean			valid;
		
		x		= 0.0;
		y		= 0.0;
		z		= 0.0;
		qlty		= 0;
		valid	= false;
		
		dt		= (double) delta / 1000.0;
		
		// Use GPS position
		if (gpsdata != null)
		{
			x		= gpsdata.getPos ().getEast ();
			y		= gpsdata.getPos ().getNorth ();
			z		= gpsdata.getAltitude ();
			
			qlty		= gpsdata.getNumSat ();
			valid	= true;
		}
		else
		{
			x		= prevx;
			y		= prevy;
			z		= prevz;
		}
		
		// Use INS data
		if (insdata != null)
		{
			pose.set_ang (insdata.getRoll (), insdata.getPitch (), insdata.getYaw ());
			pose.set_rate (insdata.getRollRate (), insdata.getPitchRate (), insdata.getYawRate ());
		}
		
		// Compute ground speed
		pose.set_spd ((prevx - x) / dt, (prevy - y) / dt, (prevz - z) / dt);
		pos.set (x, y, z, pose.yaw ());
		pos.valid (valid);
		
		// Update filtered vehicle data
		data.update (pos, pose, qlty, payload);
		
		// Store step information
		prevx	= x;
		prevy	= y;
		prevz	= z;
		
		newdata = true;
		vitem.set (data, System.currentTimeMillis ());
	}
	
	public void notify_config (String space, ItemConfig item)
	{
	}
}

