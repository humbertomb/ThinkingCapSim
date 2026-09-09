/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.WMDock;
import tc.shared.world.WMDoor;
import tc.shared.world.WMFArea;
import tc.shared.world.WMObject;
import tc.shared.world.WMWall;
import tc.shared.world.WMWaypoint;
import tc.shared.world.WMZone;
import tc.shared.world.World;
import wucore.utils.color.ColorTool;
import wucore.utils.geom.Ellipse2;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Polygon2;

/**
 * Interactive 2D view of a {@link World}. Draws every kind of element, and
 * implements the mouse tools used by {@link WorldEditorWindow}: selection and
 * dragging (whole elements or their handles), creation of new elements, panning
 * and zooming.
 */
public class WorldCanvas extends JPanel
{
	private static final long		serialVersionUID = 1L;

	/* Tools */
	static public final int		T_SELECT	= 0;
	static public final int		T_PAN		= 1;
	static public final int		T_START		= 2;
	static public final int		T_WALL		= 3;
	static public final int		T_OBJECT	= 4;
	static public final int		T_FAREA		= 5;
	static public final int		T_ZONE		= 6;
	static public final int		T_DOOR		= 7;
	static public final int		T_WAYPOINT	= 8;
	static public final int		T_DOCK		= 9;
	static public final int		T_BEACON	= 10;
	static public final int		T_CBEACON	= 11;
	static public final int		T_PATH		= 12;

	/** Receives notifications from the canvas. */
	public interface Listener
	{
		public void selectionChanged (WorldItem item);
		/** The world was modified by a mouse action; <code>what</code> is a short undo description. */
		public void worldChanged (String what);
		public void statusChanged (String text);
		public void toolFinished ();
		/** The world is being modified by a drag in progress (no undo entry yet). */
		public void worldPreview ();
	}

	/* Colours */
	static private final Color		C_BACK		= Color.WHITE;
	static private final Color		C_GRID		= new Color (232, 232, 232);
	static private final Color		C_GRID2		= new Color (205, 205, 205);
	static private final Color		C_AXIS		= new Color (170, 170, 170);
	static private final Color		C_ZONE		= new Color (255, 235, 130, 70);
	static private final Color		C_ZONE_B	= new Color (200, 160, 0);
	static private final Color		C_FAREA		= new Color (255, 90, 90, 70);
	static private final Color		C_FAREA_B	= new Color (200, 40, 40);
	static private final Color		C_PATH		= new Color (0, 170, 200);
	static private final Color		C_WALL		= Color.BLACK;
	static private final Color		C_DOOR		= new Color (150, 90, 30);
	static private final Color		C_DOORP		= new Color (40, 170, 40);
	static private final Color		C_BEACON	= new Color (200, 0, 200);
	static private final Color		C_WP		= new Color (30, 80, 220);
	static private final Color		C_DOCK		= new Color (0, 140, 60);
	static private final Color		C_START		= new Color (220, 30, 30);
	static private final Color		C_SEL		= new Color (255, 140, 0);
	static private final Color		C_HANDLE	= new Color (255, 255, 255);
	static private final Color		C_RUBBER	= new Color (0, 120, 215);

	static private final int		HANDLE_PX	= 4;		// half size of handle squares
	static private final int		PICK_PX		= 7;		// pick tolerance in pixels

	/* Model */
	protected World					world;
	protected WorldItem				selection;
	protected boolean[]				visible		= new boolean[WorldItem.NKINDS];
	protected Listener				listener;

	/* View */
	protected double				scale		= 50.0;		// pixels per metre
	protected double				cx			= 0.0;		// world coordinates at the centre of the panel
	protected double				cy			= 0.0;
	protected boolean				showGrid	= true;
	protected boolean				snapGrid	= false;
	protected boolean				showLabels	= true;
	protected double				gridStep	= 1.0;		// metres, recomputed from the scale

	/* Interaction */
	protected int					tool		= T_SELECT;
	protected int					dragMode	= 0;		// 0 none, 1 move, 2 handle, 3 pan, 4 rubber (creation)
	protected int					dragHandle	= -1;
	protected boolean				dragged		= false;
	protected Point					lastPx;
	protected double				anchorX, anchorY;			// world coords where the drag started
	protected double				curX, curY;					// current world coords of the mouse
	protected List<Point2>			polyPoints	= new ArrayList<Point2> ();	// points of the area being drawn
	protected boolean				spaceDown	= false;

