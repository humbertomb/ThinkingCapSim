/*
 * (c) 2003 Bernardo Canovas
 * (c) 2004 Humberto Martinez
 */
  
package tc.gui.monitor.actions;

import javax.swing.*;
import java.util.*;

public class RemoteActionCreator
{
	// static methods

	/** Creates a JMenuItem that represents the action 
	 *
	 *  - For a RMIAction with no arguments, the item created will show as:
	 *
	 *  |  action  |
     *
	 *  - For a RMIBooleanAction, the item created will be show as:
	 *		if the action is not checked:
	 *	|	[] action name	|
	 *  	if the action is checked:
	 *	|	[*] action name	|
	 *
	 *  - For a RMIMultivalueAction, the item created will be show as:
	 *		Action name --> O Value name1
	 *					    O Value name2
	 *						......
	 *						* Value name i
	 *					    ......
	 * 					    O Value name'n'
	 * 
	 *		Where the 'i' value is the selected value
     *
	 * - For a RMIFreeAction, the item created will be show as:
	 *
	 *	|	Action name...	|
	 *
	 * @param action: RMIAction that the item will represent
	 * @param servName: Name of the service that includes that action
	 *
     */
	 
	public static JMenuItem createMenuItem(RemoteAction action,String servName)
	{
		if (action instanceof RemoteBooleanAction)
			return createRemoteBooleanActionMenuItem((RemoteBooleanAction) action,servName);		
		else if (action instanceof RemoteMultivalueAction)
			return createRemoteMultivalueActionMenuItem((RemoteMultivalueAction) action,servName);		
		else if (action instanceof RemoteFreeAction)
			return createRemoteFreeActionMenuItem((RemoteFreeAction) action,servName);		
		else 	
			return createRemoteActionMenuItem(action,servName);		
	}
	
	private static JMenuItem createRemoteActionMenuItem(RemoteAction action,String servName)
	{
		JMenuItem auxMenuItem;
		auxMenuItem=new JMenuItem();
		auxMenuItem.setVisible(true);
		auxMenuItem.setText(action.getName());
		auxMenuItem.putClientProperty("ActionName",action.getName());
		auxMenuItem.putClientProperty("ServiceName",servName);
		return auxMenuItem;
	}
	
	private static JMenuItem createRemoteBooleanActionMenuItem(RemoteBooleanAction action,String servName)
	{
		JCheckBoxMenuItem auxCBItem;
		auxCBItem=new JCheckBoxMenuItem();
		auxCBItem.setText(action.getName());
		auxCBItem.setSelected(action.isChecked());						
		auxCBItem.setVisible(true);
		auxCBItem.putClientProperty("ActionName",action.getName());
		auxCBItem.putClientProperty("ServiceName",servName);
		return auxCBItem;
	}
	
	private static JMenuItem createRemoteMultivalueActionMenuItem(RemoteMultivalueAction action,String servName)
	{
		ButtonGroup bg;
		JMenu auxSubmenu;
		JRadioButtonMenuItem auxRBItem;
		Enumeration valuesEnum;
		String valueName;
					
		auxSubmenu=new JMenu();
		auxSubmenu.setText(action.getName());
		valuesEnum=action.getValues().keys();
		bg=new ButtonGroup();
		while (valuesEnum.hasMoreElements())
		{
			valueName=(String) valuesEnum.nextElement();
			auxRBItem=new JRadioButtonMenuItem();
			auxRBItem.setVisible(true);
			auxRBItem.setText(valueName);
			if (valueName.equals(action.getItemSelected()))
				auxRBItem.setSelected(true);
			else
				auxRBItem.setSelected(false);
			auxRBItem.putClientProperty("ActionName",action.getName());
			auxRBItem.putClientProperty("ServiceName",servName);
			auxRBItem.putClientProperty("Value",action.getValues().get(valueName));
			bg.add(auxRBItem);
			auxSubmenu.add(auxRBItem);
		}
		auxSubmenu.setVisible(true);
		return auxSubmenu;
	}

	private static JMenuItem createRemoteFreeActionMenuItem(RemoteFreeAction action,String servName)
	{
		javax.swing.JMenuItem auxMenuItem;
		auxMenuItem=new javax.swing.JMenuItem();
		auxMenuItem.setText(action.getName()+"...");
		auxMenuItem.setVisible(true);
		auxMenuItem.putClientProperty("Action",action);	
		auxMenuItem.putClientProperty("ServiceName",servName);	
		return auxMenuItem;
	}

	/** Creates a Dialog with the aspect
		
		---------------------------------------
		|Action name                          |
		---------------------------------------
		|ParamDesc1: ______ ParamDesc2: ______|
		|ParamDesc3: ______ ParamDesc4: ______|
		|...                                  |
		|[Accept][Cancel]                     |
		---------------------------------------
	*/
	public static JDialog createRemoteFreeActionDialog(RemoteFreeAction action,java.awt.Frame owner,String servName)
	{
		JButton acceptBut;
		JDialog dialog=new JDialog(owner,action.getName());
		int i;
		String paramsDesc[];
		paramsDesc=action.getAllParamsDesc();
		dialog.setModal(false); 
		dialog.getContentPane().setLayout(new java.awt.GridLayout(4,(int)(Math.ceil((float)paramsDesc.length*2+(float)2)/4.0)));
		for (i=0;i<paramsDesc.length;i++)
		{
			dialog.getContentPane().add(new JLabel(paramsDesc[i]+": "));
			dialog.getContentPane().add(new JTextField());
		}
		acceptBut=new javax.swing.JButton("Accept");
		acceptBut.putClientProperty("ServiceName",servName);
		acceptBut.putClientProperty("ActionName",action.getName());
		dialog.getContentPane().add(acceptBut);
		dialog.getContentPane().add(new javax.swing.JButton("Cancel"));
		dialog.pack();
		return dialog;
	}
}