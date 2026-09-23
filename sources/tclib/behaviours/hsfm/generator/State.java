
package chaos.fsm.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


/**
 * @author Daniel Garcia Nebot
 * @author Elad Rodriguez Alvaro
 * @author Miguel Cazorla
 * @version 1.0
 * @date 2-2006
 */
public class State extends chaos.fsm.classes.State{
	
	//methods
	/**
	 * empty Object constructor
	 */
	public State(int i) {
		super(i);
	}
	/**
	 * Object constructor
	 * 
	 * @param n - name of object
	 */
	public State(String n, int i) {
		super(n,i);
	}
	
	/**
	 * get whatToDo code of State from file
	 * 
	 * @return code implemented by user in file
	 */
	public String getCode(String p){
		String pathCode = this.getPathCode(p);
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
				
			} catch (Exception e){ System.out.println("Error opening file: '"+pathCode+"'"); }
			else
				System.out.println ("--[LUA] state <"+p+"> has no code file <"+pathCode+">");
		return s;
	}
	
	/**
	 * createwhatToDo file for this state
	 * 
	 * @param meta name of meta state containing this state
	 */
	public void createFile(String meta, String p){
		String pathCode = this.getPathCode(p);
		File f = new File(pathCode);
		
		try{
			f.createNewFile();		
		}catch(Exception e){
			System.out.println("Error can't create code file for state "+ this.name);
		}	
	}

	/**
	 * get global path of whatToDo file of this state
	 * @param path path of meta state containing this state
	 * @return name of whatToDo file of this state
	 */
	public String getPathCode(String PATH){
		return PATH+"state"+this.id+".acc";		
	}
}
