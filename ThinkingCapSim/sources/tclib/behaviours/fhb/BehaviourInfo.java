/*
 * @(#)BehaviourInformation.java		1.1 2004/01/29
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb;


import java.io.Serializable;
import java.lang.IndexOutOfBoundsException;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * This class contains the behaviour information needed to have a good vision of the whole
 * behaviour. This information is useful when one creates a new behaviour and he want to debug it.
 * 
 * @version	1.1		29 Jan 2004
 * @author 	Denis Remondini
 */
public class BehaviourInfo implements Serializable {

	/* The behaviour name */
	private String behName;
	/* The map with the fuzzy predicates (used in the antecedents of the rules)and their values */
	private DoubleMap antecedentPredicates;
	/* The map of the behaviour's rules */
	private HashMap rulesList;
	/* The list containing the names of the rules */
	private ArrayList rulesNames;
	/* The map containing the parameters used by the rule sub-behaviour */
	private HashMap rulesParameters;
	/* The output fuzzy sets of the behaviour */
	private ControlVariables finalOutputFSets;
	/* The max value of the antecedents of the rules */
	private double maxAntecedentValue;
	private double[] valuesSentToRobot;
	
	/**
	 * Constructs an empty structure to contain information about a behaviour 
	 * @param behName the behaviour name
	 */
	public BehaviourInfo(String behName) {
		/* Initializes the max value of the antecedents. Each rule's antecedent 
		 * value is in the [0,1] interval 
		 */
		maxAntecedentValue = -1; 
		this.behName = behName;
		rulesList = new HashMap();
		rulesNames = new ArrayList();
		rulesParameters = new HashMap();
	}
	
	/**
	 * Sets the fuzzy predicates used in the rules antecedents evaluation
	 * @param antPredicates map containing the fuzzy predicates and their values
	 */
	public void setAntecedentPredicates(DoubleMap antPredicates) {
		antecedentPredicates = antPredicates;
	}
	
	/**
	 * Sets the output fuzzy sets of the behaviour
	 * @param outputFSets the output fuzzy sets
	 */
	public void setOutputFSets(ControlVariables outputFSets)  {
		finalOutputFSets = outputFSets;
	}
	
	/**
	 * Sets the values sent to the robot
	 * @param crispValues the values sent to the robot
	 */
	public void setValuesSentToRobot(double[] crispValues) {
		valuesSentToRobot = crispValues;
	}
	
	/**
	 * Returns the values sent to the robot
	 * @return the values sent to the robot
	 */
	public double[] getValuesSentToRobot() {
		return valuesSentToRobot;
	}
	
	/**
	 * Adds information about a behaviour rule
	 * @param ruleName the name of the rule
	 * @param antValue the antecedent value
	 * @param outputFSets the rule output fuzzy sets
	 * @param subBehaviourName the sub-behaviour associated to the rule (it can be null)
	 * @param parameters the parameters used by the sub-behaviour (it can be null)
	 */
	public void addRule(String ruleName, double antValue, ControlVariables outputFSets,
						String subBehaviourName, ArrayList parameters)  {
		
		/* Creates the information about the rule */
		RuleInformation rule = new RuleInformation(ruleName,antValue,outputFSets,
				subBehaviourName,parameters);
		/* if the rule name is already int the set, the associated rule will be overwritten */
		if (!rulesNames.contains(ruleName))
			rulesNames.add(ruleName);
		rulesList.put(ruleName,rule);
		/* Sets the parameters associated to the rule sub-behaviour */
		rulesParameters.put(ruleName,parameters);
	}
	
	/**
	 * Returns the behaviour name
	 * @return the behaviour name
	 */
	public String getName() {
		return this.behName;
	}
	
	/**
	 * Returns the map containing the fuzzy predicates used to the antecedents evaluation
	 * @return the map containing the fuzzy predicates and their values
	 */
	public DoubleMap getAntecedentPredicates() {
		return this.antecedentPredicates;
	}
	
	/**
	 * Returns the number of the behaviour rules
	 * @return the number of the behaviour rules
	 */
	public int getRulesNumber() {
		return this.rulesNames.size();
	}
	
	/**
	 * Returns the parameters used by the sub-behaviour associated to the rule
	 * @param ruleNumber the number of the behaviour rule
	 * @return the parameters used by the sub-behaviour associated to the rule
	 * @throws IndexOutOfBoundsException if the requested rule doesn't exist
	 */
	public ArrayList getRuleParameters(int ruleNumber) throws IndexOutOfBoundsException {
		String ruleName = (String) rulesNames.get(ruleNumber);
		return (ArrayList) rulesParameters.get(ruleName);
	}
	
