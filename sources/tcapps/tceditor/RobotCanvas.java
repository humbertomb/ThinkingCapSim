/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

import tc.vrobot.RobotDef;
import tc.vrobot.RobotImage;
import wucore.utils.geom.Line2;

/**
 * Plan view of a robot description, in robot coordinates: the drawing of the
 * platform, its virtual radius, the bumpers and where each sensor sits, with
 * the direction it looks at. The X axis points forward.
 *
 * The view pans with the mouse and zooms with the wheel; clicking selects the
 * element under the pointer, which the editor shows in its tree and property
 * table.
 */
public class RobotCanvas extends JPanel
{
	private static final long		serialVersionUID = 1L;

	static public final Color		C_BG		= Color.WHITE;
	static public final Color		C_GRID		= new Color (235, 235, 235);
	static public final Color		C_AXIS		= new Color (200, 200, 200);
	static public final Color		C_ICON		= new Color (60, 60, 60);
	static public final Color		C_RADIUS	= new Color (120, 120, 200);
	static public final Color		C_BUMPER	= new Color (200, 60, 60);
	static public final Color		C_SENSOR	= new Color (40, 120, 200);
	static public final Color		C_SEL		= new Color (255, 140, 0);
	static public final Color		C_HANDLE	= new Color (255, 255, 255);		// handles, as in the world editor
	static public final Color		C_COVER_FILL	= new Color (255, 140, 0, 40);	// what the selected sensor covers
	static public final Color		C_COVER_LINE	= new Color (255, 140, 0, 140);

	static public final double		MIN_SCALE	= 10.0;			// pixels per metre
	static public final double		MAX_SCALE	= 2000.0;
	static public final double		HIT			= 6.0;			// selection tolerance (pixels)
	static public final double		PENDING_PX	= 40.0;			// where the range handle of a sensor that has none sits

	/** What the editor needs to know about the view. */
	public interface Listener
	{
		void selectionChanged (RobotItem item);
		/** The element was moved or rotated on the view. */
		void elementChanged (RobotItem item);
	}

	protected RobotDef				robot;
	protected RobotItem				selection;
	protected Listener				listener;

	static public final double		ARROW		= 22.0;			// length of the direction arrow of a sensor (pixels)
	static public final int			HANDLE_PX	= 4;			// half size of the handles (pixels), as in the world editor

	static private final int		D_NONE		= 0;
	static private final int		D_PAN		= 1;
	static private final int		D_MOVE		= 2;			// dragging the element itself
	static private final int		D_HANDLE	= 3;			// dragging one of its handles

	protected double				scale	= 200.0;			// pixels per metre
	protected double				cx, cy;						// world point at the centre of the view
	protected int					dragX, dragY;
	protected int					drag	= D_NONE;
	protected int					dragHandle;					// handle being dragged
	protected boolean				gridVisible		= true;
	protected boolean				imageVisible	= true;
	protected boolean				snapGrid		= false;	// take the handles to the grid
	protected double				gridStep		= 0.1;		// metres, recomputed from the scale
	protected double				grabX, grabY;				// where the element was grabbed (world coordinates)

	public RobotCanvas (RobotDef robot)
	{
		this.robot	= robot;
		setBackground (C_BG);
		setPreferredSize (new Dimension (600, 500));
		setFocusable (true);

		MouseAdapter	mouse = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)
			{
				RobotItem	hit;
				int			h;

				requestFocusInWindow ();
				if (e.isPopupTrigger () || javax.swing.SwingUtilities.isMiddleMouseButton (e) || e.isShiftDown ())
				{
					drag	= D_PAN;
					dragX	= e.getX ();
					dragY	= e.getY ();
					return;
				}
				// a handle of the selection first: it sits over the element itself
				h		= handleAt (e.getX (), e.getY ());
				if (h >= 0)
				{
					drag		= D_HANDLE;
					dragHandle	= h;
					return;
				}
				hit		= pick (e.getX (), e.getY ());
				setSelection (hit);
				grabX	= wx (e.getX ());
				grabY	= wy (e.getY ());
				drag	= isMovable (hit) ? D_MOVE : D_NONE;
			}

