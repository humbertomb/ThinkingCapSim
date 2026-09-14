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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tc.DeployArch;

/**
 * Stand-alone editor of deployment architectures (.deploy files): a window
 * hosting a {@link DeploymentPanel} with its File menu. It needs no
 * simulator, so a deployment can be prepared before running anything.
 *
 * <pre>
 *   java tcapps.tceditor.DeploymentWindow [file.deploy]
 * </pre>
 */
public class DeploymentWindow extends JFrame implements DeploymentPanel.Host
{
	private static final long		serialVersionUID = 1L;

	static public final String		TITLE		= DeploymentPanel.TITLE;

	protected DeploymentPanel		editor;

	public DeploymentWindow ()
	{
		this (DeploymentPanel.newDeploy ());
	}

	public DeploymentWindow (DeployArch deploy)
	{
		super (TITLE);
		editor	= new DeploymentPanel (deploy, this);
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (editor, BorderLayout.CENTER);
		setJMenuBar (editor.buildMenuBar (true));
		deploymentStateChanged (editor);

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

	public DeploymentPanel	getEditor ()				{ return editor; }

	/** Loads a deployment into the editor (asking first about unsaved changes). */
	public void loadDeployment (File f)
	{
		if (!editor.confirmDiscard ())			return;
		try
		{
			editor.setDeployment (DeployArch.load (f));
		} catch (Exception e)
		{
			e.printStackTrace ();
			JOptionPane.showMessageDialog (this, "Cannot load " + f.getName () + ":\n" + e, TITLE, JOptionPane.ERROR_MESSAGE);
		}
	}

	public void deploymentStateChanged (DeploymentPanel panel)
	{
		setTitle (TITLE + " - " + panel.getTitle ());
	}

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
			System.setProperty ("apple.laf.useScreenMenuBar", "false");		// menus inside the window, as in the dialogs
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ());
		} catch (Exception e) { }

		final String	name = (args.length > 0) ? args[0] : null;
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()
			{
				DeploymentWindow	win = new DeploymentWindow ();
				win.setVisible (true);
				if (name != null)		win.loadDeployment (new File (name));
			}
		});
	}
}
