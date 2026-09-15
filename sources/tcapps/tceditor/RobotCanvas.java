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

	static public final double		MIN_SCALE	= 10.0;			// pixels per metre
	static public final double		MAX_SCALE	= 2000.0;
	static public final double		HIT			= 6.0;			// selection tolerance (pixels)

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
	static public final double		HANDLE		= 4.0;			// half side of the rotation handle (pixels)

	static private final int		D_NONE		= 0;
	static private final int		D_PAN		= 1;
	static private final int		D_MOVE		= 2;			// dragging a sensor (rho, theta)
	static private final int		D_ROTATE	= 3;			// dragging its handle (orientation)

	protected double				scale	= 200.0;			// pixels per metre
	protected double				cx, cy;						// world point at the centre of the view
	protected int					dragX, dragY;
	protected int					drag	= D_NONE;

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

				requestFocusInWindow ();
				if (e.isPopupTrigger () || javax.swing.SwingUtilities.isMiddleMouseButton (e) || e.isShiftDown ())
				{
					drag	= D_PAN;
					dragX	= e.getX ();
					dragY	= e.getY ();
					return;
				}
				// the handle of the selected sensor turns it; its body moves it
				if (onHandle (e.getX (), e.getY ()))			{ drag = D_ROTATE; return; }
				hit		= pick (e.getX (), e.getY ());
				setSelection (hit);
				drag	= ((hit != null) && (hit.kind == RobotItem.SENSOR)) ? D_MOVE : D_NONE;
			}

			public void mouseDragged (MouseEvent e)
			{
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
					moveSensor (wx (e.getX ()), wy (e.getY ()));
					break;
				case D_ROTATE:
					turnSensor (wx (e.getX ()), wy (e.getY ()));
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

	/* Coordinates */

	public double px (double x)						{ return getWidth () / 2.0 + (x - cx) * scale; }
	public double py (double y)						{ return getHeight () / 2.0 - (y - cy) * scale; }
	public double wx (double px)					{ return cx + (px - getWidth () / 2.0) / scale; }
	public double wy (double py)					{ return cy - (py - getHeight () / 2.0) / scale; }

	public void zoom (double factor)
	{
		scale	= Math.max (MIN_SCALE, Math.min (MAX_SCALE, scale * factor));
		repaint ();
	}

	public void zoomIn ()							{ zoom (1.25); }
	public void zoomOut ()							{ zoom (0.8); }

	/** Frames the whole robot (drawing, bumpers, sensors and radius). */
	public void zoomToFit ()
	{
		double[]	b = robotBounds ();

		if (b == null)			{ cx = cy = 0.0; scale = 200.0; repaint (); return; }

		double	w = Math.max (b[2] - b[0], 0.2), h = Math.max (b[3] - b[1], 0.2);
		cx		= (b[0] + b[2]) / 2.0;
		cy		= (b[1] + b[3]) / 2.0;
		if ((getWidth () > 0) && (getHeight () > 0))
			scale	= Math.max (MIN_SCALE, Math.min (MAX_SCALE, 0.85 * Math.min (getWidth () / w, getHeight () / h)));
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

	static private double[] grow (double[] b, double x, double y)
	{
		b[0] = Math.min (b[0], x);		b[1] = Math.min (b[1], y);
		b[2] = Math.max (b[2], x);		b[3] = Math.max (b[3], y);
		return b;
	}

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

	/** Point of the rotation handle of the selected sensor: the tip of its direction arrow. */
	private double[] handle ()
	{
		RobotDef.Sensor		s = selectedSensor ();
		double				a;

		if (s == null)				return null;
		a	= Math.toRadians (s.orientation);
		return new double[] { px (sx (s)) + ARROW * Math.cos (a), py (sy (s)) - ARROW * Math.sin (a) };
	}

	private boolean onHandle (int mx, int my)
	{
		double[]	h = handle ();
		return (h != null) && (Math.abs (mx - h[0]) <= HANDLE + 2) && (Math.abs (my - h[1]) <= HANDLE + 2);
	}

	/** Moves the selected sensor to a point of the robot: its polar position follows. */
	public void moveSensor (double x, double y)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		s.rho		= Math.hypot (x, y);
		s.theta		= Math.toDegrees (Math.atan2 (y, x));
		repaint ();
		if (listener != null)		listener.elementChanged (selection);
	}

	/** Turns the selected sensor towards a point of the robot. */
	public void turnSensor (double x, double y)
	{
		RobotDef.Sensor		s = selectedSensor ();

		if (s == null)				return;
		s.orientation	= Math.toDegrees (Math.atan2 (y - sy (s), x - sx (s)));
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

		drawGrid (g);
		drawRadius (g);
		drawIcon (g);
		drawBumpers (g);
		drawSensors (g);
		drawScaleBar (g);
	}

	private void drawGrid (Graphics2D g)
	{
		double		step = 0.1;

		while (step * scale < 20)		step *= 2;
		while (step * scale > 80)		step /= 2;

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
				g.fill (new Ellipse2D.Double (x - 3, y - 3, 6, 6));
				g.draw (new Line2D.Double (x, y, x + len * Math.cos (a), y - len * Math.sin (a)));
				if (sel)				// the tip of the arrow is the handle that turns it
					g.fill (new java.awt.geom.Rectangle2D.Double (x + len * Math.cos (a) - HANDLE, y - len * Math.sin (a) - HANDLE, 2 * HANDLE, 2 * HANDLE));
				if (sel || (scale > 150))
				{
					g.setFont (getFont ().deriveFont (10f));
					g.drawString (fam + i, (float) (x + 6), (float) (y - 6));
				}
			}
		}
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
