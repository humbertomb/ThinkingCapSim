/*
 * @(#)Cross.java		1.0 2004/2/25
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.util.HashMap;
import java.io.IOException;

import tc.shared.lps.lpo.*;
import tc.vrobot.*;
import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;

/**
 * 
 * @author Denis Remondini
 */
public class Cross extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour subBeh;
	
	public String getName() {
		return "Cross";
	}

	protected void createRules() {
		Histogram turnLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnLeft.setYValue(ControlVariables.TURN_SL,1);
		
		Histogram turnRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnRight.setYValue(ControlVariables.TURN_SR,1);
		
		Histogram turnStraight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnStraight.setYValue(ControlVariables.TURN_Z,1);
		
		subBeh = BehaviourFactory.createBehaviour("KeepVelocity",false);
		
			
		try {
			
			rules.addNewRule("cross_Speed","Always",subBeh);
			
			rules.addNewRule("AdjustLeft","(AND AngledRight LeftClear)",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("AdjustRight","(AND AngledLeft RightClear)",
					ControlVariables.ROTATION,turnRight);
			
			rules.addNewRule("AdjustStraight","(AND (NOT AngledRight) (NOT AngledLeft))",
					ControlVariables.ROTATION,turnStraight);
			
					
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
		
		final double CollisionSideMax 	= 3;
		double angledRight, angledLeft, leftClear, rightClear;
		double alpha, leftClearance, rightClearance;
		LPO door;
		
		subBeh.setParam("speed",params.get("crossSpeed"));
		subBeh.setParam("RBuffer",params.get("RBuffer"));
		LPORangeBuffer sensors = (LPORangeBuffer) params.get("RBuffer");
		door = (LPO) params.get("LPO0");
		
		/* update the parameters used by this behaviour. This information is used to
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(door.label());
//		System.out.println("DEBUG: cross lpo = "+door.label());
		FeaturePos f = new FeaturePos();
		/* get the distance from the nearest object in the left area (40,80 degrees)*/
		f.set_xy(0,0,60*Math.PI/180);
		leftClearance = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
		
		
		/* get the distance from the nearest object in the right area (-40,-80 degrees) */
		f.set_xy(0,0,-60*Math.PI/180);
		rightClearance = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
			
		alpha = door.alpha()*180/Math.PI;
//		System.out.println("DEBUG: door alpha->"+alpha);
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(0,30);
		angledRight = ramp.dmember(alpha);
		angledLeft = ramp.dmember(-alpha);
		/* 0.18 is the half width of rasmus */
		ramp.setPoints(0.18,0.3);
		leftClear = ramp.dmember(leftClearance);
		rightClear = ramp.dmember(rightClearance);
	
		antecedentValues.setValue("AngledRight",angledRight);
		antecedentValues.setValue("AngledLeft",angledLeft);
		antecedentValues.setValue("RightClear",rightClear);
		antecedentValues.setValue("LeftClear",leftClear);
	}

	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new Cross(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("Cross", new Cross.Factory());
	}
}
