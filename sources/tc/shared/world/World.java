/*
 * (c) 1997-2001,2004 Humberto Martinez
 * (c) 2002 Juan Pedro Canovas Quiñonero
 * (c) 2003 Bernardo Canovas Segura (3D Stuff)
 */

package tc.shared.world;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;
import wucore.utils.geom.Polygon2;

import tclib.planning.htopol.HTopolMap;

public class World extends Object
{
	static public final String		SUFFIX	= ".world";
	
	// Object types
	static public final int			NONE		= 0;
	static public final int			WP			= 1;
	static public final int			DOOR		= 2;
	static public final int			DOCK		= 3;
	static public final int			ZONE		= 4;
	
	// Graphical representation properties
	public double					G_RADIUS	= 0.15;		// Goal point radius (m)
	public double					G_LENGHT	= 0.35;		// Goal point arrow lenght (m)
	public double					D_LENGHT	= 0.25;		// Dock icon lenght (m)
	
	// Behaviour properties (how the world is used by the simulation)
	static public final boolean		DEF_APW		= false;
	public boolean					apw			= DEF_APW;	// A priori world knowledge: the robots know the map beforehand

	// Robot starting locations (at least one; the i-th robot of a simulation takes the i-th one)
	protected ArrayList<WMStart>		starts		= new ArrayList<WMStart> ();
	{ starts.add (new WMStart (0.0, 0.0, 0.0, 0.0)); }
	
	// Proposed robot's path points
	protected ArrayList<Point2>		path		= new ArrayList<Point2> ();
	
	// Map components
	protected ArrayList<WMIcon>		icons		= new ArrayList<WMIcon> ();		// Icon library the objects refer to
	protected ArrayList<WMObject>	objects		= new ArrayList<WMObject> ();
	protected ArrayList<WMAObject>	aobjects	= new ArrayList<WMAObject> ();	// Animated objects
	protected WMFAreas				fareas;
	protected WMWalls				walls;
	protected WMMarkings			markings;									// Lines drawn on the floor (visual guides)
	protected WMZones				zones;
	protected WMConnectors			connectors;	
	protected ArrayList<WMCBeacon>	cbeacons	= new ArrayList<WMCBeacon> ();
	protected ArrayList<WMBeacon>	beacons		= new ArrayList<WMBeacon> ();
	protected ArrayList<WMWaypoint>	waypoints	= new ArrayList<WMWaypoint> ();
	protected ArrayList<WMDock>		docks		= new ArrayList<WMDock> ();
	
	protected HTopolMap				topol;
		
	/* Constructors */
	public World ()
	{
		
	}
	
