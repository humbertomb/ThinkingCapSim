package tclib.tracking;

import tc.vrobot.models.*;

import tclib.utils.fuzzy.*;

import devices.pos.*;
import wucore.utils.math.*;

public class FuzzyControl {
	
	// Configuration parameters
	protected double looka_dst	= 0.05;	// Current look-ahead distance (m)
	
	// Sampling period
	double			dt;			// Time control sampling
	
	// Maneouvre parameters
	protected double	tol_pos	= 0.05;
	
	// Robot model
	TricycleDrive	rmodel;
	double del, vm;				// Wheel's values
	
	// Control variables
	protected double vr;		// Desired output linear spped (m/s)
	protected double wr;		// Desired output rotational speed (rad/s)
	
	// Look-ahead related variables
	protected Position	pos;		// Current robot location
	protected Position	looka;		// Current look-ahead point
	protected double	path_dst;	// Current robot to desired path distance (m)
	
	// Path to follow
	protected Path		rpath;		// Path refined after the method
	
	// Follow_path behaviour parameters
	static public final double	FOLLOW_STP		= 3.5;		// Distance to begin stopping the vehicle
	static public final double	FOLLOW_LCS		= 50.0;		// Control cycles to stop the vehicle	
	static public final double	FOLLOW_DANGER	= 10.0;		// Range for collision danger (m)
	
	// Manouver behaviour parameters
	static public final double	MANOUVER_STP	= 0.5;		// Distance to stop the vehicle
	static public final double	MANOUVER_LCS	= 10.0;		// Control cycles to stop the vehicle 
	
	// Additional constants
	static public final int		MAX_INPUTS		= 5;			// Maximum number of input variables
	
	// Fuzzy Logic controller
	protected MIMOrules	p_flc;		// FLC rules for path following controller
	protected MIMOrules	m_flc;		// FLC rules for manouvering controller
	
	// ---------------------------------------------------------------
	// PATH FOLLOWING
	// ---------------------------------------------------------------
	
	// Fuzzy sets for heading-error					[INPUT]
	protected Trapezoid	E_P_NLARGE;
	protected Trapezoid	E_P_NMEDIUM;
	protected Trapezoid	E_P_NSMALL;
	protected Trapezoid	E_P_NZERO;
	protected Trapezoid	E_P_ZERO;
	protected Trapezoid	E_P_PZERO;
	protected Trapezoid	E_P_PSMALL;
	protected Trapezoid	E_P_PMEDIUM;
	protected Trapezoid	E_P_PLARGE;
	
	// Fuzzy sets for angular velocity control  	[OUTPUT]
	protected Crisp T_P_NLARGE;
	protected Crisp	T_P_NMEDIUM;
	protected Crisp	T_P_NSMALL;
	protected Crisp	T_P_NZERO;
	protected Crisp	T_P_ZERO;
	protected Crisp	T_P_PZERO;
	protected Crisp	T_P_PSMALL;
	protected Crisp	T_P_PMEDIUM;
	protected Crisp	T_P_PLARGE;
	
	// Fuzzy sets for linear velocity control 		[OUTPUT]
	protected Crisp	S_P_PZERO;
	protected Crisp	S_P_PSMALL;
	protected Crisp	S_P_PLARGE;
	protected Crisp	S_P_PFULL;
	
	// ---------------------------------------------------------------
	// MANOUVERING
	// ---------------------------------------------------------------
	
	// Fuzzy sets for heading-error 				[INPUT]
	protected Trapezoid	E_M_NLARGE;
	protected Trapezoid	E_M_NMEDIUM;
	protected Trapezoid	E_M_NSMALL;
	protected Trapezoid	E_M_ZERO;
	protected Trapezoid	E_M_PSMALL;
	protected Trapezoid	E_M_PMEDIUM;
	protected Trapezoid	E_M_PLARGE;

	// Fuzzy sets for angular velocity control 		[OUTPUT]
	protected Crisp	T_M_NLARGE;
	protected Crisp	T_M_NMEDIUM;
	protected Crisp	T_M_NSMALL;
	protected Crisp	T_M_ZERO;
	protected Crisp	T_M_PSMALL;
	protected Crisp	T_M_PMEDIUM;
	protected Crisp	T_M_PLARGE;
	
	// Fuzzy sets for linear velocity control 						[OUTPUT]
	protected Crisp	S_M_NFULL;
	protected Crisp	S_M_NMEDIUM;
	protected Crisp	S_M_NSMALL;
	protected Crisp	S_M_NZERO;
	
