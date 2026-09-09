/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import devices.pos.Position;
import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.WMDock;
import tc.shared.world.WMDoor;
import tc.shared.world.WMFArea;
import tc.shared.world.WMObject;
import tc.shared.world.WMWall;
import tc.shared.world.WMWaypoint;
import tc.shared.world.WMZone;
import tc.shared.world.World;
import wucore.utils.color.ColorTool;
import wucore.utils.geom.Ellipse2;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;
import wucore.utils.geom.Polygon2;

/**
 * Static helpers that let the editor treat every element of a {@link World}
 * uniformly: creation, deletion, hit-testing, dragging (whole element or one of
 * its handles) and a name/value property view. It also provides text snapshots
 * of the whole world, used for undo/redo.
 */
public final class WorldEdit
{
	static public final double		ARROW		= 0.5;		// Length of orientation handles (m)

	private WorldEdit () { }

	/* ------------------------------------------------------------------ */
	/* World creation and snapshots                                        */
	/* ------------------------------------------------------------------ */

	/** A world with all its collections created but empty. */
	static public World newWorld ()
	{
		return new World (new Properties ());
	}

	/** Serialises the world to the .world property format (in memory). */
	static public String snapshot (World w)
	{
		Properties		props = new Properties ();

		props.setProperty ("START", w.start_x () + ", " + w.start_y () + ", " + Math.toDegrees (w.start_a ()));
		w.path ().toProperties (props);
		w.walls ().toProperties (props);
		w.objects ().toProperties (props);
		w.zones ().toProperties (props);
		w.fareas ().toProperties (props);
		w.doors ().toProperties (props);
		w.wps ().toProperties (props);
		w.docks ().toProperties (props);
		w.beacons ().toProperties (props);
		w.cbeacons ().toProperties (props);

		// toProperties() does not save the default values, toFile() does
		props.setProperty ("LINE_DEF_WIDTH", Double.toString (w.walls ().defaultWidth ()));
		props.setProperty ("LINE_DEF_HEIGHT", Double.toString (w.walls ().defaultHeight ()));
		props.setProperty ("LINE_DEF_TEXTURE", w.walls ().defaultTexture ());
		props.setProperty ("DOOR_DEF_WIDTH", Double.toString (w.doors ().defaultWidth ()));
		props.setProperty ("DOOR_DEF_HEIGHT", Double.toString (w.doors ().defaultHeight ()));
		props.setProperty ("DOOR_DEF_TEXTURE", w.doors ().defaultTexture ());
		props.setProperty ("ZONE_DEF_TEXTURE", w.zones ().defaultTexture ());
		props.setProperty ("FAREA_DEF_TEXTURE", w.fareas ().defaultTexture ());

		try
		{
			StringWriter	out = new StringWriter ();
			props.store (out, null);
			return out.toString ();
		} catch (Exception e) { return ""; }
	}

	/** Rebuilds a world from a {@link #snapshot(World)} string. */
	static public World restore (String snapshot)
	{
		Properties		props = new Properties ();

		try { props.load (new StringReader (snapshot)); } catch (Exception e) { }
		return new World (props);
	}

	/* ------------------------------------------------------------------ */
	/* Generic access                                                      */
	/* ------------------------------------------------------------------ */

	static public int count (World w, int kind)
	{
		switch (kind)
		{
		case WorldItem.ZONE:		return w.zones ().n ();
		case WorldItem.FAREA:		return w.fareas ().n ();
		case WorldItem.PATH:		return w.path ().n ();
		case WorldItem.WALL:		return w.walls ().n ();
		case WorldItem.OBJECT:		return w.objects ().n ();
		case WorldItem.DOOR:		return w.doors ().n ();
		case WorldItem.BEACON:		return w.beacons ().n ();
		case WorldItem.CBEACON:		return w.cbeacons ().n ();
		case WorldItem.WAYPOINT:	return w.wps ().n ();
		case WorldItem.DOCK:		return w.docks ().n ();
		case WorldItem.START:
		case WorldItem.DEFAULTS:	return 1;
		}
		return 0;
	}

	static public boolean valid (World w, WorldItem it)
	{
		return (it != null) && (it.index >= 0) && (it.index < count (w, it.kind));
	}

	/** Short text used in the element tree. */
	static public String describe (World w, WorldItem it)
	{
		if (!valid (w, it))				return "?";

		switch (it.kind)
		{
		case WorldItem.ZONE:		return w.zones ().at (it.index).label;
		case WorldItem.FAREA:		return w.fareas ().at (it.index).label;
		case WorldItem.PATH:		return "P" + it.index + " (" + fmt (w.path ().at (it.index).x ()) + ", " + fmt (w.path ().at (it.index).y ()) + ")";
		case WorldItem.WALL:
		{
			Line2	l = w.walls ().at (it.index).edge;
			return "LINE_" + it.index + " (" + fmt (l.orig ().x ()) + ", " + fmt (l.orig ().y ()) + ") - (" + fmt (l.dest ().x ()) + ", " + fmt (l.dest ().y ()) + ")";
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			return "OBJECT_" + it.index + ((o.shape != null) ? " " + shortName (o.shape) : "");
		}
		case WorldItem.DOOR:		return w.doors ().at (it.index).label;
		case WorldItem.BEACON:		return w.beacons ().at (it.index).label;
		case WorldItem.CBEACON:		return w.cbeacons ().at (it.index).label;
		case WorldItem.WAYPOINT:	return w.wps ().at (it.index).label;
		case WorldItem.DOCK:		return w.docks ().at (it.index).label;
		case WorldItem.START:		return "START (" + fmt (w.start_x ()) + ", " + fmt (w.start_y ()) + ", " + fmt (Math.toDegrees (w.start_a ())) + "º)";
		case WorldItem.DEFAULTS:	return "Default values";
		}
		return "?";
	}

