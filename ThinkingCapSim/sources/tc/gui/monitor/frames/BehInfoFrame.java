/*
 * @(#)BehInfoFrame.java		1.0 2004/01/24
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.gui.monitor.frames;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.*;
import java.text.DecimalFormat;

import tc.shared.linda.*;
import tc.modules.*;
import tclib.behaviours.fhb.*;
import tclib.utils.fuzzy.*;

import wucore.gui.*;
import wucore.widgets.*;


/**
 * This class implements a window that displays the behaviour information.
 * 
 * @version	1.0		24 Jan 2004
 * @author Denis Remondini
 */
public class BehInfoFrame extends MonitorFrame
{ 

	/* The width of each histogram bar */
	private final int HISTOGRAM_BAR_WIDTH = 20;
	/* The row height for the table in the bottom side of the window */
	private final int BOTTOM_TABLE_ROW_HEIGHT = 45;
	/* The row height for the main table */
	private final int MAIN_TABLE_ROW_HEIGHT = 30;
	/* The widths of the table's columns */
	private int tableRowWidth[];
	/* The minimum width of the panel containing the behaviour information */
	private final int BEH_PANEL_MIN_WIDTH = 350; 
	/* The default location for the divider component that divides the tree from the behaviour panel */
	private final int DIVIDER_LOCATION = 150;
	
	/* Graphical components */
	private JPanel upPanel,bottomPanel,behPanel,hierarchyPanel;
	private JScrollPane centerScrollPanel, bottomScrollPanel;
	private JScrollPane hierarchyPane;
	private JTree behTree;
	private JTable table;					// the main table
	private JTable table2;					// the table diplayed in the bottom side
	private JLabel behNameLabel;
	private JButton btnReload;
	private JButton btnUp;
	private JToggleButton btnFuzzyWindow;
	private GaugeBar crispValuesGui[];
	private JLabel minCrispValues[];
	private JLabel maxCrispValues[];
	private JLabel realCrispValues[];
	private DecimalFormat fmt;
	
	/* Stores the path to reach the active node in the tree */
	private TreePath pathToActiveNode;
	/* Stores the root node of the tree */
	private DefaultMutableTreeNode behRootNode;
	/* Stores the active node of the tree */
	private DefaultMutableTreeNode activeBehNode;
	/* Stores the data displayed by the tree */
	private DefaultTreeModel behTreeModel;
	
	/* The main window that contains this internal frame */
	private ChildWindowListener container;
	/* The window that will be displayed to show the fuzzy predicates (and their values) used by the behaviour */
	private FuzzyPredicatesFrame fuzzyPredicatesWindow;
	/* Stores the location of the fuzzy predicates window */
	private Point fuzzyPredWindowLoc;
	/* A map containing the fuzzy predicates, used by the behaviour, and their values */
	private DoubleMap antecedentFuzzyPredicates;
	/* Stores the data displayed by the main table */
	private MyTableModel tableData;
	/* Stores the data display by the table in the bottom side */
	private MyTableModel tableData2;
	private Monitor monitor;
	/* This variable is used to say when the tree has to be updated */
	private boolean treeUpdate;
	/* This variable is used to say when the reload command has been sent */
	private boolean reloadCommandSent;
	/* Stores the information about a behaviour */
	private BehaviourInfo behInformation;
	/* The window title */
	private final String originalTitle = "Behaviour Information";
	
	/**
	 * Creates and displays a new window arranged to show behaviour information
	 * @param identifier the ID of the robot that uses the behaviour
	 * @param monitor the monitor object 
	 * @param container the window that will contain the behaviour information window
	 * @param xPos x coordinate of the window location
	 * @param yPos y coordinate of the window location
	 */
	public BehInfoFrame (String identifier, Monitor monitor, ChildWindowListener container,int xPos, int yPos) {
		this.container = container;
		this.identifier = identifier;
		this.monitor = monitor;
		pathToActiveNode = null;
		antecedentFuzzyPredicates = null;
		fuzzyPredicatesWindow = null;
		fuzzyPredWindowLoc = null;
		treeUpdate = true;
		reloadCommandSent = false;
		tableRowWidth = new int[ControlVariables.NVARIABLES+2];
		crispValuesGui = new GaugeBar[ControlVariables.NVARIABLES];
		minCrispValues = new JLabel[ControlVariables.NVARIABLES];
		maxCrispValues = new JLabel[ControlVariables.NVARIABLES];
		realCrispValues = new JLabel[ControlVariables.NVARIABLES];
		/* Graphical components initialization */
		initComponents();
		/* The window can be closed, resized and iconified by the user */
		setResizable(true);
		setVisible(true);
		pack();
		setLocation(xPos,yPos);
		/* Send the command to say that the window is waiting to receive the behaviour information */
		sendDebugCommand(ItemBehDebug.START);
	}
	
