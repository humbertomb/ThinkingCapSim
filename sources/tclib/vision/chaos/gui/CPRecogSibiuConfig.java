/**
 * @author Humberto Martinez Barbera 
 */
package tclib.vision.chaos.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import tclib.vision.chaos.recognize.*;

public class CPRecogSibiuConfig extends JPanel 
{
	// GUI components
	private JTextField				minesxmin;
	private JTextField				minesymin;
	private JTextField				minedensity;
	
	public RecognizerSibiu			recognizer;
	
	public CPRecogSibiuConfig (RecognizerSibiu recognizer)
	{
		JScrollPane	scroll;
		JPanel		view;
		
		this.recognizer = recognizer;
		
		minesxmin 	= new JTextField (Integer.valueOf (recognizer.mineSxMin).toString ());
		minesymin 	= new JTextField (Integer.valueOf (recognizer.mineSyMin).toString ());
		minedensity	= new JTextField (Integer.valueOf (recognizer.mineDensity).toString ());
		
		view = new JPanel ();
		view.setLayout (new BoxLayout (view, BoxLayout.Y_AXIS));
		view.add (createRobotPanel ());
		scroll = new JScrollPane (view);
		
		setLayout (new GridLayout (1, 1));
		add (scroll);
		
		setVisible(true);
	}
	
	protected void updateValues ()
	{
	}
						
	private JPanel createRobotPanel()
	{
		JPanel panel, left, right;
			
		left = new JPanel();
		left.setLayout(new GridLayout (6, 1));
		left.add(new JLabel ("Min width (pix)"));
		left.add(new JLabel ("Min height (pix)"));
		left.add(new JLabel ("Min density (area/pix)"));

		right = new JPanel();
		right.setLayout(new GridLayout (6, 1));
		right.add(minesxmin);
		right.add(minesymin);
		right.add(minedensity);

		panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Mine Recognition", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new BorderLayout ());
		panel.add (left, BorderLayout.WEST);
		panel.add (right, BorderLayout.CENTER);

		minesxmin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				recognizer.mineSxMin = Integer.valueOf (minesxmin.getText ()).intValue ();
			}
		});
		minesymin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				recognizer.mineSyMin = Integer.valueOf (minesymin.getText ()).intValue ();
			}
		});
		minedensity.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				recognizer.mineDensity = Integer.valueOf (minedensity.getText ()).intValue ();
			}
		});

		return panel;
	}
}
