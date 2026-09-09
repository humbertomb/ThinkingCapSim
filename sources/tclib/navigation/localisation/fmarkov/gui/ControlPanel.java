/*
 * (c) 2004 David Herrero Perez
 *
 * Panel with several events for Fuzzy Markov Localisation Process
 *
 */
 
package tclib.navigation.localisation.fmarkov.gui;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import tclib.navigation.localisation.fmarkov.*;

public class ControlPanel extends JPanel {
	class ButtonControlAction extends AbstractAction {
		
		public ButtonControlAction(String name) {
			super(name);	
		}
		
		public void actionPerformed(ActionEvent e) {
			String sevent = e.getActionCommand();
			
			if (sevent.equalsIgnoreCase(INIT_ACTION))
			{
				//mkgrid.initMap(mkcanvas);
				mkgrid.initialiseMap(5.0, 2.0, -Math.toRadians(90.0), 1.5, 1.5, Math.toRadians(100.0));
				mkgrid.locate();
				mkcanvas.repaint();
			} else if (sevent.equalsIgnoreCase(RESET_ACTION)) {
				mkgrid.clearGrid();
				mkgrid.locate();
				mkcanvas.repaint();
			} else if (sevent.equalsIgnoreCase(DEBUG_ACTION)) {
				///*
				mkgrid.introducePerceptions(
						15.7, // rho_percept
						-1.3698 // theta_percept
						);
				//*/
				///*
				mkgrid.introducePerceptions(
						10.14, // rho_percept
						-0.1438 // theta_percept
						);
				//*/
				///*
				mkgrid.introducePerceptions(
						10.93, // rho_percept
						0.2483 // theta_percept
						);
				//*/
				///*
				mkgrid.introducePerceptions(
						3.61, // rho_percept
						1.7872 // theta_percept
						);
				//*/
				
				mkgrid.locate();
				mkcanvas.repaint();
			} else if (sevent.equalsIgnoreCase(CONVOLVE_ACTION)) {
				//mkgrid.convolve(0.5, 0.0, Math.toRadians(30.0));
				//mkgrid.convolve(0.6, 0.0, Math.toRadians(0.0));
				mkgrid.convolve(0.05, -0.0005, Math.toRadians(0.0));
				//mkgrid.convolve(0.7, 0.0, 0.0);
				//mkgrid.convolve(-0.7, -0.8, 0.0);
				mkcanvas.repaint();
			} else if (sevent.equalsIgnoreCase(LOCALISE_ACTION)) {
				mkgrid.locate();
				mkcanvas.repaint();
			}
		}
	}
	
	class CheckBoxControlAction extends AbstractAction {
		
		public CheckBoxControlAction(String name) {
			super(name);	
		}
		
		public void actionPerformed(ActionEvent e) {
			// Perform action
			JCheckBox cb = (JCheckBox)e.getSource();
			String sevent = e.getActionCommand();
			
			if (sevent.equalsIgnoreCase(SHOW_ANGLES_ACTION))
			{
				mkcanvas.setShowAngle(cb.isSelected());
				mkcanvas.repaint();
			}
		}
	}
	
	static protected final String INIT_ACTION = "Init";
	static protected final String RESET_ACTION = "Reset";
	static protected final String DEBUG_ACTION = "Debug";
	static protected final String CONVOLVE_ACTION = "Convolve";
	static protected final String LOCALISE_ACTION = "Localise";
	
	static protected final String BORDER_CONTROL_ACTION = "Cotrol Actions";
	
	static protected final String SHOW_ANGLES_ACTION = "Show Angles";
	
	MKCanvas mkcanvas; // Fuzzy Markov GUI Component
	MK2_5FGrid mkgrid;
	
	public ControlPanel(MK2_5FGrid mkgrid, MKCanvas mkcanvas)
	{
		this.mkcanvas = mkcanvas;
		this.mkgrid = mkgrid;
		
		initComponents();
	}
	
	private void initComponents()
	{
		setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), BORDER_CONTROL_ACTION, 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		setLayout(new BorderLayout());
    	
		add(createControlPanel(), BorderLayout.WEST);
		add(createConfigPanel(), BorderLayout.EAST);
		
		setVisible(true);
	}
	
	private JPanel createControlPanel()
	{
		JPanel panel = new JPanel();
		
		JButton bt_init_act = new JButton();
		bt_init_act.setAction(new ButtonControlAction(INIT_ACTION));
		panel.add(bt_init_act);
		
		JButton bt_res_act = new JButton();
		bt_res_act.setAction(new ButtonControlAction(RESET_ACTION));
		panel.add(bt_res_act);
		
		JButton bt_deb_act = new JButton();
		bt_deb_act.setAction(new ButtonControlAction(DEBUG_ACTION));
		panel.add(bt_deb_act);
		
		JButton bt_con_act = new JButton();
		bt_con_act.setAction(new ButtonControlAction(CONVOLVE_ACTION));
		panel.add(bt_con_act);
		
		JButton bt_loc_act = new JButton();
		bt_loc_act.setAction(new ButtonControlAction(LOCALISE_ACTION));
		panel.add(bt_loc_act);
		
		return panel;
	}
	
	private JPanel createConfigPanel()
	{
		JPanel panel = new JPanel();
		
		JCheckBox jcbShowAngles = new JCheckBox();
		jcbShowAngles.setAction(new CheckBoxControlAction(SHOW_ANGLES_ACTION));
		panel.add(jcbShowAngles);
		
		return panel;
	}
}