	public World (String name) throws Exception
	{
		this ();
		fromFile (name);
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
	public final List<Point2>	path ()				{ return path; }
	public final WMWalls 		walls ()			{ return walls; }
	public final WMMarkings		markings ()			{ return markings; }
	public final List<WMObject>	objects ()			{ return objects; }
	public final List<WMAObject> aobjects ()		{ return aobjects; }

	/** All the objects, static and animated (a new list; editing it does not change the world). */
	public final List<WMObject>	allObjects ()
	{
		List<WMObject>	all = new ArrayList<WMObject> (objects.size () + aobjects.size ());
		all.addAll (objects);
		all.addAll (aobjects);
		return all;
	}
	public final List<WMIcon>	icons ()			{ return icons; }
	public final WMZones		zones ()			{ return zones; }
	public final WMFAreas		fareas ()			{ return fareas; }
	public final WMConnectors	connectors ()		{ return connectors; }
	public final List<WMBeacon>	beacons ()			{ return beacons; }
	public final List<WMCBeacon> cbeacons ()		{ return cbeacons; }
	public final List<WMWaypoint> wps ()			{ return waypoints; }
	public final List<WMDock>	docks ()			{ return docks; }

	/* Lookup by label */

	/** Index of the element with the given label in a list, or -1. */
	static public int index (List<? extends WMElement> list, String label)
	{
		if ((label == null) || (list == null))		return -1;
		for (int i = 0; i < list.size (); i++)
			if (label.equals (list.get (i).label))	return i;
		return -1;
	}

	/** The element with the given label in a list, or null. */
	static public <T extends WMElement> T find (List<T> list, String label)
	{
		int		i = index (list, label);
		return (i < 0) ? null : list.get (i);
	}

	public final WMWaypoint		waypoint (String label)		{ return find (waypoints, label); }
	public final WMDock			dock (String label)			{ return find (docks, label); }
	public final WMBeacon		beacon (String label)		{ return find (beacons, label); }
	public final WMCBeacon		cbeacon (String label)		{ return find (cbeacons, label); }
	public final WMIcon			icon (String label)			{ return find (icons, label); }

	/* Icon library */

	/** First icon with exactly the same segments, or null. */
	public WMIcon findIcon (Line2[] lines)
	{
		WMIcon		probe = new WMIcon ("?", lines);
		for (WMIcon ic : icons)
			if (ic.sameGeometry (probe))		return ic;
		return null;
	}

	/** An icon label not yet used: prefix, prefix_2, prefix_3 ... */
	public String uniqueIconLabel (String prefix)
	{
		if ((prefix == null) || (prefix.length () == 0))		prefix = "icon";
		if (index (icons, prefix) < 0)			return prefix;
		for (int i = 2; ; i++)
			if (index (icons, prefix + "_" + i) < 0)	return prefix + "_" + i;
	}

	/**
	 * Registers an icon for the given local segments, reusing an existing one
	 * with the same geometry when possible.
	 */
	public WMIcon registerIcon (Line2[] lines, String preferredLabel)
	{
		WMIcon		icon = findIcon (lines);
		if (icon != null)				return icon;
		icon = new WMIcon (uniqueIconLabel (preferredLabel), lines);
		icons.add (icon);
		return icon;
	}

	/** Elevation of a path point (0 if it carries none). */
	static public double z (Point2 p)			{ return (p instanceof Point3) ? ((Point3) p).z () : 0.0; }

	/** Index of the (plate) beacon first crossed by a segment, or -1 when it crosses none. */
	public int crossBeacon (Line2 line)
	{
		return crossBeacon (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
	}

	public int crossBeacon (double x1, double y1, double x2, double y2)
	{
		int			index = -1;
		double		d = Double.MAX_VALUE;
		for (int i = 0; i < beacons.size (); i++)
		{
			Point2	pt = beacons.get (i).getLine ().intersection (x1, y1, x2, y2);
			if ((pt != null) && (pt.distance (x1, y1) < d))
			{
				d		= pt.distance (x1, y1);
				index	= i;
			}
		}
		return index;
	}

	/** The hierarchical topological map of the world, or null when the world has none. */
	public final HTopolMap		topology ()			{ return topol; }
	public final boolean		hasTopology ()		{ return topol != null; }
	public final void			setTopology (HTopolMap t)	{ topol = t; if (t != null) t.setWorld (this); }
	
	public final Line2[] getLines()	
	{
		Line2[] lin = walls.getLines();
		Line2[] obj = objectLines ();
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
			return dock (label).getAng();
		case WP:
			return waypoint (label).getAng();
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
			return dock (label).getPos();
		case WP:
			return waypoint (label).getPos();
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
		if (index (waypoints, name) != -1)
			return WP;	
		
		if (connectors.index (name) != -1)
			return DOOR;
		
		if (zones.index (name) != -1)
			return ZONE;
		
		if (index (docks, name) != -1)
			return DOCK;
		
		
		
		return NONE;
	}
	
	/** Reads a JSON <code>.world</code> file. */
	public void fromFile (String name) throws Exception
	{
		if (!name.endsWith (SUFFIX))
			System.out.println ("Loading world: Unknown file-extension. Continue loading ...");
		fromJson (parse (new String (Files.readAllBytes (Paths.get (name)), StandardCharsets.UTF_8)));
	}

	/** Writes the world as a JSON <code>.world</code> file. */
	public void toFile (String name) throws Exception
	{
		Files.write (Paths.get (name), toJsonText ().getBytes (StandardCharsets.UTF_8));
	}

	/* JSON representation: {starts, path, walls, icons, objects, aobjects, zones, fareas, connectors, waypoints, docks, beacons, cbeacons} */

	/** An empty world (one start point at the origin and every collection created but empty). */
	static public World empty ()
	{
		World	w = new World ();
		w.fromJson (null);
		return w;
	}

	/** A world built from its JSON text (the form in which it travels between modules through Linda). */
	static public World fromJsonText (String jsonText)
	{
		World	w = new World ();
		w.fromJson (parse (jsonText));
		return w;
	}

	public void fromJson (JsonObject o)
	{
		if (o == null)			o = new JsonObject ();

		apw			= getBoolean (o, "apw", DEF_APW);

		starts.clear ();
		for (JsonElement e : getArray (o, "starts"))		starts.add (new WMStart (e.getAsJsonObject ()));
		if (starts.isEmpty ())		starts.add (new WMStart (0.0, 0.0, 0.0, 0.0));

		path.clear ();
		for (JsonElement e : getArray (o, "path"))			path.add (toPoint (e.getAsJsonObject ()));
		walls		= new WMWalls (o.get ("walls"));
		markings	= new WMMarkings (o.get ("markings"));
		icons.clear ();
		for (JsonElement e : getArray (o, "icons"))			icons.add (new WMIcon (e.getAsJsonObject ()));
		objects.clear ();
		for (JsonElement e : getArray (o, "objects"))		objects.add (new WMObject (e.getAsJsonObject (), icons));
		aobjects.clear ();
		for (JsonElement e : getArray (o, "aobjects"))		aobjects.add (new WMAObject (e.getAsJsonObject (), icons));
		fareas		= new WMFAreas (o.get ("fareas"));
		zones		= new WMZones (o.get ("zones"));
		connectors	= new WMConnectors (o.get ("connectors"));
		waypoints.clear ();
		for (JsonElement e : getArray (o, "waypoints"))		waypoints.add (new WMWaypoint (e.getAsJsonObject ()));
		docks.clear ();
		for (JsonElement e : getArray (o, "docks"))			docks.add (new WMDock (e.getAsJsonObject ()));
		beacons.clear ();
		for (JsonElement e : getArray (o, "beacons"))		beacons.add (new WMBeacon (e.getAsJsonObject ()));
		cbeacons.clear ();
		for (JsonElement e : getArray (o, "cbeacons"))		cbeacons.add (new WMCBeacon (e.getAsJsonObject ()));
		topol		= o.has ("topology") ? new HTopolMap (this, getObject (o, "topology")) : null;
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("apw", apw);
		JsonArray	st = new JsonArray ();
		for (WMStart s : starts)		st.add (s.toJson ());
		o.add ("starts", st);
		JsonArray	pa = new JsonArray ();
		for (Point2 p : path)			pa.add (point (p));
		o.add ("path", pa);
		o.add ("walls", walls.toJson ());
		o.add ("markings", markings.toJson ());
		JsonArray	ia = new JsonArray ();
		for (WMIcon x : icons)			ia.add (x.toJson ());
		o.add ("icons", ia);
		JsonArray	oa = new JsonArray ();
		for (WMObject x : objects)		oa.add (x.toJson ());
		o.add ("objects", oa);
		JsonArray	aa = new JsonArray ();
		for (WMAObject x : aobjects)	aa.add (x.toJson ());
		o.add ("aobjects", aa);
		o.add ("zones", zones.toJson ());
		o.add ("fareas", fareas.toJson ());
		o.add ("connectors", connectors.toJson ());
		JsonArray	wa = new JsonArray ();
		for (WMWaypoint x : waypoints)	wa.add (x.toJson ());
		o.add ("waypoints", wa);
		JsonArray	da = new JsonArray ();
		for (WMDock x : docks)			da.add (x.toJson ());
		o.add ("docks", da);
		JsonArray	ba = new JsonArray ();
		for (WMBeacon x : beacons)		ba.add (x.toJson ());
		o.add ("beacons", ba);
		JsonArray	ca = new JsonArray ();
		for (WMCBeacon x : cbeacons)	ca.add (x.toJson ());
		o.add ("cbeacons", ca);
		if (topol != null)		o.add ("topology", topol.toJson ());
		return o;
	}

	/** Pretty-printed JSON text of the world (the file contents). */
	public String toJsonText ()					{ return toText (toJson ()); }
	
	
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////	A PARTIR DE AQUI NO TOCAR
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	
	public Line2 crossline (Line2 line)
	{		
		return crossline (line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
	}
	
	public Line2 crossline (double x1, double y1, double x2, double y2){
		Line2 line1 = walls.crossline(x1,y1,x2,y2);
		Line2 line2 = objectsCrossline(x1,y1,x2,y2);
		
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
		
		Line2 line1 = walls.crossline(x1,y1,x2,y2,virtuals,nvirtual,skip);
		Line2 line2 = objectsCrossline(x1,y1,x2,y2);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.intersection(x1,y1,x2,y2).distance(x1,y1) < line2.intersection(x1,y1,x2,y2).distance(x1,y1))
			return line1;
		return line2;	
	}
	
	public Line2 closer (double x1, double y1){
		Line2 line1 = walls.closer(x1,y1);
		Line2 line2 = objectsCloser(x1,y1);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.distance(x1,y1) < line2.distance(x1,y1))
			return line1;
		return line2;	
	}
	
	public Line2 closer (double x1, double y1, Line2[][] virtuals, int nvirtual, int skip){
		Line2 line1 = walls.closer(x1,y1,virtuals,nvirtual,skip);
		Line2 line2 = objectsCloser(x1,y1);
		
		if(line1 == null && line2 == null) return null;
		if(line1 == null && line2 !=null) return line2;
		if(line2 == null && line1 !=null) return line1;
		
		if(line1.distance(x1,y1) < line2.distance(x1,y1))
			return line1;
		return line2;
	}
	
	/* Extent of the world */

	static public final double		EMPTY_HALF_SIZE	= 5.0;		// half size (m) of the extent of a world without geometry

	/**
	 * Bounding box {minx, miny, maxx, maxy} of every element of the world
	 * (walls, zones, forbidden areas, connectors, objects, beacons, waypoints,
	 * docks, path and start points). A world without any of them extends
	 * {@link #EMPTY_HALF_SIZE} metres around its first start point, so that
	 * the maps built from it (grids, floors) are never empty or negative.
	 */
	public double[] bounds ()
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };

