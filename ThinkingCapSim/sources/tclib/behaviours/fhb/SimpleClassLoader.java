/*
 * SimpleClassLoader.java - a bare bones class loader.
 *
 * Copyright (c) 1996 Chuck McManis, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * CHUCK MCMANIS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. CHUCK MCMANIS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package tclib.behaviours.fhb;

import java.util.Hashtable;
import java.io.FileInputStream;

public class SimpleClassLoader extends ClassLoader {
	
	/* map containing the classes already returned */
	private Hashtable classes = new Hashtable();
	/* folder where the class loader can find the class files */
	private String classImplementationPath;

	/**
	 * Constructs a simple class loader
	 * @param classImplementationPath folder where the class loader can find the class files
	 */
	public SimpleClassLoader(String classImplementationPath) {
		this.classImplementationPath = classImplementationPath;
	}

	/* Reads the class from the file stored in the repository */
	private byte getClassImplFromFileSystem(String className)[] {
//		System.out.println("DEBUG: Fetching the implementation of "+className);
		byte result[];
		
		try {
			FileInputStream fi = new FileInputStream(classImplementationPath+"/"+className.replace('.','/')+".class");
			result = new byte[fi.available()];
			fi.read(result);
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Loads a class
	 * @param className the class name
	 * @throws ClassNotFoundException if the class is not found
	 */
	public Class loadClass(String className) throws ClassNotFoundException {
		return (loadClass(className, true));
	}

	/**
	 * Loads a class
	 * @param className the class name
	 * @param resolveIt if true it causes any classes that are referenced by the 
	 * 				 	loaded class explicitly to be loaded and a prototype object
	 * 					for this class to be created; then, it invokes the verifier  
	 * 					to do dynamic verification of the legitimacy of the bytecodes 
	 * 					in this class
	 * @throws ClassNotFoundException if the class is not found
	 */
	public synchronized Class loadClass(String className, boolean resolveIt)
		throws ClassNotFoundException {
		Class result;
		byte  classData[];

//		System.out.println("DEBUG: Load class : "+className);

		/* It takes a class name and searches a local hash table that our class 
		 * loader is maintaining of classes it has already returned. It is 
		 * important to keep this hash table around since you must return the same 
		 * class object reference for the same class name every time you are asked 
		 * for it. Otherwise the system will believe there are two different 
		 * classes with the same name and will throw a ClassCastException whenever 
		 * you assign an object reference between them. 
		 * It's also important to keep a cache because the loadClass() method is 
		 * called recursively when a class is being resolved, and you will need to 
		 * return the cached result rather than chase it down for another copy.
		 */ 
		result = (Class)classes.get(className);
		if (result != null) {
//			System.out.println("DEBUG: Returning cached result.");
			return result;
		}

		/* Check with the primordial class loader */
		try {
			result = super.findSystemClass(className);
//			System.out.println("DEBUG: Returning system class "+className+" (in CLASSPATH).");
			return result;
		} catch (ClassNotFoundException e) {
			System.out.println("--[ClassLoader] Not a system class.");
		}

		/* Try to load it from our repository */
		classData = getClassImplFromFileSystem(className);
		if (classData == null) {
			throw new ClassNotFoundException();
		}

		/* Define it (parse the class file) */
		result = defineClass(null,classData, 0, classData.length);
		if (result == null) {
			throw new ClassFormatError();
		}

		if (resolveIt) {
			resolveClass(result);
		}
		
		classes.put(className, result);
		System.out.println("  [ClassLoader] Returning newly loaded class "+className);
		return result;
	}
}

