/*
 * @(#)MainBehaviour.java		1.0 2003/11/28
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.umu.indoor.behs;

import java.util.*;


import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

public class MainBehaviour extends Behaviour 
{
	// Behaviours
	protected Behaviour[]		subBeh;

	// Fuzzy predicates
	private Ramp				indanger	= new Ramp (Ramp.RAMP_DOWN, 0.25, 0.55);

	public String getName()
	{
		return "MainBehaviour";
	}
	
	protected void createRules()
	{
		subBeh		= new Behaviour[2];
		
		try
		{	
			subBeh[0] = BehaviourFactory.createBehaviour ("AvoidCollision",false);
			rules.addNewRule ("AvoidCollision", "inDanger", subBeh[0]);
			
//			subBeh[1] = BehaviourFactory.createBehaviour ("ComplexTask",false);
//			rules.addNewRule ("TaskBehaviour", "(NOT inDanger)", subBeh[1]);

			subBeh[1] = BehaviourFactory.createBehaviour ("FollowPath",false);
			rules.addNewRule ("FollowPath", "(NOT inDanger)", subBeh[1]);
		}
		catch (Exception e) { e.printStackTrace (); }
	}

	protected void update (HashMap params) 
	{
		int				i;
		int				groups;
		double			minrange, tmp;
		
		// Send current parameters to all sub-behaviours
		for (i = 0; i < subBeh.length; i++)
			subBeh[i].setParams (params);
		
		// Get the minimum distance to objects (virtual sensors)
		minrange	= Double.MAX_VALUE;
		groups		= (int) ((Double) params.get ("Groups")).doubleValue ();
		for (i = 0; i < groups; i++)
		{
			tmp			= ((Double) params.get ("Group"+i)).doubleValue ();
			if (tmp < minrange)
				minrange	= tmp;
		}
			
		antecedentValues.setValue ("inDanger", indanger.dmember (minrange));
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