/**
 * @author Humberto Martinez Barbera 
 */
package tcrob.umu.quaky2.gui;

import java.awt.*;
import javax.swing.*;

import tcrob.umu.quaky2.SoccerRecognizer;

public class SoccerRecognitonConfigPanel extends JPanel 
{
	// GUI components
	private JTextField ballsxmin;
	private JTextField ballsymin;
	private JTextField ballhorihgt;
	private JTextField balldensity;
	private JTextField ballxdisp;
	private JTextField ballydisp;
	private JTextField netsxmin;
	private JTextField netsymin;
	private JTextField nethorihgt;
	private JTextField netdensity;
	private JTextField netinminx;
	private JTextField netinminy;
	private JTextField netinmina;
	private JTextField netinmemo;
	private JTextField lmsxmin;
	private JTextField lmsymin;
	private JTextField lmhorihgt;
	private JTextField lmdensity;
	
	public SoccerRecognizer			recognizer;
	
	public SoccerRecognitonConfigPanel (SoccerRecognizer recognizer)
	{
		JScrollPane	scroll;
		JPanel		view;
		
		this.recognizer = recognizer;
		
		ballsxmin 	= new JTextField (Integer.valueOf (recognizer.BALL_SX_MIN).toString ());
		ballsymin 	= new JTextField (Integer.valueOf (recognizer.BALL_SY_MIN).toString ());
		ballhorihgt	= new JTextField (Integer.valueOf (recognizer.BALL_HORIZ_HGT).toString ());
		balldensity	= new JTextField (Integer.valueOf (recognizer.BALL_DENSITY).toString ());
		ballxdisp 	= new JTextField (Integer.valueOf (recognizer.BALL_XDISP).toString ());
		ballydisp 	= new JTextField (Integer.valueOf (recognizer.BALL_YDISP).toString ());
		netsxmin 	= new JTextField (Integer.valueOf (recognizer.NET_SX_MIN).toString ());
		netsymin 	= new JTextField (Integer.valueOf (recognizer.NET_SY_MIN).toString ());
		nethorihgt	= new JTextField (Integer.valueOf (recognizer.NET_HORIZ_HGT).toString ());
		netdensity	= new JTextField (Integer.valueOf (recognizer.NET_DENSITY).toString ());
		netinminx 	= new JTextField (Integer.valueOf (recognizer.NET_IN_MINX).toString ());
		netinminy 	= new JTextField (Integer.valueOf (recognizer.NET_IN_MINY).toString ());
		netinmina	= new JTextField (Integer.valueOf (recognizer.NET_IN_MINA).toString ());
		netinmemo	= new JTextField (Integer.valueOf (recognizer.NET_IN_MEMO).toString ());
		lmsxmin 		= new JTextField (Integer.valueOf (recognizer.LM_SX_MIN).toString ());
		lmsymin 		= new JTextField (Integer.valueOf (recognizer.LM_SY_MIN).toString ());
		lmhorihgt	= new JTextField (Integer.valueOf (recognizer.LM_HORIZ_HGT).toString ());
		lmdensity	= new JTextField (Integer.valueOf (recognizer.LM_DENSITY).toString ());
		
		view = new JPanel ();
		view.setLayout (new BoxLayout (view, BoxLayout.Y_AXIS));
		view.add (createBallPanel ());
		view.add (createNetPanel ());
		view.add (createLmPanel ());
		scroll = new JScrollPane (view);
		
		setLayout (new GridLayout (1, 1));
		add (scroll);
		
		setVisible(true);
	}
	
