/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;

/**
 * A pane to write Lua in: the text with its syntax coloured, and the numbers of
 * the lines beside it, which are drawn and not written, so there is no way to
 * type on them.
 *
 * It says nothing about what it holds: whoever uses it gives it a listener and
 * is told when the text changed.
 */
public class CodeEditor extends JPanel
{
	private static final long		serialVersionUID = 1L;

	/** Told whenever the text of the pane changed. */
	public interface Listener
	{
		public void codeChanged (CodeEditor editor);
	}

	static public final Font		FONT		= new Font (Font.MONOSPACED, Font.PLAIN, 12);

	static private final Color		C_BACK		= Color.WHITE;
	static private final Color		C_GUTTER	= new Color (240, 240, 240);
	static private final Color		C_GUTTERN	= new Color (130, 130, 130);
	static private final Color		C_GUTTERL	= new Color (205, 205, 205);
	static private final Color		C_OFF		= new Color (246, 246, 246);		// nothing to write in

	protected JTextPane				text;
	protected Gutter				gutter;
	protected JScrollPane			scroll;
	protected LuaHighlighter		lua;
	protected Listener				listener;
	protected boolean				quiet;						// setting the text from the outside is no change
	protected javax.swing.undo.UndoManager	undos = new javax.swing.undo.UndoManager ();

	public CodeEditor (String title)
	{
		super (new BorderLayout ());

		lua		= new LuaHighlighter ();
		text	= new JTextPane ()													// code is not wrapped: long lines scroll
		{
			private static final long	serialVersionUID = 1L;

			public boolean getScrollableTracksViewportWidth ()
			{
				java.awt.Container	parent = getParent ();

				return (parent == null) || (getUI ().getPreferredSize (this).width <= parent.getSize ().width);
			}
		};
		text.setFont (FONT);
		text.setBackground (C_BACK);
		text.setBorder (BorderFactory.createEmptyBorder (2, 4, 2, 4));
		text.getDocument ().addDocumentListener (new DocumentListener ()
		{
			public void insertUpdate (DocumentEvent e)		{ changed (); }
			public void removeUpdate (DocumentEvent e)		{ changed (); }
			public void changedUpdate (DocumentEvent e)		{ }
		});

		// what was typed can be taken back; the colouring, which is a change of
		// attributes and not of the text, is not something anybody undoes
		text.getDocument ().addUndoableEditListener (new javax.swing.event.UndoableEditListener ()
		{
			public void undoableEditHappened (javax.swing.event.UndoableEditEvent e)
			{
				javax.swing.undo.UndoableEdit	ed = e.getEdit ();

				if (ed instanceof javax.swing.text.AbstractDocument.DefaultDocumentEvent)
					if (((javax.swing.text.AbstractDocument.DefaultDocumentEvent) ed).getType ()
						== javax.swing.event.DocumentEvent.EventType.CHANGE)		return;
				undos.addEdit (ed);
			}
		});

		gutter	= new Gutter ();
		scroll	= new JScrollPane (text);
		scroll.setRowHeaderView (gutter);
		scroll.setBorder (BorderFactory.createTitledBorder (title));
		add (scroll, BorderLayout.CENTER);
		setPreferredSize (new Dimension (420, 260));
	}

	public void setListener (Listener l)				{ listener = l; }
	public JTextPane getTextPane ()						{ return text; }

	/** What is written, never null. */
	public String getCode ()
	{
		try { return text.getDocument ().getText (0, text.getDocument ().getLength ()); }
		catch (BadLocationException e) { return ""; }
	}

	/** Whether what was typed can be taken back, and put back again. */
	public boolean canUndo ()							{ return undos.canUndo (); }
	public boolean canRedo ()							{ return undos.canRedo (); }

	/** Takes back the last thing typed, if there is one. */
	public void undo ()
	{
		try { if (undos.canUndo ())		undos.undo (); }
		catch (javax.swing.undo.CannotUndoException e) { }
	}

	/** Puts back what was taken back, if there is any. */
	public void redo ()
	{
		try { if (undos.canRedo ())		undos.redo (); }
		catch (javax.swing.undo.CannotRedoException e) { }
	}

	/** Puts a text in without telling the listener it changed. */
	public void setCode (String code)
	{
		quiet	= true;
		text.setText ((code != null) ? code : "");
		text.setCaretPosition (0);
		quiet	= false;
		undos.discardAllEdits ();					// a text put in is not something that was typed
		colour ();
		gutter.refresh ();
	}

