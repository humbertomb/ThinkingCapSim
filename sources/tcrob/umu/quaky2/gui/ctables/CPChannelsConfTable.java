/**
 * @author Boyan Ivanov Bonev [bib at alu.ua.es]
 * @author Humberto Martinez Barbera
 * 
 * Copyright (c) 2004 University of Murcia (Spain) and Team Chaos, Sweden and Spain.
 * All Rights Reserved.
 * 
 */

package tcrob.umu.quaky2.gui.ctables;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import tcrob.umu.quaky2.*;

public class CPChannelsConfTable extends JPanel
{
	protected SoccerVision pam;
	protected ChannelsConfTableModel chsconf;
	protected JTable paramTable;
	protected JScrollPane paramScroll;
	protected ListSelectionModel paramSelect;
	protected CPColorTable ctable;
		
	public CPChannelsConfTable (CPColorTable rctable, SoccerVision pam)
	{
		int			csize, rsize;
		
		this.ctable	= rctable;
		this.pam		= pam;
		
		chsconf = new ChannelsConfTableModel();
		paramTable = new JTable(chsconf);
		paramTable.setDefaultRenderer (Object.class, new ChannelsConfRenderer ());
		paramTable.setGridColor (Color.lightGray);
		paramTable.setShowGrid (true);
		paramTable.setShowHorizontalLines (true);
		paramTable.setShowVerticalLines (false);
		paramTable.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
//		paramTable.setRowSelectionInterval(ctable.getSelectedChannel(), ctable.getSelectedChannel());
		paramSelect = paramTable.getSelectionModel();		
		paramScroll = new JScrollPane(paramTable);
		paramScroll.setHorizontalScrollBarPolicy (JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		csize = initColumnSizes (paramTable);
		rsize = initRowSizes (paramTable);

		paramSelect.addListSelectionListener (new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent e) 
			{
				if (e.getValueIsAdjusting ())				return;

				ctable.setSelectedChannel (paramSelect.getAnchorSelectionIndex ());
			}
		});	
		setLayout (new GridLayout (1, 1));
		setPreferredSize (new Dimension (csize, rsize));
		add (paramScroll);
		setVisible(true);
	}
	
	public class ChannelsConfRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent (JTable table, Object value, boolean sel, boolean focus, int row, int column)
		{
			if (value instanceof String)
			{
				JLabel		label;
				
				label	= new JLabel (((String) value).substring (0, 1)+" ");
				label.setOpaque (true);
				label.setBackground (pam.vconfig.channels.at(row).color);

				return label;
			}
			else
				return super.getTableCellRendererComponent (table, value, sel, focus, row, column);
		}
	}
	
	public class ChannelsConfTableModel extends AbstractTableModel
	{
		protected String[]	columnNames	= { "Seg", "Blo", "Col", "Thr", "Gap" };
		protected Class<?>[]	columnClass	= { Boolean.class, Boolean.class, String.class, Integer.class, Integer.class };
		protected boolean[]	columnEdit	= { true, true, false, true, true };

		// Constructors
		public ChannelsConfTableModel ()
		{
		}

		public String		getColumnName (int col)				{ return columnNames[col]; }
		public Class<?>		getColumnClass (int col)			{ return columnClass[col]; }
		public int			getRowCount ()						{ if (pam.vconfig.channels == null) return 0; else return pam.vconfig.channels.size (); }
		public int			getColumnCount ()					{ return columnNames.length; }
        public boolean		isCellEditable (int row, int col)	{ return columnEdit[col]; }

		public Object getValueAt (int row, int col)
		{
			Object obj = null;
			
			switch (col)
			{
			case 0:
				obj	= Boolean.valueOf (pam.vconfig.channels.at(row).segmented);
				break;
				
			case 1:
				obj	= Boolean.valueOf (pam.vconfig.channels.at(row).blobbed);
				break;
				
			case 2: 
				obj = pam.vconfig.channels.at(row).name;
				break;
								
			case 3: 
				obj = Integer.valueOf (pam.vconfig.channels.at(row).threshold);
				break;
				
			case 4: 
				obj = Integer.valueOf (pam.vconfig.channels.at(row).gap);
				break;
								
			default:
			}
			
			return obj;
		}
		
        public void setValueAt (Object value, int row, int col)
        {
        		switch (col)
			{
        		case 0:
				pam.vconfig.channels.at(row).segmented = ((Boolean) value).booleanValue ();
        			break;
        			
        		case 1:
    				pam.vconfig.channels.at(row).blobbed = ((Boolean) value).booleanValue ();
            			break;
            			
       		case 2: 
				pam.vconfig.channels.at(row).name = (String) value;
				break;

       		case 3: 
				pam.vconfig.channels.at(row).threshold = ((Integer) value).intValue ();
				break;

       		case 4: 
				pam.vconfig.channels.at(row).gap = ((Integer) value).intValue ();
				break;
			}
        		
            fireTableCellUpdated (row, col);
        }		
	}

	/**
	 * This method picks good column sizes.
	 * If all column heads are wider than the column's cells'
	 * contents, then you can just use column.sizeWidthToFit().
	 *  @param table
	 */
	protected int initColumnSizes (JTable table)
	{
		int			width;
		int			total;
		
		ChannelsConfTableModel model = (ChannelsConfTableModel)table.getModel();
		TableColumn column = null;
		Component comp = null;
		int headerWidth = 0;
		int cellWidth = 0;
		TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
		
		total = 0;
		for (int i = 0; i < model.getColumnCount (); i++)
		{
			column = table.getColumnModel().getColumn(i);
			
			comp = headerRenderer.getTableCellRendererComponent( null, column.getHeaderValue(), false, false, 0, 0);
			headerWidth = comp.getPreferredSize().width;
			
			comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(table, model.getValueAt(0, i), false, false, 0, i);
			cellWidth = comp.getPreferredSize().width;
			
			width = Math.max (headerWidth, cellWidth);
			column.setPreferredWidth (width);
			total += width;
		}
		
		return total;
	}
	
	protected int initRowSizes (JTable table)
	{
		int			height;
		
		ChannelsConfTableModel model = (ChannelsConfTableModel)table.getModel();
		Component comp;
		
		height = 0;
		for (int i = 0; i < model.getColumnCount (); i++)
		{
			comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(table, model.getValueAt(0, i), false, false, 0, i);			
			height = Math.max (height, comp.getPreferredSize().height);
		}
		
		return height * model.getRowCount ();
	}
}
