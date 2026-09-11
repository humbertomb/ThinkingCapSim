/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.arch;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import tcapps.tcsimulator.arch.ArchModel.Block;

/**
 * Block diagram of an architecture ({@link ArchModel}): the global Linda
 * space on top, and below it the robot as a dashed region with its router
 * straddling the upper border, the local Linda space in the centre, the
 * modules on both sides and the virtual robot at the bottom, every one of
 * them linked to the local Linda with a double arrow. The layout is
 * automatic; blocks can be selected with the mouse.
 */
public class ArchCanvas extends JPanel
{
	private static final long		serialVersionUID = 1L;

	/** Notified when the selected block changes (null: nothing selected). */
	public interface Listener
	{
		public void blockSelected (Block block);
		public void blockActivated (Block block);		// double click on a block that cannot be renamed in place
		public void blockRenamed (Block block);			// name edited in place (robot name or module INFO)
	}

	// --- geometry (pixels)
	static final int				MARGIN		= 24;
	static final int				BOX_W		= 124,	BOX_H		= 44;
	static final int				LINDA_W		= 132,	LINDA_H		= 66;
	static final int				VROB_W		= 136,	VROB_H		= 50;
	static final int				COL_DX		= 205;						// module column offset from the centre
	static final int				ROW_DY		= 66;						// module row pitch
	static final int				REGION_HW	= 290;						// robot region half width
	static final int				REGION_PAD	= 22;
	static final int				REGION_GAP	= 40;						// between robots

	// --- colours
	static final Color				C_LINDA		= new Color (205, 225, 250);
	static final Color				C_ROUTER	= new Color (255, 238, 195);
	static final Color				C_MODULE	= new Color (222, 242, 222);
	static final Color				C_VROBOT	= new Color (250, 222, 222);
	static final Color				C_LINE		= new Color (60, 60, 60);
	static final Color				C_REGION	= new Color (150, 150, 150);
	static final Color				C_REGION_BG	= new Color (245, 245, 245);
	static final Color				C_SELECT	= new Color (30, 110, 230);
	static final Color				C_ARROW		= new Color (90, 90, 90);

	protected ArchModel				model;
	protected Block					selection;
	protected List<Listener>		listeners	= new ArrayList<Listener> ();

	// layout, rebuilt at every paint
	protected Map<Block, Rectangle>	bounds		= new LinkedHashMap<Block, Rectangle> ();	// robot containers included
	protected Map<Integer, Rectangle>	robotLabels	= new java.util.HashMap<Integer, Rectangle> ();	// name bounds per robot
	protected Dimension				layoutSize	= new Dimension (600, 400);
	protected int					offx, offy;			// centring offset

	// in-place name editor
	protected javax.swing.JTextField	editor;
	protected Block					editing;

