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

		// docked on the right of the window it goes with, level with its top (and on the screen)
		if (frame != null)
		{
			Rectangle	screen = (frame.getGraphicsConfiguration () != null) ? frame.getGraphicsConfiguration ().getBounds ()
									: new Rectangle (Toolkit.getDefaultToolkit ().getScreenSize ());
			int			x = frame.getX () + frame.getWidth (), y = frame.getY ();

			x	= Math.max (screen.x, Math.min (x, screen.x + screen.width - getWidth ()));
			y	= Math.max (screen.y, Math.min (y, screen.y + screen.height - getHeight ()));
			setLocation (x, y);
		}

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
