/** * Copyright: Copyright (c) 2002 * Company: Grupo ANTS - Proyecto MIMICS * @author Humberto Martinez Barbera (humberto@um.es) * @version 1.0 */package devices.drivers.beacon;public class LaserBeacon {	static public final double			DTOR			= (Math.PI / 180.0);		private String						clase;	private String						port;		boolean			debugError = false;	boolean			debugAll = false;			protected double					offset;		// Constructors     public static LaserBeacon getLaser (String props, double offset) throws LaserBeaconException    {      LaserBeacon		laser;      try      {		laser = getLaser (props);		laser.setOffset (offset * DTOR);      } catch (Exception e) { throw (new LaserBeaconException (e.toString())); }            return laser;    }	public static LaserBeacon getLaser (String props) throws LaserBeaconException	{		LaserBeacon		laser;		String			cname;		String			param;		Class			clase;		try		{   				cname	= props.substring (0,props.indexOf("|"));			param	= props.substring (props.indexOf("|")+1,props.length());			clase	= Class.forName (cname);        			System.out.println ("Laser: initialising "+cname+" with "+param+".");        			laser		= (LaserBeacon) clase.newInstance ();			laser.setType (cname);			laser.setConnection (param);			laser.initialise (param);        		} catch (Exception e) { throw new LaserBeaconException ("(getLaser) "+e.toString ()); }				return laser;		}		// Accessors	public void			setConnection (String port)			{ this.port = port; }		public String		getConnection ()					{ return port; }	public void			setType (String clase)				{ this.clase = clase; }	public String		getType ()							{ return clase; }    public void			setOffset (double offset)			{ this.offset = offset; }			// Debugging methods	public void debugAll(boolean debug){	debugAll = debug;	}	public void debugError(boolean debug){	debugError = debug;	}				// Instance methods	public void initialise (String param) throws LaserBeaconException	{	}			/////////////////////  P O S I T I O N  M O D E /////////////////////////		/** Request for position with automatic speed (with odometry)  (PP command)*/	public double[] getPosition () throws LaserBeaconException	{		return null;	}		/** Request for position with input of speed in the laser coordinate system (Pv command) */	public double[] getPosition(double Vx, double Vy) throws LaserBeaconException{		return null;	}		/** Request for position with input of speed and angular velocity in laser scanner coordinate system (Pw command) */	public double[] getPosition(double Vx, double Vy, double Va) throws LaserBeaconException{		return null;	}			/** Request for position with input of vehicle system speed (PV command) */	public double[] getPositionAbs(double Vx, double Vy, double Va) throws LaserBeaconException{		return null;	}			/** Activate positioning mode (PP command) */	public boolean activatePos () throws LaserBeaconException	{		return true;	}		/** Activate positioning mode with depth of smoothing input (PN command) */	public boolean activatePos (int number) throws LaserBeaconException	{		return true;	}		/** Selection of action radius (PO) */	public boolean changeRad (double Rfr,double Rto) throws LaserBeaconException {		return true;	}		/** Layer Selection (PL) */	public boolean selecLayer (int layer) throws LaserBeaconException {		return true;	}		public int findLayer() throws LaserBeaconException{		return -1;	}		/** Selection of number of N nearest (densest) (PC command) */	public boolean selecNearest (int N) throws LaserBeaconException {		return true;	}		/** Selection of layer with definition of position (PM command) */	public boolean changePos (double posx,double posy, double angle, int layer) throws LaserBeaconException	{		return true;	}					/////////////////////   S T A N D B Y    M O D E /////////////////////////		/** Activate standby mode (SA) */	public boolean activateStandby () throws LaserBeaconException {		return true;	}		/** Define direction of scanner rotation (SU) */	public boolean clockWise (int number) throws LaserBeaconException {		return true;	}	 		/** Request for reflector position (SR) */	public double[] getReflector(int layer, int nbeacon) throws LaserBeaconException{		return null;	}		/** Change a reflector position (SC) (position in meter) */	public boolean changeReflector(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{		return false;	}		/** Insert a reflector position (SI) (position in meter) */	public boolean insertReflector(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{		return false;	}		/** Delete a reflector position (SD) */	public boolean deleteReflector(int layer, int nbeacon) throws LaserBeaconException{		return false;	}				   //////////////////      C H A N G E    R A D I U S  //////////////			/** Read the reflector radius of layer (RG) */	public int getRadius (int layer) throws LaserBeaconException {		return -1;	}		/** Set the reflector radius (mm) for a layer (RS) */	public boolean setRadius (int layer, int radius) throws LaserBeaconException {		return false;	}				/////////////////////  M E A S U R E M E N T  M O D E /////////////////////////			/** Activated Measurement mode (TA) */	public boolean activateMeasures () throws LaserBeaconException {		return false;	}		/** Get measures of laser scanner  */	public double[] getMeasures() throws LaserBeaconException{		return null;	}			/** Get measures of laser scanner filtered (one measure for reflector) */	public double[][] getMeasures(int Nmin){		return null;	}		/////////////////////  M A P P I N G     M O D E   /////////////////////////		/** Activated Mapping mode  (MA) */	public boolean activateMapping () throws LaserBeaconException {		return false;	}		/** Start Mapping (MS) */	public int startMapping (double posx, double posy, double angle, int refRad, int layer) throws LaserBeaconException {		return -1;	}		/** Start Mapping measurement (MM) */	public int startMapping (double posx, double posy, double angle, int refRad, int layer, int mean) throws LaserBeaconException {		return -1;	}		/** Start negative mapping measurement (MN) */	public int startMappingNeg (double posx, double posy, double angle, int refRad, int layer, int mean) throws LaserBeaconException {		return -1;	}		/** Request for Mapping position (MR)  */	public double[] getMapping(int layer, int nbeacon) throws LaserBeaconException{		return null;	}				///////////////////// U P L O A D      M O D E/////////////////			/** Activated Upload mode (UA) */	public boolean activateUpload () throws LaserBeaconException {		return false;	}		/** Request Upload transmition (UR) */	public double[] getUpload(int layer) throws LaserBeaconException{		return null;	}	/////////////////////  D O W N L O A D    M O D E  /////////////////////////		/** Activated Download mode (DA) */	public boolean activateDownload () throws LaserBeaconException {		return false;	}		/** Download Reflector Posicion (DR) */	public boolean setDownload(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{		return false;	}		public void close(){}	}