package tclib.utils.petrinets.gui.dialog;

import java.awt.*;
import javax.swing.*;

import tclib.utils.petrinets.*;

public class PNEdgeDialog extends JDialog {

   JPanel Input, Buttons;
   PNEdge actual;
   JTextField WeightField;
   JCheckBox Negated;
   int WeightBackup;

   public PNEdgeDialog(JFrame parent, PNEdge e) {
      super(parent, "Edit Edges", false);

      getContentPane ().setLayout(new BorderLayout());

      Input = new JPanel();
      Input.setLayout(new GridLayout(0,2,2,2));
      actual = e;

      getContentPane ().add("North", new JLabel("<HTML><B>Edge</B></HTML>"));
      WeightBackup = actual.getWeight();
      WeightField = new JTextField(Integer.toString(WeightBackup), 1);
      Negated = new JCheckBox("Negated");
      Negated.setSelected(actual.isNegated());
      Input.add(new JLabel(""));
      Input.add(new JLabel(""));
      Input.add(new JLabel("Weight",JLabel.RIGHT));
      Input.add(WeightField);
      Input.add(new JLabel(""));
      Input.add(Negated);

      getContentPane ().add("Center", Input);

      JButton ok, ko;
      
      ok	= new JButton("Ok");
      ko = new JButton("Cancel");
      Buttons = new JPanel();
      Buttons.setLayout(new FlowLayout());
      Buttons.add(ok);
      Buttons.add(ko);
      getContentPane ().add("South",Buttons);
      this.pack();
      this.setSize (250,180);
      testModifyable();
      setLocation (parent.getLocation().x + 50, parent.getLocation().y + 50);
      this.setResizable(false);

  	Negated.addActionListener(new java.awt.event.ActionListener() {
		public void  actionPerformed(java.awt.event.ActionEvent e) {
			negatedActionPerformed(e);
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
        try {
            actual.setWeight(Integer.parseInt(WeightField.getText()));
        }
        catch (NumberFormatException n) {}
        actual.setNegated(Negated.isSelected());
        this.setVisible(false);
	}
	
	protected void koActionPerformed (java.awt.event.ActionEvent event)
	{
        this.setVisible(false);
   	}
	
	protected void negatedActionPerformed (java.awt.event.ActionEvent event)
	{
        if (! Negated.isSelected()) {
            WeightField.setText(Integer.toString(WeightBackup));
            WeightField.setEnabled(true);
        } else {
            WeightBackup = Integer.parseInt(WeightField.getText());
            WeightField.setText(Integer.toString(1));
            WeightField.setEnabled(false);
        }
   	}
	
   void testModifyable() {
      if (actual.isNegated()) {
          WeightField.setText(Integer.toString(1));
          WeightField.setEnabled(false);
      } else {
          WeightField.setEnabled(true);
      }
   }

   public void setEdge(PNEdge e) {
      actual = e;
      WeightBackup = actual.getWeight();
      WeightField.setText(Integer.toString(WeightBackup));
      Negated.setSelected(actual.isNegated());
      testModifyable();
   }

} /* class EdgeDialog */
