package tclib.tracking;

import devices.pos.Path;
import devices.pos.Position;
import wucore.utils.math.Angles;

import tc.vrobot.models.*;

public class ScrewControl {
	
	// Configuration parameters
	protected double	k			= 2.0;
	protected double	VSET		= 0.37;
	protected double	looka_dst	= 0.05;	// Current look-ahead distance (m)
	
	// Maneouvre parameters
	protected double	tol_pos		= 0.05;
	
	// Sampling period
	double				dt;			// Time control sampling
	
	// Robot model
	TricycleDrive		rmodel;
	double del, vm;					// Wheel's values
	
	// Look-ahead related variables
	protected Position	pos;		// Current robot location
	protected Position	looka;		// Current look-ahead point
	protected int		looka_pts;	// Current look-ahead distance (points)
	protected double	path_dst;	// Current robot to desired path distance (m)
	
	// Control variables
	protected double	vr;			// Desired output linear spped (m/s)
	protected double	wr;			// Desired output rotational speed (rad/s)
	
	// Path to follow
	protected Path		rpath;		// Path refined after the method
	
	public ScrewControl(double dt, TricycleDrive rmodel) {
		
		// Sampling period
		this.dt = dt;
		
		// Robot model
		this.rmodel = rmodel;
		
		// Current robot location
		pos = new Position ();
		
		// look-ahead variables
		looka = new Position ();
		looka_pts = 4; // for point-to-point navigation
		path_dst = 0.0;
		
		// Control variables
		vr = 0.0;
		wr = 0.0;
		
		// Wheel's parameters
		del = 0.0;
		vm = 0.0;
		
		// Path to follow
		rpath = null;
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
	
	public double[] step(Position robot, Position init, Position goal) {
		
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
		
		double dx, dy, dist, heading, delta;
		double vy, vx, wxd, wyd, vyd, phi;
		double vmax, rmax, km;
		
		double vel;
		double distinit;
		double distgoal;
		
		distgoal = pos.distance(goal.x(), goal.y());
		distinit = pos.distance(init.x(), init.y());
		
		/*
		double b_aux = 0.01;
		double dist_max = 0.1;
		if(distinit < dist_max)
		{
			vel = -(((VSET-b_aux)/dist_max)*distinit  + b_aux);
			if(vel == 0.0)
				vel = -b_aux;
		} else if(distgoal < 1.75) {
			if(distgoal < tol_pos)
				vel = 0.0;
			else
				vel = - VSET * (distgoal/(1.75));
				//vel = - Math.max(VSET * (distgoal/1.75), 0.05); // Velocidad cuando esta cerca de atracar
		} else {
			vel = -VSET; // Velocidad normal
		}
		*/
		
		if(distgoal < 1.75)
		{
			if(distgoal < tol_pos)
				vel = 0.0;	
			else
				vel = - Math.max(VSET * (distgoal/1.75), 0.05); // Velocidad cuando esta cerca de atracar
		} else
			vel = -VSET; // Velocidad normal
		
		wxd = 0; wyd = 0; phi = 0; vyd = 0; phi = 0; km = 0;
		
		dx		= looka.x () - pos.x ();
		dy		= looka.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);
		vy = dy * Math.cos(pos.alpha()) - dx * Math.sin(pos.alpha());
		vx = dx * Math.cos(pos.alpha()) + dy * Math.sin(pos.alpha());
		heading	= Math.atan2 (dy, dx);									// [rad]
		delta	= Angles.radnorm_180 (heading - pos.alpha ());			// [rad]
		delta	= Angles.radnorm_180 (Math.PI + delta);
		
		if(vy != 0.0)
		{
			//phi = Angles.radnorm_360(Math.atan2(2*vy*vy-dist*dist,2*vx*vy) - Math.atan2(vy,0));
			phi = Angles.radnorm_180(Math.atan2(-2*vx , (vy*vy - vx*vx)/vy) + Math.atan2(0,-vy));
			
			//km = (k * phi) / ((k-1) * phi + Angles.radnorm_180(looka.alpha() - pos.alpha() - Math.PI));
			if(vy>0)
				km =(-k * Angles.radnorm_360b(-phi))/ ((-k  * Angles.radnorm_360b(-phi)) + Angles.radnorm_180(looka.alpha() - pos.alpha() - Math.PI - phi));
			else
				km =(k * Angles.radnorm_360b(phi))/ ((k  * Angles.radnorm_360b(phi)) + Angles.radnorm_180(looka.alpha() - pos.alpha() - Math.PI - phi));
			
			wxd = pos.x() - (dist * dist * Math.sin(pos.alpha()) / (2*vy )) * km; 
			wyd = pos.y() + (dist * dist * Math.cos(pos.alpha()) / (2*vy )) * km;		
			
		} else if(looka.alpha() != pos.alpha()) {
			
			double dangles;
			dangles = Angles.radnorm_180(looka.alpha()-pos.alpha()+Math.PI);
			
			wxd = pos.x() - k * (looka.y() - pos.y())/dangles;
			wyd = pos.y() - k * (looka.x() - pos.x())/dangles;
			
		}
		
		if ((vy == 0.0) && (looka.alpha() == pos.alpha()))
		{
			vr = vel; // Velocidad de la Carretilla constante 0.1m/s	
			wr = 0;
			vm = vel;
			del = 0.0;
		}
		else
		{
			vyd = (wyd-pos.y()) * Math.cos(pos.alpha()) - (wxd-pos.x()) * Math.sin(pos.alpha());
			
			// Control con wr, vr(constante)
			//vr = v_dock;			
			//wr = vr / vyd;
			
			// Control del, vm (constante)
			if(vyd >= 0)
				del = Math.atan2(rmodel.l, vyd);
			else
				del = -Math.atan2(rmodel.l, -vyd);
			
			if(del > Math.toRadians(60)) del = Math.toRadians(60);
			if(del < Math.toRadians(-60)) del = Math.toRadians(-60);
			vm = vel;
			
			// Compute direct kynematics
			//vr = vm * Math.cos (del) + wr * (rmodel.b + rmodel.r * Math.cos (del));
			//wr = Math.sin (del) * vm / (rmodel.l - rmodel.r * Math.sin (del));
			
			vr = vm * Math.cos(del);
			wr = vr*Math.tan(del)/rmodel.l;
			
			//System.out.println(" <vm,del>: <" + vm + "," + del + ">");
			//System.out.println(" <vr,wr>: <" + vr + "," + wr + ">");
			
		}
		
		// Set up an upper boundary for the linear velocity (depending on the distance)
		vr	= Math.max (-dist / dt, vr); // [m/s]
		
		// Check for maximum planned velocities 
		vmax	= rmodel.Vmax;	// [m/s]
		rmax	= rmodel.Rmax;	// [rad/s]
		
		// Apply speed limits
		wr		= Math.max (Math.min (wr, rmax), -rmax); // [rad/s]
		vr		= Math.max (Math.min (vr, vmax), -vmax); // [m/s]
		
		//System.out.println(" Flatness vm: " + vm + " del: " + del);
		//System.out.println(" Screw vr: " + vr + " wr: " + wr);
		
		double[] velocities;
		velocities = new double[2];
		
		velocities[0] = vr;
		velocities[1] = wr;
		
		return velocities;
	}
}
