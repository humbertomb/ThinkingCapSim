/**
 * Created on 16-mar-2006
 *
 * @author Humberto Martinez Barbera
 */
package devices.gui;

import java.awt.*;
import javax.swing.*;

import devices.data.*;
import wucore.utils.math.*;
import wucore.widgets.*;


public class InsDataPanel extends JPanel
{
	static public final int		DIGITS		= 3;
	
	// Main data panels
	protected JPanel				topPA		= new JPanel ();
	
	// Euler angles panel
	protected JPanel				eulerPA		= new JPanel ();
	protected JLabel				e_rollLA		= new JLabel ();
	protected JLabel				e_pitchLA	= new JLabel ();
	protected JLabel				e_yawLA		= new JLabel ();
	
	// Gyro data panel
	protected JPanel				gyroPA		= new JPanel ();
	protected JLabel				g_rollLA		= new JLabel ();
	protected JLabel				g_pitchLA	= new JLabel ();
	protected JLabel				g_yawLA		= new JLabel ();

	// Acceleration data panel
	protected JPanel				accPA		= new JPanel ();
	protected JLabel				a_xLA		= new JLabel ();
	protected JLabel				a_yLA		= new JLabel ();
	protected JLabel				a_zLA		= new JLabel ();

	// Magnetic data panel
	protected JPanel				magPA		= new JPanel ();
	protected JLabel				m_xLA		= new JLabel ();
	protected JLabel				m_yLA		= new JLabel ();
	protected JLabel				m_zLA		= new JLabel ();

	// Gauges panel
	protected JPanel				gaugePA		= new JPanel ();
	protected Compass			compassGA	= new Compass ();
	protected Gauge				speedGA		= new Gauge ();
	protected Gauge				varioGA		= new Gauge ();
	protected Horizont			horizontGA	= new Horizont ();
	protected Gauge				heightGA		= new Gauge ();

	// Constructors
	public InsDataPanel ()
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

		// Gyro data panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		g_rollLA.setFont (courier);
		g_pitchLA.setFont (courier);
		g_yawLA.setFont (courier);
		gyroPA.setVisible (true);
		gyroPA.setLayout (new GridLayout(3,1));
		gyroPA.add (g_rollLA);
		gyroPA.add (g_pitchLA);
		gyroPA.add (g_yawLA);
		gyroPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Gyro Rates", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// Acceleration data panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		a_xLA.setFont (courier);
		a_yLA.setFont (courier);
		a_zLA.setFont (courier);
		accPA.setVisible (true);
		accPA.setLayout (new GridLayout(3,1));
		accPA.add (a_xLA);
		accPA.add (a_yLA);
		accPA.add (a_zLA);
		accPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Acceleration", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		// Magnetic data panel
		courier		= new Font ("Courier", Font.PLAIN, 14);
		m_xLA.setFont (courier);
		m_yLA.setFont (courier);
		m_zLA.setFont (courier);
		magPA.setVisible (true);
		magPA.setLayout (new GridLayout(3,1));
		magPA.add (m_xLA);
		magPA.add (m_yLA);
		magPA.add (m_zLA);
		magPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Magnetic Field", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));

		compassGA.setSphere(java.awt.Color.orange);
		compassGA.setVisible(true);
		compassGA.setLayout(null);
		compassGA.setMarks(true);
		compassGA.setToolTipText("Compass");
	
		speedGA.setSphere(java.awt.Color.orange);
		speedGA.setVisible(true);
		speedGA.setStep(10.0);
		speedGA.setMarks(true);
		speedGA.setMin (0.0);
		speedGA.setMax (300.0);
		speedGA.setValue (0.0);
		speedGA.setLayout(null);
		speedGA.setToolTipText("Speedometer");
	
		varioGA.setSphere(java.awt.Color.orange);
		varioGA.setVisible(true);
		varioGA.setStep(10.0);
		varioGA.setMarks(true);
		varioGA.setMin (-100.0);
		varioGA.setMax (100.0);
		varioGA.setValue (0.0);
		varioGA.setLayout(null);
		varioGA.setToolTipText("Vario meter");

		horizontGA.setSphere(java.awt.Color.orange);
		horizontGA.setVisible(true);
		horizontGA.setLayout(null);
		horizontGA.setStep(30.0 * Angles.DTOR);
		horizontGA.setMarks(true);
		horizontGA.setToolTipText("Artificial horizont");
	
		heightGA.setSphere(java.awt.Color.orange);
		heightGA.setVisible(true);
		heightGA.setStep(100.0);
		heightGA.setMarks(true);
		heightGA.setMin (0.0);
		heightGA.setMax (2000.0);
		heightGA.setValue (0.0);
		heightGA.setLayout(null);
		heightGA.setToolTipText("Altimeter");

		// Gauges panel
		gaugePA.setVisible (true);
		gaugePA.setLayout (new GridLayout(1, 4));
		gaugePA.add (compassGA);
		gaugePA.add (horizontGA);
		gaugePA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Gauges", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		// Top panel
		topPA.setVisible (true);
		topPA.setLayout (new GridLayout(4, 1));
		topPA.add (eulerPA);
		topPA.add (gyroPA);
		topPA.add (accPA);
		topPA.add (magPA);

		setLayout(new BorderLayout ());
		setBorder (BorderFactory.createLineBorder (new Color(153, 153, 153)));
		add (gaugePA, BorderLayout.CENTER);		
		add (topPA, BorderLayout.NORTH);
	}

	public void update (InsData data)
	{		  
		e_rollLA.setText ("Roll:  "+format (data.getRoll ()*Angles.RTOD, DIGITS)+" deg");
		e_pitchLA.setText ("Pitch: "+format (data.getPitch ()*Angles.RTOD, DIGITS)+" deg");
		e_yawLA.setText ("Yaw:   "+format (data.getYaw ()*Angles.RTOD, DIGITS)+" deg");
  
		g_rollLA.setText ("Roll:  "+format (data.getRollRate ()*Angles.RTOD, DIGITS)+" deg/s");
		g_pitchLA.setText ("Pitch: "+format (data.getPitchRate ()*Angles.RTOD, DIGITS)+" deg/s");
		g_yawLA.setText ("Yaw:   "+format (data.getYawRate ()*Angles.RTOD, DIGITS)+" deg/s");

		a_xLA.setText ("X: "+format (data.getAccX (), DIGITS)+" m/s2");
		a_yLA.setText ("Y: "+format (data.getAccY (), DIGITS)+" m/s2");
		a_zLA.setText ("Z: "+format (data.getAccZ (), DIGITS)+" m/s2");

		m_xLA.setText ("X: "+format (data.getMagX (), DIGITS)+" au");
		m_yLA.setText ("Y: "+format (data.getMagY (), DIGITS)+" au");
		m_zLA.setText ("Z: "+format (data.getMagZ (), DIGITS)+" au");

//		speedGA.setValue (Math.sqrt (data.pose.velx () * data.pose.velx () + data.pose.vely () * data.pose.vely ()) * MSTOKMH);		// [Km/h]
//		varioGA.setValue (data.pose.velz () * MSTOKMH);																				// [Km/h]
		compassGA.setValue (data.getYaw ()+3.1416/2);																						// [rad]
//		heightGA.setValue (data.cur.z ());																							// [m]
		horizontGA.setValues (data.getPitch (), -data.getRoll ());																// [rad]
   }
}
