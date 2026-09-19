/*
 * Created on 04-sep-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.vrobot.models;

import java.util.*;

import tc.vrobot.*;
import wucore.utils.math.Angles;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TricycleDrive extends RobotModel
{
	// Kynematics outputs
 	public transient double		vm, del;			// Tricycle model (forward speed and signus, wheel angle)

	// Dymanics simulation
	protected transient double		lvm, ldel;		// Tricycle model (previously issued control commands)

	// Parameters
	public double		l;				// Distance between wheel axles (m)
	public double		b;				// Wheel base (m)	Tricyle => Distance from wheel to center
	public double		r;				// Front wheel displacement

	/*
	 * Maximum values of kynematics parameters.
	 *
	 * None of them is given a value here on purpose: the constructor of
	 * RobotModel reads the properties, so update () runs before the fields of
	 * this class would be initialised, and anything set here would be written
	 * over what was read.  They are defaulted at the head of update () instead.
	 */
	private double		SAmax;							// Maximum steering-wheel angular velocity (rad/s)
	private double		LAmax;							// Maximum linear acceleration (m/s2)
	private double		LDmax;							// Maximum linear decceleration (m/s2)
	public double		MOTmax; 							// Maximum traction speed (m/s), as fast as the robot goes
	public double		STRmax; 							// Maximum steering angle (rad), from how fast it turns

	// Constructors
	public TricycleDrive (RobotDesc rdesc)
	{
		this (rdesc, null);
	}

	public TricycleDrive (RobotDesc rdesc, Properties props)
	{
		super (rdesc, props);

		// Initialise simulation parameters
		lvm		= 0.0;
		ldel		= 0.0;
	}
	
	// Instance methdos
	public void update (Properties props)
	{
		super.update (props);

		SAmax	= 0.0;
		LAmax	= Double.MAX_VALUE;
		LDmax	= Double.MAX_VALUE;
		r		= 0.0;
		b		= 0.0;
		l		= 0.0;
		try { SAmax	 = Double.valueOf (props.getProperty ("SAMAX")).doubleValue () * Angles.DTOR; } 	catch (Exception e) 		{ }
		try { LAmax	 = Double.valueOf (props.getProperty ("LAMAX")).doubleValue (); } 				catch (Exception e) 		{ }
		try { LDmax	 = Double.valueOf (props.getProperty ("LDMAX")).doubleValue (); } 				catch (Exception e) 		{ }

		try { r		= Double.valueOf (props.getProperty ("RWHEEL")).doubleValue (); }				catch (Exception e)		{ }
		try { b		= Double.valueOf (props.getProperty ("BASE")).doubleValue (); } 				catch (Exception e) 		{ }
		try { l		= Double.valueOf (props.getProperty ("LENGHT")).doubleValue (); } 				catch (Exception e) 		{ }

		/*
		 * How fast the traction wheel turns and how far the steering one goes over,
		 * which a description says as MAXMOTOR and MAXSTEER and hardly any of them
		 * does. Left at nothing they would saturate every command to a standstill, so
		 * they are worked out of what the robot says of itself, as the Ackerman model
		 * works its own steering out.
		 *
		 * As fast as the robot goes is as fast as the wheel that drives it can take
		 * it; and Rmax = Vmax * sin (STRmax) / l, so how far it steers is what turning
		 * that fast asks for.  With nothing to go on: hard over at a right angle.
		 */
		MOTmax	= Vmax;
		if ((Vmax > 0.0) && (l > 0.0) && (Rmax * l / Vmax < 1.0))	STRmax = Math.asin (Rmax * l / Vmax);
		else														STRmax = 90.0 * Angles.DTOR;
		try { MOTmax	= Double.valueOf (props.getProperty ("MAXMOTOR")).doubleValue (); } 			catch (Exception e) 		{ }
		try { STRmax	= Double.valueOf (props.getProperty ("MAXSTEER")).doubleValue () * Angles.DTOR; } catch (Exception e) 	{ }
	}
	
	public void kynematics_direct (double vm, double del)
	{
		double			sdmax;
		double			amax, dmax;
		
		this.vm		= vm;
		this.del		= del;
		
		// Check for steering dynamics constraints
		sdmax		= (SAmax * rdesc.DTIME / 1000.0);

		if (del - ldel > sdmax)
			del		= ldel + sdmax;
		else if (del - ldel < -sdmax)
			del		= ldel - sdmax;

		// Check for acceleration/decceleration constraints
		amax			= (LAmax * rdesc.DTIME / 1000.0); 
		dmax			= (LDmax * rdesc.DTIME / 1000.0);
		if( Math.abs(vm) > Math.abs(lvm) )
		{
			if(Math.abs(vm-lvm) > amax)
			{
				if(vm > 0)
					vm = lvm + amax;
				else
					vm = lvm - amax;
			}
		}
		else
		{
			if(Math.abs(vm-lvm) > dmax)
			{
				if(lvm > 0)
					vm = lvm - dmax;
				else
					vm = lvm + dmax;
			}
		}
		
		// Compute direct kynematics
		wr		= Math.sin (del) * vm / (l - r * Math.sin (del));
		vr		= vm * Math.cos (del) + wr * (b + r * Math.cos (del));			
		
		ldel		= del;
		lvm		= vm;
	}
	
	public void kynematics_inverse (double speed, double turn)
	{
		// Set motion commands into the correct range
		vr	= Math.min (Math.max (speed, -Vmax), Vmax);
		wr	= Math.min (Math.max (turn, -Rmax), Rmax);

		// Compute robot command (inverse kynematics)
		if( (vr == 0.0) && (wr == 0.0) )
		{
			vm	= 0.0;
			del	= 0.0;
		}
		else 
		{
			if(vr > 0.0)
			{
				vm = - wr * r + Math.sqrt(Math.pow(vr-wr*b,2.0) + Math.pow(wr*l,2.0) );
				del = Math.atan2(wr*l, vr - wr * b);
			}
			else if(vr < 0.0)
			{
				vm = - wr * r - Math.sqrt(Math.pow(vr-wr*b,2.0) + Math.pow(wr*l,2.0) );
				del = Angles.radnorm_180(Math.atan2(wr*l, vr - wr * b)+Math.PI);
			}
		}

		//System.out.println ("vr="+vr+", wr="+wr*Angles.RTOD+" => vm="+vm+"), del="+del*Angles.RTOD+")");		

		// Check for kynematics constraints
		vm	= Math.min (Math.max (vm, -MOTmax), MOTmax);
		del	= Math.min (Math.max (del, -STRmax), STRmax);
	}
	
	public void kynematics_simulation (double speed, double turn)
	{
		// Compute robot command (inverse kynematics)
		kynematics_inverse (speed, turn);
		
		// Add some noise to control commands
		del += rnd.nextGaussian() * 1.5 * Angles.DTOR; 
		vm  += rnd.nextGaussian() * vm * 0.10;
		
		// Compute robot displacement (direct kynematics)
		kynematics_direct (vm, del);
	}
	
	// vm y wmax positivos
	public double maxDelta(double vm, double wmax){
		double delta = Math.toRadians(90); 
		if(vm > wmax * l)
			delta = Angles.radnorm_180(Math.asin(wmax * l / vm));
		return delta;
	}
	
	
}