	/* Graphical components initialization */
	private void initComponents() {
		
		this.setTitle(originalTitle);
		/* Creates the root node of the tree */
		behRootNode = new DefaultMutableTreeNode("Main Behaviour");
		/* Creates a new model that contains the data visualized by the tree */
		behTreeModel = new DefaultTreeModel(behRootNode);
		/* Creates a tree */
		behTree = new JTree(behTreeModel);
		/* It's possible to select only one node at time */
		behTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		/* Sets the double-click event handler */
		behTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				/* If the reload command has been sent the double-click has no effect */
				if (!reloadCommandSent) {
					/* gets the path to reach the double-clicked node */
					int selRow = behTree.getRowForLocation(e.getX(), e.getY());
					TreePath selPath = behTree.getPathForLocation(e.getX(), e.getY());
					if (selRow != -1) {
						if (e.getClickCount() == 2) {
							pathToActiveNode = selPath;
							/* ask to the controller to receive information about the behaviour
							 * associated with the double-clicked node
							 */
							sendCurrentBehPath();
						}
					}
				}
			}
		});
		
		/* Creates the pane containing the tree */
		hierarchyPane = new JScrollPane(behTree);

		behNameLabel = new JLabel("BEHAVIOUR: MainBehaviour");
		behNameLabel.setHorizontalAlignment(JLabel.CENTER);
		btnReload = new JButton("Reload Behaviour");
		btnReload.setEnabled(false);
		btnReload.addActionListener(new ActionListener()
				{
			public void actionPerformed(ActionEvent e) {
				sendReloadCommand();
			}
		});
		
		btnFuzzyWindow = new JToggleButton("View Fuzzy Predicates");
		btnFuzzyWindow.setEnabled(false);
		btnFuzzyWindow.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				showFuzzyPredicatesWindow();	
			}
		});
		
		btnUp = new JButton("UP");
		btnUp.setEnabled(false);
		btnUp.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				if (reloadCommandSent)
					return;
				if (pathToActiveNode != null)  {
					if (pathToActiveNode.getPathCount() > 1) 
						pathToActiveNode = pathToActiveNode.getParentPath();
					sendCurrentBehPath();
				}				
			}
		});
		
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1,3));
		btnPanel.add(btnUp);
		btnPanel.add(btnReload);
		btnPanel.add(btnFuzzyWindow);
		
		upPanel = new JPanel();
		upPanel.setLayout(new GridLayout(2,1));
		upPanel.add(behNameLabel);
		upPanel.add(btnPanel);
		
		/* Creates the container that stores the data displayed by the main table */
		tableData = new MyTableModel();
		/* Sets the column's names of the main table */
		String[] names = new String[3+ControlVariables.NVARIABLES];
		names[0] = "Rule name";
		names[1] = "Antecedent value";
		for (int i = 0; i < ControlVariables.NVARIABLES; i++)
			names[i+2] = ControlVariables.getVariableName(i);
		names[ControlVariables.NVARIABLES+2] = "SubBehaviour";
		tableData.setColsNames(names);
		/* Sets the widths of the table columns */
		tableRowWidth[0] = 60;
		tableRowWidth[1] = 40;
		for (int i = 0; i < ControlVariables.NVARIABLES; i++)
			tableRowWidth[2+i] = 100;
		/* Creates the main table */
		table = new JTable(tableData);
		table.setForeground(this.getForeground());
		table.setSelectionBackground (new javax.swing.plaf.ColorUIResource(204,204,255));
		/* The last column contains information that I don't want to display */
		table.removeColumn(table.getColumnModel().getColumn(ControlVariables.NVARIABLES+2));
		
		table.setRowHeight(MAIN_TABLE_ROW_HEIGHT);
		/* Specifies how to render some particolar objects during the table rendering process */
		table.setDefaultRenderer(BarPanel.class,new BarPanel(0));
		table.setDefaultRenderer(HistogramPanel.class,new HistogramPanel(20,table.getRowHeight()));
		table.setShowVerticalLines(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(tableRowWidth[0]);
		table.getColumnModel().getColumn(1).setPreferredWidth(tableRowWidth[1]);
		for (int i = 0; i < ControlVariables.NVARIABLES; i++)
			table.getColumnModel().getColumn(2+i).setPreferredWidth(tableRowWidth[2+i]);
		/* It's possible to select only one row at time */
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionAllowed (true);
		table.setColumnSelectionAllowed (false);
		
		/* Sets the double-click event handler */
		table.addMouseListener(new MouseAdapter() 
				{
			public void mouseClicked(MouseEvent e) 
			{
				if (e.getClickCount() == 2) {
					/* If the reload command has been sent the double-click has no effect */
					if (!reloadCommandSent)
						manageTableDoubleClick();
				}				
			}
		}); 
				
		/* Creates the pane containing the main table */
		centerScrollPanel = new JScrollPane(table);
		
		/* Creates the container that stores the data displayed by the bottom side table */
		tableData2 = new MyTableModel();
		/* Sets the column's names of the bottom side table */
		String[] names2 = new String[2+ControlVariables.NVARIABLES];
		names2[0] = "Rule name";
		names2[1] = "Antecedent value";
		for (int i = 0; i < ControlVariables.NVARIABLES; i++)
			names2[i+2] = ControlVariables.getVariableName(i);
		tableData2.setColsNames(names2);
		/* Creates the bottom side table */
		table2 = new JTable(tableData2);
		table2.setForeground(this.getForeground());
		table2.setShowVerticalLines(false);
		table2.setRowHeight(BOTTOM_TABLE_ROW_HEIGHT);
		/* Specifies how to render some particolar objects during the table rendering process */
		table2.setDefaultRenderer(BarPanel.class,new BarPanel(0));
		table2.setDefaultRenderer(HistogramPanel.class,new HistogramPanel(20,table2.getRowHeight()));
		table2.getColumnModel().getColumn(0).setPreferredWidth(tableRowWidth[0]);
		table2.getColumnModel().getColumn(1).setPreferredWidth(tableRowWidth[1]);
		for (int i = 0; i < ControlVariables.NVARIABLES; i++)
			table2.getColumnModel().getColumn(2+i).setPreferredWidth(tableRowWidth[2+i]);
		/* It's possible to select only one row at time */
		table2.setCellSelectionEnabled (false);
		table2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		/* Creates the pane containing the bottom side table */
		bottomScrollPanel = new JScrollPane(table2);
		bottomScrollPanel.setPreferredSize(new Dimension(table2.getWidth(),table2.getRowHeight()+20));
		JPanel commandValuesPanel = new JPanel();
		commandValuesPanel.setLayout(new GridLayout(1,ControlVariables.NVARIABLES));
		commandValuesPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Control values sent to the robot"),
                        BorderFactory.createEmptyBorder(5,5,5,5)));


		fmt = new DecimalFormat();
		fmt.setMaximumFractionDigits(2);
		fmt.setMinimumFractionDigits(2);
		for (int i = 0; i < ControlVariables.NVARIABLES; i++) {
			
			crispValuesGui[i] = new GaugeBar();
			
			JPanel commandValuePanel = new JPanel();
			commandValuePanel.setLayout(new BoxLayout(commandValuePanel,BoxLayout.Y_AXIS));
			JLabel commandLabel = new JLabel(ControlVariables.getVariableName(i));
			commandLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
			JPanel commandLabelPanel = new JPanel();
			commandLabelPanel.setLayout(new BoxLayout(commandLabelPanel,BoxLayout.X_AXIS));
			commandLabelPanel.add(Box.createHorizontalGlue());
			commandLabelPanel.add(commandLabel);
			commandLabelPanel.add(Box.createHorizontalGlue());
			minCrispValues[i] = new JLabel("min");
			maxCrispValues[i] = new JLabel("max");
			realCrispValues[i] = new JLabel("realValue");
			realCrispValues[i].setForeground(Color.BLUE);
			JPanel crispPanel = new JPanel();
			crispPanel.setLayout(new BoxLayout(crispPanel,BoxLayout.X_AXIS));
			crispPanel.add(minCrispValues[i]);
			crispPanel.add(Box.createHorizontalGlue());
			crispPanel.add(realCrispValues[i]);
			crispPanel.add(Box.createHorizontalGlue());
			crispPanel.add(maxCrispValues[i]);
			commandValuePanel.add(commandLabelPanel);
			commandValuePanel.add(crispValuesGui[i]);
			commandValuePanel.add(crispPanel);
			commandValuePanel.setBorder(BorderFactory.createEtchedBorder());
			commandValuesPanel.add(commandValuePanel);
		}
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel,BoxLayout.Y_AXIS));
		bottomPanel.add(bottomScrollPanel);
		bottomPanel.add(commandValuesPanel);
		/* Creates the panel containing the information about a behaviour */
		behPanel = new JPanel();
		behPanel.setLayout(new BorderLayout());	
		behPanel.add(upPanel,BorderLayout.NORTH);
		behPanel.add(bottomPanel,BorderLayout.SOUTH);
		behPanel.add(centerScrollPanel,BorderLayout.CENTER);
		behPanel.setMinimumSize(new Dimension(BEH_PANEL_MIN_WIDTH,getHeight()));
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,hierarchyPane, behPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(DIVIDER_LOCATION);
		
		getContentPane().add(splitPane);
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				close ();
			}
		});
	}
	
	/* Sends a debug command.
	 * This method is used to tell to the controller if it has to start or to stop 
	 * sending the behaviour information.
	 */
	private void sendDebugCommand(int command) {
		monitor.setID(identifier);
		monitor.setBehaviourDebug(command);
	}
	
	/*
	 * Sends a reload command.
	 * This method is used to tell to the controller that a particular behaviour has to
	 * be reloaded.
	 */
	private void sendReloadCommand() {
		monitor.setID(identifier);			
		monitor.setBehaviourName(behInformation.getName());
//		System.out.println("DEBUG: beh reload requested -> "+behInformation.getName());
		reloadCommandSent = true;
		treeUpdate = true;
	}
	
	/*
	 * Shows the fuzzy predicates window
	 */
	private void showFuzzyPredicatesWindow() {
		if (!btnFuzzyWindow.isSelected()) {
			/* the user want to close the fuzzy predicates window */
			if (fuzzyPredicatesWindow != null)
					fuzzyPredicatesWindow.close ();
		} else {
			/* the user want to open the fuzzy predicates window */
			if (fuzzyPredicatesWindow == null) {
				fuzzyPredicatesWindow = new FuzzyPredicatesFrame(container,this);
				fuzzyPredicatesWindow.setSize(new java.awt.Dimension(200, 200));
				if (fuzzyPredWindowLoc == null)
					fuzzyPredicatesWindow.setLocation(this.getLocation().x+getWidth(),this.getLocation().y);
				else
					fuzzyPredicatesWindow.setLocation(fuzzyPredWindowLoc);
				if (antecedentFuzzyPredicates != null)
					fuzzyPredicatesWindow.updateData(behInformation.getName(),antecedentFuzzyPredicates);
			}
			fuzzyPredicatesWindow.toFront();
		}
	}
	
	/**
	 * This method has to be called when the fuzzy predicates window is closing
	 */
	public void removeFuzzyPredicatesWindow() {
		if (fuzzyPredicatesWindow != null)
			fuzzyPredWindowLoc = fuzzyPredicatesWindow.getLocation();
		fuzzyPredicatesWindow = null;
		btnFuzzyWindow.setSelected(false);
	}
	
	/*
	 * This method add to a node the information about a behaviour 
	 */
	private void setBehTree(BehaviourInfo behInfo, DefaultMutableTreeNode behNode) {
		String nodeLabel;
		ArrayList ruleParameters;
		DefaultMutableTreeNode node = null;				// the new child node
		int ruleNumber = behInfo.getRulesNumber();
		String params="";
		
		/* for each complex rule (it means a rule with a sub-behaviour) it is created a child node */
		for (int i = 0; i < ruleNumber; i++)  {
			String subBehName = behInfo.getRuleSubBehaviour(i);
			params="";
			if (subBehName != null) {
				/* Builds a string containing the list of the parameters used by the sub-behaviour */
				ruleParameters = behInfo.getRuleParameters(i);
				if ((ruleParameters != null) && ruleParameters.size() > 0) {
					params =  (String) ruleParameters.get(0);
					for (int j = 1; j < ruleParameters.size(); j++)
						params += ", " + (String) ruleParameters.get(j);
				}
				nodeLabel = behInfo.getRuleName(i) + "("+params+")";
				/* Creates and adds the new child node */
				node = new DefaultMutableTreeNode(nodeLabel);
				behNode.add(node);
			}		
		}
	}
	
	/*
	 * Returns the tree node that is current selected.
	 */
	private DefaultMutableTreeNode getActiveBehNode(DefaultMutableTreeNode node, TreePath path) {
		int i = 1, j = 0;
		boolean activeNodeFounded = false;
		DefaultMutableTreeNode currentNode;
		
		currentNode = node;
		if (path.getPathCount() != 1)
			/*
			 * Go through the tree following the path indicated
			 */
			while ((activeNodeFounded != true) && (j < currentNode.getChildCount())) {
//				System.out.println("DEBUG: current node: "+currentNode.toString());
//				System.out.println("DEBUG: current path node: "+path.getPathComponent(i));
				String tmp1 = (String)((DefaultMutableTreeNode)path.getPathComponent(i)).getUserObject();
				String tmp2 = (String)((DefaultMutableTreeNode)currentNode.getChildAt(j)).getUserObject();
//				System.out.println("DEBUG: 1->"+tmp1+" 2->"+tmp2);
				if (tmp2.equals(tmp1)) {
					currentNode = (DefaultMutableTreeNode)currentNode.getChildAt(j);
					j = 0;
					if (path.getPathCount() == i+1)
						activeNodeFounded = true;
					else i++;
				}
				else j++;
			}
//		System.out.println("DEBUG: active Beh = "+(String) currentNode.getUserObject());
		return currentNode;
	}
	
	/*
	 * Shows the active node in the tree
	 */
	private void showActiveNode(DefaultMutableTreeNode node, TreePath path) {
		DefaultMutableTreeNode currentNode;
		TreePath newPath;
		
		currentNode = getActiveBehNode(node,path);
		if (currentNode.isLeaf()) {
			newPath = new TreePath(currentNode.getPath());
			behTree.addSelectionPath(newPath);
		} else {
			newPath = new TreePath(((DefaultMutableTreeNode) currentNode.getFirstChild()).getPath());
			behTree.addSelectionPath(newPath.getParentPath());
		}
		behTree.scrollPathToVisible(newPath);
	}
	
	/*
	 * Creates a new histogram panel
	 */
	private HistogramPanel createHistogramPanel(Histogram source, int barWidth, int barHeight) {
		double[] yValues = new double[source.getBarsNumber()];
		double[] xValues = new double[source.getBarsNumber()];
		
		for (int j=0; j < source.getBarsNumber();j++) {
			xValues[j] = source.getXValue(j);
			yValues[j] = source.getYValue(j);
		}	
		
		return new HistogramPanel(xValues,yValues,barWidth,barHeight);
	}
	
	/**
	 * Updates the behaviour information displayed in the window
	 * @param behItem the item containing the new behaviour information
	 */
	public void updateData(ItemBehInfo behItem) {
		if (behItem != null) {
			int row,pos;
			HistogramPanel histogramPanel;
			Histogram hi;
			String params = "";
			ArrayList ruleParameters;
			
			this.behInformation = behItem.get();
			tableData.setRowsNumber(behInformation.getRulesNumber());
			
			/* Checks if the tree has to be updated */
			if (treeUpdate) {	
				/* Checks if there is an active node */
				if (pathToActiveNode == null) {
					/* if there is no active node it will create a new tree with only the root node */
					behRootNode.removeAllChildren();
					behRootNode.setUserObject(new String(behInformation.getName()));
					pathToActiveNode = new TreePath(behRootNode);
				}
				/* Finds the active node and update his children */
				activeBehNode = getActiveBehNode(behRootNode,pathToActiveNode);
				activeBehNode.removeAllChildren();
				setBehTree(behInformation,activeBehNode);
				behTreeModel.reload();
				/* Makes the active node visible */
				showActiveNode(behRootNode,pathToActiveNode);
				hierarchyPane.repaint();
				repaint();
				treeUpdate = false;
				reloadCommandSent = false;
			}
			/* Builds a string with the active behaviour parameters */	
			params = (String) activeBehNode.getUserObject();
			pos = params.indexOf('(');
			if (pos != -1)
				params = params.substring(pos);
			else
				params = "()";
			behNameLabel.setText("BEHAVIOUR: "+behInformation.getName()+params);
			
			/* be sure that the Reload and Fuzzy Predicates buttons are enabled */
			btnReload.setEnabled(true);
			btnFuzzyWindow.setEnabled(true);
			
			/* if the current behaviour is not the main one, the UP button has to be enabled */
			if (pathToActiveNode.getPathCount() > 1)
				btnUp.setEnabled(true);
			else
				btnUp.setEnabled(false);
			
			/*
			 * Shows information about each rule of the current behaviour
			 */
			for (row = 0; row < behInformation.getRulesNumber(); row++) {
				
				/* gets the parameters used by the rule sub-behaviour */
				ruleParameters = behInformation.getRuleParameters(row);
				params = "";
				/* if there are parameters it creates a string with the list of them */
				if ((ruleParameters != null) && (ruleParameters.size() > 0)) {
					params =  (String) ruleParameters.get(0);
					for (int j = 1; j < ruleParameters.size(); j++)
						params += ", " + (String) ruleParameters.get(j);
				}
				/* updates the first column of the current rule showed in the main table */
				table.getModel().setValueAt(behInformation.getRuleName(row) + "("+params+")",row,0);
				/* updates the second column of the current rule showed in the main table */
				table.getModel().setValueAt(new BarPanel(behInformation.getRuleAntecedentValue(row)),row,1);
				for (int i=0; i < ControlVariables.NVARIABLES; i++) {
					/* creates an histogram to display one of the output fuzzy sets of the current rule */
					hi = ((ControlVariables)behInformation.getRuleOutputFSets(row)).getOutputFSet(i);
					histogramPanel = createHistogramPanel(hi,HISTOGRAM_BAR_WIDTH,table.getRowHeight());
					table.getModel().setValueAt(histogramPanel,row,i+2);
				}
				/* updates the sub-behaviour name of the current rule */
				table.getModel().setValueAt(behInformation.getRuleSubBehaviour(row),row,2+ControlVariables.NVARIABLES);

			}
			/* repaint the main table contents */
			table.revalidate();
			table.repaint();
			
			/* Sets the information to display about the behaviour */
			tableData2.setRowsNumber(1);
			table2.getModel().setValueAt(new String("Overall"),0,0);
			table2.getModel().setValueAt(new BarPanel(behInformation.getMaxAntecedentValue()),0,1);
			for (int i=0; i < ControlVariables.NVARIABLES; i++) {
				/* creates an histogram to display one of the output fuzzy sets of the current rule */
				hi = ((ControlVariables)behInformation.getOutputFSets()).getOutputFSet(i);
				histogramPanel = createHistogramPanel(hi,HISTOGRAM_BAR_WIDTH,table2.getRowHeight());
				histogramPanel.setDrawXValues(true);
				table2.getModel().setValueAt(histogramPanel,0,i+2);
			}
			
			/* repaint the bottom side table contents */
			table2.revalidate();
			table2.repaint();
			
			double[] commandValues = behInformation.getValuesSentToRobot();
			int scale = 1;
			for (int i = 0; i < ControlVariables.NVARIABLES; i++) {
				hi = ((ControlVariables)behInformation.getOutputFSets()).getOutputFSet(i);
				if (hi.getMaximum()-hi.getMinimum() < 5)
					scale = 1000;
				crispValuesGui[i].setMinimum((int)(scale*hi.getMinimum()));
				crispValuesGui[i].setMaximum((int)(scale*hi.getMaximum()));
				if (i == 0) {
					crispValuesGui[i].setInverted(true);
					maxCrispValues[i].setText(fmt.format(hi.getMinimum()));
					minCrispValues[i].setText(fmt.format(hi.getMaximum()));
				}
				else {
					minCrispValues[i].setText(fmt.format(hi.getMinimum()));
					maxCrispValues[i].setText(fmt.format(hi.getMaximum()));
				}
				crispValuesGui[i].setValue((int)(commandValues[i]*scale));
				realCrispValues[i].setText(fmt.format(commandValues[i]));
			}
			/* if the fuzzy predicates window is displayed it will be updated */
			antecedentFuzzyPredicates = behInformation.getAntecedentPredicates();
			if (fuzzyPredicatesWindow != null) 
				fuzzyPredicatesWindow.updateData(behInformation.getName(),antecedentFuzzyPredicates);
		}
	}
		
	/* Table double-click event handler */
	private void manageTableDoubleClick() {
		/* Checks if the rule has a sub-behaviour */
		if (tableData.getValueAt(table.getSelectedRow(),ControlVariables.NVARIABLES+2) != null) {
			/* Updates the path to reach the rule sub-behaviour */
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode((String)tableData.getValueAt(table.getSelectedRow(),0));
			pathToActiveNode = pathToActiveNode.pathByAddingChild(newNode);
			/* ask to the controller to send information about a new behaviour */
			sendCurrentBehPath();
		}
	}

	/* Sends to the controller the list of the rules that bring you to the desidered behaviour */
	private void sendCurrentBehPath() {
		ArrayList rulesNames = new ArrayList();
		String ruleName;
		int pos;
		
		if (pathToActiveNode != null) {		
			/* Builds the rule list*/
			for (int i = 1; i < pathToActiveNode.getPathCount(); i++) {
				ruleName = (String)((DefaultMutableTreeNode)pathToActiveNode.getPathComponent(i)).getUserObject();
				/* Adds the parameters used by the rule sub-behaviour */
				pos = ruleName.indexOf('(');
				if (pos != -1)
					ruleName = ruleName.substring(0,pos);
				rulesNames.add(ruleName);
			}
		}
		/* Sends the list to the controller */
		monitor.setID(identifier);			
		monitor.setBehaviourRuleNames(rulesNames);
		/* The tree has to be updated to show the new current behaviour */
		treeUpdate = true;
	}
	
	/**
	 * This method is called when the window is closing and it notifies the closing operation to the main
	 * window that contains it. Moreover it send a STOP command to the controller in order to stop the debug
	 * information transmission. If the fuzzy predicates window is open it will be closed.
	 *
	 */
	public void close()
	{   
		if (container != null) {
			container.childClosed (this);
		}
		setVisible(false);
		/* Sends to the controller the STOP command so it won't send debug information anymore */
		sendDebugCommand(ItemBehDebug.STOP);
		pathToActiveNode = null;
		/* Closes the fuzzy predicates window if it was open */
		if (fuzzyPredicatesWindow != null) {
				fuzzyPredicatesWindow.close ();	
				fuzzyPredicatesWindow = null;
		}
	}
}


