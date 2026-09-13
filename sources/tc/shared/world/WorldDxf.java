/*
 * (c) 2004 Juan Pedro Canovas Quiñonero
 * (c) 2004-2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import java.util.ArrayList;
import java.util.StringTokenizer;

import devices.pos.Position;

import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;
import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.BlockDxf;
import wucore.utils.dxf.entities.CircleDxf;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.InsertDxf;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.entities.VertexDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * Import and export of {@link World} maps as AutoCAD DXF drawings. This is the
 * only place where the world model knows about the DXF entities; the map
 * elements themselves are persisted in JSON (see {@link World}).
 *
 * Layers: "0" walls (lines and polylines), ZONES (closed polylines), DOORS
 * (lines), OBJECTS (block inserts), PATH (one polyline), WAYPOINTS and
 * DOCKINGS (texts), BEACONS (texts or lines), CBEACONS (circles or texts) and
 * OTHERS (start points). Defaults travel as "NAME = value" texts.
 */
public class WorldDxf
{
	static private final double		TEXT_H		= 0.2;			// height of the label texts (m)

	private WorldDxf ()				{ }

	/* ================================================================== */
	/* Reading                                                              */
	/* ================================================================== */

	/** Builds a world from a DXF file. */
	static public World read (String name) throws Exception
	{
		DXFWorldFile	dxf = new DXFWorldFile ();
		dxf.load (name);

		World			w = World.empty ();
		readDefaults (dxf, w);
		readWalls (dxf, w);
		readZones (dxf, w);
		readConnectors (dxf, w);
		readObjects (dxf, w);
		readPath (dxf, w);
		readWaypoints (dxf, w);
		readDocks (dxf, w);
		readBeacons (dxf, w);
		readCBeacons (dxf, w);
		readStarts (dxf, w);
		return w;
	}

	/** The value after the '=' of a "NAME = value" text, or null when the text is not one. */
	static private String valueOf (Entity entity, String key)
	{
		if (!(entity instanceof TextDxf))		return null;
		String	text = ((TextDxf) entity).getText ();
		if (!text.startsWith (key))				return null;
		return text.substring (text.lastIndexOf ("=") + 1).trim ();
	}

	static private void readDefaults (DXFWorldFile dxf, World w)
	{
		double	wallW = w.walls ().defaultWidth (), wallH = w.walls ().defaultHeight ();
		String	wallT = w.walls ().defaultTexture ();
		double	doorW = w.connectors ().defaultWidth (), doorH = w.connectors ().defaultHeight ();
		String	doorT = w.connectors ().defaultTexture ();
		String	zoneT = w.zones ().defaultTexture ();
		String	v;

		for (Entity entity : dxf.getEntities ())
		{
			try
			{
				if ((v = valueOf (entity, "LINE_DEF_WIDTH")) != null)			wallW = Double.parseDouble (v);
				else if ((v = valueOf (entity, "LINE_DEF_HEIGHT")) != null)		wallH = Double.parseDouble (v);
				else if ((v = valueOf (entity, "LINE_DEF_TEXTURE")) != null)	wallT = v;
				else if ((v = valueOf (entity, "DOOR_DEF_WIDTH")) != null)		doorW = Double.parseDouble (v);
				else if ((v = valueOf (entity, "DOOR_DEF_HEIGHT")) != null)		doorH = Double.parseDouble (v);
				else if ((v = valueOf (entity, "DOOR_DEF_TEXTURE")) != null)	doorT = v;
				else if ((v = valueOf (entity, "ZONE_DEF_TEXTURE")) != null)	zoneT = v;
			} catch (Exception e) { }
		}
		w.walls ().setDefaults (wallW, wallH, wallT);
		w.connectors ().setDefaults (doorW, doorH, doorT);
		w.zones ().setDefaultTexture (zoneT);
	}

