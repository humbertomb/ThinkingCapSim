/*
 * @(#)AvoidFront.java		1.0 2004/01/29
 * 
 * (c) 2004 Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;


import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

public class AvoidFront extends Behaviour 
{	
	// Fuzzy predicates
	private Ramp				isClose		= new Ramp (Ramp.RAMP_DOWN, 0.2, 0.3);
	private Ramp				isFar		= new Ramp (Ramp.RAMP_UP, 0.6, 0.7);
	
	public String getName() 
	{
		return "AvoidFront";
	}
		
	protected void createRules() 
	{
		Histogram		turnTR, turnR, turnZ, turnL, turnTL; 
		Histogram		speedZ;
		
		turnTR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTR.setYValue(ControlVariables.TURN_TR,1);
		turnR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnR.setYValue(ControlVariables.TURN_R,1);
		turnZ		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnZ.setYValue(ControlVariables.TURN_Z,1);
		turnL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnL.setYValue(ControlVariables.TURN_L,1);
		turnTL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTL.setYValue(ControlVariables.TURN_TL,1);

		speedZ		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedZ.setYValue(ControlVariables.SPEED_Z,1);
		
		try
		{
			rules.addNewRule ("TR1", "(AND (AND leftClose frontClose) (NOT rightClose))", ControlVariables.ROTATION, turnTR);
			rules.addNewRule ("TR2", "(AND (AND rightClose frontClose) (NOT leftClose))", ControlVariables.ROTATION, turnTL);

			rules.addNewRule ("SR1", "(NOT frontFar)", ControlVariables.SPEED, speedZ);
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	protected void update (HashMap params)
	{
		double			left, front, right;
		
		// Fuzzy predicates
		left	= ((Double) params.get ("Group1")).doubleValue ();
		front	= ((Double) params.get ("Group2")).doubleValue ();
		right	= ((Double) params.get ("Group3")).doubleValue ();
		
		antecedentValues.setValue ("leftClose", isClose.dmember (left));
		antecedentValues.setValue ("leftFar", isFar.dmember (left));		
		antecedentValues.setValue ("frontClose", isClose.dmember (front));
		antecedentValues.setValue ("frontFar", isFar.dmember (front));		
		antecedentValues.setValue ("rightClose", isClose.dmember (right));
		antecedentValues.setValue ("rightFar", isFar.dmember (right));		
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new AvoidFront(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("AvoidFront", new AvoidFront.Factory());
	}
}