/**
 * Simple and general model to store the information displayed by a table
 * 
 * @version	1.0		24 Jan 2004
 * @author Denis Remondini
 */
class MyTableModel extends AbstractTableModel{
	
	/* Contains the table data: each element will be a vector */
	private Vector data;
	/* Contains the names of the table columns */
	private Vector columnNames;
	
	/**
	 * Creates an empty model.
	 */
	public MyTableModel()
	{
		data = new Vector();
		columnNames = new Vector();
	}
	
	/**
	 * Sets the names of the table columns
	 * 
	 * @param names the names of the table columns
	 */
	public void setColsNames(String[] names) {
		int i = 0;
		while(i < Array.getLength(names))
		{
			columnNames.add(names[i]);
			++i;
		}	
	}
	
	/**
	 * Sets the number of table rows.
	 * @param num the number of table rows.
	 */
	public void setRowsNumber(int num) {
		data.setSize(num);
	}
	
	/**
	 * Sets the number of table columns.
	 * @param num the number of table columns
	 */
	public void setColsNumber(int num) {
		columnNames.setSize(num);
	}
	
	/**
	 * Returns the number of table rows.
	 * @return the number of table rows.
	 */
	public int getRowCount()
	{
		return data.size();
	}
	
	/**
	 * Returns the number of table columns.
	 * @return the number of table columns.
	 */
	public int getColumnCount()
	{
		return columnNames.size();
	}
	
