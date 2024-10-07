/*
 * @(#)MetricPredicates.java		1.0 2004/03/24
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.util.*;

import tc.shared.lps.lpo.LPO;
import tclib.utils.fuzzy.*;

/**
 * This class provides methods to calculate the metric predicates.
 * 
 * @version 1.0		24 Mar 2004
 * @author Denis Remondini
 */
public class MetricPredicates {
	/*
	 * This class implementation is very simple and has to be improved.
	 */
	
	/* Some functions are implemented using the hypothesis that the robot
	 * has a circular shape. This is the robot's radius.
	 */
	private static final double robotRadius = 0.5;
	/* Fuzzy set used to calculate the predicates */
	private Ramp ramp = new Ramp(0,0);

	/*
	 * This methods gives the position where the object sees the robot.
	 */
	private double[] getRobotPosFromObj(LPO obj) {
		double x, y, th;	// temporary variables
		double x1, y1;		// position where the obj sees the robot
		double pos[] = new double[2];
		
		x = -obj.x();
		y = -obj.y();
		th = obj.alpha();
		x1 = x*Math.cos(th)+y*Math.sin(th);
		y1 = y*Math.cos(th)-x*Math.sin(th);
		
		pos[0] = x1;
		pos[1] = y1;
		
		return pos;
	}
	
	/*
	 * This method returns the width of an LPO object. The implementation has to be
	 * changed when it will be available the information about the type of an LPO: so far
	 * we use the label to distinguish among different kind of LPO.
	 */
	private double getLPOWidth(LPO object) {
		if (object.label().startsWith("Door"))
			return 0.9;
		if (object.label().startsWith("Goal"))
			return 0.5;
		return 3.0;
	}
	
	/*
	 * This method returns the length of an LPO object. The implementation has to be
	 * changed when it will be available the information about the type of an LPO: so far
	 * we use the label to distinguish among different kind of LPO.
	 */
	private double getLPOLength(LPO object) {
		if (object.label().startsWith("Corridor"))
			return 12.0;
		if (object.label().startsWith("Door"))
			return 0.15;
		if (object.label().startsWith("Goal"))
			return 0.5;
		return 3.0;
	}
	
	/*
	 * This function gives the distance between the LPO object and the robot.
	 */
	private double mapElementDistance(LPO object, double radius) {
		double width, length, dx, dy, distance;
		double robotPos[];
		
		width = radius + 0.5 * getLPOWidth(object);
		length = radius + 0.5 * getLPOLength(object);
		robotPos = getRobotPosFromObj(object);
		dx = Math.abs(robotPos[0]) - length;
		dy = Math.abs(robotPos[1]) - width;
		if (dx < 0)
			dx = 0;
		if (dy < 0)
			dy = 0;
		distance = Math.sqrt(dx*dx+dy*dy);
		
		return distance;
	}
	
	/*
	 * Calculates the predicate AT
	 */
	private double at(LPO object) {
		ramp.type = Ramp.RAMP_DOWN;
		return ramp.dmember(mapElementDistance(object,0),0,robotRadius);
	}
	
	/*
	 * Calculates the predicate NEAR
	 */
	private double near(LPO object) {
		ramp.type = Ramp.RAMP_DOWN;
		return ramp.dmember(mapElementDistance(object,0),robotRadius*3/2,robotRadius*3);
	}
	
	/*
	 * Calculates the predicate ORIENTED
	 */
	private double oriented(LPO object) {
		ramp.type = Ramp.RAMP_DOWN;
		return ramp.dmember(object.phi(),5*Math.PI/180,20*Math.PI/180);
	}
	
	/**
	 * This methods calculates the truth value of a metric predicate
	 * @param pred the name of the metric predicate
	 * @param params parameters needed to calculate the truth value of the metric predicate
	 * @return the truth value of the metric predicate
	 */
	public double calculate(String pred, Vector params) {
		double res = 0;
		
		if (pred.equals("AT"))
			res = at((LPO)params.get(0));
		else if (pred.equals("NEAR"))
			res = near((LPO)params.get(0));
		else if (pred.equals("ORIENTED"))
			res = oriented((LPO)params.get(0));
		
		return res;
	}
	
	/**
	 * This methods tell you if a predicate is a metric predicate or not
	 * @param pred the name of the predicate
	 * @return true if the predicate is a metric predicate, false otherwise
	 */
	public static boolean isMetricPred(String pred) {
		if ((pred.equals("AT")) || (pred.equals("NEAR")) || (pred.equals("ORIENTED")))
			return true;
		
		return false;			
	}
	
	/**
	 * This methods returns the arity of a metric predicate
	 * @return the arity of a metric predicate. If the predicate is not a metric one, it 
	 * 			returns -1.
	 */
	public static int getArity(String pred) {
		if ((pred.equals("AT")) || (pred.equals("NEAR")) || (pred.equals("ORIENTED")))
			return 1;
		return -1;
	}
}
