/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2003-2004 Humberto Martinez
 */

package tc.vrobot;

import java.util.*;
import java.io.*;

import tc.fleet.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class RobotDesc extends VehicleDesc implements Serializable
{
	// Robot platform description (standard range sensors)
	public int					MAXBUMPER; 		// Number of bumper sensors

	public int					MAXSONAR; 		// Number of sonar sensors
	public double				RANGESON; 		// Maximum sonar range (m)
	public double				MINIMSON;    	// Minimum sonar range (m)
	public double				CONESON; 		// Sonar aperture range (rad)
	public int					CYCLESON; 		// Number of sonar firing cycles

	public int					MAXIR; 			// Number of ir sensors
	public double				RANGEIR; 		// Maximum ir range (m)
	public double				MINIMIR; 		// Minimum ir range (m)
	public double				CONEIR; 			// Ir aperture range (rad)
	public int					CYCLEIR; 		// Number of ir firing cycles

	public int 					MAXLRF;	 		// Number of laser range finder sensors
	public double 				RANGELRF;		// Maximum lrf range (m)
	public double 				MINIMLRF;	 	// Minimum lrf range (m)
	public double 				CONELRF;	 		// Lrf aperture range (rad)
	public int	 				RAYLRF;			// Number of rays.
	public int					CYCLELRF; 		// Number of lrf firing cycles

	public int 					MAXLSB;	 		// Number of lsb sensors
	public double 				RANGELSB;	 	// Maximum lsb range (m)
	public double 				MINIMLSB;	 	// Minimum lsb range (m)
	public double 				CONELSB;	 		// Angulo de apertura del laser (rad)
	public int					CYCLELSB; 		// Number of lsb firing cycles
	public double 				REFLSB;		 	// Angulo maximo en el que se refleja el rayo (rad)
	public int	 				BEACLSB;			// Numero de balizas maximas que puede detectar.
	public int	 				RAYLSB;			// Numero de rayos.
	public boolean				RANGE;			// Con medidas de rango
	public boolean				ANGLE;			// Con medidas de angulo de orientacion
	public int					MAXLAYER;		// Numero de Layer del Laser NAV200
	public int					INITLAYER;		// Layer en la cual esta el robot inicialmente
	public String[]				lsblayer;		// Nombre de las Zonas asignadas a los Layer
	
	public int					MAXTRACKER; 		// Number of tracking sensors
	public double				RANGETRK; 		// Maximum tracking range (m)
	public double				MINIMTRK;    	// Minimum tracking range (m)
	public double				CONETRK; 		// Tracker aperture range (rad)
	public int					OBJTRK; 			// Number of tracked objects

	public int					MAXVISION; 		// Maximum number of vision based sensors
	public double				CONEVIS; 		// Vision field of view (rad)
	public int					CYCLEVIS; 		// Number of vision firing cycles

	public RobotModel			model;			// Kynematics model of the robot platform
	public SensorPos[]			trkfeat; 		// Tracking sensors robot-local position
	public SensorPos[]			sonfeat; 		// Sonar sensors robot-local position
	public SensorPos[]			irfeat; 			// IR sensors robot-local position
	public SensorPos[]			lrffeat; 		// LRF sensors robot-local position
	public SensorPos[]			lsbfeat; 		// LSB sensors robot-local position
	public SensorPos[]			visfeat; 		// Vision sensors robot-local position
	public Line2[]			    bumfeat;			// Bumper sensors detection robot-local line
	public String[]				digfeat;			// Digitizer configuration

	// Robot platform description (external devices)
	public int					MAXGPS; 			// Maximum number of GPS terminals
	public int					MAXCOMPASS; 		// Maximum number of compass sensors
	public int					MAXINS;			// MAximum number fo INS sensors
	public int					MAXRADAR; 		// Maximum number of radar sensors
	public int					MAXENCS; 		// Maximum number of encoders
	public int					MAXCAMERA; 		// Maximum number of video cameras
	public int					MAXDIGITIZER; 	// Maximum number of video digitizers

	/* Constructors */
	public RobotDesc (Properties props)
	{
		super (props);
	}

	public RobotDesc (Properties props, long dtime)
	{
		super (props);
		
		DTIME	= dtime;
	}

	/* Instance methods */
	protected void update (Properties props)
	{
		int				i, stp = 0;
		double			len = 0.0, rho = 0.0, alpha = 0.0, hgt = 0.0;
		double			xi = 0.0, yi = 0.0;
		double			xf = 0.0, yf = 0.0;
		boolean			son_modified = true, lrf_modified = true, ir_modified = true;
		boolean			lsb_modified = true, trk_modified = true;

		super.update (props);
		
		// Load robot properties
		try { MAXBUMPER 		= Integer.valueOf (props.getProperty ("MAXBUMPER")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXSONAR 		= Integer.valueOf (props.getProperty ("MAXSONAR")).intValue (); } 				catch (Exception e) 	{  son_modified = false; }
		try { RANGESON	 	= Double.valueOf (props.getProperty ("RANGESON")).doubleValue (); } 			catch (Exception e) 	{ }
		try { MINIMSON	 	= Double.valueOf (props.getProperty ("MINIMSON")).doubleValue (); } 			catch (Exception e) 	{ }
		try { CONESON	 	= Double.valueOf (props.getProperty ("CONESON")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
		try { CYCLESON	 	= Integer.valueOf (props.getProperty ("CYCLESON")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXTRACKER 	= Integer.valueOf (props.getProperty ("MAXTRACKER")).intValue (); } 			catch (Exception e) 	{ trk_modified = false; }
		try { RANGETRK	 	= Double.valueOf (props.getProperty ("RANGETRK")).doubleValue (); } 			catch (Exception e) 	{ }
		try { MINIMTRK	 	= Double.valueOf (props.getProperty ("MINIMTRK")).doubleValue (); } 			catch (Exception e) 	{ }
		try { CONETRK	 	= Double.valueOf (props.getProperty ("CONETRK")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
		try { OBJTRK	 		= Integer.valueOf (props.getProperty ("OBJTRK")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXIR	 		= Integer.valueOf (props.getProperty ("MAXIR")).intValue (); } 					catch (Exception e) 	{ ir_modified = false; }
		try { RANGEIR	 	= Double.valueOf (props.getProperty ("RANGEIR")).doubleValue (); } 				catch (Exception e) 	{ }
		try { MINIMIR	 	= Double.valueOf (props.getProperty ("MINIMIR")).doubleValue (); } 				catch (Exception e) 	{ }
		try { CONEIR	 		= Double.valueOf (props.getProperty ("CONEIR")).doubleValue () * Angles.DTOR; }	catch (Exception e)	{ }
		try { CYCLEIR	 	= Integer.valueOf (props.getProperty ("CYCLEIR")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXLRF	 		= Integer.valueOf (props.getProperty ("MAXLRF")).intValue (); } 				catch (Exception e) 	{ lrf_modified = false; }
		try { RANGELRF	 	= Double.valueOf (props.getProperty ("RANGELRF")).doubleValue (); } 			catch (Exception e) 	{ }
		try { MINIMLRF	 	= Double.valueOf (props.getProperty ("MINIMLRF")).doubleValue (); } 			catch (Exception e) 	{ }
		try { CONELRF	 	= Double.valueOf (props.getProperty ("CONELRF")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
		try { RAYLRF	 		= Integer.valueOf (props.getProperty ("RAYLRF")).intValue (); } 				catch (Exception e) 	{ }
		try { CYCLELRF	 	= Integer.valueOf (props.getProperty ("CYCLELRF")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXLSB	 		= Integer.valueOf (props.getProperty ("MAXLSB")).intValue (); } 				catch (Exception e) 	{ lsb_modified = false; }
		try { RANGELSB	 	= Double.valueOf (props.getProperty ("RANGELSB")).doubleValue (); } 			catch (Exception e) 	{ }
		try { MINIMLSB	 	= Double.valueOf (props.getProperty ("MINIMLSB")).doubleValue (); } 			catch (Exception e) 	{ }
		try { CONELSB	 	= Double.valueOf (props.getProperty ("CONELSB")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
		try { CYCLELSB	 	= Integer.valueOf (props.getProperty ("CYCLELSB")).intValue (); } 				catch (Exception e) 	{ }
		try { REFLSB	 		= Double.valueOf (props.getProperty ("REFLSB")).doubleValue () * Angles.DTOR; } 	catch (Exception e) 	{ }
		try { RAYLSB	 		= Integer.valueOf (props.getProperty ("RAYLSB")).intValue (); } 				catch (Exception e) 	{ }
		try { BEACLSB	 	= Integer.valueOf (props.getProperty ("BEACLSB")).intValue (); } 				catch (Exception e) 	{ }
		try { RANGE	 		= Boolean.valueOf (props.getProperty ("RANGE")).booleanValue (); } 				catch (Exception e) 	{ }
		try { ANGLE		 	= Boolean.valueOf (props.getProperty ("ANGLE")).booleanValue (); } 				catch (Exception e) 	{ }
		
		try { 
			MAXLAYER		= Integer.parseInt(props.getProperty ("MAXLAYER")); 
			if(MAXLAYER>0){
				lsblayer = new String[MAXLAYER];
				for(int j = 0; j<MAXLAYER;j++){
					lsblayer[j] = props.getProperty("LAYER_"+j);
				}
			}	
		} catch (Exception e) 	{ }
		try { INITLAYER	 	= Integer.parseInt(props.getProperty ("INITLAYER")); } catch (Exception e) 	{ }
		
		
		try { MAXVISION 		= Integer.valueOf (props.getProperty ("MAXVISION")).intValue (); }				catch (Exception e) 	{ }
		try { CONEVIS	 	= Double.valueOf (props.getProperty ("CONEVIS")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
		try { CYCLEVIS	 	= Integer.valueOf (props.getProperty ("CYCLEVIS")).intValue (); } 				catch (Exception e) 	{ }

		try { MAXGPS 		= Integer.valueOf (props.getProperty ("MAXGPS")).intValue (); } 				catch (Exception e) 	{ }
		try { MAXCOMPASS 	= Integer.valueOf (props.getProperty ("MAXCOMPASS")).intValue (); } 			catch (Exception e) 	{ }
		try { MAXINS 		= Integer.valueOf (props.getProperty ("MAXINS")).intValue (); } 				catch (Exception e) 	{ }
		try { MAXRADAR	 	= Integer.valueOf (props.getProperty ("MAXRADAR")).intValue (); }				catch (Exception e) 	{ }
		try { MAXENCS 		= Integer.valueOf (props.getProperty ("MAXENCS")).intValue (); } 				catch (Exception e) 	{ }
		try { MAXCAMERA 		= Integer.valueOf (props.getProperty ("MAXCAMERA")).intValue (); }				catch (Exception e) 	{ }
		try { MAXDIGITIZER 	= Integer.valueOf (props.getProperty ("MAXDIGITIZER")).intValue (); }			catch (Exception e) 	{ }

		// Create robot model
		if (model == null)
			model		= RobotModel.getModel (this, props);
		else
			model.update (props);

		trkfeat		= new SensorPos [MAXTRACKER];
		sonfeat		= new SensorPos [MAXSONAR];
		irfeat		= new SensorPos [MAXIR];
		lrffeat		= new SensorPos [MAXLRF];
		lsbfeat		= new SensorPos [MAXLSB];
		visfeat		= new SensorPos [MAXVISION];
		bumfeat		= new Line2 [MAXBUMPER];
		digfeat		= new String [MAXDIGITIZER];

		// TODO the user MUST define all sensor related parameters. There could be a betetr way to do it
		if (trk_modified)
			for (i = 0; i < MAXTRACKER; i++)
			{
				try { alpha		= Double.valueOf (props.getProperty ("trkfeat" + i)).doubleValue (); }			catch (Exception e) 	{ }
				try { len		= Double.valueOf (props.getProperty ("trklen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { rho		= Double.valueOf (props.getProperty ("trkrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { hgt		= Double.valueOf (props.getProperty ("trkhgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				trkfeat[i]		= new SensorPos ();
				trkfeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
				trkfeat[i].set_height (hgt);
			}

		if (son_modified)
			for (i = 0; i < MAXSONAR; i++)
			{
				try { alpha		= Double.valueOf (props.getProperty ("sonfeat" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { len		= Double.valueOf (props.getProperty ("sonlen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { rho		= Double.valueOf (props.getProperty ("sonrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { hgt		= Double.valueOf (props.getProperty ("sonhgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { stp		= Integer.valueOf (props.getProperty ("sonstep" + i)).intValue (); } 			catch (Exception e) 	{ }
				sonfeat[i]		= new SensorPos ();
				sonfeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
				sonfeat[i].set_height (hgt);
				sonfeat[i].step (stp);
			}

		if (ir_modified)
			for (i = 0; i < MAXIR; i++)
			{
				try { alpha		= Double.valueOf (props.getProperty ("irfeat" + i)).doubleValue (); } 			catch (Exception e)	{ }
				try { len		= Double.valueOf (props.getProperty ("irlen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { rho		= Double.valueOf (props.getProperty ("irrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { hgt		= Double.valueOf (props.getProperty ("irhgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { stp		= Integer.valueOf (props.getProperty ("irstep" + i)).intValue (); } 			catch (Exception e) 	{ }
				irfeat[i]		= new SensorPos ();
				irfeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
				irfeat[i].set_height (hgt);
				irfeat[i].step (stp);
			}

		if (lrf_modified)
			for (i = 0; i < MAXLRF; i++)
			{
				try { alpha		= Double.valueOf (props.getProperty ("lrffeat" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { len		= Double.valueOf (props.getProperty ("lrflen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { rho		= Double.valueOf (props.getProperty ("lrfrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { hgt		= Double.valueOf (props.getProperty ("lrfhgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { stp		= Integer.valueOf (props.getProperty ("lrfstep" + i)).intValue (); } 			catch (Exception e) 	{ }
				lrffeat[i]		= new SensorPos ();
				lrffeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
				lrffeat[i].set_height (hgt);
				lrffeat[i].step (stp);
			}

		if (lsb_modified)
			for (i = 0; i < MAXLSB; i++)
			{
				try { alpha		= Double.valueOf (props.getProperty ("lsbfeat" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { len		= Double.valueOf (props.getProperty ("lsblen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { rho		= Double.valueOf (props.getProperty ("lsbrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { hgt		= Double.valueOf (props.getProperty ("lsbhgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
				try { stp		= Integer.valueOf (props.getProperty ("lsbstep" + i)).intValue (); } 			catch (Exception e) 	{ }
				lsbfeat[i]		= new SensorPos ();
				lsbfeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
				lsbfeat[i].set_height (hgt);
				lsbfeat[i].step (stp);
			}
		
		for (i = 0; i < MAXVISION; i++)
		{
			try { alpha		= Double.valueOf (props.getProperty ("visfeat" + i)).doubleValue (); } 			catch (Exception e) 	{ }
			try { len		= Double.valueOf (props.getProperty ("vislen" + i)).doubleValue (); } 			catch (Exception e) 	{ }
			try { rho		= Double.valueOf (props.getProperty ("visrho" + i)).doubleValue (); } 			catch (Exception e) 	{ }
			try { hgt		= Double.valueOf (props.getProperty ("vishgt" + i)).doubleValue (); } 			catch (Exception e) 	{ }
			try { stp		= Integer.valueOf (props.getProperty ("visstep" + i)).intValue (); } 			catch (Exception e) 	{ }
			visfeat[i]		= new SensorPos ();
			visfeat[i].set_polar (len, rho * Angles.DTOR, alpha * Angles.DTOR);
			visfeat[i].set_height (hgt);
			visfeat[i].step (stp);
		}
		
		for (i = 0; i < MAXBUMPER; i++)
		{
			try { xi		= Double.valueOf (props.getProperty ("bumxi" + i)).doubleValue (); } 				catch (Exception e) 	{ }
			try { yi		= Double.valueOf (props.getProperty ("bumyi" + i)).doubleValue (); } 				catch (Exception e) 	{ }
			try { xf		= Double.valueOf (props.getProperty ("bumxf" + i)).doubleValue (); } 				catch (Exception e) 	{ }
			try { yf		= Double.valueOf (props.getProperty ("bumyf" + i)).doubleValue (); } 				catch (Exception e) 	{ }
			bumfeat[i]	= new Line2 ();
			bumfeat[i].set (xi, yi, xf, yf);
		}

		for (i = 0; i < MAXDIGITIZER; i++)
			digfeat[i]		= props.getProperty ("DIGITIZER" + i);
	}
	
	public String toString ()
	{
		String			str;
		
		str		= super.toString ();
		
		if (MAXSONAR > 0)		str += ",SON="+MAXSONAR;
		if (MAXIR > 0)			str += ",IR="+MAXIR;
		if (MAXLSB > 0)			str += ",LSB="+MAXLSB;
		if (MAXLRF > 0)			str += ",LRF="+MAXLRF;
		if (MAXBUMPER > 0)		str += ",BUM="+MAXBUMPER;
		if (MAXTRACKER > 0)		str += ",TRK="+MAXTRACKER;
		if (MAXGPS > 0)			str += ",GPS="+MAXGPS;
		if (MAXCOMPASS > 0)		str += ",COM="+MAXCOMPASS;
		if (MAXINS > 0)			str += ",INS="+MAXINS;		
		if (MAXRADAR > 0)		str += ",RAD="+MAXRADAR;
		if (MAXENCS > 0)			str += ",ENC="+MAXENCS;
		if (MAXCAMERA > 0)		str += ",CAM="+MAXCAMERA;
		if (MAXDIGITIZER > 0)		str += ",DIG="+MAXDIGITIZER;
		if (MAXVISION > 0)		str += ",VIS="+MAXVISION;
		
		return str;
	}
}

