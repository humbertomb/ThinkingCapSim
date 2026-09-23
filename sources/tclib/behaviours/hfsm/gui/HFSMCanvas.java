/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import tclib.behaviours.hfsm.MetaState;
import tclib.behaviours.hfsm.State;
import tclib.behaviours.hfsm.Transition;

/**
 * The diagram of a machine of states: the states of one level as circles -- a
 * lighter grey for a plain state and a darker one for a meta state -- and its
 * transitions as boxes in light cyan, joined by arrows from where they leave to
 * where they arrive.
 *
 * One level is shown at a time: going into a meta state (double click) shows
 * what it holds, and going up shows the level that holds it.
 */
public class HFSMCanvas extends JPanel
{
	private static final long		serialVersionUID = 1L;

	/* Tools */
	static public final int			T_SELECT	= 0;
	static public final int			T_PAN		= 1;
	static public final int			T_STATE		= 2;
	static public final int			T_META		= 3;
	static public final int			T_TRANS		= 4;
	static public final int			T_LINK		= 5;
	static public final int			NTOOLS		= 6;

	/* How the blocks are drawn (in the units of the file, which are pixels of the editor) */
	static public final int			RADIUS		= 34;		// a state
	static public final int			META_RADIUS	= 42;		// a meta state, which holds more
	static public final int			BOX_W		= 116;		// a transition
	static public final int			BOX_H		= 30;

	/* Colours */
	static private final Color		C_BACK		= new Color (252, 252, 252);
	static private final Color		C_GRID		= new Color (238, 238, 238);
	static private final Color		C_STATE		= new Color (216, 216, 216);
	static private final Color		C_META		= new Color (168, 168, 168);
	static private final Color		C_TRANS		= new Color (180, 240, 240);
	static private final Color		C_EDGE		= new Color (80, 80, 80);
	static private final Color		C_ARROW		= new Color (110, 110, 110);
	static private final Color		C_TEXT		= new Color (20, 20, 20);
	static private final Color		C_SEL		= new Color (255, 140, 0);
	static private final Color		C_RUBBER	= new Color (0, 120, 215);
	static private final Color		C_LEVEL		= new Color (120, 120, 120);
	static private final Color		C_LOST		= new Color (200, 60, 60);		// a transition that arrives nowhere
	static private final Color		C_LIVE		= new Color (230, 60, 60);		// where the machine is now
	static private final Color		C_LIVEF		= new Color (255, 205, 205);	// and what it is filled with
	static private final Color		C_LIVEM		= new Color (240, 170, 170);	// a meta state it is inside of

	/** What the editor around the canvas is told. */
	public interface Listener
	{
		/** A state, a meta state, a transition or nothing at all was selected. */
		public void selectionChanged (Object selection);
		/** The level being shown changed. */
		public void levelChanged (MetaState level);
		/** The machine was changed; <code>what</code> says how, in a few words. */
		public void machineChanged (String what);
		public void statusChanged (String text);
		public void usageChanged (String text);
		/** A tool finished what it was for (back to Select). */
		public void toolFinished ();
	}

	/* Model */
	protected MetaState				root;
	protected MetaState				level;						// the machine being shown
	protected Object				selection;					// a State or a Transition

	/* Watching a machine run: where it is, and nothing of it can be changed */
	protected List<State>			live = new ArrayList<State> ();
	protected boolean				watch;

	/* View */
	protected double				scale		= 1.0;
	protected double				cx, cy;						// what is at the centre of the view
	protected int					tool		= T_SELECT;
	protected Listener				listener;

	/* Dragging */
	protected int					dragMode;					// 0 none, 1 move, 2 pan, 3 rubber (join)
	protected double				anchorX, anchorY;			// where the drag started
	protected double				curX, curY;					// where the cursor is
	protected Object				dragging;					// what is being moved or joined
	protected int					grabX, grabY;				// where inside the block it was grabbed

	public HFSMCanvas (MetaState root)
	{
		setBackground (C_BACK);
		setPreferredSize (new Dimension (700, 600));
		setFocusable (true);
		setMachine (root);
		hooks ();
	}

