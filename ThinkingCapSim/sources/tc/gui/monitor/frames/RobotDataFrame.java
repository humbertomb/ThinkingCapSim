/*
 * (c) 2002-2003 Bernardo Canovas Segura
 */
 
package tc.gui.monitor.frames;

import javax.swing.tree.*;

import tc.vrobot.*;

import wucore.gui.*;

class RobotDataFrameRenderer extends DefaultTreeCellRenderer
{
	DefaultMutableTreeNode irsNode;
	DefaultMutableTreeNode sonarsNode;
	DefaultMutableTreeNode lrfsNode;
	
	RobotData data=null;

	private javax.swing.JLabel finalValue;
	
	public RobotDataFrameRenderer(
		DefaultMutableTreeNode irsNode,
		DefaultMutableTreeNode sonarsNode,
		DefaultMutableTreeNode lrfsNode)
	{
		finalValue=new javax.swing.JLabel();
		this.irsNode=irsNode;
		this.sonarsNode=sonarsNode;
		this.lrfsNode=lrfsNode;
	}
	
	public void setRobotData(RobotData data)
	{
		this.data=data;
	}

	public java.awt.Component getTreeCellRendererComponent(javax.swing.JTree tree,Object value,boolean sel,boolean expanded,boolean leaf,int row,boolean hasFocus) 
	{
		DefaultMutableTreeNode parent;
		DefaultMutableTreeNode grandparent;
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
			if (parent==irsNode)
			{
				index=parent.getIndex(thisNode);
				if (data.irs_flg[index])
				{
					finalValue.setForeground(java.awt.Color.green);
				}
				else
				{
					finalValue.setForeground(java.awt.Color.red);
				}
			}
			else if (parent==sonarsNode)
			{
				index=parent.getIndex(thisNode);
				if (data.sonars_flg[index])
				{
					finalValue.setForeground(java.awt.Color.green);
				}
				else
				{
					finalValue.setForeground(java.awt.Color.red);
				}
			} 
			else if ((parent.isNodeAncestor(lrfsNode))&&(parent!=lrfsNode))
			{
				grandparent=(DefaultMutableTreeNode)parent.getParent();
				index=grandparent.getIndex(parent);
				if (data.lrfs_flg[index])
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

public class RobotDataFrame extends MonitorFrame
{
	javax.swing.JPanel odomPanel;
	javax.swing.JLabel labelaux;
	javax.swing.JLabel odomposLabel;
	javax.swing.JLabel odomaLabel;
	javax.swing.JLabel realposLabel;
	javax.swing.JLabel realaLabel;

	javax.swing.JPanel otherPanel;
	javax.swing.JLabel turnLabel;
	javax.swing.JLabel speedLabel;
	
	javax.swing.JTree sensorTree;
	DefaultMutableTreeNode sensorsNode;
	DefaultMutableTreeNode trackersNode;
	DefaultMutableTreeNode irsNode;
	DefaultMutableTreeNode sonarsNode;
	DefaultMutableTreeNode lrfsNode;
	DefaultMutableTreeNode bumpersNode;

	RobotDesc rdesc=null;
	ChildWindowListener parent;
	RobotDataFrameRenderer renderer;

	public RobotDataFrame (RobotDesc rdesc,ChildWindowListener parent)
	{
		this.rdesc=rdesc;
		this.parent=parent;
		
		initComponents();
		setVisible(true);
		getContentPane().setLayout(new javax.swing.BoxLayout(this.getContentPane(),javax.swing.BoxLayout.Y_AXIS));
		pack();
		setResizable(true);
	}
	
	private void initComponents()
	{
		int i,j;
		odomPanel=new javax.swing.JPanel(new java.awt.GridLayout(6,2));
		odomposLabel=new javax.swing.JLabel();
		odomaLabel=new javax.swing.JLabel();
		realposLabel=new javax.swing.JLabel();
		realaLabel=new javax.swing.JLabel();

		otherPanel=new javax.swing.JPanel(new java.awt.GridLayout(3,2));
		turnLabel=new javax.swing.JLabel();
		speedLabel=new javax.swing.JLabel();

		odomPanel.setBorder(new javax.swing.border.TitledBorder("Odometry"));
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Position: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		odomposLabel.setText("");
		odomposLabel.setVisible(true);
		odomPanel.add(odomposLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Rho: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Alpha: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		odomaLabel.setText("");
		odomaLabel.setVisible(true);
		odomPanel.add(odomaLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Real position: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		realposLabel.setText("");
		realposLabel.setVisible(true);
		odomPanel.add(realposLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Real alpha: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		realaLabel.setText("");
		realaLabel.setVisible(true);
		odomPanel.add(realaLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Compass: ");
		labelaux.setVisible(true);
		odomPanel.add(labelaux);
		getContentPane().add(odomPanel);

		otherPanel.setBorder(new javax.swing.border.TitledBorder("Others"));
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Turn: ");
		labelaux.setVisible(true);
		otherPanel.add(labelaux);
		turnLabel.setText("");
		turnLabel.setVisible(true);
		otherPanel.add(turnLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Speed: ");
		labelaux.setVisible(true);
		otherPanel.add(labelaux);
		speedLabel.setText("");
		speedLabel.setVisible(true);
		otherPanel.add(speedLabel);
		labelaux=new javax.swing.JLabel();
		labelaux.setText("Quality: ");
		labelaux.setVisible(true);
		otherPanel.add(labelaux);
		getContentPane().add(otherPanel);
	
		DefaultMutableTreeNode auxNode1,auxNode2;

		sensorsNode=new DefaultMutableTreeNode("Sensors");
		trackersNode=new DefaultMutableTreeNode("Trakers");
		irsNode=new DefaultMutableTreeNode("IR Sensors");
		sonarsNode=new DefaultMutableTreeNode("Sonars");
		lrfsNode=new DefaultMutableTreeNode("Laser range finders");
		bumpersNode=new DefaultMutableTreeNode("Bumpers");

		if (rdesc.MAXTRACKER>0)
		{
			for(i=0;i<3;i++)
			{
				auxNode1=new DefaultMutableTreeNode("Tracker "+i);
				for (j=0;j<rdesc.MAXTRACKER;j++)
				{
					auxNode2=new DefaultMutableTreeNode(new String("---"));		
					auxNode1.add(auxNode2);
				}
				trackersNode.add(auxNode1);
			}
			sensorsNode.add(trackersNode);
		}

		if (rdesc.MAXIR>0)
		{
			for (i=0;i<rdesc.MAXIR;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				irsNode.add(auxNode1);
			}
			sensorsNode.add(irsNode);
		}
		
		if (rdesc.MAXSONAR>0)
		{
			for (i=0;i<rdesc.MAXSONAR;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				sonarsNode.add(auxNode1);
			}
			sensorsNode.add(sonarsNode);
		}
		
		if (rdesc.MAXLRF>0)
		{
			for(i=0;i<rdesc.MAXLRF;i++)
			{
				auxNode1=new DefaultMutableTreeNode("LRF"+i+" rays");
				for (j=0;j<rdesc.RAYLRF;j++)
				{
					auxNode2=new DefaultMutableTreeNode(new String("---"));		
					auxNode1.add(auxNode2);
				}
				lrfsNode.add(auxNode1);
			}
			sensorsNode.add(lrfsNode);
		}
		
		if (rdesc.MAXBUMPER>0)
		{
			for (i=0;i<rdesc.MAXBUMPER;i++)
			{
				auxNode1=new DefaultMutableTreeNode(new String("---"));
				bumpersNode.add(auxNode1);
			}
			sensorsNode.add(bumpersNode);
		}	

		sensorTree=new javax.swing.JTree(sensorsNode);
		sensorTree.putClientProperty("JTree.lineStyle", "Angled");
		sensorTree.setBorder(new javax.swing.border.TitledBorder("Sensors"));
		renderer = new RobotDataFrameRenderer(irsNode,sonarsNode,lrfsNode);
		renderer.setLeafIcon(null); // No icons will be shown in the leaves
		sensorTree.setCellRenderer(renderer);

		javax.swing.JScrollPane scrollTree=new javax.swing.JScrollPane(sensorTree);
		getContentPane().add(scrollTree);

		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				close ();
			}
		});
	}

	public void updateData (RobotData data)
	{
		int			ndx,i,j;
		String		tmp;
		String		sx, sy, sa;
		DefaultMutableTreeNode auxNode1;
		DefaultMutableTreeNode auxNode2;
		
		if ((rdesc == null) || (data == null))			return;

		renderer.setRobotData(data);

		// Read odometry data
		tmp = sx = new Double (data.odom_x).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 4))		
			sx = tmp.substring (0, ndx + 4);
		
		tmp = sy = new Double (data.odom_y).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 4))		
			sy = tmp.substring (0, ndx + 4);
		
		odomposLabel.setText(sx+", "+sy);
		
		tmp = sa = new Double (data.odom_a ).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 2))		
			sa = tmp.substring (0, ndx + 2);
			
