/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

import tc.shared.world.WMConnector;
import tc.shared.world.WMWall;
import tc.shared.world.WMZone;
import tc.shared.world.World;
import tclib.planning.htopol.GNodeFL;
import tclib.planning.htopol.GNodeSL;
import tclib.planning.htopol.HTopolMap;
import tclib.utils.graphs.GNode;
import tclib.utils.graphs.Graph;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * Canvas of one level of the hierarchical topological map, drawn over the
 * world it belongs to. At the root level the candidate nodes are the zones
 * of the world; below, the places of the zone (waypoints, docks and doors).
 * Nodes are added by clicking on a candidate place, arcs by clicking on two
 * nodes; the selection (a node or an arc) is edited in the dialog.
 */
public class TopolCanvas extends JPanel
{
	private static final long		serialVersionUID = 1L;

	static public final int			T_SELECT	= 0;
	static public final int			T_NODE		= 1;
	static public final int			T_ARC		= 2;

	static private final double		NODE_R		= 9.0;		// node radius (px)
	static private final double		PLACE_R		= 5.0;		// candidate place radius (px)
	static private final double		ARC_GAP		= 5.0;		// separation of reciprocal arcs (px)

	static private final Color		C_BG		= new Color (253, 251, 240);
	static private final Color		C_GRID		= new Color (235, 231, 215);
	static private final Color		C_WALL		= new Color (150, 150, 150);
	static private final Color		C_ZONE		= new Color (120, 160, 210);
	static private final Color		C_ZONE_FILL	= new Color (120, 160, 210, 22);
	static private final Color		C_PLACE		= new Color (150, 150, 150);
	static private final Color		C_NODE		= new Color (40, 90, 170);
	static private final Color		C_NODE_FILL	= new Color (215, 230, 250);
	static private final Color		C_ARC		= new Color (60, 60, 60);
	static private final Color		C_SEL		= new Color (220, 60, 40);
	static private final Color		C_PENDING	= new Color (240, 150, 40);

	/** What the dialog needs to know. */
	public interface Listener
	{
		void selectionChanged (TopolCanvas canvas);
		void graphChanged (TopolCanvas canvas, String what);
		void openNode (TopolCanvas canvas, GNode node);
		void usageChanged (String text);
	}

	/** A candidate place for a node: a labelled element of the world. */
	static class Place
	{
		String	label;
		double	x, y;
		Place (String label, double x, double y)		{ this.label = label; this.x = x; this.y = y; }
	}

	protected World					world;
	protected Graph					graph;				// the level being edited
	protected boolean				root;				// true: graph is the HTopolMap (nodes are zones)
	protected String				zone;				// zone the level belongs to (null at root)
	protected GNode					owner;				// node whose graph this is (null at root)
	protected Listener				listener;

	protected int					tool		= T_SELECT;
	protected GNode					selNode;			// selected node
	protected GNode					selFrom, selTo;		// selected arc
	protected GNode					pending;			// first node of an arc being created

	protected double				scale		= 30.0;
	protected double				cx, cy;
	protected int					lastX, lastY;
	protected boolean				panning;
	protected boolean				fitted;

	protected List<Place>			places		= new ArrayList<Place> ();

