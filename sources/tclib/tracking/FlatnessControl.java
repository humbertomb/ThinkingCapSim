package tclib.tracking;

import tc.vrobot.models.TricycleDrive;
import devices.pos.Path;
import devices.pos.Position;
import wucore.utils.math.Angles;

public class FlatnessControl {
	
	// These are constant values which should be in the initialisation
	final protected double dthreshold = 0.15;	// Derivative threshold for avoiding singularities
	final protected double TcDx = 0.1; // parameter for config low pass filter
	final protected double TcDy = 0.1;
	
	final protected double K_P1 = 40.0;
	final protected double K_D1 = 3.0;
	final protected double K_P2 = 40.0;
	final protected double K_D2 = 3.0;
	
	// Intelligent PID
	final protected double alpha1 = 100.0;
	final protected double alpha2 = 100.0;
	
	// Velocity profile
	final private double SDmax = 0.3;
	final private double Tmax = 0.1;
	
	double SDmax2, SDmax3, SDmax4, SDmax5, SDmax6, SDmax8;
	double Tmax2, Tmax3, Tmax4;
	
	// Sampling period
	double				dt;			// Time control sampling
	
	// Robot model
	TricycleDrive		rmodel;
	double u;		// Velocity of tricycle vehicle
	double deltaf;	// Acting
	double vrf, wrf;
	
	// Config variables
	boolean positive_vel;	// for switching velocities
	
	// Variables for initialization
	double maxtime;	// Maximum time for the manouvres
	double dttime;	// Time table sampling
	
	// Variables for execution
	boolean initialised_low_pass;
	double last_x, last_y;		// for implementing low pass filter
	double xde_last, yde_last;
	double thetar_last;
	double v_last;
	double v1_last, v2_last;
	double v1_nominal_last, v2_nominal_last;
	double sdint_last;
	double deltaf_last;
	
	// Control related variables
	protected Position pos;	// Current robot location
	
	// Variables for flatness control
	double[] sr;
	// LookUp Tables
	double[] xx;
	double[] yy;
	double[] xx_d;
	double[] yy_d;
	double[] xx_dd;
	double[] yy_dd;
	// Runtime shapes
	double[] s;
	double[] sd;
	double[] time;
	
	// For testing
	double[] v;
	double[] phi;
	double[] phi_d;
	
	double[] aux;
	
	// Path to follow
	protected Path rpath;	// Path refined after the method
	
	public FlatnessControl (double dt, TricycleDrive rmodel) {
		// Sampling period
		this.dt = dt;
		
		// Robot model
		this.rmodel = rmodel;
		
		// Temporal locations
		pos = new Position ();
		
		reset();
	}
	
	public void reset() {
		// Control variables
		u = 0.0;
		deltaf = 0.0;
		vrf = 0.0;
		wrf = 0.0;
		
		initControlVariables();
		
		// Config variables
		positive_vel = false;
		
		// Time for the manouvres
		maxtime = 0.0;	
		dttime = 0.0;
		
		// Execution variables
		initialised_low_pass = false;
		v_last = 0.0;
		last_x = 0.0;
		last_y = 0.0;
		xde_last = 0.0;
		yde_last = 0.0;
		thetar_last = 0.0;
		v1_last = 0.0;
		v2_last = 0.0;
		v1_nominal_last = 0.0;
		v2_nominal_last = 0.0;
		
		sdint_last = 0.0;
		deltaf_last = 0.0;
		
		// Variables for flatness control
		sr		= null;
		// LookUp Tables
		xx		= null;
		yy		= null;
		xx_d	= null;
		yy_d	= null;
		xx_dd	= null;
		yy_dd	= null;
		// Runtime shapes
		s		= null;
		sd		= null;
		time	= null;
		// For testing
		v		= null;
		phi		= null;
		phi_d	= null;
		aux		= null;
		// Path to follow
		rpath	= null;
	}
	
	public void setPath(Path rpath) {
		this.rpath = rpath;
	}
	
