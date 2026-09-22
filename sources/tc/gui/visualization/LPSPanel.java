/*
 * (c) 2000-2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */

package tc.gui.visualization;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tc.vrobot.*;
import tclib.navigation.pathplanning.*;

import devices.pos.*;
import wucore.utils.geom.*;
import wucore.widgets.*;

/**
 * The Local Perceptual Space of a robot, drawn with Swing: the robot in the
 * middle, heading up the panel, and around it what its LPS holds -- each LPO
 * drawn as it draws itself, with its anchoring (how sure the LPS still is of
 * it) under its name -- and the path it follows.
 *
 * The robot is drawn with the image of its description when it has one, over
 * the box of its drawing as the windows of the world do, and with its drawing
 * (or a circle and its heading) when it has not. Both axes are ruled from side
 * to side of the panel.
 *
 * The wheel zooms about the pointer, dragging moves the view, and a double
 * click brings back the first view (the reach of the sensors fitting in it).
 */
public class LPSPanel extends JPanel
{
	private static final long		serialVersionUID	= 1L;

	static public final double		ZOOM_STEP	= 1.15;			// one notch of the wheel
	static public final double		MIN_ZOOM	= 0.05;
	static public final double		MAX_ZOOM	= 50.0;
	static public final int			MAX_MARKS	= 60;			// as many marks as an axis carries, about
	static public final int			SHORT_MARK	= 3;			// pixels each side of an axis
	static public final int			LONG_MARK	= 7;
	static public final double		LOW_ANCHOR	= 0.2;			// an anchoring below this is shown in red

	static protected final Color	C_BACK		= Color.WHITE;
	static protected final Color	C_AXIS		= new Color (90, 90, 90);
	static protected final Color	C_SCALE		= new Color (140, 140, 140);
	static protected final Color	C_ROBOT		= Color.RED;
	static protected final Color	C_PATH		= Color.ORANGE;
	static protected final Color	C_LOW		= new Color (210, 0, 0);
	static protected final Color	C_HIGH		= new Color (0, 150, 0);

	// The colour of the name of an LPO, by where what it stands for comes from
	static protected final Color	C_MAP			= new Color (0, 70, 210);		// blue
	static protected final Color	C_PERCEPT		= new Color (0, 115, 0);		// dark green
	static protected final Color	C_ARTIFACT		= new Color (190, 0, 190);		// magenta
	static protected final Color	C_COORDINATION	= new Color (235, 125, 0);		// orange

	static protected final Font		F_TEXT		= new Font ("SansSerif", Font.PLAIN, 12);
	static protected final Font		F_ANCHOR	= new Font ("SansSerif", Font.PLAIN, 10);
	static protected final Font		F_LABEL		= new Font ("Monospaced", Font.PLAIN, 12);
	static protected final int		NOTE_MARGIN	= 6;			// of the notes from the corner (pixels)
	static protected final int		NAME_GAP	= 4;			// of the name of an LPO from its drawing (pixels)
	static protected final Font		F_SCALE		= new Font ("SansSerif", Font.PLAIN, 9);

