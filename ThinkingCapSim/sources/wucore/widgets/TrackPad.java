/*
 * (c) 2001 Juan Pedro Canovas Qui–onero
 * (c) 2002 Humberto Martinez Barbera
 * (c) 2003 David Herrero Perez
 */

package wucore.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import wucore.widgets.input.*;

class Pad extends JComponent
{
	protected int					pointX;
	protected int					pointY;
	protected boolean				moved		= false;
	
	// Constructors
	public Pad ()
	{
		this.setEnabled(true);
		this.setBorder(BorderFactory.createLineBorder(Color.black,2));
		
		this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				thisMouseDragged(e);
			}
		});
		this.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent e) {
				thisMouseReleased(e);
			}
		});
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				super.componentResized (e);
				thisComponentResized(e);
			}
		});
	}  
	
	public void thisComponentResized (ComponentEvent e)
	{
		Dimension		d;
		
		d		= getSize ();
		pointX	= d.width / 2;
		pointY	= d.height / 2;
		repaint ();
	}
	
	public void thisMouseReleased (MouseEvent e)
	{
		Dimension		d;
		
		d		= getSize ();
		pointX	= (d.width/2);
		pointY	= (d.height/2);
		moved	= false;
		repaint ();
	}
	
	public void thisMouseDragged (MouseEvent e)
	{
		pointX	= e.getX ();
		pointY	= e.getY ();
		moved	= true;
		repaint ();
	}
	
	protected void paintComponent (Graphics g)
	{
		Dimension		d;
		int				w;
		
		d = getSize ();
		w = (int)(d.width*0.05);
		g.clearRect(0,0,d.width,d.height);
		g.drawLine (d.width/2,0,d.width/2,d.height);
		g.drawLine (0,d.height/2,d.width,d.height/2);
		g.fillOval (pointX-(w/2),pointY-(w/2),w,w);    	
	}
}

public class TrackPad extends JPanel
{
	protected TrackPadThread			tpthread;
	protected boolean				moved		= false;
	protected boolean				but1_prs		= false;
	protected boolean				but2_prs		= false;
	protected boolean				but3_prs		= false;
	protected boolean				but4_prs		= false;
	protected boolean				but5_prs		= false;
	protected boolean				but6_prs		= false;
	protected boolean				but7_prs		= false;
	protected boolean				but8_prs		= false;
	
	// Widgets
	protected Pad					pad;
	protected JSlider 				zbar;
	protected JPanel					butP;
	protected JButton				but1;
	protected JButton				but2;
	protected JButton				but3;
	protected JButton				but4;
	protected JButton				but5;
	protected JButton				but6;
	protected JButton				but7;
	protected JButton				but8;
	
	// Constructors
	public TrackPad ()
	{
		pad	= new Pad ();
		pad.setVisible (true);
		zbar = new JSlider ();
		zbar.setOrientation (JSlider.VERTICAL);
		zbar.setVisible (true);
		butP = new JPanel ();
		butP.setVisible (true);
		butP.setLayout (new GridLayout (2, 4));
		but1 = new JButton ("1");
		but1.setVisible (true);
		but1.setPreferredSize (new Dimension (50,30));
		but2 = new JButton ("2");
		but2.setVisible (true);
		but2.setPreferredSize (new Dimension (50,30));
		but3 = new JButton ("3");
		but3.setVisible (true);
		but3.setPreferredSize (new Dimension (50,30));
		but4 = new JButton ("4");
		but4.setVisible (true);
		but4.setPreferredSize (new Dimension (50,30));
		but5 = new JButton ("5");
		but5.setVisible (true);
		but5.setPreferredSize (new Dimension (50,30));
		but6 = new JButton ("6");
		but6.setVisible (true);
		but6.setPreferredSize (new Dimension (50,30));
		but7 = new JButton ("7");
		but7.setVisible (true);
		but7.setPreferredSize (new Dimension (50,30));
		but8 = new JButton ("8");
		but8.setVisible (true);
		but8.setPreferredSize (new Dimension (50,30));
		butP.add (but1);
		butP.add (but2);
		butP.add (but3);
		butP.add (but4);
		butP.add (but5);
		butP.add (but6);
		butP.add (but7);
		butP.add (but8);
		
		this.setSize (230,230);
		this.setEnabled (true);   	
		this.setLayout (new java.awt.BorderLayout());
		this.add (pad, BorderLayout.CENTER);
		this.add (zbar, BorderLayout.WEST);
		this.add (butP, BorderLayout.SOUTH);
		this.setVisible (true); 
		
		zbar.addChangeListener(new javax.swing.event.ChangeListener(){
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				zbarAdjustmentValueChanged(e);
			}
		}); 
		
		but1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but1ActionPerformed(e);
			}
		});
		but2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but2ActionPerformed(e);
			}
		});
		but3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but3ActionPerformed(e);
			}
		});
		but4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but4ActionPerformed(e);
			}
		});
		but5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but5ActionPerformed(e);
			}
		});
		but6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but6ActionPerformed(e);
			}
		});
		but7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but7ActionPerformed(e);
			}
		});
		but8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				but8ActionPerformed(e);
			}
		});
	}  
	
	public void start (InputDevice[] inputs, TrackPadListener listener)
	{
		Thread			thread;
		
		tpthread	= new TrackPadThread (this, inputs, listener);
		thread		= new Thread (tpthread);
		thread.start ();
	}
	
	public void stop ()
	{
		if (tpthread != null)
		{
			tpthread.stop ();
			tpthread	= null;
		}
	}
	
	public void setTrackPadListener (TrackPadListener listener) { if (tpthread != null) tpthread.setTrackPadListener(listener); }
	
	public void zbarAdjustmentValueChanged(javax.swing.event.ChangeEvent e)
	{
		moved		= true;
	}
	
	public void but1ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but1_prs	= true;
	}
	
	public void but2ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but2_prs	= true;
	}
	
	public void but3ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but3_prs	= true;
	}
	
	public void but4ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but4_prs	= true;
	}
	public void but5ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but5_prs	= true;
	}
	
	public void but6ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but6_prs	= true;
	}
	
	public void but7ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but7_prs	= true;
	}
	
	public void but8ActionPerformed(java.awt.event.ActionEvent e) 
	{
		but8_prs	= true;
	}
}