	public Position getCurrentPosition (double current_time) {
		Position paux;
		
		double sint;
		double xxr, yyr;
		double xxrd, yyrd;
		double thetar;
		
		sint	= get1DInterpolation(s, time, current_time);
		xxr		= get1DInterpolation(xx, sr, sint);
		xxrd	= get1DInterpolation(xx_d, sr, sint);
		yyr		= get1DInterpolation(yy, sr, sint);
		yyrd	= get1DInterpolation(yy_d, sr, sint);
		thetar	= Math.atan2(yyrd, xxrd) + Math.PI;
		
		paux = new Position(xxr, yyr, thetar);
		
		return paux;
	}
	
	public double[] getLUTS()		{	return s; }
	public double[] getLUTSd()		{	return sd; }
	public double[] getLUTTime()	{	return time; }
	
	public double[] getLUTV()		{	return v; }
	public double[] getLUTPhi()		{	return phi; }
	public double[] getLUTPhid()	{	return phi_d; }
	
	public double getWheelAngle()		{ return deltaf; }
	public double getWheelVelocity()	{ return u/Math.cos(deltaf); }
	public double getVelocity()			{ return vrf; }
	public double getAngularVelocity()	{ return wrf; }
	
	private void initControlVariables() {
		SDmax2 = SDmax*SDmax;
		SDmax3 = SDmax*SDmax*SDmax;
		SDmax4 = SDmax*SDmax*SDmax*SDmax;
		SDmax5 = SDmax*SDmax*SDmax*SDmax*SDmax;
		SDmax6 = SDmax*SDmax*SDmax*SDmax*SDmax*SDmax;
		// SDmax7 = SDmax*SDmax*SDmax*SDmax*SDmax*SDmax*SDmax;
		SDmax8 = SDmax*SDmax*SDmax*SDmax*SDmax*SDmax*SDmax*SDmax;
		
		Tmax2 = Tmax*Tmax;
		Tmax3 = Tmax*Tmax*Tmax;
		Tmax4 = Tmax*Tmax*Tmax*Tmax;
	}
	
