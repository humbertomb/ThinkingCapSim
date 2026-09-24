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
	private static Map<String, BehaviourFactory> factories = new HashMap<String, BehaviourFactory>();


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
		/* returns the behaviour object requested, or nothing when it never loaded:
		 * whoever asked for it runs without it (FHBController does nothing at all
		 * until it has one) instead of being brought down by it */
		BehaviourFactory		factory = factories.get(id);

		if (factory == null)
		{
			System.out.println("--[BehFactory] Behaviour <" + behPackage + "." + id + "> is not there: nothing to run");
			return null;
		}
		return factory.create();
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

		/* Creates the personalized loader: what was just compiled is what is loaded */
		SimpleClassLoader loader = new SimpleClassLoader(outputPath, recompile);


		if (recompile)
			compile(id);

		try {
			/* load class through the personalized loader */
			Class.forName(behPackage+"."+id,true,loader);
		}
		catch (ClassNotFoundException e) {
//			System.out.println("DEBUG: classNotFoundExcpetion: "+e.toString());

			/* the source may be there without its class: compile it and look again */
			compile(id);

			/* load class through the personalized loader */
			Class.forName(behPackage+"."+id,true,loader);
		}
	}

	/**
	 * Compiles the source of a behaviour into the outputPath folder, with the
	 * compiler of the runtime the modules are running in
	 * ({@link javax.tools.ToolProvider#getSystemJavaCompiler}) and against their own
	 * classpath, so that what is compiled here is the same code, of the same
	 * version, as everything that is already loaded.
	 *
	 * Whatever the compiler has to say is said here, prefixed as everything else, and
	 * a source that is not there or a runtime with no compiler in it (a JRE) is said
	 * out loud too: it is not for this to bring down whoever asked for a behaviour.
	 *
	 * @param id the name of the class, as the behaviour is named in the deployment
	 * @return whether it compiled
	 */
	private static boolean compile(String id) {

		javax.tools.JavaCompiler	javac = javax.tools.ToolProvider.getSystemJavaCompiler();
		java.io.File				src = new java.io.File(sourcePath, id + ".java");

		if (javac == null) {
			System.out.println("--[BehFactory] No compiler in this runtime: " + id + " has to be compiled with the project");
			return false;
		}
		if (!src.isFile()) {
			System.out.println("--[BehFactory] Cannot find the source of " + id + " at <" + src.getPath() + ">");
			return false;
		}
		new java.io.File(outputPath).mkdirs();

		java.io.ByteArrayOutputStream	said = new java.io.ByteArrayOutputStream();
		int								code = javac.run(null, said, said,
										  "-d", outputPath,
										  "-classpath", System.getProperty("java.class.path"),
										  src.getPath());

		for (String line : said.toString().split("\\r?\\n"))
			if (line.trim().length() > 0)		System.out.println("  [BehFactory] " + line);

		if (code == 0)
			System.out.println("  [BehFactory] "+id+" class compiled correctly");
		else
			System.out.println("--[BehFactory] Error in the "+id+" class compiling");
		return code == 0;
	}

	/*
	 * Loads the loading configuration from a configuration file. It configures the folder where has to be
	 * searched the source code, the folder where has to be stored the compiled code and the name of the
	 * package that contains the behaviours you want to create.
	 */
	public static void loadConfiguration (String prop)
	{
		StringTokenizer			st;

		if (prop == null)
			throw new IllegalArgumentException ("BehaviourFactory: missing FACT property (package, source path, output path) of the behaviour controller module");
		st		= new StringTokenizer (prop, ", \t");
		BehaviourFactory.behPackage = st.nextToken ();
		BehaviourFactory.sourcePath = st.nextToken ();
		BehaviourFactory.outputPath = st.nextToken ();
	}
}
