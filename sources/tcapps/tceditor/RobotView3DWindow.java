/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.TransformGroup;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.universe.SimpleUniverse;

import tc.vrobot.RobotDef;
import tcapps.tcsim.gui.visualization.Scene3D;

/**
 * Java 3D view of the models a robot description carries: the platform
 * (<code>shapeRobot</code>) and its actuator (<code>shapeActuator</code>),
 * each one drawn where the description places it, over the axes of the robot.
 * The scene is rebuilt every time the editor reports a change, so choosing
 * another 3D file shows it right away.
 */
public class RobotView3DWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "Robot 3D View";
	static private final int		REBUILD_MS	= 120;		// coalescing delay for scene rebuilds
	static private final double		AXIS_LEN	= 1.0;		// length of the axes drawn under the robot (m)
	static private final float		BASE_SHADE	= 0.6f;		// how much darker the base of a pyramid is than its faces

	protected RobotDef				robot;
	protected Canvas3D				canvas;
	protected RobotScene			scene;
	protected BranchGroup			branch;					// detachable: everything drawn
	protected int					vmode		= Scene3D.M_MOVE;

	protected JLabel				statusLabel;
	protected JCheckBox				robotCB, actuatorCB, axesCB;
	protected RobotItem				selection;				// what the editor has selected (a sensor draws what it covers)
	protected Timer					rebuildTimer;
	protected Runnable				onHide;

	/* ------------------------------------------------------------------ */

	/**
	 * @param robot   description to show
	 * @param onHide  invoked when the user closes the window (so the editor can
	 *                update its toggle button); may be null.
	 */
	public RobotView3DWindow (RobotDef robot, Runnable onHide)
	{
		super (TITLE);
		this.robot	= robot;
		this.onHide	= onHide;

		canvas	= new Canvas3D (SimpleUniverse.getPreferredConfiguration ());
		canvas.setPreferredSize (new Dimension (700, 540));
		scene	= new RobotScene (canvas);

		statusLabel	= new JLabel (" ");
		statusLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.NORTH);
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (statusLabel, BorderLayout.SOUTH);

		installMouse ();

		rebuildTimer = new Timer (REBUILD_MS, new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ rebuildNow (); }
		});
		rebuildTimer.setRepeats (false);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)
			{
				setVisible (false);
				if (RobotView3DWindow.this.onHide != null)		RobotView3DWindow.this.onHide.run ();
			}
		});
		pack ();
		rebuildNow ();
	}

	private JToolBar buildToolBar ()
	{
		JToolBar		tb = new JToolBar ();
		ButtonGroup		group = new ButtonGroup ();
		JButton			fit, top;

		tb.setFloatable (false);
		tb.add (new JLabel (" Drag: "));
		tb.add (modeButton (group, "Move", Scene3D.M_MOVE, true));
		tb.add (modeButton (group, "Rotate", Scene3D.M_ROTATE, false));
		tb.add (modeButton (group, "Zoom", Scene3D.M_ZOOM, false));
		tb.addSeparator ();

		fit		= new JButton ("Fit", new ToolIcon (ToolIcon.ZOOM_FIT, 16));
		fit.setToolTipText ("Centre the view on the robot");
		ToolButtons.flat (fit);
		fit.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ fitView (); }
		});
		tb.add (fit);

		top		= new JButton ("Top");
		top.setToolTipText ("View from above");
		ToolButtons.flat (top);
		top.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ scene.setAngles (-Math.PI / 2.0, Math.PI / 2.0 - 0.01); }
		});
		tb.add (top);
		tb.addSeparator ();

		robotCB		= modelBox ("Robot", "Draw the 3D model of the platform");
		actuatorCB	= modelBox ("Actuator", "Draw the 3D model of the actuator (fork, arm, ...)");
		axesCB		= modelBox ("Axes", "Draw the axes of the robot (X red, Y green)");
		tb.add (robotCB);
		tb.add (actuatorCB);
		tb.add (axesCB);
		return tb;
	}

	private JCheckBox modelBox (String name, String tip)
	{
		JCheckBox	cb = new JCheckBox (name, true);

		cb.setToolTipText (tip);
		cb.setFocusable (false);
		cb.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ scheduleRebuild (); }
		});
		return cb;
	}

	private JToggleButton modeButton (ButtonGroup group, String name, final int mode, boolean on)
	{
		JToggleButton	b = new JToggleButton (name, on);

		b.setFocusable (false);
		b.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ vmode = mode; }
		});
		group.add (b);
		return b;
	}

	private void installMouse ()
	{
		MouseAdapter	ma = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)
			{
				canvas.requestFocusInWindow ();
				scene.mouseDown (e.getX (), e.getY ());
			}

			public void mouseDragged (MouseEvent e)
			{
				int		mode = vmode;

				if (SwingUtilities.isRightMouseButton (e))			mode = Scene3D.M_ROTATE;
				else if (SwingUtilities.isMiddleMouseButton (e))	mode = Scene3D.M_ZOOM;
				else if (e.isShiftDown ())							mode = Scene3D.M_ROTATE;
				scene.mouseDrag (mode, e.getX (), e.getY ());
			}

			public void mouseWheelMoved (MouseWheelEvent e)
			{
				scene.zoom (Math.pow (1.12, e.getWheelRotation ()));
			}
		};
		canvas.addMouseListener (ma);
		canvas.addMouseMotionListener (ma);
		canvas.addMouseWheelListener (ma);

		canvas.addKeyListener (new KeyAdapter ()
		{
			public void keyPressed (KeyEvent e)
			{
				switch (e.getKeyCode ())
				{
				case KeyEvent.VK_LEFT:	case KeyEvent.VK_A:		scene.keypress (Scene3D.M_MOVE, -Scene3D.KEYMOVE, 0);		break;
				case KeyEvent.VK_RIGHT:	case KeyEvent.VK_D:		scene.keypress (Scene3D.M_MOVE, Scene3D.KEYMOVE, 0);		break;
				case KeyEvent.VK_UP:	case KeyEvent.VK_W:		scene.keypress (Scene3D.M_MOVE, 0, Scene3D.KEYMOVE);		break;
				case KeyEvent.VK_DOWN:	case KeyEvent.VK_S:		scene.keypress (Scene3D.M_MOVE, 0, -Scene3D.KEYMOVE);		break;
				case KeyEvent.VK_R:		scene.keypress (Scene3D.M_ZOOM, 0, -Scene3D.KEYZOOM);		break;
				case KeyEvent.VK_F:		scene.keypress (Scene3D.M_ZOOM, 0, Scene3D.KEYZOOM);		break;
				case KeyEvent.VK_G:		scene.keypress (Scene3D.M_ROTATE, -Scene3D.KEYROTATE, 0);	break;
				case KeyEvent.VK_J:		scene.keypress (Scene3D.M_ROTATE, Scene3D.KEYROTATE, 0);	break;
				case KeyEvent.VK_H:		scene.keypress (Scene3D.M_ROTATE, 0, Scene3D.KEYROTATE);	break;
				case KeyEvent.VK_Y:		scene.keypress (Scene3D.M_ROTATE, 0, -Scene3D.KEYROTATE);	break;
				case KeyEvent.VK_0:		fitView ();		break;
				}
			}
		});
	}

	/* ------------------------------------------------------------------ */
	/* The scene                                                           */
	/* ------------------------------------------------------------------ */

	/** Another description is being edited. */
	public void setRobot (RobotDef robot)
	{
		this.robot	= robot;
		rebuildNow ();
		fitView ();
	}

	/**
	 * What the editor has selected: a sensor draws what it covers. The view is
	 * left where it is, so a sensor that reaches far does not pull the zoom out.
	 */
	public void setSelection (RobotItem it)
	{
		selection	= it;
		scheduleRebuild ();
	}

	/** The description changed: the models are drawn again (rapid changes are coalesced). */
	public void robotChanged ()
	{
		scheduleRebuild ();
	}

	protected void scheduleRebuild ()
	{
		if (isVisible ())		rebuildTimer.restart ();
	}

	protected void rebuildNow ()
	{
		BranchGroup		bg = new BranchGroup ();
		StringBuilder	st = new StringBuilder ();

		bg.setCapability (BranchGroup.ALLOW_DETACH);
		if (axesCB.isSelected ())		bg.addChild (axes ());
		st.append (model (bg, robot.shapeRobot, robotCB.isSelected (), "Robot"));
		st.append (model (bg, robot.shapeActuator, actuatorCB.isSelected (), "Actuator"));
		st.append (coverage (bg));

		if (branch != null)		branch.detach ();
		branch	= bg;
		scene.addBranch (bg);
		statusLabel.setText ((st.length () > 0) ? st.toString ().trim () : "The robot has no 3D models");
	}

	/** Adds one of the models of the robot; returns what to say about it. */
	private String model (BranchGroup bg, String path, boolean show, String what)
	{
		TransformGroup	tg;

		if ((path == null) || (path.trim ().length () == 0))		return "";
		if (!show)							return what + ": hidden.   ";
		if (!new File (path).isFile ())		return what + ": <" + path + "> not found.   ";

		tg	= scene.getCachedObject (path, null);
		if (tg == null)						return what + ": <" + path + "> cannot be read.   ";
		bg.addChild (tg);
		return what + ": " + new File (path).getName () + ".   ";
	}

	/**
	 * What the selection covers: the circular sector of the selected sensor, or
	 * of every sensor of the selected family, centred on the direction each one
	 * looks at, <code>cone</code> wide (half of it to each side), from
	 * <code>range min</code> to <code>range max</code>, drawn flat at the height
	 * of the sensor.
	 *
	 * @return what to say about it
	 */
	private String coverage (BranchGroup bg)
	{
		RobotDef.Family		f;
		int					n = 0;

		if (selection == null)					return "";
		if (selection.kind == RobotItem.SENSOR)
		{
			f	= robot.family (selection.family);
			if (selection.index >= f.n ())		return "";
			return sector (bg, selection.family, f.sensors.get (selection.index),
							selection.family + selection.index + ": ");
		}
		if ((selection.kind == RobotItem.GROUP) || (selection.kind == RobotItem.FUSED))
		{
			java.util.List<? extends RobotDef.Sector>	l = sectorsOf (selection.kind);

			if (selection.index >= l.size ())			return "";
			return sector (bg, l.get (selection.index), name (selection.kind, selection.index) + ": ");
		}
		if ((selection.kind == RobotItem.GROUPS) || (selection.kind == RobotItem.FUSEDS))
		{
			int		kind = (selection.kind == RobotItem.FUSEDS) ? RobotItem.FUSED : RobotItem.GROUP;

			for (RobotDef.Sector q : sectorsOf (kind))
				if (sector (bg, q, null).length () > 0)		n++;
			return (n > 0) ? ((kind == RobotItem.FUSED) ? "Fused sensors: " : "Area groups: ") + n + ".   " : "";
		}
		if (selection.kind != RobotItem.FAMILY)	return "";

		f	= robot.family (selection.family);
		for (RobotDef.Sensor s : f.sensors)
			if (sector (bg, selection.family, s, null).length () > 0)		n++;
		return (n > 0) ? RobotDef.familyName (selection.family) + ": " + n + " sensors.   " : "";
	}

	/**
	 * Adds the sector one sensor covers.
	 *
	 * @param what  what to call it in the status bar, or null to say nothing
	 * @return what to say about it
	 */
	private String sector (BranchGroup bg, String fam, RobotDef.Sensor s, String what)
	{
		double[]	d = robot.detection (fam, s);
		double		x, y, z, rmax, rmin, a0, ext;
		int			steps;

		rmax	= d[0];		rmin = Math.max (0.0, d[1]);
		if (rmax <= 0.0)		return (what != null) ? what + "no range.   " : "";
		if (rmin > rmax)		rmin = 0.0;
		// a camera sees a rectangle: a pyramid says it better than a flat sector
		if (RobotDef.hasFov (fam) && (s.hfov > 0.0) && (s.vfov > 0.0))		return pyramid (bg, fam, s, what);

		x		= s.rho * Math.cos (Math.toRadians (s.theta));
		y		= s.rho * Math.sin (Math.toRadians (s.theta));
		z		= s.height;
		ext		= Math.toRadians ((d[2] > 0.0) ? Math.min (d[2], 360.0) : 0.0);
		a0		= Math.toRadians (s.orientation) - ext / 2;
		if (ext <= 0.0)
			return (what != null) ? what + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax) + " m, no aperture.   " : "";

		fan (bg, x, y, z, s.orientation, s.elevation, rmax, rmin, ext, coverAppearance (fam, 1.0f));

		return (what != null) ? what + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax)
								+ " m, " + RobotDef.fmt (d[2]) + " deg.   " : " ";
	}

	/**
	 * The flat sector something covers: from <code>rmin</code> to <code>rmax</code>,
	 * <code>ext</code> wide (radians, half of it to each side of where it looks),
	 * in the plane it looks along, which its elevation tilts.
	 */
	private void fan (BranchGroup bg, double x, double y, double z, double orientation, double elevation,
					  double rmax, double rmin, double ext, Appearance app)
	{
		int			steps = Math.max (8, (int) Math.round (Math.toDegrees (ext) / 3.0));
		double[]	f = forward (orientation, elevation), w = across (orientation);
		QuadArray	qa = new QuadArray (4 * steps, QuadArray.COORDINATES);

		for (int i = 0; i < steps; i++)
		{
			double	a1 = -ext / 2 + ext * i / steps;
			double	a2 = a1 + ext / steps;
			qa.setCoordinate (4 * i,     at (x, y, z, f, w, a1, rmin));
			qa.setCoordinate (4 * i + 1, at (x, y, z, f, w, a1, rmax));
			qa.setCoordinate (4 * i + 2, at (x, y, z, f, w, a2, rmax));
			qa.setCoordinate (4 * i + 3, at (x, y, z, f, w, a2, rmin));
		}
		bg.addChild (new Shape3D (qa, app));
	}

	/**
	 * Adds the sector one virtual sensor covers, in a light green of its own: what
	 * it reads is worked out from the other sensors, not taken from the world.
	 *
	 * @return what to say about it
	 */
	private String sector (BranchGroup bg, RobotDef.Sector q, String what)
	{
		double		x, y, rmax, rmin, ext;

		rmax	= q.rangemax;		rmin = Math.max (0.0, q.rangemin);
		if (rmax <= 0.0)		return (what != null) ? what + "no range.   " : "";
		if (rmin > rmax)		rmin = 0.0;
		ext		= Math.toRadians ((q.cone > 0.0) ? Math.min (q.cone, 360.0) : 0.0);
		if (ext <= 0.0)
			return (what != null) ? what + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax) + " m, no aperture.   " : "";

		x		= q.rho * Math.cos (Math.toRadians (q.theta));
		y		= q.rho * Math.sin (Math.toRadians (q.theta));
		fan (bg, x, y, q.height, q.orientation, q.elevation, rmax, rmin, ext,
			 virtualAppearance ((q instanceof RobotDef.Fused) ? C_FUSED : C_VIRTUAL));

		return (what != null) ? what + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax)
								+ " m, " + RobotDef.fmt (q.cone) + " deg.   " : " ";
	}

	/** How what a virtual sensor covers is painted: the colour of its kind, seen through. */
	private Appearance virtualAppearance (Color3f colour)
	{
		Appearance	app = new Appearance ();

		app.setColoringAttributes (new ColoringAttributes (colour, ColoringAttributes.SHADE_FLAT));
		app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.BLENDED, 0.65f));
		app.setPolygonAttributes (new PolygonAttributes (PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
		app.setRenderingAttributes (new RenderingAttributes ());
		return app;
	}

	/**
	 * What a camera sees: the pyramid from where it sits out to its range max,
	 * <code>hfov</code> wide and <code>vfov</code> tall, with its apex on the
	 * camera. The four faces and the base are drawn.
	 *
	 * @return what to say about it
	 */
	private String pyramid (BranchGroup bg, String fam, RobotDef.Sensor s, String what)
	{
		double		x = s.rho * Math.cos (Math.toRadians (s.theta));
		double		y = s.rho * Math.sin (Math.toRadians (s.theta));
		double		z = s.height, r = s.rangemax;
		double		hw = r * Math.tan (Math.toRadians (s.hfov) / 2), hh = r * Math.tan (Math.toRadians (s.vfov) / 2);
		// where it looks at, and the two directions across it (its elevation tilts them)
		double[]	f = forward (s), w = across (s), u = up (f, w);
		Point3d		ap = new Point3d (x, y, z);
		Point3d[]	c = new Point3d[4];

		for (int i = 0; i < 4; i++)
		{
			double	sw = ((i == 0) || (i == 3)) ? -hw : hw;			// left, right
			double	sh = (i < 2) ? hh : -hh;						// top, bottom
			c[i]	= new Point3d (x + r * f[0] + sw * w[0] + sh * u[0],
								   y + r * f[1] + sw * w[1] + sh * u[1],
								   z + r * f[2] + sw * w[2] + sh * u[2]);
		}

		TriangleArray	ta = new TriangleArray (12, TriangleArray.COORDINATES);
		for (int i = 0; i < 4; i++)										// the four faces, from the camera
		{
			ta.setCoordinate (3 * i,     ap);
			ta.setCoordinate (3 * i + 1, c[i]);
			ta.setCoordinate (3 * i + 2, c[(i + 1) % 4]);
		}
		QuadArray	qa = new QuadArray (4, QuadArray.COORDINATES);		// what it sees at its range max
		for (int i = 0; i < 4; i++)		qa.setCoordinate (i, c[i]);

		bg.addChild (new Shape3D (ta, coverAppearance (fam, 1.0f)));
		bg.addChild (new Shape3D (qa, coverAppearance (fam, BASE_SHADE)));		// the base, a shade darker than the faces

		return (what != null) ? what + RobotDef.fmt (r) + " m, " + RobotDef.fmt (s.hfov) + " x "
								+ RobotDef.fmt (s.vfov) + " deg.   " : " ";
	}

	/** Where a sensor looks at: its orientation over the ground, raised by its elevation. */
	static private double[] forward (RobotDef.Sensor s)					{ return forward (s.orientation, s.elevation); }

	static private double[] forward (double orientation, double elevation)
	{
		double	a = Math.toRadians (orientation), e = Math.toRadians (elevation);

		return new double[] { Math.cos (a) * Math.cos (e), Math.sin (a) * Math.cos (e), Math.sin (e) };
	}

	/** Across what a sensor looks at, on the ground: the axis its elevation turns about. */
	static private double[] across (RobotDef.Sensor s)					{ return across (s.orientation); }

	static private double[] across (double orientation)
	{
		double	a = Math.toRadians (orientation);

		return new double[] { Math.sin (a), -Math.cos (a), 0.0 };
	}

	/** The remaining axis of the sensor: up from where it looks at. */
	static private double[] up (double[] f, double[] w)
	{
		return new double[] { w[1] * f[2] - w[2] * f[1], w[2] * f[0] - w[0] * f[2], w[0] * f[1] - w[1] * f[0] };
	}

	/** A point of a sector: turned <code>a</code> from where the sensor looks at, at distance <code>r</code>. */
	static private Point3d at (double x, double y, double z, double[] f, double[] w, double a, double r)
	{
		double	ca = Math.cos (a) * r, sa = Math.sin (a) * r;

		return new Point3d (x + ca * f[0] + sa * w[0], y + ca * f[1] + sa * w[1], z + ca * f[2] + sa * w[2]);
	}

	/**
	 * How what a sensor covers is painted: its family colour, seen through.
	 *
	 * @param shade  1 for the colour of the family, less for a darker tone of it
	 */
	private Appearance coverAppearance (String fam, float shade)
	{
		Appearance	app = new Appearance ();
		Color3f		c = familyColor (fam);

		if (shade != 1.0f)		c = new Color3f (c.x * shade, c.y * shade, c.z * shade);
		app.setColoringAttributes (new ColoringAttributes (c, ColoringAttributes.SHADE_FLAT));
		app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.BLENDED, 0.65f));
		app.setPolygonAttributes (new PolygonAttributes (PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
		app.setRenderingAttributes (new RenderingAttributes ());
		return app;
	}

	/** The colour of a sensor of an area: light green, so that it is not taken for a family of real ones. */
	static public final Color3f		C_VIRTUAL	= new Color3f (0.55f, 0.95f, 0.55f);
	/** The colour of a fused sensor: pink. */
	static public final Color3f		C_FUSED		= new Color3f (0.95f, 0.5f, 0.75f);

	/** The colour each family of sensors is drawn in, so that several of them are told apart. */
	static public Color3f familyColor (String fam)
	{
		if ("son".equals (fam))		return new Color3f (1f, 0.55f, 0f);			// orange
		if ("ir".equals (fam))		return new Color3f (1f, 0.45f, 0.75f);		// pink
		if ("lrf".equals (fam))		return new Color3f (0f, 0.85f, 0.9f);		// cyan
		if ("lsb".equals (fam))		return new Color3f (0f, 0.85f, 0.9f);		// cyan (a laser as well)
		if ("trk".equals (fam))		return new Color3f (0.4f, 0.9f, 0.4f);		// green
		return new Color3f (0.95f, 0.9f, 0.3f);									// vision: yellow
	}

	/** The axes of the robot: X (forward) in red, Y in green and Z (up) in blue. */
	private Shape3D axes ()
	{
		LineArray	la = new LineArray (6, LineArray.COORDINATES | LineArray.COLOR_3);
		Appearance	app = new Appearance ();
		Color3f[]	c = { color (RobotCanvas.C_AXIS_X), color (RobotCanvas.C_AXIS_Y), color (RobotCanvas.C_AXIS_Z) };
		Point3d[]	e = { new Point3d (AXIS_LEN, 0.0, 0.0), new Point3d (0.0, AXIS_LEN, 0.0), new Point3d (0.0, 0.0, AXIS_LEN) };

		for (int i = 0; i < 3; i++)
		{
			la.setCoordinate (2 * i, new Point3d (0.0, 0.0, 0.0));		la.setColor (2 * i, c[i]);
			la.setCoordinate (2 * i + 1, e[i]);							la.setColor (2 * i + 1, c[i]);
		}

		app.setLineAttributes (new LineAttributes (2f, LineAttributes.PATTERN_SOLID, true));
		app.setColoringAttributes (new ColoringAttributes (1f, 1f, 1f, ColoringAttributes.SHADE_FLAT));
		return new Shape3D (la, app);
	}

	static private Color3f color (java.awt.Color c)
	{
		return new Color3f (c.getRed () / 255f, c.getGreen () / 255f, c.getBlue () / 255f);
	}

	/** Centres the view on the robot and on what the selection covers. */
	public void fitView ()
	{
		double	size = Math.max (2 * Math.max (robot.radius, 0.25), 1.0);

		size	= Math.max (size, 2 * coverExtent ());
		scene.lookAt (0.0, 0.0, 0.0, 2.5 * size);
	}

	/** How far from the robot what the selection covers reaches (m); zero when nothing is shown. */
	private double coverExtent ()
	{
		RobotDef.Family		f;
		double				e = 0.0;

		if (selection == null)			return 0.0;
		if (selection.kind == RobotItem.SENSOR)
		{
			f	= robot.family (selection.family);
			return (selection.index < f.n ()) ? reach (selection.family, f.sensors.get (selection.index)) : 0.0;
		}
		if ((selection.kind == RobotItem.GROUP) || (selection.kind == RobotItem.FUSED))
		{
			java.util.List<? extends RobotDef.Sector>	l = sectorsOf (selection.kind);

			return (selection.index < l.size ()) ? reach (l.get (selection.index)) : 0.0;
		}
		if ((selection.kind == RobotItem.GROUPS) || (selection.kind == RobotItem.FUSEDS))
		{
			for (RobotDef.Sector q : sectorsOf ((selection.kind == RobotItem.FUSEDS) ? RobotItem.FUSED : RobotItem.GROUP))
				e	= Math.max (e, reach (q));
			return e;
		}
		if (selection.kind != RobotItem.FAMILY)		return 0.0;
		for (RobotDef.Sensor s : robot.family (selection.family).sensors)
			e	= Math.max (e, reach (selection.family, s));
		return e;
	}

	/** The virtual sensors of a kind, and what one of them is called. */
	private java.util.List<? extends RobotDef.Sector> sectorsOf (int kind)
	{
		if (kind == RobotItem.FUSED)		return robot.fused;
		return robot.groups;
	}

	static private String name (int kind, int index)
	{
		return ((kind == RobotItem.FUSED) ? "fusion" : "group") + index;
	}

	/** How far from the centre of the robot one virtual sensor reaches (m). */
	private double reach (RobotDef.Sector q)
	{
		return (q.rangemax > 0.0) ? q.rho + q.rangemax : 0.0;
	}

	/** How far from the centre of the robot one sensor reaches (m). */
	private double reach (String fam, RobotDef.Sensor s)
	{
		double[]	d = robot.detection (fam, s);

		return (d[0] > 0.0) ? s.rho + d[0] : 0.0;
	}

	/* ------------------------------------------------------------------ */

	/** The scene of this window: closer clipping planes, and a viewpoint over the robot. */
	static protected class RobotScene extends Scene3D
	{
		public RobotScene (Canvas3D canvas)
		{
			super (canvas);
			rho		= 0.6;
			theta	= -Math.PI / 2.0;
			len		= 5.0;
			updateClips ();
			setViewpoint ();
		}

		/**
		 * The clipping planes follow how far the view is: a radar reaching 140 m is
		 * as much a part of the scene as a robot half a metre wide, and a fixed pair
		 * of planes cannot hold both.
		 */
		private void updateClips ()
		{
			universe.getViewer ().getView ().setFrontClipDistance (Math.max (0.05, len / 1000.0));
			universe.getViewer ().getView ().setBackClipDistance (Math.max (100.0, 10.0 * len));
		}

		public void addBranch (BranchGroup bg)		{ scene.addChild (bg); }

		/** Looks at a point from a distance, keeping the current angles. */
		public void lookAt (double x, double y, double z, double distance)
		{
			focus.set (x, y, z);
			len		= Math.max (0.5, distance);
			updateClips ();
			setViewpoint ();
		}

		public void setAngles (double theta, double rho)
		{
			this.theta	= theta;
			this.rho	= rho;
			setViewpoint ();
		}

		public void zoom (double factor)
		{
			len		= Math.max (0.2, Math.min (5000.0, len * factor));
			updateClips ();
			setViewpoint ();
		}
	}
}
