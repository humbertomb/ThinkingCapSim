/*
 * Created on 10-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

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
			
	// Constructors
	public WMObjects (Properties props)
	{
		fromProperties (props);
	}
	
	public WMObjects (DXFWorldFile dxf){
			Vector entities = dxf.getEntities();
			Vector object = new Vector();
			Entity entity;
			for(int i = 0; i<entities.size(); i++){
				entity = (Entity)entities.get(i);
				if(entity.getLayer().equalsIgnoreCase("OBJECTS")){
					if(entity instanceof InsertDxf) 
						object.add(new WMObject((InsertDxf)entity, dxf.getBlocks(((InsertDxf)entity).getBlockname())));
				}
			}
			objects	= new WMObject[object.size()];
			for(int i = 0; i<object.size(); i++){
				objects[i] = (WMObject) object.get(i);
			}
	}
	
	// Accessors
	public final int	 		n () 				{ return objects.length; }
	public final WMObject[]	object ()			{ return objects; }
	
	// Instance methods
	public WMObject at (int i)
	{
		if ((i < 0) || (i >= objects.length)) 
		    return null;
		return objects[i];
	}
	
	public void fromProperties (Properties props)
	{
		String				prop;
		
		objects	= new WMObject[Integer.parseInt (props.getProperty ("MAX_OBJECTS", "0"))];
				
		// Read in objects
		for (int i = 0; i < objects.length; i++)
		{
			prop = props.getProperty ("OBJECT_"+i);		
			objects[i] = new WMObject (prop);
		}
	}
	
	
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("MAX_OBJECTS",Integer.toString (objects.length));
		
		for (i = 0; i < objects.length; i++)
			props.setProperty ("OBJECT_"+i, objects[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		int				i;
		
		// Print Line Segments
		out.println("# ==============================");
		out.println("# EXTERNAL OBJECTS");
		out.println("# ==============================");
		out.println ("MAX_OBJECTS = " + objects.length);	
		out.println("");
		
		for (i = 0; i < objects.length; i++) 
			out.println ("OBJECT_" + i + " = " + objects[i].toRawString ());

		out.println ();		
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
				for( j=0; j<objects[i].icon.length; j++)
				{
					pt = objects[i].icon[j].intersection (x1, y1, x2, y2);
					if (pt != null)
					{
						d1 = pt.distance (x1, y1);
						if (d1 < d)
						{
							d 	= d1;
							cln	= objects[i].icon[j];
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
				for( j=0; j<objects[i].icon.length; j++)
				{
					len = objects[i].icon[j].distance (x1, y1);
					if (len < d)
					{
						tmp = objects[i].icon[j];
						d = len;
					}
				}

		return tmp;
	}
	
	public Line2[] getLines(){
		LinkedList lines;
		lines = new LinkedList();
		for(int i=0; i<objects.length; i++)
			if(objects[i].visible)
				for(int j=0; j<objects[i].icon.length; j++)
					lines.add(objects[i].icon[j]);
		return ((Line2[]) lines.toArray(new Line2[0]));
	}
	

	
}
