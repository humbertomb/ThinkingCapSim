/*
 * (c) 2006 Miguel Juli Cristbal
 */

package tclib.navigation.localisation.outdoor;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.StringTokenizer;

import devices.data.*;

import devices.pos.*;
import wucore.utils.math.jama.*;

public class KalmanFilter implements Filter
{	
	public static Properties props;

	// state matrixes
	public Matrix X		= new Matrix(6,1);	// position
	public Matrix Xprev	= new Matrix(6,1);	// previous position
	
	// measure matrixes
	Matrix XGPS = new Matrix(3,1);	// GPS position
	Matrix AINS	= new Matrix(3,1);	// INS acceleration
	
	// time variables
	public double T;
	
	static double q = 10;	// covarianza del ruido del ins
	static double r = 2;		// covarianza del ruido del gps
	
	static double [][] aF = {{1,0,0,0,0,0},
							 {0,1,0,0,0,0},
							 {0,0,1,0,0,0},
							 {0,0,0,1,0,0},
							 {0,0,0,0,1,0},
							 {0,0,0,0,0,1},};

	static double [][] aB = {{1,0,0},
							 {0,1,0},
							 {0,0,1},
							 {1,0,0},
							 {0,1,0},
							 {0,0,1},};
	
	static double [][] aH = {{1,0,0,0,0,0},
							 {0,1,0,0,0,0},
							 {0,0,1,0,0,0}};
	
	static double [][] aQ = {{0.000025*q,0,0,0.0005*q,0,0},
							 {0,0.000025*q,0,0,0.0005*q,0},
							 {0,0,0.000025*q,0,0,0.0005*q},
							 {0.0005*q,0,0,0.01*q,0,0},
							 {0,0.0005*q,0,0,0.01*q,0},
							 {0,0,0.0005*q,0,0,0.01*q}};
	
	static double [][] aR = {{r,0,0},
							 {0,r,0},
							 {0,0,r}};
							 
	static double [][] aI = {{1,0,0,0,0,0},
							 {0,1,0,0,0,0},
							 {0,0,1,0,0,0},
							 {0,0,0,1,0,0},
							 {0,0,0,0,1,0},
							 {0,0,0,0,0,1}};

	public Matrix U	= new Matrix(3,1);	// input 				(3x1)
	public Matrix F	= new Matrix(aF);	// transition matrix 	(6x6)
	public Matrix B	= new Matrix(aB);	// transition matrix 	(6x3)
	public Matrix H	= new Matrix(aH);	// observation matrix 	(3x6)
	public Matrix Q	= new Matrix(aQ);	// covarianza input 	(6x6)
	public Matrix R	= new Matrix(aR);	// covarianza obser 	(3x3)
	public Matrix Wi= new Matrix(6,1);	// input noise			(6x1)
	public Matrix Wo= new Matrix(3,1);	// observation noise	(3x1)
	public Matrix I = new Matrix(aI);	// unit matrix			(3x1)
	public Matrix P ;					// covarianze filter	(3x3)
	public Matrix K	= new Matrix(6,3);	// kalman gain			(6x3)
	public Matrix S	= new Matrix(3,3);	// covariance innova	(3x3)
	public Matrix Ze= new Matrix(3,1);	// estimation observ	(3x3)
	public Matrix In= new Matrix(3,1);	// innovation			(3x3)
	public Matrix NIS=new Matrix(1,1);  // Normalized Innovation Squared

	public Matrix GPSLoc 	 = new Matrix(3,1);
	public Matrix INSLoc	 = new Matrix(6,1);
	
	// noise temp variables
	protected int					step;
	
	// Filter inputs
	protected Variables				variables;
	
	// Filter outputs (in world navigation frame)
	protected Position				out;
	protected Pose					pose;
	protected double				outvx, outvy, outvz;
	protected double[]				log;
	
