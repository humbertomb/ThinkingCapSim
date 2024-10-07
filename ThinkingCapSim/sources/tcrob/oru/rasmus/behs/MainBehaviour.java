/*
 * @(#)MainBehaviour.java		1.0 2003/11/28
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
import tc.shared.lps.lpo.*;
import tc.vrobot.*;


public class MainBehaviour extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh, subBeh2;

	
	public String getName() {
		return("MainBehaviour");
	}
	
	protected void createRules() {
		
		try {
			
			subBeh = BehaviourFactory.createBehaviour("AvoidCollision",false);
			rules.addNewRule("AvoidCollision","alpha",subBeh);
			
			subBeh2 = BehaviourFactory.createBehaviour("ComplexTask",false);
			rules.addNewRule("TaskBehaviour","(NOT alpha)",subBeh2);
			
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
		
		/* The biggest problem is how to choose good values for these variables */
		final double CollisionSideMax 	= 1.0; //this is for a speed of 0.3; for speed=0.2 is enough 0.8
		final double CollisionFrontMax 	= 1.0; //this is for a speed of 0.3; for speed=0.2 is enough 0.8
		final double CollisionMin		= 0.2;		  
		
		double alpha;
		double minrange,tmprange;
		
		subBeh.setParams(params);
		subBeh2.setParams(params);
		
		LPORangeBuffer sensors = (LPORangeBuffer) params.get("RBuffer");
		
		/* Checks the distance of the nearest object around the robot */
		FeaturePos f = new FeaturePos();
		f.set_xy(0,0,20*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionFrontMax,false);
		minrange = tmprange;
		
		f.set_xy(0,0,-20*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionFrontMax,false);
		if (tmprange < minrange)
			minrange = tmprange;
		

		f.set_xy(0,0,60*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
		if (tmprange < minrange)
			minrange = tmprange;
		
		
		f.set_xy(0,0,-60*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
		if (tmprange < minrange)
			minrange = tmprange;
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(CollisionMin,CollisionSideMax);
		alpha = ramp.dmember(minrange);
		
		antecedentValues.setValue("alpha",alpha);
	}
	
		
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new MainBehaviour(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("MainBehaviour", new MainBehaviour.Factory());
	}


}