	public WorldCanvas (World world)
	{
		this.world	= world;
		for (int i = 0; i < visible.length; i++)		visible[i] = true;

		setBackground (C_BACK);
		setPreferredSize (new Dimension (800, 600));
		setFocusable (true);

		MouseAdapter	ma = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)		{ requestFocusInWindow (); onPress (e); }
			public void mouseReleased (MouseEvent e)	{ onRelease (e); }
			public void mouseDragged (MouseEvent e)		{ onDrag (e); }
			public void mouseMoved (MouseEvent e)		{ onMove (e); }
			public void mouseClicked (MouseEvent e)		{ onClick (e); }
			public void mouseWheelMoved (MouseWheelEvent e)	{ onWheel (e); }
			public void mouseExited (MouseEvent e)		{ if (listener != null) listener.statusChanged (""); }
		};
		addMouseListener (ma);
		addMouseMotionListener (ma);
		addMouseWheelListener (ma);

		addKeyListener (new KeyAdapter ()
		{
			public void keyPressed (KeyEvent e)		{ onKey (e); }
			public void keyReleased (KeyEvent e)	{ if (e.getKeyCode () == KeyEvent.VK_SPACE) spaceDown = false; }
		});
	}

	/* ------------------------------------------------------------------ */
	/* Public API                                                          */
	/* ------------------------------------------------------------------ */

	public void setListener (Listener l)			{ listener = l; }
	public World getWorld ()						{ return world; }
	public WorldItem getSelection ()				{ return selection; }
	public int getTool ()							{ return tool; }
	public boolean isGridVisible ()					{ return showGrid; }
	public boolean isSnapEnabled ()					{ return snapGrid; }
	public boolean areLabelsVisible ()				{ return showLabels; }
	public boolean isKindVisible (int kind)			{ return visible[kind]; }
	public double getScale ()						{ return scale; }

	public void setWorld (World world)
	{
		this.world	= world;
		selection	= null;
		polyPoints.clear ();
		repaint ();
		if (listener != null)		listener.selectionChanged (null);
	}

	public void setSelection (WorldItem item)
	{
		if (!WorldEdit.valid (world, item))		item = null;
		selection = item;
		repaint ();
		if (listener != null)		listener.selectionChanged (item);
	}

	public void setTool (int tool)
	{
		this.tool = tool;
		polyPoints.clear ();
		setCursor ((tool == T_PAN) ? Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR)
				: (tool == T_SELECT) ? Cursor.getDefaultCursor () : Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
		repaint ();
	}

	public void setGridVisible (boolean b)			{ showGrid = b; repaint (); }
	public void setSnapEnabled (boolean b)			{ snapGrid = b; }
	public void setLabelsVisible (boolean b)		{ showLabels = b; repaint (); }
	public void setKindVisible (int kind, boolean b)
	{
		visible[kind] = b;
		if ((selection != null) && (selection.kind == kind) && !b)		setSelection (null);
		repaint ();
	}

	public void zoom (double factor)
	{
		scale = Math.max (2.0, Math.min (5000.0, scale * factor));
		repaint ();
	}

	/** Adjusts the view so that the whole world is visible. */
	public void zoomToFit ()
	{
		double[]	b = WorldEdit.bounds (world);
		if (b == null)
		{
			b = new double[] { world.start_x () - 5, world.start_y () - 5, world.start_x () + 5, world.start_y () + 5 };
		}
		double		w = Math.max (1.0, b[2] - b[0]);
		double		h = Math.max (1.0, b[3] - b[1]);
		int			pw = Math.max (50, getWidth ()), ph = Math.max (50, getHeight ());
		scale	= 0.9 * Math.min (pw / w, ph / h);
		scale	= Math.max (2.0, Math.min (5000.0, scale));
		cx		= (b[0] + b[2]) / 2.0;
		cy		= (b[1] + b[3]) / 2.0;
		repaint ();
	}

	/** Finishes the polygon being drawn (forbidden area). */
	public void finishPolygon ()
	{
		if ((tool == T_FAREA) && (polyPoints.size () >= 3))
		{
			WorldItem	it = WorldEdit.addFArea (world, polyPoints);
			polyPoints.clear ();
			changed ("Add forbidden area");
			setSelection (it);
		}
		else
			polyPoints.clear ();
		repaint ();
	}

	public void cancelDrawing ()
	{
		polyPoints.clear ();
		dragMode = 0;
		repaint ();
	}

	/** Deletes the current selection. */
	public void deleteSelection ()
	{
		if (selection == null)			return;
		if (WorldEdit.remove (world, selection))
		{
			String	what = "Delete " + WorldItem.NAMES[selection.kind].toLowerCase ();
			selection = null;
			changed (what);
			setSelection (null);
		}
	}

	/** Moves the selection by (dx, dy) grid steps (keyboard nudging). */
	public void nudgeSelection (int dx, int dy)
	{
		if (selection == null)			return;
		double	step = snapGrid ? gridStep : gridStep / 10.0;
		WorldEdit.translate (world, selection, dx * step, dy * step);
		changed ("Move " + WorldItem.NAMES[selection.kind].toLowerCase ());
	}

	/* ------------------------------------------------------------------ */
	/* Coordinate transforms                                               */
	/* ------------------------------------------------------------------ */

	public double toWorldX (int px)					{ return cx + (px - getWidth () / 2.0) / scale; }
	public double toWorldY (int py)					{ return cy - (py - getHeight () / 2.0) / scale; }
	public int toPixelX (double x)					{ return (int) Math.round (getWidth () / 2.0 + (x - cx) * scale); }
	public int toPixelY (double y)					{ return (int) Math.round (getHeight () / 2.0 - (y - cy) * scale); }
	private double px (double x)					{ return getWidth () / 2.0 + (x - cx) * scale; }
	private double py (double y)					{ return getHeight () / 2.0 - (y - cy) * scale; }

	private double snap (double v)
	{
		if (!snapGrid)		return v;
		return Math.rint (v / gridStep) * gridStep;
	}

	private void updateGridStep ()
	{
		// choose a step so that grid lines are at least ~25 px apart
		double[]	steps = { 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 };
		gridStep = steps[steps.length - 1];
		for (int i = 0; i < steps.length; i++)
			if (steps[i] * scale >= 25.0) { gridStep = steps[i]; break; }
	}

	/* ------------------------------------------------------------------ */
	/* Mouse handling                                                      */
	/* ------------------------------------------------------------------ */

	private void changed (String what)
	{
		repaint ();
		if (listener != null)		listener.worldChanged (what);
	}

	private void status (String s)
	{
		if (listener != null)		listener.statusChanged (s);
	}

	private void onPress (MouseEvent e)
	{
		lastPx	= e.getPoint ();
		curX	= toWorldX (e.getX ());
		curY	= toWorldY (e.getY ());
		anchorX	= snap (curX);
		anchorY	= snap (curY);
		dragged	= false;

		boolean		pan = SwingUtilities.isMiddleMouseButton (e) || (tool == T_PAN) || spaceDown
						|| (SwingUtilities.isLeftMouseButton (e) && e.isAltDown ());
		if (pan)
		{
			dragMode = 3;
			return;
		}
		if (!SwingUtilities.isLeftMouseButton (e))			return;

		double		tol = PICK_PX / scale;

		switch (tool)
		{
		case T_SELECT:
		{
			// handle of the current selection?
			if (selection != null)
			{
				Point2[]	hs = WorldEdit.handles (world, selection);
				int			best = -1;
				double		bd = tol * 1.3;
				for (int i = 0; i < hs.length; i++)
				{
					double	d = hs[i].distance (curX, curY);
					if (d < bd) { bd = d; best = i; }
				}
				if (best >= 0)
				{
					dragMode	= 2;
					dragHandle	= best;
					return;
				}
			}
			WorldItem	hit = WorldEdit.pick (world, curX, curY, tol, visible);
			// keep the current selection if it is also under the cursor (areas would steal it)
			if ((selection != null) && (hit != null) && !hit.equals (selection)
					&& (WorldEdit.distance (world, selection, curX, curY) < tol))
				hit = selection;
			setSelection (hit);
			if (hit != null)
			{
				dragMode = 1;
				anchorX	= curX;	anchorY = curY;			// unsnapped: movement is relative
			}
			break;
		}
		case T_WALL:
		case T_DOOR:
		case T_ZONE:
			dragMode = 4;
			break;
		case T_FAREA:
			if (e.getClickCount () == 1)
			{
				polyPoints.add (new Point2 (anchorX, anchorY));
				repaint ();
			}
			break;
		case T_PATH:
			setSelection (WorldEdit.addPathPoint (world, anchorX, anchorY));
			changed ("Add path point");
			break;
		case T_OBJECT:
			setSelection (WorldEdit.addObject (world, anchorX, anchorY, 0.4));
			changed ("Add object");
			break;
		case T_WAYPOINT:
			setSelection (WorldEdit.addWaypoint (world, anchorX, anchorY));
			changed ("Add waypoint");
			break;
		case T_DOCK:
			setSelection (WorldEdit.addDock (world, anchorX, anchorY));
			changed ("Add dock");
			break;
		case T_BEACON:
			setSelection (WorldEdit.addBeacon (world, anchorX, anchorY));
			changed ("Add strip beacon");
			break;
		case T_CBEACON:
			setSelection (WorldEdit.addCBeacon (world, anchorX, anchorY));
			changed ("Add cylindrical beacon");
			break;
		case T_START:
			world.setStart (anchorX, anchorY, world.start_a ());
			setSelection (new WorldItem (WorldItem.START, 0));
			changed ("Set start point");
			break;
		}
	}

	private void onDrag (MouseEvent e)
	{
		double		nx = toWorldX (e.getX ());
		double		ny = toWorldY (e.getY ());

		switch (dragMode)
		{
		case 3:		// pan
			cx -= (e.getX () - lastPx.x) / scale;
			cy += (e.getY () - lastPx.y) / scale;
			repaint ();
			break;
		case 1:		// move selection
		{
			double	dx, dy;
			if (snapGrid)
			{
				// move in whole grid steps
				dx = Math.rint ((nx - anchorX) / gridStep) * gridStep - Math.rint ((curX - anchorX) / gridStep) * gridStep;
				dy = Math.rint ((ny - anchorY) / gridStep) * gridStep - Math.rint ((curY - anchorY) / gridStep) * gridStep;
			}
			else
			{
				dx = nx - curX;
				dy = ny - curY;
			}
			if ((dx != 0.0) || (dy != 0.0))
			{
				WorldEdit.translate (world, selection, dx, dy);
				dragged = true;
				repaint ();
				if (listener != null)		listener.worldPreview ();
			}
			break;
		}
		case 2:		// drag handle
			WorldEdit.setHandle (world, selection, dragHandle, snap (nx), snap (ny));
			dragged = true;
			repaint ();
			if (listener != null)		listener.worldPreview ();
			break;
		case 4:		// rubber band for new element
			dragged = true;
			repaint ();
			break;
		}
		lastPx	= e.getPoint ();
		curX	= nx;
		curY	= ny;
		showStatus ();
	}

	private void onRelease (MouseEvent e)
	{
		double		nx = snap (toWorldX (e.getX ()));
		double		ny = snap (toWorldY (e.getY ()));

		switch (dragMode)
		{
		case 1:
			if (dragged)		changed ("Move " + WorldItem.NAMES[selection.kind].toLowerCase ());
			break;
		case 2:
			if (dragged)		changed ("Edit " + WorldItem.NAMES[selection.kind].toLowerCase ());
			break;
		case 4:
		{
			double	len = Math.hypot (nx - anchorX, ny - anchorY);
			if (len > 1e-6)
			{
				WorldItem	it = null;
				String		what = null;
				if (tool == T_WALL)			{ it = WorldEdit.addWall (world, anchorX, anchorY, nx, ny);		what = "Add wall"; }
				else if (tool == T_DOOR)	{ it = WorldEdit.addDoor (world, anchorX, anchorY, nx, ny);		what = "Add door"; }
				else if ((tool == T_ZONE) && (Math.abs (nx - anchorX) > 1e-6) && (Math.abs (ny - anchorY) > 1e-6))
											{ it = WorldEdit.addZone (world, anchorX, anchorY, nx, ny);		what = "Add zone"; }
				if (it != null)
				{
					setSelection (it);
					changed (what);
				}
			}
			break;
		}
		}
		dragMode	= 0;
		dragHandle	= -1;
		repaint ();
	}

	private void onClick (MouseEvent e)
	{
		if ((tool == T_FAREA) && ((e.getClickCount () == 2) || SwingUtilities.isRightMouseButton (e)))
		{
			if (SwingUtilities.isRightMouseButton (e) && (polyPoints.size () == 0))		return;
			finishPolygon ();
		}
		else if ((tool != T_SELECT) && (tool != T_PAN) && (tool != T_FAREA) && SwingUtilities.isRightMouseButton (e))
		{
			// right click with a creation tool returns to the selection tool
			if (listener != null)		listener.toolFinished ();
		}
	}

	private void onMove (MouseEvent e)
	{
		curX	= toWorldX (e.getX ());
		curY	= toWorldY (e.getY ());
		if ((tool == T_FAREA) && (polyPoints.size () > 0))		repaint ();
		showStatus ();
	}

	private void onWheel (MouseEvent e)
	{
		int		rot = ((MouseWheelEvent) e).getWheelRotation ();
		double	f = Math.pow (1.15, -rot);
		// zoom around the cursor position
		double	wx = toWorldX (e.getX ()), wy = toWorldY (e.getY ());
		scale	= Math.max (2.0, Math.min (5000.0, scale * f));
		cx		= wx - (e.getX () - getWidth () / 2.0) / scale;
		cy		= wy + (e.getY () - getHeight () / 2.0) / scale;
		repaint ();
		showStatus ();
	}

	private void onKey (KeyEvent e)
	{
		switch (e.getKeyCode ())
		{
		case KeyEvent.VK_SPACE:		spaceDown = true;		break;
		case KeyEvent.VK_ESCAPE:
			if (polyPoints.size () > 0)		cancelDrawing ();
			else if (tool != T_SELECT)		{ if (listener != null) listener.toolFinished (); }
			else							setSelection (null);
			break;
		case KeyEvent.VK_ENTER:		if (tool == T_FAREA) finishPolygon ();		break;
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:	deleteSelection ();		break;
		case KeyEvent.VK_LEFT:		nudgeSelection (-1, 0);		break;
		case KeyEvent.VK_RIGHT:		nudgeSelection (1, 0);		break;
		case KeyEvent.VK_UP:		nudgeSelection (0, 1);		break;
		case KeyEvent.VK_DOWN:		nudgeSelection (0, -1);		break;
		}
	}

	private void showStatus ()
	{
		String	s = "x = " + WorldEdit.fmt (curX) + " m,  y = " + WorldEdit.fmt (curY) + " m";
		if (world.zones ().n () > 0)		s += "   |   zone: " + world.zones ().inZone (curX, curY);
		s += "   |   grid " + WorldEdit.fmt (gridStep) + " m";
		if (tool == T_FAREA)
			s += "   |   click to add vertices, double-click / Enter to close the area, Esc to cancel";
		else if ((tool == T_WALL) || (tool == T_DOOR) || (tool == T_ZONE))
			s += "   |   drag to draw, right click to go back to selection";
		status (s);
	}

	/* ------------------------------------------------------------------ */
	/* Painting                                                            */
	/* ------------------------------------------------------------------ */

	protected void paintComponent (Graphics g0)
	{
		super.paintComponent (g0);
		Graphics2D	g = (Graphics2D) g0;
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		updateGridStep ();
		if (showGrid)		drawGrid (g);

		if (visible[WorldItem.ZONE])		for (int i = 0; i < world.zones ().n (); i++)		drawZone (g, world.zones ().at (i), isSel (WorldItem.ZONE, i));
		if (visible[WorldItem.FAREA])		for (int i = 0; i < world.fareas ().n (); i++)		drawFArea (g, world.fareas ().at (i), isSel (WorldItem.FAREA, i));
		if (visible[WorldItem.PATH])		drawPath (g);
		if (visible[WorldItem.WALL])		for (int i = 0; i < world.walls ().n (); i++)		drawWall (g, world.walls ().at (i), isSel (WorldItem.WALL, i));
		if (visible[WorldItem.OBJECT])		for (int i = 0; i < world.objects ().n (); i++)		drawObject (g, world.objects ().at (i), isSel (WorldItem.OBJECT, i));
		if (visible[WorldItem.DOOR])		for (int i = 0; i < world.doors ().n (); i++)		drawDoor (g, world.doors ().at (i), isSel (WorldItem.DOOR, i));
		if (visible[WorldItem.BEACON])		for (int i = 0; i < world.beacons ().n (); i++)		drawBeacon (g, world.beacons ().at (i), isSel (WorldItem.BEACON, i));
		if (visible[WorldItem.CBEACON])		for (int i = 0; i < world.cbeacons ().n (); i++)	drawCBeacon (g, world.cbeacons ().at (i), isSel (WorldItem.CBEACON, i));
		if (visible[WorldItem.WAYPOINT])	for (int i = 0; i < world.wps ().n (); i++)			drawWaypoint (g, world.wps ().at (i), isSel (WorldItem.WAYPOINT, i));
		if (visible[WorldItem.DOCK])		for (int i = 0; i < world.docks ().n (); i++)		drawDock (g, world.docks ().at (i), isSel (WorldItem.DOCK, i));
		if (visible[WorldItem.START])		drawStart (g, isSel (WorldItem.START, 0));

		drawRubber (g);
		if (selection != null)		drawHandles (g);
		drawScaleBar (g);
	}

	private boolean isSel (int kind, int index)
	{
		return (selection != null) && (selection.kind == kind) && (selection.index == index);
	}

	private Stroke stroke (float w)
	{
		return new BasicStroke (w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	private Stroke dashed (float w)
	{
		return new BasicStroke (w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] { 5f, 4f }, 0f);
	}

	private void drawGrid (Graphics2D g)
	{
		double		x0 = toWorldX (0), x1 = toWorldX (getWidth ());
		double		y0 = toWorldY (getHeight ()), y1 = toWorldY (0);
		double		step = gridStep;
		int			major = 5;

		g.setStroke (stroke (1f));
		long		i0 = (long) Math.floor (x0 / step), i1 = (long) Math.ceil (x1 / step);
		for (long i = i0; i <= i1; i++)
		{
			int	p = toPixelX (i * step);
			g.setColor ((i % major == 0) ? C_GRID2 : C_GRID);
			g.drawLine (p, 0, p, getHeight ());
		}
		long		j0 = (long) Math.floor (y0 / step), j1 = (long) Math.ceil (y1 / step);
		for (long j = j0; j <= j1; j++)
		{
			int	p = toPixelY (j * step);
			g.setColor ((j % major == 0) ? C_GRID2 : C_GRID);
			g.drawLine (0, p, getWidth (), p);
		}
		// axes
		g.setColor (C_AXIS);
		g.setStroke (stroke (1.5f));
		g.drawLine (toPixelX (0), 0, toPixelX (0), getHeight ());
		g.drawLine (0, toPixelY (0), getWidth (), toPixelY (0));
	}

	private void drawScaleBar (Graphics2D g)
	{
		double		len = gridStep * 5;
		int			pxl = (int) Math.round (len * scale);
		int			x = 12, y = getHeight () - 14;
		g.setColor (new Color (0, 0, 0, 160));
		g.setStroke (stroke (2f));
		g.drawLine (x, y, x + pxl, y);
		g.drawLine (x, y - 4, x, y + 4);
		g.drawLine (x + pxl, y - 4, x + pxl, y + 4);
		g.setFont (g.getFont ().deriveFont (Font.PLAIN, 11f));
		g.drawString (WorldEdit.fmt (len) + " m", x + 4, y - 5);
	}

	private void label (Graphics2D g, String text, double x, double y, Color c)
	{
		if (!showLabels || (text == null))		return;
		g.setFont (g.getFont ().deriveFont (Font.PLAIN, 11f));
		FontMetrics	fm = g.getFontMetrics ();
		int			px = toPixelX (x) + 6, py = toPixelY (y) - 6;
		g.setColor (new Color (255, 255, 255, 190));
		g.fillRect (px - 2, py - fm.getAscent (), fm.stringWidth (text) + 4, fm.getHeight ());
		g.setColor (c);
		g.drawString (text, px, py);
	}

	private void drawZone (Graphics2D g, WMZone z, boolean sel)
	{
		Rectangle2D	r = new Rectangle2D.Double (px (z.area.getMinX ()), py (z.area.getMaxY ()),
											z.area.getWidth () * scale, z.area.getHeight () * scale);
		g.setColor (C_ZONE);
		g.fill (r);
		g.setColor (sel ? C_SEL : C_ZONE_B);
		g.setStroke (sel ? stroke (2.5f) : dashed (1.2f));
		g.draw (r);
		label (g, z.label, z.area.getMinX (), z.area.getMaxY (), sel ? C_SEL : C_ZONE_B);
	}

	private void drawFArea (Graphics2D g, WMFArea f, boolean sel)
	{
		Polygon2	p = f.polygon;
		if (p.npoints == 0)			return;
		Path2D.Double	path = new Path2D.Double ();
		for (int i = 0; i < p.npoints; i++)
		{
			if (i == 0)		path.moveTo (px (p.xpoints[i]), py (p.ypoints[i]));
			else			path.lineTo (px (p.xpoints[i]), py (p.ypoints[i]));
		}
		path.closePath ();
		g.setColor (C_FAREA);
		g.fill (path);
		g.setColor (sel ? C_SEL : C_FAREA_B);
		g.setStroke (stroke (sel ? 2.5f : 1.2f));
		g.draw (path);
		label (g, f.label, p.getCoGX (), p.getCoGY (), sel ? C_SEL : C_FAREA_B);
	}

	private void drawPath (Graphics2D g)
	{
		int		n = world.path ().n ();
		if (n == 0)			return;
		g.setColor (C_PATH);
		g.setStroke (stroke (1.5f));
		for (int i = 0; i < n - 1; i++)
		{
			Point2	a = world.path ().at (i), b = world.path ().at (i + 1);
			g.drawLine (toPixelX (a.x ()), toPixelY (a.y ()), toPixelX (b.x ()), toPixelY (b.y ()));
		}
		for (int i = 0; i < n; i++)
		{
			Point2	a = world.path ().at (i);
			boolean	sel = isSel (WorldItem.PATH, i);
			g.setColor (sel ? C_SEL : C_PATH);
			g.fill (new Ellipse2D.Double (px (a.x ()) - 3.5, py (a.y ()) - 3.5, 7, 7));
			if (sel || (scale > 60))		label (g, "P" + i, a.x (), a.y (), C_PATH);
		}
	}

	private void drawWall (Graphics2D g, WMWall w, boolean sel)
	{
		Line2	l = w.edge;
		float	thick = (float) Math.max (1.5, Math.min (8.0, w.width * scale));
		g.setColor (sel ? C_SEL : C_WALL);
		g.setStroke (stroke (sel ? thick + 1.5f : thick));
		g.draw (new Line2D.Double (px (l.orig ().x ()), py (l.orig ().y ()), px (l.dest ().x ()), py (l.dest ().y ())));
	}

	private void drawObject (Graphics2D g, WMObject o, boolean sel)
	{
		Color	c = sel ? C_SEL : ColorTool.fromWColorToColor (o.color);
		if (!o.visible && !sel)		c = new Color (c.getRed (), c.getGreen (), c.getBlue (), 90);
		g.setColor (c);
		g.setStroke (stroke (sel ? 2.5f : 1.5f));
		for (Line2 l : o.icon)
			g.draw (new Line2D.Double (px (l.orig ().x ()), py (l.orig ().y ()), px (l.dest ().x ()), py (l.dest ().y ())));
		// position and heading
		double	x = px (o.pos.x ()), y = py (o.pos.y ());
		g.setStroke (stroke (1f));
		g.draw (new Line2D.Double (x - 4, y, x + 4, y));
		g.draw (new Line2D.Double (x, y - 4, x, y + 4));
		if (o.shape != null)
		{
			double	ax = px (o.pos.x () + 0.3 * Math.cos (o.a)), ay = py (o.pos.y () + 0.3 * Math.sin (o.a));
			g.draw (new Line2D.Double (x, y, ax, ay));
			if (sel)	label (g, o.shape, o.pos.x (), o.pos.y (), C_SEL);
		}
	}

	private void drawDoor (Graphics2D g, WMDoor d, boolean sel)
	{
		g.setColor (sel ? C_SEL : C_DOOR);
		g.setStroke (stroke (sel ? 4f : 3f));
		g.draw (new Line2D.Double (px (d.edge.orig ().x ()), py (d.edge.orig ().y ()), px (d.edge.dest ().x ()), py (d.edge.dest ().y ())));
		g.setColor (sel ? C_SEL : C_DOORP);
		g.setStroke (dashed (1.5f));
		g.draw (new Line2D.Double (px (d.path.orig ().x ()), py (d.path.orig ().y ()), px (d.path.dest ().x ()), py (d.path.dest ().y ())));
		g.setStroke (stroke (1f));
		g.fill (new Ellipse2D.Double (px (d.path.orig ().x ()) - 3, py (d.path.orig ().y ()) - 3, 6, 6));
		g.fill (new Ellipse2D.Double (px (d.path.dest ().x ()) - 3, py (d.path.dest ().y ()) - 3, 6, 6));
		Point2	c = d.edge.center ();
		label (g, d.label, c.x (), c.y (), sel ? C_SEL : C_DOOR);
	}

	private void drawBeacon (Graphics2D g, WMBeacon b, boolean sel)
	{
		Line2	l = b.getLine ();
		g.setColor (sel ? C_SEL : C_BEACON);
		g.setStroke (stroke (sel ? 4f : 3f));
		g.draw (new Line2D.Double (px (l.orig ().x ()), py (l.orig ().y ()), px (l.dest ().x ()), py (l.dest ().y ())));
		// normal (facing side)
		double	nx = -Math.sin (b.pos.alpha ()), ny = Math.cos (b.pos.alpha ());
		g.setStroke (stroke (1f));
		g.draw (new Line2D.Double (px (b.pos.x ()), py (b.pos.y ()), px (b.pos.x () + 0.15 * nx), py (b.pos.y () + 0.15 * ny)));
		label (g, b.label, b.pos.x (), b.pos.y (), sel ? C_SEL : C_BEACON);
	}

	private void drawCBeacon (Graphics2D g, WMCBeacon b, boolean sel)
	{
		Ellipse2	e = b.beacon;
		double		rx = Math.max (3.0, e.horiz () * scale), ry = Math.max (3.0, e.vert () * scale);
		Ellipse2D	s = new Ellipse2D.Double (px (e.center ().x ()) - rx, py (e.center ().y ()) - ry, 2 * rx, 2 * ry);
		g.setColor (new Color (200, 0, 200, 80));
		g.fill (s);
		g.setColor (sel ? C_SEL : C_BEACON);
		g.setStroke (stroke (sel ? 2.5f : 1.5f));
		g.draw (s);
		label (g, b.label, e.center ().x (), e.center ().y (), sel ? C_SEL : C_BEACON);
	}

	private void drawPose (Graphics2D g, double x, double y, double a, double r, Color c, boolean sel, boolean square)
	{
		double	cxp = px (x), cyp = py (y);
		double	rp = Math.max (5.0, r * scale);
		g.setColor (sel ? C_SEL : c);
		g.setStroke (stroke (sel ? 2.5f : 1.5f));
		if (square)		g.draw (new Rectangle2D.Double (cxp - rp, cyp - rp, 2 * rp, 2 * rp));
		else			g.draw (new Ellipse2D.Double (cxp - rp, cyp - rp, 2 * rp, 2 * rp));
		// heading arrow
		double	len = Math.max (rp * 1.8, 0.35 * scale);
		double	ax = cxp + len * Math.cos (a), ay = cyp - len * Math.sin (a);
		g.draw (new Line2D.Double (cxp, cyp, ax, ay));
		double	hx1 = ax - 7 * Math.cos (a - 0.5), hy1 = ay + 7 * Math.sin (a - 0.5);
		double	hx2 = ax - 7 * Math.cos (a + 0.5), hy2 = ay + 7 * Math.sin (a + 0.5);
		g.draw (new Line2D.Double (ax, ay, hx1, hy1));
		g.draw (new Line2D.Double (ax, ay, hx2, hy2));
	}

	private void drawWaypoint (Graphics2D g, WMWaypoint p, boolean sel)
	{
		drawPose (g, p.pos.x (), p.pos.y (), p.pos.alpha (), world.G_RADIUS, C_WP, sel, false);
		label (g, p.label, p.pos.x (), p.pos.y (), sel ? C_SEL : C_WP);
	}

	private void drawDock (Graphics2D g, WMDock d, boolean sel)
	{
		drawPose (g, d.pos.x (), d.pos.y (), d.pos.alpha (), world.D_LENGHT / 2.0, C_DOCK, sel, true);
		label (g, d.label, d.pos.x (), d.pos.y (), sel ? C_SEL : C_DOCK);
	}

	private void drawStart (Graphics2D g, boolean sel)
	{
		double	x = world.start_x (), y = world.start_y ();
		g.setColor (new Color (220, 30, 30, 60));
		double	rp = Math.max (6.0, 0.25 * scale);
		g.fill (new Ellipse2D.Double (px (x) - rp, py (y) - rp, 2 * rp, 2 * rp));
		drawPose (g, x, y, world.start_a (), 0.25, C_START, sel, false);
		label (g, "START", x, y, sel ? C_SEL : C_START);
	}

	private void drawRubber (Graphics2D g)
	{
		g.setColor (C_RUBBER);
		g.setStroke (dashed (1.5f));
		if (dragMode == 4)
		{
			double	nx = snap (curX), ny = snap (curY);
			if (tool == T_ZONE)
			{
				double	x0 = Math.min (anchorX, nx), x1 = Math.max (anchorX, nx);
				double	y0 = Math.min (anchorY, ny), y1 = Math.max (anchorY, ny);
				g.draw (new Rectangle2D.Double (px (x0), py (y1), (x1 - x0) * scale, (y1 - y0) * scale));
			}
			else
				g.draw (new Line2D.Double (px (anchorX), py (anchorY), px (nx), py (ny)));
			g.setColor (new Color (0, 0, 0, 170));
			g.setFont (g.getFont ().deriveFont (Font.PLAIN, 11f));
			g.drawString ((tool == T_ZONE) ? (WorldEdit.fmt (Math.abs (nx - anchorX)) + " x " + WorldEdit.fmt (Math.abs (ny - anchorY)) + " m")
										   : (WorldEdit.fmt (Math.hypot (nx - anchorX, ny - anchorY)) + " m"),
						  toPixelX (nx) + 10, toPixelY (ny) - 10);
		}
		if ((tool == T_FAREA) && (polyPoints.size () > 0))
		{
			Path2D.Double	path = new Path2D.Double ();
			for (int i = 0; i < polyPoints.size (); i++)
			{
				Point2	p = polyPoints.get (i);
				if (i == 0)		path.moveTo (px (p.x ()), py (p.y ()));
				else			path.lineTo (px (p.x ()), py (p.y ()));
			}
			path.lineTo (px (snap (curX)), py (snap (curY)));
			g.setColor (C_FAREA);
			g.fill (path);
			g.setColor (C_RUBBER);
			g.draw (path);
			for (Point2 p : polyPoints)
				g.fill (new Ellipse2D.Double (px (p.x ()) - 3, py (p.y ()) - 3, 6, 6));
		}
	}

	private void drawHandles (Graphics2D g)
	{
		Point2[]	hs = WorldEdit.handles (world, selection);
		g.setStroke (stroke (1.2f));
		for (int i = 0; i < hs.length; i++)
		{
			int		x = toPixelX (hs[i].x ()), y = toPixelY (hs[i].y ());
			boolean	rot = (i == hs.length - 1) && ((selection.kind == WorldItem.WAYPOINT) || (selection.kind == WorldItem.DOCK)
						|| (selection.kind == WorldItem.START) || (selection.kind == WorldItem.OBJECT));
			if (rot)
			{
				g.setColor (C_SEL);
				g.drawLine (toPixelX (hs[0].x ()), toPixelY (hs[0].y ()), x, y);
				g.setColor (C_HANDLE);
				g.fillOval (x - HANDLE_PX, y - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
				g.setColor (C_SEL);
				g.drawOval (x - HANDLE_PX, y - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
			}
			else
			{
				g.setColor (C_HANDLE);
				g.fillRect (x - HANDLE_PX, y - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
				g.setColor (C_SEL);
				g.drawRect (x - HANDLE_PX, y - HANDLE_PX, 2 * HANDLE_PX, 2 * HANDLE_PX);
			}
		}
	}
}
