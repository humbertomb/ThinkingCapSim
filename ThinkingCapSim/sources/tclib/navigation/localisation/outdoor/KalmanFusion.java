/*
 * (c) 2003-2004 Rafael Toledo Moreo
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.navigation.localisation.outdoor;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import devices.data.*;

import devices.pos.*;
import wucore.utils.math.stat.*;
import wucore.utils.math.jama.*;

public class KalmanFusion implements Filter
{	
	public static Properties props;

	// state matrixes
	public Matrix X		= new Matrix(3,1);	// position
	public Matrix Xprev	= new Matrix(3,1);	// previous position
	public Matrix V		= new Matrix(3,1);	// velocity
	public Matrix Vgps	= new Matrix(3,1);	// velocity GPS
	public Matrix Vprev	= new Matrix(3,1);	// previous velocity
	public Matrix XGPSprev = new Matrix(3,1);// previous GPS matrix
	public Matrix M		= new Matrix(1,1);	// nyquist matrix (1x1)
	
	// nyquist variables
	double m;
	double nyquist_threshold = 105.0;		// (5) 15 is quite ok,
											// WARNING The gretaer the number the,
											// less restrictive it is.
	
	// measure matrixes
	Matrix XGPS = new Matrix(3,1);	// GPS position
	Matrix AINS	= new Matrix(3,1);	// INS acceleration
	
	// position and pose variables
	public double x, y, z, vx, vy, vz, ax, ay, az;
	public double xpre, ypre, zpre, vxpre, vypre, vzpre;	// for XPRE matrix
	public double roll, pitch, yaw;
	public double xprev, yprev, zprev;
	
	// time variables
	public double prevT;
	public double T;
	
	// temp variables
	public int p;	// control for temporary data after gps starts
	double modV;
	public int n;
	public double v1, v2, v3, v4, v5, v6;
	double noiseFactor = 10;	// afecta muchisimo (demasiado, algo falla)
	
	// parte del filtro de kalman
	
	static double q = 10;	//ins
	static double r = 2;	//gps
	
	static double [][] aF = {{1,0,0},
			{0,1,0},
			{0,0,1},};
	
	static double [][] aH = {{1,0,0},
			{0,1,0},
			{0,0,1},};
	
	static double [][] aQ = {{q,0,0},
			{0,q,0},
			{0,0,q},};
	
	static double [][] aR = {{r,0,0},
			{0,r,0},
			{0,0,r},};
	
	public Matrix U	= new Matrix(3,1);	// input 				(3x1)
	public Matrix F	= new Matrix(aF);	// transition matrix 	(3x3)
	public Matrix H	= new Matrix(aH);	// observation matrix 	(3x3)
	public Matrix Q	= new Matrix(aQ);	// covarianza input 	(3x3)
	public Matrix R	= new Matrix(aR);	// covarianza obser 	(3x3)
	public Matrix Wi	= new Matrix(3,1);	// input noise			(3x1)
	public Matrix Wo	= new Matrix(3,1);	// observation noise	(3x1)
	public Matrix P	= new Matrix(3,3);	// covarianze filter	(3x3)
	public Matrix K	= new Matrix(3,3);	// kalman gain			(3x3)
	public Matrix S	= new Matrix(3,3);	// covariance innova	(3x3)
	public Matrix Ze	= new Matrix(3,1);	// estimation observ	(3x3)
	public Matrix In	= new Matrix(3,1);	// innovation			(3x3)
	
	// noise temp variables
	protected double				noise_i, noise_o;
	protected int					step;
	protected boolean				debug		= false;
	
	// Filter inputs
	protected Variables				variables;
	
	// Filter outputs (in world navigation frame)
	protected Position				out;
	protected Pose					pose;
	protected double				outvx, outvy, outvz;
	protected double[]				log;
	
	// Constructor
	public KalmanFusion ()
	{
		log		= new double[9];
		props = new Properties ();
		try
		{
			File file 				= new File ("conf/boatdevpos.conf");
			FileInputStream stream 	= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }
	}
	
	// Accessors
	public Position		getPosition ()		{ return out; }
	public Pose			getPose ()			{ return pose; }
	public int			getCounter ()		{ return step; }
	
	// Instance methdos
	public void initialise (Variables variables)
	{
		this.variables	= variables;
		
		out		= new Position ();
		pose	= new Pose();

		step		= -1;
	}
	
	public void step ()
	{
		/***************************** TIME ISSUES ************************/
		// navigation step
		step ++;
		prevT = T;
		
		/*************** COMPENSATION & COLLECTING MEASUREMENTS *************/
		// noise generation
		RandomNumberGenerator rand_i = new RandomNumberGenerator(System.currentTimeMillis());
		noise_i = (double)rand_i.nextGaussian()/noiseFactor;
		Wi.set(0,0, noise_i*0);
		Wi.set(1,0, noise_i*0);
		Wi.set(2,0, noise_i*0);
		RandomNumberGenerator rand_o = new RandomNumberGenerator (System.currentTimeMillis());
		noise_o = (double)rand_o.nextGaussian()/noiseFactor;
		Wo.set(0,0, noise_o*0);
		Wo.set(1,0, noise_o*0);
		Wo.set(2,0, noise_o*0);
		
		// compensation
		// TODO hay que comprobar los tstamps de compas/INS antes
		// de hacer la compensacion de las aceleraciones
		Matrix GPSLoc 	 = new Matrix(3,1);
		Matrix INSLoc	 = new Matrix(6,1);

		StringTokenizer	st	= new StringTokenizer (props.getProperty("GPSRELPOS"));
		GPSLoc.set(0,0, Double.parseDouble (st.nextToken() ) );
		GPSLoc.set(1,0, Double.parseDouble (st.nextToken() ) );
		GPSLoc.set(2,0, Double.parseDouble (st.nextToken() ) );

		st	= new StringTokenizer (props.getProperty("INSRELPOS"));
		INSLoc.set(0,0, Double.parseDouble (st.nextToken() ) );
		INSLoc.set(1,0, Double.parseDouble (st.nextToken() ) );
		INSLoc.set(2,0, Double.parseDouble (st.nextToken() ) );
		INSLoc.set(3,0, Double.parseDouble (st.nextToken() ) );
		INSLoc.set(4,0, Double.parseDouble (st.nextToken() ) );
		INSLoc.set(5,0, Double.parseDouble (st.nextToken() ) );

		Compensation.convert (variables, GPSLoc, INSLoc);
		
		AINS.set(0,0,Compensation.ax_n);
		AINS.set(1,0,Compensation.ay_n);
		AINS.set(2,0,Compensation.az_n);
		//XGPS.set(0,0,variables.xgps);
		//XGPS.set(1,0,variables.ygps);
		//XGPS.set(2,0,variables.zgps);
		XGPS.set(0,0,Compensation.xgps);
		XGPS.set(1,0,Compensation.ygps);
		XGPS.set(2,0,Compensation.zgps);

		/******************************** INITIALIZATION **************************/
		if (step < 1)
		{	
			X.set(0,0,variables.xgps);
			X.set(1,0,variables.ygps);
			X.set(2,0,variables.zgps);
			Xprev.set(0,0,variables.xgps);
			Xprev.set(1,0,variables.ygps);
			Xprev.set(2,0,variables.zgps);
			V.set(0,0,0);
			V.set(1,0,0);
			V.set(2,0,0);
			Vprev.set(0,0,0);
			Vprev.set(1,0,0);
			Vprev.set(2,0,0);
			XGPSprev.set(0,0,variables.xgps);
			XGPSprev.set(1,0,variables.ygps);
			XGPSprev.set(2,0,variables.zgps);
		}	
		/************************************** RUNNING **************************/
		else /* step >= 1 */
		{	
			// COMPASS YAW UPDATING if new compass data have come
			if (variables.compasscounter != variables.compasscounter_prev) {
				yaw = variables.yawcmp;
				variables.compasscounter_prev = variables.compasscounter;
				
				if (debug) System.out.println("-------------------------------COMPAS");
			}
			
			// INS ROLL & PITCH UPDATE AND STATE PREDICTION if new ins data have come
			if (variables.inscounter != variables.inscounter_prev)
			{
				// roll & pitch updatings
				if (variables.qins) {
					roll  = variables.rollins;
					pitch = variables.pitchins;
					yaw   = variables.yawins;
				}
				
				/*************** PREDICTION **********/
				if (variables.lastupdate==1)
				{	// last update is ins				
					// prediction time period = ins time period
					T = variables.tsins;
					
					// if prevT == 0, let's asume 0.2
					if (prevT==0) {V = (X.minus(Xprev)).times((double)1/0.2);}
					else{V = (X.minus(Xprev)).times((double)1/prevT);}
				}
				
				if (variables.lastupdate==2)
				{	// last update is gps
					// prediction time period = Tstampins - Tstampgps;
					T = variables.tstampins - variables.tstampgps;
					
					if(variables.penulqgps==0)
					{
						// constant velocity for one sample
						V.set(0,0,outvx);V.set(1,0,outvy);V.set(2,0,0);
					}
					else
					{
						
//						V = (X.minus(Xprev)).times((double)1/(.tsins-T)); // generates V unstable
						
						V.set(0,0,Vgps.get(0,0));
						V.set(1,0,Vgps.get(1,0));
						V.set(2,0,Vgps.get(2,0));
//						V.set(0,0,Vgps.get(0,0)/(.tsgps/.tsins));
//						V.set(1,0,Vgps.get(1,0)/(.tsgps/.tsins));
//						V.set(2,0,Vgps.get(2,0)/(.tsgps/.tsins));
					}
				}
				
				// updating previous state
				Xprev.set(0,0, X.get(0,0));
				Xprev.set(1,0, X.get(1,0));
				Xprev.set(2,0, X.get(2,0));
				Vprev.set(0,0, V.get(0,0));
				Vprev.set(1,0, V.get(1,0));
				Vprev.set(2,0, V.get(2,0));
				XGPSprev.set(0,0,XGPS.get(0,0));
				XGPSprev.set(1,0,XGPS.get(1,0));
				XGPSprev.set(2,0,XGPS.get(2,0));
				
				// recalculating velocity with yaw information
				vx = V.get(0,0);vy = V.get(1,0);vz = V.get(2,0);
				modV = Math.sqrt(vx*vx + vy*vy);
				
				vx = -modV*Math.sin(yaw);
				vy =  modV*Math.cos(yaw);
				vz = 0;
				V.set(0,0, vx);V.set(1,0, vy);V.set(2,0, vz);
				
				// prediction equation
				X = Xprev.plus(V.times(T));
				
				// INS update if ins data are reliable
				if (variables.qins) {
					
					// no kalman filter
//					X = Xprev.plus(V.times(prevT)).plus(AINS.times(0.5).times(prevT*prevT));
					
					U = F.times(V.times(T)).plus(F.times(AINS).times(0.5*T*T));
					X  = F.times(Xprev).plus(U).plus(Wi);
					Ze = H.times(X).plus(Wo);
					P  = F.times(P).times(F.transpose()).plus(Q);
					
					if (debug) System.out.println("---------------INS");
//					System.out.println("t "+.tstampins);
					
					// flow control
					variables.lastupdate = 1;
				}
				// ins counter update
				variables.inscounter_prev = variables.inscounter;
			}
			
			// GPS UPDATE	if new gps data have come and transmission is ok
			if (variables.gpscounter != variables.gpscounter_prev) 
			{
				if ((variables.qgps != GPSData.FIX_ERROR) && (variables.qgps != GPSData.FIX_INVALID))
				{
					// kalman update
					In = XGPS.minus(Ze);
					S  = H.times(P).times(H.transpose()).plus(R);
					
					// spurious control -> Nyquist equation
					M = In.transpose().times(S.inverse()).times(In);
					m = M.get(0,0);

					// m sale > 8.0E12
//					if (m < nyquist_threshold) 
//					{
						K  = P.times(H.transpose()).times(S.inverse());
						X  = F.times(Xprev).plus((K).times(In));
						P  = P.minus(K.times(S).times(K.transpose()));
						
						// gps velocity
						Vgps = XGPS.minus(XGPSprev).times(1/variables.tsgps);
						
						if (debug) System.out.println("----------------------------------------------------------------GPS");

						// practical variables
						variables.lastupdate = 2;
						if (variables.lastgpsq==0)
							variables.penulqgps=0;
						else
							variables.penulqgps=1;
						variables.lastgpsq = 1;
//					}
//					else
//						variables.lastgpsq=0;
					
					// no kalman filter, nor nyquist control
//					X.set(0,0,XGPS.get(0,0));
//					X.set(1,0,XGPS.get(1,0));
//					X.set(2,0,XGPS.get(2,0));
				}
				else
					variables.lastgpsq=0;
				
				// new gps data stuff
				variables.gpscounter_prev = variables.gpscounter;
			}
		}
		
		// pose matrix
		pose.set_ang(roll, pitch, yaw);
		
		// output variables
		out.set (X.get(0,0), X.get(1,0), X.get(2,0));
		outvx = V.get(0,0);
		outvy = V.get(1,0);
	}
	
	public double[] datalog ()
	{
		log[0]	= out.x ();
		log[1]	= out.y ();
		log[2]	= out.z ();
		log[3]	= pose.roll();;
		log[4]	= pose.pitch();
		log[5]	= pose.yaw();
		log[6]	= V.get (0, 0);
		log[7]	= V.get (1, 0);
		log[8]	= V.get (2, 0);
		
		return log;
	}
}