	public void init() {
		int endpath;
		endpath = rpath.num ();
		
		// LookUp Tables
		xx		= new double[endpath];
		yy		= new double[endpath];
		xx_d	= new double[endpath];
		yy_d	= new double[endpath];
		xx_dd	= new double[endpath];
		yy_dd	= new double[endpath];
		
		// Vector of distances
		sr		= new double[endpath];
		
		// For testing
		v		= new double[endpath];
		phi		= new double[endpath];
		phi_d	= new double[endpath];
		
		aux		= new double[endpath];
		
		int ipos;
		Position aux_position;
		for (ipos = 0; ipos < endpath; ++ipos)
		{
			aux_position = rpath.getPosition (ipos);
			xx[ipos] = aux_position.x();
			yy[ipos] = aux_position.y();
			//System.out.println(xx[ipos] + "   " + yy[ipos]);
		}
		
		// Vector of distances
		double sr_aux;
		sr[0] = 0.0;
		for (ipos = 1; ipos < endpath; ++ipos)
		{
			sr_aux = Math.sqrt(
				((xx[ipos]-xx[ipos-1])*(xx[ipos]-xx[ipos-1])) + ((yy[ipos]-yy[ipos-1])*(yy[ipos]-yy[ipos-1]))
			);
			sr[ipos] = sr[ipos-1] + sr_aux;
		}
		
		for (ipos = 0; ipos < xx_d.length - 1; ++ipos)
			xx_d[ipos] = (xx[ipos+1] - xx[ipos]) / (sr[ipos+1] - sr[ipos]);
		xx_d[xx_d.length - 1] = xx_d[xx_d.length - 2];
		
		for (ipos = 0; ipos < yy_d.length - 1; ++ipos)
			yy_d[ipos] = (yy[ipos+1] - yy[ipos]) / (sr[ipos+1] - sr[ipos]);
		yy_d[yy_d.length - 1] = yy_d[yy_d.length - 2];
		
		for (ipos = 0; ipos < xx_dd.length - 1; ++ipos)
			xx_dd[ipos] = (xx_d[ipos+1] - xx_d[ipos]) / (sr[ipos+1] - sr[ipos]);
		xx_dd[xx_dd.length - 1] = xx_dd[xx_dd.length - 2];
		
		for (ipos = 0; ipos < yy_dd.length - 1; ++ipos)
			yy_dd[ipos] = (yy_d[ipos+1] - yy_d[ipos]) / (sr[ipos+1] - sr[ipos]);
		yy_dd[yy_dd.length - 1] = yy_dd[yy_dd.length - 2];
		
		// For testing
		for (ipos = 0; ipos < v.length; ++ipos)
			v[ipos] = Math.sqrt( (xx_d[ipos]*xx_d[ipos]) + (yy_d[ipos]*yy_d[ipos]) );
		
		for (ipos = 0; ipos < phi.length; ++ipos)
			phi[ipos] = Angles.RTOD * Math.atan( ( rmodel.l*(xx_d[ipos]*yy_dd[ipos] - yy_d[ipos]*xx_dd[ipos]) ) / ( Math.pow((xx_d[ipos]*xx_d[ipos]) + (yy_d[ipos]*yy_d[ipos]), 3.0/2.0) ) );
		
		for (ipos = 0; ipos < phi_d.length - 1; ++ipos)
			phi_d[ipos] = (phi[ipos+1] - phi[ipos]) / (sr[ipos+1] - sr[ipos]);
		phi_d[phi_d.length - 1] = phi_d[phi_d.length - 2];
		
		for (ipos = 0; ipos < aux.length; ++ipos)
			aux[ipos] = ipos;
		
		// Timed Look-up tables
		double l;
		l = (double)sr[sr.length-1];
		
		int time_length;
		time_length = sr.length;
		
		maxtime = 3.0*SDmax/Tmax + (l-1.5*SDmax2/Tmax)/SDmax;
		
		dttime = maxtime / time_length;
		
		time = new double[time_length];
		for(int time_count = 0; time_count < time_length; ++time_count)
		{
			if (time_count == 0)
				time[time_count] = 0.0;
			else
				time[time_count] = time[time_count-1] + dttime;
		}
		
		//for(int index = 0; index < tmp_size; ++index)
		//	System.out.println("  time[" + index + "]: " + time[index]);
		
		double a,b,c,d,e;
		
		double cond_s3 = 3.0/2.0*SDmax/Tmax;
		double cond_s2 = cond_s3 + (l-3.0/2.0*SDmax2/Tmax)/SDmax;
		
		s = new double[time_length];
		sd = new double[time_length];
		for(int count = 0; count < time_length; ++count)
		{
			double tim = time[count];
			double tim2 = tim*tim;
			double tim3 = tim*tim*tim;
			double tim4 = tim*tim*tim*tim;
			
			// s
			if(tim < cond_s3) {
				a = -4.0/27.0/SDmax2*Tmax3;
				b = 4.0/9.0/SDmax*Tmax2;
				c = 0.0;
				d = 0.0;
				e = 0.0;
				
				s[count] = a*tim4 + b*tim3 + c*tim2 + d*tim + e;
			} else if (tim < cond_s2) {
				s[count] = (SDmax*(tim-cond_s3)) + ((3.0*SDmax2)/(4.0*Tmax));
			} else {
				a = 4.0/27.0/SDmax2*Tmax3;
				b = -4.0/27.0*Tmax2/SDmax3*(3*SDmax2+4*l*Tmax);
				c = 4.0/9.0*l*(3*SDmax2+2.0*l*Tmax)*Tmax2/SDmax4;
				d = -1.0/27.0*(3.0*SDmax2+2.0*l*Tmax)*(3.0*SDmax2+2.0*l*Tmax)/SDmax5*(-3*SDmax2+4*l*Tmax);
				e = 1.0/108.0/SDmax6*(-81.0*SDmax8+48.0*SDmax2*l*l*l*Tmax3+16.0*l*l*l*l*Tmax4)/Tmax;
				
				s[count] = a*tim4 + b*tim3 + c*tim2 + d*tim + e;
			}
			
			// s_dot
			if(tim < cond_s3) {
				a = -16.0/27.0/SDmax2*Tmax3;
				b = 12.0/9.0/SDmax*Tmax2;
				c = 0.0;
				d = 0.0;
				
				sd[count] = a*tim3 + b*tim2 + c*tim + d;
			} else if (tim < cond_s2) {
				sd[count] = SDmax;
			} else {
				a = 16.0/27.0/SDmax2*Tmax3;
				b = -4.0/9.0*Tmax2/SDmax3*(3.0*SDmax2+4.0*l*Tmax);
				c = 8.0/9.0*l*(3.0*SDmax2+2.0*l*Tmax)*Tmax2/SDmax4;
				d = -1.0/27.0*(3.0*SDmax2+2.0*l*Tmax)*(3.0*SDmax2+2.0*l*Tmax)/SDmax5*(-3.0*SDmax2+4*l*Tmax);
				
				sd[count] = a*tim3 + b*tim2 + c*tim + d;
			}
		}
		
		//System.out.println(" sd.length: " + sd.length + " l: " + l + " maxtime: " + maxtime);
		
	}
	
