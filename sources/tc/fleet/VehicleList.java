/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas Quiñonero
 * (c) 2003 Humberto Martinez
 */

package tc.fleet;

import java.util.*;
import java.awt.*;
import javax.swing.table.*;

public class VehicleList extends AbstractTableModel
{
	public static final int			MAXVEHICLES	= 	20;
	
	protected String[]				columnNames	= { "Vehicle", "Position" };
	
	protected Hashtable<String, VehicleDesc>	rdesc;						// Vehicle descriptions
	protected Hashtable<String, VehicleData>	rpdata;						// Vehicle positioning data
	protected Hashtable<String, Color>		rcol;						// Vehicle colors
	
	protected LinkedList<String>			robotsid;
	protected Color[]			colors 		= { Color.RED.brighter() };
	protected int					indcolor;
	
	// Constructors
	public VehicleList ()
	{
		this (MAXVEHICLES);
	}
	
	public VehicleList (int n)
	{
		rdesc		= new Hashtable<String, VehicleDesc>(n);
		rpdata 		= new Hashtable<String, VehicleData>(n);
		rcol		= new Hashtable<String, Color>(n);
		
		robotsid	= new LinkedList<String> ();
	}
	
	// Instance methods
	public int size ()
	{
		return (rdesc.size());
	}
	
	public boolean add (String id, VehicleDesc robotdesc)
	{
		if (rdesc.put (id, robotdesc) == null)				// Is it a new config??
		{
			robotsid.add (id);
			rcol.put (id, colors[indcolor]);
			indcolor = (indcolor+1) % colors.length;
			
			return true;
		}
		return false;
	}
	
	public void delete (String robotkey)
	{
		if (rdesc.containsKey (robotkey))
		{
			robotsid.remove(robotkey);
			rdesc.remove (robotkey);
			rpdata.remove (robotkey);
			rcol.remove (robotkey);
		}
	}
	
	public boolean update (String id, VehicleData data)
	{
		if (!rdesc.containsKey (id))
			return false;
		
		rpdata.put (id, data);
		
		return true;
	}
	
	/* TODO Check the integrity of VehicleList / RobotList
	 public VehicleDesc[] getVehicles ()
	 {
	 VehicleDesc rdescs[] = new VehicleDesc [rdesc.size()];
	 Enumeration<VehicleDesc> values = rdesc.elements();
	 int i;
	 
	 for (i=0; values.hasMoreElements(); i++)
	 rdescs[i] = values.nextElement();
	 
	 return (rdescs);    	
	 }
	 */   
	public String[] getIDs ()
	{
		String arr[] = new String[0];
		
		arr = robotsid.toArray (arr);
		return (arr);
	}
	
	public String getID (int i)
	{
		return (robotsid.get(i));
	}
	
	public VehicleDesc getVDesc (int i)
	{
		return (rdesc.get(robotsid.get(i)));
	}
	
	public VehicleDesc getVDesc (String id)
	{
		return (rdesc.get(id));	
	}
	
	public VehicleData getData (int i)
	{
		return (rpdata.get(robotsid.get(i)));
	}
	
	public VehicleData getData (String id)
	{
		return (rpdata.get(id));
	}
	
	public Color getColor (int i)
	{
		return (rcol.get(robotsid.get(i)));
	}
	
	public Color getColor (String id)
	{
		return (rcol.get(id));
	}
	
	public String toString ()
	{
		return (new String ("\tVehicleList content: "+robotsid+","+rdesc+", "+rcol));
	}
	
	//***************************
	//TABLE MODEL IMPLEMENTATION
	//***************************
	public String getColumnName (int col) 
	{ 
		return columnNames[col].toString(); 
	}
	
	public int getRowCount() { return (rdesc.size()); }
	
	public int getColumnCount() { return columnNames.length; }
	
	public Object getValueAt (int row, int column)
	{
		Object obj = null;
		
		switch (column)
		{
			case 0:
				obj = robotsid.get (row);
				break;
				
			case 1: 
				obj = rpdata.get(robotsid.get(row)); // Extract current UTM position
				break;
				
			default: break;
		}
		
		if (obj == null) return "N/A"; 		
		return obj;
	}		
}