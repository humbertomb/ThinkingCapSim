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
import java.util.ArrayList;

import javax.swing.JPanel;

import tc.vrobot.RobotDef;
import tc.vrobot.RobotImage;
import wucore.utils.geom.Line2;

/**
 * Flat view of a robot description, in robot coordinates: the drawing of the
 * platform, its virtual radius, the bumpers and where each sensor sits, with
 * the direction it looks at, over the lines of its 3D models. The X axis points
 * forward.
 *
 * It draws one of three projections: from above (x right, y up), from the front
 * (y right, z up) and from the side (x right, z up).
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
	/** The axes of the robot, the same colours the 3D view uses: X red, Y green, Z blue. */
	static public final Color		C_AXIS_X	= new Color (230, 51, 51);
	static public final Color		C_AXIS_Y	= new Color (51, 204, 51);
	static public final Color		C_AXIS_Z	= new Color (89, 128, 242);
	static public final Color		C_ICON		= new Color (60, 60, 60);
	static public final Color		C_RADIUS	= new Color (120, 120, 200);
	static public final Color		C_BUMPER	= new Color (200, 60, 60);
	static public final Color		C_SENSOR	= new Color (40, 120, 200);
	static public final Color		C_BAND_FILL	= new Color (255, 140, 0, 30);		// the rectangle that picks several
	static public final Color		C_WHEEL		= new Color (70, 74, 82);			// the drive train
	static public final Color		C_TREAD		= new Color (70, 74, 82, 60);		// a wheel that drives, filled
	static public final Color		C_SHAPE		= new Color (170, 175, 185);		// lines of the 3D model
	static public final Color		C_SEL		= new Color (255, 140, 0);
	static public final Color		C_HANDLE	= new Color (255, 255, 255);		// handles, as in the world editor
	static public final Color		C_COVER_FILL	= new Color (255, 140, 0, 40);	// what the selected sensor covers
	static public final Color		C_COVER_LINE	= new Color (255, 140, 0, 140);

	static public final double		MIN_SCALE	= 10.0;			// pixels per metre
	static public final double		MAX_SCALE	= 20000.0;		// 1 px = 0.05 mm: enough for the sensors of a quaky
	static public final double		HIT			= 6.0;			// selection tolerance (pixels)
	/**
	 * How near a vertex of the same kind has to be for a dragged one to be taken
	 * to it (pixels). Said in pixels, and not in metres, so that what counts as
	 * near is what the view shows as near: zooming in asks for a finer aim and
	 * lets two vertices be left a tenth of a millimetre apart, zooming out makes
	 * the catch wider.
	 */
	static public final double		VERTEX_HIT	= 9.0;
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

	/** Flat projections the view can draw. */
	static public final int			V_TOP		= 0;			// from above: x to the right, y up
	static public final int			V_FRONT		= 1;			// from the front: y to the right, z up
	static public final int			V_SIDE		= 2;			// from the side: x to the right, z up
	static public final String[]	V_NAMES		= { "Top", "Front", "Side" };

	static private final int		D_NONE		= 0;
	static private final int		D_PAN		= 1;
	static private final int		D_MOVE		= 2;			// dragging the element itself
	static private final int		D_HANDLE	= 3;			// dragging one of its handles
	static private final int		D_BAND		= 4;			// drawing the rectangle that picks several

	/** A band shorter than this is a click that picked nothing, not a selection (px). */
	static public final double		BAND_MIN	= 3.0;

	protected int					view	= V_TOP;			// projection being drawn
	protected double				scale	= 200.0;			// pixels per metre
	protected double				cx, cy;						// world point at the centre of the view
	protected int					dragX, dragY;
	protected int					drag	= D_NONE;
	protected int					dragHandle;					// handle being dragged
	protected boolean				gridVisible		= true;
	protected boolean				imageVisible	= true;
	protected boolean				shapeVisible	= true;		// the lines of the 3D model, over the projection
	protected boolean				snapGrid		= false;	// take the handles to the grid
	protected boolean				snapVertex		= false;	// take a dragged vertex to a near one of its own kind
	protected double				gridStep		= 0.1;		// metres, recomputed from the scale
	protected double				grabX, grabY;				// where the element was grabbed (world coordinates)
	protected int					bandX, bandY, bandX1, bandY1;	// the rectangle being drawn (px)
	protected java.util.List<RobotItem>	group = new ArrayList<RobotItem> ();	// what a band picked, beyond one element

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
				grabX	= wx (e.getX ());
				grabY	= wy (e.getY ());
				if (inGroup (hit))						// grabbing one of several moves them all
				{
					drag	= D_MOVE;
					return;
				}
				setSelection (hit);
				if (hit == null)						// nothing there: pick by drawing a rectangle
				{
					drag	= D_BAND;
					bandX	= bandX1 = e.getX ();
					bandY	= bandY1 = e.getY ();
					return;
				}
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
				case D_BAND:
					bandX1	= e.getX ();
					bandY1	= e.getY ();
					repaint ();
					break;
				}
			}

			public void mouseReleased (MouseEvent e)
			{
				if (drag == D_BAND)		selectBand ();
				drag	= D_NONE;
			}
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
		group.clear ();
		selection	= item;
		// a family is its sensors: selecting it takes them all, as a band would, so
		// that dragging any of them moves the whole family at once
		if ((item != null) && (item.kind == RobotItem.FAMILY) && (robot != null))
			for (int i = 0; i < robot.family (item.family).n (); i++)
				group.add (new RobotItem (RobotItem.SENSOR, i, item.family));
		repaint ();
		if (listener != null)		listener.selectionChanged (item);
	}

	/**
	 * True when what is selected is a whole family, and its group therefore holds
	 * the sensors it is made of rather than elements picked one by one.
	 */
	public boolean isFamilySelected ()
	{
		return (selection != null) && (selection.kind == RobotItem.FAMILY);
	}

	/** What a band picked, when it picked more than one element (empty otherwise). */
	public java.util.List<RobotItem> getGroup ()	{ return group; }

	/** True when an element is one of those a band picked. */
	public boolean inGroup (RobotItem it)
	{
		if ((it == null) || group.isEmpty ())		return false;
		for (RobotItem g : group)		if (g.equals (it))		return true;
		return false;
	}

	/** Everything the selection holds: the several a band picked, or the one element. */
	public java.util.List<RobotItem> selected ()
	{
		java.util.List<RobotItem>	all = new ArrayList<RobotItem> (group);

		if (all.isEmpty () && (selection != null))		all.add (selection);
		return all;
	}

	/**
	 * Takes as the selection everything inside the rectangle just drawn: whatever
	 * has a position, wherever that position is said in the description -- a
	 * sensor, a wheel, and, seen from above, a drawing line or a bumper, which go
	 * in when both of their ends do. A rectangle no bigger than a click picks
	 * nothing.
	 */
	public void selectBand ()
	{
		java.util.List<RobotItem>	found = new ArrayList<RobotItem> ();
		double						x0, y0, x1, y1;

		if ((Math.abs (bandX1 - bandX) < BAND_MIN) && (Math.abs (bandY1 - bandY) < BAND_MIN))
		{
			repaint ();
			return;
		}
		x0	= Math.min (wx (bandX), wx (bandX1));		x1 = Math.max (wx (bandX), wx (bandX1));
		y0	= Math.min (wy (bandY), wy (bandY1));		y1 = Math.max (wy (bandY), wy (bandY1));

		for (String fam : RobotDef.FAMILIES)
		{
			java.util.List<RobotDef.Sensor>		ss = robot.family (fam).sensors;
			for (int i = 0; i < ss.size (); i++)
			{
				RobotDef.Sensor		s = ss.get (i);
				if (in (x0, y0, x1, y1, sx (s), sy (s), sz (s)))		found.add (new RobotItem (RobotItem.SENSOR, i, fam));
			}
		}
		for (int i = 0; i < robot.wheels.size (); i++)
		{
			RobotDef.Wheel	w = robot.wheels.get (i);
			if (in (x0, y0, x1, y1, kx (w), ky (w), w.z))		found.add (new RobotItem (RobotItem.WHEEL, i));
		}
		if (isTop ())									// the drawing and the bumpers are flat
		{
			for (int i = 0; i < robot.bumpers.size (); i++)
			{
				RobotDef.Bumper	b = robot.bumpers.get (i);
				if (in (x0, y0, x1, y1, b.xi, b.yi, 0.0) && in (x0, y0, x1, y1, b.xf, b.yf, 0.0))
					found.add (new RobotItem (RobotItem.BUMPER, i));
			}
			for (int i = 0; i < robot.icon.size (); i++)
			{
				RobotDef.IconLine	l = robot.icon.get (i);
				if (in (x0, y0, x1, y1, l.xi, l.yi, 0.0) && in (x0, y0, x1, y1, l.xf, l.yf, 0.0))
					found.add (new RobotItem (RobotItem.LINE, i));
			}
		}

		group.clear ();
		if (found.size () == 1)			{ setSelection (found.get (0)); return; }
		selection	= null;
		group.addAll (found);
		repaint ();
		if (listener != null)			listener.selectionChanged (null);
	}

	/** True when a point of the robot falls inside a box of the view. */
	private boolean in (double x0, double y0, double x1, double y1, double x, double y, double z)
	{
		double		hh = h (x, y, z), vv = v (x, y, z);

		return (hh >= x0) && (hh <= x1) && (vv >= y0) && (vv <= y1);
	}

	/** The model changed behind the view. */
	public void robotChanged ()						{ repaint (); }

	/** The projection being drawn ({@link #V_TOP}, {@link #V_FRONT}, {@link #V_SIDE}). */
	public int getView ()							{ return view; }

	public void setView (int v)
	{
		if ((v < V_TOP) || (v > V_SIDE) || (v == view))		return;
		view	= v;
		repaint ();
	}

	/* --- the projection: a point of the robot on the two axes of the view */

	/** Horizontal coordinate of a point of the robot in the view (m). */
	public double h (double x, double y, double z)
	{
		return (view == V_FRONT) ? y : x;			// from the front the robot comes at you: what shows is its width
	}

	/** Vertical coordinate of a point of the robot in the view (m). */
	public double v (double x, double y, double z)
	{
		return (view == V_TOP) ? y : z;
	}

	/** A point of the robot in pixels. */
	public double ph (double x, double y, double z)		{ return px (h (x, y, z)); }
	public double pv (double x, double y, double z)		{ return py (v (x, y, z)); }

	/** True when the view is the one from above, the only one the flat drawing of the robot belongs to. */
	public boolean isTop ()							{ return view == V_TOP; }

	public boolean isGridVisible ()					{ return gridVisible; }
	public void setGridVisible (boolean on)			{ gridVisible = on; repaint (); }
	public boolean isSnapEnabled ()					{ return snapGrid; }
	public void setSnapEnabled (boolean on)			{ snapGrid = on; }
	public boolean isSnapVertexEnabled ()			{ return snapVertex; }
	public void setSnapVertexEnabled (boolean on)	{ snapVertex = on; }

	/** How near two vertices have to be to be made one, at the zoom of the moment (m). */
	public double getVertexStep ()					{ return VERTEX_HIT / scale; }
	/** Step of the grid the view is drawing (m). */
	public double getGridStep ()					{ return gridStep; }

	/**
	 * Step snapping takes a coordinate to: half the one the grid is drawn with,
	 * so that the middle of a cell is reachable too. The drawn grid keeps its
	 * lines a good way apart to stay readable, which on its own leaves the
	 * snapping coarse once the view is well zoomed in.
	 */
	public double getSnapStep ()					{ return gridStep / 2; }

	/** A coordinate taken to the grid, when snapping is on. */
	public double snap (double v)
	{
		double		step = getSnapStep ();

		return snapGrid ? Math.rint (v / step) * step : v;
	}

	/** Chooses a grid step so that the lines are at least ~25 px apart. */
	private void updateGridStep ()
	{
		double[]	steps = { 0.0001, 0.0002, 0.0005, 0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10 };

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
			double	hh = h (c[0], c[1], c[6]), vv = v (c[0], c[1], c[6]);
			if (b == null)		b = new double[] { hh, vv, hh, vv };
			b	= grow (b, hh - c[2], vv - c[2]);
			b	= grow (b, hh + c[2], vv + c[2]);
		}
		return b;
	}

	/** Whether the lines of the 3D models are drawn over the projection. */
	public boolean isShapeVisible ()				{ return shapeVisible; }
	public void setShapeVisible (boolean on)		{ shapeVisible = on; repaint (); }

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

	/** {minh, minv, maxh, maxv} of everything drawn, in the coordinates of the view, or null. */
	public double[] robotBounds ()
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		boolean		any = false;

		if (robot == null)			return null;
		if (robot.radius > 0.0)
		{
			b	= grow3 (b, -robot.radius, -robot.radius, 0.0);
			b	= grow3 (b, robot.radius, robot.radius, 0.0);
			any	= true;
		}
		for (RobotDef.IconLine l : robot.icon)
		{
			b	= grow3 (b, l.xi, l.yi, 0.0);		b = grow3 (b, l.xf, l.yf, 0.0);		any = true;
		}
		for (RobotDef.Bumper s : robot.bumpers)
		{
			b	= grow3 (b, s.xi, s.yi, 0.0);		b = grow3 (b, s.xf, s.yf, 0.0);		any = true;
		}
		for (RobotDef.Wheel w : robot.wheels)
		{
			double	r = w.radius + kwidth (w) / 2;
			b	= grow3 (b, kx (w) - r, ky (w) - r, w.z - w.radius);
			b	= grow3 (b, kx (w) + r, ky (w) + r, w.z + w.radius);
			any	= true;
		}
		for (String fam : RobotDef.FAMILIES)
			for (RobotDef.Sensor s : robot.family (fam).sensors)
			{
				b	= grow3 (b, sx (s), sy (s), sz (s));		any = true;
			}
		if (shapeVisible)
			for (String path : new String[] { robot.shapeRobot, robot.shapeActuator })
				for (double[] l : ShapeLines.get (path))
				{
					b	= grow3 (b, l[0], l[1], l[2]);		b = grow3 (b, l[3], l[4], l[5]);	any = true;
				}
		return any ? b : null;
	}

	/** Grows a box of the view with a point of the robot. */
	private double[] grow3 (double[] b, double x, double y, double z)
	{
		return grow (b, h (x, y, z), v (x, y, z));
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
	 * {x, y, rangemax, rangemin, cone, orientation, z, elevation, vfov}, or null
	 * when there is no sensor selected.
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

	/** {x, y, rangemax, rangemin, cone, orientation, z, elevation, vfov} of a sensor, or null when there is none. */
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
		return new double[] { sx (s), sy (s), rmax, rmin, cone, s.orientation, sz (s), s.elevation,
							  RobotDef.hasFov (fam) ? s.vfov : 0.0 };
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

	/**
	 * Takes the handle of a camera to a point of the view: how far it looks is
	 * what the point gives along the direction it looks at, and the aperture the
	 * angle it opens to it, the horizontal one from above and the vertical one
	 * from the side.
	 */
	private void setFov (double[] c, double hw, double vw)
	{
		RobotDef.Sensor		s = selectedSensor ();
		double[][]			fr = frame (c);
		double				fh, fv, n, dh, dv, len, ang;

		if (s == null)				return;
		fh	= h (fr[0][0], fr[0][1], fr[0][2]);		fv = v (fr[0][0], fr[0][1], fr[0][2]);
		n	= Math.hypot (fh, fv);
		if (n < 1e-9)				return;					// it looks across this view: nothing to read here
		fh	/= n;		fv /= n;
		dh	= hw - h (c[0], c[1], c[6]);		dv = vw - v (c[0], c[1], c[6]);
		len	= Math.hypot (dh, dv);
		if (len < 1e-9)				return;
		ang	= Math.abs (Math.toDegrees (Math.acos (Math.max (-1.0, Math.min (1.0, (dh * fh + dv * fv) / len)))));
		ang	= Math.min (ang, 89.0);
		s.rangemax	= Math.max (0.0, len * Math.cos (Math.toRadians (ang)));
		if (isTop ())				s.hfov = 2 * ang;
		else if (view == V_SIDE)	s.vfov = 2 * ang;
		changed ();
	}

	/** True when the selected sensor has a near limit to drag (a camera has not). */
	private boolean hasMinHandle ()
	{
		return (selection != null) && (selection.kind == RobotItem.SENSOR) && !RobotDef.hasFov (selection.family);
	}

	/** The three axes of a sensor: where it looks at, across it and up from it. */
	static private double[][] frame (double[] c)
	{
		double		o = Math.toRadians (c[5]), e = Math.toRadians (c[7]);
		double[]	f = { Math.cos (o) * Math.cos (e), Math.sin (o) * Math.cos (e), Math.sin (e) };
		double[]	w = { Math.sin (o), -Math.cos (o), 0.0 };
		double[]	u = { w[1] * f[2] - w[2] * f[1], w[2] * f[0] - w[0] * f[2], w[0] * f[1] - w[1] * f[0] };

		return new double[][] { f, w, u };
	}

	/**
	 * Where the handle of a camera sits in the view it is edited from: the corner
	 * of the aperture that projection shows, the horizontal one from above and the
	 * vertical one from the side. From the front the camera is seen end on, so
	 * there is nothing to drag.
	 *
	 * @return {x, y} in metres, or null when this view edits nothing of it
	 */
	private double[] fovCorner (double[] c)
	{
		double[][]	fr = frame (c);
		double		r = (c[2] > 0.0) ? c[2] : PENDING_PX / scale;
		double		a, s;
		double[]	across;

		if (isTop ())			{ a = c[4]; across = fr[1]; s = -1.0; }			// the horizontal aperture
		else if (view == V_SIDE)	{ a = c[8]; across = fr[2]; s = 1.0; }		// the vertical one
		else					return null;
		if (a <= 0.0)			a = 0.0;
		double	d = r * Math.tan (Math.toRadians (Math.min (a, 179.0)) / 2) * s;
		return new double[] { c[0] + r * fr[0][0] + d * across[0],
							  c[1] + r * fr[0][1] + d * across[1],
							  c[6] + r * fr[0][2] + d * across[2] };
	}

	/** The edge of the sector the handles sit on: half the aperture from the direction it looks at. */
	static private double coverEdge (double[] c)		{ return Math.toRadians (c[5] + c[4] / 2); }

	/** Position of a sensor: polar (rho, theta) around the centre of the robot, at its height. */
	static public double sx (RobotDef.Sensor s)		{ return s.rho * Math.cos (Math.toRadians (s.theta)); }
	static public double sy (RobotDef.Sensor s)		{ return s.rho * Math.sin (Math.toRadians (s.theta)); }
	static public double sz (RobotDef.Sensor s)		{ return s.height; }

	static public double kx (RobotDef.Wheel w)		{ return w.x; }
	static public double ky (RobotDef.Wheel w)		{ return w.y; }

	/** How wide the tread of a wheel is: what it says, or a share of its radius while it says nothing (m). */
	static public double kwidth (RobotDef.Wheel w)
	{
		if (w.width > 0.0)		return w.width;
		return Math.max (w.radius / 3, 0.005);
	}

	/**
	 * The direction a sensor looks at as it shows in the view: {dh, dv}, of unit
	 * length, or the azimuth alone when the direction is perpendicular to the view
	 * (a sensor looking straight up, seen from above).
	 */
	public double[] look (RobotDef.Sensor s)
	{
		double		o = Math.toRadians (s.orientation), e = Math.toRadians (s.elevation);
		double		fx = Math.cos (o) * Math.cos (e), fy = Math.sin (o) * Math.cos (e), fz = Math.sin (e);
		double		dh = h (fx, fy, fz), dv = v (fx, fy, fz), n = Math.hypot (dh, dv);

		if (n > 1e-9)		return new double[] { dh / n, dv / n };
		dh	= h (Math.cos (o), Math.sin (o), 0.0);		dv = v (Math.cos (o), Math.sin (o), 0.0);
		n	= Math.hypot (dh, dv);
		return (n > 1e-9) ? new double[] { dh / n, dv / n } : new double[] { 1.0, 0.0 };
	}

	/** Takes the selected sensor to a point of the view, leaving what the view does not show. */
	public void moveSensorTo (double hw, double vw)
	{
		RobotDef.Sensor		s = selectedSensor ();
		double				x, y;

		if (s == null)				return;
		x	= sx (s);	y = sy (s);
		switch (view)
		{
		case V_FRONT:	y = hw;		s.height = vw;		break;
		case V_SIDE:	x = hw;		s.height = vw;		break;
		default:		x = hw;		y = vw;				break;
		}
		s.rho		= Math.hypot (x, y);
		s.theta		= Math.toDegrees (Math.atan2 (y, x));
		changed ();
	}

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
			{
				RobotDef.Sensor		s = ss.get (i);
				if (Math.hypot (x - h (sx (s), sy (s), sz (s)), y - v (sx (s), sy (s), sz (s))) <= Math.max (tol, 5.0 / scale))
					return new RobotItem (RobotItem.SENSOR, i, fam);
			}
		}
		for (int i = 0; i < robot.wheels.size (); i++)	// then the wheels, which are bigger
		{
			RobotDef.Wheel	w = robot.wheels.get (i);
			double			r = Math.max (w.radius, tol);
			if (Math.hypot (x - h (kx (w), ky (w), w.z), y - v (kx (w), ky (w), w.z)) <= r + tol)
				return new RobotItem (RobotItem.WHEEL, i);
		}
		if (!isTop ())			return null;			// the drawing and the bumpers are flat: only from above
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

	/** The selected wheel, or null when the selection is something else. */
	public RobotDef.Wheel selectedWheel ()
	{
		if ((selection == null) || (selection.kind != RobotItem.WHEEL))		return null;
		return (selection.index < robot.wheels.size ()) ? robot.wheels.get (selection.index) : null;
	}

	/** True for the elements the view lets the user drag. */
	public boolean isMovable (RobotItem it)
	{
		return (it != null) && ((it.kind == RobotItem.SENSOR) || (it.kind == RobotItem.LINE)
								|| (it.kind == RobotItem.BUMPER) || (it.kind == RobotItem.WHEEL));
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
			double				hx, hy;
			double[]			d;

			if (s == null)					return new double[0];
			hx	= ph (sx (s), sy (s), sz (s));		hy = pv (sx (s), sy (s), sz (s));
			d	= look (s);
			double[]	c = coverage ();
			if ((c != null) && (c[8] > 0.0))						// a camera: the corner of what this view shows
			{
				double[]	k = fovCorner (c);
				if (k == null)		return new double[] { hx, hy, hx + ARROW * d[0], hy - ARROW * d[1] };
				return new double[] { hx, hy, hx + ARROW * d[0], hy - ARROW * d[1],
									  ph (k[0], k[1], k[2]), pv (k[0], k[1], k[2]) };
			}
			if (!isTop ())			c = null;						// a cone is edited from above
			if (c == null)
				return new double[] { hx, hy, hx + ARROW * d[0], hy - ARROW * d[1] };

			// the ends of the segment that sets how far it reaches and how wide; a
			// sensor that says no range yet gets the far one at hand, to pull it out by
			double		e = coverEdge (c), ce = Math.cos (e), se = Math.sin (e);
			double		far = (c[2] > 0.0) ? c[2] * scale : PENDING_PX;
			if (!hasMinHandle ())							// a camera has no near limit to drag
				return new double[] { hx, hy, hx + ARROW * d[0], hy - ARROW * d[1],
									  px (c[0]) + far * ce, py (c[1]) - far * se };
			return new double[] { hx, hy, hx + ARROW * d[0], hy - ARROW * d[1],
								  px (c[0] + c[3] * ce), py (c[1] + c[3] * se),
								  px (c[0]) + far * ce, py (c[1]) - far * se };
		}
		case RobotItem.WHEEL:
		{
			RobotDef.Wheel		w = selectedWheel ();
			double				hx, hy, len;
			double[]			d, q;

			if (w == null)					return new double[0];
			hx	= ph (kx (w), ky (w), w.z);		hy = pv (kx (w), ky (w), w.z);
			d	= roll (w);
			q	= rollRaw (w);

			// the rim handle sits at the radius of the wheel, along the way it rolls; a
			// wheel that says no radius yet gets it at hand, to pull it out by
			len	= (w.radius > 0.0) ? w.radius * Math.hypot (q[0], q[1]) * scale : ARROW;
			if (len < 1.0)		len = ARROW;								// it rolls across this view
			if (!isTop ())
				return new double[] { hx, hy, hx + len * d[0], hy - len * d[1] };

			// and, from above, the handle of the tread: half its width along the axle
			double		o = Math.toRadians (w.orientation);
			double		ax = -Math.sin (o), ay = Math.cos (o);
			double		half = kwidth (w) / 2;
			return new double[] { hx, hy, hx + len * d[0], hy - len * d[1],
								  ph (kx (w) + half * ax, ky (w) + half * ay, w.z),
								  pv (kx (w) + half * ax, ky (w) + half * ay, w.z) };
		}
		case RobotItem.LINE:
		{
			if (selection.index >= robot.icon.size ())		return new double[0];
			RobotDef.IconLine	l = robot.icon.get (selection.index);
			return new double[] { ph (l.xi, l.yi, 0), pv (l.xi, l.yi, 0), ph (l.xf, l.yf, 0), pv (l.xf, l.yf, 0) };
		}
		case RobotItem.BUMPER:
		{
			if (selection.index >= robot.bumpers.size ())	return new double[0];
			RobotDef.Bumper		b = robot.bumpers.get (selection.index);
			return new double[] { ph (b.xi, b.yi, 0), pv (b.xi, b.yi, 0), ph (b.xf, b.yf, 0), pv (b.xf, b.yf, 0) };
		}
		}
		return new double[0];
	}

	/** True when the handle is the one that turns the element (the last one of a sensor). */
	public boolean isRotationHandle (int handle)
	{
		return (selection != null) && (handle == 1)
				&& ((selection.kind == RobotItem.SENSOR) || (selection.kind == RobotItem.WHEEL));
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
			if (handle == 0)				// where it sits, as far as the view shows it
			{
				moveSensorTo (x, y);
				return;
			}
			else if (handle == 1)			// where it looks at: its orientation, or its elevation
			{
				turnSensorTo (x, y);
				return;
			}
			else							// what it covers: how far it reaches and how wide
			{
				double[]	c = coverage ();
				double		r, cone;

				if (c == null)				return;
				if (c[8] > 0.0)				{ setFov (c, x, y); return; }		// a camera: the aperture of this view
				r		= Math.hypot (x - c[0], y - c[1]);
				cone	= 2 * Math.abs (norm180 (Math.toDegrees (Math.atan2 (y - c[1], x - c[0])) - c[5]));
				if ((handle == 2) && hasMinHandle ())	setCoverage (c[2], r, cone);	// the near end: range min
				else									setCoverage (r, c[3], cone);	// the far end: range max
			}
			break;
		}
		case RobotItem.WHEEL:
		{
			if (selectedWheel () == null)	return;
			if (handle == 0)		moveWheelTo (x, y);
			else if (handle == 1)	{ turnWheelTo (x, y); sizeWheelTo (x, y); }
			else					treadWheelTo (x, y);
			return;
		}
		case RobotItem.LINE:
		{
			if (!isTop () || (selection.index >= robot.icon.size ()))		return;
			RobotDef.IconLine	l = robot.icon.get (selection.index);
			double[]			p = stick (x, y);
			if (handle == 0)		{ l.xi = p[0]; l.yi = p[1]; }
			else					{ l.xf = p[0]; l.yf = p[1]; }
			break;
		}
		case RobotItem.BUMPER:
		{
			if (!isTop () || (selection.index >= robot.bumpers.size ()))		return;
			RobotDef.Bumper		b = robot.bumpers.get (selection.index);
			double[]			p = stick (x, y);
			if (handle == 0)		{ b.xi = p[0]; b.yi = p[1]; }
			else					{ b.xf = p[0]; b.yf = p[1]; }
			break;
		}
		default:
			return;
		}
		changed ();
	}

	/**
	 * Where a dragged vertex really goes: the end of another element of its own
	 * kind -- a drawing line for a drawing line, a bumper for a bumper -- when one
	 * falls within {@link #VERTEX_HIT} pixels of (x, y), so that the two are left
	 * exactly on each other; (x, y) itself when there is none, or when vertex
	 * snapping is off.
	 *
	 * The ends of the element being dragged are no target, or a segment would
	 * collapse onto itself.
	 */
	private double[] stick (double x, double y)
	{
		double[]	at = new double[] { x, y };
		double		near = getVertexStep ();

		if (!snapVertex || !isTop () || (selection == null))		return at;
		switch (selection.kind)
		{
		case RobotItem.LINE:
			for (int i = 0; i < robot.icon.size (); i++)
			{
				RobotDef.IconLine	l = robot.icon.get (i);

				if (i == selection.index)		continue;
				near	= nearer (at, near, x, y, l.xi, l.yi);
				near	= nearer (at, near, x, y, l.xf, l.yf);
			}
			break;
		case RobotItem.BUMPER:
			for (int i = 0; i < robot.bumpers.size (); i++)
			{
				RobotDef.Bumper		b = robot.bumpers.get (i);

				if (i == selection.index)		continue;
				near	= nearer (at, near, x, y, b.xi, b.yi);
				near	= nearer (at, near, x, y, b.xf, b.yf);
			}
			break;
		default:
			break;
		}
		return at;
	}

	/** Keeps a vertex as the one to snap to when it is the nearest so far, and says how near that now is. */
	private double nearer (double[] at, double near, double x, double y, double vx, double vy)
	{
		double		d = Math.hypot (vx - x, vy - y);

		if (d >= near)			return near;
		at[0]	= vx;
		at[1]	= vy;
		return d;
	}

	/**
	 * Moves the selection by (dx, dy) metres: the several elements a band picked,
	 * or the one that is selected.
	 */
	public void translate (double dx, double dy)
	{
		for (RobotItem it : selected ())		move (it, dx, dy);
		changed ();
	}

	/**
	 * Moves one element by (dx, dy) metres, along the two axes the view shows and
	 * without saying anything about it: whoever moves a whole selection says it
	 * once, at the end.
	 */
	private void move (RobotItem it, double dx, double dy)
	{
		if (it == null)						return;
		switch (it.kind)
		{
		case RobotItem.SENSOR:
		{
			java.util.List<RobotDef.Sensor>		ss = robot.family (it.family).sensors;
			RobotDef.Sensor						s;
			double								x, y;

			if (it.index >= ss.size ())		return;
			s	= ss.get (it.index);
			x	= sx (s);	y = sy (s);
			switch (view)
			{
			case V_FRONT:	y += dx;	s.height += dy;		break;
			case V_SIDE:	x += dx;	s.height += dy;		break;
			default:		x += dx;	y += dy;			break;
			}
			s.rho		= Math.hypot (x, y);
			s.theta		= Math.toDegrees (Math.atan2 (y, x));
			return;
		}
		case RobotItem.WHEEL:
		{
			RobotDef.Wheel		w;

			if (it.index >= robot.wheels.size ())		return;
			w	= robot.wheels.get (it.index);
			switch (view)
			{
			case V_FRONT:	w.y += dx;	w.z += dy;		break;
			case V_SIDE:	w.x += dx;	w.z += dy;		break;
			default:		w.x += dx;	w.y += dy;		break;
			}
			return;
		}
		case RobotItem.LINE:
		{
			if (!isTop () || (it.index >= robot.icon.size ()))		return;
			RobotDef.IconLine	l = robot.icon.get (it.index);
			l.xi += dx;		l.yi += dy;		l.xf += dx;		l.yf += dy;
			return;
		}
		case RobotItem.BUMPER:
		{
			if (!isTop () || (it.index >= robot.bumpers.size ()))		return;
			RobotDef.Bumper		b = robot.bumpers.get (it.index);
			b.xi += dx;		b.yi += dy;		b.xf += dx;		b.yf += dy;
			return;
		}
		}
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

	/** Turns the selected sensor towards a point of the robot (seen from above). */
	public void turnSensor (double x, double y)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		s.orientation	= Math.toDegrees (Math.atan2 (y - sy (s), x - sx (s)));
		changed ();
	}

	/**
	 * Turns the selected sensor towards a point of the view: from above that is
	 * its orientation, and from the front or the side its elevation, measured
	 * along the axis the view shows of the direction it looks at.
	 */
	public void turnSensorTo (double hw, double vw)
	{
		RobotDef.Sensor		s = selectedSensor ();
		double				dh, dv, o, axis;

		if (s == null)				return;
		dh	= hw - h (sx (s), sy (s), sz (s));
		dv	= vw - v (sx (s), sy (s), sz (s));
		if (isTop ())
		{
			s.orientation	= Math.toDegrees (Math.atan2 (dv, dh));
			changed ();
			return;
		}
		// how much of the direction the view shows: forward is +h when it is positive
		o		= Math.toRadians (s.orientation);
		axis	= (view == V_FRONT) ? Math.sin (o) : Math.cos (o);
		if (Math.abs (axis) < 1e-6)		axis = 1.0;			// it looks across the view: take its front as the right
		s.elevation		= Math.toDegrees (Math.atan2 (dv, dh * Math.signum (axis)));
		changed ();
	}

	/**
	 * The direction a wheel rolls towards as it shows in the view: {dh, dv}, of
	 * unit length. A wheel does not lean, so from above it is its orientation and
	 * from the front or the side what that direction has along the axis the view
	 * shows.
	 */
	public double[] roll (RobotDef.Wheel w)
	{
		double		o = Math.toRadians (w.orientation);
		double		dh = h (Math.cos (o), Math.sin (o), 0.0), dv = v (Math.cos (o), Math.sin (o), 0.0);
		double		len = Math.hypot (dh, dv);

		if (len > 1e-9)		return new double[] { dh / len, dv / len };
		return new double[] { 1.0, 0.0 };						// it rolls across the view
	}

	/**
	 * How the way a wheel rolls shows in the view, at its own length: a point of
	 * its rim sits at the centre plus the radius times this. It goes to nothing
	 * when the wheel rolls across the view, which is where its radius cannot be
	 * read off the drawing.
	 */
	public double[] rollRaw (RobotDef.Wheel w)
	{
		double		o = Math.toRadians (w.orientation);

		return new double[] { h (Math.cos (o), Math.sin (o), 0.0), v (Math.cos (o), Math.sin (o), 0.0) };
	}

	/** Moves the selected wheel to a point of the view: what that view shows of where it sits follows. */
	public void moveWheelTo (double hw, double vw)
	{
		RobotDef.Wheel		w = selectedWheel ();

		if (w == null)				return;
		switch (view)
		{
		case V_FRONT:	w.y = hw;	w.z = vw;		break;
		case V_SIDE:	w.x = hw;	w.z = vw;		break;
		default:		w.x = hw;	w.y = vw;		break;
		}
		changed ();
	}

	/** Turns the selected wheel towards a point of the view (from above, where its plane shows). */
	public void turnWheelTo (double hw, double vw)
	{
		RobotDef.Wheel		w = selectedWheel ();

		if ((w == null) || !isTop ())		return;				// a wheel turns about the vertical: only from above
		w.orientation	= Math.toDegrees (Math.atan2 (vw - ky (w), hw - kx (w)));
		changed ();
	}

	/**
	 * Takes the rim of the selected wheel to a point of the view: how far that
	 * point is along the way the wheel rolls is its radius. Nothing happens in a
	 * view the wheel rolls across, where the radius does not show.
	 */
	public void sizeWheelTo (double hw, double vw)
	{
		RobotDef.Wheel		w = selectedWheel ();
		double[]			q;
		double				len2, r;

		if (w == null)				return;
		q		= rollRaw (w);
		len2	= q[0] * q[0] + q[1] * q[1];
		if (len2 < 1e-9)			return;
		r		= ((hw - h (kx (w), ky (w), w.z)) * q[0] + (vw - v (kx (w), ky (w), w.z)) * q[1]) / len2;
		w.radius	= Math.max (r, 0.0);
		changed ();
	}

	/**
	 * Takes the tread handle of the selected wheel to a point of the view: how far
	 * that point is across the way it rolls is half its width.
	 */
	public void treadWheelTo (double hw, double vw)
	{
		RobotDef.Wheel		w = selectedWheel ();
		double				o, ax, ay;

		if ((w == null) || !isTop ())		return;				// the tread shows across the wheel: only from above
		o		= Math.toRadians (w.orientation);
		ax		= -Math.sin (o);	ay = Math.cos (o);
		w.width	= 2 * Math.abs ((hw - kx (w)) * ax + (vw - ky (w)) * ay);
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
		if (imageVisible && isTop ())		drawImage (g);
		if (shapeVisible)		drawShape (g);
		drawRadius (g);
		drawIcon (g);
		drawBumpers (g);
		drawWheels (g);
		drawCoverage (g);
		drawSensors (g);
		drawHandles (g);
		drawBand (g);
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

		drawAxes (g);
	}

	/**
	 * The two axes of the robot the projection shows, in the colours of the 3D
	 * view, from the origin and on their positive side alone.
	 */
	private void drawAxes (Graphics2D g)
	{
		g.setStroke (stroke (1.5f));
		g.setColor (axisColor (true));
		g.draw (new Line2D.Double (px (0), py (0), getWidth (), py (0)));
		g.setColor (axisColor (false));
		g.draw (new Line2D.Double (px (0), py (0), px (0), 0));
	}

	/** The colour of the horizontal or the vertical axis of the view. */
	public Color axisColor (boolean horizontal)
	{
		if (horizontal)		return (view == V_FRONT) ? C_AXIS_Y : C_AXIS_X;
		return (view == V_TOP) ? C_AXIS_Y : C_AXIS_Z;
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
		if (isTop ())
			g.draw (new Ellipse2D.Double (px (0) - r, py (0) - r, 2 * r, 2 * r));
		else								// seen from the front or the side it is the width it takes on the floor
			g.draw (new Line2D.Double (px (0) - r, py (0), px (0) + r, py (0)));
	}

	/**
	 * The 3D model of the robot, drawn as the lines of its faces over the
	 * projection of the view. Nothing is hidden: every line is drawn.
	 */
	private void drawShape (Graphics2D g)
	{
		g.setStroke (stroke (1f));
		g.setColor (C_SHAPE);
		for (String path : new String[] { robot.shapeRobot, robot.shapeActuator })
			for (double[] l : ShapeLines.get (path))
				g.draw (new Line2D.Double (ph (l[0], l[1], l[2]), pv (l[0], l[1], l[2]),
										   ph (l[3], l[4], l[5]), pv (l[3], l[4], l[5])));
	}

	private void drawIcon (Graphics2D g)
	{
		for (int i = 0; i < robot.icon.size (); i++)
		{
			RobotDef.IconLine	l = robot.icon.get (i);
			boolean				sel = isSel (RobotItem.LINE, i, null);
			g.setColor (sel ? C_SEL : C_ICON);
			g.setStroke (stroke (sel ? 3f : 1.5f));
			g.draw (new Line2D.Double (ph (l.xi, l.yi, 0), pv (l.xi, l.yi, 0), ph (l.xf, l.yf, 0), pv (l.xf, l.yf, 0)));
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
			g.draw (new Line2D.Double (ph (s.xi, s.yi, 0), pv (s.xi, s.yi, 0), ph (s.xf, s.yf, 0), pv (s.xf, s.yf, 0)));
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
		if (c[8] > 0.0)			{ drawPyramid (g, c, bar); return; }		// a camera sees a rectangle, not a cone
		if (!isTop ())			{ drawSectorSide (g, c); return; }

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

	/**
	 * What a camera sees: the pyramid its two fields of view make, as it shows in
	 * the view. It is drawn as the outline of its five corners projected, so every
	 * projection gets the shape it should: the horizontal aperture from above, the
	 * vertical one from the side, and the rectangle it sees from the front.
	 */
	private void drawPyramid (Graphics2D g, double[] c, boolean bar)
	{
		double		o = Math.toRadians (c[5]), e = Math.toRadians (c[7]), r = c[2];
		double		hw = r * Math.tan (Math.toRadians (c[4]) / 2), hh = r * Math.tan (Math.toRadians (c[8]) / 2);
		double[]	f = { Math.cos (o) * Math.cos (e), Math.sin (o) * Math.cos (e), Math.sin (e) };
		double[]	w = { Math.sin (o), -Math.cos (o), 0.0 };
		double[]	u = { w[1] * f[2] - w[2] * f[1], w[2] * f[0] - w[0] * f[2], w[0] * f[1] - w[1] * f[0] };
		double[][]	pts = new double[5][];

		if (r <= 0.0)			return;
		pts[0]	= new double[] { ph (c[0], c[1], c[6]), pv (c[0], c[1], c[6]) };		// where the camera is
		for (int i = 0; i < 4; i++)												// what it sees at its range
		{
			double	sw = ((i == 0) || (i == 3)) ? -hw : hw;
			double	sh = (i < 2) ? hh : -hh;
			double	x = c[0] + r * f[0] + sw * w[0] + sh * u[0];
			double	y = c[1] + r * f[1] + sw * w[1] + sh * u[1];
			double	z = c[6] + r * f[2] + sw * w[2] + sh * u[2];
			pts[i + 1]	= new double[] { ph (x, y, z), pv (x, y, z) };
		}

		java.awt.geom.Path2D.Double		path = outline (pts);
		g.setStroke (stroke (1.2f));
		g.setColor (C_COVER_FILL);		g.fill (path);
		g.setColor (C_COVER_LINE);		g.draw (path);
		if (bar)
		{
			double[]	k = fovCorner (c);						// the edge its handle sits on
			if (k != null)
			{
				g.setColor (C_SEL);
				g.setStroke (stroke (1.5f));
				g.draw (new Line2D.Double (pts[0][0], pts[0][1], ph (k[0], k[1], k[2]), pv (k[0], k[1], k[2])));
			}
		}
	}

	/** The outline of a handful of points: their convex hull, as a closed path. */
	static private java.awt.geom.Path2D.Double outline (double[][] pts)
	{
		java.util.List<double[]>	p = new java.util.ArrayList<double[]> ();
		java.util.List<double[]>	hull = new java.util.ArrayList<double[]> ();
		int							lower;

		for (double[] q : pts)		p.add (q);
		java.util.Collections.sort (p, new java.util.Comparator<double[]> ()
		{
			public int compare (double[] a, double[] b)
			{
				return (a[0] != b[0]) ? Double.compare (a[0], b[0]) : Double.compare (a[1], b[1]);
			}
		});
		for (double[] q : p)										// the lower side, then the upper one
		{
			while ((hull.size () >= 2) && (cross (hull.get (hull.size () - 2), hull.get (hull.size () - 1), q) <= 0))
				hull.remove (hull.size () - 1);
			hull.add (q);
		}
		lower	= hull.size () + 1;
		for (int i = p.size () - 2; i >= 0; i--)
		{
			double[]	q = p.get (i);
			while ((hull.size () >= lower) && (cross (hull.get (hull.size () - 2), hull.get (hull.size () - 1), q) <= 0))
				hull.remove (hull.size () - 1);
			hull.add (q);
		}
		hull.remove (hull.size () - 1);

		java.awt.geom.Path2D.Double		path = new java.awt.geom.Path2D.Double ();
		for (int i = 0; i < hull.size (); i++)
		{
			if (i == 0)		path.moveTo (hull.get (i)[0], hull.get (i)[1]);
			else			path.lineTo (hull.get (i)[0], hull.get (i)[1]);
		}
		path.closePath ();
		return path;
	}

	static private double cross (double[] o, double[] a, double[] b)
	{
		return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
	}

	/**
	 * The sector as it shows on a view that is not from above: the shape it makes
	 * there, which its elevation tilts, drawn as the polygon of its own points.
	 */
	private void drawSectorSide (Graphics2D g, double[] c)
	{
		double		o = Math.toRadians (c[5]), e = Math.toRadians (c[7]);
		double		rmax = c[2], rmin = Math.min (c[3], c[2]), ext = Math.toRadians (c[4]);
		int			steps;

		if (rmax <= 0.0)		return;
		// the sector lies in the plane the sensor looks along: forward, and across it
		double[]	f = { Math.cos (o) * Math.cos (e), Math.sin (o) * Math.cos (e), Math.sin (e) };
		double[]	w = { Math.sin (o), -Math.cos (o), 0.0 };
		steps	= Math.max (8, (int) Math.round (c[4] / 3.0));

		java.awt.geom.Path2D.Double		path = new java.awt.geom.Path2D.Double ();
		for (int i = 0; i <= steps; i++)						// the far arc
		{
			double	a = -ext / 2 + ext * i / steps;
			point (path, c, f, w, a, rmax, i == 0);
		}
		if (rmin > 0.0)
			for (int i = steps; i >= 0; i--)					// and back along the near one
			{
				double	a = -ext / 2 + ext * i / steps;
				point (path, c, f, w, a, rmin, false);
			}
		else
			path.lineTo (ph (c[0], c[1], c[6]), pv (c[0], c[1], c[6]));
		path.closePath ();

		g.setStroke (stroke (1.2f));
		g.setColor (C_COVER_FILL);		g.fill (path);
		g.setColor (C_COVER_LINE);		g.draw (path);
	}

	/** One point of a sector, turned <code>a</code> from where the sensor looks at, added to the path. */
	private void point (java.awt.geom.Path2D.Double path, double[] c, double[] f, double[] w, double a, double r, boolean start)
	{
		double		ca = Math.cos (a) * r, sa = Math.sin (a) * r;
		double		x = c[0] + ca * f[0] + sa * w[0];
		double		y = c[1] + ca * f[1] + sa * w[1];
		double		z = c[6] + ca * f[2] + sa * w[2];

		if (start)		path.moveTo (ph (x, y, z), pv (x, y, z));
		else			path.lineTo (ph (x, y, z), pv (x, y, z));
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
				double			x = ph (sx (s), sy (s), sz (s)), y = pv (sx (s), sy (s), sz (s));
				double[]		d = look (s);								// where it looks at, in the view

				g.setColor (sel ? C_SEL : C_SENSOR);
				g.setStroke (stroke (sel ? 2.5f : 1.5f));
				g.draw (new Line2D.Double (x, y, x + ARROW * d[0], y - ARROW * d[1]));
				if (!sel)		g.fill (new Ellipse2D.Double (x - 3, y - 3, 6, 6));
				if (sel || (scale > 150))
				{
					g.setFont (getFont ().deriveFont (10f));
					g.drawString (fam + i, (float) (x + 6), (float) (y - 6));
				}
			}
		}
	}

	/**
	 * The wheels of the drive train: each one as the two rims of its tread, which
	 * come out as a circle from the side, as a rectangle from above and as the
	 * width of the tread from the front. A wheel that drives is filled in, and one
	 * that can be steered gets the pivot it turns about.
	 */
	private void drawWheels (Graphics2D g)
	{
		for (int i = 0; i < robot.wheels.size (); i++)
		{
			RobotDef.Wheel	w = robot.wheels.get (i);
			boolean			sel = isSel (RobotItem.WHEEL, i, null);
			double			cx = kx (w), cy = ky (w), hw = kwidth (w) / 2;
			double			o = Math.toRadians (w.orientation);
			double			ax = -Math.sin (o), ay = Math.cos (o);		// its axle
			double			x = ph (cx, cy, w.z), y = pv (cx, cy, w.z);

			g.setColor (sel ? C_SEL : C_WHEEL);
			g.setStroke (stroke (sel ? 2.2f : 1.4f));
			if (w.radius <= 0.0)									// no size yet: a mark where it sits
			{
				g.draw (new Line2D.Double (x - 4, y, x + 4, y));
				g.draw (new Line2D.Double (x, y - 4, x, y + 4));
				continue;
			}

			java.awt.geom.Path2D.Double[]	rims = new java.awt.geom.Path2D.Double[2];
			java.util.List<double[]>		all = new java.util.ArrayList<double[]> ();
			for (int k = 0; k < 2; k++)
			{
				double	s = (k == 0) ? -hw : hw;
				rims[k]	= rim (cx + s * ax, cy + s * ay, w.z, o, w.radius, all);
			}
			if (w.traction)													// it drives: filled in
			{
				g.setColor (C_TREAD);
				g.fill (outline (all.toArray (new double[0][])));
				g.setColor (sel ? C_SEL : C_WHEEL);
			}
			g.draw (rims[0]);
			g.draw (rims[1]);
			for (int q = 0; q < 4; q++)								// the tread, joining the two rims
			{
				double	a = q * Math.PI / 2;
				double	fx = Math.cos (o) * Math.cos (a), fy = Math.sin (o) * Math.cos (a), fz = Math.sin (a);
				double	px0 = cx - hw * ax + w.radius * fx, py0 = cy - hw * ay + w.radius * fy;
				double	px1 = cx + hw * ax + w.radius * fx, py1 = cy + hw * ay + w.radius * fy;
				double	pz = w.z + w.radius * fz;
				g.draw (new Line2D.Double (ph (px0, py0, pz), pv (px0, py0, pz), ph (px1, py1, pz), pv (px1, py1, pz)));
			}
			if (w.steerable)										// the pivot it steers about
			{
				g.setStroke (stroke (1.2f));
				g.draw (new Ellipse2D.Double (x - 3, y - 3, 6, 6));
			}
			if (sel || (scale > 150))
			{
				g.setFont (getFont ().deriveFont (10f));
				g.drawString ("w" + i, (float) (x + 6), (float) (y - 6));
			}
		}
	}

	/** One rim of a wheel: the circle its plane holds, projected on the view. */
	private java.awt.geom.Path2D.Double rim (double cx, double cy, double cz, double o, double r, java.util.List<double[]> pts)
	{
		java.awt.geom.Path2D.Double		path = new java.awt.geom.Path2D.Double ();
		int								steps = 32;

		for (int i = 0; i <= steps; i++)
		{
			double	a = 2 * Math.PI * i / steps;
			double	x = cx + r * Math.cos (o) * Math.cos (a);
			double	y = cy + r * Math.sin (o) * Math.cos (a);
			double	z = cz + r * Math.sin (a);
			if (i == 0)		path.moveTo (ph (x, y, z), pv (x, y, z));
			else			path.lineTo (ph (x, y, z), pv (x, y, z));
			if (pts != null)		pts.add (new double[] { ph (x, y, z), pv (x, y, z) });
		}
		path.closePath ();
		return path;
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

	/** The rectangle being drawn to pick several elements at once. */
	private void drawBand (Graphics2D g)
	{
		int		x, y, w, h;

		if (drag != D_BAND)				return;
		x	= Math.min (bandX, bandX1);		y = Math.min (bandY, bandY1);
		w	= Math.abs (bandX1 - bandX);	h = Math.abs (bandY1 - bandY);
		if ((w < BAND_MIN) && (h < BAND_MIN))		return;

		g.setColor (C_BAND_FILL);
		g.fillRect (x, y, w, h);
		g.setColor (C_SEL);
		g.setStroke (new BasicStroke (1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 4f, 3f }, 0f));
		g.drawRect (x, y, w, h);
	}

	private void drawScaleBar (Graphics2D g)
	{
		double		step = 0.1;
		int			y = getHeight () - 14, x = 14;

		while (step * scale < 60)		step *= 2;
		while ((step * scale > 240) && (step > 1e-5))		step /= 2;
		g.setColor (Color.GRAY);
		g.setStroke (stroke (1f));
		g.draw (new Line2D.Double (x, y, x + step * scale, y));
		g.draw (new Line2D.Double (x, y - 3, x, y + 3));
		g.draw (new Line2D.Double (x + step * scale, y - 3, x + step * scale, y + 3));
		g.setFont (getFont ().deriveFont (10f));
		g.drawString (RobotDef.fmt (step) + " m", (float) (x + step * scale + 6), (float) (y + 4));
	}

	/** True for an element that is selected, on its own or as one of several. */
	private boolean isSel (int kind, int index, String fam)
	{
		if ((selection != null) && (selection.kind == kind) && (selection.index == index)
				&& ((fam == null) ? (selection.family == null) : fam.equals (selection.family)))
			return true;
		return inGroup (new RobotItem (kind, index, fam));
	}
}
