/*
 * (c) 2003 Rafael Toledo Moreo
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.navigation.localisation.outdoor;

import devices.pos.*;
import wucore.utils.math.jama.*;

/********************************************* filtrado ***************************

TRES dimensiones. variables de salida 'x', 'y', 'z', 'roll', 'pitch' y 'yaw'.
vector de estado. [x, vx, roll, y, vy, pitch, z, vz, yaw]

(1 necesitamos integrar las medidas INS para obtener la posicion)

Fusion mediante el filtro de Kalman de los datos del ins con el gps y el compas.


************************************************************************************/

public class KalmanSimple implements Filter
{
	// caracteristicas fisicas
	public  double g2r			=	Math.PI/180;
	public  double r2g			=	180/Math.PI;
	public  double	mg			=	0.0098;							// mg en m/s2.

	protected double					dtime;
	protected Position				kpos;
	protected Pose					pose;
	protected Variables			variables;
	protected int					step		= -1;
	
	// caracteristicas del avion
	public  int		velMax		=	34;								// velocidad crucero en m/s.
	public  double 	DPREVel		=	34*dtime;							// varianza de velocidad en la prediccion
	public  double 	DPREPos		=	34;								// varianza de posicion en la prediccion
	public  double 	DPREAng		=	1;								// varianza de angulo en la prediccion	
	public  double 	DPREVAng	=	1*dtime;							// varianza de la velocidad angular en la prediccion
	public  double 	VPREVel		=	Math.pow((DPREVel*3), 2);		// varianza de velocidad en la prediccion
	public  double 	VPREPos		=	Math.pow((DPREPos*3), 2);		// varianza de posicion en la prediccion
	public  double 	VPREAng		=	Math.pow((DPREAng*3), 2);		// varianza de angulo en la prediccion
	public  double 	VPREVAng	=	Math.pow((DPREVAng*3), 2);		// varianza de la velocidad angular en la prediccion
		
	// caracteristicas INS
	public  double	Tins 		=   1;								// periodo INS
	public  double 	DINSAce		=	8.5*mg;							// desviacion ins de aceleracion
	public  double 	DINSVel		=	85*mg;							// desviacion ins de velocidad
	public  double 	DINSPos		=	850*mg;							// desviacion ins de posicion
	public  double 	DINSAng		=	g2r * 2;						// desviacion ins de posicion angular
	public  double 	DINSVAng	=	g2r * 2;						// desviacion ins de velocidad angular
	public  double 	VINSVel		=	Math.pow((DINSVel*3), 2);		// varianza ins de velocidad
	public  double 	VINSPos		=	Math.pow((DINSPos*3), 2);		// varianza ins de posicion
	public  double 	VINSVAng	=	Math.pow((DINSVAng*3), 2);		// varianza ins de velocidad angular
	public  double 	VINSAng		=	Math.pow((DINSAng*3), 2);		// varianza ins de posicin angular

	// caracteristicas GPS
	public  double 	DGPSPos		=	0.4;							// desviacion gps de posicion
	public  double 	VGPSPos		=	Math.pow((DGPSPos*3), 2);		// varianza gps de posicion
	public  double 	DGPSPosZ	=	1.0;							// desviacion gps de posicion eje Z
	public  double 	VGPSPosZ	=	Math.pow((DGPSPosZ*3), 2);		// varianza gps de posicion eje Z
	
	// compas electronico
	public  double	DCMPAng		=	g2r * 1;						// desviacion compas = 1 grado
	public  double	VCMPAng		=	Math.pow((DCMPAng*3), 2);		// varianza compas


