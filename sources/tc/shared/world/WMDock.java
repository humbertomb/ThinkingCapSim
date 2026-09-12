/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.util.StringTokenizer;

import devices.pos.Position;
import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.dxf.entities.TextDxf;
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
    
    public WMDock(String prop) {
        StringTokenizer st = new StringTokenizer (prop,", \t");
        double x1 = Double.parseDouble (st.nextToken());
        double y1 = Double.parseDouble (st.nextToken());
        double z1 = Double.parseDouble (st.nextToken());
        double	r  = Double.parseDouble (st.nextToken()); 				//orientation - degrees
        pos = new Position(x1,y1,z1,Math.toRadians(r));
        label = new String (st.nextToken());
        if (st.hasMoreTokens ())		flow = parseFlow (st.nextToken ());		// optional: older files have no flow
    }

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
    
    public WMDock(TextDxf text) {
        Point3 p3 = text.getPos();
        label = text.getText();
        double ang = 0;
        if(text.ExtendedDouble.size()>0) ang = Math.toRadians(text.getExtDouble(0));
        if(text.ExtendedText.size()>0) flow = parseFlow (text.getExtText(0));
        pos = new Position(p3.x(),p3.y(),p3.z(),ang);
    }
    
    public void toDxf(DXFWorldFile dxf) {
        TextDxf text = new TextDxf(label,getPos(),0.2,"DOCKINGS");
        text.addExtDouble(Math.toDegrees(getAng()));
        text.addExtText(flow.name ());
        dxf.addEntity(text); 
    }
    
    public Point3 getPos(){
        return new Point3(pos.x(),pos.y(),pos.z());
    }
    public double getAng(){
        return pos.alpha();
    }
    
    public String toRawString ()
    {
        return DoubleFormat.format(pos.x ()) + ", " + DoubleFormat.format(pos.y ()) + ", " + DoubleFormat.format(pos.z ()) + ", " + DoubleFormat.format(Math.toDegrees (pos.alpha())) + ", " + label + ", " + flow.name ();
    }
}
