package tclib.utils.petrinets.gui.dialog;

import java.awt.*;
import javax.swing.*;

import tclib.utils.petrinets.*;

public class PNTransitionDialog extends JDialog {
	
	JPanel Input, Buttons;
	PNTransition actual;
	JTextField NameField, PriorityField;
	JComboBox Orientation;
	JCheckBox Stop;
	
	public PNTransitionDialog(JFrame parent, PNTransition t) {
		super(parent, "Edit Transitions", false);
		
		getContentPane ().setLayout(new BorderLayout());
		
		Input = new JPanel();
		Input.setLayout(new GridLayout(0,2,2,2));
		actual = t;
		
		Orientation = new JComboBox();
		Orientation.addItem("Horizontal");
		Orientation.addItem("Vertical");
		Orientation.addItem("Square");
		Orientation.addItem("Diagonal 1");
		Orientation.addItem("Diagonal 2");
		getContentPane ().add("North", new JLabel("<HTML><B>Transition</B></HTML>"));
		
		NameField = new JTextField("", 1);
		if (actual.isNamed ())
			NameField.setText(actual.getName());

		PriorityField = new JTextField(Integer.toString(actual.getPriority()), 1);
		getOrientation();
		Stop = new JCheckBox("Stop after fire");
		Input.add(new JLabel("Name",JLabel.RIGHT));
		Input.add(NameField);
		Input.add(new JLabel("Priority",JLabel.RIGHT));
		Input.add(PriorityField);
		Input.add(new JLabel(""));
		Input.add(Stop);
		Input.add(new JLabel("Orientation",JLabel.RIGHT));
		Input.add(Orientation);
		
		getContentPane ().add("Center", Input);
		
		JButton ok, ko;
		
		Buttons = new JPanel();
		Buttons.setLayout(new FlowLayout());
		Buttons.add((ok = new JButton("Ok")));
		Buttons.add((ko = new JButton("Cancel")));
		getContentPane ().add("South",Buttons);
		Stop.setEnabled(false);
		this.pack();
		this.setSize(250,180);
		setLocation(parent.getLocation().x + 50, parent.getLocation().y + 50);
		this.setResizable(false);
		
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
		if (NameField.getText().length() > 0)
			actual.setName(NameField.getText());
		try {
			actual.setPriority(Integer.parseInt(PriorityField.getText()));
		}
		catch (NumberFormatException n) {}
		setOrientation();
		this.setVisible(false);
	}
	
	protected void koActionPerformed (java.awt.event.ActionEvent event)
	{
		this.setVisible(false);
	}
	
	void getOrientation() {
		switch (actual.getOrientation()) {
		case PNTransition.ORIENTATION_VERTICAL :
			Orientation.setSelectedItem ("Vertical");
		break;
		case PNTransition.ORIENTATION_HORIZONTAL :
			Orientation.setSelectedItem("Horizontal");
		break;
		case PNTransition.ORIENTATION_ALL :
			Orientation.setSelectedItem("Square");
		break;
		case PNTransition.ORIENTATION_DIAGONAL1 :
			Orientation.setSelectedItem("Diagonal 1");
		break;
		case PNTransition.ORIENTATION_DIAGONAL2 :
			Orientation.setSelectedItem("Diagonal 2");
		break;
		}
	}
	
	void setOrientation() {
		if (Orientation.getSelectedItem().equals("Vertical"))
			actual.setOrientation(PNTransition.ORIENTATION_VERTICAL);
		if (Orientation.getSelectedItem().equals("Horizontal"))
			actual.setOrientation(PNTransition.ORIENTATION_HORIZONTAL);
		if (Orientation.getSelectedItem().equals("Square"))
			actual.setOrientation(PNTransition.ORIENTATION_ALL);
		if (Orientation.getSelectedItem().equals("Diagonal 1"))
			actual.setOrientation(PNTransition.ORIENTATION_DIAGONAL1);
		if (Orientation.getSelectedItem().equals("Diagonal 2"))
			actual.setOrientation(PNTransition.ORIENTATION_DIAGONAL2);
	}
	
	public void setTransition(PNTransition t) {
		actual = t;
		if (actual.isNamed ())
			NameField.setText(actual.getName());
		else
			NameField.setText("");
		PriorityField.setText(Integer.toString(actual.getPriority()));
		getOrientation();
	}
	
} /* class TransitionDialog */