	// Constructor
	public KalmanFilter ()
	{
		log		= new double[9];
		props = new Properties ();
		try
		{
			File file 				= new File ("conf/boatdevpos.conf");
			FileInputStream stream 	= new FileInputStream (file);
			props.load (stream);
			stream.close ();

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
		
		/*************** COMPENSATION & COLLECTING MEASUREMENTS *************/		
		// compensation
		Compensation.convert (variables, GPSLoc, INSLoc);
		
		AINS.set(0,0,Compensation.ax_n);
		AINS.set(1,0,Compensation.ay_n);
		AINS.set(2,0,Compensation.az_n-9.8);
		XGPS.set(0,0,Compensation.xgps);
		XGPS.set(1,0,Compensation.ygps);
		XGPS.set(2,0,Compensation.zgps);
		variables.rollins = Compensation.roll_n;
		variables.pitchins = Compensation.pitch_n;
		variables.yawins = Compensation.yaw_n;
		
		/******************************** INITIALIZATION **************************/
		if (step < 1)
		{	
			X.set(0,0,0);
			X.set(1,0,0);
			X.set(2,0,0);
			X.set(3,0,0);
			X.set(4,0,0);
			X.set(5,0,0);
			Xprev.set(0,0,0);
			Xprev.set(1,0,0);
			Xprev.set(2,0,0);
			Xprev.set(3,0,0);
			Xprev.set(4,0,0);
			Xprev.set(5,0,0);
			P = new Matrix(aI);
		}	
		/************************************** RUNNING **************************/
		else  /* step >= 1 */
		{	
			// updating previous state
			Xprev.set(0,0, X.get(0,0));
			Xprev.set(1,0, X.get(1,0));
			Xprev.set(2,0, X.get(2,0));
			Xprev.set(3,0, X.get(3,0));
			Xprev.set(4,0, X.get(4,0));
			Xprev.set(5,0, X.get(5,0));

			// INS ROLL & PITCH UPDATE AND STATE PREDICTION if new ins data have come
			if (variables.inscounter != variables.inscounter_prev)
			{
				/*************** PREDICTION **********/
				if (variables.lastupdate==1){
					T = variables.tsins;
				}		
				if (variables.lastupdate==2){
					T = variables.tstampins - variables.tstampgps;
				}
				//System.out.println("T"+T);
				// prediction equation
				F.set(0,3,T);
				F.set(1,4,T);
				F.set(2,5,T);
				B.set(0,0,0.5*T*T);
				B.set(1,1,0.5*T*T);
				B.set(2,2,0.5*T*T);
				B.set(3,0,T);
				B.set(4,1,T);
				B.set(5,2,T);
				X  = F.times(Xprev).plus(B.times(AINS));
				Ze = H.times(X);
				P  = F.times(P.times(F.transpose())).plus(Q);
					
				// flow control
				variables.lastupdate = 1;
				
				// ins counter update
				variables.inscounter_prev = variables.inscounter;
			}
			
			// GPS UPDATE	if new gps data have come and transmission is ok
			if (variables.gpscounter != variables.gpscounter_prev) 
			{
				if ((variables.qgps != GPSData.FIX_ERROR) && (variables.qgps != GPSData.FIX_INVALID))
				{
					T = variables.tstampgps - variables.tstampins;

					// prediction equation
					F.set(0,3,T);
					F.set(1,4,T);
					F.set(2,5,T);
					B.set(0,0,0.5*T*T);
					B.set(1,1,0.5*T*T);
					B.set(2,2,0.5*T*T);
					B.set(3,0,T);
					B.set(4,1,T);
					B.set(5,2,T);
					X  = F.times(Xprev).plus(B.times(AINS));
					Ze = H.times(X);
					P  = F.times(P.times(F.transpose())).plus(Q);
					// kalman update
					In = XGPS.minus(Ze);
					S  = H.times(P.times(H.transpose())).plus(R);
					K  = P.times(H.transpose().times(S.inverse()));
					X  = X.plus(K.times(In));
					P  = (I.minus(K.times(H))).times(P);
					
					NIS = In.transpose().times(S.inverse().times(In));
					//System.out.println(", NIS:"+NIS.get(0,0));
					
					// practical variables
					variables.lastupdate = 2;
				
					// new gps data stuff
					variables.gpscounter_prev = variables.gpscounter;
				}
			}
			//System.out.println("GPS:"+XGPS.get(0,0)+","+XGPS.get(1,0)+","+XGPS.get(2,0));
			//System.out.println("INS:"+AINS.get(0,0)+","+AINS.get(1,0)+","+AINS.get(2,0));
			System.out.println("POS:"+X.get(0,0)+","+X.get(1,0)+","+X.get(2,0));
			System.out.println("VEL:"+X.get(3,0)+","+X.get(4,0)+","+X.get(5,0));
		}
		
		// pose matrix
		pose.set_ang(variables.rollins, variables.pitchins, variables.yawins);
		
		// output variables
		out.set (X.get(0,0), X.get(1,0), X.get(2,0), variables.yawins);
		outvx = X.get(3,0);
		outvy = X.get(4,0);
	}
	
	public double[] datalog ()
	{
		log[0]	= out.x ();
		log[1]	= out.y ();
		log[2]	= out.z ();
		log[3]	= pose.roll();
		log[4]	= pose.pitch();
		log[5]	= pose.yaw();
		log[6]	= X.get (3, 0);
		log[7]	= X.get (4, 0);
		log[8]	= X.get (5, 0);
		
		return log;
	}
}