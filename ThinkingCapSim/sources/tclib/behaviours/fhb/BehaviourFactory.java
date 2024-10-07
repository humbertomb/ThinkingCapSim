/*
 * @(#)BehaviourFactory.java		1.0 2003/11/28
 *
 * (c) 2003 Denis Remondini
 * (c) 2004 Humberto Martinez
 *
 */
package tclib.behaviours.fhb;

import java.util.*;


/**
 * This abstract class provides a way to create new behaviours. Each behaviour has to extend the abstract
 * Behaviour class and can be already compiled or not.
 *
 * @version	1.0		28 Nov 2003
 * @author 	Denis Remondini
 */
public abstract class BehaviourFactory {

	/**
	 * Indicates the path where is the source code of the behaviour requested.
	 */
	public static String sourcePath;

	/**
	 * Indicates the folder that contains the compiled classes.
	 */
	public static String outputPath;

	/**
	 * Indicates the package that contains the behaviour that you want to create.
	 */
	public static String behPackage;

	/*
	 * This method has to be implemented from those behaviours that want to be created by this factory.
	 * It is suggested to add this piece of code to each behaviour you want to create with this factory:
	 *
	 * private static class Factory extends BehaviourFactory {
	 *   protected Behaviour create() {
	 *  	return new <NameOfYourBehaviourClass>();
	 *	 }
	 *  }
	 *
	 * static {
	 * 		BehaviourFactory.addFactory("<NameOfYourBehaviourClass>",
	 *                                  new <NameOfYourBehaviourClass>.Factory());
	 * }
	 *
	 */
	protected abstract Behaviour create();

	/* Maps the name of the behaviour to its class definition */
	private static Map factories = new HashMap();


	/**
	 * Memorizes that one factory object is associated with its identifier
	 * @param id the identifier. It has to be the name of the class that the factory object will create.
	 * @param beh the factory object
	 */
	public static void addFactory(String id, BehaviourFactory beh) {
		factories.put(id, beh);
	}


	/**
	 * Creates a new behaviour object
	 * @param id the name of the class that has to be instantiated
	 * @return the behaviour object requested
	 */
	public static final Behaviour createBehaviour(String id)
	{
		return createBehaviour (id, false);
	}

	/**
	 * Creates a new behaviour object
	 * @param id the name of the class that has to be instantiated
	 * @param recompile indicates if the class requested has to be recompiled before to be loaded.
	 * @return the behaviour object requested
	 */
	public static final Behaviour createBehaviour(String id, boolean recompile)
	{
		/*
		 * Checks if it's necessary to recompile the source code of the requested class
		 * and if that one has been already loaded.
		 */
		if ( (recompile) || (!factories.containsKey(id)) ) {
			try {
				if (recompile)
					factories.remove(id);

				/* Load the class dynamically */
				load(id,recompile);
			} catch(ClassNotFoundException e) {
				System.out.println("--[BehFactory] Class not found");
			}

			/* checks if the class loaded has registered itself in the factory */
			if(!factories.containsKey(id))
				System.out.println("--[BehFactory] Error loading class");
		}
		/* returns the behaviour object requested */
		return ((BehaviourFactory)factories.get(id)).create();

	}

	/* This method works in this way:
	 * 		- if recompiled variable is true then it tries to recompile the class
	 * 		- it tries to load the class searching in the classpath
	 * 		  and in the behPackage directory
	 * 		- if it cannot load the class, it tries to compile it and then tries
	 * 		  to reload it (maybe the source class was present but not the compiled
	 * 		  version)
	*/
	private static void load(String id, boolean recompile) throws ClassNotFoundException {

		/* Creates the personalized loader */
		SimpleClassLoader loader = new SimpleClassLoader(outputPath);


		if (recompile) {
			
			/* Compiles the source code storing the result in the outputPath folder */
			int compileReturnCode =	com.sun.tools.javac.Main.compile(
										new String[] {"-d",outputPath,sourcePath+id + ".java"});
			
			if (compileReturnCode == 0)
				System.out.println("  [BehFactory] "+id+" class compiled correctly");
			else
				System.out.println("--[BehFactory] Error in the "+id+" class compiling");
		}

		try {
			/* load class through the personalized loader */
			Class.forName(behPackage+"."+id,true,loader);
		}
		catch (ClassNotFoundException e) {
//			System.out.println("DEBUG: classNotFoundExcpetion: "+e.toString());
			
			/* Compiles the source code storing the result in the outputPath folder */
			int compileReturnCode = com.sun.tools.javac.Main.compile(
										new String[] {"-d",outputPath,sourcePath+id + ".java"});

			if (compileReturnCode == 0)
				System.out.println("  [BehFactory] "+id+" class compiled correctly");
			else
				System.out.println("--[BehFactory] Error in the "+id+" class compiling");

			/* load class through the personalized loader */
			Class.forName(behPackage+"."+id,true,loader);
		}
	}

	/*
	 * Loads the loading configuration from a configuration file. It configures the folder where has to be
	 * searched the source code, the folder where has to be stored the compiled code and the name of the
	 * package that contains the behaviours you want to create.
	 */
	public static void loadConfiguration (String prop)
	{
		StringTokenizer			st;

		st		= new StringTokenizer (prop, ", \t");
		BehaviourFactory.behPackage = st.nextToken ();
		BehaviourFactory.sourcePath = st.nextToken ();
		BehaviourFactory.outputPath = st.nextToken ();
	}
}
