/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import tc.shared.world.World;

/**
 * Modal editor of a {@link World} held in memory (the world of a deployment
 * in the simulator, for instance): a {@link WorldEditor} with the Edit, View
 * and Help menus (no File menu: the world is neither loaded nor saved here).
 * A copy of the world is edited; OK returns the edited copy, Cancel returns
 * null and leaves the original untouched.
 */
public class WorldEditorDialog extends JDialog implements WorldEditor.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= "World Editor";

	protected WorldEditor			editor;
	protected World					result;

	/**
	 * @param world  the world to edit (a copy is edited)
	 * @param file   the file the world comes from (shown in the title; may be null)
	 */
	public WorldEditorDialog (Window owner, World world, File file)
	{
		super (owner, TITLE, ModalityType.APPLICATION_MODAL);
		editor	= new WorldEditor (World.fromJsonText (world.toJsonText ()), file, this);

		JButton		okBT = new JButton ("OK"), cancelBT = new JButton ("Cancel");
		okBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { accept (); } });
		cancelBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { cancel (); } });
		JPanel		buttons = new JPanel (new FlowLayout (FlowLayout.RIGHT, 8, 4));
		buttons.add (cancelBT);
		buttons.add (okBT);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		getContentPane ().add (buttons, BorderLayout.SOUTH);
		setJMenuBar (editor.buildMenuBar (false));
		getRootPane ().setDefaultButton (okBT);
		editorStateChanged (editor);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ cancel (); }
		});
		setSize (1200, 950);
		setLocationRelativeTo (owner);
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ editor.getCanvas ().zoomToFit (); }
		});
	}

	/** Shows the dialog; the edited world, or null when cancelled or unchanged. */
	public World showDialog ()
	{
		setVisible (true);
		return result;
	}

	public WorldEditor	getEditor ()			{ return editor; }

	public void editorStateChanged (WorldEditor ed)
	{
		setTitle (TITLE + " - " + ed.getTitle ());
	}

	private void accept ()
	{
		result	= editor.isDirty () ? editor.getWorld () : null;
		editor.dispose ();
		dispose ();
	}

	private void cancel ()
	{
		if (editor.isDirty ())
		{
			int	r = JOptionPane.showConfirmDialog (this, "Discard the changes made to the world?", TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (r != JOptionPane.YES_OPTION)		return;
		}
		result	= null;
		editor.dispose ();
		dispose ();
	}
}
