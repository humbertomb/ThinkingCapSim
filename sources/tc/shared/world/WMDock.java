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
    public Position pos;
    
    public WMDock(String prop) {
        StringTokenizer st = new StringTokenizer (prop,", \t");
        double x1 = Double.parseDouble (st.nextToken());
        double y1 = Double.parseDouble (st.nextToken());
        double z1 = Double.parseDouble (st.nextToken());
        double	r  = Double.parseDouble (st.nextToken()); 				//orientation - degrees
        pos = new Position(x1,y1,z1,Math.toRadians(r));
        label = new String (st.nextToken());
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
        pos = new Position(p3.x(),p3.y(),p3.z(),ang);
    }
    
    public void toDxf(DXFWorldFile dxf) {
        TextDxf text = new TextDxf(label,getPos(),0.2,"DOCKINGS");
        text.addExtDouble(Math.toDegrees(getAng()));
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
        return DoubleFormat.format(pos.x ()) + ", " + DoubleFormat.format(pos.y ()) + ", " + DoubleFormat.format(pos.z ()) + ", " + DoubleFormat.format(Math.toDegrees (pos.alpha())) + ", " + label;
    }
}
