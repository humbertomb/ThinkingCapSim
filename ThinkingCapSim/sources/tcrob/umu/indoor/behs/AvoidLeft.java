/*
 * @(#)AvoidLeft.java		1.0 2004/01/29
 * 
 * (c) 2004 Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;


import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

public class AvoidLeft extends Behaviour 
{	
	// Fuzzy predicates
	private Ramp				isClose		= new Ramp (Ramp.RAMP_DOWN, 0.2, 0.3);
	private Ramp				isFar		= new Ramp (Ramp.RAMP_UP, 0.6, 0.7);
	
	public String getName() 
	{
		return "AvoidLeft";
	}
		
	protected void createRules() 
	{
		Histogram		turnTR, turnR, turnSR, turnZ; 
		Histogram		speedZ, speedS;
		
		turnTR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTR.setYValue(ControlVariables.TURN_TR,1);
		turnR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnR.setYValue(ControlVariables.TURN_R,1);
		turnSR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSR.setYValue(ControlVariables.TURN_SR,1);
		turnZ		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnZ.setYValue(ControlVariables.TURN_Z,1);

		speedZ		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedZ.setYValue(ControlVariables.SPEED_Z,1);
		speedS		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedS.setYValue(ControlVariables.SPEED_S,1);				
		
		try
		{
			rules.addNewRule ("TR1", "leftClose", ControlVariables.ROTATION, turnTR);
			rules.addNewRule ("TR2", "(AND (AND (NOT leftClose) (NOT leftFar)) (NOT frontFar))", ControlVariables.ROTATION, turnR);
			rules.addNewRule ("TR3", "(AND (AND (NOT leftClose) (NOT leftFar)) frontFar)", ControlVariables.ROTATION, turnSR);
			rules.addNewRule ("TR4", "(AND leftFar frontClose)", ControlVariables.ROTATION, turnR);
			rules.addNewRule ("TR5", "(AND leftFar (AND (NOT frontClose) (NOT frontFar)))", ControlVariables.ROTATION, turnSR);
			rules.addNewRule ("TR6", "(AND leftFar frontFar)", ControlVariables.ROTATION, turnZ);

			rules.addNewRule ("SR1", "(AND (NOT leftFar) (NOT frontFar))", ControlVariables.SPEED, speedZ);
			rules.addNewRule ("SR2", "(OR leftFar frontFar)", ControlVariables.SPEED, speedS);
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	protected void update (HashMap params)
	{
		double			left1, left2;
		double			left, front;
		
		// Fuzzy predicates
		left1	= ((Double) params.get ("Group0")).doubleValue ();
		left2	= ((Double) params.get ("Group1")).doubleValue ();
		left	= Math.min (left1, left2);
		front	= ((Double) params.get ("Group2")).doubleValue ();
		
		antecedentValues.setValue ("leftClose", isClose.dmember (left));
		antecedentValues.setValue ("leftFar", isFar.dmember (left));		
		antecedentValues.setValue ("frontClose", isClose.dmember (front));
		antecedentValues.setValue ("frontFar", isFar.dmember (front));		
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new AvoidLeft(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("AvoidLeft", new AvoidLeft.Factory());
	}
}
