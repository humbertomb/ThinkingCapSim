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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
	static private final Color		C_AREA		= new Color (0, 120, 215, 28);		// the area dragged to select
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
		/** The right button on a state or a meta state: whoever edits may offer a menu there (screen coordinates of the canvas). */
		default public void nodeMenu (State s, int x, int y)		{ }
		/** A double click on an extern meta state: whoever edits may open its file in this canvas. Whether it did. */
		default public boolean enterExtern (MetaState m)			{ return false; }
		/** A double click on the background at the top of the machine: whoever edits may go back to the machine that holds this one. Whether it did. */
		default public boolean leaveRoot ()							{ return false; }
		/** A tool finished what it was for (back to Select). */
		public void toolFinished ();
	}

	/* Model */
	protected MetaState				root;
	protected File					file;							// the file of the root machine (null: none yet)
	protected String				outerPath;						// where the root is inside the machine that holds it (an extern machine being edited), shown in its stead; null at the top
	protected MetaState				level;						// the machine being shown
	protected Object				selection;					// the one block selected (a State or a Transition); null when none, or several
	protected LinkedHashSet<Object>	selected = new LinkedHashSet<Object> ();	// every block selected: one, or several dragged an area around
	protected Map<Object, int[]>	grabbed = new HashMap<Object, int[]> ();	// where each selected block was when the drag began

	/* Watching a machine run: where it is, and nothing of it can be changed */
	protected List<State>			live = new ArrayList<State> ();
	protected boolean				watch;

	/* View */
	protected double				scale		= 1.0;
	protected double				cx, cy;						// what is at the centre of the view
	protected int					tool		= T_SELECT;
	protected Listener				listener;

	/* Dragging */
	protected int					dragMode;					// 0 none, 1 move, 2 pan, 3 rubber (join), 4 area (select what is inside)
	protected double				anchorX, anchorY;			// where the drag started
	protected double				curX, curY;					// where the cursor is
	protected Object				dragging;					// what is being moved or joined

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

	/** The file the root machine is in (null: none yet): the paths of the extern meta states are shown from it. */
	public void setFile (File file)
	{
		if ((file == null) ? (this.file == null) : file.equals (this.file))		return;
		this.file	= file;
		repaint ();
	}

	/**
	 * The path of the file of an extern meta state as shown under it: from the
	 * folder of the root machine's file when both are known (../other/x.hfsm),
	 * else as the meta state keeps it.
	 */
	protected String externPath (MetaState m)
	{
		String		path = m.getPathExtern ();

		if ((path == null) || (path.trim ().length () == 0))		return "(no file)";
		if (file == null)										return path.trim ();
		try
		{
			File				base = file.getAbsoluteFile ().getParentFile ();
			File				f = tclib.behaviours.hfsm.HFSMJson.externFile (path, file);
			java.nio.file.Path	rel = base.getCanonicalFile ().toPath ().relativize (f.getCanonicalFile ().toPath ());

			return rel.toString ().replace (File.separatorChar, '/');
		}
		catch (Exception e)		{ return path.trim (); }
	}

	public void setMachine (MetaState root)
	{
		this.root		= root;
		this.level		= root;
		this.selection	= null;
		this.selected.clear ();
		if (listener != null)		{ listener.levelChanged (level);	listener.selectionChanged (null); }
		zoomToFit ();
		repaint ();
	}

	public MetaState				getMachine ()			{ return root; }
	public MetaState				getLevel ()				{ return level; }
	/** The one block selected, or null when none is, or several are (see {@link #getSelected}). */
	public Object					getSelection ()			{ return selection; }
	/** Every block selected, in the order they were: one, or the several an area was dragged around. */
	public List<Object>				getSelected ()			{ return new ArrayList<Object> (selected); }
	public double					getScale ()				{ return scale; }
	public void						setListener (Listener l)	{ listener = l;	showUsage (); }

	/** Shows a level of the machine (a meta state of it). */
	public void setLevel (MetaState m)
	{
		if (m == null)					return;
		level		= m;
		selection	= null;
		selected.clear ();
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

	/**
	 * Where the root of this machine is inside the machine that holds it, as
	 * <code>outer . meta</code>, when it is an extern machine opened from it;
	 * null when it is the top. Shown in the place of the root's own name, so that
	 * going into an extern meta state reads like going into any other.
	 */
	public void setOuterPath (String path)
	{
		outerPath	= path;
		repaint ();
	}

	public String outerPath ()			{ return outerPath; }

	/** Whether there is a machine that holds this one to go back to (see {@link #setOuterPath}). */
	public boolean canGoOut ()			{ return outerPath != null; }

	/** Where the level being shown is, as <code>root.meta.meta</code> (the root as the machine that holds it names it, when one does). */
	public String levelPath ()
	{
		List<String>	names = new ArrayList<String> ();

		for (State s = level; s != null; s = HFSMEdit.parent (root, s))		names.add (0, s.getName ());
		if ((outerPath != null) && !names.isEmpty ())		names.set (0, outerPath);

		StringBuffer	sb = new StringBuffer ();

		for (int i = 0; i < names.size (); i++)		sb.append ((i > 0) ? " . " : "").append (names.get (i));
		return sb.toString ();
	}

	public void setSelection (Object o)
	{
		selected.clear ();
		if (o != null)					selected.add (o);
		selection	= o;
		if (listener != null)			listener.selectionChanged (o);
		repaint ();
	}

	/** Selects several blocks at once: with one of them it is the selection, with more there is no single one. */
	public void setSelected (Collection<?> blocks)
	{
		selected.clear ();
		if (blocks != null)
			for (Object o : blocks)		if (o != null)	selected.add (o);
		selection	= (selected.size () == 1) ? selected.iterator ().next () : null;
		if (listener != null)			listener.selectionChanged (selection);
		repaint ();
	}

	/** Whether a block is among the selected ones. */
	public boolean isSelected (Object o)		{ return selected.contains (o); }

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
			if (tool != T_SELECT)			{ setTool (T_SELECT);	if (listener != null) listener.toolFinished (); return; }

			// on a state or a meta state, while editing: it is selected and offered its menu
			Object	hit = pick (x, y);

			if (!watch && (hit instanceof State))
			{
				if (!selected.contains (hit))	setSelection (hit);
				if (listener != null)			listener.nodeMenu ((State) hit, e.getX (), e.getY ());
			}
			return;
		}

		switch (tool)
		{
		case T_SELECT:
		{
			Object	hit = pick (x, y);

			if (hit == null)
			{
				// on the background: an area is dragged, and what it encloses is selected
				// on release (shift keeps what was selected and adds to it)
				if (!e.isShiftDown ())			setSelection (null);
				dragMode	= 4;
				break;
			}
			// on a block: shift adds it to (or takes it from) the selection; a block already
			// among the selected ones keeps them all, to be dragged together; any other
			// block is the one selection
			if (e.isShiftDown ())
			{
				LinkedHashSet<Object>	now = new LinkedHashSet<Object> (selected);

				if (!now.remove (hit))			now.add (hit);
				setSelected (now);
			}
			else if (!selected.contains (hit))
				setSelection (hit);
			if (!watch && selected.contains (hit))						// watching it, nothing is moved
			{
				dragMode	= 1;
				dragging	= hit;
				grabbed.clear ();
				for (Object o : selected)
					grabbed.put (o, new int[] { (int) Math.round (blockX (o)), (int) Math.round (blockY (o)) });
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
		case 1:																	// moving the selected blocks together
			if (dragging != null)
			{
				int		dx = (int) Math.round (curX - anchorX), dy = (int) Math.round (curY - anchorY);

				for (Map.Entry<Object, int[]> g : grabbed.entrySet ())
					move (g.getKey (), g.getValue ()[0] + dx, g.getValue ()[1] + dy);
				status (dragging);
				repaint ();
			}
			break;

		case 4:																	// dragging an area
			repaint ();
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
		if (dragMode == 4)			selectArea (e.isShiftDown ());

		dragMode	= 0;
		dragging	= null;
		grabbed.clear ();
		repaint ();
	}

	/**
	 * Selects the blocks of the level inside the area dragged (from the anchor to
	 * where the mouse is): a state by its centre, a transition by the centre of
	 * its box. With shift, they are added to what was selected.
	 */
	protected void selectArea (boolean add)
	{
		double		x1 = Math.min (anchorX, curX), x2 = Math.max (anchorX, curX);
		double		y1 = Math.min (anchorY, curY), y2 = Math.max (anchorY, curY);
		LinkedHashSet<Object>	now = new LinkedHashSet<Object> ();

		if (add)					now.addAll (selected);
		if ((x2 - x1 < 2) && (y2 - y1 < 2))		{ if (!add) setSelection (null);	return; }	// a click, not an area
		for (State s : level.getStatesList ())
		{
			if ((s.getX () >= x1) && (s.getX () <= x2) && (s.getY () >= y1) && (s.getY () <= y2))		now.add (s);
			for (Transition t : s.getTransitions ())
			{
				double	tx = t.getX () + BOX_W / 2.0, ty = t.getY () + BOX_H / 2.0;

				if ((tx >= x1) && (tx <= x2) && (ty >= y1) && (ty <= y2))		now.add (t);
			}
		}
		setSelected (now);
	}

	private void onClick (MouseEvent e)
	{
		if ((e.getClickCount () != 2) || !SwingUtilities.isLeftMouseButton (e))		return;

		Object		hit = pick (wx (e.getX ()), wy (e.getY ()));

		// the background goes out of the level (and out of an extern machine, back to
		// the one that holds it, at its top), a meta state goes into it -- an extern one
		// through its file, which whoever edits opens here -- and anything else is
		// renamed, but not while the machine is only being watched (then an extern meta
		// state holds its linked states, and is entered as any other)
		if (hit == null)
		{
			if (canGoUp ())						levelUp ();
			else if (listener != null)			listener.leaveRoot ();
			return;
		}
		if ((hit instanceof MetaState) && (watch || !((MetaState) hit).isExtern ()))
												{ setLevel ((MetaState) hit);		return; }
		if (hit instanceof MetaState)			{ if (listener != null)	listener.enterExtern ((MetaState) hit);	return; }
		if (!watch)								rename (hit);
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

	/** Deletes what is selected -- one block or several -- and with a state whatever joins it. */
	public void deleteSelection ()
	{
		List<Object>	gone = getSelected ();
		int				states = 0, transitions = 0;

		if (gone.isEmpty ())					return;
		// the transitions first: one of a state that goes too is gone with it either way
		for (Object o : gone)
			if ((o instanceof Transition) && (HFSMEdit.origin (root, (Transition) o) != null))
			{
				HFSMEdit.remove (root, (Transition) o);
				transitions++;
			}
		for (Object o : gone)
			if ((o instanceof State) && (o != root) && (HFSMEdit.parent (root, (State) o) != null))
			{
				HFSMEdit.remove (root, (State) o);
				states++;
			}
		setSelection (null);
		if (states + transitions == 0)			return;
		changed ((states + transitions == 1) ? ((states == 1) ? "Delete state" : "Delete transition")
											 : ("Delete " + (states + transitions) + " blocks"));
	}

	/** Expands the selected meta state: what it holds comes out to this level, and it goes (see {@link HFSMEdit#expand}). */
	public void expandSelection ()
	{
		if (!(selection instanceof MetaState) || (selection == root))		return;

		MetaState	m = (MetaState) selection;
		State		first = HFSMEdit.expand (root, m);

		setSelection (first);
		changed ("Expand meta state " + m.getName ());
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

	/** Moves what is selected, all of it, by a few pixels. */
	public void nudge (int dx, int dy)
	{
		if (selected.isEmpty ())				return;
		for (Object o : selected)
			move (o, (int) blockX (o) + dx, (int) blockY (o) + dy);
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
		boolean		sel = selected.contains (s);
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
			if (m.isExtern ())			file (g, x + r * 0.62, y + r * 0.62);	// what it holds is in a file: a sheet at its bottom right
		}
		// a name that does not fit inside the circle goes under it
		double		under = y + r + 14 * scale;								// the first line under the circle

		if (width (g, s.getName (), 12) < (2 * r - 8))
			label (g, s.getName (), x, y + 4 * scale, now ? C_LIVE : C_TEXT, 12);
		else
		{
			label (g, s.getName (), x, under, now ? C_LIVE : C_TEXT, 12);
			under	+= 12 * scale;
		}
		// an extern meta state says under it where its file is, from the file of this machine
		if ((s instanceof MetaState) && ((MetaState) s).isExtern ())
			label (g, externPath ((MetaState) s), x, under, C_LEVEL, 9);
	}

	/**
	 * A sheet of paper with its corner folded, on a circle: an extern meta state,
	 * whose states are in a file of their own. Centred on (cx, cy), in pixels.
	 */
	private void file (Graphics2D g, double cx, double cy)
	{
		double		w = 12 * scale, h = 15 * scale, f = 4 * scale;
		double		x = cx - w / 2, y = cy - h / 2;
		Path2D		sheet = new Path2D.Double ();

		sheet.moveTo (x, y);
		sheet.lineTo (x + w - f, y);
		sheet.lineTo (x + w, y + f);
		sheet.lineTo (x + w, y + h);
		sheet.lineTo (x, y + h);
		sheet.closePath ();
		g.setColor (Color.WHITE);
		g.fill (sheet);
		g.setColor (C_EDGE);
		g.setStroke (new BasicStroke (1.1f));
		g.draw (sheet);
		g.draw (new Line2D.Double (x + w - f, y, x + w - f, y + f));			// the fold
		g.draw (new Line2D.Double (x + w - f, y + f, x + w, y + f));
		g.setStroke (new BasicStroke (0.9f));
		for (int i = 1; i <= 3; i++)											// lines of text
			g.draw (new Line2D.Double (x + 2.5 * scale, y + f + i * 3 * scale, x + w - 2.5 * scale, y + f + i * 3 * scale));
	}

	/** A transition as a box in light cyan. */
	private void box (Graphics2D g, Transition t)
	{
		double		x = px (t.getX ()), y = py (t.getY ());
		double		w = BOX_W * scale, h = BOX_H * scale;
		boolean		sel = selected.contains (t);
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
		if (dragMode == 4)															// the area being dragged around blocks
		{
			double	x1 = px (Math.min (anchorX, curX)), y1 = py (Math.min (anchorY, curY));
			double	x2 = px (Math.max (anchorX, curX)), y2 = py (Math.max (anchorY, curY));
			java.awt.geom.Rectangle2D	r = new java.awt.geom.Rectangle2D.Double (x1, y1, x2 - x1, y2 - y1);

			g.setColor (C_AREA);
			g.fill (r);
			g.setColor (C_RUBBER);
			g.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 5f, 4f }, 0f));
			g.draw (r);
			return;
		}
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
		g.drawString (levelPath () + ((canGoUp () || canGoOut ()) ? "     (double click on the background to go up)" : ""), 10, 18);
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
									+ m.getMetaStateCount () + " meta states, " + m.getTransitionCount () + " transitions"
									+ (m.isExtern () ? ("  --  extern, from " + ((m.getPathExtern () != null) ? m.getPathExtern () : "no file yet")) : ""));
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
		default:		return "Click to select, drag to move; drag on the background to select an area (shift: add). Double click: a meta state opens, anything else is renamed."
							   + " Del: delete, F2: rename, arrows: nudge";
		}
	}
}
