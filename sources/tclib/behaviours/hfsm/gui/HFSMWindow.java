/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * The window of the editor of machines of states: a {@link HFSMPanel} with its
 * menu bar, in the way of the window of the world editor.
 *
 * <pre>
 *   java tclib.behaviours.hfsm.gui.HFSMWindow [machine.xas]
 * </pre>
 */
public class HFSMWindow extends JFrame implements HFSMPanel.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= HFSMPanel.TITLE;

	protected HFSMPanel				editor;

	public HFSMWindow ()
	{
		this (null);
	}

	public HFSMWindow (File file)
	{
		super (TITLE);

		editor	= new HFSMPanel (HFSMPanel.newMachine (), null, this);
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		setJMenuBar (editor.buildMenuBar ());
		editorStateChanged (editor);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		pack ();
		setSize (1280, 860);
		setLocationRelativeTo (null);
		if (file != null)						editor.load (file);
	}

	public HFSMPanel				getEditor ()		{ return editor; }

	public void load (File f)						{ editor.load (f); }

	public void editorStateChanged (HFSMPanel ed)
	{
		setTitle (TITLE + " - " + ed.getTitle ());
	}

	/** Closes the window, asking about the work in hand when it was changed. */
	public void quit ()
	{
		if (!editor.confirmDiscard ())			return;
		dispose ();
		System.exit (0);
	}

	static public void main (String[] args)
	{
		try
		{
			System.setProperty ("apple.laf.useScreenMenuBar", "false");		// the menus inside the window, as the other editors have them
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;

		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				HFSMWindow	win = new HFSMWindow ((name != null) ? new File (name) : null);

				win.setVisible (true);
			}
		});
	}
}
