/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
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
 * One window per title: asking again for a page already open brings it to the
 * front and puts the new text in it, so a menu entry clicked twice does not
 * leave two windows behind.
 */
public class HelpWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	static public final int			WIDTH	= 860;
	static public final int			HEIGHT	= 700;

	/** The windows open, by title. */
	static private final Map<String, HelpWindow>	OPEN = new HashMap<String, HelpWindow> ();

	protected JEditorPane			view;

	/**
	 * Shows a page of help, or brings the one of that title to the front and
	 * replaces what it says.
	 *
	 * @param owner  component whose window the help is placed beside (may be null)
	 * @param title  title of the window, and what tells one page of help from another
	 * @param html   the page itself
	 */
	static public HelpWindow show (Component owner, String title, String html)
	{
		HelpWindow	w = OPEN.get (title);

		if ((w == null) || !w.isDisplayable ())
		{
			w	= new HelpWindow (title);
			OPEN.put (title, w);
			w.place (owner);
		}
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

	/** Beside the window of whoever opened it, and centred on the screen when there is none. */
	protected void place (Component owner)
	{
		Window		win = (owner != null) ? SwingUtilities.getWindowAncestor (owner) : null;

		if (win == null)		{ setLocationRelativeTo (null); return; }
		Point	p = win.getLocation ();
		setLocation (p.x + Math.max (40, (win.getWidth () - getWidth ()) / 2), p.y + 40);
	}
}
