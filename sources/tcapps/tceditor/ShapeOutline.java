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

/**
 * The contour of a 3D model seen from above: the line round the floor it
 * covers, in the frame of the object that carries it, for a plan to draw the
 * model where it stands.
 *
 * Not the lines of its faces, which is what the robot editor draws: a model of
 * a loaded pallet has close to two hundred thousand of them, and a plan draws
 * many models over and over. The contour is worked out once per model and is a
 * few hundred segments.
 *
 * It is worked out over a grid laid on the floor under the model: a cell is
 * covered when a face of the model, seen from above, falls on it -- the faces
 * that lie flat cover what they cover, and the ones that stand up (a panel, a
 * post, the sides of a box) cover the line they make -- and the contour is
 * traced between the covered cells and the rest (marching squares), so it goes
 * round holes too. A grid of about {@value #CELLS} cells on the long side keeps
 * it within a centimetre for a pallet and within a few for a production line.
 *
 * Reading a model is slow, so it is done on a thread of its own: the first time
 * a model is asked for there is nothing to draw yet, and whoever asked is told
 * when there is.
 */
public class ShapeOutline
{
	/** Cells of the grid along the long side of a model. */
	static public final int			CELLS		= 128;
	/** And never smaller than this (m): a small model does not need a finer contour. */
	static public final double		MIN_CELL	= 0.005;

	static private final double[][]					NONE	= new double[0][];
	static private final Map<String, double[][]>	CACHE	= new HashMap<String, double[][]> ();
	static private final Set<String>				PENDING	= new HashSet<String> ();
	static private final List<Runnable>				WAITING	= new ArrayList<Runnable> ();
	static private java.util.concurrent.ExecutorService	worker;

	/**
	 * The contour of a model, as segments {x1, y1, x2, y2} in the frame of the
	 * object; empty when there is no model or it cannot be read. Null while it is
	 * being worked out: <code>ready</code> is then run (on the event thread) once
	 * it is, so that the plan can be drawn again.
	 */
	static public synchronized double[][] get (final String path, Runnable ready)
	{
		final String	key;

		if ((path == null) || (path.trim ().length () == 0))		return NONE;
		key		= path.trim ();
		if (CACHE.containsKey (key))			return CACHE.get (key);
		if ((ready != null) && !WAITING.contains (ready))		WAITING.add (ready);
		if (PENDING.add (key))
		{
			if (worker == null)
				worker	= java.util.concurrent.Executors.newSingleThreadExecutor (new java.util.concurrent.ThreadFactory ()
				{
					public Thread newThread (Runnable r)	{ Thread t = new Thread (r, "ShapeOutline"); t.setDaemon (true); return t; }
				});
			worker.submit (new Runnable ()
			{
				public void run ()
				{
					double[][]	c = work (key);
					List<Runnable>	tell;

					synchronized (ShapeOutline.class)
					{
						CACHE.put (key, c);
						PENDING.remove (key);
						tell	= new ArrayList<Runnable> (WAITING);
						if (PENDING.isEmpty ())		WAITING.clear ();
					}
					for (Runnable r : tell)		javax.swing.SwingUtilities.invokeLater (r);
				}
			});
		}
		return null;
	}

	/** The contour of a model, worked out now: for whoever can wait for it. */
	static public double[][] now (String path)
	{
		String		key;
		double[][]	c;

		if ((path == null) || (path.trim ().length () == 0))		return NONE;
		key		= path.trim ();
		synchronized (ShapeOutline.class)		{ c = CACHE.get (key); }
		if (c != null)							return c;
		c		= work (key);
		synchronized (ShapeOutline.class)		{ CACHE.put (key, c); }
		return c;
	}

	/** Forgets the contour of a model (the model changed), or of all of them. */
	static public synchronized void flush (String path)		{ if (path != null)		CACHE.remove (path.trim ()); }
	static public synchronized void flush ()				{ CACHE.clear (); }

	/* ------------------------------------------------------------------ */

	/** The contour of the model of a file. */
	static private double[][] work (String path)
	{
		double[][]	faces = ShapeLines.faces (path);

		try { return contour (faces); }
		catch (Throwable t)
		{
			System.out.println ("--[ShapeOutline] Cannot work the contour of <" + path + "> out: " + t);
			return NONE;
		}
	}

