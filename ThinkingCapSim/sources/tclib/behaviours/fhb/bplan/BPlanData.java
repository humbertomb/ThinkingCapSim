/*
 * @(#)BPlanData.java		1.0 2004/03/24
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb.bplan;

import java.util.*;

import tclib.behaviours.fhb.MetricPredInfo;

/**
 * This class contains all the information about the context rules included in a BPlan.
 * 
 * @version 1.0		24 Mar 2004
 * @author Denis Remondini
 */
public class BPlanData {

	/* List of behaviours, one for each context rule */
	private Vector behaviours;
	/* List of antecedents, one for each context rule */
	private Vector antecedents;
	/* List of parameters relative to the goal of a behaviour */
	private Vector goalParameters;
	/* List of parameters that are not relative to the goal of a behaviour  */
	private Vector otherParameters;
	/* List of information about metric predicates */
	private Vector predicatesData;
	
	/**
	 * Initializes the class
	 */
	public BPlanData() {
		behaviours = new Vector();
		antecedents = new Vector();
		goalParameters = new Vector();
		otherParameters = new Vector();
		predicatesData = new Vector();
	}
	
	/**
	 * Adds a behaviour name to the list of behaviour used by the context rules
	 * @param nameBehaviour the name of the behaviour
	 */
	public void addBehaviour(String nameBehaviour) {
		behaviours.addElement(nameBehaviour);
	}
	
	/**
	 * Adds an antecedent to the list of antecedents used by teh context rules
	 * @param antecedent the formula of the context rule antecedent
	 */
	public void addAntecedent(String antecedent) {
		antecedents.addElement(antecedent);
	}
	
	/**
	 * Adds the parameters relative to the goal of a behaviour
	 * @param params list of parameters
	 */
	public void addGoalParameters(Vector params) {
		goalParameters.addElement(params);
	}
	
	/**
	 * Adds the parameters not relative to the goal of a behaviour
	 * @param params list of parameters
	 */
	public void addOtherParameters(HashMap params) {
		otherParameters.addElement(params);
	}
	
	/**
	 * Adds the information about a metric predicate used by the context rules
	 * @param data the information about the metric predicate
	 */
	public void addPredicatesData(MetricPredInfo data) {
		predicatesData.addElement(data);
	}
	
	/**
	 * Returns the name of the behaviour used by the n-th context rule
	 * @param n the number of the context rule
	 * @return the name of the behaviour, or null if n is not correct.
	 */
	public String getBehaviourName(int n) {
		if ((n >= 0) && (n < behaviours.size()))
			return (String) behaviours.get(n);
		else
			return null;				
	}
	
	/**
	 * Returns the antecedent of the n-th context rule
	 * @param n the number of the context rule
	 * @return the antecedent formula, or null if n is not correct.
	 */
	public String getAntecedent(int n) {
		if ((n >= 0) && (n < antecedents.size()))
			return (String) antecedents.get(n);
		else
			return null;	
	}
	
	/**
	 * Returns the list of the metric predicates used by context rules
	 * @return the list of metric predicates
	 */
	public Vector getPredicatesData() {
		return predicatesData;
	}
	
	/**
	 * Returns the parameters relative to the goal of the behaviour used by the n-th
	 * context rule.
	 * @param n the number of the context rule
	 * @return the list of parameters, or null if n is not correct.
	 */
	public Vector getBehGoalParameters(int n) {
		if ((n >= 0) && (n < goalParameters.size()))
			return (Vector) goalParameters.get(n);
		
		return null;
	}
	
	/**
	 * Returns the map of the parameters not relative to the goal of the behaviour used
	 * bye the n-th context rule.
	 * @param n the number of the context rule
	 * @return the map of parameters, or null if n is not correct.
	 */
	public HashMap getBehOtherParameters(int n) {
		if ((n >= 0) && (n < otherParameters.size()))
			return (HashMap) otherParameters.get(n);
		
		return null;
	}
	
	/**
	 * Returns the number of context rules included in the BPlan
	 * @return the number of context rules included in the BPlan
	 */
	public int getRulesNumber() {
		return behaviours.size();
	}
	
}
