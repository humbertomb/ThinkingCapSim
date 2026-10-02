/**
 * 
 * Copyright (c) 2004 University of Murcia (Spain) and Team Chaos, Sweden and Spain.
 * All Rights Reserved.
 * 
 */

package tcrob.umu.soccer.gui.ctables;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.Segmentation;
import tcrob.umu.soccer.SoccerVision;
import tcrob.umu.soccer.gui.SoccerVisionPanel;
import tcrob.umu.soccer.gui.ctables.colspace.CPSpaceWindow;

public class CPColorTable extends JPanel
{
	static public final int				ACT_NONE		= 0;
	static public final int				ACT_ADD		= 1;
	static public final int				ACT_REMOVE	= 2;

	private JButton btundo;
	private JButton btredo;
	
	private JToggleButton viewSeeds;
	private JToggleButton addSeeds;	
	private JToggleButton subSeeds;	
	
	private int selectedChannel = 0;
	private int action = ACT_NONE;	
	private Vector<HashSet<Pixel>> undo = new Vector<HashSet<Pixel>> (); 
	private Vector<HashSet<Pixel>> redo = new Vector<HashSet<Pixel>> ();
	
	protected SoccerVision pam;
	protected SoccerVisionPanel guicamera;	
	protected CPChannelsConfTable cpchannelsconf;
	public CPSpaceWindow cpspacewin;
	protected JLabel	clusters;
		
	public CPColorTable (SoccerVisionPanel guicamera, SoccerVision pam) 
	{
		this.guicamera = guicamera;
		this.pam = pam;
		
		cpchannelsconf	= new CPChannelsConfTable(this, pam);
		clusters			= new JLabel ();

		JPanel panel = new JPanel();
		panel.setLayout (new BorderLayout ());
		panel.add (new JLabel ("Cluster: "), BorderLayout.WEST);
		panel.add (clusters, BorderLayout.CENTER);

		setLayout (new BorderLayout());
		add (createControlsPanel(), BorderLayout.NORTH); 
		add (cpchannelsconf, BorderLayout.CENTER);
		add (panel, BorderLayout.SOUTH); 
		
		setVisible(true);
	}

	/** A button of the toolbar: small, so that they all fit across the window. */
	static private <B extends AbstractButton> B small (B b)
	{
		b.setFont (b.getFont ().deriveFont (10.5f));
		b.setMargin (new Insets (2, 3, 2, 3));
		b.setFocusable (false);
		return b;
	}

	/**
	 * A FlowLayout that asks for as many rows as its components need at the width
	 * it has, so that a toolbar wider than its window goes on in a second row
	 * instead of losing the buttons at its right.
	 */
	static private class Wrap extends FlowLayout
	{
		Wrap ()								{ super (FlowLayout.LEFT, 1, 1); }

		public Dimension preferredLayoutSize (Container target)	{ return size (target, true); }
		public Dimension minimumLayoutSize (Container target)	{ Dimension d = size (target, false); d.width -= getHgap () + 1; return d; }

		private Dimension size (Container target, boolean preferred)
		{
			synchronized (target.getTreeLock ())
			{
				Container	c = target;
				int			width;
				Insets		in = target.getInsets ();
				int			max, w = 0, h = 0, rw = 0, rh = 0;

				while ((c.getSize ().width == 0) && (c.getParent () != null))	c = c.getParent ();
				width	= c.getSize ().width;
				if (width == 0)					width = Integer.MAX_VALUE;
				max		= width - (in.left + in.right + getHgap () * 2);

				for (Component m : target.getComponents ())
				{
					if (!m.isVisible ())		continue;
					Dimension	d = preferred ? m.getPreferredSize () : m.getMinimumSize ();
					if ((rw > 0) && (rw + getHgap () + d.width > max))
					{
						w	= Math.max (w, rw);
						h	+= rh + getVgap ();
						rw	= 0;
						rh	= 0;
					}
					rw	+= (rw > 0 ? getHgap () : 0) + d.width;
					rh	= Math.max (rh, d.height);
				}
				w	= Math.max (w, rw);
				h	+= rh;
				return new Dimension (w + in.left + in.right + getHgap () * 2, h + in.top + in.bottom + getVgap () * 2);
			}
		}
	}

