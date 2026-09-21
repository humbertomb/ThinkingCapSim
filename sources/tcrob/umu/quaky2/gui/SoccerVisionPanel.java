/**
 * Created on 09-dec-2018
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;

import tcrob.umu.quaky2.*;
import tclib.vision.chaos.segment.*;
import tcrob.umu.quaky2.SoccerVisionConfig;
import tcrob.umu.quaky2.gui.ctables.CPColorTable;
import tcrob.umu.quaky2.gui.images.CPImageCanvas;
import tcrob.umu.quaky2.gui.images.CPZoomCanvas;
import tcrob.umu.quaky2.gui.images.ImageFileFilter;
import tclib.vision.chaos.channels.*;

public class SoccerVisionPanel extends JPanel
{	
	static public final String			IMAGEFORMAT 			= ImageFileFilter.PNG;
		
	public enum ChaosImageFormat		{ RAW, SEG, BLOB, RECOG};
	static protected final String[]		MODES	= { "RAW", "SEGMENT.", "BLOBS", "RECOG." };

	protected SoccerVision				pam;
	protected ChaosVisionMotionMouse	mouse;

	protected CPColorTable				cpcolortable;
	protected CPZoomCanvas				cpzoom;
	protected CPImageCanvas				cpimage;
	protected JPanel					cprecogcfg;
	protected JFileChooser				chooser;

	// Channel monitor and zoom
	protected JLabel					lbx;
	protected JLabel					lby;
	protected JLabel					lbr;
	protected JLabel					lbg;
	protected JLabel					lbb;
	protected JLabel					lbh;
	protected JLabel					lbs;
	protected JLabel					lbv;
	protected JComboBox<String> 		modeCB = new JComboBox<String> (MODES);

	// Algorithms and methods
	private JComboBox<String>			lutmodeCB = new JComboBox<String> (SoccerVisionConfig.LUTMODES);
	private JComboBox<String>			segmodeCB = new JComboBox<String> (SoccerVisionConfig.SEGMODES);
	private JComboBox<String>			blobmodeCB = new JComboBox<String> (SoccerVisionConfig.BLOBMODES);
	private JButton						btlutcon;
	private JButton						btsegcon;
	private JButton						btblobcon;

	// Image management
	protected BufferedImage				imagein;
	protected BufferedImage				imageout;
	protected String					filename;

	private ChaosImageFormat 			imgmode = ChaosImageFormat.RECOG;

	// Constructors
	public SoccerVisionPanel (JFrame frame, SoccerVision pam)
	{
		this.pam = pam;
		
		chooser = new JFileChooser ();
		chooser.setCurrentDirectory (new File (System.getProperty ("user.dir")+"./tools/calibration/images"));
		chooser.setMultiSelectionEnabled (false);
		chooser.setFileSelectionMode (JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed (true);

		mouse = new ChaosVisionMotionMouse ();

		cpzoom 			= new CPZoomCanvas ();
		cpimage			= new CPImageCanvas ();
		cprecogcfg		= new SoccerRecognitonConfigPanel (pam.recognizer);
		cpcolortable	= new CPColorTable (this, pam);

		setLayout (new BorderLayout ());
		setVisible (false);
		add (createMainPanel (), BorderLayout.CENTER);

//		if (frame instanceof MouseRegisterer)		((MouseRegisterer) frame).register (mouse);

		setVisible (true);
		
		lutmodeCB.setSelectedIndex (0);
		segmodeCB.setSelectedIndex (0);
		blobmodeCB.setSelectedIndex (0);
	}

	protected JPanel createMainPanel ()
	{
		JPanel			panel, monitor, options;
		JTabbedPane		tabs;
		
		// *************** OPTIONS
		tabs = new JTabbedPane ();
		tabs.addTab ("Color Tables", (JComponent) cpcolortable);
		tabs.addTab ("Recognizer", (JComponent) cprecogcfg);
		tabs.setMinimumSize (new Dimension(300, 400));

		options = new JPanel ();
		options.setLayout (new BorderLayout());
		options.add (tabs, BorderLayout.CENTER);
		options.add (createAlgorithmsPanel (), BorderLayout.SOUTH);

		// *************** MONITOR
		monitor = new JPanel ();
		monitor.setLayout (new BorderLayout());
		monitor.add (createMonitorPanel (), BorderLayout.WEST);
		monitor.add (cpzoom, BorderLayout.CENTER);

		panel = new JPanel ();
		panel.setLayout (new BorderLayout());
		panel.add (cpimage, BorderLayout.NORTH);
		panel.add (options, BorderLayout.CENTER);
		panel.add (monitor, BorderLayout.SOUTH);

		return panel;
	}

	protected JPanel createMonitorPanel ()
	{
		JPanel			panel;
		JPanel			ptpanel, chpanel;
				
		ptpanel = new JPanel();
		ptpanel.setLayout(new BoxLayout(ptpanel, BoxLayout.Y_AXIS));
		ptpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Pointer", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
			
		lbx = new JLabel ("X: ---");
		lby = new JLabel ("Y: ---");
		
		ptpanel.add(lbx);
		ptpanel.add(lby);
		
		chpanel = new JPanel();
		chpanel.setLayout(new BoxLayout(chpanel, BoxLayout.Y_AXIS));
		chpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Channels", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		lbr = new JLabel ("R: --- H: ---");
		lbg = new JLabel ("G: --- S: ---");
		lbb = new JLabel ("B: --- V: ---");

		chpanel.add(lbr);
		chpanel.add(lbg);
		chpanel.add(lbb);

		panel = new JPanel ();
		panel.setLayout (new BorderLayout ());		
		panel.add (ptpanel, BorderLayout.NORTH);
		panel.add (chpanel, BorderLayout.CENTER);
		panel.add (modeCB, BorderLayout.SOUTH);

		modeCB.setSelectedIndex (imgmode.ordinal ());
		modeCB.addActionListener (new ActionListener() {
			public void actionPerformed (ActionEvent e) {
				imgmode = ChaosImageFormat.values ()[modeCB.getSelectedIndex ()];
				redrawBufferedImage ();
			}
		});	

		return panel;
	}

	protected JPanel createAlgorithmsPanel ()
	{
		JPanel panel = new JPanel();
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Algorithms and Methods", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.setLayout(new GridLayout(3, 1));

		btlutcon = new JButton("Conf");
		btlutcon.setEnabled (pam.lut.configurable ());
		btsegcon = new JButton("Conf");
		btsegcon.setEnabled (pam.segment.configurable ());
		btblobcon = new JButton("Conf");
		btblobcon.setEnabled (pam.blobbing.configurable ());
		
		lutmodeCB.setSelectedIndex (pam.vconfig.lutmode);
		segmodeCB.setSelectedIndex (pam.vconfig.segmode);
		blobmodeCB.setSelectedIndex (pam.vconfig.blobmode);
		
		JPanel luts = new JPanel ();
		luts.setLayout (new BorderLayout ());
		luts.add (btlutcon, BorderLayout.WEST);
		luts.add (lutmodeCB, BorderLayout.CENTER);
		
		JPanel segment = new JPanel ();
		segment.setLayout (new BorderLayout ());
		segment.add (btsegcon, BorderLayout.WEST);
		segment.add (segmodeCB, BorderLayout.CENTER);
		
		JPanel blobs = new JPanel ();
		blobs.setLayout (new BorderLayout ());
		blobs.add (btblobcon, BorderLayout.WEST);
		blobs.add (blobmodeCB, BorderLayout.CENTER);
		
		panel.add(luts);
		panel.add(segment);
		panel.add(blobs);

		lutmodeCB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				pam.vconfig.lutmode = lutmodeCB.getSelectedIndex ();
				pam.instanceLUT ();
				
				btlutcon.setEnabled (pam.lut.configurable ());
				redrawBufferedImage ();
			}
		});
		btlutcon.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				pam.lut.configureDialog (pam.vconfig.channels);
				redrawBufferedImage ();
			}
		});	
		segmodeCB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				pam.vconfig.segmode = segmodeCB.getSelectedIndex ();
				pam.instanceSegment ();

				btsegcon.setEnabled (pam.segment.configurable ());
				redrawBufferedImage ();
			}
		});
		btsegcon.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				pam.segment.configureDialog ();
				redrawBufferedImage ();
			}
		});	
		blobmodeCB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				pam.vconfig.blobmode = blobmodeCB.getSelectedIndex ();
				pam.instanceBlob ();
				
				btblobcon.setEnabled (pam.blobbing.configurable ());
				redrawBufferedImage ();
			}
		});
		btblobcon.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) 
			{
				pam.blobbing.configureDialog ();
				redrawBufferedImage ();
			}
		});	

		return panel;
	}

	public void updateBufferedImage (BufferedImage image) 
	{
		if (image == null)					return;

		if (imagein != image)
			imagein = image;

		switch (imgmode) 
		{
		case RAW:	imageout = imagein; break;								// the frame as the camera took it
		case SEG:	imageout = pam.segment.getSegmentedImage (); break;
		case BLOB:	imageout = pam.blobbing.getBlobbedImage (); break;
		case RECOG:	imageout = (pam.recognized != null) ? pam.recognized : imagein; break;
		}

		cpzoom.updateBufferedImage (imagein);
		cpimage.updateBufferedImage (imageout);
		
		if ((cpcolortable.cpspacewin != null) && cpcolortable.cpspacewin.isVisible ())
			cpcolortable.cpspacewin.update (imagein, pam.vconfig.channels);
	}

	public void updateSeeds(Channel channel) 
	{
		cpzoom.updateSeeds (channel);
		cpimage.updateSeeds (channel);
	}

	public void redrawBufferedImage () 
	{
		updateBufferedImage (imagein);
	}

	// Incoming image from movie file
	public void receivedImage(BufferedImage bufferedImage) 
	{
		if (bufferedImage != null)
			cpzoom.updateBufferedImage(Segmentation.ycrcbToRgb(bufferedImage));
	}

	/*
		 TODO: codigo pendiente
		 HABRA QURE AHCERLO CON MENUS O BOTONES

		 		btsavepict.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					guicamera.cpimadisplay.saveImage ("./images/img" + count, ChaosVisionPanel.IMAGEFORMAT);
					count++;
				}
			});	
			btloadpict.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					guicamera.loadOfflineImage ();
				}
			});	
			btloaddir.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					guicamera.loadOfflineImages ();
				}
			});	
			btloadmov.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) 
				{
					guicamera.chooser.resetChoosableFileFilters ();
					guicamera.chooser.setFileFilter (new QTFileFilter ());		
					if (guicamera.chooser.showOpenDialog (guicamera) == JFileChooser.APPROVE_OPTION)
						movcanvas.setSourceMovie (guicamera.chooser.getSelectedFile ().getPath ());
				}
			});	

	 */
	public void loadOfflineImage() 
	{
		File file;

		chooser.resetChoosableFileFilters();
		chooser.setFileFilter(new ImageFileFilter());
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
		{

			file = new File(chooser.getSelectedFile().getPath());
			filename = file.getName();
			try { imagein = Segmentation.rgbToYcrcb (ImageIO.read (file)); } catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	public class ChaosVisionMotionMouse extends MouseInputAdapter
	{	
		public void mousePressed (MouseEvent evt) 
		{
			int			x, y;
			int			rgb;

			if (imagein == null)				return;
			
			x	= mouseToImageX (evt, imagein);
			y	= mouseToImageY (evt, imagein);

			rgb	= imagein.getRGB (x, y);
			
			cpcolortable.modifySeed (rgb);			
		}

		public void mouseMoved (MouseEvent evt) 
		{
			int			x, y;
			int			rgb;

			if (imagein == null)				return;

			x	= mouseToImageX (evt, imagein);
			y	= mouseToImageY (evt, imagein);

			rgb	= imagein.getRGB (x, y);
			cpzoom.pixel (x, y, rgb);
			pixel (x, y, rgb);	
		}

		private final int mouseToImageX (MouseEvent evt, BufferedImage image)
		{
			int				offx, imgx, imgy;
			double			ix, scale;
			Dimension		dim;

			dim		= ((JComponent) evt.getSource ()).getSize ();
			imgx		= (int) (image.getWidth () * SoccerVision.FACTOR);
			imgy		= (int) (image.getHeight () * SoccerVision.FACTOR);
			scale	= Math.min (dim.getWidth () / (double) imgx, dim.getHeight () / (double) imgy);
			offx		= (int) Math.max ((dim.getWidth () - imgx*scale) / 2.0, 0.0);
			ix		= Math.round ((double) (evt.getX () - offx) / (SoccerVision.FACTOR * scale));

			return Math.max (Math.min ((int) ix, image.getWidth ()-1), 0);
		}

		private final int mouseToImageY (MouseEvent evt, BufferedImage image)
		{
			int				offy, imgx, imgy;
			double			iy, scale;
			Dimension		dim;

			dim		= ((JComponent) evt.getSource ()).getSize ();
			imgx		= (int) (image.getWidth () * SoccerVision.FACTOR);
			imgy		= (int) (image.getHeight () * SoccerVision.FACTOR);
			scale	= Math.min (dim.getWidth () / (double) imgx, dim.getHeight () / (double) imgy);
			offy		= (int) Math.max ((dim.getHeight () - imgy*scale) / 2.0, 0.0);
			iy		= Math.round ((double) (evt.getY () - offy) / (SoccerVision.FACTOR * scale));

			return Math.max (Math.min ((int) iy, image.getHeight ()-1), 0);
		}
		
		private String format (int value, int len)
		{
			String out;
			
			out	= Integer.valueOf (value).toString ();
			while (out.length () < len)
				out = "0" + out;
			return out;
		}
		
		private void pixel (int x, int y, int rgb)
		{
			int		r, g, b;
			int		hsv, h, s, v;
			
			r	= Pixel.getR (rgb);
			g	= Pixel.getG (rgb);
			b	= Pixel.getB (rgb);

			hsv	= Segmentation.rgbToHsv (rgb);
			
			h	= Pixel.getR (hsv);
			s	= Pixel.getG (hsv);
			v	= Pixel.getB (hsv);

			lbx.setText ("X: " + format (x, 3));
			lby.setText ("Y: " + format (y, 3));
			
			lbr.setText ("R: " + format (r, 3)+" H: " + format (h, 3));
			lbg.setText ("G: " + format (g, 3)+" S: " + format (s, 3));
			lbb.setText ("B: " + format (b, 3)+" V: " + format (v, 3));
		}
	}
}
