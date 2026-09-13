/*
 * Created on 20-abr-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.entities.TextDxf;
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
public class WMConnectors
{
	protected WMConnector[]				edges;
	
	private double					defWidth		= 0.005;
	private double					defHeight	= 1.90;
	private String					defTexture	= "./conf/3dmodels/textures/wall.jpg";
		
	
	public WMConnectors (DXFWorldFile dxf)
	{
		ArrayList<Entity> entities = dxf.getEntities();
		ArrayList<LineDxf> doors = new ArrayList<LineDxf>();
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("DOORS")){
				if(entity instanceof LineDxf) 
					doors.add((LineDxf)entity);  
			}
			if(entity instanceof TextDxf){
				try{
					String texto = ((TextDxf)entity).getText();
					if(texto.startsWith("DOOR_DEF_WIDTH")){
						defWidth = Double.parseDouble(texto.substring(texto.lastIndexOf("=")+1));
					}
					else if(texto.startsWith("DOOR_DEF_HEIGHT")){
						defHeight = Double.parseDouble(texto.substring(texto.lastIndexOf("=")+1));
					}
					else if(texto.startsWith("DOOR_DEF_TEXTURE")){
						defTexture =texto.substring(texto.lastIndexOf("=")+1).trim();
					}
				}catch(Exception e){}
			}
		}
		edges	= new WMConnector[doors.size()];
		for(int i = 0; i<doors.size(); i++){
			edges[i] = new WMConnector(doors.get(i),defWidth, defHeight, defTexture); 
		}
	}
	
	// Accessors
	public final int	 		n () 				{ return edges.length; }
	public final WMConnector[]		edges ()				{ return edges; }

	public final String	 	defaultTexture () 	{ return defTexture; }

	// Instance methods
	public WMConnector at (int i)
	{
		if ((i < 0) || (i >= edges.length)) return null;
		return edges[i];
	}
	
	public WMConnector at (String label)
	{
		return at(index(label));
	} 

	public Point2 at (int i, WMZones zones, String zone)
	{
		Line2		l;
		
		if ((i < 0) || (i >= edges.length)) return null;
		
		l		= edges[i].path;

		if (zone.equals (zones.inZone(l.orig().x(), l.orig().y()))) return l.orig();
		else if (zone.equals (zones.inZone(l.dest().x(), l.dest().y()))) return l.dest();
		else return null;		
	}

	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (edges == null))			return -1;

		for (i = 0; i < edges.length; i++)
			if (label.equals (edges[i].label))			return i;

		return -1;
	}
	
	public void toDxfFile (DXFWorldFile dxf){
			
	   // Define una capa con un color determinado (opcional)
	   dxf.addLayer(new Layer("DOORS",ACADColor.LIGHT_GRAY));
	    
		for(int i = 0; i<edges.length; i++){
			edges[i].toDxf(dxf);
		}
	}

	/* Edition methods (world editor) */
	public void add (WMConnector e)
	{
		WMConnector[]	tmp = new WMConnector[edges.length + 1];
		System.arraycopy (edges, 0, tmp, 0, edges.length);
		tmp[edges.length] = e;
		edges = tmp;
	}

	public WMConnector remove (int i)
	{
		if ((i < 0) || (i >= edges.length))		return null;
		WMConnector		old = edges[i];
		WMConnector[]	tmp = new WMConnector[edges.length - 1];
		System.arraycopy (edges, 0, tmp, 0, i);
		System.arraycopy (edges, i + 1, tmp, i, edges.length - i - 1);
		edges = tmp;
		return old;
	}

	public int indexOf (WMConnector e)
	{
		for (int i = 0; i < edges.length; i++)
			if (edges[i] == e)				return i;
		return -1;
	}

	public final double	 	defaultHeight () 	{ return defHeight; }
	public final double	 	defaultWidth () 	{ return defWidth; }

	public void setDefaults (double width, double height, String texture)
	{
		defWidth	= width;
		defHeight	= height;
		defTexture	= texture;
	}

	/* JSON: {defaults: {width, height, texture}, items: [...]} */

	public WMConnectors ()						{ edges = new WMConnector[0]; }
	public WMConnectors (JsonElement e)			{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonObject	o = ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
		JsonObject	def = WorldJson.getObject (o, "defaults");
		JsonArray	arr = WorldJson.getArray (o, "items");
		defWidth	= WorldJson.getDouble (def, "width", defWidth);
		defHeight	= WorldJson.getDouble (def, "height", defHeight);
		defTexture	= WorldJson.getString (def, "texture", defTexture);
		edges	= new WMConnector[arr.size ()];
		for (int i = 0; i < edges.length; i++)		edges[i] = new WMConnector (arr.get (i).getAsJsonObject (), defWidth, defHeight, defTexture);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonObject	def = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		def.addProperty ("width", WorldJson.num (defWidth));
		def.addProperty ("height", WorldJson.num (defHeight));
		def.addProperty ("texture", defTexture);
		for (WMConnector c : edges)		arr.add (c.toJson (defWidth, defHeight, defTexture));
		o.add ("defaults", def);
		o.add ("items", arr);
		return o;
	}
}
