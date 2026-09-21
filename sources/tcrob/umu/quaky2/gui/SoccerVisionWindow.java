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

public class SoccerVisionWindow extends JFrame
{	
	static public final int				GUI_IMAGE_WIDTH		= 275;
	static public final int				GUI_IMAGE_HEIGHT	= 155;		// the image canvases (CPImageCanvas and the others take it as their size)
	static public final int				WIN_WIDTH			= 275;		// the window, with all its panels
	static public final int				WIN_HEIGHT			= 720;		// 690 and the toolbar
	
	protected SoccerVisionPanel			panel;

	// Constructors
	public SoccerVisionWindow (JFrame frame, SoccerVision pam)
	{
		panel = new SoccerVisionPanel (frame, pam);
		
		setLayout (new GridLayout (1, 1));
		setVisible (false);
		add (panel);

		setTitle ("Chaos Vision Monitor");
		setSize (new Dimension (WIN_WIDTH, WIN_HEIGHT));
		setLocationRelativeTo (frame);

		if (frame != null)
			setLocation (frame.getWidth (), 0);

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
