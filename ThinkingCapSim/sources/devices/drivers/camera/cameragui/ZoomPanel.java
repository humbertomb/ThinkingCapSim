/*
 * ZoomPanel.java
 *
 * Created on 31 de enero de 2002, 11:43
 */


/**
 *
 * @author  Administrador
 * @version
 */
 
package devices.drivers.camera.cameragui;

import devices.drivers.camera.*;

public class ZoomPanel extends javax.swing.JPanel {
    
    private Camera port;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton zout;
    private javax.swing.JButton zin;
    private javax.swing.JComboBox velCombo;
    private javax.swing.JCheckBox digiZoom;
	private javax.swing.JPanel upPanel;
	private javax.swing.JPanel downPanel;
    // End of variables declaration//GEN-END:variables
    
    /** Creates new form ZoomPanel */
    public ZoomPanel (Camera port) {
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
        zout = new javax.swing.JButton();
        zin = new javax.swing.JButton();
        velCombo = new javax.swing.JComboBox();
		digiZoom = new javax.swing.JCheckBox();
		upPanel = new javax.swing.JPanel();
		downPanel = new javax.swing.JPanel();

        setLayout(new javax.swing.BoxLayout(this,javax.swing.BoxLayout.Y_AXIS));
        upPanel.setLayout(new java.awt.FlowLayout());
        setBorder(new javax.swing.border.TitledBorder("Control Zoom"));
        
        zout.setText("-");
        zout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                zoutMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                zoutMouseReleased(evt);
            }
        }
        );
        
        zin.setText("+");
        zin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                zinMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                zinMouseReleased(evt);
            }
        }
        );
        
        velCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                velComboActionPerformed(evt);
            }
        }
        );
        
        digiZoom.setText("Enable digital zoom");
        digiZoom.setSelected(false);
		digiZoom.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				digiZoomActionPerformed(evt);
			}
		});
		
		if (port.isAvaiable(Comandos.ZMT))
        	upPanel.add(zout);        
		if (port.isAvaiable(Comandos.ZSS)||
			port.isAvaiable(Comandos.ZSH)||
			port.isAvaiable(Comandos.ZSM)||
			port.isAvaiable(Comandos.ZSL))
	        upPanel.add(velCombo);
		if (port.isAvaiable(Comandos.ZMW))
        	upPanel.add(zin);
		add(upPanel);
		if (port.isAvaiable(Comandos.ZDO))
		{
			downPanel.add (digiZoom);
			add(downPanel);
		}
    }//GEN-END:initComponents
    
  private void zinMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseReleased
      // Add your handling code here:
      port.send(Comandos.ZMS);
  }//GEN-LAST:event_jButton2MouseReleased
  
  private void zinMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MousePressed
      // Add your handling code here:
      port.send(Comandos.ZMT);
  }//GEN-LAST:event_jButton2MousePressed
  
  private void velComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
      // Add your handling code here:
      Comando comando = (Comando) velCombo.getSelectedItem();
      port.send(comando);
  }//GEN-LAST:event_jComboBox1ActionPerformed
  
  private void zoutMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseReleased
      // Add your handling code here:
      port.send(Comandos.ZMS);
  }//GEN-LAST:event_jButton1MouseReleased
  
  private void zoutMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MousePressed
      // Add your handling code here:
      port.send(Comandos.ZMW);
  }//GEN-LAST:event_jButton1MousePressed
  
  private void digiZoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
      // Add your handling code here:
      if (digiZoom.isSelected())
      	port.send(Comandos.ZDO);
      else
      	port.send(Comandos.ZDF);
  }

  private void AddItemsComboBoxs() {
        velCombo.addItem(Comandos.ZSS);
        velCombo.addItem(Comandos.ZSH);
        velCombo.addItem(Comandos.ZSM);
        velCombo.addItem(Comandos.ZSL);
    }
}