/*
 * (c) 2003 Bernardo Canovas
 * (c) 2004 Humberto Martinez
 */
  
package tc.gui.monitor.actions;
  
public class RemoteFreeAction extends RemoteAction implements java.io.Serializable
{
	/** Name of the selected value */
	private String itemSelected;
	/** Descripions of the params */
	private String[] paramsDesc;

	public RemoteFreeAction (String name, String method,Class[] params)
	{
		super (name,method);
		int i;
		this.params=params;
		this.paramsDesc=new String[params.length];
		for (i=0;i<params.length;i++)
			paramsDesc[i]="";
		this.type=FREE_ACTION;
	}
	
	/** Adds a description for a parameter */
	public void setParamDesc(int order, String desc)
	{
		paramsDesc[order]=desc;
	}	
	
	/** Gets the description of a parameter */
	public String getParamDesc (int order)
	{
		return paramsDesc[order];
	}
	
	/** Gets the description of all parameters */
	public String[] getAllParamsDesc()
	{
		return paramsDesc;
	}		
}