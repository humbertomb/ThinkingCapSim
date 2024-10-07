/*
 * @(#)Histogram.java	1.0 2003/12/08
 * 
 * (c) 2003 Denis Remondini
 */

package tclib.utils.fuzzy;

import java.lang.reflect.Array;


/**
 * This class implements an histogram fuzzy set.
 *  
 * @version	1.0 	08 Dic 2003
 * @author 	Denis Remondini
 */
public class Histogram extends FSet {

	private int nBars;			//number of bars

	/**
	 * Constructs an empty histogram
	 */
	public Histogram() {
		nBars = 0;
	}

	/**
	 * Construct an histogram.
	 * The number of bars cannot be greater than the MAXPOINTS value.
	 * @param xValues x values of the bars
	 * @param yValues heights of the bars
	 */
	public Histogram(double xValues[], double[] yValues)
	{
		nBars = Array.getLength(yValues);
		if (nBars > MAXPOINTS)
			nBars = MAXPOINTS;
	
		for (int i=0; i < nBars; i++) {
			shape[i].x (xValues[i]);
			shape[i].y (yValues[i]);
		}
	}
	
	/**
	 * Returns the number of bars of this histogram
	 * @return number of bars
	 */
	public int getBarsNumber() {
		return nBars;
	}

	/**
	 * Sets the height of all bars to 0.
	 */
	public void clearYValues() {
		for (int i = 0; i < MAXPOINTS; i++)
			shape[i].y (0);
	}
	
	/**
	 * Sets the height of a bar
	 * @param pos number of the bar
	 * @param yValue new height of the bar
	 */
	public void setYValue(int pos, double yValue) {
		if ((pos < 0) || (pos > MAXPOINTS))
			return;
		if (pos > nBars-1)
			nBars = pos+1;
		shape[pos].y (yValue);
	}

	/**
	 * Returns the height of a bar
	 * @param pos the number of the bar
	 * @return the height of the bar
	 */
	public double getYValue(int pos) {
		if ((pos < 0) || (pos > MAXPOINTS))
			return -1;

		return shape[pos].y ();
	}

	/**
	 * Returns the x coordinate of a bar
	 * @param pos the number of the bar
	 * @return x coordinate of the bar or Double.NaN if the input parameter is wrong 
	 */
	public double getXValue(int pos) {
		if ((pos < 0) || (pos > MAXPOINTS))
			return Double.NaN;

		return shape[pos].x ();
	}
	
	/**
	 * Specifies the x coordinate of a bar
	 * @param pos the number of the bar
	 * @param xValue x coordinate of the bar
	 */
	public void setXValue(int pos, double xValue) {
		if ((pos < 0) || (pos > MAXPOINTS))
			return;
		if (pos > nBars-1)
			nBars = pos+1;
		shape[pos].x (xValue);
	}
	
	/**
	 * Duplicates the histogram fuzzy set
	 */
	public FSet dupset ()  
	{
		double xValues[] = new double[nBars];
		double yValues[] = new double[nBars];

		for (int i = 0; i < nBars; i++) {
			xValues[i] = shape[i].x();
			yValues[i] = shape[i].y();
		}
		return new Histogram(xValues, yValues);
	}
	
	/**
	 * Make a union of two histogram fuzzy sets. If the histograms have different
	 * number of bars it makes a union considering only the bars that are in common.
	 * @param h1 first histogram fuzzy set. This will store the results of the union
	 * 			 operation.
	 * @param h2 second histogram fuzzy set.
	 */
	public static void union(Histogram h1, Histogram h2) {
		int i;
		
		if (h2 == null) 
			return;
		
		if (h1 == null)
			h1 = h2;
		else { 
			for (i = 0; i < h1.getBarsNumber(); i++)
				h1.setYValue(i,tconorm(h1.getYValue(i),h2.getYValue(i)));
		}
	}
	
	/**
	 * Defuzzifies the fuzzy set.
	 * @return the result of the defuzzification process.
	 */
	public double defuzzify() {
		double mom = 0.0;
		double sum = 0.0;
		double res = 0.0;
		double max = 0.0;
		
		if (defuz == FSet.DE_MAX) {
			for (int i = 0; i < nBars; i++) {
				if (shape[i].y () > max) {
					max = shape[i].y ();
					res = shape[i].x ();
				}
			}
		}
		else {
			/* Default defuzzification (CoG) */
			for (int i = 0; i < nBars; i++) {
				mom += shape[i].y () * shape[i].x ();
				sum += shape[i].y ();
			}
			
			if (sum == 0)
				res = 0.0;
			else
				res = (mom / sum);
		}
		return res;
	}
	
	/**
	 * Calculates the membership degree of the fuzzy predicate. This function works
	 * well only if the bars are placed with x coordinates in a crescent order.
	 * @param x fuzzy predicate 
	 * @return membership degree
	 */
	public double dmember (double x) {
		// is not used, because the Histogram is usually used as output fuzzy set
		
		double res = 0.0;

		if (nBars > 0) {
		
				for (int i = 0; i < nBars; i++) 
					if (shape[i].x() >= x) {
						res = shape[i].y();
						break;
					}			
			
		}
		return res;
	}
	
	/**
	 * Discounts the fuzzy set
	 * @param alpha discount value
	 * @return a new fuzzy set discounted
	 */
	public FSet alphacut(double alpha) {		
		FSet set;
		
		set = dupset();
		alphacut(set, alpha);
		
		return set;
	}
	
	/**
	 * Discounts the fuzzy set
	 * @param set the fuzzy set to discount
	 * @param alpha discount value
	 */
	public void alphacut(FSet set, double alpha) {
		for (int i = 0; i < Array.getLength(set.shape); i++)
			set.shape[i].y (tnorm(set.shape[i].y(),alpha));
	}
	
	public double getMinimum() {
		double min = shape[0].x ();
		for (int i = 1; i < nBars; i++) 
			if (shape[i].x () < min)
				min = shape[i].x ();
		return min;	
	}
	
	public double getMaximum() {
		double max = shape[0].x ();
		for (int i = 1; i < nBars; i++) 
			if (shape[i].x () > max)
				max = shape[i].x ();
		return max;	
	}


	/**
	 * This method MUST not be used with this kind of fuzzy set
	 * @return a value of 1.0
	 */
	public double integral ()  
	{
		return 1.0;
	}

	/**
	 * This method MUST not be used with this kind of fuzzy set
	 * @return a value of 1.0
	 */
	public double moment ()  
	{
		return 1.0;
	}

	/**
	 * Returns a string representation of this fuzzy set
	 */
	public String toString ()  
	{
		String str = new String("[" + shape[0].toString() );
		for (int i=1; i < nBars; i++)
			str += ", " + shape[i].toString();
		
		str += "]";
		return str;
	}
	
}
