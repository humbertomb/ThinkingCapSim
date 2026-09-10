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
/** A connector between two zones (a door, a gate, an opening...): the physical opening and the path to cross it. */
/**
 * A connector between two zones (a door, a gate, an opening...): the physical
 * opening (edge) and the segment to cross it (path). Kept as DOOR_i in the files.
 */
public class WMConnector extends WMElement
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
    public WMConnector (String prop, double dwidth, double dheight, String dtexture)
    {
        StringTokenizer		st;
        double				x1, x2, y1, y2, z1, z2;
        double				px1, px2, py1, py2, pz1, pz2;
        
        st		= new StringTokenizer (prop,", \t");
        x1		= Double.parseDouble (st.nextToken());
        y1		= Double.parseDouble (st.nextToken());
        z1		= Double.parseDouble (st.nextToken());
        x2		= Double.parseDouble (st.nextToken());
        y2		= Double.parseDouble (st.nextToken());
        z2		= Double.parseDouble (st.nextToken());
        label	= st.nextToken(); 
        edge		= new Line2 (x1, y1, z1, x2, y2, z2);
        
        px1		= x1;	py1		= y1;	pz1		= z1;
        px2		= x2;	py2		= y2;	pz2		= z2;
        
        if (st.hasMoreTokens())
        {
            px1		= Double.parseDouble (st.nextToken());
            py1		= Double.parseDouble (st.nextToken());
            pz1		= Double.parseDouble (st.nextToken());
            px2		= Double.parseDouble (st.nextToken());
            py2		= Double.parseDouble (st.nextToken());
            pz2		= Double.parseDouble (st.nextToken());
        }
        path		= new Line2 (px1, py1, pz1, px2, py2, pz2);
        
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
    
    public WMConnector(){
    }
    
    public WMConnector(LineDxf line,double dwidth, double dheight, String dtexture) {
        texture = dtexture;
        width = dwidth;
        height = dheight;
        
        path = new Line2(line.getStart().x(),line.getStart().y(),line.getStart().z(),line.getEnd().x(),line.getEnd().y(),line.getEnd().z());
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
        return pointsRawString ()+", "+DoubleFormat.format(width)+", "+DoubleFormat.format(height)+", "+texture;
    }

    /** "x1, y1, z1, x2, y2, z2, label, px1, py1, pz1, px2, py2, pz2" */
    public String pointsRawString ()
    {
        return line3 (edge)+", "+label+", "+line3 (path);
    }

    static String line3 (Line2 l)
    {
        return DoubleFormat.format(l.orig().x())+", "+DoubleFormat.format(l.orig().y())+", "+DoubleFormat.format(l.z1())
             +", "+DoubleFormat.format(l.dest().x())+", "+DoubleFormat.format(l.dest().y())+", "+DoubleFormat.format(l.z2());
    }
    
}
