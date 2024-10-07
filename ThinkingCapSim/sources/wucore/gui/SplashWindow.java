/*
 * This file is part of the package <code>com.zerjio.windows</code>.
 *
 * Copyright (C) 2003  Sergio Alonso Burgos (Zerjillo, http://zerjio.com, zerjiopropaganda@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package wucore.gui;

import javax.swing.*;
import java.awt.*;

/**
 * This is a Splash Window with some transparency capabilities and progress bar
 * that can be used during the load of an application.
 * <p>
 * It allows a image with transparency (example: png or gif) to be used as a
 * background. It also has an optional progress bar and an optional progress
 * message. Additionally more <code>Component</code>s can be added to it to
 * extend its functionality.
 * <p>
 * <b>Note:</b> This class needs the <code>com.zerjio.BackgroundPanel</code>
 * that should come distributed along with this file.
 *
 * @author  Sergio Alonso Burgos, (Zerjillo, http://zerjio.com, zerjiopropaganda@hotmail.com)
 * @version 1.0
 */
public class SplashWindow extends JWindow 
{
	/**
	 * Delay between increments of the progress bar.
	 *
	 * @see   #setDelay(int delay)  setDelay(int delay)
	 * @see   #getDelay()           getDelay()
	 */
	private int delay = 0;
	
	private JLayeredPane jLayeredPane;
	private BackgroundPanel backgroundPanel;
	private JLabel imageLabel;
	
	/**
	 * The progress bar.
	 */
	private JProgressBar progressBar;
	/**
	 * The progress text.
	 */
	private JLabel text;
	
	/**
	 * Constructor of the splash window.
	 * <p>
	 * The size of the splash window will be set as the background image size.
	 * It will be centered in the middle of the screen.
	 *
	 * @param   image         The path of the image that should be used as a
	 *                        background.
	 * @param   transparent   must be set to <code>true</code> if the background
	 *                        image uses transparency. If not can be set to
	 *                        <code>false</code> (which should be faster).
	 */
	public SplashWindow(String image, boolean transparent)
	{
		ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource (image)); 
		int iconWidth = icon.getIconWidth();
		int iconHeight = icon.getIconHeight();
		
		// Setting bounds
		setSize (new Dimension (iconWidth, iconHeight));
		setLocationRelativeTo (null);
		setAlwaysOnTop (true);
		
		// Creating main components.
		jLayeredPane = new JLayeredPane();
		
		// If uses transparency we need a background panel
		if (transparent) {
			backgroundPanel = new BackgroundPanel((int)getLocation().getX(), (int)getLocation().getY(), (int)getSize().getWidth(), (int)getSize().getHeight());
			backgroundPanel.setPreferredSize(new Dimension(310, 164));
			backgroundPanel.setBounds(0, 0, getWidth(), getHeight());
			jLayeredPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
		}
		
		// Preparing the background image
		imageLabel = new JLabel();
		imageLabel.setIcon(icon);
		imageLabel.setBounds(0, 0, iconWidth, iconHeight);
		jLayeredPane.add(imageLabel, JLayeredPane.PALETTE_LAYER);
		
		// Preparing the progress bar
		progressBar = new JProgressBar();
		progressBar.setOpaque(false);
		progressBar.setBorderPainted(false);
		progressBar.setVisible(false);
		jLayeredPane.add(progressBar, JLayeredPane.MODAL_LAYER);
		
		// Preparing the progress Text.
		text = new JLabel();
		text.setVisible(false);
		jLayeredPane.add(text, JLayeredPane.MODAL_LAYER);
		
