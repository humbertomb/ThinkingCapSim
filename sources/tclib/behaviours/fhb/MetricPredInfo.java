/*
 * @(#)MetricPredInfo.java		1.0 2004/03/01
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import java.util.ArrayList;

import tc.shared.lps.lpo.LPO;

/**
 * This class contains the information about a metric predicate, like the name
 * of the metric predicate, the parameters needed to calculate it and the name of 
 * the fuzzy predicate that will be used in the rule antecedent. 
 * <br>
 * For example, if the rule antecedent contains (AT Room1) <br>
 * then this class contains: <br>
 * metricPredicate: AT
 * parameters: [Room1]
 * predName: ATRoom1
 * 
 * @version 1.0		24 Mar 2004
 * @author Denis Remondini
 */
public class MetricPredInfo {
	
	private String predName;
	private String metricPredicate;
	private ArrayList<LPO> parameters;
	
	/**
	 * Initializes the information structure about a metric predicate.
	 * @param metricPredicate the name of the metric predicate
	 */
	public MetricPredInfo(String metricPredicate) {
		this.metricPredicate = metricPredicate;
		predName = null;
		parameters = new ArrayList<LPO>();
	}

	/**
	 * Sets the name of the fuzzy predicate that will be used inside a rule antecedent.
	 * @param predName the name of the fuzzi predicate
	 */
	public void setPredName(String predName) {
		this.predName = predName;
	}
	
	/**
	 * Adds a parameter needed to calculate the metric predicate
	 * @param param the parameter
	 */
	public void addParameter(LPO param) {
		parameters.add (param);
	}
	
	/**
	 * Returns the name of the fuzzy predicat that will be used inside a rule antecedent.
	 * @return the name of the fuzzy predicat that will be used inside a rule antecedent.
	 */
	public String getPredName() {
		return predName;
	}
	
	/**
	 * Return the name of the metric predicate
	 * @return the name of the metric predicate
	 */
	public String getMetricPredicate() {
		return metricPredicate;
	}
	
	/**
	 * Returns the list of parameters needed to calculate the metric predicate
	 * @return the list of parameters
	 */
	public ArrayList<LPO> getParameters() {
		return parameters;
	}
	
}
