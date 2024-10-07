/*
 * @(#)Task2.java		1.0 2003/11/28
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.io.IOException;
import java.util.HashMap;

import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;


public class Task2 extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh;

	
	public String getName() {
		return("Task2");
	}
	
	protected void createRules() {
		
		try {
			
			subBeh = BehaviourFactory.createBehaviour("FollowCorridor",false);
			rules.addNewRule("useFollow","Always",subBeh);
			
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
					
		subBeh.setParam("RBuffer",params.get("RBuffer"));
		subBeh.setParam("speed",new Double("0.2"));
		//subBeh.setParam("speed",params.get("followSpeed"));
		
	}
	
	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Task2(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Task2", new Task2.Factory());
	}


}