/*
 * @(#)AvoidRight.java		1.0 2004/01/29
 * 
 * (c) 2004 Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;


import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

public class AvoidRight extends Behaviour 
{	
	// Fuzzy predicates
	private Ramp				isClose		= new Ramp (Ramp.RAMP_DOWN, 0.2, 0.3);
	private Ramp				isFar		= new Ramp (Ramp.RAMP_UP, 0.6, 0.7);
	
	public String getName() 
	{
		return "AvoidRight";
	}
		
	protected void createRules() 
	{
		Histogram		turnTL, turnL, turnSL, turnZ; 
		Histogram		speedZ, speedS;
		
		turnTL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTL.setYValue(ControlVariables.TURN_TL,1);
		turnL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnL.setYValue(ControlVariables.TURN_L,1);
		turnSL		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSL.setYValue(ControlVariables.TURN_SL,1);
		turnZ		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnZ.setYValue(ControlVariables.TURN_Z,1);

		speedZ		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedZ.setYValue(ControlVariables.SPEED_Z,1);
		speedS		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedS.setYValue(ControlVariables.SPEED_S,1);				
		
		try
		{
			rules.addNewRule ("TR1", "rightClose", ControlVariables.ROTATION, turnTL);
			rules.addNewRule ("TR2", "(AND (AND (NOT rightClose) (NOT rightFar)) (NOT frontFar))", ControlVariables.ROTATION, turnL);
			rules.addNewRule ("TR3", "(AND (AND (NOT rightClose) (NOT rightFar)) frontFar)", ControlVariables.ROTATION, turnSL);
			rules.addNewRule ("TR4", "(AND rightFar frontClose)", ControlVariables.ROTATION, turnL);
			rules.addNewRule ("TR5", "(AND rightFar (AND (NOT frontClose) (NOT frontFar)))", ControlVariables.ROTATION, turnSL);
			rules.addNewRule ("TR6", "(AND rightFar frontFar)", ControlVariables.ROTATION, turnZ);

			rules.addNewRule ("SR1", "(AND (NOT rightFar) (NOT frontFar))", ControlVariables.SPEED, speedZ);
			rules.addNewRule ("SR2", "(OR rightFar frontFar)", ControlVariables.SPEED, speedS);
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	protected void update (HashMap params)
	{
		double			right1, right2;
		double			right, front;
		
		// Fuzzy predicates
		front	= ((Double) params.get ("Group2")).doubleValue ();
		right1	= ((Double) params.get ("Group3")).doubleValue ();
		right2	= ((Double) params.get ("Group4")).doubleValue ();
		right	= Math.min (right1, right2);
		
		antecedentValues.setValue ("rightClose", isClose.dmember (right));
		antecedentValues.setValue ("rightFar", isFar.dmember (right));		
		antecedentValues.setValue ("frontClose", isClose.dmember (front));
		antecedentValues.setValue ("frontFar", isFar.dmember (front));		
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new AvoidRight(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("AvoidRight", new AvoidRight.Factory());
	}
}
