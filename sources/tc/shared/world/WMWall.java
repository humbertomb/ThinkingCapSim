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


import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.LineDxf;
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
	
	
	public WMWall (LineDxf line, double dwidth, double dheight, String dtexture){
	      edge = new Line2(line.getStart().x(),line.getStart().y(),line.getStart().z(),line.getEnd().x(),line.getEnd().y(),line.getEnd().z());
			if(line.ExtendedDouble.size()>0) 
			    height = line.getExtDouble(0);
			else
			    height = dheight;
			if(line.ExtendedDouble.size()>1) 
			    width = line.getExtDouble(1);
			else										
			    width = dwidth;
			if(line.ExtendedText.size()>0) 
			    texture = line.getExtText(0);
			else
			    texture = dtexture;
			label = "LINE_?";
	}
	
	public void toDxf(DXFWorldFile dxf){
	    LineDxf line = new LineDxf(
               new Point3(edge.orig().x(), edge.orig().y(), edge.z1()),
               new Point3(edge.dest().x(), edge.dest().y(), edge.z2()),
               "0"
       );
       line.addExtDouble(0,height);
       line.addExtDouble(1,width);
       line.addExtText(0,texture);
       dxf.addEntity(line);
	}
	


	/* JSON: {x1, y1, z1, x2, y2, z2 [, width, height, texture]} (missing values take the collection defaults) */

	public WMWall (JsonObject o, double dwidth, double dheight, String dtexture)
	{
		edge	= WorldJson.toLine (o);
		width	= WorldJson.getDouble (o, "width", dwidth);
		height	= WorldJson.getDouble (o, "height", dheight);
		texture	= WorldJson.getString (o, "texture", dtexture);
	}

	/** Width, height and texture are only written when they differ from the defaults. */
	public JsonObject toJson (double dwidth, double dheight, String dtexture)
	{
		JsonObject	o = WorldJson.line (edge);
		if (width != dwidth)							o.addProperty ("width", WorldJson.num (width));
		if (height != dheight)							o.addProperty ("height", WorldJson.num (height));
		if ((texture != null) && !texture.equals (dtexture))	o.addProperty ("texture", texture);
		return o;
	}
}
