/*
 * @(#)GoTo.java		1.0 2003/11/28
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tcrob.umu.indoor.behs;

import java.io.IOException;
import java.util.HashMap;

import tc.shared.lps.lpo.*;
import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;



public class GoTo extends Behaviour {
	
	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);

	
	public String getName() {
		return("GoTo");
	}
	
	protected void createRules() {
	
		Histogram turnLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnLeft.setYValue(ControlVariables.TURN_L,1);

		Histogram turnRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnRight.setYValue(ControlVariables.TURN_R,1);
		
		Histogram turnAhead = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnAhead.setYValue(ControlVariables.TURN_Z,1);
			
		Histogram goToTargetSpeed = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goToTargetSpeed.setYValue(ControlVariables.SPEED_S,1);
		
		Histogram goStay = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goStay.setYValue(ControlVariables.SPEED_Z,1);

		try {
			
			rules.addNewRule("turnLeft","(AND TargetLeft (NOT TargetHere))",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("turnRight","(AND TargetRight (NOT TargetHere))",
					ControlVariables.ROTATION,turnRight);
			
			rules.addNewRule("turnAhead","(NOT (OR TargetRight TargetLeft))",
					ControlVariables.ROTATION,turnAhead);
			
			rules.addNewRule("goForward","(NOT TargetHere)",ControlVariables.SPEED,goToTargetSpeed);

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
		
//		LPO target = (LPO) params.get("Goal");
		
		//TODO: it is used whit auto generated behaviours (using bplan)
		LPO target = (LPO) params.get("LPO0");
		
		/* update the parameters used by this behaviour. This information is used to
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(target.label());
		
		double x = target.x();
		double y = target.y();
//		System.out.println("DEBUG: target x = "+x);
//		System.out.println("DEBUG: target y = "+y);
//		double phi = Math.atan2(y,x)*180/Math.PI;  // from rad to deg
		//TODO: it's just to test the path following, remove it
		double phi = Math.atan2(y,x)*180/Math.PI;  
//		phi = (phi-target.alpha())*180/Math.PI;// from rad to deg
		double rho = Math.sqrt(x*x+y*y);
		double targetLeft, targetRight, targetHere;
		
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(5.0,30.0);
		targetRight = ramp.dmember(-phi);
		targetLeft = ramp.dmember(phi);
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(0.1,0.5);
		targetHere = ramp.dmember(rho);
						
		antecedentValues.setValue("TargetRight",targetRight);
		antecedentValues.setValue("TargetLeft",targetLeft);
		antecedentValues.setValue("TargetHere",targetHere);
	}

	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new GoTo();
		}
	}
	
	
	static {
		BehaviourFactory.addFactory("GoTo", new GoTo.Factory());
	}


}