	private double get1DInterpolation(double[] var, double[] abc_val, double c_val)
	{
		int time_pos_d, time_pos_u;
		double time_d, time_u;
		double var_d, var_u;
		
		double dist;
		double dist_min = Double.MAX_VALUE;
		int closer = 0;
		
		if(abc_val == null)
			return 0;
		
		for(int index = 0; index < abc_val.length; index++)
		{
			dist = Math.abs(abc_val[index] - c_val);
			if (dist < dist_min)
			{
				dist_min = dist;
				closer = index;
			}
		}
		
		if(c_val < abc_val[closer])
			--closer;
		
		time_pos_d = closer;
		time_pos_u = time_pos_d + 1;
		
		if (time_pos_u == abc_val.length)
		{
			--time_pos_d;
			--time_pos_u;
		}
		
//System.out.println(" time_pos_u: " + time_pos_u);
//System.out.println(" abc_val[" + time_pos_u + "]: " + abc_val[time_pos_u] + " size: " + abc_val.length);
		
		time_d = abc_val[time_pos_d];
		time_u = abc_val[time_pos_u];
		var_d = var[time_pos_d];
		var_u = var[time_pos_u];
		
		double inter1d;
		
		inter1d = var_d + ((c_val-time_d)*(var_u-var_d))/(time_u-time_d);
		
		return (double) inter1d;
	}
	
	public boolean hasFinished(double abs_time) {
		if(abs_time < (maxtime - dt))
			return false;
		
		return true;
	}
	
