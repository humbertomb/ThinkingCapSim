/*
 * @(#)BPlanParser.java		1.0 Mar 24, 2004
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb.logic;

import java.io.*;
import java.util.Vector;
import java.util.HashMap;

import tclib.behaviours.fhb.MetricPredInfo;
import tclib.behaviours.fhb.MetricPredicates;
import tclib.behaviours.fhb.bplan.BPlanData;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tc.shared.lps.LPS;



/**
 * @author Denis Remondini
 *
 */
public class BPlanParser {

	/**
	 * Identifies a metric predicate
	 */
	public static final int METRIC_PRED = 0;
	
	/**
	 * Identifies an operator
	 */
	public static final int OPERATOR = 1;
	
	/* Take care that the lexical analizer is case sensitive */
	private LexicalAnalizer la;	
	/* Contains the information about a metric predicate */
	private MetricPredInfo mPredInfo;
	/* Contains the data about the B-Plan*/
	private BPlanData bPlanData;
	/* It represent the Local Perceptual Space where the LPO object can be found */
	private LPS lps;
	/* It is the input to parse. It contains the B-Plan */
	private BufferedReader inputBuffer;
	/* This map is used to be sure that the predicates are stored only once */
	private HashMap previousPredNames;
	/* Contains the antecedent formula of the context rule after per parsing process */
	private String parsedFormula;  			
	
	/**
	 * Creates a new BPlan parser
	 * @param inputBuffer the buffer that contains the BPlan to parse
	 * @param lps the Local Perceptual Space needed to parse the BPlan
	 */
	public BPlanParser(BufferedReader inputBuffer, LPS lps) {
		this.lps = lps;
		this.inputBuffer = inputBuffer;
		bPlanData = new BPlanData();
		previousPredNames = new HashMap();
	}
	
	/**
	 * This function provides a very simple parsing procedure.
	 * @throws SyntaxError if the input contains some syntax errors
	 * @throws LexicalError if the input contains some lexical errors
	 * @throws IOException if there is an I/0 error
	 */
	public void parse() throws SyntaxError,LexicalError,IOException {
		String line = inputBuffer.readLine();
		while (line != null) {
			parseAntecedent(line);
			line = inputBuffer.readLine();
			parseConsequent(line);
			line = inputBuffer.readLine();
			parseParameters(line);
			line = inputBuffer.readLine();
		}
	}
	
	/*
	 * Parses the parameters of a context rule included in the B-Plan.
	 * The parameters have to be in the same line and each one is a couple formed by
	 * a parameter name and a parameter value. A possible example is:
	 * (followSpeed 0.4 crossSpeed 0.2)
	 */
	private void parseParameters(String bPlanRuleParams) throws SyntaxError, LexicalError, IOException {
		String paramName;
		double paramValue;
		la = new LexicalAnalizer(bPlanRuleParams);
		HashMap params = new HashMap();

		GenericToken gt = la.nextToken();
		if ((gt.getType() == GenericToken.SYMBOL) && (((SymbolToken)gt).getSymbol() == '(')) {
			gt = la.nextToken();
			while (gt.getType() != GenericToken.SYMBOL) {
				if (gt.getType() == GenericToken.VARIABLE) {
					paramName = ((VariableToken)gt).getName();
				}
				else
					throw new SyntaxError("I expected the parameter name");
				
				gt = la.nextToken();
				if (gt.getType() == GenericToken.NUMBER) {
					paramValue = ((NumberToken)gt).getValue();
					params.put(paramName, new Double(paramValue));
				}
				else
					throw new SyntaxError("I expected the parameter value");
				
				gt = la.nextToken();
			}				
			if (((SymbolToken)gt).getSymbol() != ')')
				throw new SyntaxError("I expected the ')' character");
		}
		else 
			throw new SyntaxError("I expected the '(' character");
		
		if (la.nextToken().getType() != GenericToken.EOE)
			throw new SyntaxError("I expected the end of the expression");
		
		bPlanData.addOtherParameters(params);
	}
	
	/*
	 * Parses the consequent of a context rule included in the B-Plan.
	 * The consequent must have this form:
	 * <behaviourName>(<param1> <param2> <paramN>)
	 * where the parameters are the names of LPO objects contained in the LPS
	 */
	private void parseConsequent(String bPlanRuleConsequent) throws SyntaxError, LexicalError, IOException {
		la = new LexicalAnalizer(bPlanRuleConsequent);
		String behaviourName = "";
		Vector behaviourParams = new Vector();
		
		GenericToken gt = la.nextToken();
		if (gt.getType() == GenericToken.VARIABLE) {
			// reads the behaviour name
			behaviourName = ((VariableToken) gt).getName();
			gt = la.nextToken();
			// reads the list of parameters 
			if ((gt.getType() == GenericToken.SYMBOL) && ((SymbolToken)gt).getSymbol() == '(') {
				gt = la.nextToken();
				while (gt.getType() != GenericToken.SYMBOL) {
					if (gt.getType() == GenericToken.VARIABLE) 
						behaviourParams.addElement(lps.find(((VariableToken)gt).getName()));
					else
						throw new SyntaxError("I expected a variable as parameter of the behaviour");
					gt = la.nextToken();
				}
				if (((SymbolToken)gt).getSymbol() != ')')
					throw new SyntaxError("I expected the ')' character");
			}
			else
				throw new SyntaxError("I expected the '(' character");
		}
		else
			throw new SyntaxError("I expected a variable");
		
		if (la.nextToken().getType() != GenericToken.EOE)
			throw new SyntaxError("I expected the end of the expression");
		
		bPlanData.addBehaviour(behaviourName);
		bPlanData.addGoalParameters(behaviourParams);		
	}
	
	
	/*
	 * Parses the antecedent of a context rule included in the B-Plan.
	 * 
	 */
	private void parseAntecedent(String bPlanRuleAntecedent) throws SyntaxError, LexicalError, IOException {
		parsedFormula = "";
		la = new LexicalAnalizer(bPlanRuleAntecedent);
		
		parseExpression();
		if (la.nextToken().getType() != GenericToken.EOE)
			throw new SyntaxError("I expected the end of the expression");
		
		bPlanData.addAntecedent(parsedFormula);
	}
	
