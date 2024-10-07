/*
 * @(#)Task1.java		1.0 2003/11/28
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


public class Task1 extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh, subBeh2;

	
	public String getName() {
		return("Task1");
	}
	
	protected void createRules() {
		
		try {
			
//			subBeh = BehaviourFactory.createBehaviour("GoTo",false);
//			rules.addNewRule("useGoTo","(NOT (OR TargetRight TargetLeft))",subBeh);
//			
//			subBeh2 = BehaviourFactory.createBehaviour("Face",false);
//			rules.addNewRule("useFace","(OR TargetRight TargetLeft)",subBeh2);

			subBeh = BehaviourFactory.createBehaviour("GoTo",false);
			rules.addNewRule("useGoTo","Always",subBeh);
			
		}
		catch (SyntaxError se) {
			System.out.println(se.toString());
		}
		catch (LexicalError le) {
			System.out.println(le.toString());
		}
		catch (IOException ioe) {
			System.out.println("Input Output error");
		}
			
	}
	
	protected void update(HashMap params) {
		
		double targetLeft, targetRight;
		double x, y, phi;
		
		subBeh.setParams(params);
//		subBeh2.setParams(params);
		LPO target = (LPO) params.get("Goal");
		
		x = target.x();
		y = target.y();
		phi = Math.atan2(y,x)*180/Math.PI;  // from rad to deg
		
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(5.0,30.0);
		targetLeft = ramp.dmember(phi);
		targetRight = ramp.dmember(-phi);
	
		antecedentValues.setValue("TargetRight",targetRight);
		antecedentValues.setValue("TargetLeft",targetLeft);

	}
	
	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Task1(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Task1", new Task1.Factory());
	}


}