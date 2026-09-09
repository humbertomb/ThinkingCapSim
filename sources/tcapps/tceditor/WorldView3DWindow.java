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
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
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
import tc.shared.world.WMDoor;
import tc.shared.world.WMFArea;
import tc.shared.world.WMWall;
import tc.shared.world.World;
import tcapps.tcsim.gui.visualization.Scene3D;
import tcapps.tcsim.gui.visualization.objects.World3D;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Polygon2;

/**
 * Java 3D view of the world being edited in {@link WorldEditorWindow}. The
 * scene is rebuilt (coalescing rapid changes) every time the editor reports a
 * modification, and the current selection is highlighted in orange. It reuses
 * the simulator's {@link Scene3D} (lights, textures, 3DS objects, viewpoint
 * control) and {@link World3D} (walls, zones, objects, docks, waypoints,
 * doors), and adds the elements the simulator does not draw: forbidden areas,
 * path, beacons and start point.
 */
public class WorldView3DWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "World 3D View";
	static private final int		REBUILD_MS	= 120;		// coalescing delay for scene rebuilds

	/* Colours */
	static private final Color3f	C_SEL		= new Color3f (1.0f, 0.55f, 0.0f);
	static private final Color3f	C_FAREA		= new Color3f (0.85f, 0.15f, 0.15f);
	static private final Color3f	C_PATH		= new Color3f (0.0f, 0.7f, 0.85f);
	static private final Color3f	C_BEACON	= new Color3f (0.8f, 0.0f, 0.8f);
	static private final Color3f	C_DOORP		= new Color3f (0.15f, 0.7f, 0.15f);
	static private final Color3f	C_START		= new Color3f (0.9f, 0.1f, 0.1f);
	static private final Color3f	C_FLOOR		= new Color3f (0.55f, 0.55f, 0.5f);

	/* Model */
	protected World					world;
	protected WorldItem				selection;
	protected boolean				needsRebuild	= true;
	protected boolean				needsFit		= true;

	/* Java 3D */
	protected Canvas3D				canvas;
	protected EditorScene			scene;
	protected BranchGroup			worldBranch;		// detachable: World3D + extras
	protected BranchGroup			selBranch;			// detachable: selection highlight
	protected int					vmode			= Scene3D.M_MOVE;

	/* GUI */
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

		canvas	= new Canvas3D (SimpleUniverse.getPreferredConfiguration ());
		canvas.setPreferredSize (new Dimension (800, 600));
		scene	= new EditorScene (canvas);

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
		fit.setFocusable (false);
		fit.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ fitView (); }
		});
		tb.add (fit);

		JButton		top = new JButton ("Top");
		top.setToolTipText ("View from above");
		top.setFocusable (false);
		top.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ scene.setAngles (-Math.PI / 2.0, Math.PI / 2.0 - 0.01); }
		});
		tb.add (top);
		tb.addSeparator ();

		floorCB	= new JCheckBox ("Floor", true);
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
		if (this.world != world)		needsFit = true;
		this.world	= world;
		scheduleRebuild ();
	}

	/** The editor is dragging something: same as setWorld but never refits the view. */
	public void worldChanged ()
	{
		scheduleRebuild ();
	}

	public void setSelection (WorldItem item)
	{
		selection = item;
		if (isVisible ())
		{
			updateSelection ();
			if (followCB.isSelected () && WorldEdit.valid (world, item))
			{
				Point2[]	hs = WorldEdit.handles (world, item);
				if (hs.length > 0)		scene.setFocus (hs[0].x (), hs[0].y ());
			}
		}
	}

	public void fitView ()
	{
		double[]	b = WorldEdit.bounds (world);
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
			bg.addChild (new World3D (world, scene));
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
			for (int k = 0; k < WorldItem.DEFAULTS; k++)	n += WorldEdit.count (world, k);
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

	/** Elements not drawn by World3D: forbidden areas, path, beacons, door paths, start point. */
	private BranchGroup createExtras ()
	{
		BranchGroup		bg = new BranchGroup ();
		int				i;

		// forbidden areas: translucent red polygon just above the floor
		for (i = 0; i < world.fareas ().n (); i++)
		{
			WMFArea		f = world.fareas ().at (i);
			Shape3D		s = polygon (f.polygon, 0.015, C_FAREA, 0.45f);
			if (s != null)		bg.addChild (s);
			bg.addChild (polyline (f.polygon, true, 0.02, C_FAREA, 2f));
		}

		// path
		if (world.path ().n () >= 2)
		{
			Point2[]	pts = world.path ().points ();
			bg.addChild (polyline (pts, false, 0.03, C_PATH, 3f));
		}
		for (i = 0; i < world.path ().n (); i++)
			bg.addChild (sphere (world.path ().at (i).x (), world.path ().at (i).y (), 0.03, 0.06, C_PATH));

		// strip beacons: thin vertical plates
		for (i = 0; i < world.beacons ().n (); i++)
		{
			WMBeacon		b = world.beacons ().at (i);
			Transform3D		t = new Transform3D ();
			t.rotZ (b.pos.alpha ());
			t.setTranslation (new Vector3d (b.pos.x (), b.pos.y (), 0.4));
			TransformGroup	tg = new TransformGroup (t);
			tg.addChild (new Box ((float) Math.max (0.01, b.width / 2.0), 0.01f, 0.3f, matAppearance (C_BEACON, 0f)));
			bg.addChild (tg);
		}

		// cylindrical beacons
		for (i = 0; i < world.cbeacons ().n (); i++)
		{
			WMCBeacon		b = world.cbeacons ().at (i);
			double			r = Math.max (0.02, Math.max (b.beacon.horiz (), b.beacon.vert ()));
			bg.addChild (cylinder (b.beacon.center ().x (), b.beacon.center ().y (), 0.0, r, 0.8, C_BEACON, 0f));
		}

		// door crossing paths
		for (i = 0; i < world.doors ().n (); i++)
		{
			WMDoor		d = world.doors ().at (i);
			bg.addChild (polyline (new Point2[] { d.path.orig (), d.path.dest () }, false, 0.02, C_DOORP, 2f));
		}

		// start point: red disc with heading bar
		double		sx = world.start_x (), sy = world.start_y (), sa = world.start_a ();
		bg.addChild (cylinder (sx, sy, 0.0, 0.25, 0.04, C_START, 0.3f));
		bg.addChild (polyline (new Point2[] { new Point2 (sx, sy), new Point2 (sx + 0.5 * Math.cos (sa), sy + 0.5 * Math.sin (sa)) }, false, 0.05, C_START, 3f));

		return bg;
	}

	private BranchGroup createFloor ()
	{
		double[]	b = WorldEdit.bounds (world);
		if (b == null)			return null;
		double		m = 1.0;		// margin
		double		w = (b[2] - b[0]) / 2.0 + m, h = (b[3] - b[1]) / 2.0 + m;
		Transform3D	t = new Transform3D ();
		t.setTranslation (new Vector3d ((b[0] + b[2]) / 2.0, (b[1] + b[3]) / 2.0, -0.03));
		TransformGroup	tg = new TransformGroup (t);
		tg.addChild (new Box ((float) w, (float) h, 0.01f, matAppearance (C_FLOOR, 0f)));
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
		if (!WorldEdit.valid (world, selection) || (selection.kind == WorldItem.DEFAULTS))		return;

		BranchGroup		bg = new BranchGroup ();
		bg.setCapability (BranchGroup.ALLOW_DETACH);
		Point2[]		hs = WorldEdit.handles (world, selection);
		double			z = 0.08;

		switch (selection.kind)
		{
		case WorldItem.WALL:
		{
			WMWall	w = world.walls ().at (selection.index);
			bg.addChild (polyline (hs, false, 0.02, C_SEL, 4f));
			bg.addChild (polyline (hs, false, w.height + 0.02, C_SEL, 4f));
			break;
		}
		case WorldItem.DOOR:
		{
			WMDoor	d = world.doors ().at (selection.index);
			bg.addChild (polyline (new Point2[] { hs[0], hs[1] }, false, 0.02, C_SEL, 4f));
			bg.addChild (polyline (new Point2[] { hs[0], hs[1] }, false, d.height + 0.02, C_SEL, 4f));
			bg.addChild (polyline (new Point2[] { hs[2], hs[3] }, false, 0.03, C_SEL, 3f));
			break;
		}
		case WorldItem.ZONE:
		case WorldItem.FAREA:
			bg.addChild (polyline (hs, true, 0.04, C_SEL, 4f));
			break;
		case WorldItem.OBJECT:
			for (wucore.utils.geom.Line2 l : world.objects ().at (selection.index).icon)
				bg.addChild (polyline (new Point2[] { l.orig (), l.dest () }, false, 0.04, C_SEL, 3f));
			bg.addChild (polyline (hs, false, 0.05, C_SEL, 3f));
			break;
		case WorldItem.WAYPOINT:
		case WorldItem.DOCK:
		case WorldItem.START:
		case WorldItem.BEACON:
		case WorldItem.CBEACON:
			bg.addChild (polyline (hs, false, 0.1, C_SEL, 3f));
			z = 0.12;
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

	static private Shape3D polyline (Point2[] pts, boolean closed, double z, Color3f color, float width)
	{
		int				n = pts.length + (closed ? 1 : 0);
		if (pts.length < 2)
			return new Shape3D ();
		LineStripArray	geo = new LineStripArray (n, GeometryArray.COORDINATES, new int[] { n });
		for (int i = 0; i < n; i++)
		{
			Point2	p = pts[i % pts.length];
			geo.setCoordinate (i, new Point3d (p.x (), p.y (), z));
		}
		return new Shape3D (geo, lineAppearance (color, width));
	}

	static private Shape3D polyline (Polygon2 poly, boolean closed, double z, Color3f color, float width)
	{
		Point2[]	pts = new Point2[poly.npoints];
		for (int i = 0; i < poly.npoints; i++)		pts[i] = new Point2 (poly.xpoints[i], poly.ypoints[i]);
		return polyline (pts, closed, z, color, width);
	}

	/** Filled (possibly concave) polygon, both faces visible. */
	static private Shape3D polygon (Polygon2 poly, double z, Color3f color, float transparency)
	{
		if (poly.npoints < 3)			return null;
		try
		{
			Point3d[]		coords = new Point3d[poly.npoints];
			for (int i = 0; i < poly.npoints; i++)
				coords[i] = new Point3d (poly.xpoints[i], poly.ypoints[i], z);
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
			universe.getViewer ().getView ().setFrontClipDistance (0.05);
			universe.getViewer ().getView ().setBackClipDistance (2000.0);
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
		World	w = (args.length > 0) ? new World (args[0]) : WorldEdit.newWorld ();
		WorldView3DWindow	win = new WorldView3DWindow (w, new Runnable () { public void run () { System.exit (0); } });
		win.setVisible (true);
	}
}
