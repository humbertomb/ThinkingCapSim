/*
 * Created on 31-ago-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.gui.monitor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class EventListRenderer extends DefaultTableCellRenderer
{	
	static public int			COL_TYPE		= 3;
	
	// Constructors
	public EventListRenderer (JTable table)
	{
		super ();
		
		initColumnSizes (table);
	}
		
	// Instance methods		
	protected void initColumnSizes (JTable table)
	{
		AbstractTableModel model = (AbstractTableModel) table.getModel ();
		TableColumn column = null;
		Component comp = null;
		int headerWidth = 0;
		int cellWidth = 0;
		TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
		
		if (model.getRowCount () == 0)			return;
		
		for (int i = 0; i < model.getColumnCount (); i++)
		{
			column = table.getColumnModel().getColumn (i);
			
			comp = headerRenderer.getTableCellRendererComponent (null, column.getHeaderValue(), false, false, 0, 0);
			headerWidth = comp.getPreferredSize().width;
			
			comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(table, model.getValueAt(0, i), false, false, 0, i);
			cellWidth = comp.getPreferredSize().width;
							
			column.setPreferredWidth(Math.max(headerWidth, cellWidth));
		}
	}

	public Component getTableCellRendererComponent (JTable table, Object value, boolean sel, boolean focus, int row, int col)
	{
		JLabel				label;
		AbstractTableModel	model;
		Color				fcolor, bcolor;
		String				text;
		int					type;
		
		model	= (AbstractTableModel) table.getModel ();
		type		= ((Integer) model.getValueAt (row, COL_TYPE)).intValue ();
		
		// Set foreground color
		switch (type)
		{
		case ItemStatus.ALARM:		fcolor = Color.orange;	break;
		case ItemStatus.FAILED:		fcolor = Color.red;		break;		
		case ItemStatus.COMPLETED:
		default:						fcolor = table.getForeground ();
		}
		
		// Set background color
		if (sel)
			bcolor	= table.getSelectionBackground ();
		else
			bcolor	= table.getBackground ();
		
		// Set component text
		text		= value.toString ();
		if (col == COL_TYPE)
			text		= ItemStatus.typeToString (((Integer) value).intValue ());
		
		// Set up drawing component
		label	= new JLabel (text);
		label.setForeground (fcolor);
		label.setBackground (bcolor);
		label.setOpaque (true);
	
		return label;
	}
}
