/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
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
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import tclib.behaviours.hfsm.MetaState;
import tclib.behaviours.hfsm.State;
import tclib.behaviours.hfsm.Transition;
import tclib.behaviours.hfsm.XMLParser;
import tclib.behaviours.hfsm.XMLWriter;

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
	static public final String		SUFFIX		= ".xas";

	/** What the window hosting the editor needs to know. */
	public interface Host
	{
		/** The file or the modified mark changed. */
		public void editorStateChanged (HFSMPanel editor);
	}

	/* Model */
	protected MetaState				root;
	protected File					file;						// the .xas it came from, or null
	protected List<XMLParser.PrivateVar>	vars = new ArrayList<XMLParser.PrivateVar> ();
	protected boolean				dirty;

	/* GUI */
	protected HFSMCanvas			canvas;
	protected CodeEditor			testCode;
	protected CodeEditor			actionCode;
	protected JLabel				selLabel;
	protected JLabel				statusLabel;
	protected JLabel				usageLabel;
	protected JToggleButton[]		toolButtons	= new JToggleButton[HFSMCanvas.NTOOLS];
	protected Action				deleteAction, renameAction, initialAction, upAction;
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
			public void run ()		{ canvas.levelUp (); }
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

		// the letters of the tools, with the diagram focused
		key ("S", HFSMCanvas.T_SELECT);		key ("H", HFSMCanvas.T_PAN);		key ("T", HFSMCanvas.T_STATE);
		key ("M", HFSMCanvas.T_META);		key ("N", HFSMCanvas.T_TRANS);		key ("J", HFSMCanvas.T_LINK);
		bind (KeyStroke.getKeyStroke (KeyEvent.VK_I, 0), "initial", initialAction);
		bind (KeyStroke.getKeyStroke (KeyEvent.VK_0, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ()), "fit",
			  action ("fit", HFSMIcon.ZOOM_FIT, null, new Runnable () { public void run () { canvas.zoomToFit (); } }));

		return tb;
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

	private void bind (KeyStroke key, String name, Action a)
	{
		getInputMap (JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put (key, name);
		getActionMap ().put (name, a);
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

		file.add (item ("New", KeyStroke.getKeyStroke (KeyEvent.VK_N, mask), new Runnable ()
		{
			public void run ()		{ newFile (); }
		}));
		file.add (item ("Load", KeyStroke.getKeyStroke (KeyEvent.VK_O, mask), new Runnable ()
		{
			public void run ()		{ load (); }
		}));
		file.addSeparator ();
		file.add (item ("Save", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask), new Runnable ()
		{
			public void run ()		{ save (); }
		}));
		file.add (item ("Save as State Machine", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), new Runnable ()
		{
			public void run ()		{ saveAs (); }
		}));
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
		edit.addSeparator ();
		edit.add (item ("Priority of the transition...", null, new Runnable ()
		{
			public void run ()		{ priority (); }
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

		return bar;
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
		if (!confirmDiscard ())					return;
		root	= newMachine ();
		file	= null;
		vars	= new ArrayList<XMLParser.PrivateVar> ();
		dirty	= false;
		canvas.setMachine (root);
		refresh ();
	}

	/** Loads a machine, asking which file. */
	public void load ()
	{
		if (!confirmDiscard ())					return;

		JFileChooser	fc = chooser ();

		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		load (fc.getSelectedFile ());
	}

	/** Loads a machine from a file, with the scripts of the folder of the file. */
	public void load (File f)
	{
		try
		{
			XMLParser	parser = XMLParser.parse (f);

			root	= parser.root ();
			vars	= parser.privateVars ();
			root.loadCode (f.getParent ());
			root.sortAll ();
			file	= f;
			dirty	= false;
			canvas.setMachine (root);
			refresh ();

			status ("Loaded " + f.getName () + ": " + XMLWriter.count (root, true) + " states, " + transitions ()
					+ " transitions" + (parser.problems ().isEmpty () ? "" : (", " + parser.problems ().size () + " problems")));
			if (!parser.problems ().isEmpty ())
				JOptionPane.showMessageDialog (this, join (parser.problems ()), "What the file says", JOptionPane.WARNING_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot load " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
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
		JFileChooser	fc = chooser ();

		fc.setSelectedFile (new File ((file != null) ? file.getName () : (root.getName () + SUFFIX)));
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
			XMLWriter.write (root, f, vars);
			file	= f;
			dirty	= false;
			refresh ();
			status ("Saved " + f.getName () + " and the scripts of its states in " + f.getParent ());
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot save " + f + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private JFileChooser chooser ()
	{
		JFileChooser	fc = new JFileChooser ();

		fc.setFileFilter (new FileNameExtensionFilter ("State machines (*" + SUFFIX + ")", "xas"));
		if (file != null)						fc.setCurrentDirectory (file.getParentFile ());
		else
		{
			File	def = new File ("./conf/programs/hfsm");

			if (def.exists ())					fc.setCurrentDirectory (def);
		}
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

	protected void close ()
	{
		if (!confirmDiscard ())					return;

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

	/** Says what is wrong with the machine, if anything, and whether its scripts are Lua. */
	protected void check ()
	{
		List<String>	problems = new ArrayList<String> ();

		root.isCorrect ();
		if (root.getError ().trim ().length () > 0)
			for (String line : root.getError ().split ("\n"))
				if (line.trim ().length () > 0)		problems.add (line.trim ());
		root.compileAll (problems);

		JOptionPane.showMessageDialog (this, problems.isEmpty () ? "The machine is correct." : join (problems), TITLE,
									   problems.isEmpty () ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
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
		upAction.setEnabled (canvas.canGoUp ());

		Object		sel = canvas.getSelection ();

		deleteAction.setEnabled ((sel != null) && (sel != root));
		renameAction.setEnabled (sel != null);
		initialAction.setEnabled (sel instanceof State);
		showCode (sel);
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

	protected void status (String text)
	{
		statusLabel.setText (((text == null) || (text.length () == 0)) ? " " : text);
	}

	/* ---------------- what the canvas says ---------------- */

	public void selectionChanged (Object selection)		{ refresh (); }

	public void levelChanged (MetaState level)
	{
		upAction.setEnabled (canvas.canGoUp ());
		status ("Showing " + canvas.levelPath ());
	}

	public void machineChanged (String what)
	{
		dirty	= true;
		refresh ();
		status (what);
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