	/**
	 * The contour of a set of faces seen from above: each face as its corners
	 * {x, y, z, x, y, z, ...}, the contour as segments {x1, y1, x2, y2}.
	 */
	static public double[][] contour (double[][] faces)
	{
		double		x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
		double		cell;
		int			nx, ny;
		boolean[][]	in;
		List<double[]>	out = new ArrayList<double[]> ();

		if ((faces == null) || (faces.length == 0))		return NONE;
		for (double[] f : faces)
			for (int i = 0; i + 2 < f.length; i += 3)
			{
				x0 = Math.min (x0, f[i]);	x1 = Math.max (x1, f[i]);
				y0 = Math.min (y0, f[i + 1]);	y1 = Math.max (y1, f[i + 1]);
			}
		cell	= Math.max (MIN_CELL, Math.max (x1 - x0, y1 - y0) / CELLS);
		// a cell of margin all round, so that the contour closes
		x0	-= cell;		y0 -= cell;
		nx	= (int) Math.ceil ((x1 - x0) / cell) + 2;
		ny	= (int) Math.ceil ((y1 - y0) / cell) + 2;
		in	= new boolean[nx][ny];

		for (double[] f : faces)
		{
			int		n = f.length / 3;

			// what a face lying down covers: its triangles, as a fan from its first corner
			for (int k = 1; k + 1 < n; k++)
				fill (in, x0, y0, cell, f[0], f[1], f[3 * k], f[3 * k + 1], f[3 * k + 3], f[3 * k + 4]);
			// and what one standing up covers: the lines of its sides
			for (int k = 0; k < n; k++)
			{
				int		j = (k + 1) % n;
				line (in, x0, y0, cell, f[3 * k], f[3 * k + 1], f[3 * j], f[3 * j + 1]);
			}
		}
		march (in, x0, y0, cell, out);
		return out.toArray (new double[0][]);
	}

	/** Marks the samples of the grid a triangle covers. */
	static private void fill (boolean[][] in, double x0, double y0, double cell,
							  double ax, double ay, double bx, double by, double cx, double cy)
	{
		double		area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
		int			i0, i1, j0, j1;

		if (Math.abs (area) < 1e-12)		return;							// a line, which line () marks
		i0	= Math.max (0, (int) Math.floor ((Math.min (ax, Math.min (bx, cx)) - x0) / cell));
		i1	= Math.min (in.length - 1, (int) Math.ceil ((Math.max (ax, Math.max (bx, cx)) - x0) / cell));
		j0	= Math.max (0, (int) Math.floor ((Math.min (ay, Math.min (by, cy)) - y0) / cell));
		j1	= Math.min (in[0].length - 1, (int) Math.ceil ((Math.max (ay, Math.max (by, cy)) - y0) / cell));
		for (int i = i0; i <= i1; i++)
			for (int j = j0; j <= j1; j++)
			{
				double	px = x0 + i * cell, py = y0 + j * cell;
				double	w0 = (bx - ax) * (py - ay) - (by - ay) * (px - ax);
				double	w1 = (cx - bx) * (py - by) - (cy - by) * (px - bx);
				double	w2 = (ax - cx) * (py - cy) - (ay - cy) * (px - cx);
				if (((w0 >= 0) && (w1 >= 0) && (w2 >= 0)) || ((w0 <= 0) && (w1 <= 0) && (w2 <= 0)))
					in[i][j]	= true;
			}
	}

	/** Marks the samples of the grid nearest to a segment, a step every half cell along it. */
	static private void line (boolean[][] in, double x0, double y0, double cell, double ax, double ay, double bx, double by)
	{
		double		len = Math.hypot (bx - ax, by - ay);
		int			steps = Math.max (1, (int) Math.ceil (2 * len / cell));

		for (int s = 0; s <= steps; s++)
		{
			double	t = s / (double) steps;
			int		i = (int) Math.round ((ax + t * (bx - ax) - x0) / cell);
			int		j = (int) Math.round ((ay + t * (by - ay) - y0) / cell);
			if ((i >= 0) && (j >= 0) && (i < in.length) && (j < in[0].length))		in[i][j] = true;
		}
	}

	/**
	 * The line between the covered samples of the grid and the rest, square by
	 * square: each square of four samples is crossed by none, one or two pieces
	 * of it, which join the middles of its sides where the samples differ.
	 */
	static private void march (boolean[][] in, double x0, double y0, double cell, List<double[]> out)
	{
		for (int i = 0; i + 1 < in.length; i++)
			for (int j = 0; j + 1 < in[0].length; j++)
			{
				boolean	a = in[i][j], b = in[i + 1][j], c = in[i + 1][j + 1], d = in[i][j + 1];
				int		k = (a ? 1 : 0) | (b ? 2 : 0) | (c ? 4 : 0) | (d ? 8 : 0);
				double	x = x0 + i * cell, y = y0 + j * cell, h = cell / 2;
				// the middles of the four sides: bottom, right, top, left
				double[]	B = { x + h, y }, Rr = { x + cell, y + h }, T = { x + h, y + cell }, L = { x, y + h };

				switch (k)
				{
				case 1:	case 14:	seg (out, L, B);	break;
				case 2:	case 13:	seg (out, B, Rr);	break;
				case 3:	case 12:	seg (out, L, Rr);	break;
				case 4:	case 11:	seg (out, Rr, T);	break;
				case 6:	case 9:		seg (out, B, T);	break;
				case 7:	case 8:		seg (out, L, T);	break;
				case 5:				seg (out, L, T);	seg (out, B, Rr);	break;		// two corners across: two pieces
				case 10:			seg (out, L, B);	seg (out, Rr, T);	break;
				default:
				}
			}
	}

	static private void seg (List<double[]> out, double[] p, double[] q)
	{
		out.add (new double[] { p[0], p[1], q[0], q[1] });
	}
}
