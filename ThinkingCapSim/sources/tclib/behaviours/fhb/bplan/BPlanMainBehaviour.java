/*
 * @(#)BPlanMainBehaviour.java		1.0 2004/03/24
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb.bplan;

import java.io.IOException;
import java.util.HashMap;

import tc.shared.lps.lpo.*;
import tc.vrobot.*;
import tclib.behaviours.fhb.Behaviour;
import tclib.behaviours.fhb.BehaviourFactory;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;

/**
 * This class implements the main behaviour that is used when the controller want
 * to execute a BPlan.
 * 
 * @version 1.0		24 Mar 2004
 * @author Denis Remondini
 */
public class BPlanMainBehaviour extends Behaviour {

	/* Used to calculate the fuzzy predicates values */
	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh;
	private BPlanTaskBehaviour subBeh2;

	/**
	 * Initializes the behaviour
	 */
	public BPlanMainBehaviour() {
		super();
	}
	
	/**
	 * Returns the name of the behaviour
	 * 
	 * @return the name of the behaviour
	 */
	public String getName() {
		return("BPlanMainBehaviour");
	}
	
	/* Not used in this behaviour (see below) */
	protected void createRules() {
		// nothing because it will never used!
	}

	/**
	 * Creates the rule for the behaviour, using the information about the BPlan
	 * 
	 * @param data information about the BPlan
	 */
	public void createRules(BPlanData data) {
		
		try {
			/* creates the avoid obstacle behaviour */
			subBeh = BehaviourFactory.createBehaviour("AvoidCollision",false);
			rules.addNewRule("useAvoidCollision","alpha",subBeh);
			
			/* creates the behaviour that execute the BPlan */
			subBeh2 = new BPlanTaskBehaviour();
			subBeh2.createRules(data);
			rules.addNewRule("useTaskBehaviour","(NOT alpha)",subBeh2);
			
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
	
	/*
	 * Updates the predicates used by the behaviour rules
	 */
	protected void update(HashMap params) {
		
		/* The biggest problem is how to choose good values for these variables */
		final double CollisionSideMax 	= 0.8;
		final double CollisionFrontMax 	= 0.8;
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

}