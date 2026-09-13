/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;

import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMPath
{
	
 	// Proposed robot's path points
 	protected Point2[]			points;

	
    // Accessors
	public final int	 		n () 				{ return points.length; }

	/** Elevation of a path point (0 if it carries none). */
	static public double z (Point2 p)			{ return (p instanceof Point3) ? ((Point3) p).z () : 0.0; }

	
	// Instance methods
	public Point2 at (int i)
	{
		if ((i < 0) || (i >= points.length)) return null;
		return points[i];
	}
	
	/* Edition methods (world editor) */
	public final Point2[]		points ()			{ return points; }

	public void add (Point2 p)
	{
		insert (points.length, p);
	}

	public void insert (int i, Point2 p)
	{
		if (i < 0)						i = 0;
		if (i > points.length)			i = points.length;
		Point2[]	tmp = new Point2[points.length + 1];
		System.arraycopy (points, 0, tmp, 0, i);
		tmp[i] = p;
		System.arraycopy (points, i, tmp, i + 1, points.length - i);
		points = tmp;
	}

	public Point2 remove (int i)
	{
		if ((i < 0) || (i >= points.length))		return null;
		Point2		old = points[i];
		Point2[]	tmp = new Point2[points.length - 1];
		System.arraycopy (points, 0, tmp, 0, i);
		System.arraycopy (points, i + 1, tmp, i, points.length - i - 1);
		points = tmp;
		return old;
	}

	/* JSON: [{x, y, z}, ...] */

	public WMPath ()							{ points = new Point2[0]; }
	public WMPath (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		points	= new Point2[arr.size ()];
		for (int i = 0; i < points.length; i++)		points[i] = World.toPoint (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (Point2 p : points)		arr.add (World.point (p));
		return arr;
	}
}