	/** The toolbar over the table: the seeds of the channels (new, reset), the colour space, undo and redo, and what the image shows and the clicks do. */
	private JToolBar createControlsPanel()
	{
		JToolBar panelbuttons = new JToolBar ();
		panelbuttons.setFloatable (false);
		panelbuttons.setRollover (true);
		panelbuttons.setLayout (new Wrap ());					// the window is narrower than the buttons: they go on as many rows as needed
				
		JButton btResetChannels = new JButton("Reset");
		btResetChannels.setToolTipText("Create new seeds in ALL CHANNELS");
		JButton btcolspc = new JButton("CSpace");		
		btcolspc.setToolTipText("Color space analysis in 2D and 3D");
		btundo = new JButton("Undo");
		btundo.setEnabled(false);		
		btredo = new JButton("Redo");
		btredo.setEnabled(false);		
		JButton newSeeds = new JButton("New");
		newSeeds.setToolTipText("Create new seeds in current channel");
		newSeeds.setSelected(false);
		
		viewSeeds = new JToggleButton("View");
		viewSeeds.setToolTipText("View seeds in image");
		viewSeeds.setSelected(false);	
		addSeeds = new JToggleButton("Add");
		addSeeds.setToolTipText ("Add seed to current channel");
		addSeeds.setSelected(false);
		addSeeds.setEnabled (viewSeeds.isSelected ());
		subSeeds = new JToggleButton("Remove");
		subSeeds.setToolTipText ("Remove seed from current channel");
		subSeeds.setSelected(false);
		subSeeds.setEnabled (viewSeeds.isSelected ());

		btcolspc.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				if (cpspacewin == null)
					cpspacewin	= new CPSpaceWindow ();				
				else
					cpspacewin.setVisible (true);						// it was closed: closing only hides it
			}
		});	
		btundo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				undo ();
			}
		});	
		btredo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				redo ();
			}
		});	
		newSeeds.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				pam.vconfig.channels.at(selectedChannel).resetCluster ();		
				pam.lut.initialise (pam.vconfig.channels);
				guicamera.updateSeeds (shownSeeds ());
				guicamera.redrawBufferedImage ();
			}
		});
		btResetChannels.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
	            if(JOptionPane.showConfirmDialog(null,"Press 'Yes' to apply New Seeds to all channels.","Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)                
	            {
					for(int i=0; i<pam.vconfig.channels.getNumChannels(); i++)
						pam.vconfig.channels.at(i).resetCluster ();				
	            }
				pam.lut.initialise (pam.vconfig.channels);
				guicamera.updateSeeds (shownSeeds ());
				guicamera.redrawBufferedImage ();
			}
		});	
		viewSeeds.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				addSeeds.setEnabled (viewSeeds.isSelected ());
				subSeeds.setEnabled (viewSeeds.isSelected ());
				if (!viewSeeds.isSelected ())
				{
					addSeeds.setSelected (false);
					subSeeds.setSelected (false);
					action = ACT_NONE;
				}
				guicamera.updateSeeds (shownSeeds ());
				guicamera.redrawBufferedImage ();
			}
		});	
		addSeeds.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				if (addSeeds.isSelected ())
				{
					subSeeds.setSelected (false);
					action = ACT_ADD;
				}
				else
					action = ACT_NONE;							// let go: the clicks do nothing again
			}
		});	
		subSeeds.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				if (subSeeds.isSelected ())
				{
					addSeeds.setSelected (false);
					action = ACT_REMOVE;
				}
				else
					action = ACT_NONE;
			}
		});	
		
		panelbuttons.add(small (newSeeds));
		panelbuttons.add(small (btResetChannels));
		panelbuttons.add(small (btcolspc));
		panelbuttons.addSeparator (new Dimension (4, 4));
		panelbuttons.add(small (btundo));
		panelbuttons.add(small (btredo));		
		panelbuttons.addSeparator (new Dimension (4, 4));
		panelbuttons.add(small (viewSeeds));
		panelbuttons.add(small (addSeeds));	
		panelbuttons.add(small (subSeeds));	

		return panelbuttons;
	}
			
	private void undo ()
	{
		if (undo.size() > 0)
		{
			HashSet<Pixel>		set;
			
			set = undo.lastElement();
			undo.removeElementAt (undo.size() - 1);
			redo.add(pam.vconfig.channels.at(selectedChannel).getCluster().cloneSeeds());
			
			if (undo.size() == 0)
				btundo.setEnabled(false);
			btredo.setEnabled(true);
			
			pam.vconfig.channels.at(selectedChannel).getCluster().setSeeds (set);
			
			guicamera.updateSeeds (shownSeeds ());
			guicamera.redrawBufferedImage ();
			clusters.setText (pam.vconfig.channels.at (selectedChannel).getCluster ().paramCookedData ());
		}
	}
	
	private void redo ()
	{
		if (redo.size() > 0)
		{
			HashSet<Pixel>		set;
			
			set = redo.lastElement ();
			redo.removeElementAt(redo.size() - 1);
			undo.add(pam.vconfig.channels.at(selectedChannel).getCluster().cloneSeeds());

			if (redo.size() == 0)
				btredo.setEnabled(false);
			btundo.setEnabled(true);
			
			pam.vconfig.channels.at(selectedChannel).getCluster().setSeeds (set);
				
			guicamera.updateSeeds (shownSeeds ());
			guicamera.redrawBufferedImage ();
			clusters.setText (pam.vconfig.channels.at (selectedChannel).getCluster ().paramCookedData ());
		}	
	}
	
	public void modifySeed (int rgb)
	{
		int			hsv;
		Channel		channel;
			
		if (action == ACT_NONE)				return;

		channel	= pam.vconfig.channels.at (selectedChannel);		
		hsv	= Segmentation.rgbToHsv (rgb);

		undo.add (channel.getCluster ().cloneSeeds());
		redo.removeAllElements ();
		btundo.setEnabled (true);
		btredo.setEnabled (false);

		switch (action)
		{
		case ACT_ADD:
			channel.addSeed (hsv);
			break;
			
		case ACT_REMOVE:
			channel.removeSeed (hsv);
			break;
		}
		
		testCollisions ();
		clusters.setText (channel.getCluster ().paramCookedData ());
		
		pam.lut.update (pam.vconfig.channels, selectedChannel);
//		pam.lut.initialise (pam.chs);
		
		guicamera.updateSeeds (shownSeeds ());
		guicamera.redrawBufferedImage ();
	}
			
	/**
	 * The configuration of the vision was replaced (new, loaded): its channels
	 * are shown, the first one selected, and what could be undone is forgotten.
	 */
	public void configChanged ()
	{
		undo.removeAllElements ();
		redo.removeAllElements ();
		btundo.setEnabled (false);
		btredo.setEnabled (false);
		selectedChannel	= 0;
		cpchannelsconf.refresh ();
		clusters.setText ((pam.vconfig.channels.size () > 0) ? pam.vconfig.channels.at (0).getCluster ().paramCookedData () : "");
		guicamera.updateSeeds (shownSeeds ());
	}

	/** The channel whose seeds the image shows: the selected one while View is on, none otherwise. */
	public Channel shownSeeds ()
	{
		return (viewSeeds.isSelected () && (selectedChannel < pam.vconfig.channels.size ())) ? pam.vconfig.channels.at (selectedChannel) : null;
	}

	public int getSelectedChannel ()
	{
		return selectedChannel;
	}
	
	public void setSelectedChannel (int ch)
	{
		selectedChannel = ch;

		clusters.setText (pam.vconfig.channels.at (ch).getCluster ().paramCookedData ());

		if (viewSeeds.isSelected ())
		{
			guicamera.updateSeeds (shownSeeds ());
			guicamera.redrawBufferedImage ();
		}
	}
		
	private void testCollisions()
	{
		Vector<Integer> collisions = pam.vconfig.channels.testCollisions();
		
		if (!collisions.isEmpty())
		{
			String msg = new String();
			
			for (int i = 0; i < collisions.size(); i+=2)
				msg = msg + "Channels: " + pam.vconfig.channels.at (((Integer) collisions.get(i)).intValue()).name + "-" + pam.vconfig.channels.at (((Integer) collisions.get(i+1)).intValue()).name + "\n";	
			
			JOptionPane.showMessageDialog(this, msg, "Collision warning", JOptionPane.WARNING_MESSAGE);
		}
	}
}
