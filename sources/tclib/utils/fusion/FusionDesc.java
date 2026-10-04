/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package tclib.utils.fusion;

import java.util.*;
import java.io.*;

import tc.vrobot.*;

import wucore.utils.math.*;

/**
 * How the sensors of a robot are fused (see {@link Fusion}): its fused sensors,
 * virtual scanners and sensors of an area, read from the properties of its
 * description. Every sensor of an area is an arc; the ways of fusing that are
 * gone (the 2x1 filter, the rectangles) are read as the ones that replace them.
 */
public class FusionDesc extends Object
{
	// Low-level sensor fusion parameters
	public static final int			V_UNDEF		= -1;		// Undefined virtual mode
	public static final int			V_SONAR		= 0;		// Use sonar as virtual sensor
	public static final int			V_IR		= 1;		// Use ir as virtual sensor
	public static final int			V_MIN		= 2;		// Fuse sensors using the minimum
	public static final int			V_FLYNN		= 3;		// Fuse sensors using Flynn's rules (it was 4, read as 3)

	public static final int			G_BUF_ARC	= 0;		// The nearest reading of the range buffer in the arc of the sensor
	public static final int			G_WBUF_ARC	= 1;		// The same, an older reading counting as farther

	public static final int			S_UNDEF		= -1;		// Undefined scanner mode
	public static final int			S_MIN		= 0;		// Use the minimum fusion
	public static final int			S_AVG		= 1;		// Use the average fusion

	// Perception modes
	protected int					MODEVIRTU; 				// Virtual sensor fusion method

	// Robot sensor features
	public int						MAXVIRTU; 				// Number of virtual sensors
	public double					RANGEVIRTU; 			// Maximum virtual sensor range (m)
	public double					CONEVIRTU; 				// Virtual sensor aperture range (rad)

	public int						MAXGROUP; 				// Number of group sensors
	public double					RANGEGROUP; 			// Maximum group sensor range (m)
	public double					CONEGROUP; 				// Group sensor aperture range (rad)
	
	public int						MAXSCAN;				// Number of virtual scanners (reduced laser scans); the first is the one of RAYSCAN, scanfeat, ...
	public int 						RAYSCAN;	 			// Number of virtual scanner rays
	public double 					RANGESCAN;	 			// Maximum virtual scanner range (m)
	public double 					CONESCAN;	 			// Virtual scanner aperture range (rad)

	public int						MAXDSIG; 				// Number of digital inputs

	public SensorPos[]				virtufeat; 				// Virtual sensors angular position
	public FeaturePos[]				groupfeat; 				// Group sensors angular position
	public SensorPos				scanfeat; 				// Virtual scanner angular position
	public SensorPos[]				scanfeats;				// Every virtual scanner: where it is and where it looks (scanfeats[0] is scanfeat)
	public int[]					scanrays;				// ... how many rays it has (scanrays[0] is RAYSCAN)
	public double[]					scancones;				// ... how wide it opens (rad; scancones[0] is CONESCAN)
	public double[]					scanranges;				// ... and how far it reads (m; scanranges[0] is RANGESCAN)
	public SensorPos[]				dsigfeat; 				// Digital inputs angular position
	
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
	public final void 			virtu_mode (int mod)	{ this.MODEVIRTU = virtuMode (mod); }

	/* Class methods */
	/** The mode of a fused sensor, as it is now: Flynn's rules were 4 (and 3, the 2x1 filter that is gone). */
	static public int virtuMode (int mode)
	{
		if (mode == 4)								return V_FLYNN;
		if ((mode < V_UNDEF) || (mode > V_FLYNN))	return V_UNDEF;
		return mode;
	}

	/**
	 * The mode of a sensor of an area: every one is an arc of the range buffer,
	 * weighted (1) or as it is (0, and anything else).
	 */
	static public int groupMode (int mode)
	{
		return (mode == G_WBUF_ARC) ? G_WBUF_ARC : G_BUF_ARC;
	}