	static private String shortName (String path)
	{
		int		i = path.lastIndexOf ('/');
		return (i >= 0) ? path.substring (i + 1) : path;
	}

	static public String fmt (double v)
	{
		if (Math.abs (v - Math.rint (v)) < 1e-9)
			return Long.toString (Math.round (v));
		String s = String.format (Locale.US, "%.3f", v);
		while (s.endsWith ("0"))		s = s.substring (0, s.length () - 1);
		return s;
	}

	/* ------------------------------------------------------------------ */
	/* Creation and deletion                                               */
	/* ------------------------------------------------------------------ */

	/** A label not used by any zone, door, waypoint or dock (they share a namespace in World.getType). */
	static public String uniqueLabel (World w, String prefix)
	{
		int		i = 0;
		while (true)
		{
			String	name = prefix + i;
			if ((w.getType (name) == World.NONE) && (indexOfLabel (w, WorldItem.BEACON, name) < 0)
					&& (indexOfLabel (w, WorldItem.CBEACON, name) < 0) && (indexOfLabel (w, WorldItem.FAREA, name) < 0))
				return name;
			i++;
		}
	}

	static private int indexOfLabel (World w, int kind, String label)
	{
		int		n = count (w, kind);
		for (int i = 0; i < n; i++)
		{
			String	l = label (w, new WorldItem (kind, i));
			if (label.equals (l))		return i;
		}
		return -1;
	}

	static public String label (World w, WorldItem it)
	{
		switch (it.kind)
		{
		case WorldItem.ZONE:		return w.zones ().at (it.index).label;
		case WorldItem.FAREA:		return w.fareas ().at (it.index).label;
		case WorldItem.DOOR:		return w.doors ().at (it.index).label;
		case WorldItem.BEACON:		return w.beacons ().at (it.index).label;
		case WorldItem.CBEACON:		return w.cbeacons ().at (it.index).label;
		case WorldItem.WAYPOINT:	return w.wps ().at (it.index).label;
		case WorldItem.DOCK:		return w.docks ().at (it.index).label;
		}
		return null;
	}

	static public WorldItem addWall (World w, double x1, double y1, double x2, double y2)
	{
		WMWall		wall = new WMWall ();
		wall.edge		= new Line2 (x1, y1, x2, y2);
		wall.width		= w.walls ().defaultWidth ();
		wall.height		= w.walls ().defaultHeight ();
		wall.texture	= w.walls ().defaultTexture ();
		wall.label		= "LINE_" + w.walls ().n ();
		w.walls ().add (wall);
		return new WorldItem (WorldItem.WALL, w.walls ().n () - 1);
	}

	static public WorldItem addDoor (World w, double x1, double y1, double x2, double y2)
	{
		WMDoor		door = new WMDoor ();
		door.edge		= new Line2 (x1, y1, x2, y2);
		door.path		= new Line2 (x1, y1, x2, y2);
		door.width		= w.doors ().defaultWidth ();
		door.height		= w.doors ().defaultHeight ();
		door.texture	= w.doors ().defaultTexture ();
		door.label		= uniqueLabel (w, "Door");
		w.doors ().add (door);
		return new WorldItem (WorldItem.DOOR, w.doors ().n () - 1);
	}

	static public WorldItem addZone (World w, double x1, double y1, double x2, double y2)
	{
		WMZone		zone = new WMZone ();
		zone.area		= new java.awt.geom.Rectangle2D.Double (Math.min (x1, x2), Math.min (y1, y2), Math.abs (x2 - x1), Math.abs (y2 - y1));
		zone.texture	= w.zones ().defaultTexture ();
		zone.label		= uniqueLabel (w, "Zone");
		w.zones ().add (zone);
		return new WorldItem (WorldItem.ZONE, w.zones ().n () - 1);
	}

	static public WorldItem addFArea (World w, List<Point2> pts)
	{
		WMFArea		fa = new WMFArea ();
		fa.polygon		= new Polygon2 ();
		for (Point2 p : pts)
			fa.polygon.addPoint (p.x (), p.y ());
		fa.texture		= w.fareas ().defaultTexture ();
		fa.label		= uniqueLabel (w, "FArea");
		w.fareas ().add (fa);
		return new WorldItem (WorldItem.FAREA, w.fareas ().n () - 1);
	}

	/** Object with a square icon of side <code>size</code> centred at (x, y). */
	static public WorldItem addObject (World w, double x, double y, double size)
	{
		WMObject	obj = new WMObject ();
		double		h = size / 2.0;
		obj.pos			= new Point3 (x, y, 0.0);
		obj.a			= 0.0;
		obj.shape		= null;
		obj.color		= ColorTool.getColorFromName ("gray_dark");
		obj.usecolor	= false;
		obj.visible		= true;
		obj.label		= "OBJECT_" + w.objects ().n ();
		obj.icon		= new Line2[] {
			new Line2 (x - h, y - h, x + h, y - h), new Line2 (x + h, y - h, x + h, y + h),
			new Line2 (x + h, y + h, x - h, y + h), new Line2 (x - h, y + h, x - h, y - h) };
		w.objects ().add (obj);
		return new WorldItem (WorldItem.OBJECT, w.objects ().n () - 1);
	}

	static public WorldItem addWaypoint (World w, double x, double y)
	{
		w.wps ().add (new WMWaypoint (new Position (x, y, 0.0), uniqueLabel (w, "wp")));
		return new WorldItem (WorldItem.WAYPOINT, w.wps ().n () - 1);
	}

	static public WorldItem addDock (World w, double x, double y)
	{
		w.docks ().add (new WMDock (new Position (x, y, 0.0, 0.0), uniqueLabel (w, "dock")));
		return new WorldItem (WorldItem.DOCK, w.docks ().n () - 1);
	}