		odomaLabel.setText(sa);
			
		tmp = sx = new Double (data.real_x).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 4))		
			sx = tmp.substring (0, ndx + 4);
		
		tmp = sy = new Double (data.real_y).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 4))		
			sy = tmp.substring (0, ndx + 4);
		
		realposLabel.setText(sx+", "+sy);

		tmp = sa = new Double (data.real_a ).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 2))		
			sa = tmp.substring (0, ndx + 2);
			
		realaLabel.setText(sa);
		
		// Read other data
		
/*		tmp = new Double (data.turn ).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 2))		
			sa = tmp.substring (0, ndx + 2);
		
		turnLabel.setText(sa);

		tmp = new Double (data.speed ).toString ();
		if (((ndx = tmp.indexOf (".")) != 0) && (tmp.length () >= ndx + 2))		
			sa = tmp.substring (0, ndx + 2);
		
		speedLabel.setText(sa);
*/
		// Read sensor data
	
		// Shows the trakers data
/*
		if (rdesc.MAXTRACKER>0)
		{
			for(i=0;i<3;i++)
			{
				auxNode1 = (DefaultMutableTreeNode)trackersNode.getChildAt(i);
				auxNode1.removeAllChildren();	
				for (j=0;j<rdesc.MAXTRACKER;j++)
				{
					auxNode2=new DefaultMutableTreeNode(String.valueOf(data.trackers[j][i]));
					auxNode1.add(auxNode2);
				}
				((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(auxNode1);
			}
		}
*/		
		// Shows the irs data
		if(rdesc.MAXIR>0)
		{
			irsNode.removeAllChildren();
			for (i=0;i<rdesc.MAXIR;i++)
			{
				auxNode1=new DefaultMutableTreeNode(String.valueOf(data.irs[i]));
				irsNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(irsNode);
		}

		// Shows the sonar data
		if(rdesc.MAXSONAR>0)
		{
			sonarsNode.removeAllChildren();
			for (i=0;i<rdesc.MAXSONAR;i++)
			{
				auxNode1=new DefaultMutableTreeNode(String.valueOf(data.sonars[i]));
				sonarsNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(sonarsNode);
		}
		
		//Shows the laser range finder data
		if (rdesc.MAXLRF>0)
		{
			for(i=0;i<rdesc.MAXLRF;i++)
			{
				auxNode1 = (DefaultMutableTreeNode)lrfsNode.getChildAt(i);
				auxNode1.removeAllChildren();	
				for (j=0;j<rdesc.RAYLRF;j++)
				{
					auxNode2=new DefaultMutableTreeNode(String.valueOf(data.lrfs[i][j]));
					auxNode1.add(auxNode2);
				}
				((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(auxNode1);
			}
		}
				
		// Shows the bumpers data
		if (rdesc.MAXBUMPER>0)
		{
			bumpersNode.removeAllChildren();
			for (i=0;i<rdesc.MAXBUMPER;i++)
			{
				if (data.bumpers[i])
					auxNode1=new DefaultMutableTreeNode(new String("ON"));
				else
					auxNode1=new DefaultMutableTreeNode(new String("OFF"));
				bumpersNode.add(auxNode1);
			}
			((DefaultTreeModel)sensorTree.getModel()).nodeStructureChanged(bumpersNode);
		}
	}
	
	public void close()
	{
		if (parent != null)
			parent.childClosed (this);
		setVisible(false);
	}
}