		if (walls.n () > 0)
			for (int i = 0; i < walls.n (); i++)		extend (b, walls.at (i).edge);
		for (int i = 0; i < markings.n (); i++)			extend (b, markings.at (i).edge);
		for (int i = 0; i < zones.n (); i++)
		{
			java.awt.geom.Rectangle2D	r = zones.at (i).area;
			extend (b, r.getMinX (), r.getMinY ());	extend (b, r.getMaxX (), r.getMaxY ());
		}
		for (int i = 0; i < fareas.n (); i++)
		{
			Polygon2	p = fareas.at (i).polygon;
			for (int k = 0; k < p.npoints; k++)			extend (b, p.xpoints[k], p.ypoints[k]);
		}
		for (int i = 0; i < connectors.n (); i++)
		{
			extend (b, connectors.at (i).edge);
			extend (b, connectors.at (i).path);
		}
		for (WMObject o : objects)
		{
			extend (b, o.pos.x (), o.pos.y ());
			for (Line2 l : o.absIcon ())			extend (b, l);
		}
		for (WMAObject o : aobjects)
		{
			extend (b, o.pos.x () - o.radius, o.pos.y () - o.radius);	extend (b, o.pos.x () + o.radius, o.pos.y () + o.radius);
			for (Line2 l : o.absIcon ())			extend (b, l);
		}
		for (WMBeacon x : beacons)				extend (b, x.getLine ());
		for (WMCBeacon x : cbeacons)			{ extend (b, x.x () - x.radius (), x.y () - x.radius ());	extend (b, x.x () + x.radius (), x.y () + x.radius ()); }
		for (WMWaypoint x : waypoints)			extend (b, x.pos.x (), x.pos.y ());
		for (WMDock x : docks)					extend (b, x.pos.x (), x.pos.y ());
		for (Point2 p : path)					extend (b, p.x (), p.y ());