	static private void readWalls (DXFWorldFile dxf, World w)
	{
		WMWalls		walls = w.walls ();
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("0"))		continue;
			if (entity instanceof LineDxf)
				walls.add (toWall ((LineDxf) entity, walls));
			else if (entity instanceof PolylineDxf)
				for (LineDxf line : ((PolylineDxf) entity).toDxfLines ())
					walls.add (toWall (line, walls));
		}
	}

	static private WMWall toWall (LineDxf line, WMWalls walls)
	{
		WMWall	wall = new WMWall ();
		wall.edge		= toLine (line);
		wall.height		= (line.ExtendedDouble.size () > 0) ? line.getExtDouble (0) : walls.defaultHeight ();
		wall.width		= (line.ExtendedDouble.size () > 1) ? line.getExtDouble (1) : walls.defaultWidth ();
		wall.texture	= (line.ExtendedText.size () > 0) ? line.getExtText (0) : walls.defaultTexture ();
		wall.label		= "LINE_?";
		return wall;
	}

	static private void readZones (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("ZONES") || !(entity instanceof PolylineDxf))		continue;
			PolylineDxf	pol = (PolylineDxf) entity;
			WMZone		zone = new WMZone ();
			zone.z			= pol.minlimit.z ();
			zone.area		= new java.awt.geom.Rectangle2D.Double (pol.minlimit.x (), pol.minlimit.y (), pol.maxlimit.x () - pol.minlimit.x (), pol.maxlimit.y () - pol.minlimit.y ());
			zone.label		= (pol.ExtendedText.size () > 0) ? pol.getExtText (0) : "ZONE_?";
			zone.texture	= (pol.ExtendedText.size () > 1) ? pol.getExtText (1) : w.zones ().defaultTexture ();
			w.zones ().add (zone);
		}
	}

	static private void readConnectors (DXFWorldFile dxf, World w)
	{
		WMConnectors	conns = w.connectors ();
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("DOORS") || !(entity instanceof LineDxf))		continue;
			LineDxf		line = (LineDxf) entity;
			WMConnector	c = new WMConnector ();
			c.texture	= conns.defaultTexture ();
			c.width		= conns.defaultWidth ();
			c.height	= conns.defaultHeight ();
			c.path		= toLine (line);
			c.label		= (line.ExtendedText.size () > 0) ? line.getExtText (0) : "DOOR_?";
			c.edge		= new Line2 ();
			if (line.ExtendedText.size () > 1)
			{
				StringTokenizer	tk = new StringTokenizer (line.getExtText (1), ", ");
				if (tk.countTokens () >= 4)
					c.edge = new Line2 (Double.parseDouble (tk.nextToken ()), Double.parseDouble (tk.nextToken ()), Double.parseDouble (tk.nextToken ()), Double.parseDouble (tk.nextToken ()));
			}
			if (line.ExtendedText.size () > 2)			c.texture = line.getExtText (2);
			if (line.ExtendedDouble.size () > 0)		c.height = line.getExtDouble (0);
			if (line.ExtendedDouble.size () > 1)		c.width = line.getExtDouble (1);
			conns.add (c);
		}
	}

	static private void readObjects (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("OBJECTS") || !(entity instanceof InsertDxf))		continue;
			InsertDxf	insert = (InsertDxf) entity;
			w.objects ().add (toObject (insert, dxf.getBlocks (insert.getBlockname ()), w.icons ()));
		}
	}

	/** An object from a DXF insert: the block lines become its (shared) icon. */
	static private WMObject toObject (InsertDxf insert, BlockDxf block, WMIcons icons)
	{
		WMObject	o = new WMObject ();
		o.pos		= insert.getPos ();
		o.a			= insert.getRot ();
		o.color		= (insert.ExtTextSize () > 0) ? ColorTool.getColorFromName (insert.getExtText (0)) : WColor.BLACK;
		o.shape		= (insert.ExtTextSize () > 1) ? insert.getExtText (1) : null;
		if ((o.shape != null) && o.shape.equalsIgnoreCase ("none"))		o.shape = null;
		o.usecolor	= (insert.ExtTextSize () > 2) && Boolean.parseBoolean (insert.getExtText (2));

		ArrayList<Line2>	lines = new ArrayList<Line2> ();
		if (block != null)
			for (Entity e : block.entities)
				if (e instanceof LineDxf)		lines.add (toLine ((LineDxf) e));
		Line2[]		arr = lines.toArray (new Line2[0]);
		String		name = insert.getBlockname ();
		WMIcon		icon = icons.at (name);
		if ((icon == null) || !icon.sameGeometry (new WMIcon (name, arr)))
			icon = icons.register (arr, name);
		o.setIcon (icon);
		o.label		= "OBJECT";
		return o;
	}

	static private void readPath (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("PATH") || !(entity instanceof PolylineDxf))		continue;
			PolylineDxf	pol = (PolylineDxf) entity;
			for (int j = 0; j < pol.vertexs.size (); j++)
				w.path ().add (new Point2 (pol.getPoint (j).x (), pol.getPoint (j).y ()));
			return;		// only the first polyline
		}
	}

	static private void readWaypoints (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("WAYPOINTS") || !(entity instanceof TextDxf))		continue;
			TextDxf		text = (TextDxf) entity;
			Point3		p = text.getPos ();
			double		ang = (text.ExtendedDouble.size () > 0) ? Math.toRadians (text.getExtDouble (0)) : 0.0;
			w.wps ().add (new WMWaypoint (new Position (p.x (), p.y (), p.z (), ang), text.getText ()));
		}
	}

	static private void readDocks (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("DOCKINGS") || !(entity instanceof TextDxf))		continue;
			TextDxf		text = (TextDxf) entity;
			Point3		p = text.getPos ();
			double		ang = (text.ExtendedDouble.size () > 0) ? Math.toRadians (text.getExtDouble (0)) : 0.0;
			WMDock.FlowType	flow = (text.ExtendedText.size () > 0) ? WMDock.parseFlow (text.getExtText (0)) : WMDock.DEFAULT_FLOW;
			w.docks ().add (new WMDock (new Position (p.x (), p.y (), p.z (), ang), text.getText (), flow));
		}
	}

	static private void readBeacons (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("BEACONS"))		continue;
			if (entity instanceof TextDxf)
			{
				TextDxf		text = (TextDxf) entity;
				Point3		p = text.getPos ();
				double		rot = (text.ExtendedDouble.size () > 0) ? Math.toRadians (text.getExtDouble (0)) : 0.0;
				double		width = (text.ExtendedDouble.size () > 1) ? text.getExtDouble (1) : 0.0;
				double		height = (text.ExtendedDouble.size () > 2) ? text.getExtDouble (2) : WMBeacon.DEF_HEIGHT;
				w.beacons ().add (new WMBeacon (text.getText (), new Position (p.x (), p.y (), rot), width, height));
			}
			else if (entity instanceof LineDxf)
			{
				LineDxf		ldxf = (LineDxf) entity;
				Line2		line = new Line2 ();
				line.set (ldxf.getStart (), ldxf.getEnd ());
				Point3		p = new Point3 (line.center ());
				String		label = (ldxf.ExtendedText.size () > 0) ? ldxf.getExtText (0) : null;
				w.beacons ().add (new WMBeacon (label, new Position (p.x (), p.y (), line.angle ()), 0.0));
			}
		}
	}

	static private void readCBeacons (DXFWorldFile dxf, World w)
	{
		int		index = 0;
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("CBEACONS"))		continue;
			if (entity instanceof TextDxf)
			{
				TextDxf		text = (TextDxf) entity;
				Point3		p = text.getPos ();
				double		diameter = (text.ExtendedDouble.size () > 0) ? text.getExtDouble (0) : WMCBeacon.DEF_DIAMETER;
				double		height = (text.ExtendedDouble.size () > 1) ? text.getExtDouble (1) : WMCBeacon.DEF_HEIGHT;
				w.cbeacons ().add (new WMCBeacon (p.x (), p.y (), 0.0, diameter, height, text.getText ()));
			}
			else if (entity instanceof CircleDxf)
			{
				CircleDxf	circle = (CircleDxf) entity;
				Point3		p = circle.getCenter ();
				String		label = (circle.ExtendedText.size () > 0) ? circle.getExtText (0) : "CB" + (index++);
				w.cbeacons ().add (new WMCBeacon (p.x (), p.y (), 0.0, 2.0 * circle.getRadius (), WMCBeacon.DEF_HEIGHT, label));
			}
		}
	}

	static private void readStarts (DXFWorldFile dxf, World w)
	{
		for (Entity entity : dxf.getEntities ())
		{
			if (!entity.getLayer ().equalsIgnoreCase ("OTHERS"))		continue;
			String	v = valueOf (entity, "START");
			if (v == null)		continue;
			// texts written by older versions carry "x, y, angle" (no z); START_i texts add start points
			String	text = ((TextDxf) entity).getText ();
			WMStart	st = new WMStart (v);
			if (text.startsWith ("START_") && !text.startsWith ("START_1"))		w.starts ().add (st);
			else		w.starts ().set (0, st);
		}
	}

	static private Line2 toLine (LineDxf line)
	{
		return new Line2 (line.getStart ().x (), line.getStart ().y (), line.getStart ().z (), line.getEnd ().x (), line.getEnd ().y (), line.getEnd ().z ());
	}

	/* ================================================================== */
	/* Writing                                                              */
	/* ================================================================== */

	/** Writes a world as a DXF file. */
	static public void write (World w, String name) throws Exception
	{
		DXFWorldFile	dxf = new DXFWorldFile ();

		writePath (dxf, w);
		writeWalls (dxf, w);
		writeObjects (dxf, w);
		writeZones (dxf, w);
		writeWaypoints (dxf, w);
		writeDocks (dxf, w);
		writeCBeacons (dxf, w);
		writeBeacons (dxf, w);
		writeConnectors (dxf, w);
		writeStarts (dxf, w);
		dxf.createDxf (name);
	}

	/** Adds a "NAME = value" text at the running text position of the drawing. */
	static private void writeValue (DXFWorldFile dxf, String key, Object value, String layer)
	{
		dxf.addEntity (new TextDxf (key + " = " + value, new Point3 (dxf.posx, dxf.posy, 0.0), TEXT_H, layer));
		dxf.posy -= 0.5;
	}

	static private void writePath (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("PATH", ACADColor.CYAN));
		if (w.path ().n () == 0)		return;
		PolylineDxf	pol = new PolylineDxf ();
		pol.setLayer ("PATH");
		for (int i = 0; i < w.path ().n (); i++)
			pol.addVertex (new VertexDxf (new Point3 (w.path ().at (i))));
		dxf.addEntity (pol);
	}

	static private void writeWalls (DXFWorldFile dxf, World w)
	{
		WMWalls		walls = w.walls ();
		for (int i = 0; i < walls.n (); i++)
		{
			WMWall	wall = walls.at (i);
			LineDxf	line = new LineDxf (new Point3 (wall.edge.orig ().x (), wall.edge.orig ().y (), wall.edge.z1 ()), new Point3 (wall.edge.dest ().x (), wall.edge.dest ().y (), wall.edge.z2 ()), "0");
			line.addExtDouble (0, wall.height);
			line.addExtDouble (1, wall.width);
			line.addExtText (0, wall.texture);
			dxf.addEntity (line);
		}
		writeValue (dxf, "LINE_DEF_HEIGHT", walls.defaultHeight (), "0");
		writeValue (dxf, "LINE_DEF_WIDTH", walls.defaultWidth (), "0");
		writeValue (dxf, "LINE_DEF_TEXTURE", walls.defaultTexture (), "0");
	}

	static private void writeObjects (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("OBJECTS", ACADColor.GREEN));
		for (int i = 0; i < w.objects ().n (); i++)
		{
			WMObject	o = w.objects ().at (i);
			String		name = (o.iconId != null) ? o.iconId : "icon";
			InsertDxf	insert = new InsertDxf (o.pos, (o.shape != null) ? o.shape : name, "OBJECTS");
			insert.setRot (o.a);
			insert.setBlockname (name);
			BlockDxf	block = new BlockDxf (name);
			for (Line2 l : o.getLocalIcon ())
				block.entities.add (new LineDxf (new Point3 (l.orig ().x (), l.orig ().y (), l.z1 ()), new Point3 (l.dest ().x (), l.dest ().y (), l.z2 ())));
			insert.addExtText (0, ColorTool.getNameFromColor (o.color));
			insert.addExtText (1, (o.shape != null) ? o.shape : "none");
			insert.addExtText (2, Boolean.toString (o.usecolor));
			dxf.addBlock (block);
			dxf.insertBlock (insert);
		}
	}

	static private void writeZones (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("ZONES", ACADColor.YELLOW));
		for (int i = 0; i < w.zones ().n (); i++)
		{
			WMZone		z = w.zones ().at (i);
			PolylineDxf	pol = new PolylineDxf ();
			pol.addVertex (new VertexDxf (new Point3 (z.minx (), z.miny (), z.z)));
			pol.addVertex (new VertexDxf (new Point3 (z.minx () + z.width (), z.miny (), z.z)));
			pol.addVertex (new VertexDxf (new Point3 (z.minx () + z.width (), z.miny () + z.height (), z.z)));
			pol.addVertex (new VertexDxf (new Point3 (z.minx (), z.miny () + z.height (), z.z)));
			pol.addExtText (0, z.label);
			pol.addExtText (1, z.texture);
			pol.setLayer ("ZONES");
			dxf.addEntity (pol);
		}
		writeValue (dxf, "ZONE_DEF_TEXTURE", w.zones ().defaultTexture (), "ZONES");
	}

	static private void writeWaypoints (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("WAYPOINTS", ACADColor.MAGENTA));
		for (int i = 0; i < w.wps ().n (); i++)
		{
			WMWaypoint	wp = w.wps ().at (i);
			TextDxf		text = new TextDxf (wp.label, wp.getPos (), TEXT_H, "WAYPOINTS");
			text.addExtDouble (Math.toDegrees (wp.pos.alpha ()));
			dxf.addEntity (text);
		}
	}

	static private void writeDocks (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("DOCKINGS", ACADColor.CYAN));
		for (int i = 0; i < w.docks ().n (); i++)
		{
			WMDock		d = w.docks ().at (i);
			TextDxf		text = new TextDxf (d.label, d.getPos (), TEXT_H, "DOCKINGS");
			text.addExtDouble (Math.toDegrees (d.getAng ()));
			text.addExtText (d.flow.name ());
			dxf.addEntity (text);
		}
	}

	static private void writeCBeacons (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("CBEACONS", ACADColor.RED));
		for (int i = 0; i < w.cbeacons ().n (); i++)
		{
			WMCBeacon	b = w.cbeacons ().at (i);
			CircleDxf	circle = new CircleDxf (new Point3 (b.pos.x (), b.pos.y (), 0.0), b.radius (), "CBEACONS");
			circle.addExtText (b.label);
			dxf.addEntity (circle);
		}
	}

	static private void writeBeacons (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("BEACONS", ACADColor.RED));
		for (int i = 0; i < w.beacons ().n (); i++)
		{
			WMBeacon	b = w.beacons ().at (i);
			TextDxf		text = new TextDxf (b.label, new Point3 (b.pos.x (), b.pos.y (), 0.0), TEXT_H, "BEACONS");
			text.addExtDouble (Math.toDegrees (b.pos.alpha ()));
			text.addExtDouble (b.width);
			text.addExtDouble (b.height);
			dxf.addEntity (text);
		}
	}

	static private void writeConnectors (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("DOORS", ACADColor.LIGHT_GRAY));
		for (int i = 0; i < w.connectors ().n (); i++)
		{
			WMConnector	c = w.connectors ().at (i);
			LineDxf		line = new LineDxf (new Point3 (c.path.orig ()), new Point3 (c.path.dest ()), "DOORS");
			line.addExtText (0, c.label);
			line.addExtText (1, c.edge.toRawString ());
			line.addExtText (2, c.texture);
			line.addExtDouble (0, c.height);
			line.addExtDouble (1, c.width);
			dxf.addEntity (line);
		}
	}

	static private void writeStarts (DXFWorldFile dxf, World w)
	{
		dxf.addLayer (new Layer ("OTHERS", ACADColor.BLUE));
		for (int i = 0; i < w.starts ().size (); i++)
		{
			WMStart	st = w.starts ().get (i);
			dxf.addEntity (new TextDxf ("START_" + (i + 1) + " = " + st.toProperty (), new Point3 (st.x (), st.y (), st.z ()), TEXT_H, "OTHERS"));
		}
	}
}
