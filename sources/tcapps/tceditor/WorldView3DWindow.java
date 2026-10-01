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
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.WMConnector;
import tc.shared.world.WMFArea;
import tc.shared.world.WMObject;
import tc.shared.world.WMWall;
import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tcapps.tceditor.visualization.Scene3D;
import tcapps.tceditor.visualization.Articulated3D;
import tcapps.tceditor.visualization.FloorName;
import tcapps.tceditor.visualization.Robot3D;
import tcapps.tceditor.visualization.World3D;
import tcapps.tcsimulator.simulator.SimulatorDesc;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;
import wucore.utils.geom.Polygon2;

/**
 * Java 3D view of the world being edited in {@link WorldEditorWindow}. The
 * scene is rebuilt (coalescing rapid changes) every time the editor reports a
 * modification, and the current selection is highlighted in orange. It reuses
 * the simulator's {@link Scene3D} (lights, textures, 3DS objects, viewpoint
 * control) and {@link World3D} (walls, zones, objects, docks, waypoints,
 * connectors), and adds the elements the simulator does not draw: forbidden areas,
 * path, beacons and start point.
 */
public class WorldView3DWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "World 3D View";
	static private final int		REBUILD_MS	= 120;		// coalescing delay for scene rebuilds
	/** Height of the top face of the plain floor (m). Clearly below 0 so that zones
	 *  (boxes 1 cm thick around z = 0), areas and markers never z-fight with it. */
	static private final double		FLOOR_TOP	= -0.20;
	static private final double		FLOOR_THICK	= 0.05;

	/* Colours */
	static private final Color3f	C_SEL		= new Color3f (1.0f, 0.55f, 0.0f);
	static private final Color3f	C_FAREA		= new Color3f (0.85f, 0.15f, 0.15f);
	static private final Color3f	C_PATH		= new Color3f (0.0f, 0.7f, 0.85f);
	static private final Color3f	C_BEACON	= new Color3f (0.8f, 0.0f, 0.8f);
	static private final Color3f	C_CONNP		= new Color3f (0.15f, 0.7f, 0.15f);
	static private final Color3f	C_START		= new Color3f (0.9f, 0.1f, 0.1f);
	static private final Color3f	C_FLOOR		= new Color3f (0.55f, 0.55f, 0.5f);

	/* Model */
	protected World					world;
	protected WorldItem				selection;
	protected boolean				handles = true;		// whether the selected element is marked: the simulator does without
	protected boolean				needsRebuild	= true;
	protected boolean				needsFit		= true;

	/* Java 3D */
	protected Canvas3D				canvas;
	protected EditorScene			scene;
	protected BranchGroup			worldBranch;		// detachable: World3D + extras
	protected BranchGroup			selBranch;			// detachable: selection highlight
	protected BranchGroup			robotsBranch;		// live: simulated robots (Robot3D children)
	protected java.util.List<Robot3D>	robots = new java.util.ArrayList<Robot3D> ();
	protected java.util.List<Articulated3D>	articulated = new java.util.ArrayList<Articulated3D> ();	// the articulated body of each robot, null for a rigid one
	protected java.util.List<tc.vrobot.articulated.WalkingModel>	walkers = new java.util.ArrayList<tc.vrobot.articulated.WalkingModel> ();	// what moves its joints, null for none
	protected java.util.List<long[]>	stepped = new java.util.ArrayList<long[]> ();	// when each was last moved on [ns], for the time step of its walk
	protected BranchGroup			objectsBranch;		// live: simulated animated objects (one TransformGroup each)
	protected java.util.List<TransformGroup>	objects = new java.util.ArrayList<TransformGroup> ();
	protected boolean				showAnimated	= true;	// draw the world's animated objects at their initial pose (off while simulating)
	protected boolean				showStarts		= true;	// View menu: the starting positions of the robots
	protected boolean				showNames		= true;	// View menu: the names of the robots
	protected boolean				showLabels		= true;	// View menu: the names of the objects
	protected JCheckBoxMenuItem		startsItem, labelsItem;	// ... and their items, which follow setOverlays
	protected boolean				showFOVs		= true;	// View menu: what the cameras of the robots see
	protected java.util.List<FloorName>	labels = new java.util.ArrayList<FloorName> ();	// the names of the live objects, one per object (null for an unnamed one)
	protected World3D				world3d;				// the world as drawn now (its names take the size of letter of the scene)
	protected double				nameScale		= 1.0;	// the size of letter of every name, set by the robots (see FloorName.fit)
	protected int					vmode			= Scene3D.M_MOVE;

	/* GUI */
	static private final java.awt.Font	FPS_FONT = new java.awt.Font ("SansSerif", java.awt.Font.BOLD, 12);

	protected JLabel				statusLabel;
	protected JCheckBox				floorCB;
	protected JCheckBox				followCB;
	protected Timer					rebuildTimer;
	protected Runnable				onHide;

	/* ------------------------------------------------------------------ */

	/**
	 * @param onHide invoked when the user closes the window (so the editor can
	 *               update its toggle button); may be null.
	 */
	public WorldView3DWindow (World world, Runnable onHide)
	{
		super (TITLE);
		this.world	= world;
		this.onHide	= onHide;

		canvas	= new Canvas3D (SimpleUniverse.getPreferredConfiguration ())
		{
			private static final long	serialVersionUID = 1L;

			// how many frames were drawn: counted as they are swapped, told once a second
			private int		frames;
			private long	since	= System.currentTimeMillis ();
			private String	fps		= "";

			public void postSwap ()
			{
				long	now = System.currentTimeMillis ();

				frames++;
				if ((now - since) >= 1000)
				{
					fps		= String.format ("%.0f fps", frames * 1000.0 / (now - since));
					frames	= 0;
					since	= now;
				}
			}

			public void postRender ()
			{
				if (fps.length () == 0)		return;

				javax.media.j3d.J3DGraphics2D	g = getGraphics2D ();

				g.setFont (FPS_FONT);
				int		w = g.getFontMetrics ().stringWidth (fps);

				g.setColor (java.awt.Color.BLACK);
				g.drawString (fps, getWidth () - w - 7, 17);
				g.setColor (java.awt.Color.WHITE);
				g.drawString (fps, getWidth () - w - 8, 16);
				g.flush (false);
			}
		};
		canvas.setPreferredSize (new Dimension (800, 600));
		scene	= new EditorScene (canvas);

		statusLabel	= new JLabel (" ");
		statusLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.NORTH);
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (statusLabel, BorderLayout.SOUTH);
		setJMenuBar (buildMenuBar ());

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
				if (WorldView3DWindow.this.onHide != null)		WorldView3DWindow.this.onHide.run ();
			}
		});
		pack ();
	}

	private JToolBar buildToolBar ()
	{
		JToolBar		tb = new JToolBar ();
		tb.setFloatable (false);
		ButtonGroup		group = new ButtonGroup ();

		tb.add (new JLabel (" Drag: "));
		tb.add (modeButton (group, "Move", Scene3D.M_MOVE, true));
		tb.add (modeButton (group, "Rotate", Scene3D.M_ROTATE, false));
		tb.add (modeButton (group, "Zoom", Scene3D.M_ZOOM, false));
		tb.addSeparator ();

		JButton		fit = new JButton ("Fit", new ToolIcon (ToolIcon.ZOOM_FIT, 16));
		fit.setToolTipText ("Centre the view on the whole map");
		ToolButtons.flat (fit);
		fit.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ fitView (); }
		});
		tb.add (fit);

		JButton		top = new JButton ("Top");
		top.setToolTipText ("View from above");
		ToolButtons.flat (top);
		top.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ scene.setAngles (-Math.PI / 2.0, Math.PI / 2.0 - 0.01); }
		});
		tb.add (top);
		tb.addSeparator ();

		floorCB	= new JCheckBox ("Floor", (world == null) || (world.zones ().n () == 0));	// off by default when the world has zones
		floorCB.setToolTipText ("Draw a plain floor under the map");
		floorCB.setFocusable (false);
		floorCB.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ scheduleRebuild (); }
		});
		tb.add (floorCB);

		followCB = new JCheckBox ("Follow selection", false);
		followCB.setToolTipText ("Centre the view on the selected element");
		followCB.setFocusable (false);
		tb.add (followCB);

		return tb;
	}

	/** The View menu: what is drawn over the world, each on or off. */
	private JMenuBar buildMenuBar ()
	{
		JMenuBar	mb = new JMenuBar ();
		JMenu		view = new JMenu ("View");

		view.add (startsItem = check ("Show starting positions", showStarts, new Runnable ()
		{
			public void run ()		{ showStarts = !showStarts;	scheduleRebuild (); }
		}));
		view.add (check ("Show robot names", showNames, new Runnable ()
		{
			public void run ()		{ showNames = !showNames;	updateNames (); }
		}));
		view.add (labelsItem = check ("Show object labels", showLabels, new Runnable ()
		{
			public void run ()		{ showLabels = !showLabels;	updateLabels ();	scheduleRebuild (); }
		}));
		view.add (check ("Show camera FOVs", showFOVs, new Runnable ()
		{
			public void run ()		{ showFOVs = !showFOVs;	updateFOVs (); }
		}));
		mb.add (view);
		return mb;
	}

	private JCheckBoxMenuItem check (String text, boolean on, final Runnable toggle)
	{
		JCheckBoxMenuItem	mi = new JCheckBoxMenuItem (text, on);

		mi.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ toggle.run (); }
		});
		return mi;
	}

	/** The names of the live objects on or off the scene, as the View menu says. */
	private void updateLabels ()
	{
		// they all follow the menu: every one is on when it says so, and off otherwise
		if (objectsBranch == null)		return;
		for (FloorName fn : labels)
		{
			if (fn == null)			continue;
			if (showLabels)			objectsBranch.addChild (fn);
			else					fn.detach ();
		}
	}

	/**
	 * The one size of letter of the scene: the one at which every robot's name
	 * fits its robot (the smallest of them), given to every name there is -- the
	 * robots', the live objects' and the world's. As written, with no robot.
	 */
	private void updateNameScale ()
	{
		double	s = 1.0;

		for (Robot3D r : robots)		s = Math.min (s, r.nameFit ());
		nameScale	= s;
		for (Robot3D r : robots)		if (r.name () != null)		r.name ().setScale (s);
		for (FloorName fn : labels)		if (fn != null)				fn.setScale (s);
		if (world3d != null)			for (FloorName fn : world3d.names ())		fn.setScale (s);
	}

	/** The names of the live robots on or off the floor, as the View menu says. */
	private void updateNames ()
	{
		for (Robot3D r : robots)		r.showName (showNames);
	}

	/** What the cameras of the live robots see, on or off, as the View menu says. */
	private void updateFOVs ()
	{
		for (Robot3D r : robots)		r.showCameras (showFOVs);
	}

	/**
	 * The outer walls of the world, which the prisms of the cameras are cut at:
	 * the box round every wall (xmin, ymin, xmax, ymax); null with no walls.
	 */
	private double[] wallBounds ()
	{
		if ((world == null) || (world.walls ().n () == 0))		return null;

		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };

		for (int i = 0; i < world.walls ().n (); i++)
		{
			WMWall	w = world.walls ().at (i);

			for (Point2 p : new Point2[] { w.edge.orig (), w.edge.dest () })
			{
				b[0] = Math.min (b[0], p.x ());	b[1] = Math.min (b[1], p.y ());
				b[2] = Math.max (b[2], p.x ());	b[3] = Math.max (b[3], p.y ());
			}
		}
		return b;
	}

	private JToggleButton modeButton (ButtonGroup group, String name, final int mode, boolean sel)
	{
		JToggleButton	b = new JToggleButton (name, sel);
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
				int	mode = vmode;
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

		// same keys as the simulator window
		canvas.addKeyListener (new KeyAdapter ()
		{
			public void keyPressed (KeyEvent e)
			{
				switch (e.getKeyCode ())
				{
				case KeyEvent.VK_LEFT:	case KeyEvent.VK_A:		scene.keypress (Scene3D.M_MOVE, -Scene3D.KEYMOVE, 0);	break;
				case KeyEvent.VK_RIGHT:	case KeyEvent.VK_D:		scene.keypress (Scene3D.M_MOVE, Scene3D.KEYMOVE, 0);	break;
				case KeyEvent.VK_UP:	case KeyEvent.VK_W:		scene.keypress (Scene3D.M_MOVE, 0, Scene3D.KEYMOVE);	break;
				case KeyEvent.VK_DOWN:	case KeyEvent.VK_S:		scene.keypress (Scene3D.M_MOVE, 0, -Scene3D.KEYMOVE);	break;
				case KeyEvent.VK_R:		scene.keypress (Scene3D.M_ZOOM, 0, -Scene3D.KEYZOOM);	break;
				case KeyEvent.VK_F:		scene.keypress (Scene3D.M_ZOOM, 0, Scene3D.KEYZOOM);	break;
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
	/* Public API (called by the editor)                                   */
	/* ------------------------------------------------------------------ */

	/** The editor's world changed (or is another instance). */
	public void setWorld (World world)
	{
		if (this.world != world)
		{
			needsFit = true;
			// the plain floor is only useful when the world has no zones of its own
			if ((floorCB != null) && (world != null))		floorCB.setSelected (world.zones ().n () == 0);
		}
		this.world	= world;
		scheduleRebuild ();
	}

	/** The editor is dragging something: same as setWorld but never refits the view. */
	public void worldChanged ()
	{
		scheduleRebuild ();
	}

	/**
	 * What the View menu starts with: the starting positions of the robots and
	 * the names of the objects, on or off (both on by default; the simulator
	 * starts with both off). The menu follows, and can turn them on again.
	 */
	public void setOverlays (boolean starts, boolean labels)
	{
		if (showStarts != starts)		{ showStarts = starts;	scheduleRebuild (); }
		if (showLabels != labels)		{ showLabels = labels;	updateLabels ();	scheduleRebuild (); }
		if (startsItem != null)			startsItem.setSelected (showStarts);
		if (labelsItem != null)			labelsItem.setSelected (showLabels);
	}

	/**
	 * Whether the selected element is marked with its handles in the view. The
	 * simulator turns it off: what is picked on its 2D view is looked at, not
	 * edited, and the markers would only hide the robots and objects.
	 */
	public void setHandlesShown (boolean b)
	{
		handles	= b;
		if (isVisible ())		updateSelection ();
	}

	public void setSelection (WorldItem item)
	{
		selection = item;
		if (isVisible ())
		{
			updateSelection ();
			if (followCB.isSelected () && WorldEditor.valid (world, item))
			{
				Point2[]	hs = WorldEditor.handles (world, item);
				if (hs.length > 0)		scene.setFocus (hs[0].x (), hs[0].y ());
			}
		}
	}

	public void fitView ()
	{
		double[]	b = WorldEditor.bounds (world);
		if (b == null)
			b = new double[] { world.start_x () - 5, world.start_y () - 5, world.start_x () + 5, world.start_y () + 5 };
		scene.fit (b);
		needsFit = false;
	}

	public void setVisible (boolean b)
	{
		super.setVisible (b);
		if (b && needsRebuild)		rebuildNow ();
	}

	/* ------------------------------------------------------------------ */
	/* Scene construction                                                  */
	/* ------------------------------------------------------------------ */

	private void scheduleRebuild ()
	{
		needsRebuild = true;
		if (isVisible ())		rebuildTimer.restart ();
	}

	private void rebuildNow ()
	{
		needsRebuild = false;
		long		t0 = System.currentTimeMillis ();
		try
		{
			BranchGroup		bg = new BranchGroup ();
			bg.setCapability (BranchGroup.ALLOW_DETACH);
			world3d	= new World3D (world, scene, showAnimated, true, showLabels);
			for (FloorName fn : world3d.names ())		fn.setScale (nameScale);
			bg.addChild (world3d);
			bg.addChild (createExtras ());
			if (floorCB.isSelected ())
			{
				BranchGroup	floor = createFloor ();
				if (floor != null)		bg.addChild (floor);
			}
			bg.compile ();

			if (worldBranch != null)	worldBranch.detach ();
			worldBranch = bg;
			scene.addBranch (bg);

			updateSelection ();
			if (needsFit)		fitView ();

			int		n = 0;
			for (int k = 0; k < WorldItem.DEFAULTS; k++)	n += WorldEditor.count (world, k);
			statusLabel.setText (n + " elements   |   left drag: " + modeName () + ", right drag: rotate, wheel: zoom, keys: WASD move, R/F zoom, G/J/H/Y rotate, 0 fit"
					+ "   |   rebuilt in " + (System.currentTimeMillis () - t0) + " ms");
		} catch (Throwable e)
		{
			e.printStackTrace ();
			statusLabel.setText ("Cannot build the 3D scene: " + e);
		}
	}

	private String modeName ()
	{
		return (vmode == Scene3D.M_MOVE) ? "move" : (vmode == Scene3D.M_ROTATE) ? "rotate" : "zoom";
	}

	/** Elements not drawn by World3D: forbidden areas, path, beacons, connector paths, start point. */
	private BranchGroup createExtras ()
	{
		BranchGroup		bg = new BranchGroup ();
		int				i;

		// forbidden areas: translucent red polygon just above the floor
		for (i = 0; i < world.fareas ().n (); i++)
		{
			WMFArea		f = world.fareas ().at (i);
			Shape3D		s = polygon (f.polygon, 0.03, C_FAREA, 0.45f);
			if (s != null)		bg.addChild (s);
			bg.addChild (polyline (f.polygon, true, 0.035, C_FAREA, 2f));
		}

		// path
		if (world.path ().size () >= 2)
		{
			Point2[]	pts = world.path ().toArray (new Point2[0]);
			bg.addChild (polyline (pts, false, 0.03, C_PATH, 3f));
		}
		for (i = 0; i < world.path ().size (); i++)
			bg.addChild (sphere (world.path ().get (i).x (), world.path ().get (i).y (), pz (world.path ().get (i)) + 0.03, 0.06, C_PATH));

		// strip beacons: thin vertical plates
		for (i = 0; i < world.beacons ().size (); i++)
		{
			WMBeacon		b = world.beacons ().get (i);
			Transform3D		t = new Transform3D ();
			t.rotZ (b.pos.alpha);
			double			h = Math.max (0.02, b.height);
			t.setTranslation (new Vector3d (b.pos.x (), b.pos.y (), b.pos.z () + h / 2.0));
			TransformGroup	tg = new TransformGroup (t);
			tg.addChild (new Box ((float) Math.max (0.01, b.width / 2.0), 0.01f, (float) (h / 2.0), matAppearance (C_BEACON, 0f)));
			bg.addChild (tg);
		}

		// cylindrical beacons
		for (i = 0; i < world.cbeacons ().size (); i++)
		{
			WMCBeacon		b = world.cbeacons ().get (i);
			bg.addChild (cylinder (b.pos.x (), b.pos.y (), b.pos.z (), Math.max (0.01, b.radius ()), Math.max (0.02, b.height), C_BEACON, 0f));
		}

		// connector crossing paths
		for (i = 0; i < world.connectors ().n (); i++)
		{
			WMConnector		d = world.connectors ().at (i);
			bg.addChild (polyline (new Point2[] { d.path.orig (), d.path.dest () }, false, 0.02, C_CONNP, 2f));
		}

		// start point: red disc with heading bar
		if (showStarts)		for (tc.shared.world.WMStart st : world.starts ())
		{
			double	sx = st.x (), sy = st.y (), sz = st.z (), sa = st.orientation;
			// as wide as the start point says it is, with the heading bar going on past
			// the disc 30% of its diameter
			double	sr = st.radius (), sl = sr * (1.0 + WorldCanvas.HEADING);

			bg.addChild (cylinder (sx, sy, sz, sr, 0.04, C_START, 0.3f));
			bg.addChild (segment (sx, sy, sz + 0.05, sx + sl * Math.cos (sa), sy + sl * Math.sin (sa), sz + 0.05, C_START, 3f));
		}

		return bg;
	}

	private BranchGroup createFloor ()
	{
		double[]	b = WorldEditor.bounds (world);
		if (b == null)			return null;
		double		m = 1.0;		// margin
		double		w = (b[2] - b[0]) / 2.0 + m, h = (b[3] - b[1]) / 2.0 + m;
		Transform3D	t = new Transform3D ();
		t.setTranslation (new Vector3d ((b[0] + b[2]) / 2.0, (b[1] + b[3]) / 2.0, FLOOR_TOP - FLOOR_THICK / 2.0));
		TransformGroup	tg = new TransformGroup (t);
		tg.addChild (new Box ((float) w, (float) h, (float) (FLOOR_THICK / 2.0), matAppearance (C_FLOOR, 0f)));
		BranchGroup	bg = new BranchGroup ();
		bg.addChild (tg);
		return bg;
	}

	/** Orange markers on the handles of the selected element (and its outline). */
	private void updateSelection ()
	{
		if (selBranch != null)
		{
			selBranch.detach ();
			selBranch = null;
		}
		if (!handles)			return;
		if (!WorldEditor.valid (world, selection) || WorldItem.isSettings (selection.kind) || (selection.kind == WorldItem.ICON))		return;

		BranchGroup		bg = new BranchGroup ();
		bg.setCapability (BranchGroup.ALLOW_DETACH);
		Point2[]		hs = WorldEditor.handles (world, selection);
		double			base = WorldEditor.elevation (world, selection);		// handles are planar: lift them to the element
		double			z = base + 0.08;

		switch (selection.kind)
		{
		case WorldItem.WALL:
		{
			WMWall	w = world.walls ().at (selection.index);
			bg.addChild (segment (hs[0].x (), hs[0].y (), w.edge.z1 () + 0.02, hs[1].x (), hs[1].y (), w.edge.z2 () + 0.02, C_SEL, 4f));
			bg.addChild (segment (hs[0].x (), hs[0].y (), w.edge.z1 () + w.height + 0.02, hs[1].x (), hs[1].y (), w.edge.z2 () + w.height + 0.02, C_SEL, 4f));
			break;
		}
		case WorldItem.CONNECTOR:
		{
			WMConnector	d = world.connectors ().at (selection.index);
			bg.addChild (segment (hs[0].x (), hs[0].y (), d.edge.z1 () + 0.02, hs[1].x (), hs[1].y (), d.edge.z2 () + 0.02, C_SEL, 4f));
			bg.addChild (segment (hs[0].x (), hs[0].y (), d.edge.z1 () + d.height + 0.02, hs[1].x (), hs[1].y (), d.edge.z2 () + d.height + 0.02, C_SEL, 4f));
			bg.addChild (segment (hs[2].x (), hs[2].y (), d.path.z1 () + 0.03, hs[3].x (), hs[3].y (), d.path.z2 () + 0.03, C_SEL, 3f));
			break;
		}
		case WorldItem.MARKING:
		{
			tc.shared.world.WMMarking	m = world.markings ().at (selection.index);
			bg.addChild (segment (hs[0].x (), hs[0].y (), m.edge.z1 () + 0.03, hs[1].x (), hs[1].y (), m.edge.z2 () + 0.03, C_SEL, 4f));
			break;
		}
		case WorldItem.ZONE:
			bg.addChild (polyline (hs, true, base + 0.04, C_SEL, 4f));
			break;
		case WorldItem.FAREA:
			bg.addChild (polyline (world.fareas ().at (selection.index).polygon, true, 0.04, C_SEL, 4f));
			break;
		case WorldItem.OBJECT:
		case WorldItem.AOBJECT:
			for (wucore.utils.geom.Line2 l : WorldEditor.object (world, selection).absIcon ())
				bg.addChild (polyline (new Point2[] { l.orig (), l.dest () }, false, 0.04, C_SEL, 3f));
			bg.addChild (polyline (hs, false, base + 0.05, C_SEL, 3f));
			break;
		case WorldItem.WAYPOINT:
		case WorldItem.DOCK:
		case WorldItem.START:
		case WorldItem.BEACON:
		case WorldItem.CBEACON:
			bg.addChild (polyline (hs, false, base + 0.1, C_SEL, 3f));
			z = base + 0.12;
			break;
		}
		for (int i = 0; i < hs.length; i++)
			bg.addChild (sphere (hs[i].x (), hs[i].y (), z, 0.07, C_SEL));

		bg.compile ();
		selBranch = bg;
		scene.addBranch (bg);
	}

	/* ------------------------------------------------------------------ */
	/* Geometry helpers                                                    */
	/* ------------------------------------------------------------------ */

	static private Appearance lineAppearance (Color3f color, float width)
	{
		Appearance		app = new Appearance ();
		app.setColoringAttributes (new ColoringAttributes (color, ColoringAttributes.SHADE_FLAT));
		app.setLineAttributes (new LineAttributes (width, LineAttributes.PATTERN_SOLID, true));
		return app;
	}

	static private Appearance matAppearance (Color3f color, float transparency)
	{
		Appearance		app = new Appearance ();
		Material		mat = new Material (color, new Color3f (0f, 0f, 0f), color, new Color3f (0.3f, 0.3f, 0.3f), 32f);
		mat.setLightingEnable (true);
		app.setMaterial (mat);
		app.setColoringAttributes (new ColoringAttributes (color, ColoringAttributes.SHADE_GOURAUD));
		if (transparency > 0f)
			app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.BLENDED, transparency));
		return app;
	}

	/** Elevation carried by a point (Point3), or 0. */
	static private double pz (Point2 p)
	{
		return (p instanceof Point3) ? ((Point3) p).z () : 0.0;
	}

	/** Polyline through the points; <code>dz</code> is added to each point's own elevation. */
	static private Shape3D polyline (Point2[] pts, boolean closed, double dz, Color3f color, float width)
	{
		int				n = pts.length + (closed ? 1 : 0);
		if (pts.length < 2)
			return new Shape3D ();
		LineStripArray	geo = new LineStripArray (n, GeometryArray.COORDINATES, new int[] { n });
		for (int i = 0; i < n; i++)
		{
			Point2	p = pts[i % pts.length];
			geo.setCoordinate (i, new Point3d (p.x (), p.y (), pz (p) + dz));
		}
		return new Shape3D (geo, lineAppearance (color, width));
	}

	/** A segment between two points with explicit elevations. */
	static private Shape3D segment (double x1, double y1, double z1, double x2, double y2, double z2, Color3f color, float width)
	{
		return polyline (new Point2[] { new Point3 (x1, y1, z1), new Point3 (x2, y2, z2) }, false, 0.0, color, width);
	}

	static private Shape3D polyline (Polygon2 poly, boolean closed, double dz, Color3f color, float width)
	{
		Point2[]	pts = new Point2[poly.npoints];
		for (int i = 0; i < poly.npoints; i++)		pts[i] = new Point3 (poly.xpoints[i], poly.ypoints[i], poly.zpoints[i]);
		return polyline (pts, closed, dz, color, width);
	}

	/** Filled (possibly concave) polygon, both faces visible. */
	static private Shape3D polygon (Polygon2 poly, double z, Color3f color, float transparency)
	{
		if (poly.npoints < 3)			return null;
		try
		{
			Point3d[]		coords = new Point3d[poly.npoints];
			for (int i = 0; i < poly.npoints; i++)
				coords[i] = new Point3d (poly.xpoints[i], poly.ypoints[i], poly.zpoints[i] + z);
			GeometryInfo	gi = new GeometryInfo (GeometryInfo.POLYGON_ARRAY);
			gi.setCoordinates (coords);
			gi.setStripCounts (new int[] { poly.npoints });
			gi.setContourCounts (new int[] { 1 });
			new NormalGenerator ().generateNormals (gi);
			Appearance		app = matAppearance (color, transparency);
			app.setPolygonAttributes (new PolygonAttributes (PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
			return new Shape3D (gi.getGeometryArray (), app);
		} catch (Exception e)
		{
			return null;		// degenerate polygon: the outline is still drawn
		}
	}

	static private TransformGroup sphere (double x, double y, double z, double r, Color3f color)
	{
		Transform3D		t = new Transform3D ();
		t.setTranslation (new Vector3d (x, y, z));
		TransformGroup	tg = new TransformGroup (t);
		tg.addChild (new Sphere ((float) r, matAppearance (color, 0f)));
		return tg;
	}

	/** Vertical cylinder standing on z0. */
	static private TransformGroup cylinder (double x, double y, double z0, double r, double h, Color3f color, float transparency)
	{
		Transform3D		t = new Transform3D ();
		t.rotX (Math.PI / 2.0);				// Java 3D cylinders are along Y
		t.setTranslation (new Vector3d (x, y, z0 + h / 2.0));
		TransformGroup	tg = new TransformGroup (t);
		tg.addChild (new Cylinder ((float) r, (float) h, matAppearance (color, transparency)));
		return tg;
	}

	/* ------------------------------------------------------------------ */
	/* Scene with editor-friendly extensions                               */
	/* ------------------------------------------------------------------ */
	/* Simulated robots (live objects, independent of the world rebuilds)   */
	/* ------------------------------------------------------------------ */

	/**
	 * Adds a robot (3DS body and optional lift from its simulator description)
	 * and returns its index for {@link #updateRobot}. Robots survive world
	 * rebuilds: they hang from their own branch.
	 */
	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc, double x, double y, double a)
	{
		return addRobot (rdesc, sdesc, x, y, a, null);
	}

	/** Adds a simulated robot with its name shown above it. */
	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc, double x, double y, double a, String name)
	{
		if (robotsBranch == null)
		{
			robotsBranch = new BranchGroup ();
			robotsBranch.setCapability (BranchGroup.ALLOW_DETACH);
			robotsBranch.setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
			robotsBranch.setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
			scene.addBranch (robotsBranch);
		}
		// an articulated robot: its kinematic model in the place of the 3D model, and
		// its walking model to move the joints as it goes (a robot with a .kine and no
		// walking model stands articulated at rest)
		Articulated3D	art = null;
		tc.vrobot.articulated.WalkingModel	walker = null;

		if ((sdesc.KINEFILE != null) && (sdesc.KINEFILE.trim ().length () > 0))
		{
			try
			{
				tc.vrobot.articulated.KineModel	km = tc.vrobot.articulated.KineJson.read (new File (sdesc.KINEFILE.trim ()));

				art		= new Articulated3D (km, sdesc.V3DPARTS);
				walker	= tc.vrobot.articulated.WalkingModel.create (sdesc.WALKMODEL, km);
				if (walker != null)		{ walker.stand ();	art.update (); }

				double[]	st = ShapeLines.standing (km, walker, sdesc.V3DPARTS);			// pitched onto its feet, the lowest of them on the floor

				art.move (0.0, 0.0, st[1], 0.0, st[0]);
			}
			catch (Exception e)
			{
				System.out.println ("  [WorldView3D] Cannot read the kinematic model " + sdesc.KINEFILE + ": " + e);
				art	= null;
			}
		}

		TransformGroup	body = (art != null) ? null : ((sdesc.V3DFILE != null) ? scene.getCachedObject (sdesc.V3DFILE, null) : null);
		TransformGroup	lift = (art != null) ? null : ((sdesc.V3DLIFT != null) ? scene.getCachedObject (sdesc.V3DLIFT, null) : null);
		if (art != null)
		{
			body	= new TransformGroup ();
			body.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
			body.addChild (art);
		}
		else if (body == null)
		{
			// no 3D model: a box the size of the robot
			body = new TransformGroup ();
			double	r = Math.max (0.1, rdesc.RADIUS);
			body.addChild (new Box ((float) r, (float) r, (float) (r / 2.0), matAppearance (new Color3f (0.2f, 0.4f, 0.9f), 0f)));
			body.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		}
		body.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		if (lift != null)		lift.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		Robot3D		r3d = new Robot3D (rdesc, body, lift, new Point3 (x, y, 0.0), 0.0, a, name);
		r3d.setCameraBounds (wallBounds ());						// what its cameras see stops at the outer walls
		r3d.showCameras (showFOVs);
		r3d.showName (showNames);
		robots.add (r3d);
		articulated.add (art);
		walkers.add (walker);
		stepped.add (new long[] { 0L });
		updateNameScale ();											// another robot may want the letters smaller
		robotsBranch.addChild (r3d);
		return robots.size () - 1;
	}

	/** Moves a robot (and its sensor displays) to the pose of <code>data</code>. */
	public void updateRobot (int index, RobotData data)
	{
		updateRobot (index, data, null, null);
	}

	/** The same, with its cameras turned as the simulation has them (pan, tilt of each, rad; null: as they were). */
	public void updateRobot (int index, RobotData data, double[] pans, double[] tilts)
	{
		updateRobot (index, data, pans, tilts, null);
	}

	/**
	 * The same, with the control action the robot is carrying out ({vlin, vlat,
	 * vrot}; null for none): an articulated robot walks with it, its walking
	 * model moving the joints on by the time gone since it was last drawn, and
	 * its head turned where the first camera looks.
	 */
	public void updateRobot (int index, RobotData data, double[] pans, double[] tilts, double[] vel)
	{
		if ((index < 0) || (index >= robots.size ()))		return;
		robots.get (index).move (data, new Point3 (data.real_x, data.real_y, 0.0), data.fork, data.real_a, pans, tilts);

		Articulated3D	art = articulated.get (index);
		tc.vrobot.articulated.WalkingModel	walker = walkers.get (index);

		if ((art == null) || (walker == null))		return;

		long[]	last = stepped.get (index);
		long	now = System.nanoTime ();
		double	dt = (last[0] == 0L) ? 0.04 : Math.min (0.2, (now - last[0]) / 1e9);

		last[0]	= now;
		if (vel != null)		walker.setVelocities (vel[0], vel[1], vel[2]);
		if ((pans != null) && (pans.length > 0) && (tilts != null) && (tilts.length > 0) && Double.isFinite (pans[0]) && Double.isFinite (tilts[0]))
			walker.setHead (pans[0], tilts[0]);
		walker.step (dt);
		art.update ();
	}

	public void clearRobots ()
	{
		if (robotsBranch != null)		robotsBranch.detach ();
		robotsBranch = null;
		robots.clear ();
		articulated.clear ();
		walkers.clear ();
		stepped.clear ();
		updateNameScale ();											// no robot to set the letters: as written
	}

	/* --- simulated animated objects: live copies moved by the simulator --- */

	/**
	 * Whether the animated objects of the world are drawn (at their initial
	 * pose) as part of the world. The simulator turns it off and adds its own
	 * live objects with {@link #addObject}.
	 */
	public void setAnimatedVisible (boolean b)
	{
		if (showAnimated == b)			return;
		showAnimated = b;
		scheduleRebuild ();
	}

	/**
	 * Adds a live object and returns its index for {@link #updateObject}: its 3DS
	 * shape; with none, the picture it carries, lying flat over the ground its icon
	 * covers; and with neither, the segments of its icon. Its name floats above it.
	 */
	public int addObject (WMObject o, double x, double y, double z, double a)
	{
		if (objectsBranch == null)
		{
			objectsBranch = new BranchGroup ();
			objectsBranch.setCapability (BranchGroup.ALLOW_DETACH);
			objectsBranch.setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
			objectsBranch.setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
			scene.addBranch (objectsBranch);
		}
		TransformGroup	tg = new TransformGroup ();
		tg.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		TransformGroup	model = (o.shape != null) ? scene.getCachedObject (o.shape, o.usecolor ? wucore.utils.color.ColorTool.fromWColorToColor (o.color) : null) : null;
		TransformGroup	plate = (model == null) ? scene.getObjectImage (o) : null;
		if (model != null)
			tg.addChild (model);
		else if (plate != null)
			tg.addChild (plate);								// no 3D model but a picture of its own: the picture over the ground its icon covers
		else
		{
			// no 3D model: the icon segments at floor level
			java.awt.Color	c = wucore.utils.color.ColorTool.fromWColorToColor (o.color);
			Color3f			col = new Color3f (c.getRed () / 255f, c.getGreen () / 255f, c.getBlue () / 255f);
			for (wucore.utils.geom.Line2 l : o.getLocalIcon ())
				tg.addChild (segment (l.orig ().x (), l.orig ().y (), l.z1 () + 0.02, l.dest ().x (), l.dest ().y (), l.z2 () + 0.02, col, 2f));
		}
		// its name on the floor under it, in the size of letter of the scene, in a branch
		// of its own beside the object (it does not turn with it), so that the View menu
		// can take it off and put it back
		FloorName		fn = null;
		if ((o.label != null) && (o.label.length () > 0))
		{
			fn	= new FloorName (o.label, nameScale);
			fn.place (x, y, 0.0);									// on the floor: at the height of the object it would lie over a small one (the ball)
			if (showLabels)		objectsBranch.addChild (fn);
		}
		labels.add (fn);
		BranchGroup		bg = new BranchGroup ();
		bg.setCapability (BranchGroup.ALLOW_DETACH);
		bg.addChild (tg);
		objects.add (tg);
		objectsBranch.addChild (bg);
		updateObject (objects.size () - 1, x, y, z, a);
		return objects.size () - 1;
	}

	/** Moves a live object to a pose. */
	public void updateObject (int index, double x, double y, double z, double a)
	{
		if ((index < 0) || (index >= objects.size ()))		return;
		Transform3D		t = new Transform3D ();
		t.rotZ (a);
		t.setTranslation (new Vector3d (x, y, z));
		objects.get (index).setTransform (t);
		if ((index < labels.size ()) && (labels.get (index) != null))		labels.get (index).place (x, y, 0.0);
	}

	public void clearObjects ()
	{
		if (objectsBranch != null)		objectsBranch.detach ();
		objectsBranch = null;
		objects.clear ();
		labels.clear ();
	}

	/* ------------------------------------------------------------------ */

	/**
	 * Scene3D that tolerates missing texture files (they are typed by hand in
	 * the editor) and exposes viewpoint helpers.
	 */
	static protected class EditorScene extends Scene3D
	{
		protected java.util.Hashtable<String, Appearance>	missing = new java.util.Hashtable<String, Appearance> ();

		public EditorScene (Canvas3D canvas)
		{
			super (canvas);
			// large maps: the default clipping planes (0.1 .. 10 m) would hide most of the world
			// (the near/far ratio also sets the depth-buffer precision: keep it moderate to avoid z-fighting)
			universe.getViewer ().getView ().setFrontClipDistance (0.3);
			universe.getViewer ().getView ().setBackClipDistance (1500.0);
			rho		= 0.7;
			theta	= -Math.PI / 2.0;
			len		= 20.0;
			setViewpoint ();
		}

		public Appearance getCachedTexture (String name, boolean horiz)
		{
			if ((name != null) && new File (name).isFile ())
			{
				try { return super.getCachedTexture (name, horiz); }
				catch (Exception e) { System.out.println ("  [WorldView3D] Cannot load texture <" + name + ">: " + e); }
			}
			Appearance	app = missing.get (String.valueOf (name));
			if (app == null)
			{
				System.out.println ("  [WorldView3D] Texture not found <" + name + ">, using plain colour");
				app = matAppearance (horiz ? new Color3f (0.75f, 0.72f, 0.6f) : new Color3f (0.8f, 0.8f, 0.85f), 0f);
				missing.put (String.valueOf (name), app);
			}
			return app;
		}

		public void addBranch (BranchGroup bg)		{ scene.addChild (bg); }

		public void fit (double[] b)
		{
			focus.set ((b[0] + b[2]) / 2.0, (b[1] + b[3]) / 2.0, 0.0);
			len = Math.max (4.0, 0.9 * Math.max (b[2] - b[0], b[3] - b[1]));
			setViewpoint ();
		}

		public void setFocus (double x, double y)
		{
			focus.set (x, y, 0.0);
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
			len = Math.max (0.5, len * factor);
			setViewpoint ();
		}
	}

	/* ------------------------------------------------------------------ */

	/** Stand-alone test: java tcapps.tceditor.WorldView3DWindow file.world */
	static public void main (String[] args) throws Exception
	{
		World	w = (args.length > 0) ? new World (args[0]) : WorldEditor.newWorld ();
		WorldView3DWindow	win = new WorldView3DWindow (w, new Runnable () { public void run () { System.exit (0); } });
		win.setVisible (true);
	}
}