	public double[] step(Position robot, double abs_time) {

		double sint, sdint;
		double xxr, xxrd, xxrdd;
		double yyr, yyrd, yyrdd;
		
		if(rpath == null || time == null || sr == null)
			return null;
		
		pos.set (robot);
		
		sint	= get1DInterpolation(s, time, abs_time);
		sdint	= get1DInterpolation(sd, time, abs_time);
		
		xxr		= get1DInterpolation(xx, sr, sint);
		xxrd	= get1DInterpolation(xx_d, sr, sint);
		xxrdd	= get1DInterpolation(xx_dd, sr, sint);
		
		yyr		= get1DInterpolation(yy, sr, sint);
		yyrd	= get1DInterpolation(yy_d, sr, sint);
		yyrdd	= get1DInterpolation(yy_dd, sr, sint);
		
		double thetar;
		thetar = Math.atan2(yyrd, xxrd);
		//thetar = Math.atan(yyrd/xxrd);
		
		// angle normalization
		while(thetar < 0.0)			thetar += 2*Math.PI;
		while(thetar > 2*Math.PI)	thetar -= 2*Math.PI;
		
		double v1, v2;
		double F1, F2;
		double v1_nominal, v2_nominal;
		
		// Loss pass filter for estimating the velocity
		double v;
		
		double xde, yde;
		double xde_aux, yde_aux;
		double a1x, a2x;
		double a1y, a2y;
		if(initialised_low_pass)
		{
			a1x = dt+2*TcDx;
			a2x = dt-2*TcDx;
			a1y = dt+2*TcDy;
			a2y = dt-2*TcDy;
			
			xde_aux = (2.0*(pos.x() - last_x) - a2x*xde_last)/a1x;
			//xde_aux = (pos.x() - last_x)/dt;
			yde_aux = (2.0*(pos.y() - last_y) - a2y*yde_last)/a1y;
			
			//System.out.println(" a1x: " + a1x + " a2x: " + a2x);
			//System.out.println(" a1y: " + a1y + " a2y: " + a2y);
			
			//System.out.println(" num1: " + (pos.x()-last_x) + " num2: " + (a2x*xde_last) + " num: " + (2.0*(pos.x() - last_x) - a2x*));
			//System.out.println(" xde_aux: " + xde_aux + " xde_last: " + xde_last);
			
			if(sdint < dthreshold)
			{
				xde = xxrd;
			} else {
				xde = xde_aux / sdint;
			}
			
			if(sdint < dthreshold)
			{
				yde = yyrd;
			} else {
				yde = yde_aux / sdint;
			}
			
			v1_nominal = xxrdd + K_D1*(xxrd - xde) + K_P1*(xxr-pos.x());
			F1 = xde - alpha1*v1_last;
			//F1 = xde - alpha1*v1_nominal_last;
			v1 = (F1 - xxrd)/alpha1 + v1_nominal;
			
			v2_nominal = yyrdd + K_D2*(yyrd - yde) + K_P2*(yyr-pos.y());
			F2 = yde - alpha2*v2_last;
			//F2 = yde - alpha2*v2_nominal_last;
			v2 = (F2 - yyrd)/alpha2 + v2_nominal;
			
			// Remove Intelligent PID
			 v1 = v1_nominal;
			 v2 = v2_nominal;
			
			v = v_last + dt*sdint*(Math.cos(thetar)*v1 + Math.sin(thetar)*v2);
			//v = v_last + dt*sdint_last*(Math.cos(thetar_last)*v1_last + Math.sin(thetar_last)*v2_last);
			
		} else {
			initialised_low_pass = true;
			
			//xde = xx_d[0];
			xde = xxrd;
			xde_aux = xde;
			//yde = yy_d[0];
			yde = yyrd;
			yde_aux = yde;
			
			v = Math.sqrt(xx_d[0]*xx_d[0] + yy_d[0]*yy_d[0]);
			
			//v1 = xxrdd + K_D1*(xxrd - xde) + K_P1*(xxr-pos.x());
			//v2 = yyrdd + K_D2*(yyrd - yde) + K_P2*(yyr-pos.y());
			
			v1_nominal = 0.0;
			v2_nominal = 0.0;
			
			v1 = 0.0;
			v2 = 0.0;
		}
		
		//System.out.println(" xde: " + xde + " xde_last: " + xde_last);
		
		// System.out.println("  xde: " + xde + " yde: " + yde + " v: " + v);
		
		// if negative velocities
		if(positive_vel)
		{
			u = v*sdint;	// Velocity of tricycle vehicle
		} else {
			u = -v*sdint;	// Velocity of tricycle vehicle
		}
		
		if (v > 0.0001)
		{
			deltaf = Math.atan((rmodel.l*(Math.cos(thetar)*v2 - Math.sin(thetar)*v1))/(v*v));
		} else {
			deltaf = deltaf_last;
		}
		
		//System.out.println(" Vel: u: " + u + " phi: " + Math.toDegrees(deltaf));
		
		last_x = pos.x();
		last_y = pos.y();
		xde_last = xde_aux;
		yde_last = yde_aux;
		thetar_last = thetar;
		v_last = v;
		v1_last = v1;
		v2_last = v2;
		v1_nominal_last = v1_nominal;
		v2_nominal_last = v2_nominal;
		sdint_last = sdint;
		deltaf_last = deltaf;
		
		//System.out.print(" deltaf before: " + Math.toDegrees(deltaf));
		
		// Delta normalisation
		if(deltaf > Math.toRadians(60))		deltaf = Math.toRadians(60);
		if(deltaf < Math.toRadians(-60))	deltaf = Math.toRadians(-60);
		
		//System.out.println(" deltaf after: " + Math.toDegrees(deltaf));
		
		// Compute direct kynematics
		vrf = u;
		wrf = Math.abs(u)*Math.tan(deltaf)/rmodel.l;
		
		double vmax, rmax;
		// Check for maximum planned velocities 
		vmax	= rmodel.Vmax;	// [m/s]
		rmax	= rmodel.Rmax;	// [rad/s]
		
		// Apply speed limits
		wrf		= Math.max (Math.min (wrf, rmax), -rmax); // [rad/s]
		vrf		= Math.max (Math.min (vrf, vmax), -vmax); // [m/s]
		
		double[] velocities;
		velocities = new double[2];
		
		velocities[0] = vrf;
		velocities[1] = wrf;
		
		return velocities;
	}
}
