/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;

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

	
	public WMPath (DXFWorldFile dxf){
	    ArrayList<Entity> entities = dxf.getEntities();
	    Entity entity;
	    for(int i = 0; i<entities.size(); i++){
	        entity = entities.get(i);
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

	/** Elevation of a path point (0 if it carries none). */
	static public double z (Point2 p)			{ return (p instanceof Point3) ? ((Point3) p).z () : 0.0; }

	
	// Instance methods
	public Point2 at (int i)
	{
		if ((i < 0) || (i >= points.length)) return null;
		return points[i];
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

	/* Edition methods (world editor) */
	public final Point2[]		points ()			{ return points; }

	public void add (Point2 p)
	{
		insert (points.length, p);
	}

	public void insert (int i, Point2 p)
	{
		if (i < 0)						i = 0;
		if (i > points.length)			i = points.length;
		Point2[]	tmp = new Point2[points.length + 1];
		System.arraycopy (points, 0, tmp, 0, i);
		tmp[i] = p;
		System.arraycopy (points, i, tmp, i + 1, points.length - i);
		points = tmp;
	}

	public Point2 remove (int i)
	{
		if ((i < 0) || (i >= points.length))		return null;
		Point2		old = points[i];
		Point2[]	tmp = new Point2[points.length - 1];
		System.arraycopy (points, 0, tmp, 0, i);
		System.arraycopy (points, i + 1, tmp, i, points.length - i - 1);
		points = tmp;
		return old;
	}

	/* JSON: [{x, y, z}, ...] */

	public WMPath ()							{ points = new Point2[0]; }
	public WMPath (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		points	= new Point2[arr.size ()];
		for (int i = 0; i < points.length; i++)		points[i] = WorldJson.toPoint (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (Point2 p : points)		arr.add (WorldJson.point (p));
		return arr;
	}
}
