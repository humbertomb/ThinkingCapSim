/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;

import java.awt.geom.Rectangle2D;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMZone extends WMElement
{
    public Rectangle2D.Double			area;
    public double					z;				// Elevation of the zone floor (m)
    public String					texture;
    
    // Accessors
    public double		minx ()			{ return area.getX (); }
    public double		miny ()			{ return area.getY (); }
    public double		width ()			{ return area.getWidth (); }
    public double		height ()		{ return area.getHeight (); }
    
    
    public WMZone(){
    }
    
    public Line2[] toLines(){
    	Line2[] lines = new Line2[4];
    	lines[0] = new Line2(area.getMinX(),area.getMinY(),area.getMinX(),area.getMaxY());
    	lines[1] = new Line2(area.getMinX(),area.getMinY(),area.getMaxX(),area.getMinY());
    	lines[2] = new Line2(area.getMaxX(),area.getMaxY(),area.getMinX(),area.getMaxY());
    	lines[3] = new Line2(area.getMaxX(),area.getMaxY(),area.getMaxX(),area.getMinY());
    	return lines;
    }
    


    /* JSON: {label, x, y, z, width, height [, texture]} */

    public WMZone (JsonObject o, String dtexture)
    {
        label	= World.getString (o, "label", "zone");
        z		= World.getDouble (o, "z", 0.0);
        area 	= new Rectangle2D.Double (World.getDouble (o, "x"), World.getDouble (o, "y"), World.getDouble (o, "width"), World.getDouble (o, "height"));
        texture	= World.getString (o, "texture", dtexture);
    }

    public JsonObject toJson (String dtexture)
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        World.putPoint (o, area.getX (), area.getY (), z);
        o.addProperty ("width", World.num (area.getWidth ()));
        o.addProperty ("height", World.num (area.getHeight ()));
        if ((texture != null) && !texture.equals (dtexture))		o.addProperty ("texture", texture);
        return o;
    }
}