	/**
	 * Returns the name of a table column.
	 * @return the name of a table column.
	 */
	public String getColumnName(int col) {
		return (String)columnNames.get(col);
	}
	
	/**
	 * Returns the object stores in a particular position in the table.
	 * @param row the row number where the object is stored
	 * @param column the column number where the object is stored
	 * @return the object stores in the specified table position
	 */
	public Object getValueAt(int row, int column)
	{
		if (data == null) return null;
		Vector c = (Vector)data.elementAt(row);
		Object q = (Object)c.elementAt(column);
		return q;
	}
	
	/**
	 * Stores an object in the specified table position
	 * @param obj the object to store
	 * @param row the number of the column where has to be stored the object
	 * @param column the number of the column where has to be stored the object
	 */
	public void setValueAt(Object obj, int row, int column) {
		/* if the row doesn't exist it will be created */
		if (data.get(row) == null) {
			Vector r = new Vector();
			r.setSize(columnNames.size());
			data.setElementAt(r,row);
		}
		((Vector) data.get(row)).setElementAt(obj,column);
	}
	
	/**
	 * Return the class of the objects stored in a particular table column.
	 * @param column the column number
	 */
	public Class getColumnClass(int column) {
		return getValueAt(0,column).getClass();
	}
}

/**
 * This class implements a simple table cell renderer to display an horizontal bar.
 * 
 * @version	1.0		24 Jan 2004
 * @author Denis Remondini
 */