	static public WorldItem addBeacon (World w, double x, double y)
	{
		w.beacons ().add (new WMBeacon (uniqueLabel (w, "b"), new Position (x, y, 0.0), 0.2));
		return new WorldItem (WorldItem.BEACON, w.beacons ().n () - 1);
	}

	static public WorldItem addCBeacon (World w, double x, double y)
	{
		w.cbeacons ().add (new WMCBeacon (new Ellipse2 (x, y, 0.05, 0.05), uniqueLabel (w, "cb")));
		return new WorldItem (WorldItem.CBEACON, w.cbeacons ().n () - 1);
	}

	static public WorldItem addPathPoint (World w, double x, double y)
	{
		w.path ().add (new Point2 (x, y));
		return new WorldItem (WorldItem.PATH, w.path ().n () - 1);
	}

	static public boolean remove (World w, WorldItem it)
	{
		if (!valid (w, it))				return false;

		switch (it.kind)
		{
		case WorldItem.ZONE:		w.zones ().remove (it.index);		return true;
		case WorldItem.FAREA:		w.fareas ().remove (it.index);		return true;
		case WorldItem.PATH:		w.path ().remove (it.index);		return true;
		case WorldItem.WALL:		w.walls ().remove (it.index);		return true;
		case WorldItem.OBJECT:		w.objects ().remove (it.index);		return true;
		case WorldItem.DOOR:		w.doors ().remove (it.index);		return true;
		case WorldItem.BEACON:		w.beacons ().remove (it.index);		return true;
		case WorldItem.CBEACON:		w.cbeacons ().remove (it.index);	return true;
		case WorldItem.WAYPOINT:	w.wps ().remove (it.index);			return true;
		case WorldItem.DOCK:		w.docks ().remove (it.index);		return true;
		}
		return false;		// START and DEFAULTS cannot be removed
	}

	/* ------------------------------------------------------------------ */
	/* Geometry: hit testing, translation, handles                         */
	/* ------------------------------------------------------------------ */

	static public double segDist (Line2 l, double x, double y)
	{
		return segDist (l.orig ().x (), l.orig ().y (), l.dest ().x (), l.dest ().y (), x, y);
	}

	static public double segDist (double x1, double y1, double x2, double y2, double x, double y)
	{
		double		dx = x2 - x1, dy = y2 - y1;
		double		l2 = dx * dx + dy * dy;
		double		t = 0.0;
		if (l2 > 1e-12)
			t = Math.max (0.0, Math.min (1.0, ((x - x1) * dx + (y - y1) * dy) / l2));
		double		px = x1 + t * dx, py = y1 + t * dy;
		return Math.hypot (x - px, y - py);
	}

