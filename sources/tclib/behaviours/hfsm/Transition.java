
package tclib.behaviours.hfsm;

/**
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @version 0.1
 * @date 2-2005
 */
public class Transition {

	// protected vars

	/**
	 *  transition priority
	 */
	protected int priority;
	/**
	 *  transition name
	 */
	protected String name;
	/**
	 * arrival state
	 */
	protected Object arrivalState;
	
	/**
	 * id
	 */
	protected int id;
	
	//methods
	/**
	 * empty object constructor
	 */
	public Transition(int i)
	{
		this.id=i;
		this.arrivalState = null;
		this.name = new String();
		this.priority = 1;
	}
	
	/**
	 * object constructor
	 * @param n - name of object
	 * @param a arrival state of transtion
	 */
	public Transition(Object a,String n, int i)
	{
		this.id=i;
		this.arrivalState = a;
		this.priority = 1;
		this.name = n;

	}
	
	//SET METHODS
	/**
	 *  set transition name method
	 * 
	 *@param  nom new name of transition
	 */
	public void setName(String nom)
	{
		this.name = nom;
	}
	/**
	 *  set transition priority method
	 * 
	 *@param  n  new priority of trasition
	 */
	public void setPriority(int n)
	{
		this.priority = n;
	}
	/**
	 *  set arrival state method
	 * 
	 *@param  a new arrival state
	 */
	public void setArrivalState(Object a){
		this.arrivalState = a;
	}
	
	//GET METHODS
	
	/**
	 *  get name of transition
	 *
	 *@return name of state
	 */
	public String getName()
	{
		return this.name;
	}
	/**
	 *  get transition priority method
	 * 
	 *@return  priority of trasition
	 */
	public int getPriority()
	{
		return this.priority;
	}
	/**
	 *  get arrival state
	 *
	 *@return arrival state
	 */
	public Object getArrivalState()
	{
		return this.arrivalState;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getId(){
		return this.id;
	}
	
	/**
	 * 
	 */
	public String toString(){
		return this.name;
	}
	
	public static boolean isTransition(Object o){
		
		String[] aux;
		int n;
		
		aux= o.getClass().getName().split("[.]");
		n = aux.length;
		
		if(aux[n-1].equals("Transition"))
			return true;
		else return false;
		
		
		
	}
	
	public String getTestfunctionName() {
		return "transition_"+this.name+"_test";
	}
	
	public String getDofunctionName() {
		return "transition_"+this.name+"_do";
	}
}
