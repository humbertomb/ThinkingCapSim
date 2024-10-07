/*
 * @(#)AvoidCollision.java		1.0 2004/01/29
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

public class AvoidCollision extends Behaviour {
	
	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	
	
	public String getName() {
		return "AvoidCollision";
	}
	
	protected void createRules() {
		
		Histogram turnLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnLeft.setYValue(ControlVariables.TURN_L,1);

		Histogram turnRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnRight.setYValue(ControlVariables.TURN_R,1);
		
		Histogram turnSmoothLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSmoothLeft.setYValue(ControlVariables.TURN_SL,1);
			
		Histogram turnStraight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnStraight.setYValue(ControlVariables.TURN_Z,1);
		
		Histogram turnSmoothRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnSmoothRight.setYValue(ControlVariables.TURN_SR,1);
		
		Histogram goSlow = cv.getDefaultOutputFSet(ControlVariables.SPEED);
		goSlow.setYValue(ControlVariables.SPEED_Z,1);
				
		
		try {
	
			rules.addNewRule("danger_Right","(AND DangerRight LeftClear)",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("danger_Left","(AND DangerLeft RightClear)",
					ControlVariables.ROTATION,turnRight);
		
			rules.addNewRule("danger_Straight","(AND LeftFreeMore (AND (EQL RightClear LeftClear 0.2) (NOT FrontClear)))",
					ControlVariables.ROTATION,turnSmoothLeft);
		
			rules.addNewRule("danger_Straight2","(AND (NOT LeftFreeMore) (AND (EQL RightClear LeftClear 0.2) (NOT FrontClear)))",
					ControlVariables.ROTATION,turnSmoothRight);
						
			rules.addNewRule("Clear_Front","(AND (EQL (AND DangerLeft RightClear) (AND DangerRight LeftClear) 0.15) FrontClear)",
					ControlVariables.ROTATION,turnStraight);


			/*
			 * If we use the (NOT (AND (AND LeftClear RightClear) FrontClear)) condition, there are
			 * some cases where the avoid collision behaviour blending with GoTo and Face behaviour
			 * produces a still situation. How to fix that? The always condition is not the solution
			 * because the robot is always moving very slow. 
			 */			
			rules.addNewRule("Brake","(OR 0.0 (NOT (AND (AND LeftClear RightClear) FrontClear)))",
					ControlVariables.SPEED,goSlow);
			
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
		
		/* The biggest problem is how to choose good values for these variables */
		final double CollisionSideMin 	= 0.5;			/* side minimum dist, m */
		final double CollisionSideMax 	= 1.8;
		final double CollisionFrontMin 	= 0.5;		    /* front minimum dist, m */
		final double CollisionFrontMax 	= 1.8;		   
		
		double lstand, rstand, fstand;					/* standoffs */
		double llook, rlook, flook;		    			/* lookahead */
		double lrange, rrange, flrange, frrange;	    /* min ranges on L, R, front L, front R */
		double tmprange;
		double LeftFreeMore, DangerLeft, DangerRight, DangerFrontLeft, DangerFrontRight, LeftClear, RightClear, FrontClear;
		
		lrange = lstand = llook = rrange = rstand = rlook = flrange = flook = frrange = 0;
		
		
		LPORangeBuffer sensors = (LPORangeBuffer) params.get("RBuffer");
		
		/* update the parameters used by this behaviour. This information is used to
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(sensors.label());
		
		FeaturePos f = new FeaturePos();
		
		/* get the distance from the nearest object in the front left area (0,40 degrees)*/
		f.set_xy(0,0,20*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionFrontMax,false);
		flrange = tmprange;
	
		/* get the distance from the nearest object in the front right area (0,-40 degrees)*/
		f.set_xy(0,0,-20*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionFrontMax,false);
		frrange = tmprange;
				
		/* get the distance from the nearest object in the left area (40,80 degrees)*/
		f.set_xy(0,0,60*Math.PI/180);
		tmprange = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
		lrange = tmprange;
	
		/* get the distance from the nearest object in the right area (-40,-80 degrees)*/
		f.set_xy(0,0,-60*Math.PI/180);
		rrange = sensors.occupied_arc(f,20*Math.PI/180,CollisionSideMax,false);
		lstand = CollisionSideMin;
		llook = CollisionSideMax;
		rstand = CollisionSideMin;
		rlook = CollisionSideMax;
		fstand = CollisionFrontMin;
		flook = CollisionFrontMax;
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(lstand,llook);
		DangerLeft     = ramp.dmember(lrange);
		ramp.setPoints(rstand,rlook);
		DangerRight    = ramp.dmember(rrange);
		ramp.setPoints(fstand,flook);
		DangerFrontLeft = ramp.dmember(flrange);
		ramp.setPoints(fstand,flook);
		DangerFrontRight = ramp.dmember(frrange);
		
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(lstand,llook/2);
		LeftClear = ramp.dmember(lrange);
		
		ramp.setPoints(rstand,rlook/2);
		RightClear   = ramp.dmember(rrange);
		FrontClear   = 1 - Math.max(DangerFrontLeft, DangerFrontRight);
		if (LeftClear > RightClear)
			LeftFreeMore = 1;
		else
			LeftFreeMore = 0;
		
		antecedentValues.setValue("LeftFreeMore",LeftFreeMore);
		antecedentValues.setValue("DangerLeft",DangerLeft);
		antecedentValues.setValue("DangerRight",DangerRight);
		antecedentValues.setValue("LeftClear",LeftClear);
		antecedentValues.setValue("RightClear",RightClear);
		antecedentValues.setValue("FrontClear",FrontClear);				
	}

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new AvoidCollision(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("AvoidCollision", new AvoidCollision.Factory());
	}
	
}
