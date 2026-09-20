/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
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
 */
public class CameraWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** The frame of one camera, drawn to the size of the view. */
	static protected class View extends JPanel
	{
		private static final long	serialVersionUID = 1L;

		protected BufferedImage		frame;
		protected String			title;

		public View (String title, int w, int h)
		{
			this.title	= title;
			setPreferredSize (new Dimension (w, h));
			setBackground (Color.DARK_GRAY);
			setBorder (BorderFactory.createTitledBorder (title));
		}

		public void show (BufferedImage frame)
		{
			this.frame	= frame;
			repaint ();
		}

		protected void paintComponent (Graphics g)
		{
			java.awt.Insets		in = getInsets ();
			int					w = getWidth () - in.left - in.right;
			int					h = getHeight () - in.top - in.bottom;

			super.paintComponent (g);
			if ((frame == null) || (w <= 0) || (h <= 0))		return;
			// the frame of a camera is not the shape of the view: as large as it fits, centred
			double	s = Math.min (w / (double) frame.getWidth (), h / (double) frame.getHeight ());
			int		fw = (int) Math.round (frame.getWidth () * s), fh = (int) Math.round (frame.getHeight () * s);
			g.drawImage (frame, in.left + (w - fw) / 2, in.top + (h - fh) / 2, fw, fh, null);
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
		getContentPane ().add (new JPanel (new FlowLayout (FlowLayout.LEFT, 8, 8)), BorderLayout.CENTER);
		addWindowListener (new java.awt.event.WindowAdapter ()
		{
			public void windowClosed (java.awt.event.WindowEvent e)
			{
				if (CameraWindow.this.owner != null)		CameraWindow.this.owner.childClosed (CameraWindow.this);
			}
		});
	}

	/** Adds a view for a camera, of the size of its frames, and returns its number. */
	public int add (String title, int w, int h)
	{
		View		v = new View (title, Math.max (80, w), Math.max (60, h) + 16);

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
