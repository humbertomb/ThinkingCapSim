/*
 * @(#)KeepVelocity.java		1.0 2004/02/16
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.util.HashMap;
import java.io.*;

import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;



public class KeepVelocity extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	
	public String getName() {
		return "KeepVelocity";
	}

	protected void createRules() {
		Histogram goSlow = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goSlow.setYValue(ControlVariables.SPEED_Z,1);
		
		Histogram goFast = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goFast.setYValue(ControlVariables.SPEED_M,1);
		
		try {
		
			rules.addNewRule("Accelerate","TooSlow",ControlVariables.SPEED,goFast);
			
			rules.addNewRule("Slow-down","TooFast",ControlVariables.SPEED,goSlow);
			
		}
		catch (SyntaxError se){
			System.out.println(se.toString());
		}
		catch (LexicalError le) {
			System.out.println(le.toString());
		}
		catch (IOException ioe){
			System.out.println("Input Output error");
		}
	}

	protected void update(HashMap params) {

		double speed = ((Double)params.get("speed")).doubleValue();
		double tooSlow, tooFast;

/*		double clearance;
		double range;
		double maxSpeed;
		LPORangeBuffer sensors = (LPORangeBuffer) params.get("RBuffer");
				
		FeaturePos f = new FeaturePos();
		f.set_xy(0.2,0,0);
		range = 0.8;
		clearance = sensors.occupied_rect(f,0.2,range,false);
		maxSpeed = ((clearance == range) ? 0.4 : clearance/2);
*/		
//		System.out.println("DEBUG: clearance = "+clearance+" | MaxSpeed = "+maxSpeed);
//		System.out.println("DEBUG: speed = "+speed);
		// 0.4 is the max speed of the Rasmus robot.
		if (speed > 0.4)
			speed = 0.4;
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(0,0.4);
		tooFast = ramp.dmember(speed);
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(0,0.4);
		tooSlow = ramp.dmember(speed);
//		System.out.println("tooSlow = "+tooSlow);
//		System.out.println("tooFast = "+tooFast);
		antecedentValues.setValue("TooFast",tooFast);
		antecedentValues.setValue("TooSlow",tooSlow);
								
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new KeepVelocity(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("KeepVelocity", new KeepVelocity.Factory());
	}
	
	
}
