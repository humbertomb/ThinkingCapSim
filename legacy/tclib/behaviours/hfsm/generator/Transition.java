package tclib.behaviours.hfsm.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


/**
 * @author Daniel Garc�a Nebot
 * @author Elad Rodriguez �lvaro
 * @author Miguel Cazorla
 * @version 1.0
 * @date 2-2006
 */
public class Transition extends chaos.fsm.classes.Transition{
	
	//methods
	/**
	 * empty object constructor
	 */
	public Transition(int i) {
		super(i);
	}
	
	/**
	 * object constructor
	 * @param n - name of object
	 * @param a arrival state of transtion
	 */
	public Transition(Object a, String n,int i) {
		super(a,n,i);
	}
	
	/**
	 * get test code of transition from file
	 * 
	 * @return code implemented by user in file
	 */
	public String getTestCode(String p){


		String pathCode = this.getPathTestCode(p);
		File f = new File(pathCode);
		String s ="", tab="\t";
		
		if(f.exists()) 
			try {
				BufferedReader in = new BufferedReader(new FileReader(f));
				char aux[] = new char[(int)f.length()];
				in.read(aux,0,(int)f.length());
 
				s = new String(aux);
				in.close();
				s = tab.concat(s);
				s = s.replaceAll("\n", "\n"+tab);
			} catch (Exception e) {
				System.out.println("Error opening file: '"+pathCode+"'");
			}
		return s;
		
	}
	
	/**
	 * get test code of transition from file
	 * 
	 * @return code implemented by user in file
	 */
	public String getDoCode(String p){
		String pathCode = this.getPathDoCode(p);
		File f = new File(pathCode);
		String s ="", tab="\t";
		
		if(f.exists())
			try {
				BufferedReader in = new BufferedReader(new FileReader(f));
				char aux[] = new char[(int)f.length()];
				in.read(aux,0,(int)f.length());
 
				s = new String(aux);
				in.close();
				s = tab.concat(s);
				s = s.replaceAll("\n", "\n"+tab);
			} catch (Exception e) {
				System.out.println("Error opening file: '"+pathCode+"'");
			}
		return s;
	}
	
	/**
	 * get global path of Test file of this state
	 */
	public String getPathTestCode(String PATH){
		return PATH+"trans"+this.id+"_Test.acc";
	}
	
	/**
	 * get global path of Do file of this state
	 */
	public String getPathDoCode(String PATH){
		return PATH+"trans"+this.id+"_Do.acc";
	}
}
