/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tc.vrobot.RobotDef;

/**
 * Stand-alone editor of robot descriptions (.robot files): a window hosting a
 * {@link RobotEditorPanel} with its File menu.
 *
 * <pre>
 *   java tcapps.tceditor.RobotEditorWindow [file.robot]
 * </pre>
 */
public class RobotEditorWindow extends JFrame implements RobotEditorPanel.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= RobotEditorPanel.TITLE;

	protected RobotEditorPanel		editor;

	public RobotEditorWindow ()
	{
		this (RobotDef.create ());
	}

	public RobotEditorWindow (RobotDef robot)
	{
		super (TITLE);
		editor	= new RobotEditorPanel (robot, this);
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		setJMenuBar (editor.buildMenuBar (true));
		robotStateChanged (editor);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		pack ();
		setMinimumSize (new Dimension (760, 520));
		setSize (1040, 780);
		setLocationRelativeTo (null);
	}

	public RobotEditorPanel	getEditor ()				{ return editor; }

	/** Loads a robot description into the editor (asking first about unsaved changes). */
	public void loadRobot (File f)
	{
		if (!editor.confirmDiscard ())			return;
		editor.loadRobot (f);
	}

	public void robotStateChanged (RobotEditorPanel panel)
	{
		setTitle (TITLE + " - " + panel.getTitle ());
	}

	public void quit ()
	{
		if (!editor.confirmDiscard ())			return;
		editor.dispose ();						// closes the 3D view
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
				RobotEditorWindow	win = new RobotEditorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadRobot (new File (name));
			}
		});
	}
}
