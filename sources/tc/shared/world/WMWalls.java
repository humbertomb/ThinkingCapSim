/*
 * Created on 08-feb-2004
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
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMWalls extends Object
{
	protected WMWall[]				edges;

	// Size of the map
	protected double					minx;
	protected double					miny;
	protected double					maxx;
	protected double					maxy;
	
	private double					defWidth		= 0.01;
	private double					defHeight	= 0.75;
	private String					defTexture	= "./conf/3dmodels/textures/wall.jpg";
		
	// Constructors
	public WMWalls (int n){
		edges = new WMWall[n];
	}
	
	public WMWalls (DXFWorldFile dxf)
	{
		ArrayList<Entity> entities = dxf.getEntities();
		ArrayList<LineDxf> walls = new ArrayList<LineDxf>();
		Entity entity;
		
		// Se guardan las lineas de la Capa 0 (lineas de Wall) en un vector y se leen las propiedades por defecto
		for(int i = 0; i<entities.size(); i++){
			entity = entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("0")){
				if(entity instanceof LineDxf){
				  walls.add((LineDxf)entity);
				}
				else if(entity instanceof PolylineDxf){
				    LineDxf[] poly = ((PolylineDxf)entity).toDxfLines();
				    for(int j = 0; j<poly.length; j++) walls.add(poly[j]);
				}
			}
			if(entity instanceof TextDxf){
				try{
					String texto = ((TextDxf)entity).getText();
					if(texto.startsWith("LINE_DEF_WIDTH")){
						defWidth = Double.parseDouble(texto.substring(texto.lastIndexOf("=")+1));
					}
					else if(texto.startsWith("LINE_DEF_HEIGHT")){
						defHeight = Double.parseDouble(texto.substring(texto.lastIndexOf("=")+1));
					}
					else if(texto.startsWith("LINE_DEF_TEXTURE")){
						defTexture =texto.substring(texto.lastIndexOf("=")+1).trim();
					}
				}catch(Exception e){}
			}
		}
		
		// Se generan las lineas Wall (WMWall)
		edges	= new WMWall[walls.size()];
		for(int i = 0; i<walls.size(); i++){
			edges[i] = new WMWall(walls.get(i),defWidth, defHeight, defTexture);
			update(edges[i].edge);
		}
	}
	
	// Accessors
	public final int	 		n () 				{ return edges.length; }
	public final WMWall[]		edges ()				{ return edges; }

	public final String	 	defaultTexture () 	{ return defTexture; }
	public final double	 	defaultHeight () 	{ return defHeight; }
	public final double	 	defaultWidth () 		{ return defWidth; }
	
	public final double	 	maxx ()		 		{ return maxx; }
	public final double	 	maxy ()				{ return maxy; }
	public final double	 	minx () 				{ return minx; }
	public final double	 	miny () 				{ return miny; }	
	
	// Instance methods
	public WMWall at (int i)
	{
		if ((i < 0) || (i >= edges.length)) 
		    return null;
		return edges[i];
	}
	
	protected void update (Line2 edge)
	{
		if (edge.orig().x() < minx) 	minx = edge.orig().x();
		if (edge.orig().x() > maxx) 	maxx = edge.orig().x();
		if (edge.dest().x() < minx) 	minx = edge.dest().x();
		if (edge.dest().x() > maxx) 	maxx = edge.dest().x();
		
		if (edge.orig().y() < miny) 	miny = edge.orig().y();
		if (edge.orig().y() > maxy) 	maxy = edge.orig().y();
		if (edge.dest().y() < miny) 	miny = edge.dest().y();
		if (edge.dest().y() > maxy) 	maxy = edge.dest().y();
	}
	
	public double intersection (double x1, double y1, double x2, double y2)
	{
		int				i;
		Point2			pt;
		double			d1;
		double			d;
		
		d = Double.MAX_VALUE;
		for (i = 0; i < edges.length; i++)
		{
			pt = edges[i].edge.intersection (x1, y1, x2, y2);
			if (pt != null)
			{
				d1 = pt.distance (x1, y1);
				if (d1 < d)
					d = d1;
			}
		}
		return d;
	}
	
	
	public void toDxfFile (DXFWorldFile dxf){
	    TextDxf text;
	    
	    for (int i = 0; i < edges.length; i++)
	        edges[i].toDxf(dxf);
   
	    text = new TextDxf(
	            "LINE_DEF_HEIGHT = "+defHeight,
	            new Point3(dxf.posx,dxf.posy,0.0),
	            0.2,
	            "0"
	    );
	    dxf.addEntity(text);
	    dxf.posy-= 0.5;
	    
	    text = new TextDxf(
	            "LINE_DEF_WIDTH = "+defWidth,
	            new Point3(dxf.posx,dxf.posy,0.0),
	            0.2,
	            "0"
	    );
	    dxf.addEntity(text);
	    dxf.posy-= 0.5;
	    
	    text = new TextDxf(
	            "LINE_DEF_TEXTURE = "+defTexture,
	            new Point3(dxf.posx,dxf.posy,0.0),
	            0.2,
	            "0"
	    );
	    dxf.addEntity(text);
	    dxf.posy-= 0.5;
	    
	}

	public Line2 crossline (double x1, double y1, double x2, double y2)
	{
		int				i;
		Point2			pt;
		Line2			cln;
		double			d1;
		double			d;
		
		d 	= Double.MAX_VALUE;
		cln	= null;
		for (i = 0; i < edges.length; i++)
		{
			pt = edges[i].edge.intersection (x1, y1, x2, y2);
			if (pt != null)
			{
				d1 = pt.distance (x1, y1);
				if (d1 < d)
				{
					d 	= d1;
					cln	= edges[i].edge;
				}
			}
		}
		return cln;
	}	

	public Line2 crossline (Line2 line)
	{		
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
	}	

	public Line2 crossline (double x1, double y1, double x2, double y2, Line2[][] virtuals, int nvirtual, int skip)
	{
		int				i,j;
		Point2			pt;
		Line2			cln;
		double			d1;
		double			d;
		
		//System.out.println ("\tSimulated crossline. "+nvirtual+" robots. Skipping "+skip);
		// Obtain cross line with real edges
		d 	= Double.MAX_VALUE;
		cln	= null;
		for (i = 0; i < edges.length; i++)
		{
			pt = edges[i].edge.intersection (x1, y1, x2, y2);
			if (pt != null)
			{
				d1 = pt.distance (x1, y1);
				if (d1 < d)
				{
					d 	= d1;
					cln	= edges[i].edge;
				}
			}
		}
		
		// Obtain cross line with virtual lines
		for (i=0; i < nvirtual; i++)	
			if (i != skip)
				for (j=0; j < virtuals[i].length; j++)
				{
					pt = virtuals[i][j].intersection (x1, y1, x2, y2);
					if (pt != null)
					{
						d1 = pt.distance (x1, y1);
						if (d1 < d)
						{
							d 	= d1;
							cln	= virtuals[i][j];
						}
					}
				}	
		
		return cln;		
	}
	
	public Line2 crossline (Line2 line, Line2[][] virtuals, int nvirtual, int skip)
	{
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y (), virtuals, nvirtual, skip);
	}
	
	public Line2 closer (double x1, double y1)
	{
		int				i;
		double			d, len;
		Line2			tmp;
		
		tmp = edges[0].edge;
		d = edges[0].edge.distance (x1, y1);
		for (i = 1; i < edges.length; i++)
		{
			len = edges[i].edge.distance (x1, y1);
			if (len < d)
			{
				tmp = edges[i].edge;
				d = len;
			}
		}

		return tmp;
	}
	
	public Line2 closer (double x1, double y1, Line2[][] virtuals, int nvirtual, int skip)
	{
		int				i,j;
		double			d, len;
		Line2			tmp;
		
		if ((edges == null) || (edges.length == 0))			return null;
		
		tmp = edges[0].edge;
		d = edges[0].edge.distance (x1, y1);
		for (i = 1; i < edges.length; i++)
		{
			len = edges[i].edge.distance (x1, y1);
			if (len < d)
			{
				tmp = edges[i].edge;
				d = len;
			}
		}	
		
		// Find closer line between virtual lines
		for (i=0; i < nvirtual; i++)	
		{
			if (i != skip)
			{
				for (j=0; j < virtuals[i].length; j++)
				{
					len = virtuals[i][j].distance (x1, y1);
					if (len < d)
					{
						tmp = virtuals[i][j];
						d = len;
					}
				}
			}
		}
		
		return tmp;
	}
	
	public Line2[] getLines(){
		Line2[] lines = new Line2[edges.length];
		for(int i=0; i<edges.length; i++)
			lines[i] = edges[i].edge;
		return lines;
	}

	/* Edition methods (world editor) */
	public void add (WMWall e)
	{
		WMWall[]	tmp = new WMWall[edges.length + 1];
		System.arraycopy (edges, 0, tmp, 0, edges.length);
		tmp[edges.length] = e;
		edges = tmp;
		recomputeBounds ();
	}

	public WMWall remove (int i)
	{
		if ((i < 0) || (i >= edges.length))		return null;
		WMWall		old = edges[i];
		WMWall[]	tmp = new WMWall[edges.length - 1];
		System.arraycopy (edges, 0, tmp, 0, i);
		System.arraycopy (edges, i + 1, tmp, i, edges.length - i - 1);
		edges = tmp;
		recomputeBounds ();
		return old;
	}

	public int indexOf (WMWall e)
	{
		for (int i = 0; i < edges.length; i++)
			if (edges[i] == e)				return i;
		return -1;
	}

	public void setDefaults (double width, double height, String texture)
	{
		defWidth	= width;
		defHeight	= height;
		defTexture	= texture;
	}

	public void recomputeBounds ()
	{
		minx	= Double.MAX_VALUE;
		miny	= Double.MAX_VALUE;
		maxx	= -Double.MAX_VALUE;
		maxy	= -Double.MAX_VALUE;
		for (int i = 0; i < edges.length; i++)
			update (edges[i].edge);
	}

	/* JSON: {defaults: {width, height, texture}, items: [...]} */

	public WMWalls ()							{ this (0); recomputeBounds (); }
	public WMWalls (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonObject	o = ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
		JsonObject	def = World.getObject (o, "defaults");
		JsonArray	arr = World.getArray (o, "items");

		defWidth	= World.getDouble (def, "width", defWidth);
		defHeight	= World.getDouble (def, "height", defHeight);
		defTexture	= World.getString (def, "texture", defTexture);

		edges	= new WMWall[arr.size ()];
		for (int i = 0; i < edges.length; i++)		edges[i] = new WMWall (arr.get (i).getAsJsonObject (), defWidth, defHeight, defTexture);
		recomputeBounds ();
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonObject	def = new JsonObject ();
		JsonArray	arr = new JsonArray ();
		def.addProperty ("width", World.num (defWidth));
		def.addProperty ("height", World.num (defHeight));
		def.addProperty ("texture", defTexture);
		for (WMWall w : edges)		arr.add (w.toJson (defWidth, defHeight, defTexture));
		o.add ("defaults", def);
		o.add ("items", arr);
		return o;
	}
}