	/* ------------------------------------------------------------------ */
	/* Model and view                                                      */
	/* ------------------------------------------------------------------ */

	public void setMachine (MetaState root)
	{
		this.root		= root;
		this.level		= root;
		this.selection	= null;
		if (listener != null)		{ listener.levelChanged (level);	listener.selectionChanged (null); }
		zoomToFit ();
		repaint ();
	}

	public MetaState				getMachine ()			{ return root; }
	public MetaState				getLevel ()				{ return level; }
	public Object					getSelection ()			{ return selection; }
	public double					getScale ()				{ return scale; }
	public void						setListener (Listener l)	{ listener = l;	showUsage (); }

	/** Shows a level of the machine (a meta state of it). */
	public void setLevel (MetaState m)
	{
		if (m == null)					return;
		level		= m;
		selection	= null;
		if (listener != null)			{ listener.levelChanged (level);	listener.selectionChanged (null); }
		zoomToFit ();
		repaint ();
	}

	/** Goes out of the level being shown, into the one that holds it. */
	public void levelUp ()
	{
		MetaState		up = HFSMEdit.parent (root, level);

		if (up != null)					setLevel (up);
	}

	public boolean canGoUp ()			{ return HFSMEdit.parent (root, level) != null; }

	/** Where the level being shown is, as <code>root.meta.meta</code>. */
	public String levelPath ()
	{
		List<String>	names = new ArrayList<String> ();

		for (State s = level; s != null; s = HFSMEdit.parent (root, s))		names.add (0, s.getName ());

		StringBuffer	sb = new StringBuffer ();

		for (int i = 0; i < names.size (); i++)		sb.append ((i > 0) ? " . " : "").append (names.get (i));
		return sb.toString ();
	}

	public void setSelection (Object o)
	{
		selection	= o;
		if (listener != null)			listener.selectionChanged (o);
		repaint ();
	}

	/**
	 * Where a machine that is running is, as the state of every level (see
	 * {@link tclib.behaviours.hfsm.HFSM#active}): those states are drawn in red.
	 */
	public void setLive (List<State> states)
	{
		live.clear ();
		if (states != null)				live.addAll (states);
		repaint ();
	}

	public List<State> getLive ()		{ return new ArrayList<State> (live); }

	/**
	 * Whether the machine is only being watched: nothing of it can then be moved,
	 * renamed, added or deleted, and all that is left is looking, selecting, and
	 * going in and out of the levels.
	 */
	public void setWatching (boolean b)
	{
		watch	= b;
		if (watch)						setTool (T_SELECT);
		showUsage ();
	}

	public boolean isWatching ()		{ return watch; }

	public void setTool (int t)
	{
		tool	= (watch && (t != T_PAN)) ? T_SELECT : t;
		dragMode	= 0;
		dragging	= null;
		setCursor ((tool == T_PAN) ? Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR)
								   : (tool == T_SELECT) ? Cursor.getDefaultCursor ()
														: Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
		showUsage ();
		repaint ();
	}

	public int						getTool ()				{ return tool; }

	/* ---------------- zoom and pan ---------------- */

	public void zoom (double factor)
	{
		scale	= Math.max (0.2, Math.min (4.0, scale * factor));
		repaint ();
	}

	/** Puts the whole level in view. */
	public void zoomToFit ()
	{
		double		minx = Double.MAX_VALUE, miny = Double.MAX_VALUE;
		double		maxx = -Double.MAX_VALUE, maxy = -Double.MAX_VALUE;
		boolean		any = false;

		for (State s : level.getStatesList ())
		{
			int		r = radius (s);

			minx = Math.min (minx, s.getX () - r);		maxx = Math.max (maxx, s.getX () + r);
			miny = Math.min (miny, s.getY () - r);		maxy = Math.max (maxy, s.getY () + r);
			any	= true;
			for (Transition t : s.getTransitions ())
			{
				minx = Math.min (minx, t.getX ());	maxx = Math.max (maxx, t.getX () + BOX_W);
				miny = Math.min (miny, t.getY ());	maxy = Math.max (maxy, t.getY () + BOX_H);
			}
		}
		if (!any)
		{
			cx		= 0.0;
			cy		= 0.0;
			scale	= 1.0;
			repaint ();
			return;
		}

		cx		= (minx + maxx) / 2.0;
		cy		= (miny + maxy) / 2.0;

		int			w = Math.max (100, getWidth ()), h = Math.max (100, getHeight ());

		scale	= Math.max (0.2, Math.min (2.0, Math.min ((w - 60) / Math.max (1.0, maxx - minx), (h - 60) / Math.max (1.0, maxy - miny))));
		repaint ();
	}

