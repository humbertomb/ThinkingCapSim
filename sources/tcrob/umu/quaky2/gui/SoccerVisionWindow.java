/**
 * Created on 09-dec-2018
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import tcrob.umu.quaky2.*;

//import static dasboot.utils.WindowUtils.adjustLocationToScreenBounds;

public class SoccerVisionWindow extends JFrame
{	
	static public final int				GUI_IMAGE_WIDTH		= 275;
	static public final int				GUI_IMAGE_HEIGHT		= 155;
	
	protected SoccerVisionPanel			panel;

	// Constructors
	public SoccerVisionWindow (JFrame frame, SoccerVision pam)
	{
		panel = new SoccerVisionPanel (frame, pam);
		
		setLayout (new GridLayout (1, 1));
		setVisible (false);
		add (panel);

		setTitle ("Chaos Vision Monitor");
		setSize (new Dimension (275, 690));
		setLocationRelativeTo (frame);

		if (frame != null)
			setLocation (frame.getWidth (), 0);

		// Ensure the window is within screen bounds
		//adjustLocationToScreenBounds(this);

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
