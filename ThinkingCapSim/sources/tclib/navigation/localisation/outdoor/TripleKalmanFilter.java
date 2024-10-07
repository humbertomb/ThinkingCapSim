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


public class TripleKalmanFilter implements Filter
{	
	public static Properties props;
	public static int INS = 1;
	public static int GPS = 2;
	public static int CMP = 3;
	
	public  double g2r			=	Math.PI/180;
	public  double r2g			=	180/Math.PI;

	// state matrixes
	public Matrix X		= new Matrix(15,1);	// position
	public Matrix Xprev	= new Matrix(15,1);	// previous position
	
	// measure matrixes
	Matrix XGPS = new Matrix(3,1);	// GPS position
	Matrix VGPS = new Matrix(3,1);  // GPS velocity

	Matrix AINS	= new Matrix(3,1);	// INS acceleration
	Matrix QINS	= new Matrix(3,1);	// INS orientation
	Matrix WINS = new Matrix(3,1);  // INS Angular speed

	Matrix QCMP = new Matrix(3,1);  // CMP orientation
	
	// time variables
	public double T;
	
	// Experimentalmente
	static double qp = 0.1;			// covarianza del ruido del sistema
	static double qv = 0.0625;		// covarianza del ruido del sistema
	static double qa = 2.25;		// covarianza del ruido del sistema
	static double qq = 0.0025;		// covarianza del ruido del sistema
	static double qw = 0.0225;		// covarianza del ruido del sistema
	
	// De las hojas de caractersticas
	static double rGPSp = 4;		// covarianza del ruido del gps podria depender de nsats
	static double rGPSv = 0.0025;	// covarianza del ruido del gps podria depender de nsats
	static double rINSa = 0.0004;	// covarianza del ruido del ins
	static double rINSq = 0.0012;	// covarianza del ruido del ins
	static double rINSw = 0.0075;	// covarianza del ruido del ins
	static double rCMP  = 0.000075;	// covarianza del ruido del cmp
	