class BarPanel extends JPanel implements TableCellRenderer {
	
	/* The max value that can be displayed by the bar */
	private static final int MAXVALUE = 100;
	private JProgressBar bar;
	private JPanel panel;

	/**
	 * Creates a new panel containing a bar that displays the desidered value
	 * @param value the desidered value that will be displayed by the bar. This number has to be in the [0,1] interval
	 */
	public BarPanel(double value) {
	
		bar = new JProgressBar(0,MAXVALUE);
		bar.setValue((int) Math.round(value*MAXVALUE));
		
		/* Displays the bar in the middle of the container. Quite tricky.
		 * TODO: check if it's possibile to write it in a clear way.
		 */
		Box b = Box.createHorizontalBox();
		b.add(new JLabel(""));
		panel = new JPanel();
		panel.setLayout(new GridLayout(3,1));
		panel.add(b);	
		panel.add(bar);
		setLayout(new BorderLayout());
		add(panel,BorderLayout.CENTER);
	}
	
	/**
	 * Sets the value that will be displayed by the bar
	 * @param value value that will be displayed by the bar. This number has to be in the [0,1] interval
	 */
	public void setValue(double value) {
		bar.setValue((int)Math.round(value*MAXVALUE));
	}
	
	/**
	 * This method will be called to render the table cell containing this class.
	 */
	public Component getTableCellRendererComponent(
			JTable table, Object obj, boolean isSelected, boolean hasFoucs,
			int row, int colums) {
		
		this.bar.setValue(((BarPanel)obj).bar.getValue());

		if (isSelected)
			this.panel.setBackground(new javax.swing.plaf.ColorUIResource(204,204,255));
		else
			this.panel.setBackground(table.getBackground()); 

		return this;
	}
	
}

