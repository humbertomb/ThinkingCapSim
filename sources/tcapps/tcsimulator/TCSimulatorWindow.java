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

import tc.ExecArch;
import tc.shared.world.World;
import tcapps.tceditor.StatusBar;
import tcapps.tceditor.ToolButtons;
import tcapps.tceditor.ToolIcon;
import tcapps.tceditor.View3DController;
import tcapps.tceditor.WorldCanvas;
import tcapps.tceditor.WorldEdit;
import tcapps.tceditor.WorldItem;

/**
 * Main window of the new ThinkingCap simulator. Everything the simulator
 * runs (modules, robot type, world, ...) comes from an architecture
 * definition file (conf/archs/*.arch) managed through {@link ExecArch};
 * the world shown is the one of the architecture's virtual robot. The
 * visualisation is shared with the editor: the 2D view is a read-only
 * {@link WorldCanvas} and the 3D view a {@link View3DController}.
 * Functionality will be added incrementally.
 */
public class TCSimulatorWindow extends JFrame implements WorldCanvas.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "ThinkingCap Simulator";
	static public final String		MAPS_DIR	= "./conf/maps";
	static public final String		ARCHS_DIR	= "./conf/archs";

	protected ExecArch				arch;					// Architecture in use (never null)
	protected World					world;					// World of the architecture's virtual robot
	protected File					worldFile;
	protected boolean				worldModified;			// world changed since the architecture was loaded/saved

	protected WorldCanvas			canvas;
	protected StatusBar				statusBar;
	protected View3DController		view3d;

	public TCSimulatorWindow ()
	{
		super (TITLE);
		arch	= ExecArch.create ();
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

		tb.add (ToolButtons.flatButton (openArchAction ()));
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

	private Action openArchAction ()
	{
		return ToolButtons.action ("Load Architecture...", ToolIcon.FOLDER, "Load architecture  [Ctrl+O]", new Runnable () { public void run () { loadArch (); } });
	}

	private Action openWorldAction ()
	{
		return ToolButtons.action ("Change World...", ToolIcon.WORLD, "Change the world of the architecture  [Ctrl+W]", new Runnable () { public void run () { loadWorld (); } });
	}

	private JMenuBar buildMenuBar ()
	{
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();
		JMenuBar	mb = new JMenuBar ();

		JMenu		mfile = new JMenu ("File");
		mfile.add (item ("New Architecture", KeyEvent.VK_N, mask, new Runnable () { public void run () { newArch (); } }));
		JMenuItem	load = new JMenuItem (openArchAction ());
		load.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, mask));
		mfile.add (load);
		mfile.add (item ("Save Architecture", KeyEvent.VK_S, mask, new Runnable () { public void run () { saveArch (false); } }));
		mfile.add (item ("Save Architecture As...", KeyEvent.VK_S, mask | KeyEvent.SHIFT_DOWN_MASK, new Runnable () { public void run () { saveArch (true); } }));
		mfile.addSeparator ();
		JMenuItem	wld = new JMenuItem (openWorldAction ());
		wld.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W, mask));
		mfile.add (wld);
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

	private JMenuItem item (String name, int key, int mask, final Runnable body)
	{
		JMenuItem	mi = new JMenuItem (name);
		mi.setAccelerator (KeyStroke.getKeyStroke (key, mask));
		mi.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ body.run (); }
		});
		return mi;
	}

	private JFileChooser chooser (File current, String defDir, String ext, String desc)
	{
		File		dir = (current != null) ? current.getParentFile () : new File (defDir);
		if ((dir == null) || !dir.isDirectory ())		dir = new File (".");
		JFileChooser	fc = new JFileChooser (dir);
		fc.setFileFilter (new FileNameExtensionFilter (desc, ext));
		return fc;
	}

	/* ------------------------------------------------------------------ */
	/* Architectures                                                       */
	/* ------------------------------------------------------------------ */

	public ExecArch getArch ()			{ return arch; }

	private boolean isModified ()		{ return worldModified || arch.isModified (); }

	/** Asks what to do with unsaved changes; false when the user cancels. */
	private boolean confirmDiscard ()
	{
		if (!isModified ())				return true;
		int		r = JOptionPane.showConfirmDialog (this, "The architecture has unsaved changes. Save them first?",
					TITLE, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (r == JOptionPane.CANCEL_OPTION)	return false;
		if (r == JOptionPane.YES_OPTION)		return saveArch (false);
		return true;
	}

	public void newArch ()
	{
		if (!confirmDiscard ())			return;
		setArch (ExecArch.create ());
	}

	public void loadArch ()
	{
		if (!confirmDiscard ())			return;
		JFileChooser	fc = chooser (arch.getFile (), ARCHS_DIR, "arch", "Architecture definition files (*.arch)");
		fc.setDialogTitle ("Load Architecture");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadArch (fc.getSelectedFile ());
	}

	public void loadArch (File f)
	{
		try
		{
			setArch (ExecArch.load (f));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Installs an architecture and shows the world of its virtual robot. */
	private void setArch (ExecArch a)
	{
		arch			= a;
		worldModified	= false;
		String	wname = arch.getWorldFile ();
		if (wname != null)		showWorld (new File (wname));
		else					showWorld (null);
		updateTitle ();
	}

	public boolean saveArch (boolean saveAs)
	{
		File	f = arch.getFile ();
		if (saveAs || (f == null))
		{
			JFileChooser	fc = chooser (f, ARCHS_DIR, "arch", "Architecture definition files (*.arch)");
			fc.setDialogTitle (saveAs ? "Save Architecture As" : "Save Architecture");
			if (f != null)		fc.setSelectedFile (f);
			if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;
			f = fc.getSelectedFile ();
			if (!f.getName ().toLowerCase ().endsWith (".arch"))		f = new File (f.getPath () + ".arch");
			if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " already exists. Overwrite?", TITLE,
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))		return false;
		}
		try
		{
			arch.save (f);
			worldModified	= false;
			updateTitle ();
			return true;
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot save " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/* ------------------------------------------------------------------ */
	/* Worlds                                                              */
	/* ------------------------------------------------------------------ */

	/** Changes the world of the architecture's virtual robot (VROBOT+"WORLD" property). */
	public void loadWorld ()
	{
		JFileChooser	fc = chooser (worldFile, MAPS_DIR, "world", "World maps (*.world)");
		fc.setDialogTitle ("Change World");
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		loadWorld (fc.getSelectedFile ());
	}

	public void loadWorld (File f)
	{
		if (!showWorld (f))				return;
		arch.setWorldFile (relativePath (f));
		worldModified	= true;
		updateTitle ();
	}

	/** Loads and displays a world (null: empty world); false if the file cannot be read. */
	private boolean showWorld (File f)
	{
		World	w;
		if (f == null)
			w = WorldEdit.newWorld ();
		else
		{
			try
			{
				w = new World (f.getPath ());
			} catch (Exception e)
			{
				e.printStackTrace ();
				JOptionPane.showMessageDialog (this, "Cannot load world " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		world		= w;
		worldFile	= f;
		canvas.setWorld (world);
		canvas.zoomToFit ();
		view3d.worldChanged ();
		return true;
	}

	/** Path relative to the working directory when possible (as used in the .arch files), with '/' separators. */
	static private String relativePath (File f)
	{
		String	path = f.getAbsolutePath ();
		String	base = new File (".").getAbsoluteFile ().getParentFile ().getPath ();
		if (path.startsWith (base + File.separator))		path = path.substring (base.length () + 1);
		return path.replace (File.separatorChar, '/');
	}

	public World getWorld ()			{ return world; }
	public WorldCanvas getCanvas ()		{ return canvas; }

	private void updateTitle ()
	{
		File	f = arch.getFile ();
		setTitle (TITLE + " - " + ((f != null) ? f.getName () : "untitled.arch") + (isModified () ? " *" : ""));
	}

	public void quit ()
	{
		if (!confirmDiscard ())			return;
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
				if (name != null)		win.loadArch (new File (name));
				else					win.canvas.zoomToFit ();
			}
		});
	}
}
