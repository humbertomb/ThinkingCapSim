/*
 * @(#)LexicalAnalizer.java		1.1 2003/12/10
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.logic;

import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;

import tclib.behaviours.fhb.exceptions.LexicalError;

/**
 * This class provides a lexical analizer for a logic formula stored in a
 * string.
 * 
 * @version	1.1		10 Dic 2003
 * @author 	Denis Remondini
 */
public class LexicalAnalizer {
	
	private final int TT_EOF = StreamTokenizer.TT_EOF;
	private final int TT_WORD = StreamTokenizer.TT_WORD;
	private final int TT_NUMBER = StreamTokenizer.TT_NUMBER;
	
	private StreamTokenizer st;

	/**
	 * Construct a new lexical analizer for a specified logic formula
	 * @param formula logic formula to analize
	 */
	public LexicalAnalizer( String formula ) {
		st = new StreamTokenizer(new StringReader(formula));
//		st.lowerCaseMode(true);
		st.ordinaryChars(124,255);		// serves to consider the extended characters as the normal ones
		st.ordinaryChar('.');			// serves to consider the '.' character as a single unit, not part of a word
		st.wordChars('a','z');
		st.wordChars('A','Z');
		st.wordChars('_','_');			// serves to indicate that the '_' character can be in a word
		st.eolIsSignificant(false);
	}

	/**
	 * Return the next token analized in the logic formula
	 * @return the token analized. It can be:
	 * 	<ul>
	 * 		<li> GenericToken 	</li>
	 * 		<li> OperatorToken 	</li>
	 *      <li> NumberToken 	</li>
	 * 		<li> SymbolToken 	</li>
	 *  </ul>
	 * @throws IOException if there is an I/O error reading the logic formula
	 * @throws LexicalError if there are some unknown characters in the logic formula
	 */
	public GenericToken nextToken()	throws IOException, LexicalError {
		int t;

		t = st.nextToken();

		switch (t) {
			case TT_EOF: 
				return new GenericToken(GenericToken.EOE);
			case TT_WORD: 
				if (OperatorToken.isOperator(st.sval))
					return new OperatorToken(st.sval);
				return new VariableToken(st.sval);
			case TT_NUMBER:
				return new NumberToken(st.nval);
			case '(': case ')': return new SymbolToken((char)t);
			default: throw new LexicalError("Character not valid \'"+(char)t+"\' ("+t+").");
		}
	}

}


/**
 * This class implements a generic token.
 * 
 * @version	1.0		10 Dic 2003
 * @author Denis Remondini
 */
class GenericToken {
	
	/**
	 * Indicates that the token is a symbol
	 */
	public static final int SYMBOL = 0;
	
	/**
	 * Indicates that the token is a variable
	 */
	public static final int VARIABLE = 1;
	
	/**
	 * Indicates that the token is an operator
	 */
	public static final int OPERATOR = 2;
	
	/**
	 * Indicates that the token is a number
	 */
	public static final int NUMBER = 3;
	
	/**
	 * Indicates that the end of the expression is reached
	 */
	public static final int EOE = 4;

	private int tk_type;			// it stores the kind of the token

	
	/**
	 * Creates a new token 
	 * @param _tk_type specifies the type of this token. It can be:
	 *  <ul>
	 *  	<li> SYMBOL 	</li>
	 * 		<li> VARIABLE	</li>
	 * 		<li> OPERATOR	</li>
	 * 		<li> NUMBER		</li>
	 * 		<li> EOE		</li>
	 *  </ul>
	 */
	public GenericToken( int _tk_type ) {
		tk_type = _tk_type;
	}

	/**
	 * Return the type of the token
	 * @return the type of the token. It can be:
	 *  <ul>
	 *  	<li> SYMBOL 	</li>
	 * 		<li> VARIABLE	</li>
	 * 		<li> OPERATOR	</li>
	 * 		<li> NUMBER		</li>
	 * 		<li> EOE		</li>
	 *  </ul>
	 */
	public int getType() {
		return tk_type;
	}

	/**
	 * Returns a string representation of the token type
	 */
	public String toString() {
		String s;

		switch (getType()) {
			case SYMBOL:   s = "SIMBOL"; break;
			case VARIABLE: s = "VARIABLE"; break;
			case OPERATOR: s = "OPERATOR"; break;
			case NUMBER:   s = "NUMBER"; break;
			case EOE:      s = "END OF EXPRESSION"; break;
			default: s = "NONE";
		}

		return s;
	}
}

/**
 * This class implements a symbol token.
 * 
 * @version 1.0		10 Dic 2003
 * @author Denis Remondini
 */
class SymbolToken extends GenericToken {
	
	private char symbol;

	/**
	 * Creates a new symbol token
	 * @param _symbol the value of the symbol token
	 */
	public SymbolToken( char _symbol ) {
		super(SYMBOL);
		symbol = _symbol;
	}

	/**
	 * Returns the value of the symbol token
	 * @return the value of the symbol token
	 */
	public char getSymbol() {
		return symbol;
	}

	/**
	 * Returns a string representation of the token type
	 */
	public String toString() {
		return super.toString()+" -> "+"\""+getSymbol()+"\"";
	}
}


/**
 * This class implements a variable token.
 * 
 * @version 1.0		10 Dic 2003
 * @author Denis Remondini
 */
class VariableToken extends GenericToken {
	
