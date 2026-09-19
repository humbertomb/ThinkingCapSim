/*
 * @(#)TaskBehaviour.java		1.0 2004/03/24
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tclib.behaviours.fhb.bplan;

import java.util.*;
import java.io.IOException;

import tclib.behaviours.fhb.Behaviour;
import tclib.behaviours.fhb.BehaviourFactory;
import tclib.behaviours.fhb.MetricPredInfo;
import tclib.behaviours.fhb.MetricPredicates;
import tclib.behaviours.fhb.exceptions.LexicalError;
import tclib.behaviours.fhb.exceptions.SyntaxError;
import tc.shared.lps.lpo.LPO;

/**
 * This class implement the behaviour that execute the BPlan.
 * Each rule is a meta-rule.
 * 
 * @version 1.0		24 Mar 2004
 * @author Denis Remondini
 */
public class BPlanTaskBehaviour extends Behaviour {

	/* list of sub-behaviour */
	private Behaviour beh[];
	/* list of parameter sets: the parameters are relative to the goal of the behaviours */
	private java.util.List<ArrayList<LPO>> behGoalParams;
	/* list of parameter sets: the parameters are not relative to the goal of the behaviours */
	private java.util.List<HashMap<String, Object>> behOtherParams;
	/* list of the information about the metric predicates */
	private ArrayList<MetricPredInfo> predicatesData;
	/* contains the information about a metric predicate */
	private MetricPredInfo mPredInfo;
	/* Used to calculate the truth value of metric predicates */
	private MetricPredicates metricPred;
	/* Indicates how many meta-rules are in the BPlan */
	private int rulesNum;
	
	/**
	 * Initializes the behaviour
	 */
	public BPlanTaskBehaviour() {
		super();
		metricPred = new MetricPredicates();
	}
	
	/**
	 * Returns the name of the behaviour
	 * 
	 * @return the name of the behaviour
	 */
	public String getName() {
		return "BPlanTaskBehaviour";
	}

	/* Not used in this behaviour (see below)*/
	protected void createRules() {
		// nothing because it will never used!
	}

	/**
	 * Creates the rule for the behaviour, using the information about the BPlan
	 * 
	 * @param data information about the BPlan
	 */
	public void createRules(BPlanData data) {
		if (data == null) 
			return;
		
		rulesNum = data.getRulesNumber();
		predicatesData = data.getPredicatesData();
		beh = new Behaviour[rulesNum];
		behOtherParams = new ArrayList<HashMap<String, Object>> ();
		behGoalParams = new ArrayList<ArrayList<LPO>> ();
		
		try {
			/* creates all the rules of the behaviour */
			for (int i = 0; i < rulesNum; i++) {
				beh[i] = BehaviourFactory.createBehaviour(data.getBehaviourName(i),false);
				rules.addNewRule("Rule "+i,data.getAntecedent(i),beh[i]);
				behGoalParams.add (data.getBehGoalParameters(i));
				behOtherParams.add (data.getBehOtherParameters(i));
			}
			
		} 
		catch (SyntaxError se){
			System.out.println(se.toString());
		}
		catch (LexicalError le) {
			System.out.println(le.toString());
		}
		catch (IOException ioe) {
			System.out.println(ioe.toString());
		}
		
	}
	
	/*
	 * Updates the predicates used by the behaviour rules
	 */
	protected void update(HashMap<String, Object> params) {
		double predValue;
		ArrayList<LPO> behParam;
		
		/* Calculates the metric predicates and append them to the list of 
		 * predicates
		 */
		for (int i = 0; i < predicatesData.size(); i++) {
			mPredInfo = predicatesData.get(i);
			predValue = metricPred.calculate(mPredInfo.getMetricPredicate(),mPredInfo.getParameters());
			antecedentValues.setValue(mPredInfo.getPredName(),predValue);
		}
		/* sets the parameters for each sub-behaviour used by the fuzzy meta-rules */
		for (int i = 0; i < rulesNum; i++) {
			behOtherParams.get(i).putAll(params);
			beh[i].setParams(behOtherParams.get(i));
			behParam = behGoalParams.get(i);
			for (int j = 0; j < behParam.size(); j++) {
				beh[i].setParam("LPO"+j,behParam.get(j));
//				System.out.println("DEBUG: LPO"+j+" = "+((tc.shared.lps.lpo.LPO)(behParam.get(j))).label());
			}
		}
	}

}
