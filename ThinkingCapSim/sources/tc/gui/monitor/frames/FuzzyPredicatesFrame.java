/*
 * @(#)FuzzyPredicatesFrame.java		1.0 2004/01/28
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.gui.monitor.frames;

import wucore.gui.*;
import tclib.behaviours.fhb.DoubleMap;
import tclib.behaviours.fhb.exceptions.ElementNotFound;

import java.awt.*;
import javax.swing.*;

/**
 * This class implements a window that shows some fuzzy predicates with their
 * values. The values are represented with horizontal bars.
 * 
 * @version	1.0		28 Jan 2004
 * @author Denis Remondini
 */
public class FuzzyPredicatesFrame extends MonitorFrame
{
	
	/* The main window that contains this internal frame */
	private ChildWindowListener container;
	/* The window that has opened this one */
	private BehInfoFrame parent;
	/* Graphical components */
	private JPanel upPanel;	
	private JLabel behLabel;
	private JScrollPane centerScrollPanel;
	private JTable table;
	/* The data container for the JTable component */
	private MyTableModel tableData;
	
	/**
	 * Creates and visualizes a new fuzzy predicates window.
	 * @param container the main window that contains the fuzzy predicates window
	 * @param parent the window that want to open the fuzzy predicates window
	 */
	public FuzzyPredicatesFrame(ChildWindowListener container,BehInfoFrame parent) {
		this.container = container;
		this.parent = parent;
		getContentPane().setLayout(new BorderLayout());
		/* Graphical components initialization */
		initComponents();
		/* The window can be closed by the user */
		setResizable(true);
		setVisible(true);
		pack();
		
		/* When the window is closed the internal method close() is called */
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				close ();
			}
		});
	}
	
	/* This method initializes the graphical components */
	private void initComponents() {
		this.setTitle("Fuzzy Predicates Values");
		
		behLabel = new JLabel("BEHAVIOUR: ");
		upPanel = new JPanel();
		upPanel.add(behLabel);

		tableData = new MyTableModel();
		/* Sets the table columns names*/
		String[] names = new String[2];
		names[0] = "Fuzzy Predicate";
		names[1] = "Value";

		tableData.setColsNames(names);
		table = new JTable(tableData);
		table.setForeground(this.getForeground());
		/* Specifies how to render a particolar object during the table rendering process */
		table.setDefaultRenderer(BarPanel.class,new BarPanel(0));
		table.setRowHeight(15);
		
		table.setShowVerticalLines(false);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		/* It's possible to select only one row at time */
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		centerScrollPanel = new JScrollPane(table);
		getContentPane().add(upPanel,BorderLayout.NORTH);
		getContentPane().add(centerScrollPanel,BorderLayout.CENTER);	
	}
	
	/**
	 * This method updates the window with new data.
	 * @param behName the name of the behaviour
	 * @param values the map containing the fuzzy predicates and their values
	 */
	public void updateData(String behName, DoubleMap values) {
		/* A safety control */
		if (values == null) return;
		/* Stores the fuzzy predicates names */
		String[] names = values.getAntecedentKeys();
		/* Stores the number of fuzzy predicates */
		int num = values.getSize();
		/* Auxiliary variable to store a fuzzy predicate value */
		double value;
		
		behLabel.setText("BEHAVIOUR: "+behName);
		/* Sets the data displayed by the JTable component */
		tableData.setRowsNumber(num);
		for (int row = 0; row < num; row++) {
			tableData.setValueAt(names[row],row,0);
			try {
				value = values.getValue(names[row]);
				tableData.setValueAt(new BarPanel(value),row,1);
			} catch (ElementNotFound e) {
				System.out.println(e.toString());
			}
			
		}
		/* Displays the new contents */
		table.revalidate();
		table.repaint();
	}
	
	/**
	 * This method notifies to the parent window that this frame has been closed.
	 *
	 */
	public void close() {
		parent.removeFuzzyPredicatesWindow();
	}
	
}