	/**
	 * Returns the name of a behaviour rule
	 * @param ruleNumber the number of the behaviour rule
	 * @return the name of the behaviour rule
	 * @throws IndexOutOfBoundsException if the requested rule doesn't exists
	 */
	public String getRuleName(int ruleNumber) throws IndexOutOfBoundsException {
		return (String) rulesNames.get(ruleNumber);
	}
	
	/**
	 * Returns the value of a rule's antecedent
	 * @param ruleNumber the number of the behaviour rule
	 * @return the value of the rule antecedent
	 * @throws IndexOutOfBoundsException if the requested rule doesn't exist
	 */
	public double getRuleAntecedentValue(int ruleNumber) throws IndexOutOfBoundsException {
		String ruleName = (String) rulesNames.get(ruleNumber);
		return ((RuleInformation) rulesList.get(ruleName)).getAntecedentValue();
	}
	
	/**
	 * Returns the max value of the antecedents or -1 if there is an error.
	 * @return the max value of the antecedents or -1 if there is an error.
	 */
	public double getMaxAntecedentValue() {
		double value;
		
		maxAntecedentValue = -1;
		for (int i = 0; i < this.getRulesNumber(); i++) {
			try {
			  value = getRuleAntecedentValue(i);
			  if (value > maxAntecedentValue)
			  	maxAntecedentValue = value;
			}
			catch (IndexOutOfBoundsException e) {
				return -1;
			}
		}
		return maxAntecedentValue;
	}
	
	/**
	 * Returns the output fuzzy sets associated to a behaviour rule
	 * @param ruleNumber the number of the behaviour rule
	 * @return the output fuzzy sets associated to the behaviour rule
	 * @throws IndexOutOfBoundsException if the requested rule doesn't exist
	 */
	public ControlVariables getRuleOutputFSets(int ruleNumber) throws IndexOutOfBoundsException {
		String ruleName = (String) rulesNames.get(ruleNumber);
		return ((RuleInformation) rulesList.get(ruleName)).getOutputFSets();
	}

	/**
	 * Returns the sub-behaviour associated to a behaviour rule
	 * @param ruleNumber the number of the behaviour rule
	 * @return the sub-behaviour associated to the behaviour rule or null if there is 
	 * 		   no sub-behaviour associated.
	 * @throws IndexOutOfBoundsException if the requested rule doesn't exist
	 */
	public String getRuleSubBehaviour(int ruleNumber) throws IndexOutOfBoundsException {
		String ruleName = (String) rulesNames.get(ruleNumber);
		return ((RuleInformation) rulesList.get(ruleName)).getSubBehaviourName();
	}
	
	/**
	 * Returns the output fuzzy sets of the behaviour
	 * @return the output fuzzy sets of the behaviour
	 */
	public ControlVariables getOutputFSets() {
		return finalOutputFSets;
	}
	
}

/**
 * This class contains all the informations associated a behaviour rule.
 * 
 * @version	1.1		29 Jan 2004
 * @author Denis Remondini
 *
 */
class RuleInformation implements Serializable {
	
	/* The rule name */
	private String ruleName;
	/* The value of the rule antecedent */
	private double antecedentValue;
	/* The rule output fuzzy sets */
	private ControlVariables outputFSets;
	/* The parameters used by the rule sub-behaviour */
	private ArrayList subBehParameters;
	/* The name of the rule sub-behaviour */
	private String subBehaviourName;

	
	/**
	 * Sets all the informations about a behaviour rule
	 * @param ruleName the rule name
	 * @param antValue the rule antecedent value
	 * @param outputFSets the rule output fuzzy sets
	 * @param subBehaviourName the name of the rule sub-behaviour (it can be null)
	 * @param subBehParameters the parameters used by the rule sub-behaviour (it can be null)
	 */
	public RuleInformation(String ruleName, double antValue, ControlVariables outputFSets, 
						   String subBehaviourName, ArrayList subBehParameters)  {
		this.ruleName = ruleName;
		this.antecedentValue = antValue;
		this.outputFSets = outputFSets;
		this.subBehaviourName = subBehaviourName;
		this.subBehParameters = subBehParameters;
	}
	
	/**
	 * Returns the rule antecedent value
	 * @return the rule antecedent value
	 */
	public double getAntecedentValue() {
		return antecedentValue;
	}
	
	/**
	 * Returns the rule output fuzzy sets
	 * @return the rule output fuzzy sets
	 */
	public ControlVariables getOutputFSets() {
		return outputFSets;
	}
	
	/**
	 * Returns the name of the rule sub-behaviour.
	 * @return the name of the rule sub-behaviour or null if there is no sub-behaviour.
	 */
	public String getSubBehaviourName() {
		return subBehaviourName;
	}
	
	/**
	 * Returns the parameters used by the rule sub-behaviour.
	 * @return the parameters used by the rule sub-behavour or null if there is no sub-behaviour.
	 */
	public ArrayList getSubBehParameters() {
		return subBehParameters;
	}
	
}