/**
 * This class implements a simple table cell renderer to display an histogram.
 * 
 * @version	1.0		24 Jan 2004
 * @author Denis Remondini
 */
class HistogramPanel extends JPanel implements TableCellRenderer {
	
	/* Specifies if the x coordinates have to be visualized.*/
	private boolean drawXValues;
	/* The values that identify the histogram (x and y coordinates) */
	private double[] xValues, yValues;
	/* The histogram graphical component */
	private Histogram2D hi;
	/* The width of the histogram bars */
	private int barWidth;
	/* The height the histogram bars */
	private int barHeight;
	
	/**
	 * Creates a new panel containing an empty histogram. 
	 * @param barWidth the width of the histogram bars
	 * @param barHeight the height of the histogram bars
	 */
	public HistogramPanel(int barWidth, int barHeight) {
		setDrawXValues(false);
		this.barWidth = barWidth;
		this.barHeight = barHeight;
	}
		
	/**
	 * Creates a new panel containing an inizialized histogram.
	 * @param xValues the x coordinates
	 * @param yValues the y coordinates
	 * @param barWidth the width of the histogram bars
	 * @param barHeight the height of the histogram bars
	 */
	public HistogramPanel(double[] xValues, double[] yValues, int barWidth, int barHeight) {

		this.barWidth = barWidth;
		this.barHeight = barHeight;
		initHistogram(xValues,yValues);
		setDrawXValues(false);
	}	
	
