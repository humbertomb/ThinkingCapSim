/**
 * VModePanel.java
 *
 * Panel to set the visual mode in a camera
 */

package devices.drivers.camera.cameragui;

import devices.drivers.camera.*;

public class VModePanel extends javax.swing.JPanel {

    private Camera port;
    private javax.swing.JComboBox modeCombo;
    
    /** Creates new form VModePanel */
    public VModePanel(Camera port) {
        this.port = port;
        initComponents ();
        AddItemsComboBoxs();
    }

    private void initComponents() 
    {
		modeCombo = new javax.swing.JComboBox();
        setLayout(new java.awt.FlowLayout());
        setBorder(new javax.swing.border.TitledBorder("Control Visual Mode"));
        
        modeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modeComboActionPerformed(evt);
            }
        }
        );
        
        add(modeCombo);
      
    }//GEN-END:initComponents

  private void modeComboActionPerformed(java.awt.event.ActionEvent evt) 
  {
      Comando comando = (Comando) modeCombo.getSelectedItem();
      port.send(comando);
  }


    private void AddItemsComboBoxs() {
		if (port.isAvaiable(Comandos.VMD))
			modeCombo.addItem(Comandos.VMD);
		if (port.isAvaiable(Comandos.VMN))
			modeCombo.addItem(Comandos.VMN);
    }
}