	public ArchCanvas (ArchModel model)
	{
		this.model	= model;
		setLayout (null);
		setBackground (Color.white);
		setFont (getFont ().deriveFont (Font.PLAIN, 12f));
		addComponentListener (new java.awt.event.ComponentAdapter ()
		{
			public void componentResized (java.awt.event.ComponentEvent e)		{ layoutBlocks (); placeEditor (); }
		});
		addMouseListener (new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)
			{
				stopEditing (true);
				requestFocusInWindow ();
				Block	b = blockAt (e.getPoint ());
				setSelection (b);
				if ((e.getClickCount () == 2) && (b != null))
				{
					if (model.isRenameable (b))
					{
						// the robot is renamed by double-clicking on its name; modules anywhere on the box
						if ((b.kind != ArchModel.ROBOT) || isOnRobotName (e.getPoint ()))		startEditing (b);
					}
					else
						for (Listener l : listeners)		l.blockActivated (b);
				}
			}
		});
	}

	public void addListener (Listener l)		{ listeners.add (l); }
	public void removeListener (Listener l)		{ listeners.remove (l); }

	public void setModel (ArchModel m)			{ stopEditing (false); model = m; selection = null; modelChanged (); }
	public ArchModel getModel ()				{ return model; }

	/** The model changed: recomputes the layout and repaints. */
	public void modelChanged ()
	{
		stopEditing (false);
		if ((selection != null) && !exists (selection))		selection = null;
		layoutBlocks ();
		revalidate ();
		repaint ();
	}

	public Block getSelection ()				{ return selection; }

	public void setSelection (Block b)
	{
		if ((b == null) ? (selection == null) : b.equals (selection))
		{
			repaint ();
			return;
		}
		selection = b;
		repaint ();
		for (Listener l : listeners)		l.blockSelected (b);
	}

	/** True when the block is part of the current model. */
	public boolean blockExists (Block b)		{ return exists (b); }

	protected boolean exists (Block b)			{ return model.exists (b); }

	/** True when the point is over the name of a robot (top-left corner of its region). */
	public boolean isOnRobotName (Point p)
	{
		for (Rectangle r : robotLabels.values ())
			if (r.contains (p.x - offx, p.y - offy))		return true;
		return false;
	}

	/** Block under a point (a robot region counts when nothing inside it does). */
	public Block blockAt (Point p)
	{
		Point	q = new Point (p.x - offx, p.y - offy);
		for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
			if ((e.getKey ().kind != ArchModel.ROBOT) && e.getValue ().contains (q))		return e.getKey ();
		for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
			if ((e.getKey ().kind == ArchModel.ROBOT) && e.getValue ().contains (q))		return e.getKey ();
		return null;
	}

	/* ------------------------------------------------------------------ */
	/* In-place editing of names                                           */
	/* ------------------------------------------------------------------ */

	/** Opens a text field over the name of the block (robot name or module INFO). */
	public void startEditing (Block b)
	{
		stopEditing (true);
		Rectangle	r = (b.kind == ArchModel.ROBOT) ? robotLabels.get (b.robot) : bounds.get (b);
		if (r == null)					return;
		editing	= b;
		editor	= new javax.swing.JTextField (model.nameOf (b));
		editor.setFont (getFont ().deriveFont (Font.BOLD, 12f));
		editor.setHorizontalAlignment ((b.kind == ArchModel.ROBOT) ? javax.swing.JTextField.LEFT : javax.swing.JTextField.CENTER);
		editor.setBorder (javax.swing.BorderFactory.createLineBorder (C_SELECT, 1));
		placeEditor ();
		editor.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (java.awt.event.ActionEvent e)		{ stopEditing (true); }
		});
		editor.getInputMap ().put (javax.swing.KeyStroke.getKeyStroke (java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancel");
		editor.getActionMap ().put ("cancel", new javax.swing.AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (java.awt.event.ActionEvent e)		{ stopEditing (false); }
		});
		editor.addFocusListener (new java.awt.event.FocusAdapter ()
		{
			public void focusLost (java.awt.event.FocusEvent e)		{ stopEditing (true); }
		});
		add (editor);
		editor.selectAll ();
		editor.requestFocusInWindow ();
		repaint ();
	}

	/** Puts the editor over the name of the block being edited (also after the diagram moved). */
	protected void placeEditor ()
	{
		if (editor == null)				return;
		Rectangle	r = (editing.kind == ArchModel.ROBOT) ? robotLabels.get (editing.robot) : bounds.get (editing);
		if (r == null)					return;
		int		w = Math.max (r.width, 120), h = 24;
		int		x = (editing.kind == ArchModel.ROBOT) ? r.x : r.x + (r.width - w) / 2;
		editor.setBounds (x + offx, r.y + offy + (r.height - h) / 2, w, h);
	}

	/** Closes the name editor, applying the new name when <code>commit</code>. */
	public void stopEditing (boolean commit)
	{
		if (editor == null)				return;
		javax.swing.JTextField	ed = editor;
		Block					b = editing;
		editor	= null;
		editing	= null;
		String	name = ed.getText ().trim ();
		remove (ed);
		if (commit && (name.length () > 0) && !name.equals (model.nameOf (b)))
		{
			model.setName (b, name);
			for (Listener l : listeners)		l.blockRenamed (b);
		}
		repaint ();
	}

	public boolean isEditing ()					{ return editor != null; }

	/* ------------------------------------------------------------------ */
	/* Layout                                                              */
	/* ------------------------------------------------------------------ */

	protected void layoutBlocks ()
	{
		bounds.clear ();
		robotLabels.clear ();
		List<Integer>	robots = model.robots ();
		int				n = Math.max (1, robots.size ());
		int				total = n * 2 * REGION_HW + (n - 1) * REGION_GAP;		// width of the row of robots
		int				cx = MARGIN + total / 2;
		int				y = MARGIN;

		if (model.hasGlobalLinda ())
		{
			bounds.put (new Block (ArchModel.GLOBAL_LINDA, "GLIN", -1), new Rectangle (cx - LINDA_W / 2, y, LINDA_W, LINDA_H));
			y += LINDA_H + 52;
		}

		int		bottom = y;
		for (int i = 0; i < robots.size (); i++)
		{
			int		r = robots.get (i);
			int		rcx = MARGIN + REGION_HW + i * (2 * REGION_HW + REGION_GAP);
			int		top = y + BOX_H / 2;								// region top: the router straddles it
			if (model.hasRouter (r))
				bounds.put (new Block (ArchModel.ROUTER, model.routerPrefix (r), r), new Rectangle (rcx - BOX_W / 2, y, BOX_W, BOX_H));
			int		modTop = top + BOX_H / 2 + 34;
			List<String>	mods = model.modulePrefixes (r);
			int		nrows = (mods.size () + 1) / 2;
			int		modsH = Math.max (nrows * ROW_DY - (ROW_DY - BOX_H), LINDA_H);
			for (int m = 0; m < mods.size (); m++)
			{
				int		col = (m % 2 == 0) ? -1 : 1;
				int		row = m / 2;
				int		my = modTop + row * ROW_DY;
				if (nrows * ROW_DY - (ROW_DY - BOX_H) < LINDA_H)		my += (LINDA_H - (nrows * ROW_DY - (ROW_DY - BOX_H))) / 2;
				bounds.put (new Block (ArchModel.MODULE, mods.get (m), r), new Rectangle (rcx + col * COL_DX - BOX_W / 2, my, BOX_W, BOX_H));
			}
			if (model.hasLocalLinda (r))
				bounds.put (new Block (ArchModel.LOCAL_LINDA, "LLIN", r), new Rectangle (rcx - LINDA_W / 2, modTop + (modsH - LINDA_H) / 2, LINDA_W, LINDA_H));
			int		vy = modTop + modsH + 40;
			if (model.hasVRobot (r))
			{
				bounds.put (new Block (ArchModel.VROBOT, model.vrobotPrefix (r), r), new Rectangle (rcx - VROB_W / 2, vy, VROB_W, VROB_H));
				vy += VROB_H;
			}
			Rectangle	region = new Rectangle (rcx - REGION_HW, top, 2 * REGION_HW, vy + REGION_PAD - top);
			Block		robot = new Block (ArchModel.ROBOT, null, r);
			bounds.put (robot, region);
			// bounds of the robot name (top-left corner of the region), for hit testing and in-place editing
			FontMetrics	fm = getFontMetrics (getFont ().deriveFont (Font.BOLD, 12f));
			String		name = model.labelOf (robot);
			int			lx = region.x + 14, ly = region.y + BOX_H / 2 + 22;
			robotLabels.put (r, new Rectangle (lx - 4, ly - fm.getAscent () - 2, fm.stringWidth (name) + 8, fm.getHeight () + 4));
			bottom = Math.max (bottom, region.y + region.height);
		}
		layoutSize	= new Dimension (2 * MARGIN + total, bottom + MARGIN);
		setPreferredSize (layoutSize);
		// centring offset (the diagram is centred when the panel is larger than it)
		offx	= Math.max (0, (getWidth () - layoutSize.width) / 2);
		offy	= Math.max (0, (getHeight () - layoutSize.height) / 2);
	}

	/* ------------------------------------------------------------------ */
	/* Painting                                                            */
	/* ------------------------------------------------------------------ */

	protected void paintComponent (Graphics g0)
	{
		super.paintComponent (g0);
		layoutBlocks ();
		Graphics2D	g = (Graphics2D) g0.create ();
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.translate (offx, offy);

		if (bounds.isEmpty ())
		{
			g.setColor (Color.gray);
			String	msg = "Empty architecture: add a Linda space or a robot with the toolbar";
			FontMetrics	fm = g.getFontMetrics ();
			g.drawString (msg, (getWidth () - fm.stringWidth (msg)) / 2 - offx, getHeight () / 2 - offy);
			g.dispose ();
			return;
		}

		// --- robot regions
		for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
		{
			Block	robot = e.getKey ();
			if (robot.kind != ArchModel.ROBOT)		continue;
			Rectangle	region = e.getValue ();
			boolean		sel = robot.equals (selection);
			g.setColor (C_REGION_BG);
			g.fillRoundRect (region.x, region.y, region.width, region.height, 18, 18);
			Stroke	old = g.getStroke ();
			g.setStroke (new BasicStroke (sel ? 2.4f : 1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0f, new float[] { 7f, 5f }, 0f));
			g.setColor (sel ? C_SELECT : C_REGION);
			g.drawRoundRect (region.x, region.y, region.width, region.height, 18, 18);
			g.setStroke (old);
			g.setFont (getFont ().deriveFont (Font.BOLD, 12f));
			if (!robot.equals (editing))		g.drawString (model.labelOf (robot), region.x + 14, region.y + BOX_H / 2 + 22);
		}

		// --- arrows (below the blocks)
		Rectangle	glinda = bounds.get (new Block (ArchModel.GLOBAL_LINDA, "GLIN", -1));
		g.setColor (C_ARROW);
		g.setStroke (new BasicStroke (1.5f));
		for (int r : model.robots ())
		{
			Rectangle	llinda = bounds.get (new Block (ArchModel.LOCAL_LINDA, "LLIN", r));
			Rectangle	router = model.hasRouter (r) ? bounds.get (new Block (ArchModel.ROUTER, model.routerPrefix (r), r)) : null;
			if ((router != null) && (glinda != null))		doubleArrow (g, router, glinda);
			if (llinda != null)
				for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
				{
					Block	b = e.getKey ();
					if ((b.robot == r) && ((b.kind == ArchModel.ROUTER) || (b.kind == ArchModel.MODULE) || (b.kind == ArchModel.VROBOT)))		doubleArrow (g, e.getValue (), llinda);
				}
		}

		// --- blocks
		for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
		{
			Block		b = e.getKey ();
			if (b.kind == ArchModel.ROBOT)		continue;
			paintBlock (g, b, e.getValue (), b.equals (selection));
		}
		g.dispose ();
	}

	protected void paintBlock (Graphics2D g, Block b, Rectangle r, boolean selected)
	{
		Color	border = selected ? C_SELECT : C_LINE;
		g.setStroke (new BasicStroke (selected ? 2.4f : 1.4f));
		switch (b.kind)
		{
		case ArchModel.GLOBAL_LINDA:
		case ArchModel.LOCAL_LINDA:
		{
			int		eh = 16;										// ellipse height
			g.setColor (C_LINDA);
			g.fillRect (r.x, r.y + eh / 2, r.width, r.height - eh);
			g.fillOval (r.x, r.y + r.height - eh, r.width, eh);
			g.fillOval (r.x, r.y, r.width, eh);
			g.setColor (border);
			g.drawOval (r.x, r.y, r.width, eh);
			g.drawLine (r.x, r.y + eh / 2, r.x, r.y + r.height - eh / 2);
			g.drawLine (r.x + r.width, r.y + eh / 2, r.x + r.width, r.y + r.height - eh / 2);
			g.drawArc (r.x, r.y + r.height - eh, r.width, eh, 180, 180);
			String	label = model.labelOf (b);
			String[] lines = (b.kind == ArchModel.GLOBAL_LINDA) ? new String[] { "Global", "Linda Space" } : new String[] { "Local", "Linda Space" };
			if (!label.equals ("Global Linda Space") && !label.equals ("Local Linda Space"))		lines = new String[] { label };
			centeredText (g, lines, new Rectangle (r.x, r.y + eh, r.width, r.height - eh - eh / 2), Font.PLAIN);
			break;
		}
		case ArchModel.ROUTER:
			g.setColor (C_ROUTER);
			g.fillRect (r.x, r.y, r.width, r.height);
			g.setColor (border);
			g.drawRect (r.x, r.y, r.width, r.height);
			g.drawLine (r.x + 12, r.y, r.x + 12, r.y + r.height);
			g.drawLine (r.x + r.width - 12, r.y, r.x + r.width - 12, r.y + r.height);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, new Rectangle (r.x + 12, r.y, r.width - 24, r.height), Font.BOLD);
			break;
		case ArchModel.MODULE:
			g.setColor (C_MODULE);
			g.fillRect (r.x, r.y, r.width, r.height);
			g.setColor (border);
			g.drawRect (r.x, r.y, r.width, r.height);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, r, Font.BOLD);
			break;
		case ArchModel.VROBOT:
		{
			g.setColor (C_VROBOT);
			g.fillRoundRect (r.x, r.y, r.width, r.height, 22, 22);
			g.setColor (border);
			g.drawRoundRect (r.x, r.y, r.width, r.height, 22, 22);
			// wheels
			g.setColor (C_LINE);
			g.fillRoundRect (r.x + 16, r.y + r.height - 3, 22, 6, 3, 3);
			g.fillRoundRect (r.x + r.width - 38, r.y + r.height - 3, 22, 6, 3, 3);
			g.setColor (border);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, r, Font.BOLD);
			break;
		}
		}
	}

	protected void centeredText (Graphics2D g, String[] lines, Rectangle r, int style)
	{
		g.setFont (getFont ().deriveFont (style, 12f));
		FontMetrics	fm = g.getFontMetrics ();
		int			lh = fm.getHeight ();
		int			y = r.y + (r.height - lh * lines.length) / 2 + fm.getAscent ();
		for (String s : lines)
		{
			String	t = s;
			while ((fm.stringWidth (t) > r.width - 6) && (t.length () > 3))		t = t.substring (0, t.length () - 2).trim () + "…";
			g.drawString (t, r.x + (r.width - fm.stringWidth (t)) / 2, y);
			y += lh;
		}
	}

	/** Double-headed arrow between the borders of two rectangles, along the line joining their centres. */
	protected void doubleArrow (Graphics2D g, Rectangle a, Rectangle b)
	{
		Point2D	ca = new Point2D.Double (a.getCenterX (), a.getCenterY ());
		Point2D	cb = new Point2D.Double (b.getCenterX (), b.getCenterY ());
		Point2D	pa = exit (a, ca, cb);
		Point2D	pb = exit (b, cb, ca);
		if ((pa == null) || (pb == null))		return;
		g.draw (new Line2D.Double (pa, pb));
		arrowHead (g, pb, pa);
		arrowHead (g, pa, pb);
	}

	/** Point where the segment from the centre <code>c</code> of <code>r</code> towards <code>t</code> leaves the rectangle. */
	static protected Point2D exit (Rectangle r, Point2D c, Point2D t)
	{
		double	dx = t.getX () - c.getX (), dy = t.getY () - c.getY ();
		if ((dx == 0) && (dy == 0))		return null;
		double	tx = (dx != 0) ? (r.width / 2.0) / Math.abs (dx) : Double.MAX_VALUE;
		double	ty = (dy != 0) ? (r.height / 2.0) / Math.abs (dy) : Double.MAX_VALUE;
		double	k = Math.min (tx, ty);
		return new Point2D.Double (c.getX () + dx * k, c.getY () + dy * k);
	}

	/** Arrow head at <code>tip</code>, pointing away from <code>from</code>. */
	static protected void arrowHead (Graphics2D g, Point2D tip, Point2D from)
	{
		double	ang = Math.atan2 (tip.getY () - from.getY (), tip.getX () - from.getX ());
		double	len = 9, w = 0.45;
		int[]	xs = { (int) Math.round (tip.getX ()), (int) Math.round (tip.getX () - len * Math.cos (ang - w)), (int) Math.round (tip.getX () - len * Math.cos (ang + w)) };
		int[]	ys = { (int) Math.round (tip.getY ()), (int) Math.round (tip.getY () - len * Math.sin (ang - w)), (int) Math.round (tip.getY () - len * Math.sin (ang + w)) };
		g.fillPolygon (xs, ys, 3);
	}
}
