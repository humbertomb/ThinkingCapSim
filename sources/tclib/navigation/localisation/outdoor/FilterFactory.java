/*
 * Created on 23-jul-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.navigation.localisation.outdoor;

import java.util.*;

import devices.data.*;

import devices.pos.*;
import wucore.utils.logs.*;
import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 * @author Rafael Toledo Moreo
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FilterFactory extends Object
{
	// Filter and positioning variables
	protected Filter				filter;
	protected Variables				variables;
	protected boolean				block;
	
	// Log and support variables
	protected LogFile				log;
	protected boolean				doinlog;   
	protected boolean				dooutlog;   
	
	// Constructors
	public FilterFactory (String prop)
	{
		StringTokenizer		st;
		String				token;
		String 				fltname;
		Class				fltclass;

		// Default values
		block		= false;
		doinlog		= false;
		dooutlog	= false;
		
		// Create filter instance
		try
		{
			st			= new StringTokenizer (prop,"|, \t");
			fltname		= st.nextToken ();
			while (st.hasMoreTokens ())
			{
				token	= st.nextToken ().toLowerCase ();
				
				if (token.equals ("block"))
					block		= true;
				else if (token.equals ("inlog"))
					doinlog		= true;
				else if (token.equals ("outlog"))
					dooutlog		= true;
				else if (token.equals ("log"))
				{
					doinlog		= true;
					dooutlog		= true;
				}
			}
//			xxx			= Boolean.valueOf (st.nextToken ()).booleanValue ();	
//			xxx 		= Integer.parseInt (st.nextToken());

			fltclass		= Class.forName (fltname);
			filter		= (Filter) fltclass.newInstance();
			
 			System.out.println ("# Created instance of filter <"+fltname+"> "+toString ());
 		} catch (Exception e) { e.printStackTrace(); }

 		// Initialise variables
 		variables		= new Variables ();
		variables.tini	= System.currentTimeMillis ();			
 		filter.initialise (variables);

		// Initialise logging
		if (doinlog || dooutlog)
		{
			log		= new LogFile ("logs/filter", ".dump");
			log.open ();
			log.write ("INI", variables.tini, -1, null);
		}
	}
	
	// Accessors
	public Position		getPosition ()		{ return filter.getPosition (); }
	public Pose			getPose ()			{ return filter.getPose (); }
	public boolean		isBlockMode ()		{ return block; }
	
	// Instance methods
	public void update (CompassData data, long ctime)
	{
		variables.compasscounter ++;
		variables.tstampcmp		= (double) (ctime - variables.tini) / 1000.0;
		variables.tscmp			= (double) (variables.tstampcmp - variables.t1cmp);

		// collecting data
		variables.yawcmp		= data.getHeading ();
		variables.rollcmp		= data.getRoll();
		variables.pitchcmp		= data.getPitch();
		
		// time stuff
		variables.t1cmp			= variables.tstampcmp;		
		
		if (doinlog)
			log.write ("CMP", ctime, variables.compasscounter, data.toDatalog ());
	}

	public void update (GPSData data, long ctime)
	{
	  	UTMPos		upos;
	  	Point3		vel;

		// time issues
		variables.gpscounter ++;
		variables.tstampgps		= (double) (ctime - variables.tini) / 1000.0;
		variables.tsgps			= (double) (variables.tstampgps - variables.t1gps);

	  	// collecting data
	  	upos					= data.getPos ();
   		variables.xgps 			= upos.getEast ();
		variables.ygps 			= upos.getNorth ();
		variables.zgps			= data.getAltitude ();
	  	vel						= data.getVel ();
   		variables.xvgps			= vel.x();
   		variables.yvgps			= vel.y();
   		variables.zvgps			= vel.z();
		variables.qgps			= data.getFix ();
		variables.towgps		= data.getWeekSeconds();
		
		// time stuff
		variables.t1gps			= variables.tstampgps;

		if (doinlog)
			log.write ("GPS", ctime, variables.gpscounter, data.toDatalog ());
	}
	
	public void update (InsData data, long ctime)
	{
		// time issues
		variables.inscounter ++;
        variables.tstampins		= (double) (ctime - variables.tini) / 1000.0;
		variables.tsins			= (double) (variables.tstampins - variables.t1ins);
		
		// collecting data
		variables.rollins		= data.getRoll ();
		variables.pitchins		= data.getPitch ();
		variables.yawins		= data.getYaw ();
		variables.rollrateins	= data.getRollRate ();
		variables.pitchrateins	= data.getPitchRate ();
		variables.yawrateins	= data.getYawRate ();
		variables.axins			= data.getAccX ();
		variables.ayins			= data.getAccY ();
		variables.azins			= data.getAccZ ();
		variables.tempins		= data.getTemp ();
		variables.qins			= true;
		// yawrateins viene al revs que el dijujo del ins y que el comps
		// le cambiamos el signo para que quede igual en todas partes

		// time stuff
		variables.t1ins			= variables.tstampins;

		if (doinlog)
			log.write ("INS", ctime, variables.inscounter, data.toDatalog ());
	}
	
	public void filter (GPSData gps, CompassData cmp, InsData ins, long ctime)
	{
		if (gps != null)	
		{
			update (gps, ctime);
			if (!block)		filter (ctime);
		}
		if (cmp != null)				
		{
			update (cmp, ctime);
			if (!block)		filter (ctime);
		}
		if (ins != null)						
		{
			update (ins, ctime);
			if (!block)		filter (ctime);
		}
		
		if (block)			filter (ctime);
	}
	
	public Position filter (long ctime)
	{
		// running fusion
		filter.step ();

		if (dooutlog && variables.gpscounter > 1)
			log.write ("EKF", ctime, filter.getCounter (), filter.datalog ());

		return filter.getPosition ();
	}

	public void stop ()
	{
		if (log != null)
			log.close ();
			
		log		= null;
	}
	
	public String toString ()
	{
		String		out;
		
		out	= "[";
		if (block)			out += "BLOCK ";
		if (doinlog)			out += "IN_LOG ";
		if (dooutlog)		out += "OUT_LOG ";
		out	= "]";
		
		return out;
	}
}
