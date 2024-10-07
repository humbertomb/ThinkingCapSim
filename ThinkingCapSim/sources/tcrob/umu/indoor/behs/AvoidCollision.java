/*
 * @(#)AvoidCollision.java		1.0 2003/11/28
 * 
 * (c) 2004 Humberto Martinez
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;


import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

public class AvoidCollision extends Behaviour 
{
	// Behaviours
	protected Behaviour[]		subBeh;

	// Fuzzy predicates
	private Ramp				inDanger	= new Ramp (Ramp.RAMP_DOWN, 0.25, 0.60);

	public String getName()
	{
		return "AvoidCollision";
	}
	
	protected void createRules()
	{
		subBeh		= new Behaviour[4];
		
		try
		{	
			subBeh[0] = BehaviourFactory.createBehaviour ("AvoidLeft");
			rules.addNewRule ("AvoidLeft", "dangerLeft", subBeh[0]);
			
			subBeh[1] = BehaviourFactory.createBehaviour ("AvoidRight");
			rules.addNewRule ("AvoidRight", "dangerRight", subBeh[1]);
			
			subBeh[2] = BehaviourFactory.createBehaviour ("AvoidFront");
			rules.addNewRule ("AvoidFront", "dangerFront", subBeh[2]);
			
			subBeh[3] = BehaviourFactory.createBehaviour ("Escape");
			rules.addNewRule ("Escape", "(AND (AND dangerLeft dangerRight) dangerFront)", subBeh[3]);
		}
		catch (Exception e) { e.printStackTrace (); }
	}

	protected void update (HashMap params) 
	{
		int				i;
		double			left1, left2;
		double			right1, right2;
		double			left, front, right;
		
		// Send current parameters to all sub-behaviours
		for (i = 0; i < subBeh.length; i++)
			subBeh[i].setParams (params);
					
		// Fuzzy predicates
		left1	= ((Double) params.get ("Group0")).doubleValue ();
		left2	= ((Double) params.get ("Group1")).doubleValue ();
		left	= Math.min (left1, left2);
		
		front	= ((Double) params.get ("Group2")).doubleValue ();
		
		right1	= ((Double) params.get ("Group3")).doubleValue ();
		right2	= ((Double) params.get ("Group4")).doubleValue ();
		right	= Math.min (right1, right2);
		
		antecedentValues.setValue ("dangerLeft", inDanger.dmember (left));
		antecedentValues.setValue ("dangerFront", inDanger.dmember (front));
		antecedentValues.setValue ("dangerRight", inDanger.dmember (right));
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