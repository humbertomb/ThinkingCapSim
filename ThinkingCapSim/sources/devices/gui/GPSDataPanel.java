/*
 * Created on 28-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.gui;

import javax.swing.*;
import java.awt.*;

import devices.data.*;

import devices.pos.*;

/**
 * @author Juan Pedro Canovas Qui–onero
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class GPSDataPanel extends JPanel
{
	// Data components
	protected String				name;
	
	// Main data panels
	protected JPanel				topPA		= new JPanel ();
	protected Satellites			satsCO		= new Satellites ();
	
	// UTM position panel
	protected JPanel				utmPA		= new JPanel ();
	protected JLabel				u_eastLA		= new JLabel ();
	protected JLabel				u_northLA	= new JLabel ();
	protected JLabel				u_zoneLA		= new JLabel ();
	
	// LLA position panel
	protected JPanel				llaPA		= new JPanel ();
	protected JLabel				l_lonLA		= new JLabel ();
	protected JLabel				l_latLA		= new JLabel ();
	protected JLabel				l_altLA		= new JLabel ();

	// Velocity panel
	protected JPanel				velPA		= new JPanel ();
	protected JLabel				v_grnLA		= new JLabel ();
	protected JLabel				v_upLA		= new JLabel ();
  
	// GPS data panel
	protected JPanel				datPA		= new JPanel ();
	protected JLabel				d_fixLA		= new JLabel ();
	protected JLabel				d_numLA		= new JLabel ();
  
	// Constructors
	public GPSDataPanel ()
	{
		try { initComponents (); } catch (Exception e) { e.printStackTrace(); }	
	}
	
	// Class methods
	static public String format (double v)
	{
		return LLAPos.zeroFill(Math.round (v * 1000.0) / 1000.0, 3, 1); 
	}
	
	// Accessors
	public final void			setName (String name) 	{ this.name = name; }
	public final String			getName ()				{ return name; }
	public final boolean			isName (String name)		{ return this.name.equalsIgnoreCase (name); }

	// Instance methods
	private void initComponents () throws Exception
	{
		Font			courier;
				
		// UTM position panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		u_northLA.setFont (courier);
		u_eastLA.setFont (courier);
		u_zoneLA.setFont (courier);
		utmPA.setVisible (true);
		utmPA.setLayout (new GridLayout(3,1));
		utmPA.add (u_eastLA);
		utmPA.add (u_northLA);
		utmPA.add (u_zoneLA);
		utmPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "UTM Position", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// LLA position panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		l_lonLA.setFont (courier);
		l_latLA.setFont (courier);
		l_altLA.setFont (courier);
		llaPA.setVisible (true);
		llaPA.setLayout (new GridLayout(3,1));
		llaPA.add (l_latLA);
		llaPA.add (l_lonLA);
		llaPA.add (l_altLA);
		llaPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "LLA Position", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// Velocity panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		v_grnLA.setFont (courier);
		v_upLA.setFont (courier);
		velPA.setVisible (true);
		velPA.setLayout (new GridLayout(3,1));
		velPA.add (v_grnLA);
		velPA.add (v_upLA);
		velPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Velocity", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// Velocity panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		d_fixLA.setFont (courier);
		d_numLA.setFont (courier);
		datPA.setVisible (true);
		datPA.setLayout (new GridLayout(3,1));
		datPA.add (d_fixLA);
		datPA.add (d_numLA);
		datPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "GPS Info", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// Top panel
		topPA.setVisible (true);
		topPA.setLayout (new GridLayout(4, 1));
		topPA.add (utmPA);
		topPA.add (llaPA);
		topPA.add (velPA);
		topPA.add (datPA);

		setLayout(new BorderLayout ());
		setBorder (BorderFactory.createLineBorder (new Color(153, 153, 153)));
		add (satsCO, BorderLayout.CENTER);		
		add (topPA, BorderLayout.NORTH);
	}

	public void update (GPSData data, SatelliteData[] sats)
	{
		UTMPos			utm;
		LLAPos			lla;
		double 			v, vx, vy;	 

		utm	= data.getPos ();
		lla	= GeoPos.UTMtoLL (utm, "WGS-84");
      
		u_northLA.setText ("North: "+Math.round (utm.getNorth ())+" m");
		u_eastLA.setText ("East:  "+Math.round (utm.getEast ())+" m");
		u_zoneLA.setText ("Zone:  "+utm.getZone ());
  
		l_latLA.setText ("Lat: "+lla.getLatStr ());
		l_lonLA.setText ("Lon: "+lla.getLonStr ());
		l_altLA.setText ("Alt: "+LLAPos.whiteFill ((int) data.getAltitude (), 3)+" m");

		vx	= data.getVel ().x ();
		vy	= data.getVel ().y ();
		v	= Math.sqrt (vx * vx + vy * vy);
		v_grnLA.setText ("Ground: "+format (v)+" m/s");
		v_upLA.setText ("Upward: "+format (data.getVel ().z ())+" m/s");

		d_fixLA.setText ("Fix:     "+GPSData.fixToString (data.getFix ()));
		d_numLA.setText ("NumSats: "+data.getNumSat());

		satsCO.update (sats);
   }
}