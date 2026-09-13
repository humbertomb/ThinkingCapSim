package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import wucore.utils.geom.Polygon2;

public class WMFAreas
{
	protected WMFArea[]	fareas;
	
	private String defTexture		= "./conf/3dmodels/textures/farea.jpg";
	
	
	// Accessors
	public final int	 	n () 		{ return fareas.length; }
	public final WMFArea[]	areas ()	{ return fareas; }
	
	public final String	 	defaultTexture () 	{ return defTexture; }
	
	// Instance methods
	public WMFArea at (int i)
	{
		if ((i < 0) || (i >= fareas.length))
			return null;
		return fareas[i];
	}
	
	public String toString ()					{ return World.toText (toJson ()); }

	public Polygon2[] getPolygons(){
		Polygon2[] polygons = new Polygon2[fareas.length];
		
		for(int i = 0; i < fareas.length; i++)
			polygons[i] = fareas[i].polygon;
		
		return polygons;
	}

	/* Edition methods (world editor) */
	public void add (WMFArea e)
	{
		WMFArea[]	tmp = new WMFArea[fareas.length + 1];
		System.arraycopy (fareas, 0, tmp, 0, fareas.length);
		tmp[fareas.length] = e;
		fareas = tmp;
	}

	public WMFArea remove (int i)
	{
		if ((i < 0) || (i >= fareas.length))		return null;
		WMFArea		old = fareas[i];
		WMFArea[]	tmp = new WMFArea[fareas.length - 1];
		System.arraycopy (fareas, 0, tmp, 0, i);
		System.arraycopy (fareas, i + 1, tmp, i, fareas.length - i - 1);
		fareas = tmp;
		return old;
	}

	public int indexOf (WMFArea e)
	{
		for (int i = 0; i < fareas.length; i++)
			if (fareas[i] == e)				return i;
		return -1;
	}

	public void setDefaultTexture (String texture)		{ defTexture = texture; }

	/* JSON: {defaults: {texture}, items: [...]} */

	public WMFAreas ()							{ fareas = new WMFArea[0]; }
	public WMFAreas (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonObject	o = ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
		JsonObject	def = World.getObject (o, "defaults");
		JsonArray	arr = World.getArray (o, "items");
		defTexture	= World.getString (def, "texture", defTexture);
		fareas	= new WMFArea[arr.size ()];
		for (int i = 0; i < fareas.length; i++)		fareas[i] = new WMFArea (arr.get (i).getAsJsonObject (), defTexture);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonObject	def = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		def.addProperty ("texture", defTexture);
		for (WMFArea f : fareas)		arr.add (f.toJson (defTexture));
		o.add ("defaults", def);
		o.add ("items", arr);
		return o;
	}
}