		if (b[0] > b[2])		// nothing at all: a square around the first start point
		{
			WMStart	st = starts.get (0);
			return new double[] { st.x () - EMPTY_HALF_SIZE, st.y () - EMPTY_HALF_SIZE, st.x () + EMPTY_HALF_SIZE, st.y () + EMPTY_HALF_SIZE };
		}
		for (WMStart st : starts)				extend (b, st.x (), st.y ());
		return b;
	}

	static private void extend (double[] b, double x, double y)
	{
		if (x < b[0])		b[0] = x;
		if (y < b[1])		b[1] = y;
		if (x > b[2])		b[2] = x;
		if (y > b[3])		b[3] = y;
	}

	static private void extend (double[] b, Line2 l)
	{
		extend (b, l.orig ().x (), l.orig ().y ());
		extend (b, l.dest ().x (), l.dest ().y ());
	}

	public final double		minx ()				{ return bounds ()[0]; }
	public final double		miny ()				{ return bounds ()[1]; }
	public final double		maxx ()				{ return bounds ()[2]; }
	public final double		maxy ()				{ return bounds ()[3]; }

	/* Geometry of the (visible) objects: the segments of their icons in world coordinates */

	/** Segments of all the visible objects. */
	public Line2[] objectLines ()
	{
		ArrayList<Line2>	lines = new ArrayList<Line2> ();
		for (WMObject ob : objects)
			if (ob.visible)
				for (Line2 l : ob.absIcon ())		lines.add (l);
		return lines.toArray (new Line2[0]);
	}

	/** First object segment crossed by a segment, or null. */
	public Line2 objectsCrossline (double x1, double y1, double x2, double y2)
	{
		Line2		cln = null;
		double		d = Double.MAX_VALUE;
		for (WMObject ob : objects)
			if (ob.visible)
				for (Line2 l : ob.absIcon ())
				{
					Point2	pt = l.intersection (x1, y1, x2, y2);
					if ((pt != null) && (pt.distance (x1, y1) < d))
					{
						d	= pt.distance (x1, y1);
						cln	= l;
					}
				}
		return cln;
	}

	/** Object segment closest to a point, or null. */
	public Line2 objectsCloser (double x1, double y1)
	{
		Line2		tmp = null;
		double		d = Double.MAX_VALUE;
		for (WMObject ob : objects)
			if (ob.visible)
				for (Line2 l : ob.absIcon ())
					if (l.distance (x1, y1) < d)
					{
						tmp	= l;
						d	= l.distance (x1, y1);
					}
		return tmp;
	}

	public String toString (double x, double y)
	{
		int i, index;
		double d, mindist;
		
		String stret = new String ("");
		
		stret += "Zone: "+zones.inZone (x,y)+". ";
		
		mindist = Double.POSITIVE_INFINITY;
		index = -1;
		for (i=0; i < docks.size (); i++)
		{
			d = docks.get (i).pos.distance (x,y);
			if (d < mindist)
			{
				mindist = d;
				index = i;
			}
		}
		if (index != -1)
			stret += "Nearest dock: "+docks.get (index).label;
		
		return (stret);
	} 	

	/* ================================================================== */
	/* JSON helpers: points are objects with x, y, z, segments with          */
	/* x1, y1, z1, x2, y2, z2; angles in degrees; numbers rounded to        */
	/* DECIMALS decimals when written                                       */
	/* ================================================================== */

	/** Decimals kept when writing coordinates and sizes. */
	static public final int			DECIMALS	= 4;

	static private final Gson		GSON		= new GsonBuilder ().disableHtmlEscaping ().serializeSpecialFloatingPointValues ().create ();

	/* ---- numbers ---- */

	/** Rounds a value to {@link #DECIMALS} decimals (integral values are written as integers). */
	static public Number num (double v)
	{
		double	f = Math.pow (10.0, DECIMALS);
		double	r = Math.round (v * f) / f;
		if (Double.isNaN (v) || Double.isInfinite (v))		return Double.valueOf (v);
		if ((r == Math.rint (r)) && (Math.abs (r) < 1e15))	return Long.valueOf ((long) r);
		return Double.valueOf (r);
	}

	static public double getDouble (JsonObject o, String key, double def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsDouble () : def;
	}

	static public double getDouble (JsonObject o, String key)
	{
		JsonElement	e = o.get (key);
		if ((e == null) || !e.isJsonPrimitive ())
			throw new IllegalArgumentException ("World: missing number \"" + key + "\" in " + o);
		return e.getAsDouble ();
	}

	static public String getString (JsonObject o, String key, String def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsString () : def;
	}

	static public boolean getBoolean (JsonObject o, String key, boolean def)
	{
		JsonElement	e = o.get (key);
		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsBoolean () : def;
	}

	/** The array under a key, or an empty one when absent. */
	static public JsonArray getArray (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
	}

	/** The object under a key, or an empty one when absent. */
	static public JsonObject getObject (JsonObject o, String key)
	{
		JsonElement	e = (o != null) ? o.get (key) : null;
		return ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
	}

	/* ---- geometry ---- */

	/** {x, y, z} */
	static public JsonObject point (double x, double y, double z)
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("x", num (x));
		o.addProperty ("y", num (y));
		o.addProperty ("z", num (z));
		return o;
	}

	static public JsonObject point (Point3 p)			{ return point (p.x (), p.y (), p.z ()); }
	static public JsonObject point (Point2 p)			{ return point (p.x (), p.y (), z (p)); }

	/** Adds x, y, z to an existing object. */
	static public void putPoint (JsonObject o, double x, double y, double z)
	{
		o.addProperty ("x", num (x));
		o.addProperty ("y", num (y));
		o.addProperty ("z", num (z));
	}

	static public Point3 toPoint (JsonObject o)
	{
		return new Point3 (getDouble (o, "x"), getDouble (o, "y"), getDouble (o, "z", 0.0));
	}

	/** {x1, y1, z1, x2, y2, z2} */
	static public JsonObject line (Line2 l)
	{
		JsonObject	o = new JsonObject ();
		putLine (o, l);
		return o;
	}

	/** Adds x1..z2 to an existing object. */
	static public void putLine (JsonObject o, Line2 l)
	{
		o.addProperty ("x1", num (l.orig ().x ()));
		o.addProperty ("y1", num (l.orig ().y ()));
		o.addProperty ("z1", num (l.z1 ()));
		o.addProperty ("x2", num (l.dest ().x ()));
		o.addProperty ("y2", num (l.dest ().y ()));
		o.addProperty ("z2", num (l.z2 ()));
	}

	static public Line2 toLine (JsonObject o)
	{
		return new Line2 (getDouble (o, "x1"), getDouble (o, "y1"), getDouble (o, "z1", 0.0),
						  getDouble (o, "x2"), getDouble (o, "y2"), getDouble (o, "z2", 0.0));
	}

	/* ---- text ---- */

	/**
	 * Pretty-printed text with objects made only of primitives (points,
	 * segments, waypoints, ...) written on a single line, so that the file
	 * stays readable and compact.
	 */
	static public String toText (JsonElement e)
	{
		StringBuilder	sb = new StringBuilder ();
		write (sb, e, 0);
		sb.append ('\n');
		return sb.toString ();
	}

	static private boolean isLeaf (JsonElement e)
	{
		if (!e.isJsonObject ())		return false;
		for (Map.Entry<String, JsonElement> en : e.getAsJsonObject ().entrySet ())
			if (!en.getValue ().isJsonPrimitive () && !en.getValue ().isJsonNull ())		return false;
		return true;
	}

	static private void indent (StringBuilder sb, int level)
	{
		for (int i = 0; i < level; i++)		sb.append ("  ");
	}

	static private void write (StringBuilder sb, JsonElement e, int level)
	{
		if (e.isJsonObject ())
		{
			JsonObject	o = e.getAsJsonObject ();
			if (o.size () == 0)		{ sb.append ("{}"); return; }
			if (isLeaf (o))
			{
				sb.append ("{ ");
				boolean	first = true;
				for (Map.Entry<String, JsonElement> en : o.entrySet ())
				{
					if (!first)		sb.append (", ");
					first = false;
					sb.append (GSON.toJson (en.getKey ())).append (": ").append (GSON.toJson (en.getValue ()));
				}
				sb.append (" }");
				return;
			}
			sb.append ("{\n");
			boolean	first = true;
			for (Map.Entry<String, JsonElement> en : o.entrySet ())
			{
				if (!first)		sb.append (",\n");
				first = false;
				indent (sb, level + 1);
				sb.append (GSON.toJson (en.getKey ())).append (": ");
				write (sb, en.getValue (), level + 1);
			}
			sb.append ('\n');
			indent (sb, level);
			sb.append ('}');
		}
		else if (e.isJsonArray ())
		{
			JsonArray	a = e.getAsJsonArray ();
			if (a.size () == 0)		{ sb.append ("[]"); return; }
			sb.append ("[\n");
			for (int i = 0; i < a.size (); i++)
			{
				if (i > 0)		sb.append (",\n");
				indent (sb, level + 1);
				write (sb, a.get (i), level + 1);
			}
			sb.append ('\n');
			indent (sb, level);
			sb.append (']');
		}
		else
			sb.append (GSON.toJson (e));
	}

	static public JsonObject parse (String text)
	{
		JsonElement	e = JsonParser.parseString (text);
		if (!e.isJsonObject ())		throw new IllegalArgumentException ("World: the JSON text is not an object");
		return e.getAsJsonObject ();
	}
}
