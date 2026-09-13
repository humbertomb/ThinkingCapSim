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

import devices.pos.Position;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMZones
{
	protected WMZone[]				areas;
	
	private String					defTexture		= "./conf/3dmodels/textures/floor.jpg";
	
	
	// Accessors
	public final int	 		n () 				{ return areas.length; }
	public final WMZone[]		areas ()				{ return areas; }

	public final String	 	defaultTexture () 	{ return defTexture; }

	// Instance methods
	public WMZone at (int i)
	{
		if ((i < 0) || (i >= areas.length)) 
		    return null;
		return areas[i];
	} 

	public WMZone at (String label)
	{
		int			i;
		
		i	= index (label);		
		if ((i < 0) || (i >= areas.length)) return null;
		return areas[i];
	} 

	public String inZone (Point2 pt)
	{
		return inZone (pt.x (), pt.y ());
	}
	
	public String inZone (Position pt)
	{
		return inZone (pt.x (), pt.y ());
	}
	
	// Devuelve el nombre de la zona de pertenece esa posicion
	public String inZone (double x, double y)
	{
		int			i;
		
		for (i=0; i < areas.length; i++)
			if (areas[i].area.contains (x, y)) 
				return areas[i].label;
			
		return "Unknown";		
	}

	public int index (String label)
	{
		int  i;
		
		if ((label == null) || (areas == null))			return -1;

		for (i = 0; i < areas.length; i++)
			if (label.equals (areas[i].label))			return i;

		return -1;
	}
	
	/* Edition methods (world editor) */
	public void add (WMZone e)
	{
		WMZone[]	tmp = new WMZone[areas.length + 1];
		System.arraycopy (areas, 0, tmp, 0, areas.length);
		tmp[areas.length] = e;
		areas = tmp;
	}

	public WMZone remove (int i)
	{
		if ((i < 0) || (i >= areas.length))		return null;
		WMZone		old = areas[i];
		WMZone[]	tmp = new WMZone[areas.length - 1];
		System.arraycopy (areas, 0, tmp, 0, i);
		System.arraycopy (areas, i + 1, tmp, i, areas.length - i - 1);
		areas = tmp;
		return old;
	}

	public int indexOf (WMZone e)
	{
		for (int i = 0; i < areas.length; i++)
			if (areas[i] == e)				return i;
		return -1;
	}

	public void setDefaultTexture (String texture)		{ defTexture = texture; }

	/* JSON: {defaults: {texture}, items: [...]} */

	public WMZones ()							{ areas = new WMZone[0]; }
	public WMZones (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonObject	o = ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
		JsonObject	def = World.getObject (o, "defaults");
		JsonArray	arr = World.getArray (o, "items");
		defTexture	= World.getString (def, "texture", defTexture);
		areas	= new WMZone[arr.size ()];
		for (int i = 0; i < areas.length; i++)		areas[i] = new WMZone (arr.get (i).getAsJsonObject (), defTexture);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonObject	def = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		def.addProperty ("texture", defTexture);
		for (WMZone z : areas)		arr.add (z.toJson (defTexture));
		o.add ("defaults", def);
		o.add ("items", arr);
		return o;
	}
}