	public FuzzyControl(double dt, TricycleDrive rmodel) {
		
		// Sampling period
		this.dt = dt;
		
		// Robot model
		this.rmodel = rmodel;
		
		// Current robot location
		pos = new Position ();
		
		// look-ahead variables
		looka = new Position ();
		path_dst = 0.0;
		
		// Control variables
		vr = 0.0;
		wr = 0.0;
		
		// Wheel's parameters
		del = 0.0;
		vm = 0.0;
		
		// Path to follow
		rpath = null;
		
		// Initialise fuzzy rules
		initialise_rules ();
	}
	
	public void setLookaDistance(double looka_dst) {
		this.looka_dst = looka_dst;
	}
	
	public void setPath(Path rpath) {
		this.rpath = rpath;
	}
	
	public void setTolPosition(double tol_pos) {
		this.tol_pos = tol_pos;
	}
	
	public double getWheelAngle()		{ return del; }
	public double getWheelVelocity()	{ return vm; }
	public double getVelocity()			{ return vr; }
	public double getAngularVelocity()	{ return wr; }
	
	protected void initialise_rules ()
	{
		// ---------------------
		// PATH FOLLOWING
		// ---------------------
		
		// Define fuzzy sets for heading-error (deg)
		E_P_NLARGE		= new Trapezoid (-180.0, -180.0, -45.0, -35.0);
		E_P_NMEDIUM		= new Trapezoid (-45.0, -35.0, -25.0, -15.0);
		E_P_NSMALL		= new Trapezoid (-25.0, -15.0, -10.0, -5.0);
		E_P_NZERO		= new Trapezoid (-10.0, -5.0, -3.0, 0.0);
		E_P_ZERO		= new Trapezoid (-3.0, 0.0, 0.0, 3.0);
		E_P_PZERO		= new Trapezoid (0.0, 3.0, 5.0, 10.0);
		E_P_PSMALL		= new Trapezoid (5.0, 10.0, 15.0, 25.0);
		E_P_PMEDIUM		= new Trapezoid (15.0, 25.0, 35.0, 45.0);
		E_P_PLARGE		= new Trapezoid (35.0, 45.0, 180.0, 180.0);
		
		//Con estas reglas oscila menos la direccion pero con poca precision cuando llega al goal
		// Define fuzzy sets for heading-error (deg)
		//E_P_NLARGE		= new Trapezoid (-180.0, -180.0, -45.0, -35.0);
		//E_P_NMEDIUM		= new Trapezoid (-45.0, -35.0, -28.0, -22.0);
		//E_P_NSMALL		= new Trapezoid (-28.0, -22.0, -17.0, -12.0);
		//E_P_NZERO		= new Trapezoid (-17.0, -12.0, -7.0, 0.0);
		//E_P_ZERO			= new Trapezoid (-7.0, 0.0, 0.0, 7.0);
		//E_P_PZERO		= new Trapezoid (0.0, 7.0, 12.0, 17.0);
		//E_P_PSMALL		= new Trapezoid (12.0, 17.0, 22.0, 28.0);
		//E_P_PMEDIUM		= new Trapezoid (22.0, 28.0, 35.0, 45.0);
		//E_P_PLARGE		= new Trapezoid (35.0, 45.0, 180.0, 180.0);
		
		// Define fuzzy sets for angular velocity control (deg/s)
		T_P_NLARGE		= new Crisp (-60.0);
		T_P_NMEDIUM		= new Crisp (-20.0);
		T_P_NSMALL		= new Crisp (-10.0);
		T_P_NZERO		= new Crisp (-5.0);
		T_P_ZERO		= new Crisp (0.0);
		T_P_PZERO		= new Crisp (5.0);
		T_P_PSMALL		= new Crisp (10.0);
		T_P_PMEDIUM		= new Crisp (20.0);
		T_P_PLARGE		= new Crisp (60.0);
		
		// Define fuzzy sets for angular velocity control (deg/s)
		//T_P_NLARGE		= new Crisp (-30.0);
		//T_P_NMEDIUM		= new Crisp (-20.0);
		//T_P_NSMALL		= new Crisp (-7.0);
		//T_P_NZERO		= new Crisp (-3.0);
		//T_P_ZERO		= new Crisp (0.0);
		//T_P_PZERO		= new Crisp (3.0);
		//T_P_PSMALL		= new Crisp (7.0);
		//T_P_PMEDIUM		= new Crisp (20.0);
		//T_P_PLARGE		= new Crisp (30.0);
		
		// Define fuzzy sets for linear velocity control (m/s)
		S_P_PZERO		= new Crisp (0.35);
		S_P_PSMALL		= new Crisp (0.5);
		S_P_PLARGE		= new Crisp (0.8);
		S_P_PFULL		= new Crisp (1.0);
		
		// Define fuzzy sets for linear velocity control (m/s)
		//S_P_PZERO		= new Crisp (0.015);
		//S_P_PSMALL		= new Crisp (0.10);
		//S_P_PLARGE		= new Crisp (0.70);
		//S_P_PFULL		= new Crisp (1.25);

		// Fuzzy rules for heading control
		p_flc			= new MIMOrules (1, 2, 9);

		p_flc.input_sets (0, E_P_NLARGE);	p_flc.output_sets (0, T_P_NLARGE, S_P_PZERO);
		p_flc.input_sets (1, E_P_NMEDIUM);	p_flc.output_sets (1, T_P_NMEDIUM, S_P_PSMALL);
		p_flc.input_sets (2, E_P_NSMALL);	p_flc.output_sets (2, T_P_NSMALL, S_P_PLARGE);
		p_flc.input_sets (3, E_P_NZERO);	p_flc.output_sets (3, T_P_NZERO, S_P_PFULL);
		p_flc.input_sets (4, E_P_ZERO);		p_flc.output_sets (4, T_P_ZERO, S_P_PFULL);
		p_flc.input_sets (5, E_P_PZERO);	p_flc.output_sets (5, T_P_PZERO, S_P_PFULL);
		p_flc.input_sets (6, E_P_PSMALL);	p_flc.output_sets (6, T_P_PSMALL, S_P_PLARGE);
		p_flc.input_sets (7, E_P_PMEDIUM);	p_flc.output_sets (7, T_P_PMEDIUM, S_P_PSMALL);
		p_flc.input_sets (8, E_P_PLARGE);	p_flc.output_sets (8, T_P_PLARGE, S_P_PZERO);
		
		// ---------------------
		// MANOUVERING
		// ---------------------

		// Define fuzzy sets for heading-error (deg)
		E_M_NLARGE		= new Trapezoid (-180.0, -180.0, -35.0, -25.0);
		E_M_NMEDIUM		= new Trapezoid (-35.0, -25.0, -15.0, -10.0);
		E_M_NSMALL		= new Trapezoid (-15.0, -10.0, -5.0, 0.0);
		E_M_ZERO		= new Trapezoid (-5.0, 0.0, 0.0, 5.0);
		E_M_PSMALL		= new Trapezoid (0.0, 5.0, 10.0, 15.0);
		E_M_PMEDIUM		= new Trapezoid (10.0, 15.0, 25.0, 35.0);
		E_M_PLARGE		= new Trapezoid (25.0, 35.0, 180.0, 180.0);

		// Define fuzzy sets for angular velocity control (deg/s)
		T_M_NLARGE		= new Crisp (-75.0);
		T_M_NMEDIUM		= new Crisp (-10.0);
		T_M_NSMALL		= new Crisp (-7.0);
		T_M_ZERO		= new Crisp (0.0);
		T_M_PSMALL		= new Crisp (7.0);
		T_M_PMEDIUM		= new Crisp (10.0);
		T_M_PLARGE		= new Crisp (75.0);

		// Define fuzzy sets for linear velocity control (m/s)
		//S_M_NFULL		= new Crisp (-0.30);
		//S_M_NMEDIUM		= new Crisp (-0.15);
		//S_M_NSMALL		= new Crisp (-0.075);	// -0.05
		//S_M_NZERO		= new Crisp (-0.050);	// -0.015
		
		//S_M_NFULL		= new Crisp (-0.5);
		//S_M_NMEDIUM		= new Crisp (-0.40);
		//S_M_NSMALL		= new Crisp (-0.25);	// -0.05
		//S_M_NZERO		= new Crisp (-0.15);	// -0.015
		
		S_M_NFULL		= new Crisp (-0.5);
		S_M_NMEDIUM		= new Crisp (-0.35);
		S_M_NSMALL		= new Crisp (-0.13);	// -0.05
		S_M_NZERO		= new Crisp (-0.07);	// -0.015
		
		// Fuzzy rules for heading control
		m_flc			= new MIMOrules (1, 2, 7);

		m_flc.input_sets (0, E_M_NLARGE);	m_flc.output_sets (0, T_M_NLARGE, S_M_NZERO);
		m_flc.input_sets (1, E_M_NMEDIUM);	m_flc.output_sets (1, T_M_NMEDIUM, S_M_NSMALL);
		m_flc.input_sets (2, E_M_NSMALL);	m_flc.output_sets (2, T_M_NSMALL, S_M_NMEDIUM);
		m_flc.input_sets (3, E_M_ZERO);		m_flc.output_sets (3, T_M_ZERO, S_M_NFULL);
		m_flc.input_sets (4, E_M_PSMALL);	m_flc.output_sets (4, T_M_PSMALL, S_M_NMEDIUM);
		m_flc.input_sets (5, E_M_PMEDIUM);	m_flc.output_sets (5, T_M_PMEDIUM, S_M_NSMALL);
		m_flc.input_sets (6, E_M_PLARGE);	m_flc.output_sets (6, T_M_PLARGE, S_M_NZERO);	
	}
	
