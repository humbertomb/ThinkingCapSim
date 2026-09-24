/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.vrobot;

import java.util.*;
import java.lang.reflect.*;

import wucore.utils.math.*;
import wucore.utils.math.stat.*;

public abstract class RobotModel extends Object
{
	// Odometry simulation parameters
	public double								odom_et; 		// Translational odometry error - standard deviation (m/s)
	public double								odom_er; 		// Rotational odometry error  - standard deviation (deg/s)
	public double								odom_bias; 		// Coefficient for biased odometry drift ([0..1])

    // Maximum values of kynematics parameters
	public transient double						Vmax;			// Maximum linear velocity (m/s)
	public transient double						Rmax;			// Maximum angular velocity (rad/s)

	// Kynematics outputs: the velocities of the platform itself, whatever it is built
	// with (the lateral one is zero on a platform that cannot be driven sideways)
    public transient double						vr, wr;			// Desired de-normalised velocities (translation, rotation)		[INPUT]
    public transient double						ur;				// Desired de-normalised lateral velocity (m/s), to the left

	// Robot position data (including simulated one)
	public transient double						odom_x;			// Odometry-based position (m, m, rad)
	public transient double						odom_y;
	public transient double						odom_a;
	public transient double						real_x;			// Real robot position (m, m, rad)
	public transient double						real_y;
	public transient double						real_a;

	// Robot backed-up position 
	private transient double					orx, ory, ora;	// Real robot position (m, m, rad)
	private transient double					oox, ooy, ooa;	// Odometry-corrected robot position (m, m, rad)
	private transient double					ox, oy, oa;		// Odometry-based robot position (m, m, rad)

	// Additional variables
	protected transient RandomNumberGenerator	rnd;			// Gaussian pseudo-random number generator
	protected RobotDesc							rdesc;

	// Constructors
	protected RobotModel (RobotDesc rdesc, Properties props)
	{
		this.rdesc		= rdesc;
		
		// Set simulation properties
		update (props);
		
		// Initialise additional parameters and data
		rnd 		= new RandomNumberGenerator ();
	}
	
	// Class methdos
	static public RobotModel getModel (RobotDesc rdesc, Properties props)
	{
		String			name;
		Class<?>[]			types;
		Object[]			params;
		Constructor<?>		cons;
		Class<?>			mclass;
		RobotModel		model = null;
		
		name = props.getProperty ("DRIVEMODEL");
		if (name == null)
		{
			name = "tc.vrobot.models.DifferentialDrive";
			System.out.println ("--[RobotModel] No DRIVEMODEL property defined. Setting to <"+name+">");
		}
		
		try
		{
			mclass		= Class.forName (name);
			types		= new Class<?>[2];
			types[0]		= Class.forName ("tc.vrobot.RobotDesc");
			types[1]		= Class.forName ("java.util.Properties");
			cons			= mclass.getConstructor (types);
			params		= new Object[2];
			params[0]	= rdesc;
			params[1]	= props;
			model		= (RobotModel) cons.newInstance (params);
		}
		catch (Exception e) { e.printStackTrace (); }
		
		return model;
	}
	
	// Abstract methdos
	/** What the platform does with what it is built with: the wheels say it, each model in its own terms. */
	public abstract void kynematics_direct (double inp1, double inp2);

	/**
	 * What the platform is built with has to do to carry out a control action: the
	 * three velocities of the platform ([m/s], [m/s], [rad/s]), of which the lateral
	 * one is nothing to a platform that cannot be driven sideways
	 * ({@link #lateral}).
	 */
	public abstract void kynematics_inverse (double vlin, double vlat, double vrot);
	protected abstract void kynematics_simulation (double vlin, double vlat, double vrot);

	/**
	 * Whether the platform can be driven sideways, which is to say whether a lateral
	 * velocity (vlat) means anything to it: a synchro drive steers every wheel
	 * together and can, and a platform whose wheels point where they are built
	 * cannot. Only the models that can say so.
	 */
	public boolean lateral ()								{ return false; }

	/** How many times a lateral velocity nobody can carry out is said out loud before it is only counted. */
	static protected final int					LAT_SAID	= 5;

	private transient int						latsaid;		// how many such commands there have been

	/**
	 * What a platform that cannot be driven sideways makes of a lateral velocity:
	 * nothing, said out loud the first few times so that whoever commanded it knows
	 * why the robot is not doing it.
	 */
	protected double noLateral (double vlat)
	{
		if ((vlat != 0.0) && (latsaid < LAT_SAID))
		{
			System.out.println ("  [Model] " + getClass ().getSimpleName () + " cannot be driven sideways: vlat="
								+ vlat + " m/s ignored");
			latsaid++;
		}
		return 0.0;
	}

