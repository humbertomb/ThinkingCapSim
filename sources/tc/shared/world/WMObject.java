/*
 * Created on 10-dic-2004
 * (c) 2004-2026 Humberto Martinez Barbera
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.StringTokenizer;

import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;
import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.dxf.entities.BlockDxf;
import wucore.utils.dxf.entities.InsertDxf;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;
import wucore.utils.math.Angles;

/**
 * An object of the world: a 2D icon (shared {@link WMIcon}, referenced by
 * name) placed at a position and heading, plus its colour and an optional 3D
 * shape.
 *
 * File format:
 * <pre>
 *   OBJECT_i = icon, x, y, z, angle(deg), color [, shape, usecolor]
 * </pre>
 *
 * @author Humberto Martinez Barbera
 */
public class WMObject extends WMElement
{
	// 2D components
	public String				iconId;		// Name of the icon in the world's icon library
	public WMIcon				icon;		// Resolved icon (local coordinates)
	public WColor				color;		// Color of the object

	// Placement and 3D components
	public Point3				pos;			// Position of the object (m, m, m)
	public double				a;			// Heading, XY plane (rad)
	public String				shape;		// 3D object representation (may be null)
	public boolean				usecolor;	// Replace 3D object color

	public boolean				visible		= true;

	// Cache of the icon in world coordinates
	private Line2[]				absCache;
	private double				cx, cy, cz, ca;
	private WMIcon				cicon;
	private int					clines;

	/* Constructors */

	public WMObject ()
	{
		pos		= new Point3 (0.0, 0.0, 0.0);
		color	= WColor.BLACK;
	}

	/**
	 * @param prop  the property value
	 * @param icons icon library used to resolve the icon name (a missing icon
	 *              yields a warning and an empty icon)
	 */
	public WMObject (String prop, WMIcons icons)
	{
		StringTokenizer		st = new StringTokenizer (prop, ", \t");
		fromCurrent (st.nextToken (), st, icons);
	}

	private void fromCurrent (String first, StringTokenizer st, WMIcons icons)
	{
		iconId	= first;
		double	x = Double.parseDouble (st.nextToken ());
		double	y = Double.parseDouble (st.nextToken ());
		double	z = Double.parseDouble (st.nextToken ());
		pos		= new Point3 (x, y, z);
		a		= Double.parseDouble (st.nextToken ()) * Angles.DTOR;
		color	= st.hasMoreTokens () ? ColorTool.getColorFromName (st.nextToken ()) : WColor.BLACK;
		if (st.hasMoreTokens ())
		{
			shape	= st.nextToken ();
			if (shape.equalsIgnoreCase ("none"))		shape = null;
			usecolor = st.hasMoreTokens () && Boolean.parseBoolean (st.nextToken ());
		}
		icon = (icons != null) ? icons.at (iconId) : null;
		if (icon == null)
		{
			System.out.println ("  [WMObject] Warning: icon <" + iconId + "> not found, using an empty icon");
			icon = new WMIcon (iconId, new Line2[0]);
		}
	}

	/** Builds an object from a DXF insert: the block lines become its (local) icon. */
	public WMObject (InsertDxf insert, BlockDxf block, WMIcons icons)
	{
		pos		= insert.getPos ();
		a		= insert.getRot ();
		color	= (insert.ExtTextSize () > 0) ? ColorTool.getColorFromName (insert.getExtText (0)) : WColor.BLACK;
		shape	= (insert.ExtTextSize () > 1) ? insert.getExtText (1) : null;
		if ((shape != null) && shape.equalsIgnoreCase ("none"))		shape = null;
		usecolor = (insert.ExtTextSize () > 2) && Boolean.parseBoolean (insert.getExtText (2));

		int		size = 0;
		for (int i = 0; i < block.entities.size (); i++)
			if (block.entities.get (i) instanceof LineDxf)		size++;
		Line2[]	lines = new Line2[size];
		int		k = 0;
		for (int i = 0; i < block.entities.size (); i++)
			if (block.entities.get (i) instanceof LineDxf)
			{
				LineDxf	line = (LineDxf) block.entities.get (i);
				lines[k++] = new Line2 (line.getStart ().x (), line.getStart ().y (), line.getStart ().z (), line.getEnd ().x (), line.getEnd ().y (), line.getEnd ().z ());
			}
		String	name = insert.getBlockname ();
		if (icons != null)
		{
			icon = icons.at (name);
			if ((icon == null) || !icon.sameGeometry (new WMIcon (name, lines)))
				icon = icons.register (lines, name);
		}
		else
			icon = new WMIcon (name, lines);
		iconId	= icon.label;
		label	= "OBJECT";
	}

	/* Accessors */

