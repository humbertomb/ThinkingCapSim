/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.VertexDxf;
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
    public String					texture;
    
    // Accessors
    public double		minx ()			{ return area.getX (); }
    public double		miny ()			{ return area.getY (); }
    public double		width ()			{ return area.getWidth (); }
    public double		height ()		{ return area.getHeight (); }
    
    // Constructors
    public WMZone (String prop, String dtexture)
    {
        StringTokenizer		st;
        double				x1, y1;
        double				w, h;
        
        st		= new StringTokenizer (prop,", \t");
        x1		= Double.parseDouble (st.nextToken());
        y1		= Double.parseDouble (st.nextToken());
        w		= Double.parseDouble (st.nextToken());
        h		= Double.parseDouble (st.nextToken());
        area 	= new Rectangle2D.Double (x1, y1, w, h);
        label	= st.nextToken(); 
        
        texture	= dtexture;
        if (st.hasMoreTokens())
            texture	= st.nextToken();
    }
    
    public WMZone(){
    }
    
    public WMZone(PolylineDxf polyline, String defTexture) {
        texture = defTexture;
        double x = polyline.minlimit.x();
        double y = polyline.minlimit.y();
        double w = polyline.maxlimit.x()-polyline.minlimit.x();
        double h = polyline.maxlimit.y()-polyline.minlimit.y();
        
        area = new Rectangle2D.Double(x,y,w,h);
        if(polyline.ExtendedText.size()>0)
            label = polyline.getExtText(0);	
        else
            label = "ZONE_?";
        if(polyline.ExtendedText.size()>1)
            texture = polyline.getExtText(1);	
    }
    
    public Line2[] toLines(){
    	Line2[] lines = new Line2[4];
    	lines[0] = new Line2(area.getMinX(),area.getMinY(),area.getMinX(),area.getMaxY());
    	lines[1] = new Line2(area.getMinX(),area.getMinY(),area.getMaxX(),area.getMinY());
    	lines[2] = new Line2(area.getMaxX(),area.getMaxY(),area.getMinX(),area.getMaxY());
    	lines[3] = new Line2(area.getMaxX(),area.getMaxY(),area.getMaxX(),area.getMinY());
    	return lines;
    }
    
    public void toDxf(DXFWorldFile dxf) {
        PolylineDxf pol = new PolylineDxf();
        
        // Se define los vertices del cuadrado
        pol.addVertex(new VertexDxf(new Point3(minx(),miny(),0.0)));		
        pol.addVertex(new VertexDxf(new Point3(minx()+width(),miny(),0.0)));		
        pol.addVertex(new VertexDxf(new Point3(minx()+width(),miny()+height(),0.0)));		
        pol.addVertex(new VertexDxf(new Point3(minx(),miny()+height(),0.0)));		
        pol.addExtText(0,label);
        pol.addExtText(1,texture);
        pol.setLayer("ZONES");
        dxf.addEntity(pol);   
    }
    
    // Instance methods
    public String toRawString ()
    {
        return DoubleFormat.format(area.getX())+", "+DoubleFormat.format(area.getY())+", "+DoubleFormat.format(area.getWidth())+", "+DoubleFormat.format(area.getHeight())+", "+label+", "+texture;
    }
}
