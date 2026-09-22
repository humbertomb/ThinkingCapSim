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

import tc.vrobot.RobotDef;
import tc.vrobot.RobotImage;

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
	static final int				COL_DX		= 205;						// module column offset from the centre, with nothing written under a module
	static final int				COL_GAP		= COL_DX - LINDA_W / 2 - BOX_W / 2;	// and the room the arrows to the Linda space take, whatever the blocks measure
	static final int				ROW_DY		= 66;						// module row pitch, with nothing written under a module
	static final int				SYM_TOP		= 4;						// from the foot of a module to the first symbol under it
	static final int				SYM_W		= 92;						// as wide as a symbol is written, at most (SENSORS_CTRL, the longest there is)
	static final int				SYM_BUS		= 12;						// and the room the line they travel by takes
	static final int				SYM_GAP		= 12;						// between what comes in and what goes out
	static final int				SYM_PAD		= 16;						// what a block is wider than the symbols written under it
	static final int				SYM_MAX_ROWS	= 9;					// and as tall as a lot of them is written before it goes on in another column
	static final float				SYM_FONT	= 10f;
	static final int				REGION_HW	= 290;						// robot region half width
	static final int				REGION_PAD	= 22;
	static final int				LABEL_DY	= 20;						// baseline of the robot name below the region top
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
	static final Color				C_PREVIEW_BG	= new Color (252, 252, 252);
	static final Color				C_SYMBOL	= new Color (70, 95, 70);	// the symbols a module asks for
	static final Color				C_PRODUCED	= new Color (70, 85, 120);	// and the ones it writes

	/** Size of the preview of the robot, hanging off the bottom right corner of its block (px). */
	static final int				PREVIEW_W	= 88;
	static final int				PREVIEW_H	= 88;
	static final int				PREVIEW_OVER	= 16;					// how much of the block it covers
	static final int				TH_W		= 18,	TH_H		= 22;		// the mark of a block that runs on a thread of its own
	static final int				TH_GAP		= 5;						// from the block it is the mark of

	protected ArchModel				model;
	protected Block					selection;
	/** Whether the symbols of the blocks are written at all: with many of them the diagram is a thicket. */
	protected boolean				showsymbols	= true;
	/** Descriptions read for the previews, by path (a null value: one that cannot be read). */
	protected Map<String, RobotDef>	previews = new LinkedHashMap<String, RobotDef> ();
	protected List<Listener>		listeners	= new ArrayList<Listener> ();

	// measures of the layout, taken at every paint: the blocks are as wide as what
	// is written under them, and the diagram opens up to hold them
	protected int					modw		= BOX_W;					// every module box, so that a row of them reads as a row
	protected int					coldx		= COL_DX;					// how far from the centre their columns sit
	protected int					regionhw	= REGION_HW;				// and half the width of the region of a robot

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
		measure ();
		List<Integer>	robots = model.robots ();
		int				n = Math.max (1, robots.size ());
		int				total = n * 2 * regionhw + (n - 1) * REGION_GAP;		// width of the row of robots
		int				cx = MARGIN + total / 2;
		int				y = MARGIN;

		if (model.hasGlobalLinda ())
		{
			bounds.put (new Block (ArchModel.GLOBAL_LINDA, -1), new Rectangle (cx - LINDA_W / 2, y, LINDA_W, LINDA_H));
			y += LINDA_H + 52;
		}

		int		bottom = y;
		for (int i = 0; i < robots.size (); i++)
		{
			int		r = robots.get (i);
			int		rcx = MARGIN + regionhw + i * (2 * regionhw + REGION_GAP);
			int		top = y + BOX_H / 2;								// region top: the router straddles it
			if (model.hasRouter (r))
			{
				int		rw = routerWidth (r);

				bounds.put (new Block (ArchModel.ROUTER, r), new Rectangle (rcx - rw / 2, y, rw, BOX_H));
			}
			// the router writes its symbols under its block, as a module does: what
			// comes after it starts below them
			int		modTop = top + BOX_H / 2 + 34 + (model.hasRouter (r) ? symbolsHeight (symbolRows (new Block (ArchModel.ROUTER, r))) : 0);
			int		nmods = model.moduleCount (r);
			int		nrows = (nmods + 1) / 2;
			int		gap = ROW_DY - BOX_H;								// between one row and the next
			// a row is as tall as its modules plus the longer of the two columns of
			// symbols written under them, so that what is written never reaches the
			// row below
			int[]	rowH = new int[Math.max (1, nrows)];
			int		stackH = 0;
			for (int row = 0; row < nrows; row++)
			{
				int		lines = 0;
				for (int m = 2 * row; (m < nmods) && (m < 2 * row + 2); m++)
					lines	= Math.max (lines, symbolRows (new Block (ArchModel.MODULE, r, m)));
				rowH[row]	= BOX_H + symbolsHeight (lines);
				stackH		+= rowH[row] + ((row > 0) ? gap : 0);
			}
			int		modsH = Math.max (stackH, LINDA_H);
			int		my = modTop + Math.max (0, (LINDA_H - stackH) / 2);
			for (int m = 0; m < nmods; m++)
			{
				int		col = (m % 2 == 0) ? -1 : 1;
				int		row = m / 2;
				int		ry = my;
				for (int k = 0; k < row; k++)		ry += rowH[k] + gap;
				bounds.put (new Block (ArchModel.MODULE, r, m), new Rectangle (rcx + col * coldx - modw / 2, ry, modw, BOX_H));
			}
			if (model.hasLocalLinda (r))
				bounds.put (new Block (ArchModel.LOCAL_LINDA, r), new Rectangle (rcx - LINDA_W / 2, modTop + (modsH - LINDA_H) / 2, LINDA_W, LINDA_H));
			int		vy = modTop + modsH + 40;
			Rectangle	vrobot = null;
			if (model.hasVRobot (r))
			{
				int		vw = vrobotWidth (r);

				vrobot	= new Rectangle (rcx - vw / 2, vy, vw, VROB_H);
				bounds.put (new Block (ArchModel.VROBOT, r), vrobot);
				vy += VROB_H + symbolsHeight (symbolRows (new Block (ArchModel.VROBOT, r)));
			}
			Rectangle	region = new Rectangle (rcx - regionhw, top, 2 * regionhw, vy + REGION_PAD - top);
			Block		robot = new Block (ArchModel.ROBOT, r);
			bounds.put (robot, region);
			// bounds of the robot name (top-left corner of the region), for hit testing and in-place editing
			FontMetrics	fm = getFontMetrics (getFont ().deriveFont (Font.BOLD, 12f));
			String		name = model.labelOf (robot);
			int			lx = region.x + 14, ly = region.y + LABEL_DY;
			robotLabels.put (r, new Rectangle (lx - 4, ly - fm.getAscent () - 2, fm.stringWidth (name) + 8, fm.getHeight () + 4));
			bottom = Math.max (bottom, region.y + region.height);
			// the preview of the robot hangs off its block: leave room for it, so that
			// it is not cut off when the panel is at its smallest
			if ((vrobot != null) && (describedRobot (r) != null))
				bottom = Math.max (bottom, previewBox (vrobot).y + PREVIEW_H + 6);
		}
		layoutSize	= new Dimension (2 * MARGIN + total, bottom + MARGIN);
		setPreferredSize (layoutSize);
		// centring offset (the diagram is centred when the panel is larger than it)
		offx	= Math.max (0, (getWidth () - layoutSize.width) / 2);
		offy	= Math.max (0, (getHeight () - layoutSize.height) / 2);
	}

	/**
	 * Takes the measures the layout is built on: a block is as wide as the two
	 * columns written under it, so that what it is given and what it writes stay
	 * under the block instead of reaching out of one side of it, and the region of
	 * a robot opens up to hold its blocks. The modules are all of one width,
	 * because a row of them that was not would not read as a row.
	 */
	protected void measure ()
	{
		int		mw = BOX_W, rw = BOX_W;

		for (int r : model.robots ())
		{
			for (int m = 0; m < model.moduleCount (r); m++)
				mw	= Math.max (mw, blockWidth (new Block (ArchModel.MODULE, r, m), BOX_W, SYM_PAD));
			if (model.hasRouter (r))		rw = Math.max (rw, routerWidth (r));
		}
		modw		= mw;
		// the arrows to the Linda space keep the room they had, whatever the blocks measure
		coldx		= LINDA_W / 2 + COL_GAP + modw / 2;
		regionhw	= Math.max (REGION_HW, Math.max (coldx + modw / 2 + REGION_PAD, rw / 2 + REGION_PAD));
	}

	/** How wide a block is drawn: as wide as the two columns written under it, and never narrower than its own size. */
	protected int blockWidth (Block b, int least, int pad)
	{
		int		wi = width (b, symbolsOf (b), false);
		int		wo = width (b, producedBy (b), true);
		int		total = wi + wo + (((wi > 0) && (wo > 0)) ? SYM_GAP : 0);

		return (total == 0) ? least : Math.max (least, total + pad);
	}

	/** The router of a robot, which routes the whole traffic of it and so writes far more symbols than a module. */
	protected int routerWidth (int r)
	{
		return blockWidth (new Block (ArchModel.ROUTER, r), BOX_W, SYM_PAD);
	}

	/**
	 * The block of the robot of a region: wide enough besides for the drawing of
	 * the robot, which hangs off its corner and which the columns step aside from.
	 */
	protected int vrobotWidth (int r)
	{
		int		pad = (describedRobot (r) != null) ? 2 * (PREVIEW_OVER + 6) : SYM_PAD;

		return Math.min (2 * regionhw - 2 * REGION_PAD, blockWidth (new Block (ArchModel.VROBOT, r), VROB_W, pad));
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
			if (!robot.equals (editing))		g.drawString (model.labelOf (robot), region.x + 14, region.y + LABEL_DY);
		}

		// --- arrows (below the blocks)
		Rectangle	glinda = bounds.get (new Block (ArchModel.GLOBAL_LINDA, -1));
		g.setColor (C_ARROW);
		g.setStroke (new BasicStroke (1.5f));
		for (int r : model.robots ())
		{
			Rectangle	llinda = bounds.get (new Block (ArchModel.LOCAL_LINDA, r));
			Rectangle	router = model.hasRouter (r) ? bounds.get (new Block (ArchModel.ROUTER, r)) : null;
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
		paintRobotPreviews (g);
		g.dispose ();
	}

	/**
	 * Every robot the diagram holds, hanging off the bottom right corner of its
	 * block: what a description says of a robot is part of the drawing of the
	 * architecture and not something to be gone looking for.
	 */
	protected void paintRobotPreviews (Graphics2D g)
	{
		for (Map.Entry<Block, Rectangle> e : bounds.entrySet ())
			if (e.getKey ().kind == ArchModel.VROBOT)
				paintRobotPreview (g, describedRobot (e.getKey ().robot), e.getValue ());
	}

	/**
	 * One of them: its image when its description carries one and, failing that,
	 * the drawing its outline is made of.
	 */
	protected void paintRobotPreview (Graphics2D g, RobotDef robot, Rectangle block)
	{
		Rectangle	box;
		String		name;

		if ((robot == null) || (block == null))		return;
		box		= previewBox (block);

		g.setColor (C_PREVIEW_BG);
		g.fillRoundRect (box.x, box.y, box.width, box.height, 10, 10);
		g.setColor (C_REGION);
		g.drawRoundRect (box.x, box.y, box.width, box.height, 10, 10);

		name	= ((robot.name != null) && (robot.name.trim ().length () > 0)) ? robot.name.trim () : "";
		g.setFont (getFont ().deriveFont (Font.PLAIN, 10f));
		if (name.length () > 0)
		{
			FontMetrics	fm = g.getFontMetrics ();
			g.setColor (C_ARROW);
			g.drawString (name, box.x + (box.width - fm.stringWidth (name)) / 2, box.y + box.height - 5);
		}
		drawRobot (g, robot, new Rectangle (box.x + 7, box.y + 7, box.width - 14, box.height - 14 - 11));
	}

	/** Where the preview of a robot goes: off the bottom right corner of its block, covering a little of it. */
	static protected Rectangle previewBox (Rectangle block)
	{
		return new Rectangle (block.x + block.width - PREVIEW_OVER, block.y + block.height - PREVIEW_OVER,
							  PREVIEW_W, PREVIEW_H);
	}

	/** The image of a robot, or the drawing of its outline when it has no image, fitted into a box. */
	protected void drawRobot (Graphics2D g, RobotDef robot, Rectangle box)
	{
		double			minx = Double.MAX_VALUE, miny = Double.MAX_VALUE;
		double			maxx = -Double.MAX_VALUE, maxy = -Double.MAX_VALUE;
		double			k;
		double			cx, cy;
		java.awt.Image	img = (robot.image != null) ? RobotImage.get (robot.image) : null;
		int				iw, ih;

		if (img != null)											// the image comes first
		{
			iw	= img.getWidth (null);			ih = img.getHeight (null);
			if ((iw > 0) && (ih > 0))
			{
				k	= Math.min (box.width / (double) iw, box.height / (double) ih);
				iw	= (int) Math.round (iw * k);	ih = (int) Math.round (ih * k);
				g.drawImage (img, (int) Math.round (box.getCenterX () - iw / 2.0),
								  (int) Math.round (box.getCenterY () - ih / 2.0), iw, ih, null);
				return;
			}
		}

		if ((robot.icon != null) && !robot.icon.isEmpty ())
		{
			for (RobotDef.IconLine l : robot.icon)
			{
				minx	= Math.min (minx, Math.min (l.xi, l.xf));	maxx = Math.max (maxx, Math.max (l.xi, l.xf));
				miny	= Math.min (miny, Math.min (l.yi, l.yf));	maxy = Math.max (maxy, Math.max (l.yi, l.yf));
			}
			if ((maxx <= minx) && (maxy <= miny))					return;
			k		= Math.min (box.width / Math.max (1e-6, maxx - minx), box.height / Math.max (1e-6, maxy - miny));
			cx		= (minx + maxx) / 2;			cy = (miny + maxy) / 2;
			g.setColor (C_LINE);
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (RobotDef.IconLine l : robot.icon)
				g.draw (new Line2D.Double (box.getCenterX () + (l.xi - cx) * k, box.getCenterY () - (l.yi - cy) * k,
										   box.getCenterX () + (l.xf - cx) * k, box.getCenterY () - (l.yf - cy) * k));
			return;
		}
	}

	/**
	 * The description the robot of a region names, or null when there is none.
	 *
	 * Reading a description is slow and the panel is repainted at every turn, so
	 * what has been read is kept, by path.
	 */
	protected RobotDef describedRobot (int r)
	{
		Block		b = new Block (ArchModel.VROBOT, r);
		String		path;

		if (!model.exists (b))					return null;
		path	= model.get (b, "DESC");
		if (path == null)						return null;
		path	= path.trim ();
		if (path.length () == 0)				return null;
		if (previews.containsKey (path))		return previews.get (path);

		previews.put (path, null);											// what cannot be read is not read again
		try
		{
			java.io.File	f = new java.io.File (path);
			if (f.isFile ())		previews.put (path, RobotDef.load (f));
		} catch (Throwable e)		{ }									// a description that cannot be read shows nothing
		return previews.get (path);
	}

	/** Forgets the descriptions read for the previews (one of them having been edited, say). */
	public void flushPreviews ()				{ previews.clear (); repaint (); }

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
			paintThread (g, b, r, selected);
			g.setColor (C_ROUTER);
			g.fillRect (r.x, r.y, r.width, r.height);
			g.setColor (border);
			g.drawRect (r.x, r.y, r.width, r.height);
			g.drawLine (r.x + 12, r.y, r.x + 12, r.y + r.height);
			g.drawLine (r.x + r.width - 12, r.y, r.x + r.width - 12, r.y + r.height);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, new Rectangle (r.x + 12, r.y, r.width - 24, r.height), Font.BOLD);
			paintSymbols (g, b, r);
			break;
		case ArchModel.MODULE:
			paintThread (g, b, r, selected);
			g.setColor (C_MODULE);
			g.fillRect (r.x, r.y, r.width, r.height);
			g.setColor (border);
			g.drawRect (r.x, r.y, r.width, r.height);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, r, Font.BOLD);
			paintSymbols (g, b, r);
			break;
		case ArchModel.VROBOT:
		{
			paintThread (g, b, r, selected);
			g.setColor (C_VROBOT);
			g.fillRoundRect (r.x, r.y, r.width, r.height, 22, 22);
			g.setColor (border);
			g.drawRoundRect (r.x, r.y, r.width, r.height, 22, 22);
			if (!b.equals (editing))	centeredText (g, new String[] { model.labelOf (b) }, r, Font.BOLD);
			paintSymbols (g, b, r);
			break;
		}
		}
	}

	/**
	 * Whether a block runs on a thread of its own: it does unless it is passive
	 * (a module that only reacts to what it is notified of). A module that polls
	 * the Linda space is never passive, and a robot, simulated or physical, runs
	 * on its own cycle.
	 */
	protected boolean ownThread (Block b)
	{
		switch (b.kind)
		{
		case ArchModel.MODULE:
			if (Boolean.parseBoolean (model.get (b, "POLLED")))		return true;
			return !Boolean.parseBoolean (model.get (b, "PASSIVE"));
		case ArchModel.ROUTER:
		case ArchModel.VROBOT:
			return true;
		default:
			return false;
		}
	}

	/**
	 * The mark of a block that runs on a thread of its own, on the left of it: a
	 * white box with the cycle it runs on (a loop with an arrow). A passive
	 * block, which runs on the thread of whoever notifies it, has none.
	 */
	protected void paintThread (Graphics2D g, Block b, Rectangle r, boolean selected)
	{
		int			x, y;
		Stroke		old = g.getStroke ();

		if (!ownThread (b))		return;
		x	= r.x - TH_W - TH_GAP;
		y	= (int) Math.round (r.getCenterY () - TH_H / 2.0);

		g.setColor (Color.WHITE);
		g.fillRoundRect (x, y, TH_W, TH_H, 6, 6);
		g.setColor (selected ? C_SELECT : C_LINE);
		g.setStroke (new BasicStroke (1.2f));
		g.drawRoundRect (x, y, TH_W, TH_H, 6, 6);

		// the cycle it runs on: a loop open at its top right, with an arrow at the open end
		int		lx = x + 4, ly = y + 5, lw = TH_W - 9, lh = TH_H - 11;
		g.setStroke (new BasicStroke (1.6f));
		g.drawArc (lx, ly, lw, lh, 40, 285);
		int		ax = lx + lw, ay = ly + lh / 2 - 1;
		g.drawLine (ax, ay, ax - 4, ay - 3);
		g.drawLine (ax, ay, ax - 5, ay + 2);

		g.setStroke (old);
	}

	/**
	 * The symbols a module asks the Linda space for, written in a column under its
	 * block: what a module reacts to is as much part of the drawing of the
	 * architecture as what it is wired to.
	 *
	 * They are written on the ground of the region, so that an arrow passing by
	 * reads as passing behind them, and the row they sit in was made tall enough
	 * for them, so they never reach the module below.
	 */
	protected void paintSymbols (Graphics2D g, Block b, Rectangle r)
	{
		paintColumn (g, b, r, false);
		paintColumn (g, b, r, true);
	}

	/**
	 * One of the two lots, on the line it travels by: what comes in with the arrow
	 * head against the block, what goes out with it at the far end, so which way
	 * a symbol goes is read off the picture and not off the names.
	 */
	protected void paintColumn (Graphics2D g, Block b, Rectangle r, boolean produced)
	{
		List<String>	syms = produced ? producedBy (b) : symbolsOf (b);
		List<String>	std = produced ? new java.util.ArrayList<String> () : standardOf (b);
		Rectangle		box = columnBox (b, r, produced);
		FontMetrics		fm;
		int				cols, cw, stem;
		int				top, busy, foot;

		if (syms.isEmpty () || (box == null))		return;
		g.setColor (C_REGION_BG);
		g.fillRect (box.x - 3, box.y, box.width + 6, box.height);
		g.setFont (getFont ().deriveFont (Font.PLAIN, SYM_FONT));
		fm		= g.getFontMetrics ();
		g.setColor (produced ? C_PRODUCED : C_SYMBOL);
		g.setStroke (new BasicStroke (1f));

		cols	= columns (syms.size ());
		cw		= box.width / cols;
		top		= r.y + r.height;
		busy	= box.y - 2;											// where the columns hang from
		// the lot leaves the block at the middle of what is written, and always
		// against the block itself however far aside it had to be written
		stem	= (cols == 1) ? box.x + 4 : Math.max (r.x + 8, Math.min (r.x + r.width - 8, box.x + box.width / 2));

		if (cols > 1)		g.drawLine (box.x + 4, busy, box.x + (cols - 1) * cw + 4, busy);
		if (produced)		g.drawLine (stem, top, stem, busy);
		else
		{
			g.drawLine (stem, top + 3, stem, busy);
			g.fillPolygon (new int[] { stem, stem - 3, stem + 3 },
						   new int[] { top, top + 5, top + 5 }, 3);
		}

		for (int c = 0; c < cols; c++)
		{
			List<String>	part = column (syms, c);
			int				cx = box.x + c * cw;
			int				bx = cx + 4;

			if (part.isEmpty ())		continue;
			foot	= box.y + (part.size () - 1) * fm.getHeight () + fm.getHeight () / 2;
			if (produced)
			{
				g.drawLine (bx, busy, bx, foot + 5);
				g.fillPolygon (new int[] { bx, bx - 3, bx + 3 },
							   new int[] { foot + 10, foot + 5, foot + 5 }, 3);
			}
			else	g.drawLine (bx, busy, bx, foot);

			for (int i = 0; i < part.size (); i++)
			{
				// what the runtime hands out anyway is written in bold: it is asked for
				// nowhere and cannot be taken away, and it is no less given for that
				boolean		bold = std.contains (part.get (i));
				FontMetrics	cfm = symbolMetrics (bold);
				String		t = shortened (cfm, part.get (i));
				int			my = box.y + i * fm.getHeight () + fm.getHeight () / 2;

				g.setFont (getFont ().deriveFont (bold ? Font.BOLD : Font.PLAIN, SYM_FONT));
				g.drawLine (bx, my, cx + SYM_BUS - 2, my);
				g.drawString (t, cx + SYM_BUS, box.y + fm.getAscent () + i * fm.getHeight ());
			}
		}
	}

	/**
	 * The symbols a block asks for, each one once and in the order it asks for
	 * them: the modules and the virtual robot, which is given what it is to do
	 * the same way they are given what to work on.
	 */
	protected List<String> symbolsOf (Block b)
	{
		return (showsymbols && (model != null) && (b != null)) ? model.inputs (b) : new java.util.ArrayList<String> ();
	}

	/** The symbols a block writes, as the model reads them off the class it runs. */
	protected List<String> producedBy (Block b)
	{
		return (showsymbols && (model != null) && (b != null)) ? model.produces (b) : new java.util.ArrayList<String> ();
	}

	/** Whether the symbols of the blocks are written under them. */
	public boolean getShowSymbols ()			{ return showsymbols; }

	/**
	 * Writes the symbols of the blocks, or leaves them out: which symbol goes
	 * where is one thing to read an architecture for, and how it is wired together
	 * is another, and the second is read better without the first.
	 */
	public void setShowSymbols (boolean on)
	{
		if (on == showsymbols)		return;
		showsymbols	= on;
		modelChanged ();										// what is written says how much room a row takes
	}

	/**
	 * How many columns a lot of symbols is written in: one, until it would be
	 * taller than what a block leaves room for -- a router, which routes the whole
	 * traffic of a robot, names many more symbols than a module does -- and as
	 * many as it takes then.
	 */
	static protected int columns (int n)
	{
		return (n <= 0) ? 0 : (n + SYM_MAX_ROWS - 1) / SYM_MAX_ROWS;
	}

	/** How many rows the tallest of those columns has. */
	static protected int rows (int n)
	{
		int		c = columns (n);

		return (c == 0) ? 0 : (n + c - 1) / c;
	}

	/** Which symbols of a lot the column <code>c</code> of it holds. */
	static protected List<String> column (List<String> syms, int c)
	{
		int		rows = rows (syms.size ());

		return syms.subList (Math.min (c * rows, syms.size ()), Math.min ((c + 1) * rows, syms.size ()));
	}

	/** How many rows the two lots of a block take: the taller of them. */
	protected int symbolRows (Block b)
	{
		return Math.max (rows (symbolsOf (b).size ()), rows (producedBy (b).size ()));
	}

	/** How much room a column of that many symbols takes under a module (none for none). */
	protected int symbolsHeight (int lines)
	{
		if (lines <= 0)		return 0;
		return SYM_TOP + lines * getFontMetrics (getFont ().deriveFont (Font.PLAIN, SYM_FONT)).getHeight ();
	}

	/** Where every block of the diagram sits, the regions of the robots included. */
	public Map<Block, Rectangle> blockBounds ()		{ return bounds; }

	/** Where the symbols of a module are written, or null when it asks for none. */
	public Rectangle symbolsBox (Block b)			{ return symbolsBox (b, bounds.get (b)); }

	protected Rectangle symbolsBox (Block b, Rectangle r)
	{
		Rectangle	in = columnBox (b, r, false), out = columnBox (b, r, true);

		if (in == null)					return out;
		if (out == null)				return in;
		return in.union (out);
	}

	/**
	 * Where one of the two lots of a block is written: what it is given on the
	 * left, what it writes on the right, the pair of them under the middle of the
	 * block.
	 *
	 * The pair steps aside from the drawing of a robot, which hangs off the corner
	 * of its block, and stays inside the region of its robot, which is what it
	 * would run out of first.
	 */
	protected Rectangle columnBox (Block b, Rectangle r, boolean produced)
	{
		List<String>	syms = produced ? producedBy (b) : symbolsOf (b);
		FontMetrics		fm;
		int				wi, wo, x;

		if ((r == null) || syms.isEmpty ())			return null;
		fm		= symbolMetrics (false);
		wi		= width (b, symbolsOf (b), false);
		wo		= width (b, producedBy (b), true);
		x		= r.x + (r.width - (wi + wo + (((wi > 0) && (wo > 0)) ? SYM_GAP : 0))) / 2;
		if ((b.kind == ArchModel.VROBOT) && (describedRobot (b.robot) != null))
			x	= Math.min (x, previewBox (r).x - 6 - (wi + wo + SYM_GAP));
		// inside the region of its robot, whichever way it has to move to be
		Rectangle	region = bounds.get (new Block (ArchModel.ROBOT, b.robot));
		if (region != null)
		{
			x	= Math.min (x, region.x + region.width - 8 - (wi + wo + (((wi > 0) && (wo > 0)) ? SYM_GAP : 0)));
			x	= Math.max (x, region.x + 8);
		}
		if (produced)		x += wi + ((wi > 0) ? SYM_GAP : 0);
		return new Rectangle (x, r.y + r.height + SYM_TOP, produced ? wo : wi, rows (syms.size ()) * fm.getHeight ());
	}

	/** The metrics a symbol is written with: the standard events, which are written in bold, are wider. */
	protected FontMetrics symbolMetrics (boolean bold)
	{
		return getFontMetrics (getFont ().deriveFont (bold ? Font.BOLD : Font.PLAIN, SYM_FONT));
	}

	/**
	 * The symbols of a block that are no one's to edit, which are written in bold:
	 * what every thread of the runtime is given and what the code of the block
	 * itself takes out of the space. A router registers for CONFIG and EXECUTION
	 * of its own accord, and its own are written like the rest of what it asks
	 * for.
	 */
	protected List<String> standardOf (Block b)
	{
		return ((model != null) && (b != null)) ? model.fixed (b) : new java.util.ArrayList<String> ();
	}

	/** How wide a lot of symbols is written, the lines they travel by and the columns they take included (0 for none). */
	protected int width (Block b, List<String> syms, boolean produced)
	{
		List<String>	std = produced ? new java.util.ArrayList<String> () : standardOf (b);
		int				w = 0;

		if (syms.isEmpty ())		return 0;
		for (String sym : syms)
		{
			FontMetrics	fm = symbolMetrics (std.contains (sym));
			w	= Math.max (w, fm.stringWidth (shortened (fm, sym)));
		}
		return columns (syms.size ()) * (w + SYM_BUS);
	}

	/** A symbol cut to what a column under a module holds. */
	static protected String shortened (FontMetrics fm, String sym)
	{
		String	t = sym;

		while ((fm.stringWidth (t) > SYM_W) && (t.length () > 3))		t = t.substring (0, t.length () - 2).trim () + "\u2026";
		return t;
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
