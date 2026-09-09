/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.Vector;

import devices.pos.Position;
import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.TextDxf;
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
public class WMZones
{
	protected WMZone[]				areas;
	
	private String					defTexture		= "./conf/3dmodels/textures/floor.jpg";
	
	// Constructors
	public WMZones (Properties props)
	{
		fromProperties (props);
	}
	
	public WMZones (DXFWorldFile dxf){
		Vector entities = dxf.getEntities();
		Vector zones = new Vector();
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("ZONES")){
				if(entity instanceof PolylineDxf) 
					zones.add(entity);  
			}
			if(entity instanceof TextDxf){
				try{
					String texto = ((TextDxf)entity).getText();
					if(texto.startsWith("ZONE_DEF_TEXTURE")){
						defTexture =texto.substring(texto.lastIndexOf("=")+1).trim();
					}
				}catch(Exception e){}
			}
		}
		areas	= new WMZone[zones.size()];
		for(int i = 0; i<zones.size(); i++){
			areas[i] = new WMZone((PolylineDxf)zones.get(i),defTexture);
		}
	}
	
	// Accessors
	public final int	 		n () 				{ return areas.length; }
	public final WMZone[]		areas ()				{ return areas; }

	public final String	 	defaultTexture () 	{ return defTexture; }

	// Instance methods
	public WMZone at (int i)
	{
		if ((i < 0) || (i >= areas.length)) 
		    return null;
		return areas[i];
	} 

	public WMZone at (String label)
	{
		int			i;
		
		i	= index (label);		
		if ((i < 0) || (i >= areas.length)) return null;
		return areas[i];
	} 

	public String inZone (Point2 pt)
	{
		return inZone (pt.x (), pt.y ());
	}
	
	public String inZone (Position pt)
	{
		return inZone (pt.x (), pt.y ());
	}
	
	// Devuelve el nombre de la zona de pertenece esa posicion
	public String inZone (double x, double y)
	{
		int			i;
		
		for (i=0; i < areas.length; i++)
			if (areas[i].area.contains (x, y)) 
				return areas[i].label;
			
		return "Unknown";		
	}

	public int index (String label)
	{
		int  i;
		
		if ((label == null) || (areas == null))			return -1;

		for (i = 0; i < areas.length; i++)
			if (label.equals (areas[i].label))			return i;

		return -1;
	}
	
	public void fromProperties (Properties props)
	{
		int					i;
		String				prop;
		
		areas	= new WMZone[Integer.parseInt (props.getProperty ("ZONES", "0"))];

		if ((prop = props.getProperty ("ZONE_DEF_TEXTURE")) != null)	
			defTexture	= prop;
				
		for (i = 0; i < areas.length; i++)
		{
			prop		= props.getProperty ("ZONE_"+i);	
			areas[i]	= new WMZone (prop, defTexture);
		}		
	}	
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("ZONES",Integer.toString (areas.length));
		
		for (i = 0; i < areas.length; i++)
			props.setProperty ("ZONE_"+i, areas[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		int				i;
		
		// Print Line Segments
		out.println("# ==============================");
		out.println("# ZONES");
		out.println("# ==============================");
		out.println ("ZONES = " + areas.length);	
		out.println ();
		out.println ("ZONE_DEF_TEXTURE = "+defTexture);
		out.println ();
		
		for (i = 0; i < areas.length; i++) 
			if(areas[i].texture.equals(defTexture))
				out.println ("ZONE_" + i + " = " + areas[i].area.getX()+", "+areas[i].area.getY()+", "+areas[i].area.getWidth()+", "+areas[i].area.getHeight()+", "+areas[i].label);
			else
				out.println ("ZONE_" + i + " = " + areas[i].toRawString ());
		out.println ();		
	}
	
	public void toDxfFile (DXFWorldFile dxf){
		
	   //	  Define una capa con un color determinado (opcional)
	   dxf.addLayer(new Layer("ZONES",ACADColor.YELLOW));	
	   
		for (int i = 0; i < areas.length; i++){
			areas[i].toDxf(dxf);
		}

	
		dxf.addEntity(new TextDxf(
				"ZONE_DEF_TEXTURE = "+defTexture,
				new Point3(dxf.posx,dxf.posy,0.0),
				0.2,
				"ZONES"
			)
		);
		dxf.posy-= 0.5;
	

	}
	
}
