/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.VertexDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMPath
{
	
 	// Proposed robot's path points
 	protected Point2[]			points;

	// Constructors
	public WMPath (Properties props)
	{
		fromProperties (props);
	}
	
	public WMPath (DXFWorldFile dxf){
	    Vector entities = dxf.getEntities();
	    Entity entity;
	    for(int i = 0; i<entities.size(); i++){
	        entity = (Entity)entities.get(i);
	        if(entity.getLayer().equalsIgnoreCase("PATH")){
	            if(entity instanceof PolylineDxf){ 
	                points = new Point2[((PolylineDxf)entity).vertexs.size()];
	                for(int j = 0; j<points.length; j++){
	                    points[j] = new Point2(((PolylineDxf)entity).getPoint(j).x(), ((PolylineDxf)entity).getPoint(j).y());
	                } 
	                return;	// solo coge la primera polilinea
	            }
	        }
	    }
	    points = new Point2[0];
	}
	
    // Accessors
	public final int	 		n () 				{ return points.length; }
	
	// Instance methods
	public Point2 at (int i)
	{
		if ((i < 0) || (i >= points.length)) return null;
		return points[i];
	}
	
	public void fromProperties (Properties props)
	{
		String				prop;
		double				x1,y1;
		StringTokenizer 	st;
		points = new Point2[Integer.parseInt (props.getProperty ("PATH_POINTS","0"))];
		for (int i=0; i < points.length; i++){
			prop		= props.getProperty ("PATHPOINT_"+i);		
			if (prop != null)
			{
				st = new StringTokenizer (prop,", \t");
				x1 = Double.parseDouble (st.nextToken());
				y1 = Double.parseDouble (st.nextToken());
				points[i] = new Point2(x1, y1);	 
			}
		}
	}
	
	public void toProperties (Properties props)
	{
		props.setProperty ("PATH_POINTS",Integer.toString (points.length));
		
		for (int i = 0; i < points.length; i++)
			props.setProperty ("PATHPOINT_"+i, points[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
			out.println("# ==============================");
			out.println("# PATH POINTS");
			out.println("# ==============================");
			out.println ("PATH_POINTS = " + n());		
			out.println();	
			for (int i = 0; i < n(); i++) 
				out.println("PATHPOINT_" + i + " = " + points[i].x () + ", " +  points[i].y ());
			out.println();
	}
	
	public void toDxfFile (DXFWorldFile dxf){
		//	  Define una capa con un color determinado (opcional)
		dxf.addLayer(new Layer("PATH",ACADColor.CYAN));	
	   if(points == null || points.length == 0) return;
	   PolylineDxf pol = new PolylineDxf();
	   pol.setLayer("PATH");
		for(int i = 0; i < points.length; i++){
		    pol.addVertex(new VertexDxf(new Point3(points[i])));		
		}
		dxf.addEntity(pol);
	}
	
}