	double [][] aF	 =	{	{1, dtime, 0, 0, 0, 0, 0, 0, 0},
							{0, 1, 0, 0, 0, 0, 0, 0, 0},
							{0, 0, 1, 0, 0, 0, 0, 0, 0},
							{0, 0, 0, 1, dtime, 0, 0, 0, 0},
							{0, 0, 0, 0, 1, 0, 0, 0, 0},
							{0, 0, 0, 0, 0, 1, 0, 0, 0},
							{0, 0, 0, 0, 0, 0, 1, dtime, 0},
							{0, 0, 0, 0, 0, 0, 0, 1, 0},
							{0, 0, 0, 0, 0, 0, 0, 0, 1},
						};
	double [][] aBins = {	{Math.pow(dtime,2)/2, 0, 0, 0, 0, 0 },
							{dtime,	0, 0, 0, 0, 0				},
							{0, dtime, 0, 0, 0, 0				},
							{0, 0, Math.pow(dtime,2)/2, 0, 0, 0	},
							{0, 0, dtime,	0, 0, 0				},
							{0, 0, 0, dtime, 0, 0				},
							{0, 0, 0, 0, Math.pow(dtime,2)/2, 0	},
							{0, 0, 0, 0, dtime,	0				},
							{0, 0, 0, 0, 0, dtime				},
						};
	double [][] aHins = {	{1, 0, 0, 0, 0, 0, 0, 0, 0},
							{0, 1, 0, 0, 0, 0, 0, 0, 0},
							{0, 0, 1, 0, 0, 0, 0, 0, 0},
							{0, 0, 0, 1, 0, 0, 0, 0, 0},
							{0, 0, 0, 0, 1, 0, 0, 0, 0},
							{0, 0, 0, 0, 0, 1, 0, 0, 0},
							{0, 0, 0, 0, 0, 0, 1, 0, 0},
							{0, 0, 0, 0, 0, 0, 0, 1, 0},
							{0, 0, 0, 0, 0, 0, 0, 0, 1},
						};									
	double [][] aRins = {	{VINSPos, 0, 0, 0, 0, 0, 0, 0, 0},
							{0, VINSVel, 0, 0, 0, 0, 0, 0, 0},
							{0, 0, VINSAng, 0, 0, 0, 0, 0, 0},
							{0, 0, 0, VINSPos, 0, 0, 0, 0, 0},
							{0, 0, 0, 0, VINSVel, 0, 0, 0, 0},
							{0, 0, 0, 0, 0, VINSAng, 0, 0, 0},
							{0, 0, 0, 0, 0, 0, VINSPos, 0, 0},
							{0, 0, 0, 0, 0, 0, 0, VINSVel, 0},
							{0, 0, 0, 0, 0, 0, 0, 0, VINSAng},
						};
	double [][] aQpre = {	{VPREPos, 0, 0, 0, 0, 0, 0, 0, 0},
							{0, VPREVel, 0, 0, 0, 0, 0, 0, 0},
							{0, 0, VPREAng, 0, 0, 0, 0, 0, 0},
							{0, 0, 0, VPREPos, 0, 0, 0, 0, 0},
							{0, 0, 0, 0, VPREVel, 0, 0, 0, 0},
							{0, 0, 0, 0, 0, VPREAng, 0, 0, 0},
							{0, 0, 0, 0, 0, 0, VPREPos, 0, 0},
							{0, 0, 0, 0, 0, 0, 0, VPREVel, 0},
							{0, 0, 0, 0, 0, 0, 0, 0, VPREAng},
						};
	double [][] aRgps = {	{VGPSPos, 0, 0},
							{0, VGPSPos, 0},
							{0, 0, VGPSPosZ},							
						};
	double [][] aHgps = {	{1, 0, 0, 0, 0, 0, 0, 0, 0},
							{0, 0, 0, 1, 0, 0, 0, 0, 0},
							{0, 0, 0, 0, 0, 0, 1, 0, 0},							
						};
	double [][] aRcmp = {	{VCMPAng},
						};
	double [][] aHcmp = {	{0, 0, 0, 0, 0, 0, 0, 0, 1},
						};


	Matrix F	=	new Matrix(aF);
	Matrix P	=	new Matrix(9, 9);
	Matrix Q	=	new Matrix(aQpre);
	Matrix Bins	=	new Matrix(aBins);
	Matrix Hins	=	new Matrix(aHins);
	Matrix Sins	=	new Matrix(9, 9);
	Matrix Wins	=	new Matrix(9, 9);
	Matrix Rins =	new Matrix(aRins);

	Matrix Hgps	=	new Matrix(aHgps);
	Matrix Sgps	=	new Matrix(3, 3);	
	Matrix Wgps	=	new Matrix(6, 3);
	Matrix Rgps =	new Matrix(aRgps);

