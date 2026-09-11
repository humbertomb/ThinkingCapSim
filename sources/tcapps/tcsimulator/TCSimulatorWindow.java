/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import tc.shared.world.World;
import tcapps.tceditor.StatusBar;
import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tceditor.View3DController;
import tcapps.tceditor.WorldCanvas;
import tcapps.tceditor.WorldEdit;
import tcapps.tceditor.WorldItem;

/**
 * Main window of the new ThinkingCap simulator. It shares the World
 * visualisation with the editor: the 2D view is a read-only
 * {@link WorldCanvas} and the 3D view a {@link View3DController}.
 * Functionality will be added incrementally.
 */
public class TCSimulatorWindow extends JFrame implements WorldCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "ThinkingCap Simulator";
	static public final String		MAPS_DIR	= "./conf/maps";

	protected World					world;
	protected File					file;

	protected WorldCanvas			canvas;
	protected StatusBar				statusBar;
	protected View3DController		view3d;

	public TCSimulatorWindow ()
	{
		super (TITLE);
		world	= WorldEdit.newWorld ();

		buildGUI ();
		updateTitle ();

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		pack ();
		setSize (1200, 800);
		setLocationRelativeTo (null);
	}

	/* ------------------------------------------------------------------ */
	/* GUI construction                                                    */
	/* ------------------------------------------------------------------ */

	private void buildGUI ()
	{
		canvas = new WorldCanvas (world);
		canvas.setEditable (false);
		canvas.setListener (this);

		statusBar	= new StatusBar ();
		view3d		= new View3DController (this, canvas);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (buildToolBar (), BorderLayout.WEST);
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (statusBar, BorderLayout.SOUTH);
		setJMenuBar (buildMenuBar ());
	}

	private JToolBar buildToolBar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		tb.setFloatable (false);

		tb.add (ToolButtons.flatButton (openWorldAction ()));
		tb.addSeparator ();
		tb.add (ToolButtons.flatButton (ToolButtons.zoomFit (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomIn (canvas)));
		tb.add (ToolButtons.flatButton (ToolButtons.zoomOut (canvas)));

		// --- 3D view toggle, at the bottom of the toolbar
		tb.add (Box.createVerticalGlue ());
		tb.addSeparator ();
		tb.add (view3d.button ());
		return tb;
	}

	private Action openWorldAction ()
	{
		Action	a = new AbstractAction ("Load World...", new ToolIcon (ToolIcon.WORLD))
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ loadWorld (); }
		};
		a.putValue (Action.SHORT_DESCRIPTION, "Load world  [Ctrl+O]");
		return a;
	}

	private JMenuBar buildMenuBar ()
	{
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();

		JMenu		mfile = new JMenu ("File");
		JMenuItem	load = new JMenuItem (openWorldAction ());
		load.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, mask));
		mfile.add (load);
		mfile.addSeparator ();
		JMenuItem	quit = new JMenuItem ("Quit");
		quit.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q, mask));
		quit.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ quit (); }
		});
		mfile.add (quit);
		mb.add (mfile);

		JMenu		mview = new JMenu ("View");
		JMenuItem	fit = new JMenuItem (ToolButtons.zoomFit (canvas));
		fit.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_0, mask));
		mview.add (fit);
		mview.add (new JMenuItem (ToolButtons.zoomIn (canvas)));
		mview.add (new JMenuItem (ToolButtons.zoomOut (canvas)));
		mview.addSeparator ();
		mview.add (view3d.menuItem (mask));
		mb.add (mview);

		return mb;
	}

	/* ------------------------------------------------------------------ */
	/* Worlds                                                              */
	/* ------------------------------------------------------------------ */

	public void loadWorld ()
	{
		File		dir = (file != null) ? file.getParentFile () : new File (MAPS_DIR);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setDialogTitle ("Load World");
		fc.setFileFilter (new FileNameExtensionFilter ("World maps (*.world)", "world"));
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadWorld (fc.getSelectedFile ());
	}

	public void loadWorld (File f)
	{
		try
		{
			World	w = new World (f.getPath ());
			world	= w;
			file	= f;
			canvas.setWorld (world);
			canvas.zoomToFit ();
			view3d.worldChanged ();
			updateTitle ();
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public World getWorld ()			{ return world; }
	public WorldCanvas getCanvas ()		{ return canvas; }

	private void updateTitle ()
	{
		setTitle (TITLE + ((file != null) ? " - " + file.getName () : ""));
	}

	public void quit ()
	{
		view3d.dispose ();
		dispose ();
		System.exit (0);
	}

	/* ------------------------------------------------------------------ */
	/* WorldCanvas.Listener                                                */
	/* ------------------------------------------------------------------ */

	public void selectionChanged (WorldItem item)
	{
		view3d.selectionChanged (item);
	}

	public void worldChanged (String what)		{ view3d.worldChanged (); }		// not expected: the canvas is read-only
	public void worldPreview ()					{ view3d.worldPreview (); }
	public void statusChanged (String text)		{ statusBar.setStatus (text); }
	public void usageChanged (String text)		{ statusBar.setUsage (text); }
	public void toolFinished ()					{ canvas.setTool (WorldCanvas.T_SELECT); }
	public void toolRequested (int tool)		{ }

	/* ------------------------------------------------------------------ */

	static public void main (String[] args)
	{
		try
		{
			System.setProperty ("apple.laf.useScreenMenuBar", "true");
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				TCSimulatorWindow	win = new TCSimulatorWindow ();
				win.setVisible (true);
				if (name != null)		win.loadWorld (new File (name));
				else					win.canvas.zoomToFit ();
			}
		});
	}
}
