/*
 * @(#)FollowPath.java		1.0 2004/03/23
 * 
 * (c) 2004 Denis Remondini, Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;

import tc.shared.lps.lpo.*;

import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

import wucore.utils.math.*;

public class FollowPath extends Behaviour 
{	
	// Fuzzy predicates
	private Ramp				isClose		= new Ramp (Ramp.RAMP_DOWN, 0.2, 0.5);

	// Fuzzy predicates for heading-error (deg)
	private Ramp				isZero		= new Ramp (Ramp.RAMP_DOWN, 0.0, 5.0);
	private Trapezoid			isSmall		= new Trapezoid (0.0, 5.0, 15.0, 25.0);
	private Trapezoid			isMedium 	= new Trapezoid (15.0, 25.0, 35.0, 45.0);
	private Ramp				isLarge 	= new Ramp (Ramp.RAMP_UP, 35.0, 45.0);

	public String getName() 
	{
		return "FollowPath";
	}

	protected void createRules() 
	{
		Histogram		turnTR, turnR, turnSR, turnZ, turnSL, turnL, turnTL; 
		Histogram		speedZ, speedF;
		
		turnTR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTR.setYValue(ControlVariables.TURN_TR,1);
		turnR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnR.setYValue(ControlVariables.TURN_R,1);
		turnSR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSR.setYValue(ControlVariables.TURN_SR,1);
		turnZ		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnZ.setYValue(ControlVariables.TURN_Z,1);
		turnSL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSL.setYValue(ControlVariables.TURN_SL,1);
		turnL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnL.setYValue(ControlVariables.TURN_L,1);
		turnTL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTL.setYValue(ControlVariables.TURN_TL,1);

		speedZ		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedZ.setYValue(ControlVariables.SPEED_Z,1);
		speedF		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedF.setYValue(ControlVariables.SPEED_F,1);				
		
		try
		{
			rules.addNewRule ("TR1", "leftLarge", ControlVariables.ROTATION, turnTL);
			rules.addNewRule ("TR2", "leftMedium", ControlVariables.ROTATION, turnL);
			rules.addNewRule ("TR3", "leftSmall", ControlVariables.ROTATION, turnSL);
			rules.addNewRule ("TR4", "center", ControlVariables.ROTATION, turnZ);
			rules.addNewRule ("TR5", "rightSmall", ControlVariables.ROTATION, turnSR);
			rules.addNewRule ("TR6", "rightMedium", ControlVariables.ROTATION, turnR);
			rules.addNewRule ("TR7", "rightLarge", ControlVariables.ROTATION, turnTR);

			rules.addNewRule ("SR1", "close", ControlVariables.SPEED, speedZ);
			rules.addNewRule ("SR2", "(NOT close)", ControlVariables.SPEED, speedF);
		}
		catch (Exception e) { e.printStackTrace (); }
	}	
	
	protected void update(HashMap params) 
	{
		double			dx, dy;
		double			delta;
		double			dist, heading;	
		LPO				looka;
		
		// Set parameters
		looka	= (LPO) params.get ("Looka");
		
		// Compute the local heading to goal 
		dx		= looka.x ();
		dy		= looka.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);								// [m]
		heading	= Math.atan2 (dy, dx);											// [rad]
		delta	= Angles.radnorm_180 (heading - looka.alpha ()) * Angles.RTOD;	// [deg]

		/* update the parameters used by this behaviour. This information is used
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(looka.label());

		// Update antecedent values
		antecedentValues.setValue("close", isClose.dmember (dist));

		antecedentValues.setValue("leftLarge", isLarge.dmember (delta));
		antecedentValues.setValue("leftMedium", isMedium.dmember (delta));
		antecedentValues.setValue("leftSmall", isSmall.dmember (delta));
		antecedentValues.setValue("center", isZero.dmember (Math.abs (delta)));
		antecedentValues.setValue("rightSmall", isSmall.dmember (-delta));
		antecedentValues.setValue("rightMedium", isMedium.dmember (-delta));
		antecedentValues.setValue("rightLarge", isLarge.dmember (-delta));
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new FollowPath();
		}
	}
	
	static {
		BehaviourFactory.addFactory("FollowPath", new FollowPath.Factory());
	}
}