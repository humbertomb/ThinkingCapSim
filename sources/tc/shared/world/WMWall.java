/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;


import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMWall extends WMElement
{
	// 2D components
	public Line2						edge;
	
	// 2 1/2 D components
	public double					width;
	public double					height;
	
	// Visualization components
	public String					texture;					
	
	public WMWall(){
	}
	
	


	/* JSON: {x1, y1, z1, x2, y2, z2 [, width, height, texture]} (missing values take the collection defaults) */

	public WMWall (JsonObject o, double dwidth, double dheight, String dtexture)
	{
		edge	= World.toLine (o);
		width	= World.getDouble (o, "width", dwidth);
		height	= World.getDouble (o, "height", dheight);
		texture	= World.getString (o, "texture", dtexture);
	}

	/** Width, height and texture are only written when they differ from the defaults. */
	public JsonObject toJson (double dwidth, double dheight, String dtexture)
	{
		JsonObject	o = World.line (edge);
		if (width != dwidth)							o.addProperty ("width", World.num (width));
		if (height != dheight)							o.addProperty ("height", World.num (height));
		if ((texture != null) && !texture.equals (dtexture))	o.addProperty ("texture", texture);
		return o;
	}
}
