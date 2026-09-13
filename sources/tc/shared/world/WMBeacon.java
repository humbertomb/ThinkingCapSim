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


import devices.pos.Position;
import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.entities.TextDxf;
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
    
    public WMBeacon(TextDxf text) {
        Point3 p3 = text.getPos();
        label = text.getText();
        double rot = 0.0;
        width = 0.0;
        if(text.ExtendedDouble.size()>0) rot = Math.toRadians(text.getExtDouble(0));	
        if(text.ExtendedDouble.size()>1) width = text.getExtDouble(1);	
        if(text.ExtendedDouble.size()>2) height = text.getExtDouble(2);	
        pos = new Position(p3.x(),p3.y(),rot);
    }
    
    public WMBeacon(LineDxf linedxf) {
        Line2 line = new Line2();
        line.set(linedxf.getStart(),linedxf.getEnd());
        Point3 p3 = new Point3(line.center());
        if(linedxf.ExtendedText.size()>0) label = linedxf.getExtText(0);	
        
        pos = new Position(p3.x(),p3.y(),line.angle());
    }
    
    public void toDxf(DXFWorldFile dxf) {
        TextDxf text = new TextDxf(label,new Point3(pos.x(),pos.y(),0.0),0.2,"BEACONS");		
        text.addExtDouble(Math.toDegrees(pos.alpha()));
        text.addExtDouble(width);
        text.addExtDouble(height);
        
        dxf.addEntity(text);  
    }
    
    public void toDxf1(DXFWorldFile dxf) {
        Line2 line = getLine();
        LineDxf linedxf = new LineDxf(new Point3(line.orig()),new Point3(line.dest()),"BEACONS");		
        linedxf.addExtText(label);
        
        dxf.addEntity(linedxf);
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
        label	= WorldJson.getString (o, "label", "b");
        pos		= new Position (WorldJson.getDouble (o, "x"), WorldJson.getDouble (o, "y"), WorldJson.getDouble (o, "z", 0.0), Math.toRadians (WorldJson.getDouble (o, "orientation", 0.0)));
        width	= WorldJson.getDouble (o, "width", 0.2);
        height	= WorldJson.getDouble (o, "height", DEF_HEIGHT);
    }

    public JsonObject toJson ()
    {
        JsonObject	o = new JsonObject ();
        o.addProperty ("label", label);
        WorldJson.putPoint (o, pos.x (), pos.y (), pos.z ());
        o.addProperty ("orientation", WorldJson.num (Math.toDegrees (pos.alpha ())));
        o.addProperty ("width", WorldJson.num (width));
        o.addProperty ("height", WorldJson.num (height));
        return o;
    }
}
