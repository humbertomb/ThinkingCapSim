package tclib.utils.petrinets.gui.dialog;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import tclib.utils.petrinets.gui.*;

public class PNOptionsDialog extends JDialog {
	
	JPanel Input, Buttons;
	JCheckBox showbox;
	JSlider delay;
	JLabel delayCount;
	
	public PNOptionsDialog(Frame parent) {
		super(parent, "Edit Options", false);
		
		getContentPane ().setLayout(new BorderLayout());
		
		Input = new JPanel();
		Input.setLayout(new GridLayout(0,2,2,2));
		
		
		getContentPane ().add("North", new JLabel("<HTML><B>Enter Options</B></HTML>"));
		
		Input.add(new JLabel());
		showbox = new JCheckBox("Show");
		Input.add(showbox);
		Input.add(new JLabel("Delay (ms)",JLabel.RIGHT));
		delay = new JSlider (JSlider.HORIZONTAL, 0, 5000, 100);
		Input.add(delay);
		Input.add(new JLabel());
		delayCount = new JLabel(Integer.toString(delay.getValue()));
		Input.add(delayCount);
		
		getContentPane ().add("Center", Input);
		
		JButton ok, ko;
		
		Buttons = new JPanel();
		Buttons.setLayout(new FlowLayout());
		Buttons.add((ok = new JButton("Ok")));
		Buttons.add((ko = new JButton("Cancel")));
		getContentPane ().add("South",Buttons);
		this.pack();
		this.setSize(250,180);
		setLocation (parent.getLocation().x + 50, parent.getLocation().y + 50);
		this.setResizable(false);
		
		delay.addChangeListener(new ChangeListener() {
			public void  stateChanged (ChangeEvent e) {
				delayStateChanged(e);
			}
		});		
		ok.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				okActionPerformed(e);
			}
		});		
		ko.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				koActionPerformed(e);
			}
		});		
	}
	
	protected void okActionPerformed (java.awt.event.ActionEvent event)
	{
		((PNEditor)getParent()).setDelay(delay.getValue());
		((PNEditor)getParent()).setDemo(showbox.isSelected());
		this.setVisible(false);
	}
	
	protected void koActionPerformed (java.awt.event.ActionEvent event)
	{
		this.setVisible(false);
	}
	
	protected void delayStateChanged (ChangeEvent event)
	{
		delayCount.setText(Integer.toString(delay.getValue()));
	}
	
	public void setDelay(int d) {
		delay.setValue(d);
		delayCount.setText(Integer.toString(delay.getValue()));
	}
	
	public void setDemo(boolean d) {
		showbox.setSelected(d);
	}	
} /* class TransitionDialog */
