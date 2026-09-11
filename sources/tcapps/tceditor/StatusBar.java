/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Status bar of the World windows: cursor / element information on the left
 * and, right-aligned, how the current tool is used.
 */
public class StatusBar extends JPanel
{
	private static final long		serialVersionUID = 1L;

	protected JLabel				statusLabel;
	protected JLabel				usageLabel;

	public StatusBar ()
	{
		super (new BorderLayout ());
		statusLabel	= new JLabel (" ");
		statusLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		usageLabel	= new JLabel (" ", JLabel.RIGHT);
		usageLabel.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		usageLabel.setForeground (new Color (70, 70, 70));
		add (statusLabel, BorderLayout.WEST);
		add (usageLabel, BorderLayout.CENTER);
	}

	public void setStatus (String text)		{ statusLabel.setText (((text == null) || (text.length () == 0)) ? " " : text); }
	public void setUsage (String text)		{ usageLabel.setText (((text == null) || (text.length () == 0)) ? " " : text); }
}
