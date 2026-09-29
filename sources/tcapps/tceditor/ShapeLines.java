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
		return get (path, null);
	}

	/**
	 * The same for a kinematic model (.kine) standing still with the walking model
	 * named (a class; null for the joints at their defaults): the edges of its
	 * solids with the feet on the floor. A 3D Studio model ignores the walking.
	 */
	static public synchronized double[][] get (String path, String walking)
	{
		double[][]		lines;
		String			key;

		if ((path == null) || (path.trim ().length () == 0))		return new double[0][];
		path	= path.trim ();
		key		= isKine (path) ? (path + "|" + ((walking != null) ? walking.trim () : "")) : path;
		if (CACHE.containsKey (key))		return CACHE.get (key);

		lines	= isKine (path) ? edges (kineFaces (path, walking)) : read (path);
		CACHE.put (key, lines);
		return lines;
	}

	/** Whether a path names a kinematic model rather than a 3D Studio one. */
	static public boolean isKine (String path)
	{
		return (path != null) && path.trim ().toLowerCase ().endsWith (tc.vrobot.articulated.KineJson.SUFFIX);
	}

	/** Forgets what was read (the editor choosing another model, for instance). */
	static public synchronized void flush ()					{ CACHE.clear (); }
	static public synchronized void flush (String path)
	{
		if (path == null)				return;
		path	= path.trim ();
		CACHE.remove (path);
		for (String k : new ArrayList<String> (CACHE.keySet ()))	if (k.startsWith (path + "|"))	CACHE.remove (k);	// a .kine, with whatever walking
	}

	/**
	 * The faces of a kinematic model standing still: the walking model named
	 * (when there is one and it can be built) puts the joints in its standing
	 * pose and says how high the body is; without one the joints rest at their
	 * defaults and the body is as high as the lowest link is below it.
	 */
	static private double[][] kineFaces (String path, String walking)
	{
		try
		{
			tc.vrobot.articulated.KineModel		m = tc.vrobot.articulated.KineJson.read (new java.io.File (path));
			tc.vrobot.articulated.WalkingModel	w = tc.vrobot.articulated.WalkingModel.create (walking, m);
			double								lift;

			if (w != null)		{ w.setVelocities (0.0, 0.0, 0.0);	w.stand ();	lift = w.height (); }
			else				lift = -m.lowest ();
			return tc.vrobot.articulated.KineMesh.faces (m, lift + 0.012).toArray (new double[0][]);		// the paws are drawn a little below their link
		}
		catch (Throwable e)
		{
			System.out.println ("--[ShapeLines] Cannot read the kinematic model <" + path + ">: " + e);
			return new double[0][];
		}
	}

	/** The sides of some faces, each once. */
	static private double[][] edges (double[][] faces)
	{
		List<double[]>	out = new ArrayList<double[]> ();
		Set<String>		seen = new HashSet<String> ();

		for (double[] f : faces)
		{
			int		n = f.length / 3;

			for (int i = 0; i < n; i++)
			{
				int	j = (i + 1) % n;

				edge (new Point3d (f[3 * i], f[3 * i + 1], f[3 * i + 2]), new Point3d (f[3 * j], f[3 * j + 1], f[3 * j + 2]), out, seen);
			}
		}
		return out.toArray (new double[0][]);
	}

	/** What is done with every face of a model: a triangle or a quad, its corners in robot coordinates. */
	interface Faces
	{
		void face (Point3d[] corners);
	}

	static private double[][] read (String path)
	{
		final List<double[]>	out = new ArrayList<double[]> ();
		final Set<String>		seen = new HashSet<String> ();

		try
		{
			java.io.File	f = new java.io.File (path);
			if (!f.isFile ())			return new double[0][];
			Loader3DS		loader = new Loader3DS ();
			com.sun.j3d.loaders.Scene	scene = loader.load (path);
			// the sides of every face
			walk (scene.getSceneGroup (), new Transform3D (), new Faces ()
			{
				public void face (Point3d[] c)
				{
					for (int i = 0; i < c.length; i++)		edge (c[i], c[(i + 1) % c.length], out, seen);
				}
			});
		} catch (Throwable e)
		{
			System.out.println ("--[ShapeLines] Cannot read the 3D model <" + path + ">: " + e);
			return new double[0][];
		}
		return out.toArray (new double[0][]);
	}

	/* ------------------------------------------------------------------ */

	/**
	 * The faces of a model, each one as its corners {x, y, z, x, y, z, ...} in the
	 * coordinates of the robot -- three for a triangle and four for a quad. It is
	 * read again every time: whoever asks keeps what it makes of them.
	 */
	static public double[][] faces (String path)
	{
		return faces (path, null);
	}

	/** The same for a kinematic model standing still with a walking model (see {@link #get(String, String)}). */
	static public double[][] faces (String path, String walking)
	{
		final List<double[]>	out = new ArrayList<double[]> ();

		if ((path == null) || !new java.io.File (path.trim ()).isFile ())		return new double[0][];
		if (isKine (path))				return kineFaces (path.trim (), walking);
		try
		{
			com.sun.j3d.loaders.Scene	scene = new Loader3DS ().load (path.trim ());
			walk (scene.getSceneGroup (), new Transform3D (), new Faces ()
			{
				public void face (Point3d[] c)
				{
					double[]	f = new double[3 * c.length];
					for (int i = 0; i < c.length; i++)		{ f[3 * i] = c[i].x;	f[3 * i + 1] = c[i].y;	f[3 * i + 2] = c[i].z; }
					out.add (f);
				}
			});
		} catch (Throwable e)
		{
			System.out.println ("--[ShapeLines] Cannot read the 3D model <" + path + ">: " + e);
			return new double[0][];
		}
		return out.toArray (new double[0][]);
	}

	static private void walk (Node node, Transform3D t, Faces out)
	{
		if (node instanceof Shape3D)
		{
			Shape3D		shape = (Shape3D) node;
			for (int i = 0; i < shape.numGeometries (); i++)		faces (shape.getGeometry (i), t, out);
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
			for (int i = 0; i < g.numChildren (); i++)		walk (g.getChild (i), sub, out);
		}
	}

	/** The faces of one geometry: its triangles (or its quads). */
	static private void faces (Geometry geo, Transform3D t, Faces out)
	{
		GeometryArray	ga;
		int				n, first, per;

		if (!(geo instanceof GeometryArray))		return;
		ga		= (GeometryArray) geo;
		if (geo instanceof IndexedGeometryArray)	{ indexed ((IndexedGeometryArray) geo, t, out); return; }
		if (geo instanceof GeometryStripArray)		{ strips ((GeometryStripArray) geo, t, out); return; }

		per		= (geo instanceof javax.media.j3d.QuadArray) ? 4 : 3;
		n		= ga.getValidVertexCount ();
		first	= ((ga.getVertexFormat () & GeometryArray.BY_REFERENCE) != 0) ? ga.getInitialVertexIndex () : 0;
		for (int k = first; k + per <= first + n; k += per)
		{
			Point3d[]	c = new Point3d[per];
			for (int i = 0; i < per; i++)		c[i] = point (ga, k + i, t);
			out.face (c);
		}
	}

	static private void indexed (IndexedGeometryArray ga, Transform3D t, Faces out)
	{
		int		per = (ga instanceof javax.media.j3d.IndexedQuadArray) ? 4 : 3;
		int		n = ga.getValidIndexCount (), first = ga.getInitialIndexIndex ();

		for (int k = first; k + per <= first + n; k += per)
		{
			Point3d[]	c = new Point3d[per];
			for (int i = 0; i < per; i++)		c[i] = point (ga, ga.getCoordinateIndex (k + i), t);
			out.face (c);
		}
	}

	static private void strips (GeometryStripArray ga, Transform3D t, Faces out)
	{
		int[]	counts = new int[ga.getNumStrips ()];
		int		at = 0;

		ga.getStripVertexCounts (counts);
		for (int s = 0; s < counts.length; s++)
		{
			for (int k = 0; k + 2 < counts[s]; k++)					// every three in a row make a triangle
				out.face (new Point3d[] { point (ga, at + k, t), point (ga, at + k + 1, t), point (ga, at + k + 2, t) });
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
