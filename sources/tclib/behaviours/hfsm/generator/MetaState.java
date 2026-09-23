
package tclib.behaviours.hfsm.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import chaos.fsm.gui.Utils;


/**
 * @author Daniel Garc�a Nebot
 * @author Elad Rodriguez �lvaro
 * @author Miguel Cazorla
 * @version 1.0
 * @date 2-2006
 */
public class MetaState extends chaos.fsm.classes.MetaState {
	/**
	 * path to create files
	 */
	public static String PATH = null;
	
	/**
	 * local path containing all files
	 */
	protected String path;
	
	/**
	 * empty Object constructor
	 */
	public MetaState(int i) {
		super(i);
	}
	/**
	 * Object constructor
	 * 
	 * @param n - name of object
	 */
	public MetaState(String n, int i) {
		super(n, i);
	}


	/**
	 * 
	 * @param code
	 * @param include
	 */
	protected void generateStatesFiles(String code, String include){
		String NAME = this.name.toUpperCase();
		
		String s="\n#include \""+this.name+".h\"\n";
			
		s+="#include \""+ include +"Hbm.h\"\n";
		s+="#include \""+ include + "lpsconfig.h\"\n";
		s+="#include \""+ include +"Behaviors.h\"\n";
		
		s+="#ifndef META_"+NAME+"_WHATTODO_CC\n#define META_"+NAME+"_WHATTODO_CC\n\n";

		s+= code;
		
		s+= "#endif //META_"+NAME+"_WHATTODO_CC\n";
		 
		this.saveFile(PATH+this.path+"/"+this.name+"_what_to_do.cc", s);
	}
	

	/**
	 * 
	 * @param code
	 * @param include
	 */
	protected void generateTransitionsFiles(String code, String include){
		
		String NAME = this.name.toUpperCase();
		String s="\n#include \""+this.name+".h\"\n";
		
		s+="#include \""+ include +"Hbm.h\"\n";
		s+="#include \""+ include + "lpsconfig.h\"\n";
		s+="#include \""+ include +"Behaviors.h\"\n";
		
		s+="#ifndef META_"+NAME+"_TRANSITIONS_CC\n#define META_"+NAME+"_TRANSITIONS_CC\n\n";	
		s+= code;
		
		s+= "#endif //META_"+NAME+"_TRANSITIONS_CC\n";
		 
		this.saveFile(PATH+this.path+"/"+this.name+"_transitions.cc", s);
		
	}
	
	/**
	 * 
	 * @param stateConsts
	 * @param whatToDoFunctions
	 * @param transitionFunctions
	 * @param metaStateDeclarations
	 * @param metaStateIncludeClasses
	 * @param include
	 */
	protected void generateDotHFiles
		(String stateConsts,String whatToDoFunctions,String transitionFunctions,
		String metaStateDeclarations, String metaStateIncludeClasses, String metaStateClasses,String include,
		int niv){
		
		String NAME = this.name.toUpperCase();
		String s = "";
		
		s+="#ifndef META_"+NAME+"_H\n#define META_"+NAME+"_H\n\n";
		
		if(niv==0)
			s+="#include \"Hfsm.h\"\n"; //../Hfsm.h
		
		s+="#include \""+ include +"Hbm.h\"\n";
		
		//include subclases
		s+= metaStateIncludeClasses;

		s+= metaStateClasses;
		s+= "class Hbm;\n\n";
		
		//private variables
		for(int i=0; i<Utils.privVars.size(); i++){
			String [] vars=(String [])Utils.privVars.get(i);				
			if(!vars[0].equals("") && !vars[1].equals("") && !vars[2].equals("")){
				int num=Integer.parseInt(vars[2]);				
				if(num==1){
					if(niv==0){
						s+="\t\t static "+vars[1]+" "+vars[0];
						if(!vars[3].equals(""))
							s+="="+vars[3];
						s+=";\n";
					}
				}
				else {
					if (niv==0)
						s+="\t\t static "+vars[1]+" "+vars[0]+"["+num+"];\n";
				}
			}
		}
		s+= "\nclass "+this.name;
		
		if(niv==0)
			s+=" : public Hfsm";
		
		s+="\n{\n";
		
		s+="\tpublic:\n";
		s+="\t\t"+this.name+"(Hbm *pHbm);\n";
		s+="\t\t~"+this.name+"();\n\n";
		
		s+=metaStateDeclarations+"\n";
		
		s+="\t\tvoid Init_behavior(void);\n"; 
		s+="\t\tvoid Reinit_behavior(void);\n";
		s+="\t\tvoid Apply_behavior(void);\n\n"; 
		
		s+="\n";
	
		s+= transitionFunctions+"\n";
		s+= whatToDoFunctions+"\n";
		
		s+="\t\tint state;\n";
		s+= stateConsts+"\n";
		
		s+="\t\tHbm *my_hbm;\n";
		
		s+= "};\n#endif //META_"+NAME+"_H\n";

		this.saveFile(PATH+this.path+"/"+this.name+".h", s);
	}

