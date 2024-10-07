/*
 * @(#)RuleSet.java		1.0 2003/12/15
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import tclib.behaviours.fhb.exceptions.*;
import tclib.utils.fuzzy.Histogram;

/**
 * This class implements a set of fuzzy rules.
 * 
 * @version	1.0		15 Dec 2003
 * @author 	Denis Remondini
 */
public class RuleSet {

	/* A map with the rules */
	private HashMap rules;
	/* A list including the rules names*/
	private ArrayList rulesNames;
	/* It's needed only to merge the results of the rules evaluations */
	private ControlVariables cv;
	
	/**
	 * Constructs an empty set of rules
	 */
	public RuleSet() {
		rules = new HashMap();
		rulesNames = new ArrayList();
	    cv = new ControlVariables();
	}
	
	/**
	 * Returns a list including the names of the rules included in the rule set
	 * @return a list including the names of the rules included in the rule set
	 */
	public ArrayList getRulesNames() {
		return rulesNames;
	}
	
	/**
	 * Returns a rule of the rule set
	 * @param ruleName name of the rule wanted
	 * @return the rule requested or null if there is no rule with the specified name
	 */
	public Rule getRule(String ruleName) {
		if (!rulesNames.contains(ruleName))
			return null;
		
		return (Rule) rules.get(ruleName);
	}
	
	/**
	 * Evaluates the rules of the rule set, merging the corresponding results
	 * @param antecedentsValue the map with the fuzzy predicates needed to evaluate the rules antecedent
	 * @return the fuzzy sets related to the control variables
	 * @throws RuleException if there are some errors in at least one rule antecedent evaluation
	 */
	public ControlVariables evalRules(DoubleMap antecedentsValue) throws RuleException {
		 
		String ruleName;
		
		/* initialize the output sets */
		cv.clearOutputFSets();
		for (int i=0; i < rules.size(); i++) {
			ruleName = (String) rulesNames.get(i);
			cv.unionOutputFSets((ControlVariables)((Rule)rules.get(ruleName)).evaluate(antecedentsValue));
		}
			
		return cv;
	}
	
	/**
	 * Adds a new simple rule in the rule set
	 * @param ruleName the name of the rule
	 * @param Antecedent the rule antecedent
	 * @param ctrlVariable the control variable associated with the rule consequent 
	 * @param fs the fuzzy set that form the rule consequent
	 * @throws SyntaxError if there are some syntax errors in the rule antecedent
	 * @throws LexicalError if there are some lexical errors in the rule antecedent
	 * @throws IOException if there are some I/O errors reading the rule antecedent
	 */
	public void addNewRule(String ruleName,String Antecedent,int ctrlVariable, Histogram fs) 
	throws SyntaxError, LexicalError, IOException {
		
		Rule newRule = new Rule();
		newRule.setAntecedent(Antecedent);
		newRule.setConsequent(ctrlVariable,fs);
		
		/* if the rule name is already int the set, the associated rule will be overwritten */
		if (!rulesNames.contains(ruleName))
			rulesNames.add(ruleName);
		rules.put(ruleName,newRule);
	}
	
	/**
	 * Adds a new complex rule in the rule set
	 * @param ruleName the name of the rule
	 * @param Antecedent the rule antecedent
	 * @param beh the behaviour that form the consequent of the rule
	 * @throws SyntaxError if there are some syntax errors in the rule antecedent
	 * @throws LexicalError if there are some lexical errors in the rule antecedent
	 * @throws IOException if there are some I/O errors reading the rule antecedent
	 */
	public void addNewRule(String ruleName,String Antecedent, Behaviour beh) 
	throws SyntaxError, LexicalError, IOException {
		
		Rule newRule = new Rule();
		newRule.setAntecedent(Antecedent);
		newRule.setConsequent(beh);
		
		/* if the rule name is already int the set, the associated rule will be overwritten */
		if (!rulesNames.contains(ruleName))
			rulesNames.add(ruleName);
		rules.put(ruleName,newRule);
	}
	
}
