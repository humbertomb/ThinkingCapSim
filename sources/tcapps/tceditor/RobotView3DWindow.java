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
	 * What the selected sensor covers: the circular sector centred on the
	 * direction it looks at, <code>cone</code> wide (half of it to each side),
	 * from <code>range min</code> to <code>range max</code>, drawn flat at the
	 * height of the sensor.
	 *
	 * @return what to say about it
	 */
	private String coverage (BranchGroup bg)
	{
		RobotDef.Sensor		s;
		double[]			d;
		double				x, y, z, rmax, rmin, a0, ext;
		int					steps;

		if ((selection == null) || (selection.kind != RobotItem.SENSOR))		return "";
		if (selection.index >= robot.family (selection.family).n ())			return "";
		s		= robot.family (selection.family).sensors.get (selection.index);
		d		= robot.detection (selection.family, s);
		rmax	= d[0];		rmin = Math.max (0.0, d[1]);
		if (rmax <= 0.0)		return selection.family + selection.index + ": no range.   ";
		if (rmin > rmax)		rmin = 0.0;

		x		= s.rho * Math.cos (Math.toRadians (s.theta));
		y		= s.rho * Math.sin (Math.toRadians (s.theta));
		z		= s.height;
		ext		= Math.toRadians ((d[2] > 0.0) ? Math.min (d[2], 360.0) : 0.0);
		a0		= Math.toRadians (s.orientation) - ext / 2;
		if (ext <= 0.0)			return selection.family + selection.index + ": " + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax) + " m, no aperture.   ";

		steps	= Math.max (8, (int) Math.round (Math.toDegrees (ext) / 3.0));
		QuadArray	qa = new QuadArray (4 * steps, QuadArray.COORDINATES);
		for (int i = 0; i < steps; i++)
		{
			double	a1 = a0 + ext * i / steps, a2 = a0 + ext * (i + 1) / steps;
			qa.setCoordinate (4 * i,     new Point3d (x + rmin * Math.cos (a1), y + rmin * Math.sin (a1), z));
			qa.setCoordinate (4 * i + 1, new Point3d (x + rmax * Math.cos (a1), y + rmax * Math.sin (a1), z));
			qa.setCoordinate (4 * i + 2, new Point3d (x + rmax * Math.cos (a2), y + rmax * Math.sin (a2), z));
			qa.setCoordinate (4 * i + 3, new Point3d (x + rmin * Math.cos (a2), y + rmin * Math.sin (a2), z));
		}

		Appearance	app = new Appearance ();
		app.setColoringAttributes (new ColoringAttributes (1f, 0.55f, 0f, ColoringAttributes.SHADE_FLAT));
		app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.BLENDED, 0.65f));
		app.setPolygonAttributes (new PolygonAttributes (PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
		app.setRenderingAttributes (new RenderingAttributes ());
		bg.addChild (new Shape3D (qa, app));

		return selection.family + selection.index + ": " + RobotDef.fmt (rmin) + " to " + RobotDef.fmt (rmax)
				+ " m, " + RobotDef.fmt (d[2]) + " deg.   ";
	}

	/** The axes of the robot: X (forward) in red, Y in green. */
	private Shape3D axes ()
	{
		LineArray	la = new LineArray (4, LineArray.COORDINATES | LineArray.COLOR_3);
		Appearance	app = new Appearance ();

		la.setCoordinate (0, new Point3d (0.0, 0.0, 0.0));
		la.setCoordinate (1, new Point3d (AXIS_LEN, 0.0, 0.0));
		la.setCoordinate (2, new Point3d (0.0, 0.0, 0.0));
		la.setCoordinate (3, new Point3d (0.0, AXIS_LEN, 0.0));
		la.setColor (0, new Color3f (0.9f, 0.2f, 0.2f));		la.setColor (1, new Color3f (0.9f, 0.2f, 0.2f));
		la.setColor (2, new Color3f (0.2f, 0.8f, 0.2f));		la.setColor (3, new Color3f (0.2f, 0.8f, 0.2f));

		app.setLineAttributes (new LineAttributes (2f, LineAttributes.PATTERN_SOLID, true));
		app.setColoringAttributes (new ColoringAttributes (1f, 1f, 1f, ColoringAttributes.SHADE_FLAT));
		return new Shape3D (la, app);
	}

	/** Centres the view on the robot. */
	public void fitView ()
	{
		double	size = Math.max (2 * Math.max (robot.radius, 0.25), 1.0);

		scene.lookAt (0.0, 0.0, 0.0, 2.5 * size);
	}

	/* ------------------------------------------------------------------ */

	/** The scene of this window: closer clipping planes, and a viewpoint over the robot. */
	static protected class RobotScene extends Scene3D
	{
		public RobotScene (Canvas3D canvas)
		{
			super (canvas);
			universe.getViewer ().getView ().setFrontClipDistance (0.05);
			universe.getViewer ().getView ().setBackClipDistance (100.0);
			rho		= 0.6;
			theta	= -Math.PI / 2.0;
			len		= 5.0;
			setViewpoint ();
		}

		public void addBranch (BranchGroup bg)		{ scene.addChild (bg); }

		/** Looks at a point from a distance, keeping the current angles. */
		public void lookAt (double x, double y, double z, double distance)
		{
			focus.set (x, y, z);
			len		= Math.max (0.5, distance);
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
			len		= Math.max (0.2, Math.min (200.0, len * factor));
			setViewpoint ();
		}
	}
}
