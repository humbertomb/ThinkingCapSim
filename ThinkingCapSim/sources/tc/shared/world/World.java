/*
 * (c) 1997-2001,2004 Humberto Martinez
 * (c) 2002 Juan Pedro Canovas Qui�onero
 * (c) 2003 Bernardo Canovas Segura (3D Stuff)
 */

package tc.shared.world;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;
import wucore.utils.geom.Polygon2;

public class World extends Object
{
	static public final String		SUFFIX	= ".world";
	
	// Object types
	static public final int			NONE		= 0;
	static public final int			WP		= 1;
	static public final int			DOOR		= 2;
	static public final int			DOCK		= 3;
	static public final int			ZONE		= 4;
	
	// Graphical representation properties
	public double					G_RADIUS	= 0.15;		// Goal point radius (m)
	public double					G_LENGHT	= 0.35;		// Goal point arrow lenght (m)
	public double					D_LENGHT	= 0.25;		// Dock icon lenght (m)
	
	// Robot starting location
	protected double					sx;
	protected double					sy;
	protected double					sa;
	
	protected WMPath				path;
	
	// Map components
	protected WMObjects			objects;
	protected WMFAreas			fareas;
	protected WMWalls				walls;
	protected WMZones				zones;
	protected WMDoors				doors;
	
	protected WMCBeacons			cbeacons;
	protected WMBeacons			beacons;
	
	protected WMWaypoints		waypoints;
	protected WMDocks				docks;
	
	
	/* Constructors */
	public World ()
	{
		
	}
	
	public World (String name) throws Exception
	{
		this ();
		fromFile (name);
	}
	
	public World (Properties props)
	{
		this ();
		fromProperties (props);
	}
	
	/* Accessor methods */	
	
	// Robot starting location
	public final double	 	start_x () 			{ return sx; }
	public final double	 	start_y () 			{ return sy; }
	public final double	 	start_a () 			{ return sa; }
	
	// World components
	public final WMPath 			path ()				{ return path; }
	public final WMWalls 		walls ()				{ return walls; }
	public final WMObjects 		objects ()			{ return objects; }
	public final WMZones			zones ()				{ return zones; }
	public final WMFAreas			fareas ()				{ return fareas; }
	public final WMDoors			doors ()				{ return doors; }
	public final WMBeacons		beacons ()			{ return beacons; }
	public final WMCBeacons		cbeacons ()			{ return cbeacons; }
	public final WMWaypoints	wps ()				{ return waypoints; }
	public final WMDocks			docks ()				{ return docks; }
	
	public final Line2[] getLines()	
	{
		Line2[] lin = walls.getLines();
		Line2[] obj = objects.getLines();
		Line2[] all = new Line2[lin.length+obj.length];
		System.arraycopy(lin,0,all,0,lin.length);
		System.arraycopy(obj,0,all,lin.length,obj.length); 
		return all;
	}
	
	public final Polygon2[] getFAreas()	
	{
		Polygon2[] polygons = fareas.getPolygons();
		
		return polygons;
	}
	
	/* Instance methods */
	
	/* Set methods */
	// Robot starting location
	public final void setStart (double sx, double sy, double sa) 		
	{
		this.sx = sx;
		this.sy = sy;
		this.sa = sa;
	}
	
	public double getAngle (String label)
	{
		int type = getType(label);
		
		switch(type)
		{
		case DOCK:
			return docks.at(label).getAng();
		case WP:
			return waypoints.at(label).getAng();
		}
		
		return 0.0;	    
	}
	
	public Point3 getPos (String label){
		return getPos (label,"");
	}
	
	// Devuelve la posicion de un elemento segun su nombre 
	public Point3 getPos (String label, String zone)
	{
		int type = getType(label);
		
		switch(type){
		case DOCK:
			return docks.at(label).getPos();
		case WP:
			return waypoints.at(label).getPos();
		case ZONE:
			return new Point3 (zones.at (label).area.getCenterX(), zones.at (label).area.getCenterY(), 0.0);
		case DOOR:
			Line2 path = doors.at(label).path;
			if (zone.equals (zones.inZone(path.orig()))) return new Point3(path.orig());
			else if (zone.equals (zones.inZone(path.dest()))) return new Point3(path.dest());
			else System.out.println("  [World.getPos()] Warning!!! Door "+label+" no esta en zona "+zone);
		}
		
		return null;	
	}
	
