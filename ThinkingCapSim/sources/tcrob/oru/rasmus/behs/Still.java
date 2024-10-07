/*
 * @(#)Still.java		1.0 Feb 6, 2004
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.util.HashMap;
import java.io.IOException;

import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;

/**
 * @author Denis Remondini
 *
 */
public class Still extends Behaviour {

	public String getName() {
		return "Still";
	}

	
	protected void createRules() {
		Histogram stay = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		stay.setYValue(ControlVariables.SPEED_Z,1);
		
		Histogram turnStraight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnStraight.setYValue(ControlVariables.TURN_Z,1);
		
		try {
			rules.addNewRule("goStay","Always",ControlVariables.SPEED,stay);
			
			rules.addNewRule("turnStraight","Always",ControlVariables.ROTATION,turnStraight);
			
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
		
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Still(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Still", new Still.Factory());
	}
	
	
}