	/* ---------------- coordinates ---------------- */

	protected double px (double x)			{ return (x - cx) * scale + getWidth () / 2.0; }
	protected double py (double y)			{ return (y - cy) * scale + getHeight () / 2.0; }
	protected double wx (double x)			{ return (x - getWidth () / 2.0) / scale + cx; }
	protected double wy (double y)			{ return (y - getHeight () / 2.0) / scale + cy; }

	/* ------------------------------------------------------------------ */
	/* The mouse and the keyboard                                          */
	/* ------------------------------------------------------------------ */

	private void hooks ()
	{
		addMouseListener (new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)		{ requestFocusInWindow ();	onPress (e); }
			public void mouseReleased (MouseEvent e)	{ onRelease (e); }
			public void mouseClicked (MouseEvent e)		{ onClick (e); }
		});
		addMouseMotionListener (new MouseMotionAdapter ()
		{
			public void mouseDragged (MouseEvent e)		{ onDrag (e); }
			public void mouseMoved (MouseEvent e)		{ onMove (e); }
		});
		addMouseWheelListener (new MouseWheelListener ()
		{
			public void mouseWheelMoved (MouseWheelEvent e)
			{
				double	f = (e.getWheelRotation () < 0) ? 1.1 : (1.0 / 1.1);
				double	mx = wx (e.getX ()), my = wy (e.getY ());

				zoom (f);
				cx	+= mx - wx (e.getX ());					// the point under the cursor stays there
				cy	+= my - wy (e.getY ());
				repaint ();
			}
		});
		addKeyListener (new KeyAdapter ()
		{
			public void keyPressed (KeyEvent e)			{ onKey (e); }
		});
	}

	private void onPress (MouseEvent e)
	{
		double		x = wx (e.getX ()), y = wy (e.getY ());

		anchorX	= x;
		anchorY	= y;
		curX	= x;
		curY	= y;

		if (SwingUtilities.isMiddleMouseButton (e) || (tool == T_PAN))
		{
			dragMode	= 2;
			return;
		}

		if (SwingUtilities.isRightMouseButton (e))
		{
			if (tool != T_SELECT)			{ setTool (T_SELECT);	if (listener != null) listener.toolFinished (); }
			return;
		}

		switch (tool)
		{
		case T_SELECT:
		{
			Object	hit = pick (x, y);

			setSelection (hit);
			if ((hit != null) && !watch)								// watching it, nothing is moved
			{
				dragMode	= 1;
				dragging	= hit;
				grabX		= (int) Math.round (x - blockX (hit));
				grabY		= (int) Math.round (y - blockY (hit));
			}
			break;
		}
		case T_STATE:
			setSelection (HFSMEdit.addState (root, level, (int) Math.round (x), (int) Math.round (y)));
			changed ("Add state");
			break;

		case T_META:
			setSelection (HFSMEdit.addMetaState (root, level, (int) Math.round (x), (int) Math.round (y)));
			changed ("Add meta state");
			break;

		case T_TRANS:
		case T_LINK:
		{
			Object	hit = pick (x, y);

			if (hit != null)
			{
				dragMode	= 3;
				dragging	= hit;
			}
			break;
		}
		}
		repaint ();
	}

	private void onDrag (MouseEvent e)
	{
		curX	= wx (e.getX ());
		curY	= wy (e.getY ());

		switch (dragMode)
		{
		case 1:																	// moving a block
			if (dragging != null)
			{
				move (dragging, (int) Math.round (curX - grabX), (int) Math.round (curY - grabY));
				status (dragging);
				repaint ();
			}
			break;

		case 2:																	// panning
			cx	-= curX - anchorX;
			cy	-= curY - anchorY;
			repaint ();
			break;

		case 3:																	// joining two blocks
			repaint ();
			break;
		}
	}

	private void onRelease (MouseEvent e)
	{
		if (dragMode == 1)			changed ("Move");
		if (dragMode == 3)			join (pick (wx (e.getX ()), wy (e.getY ())));

		dragMode	= 0;
		dragging	= null;
		repaint ();
	}

	private void onClick (MouseEvent e)
	{
		if ((e.getClickCount () != 2) || !SwingUtilities.isLeftMouseButton (e))		return;

		Object		hit = pick (wx (e.getX ()), wy (e.getY ()));

		if (hit instanceof MetaState)			setLevel ((MetaState) hit);			// into it
		else if (watch)							{ if ((hit == null) && canGoUp ())	levelUp (); }	// nothing is renamed
		else if (hit != null)					rename (hit);
		else if (canGoUp ())					levelUp ();
	}

	private void onMove (MouseEvent e)
	{
		status (pick (wx (e.getX ()), wy (e.getY ())));
	}

	private void onKey (KeyEvent e)
	{
		if (watch)												// only looking around
			switch (e.getKeyCode ())
			{
			case KeyEvent.VK_UP:	if (e.isAltDown ())		levelUp ();		return;
			case KeyEvent.VK_ESCAPE:						setSelection (null);
			default:										return;
			}

		switch (e.getKeyCode ())
		{
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:	deleteSelection ();		break;
		case KeyEvent.VK_ESCAPE:
			if (tool != T_SELECT)		{ setTool (T_SELECT);	if (listener != null) listener.toolFinished (); }
			else						setSelection (null);
			break;
		case KeyEvent.VK_F2:			rename (selection);		break;
		case KeyEvent.VK_UP:
			if (e.isAltDown ())			levelUp ();
			else						nudge (0, -(e.isShiftDown () ? 20 : 4));
			break;
		case KeyEvent.VK_DOWN:			nudge (0, (e.isShiftDown () ? 20 : 4));		break;
		case KeyEvent.VK_LEFT:			nudge (-(e.isShiftDown () ? 20 : 4), 0);	break;
		case KeyEvent.VK_RIGHT:			nudge ((e.isShiftDown () ? 20 : 4), 0);		break;
		case KeyEvent.VK_ENTER:
			if (selection instanceof MetaState)		setLevel ((MetaState) selection);
			break;
		}
	}

	/* ------------------------------------------------------------------ */
	/* What the tools do                                                   */
	/* ------------------------------------------------------------------ */

	/** Deletes what is selected, and with a state whatever joins it. */
	public void deleteSelection ()
	{
		if (selection instanceof Transition)
		{
			HFSMEdit.remove (root, (Transition) selection);
			setSelection (null);
			changed ("Delete transition");
		}
		else if (selection instanceof State)
		{
			if (selection == root)				return;
			HFSMEdit.remove (root, (State) selection);
			setSelection (null);
			changed ("Delete state");
		}
	}

	/** Asks for a new name for a state or a transition. */
	public void rename (Object o)
	{
		if (o == null)							return;

		String		old = (o instanceof State) ? ((State) o).getName () : ((Transition) o).getName ();
		String		name = javax.swing.JOptionPane.showInputDialog (this, "Name:", old);

		if ((name == null) || (name.trim ().length () == 0))			return;
		name	= name.trim ();
		if (o instanceof State)					((State) o).setName (name);
		else									((Transition) o).setName (name);
		changed ("Rename");
		if (listener != null)					listener.selectionChanged (selection);
	}

	/** Makes the selected state the one the level starts at. */
	public void setInitial ()
	{
		if (!(selection instanceof State))		return;
		HFSMEdit.setInitial (root, (State) selection);
		changed ("Initial state");
	}

	/** Moves the selection by a few pixels. */
	public void nudge (int dx, int dy)
	{
		if (selection == null)					return;
		move (selection, (int) blockX (selection) + dx, (int) blockY (selection) + dy);
		changed ("Move");
	}

	/**
	 * Joins what was dragged with what it was dropped on: a state on a transition
	 * makes it leave that state, a transition on a state makes it arrive there, and
	 * a state on a state is a new transition between them.
	 */
	protected void join (Object target)
	{
		if ((dragging == null) || (target == null) || (dragging == target))		return;

		if ((dragging instanceof State) && (target instanceof State))
		{
			Transition	t = HFSMEdit.addTransition (root, level, (State) dragging, (State) target);

			setSelection (t);
			changed ("Add transition");
			return;
		}
		if ((dragging instanceof State) && (target instanceof Transition))
		{
			HFSMEdit.setOrigin (root, (Transition) target, (State) dragging);
			setSelection (target);
			changed ("Transition origin");
			return;
		}
		if ((dragging instanceof Transition) && (target instanceof State))
		{
			((Transition) dragging).setArrivalState ((State) target);
			setSelection (dragging);
			changed ("Transition arrival");
			return;
		}
	}

	protected void changed (String what)
	{
		if (listener != null)					listener.machineChanged (what);
		repaint ();
	}

	/* ------------------------------------------------------------------ */
	/* Where the blocks are                                                */
	/* ------------------------------------------------------------------ */

	protected double blockX (Object o)
	{
		return (o instanceof State) ? ((State) o).getX () : ((Transition) o).getX ();
	}

	protected double blockY (Object o)
	{
		return (o instanceof State) ? ((State) o).getY () : ((Transition) o).getY ();
	}

	protected void move (Object o, int x, int y)
	{
		if (o instanceof State)					((State) o).setPosition (x, y);
		else if (o instanceof Transition)		((Transition) o).setPosition (x, y);
	}

	protected int radius (State s)				{ return (s instanceof MetaState) ? META_RADIUS : RADIUS; }

	/** The block at a point of the level being shown, the transitions first. */
	public Object pick (double x, double y)
	{
		for (State s : level.getStatesList ())
			for (Transition t : s.getTransitions ())
				if ((x >= t.getX ()) && (x <= (t.getX () + BOX_W)) && (y >= t.getY ()) && (y <= (t.getY () + BOX_H)))
					return t;
		for (State s : level.getStatesList ())
			if (Math.hypot (x - s.getX (), y - s.getY ()) <= radius (s))
				return s;
		return null;
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

		grid (g);

		// the arrows first, so that the blocks lie over them
		for (State s : level.getStatesList ())
			for (Transition t : s.getTransitions ())
				arrows (g, s, t);

		for (State s : level.getStatesList ())
			state (g, s);
		for (State s : level.getStatesList ())
			for (Transition t : s.getTransitions ())
				box (g, t);

		rubber (g);
		breadcrumb (g);
	}

	private void grid (Graphics2D g)
	{
		double		step = 50.0 * ((scale < 0.5) ? 4 : (scale < 1.0) ? 2 : 1);

		g.setColor (C_GRID);
		for (double x = Math.floor (wx (0) / step) * step; x < wx (getWidth ()); x += step)
			g.draw (new Line2D.Double (px (x), 0, px (x), getHeight ()));
		for (double y = Math.floor (wy (0) / step) * step; y < wy (getHeight ()); y += step)
			g.draw (new Line2D.Double (0, py (y), getWidth (), py (y)));
	}

	/**
	 * A state as a circle: a light grey one, or a darker one when it is a machine of
	 * its own. A machine that is running has the state it is in, and the meta states
	 * that hold it, drawn in red.
	 */
	private void state (Graphics2D g, State s)
	{
		double		r = radius (s) * scale;
		double		x = px (s.getX ()), y = py (s.getY ());
		boolean		sel = (selection == s);
		boolean		initial = (level.getInitialState () == s);
		boolean		here = live.contains (s);								// the machine is in it, or inside it
		boolean		now = here && (live.indexOf (s) == (live.size () - 1));	// and this is the state it is really in

		g.setColor (here ? (now ? C_LIVEF : C_LIVEM) : (s instanceof MetaState) ? C_META : C_STATE);
		g.fill (new Ellipse2D.Double (x - r, y - r, 2 * r, 2 * r));
		g.setColor (sel ? C_SEL : here ? C_LIVE : C_EDGE);
		g.setStroke (new BasicStroke (sel ? 3f : here ? (now ? 3f : 2f) : 1.4f));
		g.draw (new Ellipse2D.Double (x - r, y - r, 2 * r, 2 * r));

		if (initial)															// a ring inside, and the arrow of the start
		{
			g.setStroke (new BasicStroke (1.2f));
			g.setColor (C_EDGE);
			g.draw (new Ellipse2D.Double (x - r + 4, y - r + 4, 2 * r - 8, 2 * r - 8));
			arrow (g, x - r - 22 * scale, y, x - r - 3, y, C_EDGE);
		}
		if (s instanceof MetaState)												// how much it holds
		{
			MetaState	m = (MetaState) s;

			label (g, m.getStateCount () + "+" + m.getMetaStateCount (), x, y + r - 8 * scale, C_LEVEL, 10);
		}
		// a name that does not fit inside the circle goes under it
		if (width (g, s.getName (), 12) < (2 * r - 8))
			label (g, s.getName (), x, y + 4 * scale, now ? C_LIVE : C_TEXT, 12);
		else
			label (g, s.getName (), x, y + r + 14 * scale, now ? C_LIVE : C_TEXT, 12);
	}

	/** A transition as a box in light cyan. */
	private void box (Graphics2D g, Transition t)
	{
		double		x = px (t.getX ()), y = py (t.getY ());
		double		w = BOX_W * scale, h = BOX_H * scale;
		boolean		sel = (selection == t);
		boolean		lost = (t.getArrivalState () == null);

		g.setColor (C_TRANS);
		g.fill (new RoundRectangle2D.Double (x, y, w, h, 8 * scale, 8 * scale));
		g.setColor (sel ? C_SEL : (lost ? C_LOST : C_EDGE));
		g.setStroke (new BasicStroke (sel ? 3f : 1.2f));
		g.draw (new RoundRectangle2D.Double (x, y, w, h, 8 * scale, 8 * scale));
		label (g, t.getName (), x + w / 2, y + h / 2 + 4 * scale, C_TEXT, 11);
		if (t.getPriority () != 1)
			label (g, Integer.toString (t.getPriority ()), x + 8 * scale, y + h - 3 * scale, C_LEVEL, 9);
	}

	/** The arrows of a transition: from where it leaves to its box, and from the box to where it arrives. */
	private void arrows (Graphics2D g, State from, Transition t)
	{
		double		bx = px (t.getX () + BOX_W / 2.0), by = py (t.getY () + BOX_H / 2.0);

		g.setStroke (new BasicStroke (1.4f));

		double		fx = px (from.getX ()), fy = py (from.getY ());
		double		fr = radius (from) * scale;
		double		fa = Math.atan2 (by - fy, bx - fx);

		arrow (g, fx + fr * Math.cos (fa), fy + fr * Math.sin (fa), bx, by, C_ARROW);

		State		to = t.getArrivalState ();

		if ((to != null) && level.getStatesList ().contains (to))
		{
			double	tx = px (to.getX ()), ty = py (to.getY ());
			double	r = radius (to) * scale;
			double	a = Math.atan2 (by - ty, bx - tx);

			arrow (g, bx, by, tx + r * Math.cos (a), ty + r * Math.sin (a), C_ARROW);
		}
		else if (to != null)													// it leaves this level
			label (g, "-> " + to.getName (), bx, by + (BOX_H / 2.0 + 12) * scale, C_LEVEL, 10);
	}

	/** What is being dragged to join two blocks. */
	private void rubber (Graphics2D g)
	{
		if (dragMode != 3)						return;

		g.setColor (C_RUBBER);
		g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 6f, 4f }, 0f));
		g.draw (new Line2D.Double (px (anchorX), py (anchorY), px (curX), py (curY)));
	}

	/** Where the level being shown is, at the top left corner. */
	private void breadcrumb (Graphics2D g)
	{
		g.setFont (g.getFont ().deriveFont (Font.BOLD, 12f));
		g.setColor (C_LEVEL);
		g.drawString (levelPath () + (canGoUp () ? "     (double click on the background to go up)" : ""), 10, 18);
	}

	static private void arrow (Graphics2D g, double x1, double y1, double x2, double y2, Color c)
	{
		double		a = Math.atan2 (y2 - y1, x2 - x1);
		double		len = 9.0;
		Path2D.Double	head = new Path2D.Double ();

		g.setColor (c);
		g.draw (new Line2D.Double (x1, y1, x2, y2));
		head.moveTo (x2, y2);
		head.lineTo (x2 - len * Math.cos (a - 0.4), y2 - len * Math.sin (a - 0.4));
		head.lineTo (x2 - len * Math.cos (a + 0.4), y2 - len * Math.sin (a + 0.4));
		head.closePath ();
		g.fill (head);
	}

	/** How wide a label would be drawn. */
	private double width (Graphics2D g, String text, float size)
	{
		if ((text == null) || (text.length () == 0))		return 0.0;
		return g.getFontMetrics (g.getFont ().deriveFont (Font.PLAIN, (float) Math.max (8.0, size * scale))).stringWidth (text);
	}

	private void label (Graphics2D g, String text, double cx, double baseline, Color c, float size)
	{
		if ((text == null) || (text.length () == 0))		return;

		g.setFont (g.getFont ().deriveFont (Font.PLAIN, (float) Math.max (8.0, size * scale)));

		FontMetrics	fm = g.getFontMetrics ();

		g.setColor (c);
		g.drawString (text, (float) (cx - fm.stringWidth (text) / 2.0), (float) baseline);
	}

	/* ------------------------------------------------------------------ */
	/* What is said in the status bar                                      */
	/* ------------------------------------------------------------------ */

	private void status (Object o)
	{
		if (listener == null)					return;
		if (o instanceof MetaState)
		{
			MetaState	m = (MetaState) o;

			listener.statusChanged ("Meta state " + m.getName () + " [" + m.getId () + "]: " + m.getStateCount () + " states, "
									+ m.getMetaStateCount () + " meta states, " + m.getTransitionCount () + " transitions");
		}
		else if (o instanceof State)
		{
			State	s = (State) o;

			listener.statusChanged ("State " + s.getName () + " [" + s.getId () + "], " + s.getTransitionsSize () + " transitions"
									+ ((s.getCode ().trim ().length () == 0) ? ", no code" : ""));
		}
		else if (o instanceof Transition)
		{
			Transition	t = (Transition) o;

			State		from = HFSMEdit.origin (root, t);

			listener.statusChanged ("Transition " + t.getName () + " [" + t.getId () + "] from "
									+ ((from != null) ? from.getName () : "nowhere") + " to "
									+ ((t.getArrivalState () != null) ? t.getArrivalState ().getName () : "nowhere")
									+ ", priority " + t.getPriority ());
		}
		else
			listener.statusChanged (null);
	}

	private void showUsage ()
	{
		if (listener == null)					return;
		listener.usageChanged (usage ());
	}

	/** How the tool in hand is used. */
	public String usage ()
	{
		if (watch && (tool != T_PAN))
			return "Double click: a meta state opens, the background goes up a level. Wheel: zoom";

		switch (tool)
		{
		case T_PAN:		return "Drag to move the diagram. Wheel: zoom";
		case T_STATE:	return "Click to put a state. Right click / Esc: back to Select";
		case T_META:	return "Click to put a meta state (double click on it to go inside). Right click / Esc: back to Select";
		case T_TRANS:	return "Drag from one state to another to put a transition between them. Right click / Esc: back to Select";
		case T_LINK:	return "Drag a state onto a transition (it leaves it) or a transition onto a state (it arrives there)";
		default:		return "Click to select, drag to move. Double click: a meta state opens, anything else is renamed."
							   + " Del: delete, F2: rename, arrows: nudge";
		}
	}
}
