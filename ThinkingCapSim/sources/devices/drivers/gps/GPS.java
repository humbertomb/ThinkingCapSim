/**
 * Title: GPS
 * Description:  Clase genérica para un dispositivo receptor GPS
 * Copyright: Copyright (c) 2000
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Qui–onero (juanpe@dif.um.es)
 * @version 1.0
 */

package devices.drivers.gps;

import devices.data.*;
import devices.utils.*;

public abstract class GPS extends Object
{
	static public final double		SEMIC_UNIT	= Math.pow (2.0, -31.0) * 180.0;

	// Factory and connection variables
	protected String					clase;
	protected String 				port;
	protected ProgressListener		progress;
	
	// ------------------------------
	// CLASS LOADING (FACTORIES)
	// ------------------------------
   
	static public GPS getGPS (String prop) throws GPSException
    {
		GPS 				tg = null;
		String 			cl;
		String 			prt;

		try
		{
			// Parse parameters and create a device instance
			cl		= prop.substring (0, prop.indexOf("|"));
			prt		= prop.substring (prop.indexOf("|")+1, prop.length());
			Class gpsclass = Class.forName(cl);
			tg		= (GPS) gpsclass.newInstance();
			
 			System.out.println ("GPS: connecting "+cl+" to port <"+prt+">");
 			
 			// Initialise device
 			tg.setType (cl);
			tg.setPort (prt);
			tg.initialise ();
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		return tg;
    }

	// Accessors
	public final void		setType (String _clase)						{ clase = _clase; }
	public final void 		setPort (String _port)						{ port = _port; }
	public final String		getType ()									{ return clase; }
	public final String		getPort ()									{ return port; }
	public final void		setProgress (ProgressListener progress)		{ this.progress = progress; }

	
	// Abstract methods
	protected abstract void				initialise () throws GPSException;
	public abstract void					close () throws GPSException;
	public abstract GPSData				getData ();
	public abstract SatelliteData[]		getSatellites ();
	
	// Instance methods (to be subclassed)
	public WaypointData[] getWaypoints () throws GPSException
	{
		return null;
	}		
	
	public TracklogData[] getTracklog () throws GPSException
	{
		return null;
	}		
}