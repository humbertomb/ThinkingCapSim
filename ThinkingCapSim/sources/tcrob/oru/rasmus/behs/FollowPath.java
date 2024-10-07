/*
 * @(#)FollowPath.java		1.0 2004/03/23
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.io.IOException;
import java.util.HashMap;

import tc.shared.lps.lpo.*;
import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;
import wucore.utils.math.*;



public class FollowPath extends Behaviour {
	
	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour keepVel;

	
	public String getName() {
		return("FollowPath");
	}
	
	protected void createRules() {
	
		Histogram turnLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnLeft.setYValue(ControlVariables.TURN_L,1);

		Histogram turnRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnRight.setYValue(ControlVariables.TURN_R,1);
		
		Histogram turnAhead = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnAhead.setYValue(ControlVariables.TURN_Z,1);
			
		Histogram goToTargetSpeed = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goToTargetSpeed.setYValue(ControlVariables.SPEED_M,1);
		
		Histogram goStay = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goStay.setYValue(ControlVariables.SPEED_Z,1);

		try {
			
			rules.addNewRule("turnLeft","(AND TargetLeft (NOT TargetHere))",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("turnRight","(AND TargetRight (NOT TargetHere))",
					ControlVariables.ROTATION,turnRight);
			
			rules.addNewRule("turnAhead","(NOT (OR TargetRight TargetLeft))",
					ControlVariables.ROTATION,turnAhead);
			
			keepVel = BehaviourFactory.createBehaviour("KeepVelocity",false);
			rules.addNewRule("goForward","(NOT TargetHere)",keepVel);

			rules.addNewRule("goStay","TargetHere",ControlVariables.SPEED,goStay);
						
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
		
		LPO target = (LPO) params.get("Goal");
		LPO looka = (LPO) params.get("Looka");
		double speed;

		/* update the parameters used by this behaviour. This information is used to
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(target.label());
		parameters.add(looka.label());
				
		double x = target.x();
		double y = target.y();
		double phi = Math.atan2(looka.y(),looka.x());  
		phi = Angles.radnorm_180((phi-looka.alpha()))*180/Math.PI;// from rad to deg
		double rho = Math.sqrt(x*x+y*y);
		double targetLeft, targetRight, targetHere;
		
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(5.0,30.0);
		targetRight = ramp.dmember(-phi);
		targetLeft = ramp.dmember(phi);
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(0.1,2);
		targetHere = ramp.dmember(rho);
		
		ramp.type = Ramp.RAMP_DOWN;
		speed = ramp.dmember(Math.abs(phi),10,30);
//		System.out.println("speed % = "+speed);
		speed = speed*((Double)params.get("wanderSpeed")).doubleValue();
//		System.out.println("keepVel speed = "+speed);
		keepVel.setParam("speed",new Double(speed));
		keepVel.setParam("RBuffer",params.get("RBuffer"));
//		System.out.println("phi = "+phi);
				
		antecedentValues.setValue("TargetRight",targetRight);
		antecedentValues.setValue("TargetLeft",targetLeft);
		antecedentValues.setValue("TargetHere",targetHere);
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