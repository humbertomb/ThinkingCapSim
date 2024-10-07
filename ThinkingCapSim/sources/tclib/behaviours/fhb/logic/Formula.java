/*
 * @(#)Formula.java		1.1 2003/12/10
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import tclib.behaviours.fhb.DoubleMap;
import tclib.behaviours.fhb.exceptions.ElementNotFound;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;


/**
 * This class provides methods to parse and evaluate a logic formula stored in a 
 * string.
 * 
 * @version	1.1		10 Dic 2003
 * @author 	Denis Remondini
 */
public class Formula {
	
	private LexicalAnalizer la;
	private String logicFormula;
	private DoubleMap variablesValue;			// map of the formula variables with their values
	
	/*
	 * Once the formula is parsed, each parsed element is stored in this list. 
	 */
	private ArrayList parsedFormula;  			
	private ListIterator parsedFormulaIterator;  // iterator to pass through the parsedFormula list
	
	/**
	 * Creates a new formula from a string.
	 * @param formula string representation of the formula
	 * @throws SyntaxError if there are some syntax errors in the formula
	 * @throws LexicalError if there are some lexical errors in the formula
	 * @throws IOException if there are some I/O errors reading the formula
	 */
	public Formula(String formula) throws SyntaxError,LexicalError,IOException {
		variablesValue = null;
		parsedFormula = null;
		parsedFormulaIterator = null;
		set(formula);
	}
	
	/**
	 * Specifies a new formula from a string.
	 * @param formula string representation of the formula
	 * @throws SyntaxError if there are some syntax errors in the formula
	 * @throws LexicalError if there are some lexical errors in the formula
	 * @throws IOException if there are some I/O errors reading the formula
	 */
	public void set(String formula) throws SyntaxError, LexicalError, IOException {
		logicFormula = formula;
		parsedFormula = new ArrayList();
		la = new LexicalAnalizer(logicFormula);
		
		parseExpression();
		if (la.nextToken().getType() != GenericToken.EOE)
			throw new SyntaxError("I expected the end of the expression");			
	}
	
	/**
	 * Evaluate the logic formula
	 * @param variablesValue map of the variables used in the formula with their values
	 * @return the result of the formula evaluation. It has to be a value between 0 and 1
	 * @throws SyntaxError if there are some syntax error in the formula
	 */
	public double evaluation(DoubleMap variablesValue) throws SyntaxError {
		double value;
		
		this.variablesValue = variablesValue;
		
		variablesValue.setValue("Always",1.0); 					// Add the Always variable
		
		// Initialize the iterator to start the parsing process from the beginning of the formula 
		parsedFormulaIterator = parsedFormula.listIterator(0);  
		value = evalExpression();

		if (parsedFormulaIterator.hasNext()) 
			throw new SyntaxError("After the last \')\' it must be the end of the formula");
		else
			return value;
	}
	
	private void parseOperator() throws SyntaxError, LexicalError, IOException {
		GenericToken gt;
		
		gt = la.nextToken();
		if (gt.getType() == GenericToken.OPERATOR) {
			parsedFormula.add(gt);						// add the parsed element in the list
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
		else {
			throw new SyntaxError("I'm waiting for an operator but I've found '"+gt+"'");
		}
	}
	
	/**
	 * Parses the logic formula
	 * @throws SyntaxError if there are some syntax errors in the formula
	 * @throws LexicalError if there are some lexical errors in the formula
	 * @throws IOException if there are some I/O errors reading the formula
	 */
	private void parseExpression() throws SyntaxError, LexicalError, IOException {
		GenericToken gt;
		
		gt = la.nextToken();
		switch (gt.getType()) {
			case GenericToken.VARIABLE:
				parsedFormula.add(gt);						// add the parsed element in the list
				break;
			
			case GenericToken.NUMBER:
				parsedFormula.add(gt);						// add the parsed element in the list
				break;
				
			case GenericToken.SYMBOL:
				
				if (((SymbolToken)gt).getSymbol()=='(') {
					parsedFormula.add(gt);					// add the parsed element in the list
					parseOperator();							
					gt = la.nextToken();
					if (gt.getType()==GenericToken.SYMBOL
							&& ((SymbolToken)gt).getSymbol()==')') {
						parsedFormula.add(gt);				// add the parsed element in the list
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
	
	/*
	 * Evaluates the formula.
	 * If there are some syntax errors a SyntaxError exception will be thrown.
	 */
	private double evalExpression() throws SyntaxError {
		GenericToken gt;
		double value1, value2,value3;
		
		/* get the next token from parsed formula */
		gt = (GenericToken)parsedFormulaIterator.next();		
		
		switch (gt.getType()) {
			case GenericToken.VARIABLE:
				/* if the token is a variable returns its value */
				try {
					return variablesValue.getValue(((VariableToken)gt).getName());
				}
				catch (ElementNotFound e) {
					throw new SyntaxError(((VariableToken)gt).getName()+": Unknown variable in the logic formula");
				}
				
			case GenericToken.NUMBER:
				/* if the token is a number returns its value */
				return ((NumberToken)gt).getValue();
				
			case GenericToken.OPERATOR:
				/* if the token is an operator checks the arity of the operator,
				 * get the needed variables and then executes the associated operation
				 */				 
				if (((OperatorToken)gt).getArity() == 3) {
					value1 = evalExpression();
					value2 = evalExpression();
					value3 = evalExpression();
					return ((OperatorToken) gt).doOp(value1,value2,value3);
				} 
				else if (((OperatorToken)gt).getArity() == 2) {
					value1 = evalExpression();
					value2 = evalExpression();
					return ((OperatorToken) gt).doOp(value1,value2);
				} 
				else {	// arity is equal to 1
					value1 = evalExpression();
					return ((OperatorToken) gt).doOp(value1);
				}
				
			case GenericToken.SYMBOL:
				if (((SymbolToken)gt).getSymbol()=='(') {
					value1 = evalExpression();
					gt = (GenericToken)parsedFormulaIterator.next();
					// The parenthesis open in the formula has to be closed 
					if (gt.getType()==GenericToken.SYMBOL
							&& ((SymbolToken)gt).getSymbol()==')') {
						return value1;
					} else
						throw new SyntaxError("I'm waiting for \')\'");
				}
			default:
				throw new SyntaxError("Some errors have happened");
		}
	}
	
	
}
