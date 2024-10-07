/*
 * (c) 2004 David Herrero Perez
 *
 *	Information Panel
 *
 */

package tclib.navigation.localisation.fmarkov.gui;

import java.awt.*;
import javax.swing.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class InforPanel extends JPanel {
	
	// For Pointer Component
	public static final String FRAME_POINTER_NAME = "Pointer";
	public static final String FRAME_MONITOR_NAME = "Localisation Monitor";
	
	public static final String P_GRID_POS_X_NAME = "Gx: ";
	public static final String P_GRID_POS_Y_NAME = "Gy: ";
	public static final String P_REAL_POS_X_NAME = "Rx: ";
	public static final String P_REAL_POS_Y_NAME = "Ry: ";
	
	protected JLabel lbpgx;
	protected JLabel lbpgy;
	protected JLabel lbprx;
	protected JLabel lbpry;
	
	// For Localisating Monitor
	public static final String REAL_POS_X_NAME = "Rx: ";
	public static final String REAL_POS_Y_NAME = "Ry: ";
	JProgressBar reliability;
	
	protected JLabel lbrx;
	protected JLabel lbry;
	
	public InforPanel()
	{
		super(new BorderLayout());
		initComponents();
		setVisible(true);
	}
	
	public void setPointerValues(String strgx, String strgy, String strrx, String strry)
	{
		lbpgx.setText(P_GRID_POS_X_NAME + strgx);
		lbpgy.setText(P_GRID_POS_Y_NAME + strgy);
		lbprx.setText(P_REAL_POS_X_NAME + strrx);
		lbpry.setText(P_REAL_POS_Y_NAME + strry);
	}
	
	public void setMonitorValues(String strrx, String strry, int rel)
	{
		lbrx.setText(REAL_POS_X_NAME + strrx);
		lbry.setText(REAL_POS_Y_NAME + strry);
		
		reliability.setValue(rel);
	}
	
	private void initComponents() 
	{
		add(createPointerComponent(), BorderLayout.WEST);
		add(createMonitorComponent(), BorderLayout.EAST);
	}
	
	private JPanel createPointerComponent()
	{
		JPanel mainpanel;
		Box mainbox; 
		JPanel gridpospanel, pospanel;
		
		mainpanel = new JPanel(new BorderLayout());
		mainpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), FRAME_POINTER_NAME, 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		gridpospanel = new JPanel();
		gridpospanel.setLayout(new BoxLayout(gridpospanel, BoxLayout.Y_AXIS));
		
		lbpgx = new JLabel(P_GRID_POS_X_NAME + "-----");
		lbpgy = new JLabel(P_GRID_POS_Y_NAME + "-----");
		
		gridpospanel.add(lbpgx);
		gridpospanel.add(lbpgy);
		
		pospanel = new JPanel();
		pospanel.setLayout(new BoxLayout(pospanel, BoxLayout.Y_AXIS));
		
		lbprx = new JLabel(P_REAL_POS_X_NAME + "-----");
		lbpry = new JLabel(P_REAL_POS_X_NAME + "-----");
		
		pospanel.add(lbprx);
		pospanel.add(lbpry);
		
		mainbox = new Box(BoxLayout.X_AXIS);
		
		mainbox.add(gridpospanel);
		int width = 10;
	    mainbox.add(Box.createHorizontalStrut(width));
		mainbox.add(pospanel);
	    
		mainpanel.add(mainbox);
		
		return mainpanel;
	}
	
	private JPanel createMonitorComponent()
	{
		JPanel mainpanel;
		Box mainbox;
		JPanel relpanel, pospanel;
		
		mainpanel = new JPanel(new BorderLayout());
		mainpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), FRAME_MONITOR_NAME, 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		pospanel = new JPanel();
		pospanel.setLayout(new BoxLayout(pospanel, BoxLayout.Y_AXIS));
		
		lbrx = new JLabel(REAL_POS_X_NAME + "-----");
		lbry = new JLabel(REAL_POS_X_NAME + "-----");
		
		pospanel.add(lbrx);
		pospanel.add(lbry);
		
		relpanel = new JPanel();
		relpanel.setLayout(new BoxLayout(relpanel, BoxLayout.Y_AXIS));
		
		// Create a horizontal progress bar
		int minimum = 0;
		int maximum = 100;
		reliability = new JProgressBar(minimum, maximum);
		reliability.setStringPainted(true); // Overlay a string showing the percentage done
		
		reliability.setValue(50);
		
		JLabel lbrel = new JLabel("Reliability");
		lbrel.setForeground(new Color(29,14,237));
		
		relpanel.add(lbrel);
		relpanel.add(reliability);
		
		mainbox = new Box(BoxLayout.X_AXIS);
		
		mainbox.add(pospanel);
		int width = 10;
	    mainbox.add(Box.createHorizontalStrut(width));
		mainbox.add(relpanel);
	    
		mainpanel.add(mainbox);
		
		return mainpanel;
	}
}
