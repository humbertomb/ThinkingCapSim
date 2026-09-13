/*
 * Created on 10-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedList;
import java.util.ArrayList;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.InsertDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMObjects extends Object
{
	protected WMObject[]				objects;
			
	protected WMIcons					icons;			// Icon library the objects refer to
			

	
	public WMObjects (DXFWorldFile dxf, WMIcons icons){
			this.icons = (icons != null) ? icons : new WMIcons ();
			ArrayList<Entity> entities = dxf.getEntities();
			ArrayList<WMObject> object = new ArrayList<WMObject>();
			Entity entity;
			for(int i = 0; i<entities.size(); i++){
				entity = entities.get(i);
				if(entity.getLayer().equalsIgnoreCase("OBJECTS")){
					if(entity instanceof InsertDxf) 
						object.add(new WMObject((InsertDxf)entity, dxf.getBlocks(((InsertDxf)entity).getBlockname()), this.icons));
				}
			}
			objects	= new WMObject[object.size()];
			for(int i = 0; i<object.size(); i++){
				objects[i] = object.get(i);
			}
	}
	
	// Accessors
	public final int	 		n () 				{ return objects.length; }
	public final WMObject[]	object ()			{ return objects; }
	public final WMIcons		icons ()			{ return icons; }
	
	// Instance methods
	public WMObject at (int i)
	{
		if ((i < 0) || (i >= objects.length)) 
		    return null;
		return objects[i];
	}
	
	public void toDxfFile (DXFWorldFile dxf){
		//	  Define una capa con un color determinado (opcional)
		dxf.addLayer(new Layer("OBJECTS",ACADColor.GREEN));
		
		for (int i = 0; i < objects.length; i++){
		    objects[i].toDxf(dxf);
		}
	}
	
	// calcula la primera linea del obstaculo que intersecta
	public Line2 crossline (double x1, double y1, double x2, double y2)
	{
		int				i,j;
		Point2			pt;
		Line2			cln;
		double			d1;
		double			d;
		
		d 	= Double.MAX_VALUE;
		cln	= null;
		
		for (i = 0; i < objects.length; i++)
			if(objects[i].visible)
				for( j=0; j<objects[i].absIcon ().length; j++)
				{
					pt = objects[i].absIcon ()[j].intersection (x1, y1, x2, y2);
					if (pt != null)
					{
						d1 = pt.distance (x1, y1);
						if (d1 < d)
						{
							d 	= d1;
							cln	= objects[i].absIcon ()[j];
						}
					}
				}
		return cln;
	}	

	public Line2 crossline (Line2 line)
	{		
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
	}	
	
	// Calcula la linea del obstaculo más cercana al punto
	public Line2 closer (double x1, double y1)
	{
		int				i,j;
		double			d, len;
		Line2			tmp;
		
		tmp = null;
		d = Double.MAX_VALUE;
		for (i = 0; i < objects.length; i++)
			if(objects[i].visible)
				for( j=0; j<objects[i].absIcon ().length; j++)
				{
					len = objects[i].absIcon ()[j].distance (x1, y1);
					if (len < d)
					{
						tmp = objects[i].absIcon ()[j];
						d = len;
					}
				}

		return tmp;
	}
	
	public Line2[] getLines(){
		LinkedList<Line2> lines;
		lines = new LinkedList<Line2>();
		for(int i=0; i<objects.length; i++)
			if(objects[i].visible)
				for(int j=0; j<objects[i].absIcon ().length; j++)
					lines.add(objects[i].absIcon ()[j]);
		return (lines.toArray(new Line2[0]));
	}

	/* Edition methods (world editor) */
	public void add (WMObject e)
	{
		WMObject[]	tmp = new WMObject[objects.length + 1];
		System.arraycopy (objects, 0, tmp, 0, objects.length);
		tmp[objects.length] = e;
		objects = tmp;
	}

	public WMObject remove (int i)
	{
		if ((i < 0) || (i >= objects.length))		return null;
		WMObject		old = objects[i];
		WMObject[]	tmp = new WMObject[objects.length - 1];
		System.arraycopy (objects, 0, tmp, 0, i);
		System.arraycopy (objects, i + 1, tmp, i, objects.length - i - 1);
		objects = tmp;
		return old;
	}

	public int indexOf (WMObject e)
	{
		for (int i = 0; i < objects.length; i++)
			if (objects[i] == e)				return i;
		return -1;
	}

	/* JSON: [{icon, x, y, z, orientation, color, ...}, ...] (icons resolved against the world's icon library) */

	public WMObjects (WMIcons icons)					{ this.icons = (icons != null) ? icons : new WMIcons (); objects = new WMObject[0]; }
	public WMObjects (JsonElement e, WMIcons icons)		{ this.icons = (icons != null) ? icons : new WMIcons (); fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		objects	= new WMObject[arr.size ()];
		for (int i = 0; i < objects.length; i++)		objects[i] = new WMObject (arr.get (i).getAsJsonObject (), icons);
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMObject ob : objects)		arr.add (ob.toJson ());
		return arr;
	}
}
