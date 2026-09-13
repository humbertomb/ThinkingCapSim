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
public class WMDock extends WMElement
{
	/**
	 * Direction of the material flow at the dock: OUT = material leaves the
	 * dock (load operations), IN = material enters the dock (unload
	 * operations), INOUT = both.
	 */
	public enum FlowType			{ IN, OUT, INOUT }

	static public final FlowType	DEFAULT_FLOW	= FlowType.INOUT;

    public Position					pos;
    public FlowType					flow	= DEFAULT_FLOW;

    public WMDock(Position pos, String label, FlowType flow){
        this.pos = pos;
        this.label = label;
        this.flow = (flow != null) ? flow : DEFAULT_FLOW;
    }

    /** Parses a flow type name (case-insensitive); unknown names give the default. */
    static public FlowType parseFlow (String name)
    {
        if (name == null)				return DEFAULT_FLOW;
        try { return FlowType.valueOf (name.trim ().toUpperCase ()); } catch (IllegalArgumentException e) { return DEFAULT_FLOW; }
    }

    /** True when a task of the given action ("load", "unload", other) can be performed at this dock. */
    public boolean accepts (String action)
    {
        if (action == null)				return true;
        if (action.equalsIgnoreCase ("load"))		return (flow == FlowType.OUT) || (flow == FlowType.INOUT);
        if (action.equalsIgnoreCase ("unload"))		return (flow == FlowType.IN) || (flow == FlowType.INOUT);
        return true;
    }
    
    public WMDock(Position pos, String label){
        this.pos = pos;
        this.label = label;
    }
    
    public Point3 getPos(){
        return new Point3(pos.x(),pos.y(),pos.z());
    }
    public double getAng(){
        return pos.alpha();
    }

    /* JSON: {label, x, y, z, orientation (deg), flow} */

    public WMDock (JsonObject o)
    {
        label	= World.getString (o, "label", "dock");
        pos		= new Position (World.getDouble (o, "x"), World.getDouble (o, "y"), World.getDouble (o, "z", 0.0), Math.toRadians (World.getDouble (o, "orientation", 0.0)));
        flow	= parseFlow (World.getString (o, "flow", null));
    }

    public JsonObject toJson ()
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        World.putPoint (o, pos.x (), pos.y (), pos.z ());
        o.addProperty ("orientation", World.num (Math.toDegrees (pos.alpha ())));
        o.addProperty ("flow", flow.name ());
        return o;
    }
}