	Matrix Hcmp	=	new Matrix(aHcmp);
	Matrix Scmp =	new Matrix(1, 1);
	Matrix Wcmp	=	new Matrix(6, 1);
	Matrix Rcmp =	new Matrix(aRcmp);
	
	Matrix estado 		= new Matrix(9, 1);
	Matrix previo 		= new Matrix(9, 1);		
	Matrix innovaIns	= new Matrix(9, 1);
	Matrix innovaGps	= new Matrix(9, 1);		
	Matrix innovaCmp	= new Matrix(9, 1);				
	Matrix estIns		= new Matrix(9, 1);

	Matrix medGps 	= new Matrix(3, 1);
	Matrix medIns 	= new Matrix(6, 1);
	Matrix medCmp 	= new Matrix(1, 1);

	protected double[]				vars		= new double [9];
	
	// Constructor
	public KalmanSimple () 
	{
	}
	
	// Accessors
	public Position		getPosition ()		{ return kpos; }
	public Pose			getPose ()			{ return pose; }
	public int			getCounter ()		{ return step; }
	
	// Instance methods
	public void initialise (Variables variables)
	{
		this.variables	= variables;
		kpos			= new Position ();
		pose			= new Pose();

	}
			
	public void step ()
	{		
		step ++;
		
		medGps.set(0, 0, variables.xgps);
		medGps.set(1, 0, variables.ygps);
		medGps.set(2, 0, variables.zgps);		
		medCmp.set(0, 0, variables.yawcmp);
		medIns.set(0, 0, variables.rollins);
		medIns.set(1, 0, variables.rollrateins);
		medIns.set(2, 0, variables.pitchins);
		medIns.set(3, 0, variables.pitchrateins);
		medIns.set(4, 0, variables.yawcmp);
		medIns.set(5, 0, variables.yawrateins);

		// prediccion
		previo		=	estado;
		estado		=	F.times(estado);
		P			=	(F.times(P).times(F.transpose())).plus(Q);

		// actualizacion con ins
		estIns		=	(F.times(previo)).plus(Bins.times(medIns));
		innovaIns	=	estIns.minus(estado);
		Sins		=	(Hins.times(P).times(Hins.transpose())).plus(Rins);
		Wins		=	(P.times(Hins.transpose())).times(Sins.inverse());
		estado		=	estado.plus(Wins.times(innovaIns));
		P			=	P.minus(Wins.times(Sins).times(Wins.transpose()));

		// actualizacion con gps
		innovaGps	=	medGps.minus(Hgps.times(estado));
		Sgps		=	(Hgps.times(P).times(Hgps.transpose())).plus(Rgps);
		Wgps		=	(P.times(Hgps.transpose())).times(Sgps.inverse());
		estado		=	estado.plus(Wgps.times(innovaGps));
		P			=	P.minus(Wgps.times(Sgps).times(Wgps.transpose()));

		// filtro compas
		innovaCmp	=	medCmp.minus(Hcmp.times(estado));
		Scmp		=	(Hcmp.times(P).times(Hcmp.transpose())).plus(Rcmp);
		Wcmp		=	(P.times(Hcmp.transpose())).times(Scmp.inverse());
		estado		=	estado.plus(Wcmp.times(innovaCmp));
		P			=	P.minus(Wcmp.times(Scmp).times(Wcmp.transpose()));

		vars[0]		= estado.get(0, 0);		// x
		vars[1]		= estado.get(3, 0);		// y
		vars[2]		= estado.get(6, 0);		// z
		vars[3]		= estado.get(2, 0);		// roll
		vars[4]		= estado.get(5, 0);		// pitch
		vars[5]		= estado.get(8, 0);		// yaw
		vars[6]		= estado.get(1, 0);		// vx
		vars[7]		= estado.get(4, 0);		// vy
		vars[8]		= estado.get(7, 0);		// vz

		kpos.set (estado.get(0, 0), estado.get(3, 0), estado.get(6, 0), estado.get(8, 0));
	}
	
	public double[] datalog ()
	{
		return vars;
	}
}