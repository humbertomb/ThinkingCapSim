/*
 * @(#)Task3.java		1.0 2003/11/28
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

public class Task4 extends Behaviour {

	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh, subBeh2;

	
	public String getName() {
		return("Task4");
	}
	
	protected void createRules() {
		
		try {

			subBeh = BehaviourFactory.createBehaviour("FollowCorridor",false);
			rules.addNewRule("useFollow","(NOT NearDoor)",subBeh);
			
			subBeh2 = BehaviourFactory.createBehaviour("Cross",false);
			rules.addNewRule("useCross","NearDoor",subBeh);
			
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
		
		double nearDoor;
		
		LPO door = (LPO) params.get("Goal");
		subBeh.setParams(params);
		subBeh2.setParams(params);
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(0.4,1);
		nearDoor = ramp.dmember(door.rho());
		antecedentValues.setValue("NearDoor",nearDoor);
	}
	
	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Task4(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Task4", new Task4.Factory());
	}


}