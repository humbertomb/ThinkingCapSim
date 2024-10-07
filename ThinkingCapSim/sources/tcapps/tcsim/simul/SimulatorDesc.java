/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2003 Bernardo Cánovas Segura (added 3D representation attributes)
 */
 
package tcapps.tcsim.simul;

import java.util.*;

import wucore.utils.math.*;

public class SimulatorDesc
{
	// Robot simulation models
	public int					MODESON; 				// Sonar simulation method
	public int					MODEIR; 					// Ir simulation method
	public int					MODELRF; 				// Lrf simulation method	
	public int					MODELSB; 				// Lsb simulation method		

	// Sensor simulation parameters	
	public double				SENSIBSON; 				// Sonar sensibility
	public int					RAYSON; 					// Number of sonar intersection rays
	public double				ERRORSON; 				// Percentual sonar error (± %)	
	public int					RAYIR; 					// Number of ir intersection rays
	public double				ERRORIR; 				// Percentual ir error (± %)
	public double				ERRORLRF; 				// Percentual lrf error (± %)
	public double				ERRORLRFGAUSS;			// Gauss range of lrf error (m)
	public double				ERRORVIS; 				// Angular error for vision (rad)	
	
	public int	 				RAYRAD;					// Number of radar rays.
	
	/* Laser balizas */
	public int					RAYLSB; 					// Number of lsb intersection rays
	public double				ERRORANGLELSB; 			// Percentual lsb error (± %) of angle
	public double				ERRORANGLELSBGAUSS;		// Gauss range of lsb error (degree) of angle
	public double				ERRORRANGELSB; 			// Percentual lsb error (± %) of range
	public double				ERRORRANGELSBGAUSS;		// Gauss range of lsb error (m) of range

	/* 3D representation info */
	public String 				V3DFILE;					// File with the robot 3D representation in Wavefront format (.obj)
	public String 				V3DLIFT;					// File with the robot-lift/grip 3D representation in Wavefront format (.obj)
	public float					V3DCOLORR;				// Red level of the robot 3D representation
	public float					V3DCOLORG;				// Green level of the robot 3D representation
	public float					V3DCOLORB;				// Blue level of the robot 3D representation

	/* Constructors */
	public SimulatorDesc (Properties props)
	{
		double			ra = Angles.DTOR;

		// Set default properties for simulated robot and environment
		try { SENSIBSON	 	= new Double (props.getProperty ("SENSIBSON")).doubleValue (); } catch (Exception e) 				{ SENSIBSON			= 0.85; }
		try { ERRORSON	 	= new Double (props.getProperty ("ERRORSON")).doubleValue (); } catch (Exception e) 				{ ERRORSON			= 0.05; }
		try { RAYSON 		= new Integer (props.getProperty ("RAYSON")).intValue (); } catch (Exception e) 					{ RAYSON			= 11; }
		try { MODESON 		= new Integer (props.getProperty ("MODESON")).intValue (); } catch (Exception e) 					{ MODESON			= Simulator.S_GEOM; }
	
		try { ERRORIR	 	= new Double (props.getProperty ("ERRORIR")).doubleValue (); } catch (Exception e) 					{ ERRORIR			= 0.05; }
		try { RAYIR 			= new Integer (props.getProperty ("RAYIR")).intValue (); } catch (Exception e) 						{ RAYIR				= 11; }
		try { MODEIR 		= new Integer (props.getProperty ("MODEIR")).intValue (); } catch (Exception e) 					{ MODEIR			= Simulator.I_GEOM; }
		
		try { MODELRF 		= new Integer (props.getProperty ("MODELRF")).intValue (); } catch (Exception e) 					{ MODELRF			= Simulator.LRF_GEOM; }
		try { ERRORLRFGAUSS	= new Double (props.getProperty ("ERRORLRFGAUSS")).doubleValue (); } catch (Exception e) 			{ ERRORLRFGAUSS		= 0.005; }

		try { RAYRAD	 		= new Integer (props.getProperty ("RAYRAD")).intValue (); } 		catch (Exception e) 			{ RAYRAD		= 16; }

		try { RAYLSB 		= new Integer (props.getProperty ("RAYLSB")).intValue (); } catch (Exception e) 					{ RAYLSB				= 360; }
		try { MODELSB 		= new Integer (props.getProperty ("MODELSB")).intValue (); } catch (Exception e) 					{ MODELSB				= Simulator.LSB_GEOM; }
		try { ERRORANGLELSBGAUSS = new Double (props.getProperty ("ERRORANGLELSBGAUSS")).doubleValue (); } catch (Exception e) 	{ ERRORANGLELSBGAUSS	= 0.05; }
		try { ERRORANGLELSB		 = new Double (props.getProperty ("ERRORANGLELSB")).doubleValue (); } catch (Exception e) 		{ ERRORANGLELSB			= 0.05; }
		try { ERRORRANGELSBGAUSS = new Double (props.getProperty ("ERRORRANGELSBGAUSS")).doubleValue (); } catch (Exception e) 	{ ERRORRANGELSBGAUSS	= 0.05; }
		try { ERRORRANGELSB	 = new Double (props.getProperty ("ERRORRANGELSB")).doubleValue (); } catch (Exception e) 			{ ERRORRANGELSB			= 0.05; }

		try { ERRORVIS	 	= new Double (props.getProperty ("ERRORVIS")).doubleValue () * ra; } catch (Exception e) 			{ ERRORVIS			= 0.0; }
		
		try { V3DFILE		= props.getProperty("V3DFILE"); } catch (Exception e) 											{ V3DFILE = null; };
		try { V3DLIFT		= props.getProperty("V3DLIFT"); } catch (Exception e) 											{ V3DLIFT = null; };
		try { V3DCOLORR		= new Float (props.getProperty("V3DCOLORR")).floatValue(); } catch (Exception e)					{ V3DCOLORR = 255.0f; };
		try { V3DCOLORG		= new Float (props.getProperty("V3DCOLORG")).floatValue(); } catch (Exception e)					{ V3DCOLORG = 0.0f;	};
		try { V3DCOLORB		= new Float (props.getProperty("V3DCOLORB")).floatValue(); } catch (Exception e)					{ V3DCOLORB = 0.0f; };
	}

	public final int 			sonar_mode ()	 		{ return MODESON; }
	public final void 			sonar_mode (int mod)		{ this.MODESON = mod; }
	public final int 			ir_mode ()	 			{ return MODEIR; }
	public final void 			ir_mode (int mod)		{ this.MODEIR = mod; }		
	public final int 			lsb_mode ()	 			{ return MODELSB; }
	public final void 			lsb_mode (int mod)		{ this.MODELSB = mod; }		
} 