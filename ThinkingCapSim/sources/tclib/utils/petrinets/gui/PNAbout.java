package tclib.utils.petrinets.gui;

import java.awt.*;
import javax.swing.*;

public class PNAbout extends JFrame {

    JButton close;

    public PNAbout(Frame par) {
        super("About");
        
        getContentPane ().setLayout(new GridLayout(20,1));
        Font f = new Font("Helvetica", Font.PLAIN, 12);
        Font fb = new Font("Helvetica", Font.BOLD, 12);
        JLabel a = new JLabel("PNS - The Petri Net Simulator", JLabel.CENTER);
        JLabel b = new JLabel("*** Java - Version ***", JLabel.CENTER);
        a.setFont(fb);
        b.setFont(fb);
        getContentPane ().add(a);
        getContentPane ().add(b);
        getContentPane ().add(new JLabel(""));
        JLabel c = new JLabel("Created by", JLabel.CENTER);
        c.setFont(fb);
        getContentPane ().add(c);
        this.setFont(f);
        getContentPane ().add(new JLabel("Achim Haessler", JLabel.CENTER));
        getContentPane ().add(new JLabel("email: amhaessl@tick.informatik.uni-stuttgart.de", JLabel.CENTER));
        getContentPane ().add(new JLabel("Martin Kada", JLabel.CENTER));
        getContentPane ().add(new JLabel("email: mnkada@tick.informatik.uni-stuttgart.de", JLabel.CENTER));
        getContentPane ().add(new JLabel("Joerg Walz", JLabel.CENTER));
        getContentPane ().add(new JLabel("email: jgwalz@tick.informatik.uni-stuttgart.de", JLabel.CENTER));
        getContentPane ().add(new JLabel(""));
        JLabel e = new JLabel("Supervisor", JLabel.CENTER);
        e.setFont(fb);
        getContentPane ().add(e);
        getContentPane ().add(new JLabel("Thomas Braeunl", JLabel.CENTER));
        getContentPane ().add(new JLabel("WEB: http://www.informatik.uni-stuttgart.de/ipvr/bv/braunl", JLabel.CENTER));
        getContentPane ().add(new JLabel("email: braunl@informatik.uni-stuttgart.de", JLabel.CENTER));
        getContentPane ().add(new JLabel(""));
        JLabel d = new JLabel("Thanks for playing around!", JLabel.CENTER);
        d.setFont(fb);
        getContentPane ().add(d);
        getContentPane ().add(new JLabel());
        getContentPane ().add(new JLabel());
        close = new JButton("Close");
        getContentPane ().add(close);
        
        setResizable(false);
        setSize (400, 600);
        setLocation (par.getLocation().x + 50, par.getLocation().y + 50);
        setVisible (true);
        
		close.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				closeActionPerformed(e);
			}
		});		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});        
    }

	// Close the window when the close box is clicked
	public void closeActionPerformed(java.awt.event.ActionEvent e) 
	{
		setVisible (false);
		dispose ();
	}

	protected void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		setVisible (false);
		dispose ();
	}
}