	protected void updateValues ()
	{
		recognizer.BALL_SX_MIN 		= Integer.valueOf (ballsxmin.getText ()).intValue ();
		recognizer.BALL_SY_MIN 		= Integer.valueOf (ballsymin.getText ()).intValue ();
		recognizer.BALL_HORIZ_HGT	= Integer.valueOf (ballhorihgt.getText ()).intValue ();
		recognizer.BALL_DENSITY 	= Integer.valueOf (balldensity.getText ()).intValue ();
		recognizer.BALL_XDISP 		= Integer.valueOf (ballxdisp.getText ()).intValue ();
		recognizer.BALL_YDISP 		= Integer.valueOf (ballydisp.getText ()).intValue ();

		recognizer.NET_SX_MIN 		= Integer.valueOf (netsxmin.getText ()).intValue ();
		recognizer.NET_SY_MIN 		= Integer.valueOf (netsymin.getText ()).intValue ();
		recognizer.NET_HORIZ_HGT	= Integer.valueOf (nethorihgt.getText ()).intValue ();
		recognizer.NET_DENSITY 		= Integer.valueOf (netdensity.getText ()).intValue ();
		recognizer.NET_IN_MINX 		= Integer.valueOf (netinminx.getText ()).intValue ();
		recognizer.NET_IN_MINY 		= Integer.valueOf (netinminy.getText ()).intValue ();
		recognizer.NET_IN_MINA 		= Integer.valueOf (netinmina.getText ()).intValue ();
		recognizer.NET_IN_MEMO 		= Integer.valueOf (netinmemo.getText ()).intValue ();

		recognizer.LM_SX_MIN	 	= Integer.valueOf (lmsxmin.getText ()).intValue ();
		recognizer.LM_SY_MIN 		= Integer.valueOf (lmsymin.getText ()).intValue ();
		recognizer.LM_HORIZ_HGT		= Integer.valueOf (lmhorihgt.getText ()).intValue ();
		recognizer.LM_DENSITY 		= Integer.valueOf (lmdensity.getText ()).intValue ();
	}
						
	private JPanel createBallPanel()
	{
		JPanel panel, left, right;
			
		left = new JPanel();
		left.setLayout(new GridLayout (6, 1));
		left.add(new JLabel ("Min width (pix)"));
		left.add(new JLabel ("Min Height (pix)"));
		left.add(new JLabel ("Max above horizont (pix)"));
		left.add(new JLabel ("Min density (area/pix)"));
		left.add(new JLabel ("Fixation X displacement (pix)"));
		left.add(new JLabel ("Fixation Y displacement (pix)"));

		right = new JPanel();
		right.setLayout(new GridLayout (6, 1));
		right.add(ballsxmin);
		right.add(ballsymin);
		right.add(ballhorihgt);
		right.add(balldensity);
		right.add(ballxdisp);
		right.add(ballydisp);

		panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Ball Recognition", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new BorderLayout ());
		panel.add (left, BorderLayout.WEST);
		panel.add (right, BorderLayout.CENTER);
						
		return panel;
	}

	private JPanel createNetPanel()
	{
		JPanel panel, left, right;
		
		left = new JPanel();
		left.setLayout(new GridLayout (8, 1));
		left.add(new JLabel ("Min width (pix)"));
		left.add(new JLabel ("Min Height (pix)"));
		left.add(new JLabel ("Min above horizont (pix)"));
		left.add(new JLabel ("Min density (area/pix)"));
		left.add(new JLabel ("InNet min width (pix)"));
		left.add(new JLabel ("InNet min height (pix)"));
		left.add(new JLabel ("InNet min angle (deg)"));
		left.add(new JLabel ("InNet memory (ms)"));

		right = new JPanel();
		right.setLayout(new GridLayout (8, 1));
		right.add(netsxmin);
		right.add(netsymin);
		right.add(nethorihgt);
		right.add(netdensity);
		right.add(netinminx);
		right.add(netinminy);
		right.add(netinmina);
		right.add(netinmemo);
					
		panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Net Recognition", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new BorderLayout ());
		panel.add (left, BorderLayout.WEST);
		panel.add (right, BorderLayout.CENTER);

		return panel;
	}
	
	private JPanel createLmPanel()
	{
		JPanel panel, left, right;
		
		left = new JPanel();
		left.setLayout(new GridLayout (4, 1));
		left.add(new JLabel ("Min width (pix)"));
		left.add(new JLabel ("Min Height (pix)"));
		left.add(new JLabel ("Min above horizont (pix)"));
		left.add(new JLabel ("Min density (area/pix)"));

		right = new JPanel();
		right.setLayout(new GridLayout (4, 1));
		right.add(lmsxmin);
		right.add(lmsymin);
		right.add(lmhorihgt);
		right.add(lmdensity);
		
		panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Landmark Recognition", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new BorderLayout ());
		panel.add (left, BorderLayout.WEST);
		panel.add (right, BorderLayout.CENTER);

		return panel;
	}
}
