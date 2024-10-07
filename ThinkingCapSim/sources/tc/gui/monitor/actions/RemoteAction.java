/*
 * (c) 2003 Bernardo Canovas
 * (c) 2004 Humberto Martinez
 */
  
 package tc.gui.monitor.actions;
 
 import java.io.*;
 
 public class RemoteAction implements Serializable
 {
	/** Indicates that the action has no arguments */
	public static final int EMPTY_ACTION=0;
	/** Indicates that the action has one only boolean parameter */
	public static final int BOOLEAN_ACTION=1;
	/** Indicates that the action has one parameter with discrete possible values */
	public static final int MULTIVALUE_ACTION=2;
	/** Indicates that the action has an undetermined number of parameters */
	public static final int FREE_ACTION=3;
	
	/** Name of the action */
	protected String name;
	/** Description of the action */
	protected String description;
	/** Method that realizes the action in the server */
	protected String method;
	/** Parameter types of the method */
	protected Class[] params;
	/** Type of the action */
	protected int type;
	
	/** Creates a free action with no arguments (an EMPTY_ACTION) */
	public RemoteAction(String name, String method)
	{
		this.name=name;
		this.method=method;
		this.description="";
		this.params=null;
		this.type=EMPTY_ACTION;
	}

	// Instance methods

	/** Returns the type of the action */
	public int getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public String getMethod()
	{
		return method;
	}

	public String getDescription()
	{
		return description;
	}

	public Class[] getParams()
	{
		return params;
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	public void setMethod(String method)
	{
		this.method=method;
	}
	
	public void setDescription(String desc)
	{
		this.description=desc;
	}
 }