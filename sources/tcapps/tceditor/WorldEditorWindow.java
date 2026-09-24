/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tc.shared.world.World;

/**
 * Stand-alone editor of {@link World} maps (.world files): a window hosting a
 * {@link WorldEditor} with its full menu bar (File, Edit, View, Help).
 *
 * <pre>
 *   java tcapps.tceditor.WorldEditorWindow [file.world]
 * </pre>
 */
public class WorldEditorWindow extends JFrame implements WorldEditor.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= WorldEditor.TITLE;

	protected WorldEditor			editor;

	public WorldEditorWindow ()
	{
		this (WorldEditor.newWorld (), null);
	}

	public WorldEditorWindow (World world, File file)
	{
		super (TITLE);
		editor	= new WorldEditor (world, file, this);
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		setJMenuBar (editor.buildMenuBar (true));
		editorStateChanged (editor);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		pack ();
		setSize (1200, 950);
		setLocationRelativeTo (null);
	}

	public WorldEditor	getEditor ()			{ return editor; }

	public void loadWorld (File f)				{ editor.loadWorld (f); }

	public void editorStateChanged (WorldEditor ed)
	{
		setTitle (TITLE + " - " + ed.getTitle ());
	}

	public void quit ()
	{
		if (!editor.confirmDiscard ())		return;
		editor.dispose ();
		dispose ();
		System.exit (0);
	}

	static public void main (String[] args)
	{
		try
		{
			System.setProperty ("apple.laf.useScreenMenuBar", "false");		// menus inside the window, as in the dialogs
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				WorldEditorWindow	win = new WorldEditorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadWorld (new File (name));
				else					win.getEditor ().getCanvas ().zoomToFit ();
			}
		});
	}
}
