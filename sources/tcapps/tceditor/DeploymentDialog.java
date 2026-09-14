/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import tc.DeployArch;

/**
 * Modal editor of the deployment architecture of a running application (the
 * simulator, for instance): a {@link DeploymentPanel} with its File menu and
 * the OK / Cancel buttons. A copy of the deployment is edited;
 * {@link #showDialog()} returns the edited copy, or null when cancelled,
 * leaving the original untouched.
 */
public class DeploymentDialog extends JDialog implements DeploymentPanel.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= DeploymentPanel.TITLE;

	protected DeploymentPanel		editor;
	protected DeployArch			result;

	/** @param deploy  deployment architecture to edit (a copy is edited; {@link #showDialog()} returns it when accepted) */
	public DeploymentDialog (Window owner, DeployArch deploy)
	{
		super (owner, TITLE, ModalityType.APPLICATION_MODAL);
		editor	= new DeploymentPanel (deploy.copy (), this);

		JButton		okBT = new JButton ("OK"), cancelBT = new JButton ("Cancel");
		okBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { accept (); } });
		cancelBT.addActionListener (new ActionListener () { public void actionPerformed (ActionEvent e) { cancel (); } });
		JPanel		buttons = new JPanel (new FlowLayout (FlowLayout.RIGHT, 6, 6));
		buttons.add (cancelBT);
		buttons.add (okBT);

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		getContentPane ().add (buttons, BorderLayout.SOUTH);
		setJMenuBar (editor.buildMenuBar (false));
		getRootPane ().setDefaultButton (okBT);
		getRootPane ().getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW).put (KeyStroke.getKeyStroke (KeyEvent.VK_ESCAPE, 0), "cancel");
		getRootPane ().getActionMap ().put ("cancel", new AbstractAction ()
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ cancel (); }
		});
		deploymentStateChanged (editor);

		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ cancel (); }
		});
		pack ();
		setMinimumSize (new Dimension (760, 520));
		setSize (1040, 780);
		setLocationRelativeTo (owner);
	}

	public DeploymentPanel	getEditor ()			{ return editor; }

	public void deploymentStateChanged (DeploymentPanel panel)
	{
		setTitle (TITLE + " - " + panel.getTitle ());
	}

	private void accept ()
	{
		editor.stopEditing ();
		result	= editor.getDeploy ();
		dispose ();
	}

	private void cancel ()
	{
		result	= null;
		dispose ();
	}

	/** Shows the dialog; returns the edited deployment, or null when cancelled. */
	public DeployArch showDialog ()
	{
		result	= null;
		setVisible (true);
		return result;
	}
}