	// Instance methods
	public void update (Properties props)
	{
		try { Vmax	 	= Double.valueOf (props.getProperty ("VMAX")).doubleValue (); } 				catch (Exception e) 		{ }
		try { Rmax	 	= Double.valueOf (props.getProperty ("RMAX")).doubleValue () * Angles.DTOR; } 	catch (Exception e) 		{ }

		try { odom_et 	= Double.valueOf (props.getProperty ("ODOM_ET")).doubleValue (); }				catch (Exception e)		{ }
		try { odom_er 	= Double.valueOf (props.getProperty ("ODOM_ER")).doubleValue (); }				catch (Exception e)		{ }
		try { odom_bias 	= Double.valueOf (props.getProperty ("ODOM_BIAS")).doubleValue (); } 			catch (Exception e)		{ }
	}
	
	public void position (RobotData data, double x, double y, double alpha)
	{
		real_x			= x;		real_y			= y;		real_a			= alpha;
		odom_x			= x;		odom_y			= y;		odom_a			= alpha;
		data.odom_x		= x;		data.odom_y		= y;		data.odom_a		= alpha;
	}

	public void backup (RobotData data)
	{
		orx 	= real_x;			ory 	= real_y; 			ora 	= real_a;
		oox 	= odom_x;			ooy 	= odom_y; 			ooa 	= odom_a;
		ox 	= data.odom_x;		oy 	= data.odom_y; 		oa 	= data.odom_a;
	}
	
	public void restore (RobotData data)
	{
		data.odom_x		= ox;		data.odom_y		= oy;		data.odom_a		= oa;
		real_x			= orx;		real_y			= ory;		real_a			= ora;
		odom_x			= oox;		odom_y			= ooy;		odom_a			= ooa;
	}

	public void update (RobotData data)
	{
		data.real_x		= real_x;	
		data.real_y		= real_y;	
		data.real_a		= real_a;
	}

	public void odometry (RobotData data, double enc1, double enc2)
	{
		kynematics_direct (enc1, enc2);			

		// Calculate odometry based position
		data.odom_rho	= vr;
		data.odom_x		+= vr * Math.cos (data.odom_a + wr * 0.5);
		data.odom_y		+= vr * Math.sin (data.odom_a + wr * 0.5);
		data.odom_a		= Angles.radnorm_180 (data.odom_a + wr);
	}

	public void simulation (RobotData data, double vlin, double vlat, double vrot, double dt)
	{
		double		rg_rho, rg_phi, rg_lat;		// Random gaussian variables (translation, rotation, lateral)
		double		rho, phi, lat;				// Local coordinates (forward, rotation, sideways)				[OUTPUT]		
 				    	    	 		
		// Compute robot command (inverse+direct kynematics)
		kynematics_simulation (vlin, vlat, vrot);
		
		// Compute new position (local reference)
		rho				= vr * dt;
		lat				= ur * dt;
		phi				= wr * dt;	

		// Calculate real robot displacement: where it points it goes forward, and to
		// the left of that it goes sideways (nowhere at all on a platform that cannot)
		real_a			= Angles.radnorm_180 (real_a + phi);
		real_x			+= rho * Math.cos (real_a);
		real_y			+= rho * Math.sin (real_a);
		if (lat != 0.0)
		{
			real_x		-= lat * Math.sin (real_a);
			real_y		+= lat * Math.cos (real_a);
		}
		
		// Calculate odometry based displacement (depending on robot velocity). A
		// platform that is not going sideways draws no random number for it, so that
		// what it does is the same to the last decimal as it was before it could
		rg_rho			= rho + rnd.nextGaussian (0.0, Math.pow (odom_et * rho, 2.0));
		rg_phi			= phi + rnd.nextGaussian (odom_bias * phi, Math.pow (odom_er * phi, 2.0));
		rg_lat			= (lat != 0.0) ? (lat + rnd.nextGaussian (0.0, Math.pow (odom_et * lat, 2.0))) : 0.0;
			
		odom_a			= Angles.radnorm_180 (odom_a + rg_phi);
		odom_x			+= rg_rho * Math.cos (odom_a);
		odom_y			+= rg_rho * Math.sin (odom_a);
		if (rg_lat != 0.0)
		{
			odom_x		-= rg_lat * Math.sin (odom_a);
			odom_y		+= rg_lat * Math.cos (odom_a);
		}

		// Calculate corrected odometry-based position
		data.odom_a		= Angles.radnorm_180 (data.odom_a + rg_phi);
		data.odom_rho	= rg_rho;
		data.odom_x		+= rg_rho * Math.cos (data.odom_a);
		data.odom_y		+= rg_rho * Math.sin (data.odom_a);
		if (rg_lat != 0.0)
		{
			data.odom_x	-= rg_lat * Math.sin (data.odom_a);
			data.odom_y	+= rg_lat * Math.cos (data.odom_a);
		}
	}
	
	public String toString ()
	{
		return "Real=["+real_x+","+real_y+","+real_a+"], Odometry=["+odom_x+","+odom_y+","+odom_a+"]";
	}
}