	/* Instance methods */
	protected void set (Properties props) 
	{
		int				i, mode;
		double			len, rho, alpha;
		double			cone, range;
		double			ra = Angles.DTOR;
				
		// Set default properties	
		try { MAXVIRTU	 	= Integer.valueOf (props.getProperty ("MAXVIRTU")).intValue (); } 			catch (Exception e) 	{ MAXVIRTU		= 0; }
		try { RANGEVIRTU	= Double.valueOf (props.getProperty ("RANGEVIRTU")).doubleValue (); } 		catch (Exception e) 	{ RANGEVIRTU	= 8.0; }
		try { CONEVIRTU	 	= Double.valueOf (props.getProperty ("CONEVIRTU")).doubleValue () * ra; }	catch (Exception e) 	{ CONEVIRTU		= 20.0 * ra; }
		try { MODEVIRTU	 	= Integer.valueOf (props.getProperty ("MODEVIRTU")).intValue (); } 			catch (Exception e) 	{ MODEVIRTU		= V_SONAR; }
		MODEVIRTU		= virtuMode (MODEVIRTU);
		if (MODEVIRTU == V_UNDEF)		MODEVIRTU = V_SONAR;

		try { MAXGROUP	 	= Integer.valueOf (props.getProperty ("MAXGROUP")).intValue (); } 			catch (Exception e) 	{ MAXGROUP		= 0; }
		try { RANGEGROUP	= Double.valueOf (props.getProperty ("RANGEGROUP")).doubleValue (); }		catch (Exception e) 	{ RANGEGROUP	= 1.0; }
		try { CONEGROUP	 	= Double.valueOf (props.getProperty ("CONEGROUP")).doubleValue () * ra; }	catch (Exception e) 	{ CONEGROUP		= 30.0 * ra; }

		try { RAYSCAN	 	= Integer.valueOf (props.getProperty ("RAYSCAN")).intValue (); } 			catch (Exception e) 	{ RAYSCAN		= 0; }
		try { RANGESCAN	 	= Double.valueOf (props.getProperty ("RANGESCAN")).doubleValue (); } 		catch (Exception e) 	{ RANGESCAN		= 10.0; }
		try { CONESCAN	 	= Double.valueOf (props.getProperty ("CONESCAN")).doubleValue () * ra; }	catch (Exception e) 	{ CONESCAN		= 180.0 * ra; }

		try { MAXDSIG		= Integer.valueOf (props.getProperty ("MAXDSIG")).intValue (); } 			catch (Exception e) 	{ MAXDSIG		= 0; }

		virtufeat		= new SensorPos [MAXVIRTU];
		groupfeat		= new FeaturePos [MAXGROUP];
		dsigfeat		= new SensorPos [MAXDSIG];

		for (i = 0; i < MAXVIRTU; i++)
		{
			try { alpha		= Double.valueOf (props.getProperty ("virtufeat" + i)).doubleValue (); }	catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= Double.valueOf (props.getProperty ("virtulen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= Double.valueOf (props.getProperty ("virturho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			virtufeat[i]	= new SensorPos ();
			
			try { mode 	= Integer.valueOf (props.getProperty ("virtumode" + i)).intValue (); }			catch (Exception e) 	{ mode  	= V_UNDEF; }
			virtufeat[i].mode (virtuMode (mode));
			virtufeat[i].set_polar (len, rho * ra, alpha * ra);			
		}

		for (i = 0; i < MAXGROUP; i++)
		{
			try { alpha		= Double.valueOf (props.getProperty ("groupfeat" + i)).doubleValue (); }	catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= Double.valueOf (props.getProperty ("grouplen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= Double.valueOf (props.getProperty ("grouprho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			groupfeat[i]	= new FeaturePos ();
			groupfeat[i].set_polar (len, rho * ra, alpha * ra);
			
			try { mode 	= Integer.valueOf (props.getProperty ("groupmode" + i)).intValue (); } 			catch (Exception e) 	{ mode  	= G_BUF_ARC; }
			groupfeat[i].mode (groupMode (mode));

			try { cone		= Double.valueOf (props.getProperty ("groupcone" + i)).doubleValue () * ra; }	catch (Exception e) { cone		= CONEGROUP; }
			try { range		= Double.valueOf (props.getProperty ("grouprng" + i)).doubleValue (); }		catch (Exception e) 	{ range		= RANGEGROUP; }
			groupfeat[i].set_shape (cone, range);
		}

		for (i = 0; i < MAXDSIG; i++)
		{
			try { alpha		= Double.valueOf (props.getProperty ("dsigfeat" + i)).doubleValue (); }		catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= Double.valueOf (props.getProperty ("dsiglen" + i)).doubleValue (); }		catch (Exception e) 	{ len		= 0.0; }
			try { rho		= Double.valueOf (props.getProperty ("dsigrho" + i)).doubleValue (); }		catch (Exception e) 	{ rho		= alpha; }
			dsigfeat[i]	= new SensorPos ();
			
			try { mode 	= Integer.valueOf (props.getProperty ("dsigmode" + i)).intValue (); }			catch (Exception e) 	{ mode  	= S_UNDEF; }
			dsigfeat[i].mode (mode);
			dsigfeat[i].set_polar (len, rho * ra, alpha * ra);			
		}

		try { alpha		= Double.valueOf (props.getProperty ("scanfeat")).doubleValue (); }				catch (Exception e) 	{ alpha		= 0.0; }
		try { len		= Double.valueOf (props.getProperty ("scanlen")).doubleValue (); }				catch (Exception e) 	{ len		= 0.0; }
		try { rho		= Double.valueOf (props.getProperty ("scanrho")).doubleValue (); }				catch (Exception e) 	{ rho		= alpha; }
		scanfeat		= new SensorPos ();

		try { mode 	= Integer.valueOf (props.getProperty ("scanmode")).intValue (); }					catch (Exception e) 	{ mode  	= S_UNDEF; }
		scanfeat.mode (mode);
		scanfeat.set_polar (len, rho * ra, alpha * ra);			

		// every virtual scanner: the first is the one above, the rest are numbered from
		// one, each with its own fan (and the fan of the first when it says nothing)
		try { MAXSCAN		= Integer.valueOf (props.getProperty ("MAXSCAN")).intValue (); }			catch (Exception e) 	{ MAXSCAN		= (RAYSCAN > 0) ? 1 : 0; }
		if (RAYSCAN <= 0)		MAXSCAN = 0;
		scanfeats		= new SensorPos [MAXSCAN];
		scanrays		= new int [MAXSCAN];
		scancones		= new double [MAXSCAN];
		scanranges		= new double [MAXSCAN];
		for (i = 0; i < MAXSCAN; i++)
		{
			if (i == 0)
			{
				scanfeats[i]	= scanfeat;
				scanrays[i]		= RAYSCAN;
				scancones[i]	= CONESCAN;
				scanranges[i]	= RANGESCAN;
				continue;
			}
			try { scanrays[i]	= Integer.valueOf (props.getProperty ("RAYSCAN" + i)).intValue (); }		catch (Exception e) 	{ scanrays[i]	= RAYSCAN; }
			try { scancones[i]	= Double.valueOf (props.getProperty ("CONESCAN" + i)).doubleValue () * ra; }	catch (Exception e) 	{ scancones[i]	= CONESCAN; }
			try { scanranges[i]	= Double.valueOf (props.getProperty ("RANGESCAN" + i)).doubleValue (); }	catch (Exception e) 	{ scanranges[i]	= RANGESCAN; }
			try { alpha		= Double.valueOf (props.getProperty ("scanfeat" + i)).doubleValue (); }			catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= Double.valueOf (props.getProperty ("scanlen" + i)).doubleValue (); }			catch (Exception e) 	{ len		= 0.0; }
			try { rho		= Double.valueOf (props.getProperty ("scanrho" + i)).doubleValue (); }			catch (Exception e) 	{ rho		= alpha; }
			try { mode 		= Integer.valueOf (props.getProperty ("scanmode" + i)).intValue (); }			catch (Exception e) 	{ mode  	= S_UNDEF; }
			scanfeats[i]	= new SensorPos ();
			scanfeats[i].mode (mode);
			scanfeats[i].set_polar (len, rho * ra, alpha * ra);
		}
	}
} 