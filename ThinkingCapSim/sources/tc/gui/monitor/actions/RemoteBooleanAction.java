/*
 * (c) 2003 Bernardo Canovas
 * (c) 2004 Humberto Martinez
 */
  
package tc.gui.monitor.actions;

public class RemoteBooleanAction extends RemoteAction implements java.io.Serializable
{
	/** Name of the selected value */
	private boolean checked;

	public RemoteBooleanAction (String name, String method,boolean checked)
	{
		super(name,method);
		this.params=new Class[1];
		try {
			this.params[0]=Class.forName("java.lang.Boolean");
		} catch (Exception e) {e.printStackTrace();};
		this.checked=checked;
		this.type=BOOLEAN_ACTION;
	}
	
	public void setChecked (boolean value)
	{
		checked=value;
	}
	
	public boolean isChecked()
	{
		return checked;
	}
}