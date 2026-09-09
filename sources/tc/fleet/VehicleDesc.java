/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2003 Humberto Martinez
 */

package tc.fleet;

import java.util.*;
import java.io.*;

import wucore.utils.geom.*;

public class VehicleDesc extends Object implements Serializable
{
	// Vehicle parameters
	public long					DTIME;					// Current update cycle (ms)
	
	// Vehicle additional data
	public PayloadDesc			pldesc;					// Payload description

	// Platform description
	public double				RADIUS; 					// Vehicle outer circle radius (m)
	public int					LINES;					// Number of line segments in the icon
	public Line2[]				icon		= null;			// Line segments in the icon
	public String				image	= null;			// Image icon file name

	/* Constructors */
	public VehicleDesc (String name)
	{
		Properties		props;
		File				file;
		FileInputStream	stream;

		props			= new Properties ();
		try
		{
			file 		= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }

		update (props);
	}

	public VehicleDesc (Properties props)
	{
		update (props);
	}

	/* Instance methods */
	protected void update (Properties props)
	{
		int				i;
		double			xi = 0.0, yi = 0.0;
		double			xf = 0.0, yf = 0.0;
		boolean			modified = true;

		// Load vehicle properties
		try { DTIME	 		= new Long (props.getProperty ("DTIME")).longValue (); } 			catch (Exception e) 	{ }

		try { RADIUS 		= new Double (props.getProperty ("RADIUS")).doubleValue (); } 		catch (Exception e) 	{ }
		try { LINES		 	= new Integer (props.getProperty ("LINES")).intValue (); } 			catch (Exception e) 	{ modified = false; }
		
		// TODO the new properties MUST include all the segments. Could be done better
		if ((LINES > 0) && modified)
		{
			icon			= new Line2 [LINES];
			for (i = 0; i < LINES; i++)
			{
				try { xi		= new Double (props.getProperty ("iconxi" + i)).doubleValue (); } 	catch (Exception e) 	{ }
				try { yi		= new Double (props.getProperty ("iconyi" + i)).doubleValue (); } 	catch (Exception e) 	{ }
				try { xf		= new Double (props.getProperty ("iconxf" + i)).doubleValue (); } 	catch (Exception e) 	{ }
				try { yf		= new Double (props.getProperty ("iconyf" + i)).doubleValue (); }	catch (Exception e) 	{ }
				icon[i]		= new Line2 ();
				icon[i].set (xi, yi, xf, yf);
			}
		}
		
		if (props.getProperty ("IMAGE") != null)
			image		= props.getProperty ("IMAGE");

		if (pldesc == null)
			pldesc		= new PayloadDesc (props);
		else
			pldesc.update (props);
	}

	public double[] getIconLimits(){

	    double xi,yi,xf,yf;
	    double minx, miny, maxx, maxy;
	    
	    maxx = Double.MIN_VALUE;
	    maxy = Double.MIN_VALUE;
	    minx = Double.MAX_VALUE;
	    miny = Double.MAX_VALUE;
	    
	    for(int i = 0; i<icon.length; i++){
	        xi = icon[i].orig().x();
	        yi = icon[i].orig().y();
	        xf = icon[i].dest().x();
	        yf = icon[i].dest().y();
	        
		    if(xi < minx) minx = xi;
			if(xf < minx) minx = xf;
			if(yi < miny) miny = yi;
			if(yf < miny) miny = yf;
			if(xi > maxx) maxx = xi;
			if(xf > maxx) maxx = xf;
			if(yi > maxy) maxy = yi;
			if(yf > maxy) maxy = yf;
	    }
	    
	    double ret[] = {minx, miny, maxx, maxy};
	    return ret;
	}
	
	public String toString ()
	{
		String			str;
		
		str		= "TIME<D=" + DTIME + ">ms";
		
		return str;
	}
}

