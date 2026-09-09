/*
 * Created on 26-may-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.gui.monitor;

import java.util.*;
import java.awt.*;
import javax.swing.*;

import tc.shared.linda.*;
import tc.coord.*;
import tc.gui.monitor.actions.*;
import tc.gui.monitor.frames.*;

import wucore.gui.*;
import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MonitorPanel extends JPanel implements ChildWindowListener
{
	protected JPanel 				debugPA		= new JPanel();
	protected JPanel 				actionsPA	= new JPanel();
	protected JPanel 				framesPA	= new JPanel();
	protected JPanel				svcsPA		= new JPanel();
	
	protected JToggleButton			rdataTB		= new JToggleButton ();
	protected JToggleButton			sensorsTB	= new JToggleButton ();
	protected JToggleButton			behinfoTB	= new JToggleButton ();
	protected JSlider 				stoutSL		= new JSlider();
	
	protected MultiRobotPanelInterf	parent;
	protected String				robotid;
	protected Properties			config;
	protected JMenu					menu;
	
	protected MonitorDataFrame		wrdata;
	protected World2DFrame			wsens;
	protected BehInfoFrame			wbehinfo;

	// Constructors
	public MonitorPanel (String robotid, MultiRobotPanelInterf parent, JMenu menu, Properties config)
	{
		super ();
		
		this.robotid		= robotid;
		this.parent		= parent;
		this.menu		= menu;
		this.config		= config;
		
		rdataTB.setVisible(true);
		rdataTB.setText("Data");
		sensorsTB.setVisible(true);
		sensorsTB.setText("Sensors");
		behinfoTB.setVisible(true);
		behinfoTB.setText("Behaviours");
		
		debugPA.setVisible (true);
		debugPA.setLayout (new GridLayout(3, 2));
		debugPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Debugging", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
	
		actionsPA.setVisible (true);
		actionsPA.setLayout (new GridLayout(3, 2));
		actionsPA.add (rdataTB);
		actionsPA.add (sensorsTB);
		actionsPA.add (behinfoTB);
		actionsPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Standard Windows", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
	
		svcsPA.setVisible (true);
		svcsPA.setLayout (new GridLayout(3, 2));

		stoutSL.setVisible (true);
		stoutSL.setMinimum(0);
		stoutSL.setMaximum(5000);
		stoutSL.setValue(2500);
		stoutSL.setLabelTable(stoutSL.createStandardLabels(1000));
		stoutSL.setPaintLabels(true);

		framesPA.setVisible (true);
		framesPA.setLayout (new BorderLayout());
		framesPA.add (stoutSL, BorderLayout.NORTH);
		framesPA.add (svcsPA, BorderLayout.CENTER);
		framesPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Monitoring Windows", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		setLayout (new BorderLayout());
		add (debugPA, BorderLayout.NORTH);
		add (framesPA, BorderLayout.CENTER);
		add (actionsPA, BorderLayout.SOUTH);

		setVisible (true);

		// Event handling
		stoutSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				stoutSliderChanged(e);
			}
		});
		rdataTB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				rdataTBActionPerformed(e);
			}
		});		
		sensorsTB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sensorsTBActionPerformed(e);
			}
		});		
		behinfoTB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				behinfoTBActionPerformed(e);
			}
		});		
	}
	
	// Instance methods
	public void delRobotJMenu(String id){
		JMenu menuitem;
		int i;
		boolean encontrado;

		i=0;
		encontrado=false;
		while(i<menu.getItemCount() && !encontrado){
			menuitem=(JMenu)menu.getItem(i);
			if(menuitem.getText().equals(id)){
				menu.remove(i);
				encontrado=true;
			}
			i++;
		}
	}
	
	public void setBehaviours (ItemBehInfo info)
	{
		if (wbehinfo != null)
			wbehinfo.updateData (info);
	}
	
	public void setRobotData (RobotList robots)
	{
		if (wrdata != null)
			wrdata.updateData (robots.getMData (robotid));
		
		if (wsens != null)
			wsens.updateData (robotid, robots);
	}
	
	public void close ()
	{
		// Close standard windows
		if (wrdata != null)			wrdata.close ();
		if (wsens != null)			wsens.close ();
		if (wbehinfo != null)		wbehinfo.close ();
	}
		
	/** Adds the correspondient mouse listener to the widget */
	private void addMouseListeners(RemoteAction act,Component2D component)	
	{
		String eventName;
		String method;
		eventName=act.getName();
		method=act.getMethod();
		if (eventName.equals("mouseClicked"))
		{
			component.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseClicked(java.awt.event.MouseEvent e) {
					componentMouseEventListener(e);
			} });
		} else {
		if (eventName.equals("mouseDragged"))
		{
			if (method.equals("")) // Must be managed by the client
			{
				component.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
					public void mouseDragged(java.awt.event.MouseEvent e) {
						mouseDraggedLocalEventListener(e);
				} });
			}
			else
			{
				component.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
					public void mouseDragged(java.awt.event.MouseEvent e) {
						componentMouseEventListener(e);
				} });
			}
		} else {
		if (eventName.equals("mouseEntered"))
		{
			component.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseEntered(java.awt.event.MouseEvent e) {
					componentMouseEventListener(e);
			} });
		} else {
		if (eventName.equals("mouseExited"))
		{
			component.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseExited(java.awt.event.MouseEvent e) {
					componentMouseEventListener(e);
			} });
		} else {
		if (eventName.equals("mouseMoved"))
		{
			component.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
				public void mouseMoved(java.awt.event.MouseEvent e) {
					componentMouseEventListener(e);
			} });
		} else {
		if (eventName.equals("mousePressed"))
		{
			if (method.equals("")) // Must be managed by the client
			{
				component.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
					public void mousePressed(java.awt.event.MouseEvent e) {
						mousePressedLocalEventListener(e);
				} });			
			}
			else
			{
				component.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mousePressed(java.awt.event.MouseEvent e) {
						componentMouseEventListener(e);
				} });
			}
		} else {
		if (eventName.equals("mouseReleased"))
		{
			if (method.equals(""))
			{
				component.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mouseReleased(java.awt.event.MouseEvent e) {
						mouseReleasedLocalEventListener(e);
				} });
			}
			else
			{
				component.addMouseListener(new java.awt.event.MouseAdapter() {
					public void mouseReleased(java.awt.event.MouseEvent e) {
						componentMouseEventListener(e);
				} });
			}
		} else {
			System.out.println("--[BGWCli] Mouse event "+eventName+" not recognized. It will not be processed.");
		}}}}}}};
	}

	/** Listener for a debug checkbox */
	protected void debugBooleanActionListener (java.awt.event.ItemEvent e)
	{
		JCheckBox		dbgCB;
		String 			actionName;
		Object[] 		value = new Object[1];
		
		dbgCB		= (JCheckBox) e.getSource();
		actionName	= (String) dbgCB.getClientProperty("ActionName");
		value[0]		= new Boolean (dbgCB.isSelected ());

		parent.getMonitor ().setGUIActionCtrl (ItemGUICtrl.ACT_DEBUG, null, actionName, value);
	}

	/** Listener for menu options with no arguments */
	protected void menuEmptyActionListener (java.awt.event.ActionEvent e)
	{
		JMenuItem mitem;
		String actionName;
		String servName;
		mitem=(JMenuItem)e.getSource();
		actionName=(String)mitem.getClientProperty("ActionName");
		servName=(String)mitem.getClientProperty("ServiceName");		

		parent.getMonitor ().setGUIActionCtrl(ItemGUICtrl.ACT_SERVICE, servName, actionName, null);
	}
	
	/** Listener for menu options with a boolean argument */
	protected void menuBooleanActionListener (java.awt.event.ItemEvent e)
	{
		JMenuItem mitem;
		String actionName;
		String servName;
		Object[] value = new Object[1];
		mitem=(JMenuItem)e.getSource();
		actionName=(String)mitem.getClientProperty("ActionName");
		servName=(String)mitem.getClientProperty("ServiceName");		
		value[0]=new Boolean(mitem.isSelected());

		parent.getMonitor ().setGUIActionCtrl(ItemGUICtrl.ACT_SERVICE, servName, actionName, value);
	}

	/** Listener for menu options with a multivaluated argument */
	protected void menuMultivalueActionListener (java.awt.event.ItemEvent e)
	{
		JRadioButtonMenuItem mitem;
		String actionName;
		String servName;
		Object[] value=new Object[1];
		mitem=(JRadioButtonMenuItem)e.getSource();
		actionName=(String)mitem.getClientProperty("ActionName");
		servName=(String)mitem.getClientProperty("ServiceName");
		value[0]=mitem.getClientProperty("Value");

		parent.getMonitor ().setGUIActionCtrl(ItemGUICtrl.ACT_SERVICE, servName, actionName, value);
	}

	/** Listener for the mouse events that occurs in the C2DRemote */
	protected void componentMouseEventListener (java.awt.event.MouseEvent e)
	{
		String servName;
		Component2D auxC2D;
		auxC2D=(Component2D)e.getSource();		
		servName=(String)auxC2D.getClientProperty("ServiceName");

		switch(e.getID())
		{
			case java.awt.event.MouseEvent.MOUSE_CLICKED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseClicked", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_DRAGGED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseDragged", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_ENTERED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseEntered", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_EXITED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseExited", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_MOVED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseMoved", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_PRESSED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseClicked", e);
				break;
			case java.awt.event.MouseEvent.MOUSE_RELEASED:
				parent.getMonitor ().setGUIEventCtrl(servName, "mouseReleased", e);
				break;
			default:
				System.out.println("--[MonPanel] Mouse event not recognized!");
		}
	}

	/** Listener for a local management of a "mousePressed" event on a C2DRemote */
	protected void mousePressedLocalEventListener (java.awt.event.MouseEvent e)
	{
		Component2D auxC2D;		
		auxC2D=(Component2D)e.getSource();		
		if (e.isControlDown ())
			auxC2D.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
		else
			auxC2D.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
		auxC2D.mouseDown (e.getX (), e.getY ());
	}

	/** Listener for a local management of a "mouseDragged" event on a C2DRemote */
	protected void mouseDraggedLocalEventListener (java.awt.event.MouseEvent e)
	{
		Component2D auxC2D;
		auxC2D=(Component2D)e.getSource();		
		if (e.isControlDown ())
		{
			auxC2D.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
			auxC2D.mouseZoom (e.getX (), e.getY ());
		}
		else
		{
			auxC2D.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
			auxC2D.mousePan (e.getX (), e.getY ());
		}
	}

	/** Listener for a local management of a "mouseReleased" event on a C2DRemote */
	protected void mouseReleasedLocalEventListener (java.awt.event.MouseEvent e)
	{
		Component2D auxC2D;
		auxC2D=(Component2D)e.getSource();
		auxC2D.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));		
	}

	private int gridIndex = 0;

	/** Listener for the frames resize	 */
	protected void frameResizeListener (java.awt.event.ComponentEvent e)
	{
		RemoteMonitorFrame frame;
		Component2D component;
		frame=(RemoteMonitorFrame)e.getSource();
		component=(Component2D) frame.getComponent();		
		component.setSize(frame.getContentPane().getSize());
	}

	/** Listener for the frames close */
	protected void frameCloseListener (java.awt.event.WindowEvent e)
	{
		RemoteMonitorFrame frame;
		JToggleButton button;
		
		frame=(RemoteMonitorFrame)e.getSource();
		button=(JToggleButton)frame.getFrameLink ();
		button.doClick();
	}

	public void childClosed (Object window)	
	{
		//String rid = ((MonitorFrame) window).getIdentifier ();
		try
		{
			if (window instanceof World2DFrame)
			{
				wsens			= null;
				sensorsTB.setSelected (false);
			}
			else if (window instanceof MonitorDataFrame)
			{
				wrdata			= null;
				rdataTB.setSelected (false);
			}
			else if (window instanceof BehInfoFrame)
			{
				wbehinfo		= null;
				behinfoTB.setSelected (false);
			}
		} catch (Exception e) { e.printStackTrace (); }
	}

	protected void rdataTBActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (!rdataTB.isSelected ())
		{
			if (wrdata != null)
				wrdata.close ();
			
			return;
		}
		
		if ((parent.getMonitor () == null) || (wrdata != null))			 return;

		wrdata = new MonitorDataFrame (parent.getRobotList ().getFusion (robotid), parent.getRobotList ().getDesc (robotid).pldesc, this);
		wrdata.configure ("RDATA", config);
		wrdata.setIdentifier (robotid);
		wrdata.setTitle("Robot <"+ robotid +"> data");
	}
	
	/** Listener for the Window->Behaviour Information->'Robotname' menu option */
	protected void behinfoTBActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (!behinfoTB.isSelected ())
		{
			if (wbehinfo != null)
				wbehinfo.close ();
			
			return;
		}
		
		if ((parent.getMonitor () == null) || (wbehinfo != null))			 return;

		 wbehinfo = new BehInfoFrame(robotid, parent.getMonitor (), this, 5, 170);
		 wbehinfo.configure ("BEHINFO", config);
		 wbehinfo.setIdentifier (robotid);
		 wbehinfo.toFront ();
	}

	/** Listener for the Window->Sensors->'RobotName' menu option */
	protected void sensorsTBActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (!sensorsTB.isSelected ())
		{
			if (wsens != null)
				wsens.close ();
			
			return;
		}
		
		if ((parent.getMonitor () == null) || (wsens != null))			 return;

		wsens = new World2DFrame (this);
		wsens.configure ("SEN", config);
		wsens.setIdentifier (robotid);
		wsens.setTitle("Robot <"+ robotid +"> sensors");
		wsens.open (parent.getWorldMap (), parent.getRobotList ().getDesc (robotid));
	}

	protected void stoutSliderChanged(javax.swing.event.ChangeEvent e)
	{
		if (stoutSL.getValueIsAdjusting()) 			return;
		
		parent.getMonitor ().setID (robotid);
		parent.getMonitor ().setGUITimeoutCtrl (stoutSL.getValue());
	}
}