	public double[] pathFollowingFuzzyController (double delta)
	{
		double[] output;
		output = new double[2];
		
		double[] inputs;
		inputs = new double[MAX_INPUTS];
		
		inputs[0]	= delta * Angles.RTOD;				// [deg]
		p_flc.MOMinference (inputs);
		output[0]	= p_flc.ovars[0] * Angles.DTOR;		// [rad/s]
		output[1]	= p_flc.ovars[1];					// [m/s]
		
		return output;
	}
	
	public double[] maneuverStep (Position robot, Position init, Position goal)
	{
		double[] inputs;
		inputs = new double[MAX_INPUTS];
		
		pos.set (robot);
		looka.set (pos);
		looka.valid (false);
		
		rpath.check_lookahead (pos, looka_dst);
		if (rpath.lookahead () != null)
		{
			path_dst = rpath.distance ();
			looka.set (rpath.lookahead ());
			looka.valid (true);
		}
		
		double	dx, dy;
		double	delta;
		double	dist, heading;	
		double	vmax, rmax;
		
		// Compute heading to look-ahead point
		dx		= looka.x () - pos.x ();
		dy		= looka.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);				// [m]
		heading	= Math.atan2 (dy, dx);							// [rad]
		delta	= Angles.radnorm_180 (heading - pos.alpha ());	// [rad]
		delta	= Angles.radnorm_180 (Math.PI + delta);
		
