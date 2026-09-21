/**
 * Created on 09-dec-2018
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import tclib.vision.chaos.*;

import static dasboot.utils.WindowUtils.adjustLocationToScreenBounds;

public class ChaosVisionWindow extends JFrame
{	
	static public final int				GUI_IMAGE_WIDTH		= 275;
	static public final int				GUI_IMAGE_HEIGHT		= 155;
	
	protected ChaosVisionPanel			panel;

	// Constructors
	public ChaosVisionWindow (JFrame frame, ChaosPam pam)
	{
		panel = new ChaosVisionPanel (frame, pam);
		
		setLayout (new GridLayout (1, 1));
		setVisible (false);
		add (panel);

		setTitle ("Chaos Vision Monitor");
		setSize (new Dimension (275, 690));
		setLocationRelativeTo (frame);

		if (frame != null)
			setLocation (frame.getWidth (), 0);

		// Ensure the window is within screen bounds
		adjustLocationToScreenBounds(this);

		setVisible (false);
		
		// event handling
		addWindowListener (new WindowAdapter() {
			public void windowClosing (WindowEvent e) 
			{
				setVisible (false);
			}
		});
	}
	
	public void updateBufferedImage (BufferedImage image) 
	{
		panel.updateBufferedImage (image);
	}
}