	static protected final Stroke	S_PLAIN		= new BasicStroke (1.0f);
	static protected final Stroke	S_THICK		= new BasicStroke (2.5f);
	static protected final Stroke	S_DASHED	= new BasicStroke (1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] { 5f, 4f }, 0f);

	// What is drawn: the drawing of the LPS (built where the LPS is updated), and what goes with it
	protected final Object			lock		= new Object ();
	protected Model2D				front		= new Model2D ();		// the one painted
	protected Model2D				back		= new Model2D ();		// the one being built
	protected Map<String, Double>	anchors		= new HashMap<String, Double> ();	// anchoring of the LPOs drawn, by name
	protected Map<Integer, double[]>	corners	= new HashMap<Integer, double[]> ();	// name of an LPO (its text) -> top right of its drawing (m)
	protected Map<Integer, Color>		inks	= new HashMap<Integer, Color> ();		// name of an LPO (its text) -> its colour (by its source)
	protected String				image;									// the image of the robot (null: its drawing)
	protected double[]				ibox;									// the box it is drawn over, in the robot
	protected LPOView				view		= new LPOView ();
	protected double				reach		= 3.0;					// how far the sensors read (m)
	protected boolean				drawpath	= true;

	// The view: pixels a metre (fitting the reach, times the zoom), and how far the robot is moved from the middle
	protected double				zoom		= 1.0;
	protected double				panx, pany;
	protected Point					drag;

	public LPSPanel ()
	{
		setBackground (C_BACK);
		setPreferredSize (new Dimension (400, 400));
		setToolTipText (null);

		MouseAdapter	mouse = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)		{ drag = e.getPoint (); }
			public void mouseReleased (MouseEvent e)	{ drag = null; }
			public void mouseDragged (MouseEvent e)
			{
				if (drag == null)		return;
				panx	+= e.getX () - drag.x;
				pany	+= e.getY () - drag.y;
				drag	= e.getPoint ();
				repaint ();
			}
			public void mouseWheelMoved (MouseWheelEvent e)
			{
				double	f = Math.pow (ZOOM_STEP, -e.getPreciseWheelRotation ());
				double	nz = Math.max (MIN_ZOOM, Math.min (MAX_ZOOM, zoom * f));
				double	cx = getWidth () / 2.0 + panx, cy = getHeight () / 2.0 + pany;

				// what is under the pointer stays under it
				f		= nz / zoom;
				panx	+= (e.getX () - cx) * (1.0 - f);
				pany	+= (e.getY () - cy) * (1.0 - f);
				zoom	= nz;
				repaint ();
			}
			public void mouseClicked (MouseEvent e)
			{
				if (e.getClickCount () == 2)		{ zoom = 1.0; panx = pany = 0.0; repaint (); }
			}
		};
		addMouseListener (mouse);
		addMouseMotionListener (mouse);
		addMouseWheelListener (mouse);
	}

	public void			drawPath (boolean draw)			{ drawpath = draw; }
	public void			orientation (double rotation)	{ view.rotation = rotation; }

	/* ------------------------------------------------------------------ */
	/* What is drawn                                                       */
	/* ------------------------------------------------------------------ */

	/**
	 * Draws the LPS (and the path) again. It can be called from any thread: the
	 * drawing is built here and handed to the panel, which paints it.
	 */
	public void update (LPS lps, Path path)
	{
		Model2D					model;
		Map<String, Double>		anch = new HashMap<String, Double> ();
		Map<Integer, double[]>	corn = new HashMap<Integer, double[]> ();
		Map<Integer, Color>		ink = new HashMap<Integer, Color> ();
		RobotDesc				rdesc;
		LPORangeBuffer			rbuffer;
		String					img = null;
		double[]				box = null;
		double					far;

		if (lps == null)		return;
		synchronized (lock)		{ model = back; }

		model.clearView ();
		rdesc	= lps.rdesc ();

		// how far the sensors read, which is what the first view fits
		rbuffer	= (lps.find ("RBuffer") instanceof LPORangeBuffer) ? (LPORangeBuffer) lps.find ("RBuffer") : null;
		if (rbuffer != null)
			reach	= Math.max (Math.max (rbuffer.getRangeSON (), rbuffer.getRangeLRF ()), 3.0);

		// the LPOs draw themselves wherever they are: the panel shows what it is moved to
		far		= 1000.0;
		view.min.set (-far, -far, -far);
		view.max.set (far, far, far);

		if ((path != null) && drawpath)		path (model, lps, path);

		view.verbose	= false;
		for (int i = 0; i < lps.lpos_n (); i++)
		{
			LPO		o = lps.lpos ()[i];
			if ((o == null) || ((o.source () == LPOSource.PERCEPT) && o.lost ()))		continue;		// a percept lost is not drawn
			int		first = model.nattr;
			o.draw (model, view);
			if (o.label () != null)		corner (model, first, o.label (), corn);
			if (o.label () != null)		ink (model, first, o.label (), ink (o.source ()), ink);
			if ((o != null) && o.active () && (o.label () != null) && anchored (o))		anch.put (o.label (), o.anchor ());
		}

		// the robot: its image, over the box of its drawing, or its drawing
		if ((rdesc != null) && (rdesc.image != null) && (RobotImage.get (rdesc.image) != null))
		{
			img		= rdesc.image;
			box		= RobotImage.box (rdesc.icon, rdesc.RADIUS);
		}
		if ((rdesc != null) && ((img == null) || (box == null)))
		{
			img		= null;
			if (rdesc.icon == null)
			{
				model.addRawCircle (0.0, 0.0, rdesc.RADIUS, C_ROBOT);
				model.addRawArrow (0.0, 0.0, rdesc.RADIUS, view.rotation, C_ROBOT);
			}
			else
				for (Line2 l : rdesc.icon)
					model.addRawRotLine (l, view.rotation, C_ROBOT);
		}

		synchronized (lock)
		{
			back	= front;
			front	= model;
			anchors	= anch;
			corners	= corn;
			inks	= ink;
			image	= img;
			ibox	= box;
		}
		repaint ();
	}

	/**
	 * Where the name of an LPO goes: the top right corner of the box of what it
	 * drew (from the element first on), kept for the text of its name, which is
	 * painted to the right of it, its top level with the top of the drawing. An
	 * LPO that draws nothing but its name keeps it where it put it.
	 */
	static protected void corner (Model2D m, int first, String label, Map<Integer, double[]> corn)
	{
		double		maxx = -Double.MAX_VALUE, maxy = -Double.MAX_VALUE;
		boolean		some = false;

		for (int i = first; i < m.nattr; i++)
		{
			Model2DAttr		a = m.attr[i];
			if ((a == null) || (a.type == Model2D.TEXT) || (a.type == Model2D.LABEL) || (a.type == Model2D.NOTE) || (a.type == Model2D.AXIS))
				continue;
			int[]			vs = (a.attype == Model2DAttr.ATTR_POLY) ? a.vset
								: (a.attype == Model2DAttr.ATTR_LINE) ? new int[] { a.vorig, a.vdest } : new int[] { a.vorig };
			for (int v : vs)
			{
				maxx	= Math.max (maxx, m.verts[v].x);
				maxy	= Math.max (maxy, m.verts[v].y);
				some	= true;
			}
		}
		if (!some)		return;
		for (int i = first; i < m.nattr; i++)
			if ((m.attr[i] != null) && (m.attr[i].type == Model2D.TEXT) && label.equals (m.attr[i].label))
				corn.put (Integer.valueOf (i), new double[] { maxx, maxy });
	}

	/** The colour of the name of an LPO, by where what it stands for comes from (null: the one it drew it with). */
	static protected Color ink (LPOSource source)
	{
		if (source == null)		return null;
		switch (source)
		{
		case MAP:			return C_MAP;
		case PERCEPT:		return C_PERCEPT;
		case ARTIFACT:		return C_ARTIFACT;
		case COORDINATION:	return C_COORDINATION;
		default:			return null;
		}
	}

	/** The texts of the name of an LPO (what it drew from the element first on) get the colour of its source. */
	static protected void ink (Model2D m, int first, String label, Color c, Map<Integer, Color> ink)
	{
		if (c == null)		return;
		for (int i = first; i < m.nattr; i++)
			if ((m.attr[i] != null) && (m.attr[i].type == Model2D.TEXT) && label.equals (m.attr[i].label))
				ink.put (Integer.valueOf (i), c);
	}

	/** The path, relative to the robot, as the old LPS window drew it. */
	protected void path (Model2D model, LPS lps, Path path)
	{
		double		xi, yi, xf, yf;
		double		xx, yy, aa, rho, phi;
		int			k;

		switch (path.type ())
		{
		case GridPath.BSPLINE:
			xi	= path.first ().x () - lps.cur.x ();
			yi	= path.first ().y () - lps.cur.y ();
			for (Position pos = path.next (); pos != null; pos = path.next ())
			{
				xf	= pos.x () - lps.cur.x ();
				yf	= pos.y () - lps.cur.y ();
				model.addRawTransRotLine (xi, yi, xf, yf, 0.0, 0.0, view.rotation - lps.cur.alpha (), C_PATH);
				xi	= xf;
				yi	= yf;
			}
			break;

		case GridPath.POLYLINE:
		default:
			k = 0;
			for (Position pos = path.first (); pos != null; pos = path.next (), k++)
			{
				if (k % 2 != 0)			continue;
				xx	= pos.x () - lps.cur.x ();
				yy	= pos.y () - lps.cur.y ();
				aa	= pos.alpha () - lps.cur.alpha ();
				rho	= Math.sqrt (xx * xx + yy * yy);
				phi	= Math.atan2 (yy, xx);
				xx	= rho * Math.cos (view.rotation + phi - lps.cur.alpha ());
				yy	= rho * Math.sin (view.rotation + phi - lps.cur.alpha ());
				model.addRawArrow (xx, yy, path.step () * 0.75, view.rotation + aa, C_PATH);
			}
		}
	}

	/* ------------------------------------------------------------------ */
	/* Painting                                                            */
	/* ------------------------------------------------------------------ */

	/** Pixels a metre: the reach fitting half the smaller side, times the zoom. */
	protected double scale ()
	{
		return zoom * 0.5 * Math.max (1, Math.min (getWidth (), getHeight ())) / reach;
	}

	protected void paintComponent (Graphics g0)
	{
		Graphics2D		g = (Graphics2D) g0.create ();
		double			s = scale ();
		double			ox = getWidth () / 2.0 + panx, oy = getHeight () / 2.0 + pany;

		super.paintComponent (g0);
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint (RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		axes (g, s, ox, oy);
		synchronized (lock)
		{
			// the image of the robot under what is drawn round it; its drawing is in the model
			if ((image != null) && (ibox != null))
			{
				double	bx = (ibox[0] + ibox[2]) / 2, by = (ibox[1] + ibox[3]) / 2;			// centre of the box, in the robot
				double	a = view.rotation;
				double	cx = bx * Math.cos (a) - by * Math.sin (a), cy = bx * Math.sin (a) + by * Math.cos (a);
				RobotImage.draw (g, RobotImage.get (image), ox + cx * s, oy - cy * s, (ibox[2] - ibox[0]) * s, (ibox[3] - ibox[1]) * s, a);
			}
			draw (g, front, s, ox, oy);
		}
		g.dispose ();
	}

	/**
	 * Whether the anchoring of an LPO is shown: not for the artifacts (the path,
	 * markers...) nor for what comes from the map, which have none; for a percept,
	 * only while it still has some (a percept lost, at zero, is not even drawn).
	 */
	static protected boolean anchored (LPO o)
	{
		if (o.source () == null)		return true;
		switch (o.source ())
		{
		case ARTIFACT:
		case MAP:			return false;
		case PERCEPT:		return o.anchor () > 0.0;
		default:			return true;
		}
	}

	/** The two axes through the robot, from side to side of the panel, ruled in tenths of a metre, metres or more. */
	protected void axes (Graphics2D g, double s, double ox, double oy)
	{
		int			w = getWidth (), h = getHeight ();
		double		step = 0.1;
		FontMetrics	fm;

		g.setColor (C_AXIS);
		g.setStroke (S_PLAIN);
		g.draw (new Line2D.Double (0, oy, w, oy));
		g.draw (new Line2D.Double (ox, 0, ox, h));

		// the marks: a tenth of a metre while that leaves an axis readable, ten times as much each time it does not
		while (Math.max (w, h) / (s * step) > MAX_MARKS)		step *= 10.0;
		g.setFont (F_SCALE);
		fm	= g.getFontMetrics ();
		for (long k = (long) Math.floor (-ox / (s * step)); k <= (long) Math.ceil ((w - ox) / (s * step)); k++)
		{
			double	x = ox + k * step * s;
			boolean	lng = (k % 10 == 0);
			g.setColor (C_AXIS);
			g.draw (new Line2D.Double (x, oy - (lng ? LONG_MARK : SHORT_MARK), x, oy + (lng ? LONG_MARK : SHORT_MARK)));
			if (lng && (k != 0))
			{
				String	t = metres (k * step);
				g.setColor (C_SCALE);
				g.drawString (t, (float) (x - fm.stringWidth (t) / 2.0), (float) (oy + LONG_MARK + fm.getAscent ()));
			}
		}
		for (long k = (long) Math.floor (-oy / (s * step)); k <= (long) Math.ceil ((h - oy) / (s * step)); k++)
		{
			double	y = oy + k * step * s;
			boolean	lng = (k % 10 == 0);
			g.setColor (C_AXIS);
			g.draw (new Line2D.Double (ox - (lng ? LONG_MARK : SHORT_MARK), y, ox + (lng ? LONG_MARK : SHORT_MARK), y));
			if (lng && (k != 0))
			{
				String	t = metres (-k * step);						// up is positive
				g.setColor (C_SCALE);
				g.drawString (t, (float) (ox + LONG_MARK + 2), (float) (y + fm.getAscent () / 2.0 - 1));
			}
		}
	}

	static protected String metres (double m)
	{
		return (Math.abs (m - Math.rint (m)) < 1e-6) ? String.format (Locale.ROOT, "%d m", Math.round (m)) : String.format (Locale.ROOT, "%.1f m", m);
	}

	/** The drawing of the LPS, element by element, as the old component drew it but in Java2D. */
	protected void draw (Graphics2D g, Model2D m, double s, double ox, double oy)
	{
		Model2DCoord[]		v = m.verts;
		Model2DAttr[]		at = m.attr;
		int					n = Math.min (m.nattr, at.length);
		List<Model2DAttr>	notes = new ArrayList<Model2DAttr> ();

		for (int i = 0; i < n; i++)
		{
			Model2DAttr		a = at[i];
			double			x1, y1, x2 = 0, y2 = 0;

			if (a == null)		continue;
			if (a.type == Model2D.NOTE)
			{
				if (a.label != null)		notes.add (a);
				continue;
			}
			g.setColor ((a.color != null) ? a.color : Color.BLACK);
			switch (a.mode)
			{
			case Model2D.DASHED:	g.setStroke (S_DASHED); break;
			case Model2D.THICK:
			case Model2D.SELECT:	g.setStroke (S_THICK); break;
			default:				g.setStroke (S_PLAIN);
			}

			if (a.attype == Model2DAttr.ATTR_POLY)
			{
				Path2D.Double	p = new Path2D.Double ();
				for (int j = 0; j < a.vset.length; j++)
				{
					double	px = ox + v[a.vset[j]].x * s, py = oy - v[a.vset[j]].y * s;
					if (j == 0)		p.moveTo (px, py);
					else			p.lineTo (px, py);
				}
				p.closePath ();
				if (a.mode == Model2D.FILLED)		g.fill (p);
				else								g.draw (p);
				continue;
			}

			x1	= ox + v[a.vorig].x * s;
			y1	= oy - v[a.vorig].y * s;
			if (a.attype == Model2DAttr.ATTR_LINE)
			{
				x2	= ox + v[a.vdest].x * s;
				y2	= oy - v[a.vdest].y * s;
				double	mx = Math.min (x1, x2), my = Math.min (y1, y2), ww = Math.abs (x2 - x1), hh = Math.abs (y2 - y1);

				switch (a.type)
				{
				case Model2D.BOX:
					if (a.mode == Model2D.FILLED)	g.fill (new Rectangle2D.Double (mx, my, ww, hh));
					else							g.draw (new Rectangle2D.Double (mx, my, ww, hh));
					break;
				case Model2D.CIRCLE:
					if (a.mode == Model2D.FILLED)	g.fill (new Ellipse2D.Double (mx, my, ww, hh));
					else							g.draw (new Ellipse2D.Double (mx, my, ww, hh));
					break;
				case Model2D.ARC:
					if (a.mode == Model2D.FILLED)	g.fill (new Arc2D.Double (mx, my, ww, hh, a.vset[0], a.vset[1], Arc2D.PIE));
					else							g.draw (new Arc2D.Double (mx, my, ww, hh, a.vset[0], a.vset[1], Arc2D.OPEN));
					break;
				case Model2D.AXIS:					// the axes are the panel's own
					break;
				case Model2D.LINE:
				default:
					g.draw (new Line2D.Double (x1, y1, x2, y2));
				}
				continue;
			}

			// points
			switch (a.type)
			{
			case Model2D.POINT:
				if (a.mode == Model2D.FILLED)	g.fill (new Ellipse2D.Double (x1 - 3, y1 - 3, 6, 6));
				else							g.draw (new Ellipse2D.Double (x1 - 3, y1 - 3, 6, 6));
				break;
			case Model2D.DOT:
				g.fill (new Rectangle2D.Double (x1 - 0.75, y1 - 0.75, (a.mode == Model2D.THICK) ? 2.5 : 1.5, (a.mode == Model2D.THICK) ? 2.5 : 1.5));
				break;
			case Model2D.ICON:
				if (a.src instanceof Image)
				{
					Image	im = (Image) a.src;
					int		iw = im.getWidth (this), ih = im.getHeight (this);
					g.drawImage (im, (int) Math.round (x1 - iw / 2.0), (int) Math.round (y1 - ih / 2.0), this);
				}
				if (a.label != null)
				{
					g.setFont (F_TEXT);
					g.drawString (a.label, (float) x1 + 8, (float) y1 + 12);
				}
				break;
			case Model2D.TEXT:
				if (a.label != null)
				{
					double[]	c = corners.get (Integer.valueOf (i));
					Color		k = inks.get (Integer.valueOf (i));
					if (k != null)		g.setColor (k);
					if (c != null)		name (g, a, ox + c[0] * s + NAME_GAP, oy - c[1] * s);
					else				text (g, a, x1, y1);
				}
				break;
			case Model2D.LABEL:
				if (a.label != null)
				{
					FontMetrics	fm;
					g.setFont (F_LABEL);
					fm	= g.getFontMetrics ();
					Rectangle2D	r = new Rectangle2D.Double (x1 - 3, y1 - fm.getAscent () - 1, fm.stringWidth (a.label) + 6, fm.getHeight () + 1);
					g.fill (r);
					g.setColor (Color.BLACK);
					g.setStroke (S_PLAIN);
					g.draw (r);
					g.drawString (a.label, (float) x1, (float) y1);
				}
				break;
			default:
			}
		}
		notes (g, notes);
	}

	/** The notes, one line under the other, right-justified in the bottom right corner of the panel. */
	protected void notes (Graphics2D g, List<Model2DAttr> notes)
	{
		FontMetrics		fm;
		double			y;

		if (notes.isEmpty ())		return;
		g.setFont (F_TEXT);
		fm	= g.getFontMetrics ();
		y	= getHeight () - NOTE_MARGIN - fm.getDescent () - (notes.size () - 1) * fm.getHeight ();
		for (Model2DAttr a : notes)
		{
			g.setColor ((a.color != null) ? a.color : Color.BLACK);
			g.drawString (a.label, (float) (getWidth () - NOTE_MARGIN - fm.stringWidth (a.label)), (float) y);
			y	+= fm.getHeight ();
		}
	}

	/** The name of an LPO, from a point to the right of its drawing, its top level with the one of the drawing. */
	protected void name (Graphics2D g, Model2DAttr a, double x, double top)
	{
		g.setFont (F_TEXT);
		g.drawString (a.label, (float) x, (float) (top + g.getFontMetrics ().getAscent ()));
		anchor (g, a, x, top + g.getFontMetrics ().getAscent ());
	}

	/**
	 * A text, justified as it asks; when it is the name of an LPO, its anchoring
	 * goes under it, from its left, in a smaller letter: red when it is low (the
	 * LPS barely holds it any more), green otherwise.
	 */
	protected void text (Graphics2D g, Model2DAttr a, double x, double y)
	{
		FontMetrics		fm;
		double			ww, hh, xx, yy;

		g.setFont (F_TEXT);
		fm	= g.getFontMetrics ();
		ww	= fm.stringWidth (a.label);
		hh	= fm.getHeight ();
		switch (a.mode)
		{
		case Model2D.J_CENTER:	xx = x - ww / 2;	break;
		case Model2D.J_RIGHT:	xx = x - ww;		break;
		default:				xx = x;
		}
		yy	= y + hh / 2;
		g.drawString (a.label, (float) xx, (float) yy);
		anchor (g, a, xx, yy);
	}

	/** The anchoring of the LPO a name is of (if it shows one), under the name, from its left (baseline of the name at y). */
	protected void anchor (Graphics2D g, Model2DAttr a, double x, double y)
	{
		Double			anchor = anchors.get (a.label);

		if (anchor == null)		return;
		g.setFont (F_ANCHOR);
		g.setColor ((anchor < LOW_ANCHOR) ? C_LOW : C_HIGH);
		g.drawString (String.format (Locale.ROOT, "%.2f", anchor), (float) x, (float) (y + g.getFontMetrics ().getAscent () + 1));
	}
}
