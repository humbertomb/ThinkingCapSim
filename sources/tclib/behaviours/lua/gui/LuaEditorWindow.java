/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultEditorKit;

import tclib.behaviours.lua.interpreter.LuaError;
import tclib.behaviours.lua.interpreter.LuaScript;

/**
 * The editor of one Lua script: a {@link CodeEditor} with the file being
 * written in it, and the File and Edit menus of any editor.
 *
 * It is the behaviours of {@link tclib.behaviours.lua.LuaController} and the
 * library the states of a machine choose from that are written here, so what it
 * offers to open and where it saves is <code>./conf/programs/lua</code>.
 *
 * <pre>
 *   java tclib.behaviours.lua.gui.LuaEditorWindow [program.lua]
 * </pre>
 */
public class LuaEditorWindow extends JFrame implements CodeEditor.Listener
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "Lua Editor";
	static public final String		SUFFIX		= ".lua";
	/** Where the scripts of the behaviours are kept. */
	static public final String		FOLDER		= "./conf/programs/lua";

	/** Told whenever the program was written, for whoever is running it. */
	public interface Saved
	{
		public void saved (File file);
	}

	protected CodeEditor			code;
	protected JLabel				status;
	protected File					file;
	protected boolean				dirty;
	protected Saved					onSave;
	protected JMenuItem				newItem, loadItem;
	protected boolean				onefile;					// one program and no other: the one that is running

	public LuaEditorWindow ()
	{
		this (null);
	}

	public LuaEditorWindow (File f)
	{
		super (TITLE);

		code	= new CodeEditor ("Lua");
		code.setListener (this);

		status	= new JLabel (" ");
		status.setBorder (BorderFactory.createEmptyBorder (3, 6, 3, 6));

		JPanel		main = new JPanel (new BorderLayout ());

		main.add (code, BorderLayout.CENTER);
		main.add (status, BorderLayout.SOUTH);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (main, BorderLayout.CENTER);
		setJMenuBar (buildMenuBar ());

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ quit (); }
		});
		setSize (760, 700);
		setLocationRelativeTo (null);

		if (f != null)					load (f);
		else							newFile ();
	}

	public final CodeEditor			getCodeEditor ()	{ return code; }

	/**
	 * Who to tell when the program was written, so that whoever is running it can
	 * read it again.
	 */
	public void setOnSave (Saved s)						{ onSave = s; }

	/**
	 * Whether this editor is of one program and no other, which is how it is opened
	 * on the program a robot is running: there is nothing to start afresh and
	 * nothing else to read in, so New and Load are not to be had. It can still be
	 * written, and written somewhere else as a copy (Save as).
	 */
	public void setOneFile (boolean b)
	{
		onefile	= b;
		if (newItem != null)				newItem.setEnabled (!b);
		if (loadItem != null)				loadItem.setEnabled (!b);
	}

	public boolean isOneFile ()							{ return onefile; }
	public final File				getFile ()			{ return file; }
	public boolean					isDirty ()			{ return dirty; }

	/** What is written, never null. */
	public String					getCode ()			{ return code.getCode (); }

	/** What the title bar says: the file being written, and whether it was changed. */
	public String documentName ()
	{
		return ((file == null) ? ("untitled" + SUFFIX) : file.getName ()) + (dirty ? " *" : "");
	}

	protected void said ()
	{
		setTitle (TITLE + " - " + documentName ());
		status.setText ((file != null) ? file.getPath () : "not saved yet");
	}

	/* ------------------------------------------------------------------ */
	/* The menus                                                           */
	/* ------------------------------------------------------------------ */

	protected JMenuBar buildMenuBar ()
	{
		JMenuBar	bar = new JMenuBar ();
		int			mask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx ();

		JMenu		menu = new JMenu ("File");

		newItem		= item ("New Program", KeyStroke.getKeyStroke (KeyEvent.VK_N, mask), new Runnable ()
		{
			public void run ()		{ newFile (); }
		});
		loadItem	= item ("Load Program", KeyStroke.getKeyStroke (KeyEvent.VK_O, mask), new Runnable ()
		{
			public void run ()		{ load (); }
		});
		newItem.setEnabled (!onefile);
		loadItem.setEnabled (!onefile);
		menu.add (newItem);
		menu.add (loadItem);
		menu.addSeparator ();
		menu.add (item ("Save Program", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask), new Runnable ()
		{
			public void run ()		{ save (); }
		}));
		menu.add (item ("Save as Program", KeyStroke.getKeyStroke (KeyEvent.VK_S, mask | InputEvent.SHIFT_DOWN_MASK), new Runnable ()
		{
			public void run ()		{ saveAs (); }
		}));
		menu.addSeparator ();
		menu.add (item ("Close", null, new Runnable ()
		{
			public void run ()		{ quit (); }
		}));
		bar.add (menu);

		JMenu		edit = new JMenu ("Edit");

		edit.add (item ("Undo", KeyStroke.getKeyStroke (KeyEvent.VK_Z, mask), new Runnable ()
		{
			public void run ()		{ code.undo (); }
		}));
		edit.add (item ("Redo", KeyStroke.getKeyStroke (KeyEvent.VK_Z, mask | InputEvent.SHIFT_DOWN_MASK), new Runnable ()
		{
			public void run ()		{ code.redo (); }
		}));
		edit.addSeparator ();
		edit.add (kit ("Cut", DefaultEditorKit.cutAction, KeyStroke.getKeyStroke (KeyEvent.VK_X, mask)));
		edit.add (kit ("Copy", DefaultEditorKit.copyAction, KeyStroke.getKeyStroke (KeyEvent.VK_C, mask)));
		edit.add (kit ("Paste", DefaultEditorKit.pasteAction, KeyStroke.getKeyStroke (KeyEvent.VK_V, mask)));
		edit.addSeparator ();
		edit.add (kit ("Select All", DefaultEditorKit.selectAllAction, KeyStroke.getKeyStroke (KeyEvent.VK_A, mask)));
		bar.add (edit);

		JMenu		what = new JMenu ("Code");

		what.add (item ("Verify Code", KeyStroke.getKeyStroke (KeyEvent.VK_F5, 0), new Runnable ()
		{
			public void run ()		{ verify (); }
		}));
		bar.add (what);

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

	/** A menu item of what the text pane does on its own (cut, copy, paste, select all). */
	private JMenuItem kit (String name, String action, KeyStroke key)
	{
		javax.swing.Action	a = code.getTextPane ().getActionMap ().get (action);
		JMenuItem			mi = new JMenuItem (name);

		if (a != null)			mi.addActionListener (a);
		if (key != null)		mi.setAccelerator (key);
		return mi;
	}

	/* ------------------------------------------------------------------ */
	/* The file                                                            */
	/* ------------------------------------------------------------------ */

	/** An empty program, the work in hand asked about when it was changed. */
	public void newFile ()
	{
		if (onefile)							return;			// this editor is of one program and no other
		if (!confirmDiscard ())					return;

		file	= null;
		code.setCode ("");
		code.setWritable (true);
		dirty	= false;
		said ();
	}

	/** Asks which program to write in, and reads it. */
	public void load ()
	{
		if (onefile)							return;			// this editor is of one program and no other
		if (!confirmDiscard ())					return;

		JFileChooser	fc = chooser ();

		if (fc.showOpenDialog (this) != JFileChooser.APPROVE_OPTION)		return;
		load (fc.getSelectedFile ());
	}

	/** Reads a program into the editor. */
	public void load (File f)
	{
		if (f == null)							return;

		try
		{
			code.setCode (new String (Files.readAllBytes (f.toPath ()), StandardCharsets.UTF_8));
			code.setWritable (true);
			file	= f;
			dirty	= false;
			said ();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot read " + f.getName () + ":\n" + e.getMessage (),
										   TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Writes what is in the editor, asking for a name when it has none. */
	public boolean save ()
	{
		if (file == null)						return saveAs ();
		return write (file);
	}

	/** Writes what is in the editor into a file of its own. */
	public boolean saveAs ()
	{
		JFileChooser	fc = chooser ();

		if (file != null)						fc.setSelectedFile (file);
		if (fc.showSaveDialog (this) != JFileChooser.APPROVE_OPTION)			return false;

		File			f = fc.getSelectedFile ();

		if (!f.getName ().toLowerCase ().endsWith (SUFFIX))
			f	= new File (f.getParentFile (), f.getName () + SUFFIX);
		if (f.exists () && (JOptionPane.showConfirmDialog (this, f.getName () + " is already there. Write over it?",
														   TITLE, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION))
			return false;
		return write (f);
	}

	/** Writes what is in the editor into a file, as Save as does once one is chosen. */
	public boolean saveTo (File f)						{ return write (f); }

	protected boolean write (File f)
	{
		// an editor of one program that writes somewhere else has written a copy: it
		// goes on being the editor of the program, which is still to be saved
		boolean			copy = onefile && (file != null) && !file.getAbsolutePath ().equals (f.getAbsolutePath ());

		try
		{
			Files.write (f.toPath (), code.getCode ().getBytes (StandardCharsets.UTF_8));
			if (!copy)
			{
				file	= f;
				dirty	= false;
			}
			said ();
			if (onSave != null)					onSave.saved (f);		// it is running somewhere: it is read again there
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog (this, "Cannot write " + f.getName () + ":\n" + e.getMessage (),
										   TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	protected JFileChooser chooser ()
	{
		JFileChooser	fc = new JFileChooser ();
		File			dir = new File (FOLDER);

		fc.setDialogTitle ("Lua programs (*" + SUFFIX + ")");
		if (dir.isDirectory ())					fc.setCurrentDirectory (dir);
		fc.setFileFilter (new FileNameExtensionFilter ("Lua programs (*" + SUFFIX + ")", "lua"));
		return fc;
	}

	/** Whether the work in hand may be lost: what was changed is offered to be saved first. */
	public boolean confirmDiscard ()
	{
		if (!dirty)								return true;

		int			r = JOptionPane.showConfirmDialog (this, documentName () + " was changed. Save it?", TITLE,
													   JOptionPane.YES_NO_CANCEL_OPTION);

		if (r == JOptionPane.CANCEL_OPTION)		return false;
		if (r == JOptionPane.YES_OPTION)		return save ();
		return true;
	}

	/** Closes the window, asking about the work in hand when it was changed. */
	public void quit ()
	{
		if (!confirmDiscard ())					return;
		dispose ();
	}

	/* ------------------------------------------------------------------ */
	/* The code itself                                                     */
	/* ------------------------------------------------------------------ */

	/**
	 * Reads what is written with the interpreter that will run it, and says whether
	 * it makes sense. What went wrong is said with the line it is on, and that line
	 * is marked in the editor, so there is no counting to do.
	 *
	 * It is only read, never run: nothing of the robot is touched by verifying.
	 */
	public boolean verify ()
	{
		String			name = (file != null) ? file.getName () : ("untitled" + SUFFIX);

		try
		{
			LuaScript	script = new LuaScript (code.getCode (), name);
			String		said = script.isEmpty () ? (name + " does nothing: it is empty or all comments")
												 : (name + " reads well: " + script.block ().stats.size () + " statements, "
													+ code.lines () + " lines");

			status.setText (said);
			JOptionPane.showMessageDialog (this, said, TITLE, JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		catch (LuaError e)
		{
			if (e.line () > 0)					code.goToLine (e.line ());
			status.setText (e.getMessage ());
			JOptionPane.showMessageDialog (this, e.getMessage (), TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
		catch (RuntimeException e)
		{
			status.setText (name + ": " + e);
			JOptionPane.showMessageDialog (this, name + " cannot be read:\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/* ------------------------------------------------------------------ */
	/* What the editor says                                                */
	/* ------------------------------------------------------------------ */

	public void codeChanged (CodeEditor editor)
	{
		dirty	= true;
		said ();
	}

	static public void main (String[] args)
	{
		try
		{
			System.setProperty ("apple.laf.useScreenMenuBar", "false");		// the menus inside the window, as the other editors have them
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		}
		catch (Exception e) { }

		final File		f = (args.length > 0) ? new File (args[0]) : null;

		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				LuaEditorWindow		w = new LuaEditorWindow (f);

				w.setVisible (true);
			}
		});
	}
}
