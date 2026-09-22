/**
 * @author Humberto Martinez Barbera 
 */
package tcrob.umu.quaky2.gui;

import java.awt.*;
import javax.swing.*;

import java.awt.event.*;
import java.util.function.IntConsumer;

import tclib.vision.chaos.channels.*;
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
	
	// The channel each object is looked for in
	private ChannelSelector carpetch;
	private ChannelSelector ballch;
	private ChannelSelector net1ch;
	private ChannelSelector net2ch;
	private ChannelSelector lmch;

	public SoccerRecognizer			recognizer;
	protected Channels				channels;
	
	public SoccerRecognitonConfigPanel (SoccerRecognizer recognizer, Channels channels)
	{
		JScrollPane	scroll;
		JPanel		view;
		
		this.recognizer = recognizer;
		this.channels	= channels;

		carpetch	= new ChannelSelector (recognizer.CARPET_CHANNEL, ch -> recognizer.CARPET_CHANNEL = ch);
		ballch		= new ChannelSelector (recognizer.BALL_CHANNEL, ch -> recognizer.BALL_CHANNEL = ch);
		net1ch		= new ChannelSelector (recognizer.NET1_CHANNEL, ch -> recognizer.NET1_CHANNEL = ch);
		net2ch		= new ChannelSelector (recognizer.NET2_CHANNEL, ch -> recognizer.NET2_CHANNEL = ch);
		lmch		= new ChannelSelector (recognizer.LM_CHANNEL, ch -> recognizer.LM_CHANNEL = ch);
		
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
		view.add (createCarpetPanel ());
		view.add (createBallPanel ());
		view.add (createNetPanel ());
		view.add (createLmPanel ());
		scroll = new JScrollPane (view);
		
		setLayout (new GridLayout (1, 1));
		add (scroll);

		// a value typed in takes effect when Enter is pressed or the field is left
		for (JTextField f : new JTextField[] { ballsxmin, ballsymin, ballhorihgt, balldensity, ballxdisp, ballydisp,
												netsxmin, netsymin, nethorihgt, netdensity, netinminx, netinminy, netinmina, netinmemo,
												lmsxmin, lmsymin, lmhorihgt, lmdensity })
		{
			f.addActionListener (_ -> updateValues ());
			f.addFocusListener (new FocusAdapter () { public void focusLost (FocusEvent e) { updateValues (); } });
		}
		
		setVisible(true);
	}

	/** The channels to choose from changed (a configuration was made or loaded). */
	public void setChannels (Channels channels)
	{
		this.channels	= channels;
		for (ChannelSelector s : new ChannelSelector[] { carpetch, ballch, net1ch, net2ch, lmch })
			s.refresh ();
	}

	/**
	 * A selector of one of the channels of the configuration, each shown with
	 * its colour and name; what is chosen is given to the recognizer.
	 */
	protected class ChannelSelector extends JComboBox<Integer>
	{
		private static final long	serialVersionUID = 1L;
		private final IntConsumer	chosen;
		private int					current;
		private boolean				filling;

		ChannelSelector (int ch, IntConsumer chosen)
		{
			this.chosen		= chosen;
			this.current	= ch;
			setRenderer (new DefaultListCellRenderer ()
			{
				private static final long	serialVersionUID = 1L;

				public Component getListCellRendererComponent (JList<?> list, Object value, int index, boolean sel, boolean focus)
				{
					super.getListCellRendererComponent (list, value, index, sel, focus);
					int		i = (value instanceof Integer) ? (Integer) value : -1;
					if ((channels != null) && (i >= 0) && (i < channels.size ()))
					{
						Channel		c = channels.at (i);
						setText (i + "  " + c.name);
						setIcon (swatch ((c.color != null) ? c.color : Color.GRAY));
					}
					else
					{
						setText ((i >= 0) ? i + "  (no such channel)" : "");
						setIcon (null);
					}
					return this;
				}
			});
			addActionListener (_ ->
			{
				if (filling || (getSelectedItem () == null))		return;
				current	= (Integer) getSelectedItem ();
				chosen.accept (current);
			});
			refresh ();
		}

		/** The channels there are now, keeping the one chosen (even if it is not there: it is shown as such). */
		void refresh ()
		{
			filling	= true;
			removeAllItems ();
			int		n = (channels != null) ? channels.size () : 0;
			for (int i = 0; i < n; i++)		addItem (i);
			if (current >= n)				addItem (current);
			setSelectedItem (current);
			filling	= false;
		}
	}

	/** A small square of a colour, with a thin border. */
	static protected Icon swatch (final Color c)
	{
		return new Icon ()
		{
			public int getIconWidth ()		{ return 14; }
			public int getIconHeight ()		{ return 12; }
			public void paintIcon (Component comp, Graphics g, int x, int y)
			{
				g.setColor (c);				g.fillRect (x, y, 13, 11);
				g.setColor (Color.DARK_GRAY);	g.drawRect (x, y, 13, 11);
			}
		};
	}
	
	protected void updateValues ()
	{
		try { values (); }
		catch (NumberFormatException e)		{ Toolkit.getDefaultToolkit ().beep (); }		// what is not a number is not taken
	}

	protected void values ()
	{
		recognizer.BALL_SX_MIN 		= Integer.valueOf (ballsxmin.getText ().trim ()).intValue ();
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
						
	/** The floor: the channel the horizon is found from (and the nets are fitted against). */
	private JPanel createCarpetPanel()
	{
		JPanel panel;

		panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Carpet (Horizon)", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new BorderLayout ());
		panel.add (channelLines (new String[] { "Channel" }, carpetch), BorderLayout.NORTH);

		return panel;
	}

	/**
	 * The first lines of a group: the channel (or channels) its object is looked
	 * for in, each the whole width of the group (the names of the channels do not
	 * fit in the column of the values).
	 */
	private JPanel channelLines (String[] labels, ChannelSelector... selectors)
	{
		JPanel		lines = new JPanel (new GridLayout (selectors.length, 1));

		for (int i = 0; i < selectors.length; i++)
		{
			JPanel		line = new JPanel (new BorderLayout (4, 0));
			line.add (new JLabel (labels[i]), BorderLayout.WEST);
			line.add (selectors[i], BorderLayout.CENTER);
			lines.add (line);
		}
		return lines;
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
		panel.add (channelLines (new String[] { "Channel" }, ballch), BorderLayout.NORTH);
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
		panel.add (channelLines (new String[] { "Net 1 channel", "Net 2 channel" }, net1ch, net2ch), BorderLayout.NORTH);
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
		panel.add (channelLines (new String[] { "Channel" }, lmch), BorderLayout.NORTH);
		panel.add (left, BorderLayout.WEST);
		panel.add (right, BorderLayout.CENTER);

		return panel;
	}
}
