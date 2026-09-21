/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

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
	
	public LUT						lut;
	public Segmentation				segment;
	public BlobForming				blobbing;
	public SoccerRecognizer			recognizer;
	public java.awt.image.BufferedImage	recognized;		// the last frame with what was recognised drawn on it

	// Local graphics: configuration and monitoring of the vision
	protected SoccerVisionWindow		win;

	// Constructors
	public SoccerVision (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
	
	// Instance methods
	public void instanceLUT ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack		= LUT.class.getPackage().getName();
			sclass	= Class.forName (pack + "." + SoccerVisionConfig.LUTMODES[vconfig.lutmode]);
			lut		= (LUT) sclass.getDeclaredConstructor().newInstance();
			lut.initialise (vconfig.channels);
		} catch (Exception ex) { ex.printStackTrace (); }		
	}

	public void instanceSegment ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack			= Segmentation.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.SEGMODES[vconfig.segmode]);
			segment 		= (Segmentation) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public void instanceBlob ()
	{
		Class<?>		sclass;
		String		pack;
		
		try 
		{
			pack			= BlobForming.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.BLOBMODES[vconfig.blobmode]);
			blobbing 	= (BlobForming) sclass.getDeclaredConstructor().newInstance();
		} catch (Exception ex) { ex.printStackTrace (); }
	}
	
	protected void lowlevel_fusion ()
	{
		int				i;
		
	    // Object-level fusion and LPS update
	    if (vision != null)
			for (i = 0; i < vision.length; i++)
				if (vision[i].valid)
					lps.set_lpo (vision[i]);
	    vision		= null;

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
		vconfig		= new SoccerVisionConfig ();
		
		instanceLUT ();
		instanceSegment ();
		instanceBlob ();
		
		recognizer	= new SoccerRecognizer ();

		// with local graphics, the window to configure and watch the vision (once the algorithms are there)
		if (localgfx && (win == null))
			javax.swing.SwingUtilities.invokeLater (new Runnable ()
			{
				public void run ()
				{
					if (win != null)		return;
					win		= new SoccerVisionWindow (null, SoccerVision.this);
					win.setTitle ("Chaos Vision Monitor [" + robotid + "]");
					win.setVisible (true);
				}
			});
	}

	protected void close_gfx ()
	{
		dispose_window (win);
		win		= null;
	}
	
	public void notify_camera (String space, ItemCamera item)
	{
		segment.process (item.image, lut, vconfig.channels);
		blobbing.process (segment);
		blobbing.postProcess ();
		recognized	= recognizer.process (item.image, segment.getSegmented(), blobbing.getBlobs (), vconfig.channels, vconfig);

		// what the camera saw, and what came out of it, to the window
		if (win != null)
		{
			final SoccerVisionWindow	w = win;
			final java.awt.image.BufferedImage	image = item.image;
			javax.swing.SwingUtilities.invokeLater (new Runnable () { public void run () { w.updateBufferedImage (image); } });
		}

		
//		// Add domain specific LPOs to the LPS
//		ball	= new Ball (BALL_RADIUS, "Ball", LPO.PERCEPT);
//		ball.color (WColor.YELLOW.darker());
//		
//		net1	= new Net (NET_SIZE, "Net1", LPO.PERCEPT);
//		net1.color (WColor.BLUE);
//		
//		net2	= new Net (NET_SIZE, "Net2", LPO.PERCEPT);
//		net2.color (WColor.RED);
//		
//		align	= new LPOPoint (0.0, 0.0, 0.0, "Align", LPO.ARTIFACT);
//		align.color (WColor.MAGENTA);
//		
//		lps.add (ball);
//		lps.add (net1);
//		lps.add (net2);
//		lps.add (align);
	}
}

