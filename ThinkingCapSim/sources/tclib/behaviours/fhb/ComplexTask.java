/*
 * @(#)ComplexTask.java		1.0 2004/02/28
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.io.IOException;
import java.util.HashMap;

import tc.shared.lps.lpo.*;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tclib.utils.fuzzy.*;


public class ComplexTask extends Behaviour {

	/* Used to calculate the fuzzy predicates values*/
	private Ramp ramp = new Ramp(0,0);
	private Behaviour follow, cross, face, goTo, keepVel;

	
	public String getName() {
		return("ComplexTask");
	}
	
	protected void createRules() {
		
		try {
			
			follow = BehaviourFactory.createBehaviour("FollowCorridor",false);
			rules.addNewRule("followCorr","(AND BeforeDoor (NOT InFrontOfDoor))",follow);
			
			face = BehaviourFactory.createBehaviour("Face",false);
			rules.addNewRule("faceDoor","(AND (AND BeforeDoor InFrontOfDoor) (AND (NOT ThroughTheDoor) (NOT Aligned)))",face);
			
			cross = BehaviourFactory.createBehaviour("Cross",false);
			rules.addNewRule("crossDoor","(AND (AND BeforeDoor InFrontOfDoor) Aligned)",cross);
			
			keepVel = BehaviourFactory.createBehaviour("KeepVelocity",false);
			rules.addNewRule("escapeFromDoor","(AND (NOT BeforeDoor) ThroughTheDoor)",keepVel);
			
			
			goTo = BehaviourFactory.createBehaviour("Task1",false);
			rules.addNewRule("goForward","(AND (NOT BeforeDoor) (NOT ThroughTheDoor))",keepVel);	
			
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
	
	
	protected void update(HashMap params) {	
		double phi0, robotPos[];
		double beforeDoor, inFrontOfDoor, aligned, throughTheDoor;
		double door_width = 0.8;
	
		LPO door = (LPO) params.get("Door4");
		follow.setParam("followSpeed",params.get("followSpeed"));
		follow.setParam("RBuffer",params.get("RBuffer"));
		
		cross.setParam("crossSpeed",params.get("crossSpeed"));
		cross.setParam("RBuffer",params.get("RBuffer"));
		cross.setParam("LPO0",door);
		
		keepVel.setParam("speed",params.get("crossSpeed"));
		keepVel.setParam("RBuffer",params.get("RBuffer"));
		
		face.setParam("LPO0",door);
		LPO target = (LPO) params.get("Goal");
		goTo.setParam("LPO0",target);
		
		robotPos = getRobotPosFromObj(door);
		phi0 = Math.atan2(robotPos[1],robotPos[0])*180/Math.PI;		
//		System.out.println("DEBUG: phi0 is "+phi0);
//		System.out.println("DEBUG: door->phi = "+door.phi()*180/Math.PI);	
//		System.out.println("DEBUG: robotPos[0]->"+robotPos[0]+" robotPos[1]->"+robotPos[1]);
//		System.out.println("DEBUG: door->x="+door.x()+" door->y="+door.y());
//		System.out.println("DEBUG: goal->x="+target.x()+" goal->y="+target.y());
		ramp.type = Ramp.RAMP_UP;
		beforeDoor = ramp.dmember(Math.abs(phi0),90,90);
		ramp.type = Ramp.RAMP_DOWN;
		inFrontOfDoor = ramp.dmember(Math.abs(robotPos[1]),0,door_width/2);
		aligned = ramp.dmember(Math.abs(door.phi()*180/Math.PI),0,20);
		throughTheDoor = ramp.dmember(robotPos[0],0,0.5);
		ramp.type = Ramp.RAMP_UP;
		throughTheDoor = FSet.and(throughTheDoor,ramp.dmember(robotPos[0],0,0));
		
		antecedentValues.setValue("BeforeDoor",beforeDoor);
		antecedentValues.setValue("InFrontOfDoor",inFrontOfDoor);
		antecedentValues.setValue("Aligned",aligned);
		antecedentValues.setValue("ThroughTheDoor",throughTheDoor);
	}
	
	
	private static class Factory extends BehaviourFactory {
		protected Behaviour create() { 
			return new ComplexTask(); 
		}
	}
	
	static {
		BehaviourFactory.addFactory("ComplexTask", new ComplexTask.Factory());
	}


}