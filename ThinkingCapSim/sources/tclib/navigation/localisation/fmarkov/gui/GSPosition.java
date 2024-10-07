/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 * 
 * Global state
 * 
 * This class manage robot's global state,
 * position and several ratios for knowing
 * estimation quality
 * 
 */

package tclib.navigation.localisation.fmarkov.gui;

import tclib.navigation.localisation.fmarkov.MKPos;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class GSPosition {
	InforPanel infopanel; // Information component - For monitor robot's believe position
	
	double focus; // Area of location in [0,1]
	double reliability; // Signal/noise ratio in [0,1]
	MKPos mkpos; // Estimated robot position
	
	// Constructor
	public GSPosition()
	{
		mkpos = new MKPos(); // Set initial start position and uncertainty
		clear();
		infopanel = null;
	}
	
	// Accessors
	public MKPos getPosition()
	{
		return mkpos;
	}
	
	public void setFocus(double focus)
	{
		this.focus = focus;
	}
	
	public double getFocus()
	{
		return focus;
	}
	
	public void setReliability(double reliability)
	{
		this.reliability = reliability;
	}
	
	public double getReliability()
	{
		return reliability;
	}
	
	public void setInfoPanel(InforPanel infopanel)
	{
		this.infopanel = infopanel;
	}
	
	public void refreshGUI()
	{
		if(infopanel != null)
			infopanel.setMonitorValues(
					Double.toString((double)Math.round(mkpos.getX() * 1000.0) / 1000.0),
					Double.toString((double)Math.round(mkpos.getY() * 1000.0) / 1000.0),
					(int)(reliability * focus * 100.0));
		//System.out.println(" reliability " + reliability + " focus " + focus);
	}
	
	public void clear()
	{
		focus = 1.0;
		reliability = 1.0;
	}
}
