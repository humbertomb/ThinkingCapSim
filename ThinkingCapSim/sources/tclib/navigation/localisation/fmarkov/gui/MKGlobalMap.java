/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 *	Fuzzy Markov Process
 *
 */
 
package tclib.navigation.localisation.fmarkov.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import tc.shared.world.*;
import tclib.navigation.localisation.fmarkov.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */

// * @param
// * @return
// * @see

public class MKGlobalMap {
	public static double RBORDER = 0.3; // Border size (m)
	public static double RPIXELSIZE = 0.2; // Desired pixelation (m)
	public static double SCALEFACTOR = 10.0; // Drawing scale
	
	MK2_5FGrid mk2_5fgrid; // 2.5D Fuzzy grid
	MKCanvas mkcanvas; // Fuzzy Markov GUI Component
	ControlPanel ctrlpanel; // Control component
	InforPanel infopanel; // Information component
	
	// Constructors	
	public MKGlobalMap(World world)
	{
		// Create the frame
		String title = "Fuzzy-Markov Localisation Grid 2.5D";
		JFrame frame = new JFrame(title);
		
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {System.exit(0);}
		});
		
		infopanel = new InforPanel();
		mk2_5fgrid = new MK2_5FGrid(world, RBORDER, RPIXELSIZE);
		mkcanvas = new MKCanvas(mk2_5fgrid, SCALEFACTOR);
		ctrlpanel = new ControlPanel(mk2_5fgrid, mkcanvas);
		
		mkcanvas.setMKPos(mk2_5fgrid.getMKPos());
		
		mkcanvas.setInfoPanel(infopanel);
		mk2_5fgrid.setInfoPanel(infopanel);
		
		frame.getContentPane().add(mkcanvas, BorderLayout.CENTER);
		frame.getContentPane().add(ctrlpanel, BorderLayout.NORTH);
		frame.getContentPane().add(infopanel, BorderLayout.SOUTH);
		
		frame.setVisible(true);
		frame.pack();
	}
	
	// Accessors
	public MKCanvas getMKCanvas()
	{
		return mkcanvas;
	}
	
	public MK2_5FGrid getMKFGrid()
	{
		return mk2_5fgrid;
	}
	
	public static void main(String[] args) 
	{
		try {
			World world = new World("./conf/maps/dulzet.world");
			new MKGlobalMap(world);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}