	/** Whether there is anything to write in: an empty pane is greyed out. */
	public void setWritable (boolean b)
	{
		text.setEditable (b);
		text.setBackground (b ? C_BACK : C_OFF);
		gutter.refresh ();
	}

	public boolean isWritable ()						{ return text.isEditable (); }

	private void changed ()
	{
		gutter.refresh ();
		SwingUtilities.invokeLater (new Runnable ()			// the document cannot be styled while it is being changed
		{
			public void run ()		{ colour (); }
		});
		if (!quiet && (listener != null))				listener.codeChanged (this);
	}

	/**
	 * Puts the caret on a line (the first one is 1) and marks the whole of it, so
	 * that whoever found something wrong there can point at it.
	 */
	public void goToLine (int line)
	{
		javax.swing.text.Element	root = text.getDocument ().getDefaultRootElement ();

		if ((line < 1) || (root.getElementCount () == 0))	return;

		javax.swing.text.Element	el = root.getElement (Math.min (line - 1, root.getElementCount () - 1));

		text.setCaretPosition (el.getStartOffset ());
		text.moveCaretPosition (Math.max (el.getStartOffset (), el.getEndOffset () - 1));
		text.requestFocusInWindow ();
	}

	/** Which line the caret is on (the first one is 1). */
	public int line ()						{ return caret ()[0]; }

	/** How many lines there are. */
	public int lines ()						{ return text.getDocument ().getDefaultRootElement ().getElementCount (); }

	/** Colours what is written, the caret left where it was. */
	public void colour ()
	{
		if (!(text.getDocument () instanceof StyledDocument))		return;
		lua.apply ((StyledDocument) text.getDocument (), getCode ());
	}

	/* ------------------------------------------------------------------ */
	/* The numbers of the lines                                            */
	/* ------------------------------------------------------------------ */

	/** The numbers of the lines, drawn beside the text: they cannot be written on. */
	protected class Gutter extends JPanel
	{
		private static final long	serialVersionUID = 1L;

		protected int				digits = 2;

		Gutter ()
		{
			setBackground (C_GUTTER);
			setFont (FONT);
			refresh ();
		}

		void refresh ()
		{
			int		lines = text.getDocument ().getDefaultRootElement ().getElementCount ();
			int		d = Math.max (2, Integer.toString (Math.max (1, lines)).length ());

			if (d != digits)
			{
				digits	= d;
				setPreferredSize (new Dimension (10 + d * getFontMetrics (FONT).charWidth ('0'), 10));
				revalidate ();
			}
			repaint ();
		}

		public Dimension getPreferredSize ()
		{
			return new Dimension (10 + digits * getFontMetrics (FONT).charWidth ('0'), Math.max (text.getHeight (), 10));
		}

		protected void paintComponent (Graphics g0)
		{
			super.paintComponent (g0);

			Graphics2D	g = (Graphics2D) g0;

			g.setRenderingHint (RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor (C_GUTTERL);
			g.drawLine (getWidth () - 1, 0, getWidth () - 1, getHeight ());
			g.setFont (FONT);
			g.setColor (C_GUTTERN);

			javax.swing.text.Element	root = text.getDocument ().getDefaultRootElement ();
			java.awt.FontMetrics		fm = g.getFontMetrics ();
			java.awt.Rectangle			clip = g.getClipBounds ();

			for (int i = 0; i < root.getElementCount (); i++)
			{
				try
				{
					java.awt.Rectangle	r = text.modelToView2D (root.getElement (i).getStartOffset ()).getBounds ();

					if (r == null)						continue;
					if ((r.y + r.height) < clip.y)		continue;
					if (r.y > (clip.y + clip.height))	break;

					String	num = Integer.toString (i + 1);

					g.drawString (num, getWidth () - 5 - fm.stringWidth (num), r.y + r.height - fm.getDescent ());
				}
				catch (Exception e) { }
			}
		}
	}

	/** Where the caret is, as line and column, for a status bar. */
	public int[] caret ()
	{
		int			pos = text.getCaretPosition ();
		int			line = text.getDocument ().getDefaultRootElement ().getElementIndex (pos);
		int			col = 0;

		try { col = pos - Utilities.getRowStart (text, pos); } catch (Exception e) { }
		return new int[] { line + 1, col + 1 };
	}
}