	/*
	 * Parses an operator included in the antecedent of a context rule.
	 * The operator can be a logical one (NOT,AND,OR,EQL) or a Metric one (NEAR,AT,...).
	 */
	private int parseOperator() throws SyntaxError, LexicalError, IOException {
		GenericToken gt;
		int type;
		
		gt = la.nextToken();
		if (gt.getType() == GenericToken.OPERATOR) {
			// the operator is a logical operator
			type = OPERATOR;
			parsedFormula = parsedFormula + ((OperatorToken)gt).getOperatorName() + " ";						
			if (((OperatorToken)gt).getArity() == 3) {	
				// find the three variables needed for the ternary operator
				parseExpression();						
				parseExpression();
				parseExpression();
			}
			else if (((OperatorToken)gt).getArity() == 2) {
				// find the two variables needed for the binary operator
				parseExpression();
				parseExpression();
			} 
			else {
				// find the variable needed for the unary operator
				parseExpression();
			}
		}
		else if (gt.getType() == GenericToken.VARIABLE) {
			// checks if it is a metric predicate
			if (MetricPredicates.isMetricPred( ((VariableToken)gt).getName() )) {
				type = METRIC_PRED;
				// deletes the last '(' in the formula
				parsedFormula = parsedFormula.substring(0,parsedFormula.length()-1);
				// read the name of the metric predicate
				String predName = ((VariableToken)gt).getName();
				// read the arity of the metric predicate
				int arity = MetricPredicates.getArity(predName);
				/* creates the structure where collect all the information about the 
				 * metric predicate 
				 */
				mPredInfo = new MetricPredInfo(predName);
				// read all the parameters of the metric predicate
				for (int i = 0; i < arity; i++) {
					gt = la.nextToken();
					if (gt.getType() != GenericToken.VARIABLE)
						throw new SyntaxError("I'm waiting for a parameter of a metric predicate but I've found '"+gt+"'");
					/* build a predName made by the concatenation of the metric predicate name 
					 * and the parameters name
					 */
					predName = predName + ((VariableToken)gt).getName();
					
					mPredInfo.addParameter(lps.find(((VariableToken)gt).getName()));
				}
				// Add the new predName (see above) to the formula
				parsedFormula = parsedFormula + predName + " ";
				mPredInfo.setPredName(predName);
				/* checks if the metric predicate has been already met in the previous
				 * context rules.
				 */
				if (!previousPredNames.containsKey(predName)) {
					previousPredNames.put(predName,null);
					bPlanData.addPredicatesData(mPredInfo);
				}
			}
			else {
				throw new SyntaxError("I'm waiting for a metric predicate but I've found '"+gt+"'");
			}
		}
		else {
			throw new SyntaxError("I'm waiting for an operator or a metric predicate but I've found '"+gt+"'");
		}
		return type;
	}
	
	/*
	 * Parses an expression of the antecedent of a context rule
	 */
	private void parseExpression() throws SyntaxError, LexicalError, IOException {
		GenericToken gt;
		
		gt = la.nextToken();
		switch (gt.getType()) {
			case GenericToken.VARIABLE:
				parsedFormula = parsedFormula + ((VariableToken)gt).getName() + " ";						
				break;
				
			case GenericToken.NUMBER:
				parsedFormula = parsedFormula + ((NumberToken)gt).getValue() + " ";						
				break;
				
			case GenericToken.SYMBOL:
				
				if (((SymbolToken)gt).getSymbol()=='(') {
					parsedFormula = parsedFormula + ((SymbolToken)gt).getSymbol();					
					int type = parseOperator();							
					gt = la.nextToken();
					if (gt.getType()==GenericToken.SYMBOL
							&& ((SymbolToken)gt).getSymbol()==')') {
						if (type == OPERATOR)
							parsedFormula = parsedFormula + ((SymbolToken)gt).getSymbol() + " ";	
					}
					else {
						throw new SyntaxError("I'm waiting for \')\'");
					}						
				}
				break;
			default:
				throw new SyntaxError("Some errors have happened");
		}	
	}
	
	/**
	 * Returns the structure containing the data about the BPlan.
	 * @return the structure containing the data about the BPlan.
	 */
	public BPlanData getBPlanData() {
		return bPlanData;
	}
	
}
