/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package tclib.utils.fusion;

import java.util.*;
import java.io.*;

import tc.vrobot.*;

import wucore.utils.math.*;

public class FusionDesc extends Object
{
	// Low-level sensor fusion parameters
	public static final int			V_UNDEF		= -1;	// Undefined virtual mode
	public static final int			V_SONAR		= 0;		// Use sonar as virtual sensor
	public static final int			V_IR			= 1;		// Use ir as virtual sensor
	public static final int			V_MIN		= 2;		// Fuse sensors using the minimum
	public static final int			V_FILTER		= 3;		// Fuse sensors using a 2x1 filter (fuzzy rules, neural network, etc)
	public static final int			V_FLYNN		= 4;		// Fuse sensors using Flynn's rules

	public static final int			G_UNDEF		= -1;	// Undefined group mode
	public static final int			G_MIN		= 0;		// Use the minimum fusion
	public static final int			G_WEIGHT		= 1;		// Use a weighted average fusion
	public static final int			G_BWEIGHT	= 2;		// Use a bounded weighted average fusion
	public static final int			G_BUF_ARC	= 3;		// Use a buffer-based circular arc sensor fusion
	public static final int			G_WBUF_ARC	= 4;		// Use a weighted buffer-based circular arc sensor fusion
	public static final int			G_BUF_RECT	= 5;		// Use a buffer-based circular arc sensor fusion
	public static final int			G_WBUF_RECT	= 6;		// Use a weighted buffer-based circular arc sensor fusion

	public static final int			S_UNDEF		= -1;	// Undefined scanner mode
	public static final int			S_MIN		= 0;		// Use the minimum fusion
	public static final int			S_AVG		= 1;		// Use the average fusion

	// Perception modes
	protected int					MODEVIRTU; 				// Virtual sensor fusion method

	// Robot sensor features
	public int						MAXVIRTU; 				// Number of virtual sensors
	public double					RANGEVIRTU; 			// Maximum virtual sensor range (m)
	public double					CONEVIRTU; 				// Virtual sensor aperture range (rad)
	public String					FILTERVIRTU;			// Name of virtual filter definition file
	
	public int						MAXGROUP; 				// Number of group sensors
	public double					RANGEGROUP; 			// Maximum group sensor range (m)
	public double					CONEGROUP; 				// Group sensor aperture range (rad)
	
	public int 						RAYSCAN;	 			// Number of virtual scanner rays
	public double 					RANGESCAN;	 			// Maximum virtual scanner range (m)
	public double 					CONESCAN;	 			// Virtual scanner aperture range (rad)

	public int						MAXDSIG; 				// Number of digital inputs

	public SensorPos[]				virtufeat; 				// Virtual sensors angular position
	public FeaturePos[]				groupfeat; 				// Group sensors angular position
	public SensorPos				scanfeat; 				// Virtual scanner angular position
	public SensorPos[]				dsigfeat; 				// Digital inputs angular position
	public Filter					vfilter; 				// Virtual sensor fusion-filter
	
	/* Constructors */
	protected FusionDesc ()
	{
	}

	public FusionDesc (String name)
	{
		Properties		props;
		File			file;
		FileInputStream	stream;
		
		props			= new Properties ();
		try 
		{
			file 		= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }

		set (props);
	}
		
	public FusionDesc (Properties props)
	{
		set (props);
	}
		
	// Accessors
	public final int 			virtu_mode ()	 		{ return MODEVIRTU; }
	public final void 			virtu_mode (int mod)	{ this.MODEVIRTU = mod; }

