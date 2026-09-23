package tclib.behaviours.hfsm;

import java.util.ArrayList;

/**
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @version 0.1
 * @date 2-2005
 * 
 * @see class State
 */
public class MetaState extends State {
	/**
	 * list of unreachable states in meta state used in validation
	 */
	protected ArrayList unreachableStatesList;
	/**
	 * extern metaState
	 */
	protected boolean extern;
	/**
	 * path extern metaState
	 */
	protected String pathExtern;
	/**
	 * initial state of meta state
	 */
	protected Object initialState;
	/**
	 * states/meta states in meta state
	 */
	protected ArrayList states;	

	/**
	 * empty Object constructor
	 */
	public MetaState(int i) {
		super(i);
		this.states = new ArrayList();
		this.verified = false;
		this.id=i;
	}
	/**
	 * Object constructor
	 * 
	 * @param n - name of object
	 */
	public MetaState(String n, int i) {
		super(n,i);
		this.initialState = null;
		this.states = new ArrayList();
		this.verified = false;
		this.id=i;
	}
	
	//SET METHODS
	/**
	 *  set initial state method
	 * 
	 *@param  a new initial state
	 */
	public void setInitialState(State a){
		this.initialState = a;
		this.verified = false;
	}
	
	/**
	 *  set initial state method
	 * 
	 *@param  a new initial state
	 */
	public void setInitialState(MetaState a){
		this.initialState = a;
		this.verified = false;
	}
	
	
	/**
	 *  get path extern if it is a external metastate
	 *
	 *@return extern - path
	 */
	
	public String getPathExtern() {
		return this.pathExtern;
	}
	
	/**
	 *  get initial state
	 *
	 *@return initial state
	 */
	public Object getInitialState() {
		return this.initialState;
	}
	
	// verification methods
	
	/**
	 * verifies meta state
	 * and forms the error string
	 * @return true if no errors found
	 */
	protected boolean verificateMetaState() {
		boolean r = true;
		if(this.states.size()==0){
			this.error += "ERROR in Meta State '"+this.name+"' : Meta State is empty.\n";
			r= false;
		}
		if(this.initialState == null){
			this.error += "ERROR in Meta State '"+this.name+"' : Meta State has no initial state.\n";
			r= false;
		}
		return r;
	}
	
	protected boolean unreachableStatesManager(){
		boolean r=true;
		Object temp;
		
		for(int i=0;i<this.unreachableStatesList.size();i++) {
			temp = this.unreachableStatesList.get(i);		
			if(!((State)temp).equals(this.getInitialState())) {
				this.error+= "ERROR in Meta State '"+this.name;
				if(State.isState(temp)) //is State
					this.error += "' : Unreachable state '"+ ((State)temp).getName()+"'.\n";
				else //is MetaState
					this.error += "' : Unreachable meta state '"+ ((MetaState)temp).getName()+"'.\n";
				r=false;
			}
		}
		return r;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getMetaStateCount() {
		int returnValue=0;
		
		for(int i=0; i<this.states.size(); i++)
			if(!State.isState(this.states.get(i)))
				returnValue++;
		return returnValue;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getStateCount() {
		return this.states.size()-this.getMetaStateCount();
	}
	
	/**
	 * 
	 * @param s
	 */
	public void addState(Object s){
		this.states.add(s);
	}
	
	/**
	 * 
	 * @param s
	 */
	public void removeState(Object s){
		if(s.equals(this.initialState))
			this.initialState=null;
		
		this.states.remove(s);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getStatesSize() {
		return this.states.size();
	}
	
	/**
	 * 
	 */
	public void resetStates(){
		this.states=new ArrayList();
		this.initialState=null;
	}
	
	/**
	 * 
	 * @param i
	 * @return
	 */
	public Object getState(int i) {
		return this.states.get(i);
	}

	/**
	 * 
	 * @return
	 */
	public int getTransitionCount(){
		int returnValue = 0;
		Object temp;
		
		for(int i=0;i<this.states.size();i++){
			temp = this.states.get(i);
			
			if(!State.isState(temp))
				returnValue += ((MetaState)temp).transitions.size();
			else
				returnValue += ((State)temp).transitions.size();	
		}			
		return returnValue;
	}
	
	public ArrayList getStatesList(){
		return this.states;
	}
	
	/**
	 *  get if it is a external metastate
	 *
	 *@return true if it is a external metastate
	 */
	public boolean isExtern() {
		return this.extern;
	}
	
	/**
	 *  set if it is a external metastate
	 *
	 *@param extern true if it is a external metastate
	 */
	public void setExtern(boolean extern) {
		this.extern=extern;
	}
	
	public static boolean isMetaState(Object o){
		String[] aux;
		int n;
		
		aux= o.getClass().getName().split("[.]");
		n = aux.length;
		if(aux[n-1].equals("MetaState"))
			return true;
		else return false;
	}
}