	private String varname;

	/**
	 * Creates a new variable token
	 * @param _varname the value of the variable token
	 */
	public VariableToken( String _varname ) {
		super(VARIABLE);
		varname = _varname;
	}

	/**
	 * Returns the value of the variable token
	 * @return the value of the variable token
	 */
	public String getName() {
		return varname;
	}

	/**
	 * Returns a string representation of the token type
	 */
	public String toString() {
		return super.toString()+" -> "+getName();
	}
}


class NumberToken extends GenericToken {
	private double value;

	public NumberToken( double value ) {
		super(NUMBER);
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public String toString() {
		return super.toString()+" -> "+"\""+String.valueOf(getValue())+"\"";
	}
}


/**
 * This class implements an operator token.
 * 
 * @version 1.0		10 Dic 2003
 * @author Denis Remondini
 */
class OperatorToken extends GenericToken {
	
	/**
	 * Specifies that the operator token will perform an AND operation
	 */
	public static final int AND = 0;
	
	/**
	 * Specifies that the operator token will perform an OR operation
	 */
	public static final int OR = 1;
	
	/**
	 * Specifies that the operator token will perform a NOT operation
	 */
	public static final int NOT = 2;
	
	/**
	 * Specifies that the operator token will perform an EQL operation
	 */
	public static final int EQL = 3;

	private int op;					// it stores the type of the operator token

	/**
	 * Creates a new operator token
	 * @param operator specifies the type of the operator. It can be:
	 *  <ul>
	 * 		<li> "and"	</li>
	 * 		<li> "or" 	</li>
	 * 		<li> "not" 	</li>
	 * 		<li> "eql" 	</li>
	 *  </ul>
	 */
	public OperatorToken(String operator) {
		super(OPERATOR);
		if (operator.equals("AND"))
			op = AND;
		if (operator.equals("OR"))
			op = OR;
		if (operator.equals("NOT"))
			op = NOT;
		if (operator.equals("EQL"))
			op = EQL;
	}

	/**
	 * Creates a new operator token
	 * @param operator specifies the type of the operator. It can be:
	 *  <ul>
	 * 		<li> AND	</li>
	 * 		<li> OR		</li>
	 * 		<li> NOT	</li>
	 * 		<li> EQL	</li>
	 *  </ul>
	 */
	public OperatorToken(int operator) {
		super(OPERATOR);
		op = operator;
	}
	
	/**
	 * Checks if the input string represent a valid operator
	 * @param str input string
	 * @return true if the input string is a valid operator, false otherwise.
	 */
	public static boolean isOperator(String str) {
		if (str.equals("AND"))
			return true;
		if (str.equals("OR"))
			return true;
		if (str.equals("NOT"))
			return true;
		if (str.equals("EQL"))
			return true;
		
		return false;
	}
	
	/**
	 * Returns the type of the operator
	 * @return the type of the operator
	 */
	public int getOp() {
		return op;
	}
	
	/**
	 * Returns the name of the operator
	 * @return the name of the operator
	 */
	public String getOperatorName() {
		switch (getOp()) {
			case AND	: return "AND";
			case OR		: return "OR";
			case NOT	: return "NOT";
			case EQL	: return "EQL";
			default		: return "ERROR: unknown operator";
		}
	}

	/**
	 * Returns the arity of the operator
	 * @return the arity of the operator. If this value is 0 it means that the 
	 * 		   operator is not valid.
	 */
	public int getArity() {
		switch (getOp()) {
			case AND	: 	return 2;
			case OR		: 	return 2;
			case NOT	: 	return 1;
			case EQL	: 	return 3;
			default		: 	return 0;
		}
	}

	/**
	 * Makes the operation associated with the unary operator.
	 * @param value variable of the operation
	 * @return the result of the operation
	 */
	public double doOp(double value) {
		switch (getOp()) {
			case NOT : 
				return (1.0 - value);
				
			default: return 0;
		}
	}
	
	/**
	 * Makes the operation associated with the binary operator.
	 * @param firstValue first variable of the operation
	 * @param secondValue second variable of the operation
	 * @return the result of the operation
	 */
	public double doOp(double firstValue, double secondValue) {
		switch (getOp()) {
			case AND	: return Math.min(firstValue,secondValue);
			case OR		: return Math.max(firstValue,secondValue);
			default		: return 0;
		}
	}
	
	/**
	 * Makes the operation associated with the ternary operator.
	 * @param firstValue first variable of the operation
	 * @param secondValue second variable of the operation
	 * @param thirdValue third variable of the operation
	 * @return the result of the operation
	 */
	public double doOp(double firstValue, double secondValue, double thirdValue) {
		double d;

		switch (getOp()) {
			case EQL : 
				d = thirdValue * (firstValue-secondValue);
				if ( d < 0 ) 
					d = -d;
				return((1.0 - Math.min(1.0, d)));
			
			default: return 0;
		}
	}
	
	/**
	 * Returns a string representation of the operator token
	 */
	public String toString() {
		String s;

		switch (getOp()) {
			case AND  	: s = "AND(x,y)"; break;
			case OR  	: s = "OR(x,y)"; break;
			case NOT  	: s = "NOT(x)"; break;
			case EQL	: s = "EQL(x,y,z)"; break;
			default		: s = "NONE"; break;
		}

		return super.toString()+" -> "+s;
	}
}