	/* Instance methods */
	protected void set (Properties props) 
	{
		int				i, mode;
		double			len, rho, alpha;
		double			base, cone, range;
		double			ra = Angles.DTOR;
				
		// Set default properties	
		FILTERVIRTU 		= props.getProperty ("FILTERVIRTU");
		try { MAXVIRTU	 	= new Integer (props.getProperty ("MAXVIRTU")).intValue (); } 			catch (Exception e) 	{ MAXVIRTU		= 0; }
		try { RANGEVIRTU	= new Double (props.getProperty ("RANGEVIRTU")).doubleValue (); } 		catch (Exception e) 	{ RANGEVIRTU	= 8.0; }
		try { CONEVIRTU	 	= new Double (props.getProperty ("CONEVIRTU")).doubleValue () * ra; }	catch (Exception e) 	{ CONEVIRTU		= 20.0 * ra; }
		try { MODEVIRTU	 	= new Integer (props.getProperty ("MODEVIRTU")).intValue (); } 			catch (Exception e) 	{ MODEVIRTU		= V_SONAR; }

		try { MAXGROUP	 	= new Integer (props.getProperty ("MAXGROUP")).intValue (); } 			catch (Exception e) 	{ MAXGROUP		= 0; }
		try { RANGEGROUP	= new Double (props.getProperty ("RANGEGROUP")).doubleValue (); }		catch (Exception e) 	{ RANGEGROUP	= 1.0; }
		try { CONEGROUP	 	= new Double (props.getProperty ("CONEGROUP")).doubleValue () * ra; }	catch (Exception e) 	{ CONEGROUP		= 30.0 * ra; }

		try { RAYSCAN	 	= new Integer (props.getProperty ("RAYSCAN")).intValue (); } 			catch (Exception e) 	{ RAYSCAN		= 0; }
		try { RANGESCAN	 	= new Double (props.getProperty ("RANGESCAN")).doubleValue (); } 		catch (Exception e) 	{ RANGESCAN		= 10.0; }
		try { CONESCAN	 	= new Double (props.getProperty ("CONESCAN")).doubleValue () * ra; }	catch (Exception e) 	{ CONESCAN		= 180.0 * ra; }

		try { MAXDSIG		= new Integer (props.getProperty ("MAXDSIG")).intValue (); } 			catch (Exception e) 	{ MAXDSIG		= 0; }

		virtufeat		= new SensorPos [MAXVIRTU];
		groupfeat		= new FeaturePos [MAXGROUP];
		dsigfeat		= new SensorPos [MAXDSIG];

		vfilter	= Filter.fromFile (FILTERVIRTU);
		for (i = 0; i < MAXVIRTU; i++)
		{
			try { alpha		= new Double (props.getProperty ("virtufeat" + i)).doubleValue (); }	catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= new Double (props.getProperty ("virtulen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= new Double (props.getProperty ("virturho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			virtufeat[i]	= new SensorPos ();
			
			try { mode 	= new Integer (props.getProperty ("virtumode" + i)).intValue (); }			catch (Exception e) 	{ mode  	= V_UNDEF; }
			virtufeat[i].mode (mode);
			virtufeat[i].set_polar (len, rho * ra, alpha * ra);			
		}

		for (i = 0; i < MAXGROUP; i++)
		{
			try { alpha		= new Double (props.getProperty ("groupfeat" + i)).doubleValue (); }	catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= new Double (props.getProperty ("grouplen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= new Double (props.getProperty ("grouprho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			groupfeat[i]	= new FeaturePos ();
			groupfeat[i].set_polar (len, rho * ra, alpha * ra);
			
			try { mode 	= new Integer (props.getProperty ("groupmode" + i)).intValue (); } 			catch (Exception e) 	{ mode  	= G_UNDEF; }
			groupfeat[i].mode (mode);
			groupfeat[i].set_equ (props.getProperty ("groupequ" + i));

			try { base		= new Double (props.getProperty ("groupbase" + i)).doubleValue (); }	catch (Exception e) 	{ base		= 0.3; }
			try { cone		= new Double (props.getProperty ("groupcone" + i)).doubleValue () * ra; }	catch (Exception e) { cone		= CONEGROUP; }
			try { range		= new Double (props.getProperty ("grouprng" + i)).doubleValue (); }		catch (Exception e) 	{ range		= RANGEGROUP; }
			groupfeat[i].set_shape (base, cone, range);
		}

		for (i = 0; i < MAXDSIG; i++)
		{
			try { alpha		= new Double (props.getProperty ("dsigfeat" + i)).doubleValue (); }		catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= new Double (props.getProperty ("dsiglen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= new Double (props.getProperty ("dsigrho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			dsigfeat[i]	= new SensorPos ();
			
			try { mode 	= new Integer (props.getProperty ("dsigmode" + i)).intValue (); }			catch (Exception e) 	{ mode  	= S_UNDEF; }
			dsigfeat[i].mode (mode);
			dsigfeat[i].set_polar (len, rho * ra, alpha * ra);			
		}

		try { alpha		= new Double (props.getProperty ("scanfeat")).doubleValue (); }				catch (Exception e) 	{ alpha		= 0.0; }
		try { len		= new Double (props.getProperty ("scanlen")).doubleValue (); }				catch (Exception e) 	{ len		= 0.0; }
		try { rho		= new Double (props.getProperty ("scanrho")).doubleValue (); }				catch (Exception e) 	{ rho		= alpha; }
		scanfeat		= new SensorPos ();

		try { mode 	= new Integer (props.getProperty ("scanmode")).intValue (); }					catch (Exception e) 	{ mode  	= S_UNDEF; }
		scanfeat.mode (mode);
		scanfeat.set_polar (len, rho * ra, alpha * ra);			
	}
} 