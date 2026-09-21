/**
 * 
 * Copyright (c) 2004 University of Murcia (Spain) and Team Chaos, Sweden and Spain.
 * All Rights Reserved.
 * 
 */

package tclib.vision.chaos.gui.ctables;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import tclib.vision.chaos.*;
import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.gui.*;
import tclib.vision.chaos.gui.ctables.colspace.*;
import tclib.vision.chaos.segment.Segmentation;

public class CPColorTable extends JPanel
{
	static public final int				ACT_NONE		= 0;
	static public final int				ACT_ADD		= 1;
	static public final int				ACT_REMOVE	= 2;

	private JButton btundo;
	private JButton btredo;
	
	private JCheckBox viewSeeds;
	private JCheckBox addSeeds;	
	private JCheckBox subSeeds;	
	
	private int selectedChannel = 0;
	private int action = ACT_NONE;	
	private Vector<HashSet<Pixel>> undo = new Vector<HashSet<Pixel>> (); 
	private Vector<HashSet<Pixel>> redo = new Vector<HashSet<Pixel>> ();
	
	protected ChaosPam pam;
	protected ChaosVisionPanel guicamera;	
	protected CPChannelsConfTable cpchannelsconf;
	public CPSpaceWindow cpspacewin;
	protected JLabel	clusters;
		
	public CPColorTable (ChaosVisionPanel guicamera, ChaosPam pam) 
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
		add (cpchannelsconf, BorderLayout.CENTER);
		add (createControlsPanel(), BorderLayout.EAST); 
		add (panel, BorderLayout.SOUTH); 
		
		setVisible(true);
	}

	private JPanel createControlsPanel()
	{
		JPanel panelbuttons = new JPanel();
		panelbuttons.setLayout(new GridLayout(11, 1));
				
		JButton btResetChannels = new JButton("Reset");
		btResetChannels.setToolTipText("Create new seeds in ALL CHANNELS");
		JButton btsave = new JButton("Save");
		JButton btcolspc = new JButton("CSpace");		
		btcolspc.setToolTipText("Color space analysis in 2D and 3D");
		btundo = new JButton("Undo");
		btundo.setEnabled(false);		
		btredo = new JButton("Redo");
		btredo.setEnabled(false);		
		JButton newSeeds = new JButton("New");
		newSeeds.setToolTipText("Create new seeds in current channel");
		newSeeds.setSelected(false);
		
		viewSeeds = new JCheckBox("View");
		viewSeeds.setToolTipText("View seeds in image");
		viewSeeds.setSelected(false);	
		addSeeds = new JCheckBox("Add");
		addSeeds.setToolTipText ("Add seed to current channel");
		addSeeds.setSelected(false);
		addSeeds.setEnabled (viewSeeds.isSelected ());
		subSeeds = new JCheckBox("Remove");
		subSeeds.setToolTipText ("Remove seed from current channel");
		subSeeds.setSelected(false);
		subSeeds.setEnabled (viewSeeds.isSelected ());

		btsave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				pam.saveConf ();
			}
		});	
		btcolspc.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				if (cpspacewin == null)
					cpspacewin	= new CPSpaceWindow ();				
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
				pam.config.channels.at(selectedChannel).resetCluster ();		
				pam.lut.initialise (pam.config.channels);
				guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
				guicamera.redrawBufferedImage ();
			}
		});
		btResetChannels.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
	            if(JOptionPane.showConfirmDialog(null,"Press 'Yes' to apply New Seeds to all channels.","Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)                
	            {
					for(int i=0; i<pam.config.channels.getNumChannels(); i++)
						pam.config.channels.at(i).resetCluster ();				
	            }
				pam.lut.initialise (pam.config.channels);
				guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
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
				guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
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
			}
		});	
		
		panelbuttons.add(btsave);		
		panelbuttons.add(newSeeds);
		panelbuttons.add(btResetChannels);
		panelbuttons.add(btcolspc);
		panelbuttons.add(btundo);
		panelbuttons.add(btredo);		
		panelbuttons.add(viewSeeds);
		panelbuttons.add(addSeeds);	
		panelbuttons.add(subSeeds);	

		return panelbuttons;
	}
			
	private void undo ()
	{
		if (undo.size() > 0)
		{
			HashSet<Pixel>		set;
			
			set = undo.lastElement();
			undo.removeElementAt (undo.size() - 1);
			redo.add(pam.config.channels.at(selectedChannel).getCluster().cloneSeeds());
			
			if (undo.size() == 0)
				btundo.setEnabled(false);
			btredo.setEnabled(true);
			
			pam.config.channels.at(selectedChannel).getCluster().setSeeds (set);
			
			guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
			guicamera.redrawBufferedImage ();
			clusters.setText (pam.config.channels.at (selectedChannel).getCluster ().paramCookedData ());
		}
	}
	
	private void redo ()
	{
		if (redo.size() > 0)
		{
			HashSet<Pixel>		set;
			
			set = redo.lastElement ();
			redo.removeElementAt(redo.size() - 1);
			undo.add(pam.config.channels.at(selectedChannel).getCluster().cloneSeeds());

			if (redo.size() == 0)
				btredo.setEnabled(false);
			btundo.setEnabled(true);
			
			pam.config.channels.at(selectedChannel).getCluster().setSeeds (set);
				
			guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
			guicamera.redrawBufferedImage ();
			clusters.setText (pam.config.channels.at (selectedChannel).getCluster ().paramCookedData ());
		}	
	}
	
	public void modifySeed (int rgb)
	{
		int			hsv;
		Channel		channel;
			
		if (action == ACT_NONE)				return;

		channel	= pam.config.channels.at (selectedChannel);		
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
		
		pam.lut.update (pam.config.channels, selectedChannel);
//		pam.lut.initialise (pam.chs);
		
		guicamera.updateSeeds (channel);
		guicamera.redrawBufferedImage ();
	}
			
	public int getSelectedChannel ()
	{
		return selectedChannel;
	}
	
	public void setSelectedChannel (int ch)
	{
		selectedChannel = ch;

		clusters.setText (pam.config.channels.at (ch).getCluster ().paramCookedData ());

		if (viewSeeds.isSelected ())
		{
			guicamera.updateSeeds (pam.config.channels.at(selectedChannel));
			guicamera.redrawBufferedImage ();
		}
	}
		
	private void testCollisions()
	{
		Vector<Integer> collisions = pam.config.channels.testCollisions();
		
		if (!collisions.isEmpty())
		{
			String msg = new String();
			
			for (int i = 0; i < collisions.size(); i+=2)
				msg = msg + "Channels: " + pam.config.channels.at (((Integer) collisions.get(i)).intValue()).name + "-" + pam.config.channels.at (((Integer) collisions.get(i+1)).intValue()).name + "\n";	
			
			JOptionPane.showMessageDialog(this, msg, "Collision warning", JOptionPane.WARNING_MESSAGE);
		}
	}
}