	/**
	 * 
	 * @param metaStateInits
	 * @param switchCodes
	 * @param include
	 */
	protected void generateDotCCFiles(String metaStateInits, String switchCodes, String include){
		
		String initialStateConstant = "";
		
		if(State.isState(this.initialState))
			initialStateConstant = "state_"+ ((State)this.initialState).getName();
		else
			initialStateConstant = "state_"+ ((MetaState)this.initialState).getName();
			
		
		String s="\n#include \""+this.name+".h\"\n";
		s+="#include \""+ include +"Hbm.h\"\n";

		s+="\n";
		
		s+="//\tConstructor\n";
		s+=this.name+"::"+this.name+"(Hbm *pHbm)\n";
		s+="{\n\tmy_hbm = pHbm;\n\n";
		
		s+= metaStateInits;

		s+="\n\tInit_behavior();\n";
		s+="}\n\n\n";
		
		s+="//\tDestructor\n";
		s+=this.name+"::~"+this.name+"()\n{\n";
		//s+="\tdelete my_hbm;\n";
      
		s+="}\n";

		s+="//\tInit_behavior\n";
		s+="void " + this.name + "::Init_behavior()\n";
		s+="{\n";
	    s+= "\tstate = "+ initialStateConstant + ";\n";
		s+="}\n";

		s+="\n";
		
		s+="//\tReinit_behavior\n";
		s+="void " + this.name + "::Reinit_behavior()\n";
		s+="{\n";
	    s+= "\tstate = "+ initialStateConstant + ";\n";
		s+="}\n";
		
		s+="//\tApply Behavior\n";
		
		s+="void " + this.name + "::Apply_behavior()\n";
		s+="{\n";
		
		s+= "\tswitch(state)\n";
		s+= "\t{\n";
		s+= switchCodes;
		s+="\t}\n";
		
		s+="}\n";
		
		this.saveFile(PATH+this.path+"/"+this.name+".cc", s);
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	// From here, LUA codes
	/////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	protected String generateLuaFiles (String switchCodes, String stateConsts, 
			String codes, String loadCodes, String ownCode, int level){
		String s = "";
		String varPriv = "\t-- Initialize private variables\n";
		ArrayList alreadyGen = new ArrayList();

		s += "-- Private variables\n";
		//private variables
		for(int i=0; i<Utils.privVars.size(); i++){
			String [] vars=(String [])Utils.privVars.get(i);				
			if(!vars[0].equals("") && !vars[1].equals("") && !vars[2].equals("")
					&& vars[4].equals(this.name)){ // Only the priv. vars owning to this MS are printed
				// First, check if the private variable was generated before
				boolean wasGen=false;
				for (int v=0; v<alreadyGen.size(); v++) {
					String[] auxVar=(String[])alreadyGen.get(v);
					if (auxVar[0].equals(vars[0]) && auxVar[4].equals(vars[4])) { 
						wasGen=true;
						break;
					}
				}
				if (!wasGen) {
					alreadyGen.add(vars);
					int num=Integer.parseInt(vars[2]);				
					if(num==1) {
						s += vars[0]+" = "+vars[3]+"\n";
						varPriv += "\t"+vars[0]+" = "+vars[3]+"\n";
					}
					else { 
						s += vars[0]+" = {}\n";
						varPriv += "\t"+vars[0]+" = {}\n";
					}
				}
			}
		}
		s += "\n";
		varPriv += "\n";
		
		// Load the definitions of the rest of Metastates belonging to this
		s += loadCodes;
		
		// It's necessary to declare it as a table
		s += "\n-- Definition of "+this.name+"\n"; 
		s += this.name + "={}\n\n";
		
		s += ownCode;
		s+= stateConsts+"\n\n";
		
		s+="-- Init_behavior\n";
		s+=this.name + ".Init_behavior = function()\n";

	    s+= "\tchaos.setCurrentState(";
	    if(State.isState(this.initialState))
	    	s+= this.name+".state_"+ ((State)this.initialState).getName()+")\n";
		else {
			s+= this.name+".state_"+ ((MetaState)this.initialState).getName()+")\n";
			s+= "\t"+((MetaState)this.initialState).getName()+".Init_behavior()\n";
		}
	    s+=varPriv;
		s+="end\n\n";
		
		s+="-- Apply Behavior\n";
		s+=this.name + ".Apply_behavior = function()\n";
		s+= switchCodes;
		s+="end\n\n";
		
		s+=codes;
		return s;
	}
	
	/**
	 *  ouputs file contents into file specified by path
	 *
	 *@param path 
	 *@param contents 
	 */
	protected void saveFile(String path, String contents) {
		File f = new File(path); 
		if(!f.exists()){		
			try{
				f.createNewFile();
			}catch (Exception e){
				System.out.println("Error saving file :"+ path);
			}
		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write(contents);
			out.close();
		} 
		catch (Exception e) {
			System.out.println("Error writting file");
		}
	}
	
	/**
	 *  creates meta state directory
	 * 
	 *@param  p new path
	 */
	public void setPath(String p) { 
		File f = new File(PATH+p);
		if(!f.isDirectory()) {	
			try{
				if(f.mkdirs()) this.path = p;
			}catch (Exception e){
				System.out.println("Error can't create meta state directory");
			}
		}
		else
			this.path = p;
	}
	
	/**
	 *  get path
	 * @return path of meta state
	 */
	public String getPath(){
		return this.path;	
	}	
}

