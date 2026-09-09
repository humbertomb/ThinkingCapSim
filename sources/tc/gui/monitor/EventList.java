/*
 * Created on 27-ago-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.gui.monitor;

import javax.swing.table.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

class EventEntry
{
	public Long					stamp;
	public String				id;
	public int					type;
	public String				desc;
}

public class EventList extends AbstractTableModel
{
	static protected String[]		columnNames	= { "Count", "Stamp", "Robot", "Type", "Description" };
	static protected int			DEF_NUM		= 1000;
	
	protected EventEntry[]		events;
	protected int				num;
	
	// Constructors
	public EventList ()
	{
		events	= new EventEntry[DEF_NUM];
		num		= 0;
	}
	
	// Accessors
	public String	getColumnName (int col)	{ return columnNames[col]; }
	public int		getColumnCount()			{ return columnNames.length; }
	public int		getRowCount()			{ return num; }
	
	// Instance methods
	public Object getValueAt (int row, int col)
	{
		Object obj = null;
		
		switch (col)
		{
		case 0:		obj = new Integer (row);					break;			
		case 1:		obj = events[row].stamp;					break;			
		case 2:		obj = events[row].id;						break;			
		case 3:		obj = new Integer (events[row].type);		break;
		case 4:		obj = events[row].desc;					break;
		}
		
		if (obj == null) return "N/A"; 		
		return obj;
	}
	
	public void addRow (String id, long stamp, int type, String desc)
	{
		EventEntry		entry;
		EventEntry[]		tevents;
		
		entry		= new EventEntry ();
		entry.stamp	= new Long (stamp);
		entry.id		= id;
		entry.type	= type;
		entry.desc	= desc;

		if (num >= events.length)
		{
			tevents		= new EventEntry[events.length + DEF_NUM];
			System.arraycopy (events, 0, tevents, 0, events.length);
			events		= tevents;
		}
		
		events[num]	= entry;
		num ++;

		fireTableRowsInserted (num-1, num-1);
	}
}