	/** Assigns a (shared) icon to the object. */
	public void setIcon (WMIcon icon)
	{
		this.icon	= icon;
		this.iconId	= (icon != null) ? icon.label : null;
		absCache	= null;
	}

	/** Icon segments in local coordinates (the shared icon definition). */
	public Line2[] getLocalIcon ()
	{
		return (icon != null) ? icon.lines : new Line2[0];
	}

	/**
	 * Icon segments in world coordinates for the current position and heading.
	 * The result is cached and recomputed when the pose or the icon change; the
	 * returned array must not be modified.
	 */
	public Line2[] absIcon ()
	{
		if (icon == null)			return new Line2[0];
		if ((absCache == null) || (cicon != icon) || (clines != icon.lines.length)
				|| (cx != pos.x ()) || (cy != pos.y ()) || (cz != pos.z ()) || (ca != a) || !sameLocal ())
		{
			absCache	= icon.toAbsolute (pos, a);
			cx = pos.x ();	cy = pos.y ();	cz = pos.z ();	ca = a;
			cicon		= icon;
			clines		= icon.lines.length;
			localCopy	= icon.copy (icon.label).lines;
		}
		return absCache;
	}

	private Line2[]				localCopy;

	private boolean sameLocal ()
	{
		if ((localCopy == null) || (localCopy.length != icon.lines.length))		return false;
		for (int i = 0; i < localCopy.length; i++)
		{
			Line2	a1 = localCopy[i], b = icon.lines[i];
			if ((a1.orig ().x () != b.orig ().x ()) || (a1.orig ().y () != b.orig ().y ()) || (a1.z1 () != b.z1 ())
			 || (a1.dest ().x () != b.dest ().x ()) || (a1.dest ().y () != b.dest ().y ()) || (a1.z2 () != b.z2 ()))
				return false;
		}
		return true;
	}

	/** Forces the recomputation of the absolute icon (after editing the icon or the pose). */
	public void invalidate ()
	{
		absCache = null;
	}

	/* Persistence */

	public void toDxf (DXFWorldFile dxf)
	{
		String		name = (iconId != null) ? iconId : "icon";
		InsertDxf	insert = new InsertDxf (pos, (shape != null) ? shape : name, "OBJECTS");
		insert.setRot (a);
		insert.setBlockname (name);
		BlockDxf	block = new BlockDxf (name);
		for (Line2 l : getLocalIcon ())
			block.entities.add (new LineDxf (new Point3 (l.orig ().x (), l.orig ().y (), l.z1 ()), new Point3 (l.dest ().x (), l.dest ().y (), l.z2 ())));
		insert.addExtText (0, ColorTool.getNameFromColor (color));
		insert.addExtText (1, (shape != null) ? shape : "none");
		insert.addExtText (2, Boolean.toString (usecolor));
		dxf.addBlock (block);
		dxf.insertBlock (insert);
	}

	public String toRawString ()
	{
		String	out = iconId + ", " + DoubleFormat.format (pos.x ()) + ", " + DoubleFormat.format (pos.y ()) + ", " + DoubleFormat.format (pos.z ())
					+ ", " + DoubleFormat.format (a * Angles.RTOD) + ", " + ColorTool.getNameFromColor (color);
		if (shape != null)
			out += ", " + shape + ", " + usecolor;
		return out;
	}

	/* JSON: {icon, x, y, z, orientation (deg), color [, shape, usecolor]} */

	public WMObject (JsonObject o, WMIcons icons)
	{
		iconId	= WorldJson.getString (o, "icon", "");
		pos		= WorldJson.toPoint (o);
		a		= Math.toRadians (WorldJson.getDouble (o, "orientation", 0.0));
		String	cname = WorldJson.getString (o, "color", null);
		color	= (cname != null) ? ColorTool.getColorFromName (cname) : WColor.BLACK;
		shape	= WorldJson.getString (o, "shape", null);
		if ((shape != null) && shape.equalsIgnoreCase ("none"))		shape = null;
		usecolor = WorldJson.getBoolean (o, "usecolor", false);
		icon = (icons != null) ? icons.at (iconId) : null;
		if (icon == null)
		{
			System.out.println ("  [WMObject] Warning: icon <" + iconId + "> not found, using an empty icon");
			icon = new WMIcon (iconId, new Line2[0]);
		}
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("icon", iconId);
		WorldJson.putPoint (o, pos.x (), pos.y (), pos.z ());
		o.addProperty ("orientation", WorldJson.num (a * Angles.RTOD));
		o.addProperty ("color", ColorTool.getNameFromColor (color));
		if (shape != null)
		{
			o.addProperty ("shape", shape);
			o.addProperty ("usecolor", usecolor);
		}
		return o;
	}
}
