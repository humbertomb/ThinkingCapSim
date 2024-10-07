/*
 * @(#)ExperimentData.java		1.0 2004/03/26
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tcrob.oru.rasmus;

import java.io.*;
import java.text.DecimalFormat;

import tclib.behaviours.fhb.BehaviourInfo;

/**
 * This class writes the useful data collected during an experiment in a stream buffer.
 *  
 * @author Denis Remondini
 * @version	1.0		26 March 2004
 */
public class ExperimentData {

	/* Buffer where the data is stored */
	BufferedWriter outputBuffer;
	DecimalFormat fmt; // used to format all real numbers with only two decimals
	
	public ExperimentData(BufferedWriter buffer) {
		outputBuffer = buffer;
		fmt = new DecimalFormat();
		fmt.setMaximumFractionDigits(2); // we want to write only two decimal digits
	}
	
	/**
	 * Writes all the information about an experiment into the buffer
	 * 
	 * @param behInfo contains all the information that will be written on the buffer
	 * @throws IOException if there is an I/O error
	 */
	public void writeBehInformation(BehaviourInfo behInfo) throws IOException {
		outputBuffer.write(" BehaviourName: "+behInfo.getName());
		outputBuffer.write(" AntecedentPredicates: ");
		outputBuffer.write(behInfo.getAntecedentPredicates().toString());
		for (int i = 0; i < behInfo.getRulesNumber(); i++) {
			outputBuffer.write(" RuleName: "+behInfo.getRuleName(i));
			outputBuffer.write(" RuleAntecedent: "+fmt.format(behInfo.getRuleAntecedentValue(i)));
			outputBuffer.write(" RuleSubBehaviour: "+behInfo.getRuleSubBehaviour(i));
		}
		outputBuffer.write(" ControlValues_sent_to_robot: ");
		double values[] = behInfo.getValuesSentToRobot();
		outputBuffer.write("[rot, "+fmt.format(values[0])+" speed, "+fmt.format(values[1])+" ]");
		outputBuffer.newLine();
	}
	
	
}
