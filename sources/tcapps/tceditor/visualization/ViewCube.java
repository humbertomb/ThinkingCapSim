/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * A view cube, as the CAD programs have (Fusion 360, say), drawn over the
 * corner of a 3D view: a cube turned as the world is seen, its faces named
 * after the views they stand for. Dragging it turns the eye round what it looks
 * at, as the cube turns under the hand; a click on a face goes to that view
 * (top, front, right...), and one on a corner to the isometric view from it,
 * the eye turning there in a short while rather than jumping. Looking square at
 * a face, the faces round it are edge-on: four arrows round the cube turn it a
 * quarter turn up, down, left or right, to the face next to it.
 *
 * The world has Z up; the front is seen from -Y (looking towards +Y, X to the
 * right), the right from +X, the top from +Z (Y up on the screen, as the front
 * has it). The cube does nothing by itself: the window draws it over the canvas
 * after every frame ({@link #paint}) and hands it the mouse when it is over it.
 */
public class ViewCube
{
	/** How big the cube is drawn (px, its side as it faces the eye), and how far from the corner. */
	static public final int			SIZE		= 62;
	static public final int			MARGIN		= 22;
	/** How long the eye takes to turn to a view (ms). */
	static public final int			TURN_MS		= 300;
	/** How much the eye turns for every pixel the cube is dragged (rad). */
	static public final double		DRAG_RAD	= 0.012;

	static private final Color		C_FACE		= new Color (232, 236, 242, 235);
	static private final Color		C_HOVER		= new Color (150, 190, 245, 245);
	static private final Color		C_EDGE		= new Color (90, 100, 115);
	static private final Color		C_TEXT		= new Color (40, 45, 55);
	static private final Font		FONT		= new Font ("SansSerif", Font.BOLD, 11);
	/** Where the arrows go {dx, dy} (screen, up is -y), and what each does to the angles {dtheta, drho}: up, down, left, right. */
	static private final int[][]	ARROWS		= { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
	static private final double[][]	QUARTERS	= { { 0, Math.PI / 2.0 }, { 0, -Math.PI / 2.0 }, { -Math.PI / 2.0, 0 }, { Math.PI / 2.0, 0 } };

	/** The faces: their outward normal, their name and the view they go to {theta, rho}. */
	static private final double[][]	NORMALS	= { { 0, 0, 1 }, { 0, 0, -1 }, { 0, -1, 0 }, { 0, 1, 0 }, { 1, 0, 0 }, { -1, 0, 0 } };
	static private final String[]	NAMES	= { "TOP", "BOTTOM", "FRONT", "BACK", "RIGHT", "LEFT" };
	static private final double[][]	VIEWS	=
	{
		{ -Math.PI / 2.0,  Math.PI / 2.0 },		// top: from over it, the front at the bottom of the screen
		{ -Math.PI / 2.0, -Math.PI / 2.0 },		// bottom
		{ -Math.PI / 2.0, 0.0 },				// front: from -Y
		{  Math.PI / 2.0, 0.0 },				// back: from +Y
		{  0.0, 0.0 },							// right: from +X
		{  Math.PI, 0.0 },						// left: from -X
	};

	protected Scene3D				scene;
	protected Runnable				redraw;					// how to have the canvas drawn again (a hover changes nothing in the scene)

	protected int					cx, cy;					// where the cube was last drawn (its centre, px)
	protected Polygon[]				shapes	= new Polygon[6];	// and its faces as seen then (null: hidden)
	protected int[][]				corners	= new int[8][];		// its corners on the screen (null: hidden)
	protected int					hover	= -1;			// the face under the mouse (0..5), a corner (6..13), an arrow (14..17), or none
	protected Polygon[]				arrows	= new Polygon[4];	// the arrows, when looking square at a face (null: not shown)
	protected boolean				pressed, dragged;
	protected int					px, py;					// where the mouse was
	protected Timer					turner;

	public ViewCube (Scene3D scene, Runnable redraw)
	{
		this.scene	= scene;
		this.redraw	= redraw;
	}

	/* ------------------------------------------------------------------ */

	/** Draws the cube in the top right corner of a canvas so wide, as the world is seen now. */
	public void paint (Graphics2D g, int width)
	{
		double		th = scene.theta (), rh = scene.rho ();
		double[]	f = { -Math.cos (rh) * Math.cos (th), -Math.cos (rh) * Math.sin (th), -Math.sin (rh) };		// where the eye looks
		double[]	r = norm (cross (f, new double[] { 0, 0, 1 }));
		double[]	u;
		double		s = SIZE / 2.0;

		if (r == null)		r = new double[] { -Math.sin (th), Math.cos (th), 0 };		// straight over or under: round by theta
		u	= cross (r, f);
		cx	= width - MARGIN - SIZE;
		cy	= MARGIN + SIZE;

		// the corners, and which of them can be seen (the one farthest from the eye cannot)
		double[][]	v = new double[8][];
		double		far = Double.NEGATIVE_INFINITY;
		int			hidden = -1;

		for (int k = 0; k < 8; k++)
		{
			v[k]	= new double[] { ((k & 1) != 0) ? 1 : -1, ((k & 2) != 0) ? 1 : -1, ((k & 4) != 0) ? 1 : -1 };
			double	d = dot (v[k], f);
			if (d > far)		{ far = d;	hidden = k; }
		}

		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont (FONT);
		for (int k = 0; k < 8; k++)
			corners[k]	= (k == hidden) ? null : new int[] { (int) Math.round (cx + s * dot (v[k], r)), (int) Math.round (cy - s * dot (v[k], u)) };

		// the faces that look at the eye, the most turned away first
		Integer[]	order = { 0, 1, 2, 3, 4, 5 };
		final double[]	ff = f;
		java.util.Arrays.sort (order, (a, b) -> Double.compare (-dot (NORMALS[a], ff), -dot (NORMALS[b], ff)));
		for (int i : order)
		{
			double[]	n = NORMALS[i];
			double		facing = -dot (n, f);

			shapes[i]	= null;
			if (facing <= 0.02)		continue;

			// the four corners of the face, round it
			double[]	a = (Math.abs (n[2]) > 0.5) ? new double[] { 1, 0, 0 } : new double[] { 0, 0, 1 };
			double[]	b = cross (n, a);
			Polygon		p = new Polygon ();

			for (int k = 0; k < 4; k++)
			{
				double	sa = ((k == 0) || (k == 3)) ? -1 : 1, sb = (k < 2) ? -1 : 1;
				double[]	q = { n[0] + sa * a[0] + sb * b[0], n[1] + sa * a[1] + sb * b[1], n[2] + sa * a[2] + sb * b[2] };
				p.addPoint ((int) Math.round (cx + s * dot (q, r)), (int) Math.round (cy - s * dot (q, u)));
			}
			shapes[i]	= p;

			g.setColor ((hover == i) ? C_HOVER : C_FACE);
			g.fillPolygon (p);
			g.setColor (C_EDGE);
			g.setStroke (new BasicStroke (1.2f));
			g.drawPolygon (p);

			// its name, when the face is turned enough to the eye to hold it
			if (facing > 0.35)
			{
				FontMetrics	fm = g.getFontMetrics ();
				int			tx = (int) Math.round (cx + s * dot (n, r)), ty = (int) Math.round (cy - s * dot (n, u));

				g.setColor (C_TEXT);
				g.drawString (NAMES[i], tx - fm.stringWidth (NAMES[i]) / 2, ty + fm.getAscent () / 2 - 1);
			}
		}

		// looking square at a face, the arrows to the faces round it
		boolean		square = false;

		for (double[] n : NORMALS)		if (-dot (n, f) > 0.995)		square = true;
		for (int i = 0; i < 4; i++)
		{
			arrows[i]	= null;
			if (!square)		continue;

			int		ax = cx + ARROWS[i][0] * (SIZE / 2 + 12), ay = cy + ARROWS[i][1] * (SIZE / 2 + 12);
			int		dx = ARROWS[i][0], dy = ARROWS[i][1];
			Polygon	p = new Polygon ();

			p.addPoint (ax + 6 * dx, ay + 6 * dy);					// the tip, outwards
			p.addPoint (ax - 4 * dx + 6 * dy, ay - 4 * dy + 6 * dx);
			p.addPoint (ax - 4 * dx - 6 * dy, ay - 4 * dy - 6 * dx);
			arrows[i]	= p;
			g.setColor ((hover == 14 + i) ? C_HOVER : C_FACE);
			g.fillPolygon (p);
			g.setColor (C_EDGE);
			g.drawPolygon (p);
		}

		// the corner under the mouse
		if ((hover >= 6) && (hover < 14) && (corners[hover - 6] != null))
		{
			g.setColor (C_HOVER.darker ());
			g.fillOval (corners[hover - 6][0] - 4, corners[hover - 6][1] - 4, 8, 8);
		}
	}

	/* ------------------------------------------------------------------ */

	/** Whether a point of the canvas is on the cube (or near enough to its corner to be taken by it). */
	public boolean contains (int x, int y)
	{
		return (Math.abs (x - cx) <= SIZE / 2 + 22) && (Math.abs (y - cy) <= SIZE / 2 + 22);
	}

	/** Whether the cube has the mouse (pressed on it and not let go yet). */
	public boolean active ()					{ return pressed; }

	/** The mouse moved over the canvas: what it is over is lit. */
	public void moved (int x, int y)
	{
		int		h = contains (x, y) ? pick (x, y) : -1;

		if (h != hover)		{ hover = h;	redraw.run (); }
	}

	/** Pressed on the cube. */
	public void press (int x, int y)
	{
		pressed	= true;
		dragged	= false;
		px		= x;
		py		= y;
		if (turner != null)		turner.stop ();
	}

	/** Dragged with it: the eye goes round as the cube turns under the hand. */
	public void drag (int x, int y)
	{
		if (!pressed)		return;
		if (Math.abs (x - px) + Math.abs (y - py) > 0)		dragged = true;
		scene.setAngles (scene.theta () - (x - px) * DRAG_RAD, scene.rho () + (y - py) * DRAG_RAD);
		px	= x;
		py	= y;
	}

	/** Let go: a click (not a drag) on a face or a corner goes to its view. */
	public void release (int x, int y)
	{
		if (!pressed)		return;
		pressed	= false;
		if (dragged)		return;

		int		h = pick (x, y);

		if ((h >= 0) && (h < 6))		turnTo (VIEWS[h][0], VIEWS[h][1]);
		else if (h >= 14)				turnTo (scene.theta () + QUARTERS[h - 14][0], scene.rho () + QUARTERS[h - 14][1]);
		else if (h >= 6)
		{
			int		k = h - 6;
			double	vx = ((k & 1) != 0) ? 1 : -1, vy = ((k & 2) != 0) ? 1 : -1, vz = ((k & 4) != 0) ? 1 : -1;

			turnTo (Math.atan2 (vy, vx), Math.atan2 (vz, Math.sqrt (2.0)));		// isometric, from that corner
		}
	}

	/** What is under a point: an arrow (14..17), a corner (6..13) when near one, else a face (0..5), else nothing (-1). */
	protected int pick (int x, int y)
	{
		for (int i = 0; i < 4; i++)
			if ((arrows[i] != null) && arrows[i].getBounds ().contains (x, y))		return 14 + i;
		for (int k = 0; k < 8; k++)
			if ((corners[k] != null) && (Math.abs (x - corners[k][0]) <= 5) && (Math.abs (y - corners[k][1]) <= 5))		return 6 + k;
		int		best = -1;
		for (int i = 0; i < 6; i++)
			if ((shapes[i] != null) && shapes[i].contains (x, y))		best = i;		// the last drawn is the one in front
		return best;
	}

	/** Turns the eye to some angles in a short while, the shorter way round. */
	public void turnTo (double theta, double rho)
	{
		final double	t0 = scene.theta (), r0 = scene.rho ();
		final double	dt = Math.atan2 (Math.sin (theta - t0), Math.cos (theta - t0)), dr = rho - r0;
		final long		start = System.currentTimeMillis ();

		if (turner != null)		turner.stop ();
		turner	= new Timer (15, null);
		turner.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)
			{
				double	k = Math.min (1.0, (System.currentTimeMillis () - start) / (double) TURN_MS);
				double	e2 = k * k * (3 - 2 * k);						// eased in and out

				scene.setAngles (t0 + dt * e2, r0 + dr * e2);
				if (k >= 1.0)		turner.stop ();
			}
		});
		turner.start ();
	}

	/* ------------------------------------------------------------------ */

	static private double dot (double[] a, double[] b)			{ return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
	static private double[] cross (double[] a, double[] b)		{ return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] }; }
	static private double[] norm (double[] a)
	{
		double	l = Math.sqrt (dot (a, a));
		return (l < 1e-9) ? null : new double[] { a[0] / l, a[1] / l, a[2] / l };
	}
}