	public TopolCanvas (World world, Graph graph, GNode owner, String zone, Listener listener)
	{
		this.world		= world;
		this.graph		= graph;
		this.owner		= owner;
		this.zone		= zone;
		this.root		= (graph instanceof HTopolMap);
		this.listener	= listener;

		setBackground (C_BG);
		setFocusable (true);
		MouseAdapter	ma = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)		{ onPress (e); }
			public void mouseReleased (MouseEvent e)	{ panning = false; }
			public void mouseDragged (MouseEvent e)		{ onDrag (e); }
			public void mouseClicked (MouseEvent e)		{ onClick (e); }
			public void mouseWheelMoved (MouseWheelEvent e)	{ onWheel (e); }
		};
		addMouseListener (ma);
		addMouseMotionListener (ma);
		addMouseWheelListener (ma);
		refreshPlaces ();
	}

	/* ---- accessors ---- */

	public Graph		getGraph ()				{ return graph; }
	public GNode		getOwner ()				{ return owner; }
	public boolean	isRoot ()				{ return root; }
	public String	getZone ()				{ return zone; }
	public GNode		getSelectedNode ()		{ return selNode; }
	public GNode		getArcFrom ()			{ return selFrom; }
	public GNode		getArcTo ()				{ return selTo; }
	public boolean	hasArcSelected ()		{ return (selFrom != null) && (selTo != null); }
	public int		getTool ()				{ return tool; }

	public void setTool (int tool)
	{
		this.tool	= tool;
		pending		= null;
		if (listener != null)		listener.usageChanged (usageText ());
		repaint ();
	}

	public String usageText ()
	{
		switch (tool)
		{
		case T_NODE:	return root ? "Click on a zone to add it as a node" : "Click on a waypoint, dock or door of the zone to add it as a node";
		case T_ARC:		return (pending == null) ? "Click on the origin node of the arc" : "Click on the destination node of the arc from " + pending.getLabel () + " (Esc: cancel)";
		default:		return "Click to select a node or an arc; double click on a node to edit its level. Drag: pan, wheel: zoom";
		}
	}

	/* ---- model helpers ---- */

	/** Candidate places of this level (zones at the root, the places of the zone below). */
	public void refreshPlaces ()
	{
		places.clear ();
		if (root)
		{
			for (int i = 0; i < world.zones ().n (); i++)
			{
				WMZone	z = world.zones ().at (i);
				places.add (new Place (z.label, z.area.getCenterX (), z.area.getCenterY ()));
			}
		}
		else if (zone != null)
		{
			for (int i = 0; i < world.wps ().n (); i++)
				if (zone.equals (world.zones ().inZone (world.wps ().at (i).pos)))
					places.add (new Place (world.wps ().at (i).label, world.wps ().at (i).pos.x (), world.wps ().at (i).pos.y ()));
			for (int i = 0; i < world.docks ().n (); i++)
				if (zone.equals (world.zones ().inZone (world.docks ().at (i).pos)))
					places.add (new Place (world.docks ().at (i).label, world.docks ().at (i).pos.x (), world.docks ().at (i).pos.y ()));
			for (int i = 0; i < world.connectors ().n (); i++)
			{
				WMConnector	c = world.connectors ().at (i);
				Point3		p = null;
				if (zone.equals (world.zones ().inZone (c.path.orig ())))			p = new Point3 (c.path.orig ());
				else if (zone.equals (world.zones ().inZone (c.path.dest ())))		p = new Point3 (c.path.dest ());
				if (p != null)		places.add (new Place (c.label, p.x (), p.y ()));
			}
		}
		repaint ();
	}

	protected Place placeOf (String label)
	{
		for (Place p : places)		if (p.label.equals (label))		return p;
		return null;
	}

	/** World position of a node of this level (its place), or null when the world no longer has it. */
	public double[] positionOf (GNode n)
	{
		Place	p = placeOf (n.getLabel ());
		return (p == null) ? null : new double[] { p.x, p.y };
	}

	public boolean hasNode (String label)			{ return graph.getNode (label) != null; }

	/** Adds a node for a place of the world. */
	public GNode addNode (String label)
	{
		if (hasNode (label))		return graph.getNode (label);
		GNode	n = root ? new GNodeFL (label) : new GNodeSL (label, zone);
		graph.insNode (n);
		return n;
	}

	/** Adds an arc; at the root the door is the first connector joining both zones, if any. */
	public boolean addArc (GNode from, GNode to)
	{
		if ((from == to) || from.hasArc (to.index ()))		return false;
		if (root)
		{
			GNodeFL	f = (GNodeFL) from;
			f.addNode (to, 1);
			f.setDoor (to.getLabel (), defaultDoor (from.getLabel (), to.getLabel ()));
		}
		else
			from.addNode (to, 0);
		return true;
	}

	/** A connector whose path joins the two zones (null when there is none). */
	public String defaultDoor (String zoneA, String zoneB)
	{
		for (int i = 0; i < world.connectors ().n (); i++)
		{
			WMConnector	c = world.connectors ().at (i);
			String		zo = world.zones ().inZone (c.path.orig ()), zd = world.zones ().inZone (c.path.dest ());
			if ((zo.equals (zoneA) && zd.equals (zoneB)) || (zo.equals (zoneB) && zd.equals (zoneA)))		return c.label;
		}
		return null;
	}

	/** Removes the selection (node or arc); false when nothing was selected. */
	public boolean deleteSelection ()
	{
		if (selNode != null)
		{
			GNode	n = selNode;
			clearSelection ();
			graph.removeNode (n);
			return true;
		}
		if ((selFrom != null) && (selTo != null))
		{
			selFrom.removeArc (selTo.index ());
			if (root)		((GNodeFL) selFrom).removeDoors (selTo.getLabel ());
			clearSelection ();
			return true;
		}
		return false;
	}

	public void clearSelection ()
	{
		selNode = selFrom = selTo = null;
		repaint ();
	}

	public void select (GNode n)
	{
		selNode = n;
		selFrom = selTo = null;
		repaint ();
		if (listener != null)		listener.selectionChanged (this);
	}

	public void selectArc (GNode from, GNode to)
	{
		selNode = null;
		selFrom = from;
		selTo = to;
		repaint ();
		if (listener != null)		listener.selectionChanged (this);
	}

	/** Cancels an arc being created. */
	public void cancelPending ()
	{
		pending = null;
		if (listener != null)		listener.usageChanged (usageText ());
		repaint ();
	}

	/* ---- view ---- */

	private double px (double x)					{ return getWidth () / 2.0 + (x - cx) * scale; }
	private double py (double y)					{ return getHeight () / 2.0 - (y - cy) * scale; }

	public void zoom (double factor)
	{
		scale = Math.max (2.0, Math.min (5000.0, scale * factor));
		repaint ();
	}

	/** Bounds of the region shown: the whole world at the root, the zone below (metres). */
	protected double[] regionBounds ()
	{
		double	minx = Double.MAX_VALUE, miny = Double.MAX_VALUE, maxx = -Double.MAX_VALUE, maxy = -Double.MAX_VALUE;
		if (!root && (zone != null) && (world.zones ().at (zone) != null))
		{
			WMZone	z = world.zones ().at (zone);
			return new double[] { z.minx (), z.miny (), z.minx () + z.width (), z.miny () + z.height () };
		}
		for (int i = 0; i < world.zones ().n (); i++)
		{
			WMZone	z = world.zones ().at (i);
			minx = Math.min (minx, z.minx ());	miny = Math.min (miny, z.miny ());
			maxx = Math.max (maxx, z.minx () + z.width ());	maxy = Math.max (maxy, z.miny () + z.height ());
		}
		for (WMWall w : world.walls ().edges ())
		{
			minx = Math.min (minx, Math.min (w.edge.orig ().x (), w.edge.dest ().x ()));	maxx = Math.max (maxx, Math.max (w.edge.orig ().x (), w.edge.dest ().x ()));
			miny = Math.min (miny, Math.min (w.edge.orig ().y (), w.edge.dest ().y ()));	maxy = Math.max (maxy, Math.max (w.edge.orig ().y (), w.edge.dest ().y ()));
		}
		if (minx > maxx)		return new double[] { -5, -5, 5, 5 };
		return new double[] { minx, miny, maxx, maxy };
	}

	public void zoomToFit ()
	{
		double[]	b = regionBounds ();
		double		w = Math.max (0.5, b[2] - b[0]), h = Math.max (0.5, b[3] - b[1]);
		double		pw = Math.max (50, getWidth () - 60), ph = Math.max (50, getHeight () - 60);
		scale	= Math.max (2.0, Math.min (5000.0, Math.min (pw / w, ph / h)));
		cx		= (b[0] + b[2]) / 2.0;
		cy		= (b[1] + b[3]) / 2.0;
		fitted	= true;
		repaint ();
	}

	/* ---- painting ---- */

	protected void paintComponent (Graphics g0)
	{
		super.paintComponent (g0);
		if (!fitted && (getWidth () > 0))		zoomToFit ();

		Graphics2D	g = (Graphics2D) g0.create ();
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		paintGrid (g);
		paintWorld (g);
		paintPlaces (g);
		paintArcs (g);
		paintNodes (g);
		g.dispose ();
	}

	private void paintGrid (Graphics2D g)
	{
		double	step = 1.0;
		while (step * scale < 25)		step *= 2;
		g.setColor (C_GRID);
		g.setStroke (new BasicStroke (1f));
		double	x0 = Math.floor ((cx - getWidth () / 2.0 / scale) / step) * step;
		double	y0 = Math.floor ((cy - getHeight () / 2.0 / scale) / step) * step;
		for (double x = x0; px (x) < getWidth (); x += step)		g.draw (new Line2D.Double (px (x), 0, px (x), getHeight ()));
		for (double y = y0; py (y) > 0; y += step)					g.draw (new Line2D.Double (0, py (y), getWidth (), py (y)));
	}

	private void paintWorld (Graphics2D g)
	{
		// zones (all of them at the root, the current one below)
		g.setStroke (new BasicStroke (1.2f));
		for (int i = 0; i < world.zones ().n (); i++)
		{
			WMZone	z = world.zones ().at (i);
			boolean	mine = root || z.label.equals (zone);
			Rectangle2D	r = new Rectangle2D.Double (px (z.minx ()), py (z.miny () + z.height ()), z.width () * scale, z.height () * scale);
			g.setColor (mine ? C_ZONE_FILL : new Color (0, 0, 0, 8));
			g.fill (r);
			g.setColor (mine ? C_ZONE : new Color (200, 200, 200));
			g.draw (r);
			if (root)
				label (g, z.label, z.minx () + 0.2, z.miny () + z.height () - 0.2, C_ZONE, false);
		}
		// walls, thin
		g.setColor (C_WALL);
		g.setStroke (new BasicStroke (1f));
		for (WMWall w : world.walls ().edges ())
		{
			Line2	l = w.edge;
			g.draw (new Line2D.Double (px (l.orig ().x ()), py (l.orig ().y ()), px (l.dest ().x ()), py (l.dest ().y ())));
		}
		// doors
		g.setColor (new Color (200, 120, 40));
		g.setStroke (new BasicStroke (2f));
		for (int i = 0; i < world.connectors ().n (); i++)
		{
			Line2	l = world.connectors ().at (i).edge;
			g.draw (new Line2D.Double (px (l.orig ().x ()), py (l.orig ().y ()), px (l.dest ().x ()), py (l.dest ().y ())));
		}
	}

	private void paintPlaces (Graphics2D g)
	{
		if (root)		return;							// the zones themselves are the candidates
		g.setStroke (new BasicStroke (1.2f));
		for (Place p : places)
		{
			if (hasNode (p.label))		continue;
			g.setColor (Color.WHITE);
			g.fill (new Ellipse2D.Double (px (p.x) - PLACE_R, py (p.y) - PLACE_R, 2 * PLACE_R, 2 * PLACE_R));
			g.setColor (C_PLACE);
			g.draw (new Ellipse2D.Double (px (p.x) - PLACE_R, py (p.y) - PLACE_R, 2 * PLACE_R, 2 * PLACE_R));
			label (g, p.label, p.x, p.y, C_PLACE, true);
		}
	}

	private void paintArcs (Graphics2D g)
	{
		g.setFont (getFont ().deriveFont (Font.PLAIN, 10f));
		for (int i = 0; i < graph.numNodes (); i++)
		{
			GNode		from = graph.getNode (i);
			double[]	a = positionOf (from);
			if (a == null)		continue;
			for (int j = 0; j < from.nList (); j++)
			{
				GNode		to = graph.getNode (from.getList (j));
				double[]	b = positionOf (to);
				if (b == null)		continue;
				boolean		sel = (from == selFrom) && (to == selTo);
				boolean		back = to.hasArc (from.index ());		// reciprocal arc: offset both sideways
				String		txt = root ? ((GNodeFL) from).getDoor (to.getLabel ()) : ((from.getPeso (j) != 0) ? String.valueOf (from.getPeso (j)) : null);
				arrow (g, px (a[0]), py (a[1]), px (b[0]), py (b[1]), back ? ARC_GAP : 0.0, sel ? C_SEL : C_ARC, sel ? 2.4f : 1.4f, txt, back);
			}
		}
	}

	private void arrow (Graphics2D g, double x1, double y1, double x2, double y2, double gap, Color c, float width, String txt, boolean reciprocal)
	{
		double	dx = x2 - x1, dy = y2 - y1, len = Math.hypot (dx, dy);
		if (len < 1e-6)		return;
		double	ux = dx / len, uy = dy / len;						// unit vector along the arc
		double	nx = -uy, ny = ux;									// left normal (screen)
		// shift sideways when there is a reciprocal arc, and stop at the node borders
		double	sx1 = x1 + nx * gap + ux * NODE_R, sy1 = y1 + ny * gap + uy * NODE_R;
		double	sx2 = x2 + nx * gap - ux * NODE_R, sy2 = y2 + ny * gap - uy * NODE_R;
		g.setColor (c);
		g.setStroke (new BasicStroke (width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw (new Line2D.Double (sx1, sy1, sx2, sy2));
		double	hl = 9.0, hw = 4.5;
		Path2D	head = new Path2D.Double ();
		head.moveTo (sx2, sy2);
		head.lineTo (sx2 - ux * hl + nx * hw, sy2 - uy * hl + ny * hw);
		head.lineTo (sx2 - ux * hl - nx * hw, sy2 - uy * hl - ny * hw);
		head.closePath ();
		g.fill (head);
		if ((txt != null) && (txt.length () > 0))
		{
			// label beside the arc; with a reciprocal arc each label sits nearer its own origin so they do not overlap
			double	t = reciprocal ? 0.35 : 0.5;
			double	mx = sx1 + (sx2 - sx1) * t + nx * 8.0, my = sy1 + (sy2 - sy1) * t + ny * 8.0;
			FontMetrics	fm = g.getFontMetrics ();
			int		tw = fm.stringWidth (txt);
			g.setColor (new Color (255, 255, 255, 200));
			g.fillRect ((int) (mx - tw / 2.0 - 2), (int) (my - fm.getAscent () / 2.0 - 1), tw + 4, fm.getAscent () + 2);
			g.setColor (c);
			g.drawString (txt, (float) (mx - tw / 2.0), (float) (my + fm.getAscent () / 2.0 - 1));
		}
	}

	private void paintNodes (Graphics2D g)
	{
		g.setFont (getFont ().deriveFont (Font.BOLD, 11f));
		for (int i = 0; i < graph.numNodes (); i++)
		{
			GNode		n = graph.getNode (i);
			double[]	p = positionOf (n);
			if (p == null)		continue;
			boolean		sel = (n == selNode), pend = (n == pending);
			boolean		deeper = (n instanceof GNodeFL) ? (((GNodeFL) n).getGraph ().numNodes () > 0) : ((n instanceof GNodeSL) && ((GNodeSL) n).hasGraph ());
			Ellipse2D	e = new Ellipse2D.Double (px (p[0]) - NODE_R, py (p[1]) - NODE_R, 2 * NODE_R, 2 * NODE_R);
			g.setColor (sel ? new Color (250, 215, 205) : C_NODE_FILL);
			g.fill (e);
			g.setColor (sel ? C_SEL : (pend ? C_PENDING : C_NODE));
			g.setStroke (new BasicStroke ((sel || pend) ? 2.5f : 1.6f));
			g.draw (e);
			if (deeper)															// a level below: inner ring
			{
				g.setStroke (new BasicStroke (1f));
				g.draw (new Ellipse2D.Double (px (p[0]) - NODE_R + 3, py (p[1]) - NODE_R + 3, 2 * NODE_R - 6, 2 * NODE_R - 6));
			}
			label (g, n.getLabel (), p[0], p[1], sel ? C_SEL : C_NODE, true);
		}
	}

	private void label (Graphics2D g, String text, double x, double y, Color c, boolean besideNode)
	{
		if (text == null)		return;
		FontMetrics	fm = g.getFontMetrics ();
		float	tx = (float) (px (x) + (besideNode ? NODE_R + 3 : 0));
		float	ty = (float) (py (y) + (besideNode ? fm.getAscent () / 2.0 - 1 : fm.getAscent ()));
		g.setColor (new Color (255, 255, 255, 190));
		g.fillRect ((int) tx - 1, (int) (ty - fm.getAscent ()), fm.stringWidth (text) + 2, fm.getAscent () + 2);
		g.setColor (c);
		g.drawString (text, tx, ty);
	}

	/* ---- picking ---- */

	protected GNode nodeAt (int x, int y)
	{
		for (int i = graph.numNodes () - 1; i >= 0; i--)
		{
			GNode		n = graph.getNode (i);
			double[]	p = positionOf (n);
			if ((p != null) && (Math.hypot (px (p[0]) - x, py (p[1]) - y) <= NODE_R + 2))		return n;
		}
		return null;
	}

	protected Place placeAt (int x, int y)
	{
		if (root)
		{
			for (int i = 0; i < world.zones ().n (); i++)
			{
				WMZone	z = world.zones ().at (i);
				if (new Rectangle2D.Double (px (z.minx ()), py (z.miny () + z.height ()), z.width () * scale, z.height () * scale).contains (x, y))
					return placeOf (z.label);
			}
			return null;
		}
		Place	best = null;
		double	bd = PLACE_R + 6;
		for (Place p : places)
		{
			double	d = Math.hypot (px (p.x) - x, py (p.y) - y);
			if (d < bd)		{ bd = d; best = p; }
		}
		return best;
	}

	/** The arc whose line passes near the point, as {from, to}. */
	protected GNode[] arcAt (int x, int y)
	{
		GNode[]	best = null;
		double	bd = 6.0;
		for (int i = 0; i < graph.numNodes (); i++)
		{
			GNode		from = graph.getNode (i);
			double[]	a = positionOf (from);
			if (a == null)		continue;
			for (int j = 0; j < from.nList (); j++)
			{
				GNode		to = graph.getNode (from.getList (j));
				double[]	b = positionOf (to);
				if (b == null)		continue;
				double	x1 = px (a[0]), y1 = py (a[1]), x2 = px (b[0]), y2 = py (b[1]);
				if (to.hasArc (from.index ()))
				{
					double	dx = x2 - x1, dy = y2 - y1, len = Math.hypot (dx, dy);
					if (len > 1e-6)		{ x1 += -dy / len * ARC_GAP; x2 += -dy / len * ARC_GAP; y1 += dx / len * ARC_GAP; y2 += dx / len * ARC_GAP; }
				}
				double	d = Line2D.ptSegDist (x1, y1, x2, y2, x, y);
				if (d < bd)		{ bd = d; best = new GNode[] { from, to }; }
			}
		}
		return best;
	}

	/* ---- mouse ---- */

	private void onPress (MouseEvent e)
	{
		requestFocusInWindow ();
		lastX = e.getX ();
		lastY = e.getY ();
		panning = (tool == T_SELECT) && (nodeAt (lastX, lastY) == null) && (arcAt (lastX, lastY) == null);
	}

	private void onDrag (MouseEvent e)
	{
		if (!panning)		return;
		cx -= (e.getX () - lastX) / scale;
		cy += (e.getY () - lastY) / scale;
		lastX = e.getX ();
		lastY = e.getY ();
		repaint ();
	}

	private void onClick (MouseEvent e)
	{
		if (e.getButton () != MouseEvent.BUTTON1)
		{
			if (tool != T_SELECT)		setTool (T_SELECT);
			return;
		}
		int		x = e.getX (), y = e.getY ();
		switch (tool)
		{
		case T_NODE:
		{
			Place	p = placeAt (x, y);
			if ((p == null) || hasNode (p.label))		return;
			GNode	n = addNode (p.label);
			select (n);
			if (listener != null)		listener.graphChanged (this, "Add node " + p.label);
			break;
		}
		case T_ARC:
		{
			GNode	n = nodeAt (x, y);
			if (n == null)		return;
			if (pending == null)
			{
				pending = n;
				if (listener != null)		listener.usageChanged (usageText ());
				repaint ();
			}
			else
			{
				GNode	from = pending;
				pending = null;
				if (addArc (from, n))
				{
					selectArc (from, n);
					if (listener != null)		listener.graphChanged (this, "Add arc " + from.getLabel () + " - " + n.getLabel ());
				}
				if (listener != null)		listener.usageChanged (usageText ());
				repaint ();
			}
			break;
		}
		default:
		{
			GNode	n = nodeAt (x, y);
			if (n != null)
			{
				select (n);
				if ((e.getClickCount () == 2) && (listener != null))		listener.openNode (this, n);
				return;
			}
			GNode[]	arc = arcAt (x, y);
			if (arc != null)		selectArc (arc[0], arc[1]);
			else					{ clearSelection (); if (listener != null) listener.selectionChanged (this); }
		}
		}
	}

	private void onWheel (MouseWheelEvent e)
	{
		double	wx = cx + (e.getX () - getWidth () / 2.0) / scale;
		double	wy = cy - (e.getY () - getHeight () / 2.0) / scale;
		double	f = (e.getWheelRotation () < 0) ? 1.15 : 1 / 1.15;
		scale	= Math.max (2.0, Math.min (5000.0, scale * f));
		cx		= wx - (e.getX () - getWidth () / 2.0) / scale;
		cy		= wy + (e.getY () - getHeight () / 2.0) / scale;
		repaint ();
	}
}
