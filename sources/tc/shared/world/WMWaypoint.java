/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;


import devices.pos.Position;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMWaypoint extends WMElement
{
    public Position pos;
    
    public WMWaypoint(Position pos, String label){
        this.pos = pos;
        this.label = label;
    }
    
    public Point3 getPos(){
        return new Point3(pos.x(),pos.y(),pos.z());
    }
    public double getAng(){
        return pos.alpha();
    }

    /* JSON: {label, x, y, z, orientation (deg)} */

    public WMWaypoint (JsonObject o)
    {
        label	= World.getString (o, "label", "wp");
        pos		= new Position (World.getDouble (o, "x"), World.getDouble (o, "y"), World.getDouble (o, "z", 0.0), Math.toRadians (World.getDouble (o, "orientation", 0.0)));
    }

    public JsonObject toJson ()
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        World.putPoint (o, pos.x (), pos.y (), pos.z ());
        o.addProperty ("orientation", World.num (Math.toDegrees (pos.alpha ())));
        return o;
    }
}