	/**
	 * Initializes the histogram values
	 * @param xValues the x coordinates
	 * @param yValues the y coordinates
	 */
	public void initHistogram(double[] xValues, double[] yValues) {
		this.yValues = yValues;
		this.xValues = xValues;
		removeAll();
		hi = new Histogram2D(Array.getLength(yValues),barWidth,barHeight);
		hi.setYValues(yValues);
		hi.setXValues(xValues);
		add(hi);
	}
	
	/**
	 * Specifies if the x coordinates have to be displayed
	 * @param draw true if the x coordinates have to be displayed, false otherwise.
	 */
	public void setDrawXValues(boolean draw) {
		drawXValues = draw;
		if (hi != null)
			hi.setDrawXValues(draw);
	}
	
	/**
	 * Returns information about the x coordinates visualization
	 * @return true if the x coordinates are displayed, false otherwise
	 */
	public boolean getDrawXValues() {
		return drawXValues;
	}
	
	/**
	 * Returns the x coordinates of the histogram
	 * @return the x coordinates of the histogram
	 */
	public double[] getXValues() {
		return xValues;
	}
	
	/**
	 * Returns the y coordinates of the histogram
	 * @return the y coordinates of the histogram
	 */
	public double[] getYValues() {
		return yValues;
	}
	
	/**
	 * This method will be called to render the table cell containing this class.
	 */
	public Component getTableCellRendererComponent(
			JTable table, Object obj, boolean isSelected, boolean hasFocus,
			int row, int colums) {
		
		this.initHistogram(((HistogramPanel)obj).getXValues(),((HistogramPanel)obj).getYValues());
		this.setDrawXValues(((HistogramPanel)obj).getDrawXValues());
		if (isSelected) {
			this.setBackground(new javax.swing.plaf.ColorUIResource(204,204,255));
			this.hi.setBackground(new javax.swing.plaf.ColorUIResource(204,204,255));
		}
		else {
			this.setBackground(table.getBackground());
			this.hi.setBackground(table.getBackground());
		}
		return this;
	}
}