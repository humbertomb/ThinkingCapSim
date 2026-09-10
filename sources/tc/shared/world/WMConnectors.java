/*
 * Created on 20-abr-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;
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
		
	// Constructors
	public WMConnectors (Properties props)
	{
		fromProperties (props);
	}
	
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
	
	public void fromProperties (Properties props)
	{
		int					i;
		String				prop;
	
		edges	= new WMConnector[Integer.parseInt (props.getProperty ("DOORS", "0"))];

		if ((prop = props.getProperty ("DOOR_DEF_WIDTH")) != null)
			defWidth	= Double.parseDouble (prop);
		if ((prop = props.getProperty ("DOOR_DEF_HEIGHT")) != null)	
			defHeight	= Double.parseDouble (prop);
		if ((prop = props.getProperty ("DOOR_DEF_TEXTURE")) != null)	
			defTexture	= prop;
				
		// Read in world door segments
		for (i=0; i < edges.length; i++)
		{
			prop		= props.getProperty ("DOOR_"+i);		
			edges[i]	= new WMConnector (prop, defWidth, defHeight, defTexture);
		}		
	}	
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("DOORS",Integer.toString (edges.length));
		
		for (i = 0; i < edges.length; i++)
			props.setProperty ("DOOR_"+i, edges[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		int				i;
		
		// Print Line Segments
		out.println("# ==============================");
		out.println("# DOORS");
		out.println("# ==============================");
		out.println ("DOORS = " + edges.length);	
		out.println("");
		out.println("DOOR_DEF_HEIGHT = "+defHeight);
		out.println("DOOR_DEF_WIDTH = "+defWidth);
		out.println("DOOR_DEF_TEXTURE = "+defTexture);
		out.println("");
		
		for (i = 0; i < edges.length; i++) 
			if(edges[i].texture.equals(defTexture) && edges[i].width == defWidth && edges[i].height == defHeight)
				out.println ("DOOR_" + i + " = " + edges[i].pointsRawString ());
			else
				out.println ("DOOR_" + i + " = " + edges[i].toRawString ());		
		out.println ();		
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
}
