/*
 * Created on 10-dic-2004
 * (c) 2004-2026 Humberto Martinez Barbera
 */
package tc.shared.world;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.StringTokenizer;

import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;
import wucore.utils.dxf.DoubleFormat;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;
import wucore.utils.math.Angles;

/**
 * An object of the world: a 2D icon (shared {@link WMIcon}, referenced by
 * name) placed at a position and heading, plus its colour, an optional 2D
 * bitmap ({@link #image}, drawn in place of the icon and scaled to the box the
 * icon occupies) and an optional 3D shape.
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
	public String				image;		// 2D bitmap drawn instead of the icon (may be null)
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
	public WMObject (String prop, List<WMIcon> icons)
	{
		StringTokenizer		st = new StringTokenizer (prop, ", \t");
		fromCurrent (st.nextToken (), st, icons);
	}

	private void fromCurrent (String first, StringTokenizer st, List<WMIcon> icons)
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
		icon = World.find (icons, iconId);
		if (icon == null)
		{
			System.out.println ("  [WMObject] Warning: icon <" + iconId + "> not found, using an empty icon");
			icon = new WMIcon (iconId, new Line2[0]);
		}
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

	public String toRawString ()
	{
		String	out = iconId + ", " + DoubleFormat.format (pos.x ()) + ", " + DoubleFormat.format (pos.y ()) + ", " + DoubleFormat.format (pos.z ())
					+ ", " + DoubleFormat.format (a * Angles.RTOD) + ", " + ColorTool.getNameFromColor (color);
		if (shape != null)
			out += ", " + shape + ", " + usecolor;
		return out;
	}

	/* JSON: {icon, x, y, z, orientation (deg), color [, shape, usecolor]} */

	public WMObject (JsonObject o, List<WMIcon> icons)
	{
		iconId	= World.getString (o, "icon", "");
		image	= World.getString (o, "image", null);
		pos		= World.toPoint (o);
		a		= Math.toRadians (World.getDouble (o, "orientation", 0.0));
		String	cname = World.getString (o, "color", null);
		color	= (cname != null) ? ColorTool.getColorFromName (cname) : WColor.BLACK;
		shape	= World.getString (o, "shape", null);
		if ((shape != null) && shape.equalsIgnoreCase ("none"))		shape = null;
		usecolor = World.getBoolean (o, "usecolor", false);
		icon = World.find (icons, iconId);
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
		if (image != null)		o.addProperty ("image", image);
		World.putPoint (o, pos.x (), pos.y (), pos.z ());
		o.addProperty ("orientation", World.num (a * Angles.RTOD));
		o.addProperty ("color", ColorTool.getNameFromColor (color));
		if (shape != null)
		{
			o.addProperty ("shape", shape);
			o.addProperty ("usecolor", usecolor);
		}
		return o;
	}
}
