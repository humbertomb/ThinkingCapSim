/*
 * (c) 1997-2001,2004 Humberto Martinez
 * (c) 2002 Juan Pedro Canovas Quiñonero
 * (c) 2003 Bernardo Canovas Segura (3D Stuff)
 */

package tc.shared.world;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.ArrayList;

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
	
	// Robot starting locations (at least one; the i-th robot of a simulation takes the i-th one)
	protected ArrayList<WMStart>		starts		= new ArrayList<WMStart> ();
	{ starts.add (new WMStart (0.0, 0.0, 0.0, 0.0)); }
	
	protected WMPath				path;
	
	// Map components
	protected WMIcons				icons;
	protected WMObjects			objects;
	protected WMFAreas			fareas;
	protected WMWalls				walls;
	protected WMZones				zones;
	protected WMConnectors			connectors;
	
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
	
	// Robot starting location (the first one)
	public final double	 	start_x () 			{ return starts.get (0).x (); }
	public final double	 	start_y () 			{ return starts.get (0).y (); }
	public final double	 	start_z () 			{ return starts.get (0).z (); }
	public final double	 	start_a () 			{ return starts.get (0).orientation; }

	/** All the start points (START_1, START_2, ...). Never empty. */
	public final java.util.List<WMStart>	starts ()	{ return starts; }
	public final int		n_starts ()				{ return starts.size (); }
	/** The i-th start point, or the first one when there are not that many (multi-robot simulations). */
	public final WMStart	start (int i)			{ return ((i >= 0) && (i < starts.size ())) ? starts.get (i) : starts.get (0); }
	public final WMStart	addStart (double x, double y, double z, double a)	{ WMStart st = new WMStart (x, y, z, a); starts.add (st); return st; }
	/** Removes a start point; the last one cannot be removed. */
	public final boolean	removeStart (int i)
	{
		if ((starts.size () <= 1) || (i < 0) || (i >= starts.size ()))		return false;
		starts.remove (i);
		return true;
	}
	
	// World components
	public final WMPath 			path ()				{ return path; }
	public final WMWalls 		walls ()				{ return walls; }
	public final WMObjects 		objects ()			{ return objects; }
	public final WMIcons			icons ()			{ return icons; }
	public final WMZones			zones ()				{ return zones; }
	public final WMFAreas			fareas ()				{ return fareas; }
	public final WMConnectors	connectors ()		{ return connectors; }
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

	/** Writes STARTS and START_1..START_n. */
	public void startsToProperties (Properties p)
	{
		p.setProperty ("STARTS", String.valueOf (starts.size ()));
		for (int i = 0; i < starts.size (); i++)
			p.setProperty ("START_" + (i + 1), starts.get (i).toProperty ());
	}
	
	/* Set methods */
	// Robot starting location
	public final void setStart (double sx, double sy, double sa) 		
	{
		starts.get (0).set (sx, sy, starts.get (0).z (), sa);
	}

	public final void setStart (double sx, double sy, double sz, double sa) 		
	{
		starts.get (0).set (sx, sy, sz, sa);
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
			Line2 path = connectors.at(label).path;
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
		
		if (connectors.index (name) != -1)
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
		connectors	= new WMConnectors (dxf);
		icons	= new WMIcons ();
		objects	= new WMObjects (dxf, icons);
		fareas	= new WMFAreas (new Properties ());
		
		ArrayList<Entity> entities = dxf.getEntities();
		
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("OTHERS")){
				if(entity instanceof TextDxf){ 
					String texto = ((TextDxf)entity).getText();
					if(texto.startsWith("START")){
						String prop = texto.substring(texto.lastIndexOf("=")+1).trim();
						// DXF files written by older versions carry "x, y, angle" (no z); START_i texts add start points
						WMStart	st = new WMStart (prop);
						if (texto.startsWith ("START_") && !texto.startsWith ("START_1"))		starts.add (st);
						else	starts.set (0, st);
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
		String				prop;
		
		if (worldprop == null)				return;
		
		// Read in Robot starting locations: STARTS = n + START_1..START_n, or the legacy single START
		starts.clear ();
		int	nstarts = 0;
		try { nstarts = Integer.parseInt (worldprop.getProperty ("STARTS", "0").trim ()); } catch (Exception e) { }
		for (int i = 1; i <= nstarts; i++)
		{
			prop = worldprop.getProperty ("START_" + i);
			if (prop != null)		starts.add (new WMStart (prop));
		}
		if (starts.isEmpty ())
			starts.add (new WMStart (worldprop.getProperty ("START", "0.0, 0.0, 0.0, 0.0")));
		
		path 			= new WMPath(worldprop);
		walls			= new WMWalls (worldprop);
		icons			= new WMIcons (worldprop);
		objects			= new WMObjects (worldprop, icons);
		fareas			= new WMFAreas(worldprop);
		zones			= new WMZones (worldprop);
		connectors			= new WMConnectors (worldprop);
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
		
		// Store Start Points
		startsToProperties (worldprop);
		
		path.toProperties (worldprop);
		walls.toProperties (worldprop);
		icons.toProperties (worldprop);
		objects.toProperties (worldprop);
		zones.toProperties (worldprop);
		fareas.toProperties(worldprop);
		connectors.toProperties (worldprop);
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
		
		// Print Start Points
		out.println("# ==============================");
		out.println("# START POINTS");
		out.println("# ==============================");
		out.println("STARTS = " + starts.size ());
		for (int i = 0; i < starts.size (); i++)
			out.println("START_" + (i + 1) + " = " + starts.get (i).toProperty ());
		out.println("");
		
		path.toFile (out);
		walls.toFile (out);
		icons.toFile (out);
		objects.toFile (out);
		zones.toFile (out);
		fareas.toFile(out);
		connectors.toFile (out);	
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
		connectors.toDxfFile(dxf);
		// Others
		//	  Define una capa con un color determinado (opcional)
		dxf.addLayer(new Layer("OTHERS",ACADColor.BLUE));
		for (int i = 0; i < starts.size (); i++)
		dxf.addEntity(
				new TextDxf(
						"START_" + (i + 1) + " = " + starts.get (i).toProperty (),
						new Point3(starts.get (i).x (), starts.get (i).y (), starts.get (i).z ()),
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

