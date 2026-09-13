package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import wucore.utils.geom.Polygon2;

public class WMFArea extends WMElement
{
	public Polygon2		polygon;
	public String		texture;
	
	// Constructors
	public WMFArea(){
	}

	/* JSON: {label, points: [{x, y, z}, ...] [, texture]} */

	public WMFArea (JsonObject o, String dtexture)
	{
		polygon	= new Polygon2 ();
		label	= World.getString (o, "label", "farea");
		for (JsonElement e : World.getArray (o, "points"))
		{
			JsonObject	p = e.getAsJsonObject ();
			polygon.addPoint (World.getDouble (p, "x"), World.getDouble (p, "y"), World.getDouble (p, "z", 0.0));
		}
		texture	= World.getString (o, "texture", dtexture);
	}

	public JsonObject toJson (String dtexture)
	{
		JsonObject	o = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		o.addProperty ("label", label);
		for (int i = 0; i < polygon.npoints; i++)		arr.add (World.point (polygon.xpoints[i], polygon.ypoints[i], polygon.zpoints[i]));
		o.add ("points", arr);
		if ((texture != null) && !texture.equals (dtexture))		o.addProperty ("texture", texture);
		return o;
	}
}
