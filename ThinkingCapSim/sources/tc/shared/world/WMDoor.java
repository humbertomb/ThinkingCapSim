/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.util.StringTokenizer;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMDoor extends WMElement
{
    // 2D components
    public Line2					edge;		// Physical location of the door
    public Line2					path;		// Points for crossing the door
    
    // 2 1/2 D components
    public double				width;
    public double				height;
    
    // Visualization components
    public String				texture;					
    
    // Constructors
    public WMDoor (String prop, double dwidth, double dheight, String dtexture)
    {
        StringTokenizer		st;
        double				x1, x2, y1, y2;
        double				px1, px2, py1, py2;
        
        st		= new StringTokenizer (prop,", \t");
        x1		= Double.parseDouble (st.nextToken());
        y1		= Double.parseDouble (st.nextToken());
        x2		= Double.parseDouble (st.nextToken());
        y2		= Double.parseDouble (st.nextToken());
        label	= st.nextToken(); 
        edge		= new Line2 (x1, y1, x2, y2);
        
        px1		= x1;
        py1		= y1;
        px2		= x2;
        py2		= y2;
        
        if (st.hasMoreTokens())
        {
            px1		= Double.parseDouble (st.nextToken());
            py1		= Double.parseDouble (st.nextToken());
            px2		= Double.parseDouble (st.nextToken());
            py2		= Double.parseDouble (st.nextToken());
        }
        path		= new Line2 (px1, py1, px2, py2);
        
        height	= dheight;
        width	= dwidth;
        texture	= dtexture;
        
        if (st.hasMoreTokens())
            width	= Double.parseDouble (st.nextToken());
        
        if (st.hasMoreTokens())
            height	= Double.parseDouble (st.nextToken());
        
        if (st.hasMoreTokens())
            texture	= st.nextToken();
    }
    
    public WMDoor(){
    }
    
    public WMDoor(LineDxf line,double dwidth, double dheight, String dtexture) {
        texture = dtexture;
        width = dwidth;
        height = dheight;
        
        path = new Line2(line.getStart().x(),line.getStart().y(),line.getEnd().x(),line.getEnd().y());
        if(line.ExtendedText.size()>0) label = line.getExtText(0);
        else									label = "DOOR_?";
        if(line.ExtendedText.size()>1){
            StringTokenizer tk = new StringTokenizer(line.getExtText(1), ", ");
            if(tk.countTokens()>=4)
                edge = new Line2(Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()),Double.parseDouble(tk.nextToken()));
            else
                edge = new Line2();
        }
        if(line.ExtendedText.size()>2) texture = line.getExtText(2);
        if(line.ExtendedDouble.size()>0) height = line.getExtDouble(0);
        if(line.ExtendedDouble.size()>1) width = line.getExtDouble(1);
    }
    
    public void toDxf(DXFWorldFile dxf) {
        LineDxf line = new LineDxf(new Point3(path.orig()),new Point3(path.dest()),"DOORS");
        line.addExtText(0,label);
        line.addExtText(1,edge.toRawString());
        line.addExtText(2,texture);
        line.addExtDouble(0,height);
        line.addExtDouble(1,width);
        dxf.addEntity(line);
    }
    
    // Instance methods
    public String toRawString ()
    {
        return DoubleFormat.format(edge.orig().x())+", "+DoubleFormat.format(edge.orig().y())+", "+DoubleFormat.format(edge.dest().x())+", "+DoubleFormat.format(edge.dest().y())+", "+label+", "+DoubleFormat.format(path.orig().x())+", "+DoubleFormat.format(path.orig().y())+", "+DoubleFormat.format(path.dest().x())+", "+DoubleFormat.format(path.dest().y())+", "+DoubleFormat.format(width)+", "+DoubleFormat.format(height)+", "+texture;
    }
    
}