class TrackPadThread implements Runnable
{
	static public final int 		TIMESTEP	= 100;		// Time step (ms)
	
	private InputDevice[]			sticks		= null;
	private Pad						pad			= null;
	private TrackPad				tpad		= null;
	private TrackPadListener		listener	= null;
	private volatile boolean		running		= false;
	
	public TrackPadThread (TrackPad tpad, InputDevice[] sticks, TrackPadListener listener)
	{
		this.tpad		= tpad;
		this.pad		= tpad.pad;
		this.sticks		= sticks;
		this.listener	= listener;
	}
	
	public void setTrackPadListener(TrackPadListener listener) { this.listener = listener; }
	
	public void stop ()
	{
		running		= false;
	}
	
	public void run ()
	{
		int				i;
		double			x, y, z;
		double			lastx = Double.MAX_VALUE;
		double			lasty = Double.MAX_VALUE;
		double			lastz = Double.MAX_VALUE;
		int				b;
		boolean			but1, but2, but3, but4;
		boolean			but5, but6, but7, but8;
		int				buttons;
		double			w2, h2, z2;
		boolean			changed;
		boolean			stopped;
		Dimension		d;
		long			ct, tk;
		
		running		= true;
		
		while (running)
		{
			// Compute time related operations
			tk		= System.currentTimeMillis ();
			
			// Compute current pad values
			d	= pad.getSize ();
			w2	= (double) (d.width / 2);
			h2	= (double) (d.height / 2);
			z2	= (double) ((tpad.zbar.getMaximum () - tpad.zbar.getMinimum ()) / 2);
			
			if ((d.height != 0) && (d.width != 0))
			{
				x	= ((double) pad.pointX - w2) / w2;
				y	= ((double) pad.pointY - h2) / h2;
				z	= ((double) tpad.zbar.getValue () - z2) / z2;
				
				but1 = tpad.but1_prs;
				but2 = tpad.but2_prs;
				but3 = tpad.but3_prs;
				but4 = tpad.but4_prs;
				but5 = tpad.but5_prs;
				but6 = tpad.but6_prs;
				but7 = tpad.but7_prs;
				but8 = tpad.but8_prs;
				
				// Check if joystick is present 
				if (sticks != null)
					for (i = 0; i < sticks.length; i++)
						if (sticks[i] != null)
						{
							// Check if mouse has not been dragged
							if (!pad.moved)
							{
								pad.pointX = (int) Math.round (sticks[i].getXPos () * w2 + w2);
								pad.pointY = (int) Math.round (sticks[i].getYPos () * h2 + h2);
								pad.repaint ();
								
								x	= ((double) pad.pointX - w2) / w2;
								y	= ((double) pad.pointY - h2) / h2;
							}
							
							// Check if slider has been moved
							if (!tpad.moved)
							{
								tpad.zbar.setValue ((int) Math.round (sticks[i].getZPos () * z2 + z2));
								z	= ((double) tpad.zbar.getValue () - z2) / z2;
							}
							
							// Check if any button is pressed
							b		= sticks[i].getButtons ();
							but1	= (((b & InputDevice.BUTTON1) > 0) || but1);
							but2	= (((b & InputDevice.BUTTON2) > 0) || but2);
							but3	= (((b & InputDevice.BUTTON3) > 0) || but3);
							but4	= (((b & InputDevice.BUTTON4) > 0) || but4);
							but5	= (((b & InputDevice.BUTTON5) > 0) || but5);
							but6	= (((b & InputDevice.BUTTON6) > 0) || but6);
							but7	= (((b & InputDevice.BUTTON7) > 0) || but7);
							but8	= (((b & InputDevice.BUTTON8) > 0) || but8);
						}
				
				changed	= ((lastx != x) || (lasty != y) || (lastz != z));
				stopped	= ((x == 0.0) && (y == 0.0) && (z == 0.0));
				buttons	= 0;
				if (but1)			buttons += InputDevice.BUTTON1;
				if (but2)			buttons += InputDevice.BUTTON2;
				if (but3)			buttons += InputDevice.BUTTON3;
				if (but4)			buttons += InputDevice.BUTTON4;
				if (but5)			buttons += InputDevice.BUTTON5;
				if (but6)			buttons += InputDevice.BUTTON6;
				if (but7)			buttons += InputDevice.BUTTON7;
				if (but8)			buttons += InputDevice.BUTTON8;
				
				// Notify listener
				if (listener != null) 
				{
					if (!stopped || (stopped && changed))		listener.updatePosition (x, y, z);
					if (buttons > 0)							listener.updateButtons (buttons);
				}
				
				lastx	= x;
				lasty	= y;
				lastz	= z; 
			}			
			
			tpad.but1_prs 	= false;
			tpad.but2_prs 	= false;
			tpad.but3_prs 	= false;
			tpad.but4_prs 	= false;
			tpad.but5_prs 	= false;
			tpad.but6_prs 	= false;
			tpad.but7_prs 	= false;
			tpad.but8_prs 	= false;
			tpad.moved		= false;
			
			// Sleep for little time	
			ct		= System.currentTimeMillis () - tk;
			if (ct < TIMESTEP)
				try { Thread.sleep (TIMESTEP - ct); } catch (Exception e) { }
		}
	}
}
