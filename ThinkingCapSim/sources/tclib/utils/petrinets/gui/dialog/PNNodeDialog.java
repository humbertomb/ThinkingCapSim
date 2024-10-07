package tclib.utils.petrinets.gui.dialog;

import java.awt.*;
import javax.swing.*;

import tclib.utils.petrinets.*;

public class PNNodeDialog extends JDialog {
	
	JPanel Input, Buttons;
	PNNode actual;
	JTextField NameField, TokenField, capacityField;
	
	public PNNodeDialog(JFrame parent, PNNode n) {
		super(parent, "Edit Nodes", true);
		
		getContentPane ().setLayout(new BorderLayout());
		
		Input = new JPanel();
		Input.setLayout(new GridLayout(0,2,2,2));
		actual = n;
		
		getContentPane ().add("North", new JLabel("<HTML><B>Node</B></HTML>"));

		NameField = new JTextField("", 1);
		if (actual.isNamed ())
			NameField.setText(actual.getName());

		TokenField = new JTextField(Integer.toString(actual.getTokens()), 1);
		capacityField = new JTextField(1);
		Input.add(new JLabel("Name",JLabel.RIGHT));
		Input.add(NameField);
		Input.add(new JLabel("Token",JLabel.RIGHT));
		Input.add(TokenField);
		Input.add(new JLabel("Capacity",JLabel.RIGHT));
		Input.add(capacityField);
		getContentPane ().add("Center", Input);
		
		JButton ok, ko;
		
		Buttons = new JPanel();
		Buttons.setLayout(new FlowLayout());
		Buttons.add((ok = new JButton("Ok")));
		Buttons.add((ko = new JButton("Cancel")));
		getContentPane ().add("South",Buttons);
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
			actual.setTokens(Integer.parseInt(TokenField.getText()));
			if (actual.getTokens() < 0)
				actual.setTokens(0);
		}
		catch (NumberFormatException n) {}

		if (capacityField.getText ().length () > 0)
		try {
			actual.setCapacity(Integer.parseInt(capacityField.getText()));
			if (actual.getCapacity() < 0)
				actual.setCapacity(0);
		}
		catch (NumberFormatException n) {}
		this.setVisible(false);
	}
	
	protected void koActionPerformed (java.awt.event.ActionEvent event)
	{
		this.setVisible(false);
	}
	
	public void setNode(PNNode n) {
		actual = n;
		if (actual.isNamed ())	
			NameField.setText(actual.getName());
		else
			NameField.setText("");

		if (actual.isLimited ())	
			capacityField.setText(Integer.toString (actual.getCapacity()));
		else
			capacityField.setText("");

		TokenField.setText(Integer.toString(actual.getTokens()));
	}
	
	
} /* class PlaceDialog */
