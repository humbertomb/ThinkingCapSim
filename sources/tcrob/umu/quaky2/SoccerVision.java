/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.awt.image.*;
import javax.swing.*;

import tc.modules.*;
import tc.runtime.thread.ModuleConfig;
import tc.shared.lps.lpo.*;
import tclib.vision.chaos.blobs.BlobForming;
import tclib.vision.chaos.segment.LUT;
import tclib.vision.chaos.segment.Segmentation;
import tc.shared.linda.*;
import tcrob.umu.quaky2.gui.SoccerVisionWindow;
import tcrob.umu.quaky2.lpo.*;

import wucore.utils.color.*;

import devices.data.*;

public class SoccerVision extends Perception
{
	static public final double		FACTOR		= 1.0;
	
	static public final double		BALL_RADIUS	= 0.11;			// Ball radius (m)
	static public final double		NET_SIZE	= 0.2;			// Net size (m)
	
	protected VisionData[]			vision;
	
	// Application LPOs
	protected Ball					ball;
	protected Net					net1;
	protected Net					net2;
	protected LPOPoint				align;

	// Vision processing
	public SoccerVisionConfig		vconfig;
	public String					vfile;			// the file the configuration was read from (PARAMS); null if none
	
	public LUT						lut;
	public Segmentation				segment;
	public BlobForming				blobbing;
	public SoccerRecognizer			recognizer;
	public BufferedImage			recognized;		// the last frame with what was recognised drawn on it

	// Local graphics: configuration and monitoring of the vision
	protected SoccerVisionWindow	win;
	
	private boolean					initialized = false;

	// Constructors
	public SoccerVision (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
		
		vconfig		= new SoccerVisionConfig ();
	}
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);
		
		String			name = null;
		
		// Load and parse a BG program
		name = cfg.get ("PARAMS");
		if (name != null)
			try { vconfig.loadFromFilename(name); vfile = name; }	catch (Exception e) { e.printStackTrace(); }
	}
	
	public void instanceLUT ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack		= LUT.class.getPackage().getName();
			sclass		= Class.forName (pack + "." + SoccerVisionConfig.LUTMODES[vconfig.lutmode]);
			lut			= (LUT) sclass.getDeclaredConstructor().newInstance();
			lut.initialise (vconfig.channels);
		} catch (Exception ex) { ex.printStackTrace (); }		
	}

	public void instanceSegment ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack		= Segmentation.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.SEGMODES[vconfig.segmode]);
			segment 	= (Segmentation) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public void instanceBlob ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack		= BlobForming.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.BLOBMODES[vconfig.blobmode]);
			blobbing 	= (BlobForming) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}
	
	public void step (long ctime)
	{
		if (state != RUN)		return;
				
//				
//		lps.add_time ((double) (System.currentTimeMillis () - ctime));
//		
//		// Update the LPS in the Linda space
//		tupd	 = ctime - stime;		
//		lstore.set (lps, tupd);		
//		linda.write (ltuple);
	}
	

	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		// Add domain specific LPOs to the LPS
		ball	= new Ball (BALL_RADIUS, "Ball", LPO.PERCEPT);
		ball.color (WColor.YELLOW.darker());
		
		net1	= new Net (NET_SIZE, "Net1", LPO.PERCEPT);
		net1.color (WColor.BLUE);
		
		net2	= new Net (NET_SIZE, "Net2", LPO.PERCEPT);
		net2.color (WColor.RED);
		
		align	= new LPOPoint (0.0, 0.0, 0.0, "Align", LPO.ARTIFACT);
		align.color (WColor.MAGENTA);
		
		lps.add (ball);
		lps.add (net1);
		lps.add (net2);
		lps.add (align);
		
		// Instance vision processing algorithms
		recognizer	= new SoccerRecognizer ();

		instanceLUT ();
		instanceSegment ();
		instanceBlob ();
		
		// with local graphics, the window to configure and watch the vision (once the algorithms are there)
		if (localgfx && (win == null))
			SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;
					
					win = new SoccerVisionWindow (hostFrame (), SoccerVision.this);		// docked on the right of the simulator
					win.setTitle ("Chaos Vision Monitor [" + robotid + "]");
					win.setVisible (true);
				}
			});
		
		initialized = true;
	}
	
	public void notify_camera (String space, ItemCamera item)
	{
		if (!initialized)			return;
		
		segment.process (item.image, lut, vconfig.channels);
		blobbing.process (segment);
		blobbing.postProcess ();
		recognized	= recognizer.process (item.image, segment.getSegmented(), blobbing.getBlobs (), vconfig.channels, vconfig);

		// what the camera saw, and what came out of it, to the window
		if (win != null)
			SwingUtilities.invokeLater(() -> win.updateBufferedImage (item.image));
		
	    // Object-level fusion and LPS update
//	    if (vision != null)
//			for (i = 0; i < vision.length; i++)
//				if (vision[i].valid)
//					lps.set_lpo (vision[i]);
//	    vision		= null;

//		ballSeen	= (ball.anchor () >= 0.1);
//		net1Seen 	= (net1.anchor () >= 0.1);
//		net2Seen 	= (net2.anchor () >= 0.1);
//
//		ballSeen	= true;
//		net1Seen 	= true;
//		net2Seen 	= true;
	    	
	    
		// Update low-level perception & LPS data
//		lps.update (data, fusion, lodom, pos, null);

	}
}

