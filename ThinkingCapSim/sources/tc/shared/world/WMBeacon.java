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
    
    public Position			pos;
    public double				width;
    
    
    public WMBeacon(String prop) {
        StringTokenizer st = new StringTokenizer (prop,", \t");
        double px	= Double.parseDouble (st.nextToken()); 
        double py 	= Double.parseDouble (st.nextToken());
        double r		= Double.parseDouble (st.nextToken()); 	//orientation -  degrees
        pos 			= new Position(px,py,Math.toRadians(r));
        width 		= Double.parseDouble (st.nextToken()); 	//width
        
        label = new String (st.nextToken());	
    }
    
    public WMBeacon(String label, Position pos, double width){
        this.label = label;
        this.pos = pos;
        this.width = width;
    }
    
    public WMBeacon(TextDxf text) {
        Point3 p3 = text.getPos();
        label = text.getText();
        double rot = 0.0;
        width = 0.0;
        if(text.ExtendedDouble.size()>0) rot = Math.toRadians(text.getExtDouble(0));	
        if(text.ExtendedDouble.size()>1) width = Math.toRadians(text.getExtDouble(0));	
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
    
    public String toRawString ()
    {
        return DoubleFormat.format(pos.x()) + ", " + DoubleFormat.format(pos.y()) + ", " + DoubleFormat.format(Math.toDegrees(pos.alpha())) + ", " + DoubleFormat.format(width) + ", " + label;
    }
    
}
