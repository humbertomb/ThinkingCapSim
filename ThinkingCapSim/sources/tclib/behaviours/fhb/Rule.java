/*
 * @(#)Rule.java		1.0 2003/12/15
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.lang.Exception;
import java.io.IOException;

import tclib.behaviours.fhb.exceptions.*;
import tclib.behaviours.fhb.logic.*;
import tclib.utils.fuzzy.*;

/**
 * This class implements a fuzzy rule that can have as consequent a fuzzy set (simple rule) or
 * a behaviour (complex rule).
 * 
 * @version	1.0		15 Dec 2003
 * @author 	Denis Remondini
 */
public class Rule {

	/* Contains the logic formula that constitutes the rule antecedent */
	private Formula antecedent;
	/* Contains the result of the antecedent evaluation */
	private double antecedentValue;
	/* Contains the behaviour that form the consequent of the rule */
	private Behaviour subBeh;
	/* Contains the result of the rule execution */
	private ControlVariables outputFuzzySet;
	/* Contains the rule consequent in the case of simple rule (the consequent
	 * is not a behaviour)
	 */ 
	private ControlVariables originalOutputFuzzySet;
	/* Contains the rule consequent in the case of complex rule (the consequent
	 * is a behaviour)
	 */
	private ControlVariables subBehOutputFuzzySet;
	
	/**
	 * Constructs a new fuzzy rule.
	 */
	public Rule() {
		outputFuzzySet = new ControlVariables();
		originalOutputFuzzySet = null;
		subBehOutputFuzzySet = null;
		subBeh = null;
		antecedent = null;
		antecedentValue = -1;
	}
	
	/**
	 * Sets the rule antecedent
	 * @param formulaAntecedent the logic formula that form the rule antecedent 
	 * @throws SyntaxError if the logic formula contains some syntax errors
	 * @throws LexicalError if the logic formula contains some lexical errors
	 * @throws IOException if there are some I/O errors reading the logic formula
	 */
	public void setAntecedent(String formulaAntecedent) throws SyntaxError,LexicalError,IOException{
		antecedent = new Formula(formulaAntecedent);		
	}
	
	/**
	 * Evaluates the rule antecedent
	 * @param predValues the map with the fuzzy predicates needed to evaluate the rule antecedent
	 */
	public void evalAntecedent(DoubleMap predValues) {
		
		try {
			antecedentValue = antecedent.evaluation(predValues);		
		}
		catch (Exception e) {
			System.out.println("Error in the antecedent evaluation: "+e.toString());
			antecedentValue = -1;
		}

	}
	
	/**
	 * Sets the consequent of a complex rule
	 * @param beh the behaviour that form the rule consequent
	 */
	public void setConsequent(Behaviour beh) {
		subBeh = beh;
	}
	
	/**
	 * Sets as rule consequent a fuzzy set related to a specific control variable
	 * @param ctrlVariable the control variable
	 * @param fs the fuzzy set
	 */
	public void setConsequent(int ctrlVariable, Histogram fs) {
		if (originalOutputFuzzySet == null) {
			originalOutputFuzzySet = new ControlVariables();
		}
			
		originalOutputFuzzySet.setOutputFSet(ctrlVariable,fs);
	}
	
	/**
	 * Returns true if the rule consequent is a behaviour.
	 * @return true if the rule consequent is a behaviour, false otherwise.
	 */
	public boolean consequentIsBeh() {
		
		return ((subBeh == null) ? false : true);
	}
	
	/**
	 * Evaluates the whole fuzzy rule.
	 * @param predValues the map with the fuzzy predicates needed to evaluate the rule antecedent
	 * @return the fuzzy sets related to the control variables
	 * @throws RuleException if there are some errors in the antecedent evaluation
	 */
	public ControlVariables evaluate(DoubleMap predValues) throws RuleException {
		
		evalAntecedent(predValues);
		if (antecedentValue == -1)
			throw new RuleException("The antecedent of the Rule is wrong");
		
		if (consequentIsBeh()) 
			subBehOutputFuzzySet = subBeh.exec();
		
		if (!consequentIsBeh())
			outputFuzzySet.outputFSetsDiscount(originalOutputFuzzySet,antecedentValue);
		else { 
			outputFuzzySet.outputFSetsDiscount(subBehOutputFuzzySet,antecedentValue);
		}
		return outputFuzzySet;
	}
	
	/**
	 * Returns the result of the rule antecedent evaluation
	 * @return the result of the rule antecedent evaluation
	 */
	public double getAntecedentValue() {
		return antecedentValue;
	}
	
	/**
	 * Returns the result of the whole rule evaluation
	 * @return the fuzzy sets associated to each control variables
	 */
	public ControlVariables getOutputFSets() {
		return outputFuzzySet;
	}
	
	/**
	 * Returns the result of the consequent behaviour execution if the rule is a complex rule, null otherwise.
	 * @return the result of the consequent behaviour execution if the rule is a complex rule, null otherwise.
	 */
	public ControlVariables getSubBehOutputFSets() {
		return subBehOutputFuzzySet;
	}
	
	/**
	 * Returns the consequent behaviour if the rule is a complex rule, null otherwise.
	 * @return the consequent behaviour if the rule is a complex rule, null otherwise.
	 */
	public Behaviour getSubBehaviour() {
		return subBeh;
	}
	
}