	/**
	 * Distance from (x, y) to the element. For areas (zones, forbidden areas)
	 * it is zero when the point is inside.
	 */
	static public double distance (World w, WorldItem it, double x, double y)
	{
		if (!valid (w, it))				return Double.MAX_VALUE;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (z.area.contains (x, y))		return 0.0;
			double	d = Double.MAX_VALUE;
			for (Line2 l : z.toLines ())	d = Math.min (d, segDist (l, x, y));
			return d;
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			if (p.contains (x, y))			return 0.0;
			double	d = Double.MAX_VALUE;
			for (int i = 0; i < p.npoints; i++)
			{
				int	j = (i + 1) % p.npoints;
				d = Math.min (d, segDist (p.xpoints[i], p.ypoints[i], p.xpoints[j], p.ypoints[j], x, y));
			}
			return d;
		}
		case WorldItem.PATH:		return w.path ().at (it.index).distance (x, y);
		case WorldItem.WALL:		return segDist (w.walls ().at (it.index).edge, x, y);
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			double	d = o.pos.distance (x, y);
			for (Line2 l : o.icon)			d = Math.min (d, segDist (l, x, y));
			return d;
		}
		case WorldItem.DOOR:
		{
			WMDoor	dr = w.doors ().at (it.index);
			return Math.min (segDist (dr.edge, x, y), segDist (dr.path, x, y));
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().at (it.index);
			return Math.min (b.pos.distance (x, y), segDist (b.getLine (), x, y));
		}
		case WorldItem.CBEACON:
		{
			Ellipse2	e = w.cbeacons ().at (it.index).beacon;
			return Math.max (0.0, e.center ().distance (x, y) - Math.max (e.horiz (), e.vert ()));
		}
		case WorldItem.WAYPOINT:	return w.wps ().at (it.index).pos.distance (x, y);
		case WorldItem.DOCK:		return w.docks ().at (it.index).pos.distance (x, y);
		case WorldItem.START:		return Math.hypot (w.start_x () - x, w.start_y () - y);
		}
		return Double.MAX_VALUE;
	}

	/** Kinds in picking priority (small things first, areas last). */
	static public final int[]	PICK_ORDER	= {
		WorldItem.START, WorldItem.WAYPOINT, WorldItem.DOCK, WorldItem.PATH, WorldItem.CBEACON, WorldItem.BEACON,
		WorldItem.DOOR, WorldItem.WALL, WorldItem.OBJECT, WorldItem.FAREA, WorldItem.ZONE
	};

	/**
	 * Nearest element to (x, y) within <code>tol</code> metres, honouring
	 * PICK_ORDER and the visibility mask (one boolean per kind).
	 */
	static public WorldItem pick (World w, double x, double y, double tol, boolean[] visible)
	{
		for (int k = 0; k < PICK_ORDER.length; k++)
		{
			int			kind = PICK_ORDER[k];
			if ((visible != null) && !visible[kind])		continue;

			int			n = count (w, kind);
			double		best = tol;
			int			bi = -1;
			for (int i = 0; i < n; i++)
			{
				double	d = distance (w, new WorldItem (kind, i), x, y);
				if (d < best) { best = d; bi = i; }
			}
			// areas: only accept "inside" or near-border hits
			if (bi >= 0)			return new WorldItem (kind, bi);
		}
		return null;
	}

	static public void translate (World w, WorldItem it, double dx, double dy)
	{
		if (!valid (w, it))				return;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			z.area.setRect (z.area.getX () + dx, z.area.getY () + dy, z.area.getWidth (), z.area.getHeight ());
			break;
		}
		case WorldItem.FAREA:		w.fareas ().at (it.index).polygon.translate (dx, dy);	break;
		case WorldItem.PATH:		w.path ().at (it.index).add (dx, dy);					break;
		case WorldItem.WALL:		moveLine (w.walls ().at (it.index).edge, dx, dy);	w.walls ().recomputeBounds ();	break;
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			o.pos = new Point3 (o.pos.x () + dx, o.pos.y () + dy, o.pos.z ());
			for (Line2 l : o.icon)		moveLine (l, dx, dy);
			break;
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = w.doors ().at (it.index);
			moveLine (d.edge, dx, dy);
			moveLine (d.path, dx, dy);
			break;
		}
		case WorldItem.BEACON:
		{
			Position	p = w.beacons ().at (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.CBEACON:
		{
			Ellipse2	e = w.cbeacons ().at (it.index).beacon;
			e.set (e.center ().x () + dx, e.center ().y () + dy, e.horiz (), e.vert ());
			break;
		}
		case WorldItem.WAYPOINT:
		{
			Position	p = w.wps ().at (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.DOCK:
		{
			Position	p = w.docks ().at (it.index).pos;
			p.x (p.x () + dx);	p.y (p.y () + dy);
			break;
		}
		case WorldItem.START:		w.setStart (w.start_x () + dx, w.start_y () + dy, w.start_a ());	break;
		}
	}

	static private void moveLine (Line2 l, double dx, double dy)
	{
		l.set (l.orig ().x () + dx, l.orig ().y () + dy, l.dest ().x () + dx, l.dest ().y () + dy);
	}

	/**
	 * Control points of the element. Dragging one of them with
	 * {@link #setHandle} reshapes the element; the last handle of oriented
	 * elements (waypoints, docks, start, objects) is the orientation arrow tip.
	 */
	static public Point2[] handles (World w, WorldItem it)
	{
		if (!valid (w, it))				return new Point2[0];

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			java.awt.geom.Rectangle2D	r = w.zones ().at (it.index).area;
			return new Point2[] { new Point2 (r.getMinX (), r.getMinY ()), new Point2 (r.getMaxX (), r.getMinY ()),
								  new Point2 (r.getMaxX (), r.getMaxY ()), new Point2 (r.getMinX (), r.getMaxY ()) };
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			Point2[]	h = new Point2[p.npoints];
			for (int i = 0; i < p.npoints; i++)		h[i] = new Point2 (p.xpoints[i], p.ypoints[i]);
			return h;
		}
		case WorldItem.PATH:		return new Point2[] { new Point2 (w.path ().at (it.index)) };
		case WorldItem.WALL:
		{
			Line2	l = w.walls ().at (it.index).edge;
			return new Point2[] { new Point2 (l.orig ()), new Point2 (l.dest ()) };
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			return new Point2[] { new Point2 (o.pos.x (), o.pos.y ()), arrow (o.pos.x (), o.pos.y (), o.a) };
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = w.doors ().at (it.index);
			return new Point2[] { new Point2 (d.edge.orig ()), new Point2 (d.edge.dest ()), new Point2 (d.path.orig ()), new Point2 (d.path.dest ()) };
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().at (it.index);
			return new Point2[] { new Point2 (b.pos.x (), b.pos.y ()), new Point2 (b.getLine ().dest ()) };
		}
		case WorldItem.CBEACON:
		{
			Ellipse2	e = w.cbeacons ().at (it.index).beacon;
			return new Point2[] { new Point2 (e.center ()), new Point2 (e.center ().x () + e.horiz (), e.center ().y ()) };
		}
		case WorldItem.WAYPOINT:
		{
			Position	p = w.wps ().at (it.index).pos;
			return new Point2[] { new Point2 (p.x (), p.y ()), arrow (p.x (), p.y (), p.alpha ()) };
		}
		case WorldItem.DOCK:
		{
			Position	p = w.docks ().at (it.index).pos;
			return new Point2[] { new Point2 (p.x (), p.y ()), arrow (p.x (), p.y (), p.alpha ()) };
		}
		case WorldItem.START:		return new Point2[] { new Point2 (w.start_x (), w.start_y ()), arrow (w.start_x (), w.start_y (), w.start_a ()) };
		}
		return new Point2[0];
	}

	static private Point2 arrow (double x, double y, double a)
	{
		return new Point2 (x + ARROW * Math.cos (a), y + ARROW * Math.sin (a));
	}

	/** Moves handle <code>h</code> of the element to (x, y). */
	static public void setHandle (World w, WorldItem it, int h, double x, double y)
	{
		if (!valid (w, it))				return;

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			java.awt.geom.Rectangle2D.Double	r = w.zones ().at (it.index).area;
			// opposite corner stays fixed
			double	ox = ((h == 0) || (h == 3)) ? r.getMaxX () : r.getMinX ();
			double	oy = ((h == 0) || (h == 1)) ? r.getMaxY () : r.getMinY ();
			r.setRect (Math.min (ox, x), Math.min (oy, y), Math.abs (ox - x), Math.abs (oy - y));
			break;
		}
		case WorldItem.FAREA:
		{
			Polygon2	p = w.fareas ().at (it.index).polygon;
			if ((h >= 0) && (h < p.npoints)) { p.xpoints[h] = x; p.ypoints[h] = y; }
			break;
		}
		case WorldItem.PATH:		w.path ().at (it.index).set (x, y);		break;
		case WorldItem.WALL:
		{
			Line2	l = w.walls ().at (it.index).edge;
			if (h == 0)		l.set (x, y, l.dest ().x (), l.dest ().y ());
			else			l.set (l.orig ().x (), l.orig ().y (), x, y);
			w.walls ().recomputeBounds ();
			break;
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			if (h == 0)		translate (w, it, x - o.pos.x (), y - o.pos.y ());
			else			setObjectPose (o, o.pos.x (), o.pos.y (), o.pos.z (), Math.atan2 (y - o.pos.y (), x - o.pos.x ()));
			break;
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = w.doors ().at (it.index);
			Line2	l = (h < 2) ? d.edge : d.path;
			if ((h % 2) == 0)	l.set (x, y, l.dest ().x (), l.dest ().y ());
			else				l.set (l.orig ().x (), l.orig ().y (), x, y);
			break;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().at (it.index);
			if (h == 0)		{ b.pos.x (x);	b.pos.y (y); }
			else
			{
				b.pos.alpha (Math.atan2 (y - b.pos.y (), x - b.pos.x ()));
				b.width = 2.0 * b.pos.distance (x, y);
			}
			break;
		}
		case WorldItem.CBEACON:
		{
			Ellipse2	e = w.cbeacons ().at (it.index).beacon;
			if (h == 0)		e.set (x, y, e.horiz (), e.vert ());
			else
			{
				double	r = Math.max (0.005, e.center ().distance (x, y));
				e.set (e.center ().x (), e.center ().y (), r, r);
			}
			break;
		}
		case WorldItem.WAYPOINT:
		case WorldItem.DOCK:
		{
			Position	p = (it.kind == WorldItem.WAYPOINT) ? w.wps ().at (it.index).pos : w.docks ().at (it.index).pos;
			if (h == 0)		{ p.x (x);	p.y (y); }
			else			p.alpha (Math.atan2 (y - p.y (), x - p.x ()));
			break;
		}
		case WorldItem.START:
			if (h == 0)		w.setStart (x, y, w.start_a ());
			else			w.setStart (w.start_x (), w.start_y (), Math.atan2 (y - w.start_y (), x - w.start_x ()));
			break;
		}
	}

	/** Re-places an object keeping its local icon shape. */
	static public void setObjectPose (WMObject o, double x, double y, double z, double a)
	{
		Line2[]		local = o.getLocalIcon ();
		o.icon	= local;
		o.pos	= new Point3 (x, y, z);
		o.a		= a;
		o.AbsIcon ();
	}

	/** Bounding box of everything in the world: {minx, miny, maxx, maxy}, or null if empty. */
	static public double[] bounds (World w)
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		boolean		any = false;

		for (int kind = 0; kind < WorldItem.DEFAULTS; kind++)
		{
			int		n = count (w, kind);
			for (int i = 0; i < n; i++)
			{
				WorldItem	it = new WorldItem (kind, i);
				Point2[]	hs = handles (w, it);
				if (kind == WorldItem.OBJECT)
				{
					hs = new Point2[0];
					for (Line2 l : w.objects ().at (i).icon)
						hs = concat (hs, new Point2[] { l.orig (), l.dest () });
				}
				for (Point2 p : hs)
				{
					b[0] = Math.min (b[0], p.x ());	b[1] = Math.min (b[1], p.y ());
					b[2] = Math.max (b[2], p.x ());	b[3] = Math.max (b[3], p.y ());
					any = true;
				}
			}
		}
		return any ? b : null;
	}

	static private Point2[] concat (Point2[] a, Point2[] b)
	{
		Point2[]	r = new Point2[a.length + b.length];
		System.arraycopy (a, 0, r, 0, a.length);
		System.arraycopy (b, 0, r, a.length, b.length);
		return r;
	}

	/* ------------------------------------------------------------------ */
	/* Property view (name / value strings)                                */
	/* ------------------------------------------------------------------ */

	static public String[] propertyNames (World w, WorldItem it)
	{
		if (!valid (w, it))				return new String[0];

		switch (it.kind)
		{
		case WorldItem.ZONE:		return new String[] { "label", "x", "y", "width", "height", "texture" };
		case WorldItem.FAREA:		return new String[] { "label", "texture", "points" };
		case WorldItem.PATH:		return new String[] { "x", "y" };
		case WorldItem.WALL:		return new String[] { "x1", "y1", "x2", "y2", "width", "height", "texture" };
		case WorldItem.OBJECT:		return new String[] { "x", "y", "z", "angle", "shape", "color", "usecolor", "icon" };
		case WorldItem.DOOR:		return new String[] { "label", "x1", "y1", "x2", "y2", "path x1", "path y1", "path x2", "path y2", "width", "height", "texture" };
		case WorldItem.BEACON:		return new String[] { "label", "x", "y", "angle", "width" };
		case WorldItem.CBEACON:		return new String[] { "label", "x", "y", "horiz", "vert" };
		case WorldItem.WAYPOINT:	return new String[] { "label", "x", "y", "angle" };
		case WorldItem.DOCK:		return new String[] { "label", "x", "y", "z", "angle" };
		case WorldItem.START:		return new String[] { "x", "y", "angle" };
		case WorldItem.DEFAULTS:	return new String[] { "wall width", "wall height", "wall texture", "door width", "door height", "door texture", "zone texture", "farea texture" };
		}
		return new String[0];
	}

	static public String getProperty (World w, WorldItem it, String name)
	{
		if (!valid (w, it))				return "";

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (name.equals ("label"))		return z.label;
			if (name.equals ("x"))			return fmt (z.area.getX ());
			if (name.equals ("y"))			return fmt (z.area.getY ());
			if (name.equals ("width"))		return fmt (z.area.getWidth ());
			if (name.equals ("height"))		return fmt (z.area.getHeight ());
			if (name.equals ("texture"))	return z.texture;
			break;
		}
		case WorldItem.FAREA:
		{
			WMFArea	f = w.fareas ().at (it.index);
			if (name.equals ("label"))		return f.label;
			if (name.equals ("texture"))	return f.texture;
			if (name.equals ("points"))
			{
				StringBuffer	sb = new StringBuffer ();
				for (int i = 0; i < f.polygon.npoints; i++)
					sb.append ((i > 0) ? "; " : "").append (fmt (f.polygon.xpoints[i])).append (", ").append (fmt (f.polygon.ypoints[i]));
				return sb.toString ();
			}
			break;
		}
		case WorldItem.PATH:
		{
			Point2	p = w.path ().at (it.index);
			if (name.equals ("x"))			return fmt (p.x ());
			if (name.equals ("y"))			return fmt (p.y ());
			break;
		}
		case WorldItem.WALL:
		{
			WMWall	wl = w.walls ().at (it.index);
			if (name.equals ("x1"))			return fmt (wl.edge.orig ().x ());
			if (name.equals ("y1"))			return fmt (wl.edge.orig ().y ());
			if (name.equals ("x2"))			return fmt (wl.edge.dest ().x ());
			if (name.equals ("y2"))			return fmt (wl.edge.dest ().y ());
			if (name.equals ("width"))		return fmt (wl.width);
			if (name.equals ("height"))		return fmt (wl.height);
			if (name.equals ("texture"))	return wl.texture;
			break;
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			if (name.equals ("x"))			return fmt (o.pos.x ());
			if (name.equals ("y"))			return fmt (o.pos.y ());
			if (name.equals ("z"))			return fmt (o.pos.z ());
			if (name.equals ("angle"))		return fmt (Math.toDegrees (o.a));
			if (name.equals ("shape"))		return (o.shape == null) ? "" : o.shape;
			if (name.equals ("color"))		return ColorTool.getNameFromColor (o.color);
			if (name.equals ("usecolor"))	return Boolean.toString (o.usecolor);
			if (name.equals ("icon"))
			{
				StringBuffer	sb = new StringBuffer ();
				Line2[]			local = o.getLocalIcon ();
				for (int i = 0; i < local.length; i++)
					sb.append ((i > 0) ? "; " : "").append (fmt (local[i].orig ().x ())).append (", ").append (fmt (local[i].orig ().y ()))
					  .append (", ").append (fmt (local[i].dest ().x ())).append (", ").append (fmt (local[i].dest ().y ()));
				return sb.toString ();
			}
			break;
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = w.doors ().at (it.index);
			if (name.equals ("label"))		return d.label;
			if (name.equals ("x1"))			return fmt (d.edge.orig ().x ());
			if (name.equals ("y1"))			return fmt (d.edge.orig ().y ());
			if (name.equals ("x2"))			return fmt (d.edge.dest ().x ());
			if (name.equals ("y2"))			return fmt (d.edge.dest ().y ());
			if (name.equals ("path x1"))	return fmt (d.path.orig ().x ());
			if (name.equals ("path y1"))	return fmt (d.path.orig ().y ());
			if (name.equals ("path x2"))	return fmt (d.path.dest ().x ());
			if (name.equals ("path y2"))	return fmt (d.path.dest ().y ());
			if (name.equals ("width"))		return fmt (d.width);
			if (name.equals ("height"))		return fmt (d.height);
			if (name.equals ("texture"))	return d.texture;
			break;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().at (it.index);
			if (name.equals ("label"))		return b.label;
			if (name.equals ("x"))			return fmt (b.pos.x ());
			if (name.equals ("y"))			return fmt (b.pos.y ());
			if (name.equals ("angle"))		return fmt (Math.toDegrees (b.pos.alpha ()));
			if (name.equals ("width"))		return fmt (b.width);
			break;
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().at (it.index);
			if (name.equals ("label"))		return b.label;
			if (name.equals ("x"))			return fmt (b.beacon.center ().x ());
			if (name.equals ("y"))			return fmt (b.beacon.center ().y ());
			if (name.equals ("horiz"))		return fmt (b.beacon.horiz ());
			if (name.equals ("vert"))		return fmt (b.beacon.vert ());
			break;
		}
		case WorldItem.WAYPOINT:
		{
			WMWaypoint	p = w.wps ().at (it.index);
			if (name.equals ("label"))		return p.label;
			if (name.equals ("x"))			return fmt (p.pos.x ());
			if (name.equals ("y"))			return fmt (p.pos.y ());
			if (name.equals ("angle"))		return fmt (Math.toDegrees (p.pos.alpha ()));
			break;
		}
		case WorldItem.DOCK:
		{
			WMDock	d = w.docks ().at (it.index);
			if (name.equals ("label"))		return d.label;
			if (name.equals ("x"))			return fmt (d.pos.x ());
			if (name.equals ("y"))			return fmt (d.pos.y ());
			if (name.equals ("z"))			return fmt (d.pos.z ());
			if (name.equals ("angle"))		return fmt (Math.toDegrees (d.pos.alpha ()));
			break;
		}
		case WorldItem.START:
			if (name.equals ("x"))			return fmt (w.start_x ());
			if (name.equals ("y"))			return fmt (w.start_y ());
			if (name.equals ("angle"))		return fmt (Math.toDegrees (w.start_a ()));
			break;
		case WorldItem.DEFAULTS:
			if (name.equals ("wall width"))		return fmt (w.walls ().defaultWidth ());
			if (name.equals ("wall height"))	return fmt (w.walls ().defaultHeight ());
			if (name.equals ("wall texture"))	return w.walls ().defaultTexture ();
			if (name.equals ("door width"))		return fmt (w.doors ().defaultWidth ());
			if (name.equals ("door height"))	return fmt (w.doors ().defaultHeight ());
			if (name.equals ("door texture"))	return w.doors ().defaultTexture ();
			if (name.equals ("zone texture"))	return w.zones ().defaultTexture ();
			if (name.equals ("farea texture"))	return w.fareas ().defaultTexture ();
			break;
		}
		return "";
	}

	/**
	 * Applies a property value typed by the user.
	 * @throws IllegalArgumentException with a user-readable message if the value is not acceptable.
	 */
	static public void setProperty (World w, WorldItem it, String name, String value)
	{
		if (!valid (w, it))				return;
		value = value.trim ();

		switch (it.kind)
		{
		case WorldItem.ZONE:
		{
			WMZone	z = w.zones ().at (it.index);
			if (name.equals ("label"))			z.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			z.area.setRect (num (value), z.area.getY (), z.area.getWidth (), z.area.getHeight ());
			else if (name.equals ("y"))			z.area.setRect (z.area.getX (), num (value), z.area.getWidth (), z.area.getHeight ());
			else if (name.equals ("width"))		z.area.setRect (z.area.getX (), z.area.getY (), Math.abs (num (value)), z.area.getHeight ());
			else if (name.equals ("height"))	z.area.setRect (z.area.getX (), z.area.getY (), z.area.getWidth (), Math.abs (num (value)));
			else if (name.equals ("texture"))	z.texture = token (value);
			return;
		}
		case WorldItem.FAREA:
		{
			WMFArea	f = w.fareas ().at (it.index);
			if (name.equals ("label"))			f.label = checkLabel (w, it, value);
			else if (name.equals ("texture"))	f.texture = token (value);
			else if (name.equals ("points"))
			{
				double[]	v = nums (value);
				if ((v.length < 6) || (v.length % 2 != 0))
					throw new IllegalArgumentException ("At least 3 points, as: x1, y1; x2, y2; x3, y3");
				Polygon2	p = new Polygon2 ();
				for (int i = 0; i < v.length; i += 2)		p.addPoint (v[i], v[i + 1]);
				f.polygon = p;
			}
			return;
		}
		case WorldItem.PATH:
		{
			Point2	p = w.path ().at (it.index);
			if (name.equals ("x"))				p.x (num (value));
			else if (name.equals ("y"))			p.y (num (value));
			return;
		}
		case WorldItem.WALL:
		{
			WMWall	wl = w.walls ().at (it.index);
			Line2	l = wl.edge;
			if (name.equals ("x1"))				l.set (num (value), l.orig ().y (), l.dest ().x (), l.dest ().y ());
			else if (name.equals ("y1"))		l.set (l.orig ().x (), num (value), l.dest ().x (), l.dest ().y ());
			else if (name.equals ("x2"))		l.set (l.orig ().x (), l.orig ().y (), num (value), l.dest ().y ());
			else if (name.equals ("y2"))		l.set (l.orig ().x (), l.orig ().y (), l.dest ().x (), num (value));
			else if (name.equals ("width"))		wl.width = num (value);
			else if (name.equals ("height"))	wl.height = num (value);
			else if (name.equals ("texture"))	wl.texture = token (value);
			w.walls ().recomputeBounds ();
			return;
		}
		case WorldItem.OBJECT:
		{
			WMObject	o = w.objects ().at (it.index);
			if (name.equals ("x"))				setObjectPose (o, num (value), o.pos.y (), o.pos.z (), o.a);
			else if (name.equals ("y"))			setObjectPose (o, o.pos.x (), num (value), o.pos.z (), o.a);
			else if (name.equals ("z"))			setObjectPose (o, o.pos.x (), o.pos.y (), num (value), o.a);
			else if (name.equals ("angle"))		setObjectPose (o, o.pos.x (), o.pos.y (), o.pos.z (), Math.toRadians (num (value)));
			else if (name.equals ("shape"))		o.shape = (value.length () == 0) ? null : token (value);
			else if (name.equals ("color"))
			{
				try { o.color = ColorTool.getColorFromName (token (value)); }
				catch (Exception e) { throw new IllegalArgumentException ("Use a colour name (red, blue, gray_dark...) or r:g:b"); }
			}
			else if (name.equals ("usecolor"))	o.usecolor = bool (value);
			else if (name.equals ("icon"))
			{
				double[]	v = nums (value);
				if ((v.length < 4) || (v.length % 4 != 0))
					throw new IllegalArgumentException ("Segments in local coordinates, as: x1, y1, x2, y2; x1, y1, x2, y2; ...");
				Line2[]		local = new Line2[v.length / 4];
				for (int i = 0; i < local.length; i++)
					local[i] = new Line2 (v[4 * i], v[4 * i + 1], v[4 * i + 2], v[4 * i + 3]);
				o.icon = local;
				o.AbsIcon ();
			}
			return;
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = w.doors ().at (it.index);
			Line2	e = d.edge, p = d.path;
			if (name.equals ("label"))			d.label = checkLabel (w, it, value);
			else if (name.equals ("x1"))		e.set (num (value), e.orig ().y (), e.dest ().x (), e.dest ().y ());
			else if (name.equals ("y1"))		e.set (e.orig ().x (), num (value), e.dest ().x (), e.dest ().y ());
			else if (name.equals ("x2"))		e.set (e.orig ().x (), e.orig ().y (), num (value), e.dest ().y ());
			else if (name.equals ("y2"))		e.set (e.orig ().x (), e.orig ().y (), e.dest ().x (), num (value));
			else if (name.equals ("path x1"))	p.set (num (value), p.orig ().y (), p.dest ().x (), p.dest ().y ());
			else if (name.equals ("path y1"))	p.set (p.orig ().x (), num (value), p.dest ().x (), p.dest ().y ());
			else if (name.equals ("path x2"))	p.set (p.orig ().x (), p.orig ().y (), num (value), p.dest ().y ());
			else if (name.equals ("path y2"))	p.set (p.orig ().x (), p.orig ().y (), p.dest ().x (), num (value));
			else if (name.equals ("width"))		d.width = num (value);
			else if (name.equals ("height"))	d.height = num (value);
			else if (name.equals ("texture"))	d.texture = token (value);
			return;
		}
		case WorldItem.BEACON:
		{
			WMBeacon	b = w.beacons ().at (it.index);
			if (name.equals ("label"))			b.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			b.pos.x (num (value));
			else if (name.equals ("y"))			b.pos.y (num (value));
			else if (name.equals ("angle"))		b.pos.alpha (Math.toRadians (num (value)));
			else if (name.equals ("width"))		b.width = Math.abs (num (value));
			return;
		}
		case WorldItem.CBEACON:
		{
			WMCBeacon	b = w.cbeacons ().at (it.index);
			Ellipse2	e = b.beacon;
			if (name.equals ("label"))			b.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			e.set (num (value), e.center ().y (), e.horiz (), e.vert ());
			else if (name.equals ("y"))			e.set (e.center ().x (), num (value), e.horiz (), e.vert ());
			else if (name.equals ("horiz"))		e.set (e.center ().x (), e.center ().y (), Math.abs (num (value)), e.vert ());
			else if (name.equals ("vert"))		e.set (e.center ().x (), e.center ().y (), e.horiz (), Math.abs (num (value)));
			return;
		}
		case WorldItem.WAYPOINT:
		{
			WMWaypoint	p = w.wps ().at (it.index);
			if (name.equals ("label"))			p.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			p.pos.x (num (value));
			else if (name.equals ("y"))			p.pos.y (num (value));
			else if (name.equals ("angle"))		p.pos.alpha (Math.toRadians (num (value)));
			return;
		}
		case WorldItem.DOCK:
		{
			WMDock	d = w.docks ().at (it.index);
			if (name.equals ("label"))			d.label = checkLabel (w, it, value);
			else if (name.equals ("x"))			d.pos.x (num (value));
			else if (name.equals ("y"))			d.pos.y (num (value));
			else if (name.equals ("z"))			d.pos.z (num (value));
			else if (name.equals ("angle"))		d.pos.alpha (Math.toRadians (num (value)));
			return;
		}
		case WorldItem.START:
			if (name.equals ("x"))				w.setStart (num (value), w.start_y (), w.start_a ());
			else if (name.equals ("y"))			w.setStart (w.start_x (), num (value), w.start_a ());
			else if (name.equals ("angle"))		w.setStart (w.start_x (), w.start_y (), Math.toRadians (num (value)));
			return;
		case WorldItem.DEFAULTS:
			if (name.equals ("wall width"))			w.walls ().setDefaults (num (value), w.walls ().defaultHeight (), w.walls ().defaultTexture ());
			else if (name.equals ("wall height"))	w.walls ().setDefaults (w.walls ().defaultWidth (), num (value), w.walls ().defaultTexture ());
			else if (name.equals ("wall texture"))	w.walls ().setDefaults (w.walls ().defaultWidth (), w.walls ().defaultHeight (), token (value));
			else if (name.equals ("door width"))	w.doors ().setDefaults (num (value), w.doors ().defaultHeight (), w.doors ().defaultTexture ());
			else if (name.equals ("door height"))	w.doors ().setDefaults (w.doors ().defaultWidth (), num (value), w.doors ().defaultTexture ());
			else if (name.equals ("door texture"))	w.doors ().setDefaults (w.doors ().defaultWidth (), w.doors ().defaultHeight (), token (value));
			else if (name.equals ("zone texture"))	w.zones ().setDefaultTexture (token (value));
			else if (name.equals ("farea texture"))	w.fareas ().setDefaultTexture (token (value));
			return;
		}
	}

	/* Parsing helpers. The .world format separates fields with ", \t", so labels and textures cannot contain them. */

	static private double num (String s)
	{
		try { return Double.parseDouble (s.trim ()); }
		catch (NumberFormatException e) { throw new IllegalArgumentException ("Not a number: " + s); }
	}

	static private double[] nums (String s)
	{
		StringTokenizer		st = new StringTokenizer (s, ",; \t");
		List<Double>		v = new ArrayList<Double> ();
		while (st.hasMoreTokens ())		v.add (Double.valueOf (num (st.nextToken ())));
		double[]			r = new double[v.size ()];
		for (int i = 0; i < r.length; i++)		r[i] = v.get (i).doubleValue ();
		return r;
	}

	static private boolean bool (String s)
	{
		if (s.equalsIgnoreCase ("true") || s.equals ("1"))		return true;
		if (s.equalsIgnoreCase ("false") || s.equals ("0"))		return false;
		throw new IllegalArgumentException ("Expected true or false");
	}

	static private String token (String s)
	{
		if ((s.length () == 0) || (s.indexOf (',') >= 0) || (s.indexOf (' ') >= 0) || (s.indexOf ('\t') >= 0))
			throw new IllegalArgumentException ("Value cannot be empty or contain spaces or commas");
		return s;
	}

	static private String checkLabel (World w, WorldItem it, String s)
	{
		token (s);
		for (int kind = 0; kind < WorldItem.DEFAULTS; kind++)
		{
			int		n = count (w, kind);
			for (int i = 0; i < n; i++)
			{
				WorldItem	o = new WorldItem (kind, i);
				if (!o.equals (it) && s.equals (label (w, o)))
					throw new IllegalArgumentException ("Label '" + s + "' is already used by " + WorldItem.NAMES[kind].toLowerCase () + " " + i);
			}
		}
		return s;
	}
}
