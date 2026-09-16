/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;

import com.mnstarfire.loaders3d.Loader3DS;

/**
 * The edges of a 3D model (a <code>.3ds</code> file), in the coordinates of the
 * robot, so that a flat view can draw the model as a wire outline over any
 * projection.
 *
 * A 3DS model is written with Y up; placed in a scene it is turned a quarter
 * turn about X, which is the same turn applied here: the model (x, y, z)
 * becomes the robot (x, -z, y).
 *
 * Reading a model is slow, so what is read is kept, and a file that cannot be
 * read is remembered as such.
 */
public class ShapeLines
{
	/** Edges longer than this are kept; shorter ones say nothing at the sizes a robot is drawn at (m). */
	static public final double		MIN_LENGTH	= 0.002;

	static private final Map<String, double[][]>	CACHE = new HashMap<String, double[][]> ();

	/**
	 * The edges of a model: {x1, y1, z1, x2, y2, z2} each, in robot coordinates.
	 * An empty array when there is no model or it cannot be read.
	 */
	static public synchronized double[][] get (String path)
	{
		double[][]		lines;

		if ((path == null) || (path.trim ().length () == 0))		return new double[0][];
		path	= path.trim ();
		if (CACHE.containsKey (path))		return CACHE.get (path);

		lines	= read (path);
		CACHE.put (path, lines);
		return lines;
	}

	/** Forgets what was read (the editor choosing another model, for instance). */
	static public synchronized void flush ()					{ CACHE.clear (); }
	static public synchronized void flush (String path)			{ if (path != null)		CACHE.remove (path.trim ()); }

	static private double[][] read (String path)
	{
		List<double[]>	out = new ArrayList<double[]> ();
		Set<String>		seen = new HashSet<String> ();

		try
		{
			java.io.File	f = new java.io.File (path);
			if (!f.isFile ())			return new double[0][];
			Loader3DS		loader = new Loader3DS ();
			com.sun.j3d.loaders.Scene	scene = loader.load (path);
			walk (scene.getSceneGroup (), new Transform3D (), out, seen);
		} catch (Throwable e)
		{
			System.out.println ("--[ShapeLines] Cannot read the 3D model <" + path + ">: " + e);
			return new double[0][];
		}
		return out.toArray (new double[0][]);
	}

	/* ------------------------------------------------------------------ */

	static private void walk (Node node, Transform3D t, List<double[]> out, Set<String> seen)
	{
		if (node instanceof Shape3D)
		{
			Shape3D		shape = (Shape3D) node;
			for (int i = 0; i < shape.numGeometries (); i++)		edges (shape.getGeometry (i), t, out, seen);
		}
		else if (node instanceof Group)
		{
			Group			g = (Group) node;
			Transform3D		sub = t;

			if (node instanceof TransformGroup)
			{
				Transform3D		own = new Transform3D ();
				((TransformGroup) node).getTransform (own);
				sub		= new Transform3D (t);
				sub.mul (own);
			}
			for (int i = 0; i < g.numChildren (); i++)		walk (g.getChild (i), sub, out, seen);
		}
	}

	/** The edges of one geometry: the sides of its triangles (or of its quads). */
	static private void edges (Geometry geo, Transform3D t, List<double[]> out, Set<String> seen)
	{
		GeometryArray	ga;
		int				n, first, per;

		if (!(geo instanceof GeometryArray))		return;
		ga		= (GeometryArray) geo;
		if (geo instanceof IndexedGeometryArray)	{ indexed ((IndexedGeometryArray) geo, t, out, seen); return; }
		if (geo instanceof GeometryStripArray)		{ strips ((GeometryStripArray) geo, t, out, seen); return; }

		per		= (geo instanceof javax.media.j3d.QuadArray) ? 4 : 3;
		n		= ga.getValidVertexCount ();
		first	= ((ga.getVertexFormat () & GeometryArray.BY_REFERENCE) != 0) ? ga.getInitialVertexIndex () : 0;
		for (int k = first; k + per <= first + n; k += per)
			for (int i = 0; i < per; i++)
				edge (point (ga, k + i, t), point (ga, k + (i + 1) % per, t), out, seen);
	}

	static private void indexed (IndexedGeometryArray ga, Transform3D t, List<double[]> out, Set<String> seen)
	{
		int		per = (ga instanceof javax.media.j3d.IndexedQuadArray) ? 4 : 3;
		int		n = ga.getValidIndexCount (), first = ga.getInitialIndexIndex ();

		for (int k = first; k + per <= first + n; k += per)
			for (int i = 0; i < per; i++)
				edge (point (ga, ga.getCoordinateIndex (k + i), t),
					  point (ga, ga.getCoordinateIndex (k + (i + 1) % per), t), out, seen);
	}

	static private void strips (GeometryStripArray ga, Transform3D t, List<double[]> out, Set<String> seen)
	{
		int[]	counts = new int[ga.getNumStrips ()];
		int		at = 0;

		ga.getStripVertexCounts (counts);
		for (int s = 0; s < counts.length; s++)
		{
			for (int k = 0; k + 2 < counts[s]; k++)					// every three in a row make a triangle
			{
				Point3d		a = point (ga, at + k, t), b = point (ga, at + k + 1, t), c = point (ga, at + k + 2, t);
				edge (a, b, out, seen);		edge (b, c, out, seen);		edge (c, a, out, seen);
			}
			at	+= counts[s];
		}
	}

	static private Point3d point (GeometryArray ga, int index, Transform3D t)
	{
		Point3d		p = new Point3d ();

		ga.getCoordinate (index, p);
		t.transform (p);
		return new Point3d (p.x, -p.z, p.y);						// the quarter turn a 3DS takes in a scene
	}

	/** Adds an edge, unless it is too short or it was already there (each one is shared by two faces). */
	static private void edge (Point3d a, Point3d b, List<double[]> out, Set<String> seen)
	{
		String		key;

		if ((Math.abs (a.x - b.x) < MIN_LENGTH) && (Math.abs (a.y - b.y) < MIN_LENGTH) && (Math.abs (a.z - b.z) < MIN_LENGTH))
			return;
		key		= key (a, b);
		if (!seen.add (key))		return;
		out.add (new double[] { a.x, a.y, a.z, b.x, b.y, b.z });
	}

	/** The name of an edge, the same whichever end comes first. */
	static private String key (Point3d a, Point3d b)
	{
		String		ka = round (a), kb = round (b);

		return (ka.compareTo (kb) <= 0) ? ka + "|" + kb : kb + "|" + ka;
	}

	static private String round (Point3d p)
	{
		return Math.round (p.x * 2000) + "," + Math.round (p.y * 2000) + "," + Math.round (p.z * 2000);
	}
}
