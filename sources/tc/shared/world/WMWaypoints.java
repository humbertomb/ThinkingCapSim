/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

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
	

	
	public WMWaypoints (DXFWorldFile dxf){
		ArrayList<Entity> entities = dxf.getEntities();
		ArrayList<WMWaypoint> wp = new ArrayList<WMWaypoint>();
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = entities.get(i);
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
	
	public void toDxfFile (DXFWorldFile dxf){
		 // Define una capa con un color determinado (opcional)
		 dxf.addLayer(new Layer("WAYPOINTS",ACADColor.MAGENTA));

	    for (int i = 0; i < waypoints.length; i++){
	        waypoints[i].toDxf(dxf);
	    }
	}

	/* Edition methods (world editor) */
	public void add (WMWaypoint e)
	{
		WMWaypoint[]	tmp = new WMWaypoint[waypoints.length + 1];
		System.arraycopy (waypoints, 0, tmp, 0, waypoints.length);
		tmp[waypoints.length] = e;
		waypoints = tmp;
	}

	public WMWaypoint remove (int i)
	{
		if ((i < 0) || (i >= waypoints.length))		return null;
		WMWaypoint		old = waypoints[i];
		WMWaypoint[]	tmp = new WMWaypoint[waypoints.length - 1];
		System.arraycopy (waypoints, 0, tmp, 0, i);
		System.arraycopy (waypoints, i + 1, tmp, i, waypoints.length - i - 1);
		waypoints = tmp;
		return old;
	}

	public int indexOf (WMWaypoint e)
	{
		for (int i = 0; i < waypoints.length; i++)
			if (waypoints[i] == e)				return i;
		return -1;
	}

	/* JSON: [{label, x, y, z, orientation}, ...] */

	public WMWaypoints ()							{ waypoints = new WMWaypoint[0]; }
	public WMWaypoints (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		waypoints	= new WMWaypoint[arr.size ()];
		for (int i = 0; i < waypoints.length; i++)		waypoints[i] = new WMWaypoint (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMWaypoint x : waypoints)		arr.add (x.toJson ());
		return arr;
	}
}