			public void mouseDragged (MouseEvent e)
			{
				double		x = wx (e.getX ()), y = wy (e.getY ());

				switch (drag)
				{
				case D_PAN:
					cx	-= (e.getX () - dragX) / scale;
					cy	+= (e.getY () - dragY) / scale;
					dragX	= e.getX ();
					dragY	= e.getY ();
					repaint ();
					break;
				case D_MOVE:
					// with snapping on, the element moves in whole steps of the grid
					translate (snap (x - grabX), snap (y - grabY));
					if (snapGrid)
					{
						grabX	+= snap (x - grabX);
						grabY	+= snap (y - grabY);
					}
					else		{ grabX = x;	grabY = y; }
					break;
				case D_HANDLE:
					setHandle (dragHandle, snap (x), snap (y));
					break;
				}
			}

			public void mouseReleased (MouseEvent e)		{ drag = D_NONE; }
		};
		addMouseListener (mouse);
		addMouseMotionListener (mouse);
		addMouseWheelListener (new MouseWheelListener ()
		{
			public void mouseWheelMoved (MouseWheelEvent e)		{ zoom (Math.pow (1.1, -e.getWheelRotation ())); }
		});
	}

	public void setListener (Listener l)			{ listener = l; }
	public RobotDef getRobot ()						{ return robot; }
	public RobotItem getSelection ()				{ return selection; }

	public void setRobot (RobotDef robot)
	{
		this.robot	= robot;
		selection	= null;
		zoomToFit ();
	}

	public void setSelection (RobotItem item)
	{
		selection	= item;
		repaint ();
		if (listener != null)		listener.selectionChanged (item);
	}

	/** The model changed behind the view. */
	public void robotChanged ()						{ repaint (); }

	public boolean isGridVisible ()					{ return gridVisible; }
	public void setGridVisible (boolean on)			{ gridVisible = on; repaint (); }
	public boolean isSnapEnabled ()					{ return snapGrid; }
	public void setSnapEnabled (boolean on)			{ snapGrid = on; }
	/** Step of the grid the view is drawing (m). */
	public double getGridStep ()					{ return gridStep; }

	/** A coordinate taken to the grid, when snapping is on. */
	public double snap (double v)
	{
		return snapGrid ? Math.rint (v / gridStep) * gridStep : v;
	}

	/** Chooses a grid step so that the lines are at least ~25 px apart. */
	private void updateGridStep ()
	{
		double[]	steps = { 0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10 };

		gridStep	= steps[steps.length - 1];
		for (int i = 0; i < steps.length; i++)
			if (steps[i] * scale >= 25.0)		{ gridStep = steps[i]; break; }
	}
	public boolean isImageVisible ()				{ return imageVisible; }
	public void setImageVisible (boolean on)		{ imageVisible = on; repaint (); }

	/* Coordinates */

	public double px (double x)						{ return getWidth () / 2.0 + (x - cx) * scale; }
	public double py (double y)						{ return getHeight () / 2.0 - (y - cy) * scale; }
	public double wx (double px)					{ return cx + (px - getWidth () / 2.0) / scale; }
	public double wy (double py)					{ return cy - (py - getHeight () / 2.0) / scale; }

	public void zoom (double factor)
	{
		scale	= Math.max (minScale (), Math.min (MAX_SCALE, scale * factor));
		updateGridStep ();
		repaint ();
	}

	/**
	 * How far out the view may be taken: far enough for everything drawn to be
	 * inside it, and then some. What a laser covers is drawn at its own size, so
	 * a fixed limit would leave part of it out of reach.
	 */
	public double minScale ()
	{
		double[]	b = contentBounds ();
		double		m = MIN_SCALE, w, h;

		if ((b == null) || (getWidth () <= 0) || (getHeight () <= 0))		return m;
		w	= Math.max (2 * Math.max (Math.abs (b[0] - cx), Math.abs (b[2] - cx)), 1e-6);
		h	= Math.max (2 * Math.max (Math.abs (b[1] - cy), Math.abs (b[3] - cy)), 1e-6);
		m	= Math.min (m, 0.5 * Math.min (getWidth () / w, getHeight () / h));
		return Math.max (m, 1e-3);
	}

	/** {minx, miny, maxx, maxy} of the robot and of what the selected sensor covers. */
	public double[] contentBounds ()
	{
		double[]	b = robotBounds ();

		for (double[] c : coverages ())
		{
			if (b == null)		b = new double[] { c[0], c[1], c[0], c[1] };
			b	= grow (b, c[0] - c[2], c[1] - c[2]);
			b	= grow (b, c[0] + c[2], c[1] + c[2]);
		}
		return b;
	}

	public void zoomIn ()							{ zoom (1.25); }
	public void zoomOut ()							{ zoom (0.8); }

	/** Frames the whole robot (drawing, bumpers, sensors and radius) and what the selection covers. */
	public void zoomToFit ()
	{
		double[]	b = contentBounds ();

		if (b == null)			{ cx = cy = 0.0; scale = 200.0; updateGridStep (); repaint (); return; }

		double	w = Math.max (b[2] - b[0], 0.2), h = Math.max (b[3] - b[1], 0.2);
		cx		= (b[0] + b[2]) / 2.0;
		cy		= (b[1] + b[3]) / 2.0;
		if ((getWidth () > 0) && (getHeight () > 0))
			scale	= Math.max (minScale (), Math.min (MAX_SCALE, 0.85 * Math.min (getWidth () / w, getHeight () / h)));
		updateGridStep ();
		repaint ();
	}

	/** {minx, miny, maxx, maxy} of everything drawn, or null when there is nothing. */
	public double[] robotBounds ()
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		boolean		any = false;

		if (robot == null)			return null;
		if (robot.radius > 0.0)
		{
			b[0] = -robot.radius;	b[1] = -robot.radius;
			b[2] = robot.radius;	b[3] = robot.radius;
			any	= true;
		}
		for (RobotDef.IconLine l : robot.icon)
		{
			b	= grow (b, l.xi, l.yi);		b = grow (b, l.xf, l.yf);		any = true;
		}
		for (RobotDef.Bumper s : robot.bumpers)
		{
			b	= grow (b, s.xi, s.yi);		b = grow (b, s.xf, s.yf);		any = true;
		}
		for (String fam : RobotDef.FAMILIES)
			for (RobotDef.Sensor s : robot.family (fam).sensors)
			{
				b	= grow (b, sx (s), sy (s));		any = true;
			}
		return any ? b : null;
	}

	/** An angle in degrees brought to (-180, 180]. */
	static private double norm180 (double a)
	{
		while (a > 180.0)		a -= 360.0;
		while (a <= -180.0)		a += 360.0;
		return a;
	}

	static private double[] grow (double[] b, double x, double y)
	{
		b[0] = Math.min (b[0], x);		b[1] = Math.min (b[1], y);
		b[2] = Math.max (b[2], x);		b[3] = Math.max (b[3], y);
		return b;
	}

	/**
	 * What the selected sensor covers, in the frame of the robot:
	 * {x, y, rangemax, rangemin, cone, orientation}, or null when there is no
	 * sensor selected or it says nothing about its range.
	 */
	public double[] coverage ()
	{
		if ((selection == null) || (selection.kind != RobotItem.SENSOR))		return null;
		return coverageOf (selection.family, selectedSensor ());
	}

	/**
	 * What is drawn as covered: what one sensor covers when one is selected, and
	 * what every sensor of a family covers when the family is.
	 */
	public java.util.List<double[]> coverages ()
	{
		java.util.List<double[]>	l = new java.util.ArrayList<double[]> ();
		double[]					c;

		if (selection == null)			return l;
		if (selection.kind == RobotItem.SENSOR)
		{
			if ((c = coverage ()) != null)		l.add (c);
		}
		else if (selection.kind == RobotItem.FAMILY)
			for (RobotDef.Sensor s : robot.family (selection.family).sensors)
				if ((c = coverageOf (selection.family, s)) != null)		l.add (c);
		return l;
	}

	/** {x, y, rangemax, rangemin, cone, orientation} of a sensor, or null when it says no range. */
	private double[] coverageOf (String fam, RobotDef.Sensor s)
	{
		double[]	d;
		double		rmax, rmin, cone;

		if (s == null)				return null;
		d	= robot.detection (fam, s);
		rmax	= Math.max (0.0, d[0]);		rmin = Math.max (0.0, d[1]);		cone = d[2];
		if (rmin > rmax)			rmin = 0.0;
		if (cone < 0.0)				cone = 0.0;
		if (cone > 360.0)			cone = 360.0;
		return new double[] { sx (s), sy (s), rmax, rmin, cone, s.orientation };
	}

	/**
	 * Writes what the selected sensor covers, where the description keeps it: in
	 * the sensor itself when its family is made of devices of their own, in the
	 * family otherwise (and then it is what all of them cover).
	 */
	private void setCoverage (double rmax, double rmin, double cone)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		rmax	= Math.max (0.0, rmax);		rmin = Math.max (0.0, Math.min (rmin, rmax));
		cone	= Math.max (0.0, Math.min (cone, 360.0));
		if (RobotDef.hasFov (selection.family))				// a camera: the aperture is its horizontal field of view
		{
			s.rangemax = rmax;		s.hfov = cone;
		}
		else if (RobotDef.hasOwnDetection (selection.family))
		{
			s.rangemax = rmax;		s.rangemin = rmin;		s.cone = cone;
		}
		else
		{
			RobotDef.Family		f = robot.family (selection.family);
			f.rangemax = rmax;		f.rangemin = rmin;		f.cone = cone;
		}
	}

	/** True when the selected sensor has a near limit to drag (a camera has not). */
	private boolean hasMinHandle ()
	{
		return (selection != null) && (selection.kind == RobotItem.SENSOR) && !RobotDef.hasFov (selection.family);
	}

	/** The edge of the sector the handles sit on: half the aperture from the direction it looks at. */
	static private double coverEdge (double[] c)		{ return Math.toRadians (c[5] + c[4] / 2); }

	/** Position of a sensor: polar (rho, theta) around the centre of the robot. */
	static public double sx (RobotDef.Sensor s)		{ return s.rho * Math.cos (Math.toRadians (s.theta)); }
	static public double sy (RobotDef.Sensor s)		{ return s.rho * Math.sin (Math.toRadians (s.theta)); }

	/* Selection */

	/** The element under a point of the view, or null. */
	public RobotItem pick (int mx, int my)
	{
		double		x = wx (mx), y = wy (my);
		double		tol = HIT / scale;

		for (String fam : RobotDef.FAMILIES)			// sensors first: they sit over the drawing
		{
			java.util.List<RobotDef.Sensor>		ss = robot.family (fam).sensors;
			for (int i = 0; i < ss.size (); i++)
				if (Math.hypot (x - sx (ss.get (i)), y - sy (ss.get (i))) <= Math.max (tol, 5.0 / scale))
					return new RobotItem (RobotItem.SENSOR, i, fam);
		}
		for (int i = 0; i < robot.bumpers.size (); i++)
		{
			RobotDef.Bumper	s = robot.bumpers.get (i);
			if (segDist (s.xi, s.yi, s.xf, s.yf, x, y) <= tol)		return new RobotItem (RobotItem.BUMPER, i);
		}
		for (int i = 0; i < robot.icon.size (); i++)
		{
			RobotDef.IconLine	l = robot.icon.get (i);
			if (segDist (l.xi, l.yi, l.xf, l.yf, x, y) <= tol)		return new RobotItem (RobotItem.LINE, i);
		}
		return null;
	}

	static public double segDist (double x1, double y1, double x2, double y2, double x, double y)
	{
		double		dx = x2 - x1, dy = y2 - y1;
		double		l2 = dx * dx + dy * dy;
		double		t = 0.0;

		if (l2 > 1e-12)		t = Math.max (0.0, Math.min (1.0, ((x - x1) * dx + (y - y1) * dy) / l2));
		return Math.hypot (x - (x1 + t * dx), y - (y1 + t * dy));
	}

	/* ------------------------------------------------------------------ */
	/* Editing the selected sensor                                         */
	/* ------------------------------------------------------------------ */

	/** The selected sensor, or null when the selection is something else. */
	public RobotDef.Sensor selectedSensor ()
	{
		if ((selection == null) || (selection.kind != RobotItem.SENSOR))		return null;
		java.util.List<RobotDef.Sensor>		ss = robot.family (selection.family).sensors;
		return (selection.index < ss.size ()) ? ss.get (selection.index) : null;
	}

	/** True for the elements the view lets the user drag. */
	public boolean isMovable (RobotItem it)
	{
		return (it != null) && ((it.kind == RobotItem.SENSOR) || (it.kind == RobotItem.LINE) || (it.kind == RobotItem.BUMPER));
	}

	/**
	 * Handles of the selection, in pixels: a sensor is dragged by its position
	 * and turned by the tip of its arrow; a drawing line and a bumper are
	 * dragged by each of their ends.
	 *
	 * @return {x0, y0, x1, y1, ...}, empty when the selection has no handles
	 */
	public double[] handles ()
	{
		if (selection == null)				return new double[0];
		switch (selection.kind)
		{
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = selectedSensor ();
			double				a;

			if (s == null)					return new double[0];
			a	= Math.toRadians (s.orientation);
			double[]	c = coverage ();
			if (c == null)
				return new double[] { px (sx (s)), py (sy (s)),
									  px (sx (s)) + ARROW * Math.cos (a), py (sy (s)) - ARROW * Math.sin (a) };

			// the ends of the segment that sets how far it reaches and how wide; a
			// sensor that says no range yet gets the far one at hand, to pull it out by
			double		e = coverEdge (c), ce = Math.cos (e), se = Math.sin (e);
			double		far = (c[2] > 0.0) ? c[2] * scale : PENDING_PX;
			if (!hasMinHandle ())							// a camera has no near limit to drag
				return new double[] { px (sx (s)), py (sy (s)),
									  px (sx (s)) + ARROW * Math.cos (a), py (sy (s)) - ARROW * Math.sin (a),
									  px (c[0]) + far * ce, py (c[1]) - far * se };
			return new double[] { px (sx (s)), py (sy (s)),
								  px (sx (s)) + ARROW * Math.cos (a), py (sy (s)) - ARROW * Math.sin (a),
								  px (c[0] + c[3] * ce), py (c[1] + c[3] * se),
								  px (c[0]) + far * ce, py (c[1]) - far * se };
		}
		case RobotItem.LINE:
		{
			if (selection.index >= robot.icon.size ())		return new double[0];
			RobotDef.IconLine	l = robot.icon.get (selection.index);
			return new double[] { px (l.xi), py (l.yi), px (l.xf), py (l.yf) };
		}
		case RobotItem.BUMPER:
		{
			if (selection.index >= robot.bumpers.size ())	return new double[0];
			RobotDef.Bumper		b = robot.bumpers.get (selection.index);
			return new double[] { px (b.xi), py (b.yi), px (b.xf), py (b.yf) };
		}
		}
		return new double[0];
	}

	/** True when the handle is the one that turns the element (the last one of a sensor). */
	public boolean isRotationHandle (int handle)
	{
		return (selection != null) && (selection.kind == RobotItem.SENSOR) && (handle == 1);
	}

	/** Index of the handle of the selection under a point of the view, or -1. */
	public int handleAt (int mx, int my)
	{
		double[]	hs = handles ();

		for (int i = 0; i < hs.length / 2; i++)
			if ((Math.abs (mx - hs[2 * i]) <= HANDLE_PX + 2) && (Math.abs (my - hs[2 * i + 1]) <= HANDLE_PX + 2))
				return i;
		return -1;
	}

	/** Takes one handle of the selection to a point of the robot. */
	public void setHandle (int handle, double x, double y)
	{
		if (selection == null)				return;
		switch (selection.kind)
		{
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = selectedSensor ();

			if (s == null)					return;
			if (handle == 0)				// where it sits: its polar position
			{
				s.rho		= Math.hypot (x, y);
				s.theta		= Math.toDegrees (Math.atan2 (y, x));
			}
			else if (handle == 1)			// where it looks at
				s.orientation	= Math.toDegrees (Math.atan2 (y - sy (s), x - sx (s)));
			else							// what it covers: how far it reaches and how wide
			{
				double[]	c = coverage ();
				double		r, cone;

				if (c == null)				return;
				r		= Math.hypot (x - c[0], y - c[1]);
				cone	= 2 * Math.abs (norm180 (Math.toDegrees (Math.atan2 (y - c[1], x - c[0])) - c[5]));
				if ((handle == 2) && hasMinHandle ())	setCoverage (c[2], r, cone);	// the near end: range min
				else									setCoverage (r, c[3], cone);	// the far end: range max
			}
			break;
		}
		case RobotItem.LINE:
		{
			if (selection.index >= robot.icon.size ())		return;
			RobotDef.IconLine	l = robot.icon.get (selection.index);
			if (handle == 0)		{ l.xi = x; l.yi = y; }
			else					{ l.xf = x; l.yf = y; }
			break;
		}
		case RobotItem.BUMPER:
		{
			if (selection.index >= robot.bumpers.size ())	return;
			RobotDef.Bumper		b = robot.bumpers.get (selection.index);
			if (handle == 0)		{ b.xi = x; b.yi = y; }
			else					{ b.xf = x; b.yf = y; }
			break;
		}
		default:
			return;
		}
		changed ();
	}

	/** Moves the selection by (dx, dy) metres. */
	public void translate (double dx, double dy)
	{
		if (selection == null)				return;
		switch (selection.kind)
		{
		case RobotItem.SENSOR:
		{
			RobotDef.Sensor		s = selectedSensor ();

			if (s == null)					return;
			moveSensor (sx (s) + dx, sy (s) + dy);
			return;
		}
		case RobotItem.LINE:
		{
			if (selection.index >= robot.icon.size ())		return;
			RobotDef.IconLine	l = robot.icon.get (selection.index);
			l.xi += dx;		l.yi += dy;		l.xf += dx;		l.yf += dy;
			break;
		}
		case RobotItem.BUMPER:
		{
			if (selection.index >= robot.bumpers.size ())	return;
			RobotDef.Bumper		b = robot.bumpers.get (selection.index);
			b.xi += dx;		b.yi += dy;		b.xf += dx;		b.yf += dy;
			break;
		}
		default:
			return;
		}
		changed ();
	}

	/** Moves the selected sensor to a point of the robot: its polar position follows. */
	public void moveSensor (double x, double y)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		s.rho		= Math.hypot (x, y);
		s.theta		= Math.toDegrees (Math.atan2 (y, x));
		changed ();
	}

	/** Turns the selected sensor towards a point of the robot. */
	public void turnSensor (double x, double y)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		s.orientation	= Math.toDegrees (Math.atan2 (y - sy (s), x - sx (s)));
		changed ();
	}

	/** The selection was edited on the view. */
	private void changed ()
	{
		repaint ();
		if (listener != null)		listener.elementChanged (selection);
	}

	/* Painting */

	private Stroke stroke (float w)					{ return new BasicStroke (w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }

	protected void paintComponent (Graphics g0)
	{
		Graphics2D	g = (Graphics2D) g0;

		super.paintComponent (g);
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (robot == null)			return;

		if (gridVisible)		drawGrid (g);
		if (imageVisible)		drawImage (g);
		drawRadius (g);
		drawIcon (g);
		drawBumpers (g);
		drawCoverage (g);
		drawSensors (g);
		drawHandles (g);
		drawScaleBar (g);
	}

	private void drawGrid (Graphics2D g)
	{
		double		step = gridStep;

		g.setStroke (stroke (1f));
		g.setColor (C_GRID);
		for (double x = Math.floor (wx (0) / step) * step; x < wx (getWidth ()); x += step)
			g.draw (new Line2D.Double (px (x), 0, px (x), getHeight ()));
		for (double y = Math.floor (wy (getHeight ()) / step) * step; y < wy (0); y += step)
			g.draw (new Line2D.Double (0, py (y), getWidth (), py (y)));

		g.setColor (C_AXIS);										// the axes of the robot
		g.draw (new Line2D.Double (0, py (0), getWidth (), py (0)));
		g.draw (new Line2D.Double (px (0), 0, px (0), getHeight ()));
	}

	/** The bitmap of the robot: drawn over the box its drawing occupies. */
	private void drawImage (Graphics2D g)
	{
		java.awt.Image	img = RobotImage.get (robot.image);
		double[]		b;

		if (img == null)				return;
		b	= RobotImage.box (iconLines (), robot.radius);
		if (b == null)					return;
		RobotImage.draw (g, img, px ((b[0] + b[2]) / 2), py ((b[1] + b[3]) / 2),
							(b[2] - b[0]) * scale, (b[3] - b[1]) * scale, 0.0);
	}

	/** The bumpers as segments, for the geometry helpers of the runtime. */
	private Line2[] bumperLines ()
	{
		Line2[]		ls = new Line2[robot.bumpers.size ()];

		for (int i = 0; i < ls.length; i++)
		{
			RobotDef.Bumper	b = robot.bumpers.get (i);
			ls[i]	= new Line2 (b.xi, b.yi, b.xf, b.yf);
		}
		return ls;
	}

	/** The drawing of the robot as segments. */
	private Line2[] iconLines ()
	{
		Line2[]		ls = new Line2[robot.icon.size ()];

		for (int i = 0; i < ls.length; i++)
		{
			RobotDef.IconLine	l = robot.icon.get (i);
			ls[i]	= new Line2 (l.xi, l.yi, l.xf, l.yf);
		}
		return ls;
	}

	private void drawRadius (Graphics2D g)
	{
		double		r;

		if (robot.radius <= 0.0)		return;
		r	= robot.radius * scale;
		g.setColor (C_RADIUS);
		g.setStroke (new BasicStroke (1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] { 5f, 4f }, 0f));
		g.draw (new Ellipse2D.Double (px (0) - r, py (0) - r, 2 * r, 2 * r));
	}

	private void drawIcon (Graphics2D g)
	{
		for (int i = 0; i < robot.icon.size (); i++)
		{
			RobotDef.IconLine	l = robot.icon.get (i);
			boolean				sel = isSel (RobotItem.LINE, i, null);
			g.setColor (sel ? C_SEL : C_ICON);
			g.setStroke (stroke (sel ? 3f : 1.5f));
			g.draw (new Line2D.Double (px (l.xi), py (l.yi), px (l.xf), py (l.yf)));
		}
	}

	private void drawBumpers (Graphics2D g)
	{
		for (int i = 0; i < robot.bumpers.size (); i++)
		{
			RobotDef.Bumper	s = robot.bumpers.get (i);
			boolean			sel = isSel (RobotItem.BUMPER, i, null);
			g.setColor (sel ? C_SEL : C_BUMPER);
			g.setStroke (stroke (sel ? 4f : 2.5f));
			g.draw (new Line2D.Double (px (s.xi), py (s.yi), px (s.xf), py (s.yf)));
		}
	}

	/**
	 * What the selected sensor covers: the circular sector centred on the
	 * direction it looks at, <code>cone</code> wide (half of it to each side),
	 * from <code>range min</code> to <code>range max</code>. The view is left
	 * where it is: a sensor that reaches far would otherwise pull the zoom out.
	 */
	private void drawCoverage (Graphics2D g)
	{
		boolean		bar = (selection != null) && (selection.kind == RobotItem.SENSOR);

		for (double[] c : coverages ())		drawSector (g, c, bar);
	}

	/** One sector: filled, outlined and, for a single sensor, with the segment its handles sit on. */
	private void drawSector (Graphics2D g, double[] c, boolean bar)
	{
		double		x = px (c[0]), y = py (c[1]);
		double		rmax = c[2] * scale, rmin = c[3] * scale;
		double		ext = c[4], a0 = c[5] - ext / 2;

		if (rmax <= 0.0)						// it has no range yet: only the segment to pull it out by
		{
			if (bar)		drawCoverBar (g, x, y, coverEdge (c), 0.0, PENDING_PX);
			return;
		}

		g.setStroke (stroke (1.2f));
		if (ext <= 0.0)														// no aperture: just how far it reaches
		{
			g.setColor (C_COVER_LINE);
			double	a = Math.toRadians (c[5]);
			g.draw (new Line2D.Double (x + rmin * Math.cos (a), y - rmin * Math.sin (a),
									   x + rmax * Math.cos (a), y - rmax * Math.sin (a)));
			return;
		}

		java.awt.geom.Path2D.Double		path = new java.awt.geom.Path2D.Double ();
		if (ext >= 360.0)													// all around: a ring, with no seam
		{
			path.setWindingRule (java.awt.geom.Path2D.WIND_EVEN_ODD);
			path.append (new Ellipse2D.Double (x - rmax, y - rmax, 2 * rmax, 2 * rmax), false);
			if (rmin > 0.0)		path.append (new Ellipse2D.Double (x - rmin, y - rmin, 2 * rmin, 2 * rmin), false);
		}
		else
		{
			path.append (new java.awt.geom.Arc2D.Double (x - rmax, y - rmax, 2 * rmax, 2 * rmax, a0, ext, java.awt.geom.Arc2D.OPEN), false);
			if (rmin > 0.0)
				path.append (new java.awt.geom.Arc2D.Double (x - rmin, y - rmin, 2 * rmin, 2 * rmin, a0 + ext, -ext, java.awt.geom.Arc2D.OPEN), true);
			else
				path.lineTo (x, y);
			path.closePath ();
		}

		g.setColor (C_COVER_FILL);
		g.fill (path);
		g.setColor (C_COVER_LINE);
		g.draw (path);

		// the segment the handles sit on: from range min to range max on one edge
		if (bar)		drawCoverBar (g, x, y, coverEdge (c), rmin, rmax);
	}

	/** The segment the coverage handles sit on, in pixels from the sensor. */
	private void drawCoverBar (Graphics2D g, double x, double y, double edge, double rmin, double rmax)
	{
		double		ce = Math.cos (edge), se = Math.sin (edge);

		g.setColor (C_SEL);
		g.setStroke (stroke (1.5f));
		g.draw (new Line2D.Double (x + rmin * ce, y - rmin * se, x + rmax * ce, y - rmax * se));
	}

	private void drawSensors (Graphics2D g)
	{
		for (String fam : RobotDef.FAMILIES)
		{
			java.util.List<RobotDef.Sensor>		ss = robot.family (fam).sensors;
			for (int i = 0; i < ss.size (); i++)
			{
				RobotDef.Sensor	s = ss.get (i);
				boolean			sel = isSel (RobotItem.SENSOR, i, fam);
				double			x = px (sx (s)), y = py (sy (s));
				double			a = Math.toRadians (s.orientation);			// where it looks at
				double			len = ARROW;

				g.setColor (sel ? C_SEL : C_SENSOR);
				g.setStroke (stroke (sel ? 2.5f : 1.5f));
				g.draw (new Line2D.Double (x, y, x + len * Math.cos (a), y - len * Math.sin (a)));
				if (!sel)		g.fill (new Ellipse2D.Double (x - 3, y - 3, 6, 6));
				if (sel || (scale > 150))
				{
					g.setFont (getFont ().deriveFont (10f));
					g.drawString (fam + i, (float) (x + 6), (float) (y - 6));
				}
			}
		}
	}

	/** The handles of the selection: a square to drag each point, a round one to turn it. */
	private void drawHandles (Graphics2D g)
	{
		double[]	hs = handles ();

		g.setStroke (stroke (1.2f));
		for (int i = 0; i < hs.length / 2; i++)
			drawHandle (g, hs[2 * i], hs[2 * i + 1], isRotationHandle (i));
	}

	/** A handle of the selection: white filled and outlined in the selection colour (square to drag, round to turn). */
	private void drawHandle (Graphics2D g, double x, double y, boolean round)
	{
		int		px = (int) Math.round (x), py = (int) Math.round (y);

		g.setColor (C_HANDLE);
		if (round)		g.fillOval (px - HANDLE_PX, py - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
		else			g.fillRect (px - HANDLE_PX, py - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
		g.setColor (C_SEL);
		if (round)		g.drawOval (px - HANDLE_PX, py - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
		else			g.drawRect (px - HANDLE_PX, py - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
	}

	private void drawScaleBar (Graphics2D g)
	{
		double		step = 0.1;
		int			y = getHeight () - 14, x = 14;

		while (step * scale < 60)		step *= 2;
		g.setColor (Color.GRAY);
		g.setStroke (stroke (1f));
		g.draw (new Line2D.Double (x, y, x + step * scale, y));
		g.draw (new Line2D.Double (x, y - 3, x, y + 3));
		g.draw (new Line2D.Double (x + step * scale, y - 3, x + step * scale, y + 3));
		g.setFont (getFont ().deriveFont (10f));
		g.drawString (RobotDef.fmt (step) + " m", (float) (x + step * scale + 6), (float) (y + 4));
	}

	private boolean isSel (int kind, int index, String fam)
	{
		return (selection != null) && (selection.kind == kind) && (selection.index == index)
				&& ((fam == null) ? (selection.family == null) : fam.equals (selection.family));
	}
}
