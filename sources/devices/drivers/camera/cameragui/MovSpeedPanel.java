/*
 *	MovSpeedPanel.java
 *
 * 
 */

package devices.drivers.camera.cameragui;

import devices.drivers.camera.*;

public class MovSpeedPanel extends javax.swing.JPanel {

      private Camera port;
   
    private javax.swing.JPanel hvelPanel;
    private javax.swing.JButton hupBut;
    private javax.swing.JButton hdownBut;
    private javax.swing.JSlider hSlider;
    private javax.swing.JPanel vvelPanel;
    private javax.swing.JButton vupBut;
    private javax.swing.JButton vdownBut;
    private javax.swing.JSlider vSlider;
    
    /** Creates new form ColorPanel */
    public MovSpeedPanel(Camera port) {
        this.port = port;
        initComponents ();
    }

    private void initComponents() 
    {
		hvelPanel = new javax.swing.JPanel();
		vvelPanel = new javax.swing.JPanel();
		hupBut    = new javax.swing.JButton();
        hSlider   = new javax.swing.JSlider();
		hdownBut  = new javax.swing.JButton();
		vupBut    = new javax.swing.JButton();
		vSlider   = new javax.swing.JSlider();
		vdownBut  = new javax.swing.JButton();
		
        setLayout(new javax.swing.BoxLayout(this, 1));
        setBorder(new javax.swing.border.TitledBorder("Control Movement Speed"));
        
		hvelPanel.setLayout(new java.awt.FlowLayout());
        hvelPanel.setBorder(new javax.swing.border.TitledBorder("Horizontal Speed"));
        
        hdownBut.setText("-");
        hdownBut.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  hdownButActionPerformed(evt);
              }
        });
		hSlider.setMinimum(0);
		hSlider.setMaximum(255);
		hSlider.setValue(128);
        hSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                hSliderMouseReleased(evt);
            }
        });
		hupBut.setText("+");
		hupBut.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  hupButActionPerformed(evt);
              }
        });

        hvelPanel.add(hdownBut);
        hvelPanel.add(hSlider);
        hvelPanel.add(hupBut);            
        add(hvelPanel);

		vvelPanel.setLayout(new java.awt.FlowLayout());
        vvelPanel.setBorder(new javax.swing.border.TitledBorder("Vertical Speed"));
        
        vdownBut.setText("-");
        vdownBut.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  hdownButActionPerformed(evt);
              }
        });
		vSlider.setMinimum(0);
		vSlider.setMaximum(255);
		vSlider.setValue(128);
        vSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                vSliderMouseReleased(evt);
            }
        });
		vupBut.setText("+");
		vupBut.addActionListener(new java.awt.event.ActionListener() {
              public void actionPerformed(java.awt.event.ActionEvent evt) {
                  vupButActionPerformed(evt);
              }
        });

        vvelPanel.add(vdownBut);
        vvelPanel.add(vSlider);
        vvelPanel.add(vupBut);            
        add(vvelPanel);
    }

  private void hupButActionPerformed(java.awt.event.ActionEvent evt) {
	  hSlider.setValue(hSlider.getValue()+1);
	  port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }

  private void hdownButActionPerformed(java.awt.event.ActionEvent evt) {
	  hSlider.setValue(hSlider.getValue()-1);
	  port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }

  private void vupButActionPerformed(java.awt.event.ActionEvent evt) {
	  vSlider.setValue(vSlider.getValue()+1);
	  port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }

  private void vdownButActionPerformed(java.awt.event.ActionEvent evt) {
	  vSlider.setValue(vSlider.getValue()-1);
	  port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }

  private void vSliderMouseReleased(java.awt.event.MouseEvent evt) {
      port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }

  private void hSliderMouseReleased(java.awt.event.MouseEvent evt) {
      port.send(Comandos.MSP,hSlider.getValue(),vSlider.getValue());
  }
}