		// Finishing init
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(jLayeredPane, BorderLayout.CENTER);
	}
	
	/**
	 * Adds a <code>Component</code> to the splash window. Since the splash window
	 * has no Layout the bounds of the component must be set. The component should
	 * also be visible.
	 *
	 * @param   comp    The component to be added into the splash window.
	 * @return          The component that has been added (that is, the comp
	 *                  parameter)
	 * @see     #remove(Component comp)    remove()
	 */
	public Component add(Component comp) {
		jLayeredPane.add(comp, JLayeredPane.MODAL_LAYER);
		
		return comp;
	}
	
	/**
	 * Removes a <code>Component</code> that has been previously added into the
	 * splash window.
	 *
	 * @param   comp    The component to be removed from the splash window
	 * @see     #add(Component comp)    add()
	 */
	public void remove(Component comp) {
		jLayeredPane.remove(comp);
	}
	
	/**
	 * Sets the maximum of the progress bar. Usually this maximum should be the
	 * number of logical steps in the initialization process.
	 *
	 * @param   max   The maximum of the progress bar.
	 */
	public void setProgressBarMaximum(int max) {
		progressBar.setMaximum(max);
	}
	
	/**
	 * Sets the value of the progress bar.
	 * <p>
	 * Usually should be used <code>incrementProgressBarValue()</code>.
	 *
	 * @param   val   The new value for the progress bar.
	 * @see #incrementProgressBarValue(int increment) incrementProgressBarValue()
	 */
	public void setProgressBarValue(int val) {
		progressBar.setValue(val);
	}
	
	/**
	 * Increments the value of the progress bar in a certain amount.
	 * <p>
	 * The value is incremented using: <actual value> += <increment>
	 *
	 * @param   increment   The increment for the progress bar.
	 * @see     #setProgressBarValue(int val)   setProgressBarValue()
	 */
	public void incrementProgressBarValue(int increment) {
		progressBar.setValue(progressBar.getValue() + increment);
		
		try {
			Thread.sleep(delay);
		} catch(InterruptedException e) {}
	}
	
	/**
	 * Sets the progress bar bounds. <code>x</code> and <code>y</code> coordinates
	 * are relative to the upper left corner of the splash window.
	 *
	 * @param   x       Horizontal position in pixels of the progress bar.
	 * @param   y       Vertical position in pixels of the progress bar.
	 * @param   width   Width in pixels of the progress bar.
	 * @param   height  Height in pixels of the progress bar.
	 */
	public void setProgressBarBounds(int x, int y, int width, int height) {
		progressBar.setBounds(x, y, width, height);
	}
	
	/**
	 * Sets the color of the progress bar.
	 *
	 * @param   color   The new color for the progress bar.
	 */
	public void setProgressBarColor(Color color) {
		progressBar.setForeground(color);
	}
	
	/**
	 * Sets the progress bar visibility.
	 *
	 * @param   vis   if <code>true</code> the progress bar will be visible, 
	 *                otherwise it will be insvisible.
	 */
	public void setProgressBarVisible(boolean vis) {
		progressBar.setVisible(vis);
	}
	
	/**
	 * Gets the progress bar of the splash window. It is useful to implement
	 * aditional aspects for it.
	 *
	 * @return    The progress bar.
	 */
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	
	/**
	 * Sets the progress text bounds. <code>x</code> and <code>y</code> coordinates
	 * are relative to the upper left corner of the splash window.
	 *
	 * @param   x       Horizontal position in pixels of the progress text.
	 * @param   y       Vertical position in pixels of the progress text.
	 * @param   width   Width in pixels of the progress text.
	 * @param   height  Height in pixels of the progress text.
	 */  
	public void setProgressTextBounds(int x, int y, int width, int height) {
		text.setBounds(x, y, width, height);
	}
	
	/**
	 * Sets the font of the progress text.
	 *
	 * @param   font    The new font for the progress text.
	 */
	public void setProgressTextFont(Font font) {
		text.setFont(font);
	}
	
	/**
	 * Sets the color of the progress text.
	 *
	 * @param   color   The new color for the progress text.
	 */  
	public void setProgressTextColor(Color color) {
		text.setForeground(color);
	}
	
	/**
	 * Sets the progress text visibility.
	 *
	 * @param   vis   if <code>true</code> the progress text will be visible, 
	 *                otherwise it will be insvisible.
	 */
	public void setProgressTextVisible(boolean vis) {
		text.setVisible(vis);
	}
	
	/**
	 * Gets the progress text of the splash window. It is useful to implement
	 * aditional aspects for it.
	 *
	 * @return    The progress text.
	 */
	public JLabel getProgressTextLabel() {
		return text;
	}
	
	/**
	 * Sets the progress text.
	 *
	 * @param   text    The progress text that will be visualized.
	 */  
	public void setProgressText(String text) {
		this.text.setText(text);
	}
	
	/**
	 * Updates the background to try to avoid "transparency problems".
	 * <p>
	 * Since this is an easy emulation of a transparent window, if the loading
	 * process takes too long the changes in the background will not be reflected
	 * in the window. This method updates the background graphics to try to avoid 
	 * that ugly effect.
	 * <p>
	 * This method can take a little bit long, so its use must be moderated.
	 * <p>
	 * Please note that if the splash window is visible the new capture will get
	 * it and so the background will not be changed.
	 */
	public void updateBackground() {
		if (backgroundPanel != null) {
			backgroundPanel.capture();
		}
	}
	
	/**
	 * Gets the delay between increments of the progress bar in miliseconds.
	 *
	 * @return    The delay between increments of the progress bar
	 */ 
	public int getDelay() {
		return delay;
	}
	
	/**
	 * Sets the delay between increments of the progress bar in miliseconds.
	 * 
	 * @param   delay   The delay between increments of the prgoress bar.
	 */  
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	/**
	 * Hides and frees the resources used by the splash window.
	 */
	public void close()
	{
		setVisible(false);  
		dispose();
	}
}


