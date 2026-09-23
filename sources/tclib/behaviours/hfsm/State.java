package tclib.behaviours.hfsm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import chaos.fsm.generator.Transition;

/**
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @author Miguel Cazorla
 * @version 0.2
 * @date 5-2005
 */
public class State {
	/**
	 * has the state already been verified?
	 */
	protected boolean verified;
	/**
	 * is the state (and all its sub states in case of a meta state) correct?
	 */
	protected boolean correct;
	/**
	 *  state name
	 */
	protected String name;
	/**
	 * error or warning found in state by the verificator method
	 */
	protected String error;
	/**
	 * id
	 */
	protected int id;
	
	/**
	 * transitions
	 */
	protected ArrayList transitions;
	
	/**
	 * empty Object constructor
	 */
	public State(int i) {
		this.id=i;
		this.error ="";
		this.name = new String();
		this.transitions = new ArrayList();
	}
	/**
	 * Object constructor
	 * 
	 * @param n - name of object
	 */
	public State(String n, int i) {
		this.id = i;
		this.error="";
		this.name = new String(n);
		this.transitions = new ArrayList();
	}
		
	/**
	 *  set state name method
	 * 
	 *@param  nom  new name of state
	 */
	public void setName(String nom) {
		this.name = nom;
	}
	
	public void setVerified(boolean v){
		this.verified=v;
	}
	
	/**
	 *  get name of state
	 *
	 *@return name of state
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 *  get error
	 * @return error string of meta state
	 */
	public String getError() {
		return this.error;	
	}
	
	public String getWhatToDofunctionName() {
		return "whatToDo_in_state_"+this.name;
	}
	
	public String getConstName() {
		return "state_"+this.name;
	}
	
	public String getObjName() {
		return "obj_"+this.name;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * verifies state
	 * @param meta name of meta state containing this state
	 * @return true if state is correct
	 */
	public boolean isCorrect(String meta, ArrayList unreachableStatesList, ArrayList totalTransList){
		this.error="";
		this.correct=this.verificateTransitions(meta,unreachableStatesList, totalTransList);
		this.verified=true;
		return this.correct;
	}
	/**
	 * verifies transitions of state
	 * @param meta name of meta state containing this state
	 * @return true if all transitions in state are correct
	 */
	protected boolean verificateTransitions(String meta, ArrayList unreachableStatesList, ArrayList totalTransList){
		Transition temp;
		boolean r = true;
		
		for (int i=0;i<this.transitions.size();i++) {		
			temp = (Transition)this.transitions.get(i);
			
			if(temp.arrivalState == null){
				this.error+= "ERROR in Meta State '"+meta+"' : Transition '"+ ((Transition)temp).getName()+"' has no arrival state.\n";
				r=false;
			}
			else
				unreachableStatesList.remove(((Transition)temp).arrivalState);
			
			for (int j=i;j<totalTransList.size();j++)
				if(temp != totalTransList.get(j) &&
				   temp.getName().equals(((Transition)totalTransList.get(j)).getName())){
					this.error+= "ERROR in Meta State '"+meta+"' : Transition name repeated: '"+ ((Transition)temp).getName()+"'.\n";
					r=false;
					break;
				}
			totalTransList.add(temp);
		}
		return r;
	}

	public void sortTransitions() {
		Object [] transitionsArray;
		transitionsArray = this.transitions.toArray();
		Arrays.sort(transitionsArray, new TransitionComparator() );
		this.transitions = new ArrayList();
		for (int i=0;i<transitionsArray.length;i++)
			this.transitions.add((Transition)transitionsArray[i]);
	}
	
	public String toString(){
		return this.name;
	}
	
	public static boolean isState(Object o){
		String[] aux;
		int n;
		
		aux= o.getClass().getName().split("[.]");
		n = aux.length;
		if(aux[n-1].equals("State"))
			return true;
		else return false;
	}
	
	public int getTransitionsSize(){
		if (this.transitions==null)
			return 0;
		return this.transitions.size();
	}
	
	public Transition getTransition(int i){
		return ((Transition)this.transitions.get(i));
	}
	
	public ArrayList getTransitions(){
		return this.transitions;
	}
	
	public void addTransition(Transition t){
		this.transitions.add(t);
	}
	
	public void removeTransition(Transition t){
		this.transitions.remove(t);
	}
	
	public void cloneTransitions(ArrayList t){
		this.transitions=new ArrayList(t);
	}
}

class TransitionComparator implements Comparator {
	   // compares priority of transitions	
	   // e.g. +1 (or any +ve number) if a > b
	   // 0 if a == b
	   // -1 (or any -ve number) if a < b
	public final int compare (Object a, Object b) {
		return ((Transition)a).getPriority() - ((Transition)b).getPriority();
	}
}