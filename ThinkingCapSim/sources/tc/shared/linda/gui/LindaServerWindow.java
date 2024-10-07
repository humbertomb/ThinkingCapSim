/*
 * Created on 09-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import tc.shared.linda.LindaSpace;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaServerWindow extends Frame implements Runnable
{	
	public static final String		NAME		= "WU-LindaSpaces-Window";

	protected LindaSpace			linda;
	protected Thread				curthread	= null;
	private volatile boolean		running		= false;
	
	// GUI components
	protected JPanel 				thPanel;
	protected JPanel 				txPanel;
	protected JToggleButton			expandBU;
	protected JTextField 			freeTF;
	protected JTextField 			thsTF;
	protected JLabel 				label4;
	protected JLabel 				label3;
	protected JLabel 				spacesTA;
	protected JScrollPane			jsMain;

	// Constructors
	public LindaServerWindow (String name, LindaSpace linda)
	{
		this.linda		= linda;
		
		jsMain = new JScrollPane();
		setLayout(new java.awt.BorderLayout(0, 0));
		setVisible(false);
		setLocation (10, 50);
		setSize(400,350);
		thPanel = new JPanel();
		thPanel.setLayout(new GridLayout(1,1));
		jsMain.setViewportView(thPanel);
		add(jsMain, BorderLayout.CENTER);
		txPanel = new JPanel();
		txPanel.setLayout(new GridLayout(1,5));
		add(txPanel, BorderLayout.SOUTH);
		
		expandBU = new JToggleButton("Expand", false);
		expandBU.setVisible (true);
		freeTF = new JTextField();
		freeTF.setEditable (false);
		thsTF = new JTextField();
		thsTF.setEditable (false);
		label3 = new JLabel("Free");
		label3.setHorizontalAlignment (SwingConstants.RIGHT);
		label4 = new JLabel("Threads");
		label4.setHorizontalAlignment (SwingConstants.RIGHT);
		
		txPanel.add(expandBU);
		txPanel.add(label3);
		txPanel.add(freeTF);
		txPanel.add(label4);
		txPanel.add(thsTF);
		
		spacesTA = new JLabel();
		spacesTA.setVerticalAlignment (SwingConstants.TOP);
		spacesTA.setFont(new java.awt.Font("Monospaced", 0, 12));
		spacesTA.setText ("<HTML>  </HTML>");
		thPanel.add(spacesTA);
		
		setTitle("Linda Space Monitor: "+name);

		SymWindow aSymWindow = new SymWindow();
		this.addWindowListener(aSymWindow);
	}

	// Instance methods	
	public void open ()
	{
		setVisible (true);
		
		if (curthread == null)
		{
			curthread	= new Thread (this);
			curthread.start ();
		}
	}

	public void close ()
	{
		if (curthread != null)
		{
			running		= false;
			curthread	= null;
		}

		setVisible (false);
		dispose ();
	}

	public void run ()
	{
		Thread.currentThread ().setName (NAME);
		running		= true;
		
		while (running)
		{
			showData ();
			try { Thread.sleep (2000); } catch (Exception e) { }
			Thread.yield ();
		}
	}

	protected void showData ()
	{
		Runtime				rt;
		long				fm;
		
		rt = Runtime.getRuntime ();
		fm = rt.freeMemory () / 1000;
		
		freeTF.setText (new Integer ((int) fm).toString ());
		thsTF.setText ("----");
		spacesTA.setText (linda.toHTML (expandBU.isSelected ()));
	}		
	
	public void addNotify()
	{
		// Record the size of the window prior to calling parents addNotify.
		Dimension d = getSize();
		
		super.addNotify();

		if (fComponentsAdjusted)
			return;

		// Adjust components according to the insets
		setSize(getInsets().left + getInsets().right + d.width, getInsets().top + getInsets().bottom + d.height);
		Component components[] = getComponents();
		for (int i = 0; i < components.length; i++)
		{
			Point p = components[i].getLocation();
			p.translate(getInsets().left, getInsets().top);
			components[i].setLocation(p);
		}
		fComponentsAdjusted = true;
	}

	// Used for addNotify check.
	boolean fComponentsAdjusted = false;

	class SymWindow extends java.awt.event.WindowAdapter
	{
		public void windowClosing(java.awt.event.WindowEvent event)
		{
			Object object = event.getSource();
			if (object == LindaServerWindow.this)
				LindaSpacesWindow_WindowClosing(event);
		}
	}
	
	void LindaSpacesWindow_WindowClosing(java.awt.event.WindowEvent event)
	{
		close ();
	}
}
