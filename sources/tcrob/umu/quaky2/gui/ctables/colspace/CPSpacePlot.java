/**
 * The colours of an image in a colour space, in 3D: a point for each cell of
 * the space the image falls in, as big as how many pixels fall in it and in
 * their colour, and the prism of each segmented channel.
 *
 * Drawn with Java2D, turned with the mouse (drag to turn, wheel to zoom,
 * double click to go back to the first view), so it needs no 3D library and
 * lives in a Swing panel like any other. It replaces the one built on FreeHEP
 * (Plot3D), which is not part of the project.
 *
 * The space is drawn as a cube (RGB, YUV) or as a cylinder (HSV: the hue is
 * the angle, the saturation the radius and the value the height).
 *
 * @author Humberto Martinez Barbera
 */

package tcrob.umu.quaky2.gui.ctables.colspace;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import tclib.vision.chaos.channels.*;

public class CPSpacePlot extends JPanel
{
	private static final long		serialVersionUID	= 1L;

	/** Bits per component of the cells of the space (6: 64 x 64 x 64 cells). */
	static public final int			BITS		= 6;
	/** Pixels of the image taken, one of every STEP along each side. */
	static public final int			STEP		= 2;
	/** Turn of the first view (rad). */
	static public final double		YAW0		= Math.toRadians (-35.0);
	static public final double		PITCH0		= Math.toRadians (60.0);

	static protected final Color	C_FRAME		= new Color (120, 120, 120);
	static protected final Color	C_LABEL		= new Color (60, 60, 60);
	static protected final Color	C_BACK		= new Color (230, 230, 230);
	static protected final int		ARC			= 24;		// pieces of a circle or an arc

	// what is drawn: the cells with pixels, as x, y, z (-0.5..0.5), count and colour
	protected float[]				px, py, pz;
	protected int[]					pcount, pcolor;
	protected int					pn;
	protected int					pmax		= 1;
	protected int[]					hist;
	protected int[]					hcolor;

	// the prisms of the segmented channels, as lists of segments of 3D points
	protected List<double[][]>		prisms		= new ArrayList<double[][]> ();
	protected List<Color>			pcolors		= new ArrayList<Color> ();

	protected boolean				cylinder	= true;
	protected String[]				labels		= { "H", "S", "V" };

	// the view
	protected double				yaw			= YAW0;
	protected double				pitch		= PITCH0;
	protected double				zoom		= 1.0;
	protected Point					drag;

