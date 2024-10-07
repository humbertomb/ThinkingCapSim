/*
 * @(#)ControlVariables.java		1.0 2003/12/18
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.util.ArrayList;
import java.io.Serializable;

import tclib.utils.fuzzy.*;

/**
 * This class contains all the information about the control variables that are
 * used by the fuzzy behaviours.
 * 
 * @version	1.0		18 Dec 2003
 * @author 	Denis Remondini
 */
public class ControlVariables implements Serializable {

	/**
	 * Indicates the number of control variables
	 */
	public final static int NVARIABLES = 2;
	
	/* control variables constants
	 * ATTENTION: when you modify/add a control variable constant you have
	 *			  to modify also the getVariableName method
	 */
	public final static int ROTATION 	= 0;
	public final static int SPEED 		= 1;
	
	/* rotation constants */
	public final static int TURN_TR 	= 6;
	public final static int TURN_R 		= 5;
	public final static int TURN_SR		= 4;
	public final static int TURN_Z 		= 3;
	public final static int TURN_SL 	= 2;
	public final static int TURN_L 		= 1;
	public final static int TURN_TL 	= 0;
	
	
	/* speed constants */
	public final static int SPEED_Z		= 0;
	public final static int SPEED_S		= 1;
	public final static int SPEED_M 	= 2;
	public final static int SPEED_F 	= 3;
	
	/* a list with one fuzzy set for each control variable */
	private ArrayList outputFSets;
	/* a list with one default fuzzy set for each control variable */
	private ArrayList defaultOutputFSets;
	/* an array with one crisp value for each control variable */
	private double[] outputCrispValues;
	
	/* Creates a default fuzzy set for each control variable and decides what
	 * type of t-norm and t-conorm has to be used in the fuzzy operations
	 */
	public ControlVariables() {
		outputFSets = new ArrayList(NVARIABLES);
		defaultOutputFSets = new ArrayList(NVARIABLES);
		outputCrispValues = new double[NVARIABLES];
		
		/* Choose the TNORM and TCONORM for the behaviours computation */
		FSet.set_tnorm(FSet.T_MIN);
		FSet.set_tconorm(FSet.TCO_MAX);
		/* Choose the defuzzification to use */
		FSet.set_defuz(FSet.DE_COG);
		
		for (int i = 0; i < NVARIABLES; i++) {
			outputFSets.add(null);
			defaultOutputFSets.add(null);
		}
		setDefaultOutputFSets();
	}
	
	/**
	 * Sets the default fuzzy set for each control variable.
	 */
	public void setDefaultOutputFSets() 
	{
		// Rotation control values (deg/s)
		Histogram rotationOutputFSet = new Histogram();

		rotationOutputFSet.setXValue (TURN_TR, -110.0);
		rotationOutputFSet.setXValue (TURN_R, -70.0);
		rotationOutputFSet.setXValue (TURN_SR, -30.0);
		rotationOutputFSet.setXValue (TURN_Z, 0);
		rotationOutputFSet.setXValue (TURN_SL, 30.0);
		rotationOutputFSet.setXValue (TURN_L, 70.0);
		rotationOutputFSet.setXValue (TURN_TL, 110.0);		
		
		// Speed control values (m/s)
		Histogram speedOutputFSet = new Histogram();
		speedOutputFSet.setXValue (SPEED_Z, 0.0);
		speedOutputFSet.setXValue (SPEED_S, 0.15);
		speedOutputFSet.setXValue (SPEED_M, 0.25);
		speedOutputFSet.setXValue (SPEED_F, 0.80);
		
		defaultOutputFSets.set(ROTATION,rotationOutputFSet);
		defaultOutputFSets.set(SPEED,speedOutputFSet);
		outputFSets.set(ROTATION,rotationOutputFSet);
		outputFSets.set(SPEED,speedOutputFSet);		
	}
	
	/**
	 * Sets all the output fuzzy sets to have 0 as y value
	 */
	public void clearOutputFSets() {
		for (int i = 0; i < outputFSets.size(); i++) 
			((Histogram)outputFSets.get(i)).clearYValues();
	}
	
