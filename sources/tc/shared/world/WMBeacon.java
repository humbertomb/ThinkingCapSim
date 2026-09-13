/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;


import devices.pos.Position;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMBeacon extends WMElement
{
    
    static public final double	DEF_HEIGHT	= 0.5;		// default plate height (m)

    public Position			pos;
    public double				width;
    public double				height	= DEF_HEIGHT;
    
    public WMBeacon(String label, Position pos, double width){
        this (label, pos, width, DEF_HEIGHT);
    }
    
    public WMBeacon(String label, Position pos, double width, double height){
        this.label = label;
        this.pos = pos;
        this.width = width;
        this.height = height;
    }
    
    public Line2 getLine(){
        double Ax,Ay;
        Ax = (width*Math.cos(pos.alpha())/2);
        Ay = (width*Math.sin(pos.alpha())/2);
        
        return new Line2(pos.x()-Ax,pos.y()-Ay,pos.x()+Ax,pos.y()+Ay);	
    }
        
    public double getAng(){
        return pos.alpha();
    }
    

    /* JSON: {label, x, y, z, orientation (deg), width, height} */

    public WMBeacon (JsonObject o)
    {
        label	= World.getString (o, "label", "b");
        pos		= new Position (World.getDouble (o, "x"), World.getDouble (o, "y"), World.getDouble (o, "z", 0.0), Math.toRadians (World.getDouble (o, "orientation", 0.0)));
        width	= World.getDouble (o, "width", 0.2);
        height	= World.getDouble (o, "height", DEF_HEIGHT);
    }

    public JsonObject toJson ()
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        World.putPoint (o, pos.x (), pos.y (), pos.z ());
        o.addProperty ("orientation", World.num (Math.toDegrees (pos.alpha ())));
        o.addProperty ("width", World.num (width));
        o.addProperty ("height", World.num (height));
        return o;
    }
}
