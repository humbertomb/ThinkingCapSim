/*
 * (c) 2003-2004 Rafael Toledo Moreo
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.navigation.localisation.outdoor;

import wucore.utils.math.jama.*;

public class Compensation
{
	public static double roll_n;		// _n means navigation-frame: b-frame pose compensated
	public static double pitch_n;		// (Lned) Local east-noth-up
	public static double yaw_n;
	public static double rollrate_n;		// _n means navigation-frame: b-frame pose compensated
	public static double pitchrate_n;		// (Lned) Local east-noth-up
	public static double yawrate_n;
	public static double ax_n;
	public static double ay_n;
	public static double az_n;
	public static double xgps;
	public static double ygps;
	public static double zgps;
	public static double xvgps;
	public static double yvgps;
	public static double zvgps;
	
	public static void convert (Variables variables, Matrix GPSLoc, Matrix INSLoc)
	{
		// frames definition
		// Orientation Angles
		double roll_b;					// _b means body-frame:
		double pitch_b;					// (Bfrd) Body forward-left-up of the INS.
		double yaw_b;
		// Orientation Rates (Gyros)
		double roll_r;
		double pitch_r;
		double yaw_r;
		// Accelerations
		double ax_b;
		double ay_b;
		double az_b;
		// INS pose Correction angles
 		double INSErrorYaw;
		double INSErrorRoll;
		double INSErrorPitch;
		
		// Matrix definition
		Matrix Bfrd2Lned = new Matrix(3,3); // Rotation matrix to INS body frame
		Matrix RErr		 = new Matrix(3,3); // Rotation matrix to compensate the positioning error
		Matrix R		 = new Matrix(3,3); // Total rotation matrix
		Matrix dR 		 = new Matrix(3,3); // Second derivate of the Rotation Matrix
		Matrix ddR 		 = new Matrix(3,3); // Second derivate of the Rotation Matrix
		Matrix a_b 		 = new Matrix(3,1);
		Matrix a_n		 = new Matrix(3,1);
		Matrix Pos 		 = new Matrix(3,1);
		Matrix Vel 		 = new Matrix(3,1);
		Matrix GPSPos 	 = new Matrix(3,1);	
		Matrix GPSVel 	 = new Matrix(3,1);	
		Matrix INSLocPos = new Matrix(3,1);
		Matrix MTemp	 = new Matrix(3,1);
		Matrix MTemp2	 = new Matrix(3,1);

		// identification
		INSErrorRoll  = INSLoc.get(3,0);
		INSErrorPitch = INSLoc.get(4,0);
		INSErrorYaw   = INSLoc.get(5,0);

		GPSVel.set(0,0,variables.xvgps);
		GPSVel.set(1,0,variables.yvgps);
		GPSVel.set(2,0,variables.zvgps);
		
		// Falta compensar el offset, se corrigen despues
		roll_b 	= variables.rollins ; 
		pitch_b = variables.pitchins; 
		yaw_b 	= variables.yawins;
		
		// TODO: Falta compensar el offset
		roll_r  = variables.rollrateins;
		pitch_r  = variables.pitchrateins;
		yaw_r  = variables.yawrateins;
		
		INSLocPos.set(0,0,INSLoc.get(0,0));
		INSLocPos.set(1,0,INSLoc.get(1,0));
		INSLocPos.set(2,0,INSLoc.get(2,0));

		GPSPos.set(0,0,variables.xgps);
		GPSPos.set(1,0,variables.ygps);
		GPSPos.set(2,0,variables.zgps);	

		ax_b 	= variables.axins;
		ay_b 	= variables.ayins;
		az_b	= variables.azins;
		a_b.set(0,0, ax_b);
		a_b.set(1,0, ay_b);
		a_b.set(2,0, az_b);
		
		// Tranformation Matrix	
		RErr.set(0,0,  Math.cos(INSErrorPitch)*Math.cos(INSErrorYaw) );
		RErr.set(0,1, (Math.sin(INSErrorRoll)*Math.sin(INSErrorPitch)*Math.cos(INSErrorYaw))-(Math.cos(INSErrorRoll)*Math.sin(INSErrorYaw)) );
		RErr.set(0,2, (Math.cos(INSErrorRoll)*Math.sin(INSErrorPitch)*Math.cos(INSErrorYaw))+(Math.sin(INSErrorRoll)*Math.sin(INSErrorYaw)) );
		RErr.set(1,0,  Math.cos(INSErrorPitch)*Math.sin(INSErrorYaw) );
		RErr.set(1,1, (Math.sin(INSErrorRoll)*Math.sin(INSErrorPitch)*Math.sin(INSErrorYaw))+(Math.cos(INSErrorRoll)*Math.cos(INSErrorYaw)) );
		RErr.set(1,2, (Math.cos(INSErrorRoll)*Math.sin(INSErrorPitch)*Math.sin(INSErrorYaw))-(Math.sin(INSErrorRoll)*Math.cos(INSErrorYaw)) );
		RErr.set(2,0, -Math.sin(INSErrorPitch) );
		RErr.set(2,1,  Math.sin(INSErrorRoll)*Math.cos(INSErrorPitch) );
		RErr.set(2,2,  Math.cos(INSErrorRoll)*Math.cos(INSErrorPitch) );
		
		Bfrd2Lned.set(0,0,  Math.cos(pitch_b)*Math.cos(yaw_b) );
		Bfrd2Lned.set(0,1, (Math.sin(roll_b)*Math.sin(pitch_b)*Math.cos(yaw_b))-(Math.cos(roll_b)*Math.sin(yaw_b)) );
		Bfrd2Lned.set(0,2, (Math.cos(roll_b)*Math.sin(pitch_b)*Math.cos(yaw_b))+(Math.sin(roll_b)*Math.sin(yaw_b)) );
		Bfrd2Lned.set(1,0,  Math.cos(pitch_b)*Math.sin(yaw_b) );
		Bfrd2Lned.set(1,1, (Math.sin(roll_b)*Math.sin(pitch_b)*Math.sin(yaw_b))+(Math.cos(roll_b)*Math.cos(yaw_b)) );
		Bfrd2Lned.set(1,2, (Math.cos(roll_b)*Math.sin(pitch_b)*Math.sin(yaw_b))-(Math.sin(roll_b)*Math.cos(yaw_b)) );
		Bfrd2Lned.set(2,0, -Math.sin(pitch_b) );
		Bfrd2Lned.set(2,1,  Math.sin(roll_b)*Math.cos(pitch_b) );
		Bfrd2Lned.set(2,2,  Math.cos(roll_b)*Math.cos(pitch_b) );
		
		R = RErr.times(Bfrd2Lned);

		// Compensando el offset
		pitch_b = Math.asin(-R.get(2,0)); 
		roll_b 	= Math.atan2(R.get(2,1),R.get(2,2));
		yaw_b 	= Math.atan2(R.get(1,0),R.get(0,0)); 
		
		dR.set(0,0,-pitch_r * Math.sin(pitch_b)* Math.cos(yaw_b) - yaw_r * Math.cos(pitch_b) * Math.sin(yaw_b));
		dR.set(0,1, roll_r  * Math.cos(roll_b)*  Math.sin(pitch_b) * Math.cos(yaw_b) + Math.sin(roll_b) * ( pitch_r * Math.cos(pitch_b) * Math.cos(yaw_b) - yaw_r * Math.sin(pitch_b) * Math.sin(yaw_b) ) + roll_r * Math.sin(roll_b) * Math.sin(yaw_b) - yaw_r * Math.cos(roll_b) * Math.cos(yaw_b));
		dR.set(0,2, roll_r  * Math.sin(roll_b)*  Math.sin(pitch_b) * Math.cos(yaw_b) + Math.cos(roll_b) * ( pitch_r * Math.cos(pitch_b) * Math.cos(yaw_b) - yaw_r * Math.sin(pitch_b) * Math.sin(yaw_b) ) + roll_r * Math.cos(roll_b) * Math.sin(yaw_b) + yaw_r * Math.sin(roll_b) * Math.cos(yaw_b));

		dR.set(1,0,-pitch_r * Math.sin(pitch_b) * Math.sin(yaw_b) + yaw_r * Math.cos(pitch_b) * Math.cos(yaw_b));
		dR.set(1,1, roll_r  * Math.cos(roll_b) * Math.sin(pitch_b) * Math.sin(yaw_b) + Math.sin(roll_b) * ( pitch_r * Math.cos(pitch_b) * Math.sin(yaw_b) + yaw_r * Math.sin(pitch_b) * Math.cos(yaw_b) ) - roll_r * Math.sin(roll_b) * Math.cos(yaw_b) - yaw_r * Math.cos(roll_b) * Math.sin(yaw_b));
		dR.set(1,2, roll_r  * Math.sin(roll_b) * Math.sin(pitch_b) * Math.sin(yaw_b) + Math.cos(roll_b) * ( pitch_r * Math.cos(pitch_b) * Math.sin(yaw_b) + yaw_r * Math.sin(pitch_b) * Math.cos(yaw_b) ) - roll_r * Math.cos(roll_b) * Math.cos(yaw_b) + yaw_r * Math.cos(roll_b) * Math.cos(yaw_b));
		
		dR.set(2,0,-pitch_r * Math.cos(pitch_b));
		dR.set(2,1,roll_r * Math.cos(roll_b) * Math.cos(pitch_b) - pitch_r * Math.sin(roll_b) * Math.sin(pitch_b));
		dR.set(2,2,- roll_r * Math.sin(roll_b) * Math.cos(pitch_b) - pitch_r * Math.cos(roll_b) * Math.sin(pitch_b));
		



		// El roll y el pitch estan confundidos
		ddR.set(0,0, -(pitch_r*pitch_r+yaw_r*yaw_r)*Math.cos(pitch_b)*Math.cos(yaw_b) + 2*pitch_r*yaw_r*Math.sin(pitch_b)*Math.sin(yaw_b));
		ddR.set(0,1, - Math.sin(pitch_b)*Math.sin(roll_b)*Math.cos(yaw_b)*(pitch_r*pitch_r+roll_r*roll_r+yaw_r*yaw_r) 
				+ 2*pitch_r*roll_r*Math.cos(pitch_b)*Math.cos(roll_b)*Math.cos(yaw_b)
				- 2*roll_r*yaw_r*Math.sin(pitch_b)*Math.cos(roll_b)*Math.sin(yaw_b)
				- 2*pitch_r*yaw_r*Math.cos(pitch_b)*Math.sin(roll_b)*Math.sin(yaw_b)
				+ Math.cos(roll_b)*Math.sin(yaw_b)*(roll_r*roll_r+yaw_r*yaw_r) + 2*roll_r*yaw_r*Math.sin(roll_b)*Math.cos(yaw_b));
		ddR.set(0,2, Math.sin(pitch_b)*Math.cos(roll_b)*Math.cos(yaw_b)*(-pitch_r*pitch_r+roll_r*roll_r-yaw_r*yaw_r) 
				- 2*pitch_r*yaw_r*Math.cos(pitch_b)*Math.cos(roll_b)*Math.sin(yaw_b)
				- Math.sin(roll_b)*Math.sin(yaw_b)*(roll_r*roll_r*yaw_r*yaw_r) + 2*roll_r*yaw_r*Math.cos(roll_b)*Math.cos(yaw_b));
		ddR.set(1,0, -(pitch_r*pitch_r+yaw_r*yaw_r)*Math.cos(pitch_b)*Math.sin(yaw_b) - 2*pitch_r*yaw_r*Math.sin(pitch_b)*Math.cos(yaw_b));
		ddR.set(1,1, - Math.sin(pitch_b)*Math.sin(roll_b)*Math.sin(yaw_b)*(pitch_r*pitch_r+roll_r*roll_r+yaw_r*yaw_r) 
				+ 2*pitch_r*roll_r*Math.cos(pitch_b)*Math.cos(roll_b)*Math.sin(yaw_b)
				+ 2*roll_r*yaw_r*Math.sin(pitch_b)*Math.cos(roll_b)*Math.cos(yaw_b)
				+ 2*pitch_r*yaw_r*Math.cos(pitch_b)*Math.sin(roll_b)*Math.cos(yaw_b)
				- Math.cos(roll_b)*Math.cos(yaw_b)*(roll_r*roll_r+yaw_r*yaw_r) + 2*roll_r*yaw_r*Math.sin(roll_b)*Math.sin(yaw_b));
		ddR.set(1,2, Math.sin(pitch_b)*Math.cos(roll_b)*Math.sin(yaw_b)*(-pitch_r*pitch_r+roll_r*roll_r-yaw_r*yaw_r) 
				+ 2*pitch_r*yaw_r*Math.cos(pitch_b)*Math.cos(roll_b)*Math.cos(yaw_b)
				+ Math.sin(roll_b)*Math.cos(yaw_b)*(roll_r*roll_r*yaw_r*yaw_r) + 2*roll_r*yaw_r*Math.cos(roll_b)*Math.sin(yaw_b));
		ddR.set(2,0, pitch_r*pitch_r*Math.sin(pitch_b));
		ddR.set(2,1, -(pitch_r*pitch_r+roll_r*roll_r)*Math.sin(roll_b)*Math.cos(pitch_b) - 2*pitch_r*roll_r*Math.cos(roll_b)*Math.sin(pitch_b));
		ddR.set(2,2, -(pitch_r*pitch_r+roll_r*roll_r)*Math.cos(roll_b)*Math.cos(pitch_b) + 2*pitch_r*roll_r*Math.sin(roll_b)*Math.sin(pitch_b));
		
		// Acceleration
		a_n  = R.times(a_b).minus(ddR.times(INSLocPos));
		
		// Velocity
		MTemp = dR.times(GPSLoc);
		MTemp2.set(0,0,-MTemp.get(1,0));
		MTemp2.set(1,0,MTemp.get(0,0));
		MTemp2.set(2,0,MTemp.get(2,0));
		Vel = GPSVel.minus(MTemp2);
		
		// Position
		MTemp = R.times(GPSLoc);
		MTemp2.set(0,0,-MTemp.get(1,0));
		MTemp2.set(1,0,MTemp.get(0,0));
		MTemp2.set(2,0,MTemp.get(2,0));
		Pos = GPSPos.minus(MTemp2);

		// Output
		xgps = Pos.get(0,0);
		ygps = Pos.get(1,0);
		zgps = Pos.get(2,0);
		xvgps = Vel.get(0,0);
		yvgps = Vel.get(1,0);
		zvgps = Vel.get(2,0);
		ax_n = -a_n.get(1,0); // westing  is the y axis of the INS
		ay_n = a_n.get(0,0);  // northing is the x axis of the INS
		az_n = a_n.get(2,0);  // altitude is the z axis of the INS
		roll_n 	= -pitch_b;
		pitch_n = roll_b;
		yaw_n 	= yaw_b;
		rollrate_n 	= -pitch_r;
		pitchrate_n = roll_r;
		yawrate_n 	= yaw_r;
				
	}
	
	private Compensation ()
	{

	}
}