	/**
	 * Returns the default fuzzy set associated with a control variable
	 * @param ctrlVariable the control variable 
	 * @return the default fuzzy set associated with the control variable
	 */
	public Histogram getDefaultOutputFSet(int ctrlVariable) {
		try {
			return (Histogram) ((Histogram) defaultOutputFSets.get(ctrlVariable)).dupset();
		}
		catch (IndexOutOfBoundsException e)	{
			System.out.println("DEBUG: IndexOutOfBounds error in getDefaultOutputFSet of ControlVariables class");				
			return null;
		}
	}
	
	/**
	 * Associates a fuzzy set to a control variable
	 * @param ctrlVariable the control variable
	 * @param outputFSet the fuzzy set
	 * @return false if some errors happened, true otherwise.
	 */
	public boolean setOutputFSet(int ctrlVariable, Histogram outputFSet) {
		try {
			outputFSets.set(ctrlVariable, outputFSet);
		}
		catch (IndexOutOfBoundsException e) {
			System.out.println("DEBUG: IndexOutOfBounds error in setOutputFSet of ControlVariables class");
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the fuzzy set associated with a control variable
	 * @param ctrlVariable the control variable 
	 * @return the fuzzy set associated with the control variable
	 */
	public Histogram getOutputFSet(int ctrlVariable) {
		try {
			return (Histogram) outputFSets.get(ctrlVariable);
		}
		catch (IndexOutOfBoundsException e)	{
			System.out.println("DEBUG: IndexOutOfBounds error in getOutputFSet of ControlVariables class");
			return null;
		}
	}
	
	/**
	 * Discounts the output fuzzy sets
	 * @param alpha the discount value
	 */
	public void outputFSetsDiscount(double alpha) {
		Histogram outFSet;
		
		for (int i = 0; i < outputFSets.size(); i++) {
			outFSet = (Histogram) outputFSets.get(i);
			if (outFSet != null) {
				outputFSets.set(i,outFSet.alphacut(alpha));
			}
		}
	}
	
	/**
	 * Discounts the output fuzzy sets. This method sets the output fuzzy sets 
	 * taking the original ones and calculating the discounted values.
	 * Take care that the original fuzzy sets won't be changed.
	 * @param originalOutputFSets orginal fuzzy sets
	 * @param alpha the discount value
	 */
	public void outputFSetsDiscount(ControlVariables originalOutputFSets, double alpha) {
		Histogram outFSet;
		
		for (int i = 0; i < NVARIABLES; i++) {
			outFSet = (Histogram) originalOutputFSets.getOutputFSet(i);
			if (outFSet != null) {
				outputFSets.set(i,outFSet.alphacut(alpha));
			}
		}
	}
	
	/**
	 * Merges the fuzzy sets with those of another ControlVariables object
	 * @param cv1 the other ControlVariables object 
	 */
	public void unionOutputFSets(ControlVariables cv1) {
		Histogram out1, out2;
		
		for (int i = 0; i < outputFSets.size(); i ++) {
			out1 = (Histogram) outputFSets.get(i);
			out2 = (Histogram) cv1.getOutputFSet(i);
			Histogram.union(out1,out2);
		}

	}
	
	/**
	 * Defuzzifies the output fuzzy sets 
	 */
	public void defuzzify() {
		for (int i = 0; i < outputFSets.size(); i++)
			outputCrispValues[i] = ((Histogram) outputFSets.get(i)).defuzzify();
	}
	
	/**
	 * Returns the crisp value (the defuzzification result) associated with a 
	 * specified control variable
	 * @param ctrlVariable the control variable
	 * @return the crisp value
	 */
	public double getCrispValue(int ctrlVariable) {
		if ((ctrlVariable >= 0) && (ctrlVariable < NVARIABLES))
			return outputCrispValues[ctrlVariable];
		
		//TODO: think about throwing an exception in this case
		return 0.0;
	}
	
	/**
	 * Returns the crisp values (the defuzzification result) associated with the 
	 * control variables
	 * @return the crisp values
	 */
	public double[] getCrispValues() {
		return outputCrispValues;
	}
	/**
	 * Returns the name of a control variable
	 * @param ctrlVariable the control variable
	 * @return the control variable name
	 */
	public static String getVariableName(int ctrlVariable) {
		switch (ctrlVariable) {
			case ROTATION	: return "ROTATION";
			case SPEED		: return "SPEED";
			default			: return "ERROR: unknown variable";
		}
	}
	
}