	// Determina el tipo de un label
	public int getType (String name)
	{
		if (waypoints.index (name) != -1)
			return WP;	
		
		if (doors.index (name) != -1)
			return DOOR;
		
		if (zones.index (name) != -1)
			return ZONE;
		
		if (docks.index (name) != -1)
			return DOCK;
		
		
		
		return NONE;
	}
	
	public void fromFile (String name) throws Exception
	{
		FileInputStream		worldfile;
		Properties			worldprop = null;	
		
		if (!name.endsWith (SUFFIX))
			System.out.println ("Loading world: Unknown file-extension. Continue loading ...");
		
		worldprop = new Properties ();
		worldfile = new FileInputStream (name);
		worldprop.load (worldfile);
		
		fromProperties (worldprop);
	}
	
	public void fromDxfFile (String name) throws Exception{
		DXFWorldFile dxf = new DXFWorldFile();
		dxf.load(name);	// Carga archivo dxf
		System.out.println(dxf);
		
		path = new WMPath(dxf);
		walls	= new WMWalls (dxf);
		zones	= new WMZones (dxf);
		doors	= new WMDoors (dxf);
		objects	= new WMObjects (dxf);
		
		Vector entities = dxf.getEntities();
		
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("OTHERS")){
				if(entity instanceof TextDxf){ 
					String texto = ((TextDxf)entity).getText();
					if(texto.startsWith("START")){
						String prop = texto.substring(texto.lastIndexOf("=")+1).trim();
						StringTokenizer st = new StringTokenizer (prop,", \t");
						sx = Double.parseDouble (st.nextToken());
						sy = Double.parseDouble (st.nextToken());
						sa = Math.toRadians (Double.parseDouble (st.nextToken()));
					}
				}
			}
		}	
		
		waypoints	= new WMWaypoints (dxf);
		docks			= new WMDocks (dxf);
		beacons		= new WMBeacons (dxf);
		cbeacons		= new WMCBeacons (dxf);
		
		
	}
	
	public void fromProperties (Properties worldprop)
	{		
		StringTokenizer		st;
		String				prop;
		
		if (worldprop == null)				return;
		
		// Read in Robot starting location (x, y, alpha)
		prop = worldprop.getProperty ("START","0.0, 0.0, 0.0");
		st = new StringTokenizer (prop,", \t");
		sx = Double.parseDouble (st.nextToken());
		sy = Double.parseDouble (st.nextToken());
		sa = Math.toRadians (Double.parseDouble (st.nextToken()));
		
		path 			= new WMPath(worldprop);
		walls			= new WMWalls (worldprop);
		objects			= new WMObjects (worldprop);
		fareas			= new WMFAreas(worldprop);
		zones			= new WMZones (worldprop);
		doors			= new WMDoors (worldprop);
		waypoints		= new WMWaypoints (worldprop);
		docks			= new WMDocks (worldprop);
		beacons			= new WMBeacons (worldprop);		
		cbeacons		= new WMCBeacons (worldprop);
	}
	
	public void toFileProperties (String name) throws Exception
	{
		
		Properties			worldprop;
		FileOutputStream	worldfile;
		
		worldprop = new Properties ();
		
		// Store Start Point
		worldprop.setProperty ("START", sx + ", " + sy + ", " + Math.toDegrees(sa));
		
		path.toProperties (worldprop);
		walls.toProperties (worldprop);
		objects.toProperties (worldprop);
		zones.toProperties (worldprop);
		fareas.toProperties(worldprop);
		doors.toProperties (worldprop);
		waypoints.toProperties (worldprop);
		docks.toProperties (worldprop);
		beacons.toProperties (worldprop);
		cbeacons.toProperties (worldprop);
		
		worldfile = new FileOutputStream (name);
		worldprop.store (worldfile,"World map");
		
		worldfile.close ();				
	}
	
	public void toFile (String name) throws Exception
	{	
		PrintWriter out;
		
		out = new PrintWriter(new FileOutputStream(name));
		
		// Print Start Point
		out.println("# ==============================");
		out.println("# START POINT");
		out.println("# ==============================");
		out.println("START = " + sx + ", " + sy + ", " + Math.toDegrees(sa));
		out.println("");
		
		path.toFile (out);
		walls.toFile (out);
		objects.toFile (out);
		zones.toFile (out);
		fareas.toFile(out);
		doors.toFile (out);	
		waypoints.toFile (out);
		docks.toFile (out);
		beacons.toFile(out);
		cbeacons.toFile(out);
		
		out.close();
		
	}
	
	public void toDxfFile (String name) throws Exception{
		
		DXFWorldFile dxf = new DXFWorldFile();
		
		path.toDxfFile(dxf);
		walls.toDxfFile(dxf);
		objects.toDxfFile(dxf);
		zones.toDxfFile(dxf);
		waypoints.toDxfFile(dxf);
		docks.toDxfFile(dxf);
		cbeacons.toDxfFile(dxf);
		beacons.toDxfFile(dxf);
		doors.toDxfFile(dxf);
		// Others
		//	  Define una capa con un color determinado (opcional)
		dxf.addLayer(new Layer("OTHERS",ACADColor.BLUE));
		dxf.addEntity(
				new TextDxf(
						"START = "+sx+", "+sy+", "+Math.toDegrees(sa),
						new Point3(sx,sy,0.0),
						0.2,
						"OTHERS"
				)
		);
		dxf.createDxf(name);
		
	}
	
	
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////	A PARTIR DE AQUI NO TOCAR
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	
	public Line2 crossline (Line2 line)
	{		
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
	}
	
	public Line2 crossline (double x1, double y1, double x2, double y2){
		Line2 line1 = walls.crossline(x1,y1,x2,y2);
		Line2 line2 = objects.crossline(x1,y1,x2,y2);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.intersection(x1,y1,x2,y2).distance(x1,y1) < line2.intersection(x1,y1,x2,y2).distance(x1,y1))
			return line1;
		return line2;
	}
	
	public Line2 crossline (Line2 line, Line2[][] virtuals, int nvirtual, int skip)
	{
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y (), virtuals, nvirtual, skip);
	}
	
	public Line2 crossline (double x1, double y1, double x2, double y2, Line2[][] virtuals, int nvirtual, int skip){
		
		if (walls == null)			return null;
		if (objects == null)		return null;
		
		Line2 line1 = walls.crossline(x1,y1,x2,y2,virtuals,nvirtual,skip);
		Line2 line2 = objects.crossline(x1,y1,x2,y2);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.intersection(x1,y1,x2,y2).distance(x1,y1) < line2.intersection(x1,y1,x2,y2).distance(x1,y1))
			return line1;
		return line2;	
	}
	
	public Line2 closer (double x1, double y1){
		Line2 line1 = walls.closer(x1,y1);
		Line2 line2 = objects.closer(x1,y1);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.distance(x1,y1) < line2.distance(x1,y1))
			return line1;
		return line2;	
	}
	
	public Line2 closer (double x1, double y1, Line2[][] virtuals, int nvirtual, int skip){
		Line2 line1 = walls.closer(x1,y1,virtuals,nvirtual,skip);
		Line2 line2 = objects.closer(x1,y1);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.distance(x1,y1) < line2.distance(x1,y1))
			return line1;
		return line2;
	}
	
	public String toString (double x, double y)
	{
		int i, index;
		double d, mindist;
		
		String stret = new String ("");
		
		stret += "Zone: "+zones.inZone (x,y)+". ";
		
		mindist = Double.POSITIVE_INFINITY;
		index = -1;
		for (i=0; i < docks.n(); i++)
		{
			d = docks.at(i).pos.distance (x,y);
			if (d < mindist)
			{
				mindist = d;
				index = i;
			}
		}
		if (index != -1)
			stret += "Nearest dock: "+docks.at(index).label;
		
		return (stret);
	} 	
}

