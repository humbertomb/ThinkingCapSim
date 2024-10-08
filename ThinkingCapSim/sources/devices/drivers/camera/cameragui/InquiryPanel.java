/*
 * InquiryPanel.java
 *
 * Created on 1 de febrero de 2002, 16:41
 */

/**
 *
 * @author  diego
 */

package devices.drivers.camera.cameragui;

import devices.drivers.camera.*;

public class InquiryPanel extends javax.swing.JPanel {

    private Camera port;
    
    /** Creates new form InquiryPanel */
    public InquiryPanel(Camera port) {
        this.port = port;
        initComponents();
        AddItemsComboBoxs();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jComboBox1 = new javax.swing.JComboBox();
        jTextField1 = new javax.swing.JTextField();
        jTextField1.
//        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        setLayout(new java.awt.FlowLayout());
        
        setBorder(new javax.swing.border.TitledBorder("Control Inquiry"));
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });
        
		add(jComboBox1);
		add(jTextField1);
        
    }//GEN-END:initComponents

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged
        // Add your handling code here:
      Comando comando = (Comando) jComboBox1.getSelectedItem();
      port.send(comando);
      jTextField1.setText("");
      //jTextField1.setText(comando.getStatus());
      //System.out.println(comando.getStatus());
      //┴┴Eliminada la implementacion de comandos INQUIRY!! (JP)
    }//GEN-LAST:event_jComboBox1ItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables

    private void AddItemsComboBoxs() {
        jComboBox1.addItem(Comandos.ST0);
        jComboBox1.addItem(Comandos.ST1);
        jComboBox1.addItem(Comandos.ST3);
        jComboBox1.addItem(Comandos.ST4);
        jComboBox1.addItem(Comandos.ST5);
        jComboBox1.addItem(Comandos.ST6);
        jComboBox1.addItem(Comandos.ST7);
        jComboBox1.addItem(Comandos.ST8);
        jComboBox1.addItem(Comandos.ST9);
        jComboBox1.addItem(Comandos.STA);
        jComboBox1.addItem(Comandos.STB);
        jComboBox1.addItem(Comandos.STI);
    }
    
}