		// Check wether the robot is close to the goal
		//if (dist < MANOUVER_STP)
		//	delta = delta * (dist * dist);
		
		// Apply the fuzzy logic controller (angular and linear velocities)
		inputs[0]	= delta * Angles.RTOD;		// [deg]
		m_flc.MOMinference (inputs);
		wr 		= m_flc.ovars[0] * Angles.DTOR;	// [rad/s]
		vr		= m_flc.ovars[1];				// [m/s]
		
		// Inverse kinematics
		del		= Math.atan((wr*rmodel.l) / vr);
		vm		= vr / Math.cos(del);
		
		// Set up an upper boundary for the linear velocity (depending on the distance)
		double distinit;
		double distgoal;
		
		distinit = pos.distance(init.x(), init.y());
		distgoal = pos.distance(goal.x(), goal.y());
		
		double b_aux = 0.01;
		double dist_max = 0.1;
		double distGoalMax = 1.5;
		double distGoalToStop = tol_pos;
		
		if(distinit < dist_max)
		{
			vm = ((b_aux-vm)/dist_max)*distinit  + b_aux;
			
			if(vm == 0.0)
				vm = b_aux;
			
			vm = -vm;
		} else if(distgoal < distGoalMax) {
			if(distgoal < tol_pos)
				vm = 0.0;
			else
				vm = - Math.max(0.5 * (distgoal/distGoalMax), distGoalToStop); // Velocidad cuando esta cerca de atracar
		}
		
		// Apply speed limits (vm, del)
		vmax	= rmodel.Vmax;	// [m/s]
		rmax	= rmodel.Rmax;	// [rad/s]
		//vmax = rmodel.MOTmax;
		//rmax = rmodel.STRmax;
		
		del = Math.max (Math.min (del, rmax), -rmax);	// [rad/s]
		vm = Math.max (Math.min (vm, vmax), -vmax);		// [m/s]
		
		// Direct Kinematics
		vr = vm * Math.cos(del);
		wr = (vr * Math.tan(del))/rmodel.l;
		
		double[] velocities;
		velocities = new double[2];
		
		velocities[0] = vr;
		velocities[1] = wr;
		
		return velocities;
	}
	
}
