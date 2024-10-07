/*
 * (c) 2003 Bernardo Canovas
 * (c) 2004 Humberto Martinez
 */
  
package tc.gui.monitor.actions;
 
import java.util.*;
  
public class RemoteMultivalueAction extends RemoteAction implements java.io.Serializable
{
	/** Name of the selected value */
	private String itemSelected;
	/** Possible values of the argument and their identificatives names */
	private Hashtable values;

	public RemoteMultivalueAction (String name, String method,Class param)
	{
		super (name,method);
		this.params=new Class[1];
		this.params[0]=param;
		this.values=new Hashtable();
		this.type=MULTIVALUE_ACTION;
	}
	
	/** Adds a new pair (description, value) */
	public void addValue(String desc,Object value, boolean selected)
	{
		values.put(desc,value);
		if (selected)
			itemSelected=desc;
	}	
	
	public void setItemSelected (String item)
	{
		itemSelected=item;
	}
	
	public String getItemSelected ()
	{
		return itemSelected;
	}
	
	public Hashtable getValues()
	{
		return values;
	}
}