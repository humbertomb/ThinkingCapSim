/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;

import com.mnstarfire.loaders3d.Loader3DS;

/**
 * What the world needs to know of a 3D Studio model (.3ds): how tall it is. A
 * model is written with Y up and turned a quarter turn about X when placed in a
 * scene, so its height over its own origin is the largest Y of its vertices, as
 * the loader places them (the transforms of its parts applied, as Scene3D and
 * ShapeLines have them).
 *
 * Reading a model is slow, so what is read is kept; a file that is missing or
 * cannot be read (no 3D library where it runs, for instance) is 0 tall.
 */
public class Model3DS
{
	static private final Map<String, Double>	HEIGHTS = new HashMap<String, Double> ();

	/** How tall a model is (m): the top of its geometry over its origin, 0 when there is none or it cannot be read. */
	static public synchronized double height (String path)
	{
		if ((path == null) || (path.trim ().length () == 0))		return 0.0;
		path	= path.trim ();

		Double	h = HEIGHTS.get (path);

		if (h == null)
		{
			h	= Double.valueOf (read (path));
			HEIGHTS.put (path, h);
		}
		return h.doubleValue ();
	}

	static private double read (String path)
	{
		if (!new File (path).isFile ())		return 0.0;
		try
		{
			double[]	top = { Double.NEGATIVE_INFINITY };

			walk (new Loader3DS ().load (path).getSceneGroup (), new Transform3D (), top);
			return Double.isInfinite (top[0]) ? 0.0 : Math.max (0.0, top[0]);
		}
		catch (Throwable e)
		{
			System.out.println ("--[Model3DS] Cannot read the 3D model <" + path + ">: " + e);
			return 0.0;
		}
	}

	/** The largest Y of the vertices under a node, with the transforms down to them. */
	static private void walk (Node node, Transform3D t, double[] top)
	{
		if (node instanceof Shape3D)
		{
			Shape3D		shape = (Shape3D) node;

			for (int i = 0; i < shape.numGeometries (); i++)
			{
				Geometry	geo = shape.getGeometry (i);

				if (!(geo instanceof GeometryArray))		continue;

				GeometryArray	ga = (GeometryArray) geo;
				Point3d			p = new Point3d ();
				int				n = ga.getVertexCount ();

				for (int k = 0; k < n; k++)
				{
					ga.getCoordinate (k, p);
					t.transform (p);
					if (p.y > top[0])		top[0] = p.y;
				}
			}
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
			for (int i = 0; i < g.numChildren (); i++)		walk (g.getChild (i), sub, top);
		}
	}
}
