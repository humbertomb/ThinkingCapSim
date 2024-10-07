package tclib.navigation.localisation.outdoor.gui;

import java.awt.*;

import javax.swing.*;

import devices.data.*;
import devices.pos.Position;
import wucore.utils.math.*;


public class FilterDataPanel extends JPanel
{
	static public final int			DIGITS		= 3;
	
	// Main data panels
	protected JPanel				topPA		= new JPanel ();
	
	// Euler angles panel
	protected JPanel				eulerPA		= new JPanel ();
	protected JLabel				e_rollLA	= new JLabel ();
	protected JLabel				e_pitchLA	= new JLabel ();
	protected JLabel				e_yawLA		= new JLabel ();
	
	// UTM position panel
	protected JPanel				utmPA		= new JPanel ();
	protected JLabel				u_eastLA		= new JLabel ();
	protected JLabel				u_northLA	= new JLabel ();
	protected JLabel				u_zoneLA		= new JLabel ();

	// Constructors
	public FilterDataPanel ()
	{
		try { initComponents (); } catch (Exception e) { e.printStackTrace(); }	
	}
	
	static private String format (double value, int dec)
	{
		int			i;
		String		str;
		double		times;
		
		// Rounding
		for (i = 0, times = 1.0; i < dec; i++)
			times /= 10.0;
		value	= value + 0.5 * times;
		if ((value > -times) && (value < times))
			value = 0.0;
		
		// String adjusting
		str		= Double.toString (value);
		while (str.length () - str.indexOf ('.') > dec + 1)
			str		= str.substring (0, str.length () - 1);
		while (str.length () - str.indexOf ('.') < dec + 1)
			str		= str + "0";
		
		return str;
	}
	
	// Instance methods
	private void initComponents () throws Exception
	{
		Font			courier;
		
		// Euler angles panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		e_rollLA.setFont (courier);
		e_pitchLA.setFont (courier);
		e_yawLA.setFont (courier);
		eulerPA.setVisible (true);
		eulerPA.setLayout (new GridLayout(3,1));
		eulerPA.add (e_rollLA);
		eulerPA.add (e_pitchLA);
		eulerPA.add (e_yawLA);
		eulerPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Euler Angles", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
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
		
		// Top panel
		topPA.setVisible (true);
		topPA.setLayout (new GridLayout(1, 2));
		topPA.add (eulerPA);
		topPA.add (utmPA);
		
		setLayout(new BorderLayout ());
		setBorder (BorderFactory.createLineBorder (new Color(153, 153, 153)));
		add (topPA, BorderLayout.NORTH);
	}
	
	public void update (InsData insdata, Position pos)
	{		  
		if (insdata != null){
		e_rollLA.setText ("Roll:  "+format (insdata.getRoll ()*Angles.RTOD, DIGITS)+" deg");
		e_pitchLA.setText ("Pitch: "+format (insdata.getPitch ()*Angles.RTOD, DIGITS)+" deg");
		e_yawLA.setText ("Yaw:   "+format (insdata.getYaw ()*Angles.RTOD, DIGITS)+" deg");
		}
		if (pos != null){
		u_northLA.setText ("North: "+Math.round (pos.y ())+" m");
		u_eastLA.setText ("East:  "+Math.round (pos.x ())+" m");
		u_zoneLA.setText ("Altitude:  " + Math.round(pos.z ())+ " m");
		}
	}
}
