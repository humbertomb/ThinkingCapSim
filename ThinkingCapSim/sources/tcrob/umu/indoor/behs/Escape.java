/*
 * @(#)Escape.java		1.0 Feb 6, 2004
 * 
 * (c) 2004 Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.HashMap;

import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

/**
 * @author Denis Remondini
 *
 */
public class Escape extends Behaviour 
{
	public String getName() 
	{
		return "Escape";
	}
	
	protected void createRules() 
	{
		Histogram		turnTR; 
		Histogram		speedZ;
		
		turnTR		= cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnTR.setYValue(ControlVariables.TURN_TR,1);

		speedZ		= cv.getDefaultOutputFSet(ControlVariables.SPEED);
		speedZ.setYValue(ControlVariables.SPEED_Z,1);
			
		try {
			rules.addNewRule("stop", "Always", ControlVariables.SPEED, speedZ);
			rules.addNewRule("turnRight", "Always", ControlVariables.ROTATION, turnTR);
		}
		catch (Exception e) { e.printStackTrace (); }
	}

	protected void update(HashMap params) 
	{
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Escape(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Escape", new Escape.Factory());
	}
}
