/*
 * Created on 18-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.data;

import java.awt.*;

import devices.pos.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WaypointData
{
	// Waypoint class types (TODO using GARMIN codes)
	static public final int		WP_USER			= 0x00;
	static public final int		WP_AVTN_APT		= 0x40;
	static public final int		WP_AVTN_INT		= 0x41;
	static public final int		WP_AVTN_NDB		= 0x42;
	static public final int		WP_AVTN_VOR		= 0x43;
	static public final int		WP_AVTN_ARWY	= 0x44;
	static public final int		WP_AVTN_AINT	= 0x45;
	static public final int		WP_AVTN_ANDB	= 0x46;
	static public final int		WP_MAP_PNT		= 0x80;
	static public final int		WP_MAP_AREA		= 0x81;
	static public final int		WP_MAP_INT		= 0x82;
	static public final int		WP_MAP_ADRS		= 0x83;
	static public final int		WP_MAP_LABEL	= 0x84;
	static public final int		WP_MAP_LINE		= 0x85;
	
	public String				id				= "N/A";		// Wp identifier
	public LLAPos				pos;							// Wp position (LLA)
	public double				height			= 0.0;			// Wp height (m)
	public double				depth			= 0.0;			// Wp depth (m)
	public Color				color;
	public int					symbol;
	public int					cls				= WP_USER;		// Wp class

	// Constructor
	public WaypointData ()
	{
		pos		= new LLAPos ();
		color	= Color.BLACK;
	}
	
	// Class methods
	
	/* ---------------------------- */
	/*     UTILITY FUNCTIONS        */
	/* ---------------------------- */

	/**
	* Method that translates a wpt class type into a human-readable string. 
	*/	
	public static String classToString (int cls) 
	{
		switch (cls) 
		{
			case WP_USER :	
				return "User defined";
			case WP_AVTN_APT :
				return "Aviation Airport";
			case WP_AVTN_INT :
				return "Aviation Intersection";
			case WP_AVTN_NDB :
				return "Aviation NDB";
			case WP_AVTN_VOR :
				return "Aviation VOR";
			case WP_AVTN_ARWY :
				return "Aviation Airport Runway";
			case WP_AVTN_AINT :
				return "Aviation Airport Intersection";
			case WP_AVTN_ANDB :
				return "Aviation Airport NDB";
			case WP_MAP_PNT :
				return "Map Point";
			case WP_MAP_AREA :
				return "Map Area";
			case WP_MAP_INT :
				return "Map Intersection";
			case WP_MAP_ADRS :
				return "Map Address";
			case WP_MAP_LABEL :
				return "Map Label";
			case WP_MAP_LINE :
				return "Map Line";
			default :
				return "Unknown type "+cls;
		}
	}
	
	// Instance methods
	public void set (String id, double lat, double lon, double height)
	{
		this.id			= id;
		this.height		= height;
		
		pos.setLatLon (lat, lon);
	}
	
	public String toString ()
	{
		return "Id=<"+id+"> Pos="+pos+" Class=<"+classToString (cls)+"> Symbol=<"+symbol+">";
	}
}
