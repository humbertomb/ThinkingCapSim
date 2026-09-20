/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import wucore.gui.ChildWindowListener;

/**
 * What the cameras of a robot are taking, as they take it: one view per camera,
 * side by side, of the frame that has just gone into the Linda space.
 *
 * It is the window of a robot running with local graphics, as the window of its
 * motion commands is, and it shows rather than does: whoever renders the frames
 * hands them over with {@link #show}.
 *
 * Over each frame goes what a camera of the trade shows over its own: which
 * camera it is, how many frames a second it takes and how many it is getting,
 * how large a frame is and the shape of it.
 */
public class CameraWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/**
	 * The shapes of a frame that have a name, by how many times wider than tall
	 * they are. A frame comes out of two fields of view and a width in pixels, so
	 * it lands on one of these only by chance: what is shown is the nearest of
	 * them, and said to be near rather than exact unless it is.
	 */
	static private final int[][]	SHAPES	=
	{
		{ 1, 1 }, { 5, 4 }, { 4, 3 }, { 3, 2 }, { 16, 10 }, { 16, 9 }, { 21, 9 },
	};

	/**
	 * The named shape nearest to a frame, as "16:9", with a "~" in front unless the
	 * frame is exactly that shape -- which, coming out of two fields of view and a
	 * width in pixels, it seldom is. A frame taller than wide is said the other way
	 * round ("9:16"), and the nearest is the nearest in ratio: the distance is
	 * taken between the logarithms, so that a shape and its turned-over self are
	 * the same distance away from a square.
	 */
	static public String shapeOf (int w, int h)
	{
		double		r;
		boolean		tall;
		int[]		best = SHAPES[0];
		double		far = Double.MAX_VALUE;

		if ((w <= 0) || (h <= 0))				return "";
		tall	= (h > w);
		r		= tall ? (h / (double) w) : (w / (double) h);
		for (int[] s : SHAPES)
		{
			double	d = Math.abs (Math.log (r) - Math.log (s[0] / (double) s[1]));
			if (d < far)		{ far = d; best = s; }
		}
		int		lo = tall ? h : w, sh = tall ? w : h;				// the long side and the short one
		return ((lo * best[1] == sh * best[0]) ? "" : "~")			// exactly that shape, or near it
				+ (tall ? (best[1] + ":" + best[0]) : (best[0] + ":" + best[1]));
	}

	/** The frame of one camera, drawn to the size of the view, with its readout over it. */
	static protected class View extends JPanel
	{
		private static final long	serialVersionUID = 1L;

		static private final Color	C_TEXT		= new Color (245, 245, 245);
		static private final Color	C_SHADOW	= new Color (0, 0, 0, 170);
		static private final Color	C_OFF		= new Color (255, 190, 120);		// it is not being given what it takes
		static private final double	OFF			= 0.10;								// by more than this much

		protected BufferedImage		frame;
		protected String			title;
		protected double			fps;					// what the camera takes, as its description says
		protected double			got;					// what this view is being given
		protected long				tlast;

		public View (String title, int w, int h, double fps)
		{
			this.title	= title;
			this.fps	= fps;
			setPreferredSize (new Dimension (Math.max (80, w), Math.max (60, h)));
			setBackground (Color.DARK_GRAY);
			setBorder (BorderFactory.createLineBorder (Color.GRAY));
		}

		public void show (BufferedImage frame)
		{
			long		now = System.currentTimeMillis ();

			// how many a second it is being given, smoothed: a single frame that came
			// late is not worth a reading of its own
			if ((tlast > 0) && (now > tlast))
			{
				double	one = 1000.0 / (now - tlast);
				got		= (got > 0.0) ? (0.7 * got + 0.3 * one) : one;
			}
			tlast		= now;
			this.frame	= frame;
			repaint ();
		}

		protected void paintComponent (Graphics g)
		{
			Insets		in = getInsets ();
			int			w = getWidth () - in.left - in.right;
			int			h = getHeight () - in.top - in.bottom;
			double		s;
			int			fw, fh, fx, fy;

			super.paintComponent (g);
			if ((frame == null) || (w <= 0) || (h <= 0))		return;
			// the frame of a camera is not the shape of the view: as large as it fits, centred
			s	= Math.min (w / (double) frame.getWidth (), h / (double) frame.getHeight ());
			fw	= (int) Math.round (frame.getWidth () * s);
			fh	= (int) Math.round (frame.getHeight () * s);
			fx	= in.left + (w - fw) / 2;
			fy	= in.top + (h - fh) / 2;
			g.drawImage (frame, fx, fy, fw, fh, null);
			readout ((Graphics2D) g, fx, fy, fw, fh);
		}

		/** What the camera has to say of itself, over the corners of the frame. */
		protected void readout (Graphics2D g, int fx, int fy, int fw, int fh)
		{
			Font		font = new Font (Font.MONOSPACED, Font.BOLD, Math.max (9, Math.min (13, fh / 14)));
			int			pad = Math.max (4, font.getSize () / 2);
			int			asc;
			String		rate = ((fps == Math.rint (fps)) ? String.valueOf ((int) fps) : String.valueOf ((float) fps)) + " fps";
			boolean		off = (got > 0.0) && (fps > 0.0) && (Math.abs (got - fps) > OFF * fps);

			g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont (font);
			asc		= g.getFontMetrics ().getAscent ();

			label (g, title, fx + pad, fy + pad + asc, C_TEXT);
			label (g, rate, fx + fw - pad - g.getFontMetrics ().stringWidth (rate), fy + pad + asc, C_TEXT);
			// what it is really being given, when that is not what it takes
			if (off)
			{
				String	real = String.format ("%.1f", got);
				label (g, real, fx + fw - pad - g.getFontMetrics ().stringWidth (real), fy + pad + asc + font.getSize () + 2, C_OFF);
			}
			String		size = frame.getWidth () + "x" + frame.getHeight ();
			String		shape = shapeOf (frame.getWidth (), frame.getHeight ());
			label (g, size, fx + pad, fy + fh - pad, C_TEXT);
			label (g, shape, fx + fw - pad - g.getFontMetrics ().stringWidth (shape), fy + fh - pad, C_TEXT);
		}

		/** A word over the frame, with a shadow under it so that it reads over anything. */
		protected void label (Graphics2D g, String text, int x, int y, Color color)
		{
			g.setColor (C_SHADOW);
			g.drawString (text, x + 1, y + 1);
			g.setColor (color);
			g.drawString (text, x, y);
		}
	}

	protected ChildWindowListener	owner;
	protected List<View>			views	= new ArrayList<View> ();

	/**
	 * @param owner  told when the window is closed (may be null)
	 * @param title  title of the window: the robot the cameras are on
	 */
	public CameraWindow (ChildWindowListener owner, String title)
	{
		super (title);

		this.owner	= owner;
		setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane ().setLayout (new BorderLayout ());
		// a row of views that share what the window gives them: each one grows with
		// it and keeps the shape of its frame by drawing it as large as it fits
		getContentPane ().add (new JPanel (new java.awt.GridLayout (1, 0, 8, 8)), BorderLayout.CENTER);
		addWindowListener (new java.awt.event.WindowAdapter ()
		{
			public void windowClosed (java.awt.event.WindowEvent e)
			{
				if (CameraWindow.this.owner != null)		CameraWindow.this.owner.childClosed (CameraWindow.this);
			}
		});
	}

	/**
	 * Adds a view for a camera, of the size of its frames, and returns its number.
	 *
	 * @param fps  how many frames a second the camera takes, for its readout
	 */
	public int add (String title, int w, int h, double fps)
	{
		View		v = new View (title, w, h, fps);

		views.add (v);
		panel ().add (v);
		pack ();
		return views.size () - 1;
	}

	/** Shows a frame just taken by one of the cameras. */
	public void show (final int view, final BufferedImage frame)
	{
		if ((view < 0) || (view >= views.size ()))			return;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ views.get (view).show (frame); }
		});
	}

	/** Opens it, once everything it shows has been added. */
	public void open ()
	{
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ pack (); setVisible (true); }
		});
	}

	public void close ()
	{
		owner	= null;												// closed on purpose: nobody to tell
		dispose ();
	}

	protected JPanel panel ()		{ return (JPanel) getContentPane ().getComponent (0); }
}
