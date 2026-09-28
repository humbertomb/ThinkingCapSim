/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import tclib.behaviours.hfsm.HFSMJson;
import tclib.behaviours.hfsm.MetaState;
import tclib.behaviours.hfsm.State;
import tclib.behaviours.hfsm.Transition;
import tclib.behaviours.hfsm.XMLParser;
import tclib.behaviours.hfsm.XMLWriter;
import tclib.behaviours.lua.gui.CodeEditor;
import tclib.behaviours.lua.gui.LuaHelp;

/**
 * The editor of a machine of hierarchical states: the diagram of one level in
 * the middle, the tools on the left, and on the right the two scripts of
 * whatever is selected -- the test of a transition above and what it does
 * below, which for a state is the only one it has.
 *
 * It is the world editor of the simulator in small: the same tools, the same
 * status bar, and the file it works on kept in the title of whoever hosts it.
 */
public class HFSMPanel extends JPanel implements HFSMCanvas.Listener, CodeEditor.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "HFSM Editor";

	/** What a machine is kept as, and where. */
	static public final String		SUFFIX		= HFSMJson.SUFFIX;
	static public final String		FOLDER		= HFSMJson.FOLDER;

	/** What the machines of the Chaos editor are kept as. */
	static public final String		CHAOS_SUFFIX	= ".xas";

	/** What the window hosting the editor needs to know. */
	public interface Host
	{
		/** The file or the modified mark changed. */
		public void editorStateChanged (HFSMPanel editor);
	}

	/** Whoever wants to know when a machine is written to a file (the monitor, which has it read again). */
	public interface Saved
	{
		public void saved (File f);
	}

	protected Saved					onSave;
	protected JMenuItem				newItem, loadItem, importItem;	// File > New / Load / Import, which an editor of one file has not
	protected boolean				oneFile;					// the machine being run, and no other: New, Load and Import are out

	public void setOnSave (Saved s)					{ onSave = s; }
	/**
	 * Whether the editor is on one file and no other (the machine a robot runs,
	 * opened from its monitor): File > New, Load and Import Chaos HFSM are then
	 * disabled, as the machine the robot runs is not to be swapped from here.
	 */
	public void setOneFile (boolean b)
	{
		oneFile	= b;
		if (newItem != null)					newItem.setEnabled (!b);
		if (loadItem != null)					loadItem.setEnabled (!b);
		if (importItem != null)					importItem.setEnabled (!b);
	}
	public boolean isOneFile ()						{ return oneFile; }

	/* Model */
	protected MetaState				root;
	protected File					file;						// the .hfsm it came from, or null
	protected List<XMLParser.PrivateVar>	vars = new ArrayList<XMLParser.PrivateVar> ();

	/**
	 * A machine set aside while an extern meta state of it is edited in its own
	 * file: everything the panel holds of it, and where the editing was.
	 */
	protected static class Outer
	{
		MetaState						root;
		File							file;
		List<XMLParser.PrivateVar>		vars;
		String							behpath;
		boolean							dirty;
		MetaState						level;			// the level being shown, which holds the extern meta state
		String							path;			// where the extern meta state is, as root . meta . meta
	}

	/** The machines set aside, the one this was opened from on top (empty at the top machine). */
	protected java.util.ArrayDeque<Outer>	outers = new java.util.ArrayDeque<Outer> ();
	protected String				behpath;					// where the behaviours the states name are (kept in the file); null: not said
	protected boolean				dirty;

	/* GUI */
	protected HFSMCanvas			canvas;
	protected CodeEditor			testCode;
	protected CodeEditor			actionCode;
	protected JLabel				selLabel;
	protected JLabel				statusLabel;
	protected JLabel				usageLabel;
	protected JLabel				pathLabel;					// where the Lua behaviours are read from, on the top bar
	protected JToggleButton[]		toolButtons	= new JToggleButton[HFSMCanvas.NTOOLS];
	protected Action				deleteAction, renameAction, initialAction, upAction, expandAction;
	protected Host					host;

	public HFSMPanel ()
	{
		this (newMachine (), null, null);
	}

	public HFSMPanel (MetaState root, File file, Host host)
	{
		super (new BorderLayout ());

		this.root	= root;
		this.file	= file;
		this.host	= host;

		build ();
		refresh ();
	}

	/** An empty machine, as File / New leaves it. */
	static public MetaState newMachine ()
	{
		return new MetaState ("untitled", 0);
	}

	public MetaState				getMachine ()		{ return root; }
	public File						getFile ()			{ return file; }
	public boolean					isDirty ()			{ return dirty; }
	public HFSMCanvas				getCanvas ()		{ return canvas; }

	/** The title of the window: the file (or untitled) and the modified mark. */
	public String getTitle ()
	{
		return ((file == null) ? ("untitled" + SUFFIX) : file.getName ()) + (dirty ? " *" : "");
	}

	/* ------------------------------------------------------------------ */
	/* Building it                                                         */
	/* ------------------------------------------------------------------ */

	private void build ()
	{
		JPanel		bottom = statusBar ();			// built first: the canvas talks to it as soon as it has a listener

		canvas		= new HFSMCanvas (root);

		testCode	= new CodeEditor ("Test Code");
		actionCode	= new CodeEditor ("Action Code");
		testCode.setListener (this);
		actionCode.setListener (this);

		JSplitPane	codes = new JSplitPane (JSplitPane.VERTICAL_SPLIT, testCode, actionCode);

		codes.setResizeWeight (0.5);
		codes.setPreferredSize (new Dimension (440, 700));

		selLabel	= new JLabel (" ");
		selLabel.setBorder (BorderFactory.createEmptyBorder (2, 6, 2, 6));

		JPanel		right = new JPanel (new BorderLayout ());

		right.add (selLabel, BorderLayout.NORTH);
		right.add (codes, BorderLayout.CENTER);

		JSplitPane	centre = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, canvas, right);

		centre.setResizeWeight (1.0);

		add (toolbar (), BorderLayout.WEST);
		add (topbar (), BorderLayout.NORTH);
		add (centre, BorderLayout.CENTER);
		add (bottom, BorderLayout.SOUTH);
		canvas.setListener (this);					// everything it talks to is there now
	}

	private JToolBar toolbar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.VERTICAL);
		ButtonGroup	group = new ButtonGroup ();

		tb.setFloatable (false);

		tool (tb, group, HFSMCanvas.T_SELECT,	HFSMIcon.SELECT,	"Select",					"S");
		tool (tb, group, HFSMCanvas.T_PAN,		HFSMIcon.PAN,		"Pan",						"H");
		tb.addSeparator ();
		tool (tb, group, HFSMCanvas.T_STATE,	HFSMIcon.STATE,		"State",					"T");
		tool (tb, group, HFSMCanvas.T_META,		HFSMIcon.METASTATE,	"Meta state",				"M");
		tool (tb, group, HFSMCanvas.T_TRANS,	HFSMIcon.TRANSITION, "Transition (drag from one state to another)",	"N");
		tool (tb, group, HFSMCanvas.T_LINK,		HFSMIcon.LINK,		"Join blocks (drag one onto another)",			"J");
		tb.addSeparator ();

		initialAction	= action ("Initial state", HFSMIcon.INITIAL, "Initial state of this level  [I]", new Runnable ()
		{
			public void run ()		{ canvas.setInitial (); }
		});
		upAction		= action ("Up", HFSMIcon.UP, "Out of this meta state  [Alt+Up]", new Runnable ()
		{
			public void run ()		{ if (canvas.canGoUp ())	canvas.levelUp ();		else	leaveExtern (); }
		});
		deleteAction	= action ("Delete", HFSMIcon.DELETE, "Delete  [Del]", new Runnable ()
		{
			public void run ()		{ canvas.deleteSelection (); }
		});
		tb.add (flat (initialAction));
		tb.add (flat (upAction));
		tb.add (flat (deleteAction));
		tb.addSeparator ();
		tb.add (flat (action ("Zoom to fit", HFSMIcon.ZOOM_FIT, "Zoom to fit  [Ctrl+0]", new Runnable ()
		{
			public void run ()		{ canvas.zoomToFit (); }
		})));
		tb.add (flat (action ("Zoom in", HFSMIcon.ZOOM_IN, "Zoom in", new Runnable ()
		{
			public void run ()		{ canvas.zoom (1.25); }
		})));
		tb.add (flat (action ("Zoom out", HFSMIcon.ZOOM_OUT, "Zoom out", new Runnable ()
		{
			public void run ()		{ canvas.zoom (0.8); }
		})));

		renameAction	= action ("Rename", HFSMIcon.SELECT, "Rename  [F2]", new Runnable ()
		{
			public void run ()		{ canvas.rename (canvas.getSelection ()); }
		});
		expandAction	= action ("Expand meta state", HFSMIcon.UP, "What the meta state holds comes out to this level, and it goes", new Runnable ()
		{
			public void run ()		{ canvas.expandSelection (); }
		});

		// the letters of the tools, with the diagram focused
		key ("S", HFSMCanvas.T_SELECT);		key ("H", HFSMCanvas.T_PAN);		key ("T", HFSMCanvas.T_STATE);
		key ("M", HFSMCanvas.T_META);		key ("N", HFSMCanvas.T_TRANS);		key ("J", HFSMCanvas.T_LINK);
		bind (KeyStroke.getKeyStroke (KeyEvent.VK_I, 0), "initial", initialAction);
		bind (KeyStroke.getKeyStroke (KeyEvent.VK_0, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ()), "fit",
			  action ("fit", HFSMIcon.ZOOM_FIT, null, new Runnable () { public void run () { canvas.zoomToFit (); } }));

		return tb;
	}

	/**
	 * The bar over the diagram: the folder the Lua behaviours the states name are
	 * read from, on its right side (the machine's own, or where it lives when it
	 * says none). Edit > Default path... changes it.
	 */
	private JToolBar topbar ()
	{
		JToolBar	tb = new JToolBar (JToolBar.HORIZONTAL);

		tb.setFloatable (false);
		pathLabel	= new JLabel (" ");
		pathLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		pathLabel.setToolTipText ("Where the Lua behaviours the states name are read from (Edit > Default path...)");
		tb.add (javax.swing.Box.createHorizontalGlue ());
		tb.add (pathLabel);
		return tb;
	}

	/** The path of the Lua behaviours as the top bar shows it. */
	protected void refreshPath ()
	{
		if (pathLabel == null)					return;

		File		dir = behavioursFolder ();
		String		txt = (behpath != null) ? behpath : ((dir != null) ? (relative (dir) + "  (where the machine lives)") : "(not set)");

		pathLabel.setText ("LUA Path  " + txt);
	}

	private JPanel statusBar ()
	{
		JPanel		bar = new JPanel (new BorderLayout ());

		statusLabel	= new JLabel (" ");
		statusLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		usageLabel	= new JLabel (" ", JLabel.RIGHT);
		usageLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		usageLabel.setForeground (new Color (70, 70, 70));
		bar.add (statusLabel, BorderLayout.WEST);
		bar.add (usageLabel, BorderLayout.CENTER);
		return bar;
	}

	private void tool (JToolBar tb, ButtonGroup group, final int tool, int icon, String tip, String letter)
	{
		JToggleButton	b = new JToggleButton (new HFSMIcon (icon));

		b.setToolTipText (tip + "  [" + letter + "]");
		b.setFocusPainted (false);
		b.setBorderPainted (false);
		b.setContentAreaFilled (false);
		b.setOpaque (false);
		b.addActionListener (new java.awt.event.ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.setTool (tool); }
		});
		group.add (b);
		tb.add (b);
		toolButtons[tool]	= b;
		if (tool == HFSMCanvas.T_SELECT)		b.setSelected (true);
	}

	private Action action (String name, int icon, String tip, final Runnable what)
	{
		Action		a = new AbstractAction (name, new HFSMIcon (icon))
		{
			private static final long	serialVersionUID = 1L;

			public void actionPerformed (ActionEvent e)		{ what.run (); }
		};

		if (tip != null)		a.putValue (Action.SHORT_DESCRIPTION, tip);
		return a;
	}

	static private JButton flat (Action a)
	{
		JButton		b = new JButton (a);

		b.setHideActionText (true);
		b.setFocusPainted (false);
		b.setBorderPainted (false);
		b.setContentAreaFilled (false);
		b.setOpaque (false);
		return b;
	}

	private void key (final String letter, final int tool)
	{
		bind (KeyStroke.getKeyStroke (letter.toLowerCase ().charAt (0)), "tool" + tool,
			  action (letter, HFSMIcon.SELECT, null, new Runnable ()
		{
			public void run ()		{ canvas.setTool (tool);	toolButtons[tool].setSelected (true); }
		}));
	}

	/**
	 * A key of the diagram, taken only while the diagram has the focus: bound to
	 * the panel, for any focused child, the I of the initial state fired while a
	 * script was being typed (the refresh it brings put the caret at the start of
	 * the line, and the letter went there), and the letters of the tools changed
	 * the tool under the typist's fingers.
	 */
	private void bind (KeyStroke key, String name, Action a)
	{
		canvas.getInputMap (JComponent.WHEN_FOCUSED).put (key, name);
		canvas.getActionMap ().put (name, a);
	}

	/* ------------------------------------------------------------------ */
	/* The menus                                                           */
	/* ------------------------------------------------------------------ */

	/** The menu bar of the editor, for the window that hosts it. */
	public JMenuBar buildMenuBar ()
	{
		JMenuBar	bar = new JMenuBar ();
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();

		JMenu		file = new JMenu ("File");

		file.add (newItem = item ("New State Machine", KeyStroke.getKeyStroke (KeyEvent.VK_N, mask), new Runnable ()
		{
			public void run ()		{ newFile (); }
		}));
		file.add (loadItem = item ("Load State Machine", KeyStroke.getKeyStroke (KeyEvent.VK_O, mask), new Runnable ()
		{
			public void run ()		{ load (); }
		}));
		file.add (item ("Save State Machine", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask), new Runnable ()
		{
			public void run ()		{ save (); }
		}));
		file.add (item ("Save as State Machine", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), new Runnable ()
		{
			public void run ()		{ saveAs (); }
		}));
		file.addSeparator ();
		file.add (importItem = item ("Import Chaos HFSM", null, new Runnable ()
		{
			public void run ()		{ importChaos (); }
		}));
		setOneFile (oneFile);								// New, Load and Import as the mode says
		file.addSeparator ();
		file.add (item ("Close", null, new Runnable ()
		{
			public void run ()		{ close (); }
		}));
		bar.add (file);

		JMenu		edit = new JMenu ("Edit");

		edit.add (new JMenuItem (renameAction));
		edit.add (new JMenuItem (initialAction));
		edit.add (new JMenuItem (deleteAction));
		edit.add (new JMenuItem (expandAction));
		edit.addSeparator ();
		edit.add (item ("Priority of the transition...", null, new Runnable ()
		{
			public void run ()		{ priority (); }
		}));
		edit.addSeparator ();
		edit.add (item ("Default path...", null, new Runnable ()
		{
			public void run ()		{ defaultPath (); }
		}));
		edit.add (item ("Check the machine", null, new Runnable ()
		{
			public void run ()		{ check (); }
		}));
		bar.add (edit);

		JMenu		view = new JMenu ("View");

		view.add (new JMenuItem (upAction));
		view.add (item ("Zoom to fit", KeyStroke.getKeyStroke (KeyEvent.VK_0, mask), new Runnable ()
		{
			public void run ()		{ canvas.zoomToFit (); }
		}));
		bar.add (view);

		// the scripts of the states are Lua, and speak to the robot as the programs do
		JMenu		help = new JMenu ("Help");

		help.add (item ("Language", null, new Runnable ()
		{
			public void run ()		{ tcapps.tceditor.HelpWindow.show (HFSMPanel.this, LuaHelp.LANGUAGE, LuaHelp.language ()); }
		}));
		help.add (item ("Classes", null, new Runnable ()
		{
			public void run ()		{ tcapps.tceditor.HelpWindow.show (HFSMPanel.this, LuaHelp.CLASSES, LuaHelp.classes ()); }
		}));
		bar.add (help);

		return bar;
	}

	/* ------------------------------------------------------------------ */
	/* Where the behaviours are                                            */
	/* ------------------------------------------------------------------ */

	/** Where the behaviours the states name are read from, as the file says it (null: it does not say). */
	public String behpath ()								{ return behpath; }

	/**
	 * Asks for the folder the behaviours the states name are read from (the Lua
	 * programs of <code>chaos.setBehavior</code>), and keeps it with the machine.
	 * A folder under the working directory is kept relative to it, as the rest of
	 * the paths of the configuration are, so the file goes from one machine to
	 * another.
	 */
	public void defaultPath ()
	{
		JFileChooser	fc = new JFileChooser ();
		File			now = behavioursFolder ();

		fc.setDialogTitle ("Where the behaviours of the states are");
		fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY);
		fc.setAcceptAllFileFilterUsed (false);
		fc.setApproveButtonText ("Choose folder");
		if ((now != null) && now.exists ())		fc.setCurrentDirectory (now);
		else									fc.setCurrentDirectory (new File ("."));
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;

		File		chosen = fc.getSelectedFile ();

		if ((chosen == null) || !chosen.isDirectory ())		return;
		behpath	= relative (chosen);
		dirty	= true;
		refresh ();
		status ("The behaviours are read from " + behpath);
	}

	/** The folder the behaviours are in now: what the machine says, else where it lives, else nothing. */
	protected File behavioursFolder ()
	{
		if (behpath != null)						return new File (behpath);
		if ((file != null) && (file.getAbsoluteFile ().getParentFile () != null))		return file.getAbsoluteFile ().getParentFile ();
		return null;
	}

	/** A folder or a file as it is written in the file: under the working directory, as ./..., and otherwise as it is. */
	static protected String relative (File dir)
	{
		try
		{
			java.nio.file.Path	cwd = new File (".").getCanonicalFile ().toPath ();
			java.nio.file.Path	p = dir.getCanonicalFile ().toPath ();

			if (p.startsWith (cwd))
			{
				String	rel = cwd.relativize (p).toString ().replace (File.separatorChar, '/');

				return (rel.length () == 0) ? "." : ("./" + rel);
			}
			return p.toString ();
		}
		catch (Exception e)
		{
			return dir.getPath ();
		}
	}

	private JMenuItem item (String name, KeyStroke key, final Runnable what)
	{
		JMenuItem	mi = new JMenuItem (new AbstractAction (name)
		{
			private static final long	serialVersionUID = 1L;

			public void actionPerformed (ActionEvent e)		{ what.run (); }
		});

		if (key != null)		mi.setAccelerator (key);
		return mi;
	}

	/* ------------------------------------------------------------------ */
	/* The files                                                           */
	/* ------------------------------------------------------------------ */

	public void newFile ()
	{
		if (!leaveAll ())						return;
		root	= newMachine ();
		file	= null;
		vars	= new ArrayList<XMLParser.PrivateVar> ();
		behpath	= null;
		dirty	= false;
		canvas.setMachine (root);
		refresh ();
	}

	/** Loads a machine, asking which file. */
	public void load ()
	{
		if (!leaveAll ())						return;

		JFileChooser	fc = chooser (SUFFIX, "State machines (*" + SUFFIX + ")", "hfsm");

		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		load (fc.getSelectedFile ());
	}

	/**
	 * Loads a machine from a file: the one file of a machine (<code>.hfsm</code>),
	 * or the <code>.xas</code> of the Chaos editor when that is what it is given.
	 */
	public void load (File f)
	{
		if (f.getName ().toLowerCase ().endsWith (CHAOS_SUFFIX))		{ importChaos (f); return; }
		try
		{
			HFSMJson.Machine	machine = HFSMJson.read (f);

			took (machine, f, "Loaded");
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot load " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Reads a machine as the Chaos editor left it, asking which .xas. */
	public void importChaos ()
	{
		if (!confirmDiscard ())					return;

		JFileChooser	fc = chooser (CHAOS_SUFFIX, "Chaos state machines (*" + CHAOS_SUFFIX + ")", "xas");

		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		importChaos (fc.getSelectedFile ());
	}

	/**
	 * Reads a machine of the Chaos editor: the <code>.xas</code> and the
	 * <code>.acc</code> scripts beside it. It comes in as a machine with no file of
	 * its own yet, so that saving it writes one <code>.hfsm</code> instead of
	 * writing over what was imported.
	 */
	public void importChaos (File f)
	{
		try
		{
			HFSMJson.Machine	machine = HFSMJson.importChaos (f);

			took (machine, null, "Imported " + f.getName () + " as");
			file	= new File (FOLDER, machine.root.getName () + SUFFIX);		// where Save will put it
			dirty	= true;
			refresh ();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot import " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Takes a machine that was just read as the one being edited. */
	protected void took (HFSMJson.Machine machine, File f, String what)
	{
		outers.clear ();
		canvas.setOuterPath (null);
		root	= machine.root;
		vars	= machine.vars;
		behpath	= machine.behpath;
		file	= f;
		dirty	= false;
		canvas.setMachine (root);
		refresh ();

		status (what + " " + ((f != null) ? f.getName () : root.getName ()) + ": " + XMLWriter.count (root, true) + " states, "
				+ transitions () + " transitions"
				+ (machine.problems.isEmpty () ? "" : (", " + machine.problems.size () + " problems")));
		if (!machine.problems.isEmpty ())
			JOptionPane.showMessageDialog (this, join (machine.problems), "What the file says", JOptionPane.WARNING_MESSAGE);
	}

	/** How many transitions the whole machine holds. */
	private int transitions ()
	{
		return HFSMEdit.transitions (root).size ();
	}

	/** Writes the machine where it came from, asking where when it came from nowhere. */
	public boolean save ()
	{
		if (file == null)						return saveAs ();
		return write (file);
	}

	/** Writes the machine as another state machine, asking for the file. */
	public boolean saveAs ()
	{
		JFileChooser	fc = chooser (SUFFIX, "State machines (*" + SUFFIX + ")", "hfsm");

		fc.setSelectedFile (new File ((file != null) ? file.getName () : (root.getName () + SUFFIX)));		// its own name
		if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)		return false;

		File		f = fc.getSelectedFile ();

		if (!f.getName ().endsWith (SUFFIX))	f = new File (f.getParentFile (), f.getName () + SUFFIX);
		if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " is already there. Write over it?",
														   TITLE, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION))
			return false;
		return write (f);
	}

	protected boolean write (File f)
	{
		try
		{
			// the machine is named after its file, as the machines of the examples are
			String	name = f.getName ();
			int		dot = name.lastIndexOf ('.');

			root.setName ((dot > 0) ? name.substring (0, dot) : name);
			HFSMJson.write (root, f, vars, behpath);
			file	= f;
			dirty	= false;
			refresh ();
			status ("Saved " + f.getName () + " in " + f.getParent ());
			if (onSave != null)					onSave.saved (f);
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot save " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/** A file chooser that starts where the machines are kept. */
	private JFileChooser chooser (String suffix, String what, String extension)
	{
		JFileChooser	fc = new JFileChooser ();
		File			dir = ((file != null) && (file.getParentFile () != null)) ? file.getParentFile () : new File (FOLDER);

		fc.setFileFilter (new FileNameExtensionFilter (what, extension));
		if (dir.exists ())						fc.setCurrentDirectory (dir);
		else									fc.setCurrentDirectory (new File ("."));
		return fc;
	}

	/** Whether the work in hand may be thrown away (asks when it was changed). */
	public boolean confirmDiscard ()
	{
		if (!dirty)								return true;

		int			r = JOptionPane.showConfirmDialog (this, getTitle () + " was changed. Save it?", TITLE,
													   JOptionPane.YES_NO_CANCEL_OPTION);

		if (r == JOptionPane.CANCEL_OPTION)		return false;
		if (r == JOptionPane.YES_OPTION)		return save ();
		return true;
	}

	/* ------------------------------------------------------------------ */
	/* In and out of extern machines                                       */
	/* ------------------------------------------------------------------ */

	/**
	 * Into an extern meta state: the machine of its file becomes the one being
	 * edited, with its own file, its own changes to save, and the machine that
	 * holds it set aside to come back to (double click on the background at its
	 * top, or Up). To whoever edits it reads like going into any meta state: the
	 * path over the diagram goes on from where the meta state is.
	 */
	public boolean enterExtern (MetaState m)
	{
		if ((m == null) || !m.isExtern ())		return false;

		File		f = HFSMJson.externFile (m.getPathExtern (), file);

		if (f == null)
		{
			JOptionPane.showMessageDialog (this, "The extern meta state " + m.getName () + " names no file yet (right click > Set filename...).", TITLE, JOptionPane.WARNING_MESSAGE);
			return false;
		}
		if (!f.isFile ())
		{
			JOptionPane.showMessageDialog (this, "The file of the extern meta state " + m.getName () + " is not there:\n" + f, TITLE, JOptionPane.WARNING_MESSAGE);
			return false;
		}

		HFSMJson.Machine	machine;

		try									{ machine = HFSMJson.read (f); }
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot load " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}

		Outer		o = new Outer ();

		o.root		= root;
		o.file		= file;
		o.vars		= vars;
		o.behpath	= behpath;
		o.dirty		= dirty;
		o.level		= canvas.getLevel ();
		o.path		= canvas.levelPath () + " . " + m.getName ();
		outers.push (o);

		root	= machine.root;
		vars	= machine.vars;
		behpath	= machine.behpath;
		file	= f;
		dirty	= false;
		canvas.setMachine (root);
		canvas.setOuterPath (o.path);
		refresh ();
		status ("Editing " + f.getName () + " (the extern meta state " + m.getName () + "): " + XMLWriter.count (root, true) + " states, "
				+ transitions () + " transitions" + (machine.problems.isEmpty () ? "" : (", " + machine.problems.size () + " problems"))
				+ "  --  double click on the background to go back");
		if (!machine.problems.isEmpty ())
			JOptionPane.showMessageDialog (this, join (machine.problems), "What the file says", JOptionPane.WARNING_MESSAGE);
		return true;
	}

	/**
	 * Out of an extern machine, back to the one that holds it, at the level the
	 * extern meta state is in. What was changed here is asked about first (save,
	 * drop, or stay). False when there is nothing to go back to, or the user
	 * stays.
	 */
	public boolean leaveExtern ()
	{
		if (outers.isEmpty ())					return false;
		if (!confirmDiscard ())					return false;

		Outer		o = outers.pop ();

		root	= o.root;
		file	= o.file;
		vars	= o.vars;
		behpath	= o.behpath;
		dirty	= o.dirty;
		canvas.setMachine (root);
		canvas.setOuterPath (outers.isEmpty () ? null : outers.peek ().path);
		canvas.setLevel (o.level);
		refresh ();
		status ("Back in " + ((file != null) ? file.getName () : root.getName ()) + ", showing " + canvas.levelPath ());
		return true;
	}

	/** Out of every extern machine and, at the top, past its own changes: whether all of it may be left. */
	protected boolean leaveAll ()
	{
		while (!outers.isEmpty ())
			if (!leaveExtern ())				return false;
		return confirmDiscard ();
	}

	protected void close ()
	{
		if (!leaveAll ())						return;

		java.awt.Window	w = SwingUtilities.getWindowAncestor (this);

		if (w != null)							w.dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* What the menus do                                                   */
	/* ------------------------------------------------------------------ */

	/** Asks for the priority of the selected transition: the lower it is, the sooner it is tried. */
	protected void priority ()
	{
		if (!(canvas.getSelection () instanceof Transition))
		{
			status ("Select a transition first");
			return;
		}

		Transition	t = (Transition) canvas.getSelection ();
		String		v = JOptionPane.showInputDialog (this, "Priority of " + t.getName () + " (the lower, the sooner it is tried):",
													 Integer.toString (t.getPriority ()));

		if (v == null)							return;
		try
		{
			t.setPriority (Integer.parseInt (v.trim ()));
			machineChanged ("Priority");
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog (this, "Not a number: " + v, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Says what is wrong with the machine, if anything: how it is put together,
	 * whether its scripts are Lua, and whether the behaviours they name
	 * (<code>chaos.setBehavior ("name")</code>) are there to be run, as
	 * <code>name.lua</code> in the folder of the behaviours.
	 */
	protected void check ()
	{
		List<String>	problems = new ArrayList<String> ();

		root.isCorrect ();
		if (root.getError ().trim ().length () > 0)
			for (String line : root.getError ().split ("\n"))
				if (line.trim ().length () > 0)		problems.add (line.trim ());
		root.compileAll (problems);
		checkBehaviours (problems);
		checkExterns (root, problems);

		JOptionPane.showMessageDialog (this, problems.isEmpty () ? "The machine is correct." : join (problems), TITLE,
									   problems.isEmpty () ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
	}

	/** What a script says to run a behaviour, with the name of it: chaos.setBehavior ("name") or setBehaviour. */
	static private final java.util.regex.Pattern	SET_BEHAVIOUR =
		java.util.regex.Pattern.compile ("setBehaviou?r\\s*\\(\\s*([\"'])([^\"']*)\\1\\s*\\)");

	/** The behaviours every script of the machine names, in the order they are met, each once. */
	static public List<String> behaviours (MetaState root)
	{
		List<String>	names = new ArrayList<String> ();

		for (State s : HFSMEdit.all (root))
			named (s.getCode (), names);
		for (Transition t : HFSMEdit.transitions (root))
		{
			named (t.getTestCode (), names);
			named (t.getDoCode (), names);
		}
		return names;
	}

	static private void named (String code, List<String> names)
	{
		if (code == null)						return;

		// what is commented out is not run: the block comments go, then the line ones
		code	= code.replaceAll ("(?s)--\\[\\[.*?\\]\\]", " ").replaceAll ("(?m)--.*$", "");

		java.util.regex.Matcher	m = SET_BEHAVIOUR.matcher (code);

		while (m.find ())
			if ((m.group (2).length () > 0) && !names.contains (m.group (2)))		names.add (m.group (2));
	}

	/** Whether every behaviour the scripts name is a Lua program in the folder of the behaviours. */
	/** The extern meta states: each names a file that is there and reads as a machine, and none is this very file. */
	protected void checkExterns (MetaState m, List<String> problems)
	{
		for (State s : m.getStatesList ())
		{
			if (!(s instanceof MetaState))				continue;

			MetaState	ms = (MetaState) s;

			if (ms.isExtern ())
			{
				File	f = HFSMJson.externFile (ms.getPathExtern (), file);

				if (f == null)								problems.add ("Extern meta state '" + ms.getName () + "' names no file");
				else if (!f.isFile ())						problems.add ("Extern meta state '" + ms.getName () + "': there is no " + f);
				else if ((file != null) && f.getAbsoluteFile ().equals (file.getAbsoluteFile ()))
															problems.add ("Extern meta state '" + ms.getName () + "' is this very machine");
				else
					try
					{
						HFSMJson.Machine	other = HFSMJson.read (f);

						for (String p : other.problems)		problems.add (f.getName () + ": " + p);
						if (other.root.getInitialState () == null)
							problems.add ("Extern meta state '" + ms.getName () + "': " + f.getName () + " says nowhere to start");
					}
					catch (Exception e)						{ problems.add ("Extern meta state '" + ms.getName () + "': cannot read " + f + ": " + e.getMessage ()); }
			}
			checkExterns (ms, problems);
		}
	}

	protected void checkBehaviours (List<String> problems)
	{
		List<String>	names = behaviours (root);
		File			dir = behavioursFolder ();

		if (names.isEmpty ())					return;
		if ((dir == null) || !dir.isDirectory ())
		{
			problems.add ("The behaviours " + names + " cannot be checked: "
						  + ((dir == null) ? "the machine says no folder for them (Edit > Default path...)" : ("there is no folder " + dir)));
			return;
		}
		for (String n : names)
			if (!new File (dir, n + ".lua").isFile ())
				problems.add ("Behaviour <" + n + "> is not in " + dir + " (no " + n + ".lua)");
	}

	static private String join (List<String> lines)
	{
		StringBuffer	sb = new StringBuffer ();

		for (int i = 0; (i < lines.size ()) && (i < 20); i++)		sb.append (lines.get (i)).append ("\n");
		if (lines.size () > 20)					sb.append ("... and ").append (lines.size () - 20).append (" more");
		return sb.toString ();
	}

	/* ------------------------------------------------------------------ */
	/* Keeping everything in step                                          */
	/* ------------------------------------------------------------------ */

	/** The title, the buttons and the scripts of whatever is selected. */
	protected void refresh ()
	{
		upAction.setEnabled (canvas.canGoUp () || canvas.canGoOut ());

		Object			sel = canvas.getSelection ();				// the one block selected; null with none, or several
		List<Object>	all = canvas.getSelected ();

		// what works on several blocks at once works on any selection; the rest wants one block
		deleteAction.setEnabled (!all.isEmpty () && !((all.size () == 1) && (all.get (0) == root)));
		renameAction.setEnabled (sel != null);
		initialAction.setEnabled (sel instanceof State);
		expandAction.setEnabled ((sel instanceof MetaState) && (sel != root));
		if ((sel == null) && (all.size () > 1))		showSeveral (all);
		else										showCode (sel);
		refreshPath ();
		canvas.setFile (file);						// the paths of the extern meta states are shown from it
		if (host != null)						host.editorStateChanged (this);
	}

	/** Puts the scripts of what is selected in the two panes. */
	protected void showCode (Object sel)
	{
		if (sel instanceof Transition)
		{
			Transition	t = (Transition) sel;

			selLabel.setText ("Transition " + t.getName () + "  ->  "
							  + ((t.getArrivalState () != null) ? t.getArrivalState ().getName () : "nowhere"));
			testCode.setCode (t.getTestCode ());
			testCode.setWritable (true);
			actionCode.setCode (t.getDoCode ());
			actionCode.setWritable (true);
		}
		else if (sel instanceof State)
		{
			State	s = (State) sel;

			selLabel.setText (((s instanceof MetaState) ? "Meta state " : "State ") + s.getName ()
							  + "   (a state has no test: only what it does)");
			testCode.setCode ("");
			testCode.setWritable (false);
			actionCode.setCode (s.getCode ());
			actionCode.setWritable (true);
		}
		else
		{
			selLabel.setText ("Nothing selected");
			testCode.setCode ("");
			testCode.setWritable (false);
			actionCode.setCode ("");
			actionCode.setWritable (false);
		}
	}

	/** Several blocks selected: they are counted, and no code is shown, as there is no one script to show. */
	protected void showSeveral (List<Object> all)
	{
		int		states = 0, transitions = 0;

		for (Object o : all)
			if (o instanceof State)		states++;	else	transitions++;
		selLabel.setText (all.size () + " blocks selected: " + states + " state" + ((states == 1) ? "" : "s") + ", "
						  + transitions + " transition" + ((transitions == 1) ? "" : "s") + "   (Del deletes them, drag or arrows move them together)");
		testCode.setCode ("");
		testCode.setWritable (false);
		actionCode.setCode ("");
		actionCode.setWritable (false);
	}

	protected void status (String text)
	{
		statusLabel.setText (((text == null) || (text.length () == 0)) ? " " : text);
	}

	/* ---------------- what the canvas says ---------------- */

	public void selectionChanged (Object selection)		{ refresh (); }

	public boolean leaveRoot ()
	{
		return leaveExtern ();
	}

	public void levelChanged (MetaState level)
	{
		upAction.setEnabled (canvas.canGoUp () || canvas.canGoOut ());
		status ("Showing " + canvas.levelPath ());
	}

	public void machineChanged (String what)
	{
		dirty	= true;
		refresh ();
		status (what);
	}

	/* ------------------------------------------------------------------ */
	/* The menu of a state                                                 */
	/* ------------------------------------------------------------------ */

	/**
	 * The right button on a state or a meta state: rename it, name the file of an
	 * extern meta state, or turn a plain state into a meta state, of this file or
	 * of another. What does not apply to the one under the mouse is there, greyed.
	 */
	public void nodeMenu (final State s, int x, int y)
	{
		JPopupMenu		menu = new JPopupMenu ();
		boolean			plain = !(s instanceof MetaState);
		boolean			extern = (s instanceof MetaState) && ((MetaState) s).isExtern ();
		JMenuItem		item;

		item	= new JMenuItem ("Rename...");
		item.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ canvas.rename (s); }
		});
		menu.add (item);

		item	= new JMenuItem ("Set filename...");
		item.setEnabled (extern);
		item.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ setFilename ((MetaState) s); }
		});
		menu.add (item);
		menu.addSeparator ();

		item	= new JMenuItem ("Convert to metastate");
		item.setEnabled (plain);
		item.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ convert (s, false); }
		});
		menu.add (item);

		item	= new JMenuItem ("Convert to external metastate");
		item.setEnabled (plain);
		item.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ convert (s, true); }
		});
		menu.add (item);

		menu.show (canvas, x, y);
	}

	/** Asks which .hfsm an extern meta state takes its states from, and keeps it with the machine (as ./... under the working directory). */
	public void setFilename (MetaState m)
	{
		JFileChooser	fc = chooser (HFSMJson.SUFFIX, "State machines (*" + HFSMJson.SUFFIX + ")", "hfsm");
		File			now = HFSMJson.externFile (m.getPathExtern (), file);

		fc.setDialogTitle ("The file of the extern meta state " + m.getName ());
		if ((now != null) && now.exists ())		fc.setSelectedFile (now);
		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;

		File	chosen = fc.getSelectedFile ();

		if ((file != null) && chosen.getAbsoluteFile ().equals (file.getAbsoluteFile ()))
		{
			JOptionPane.showMessageDialog (this, "A meta state cannot take its states from the very machine it is in.", TITLE, JOptionPane.WARNING_MESSAGE);
			return;
		}
		m.setPathExtern (relative (chosen));
		canvas.setSelection (m);
		machineChanged ("Set filename");
	}

	/**
	 * A plain state becomes a meta state (of this file, or an extern one whose
	 * states are in a file to be named): its script goes, which is said first when
	 * it has one.
	 */
	public void convert (State s, boolean extern)
	{
		if ((s == null) || (s instanceof MetaState))		return;
		if ((s.getCode () != null) && (s.getCode ().trim ().length () > 0)
			&& (JOptionPane.showConfirmDialog (this, "The state " + s.getName () + " has a script, which a meta state has not: it goes. Convert it?",
											   TITLE, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION))
			return;

		MetaState	m = HFSMEdit.convert (root, s, extern);

		if (m == null)										return;
		canvas.setSelection (m);
		machineChanged (extern ? "Convert to external metastate" : "Convert to metastate");
		if (extern)											setFilename (m);
	}

	public void statusChanged (String text)				{ status (text); }
	public void usageChanged (String text)				{ usageLabel.setText ((text != null) ? text : " "); }

	public void toolFinished ()
	{
		toolButtons[HFSMCanvas.T_SELECT].setSelected (true);
	}

	/* ---------------- what the panes say ---------------- */

	/** A script was written in: it goes into the state or the transition at once. */
	public void codeChanged (CodeEditor editor)
	{
		Object		sel = canvas.getSelection ();

		if (sel instanceof Transition)
		{
			Transition	t = (Transition) sel;

			if (editor == testCode)				t.setTestCode (editor.getCode ());
			else								t.setDoCode (editor.getCode ());
		}
		else if (sel instanceof State)
		{
			if (editor == actionCode)			((State) sel).setCode (editor.getCode ());
			else								return;
		}
		else
			return;

		dirty	= true;
		if (host != null)						host.editorStateChanged (this);
	}
}
