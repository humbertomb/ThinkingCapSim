/*
 * @(#)Behaviour.java		1.1 2004/02/09
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import tclib.behaviours.fhb.exceptions.*;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * An abstract class that defines the common functions of a behaviour.
 * 
 * @version	1.1		09 Feb 2004
 * @author 	Denis Remondini
 */
public abstract class Behaviour {
	/* If you want to use the BehaviourFactory class to create a behaviour you 
	 * have to add in your behaviour class a piece of code like this one:
	 * 
	 * private static class Factory extends BehaviourFactory {
	 *   protected Behaviour create() { 
	 *  	return new <NameOfYourBehaviourClass>();
	 *	 }
	 *  }
	 * 
	 * static {
	 * 		BehaviourFactory.addFactory("<NameOfYourBehaviourClass>", 
	 *                                  new <NameOfYourBehaviourClass>.Factory());
	 * }
	 * 
	 */
	
	
	/* Stores the fuzzy predicates and their values */
	protected DoubleMap antecedentValues = new DoubleMap();
	/* Stores the rules of the behaviour */
	protected RuleSet rules;
	/* Contains the default information about the control variables */
	protected ControlVariables cv;
	/* Contains the information about the control variables after the execution
	 * of the behaviour
	 */
	protected ControlVariables outputFSets;
	/* Contains the parameters used by the behaviour */
	protected HashMap params;
	/* Contains the names of the parameters used in the behaviour */
	protected ArrayList parameters;
	
	/**
	 * Returns the name of the behaviour class.
	 * @return the name of the behaviour class.
	 */
	public 		abstract String getName();
	
	/**
	 * Creates the fuzzy rules used in the behaviour
	 */
	protected 	abstract void createRules();

	/**
	 * Updates the fuzzy predicates used in the behaviour
	 * @param params map that contain the object the behaviour needs.
	 */
	protected	abstract void update(HashMap params);
	
	/**
	 * Constructs a behaviour creating the fuzzy rules
	 */
	protected Behaviour() {
		/* Variables Initialization */
		rules = new RuleSet();
		cv = new ControlVariables();
		outputFSets = null;
		parameters = new ArrayList();
		params = new HashMap();
		/* Creates the fuzzy rules */
		createRules();
	}
	
	/**
	 * Returns a map that contains the fuzzy predicates (and their values) used
	 * in the fuzzy rules antecedent evaluation
	 * @return the map with the fuzzy predicates and their values
	 */
	public DoubleMap getAntecedentValues() {
		return antecedentValues;
	}
	
	/**
	 * Sets the parameters for the behaviour
	 * @param params a parameters map
	 */
	public void setParams(HashMap params) {
		this.params = (HashMap) params.clone();
	}
	
	/**
	 * Sets a parameter used by the behaviour
	 * @param key parameter's name
	 * @param value parameter's value
	 */
	public void setParam(Object key, Object value) {
		params.put(key,value);
	}
	
	/**
	 * Returns the parameters names used by the behaviour to calculate the fuzzy predicates
	 * @return a list of the parameters names used by the behaviour.
	 */
	public ArrayList getParameters() {
		return parameters;
	}
	
	/**
	 * Returns the set of the rules used in the behaviour
	 * @return the set of the rules used in the behaviour
	 */
	public RuleSet getRuleSet() {
		return rules;
	}
	
	/**
	 * Make an evaluation of the fuzzy rules used in the behaviour
	 * @return the informations about the control variables
	 * @throws RuleException if there are some errors in the rules evaluation
	 */
	protected ControlVariables evalRules() throws RuleException {
		outputFSets = rules.evalRules(antecedentValues);
		return outputFSets;
	}
	
	/**
	 * Executes the behaviour
	 * @return the informations about the control variables
	 * @throws RuleException if there are some errors in the rules evaluation
	 */
	public ControlVariables exec() throws RuleException {
		/* Updates the fuzzy predicates used in the rules */
		update(params);
		
		/* Evaluates the behaviour rules */
		return evalRules();
	}
	
	/**
	 * Returns the informations about the control variables
	 * @return the informations about the control variables
	 */
	public ControlVariables getOutputFSets() {
		return outputFSets;
	}
	
}
