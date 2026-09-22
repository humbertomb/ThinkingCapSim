/*
 * Created on 25-oct-2004
 * (c) 2026 Humberto Martinez Barbera
 */
package tc.shared.lps.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import tc.shared.lps.*;
import tc.gui.visualization.*;
import devices.pos.Path;

import wucore.gui.*;

/**
 * The window with the Local Perceptual Space of a robot (and the path it
 * follows), drawn with Swing by an {@link LPSPanel}.
 *
 * @author Humberto Martinez Barbera
 */
public class IndoorLPSWindow extends JFrame
{
	private static final long		serialVersionUID	= 1L;

	protected ChildWindowListener	parent;
	protected LPSPanel				panel;

	public IndoorLPSWindow (String name)
	{
		this (name, null);
	}

	public IndoorLPSWindow (String name, ChildWindowListener parent)
	{
		this.parent	= parent;
		panel		= new LPSPanel ();

		setTitle ("[" + name + "] Local Perceptual Space");
		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (panel, BorderLayout.CENTER);
		setLocation (new Point (50, 50));
		setSize (new Dimension (400, 400));

		// Close the window when the close box is clicked
		setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);
		addWindowListener (new WindowAdapter ()
		{
			public void windowClosing (WindowEvent e)		{ close (); }
		});

		setVisible (true);
	}

	/** The panel it draws with. */
	public LPSPanel panel ()							{ return panel; }

	public void close ()
	{
		if (parent != null)
			parent.childClosed (this);

		setVisible (false);
		dispose ();
	}

	/** Draws the LPS (and the path) again; it can be called from any thread. */
	public void update (LPS lps, Path path)
	{
		panel.update (lps, path);
	}
}
