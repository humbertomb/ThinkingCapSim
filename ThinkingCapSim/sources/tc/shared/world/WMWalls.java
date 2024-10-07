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

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
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
	
	public WMWalls (Properties props)
	{
		fromProperties (props);
	}
	
	public WMWalls (DXFWorldFile dxf)
	{
		Vector entities = dxf.getEntities();
		Vector walls = new Vector();
		Entity entity;
		
		// Se guardan las lineas de la Capa 0 (lineas de Wall) en un vector y se leen las propiedades por defecto
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("0")){
				if(entity instanceof LineDxf){
				  walls.add(entity);
				}
				else if(entity instanceof PolylineDxf){
				    LineDxf[] poly = ((PolylineDxf)entity).toDxfLines();
				    walls.add(poly);
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
			edges[i] = new WMWall((LineDxf)walls.get(i),defWidth, defHeight, defTexture);
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
	
	public void fromProperties (Properties props)
	{
		int					i;
		String				prop;
		
		// Initialise size of the map
		minx	= Double.MAX_VALUE;
		miny	= Double.MAX_VALUE;
		maxx	= Double.MIN_VALUE;
		maxy	= Double.MIN_VALUE;	

		edges	= new WMWall[Integer.parseInt (props.getProperty ("MAX_LINES", "0"))];

		if ((prop = props.getProperty ("LINE_DEF_WIDTH")) != null)
			defWidth	= Double.parseDouble (prop);
		if ((prop = props.getProperty ("LINE_DEF_HEIGHT")) != null)	
			defHeight	= Double.parseDouble (prop);
		if ((prop = props.getProperty ("LINE_DEF_TEXTURE")) != null)	
			defTexture	= prop;
				
		// Read in world line segments
		for (i=0; i < edges.length; i++)
		{
			prop		= props.getProperty ("LINE_"+i);		
			edges[i]	= new WMWall (prop, defWidth, defHeight, defTexture);
			update (edges[i].edge);
		}		
	}	
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("MAX_LINES",Integer.toString (edges.length));
		
		for (i = 0; i < edges.length; i++)
			props.setProperty ("LINE_"+i, edges[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		int				i;
		// Print Line Segments
		out.println("# ==============================");
		out.println("# MAP WALL LINES");
		out.println("# ==============================");
		out.println ("MAX_LINES = " + edges.length);	
		out.println("");
		out.println("LINE_DEF_HEIGHT = "+defHeight);
		out.println("LINE_DEF_WIDTH = "+defWidth);
		out.println("LINE_DEF_TEXTURE = "+defTexture);
		out.println("");
		
		for (i = 0; i < edges.length; i++){ 
			if(edges[i].texture.equals(defTexture) && edges[i].width == defWidth && edges[i].height == defHeight)
				out.println ("LINE_" + i + " = " + DoubleFormat.format(edges[i].edge.orig().x())+", "+DoubleFormat.format(edges[i].edge.orig().y())+", "+DoubleFormat.format(edges[i].edge.dest().x())+", "+DoubleFormat.format(edges[i].edge.dest().y()));
			else
				out.println ("LINE_" + i + " = " + edges[i].toRawString ());
		}
		out.println ();		
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
	
}
