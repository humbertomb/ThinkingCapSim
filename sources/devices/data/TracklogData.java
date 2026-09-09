/*
 * Created on 19-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.data;

import java.awt.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TracklogData
{
	public String				id			= "Default Log";		// Trk log identifier
	public Color			color;							// Trk log color
	public boolean				visible		= true;				// Is the Trk Log visible?
	public TracklogPoint[]		pts;								// Trk log points
	public int					npts;							// Trk log number of points
	
	// Constructor
	public TracklogData ()
	{
		pts		= new TracklogPoint[5000];
		npts	= 0;
		color	= Color.BLACK;
	}
	
	public void set (String id, boolean visible)
	{
		this.id			= id;
		this.visible	= visible;
	}
	
	public void add (double lat, double lon, double alt, long time, boolean newseg)
	{
		pts[npts]		= new TracklogPoint ();
		pts[npts].set (lat, lon, alt, time, newseg);
		
		npts++;
	}

	public String toString ()
	{
		int				i;
		String			output;
		
		output		= "TRACK LOG Name=<"+id+">\n";		
		for (i = 0; i < npts; i++)
			output		+= "\t" + pts[i] + "\n";
		
		return output;
	}
}
