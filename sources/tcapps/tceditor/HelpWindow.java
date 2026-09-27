/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

/**
 * A window showing a page of help written in HTML, rendered by the HTML
 * support of Swing ({@link HTMLEditorKit}) rather than by anything of our own.
 * Whoever opens it hands over the page; this only shows it.
 *
 * One window per window that asks: the help of an editor is one window, put
 * against its top right side, and asking for another page from the same
 * editor (Classes after Language) puts the new page and its title in that
 * window rather than opening a second one. A window that goes away is made
 * again the next time.
 */
public class HelpWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	static public final int			WIDTH	= 860;
	static public final int			HEIGHT	= 700;

	/** The windows open, by the window that asked for them (null for none). */
	static private final Map<Window, HelpWindow>	OPEN = new HashMap<Window, HelpWindow> ();

	protected JEditorPane			view;

	/**
	 * Shows a page of help in the help window of whoever asks: opened against the
	 * top right of its window the first time, and given the new page and title
	 * when it is open already.
	 *
	 * @param owner  component whose window asks for the help (may be null)
	 * @param title  title of the page, which the window takes
	 * @param html   the page itself
	 */
	static public HelpWindow show (Component owner, String title, String html)
	{
		Window		win = (owner != null) ? SwingUtilities.getWindowAncestor (owner) : null;
		HelpWindow	w = OPEN.get (win);

		if ((w == null) || !w.isDisplayable ())
		{
			w	= new HelpWindow (title);
			OPEN.put (win, w);
			w.place (win);
		}
		w.setTitle (title);
		w.setHtml (html);
		w.setVisible (true);
		w.toFront ();
		w.requestFocus ();
		return w;
	}

	protected HelpWindow (String title)
	{
		super (title);
		setDefaultCloseOperation (DISPOSE_ON_CLOSE);

		view	= new JEditorPane ();
		view.setEditorKit (new HTMLEditorKit ());				// the HTML of Swing, with its style sheet
		view.setEditable (false);
		view.putClientProperty (JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.FALSE);
		view.addHyperlinkListener (new HyperlinkListener ()
		{
			public void hyperlinkUpdate (HyperlinkEvent e)
			{
				if (e.getEventType () != HyperlinkEvent.EventType.ACTIVATED)		return;
				follow (e);
			}
		});

		JScrollPane	sp = new JScrollPane (view);
		sp.setVerticalScrollBarPolicy (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (sp, BorderLayout.CENTER);
		setSize (new Dimension (WIDTH, HEIGHT));
	}

	/** The page it shows, from the top. */
	public void setHtml (String html)
	{
		view.setContentType ("text/html");
		view.setText ((html != null) ? html : "");
		view.setCaretPosition (0);
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ view.scrollRectToVisible (new java.awt.Rectangle (0, 0, 1, 1)); }
		});
	}

	public JEditorPane getView ()		{ return view; }

	/**
	 * A link followed: one inside the page scrolls to it, and one out of it is
	 * left to the browser of the desktop when there is one.
	 */
	protected void follow (HyperlinkEvent e)
	{
		String		ref = e.getDescription ();

		if ((ref != null) && ref.startsWith ("#"))		{ view.scrollToReference (ref.substring (1)); return; }
		if (e.getURL () == null)						return;
		try
		{
			if (java.awt.Desktop.isDesktopSupported ())
				java.awt.Desktop.getDesktop ().browse (e.getURL ().toURI ());
		} catch (Throwable t)		{ }						// a page that cannot be opened is not worth a dialog
	}

	/**
	 * Against the top right side of the window of whoever opened it (its top left
	 * on that window's top right corner), as far right as the screen allows when
	 * it does not fit there; centred on the screen when there is no window.
	 */
	protected void place (Window win)
	{
		if (win == null)		{ setLocationRelativeTo (null); return; }

		java.awt.Rectangle	screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment ().getMaximumWindowBounds ();
		int					x = win.getX () + win.getWidth ();
		int					y = win.getY ();

		if ((x + getWidth ()) > (screen.x + screen.width))			x = Math.max (screen.x, screen.x + screen.width - getWidth ());
		if ((y + getHeight ()) > (screen.y + screen.height))		y = Math.max (screen.y, screen.y + screen.height - getHeight ());
		setLocation (x, y);
	}
}