	static double [][] aHGPS = {{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
								{0,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
								{0,0,1,0,0,0,0,0,0,0,0,0,0,0,0},
								{0,0,0,1,0,0,0,0,0,0,0,0,0,0,0},
								{0,0,0,0,1,0,0,0,0,0,0,0,0,0,0},
								{0,0,0,0,0,1,0,0,0,0,0,0,0,0,0}};
	
	static double [][] aHINS = {{0,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
								{0,0,0,0,0,0,0,1,0,0,0,0,0,0,0},
								{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0},
								{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0},
								{0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
								{0,0,0,0,0,0,0,0,0,0,0,1,0,0,0},
								{0,0,0,0,0,0,0,0,0,0,0,0,1,0,0},
								{0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
								{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1}};

	static double [][] aHCMP = {{0,0,0,0,0,0,1,0,0,0,0,0,0,0,0},
								{0,0,0,0,0,0,0,1,0,0,0,0,0,0,0},
								{0,0,0,0,0,0,0,0,1,0,0,0,0,0,0}};
	
	static double [][] aRGPS = {{rGPSp, 0, 0, 0, 0, 0},
								{0, rGPSp, 0, 0, 0, 0},
								{0, 0, rGPSp, 0, 0, 0},
								{0, 0, 0, rGPSv, 0, 0},
								{0, 0, 0, 0, rGPSv, 0},
								{0, 0, 0, 0, 0, rGPSv}};

	static double [][] aRINS = {{rINSa, 0, 0, 0, 0, 0, 0, 0, 0},
								{0, rINSa, 0, 0, 0, 0, 0, 0, 0},
								{0, 0, rINSa, 0, 0, 0, 0, 0, 0},
								{0, 0, 0, rINSq, 0, 0, 0, 0, 0},
								{0, 0, 0, 0, rINSq, 0, 0, 0, 0},
								{0, 0, 0, 0, 0, rINSq, 0, 0, 0},
								{0, 0, 0, 0, 0, 0, rINSw, 0, 0},
								{0, 0, 0, 0, 0, 0, 0, rINSw, 0},
								{0, 0, 0, 0, 0, 0, 0, 0, rINSw}};

	static double [][] aRCMP = {{rCMP, 0, 0},
						 	    {0, rCMP, 0},
							    {0, 0, rCMP}};	

	static double [][] aQ = {{qp,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
							 {0,qp,0,0,0,0,0,0,0,0,0,0,0,0,0},
							 {0,0,qp,0,0,0,0,0,0,0,0,0,0,0,0},
							 {0,0,0,qv,0,0,0,0,0,0,0,0,0,0,0},
							 {0,0,0,0,qv,0,0,0,0,0,0,0,0,0,0},
							 {0,0,0,0,0,qv,0,0,0,0,0,0,0,0,0},
							 {0,0,0,0,0,0,qa,0,0,0,0,0,0,0,0},
							 {0,0,0,0,0,0,0,qa,0,0,0,0,0,0,0},
							 {0,0,0,0,0,0,0,0,qa,0,0,0,0,0,0},
							 {0,0,0,0,0,0,0,0,0,qq,0,0,0,0,0},
							 {0,0,0,0,0,0,0,0,0,0,qq,0,0,0,0},
							 {0,0,0,0,0,0,0,0,0,0,0,qq,0,0,0},
							 {0,0,0,0,0,0,0,0,0,0,0,0,qw,0,0},
							 {0,0,0,0,0,0,0,0,0,0,0,0,0,qw,0},
							 {0,0,0,0,0,0,0,0,0,0,0,0,0,0,qw}};
	
	public Matrix Q		= new Matrix(aQ);	// covarianza input 	(15x15)
	public Matrix RGPS	= new Matrix(aRGPS);// covarianza obser 	(6x6)
	public Matrix RINS	= new Matrix(aRINS);// covarianza obser 	(9x9)
	public Matrix RCMP	= new Matrix(aRCMP);// covarianza obser 	(3x3)
	public Matrix I 	= new Matrix(15,15);// unit matrix			(15x15)
	public Matrix P 	= new Matrix(15,15);// covarianze filter	(15x15)
	public Matrix F 	= new Matrix(15,15);// model				(15x15)
	public Matrix KGPS	= new Matrix(15,6);	// kalman gain			(15x6)
	public Matrix KINS	= new Matrix(15,9);	// kalman gain			(15x9)
	public Matrix KCMP	= new Matrix(15,3);	// kalman gain			(15x3)
	public Matrix SGPS	= new Matrix(6,6);	// covariance innova	(6x6)
	public Matrix SINS	= new Matrix(9,9);	// covariance innova	(9x9)
	public Matrix SCMP	= new Matrix(3,3);	// covariance innova	(3x3)
	public Matrix ZeGPS = new Matrix(6,1);	// estimation observ	(6x1)
	public Matrix ZGPS  = new Matrix(6,1);	// observ				(6x1)
	public Matrix ZeINS = new Matrix(9,1);	// estimation observ	(9x1)
	public Matrix ZINS  = new Matrix(9,1);	// observ				(9x1)
	public Matrix ZeCMP = new Matrix(3,1);	// estimation observ	(3x1)
	public Matrix ZCMP  = new Matrix(3,1);	// observ				(3x1)
	public Matrix InGPS = new Matrix(6,1);	// innovation			(6x1)
	public Matrix InINS = new Matrix(9,1);	// innovation			(9x1)
	public Matrix InCMP = new Matrix(3,1);	// innovation			(3x1)
	public Matrix HGPS	= new Matrix(aHGPS);
	public Matrix HINS	= new Matrix(aHINS);
	public Matrix HCMP	= new Matrix(aHCMP);

	public Matrix GPSLoc 	 = new Matrix(3,1);
	public Matrix INSLoc	 = new Matrix(6,1);
	
	// noise temp variables
	protected int					step;
	
	// Filter inputs
	protected Variables				variables;
	
	// Filter outputs (in world navigation frame)
	protected Position				out;
	protected Pose					pose;
	//protected double				outvx, outvy, outvz;
	protected double[]				log;
	
	// Constructor
	public TripleKalmanFilter ()
	{
		log		= new double[30];
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
				
			I.identity();
			F.identity();
			
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
		
		XGPS.set(0,0,Compensation.xgps);
		XGPS.set(1,0,Compensation.ygps);
		XGPS.set(2,0,Compensation.zgps);
		VGPS.set(0,0,Compensation.xvgps);
		VGPS.set(1,0,Compensation.yvgps);
		VGPS.set(2,0,Compensation.zvgps);
		AINS.set(0,0,Compensation.ax_n);
		AINS.set(1,0,Compensation.ay_n);
		AINS.set(2,0,Compensation.az_n-9.8);
		QINS.set(0,0,Compensation.roll_n);
		QINS.set(1,0,Compensation.pitch_n);
		QINS.set(2,0,Compensation.yaw_n);
		WINS.set(0,0,Compensation.rollrate_n);
		WINS.set(1,0,Compensation.pitchrate_n);
		WINS.set(2,0,Compensation.yawrate_n);
		QCMP.set(0,0,variables.rollcmp); // NO COMPENSATED
		QCMP.set(1,0,variables.pitchcmp);
		QCMP.set(2,0,variables.yawcmp);
				
		/******************************** INITIALIZATION **************************/
		if (variables.gpscounter < 1 )
		{	
			// ESTADO a 0
			for (int i = 1; i<15; i++){
				X.set(i,0,0);
				Xprev.set(i,0,0);
			}
			X.set(0,0,XGPS.get(0,0));
			X.set(1,0,XGPS.get(1,0));
			X.set(2,0,XGPS.get(2,0));
			
			// COVARIANZA 1
			P.diagonal(1);
		}	
		/************************************** RUNNING **************************/
		else  /* step >= 1 */
		{	
			// updating previous state
			for (int i=0; i<15;i++)	Xprev.set(i,0, X.get(i,0));

			// Getting time
			// INS UPDATED
			if (variables.inscounter != variables.inscounter_prev){
				if (variables.lastupdate == INS)
					T = variables.tsins;
				if (variables.lastupdate == GPS)
					T = variables.tstampins - variables.tstampgps;
				if (variables.lastupdate == CMP)
					T = variables.tstampins - variables.tstampcmp;
				variables.lastupdate = INS;
				variables.inscounter_prev = variables.inscounter;
			}
			// GPS UPDATED
			if (variables.gpscounter != variables.gpscounter_prev){
				if ((variables.qgps != GPSData.FIX_ERROR) && (variables.qgps != GPSData.FIX_INVALID) && (variables.towgps != variables.towgps_prev)){
					if (variables.lastupdate == INS)
						T = variables.tstampgps - variables.tstampins;
					if (variables.lastupdate == GPS)
						T = variables.tsgps;
					if (variables.lastupdate == CMP)
						T = variables.tstampgps - variables.tstampcmp;
					variables.lastupdate = GPS;
					variables.gpscounter_prev = variables.gpscounter;
					variables.towgps_prev = variables.towgps;
				}
			}
			// INS UPDATED
			if (variables.compasscounter != variables.compasscounter_prev){
				if (variables.lastupdate == INS)
					T = variables.tstampcmp - variables.tstampins;
				if (variables.lastupdate == GPS)
					T = variables.tstampcmp - variables.tstampgps;
				if (variables.lastupdate == CMP)
					T = variables.tscmp;
				variables.lastupdate = CMP;
				variables.compasscounter_prev = variables.compasscounter;
			}

			/*************** PREDICTION **********/
			/**** this is common to all filters */
			F.set(0,3,T);
			F.set(1,4,T);
			F.set(2,5,T);
			F.set(0,6,0.5*T*T);
			F.set(1,7,0.5*T*T);
			F.set(2,8,0.5*T*T);
			F.set(3,6,T);
			F.set(4,7,T);
			F.set(5,8,T);
			F.set(9,12,T);
			F.set(10,13,T);
			F.set(11,14,T);
			
			X = F.times(Xprev);
			P = F.times(P.times(F.transpose())).plus(Q);
		
			// ******* UPDATE
			// INS FILTER
			if (variables.lastupdate == INS)
			{
				ZINS.set(0,0,AINS.get(0,0));
				ZINS.set(1,0,AINS.get(1,0));
				ZINS.set(2,0,AINS.get(2,0));
				ZINS.set(3,0,QINS.get(0,0));
				ZINS.set(4,0,QINS.get(1,0));
				ZINS.set(5,0,QINS.get(2,0));
				ZINS.set(6,0,WINS.get(2,0));
				ZINS.set(7,0,WINS.get(2,0));
				ZINS.set(8,0,WINS.get(2,0));

				ZeINS = HINS.times(X);
				InINS = ZINS.minus(ZeINS);
				SINS  = HINS.times(P.times(HINS.transpose())).plus(RINS);
				KINS  = P.times(HINS.transpose().times(SINS.inverse()));
				X  = X.plus(KINS.times(InINS));
				P  = (I.minus(KINS.times(HINS))).times(P);
				
			}
			
			// GPS FILTER
			if (variables.lastupdate == GPS) 
			{
				ZGPS.set(0,0,XGPS.get(0,0));
				ZGPS.set(1,0,XGPS.get(1,0));
				ZGPS.set(2,0,XGPS.get(2,0));
				ZGPS.set(3,0,VGPS.get(0,0));
				ZGPS.set(4,0,VGPS.get(1,0));
				ZGPS.set(5,0,VGPS.get(2,0));
				
				ZeGPS = HGPS.times(X);
				InGPS = ZGPS.minus(ZeGPS);
				SGPS  = HGPS.times(P.times(HGPS.transpose())).plus(RGPS);
				KGPS  = P.times(HGPS.transpose().times(SGPS.inverse()));
				X  = X.plus(KGPS.times(InGPS));
				P  = (I.minus(KGPS.times(HGPS))).times(P);
			}
			// COMPASS FILTER
			if (variables.lastupdate == CMP)
			{
				ZCMP.set(0,0,QCMP.get(0,0));
				ZCMP.set(1,0,QCMP.get(1,0));
				ZCMP.set(2,0,QCMP.get(2,0));

				ZeCMP = HCMP.times(X);
				InCMP = ZCMP.minus(ZeCMP);
				SCMP  = HCMP.times(P.times(HCMP.transpose())).plus(RCMP);
				KCMP  = P.times(HCMP.transpose().times(SCMP.inverse()));
				X  = X.plus(KCMP.times(InCMP));
				P  = (I.minus(KCMP.times(HCMP))).times(P);	
			}
		}
		
		// output variables
		out.set (X.get(0,0), X.get(1,0), X.get(2,0), X.get(9,0));
		pose.set_spd(X.get(3,0), X.get(4,0), X.get(5,0));
		pose.set_ang(X.get(9,0),X.get(10,0),X.get(11,0));
		pose.set_rate(X.get(12,0),X.get(13,0),X.get(14,0));
	}
	
	public double[] datalog ()
	{
		log[0]	= X.get(0,0);
		log[1]	= X.get(1,0);
		log[2]	= X.get(2,0);
		log[3]	= X.get(3,0);
		log[4]	= X.get(4,0);
		log[5]	= X.get(5,0);
		log[6]	= X.get(6,0);
		log[7]	= X.get(7,0);
		log[8]	= X.get(8,0);
		log[9]	= X.get(9,0);
		log[10]	= X.get(10,0);
		log[11]	= X.get(11,0);
		log[12]	= X.get(12,0);
		log[13]	= X.get(13,0);
		log[14]	= X.get(14,0);
		
		log[15]	= P.get(0,0);
		log[16]	= P.get(1,1);
		log[17]	= P.get(2,2);
		log[18]	= P.get(3,3);
		log[19]	= P.get(4,4);
		log[20]	= P.get(5,5);
		log[21]	= P.get(6,6);
		log[22]	= P.get(7,7);
		log[23]	= P.get(8,8);
		log[24]	= P.get(9,9);
		log[25]	= P.get(10,10);
		log[26]	= P.get(11,11);
		log[27]	= P.get(12,12);
		log[28]	= P.get(13,13);
		log[29]	= P.get(14,14);

		return log;
	}
}