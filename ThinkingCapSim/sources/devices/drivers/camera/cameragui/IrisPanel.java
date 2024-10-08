/*
 * IrisPanel.java
 *
 * Created on 31 de enero de 2002, 18:57
 */


/**
 *
 * @author  Administrador
 * @version
 */

package devices.drivers.camera.cameragui;

import devices.drivers.camera.*;

public class IrisPanel extends javax.swing.JPanel {
    
    private Camera port;
    
    /** Creates new form IrisPanel */
    public IrisPanel(Camera port) {
        this.port = port;
        initComponents ();
        AddItemsComboBoxs();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jSlider1 = new javax.swing.JSlider();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox();
//        setLayout(null);
jPanel1.setLayout(new java.awt.FlowLayout());
//		setLayout(new java.awt.FlowLayout());
		setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));
        setBorder(new javax.swing.border.TitledBorder("Control Iris"));
        
        jSlider1.setMinorTickSpacing(1);
        jSlider1.setMinimum(-30);
        jSlider1.setMajorTickSpacing(5);
        jSlider1.setMaximum(30);
        jSlider1.setValue(0);
        jSlider1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSlider1MouseReleased(evt);
            }
        }
        );
        
  //      add(jSlider1);
//        jSlider1.setLocation(232, 35);
//        jSlider1.setSize(jSlider1.getPreferredSize());
        
        
    //    jButton1.setFont(new java.awt.Font ("Dialog", 1, 24));
        jButton1.setText("-");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        }
        );
        
//        add(jButton1);
  //      jButton1.setLocation(177, 17);
//        jButton1.setSize(jButton1.getPreferredSize());
        
        
  //      jButton2.setFont(new java.awt.Font ("Dialog", 1, 24));
        jButton2.setText("+");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        }
        );
        
//        add(jButton2);
//        jButton2.setLocation(436, 19);
  //      jButton2.setSize(jButton2.getPreferredSize());
        
        
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        }
        );
        
        add(jComboBox1);
    //    jComboBox1.setLocation(11, 33);
      //  jComboBox1.setSize(jComboBox1.getPreferredSize());
      	jPanel1.add(jButton1);
      	jPanel1.add(jSlider1);
      	jPanel1.add(jButton2);
      	add(jPanel1);
        
    }//GEN-END:initComponents
    
  private void jSlider1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSlider1MouseReleased
      // Add your handling code here:
      port.send(Comandos.IRM,jSlider1.getValue());
  }//GEN-LAST:event_jSlider1MouseReleased
  
  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
      port.send(Comandos.IRU);
      jSlider1.setValue(jSlider1.getValue()+1);
  }//GEN-LAST:event_jButton2ActionPerformed
  
  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
      // Add your handling code here:
      port.send(Comandos.IRD);
      jSlider1.setValue(jSlider1.getValue()-1);
  }//GEN-LAST:event_jButton1ActionPerformed
  
  private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
      Comando comando = (Comando) jComboBox1.getSelectedItem();
      port.send(comando);
      if (comando.equals(Comandos.IRM)) {
          jSlider1.setEnabled(true);
          jButton1.setEnabled(true);
          jButton2.setEnabled(true);
      }
      else {
          jSlider1.setEnabled(false);
          jButton1.setEnabled(false);
          jButton2.setEnabled(false);
      }
  }//GEN-LAST:event_jComboBox1ActionPerformed
  
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JSlider jSlider1;
  private javax.swing.JButton jButton1;
  private javax.swing.JButton jButton2;
  private javax.swing.JComboBox jComboBox1;
  // End of variables declaration//GEN-END:variables
    
    
    private void AddItemsComboBoxs() {
        jComboBox1.addItem(Comandos.IRA);
        jComboBox1.addItem(Comandos.IRM);
    }
}
