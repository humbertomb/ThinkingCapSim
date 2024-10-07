/*
 * @(#)FollowCorridor.java		1.0 2004/02/09
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus.behs;

import java.util.HashMap;
import java.io.IOException;

import tc.shared.lps.lpo.*;
import tclib.behaviours.fhb.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;
import wucore.utils.geom.Point2;



public class FollowCorridor extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour keepVel;
	
	
	public String getName() {
		return "FollowCorridor";
	}

	/*
	 * This method is used to find the distance of the nearest object that is situated in the
	 * rectangle area (x0,0)->(x1,range)
	 */
	private Point2 getNearestObstacle(LPORangeBuffer sensors, double x0, double x1, double range) {
		
		LPORangePoint[] buffer = sensors.buffer();
		LPORangePoint	s;
		Point2 nearestObj = new Point2(x1,range);
		
		for (int i = 0; i < sensors.getSize(); i++) 
		{
			s = buffer[i];
			if (s.active())
			{	
				if ((range >= 0) && (s.y() >= 0)) {
					if ((s.x() >= x0) && (s.x() <= x1) && (s.y() < range) && (nearestObj.y() > s.y())) { 
						nearestObj.x(s.x());
						nearestObj.y(s.y());
					}
				} else if ((range < 0) && (s.y() < 0)) {
					if ((s.x() >= x0) && (s.x() <= x1) && (s.y() > range) && (nearestObj.y() < s.y())) {
						nearestObj.x(s.x());
						nearestObj.y(s.y());
					}
				}
			}
		}
		
		return nearestObj;
	}
	
	protected void createRules() {
		
		Histogram turnLeft = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnLeft.setYValue(ControlVariables.TURN_L,1);
		
		Histogram turnRight = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnRight.setYValue(ControlVariables.TURN_R,1);
		
		Histogram turnAhead = cv.getDefaultOutputFSet(ControlVariables.ROTATION);
		turnAhead.setYValue(ControlVariables.TURN_Z,1);
		
		keepVel = BehaviourFactory.createBehaviour("KeepVelocity",false);
		
		try {
			
			rules.addNewRule("angled_right","(AND AngledRight (NOT (OR OnRight OnLeft)))",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("angled_left","(AND AngledLeft (NOT (OR OnRight OnLeft)))",
					ControlVariables.ROTATION,turnRight);
			
			rules.addNewRule("aligned","(AND Aligned (NOT (OR OnRight OnLeft)))",
					ControlVariables.ROTATION,turnAhead);
			
			rules.addNewRule("on_left","(AND OnLeft (NOT AngledRight))",
					ControlVariables.ROTATION,turnRight);
			
			rules.addNewRule("on_right","(AND OnRight (NOT AngledLeft))",
					ControlVariables.ROTATION,turnLeft);
			
			rules.addNewRule("go","Always", keepVel);
			
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
		double x0, x1, range,alpha;
		double angledLeft, angledRight, aligned;
		double offset, width;
		double onLeft, onRight;
		Point2 point_fr; // nearest obstacle point on front right
		Point2 point_fl; // nearest obstacle point on front left
		Point2 point_br; // nearest obstacle point on back right
		Point2 point_bl; // nearest obstacle point on back left
		
		LPORangeBuffer sensors = (LPORangeBuffer) params.get("RBuffer");
		keepVel.setParam("speed",params.get("followSpeed"));
		keepVel.setParam("RBuffer",sensors);
		
		/* update the parameters used by this behaviour. This information is used to
		 * by the Debug Window to display the objects used by the behaviour.
		 */
		parameters.clear();
		parameters.add(sensors.label());
		
		x0 = 0.1;	
		x1 = 0.6;
		range = 4;
		/*
		 * Finds the position of the nearest obstacle in 4 areas: front left, front right, back left, back right.
		 */
		point_fl = getNearestObstacle(sensors,x0,x1,range);
		point_fr = getNearestObstacle(sensors,x0,x1,-range);
		point_bl = getNearestObstacle(sensors,-x1,-x0,range);
		point_br = getNearestObstacle(sensors,-x1,-x0,-range);
		
		/*
		 * In order to calculate the direction of the corridor, we consider the direction of the nearest border.
		 */
		if ((point_fl.y()+point_bl.y()) <= Math.abs(point_fr.y()+point_br.y()))
			alpha = Math.atan2((point_fl.y()-point_bl.y()),(point_fl.x()-point_bl.x()));
		else
			alpha = Math.atan2((point_fr.y()-point_br.y()),(point_fr.x()-point_br.x()));
		
		/* Calculates how much we have to move to go in the middle of the corridor */
		offset = (point_fl.y()+point_fr.y())/2;
		/* Calculates the corridor width */
		width = (point_fl.y() - point_fr.y());
		
		ramp.type = Ramp.RAMP_UP;
		ramp.setPoints(0,width/2);
		onRight = ramp.dmember(offset);
		onLeft = ramp.dmember(-offset);
		
		ramp.setPoints(0,60);
		angledRight = ramp.dmember(alpha*180/Math.PI);
		angledLeft = ramp.dmember(-alpha*180/Math.PI);
		
		ramp.type = Ramp.RAMP_DOWN;
		ramp.setPoints(0,20);
		aligned = ramp.dmember(Math.abs(alpha*180/Math.PI));

		antecedentValues.setValue("AngledRight",angledRight);
		antecedentValues.setValue("AngledLeft",angledLeft);
		antecedentValues.setValue("Aligned",aligned);
		antecedentValues.setValue("OnRight",onRight);
		antecedentValues.setValue("OnLeft",onLeft);
	}

	

	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new FollowCorridor(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("FollowCorridor", new FollowCorridor.Factory());
	}
	
	
}
