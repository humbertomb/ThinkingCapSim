/*
 * (c) 2002-2003 Bernardo Canovas Segura
 * (C) 2004 Humberto Martinez Barbera
 */
 
package tc.gui.monitor.frames;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

import tc.modules.*;
import tc.fleet.*;

import tclib.utils.fusion.*;

import wucore.gui.*;
import wucore.utils.math.*;

class MonitorDataFrameRenderer extends DefaultTreeCellRenderer
{
	DefaultMutableTreeNode virtusNode;
	DefaultMutableTreeNode groupsNode;
	
	MonitorData data=null;

	private javax.swing.JLabel finalValue;
	
	public MonitorDataFrameRenderer(
		DefaultMutableTreeNode virtusNode,
		DefaultMutableTreeNode groupsNode)
	{
		finalValue=new javax.swing.JLabel();
		this.virtusNode=virtusNode;
		this.groupsNode=groupsNode;
	}
	
	public void setMonitorData(MonitorData data)
	{
		this.data=data;
	}

	public java.awt.Component getTreeCellRendererComponent(javax.swing.JTree tree,Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus) 
	{
		DefaultMutableTreeNode parent;
		DefaultMutableTreeNode thisNode;
		int index;
		if (data==null) 
		{
			finalValue.setText(value.toString());
			return finalValue;
		}
		
		super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
		thisNode=(DefaultMutableTreeNode)value;
		parent=(DefaultMutableTreeNode)thisNode.getParent();
		if ((parent!=null)) {
			if (parent==virtusNode)
			{
				index=parent.getIndex(thisNode);
				if (data.virtuals_flg[index])
				{
					finalValue.setForeground(java.awt.Color.green);
				}
				else
				{
					finalValue.setForeground(java.awt.Color.red);
				}
			}
			else if (parent==groupsNode)
			{
				index=parent.getIndex(thisNode);
				if (data.groups_flg[index])
				{
					finalValue.setForeground(java.awt.Color.green);
				}
				else
				{
					finalValue.setForeground(java.awt.Color.red);
				}
			} 
			else 
			{ 
				finalValue.setForeground(java.awt.Color.black);
			}
		} else 
		{ 
			finalValue.setForeground(java.awt.Color.black);
		}
		finalValue.setText(value.toString());
		return finalValue;
    }
}

public class MonitorDataFrame extends MonitorFrame
{
	javax.swing.JPanel posPanel;
	javax.swing.JLabel odomposLabel	= new JLabel ();
	javax.swing.JLabel odomaLabel	= new JLabel ();
	javax.swing.JLabel curposLabel	= new JLabel ();
	javax.swing.JLabel curaLabel		= new JLabel ();
	javax.swing.JLabel realposLabel	= new JLabel ();
	javax.swing.JLabel realaLabel	= new JLabel ();
	javax.swing.JLabel qltyLabel		= new JLabel ();
	
	javax.swing.JTree sensorTree;
	DefaultMutableTreeNode topNode;
	DefaultMutableTreeNode sensorsNode;
	DefaultMutableTreeNode actuatorsNode;
	DefaultMutableTreeNode payloadNode;
	DefaultMutableTreeNode virtusNode;
	DefaultMutableTreeNode groupsNode;
	DefaultMutableTreeNode digsNode;

	protected FusionDesc fdesc;
	protected PayloadDesc pdesc;
	
	ChildWindowListener parent;
	MonitorDataFrameRenderer renderer;

	public MonitorDataFrame (FusionDesc fdesc, PayloadDesc pdesc, ChildWindowListener parent)
	{
		this.fdesc = fdesc;
		this.pdesc = pdesc;
		this.parent = parent;
		
		initComponents();
		setVisible(true);
		pack();
		setResizable(true);
	}
	
