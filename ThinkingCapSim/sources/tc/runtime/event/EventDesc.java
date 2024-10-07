/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.event;

import java.util.*;
import java.lang.reflect.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class EventDesc extends Object
{	
	// Variables to describe the event receivers
	public String				key;				// Type of Linda tuple Key
	public String				classn;			// Class name for the Linda tuple item
	public String				methodn;			// Local method name to catch/parse the event

	// Internal variables for run-time execution
	protected StdThread			object;
	protected Method				method;
	protected Object[]			params;

	// Constructors
	protected EventDesc ()
	{
	}
  	
	public EventDesc (StdThread object, Linda linda, String props)
	{
		StringTokenizer	st;

		st		= new StringTokenizer (props, " \t");
		key		= st.nextToken ();
		classn	= st.nextToken ();
		methodn	= st.nextToken ();
		
		configure (object, linda);
	}
		
	// Instance methods
	protected void configure (StdThread object, Linda linda)
	{
		Class[]				types;
		Class				pclass;
		
		this.object		= object;
		
		try
		{
			types 		= new Class[2];
			types[0] 	= Class.forName ("java.lang.String");
			types[1] 	= Class.forName (classn);
			pclass		= object.getClass ();
			method	 	= pclass.getMethod (methodn, types);
			params 		= new Object[2];
			linda.register (new Tuple (key), object);
		}
		catch (Exception e) 					
		{ 
			System.out.println ("--[Event] Error registering tuple " + this + " in module " + object.tdesc.preffix);
			e.printStackTrace (); 
		}
	}
	
	public void unregister (Linda linda)
	{
		linda.unregister (new Tuple (key), object);
	}
	
	public synchronized void notify (String space, Item item)
	{
		if (params == null)						return;
		
		try
		{
			params[0]	= space;
			params[1]	= item;
			method.invoke (object, params);			
		}
		catch (InvocationTargetException e)	{
		    System.out.println ("--[Event] Error notifying tuple " + this + " in module " + object.tdesc.preffix + " ... Invocation ERROR");
		    System.out.println("space="+space);
		    e.printStackTrace();
		}	
		catch (IllegalAccessException e)		{ 
		    System.out.println ("--[Event] Error notifying tuple " + this + " in module " + object.tdesc.preffix + " ... Access ERROR"); 
		    e.printStackTrace();
		}	
	
	}
	
	public String toString ()
	{
		String 			str;
		
		str		= "[" + key + " => " + methodn + " (" + classn + ")]";
				
		return str;
	}
}

