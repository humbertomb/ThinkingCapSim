/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.Vector;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMWaypoints
{
	protected WMWaypoint[] waypoints;
	

	// Constructors
	public WMWaypoints (Properties props)
	{
		fromProperties (props);
	}
	
	public WMWaypoints (DXFWorldFile dxf){
		Vector entities = dxf.getEntities();
		Vector wp = new Vector();
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("WAYPOINTS")){
				if(entity instanceof TextDxf){ 
					wp.add(new WMWaypoint((TextDxf)entity)); 
				}
			}
		}
		waypoints = new WMWaypoint[wp.size()];
		wp.toArray(waypoints);
	}
	
	// Accessors	
	public final int	 		n () 				{ return waypoints.length; }
	public final WMWaypoint[]	waypoints ()		{ return waypoints; }

	// Instance methods
	public WMWaypoint at (int i)
	{
		if ((i < 0) || (i >= waypoints.length)) return null;
		return waypoints[i];
	}
	
	// Instance methods
	public WMWaypoint at (String label)
	{
		int i = index(label);
		if(i<0)	
		    return null;
		return waypoints[i];
	}
	
	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (waypoints == null))			return -1;

		for (i = 0; i < waypoints.length; i++)
			if (label.equals (waypoints[i].label))			return i;

		return -1;
	}
	
	public void fromProperties (Properties props)
	{
		String				prop;
		waypoints = new WMWaypoint[Integer.parseInt (props.getProperty ("WPOINTS","0"))];
		for (int i=0; i < waypoints.length; i++){
			prop		= props.getProperty ("WPOINT_"+i);		
			waypoints[i] = new WMWaypoint (prop);
		}
	}
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("WPOINTS",Integer.toString (waypoints.length));
		
		for (i = 0; i < waypoints.length; i++)
			props.setProperty ("WPOINT_"+i, waypoints[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		// Print Cilindrical Beacon
		out.println("# ==============================");
		out.println("# WAYPOINTS");
		out.println("# ==============================");
		out.println ("WPOINTS = " + n());
		out.println("");
		for (int i = 0; i < waypoints.length; i++)
			out.println ("WPOINT_" + i + " = " + waypoints[i].toRawString ());
		out.println ();
	}
	
	public void toDxfFile (DXFWorldFile dxf){
		 // Define una capa con un color determinado (opcional)
		 dxf.addLayer(new Layer("WAYPOINTS",ACADColor.MAGENTA));

	    for (int i = 0; i < waypoints.length; i++){
	        waypoints[i].toDxf(dxf);
	    }
	}
	
}