	static public String format (double value, int len)
	{
		String			tmp;
		int				ndx;
		
		tmp = new Double (value).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + len + 1))		
			tmp = tmp.substring (0, ndx + len + 1);	
		
		return tmp;
	}
	
	private void initComponents()
	{
		int 			i;	
		DefaultMutableTreeNode auxNode1;

		getContentPane().setLayout(new BorderLayout ());
		
		posPanel=new javax.swing.JPanel(new java.awt.GridLayout(7,2));
		posPanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Position", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		posPanel.add (new JLabel ("Odometry XY: "));
		posPanel.add(odomposLabel);
		posPanel.add (new JLabel ("Odometry A: "));
		posPanel.add(odomaLabel);
		
		posPanel.add (new JLabel ("Estimated XY: "));
		posPanel.add (curposLabel);
		posPanel.add (new JLabel ("Estimated A: "));
		posPanel.add (curaLabel);
		
		posPanel.add (new JLabel ("Real XY: "));
		posPanel.add(realposLabel);
		posPanel.add (new JLabel ("Real A: "));
		posPanel.add(realaLabel);
		
		posPanel.add (new JLabel ("Quality: "));
		posPanel.add(qltyLabel);
		
		getContentPane().add(posPanel, BorderLayout.NORTH);

		topNode=new DefaultMutableTreeNode("Data");
		sensorsNode=new DefaultMutableTreeNode("Sensors");
		actuatorsNode=new DefaultMutableTreeNode("Actuators");
		payloadNode=new DefaultMutableTreeNode("Payload");
		virtusNode=new DefaultMutableTreeNode("Virtual Sensors");
		groupsNode=new DefaultMutableTreeNode("Group Sensors");
		digsNode=new DefaultMutableTreeNode("Digital Outputs");

		if (fdesc.MAXVIRTU>0)
		{
			for (i=0;i<fdesc.MAXVIRTU;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				virtusNode.add(auxNode1);
			}
			sensorsNode.add(virtusNode);
		}
		
		if (fdesc.MAXGROUP>0)
		{
			for (i=0;i<fdesc.MAXGROUP;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				groupsNode.add(auxNode1);
			}
			sensorsNode.add(groupsNode);
		}
				
		if (fdesc.MAXDSIG>0)
		{
			for (i=0;i<fdesc.MAXDSIG;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				digsNode.add(auxNode1);
			}
			actuatorsNode.add(digsNode);
		}	

		if (pdesc.MAXPAYLOAD>0)
		{
			for (i=0;i<pdesc.MAXPAYLOAD;i++)
			{
				auxNode1=new DefaultMutableTreeNode(pdesc.attrs[i].name);
				payloadNode.add(auxNode1);
			}
		}	

		topNode.add (sensorsNode);
		topNode.add (actuatorsNode);
		topNode.add (payloadNode);
		sensorTree=new javax.swing.JTree(topNode);
		sensorTree.putClientProperty("JTree.lineStyle", "Angled");
		sensorTree.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Sensors/Actuators", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		renderer = new MonitorDataFrameRenderer(virtusNode,groupsNode);
		renderer.setLeafIcon(null); // No icons will be shown in the leaves
		sensorTree.setCellRenderer(renderer);

		javax.swing.JScrollPane scrollTree=new javax.swing.JScrollPane(sensorTree);
		getContentPane().add(scrollTree, BorderLayout.CENTER);

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				close ();
			}
		});
	}

	public void updateData (MonitorData data)
	{
		int			i;
		DefaultMutableTreeNode auxNode1;
		
		if ((fdesc == null) || (data == null))			return;

		renderer.setMonitorData(data);

		// Odometry based position
		odomposLabel.setText(format(data.odom.x (), 3)+", "+format (data.odom.y (), 3)+" (m)");
		odomaLabel.setText(format(data.odom.alpha () * Angles.RTOD, 1)+" (deg)");
			
		// Current estimated position
		curposLabel.setText(format(data.cur.x (), 3)+", "+format (data.cur.y (), 3)+" (m)");
		curaLabel.setText(format(data.cur.alpha () * Angles.RTOD, 1)+" (deg)");
			
		// Real position (for simulator only)
		realposLabel.setText(format(data.real.x (), 3)+", "+format (data.real.y (), 3)+" (m)");
		realaLabel.setText(format(data.real.alpha () * Angles.RTOD, 1)+" (deg)");
		
		// Positioning quality
		qltyLabel.setText (format(data.qlty, 1));
		
		if(fdesc.MAXVIRTU>0)
		{
			virtusNode.removeAllChildren();
			for (i=0;i<fdesc.MAXVIRTU;i++)
			{
				auxNode1=new DefaultMutableTreeNode(String.valueOf(format (data.virtuals[i], 3)));
				virtusNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(virtusNode);
		}

		if(fdesc.MAXGROUP>0)
		{
			groupsNode.removeAllChildren();
			for (i=0;i<fdesc.MAXGROUP;i++)
			{
				auxNode1=new DefaultMutableTreeNode(String.valueOf(format (data.groups[i], 3)));
				groupsNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(groupsNode);
		}
		
		if (fdesc.MAXDSIG>0)
		{
			digsNode.removeAllChildren();
			for (i=0;i<fdesc.MAXDSIG;i++)
			{
				if (data.dsignals[i])
					auxNode1=new DefaultMutableTreeNode(new String("ON"));
				else
					auxNode1=new DefaultMutableTreeNode(new String("OFF"));
				digsNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(digsNode);
		}

		if (pdesc.MAXPAYLOAD>0)
		{
			payloadNode.removeAllChildren();
			for (i=0;i<pdesc.MAXPAYLOAD;i++)
			{
				auxNode1=new DefaultMutableTreeNode(pdesc.attrs[i].name+" = "+format(data.payload.data[i], 3)+" "+pdesc.attrs[i].unit);
				payloadNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(payloadNode);
		}	
	}
	
	public void close()
	{
		if (parent != null)
			parent.childClosed (this);
		setVisible(false);
	}
}