	public CPSpacePlot ()
	{
		setBackground (C_BACK);
		setPreferredSize (new Dimension (300, 300));
		setToolTipText ("Drag to turn, wheel to zoom, double click to go back to the first view");

		MouseAdapter	mouse = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)		{ drag = e.getPoint (); }
			public void mouseReleased (MouseEvent e)	{ drag = null; }
			public void mouseDragged (MouseEvent e)
			{
				if (drag == null)		return;
				yaw		+= (e.getX () - drag.x) * 0.01;
				pitch	= Math.max (0.0, Math.min (Math.PI, pitch - (e.getY () - drag.y) * 0.01));
				drag	= e.getPoint ();
				repaint ();
			}
			public void mouseWheelMoved (MouseWheelEvent e)
			{
				zoom	= Math.max (0.3, Math.min (5.0, zoom * Math.pow (1.1, -e.getPreciseWheelRotation ())));
				repaint ();
			}
			public void mouseClicked (MouseEvent e)
			{
				if (e.getClickCount () == 2)	{ yaw = YAW0; pitch = PITCH0; zoom = 1.0; repaint (); }
			}
		};
		addMouseListener (mouse);
		addMouseMotionListener (mouse);
		addMouseWheelListener (mouse);
	}

	/* ------------------------------------------------------------------ */
	/* What is shown                                                       */
	/* ------------------------------------------------------------------ */

	/** Whether the space is drawn as a cylinder (HSV) or as a cube (RGB, YUV). */
	public void setCylindrical (boolean cylinder)
	{
		if (this.cylinder == cylinder)		return;
		this.cylinder	= cylinder;
		repaint ();
	}

	public void setLabels (String xAxisLabel, String yAxisLabel, String zAxisLabel)
	{
		labels	= new String[] { xAxisLabel, yAxisLabel, zAxisLabel };
		repaint ();
	}

	/**
	 * The image, in the space (<code>space</code>: its three components in the
	 * three bytes of each pixel) and in its own colours (<code>colors</code>,
	 * which is what each point is painted in).
	 */
	public void setImage (BufferedImage space, BufferedImage colors)
	{
		int		size = 1 << BITS, shift = 8 - BITS;
		int		n = 0;

		if ((space == null) || (colors == null))		return;
		if (hist == null)		{ hist = new int[size * size * size]; hcolor = new int[size * size * size]; }
		Arrays.fill (hist, 0);

		for (int y = 0; y < space.getHeight (); y += STEP)
			for (int x = 0; x < space.getWidth (); x += STEP)
			{
				int		v = space.getRGB (x, y);
				int		c = ((Pixel.getComponent0 (v) >> shift) * size + (Pixel.getComponent1 (v) >> shift)) * size + (Pixel.getComponent2 (v) >> shift);

				if (hist[c]++ == 0)		n++;
				hcolor[c]	= colors.getRGB (x, y);
			}

		px = new float[n];	py = new float[n];	pz = new float[n];
		pcount = new int[n];	pcolor = new int[n];
		pn		= 0;
		pmax	= 1;
		for (int c = 0; c < hist.length; c++)
			if (hist[c] > 0)
			{
				double[]	p = place (((c / (size * size)) << shift) + (1 << shift) / 2,
									   (((c / size) % size) << shift) + (1 << shift) / 2,
									   ((c % size) << shift) + (1 << shift) / 2);
				px[pn] = (float) p[0];	py[pn] = (float) p[1];	pz[pn] = (float) p[2];
				pcount[pn]	= hist[c];
				pcolor[pn]	= hcolor[c];
				pmax		= Math.max (pmax, hist[c]);
				pn++;
			}
		repaint ();
	}

	/** The prisms of the channels that are segmented (their ColorPrism, in this space). */
	public void setChannels (Channels chs)
	{
		prisms.clear ();
		pcolors.clear ();
		if (chs != null)
			for (int i = 0; i < chs.getNumChannels (); i++)
			{
				Channel		ch = chs.at (i);
				if (ch.segmented && (ch.getCluster () instanceof ColorPrism) && !empty ((ColorPrism) ch.getCluster ()))
				{
					prisms.add (prism ((ColorPrism) ch.getCluster ()));
					pcolors.add ((ch.color != null) ? ch.color : Color.BLACK);
				}
			}
		repaint ();
	}

	/** A prism with no seeds yet: its limits are the wrong way round and it holds nothing. */
	static protected boolean empty (ColorPrism p)
	{
		return (p.getMin0 () > p.getMax0 ()) || (p.getMin1 () > p.getMax1 ()) || (p.getMin2 () > p.getMax2 ());
	}

	/** Forgets the prisms (the space shown has none). */
	public void clearChannels ()
	{
		prisms.clear ();
		pcolors.clear ();
		repaint ();
	}

	/* ------------------------------------------------------------------ */
	/* Geometry                                                            */
	/* ------------------------------------------------------------------ */

	/** Where the components (0..255) of a colour are, in the drawing (-0.5..0.5 each way). */
	protected double[] place (double c0, double c1, double c2)
	{
		if (cylinder)
		{
			double	rho = 0.5 * c1 / 255.0, theta = c0 / 255.0 * 2.0 * Math.PI;
			return new double[] { rho * Math.cos (theta), rho * Math.sin (theta), c2 / 255.0 - 0.5 };
		}
		return new double[] { c0 / 255.0 - 0.5, c1 / 255.0 - 0.5, c2 / 255.0 - 0.5 };
	}

	/** The edges of a prism, as segments {x1, y1, z1, x2, y2, z2}; its sides are arcs in a cylinder. */
	protected double[][] prism (ColorPrism p)
	{
		List<double[]>	segs = new ArrayList<double[]> ();
		double[]		a = { p.getMin0 (), p.getMax0 () }, b = { p.getMin1 (), p.getMax1 () }, c = { p.getMin2 (), p.getMax2 () };

		// the edges along each component, between the corners of the other two
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 2; j++)
			{
				edge (segs, a[0], b[i], c[j], a[1], b[i], c[j]);
				edge (segs, a[i], b[0], c[j], a[i], b[1], c[j]);
				edge (segs, a[i], b[j], c[0], a[i], b[j], c[1]);
			}
		return segs.toArray (new double[0][]);
	}

	/** An edge between two colours, in pieces when it is curved (along the hue of a cylinder). */
	protected void edge (List<double[]> segs, double a0, double b0, double c0, double a1, double b1, double c1)
	{
		int			n = (cylinder && (a0 != a1)) ? ARC : 1;
		double[]	p = place (a0, b0, c0);

		for (int k = 1; k <= n; k++)
		{
			double		t = k / (double) n;
			double[]	q = place (a0 + t * (a1 - a0), b0 + t * (b1 - b0), c0 + t * (c1 - c0));
			segs.add (new double[] { p[0], p[1], p[2], q[0], q[1], q[2] });
			p	= q;
		}
	}

	/* ------------------------------------------------------------------ */
	/* Drawing                                                             */
	/* ------------------------------------------------------------------ */

	// the view: turned by yaw about z, then tilted by pitch about x; x right, y up the screen, depth
	protected double[] view (double x, double y, double z)
	{
		double	cy = Math.cos (yaw), sy = Math.sin (yaw), cp = Math.cos (pitch), sp = Math.sin (pitch);
		double	x1 = x * cy - y * sy, y1 = x * sy + y * cy;
		return new double[] { x1, y1 * cp + z * sp, -y1 * sp + z * cp };
	}

	protected void paintComponent (Graphics g0)
	{
		Graphics2D	g = (Graphics2D) g0.create ();
		double		s = 0.75 * zoom * Math.min (getWidth (), getHeight ());
		double		ox = getWidth () / 2.0, oy = getHeight () / 2.0;
		Integer[]	order;
		double[]	depth;

		super.paintComponent (g0);
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// the frame of the space and its axes
		g.setColor (C_FRAME);
		g.setStroke (new BasicStroke (1f));
		for (double[] e : frame ())		line (g, e, s, ox, oy);
		axisLabels (g, s, ox, oy);

		// the points, the far ones first
		if (pn > 0)
		{
			order	= new Integer[pn];
			depth	= new double[pn];
			for (int i = 0; i < pn; i++)		{ order[i] = i; depth[i] = view (px[i], py[i], pz[i])[2]; }
			final double[]	d = depth;
			Arrays.sort (order, new Comparator<Integer> () { public int compare (Integer a, Integer b) { return Double.compare (d[a], d[b]); } });
			for (int k = 0; k < pn; k++)
			{
				int			i = order[k];
				double[]	v = view (px[i], py[i], pz[i]);
				double		r = 1.5 + 5.0 * Math.log (1.0 + pcount[i]) / Math.log (1.0 + pmax);
				Color		c = new Color (pcolor[i]);

				g.setColor (c);
				g.fill (new Ellipse2D.Double (ox + v[0] * s - r, oy - v[1] * s - r, 2 * r, 2 * r));
				g.setColor (c.darker ());
				g.draw (new Ellipse2D.Double (ox + v[0] * s - r, oy - v[1] * s - r, 2 * r, 2 * r));
			}
		}

		// the prisms of the channels, over the points
		g.setStroke (new BasicStroke (2f));
		for (int i = 0; i < prisms.size (); i++)
		{
			g.setColor (pcolors.get (i));
			for (double[] e : prisms.get (i))		line (g, e, s, ox, oy);
		}
		g.dispose ();
	}

	protected void line (Graphics2D g, double[] e, double s, double ox, double oy)
	{
		double[]	a = view (e[0], e[1], e[2]), b = view (e[3], e[4], e[5]);
		g.draw (new Line2D.Double (ox + a[0] * s, oy - a[1] * s, ox + b[0] * s, oy - b[1] * s));
	}

	/** The edges of the space: a cube, or a cylinder (its two circles, four sides and its axis). */
	protected List<double[]> frame ()
	{
		List<double[]>	f = new ArrayList<double[]> ();

		if (cylinder)
		{
			for (int k = 0; k < 2 * ARC; k++)
			{
				double	t0 = k * Math.PI / ARC, t1 = (k + 1) * Math.PI / ARC;
				for (double z : new double[] { -0.5, 0.5 })
					f.add (new double[] { 0.5 * Math.cos (t0), 0.5 * Math.sin (t0), z, 0.5 * Math.cos (t1), 0.5 * Math.sin (t1), z });
			}
			for (int k = 0; k < 4; k++)
			{
				double	t = k * Math.PI / 2;
				f.add (new double[] { 0.5 * Math.cos (t), 0.5 * Math.sin (t), -0.5, 0.5 * Math.cos (t), 0.5 * Math.sin (t), 0.5 });
			}
			f.add (new double[] { 0, 0, -0.5, 0, 0, 0.5 });
			f.add (new double[] { 0, 0, -0.5, 0.5, 0, -0.5 });			// hue 0, from no saturation to all
		}
		else
			for (int i = 0; i < 2; i++)
				for (int j = 0; j < 2; j++)
				{
					double	a = i - 0.5, b = j - 0.5;
					f.add (new double[] { -0.5, a, b, 0.5, a, b });
					f.add (new double[] { a, -0.5, b, a, 0.5, b });
					f.add (new double[] { a, b, -0.5, a, b, 0.5 });
				}
		return f;
	}

	protected void axisLabels (Graphics2D g, double s, double ox, double oy)
	{
		double[][]	at;

		if (cylinder)		// hue round the bottom, saturation along its radius, value up the axis
			at	= new double[][] { { 0.0, 0.58, -0.5 }, { 0.3, -0.06, -0.5 }, { 0.0, 0.0, 0.58 } };
		else				// at the far end of each axis from the corner of the zeros
			at	= new double[][] { { 0.58, -0.5, -0.5 }, { -0.5, 0.58, -0.5 }, { -0.5, -0.5, 0.58 } };
		g.setColor (C_LABEL);
		g.setFont (getFont ().deriveFont (Font.BOLD));
		for (int i = 0; i < 3; i++)
		{
			double[]	v = view (at[i][0], at[i][1], at[i][2]);
			FontMetrics	fm = g.getFontMetrics ();
			g.drawString (labels[i], (float) (ox + v[0] * s - fm.stringWidth (labels[i]) / 2.0), (float) (oy - v[1] * s + fm.getAscent () / 2.0));
		}
	}
}
