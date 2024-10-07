/*
 * @(#)Task3.java		1.0 2003/11/28
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


public class Task3 extends Behaviour {

	private Behaviour subBeh;

	
	public String getName() {
		return("Task3");
	}
	
	protected void createRules() {
		
		try {

			subBeh = BehaviourFactory.createBehaviour("KeepVelocity",false);
			rules.addNewRule("useKeepVelocity","Always",subBeh);
			
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
						
		subBeh.setParam("speed",params.get("wanderSpeed"));
		subBeh.setParam("RBuffer",params.get("RBuffer"));

	}
	
	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Task3(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Task3", new Task3.Factory());
	}


}