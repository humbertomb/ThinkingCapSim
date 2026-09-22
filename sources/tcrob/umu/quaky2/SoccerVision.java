/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.awt.Color;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import tc.modules.*;
import tc.runtime.thread.ModuleConfig;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tclib.vision.chaos.blobs.BlobForming;
import tclib.vision.chaos.segment.LUT;
import tclib.vision.chaos.segment.Segmentation;
import tc.shared.linda.*;
import tc.vrobot.SensorPos;
import tcrob.umu.quaky2.gui.SoccerVisionWindow;
import tcrob.umu.quaky2.lpo.*;

import wucore.utils.color.*;

import devices.data.*;

public class SoccerVision extends Perception
{
	static public final double		FACTOR		= 1.0;
	
	static public final double		BALL_RADIUS	= 0.11;			// Ball radius (m)
	static public final double		NET_SIZE	= 0.2;			// Net size (m)
	
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

	// The LPS the LPOs of the vision are in: the one of the perception module of the robot (see lps_guest)
	protected LPS					attached;

	// What is recognised, for the perception module that keeps the LPS of the robot
	protected Tuple					otuple;
	protected ItemObject			ostore;

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

		ostore		= new ItemObject ();
		otuple		= new Tuple (Tuple.OBJECT, ostore);
		
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
	
	/** The Chaos Vision Monitor goes away with the module. */
	protected void close_gfx ()
	{
		dispose_window (win);
		win		= null;
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
		
		attached	= null;								// they go into the LPS of the robot when there is something to put there
		
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

		// where what was recognised is: into the LPOs of the vision, and out (OBJECT) to the LPS of the robot
		located (item);

		// what the camera saw, and what came out of it, to the window
		if (win != null)
			SwingUtilities.invokeLater(() -> win.updateBufferedImage (item.image));
	}

	/* ------------------------------------------------------------------ */
	/* From the image to the floor                                         */
	/* ------------------------------------------------------------------ */

	/**
	 * Places the objects the recognizer found in the frame (ball, nets) around
	 * the robot, updates their LPOs and writes them as OBJECT, where the
	 * perception module that keeps the LPS of the robot takes them
	 * (SoccerPerception). The ball is put where the ray through the centre of its
	 * circle (fitted to the edge of its blob) is at the height of its centre; a
	 * net, where the ray through the bottom of its blob meets the floor.
	 */
	protected void located (ItemCamera item)
	{
		List<VisionData>	seen = new ArrayList<VisionData> ();
		int					w = item.image.getWidth (), h = item.image.getHeight ();

		LPS					l = lps_current ();

		synchronized (l)
		{
			attach (l);
			if (l == lps)		l.update_anchors ();			// a shared LPS is aged by its owner
			see (l, seen, ball, recognizer.ball, false, BALL_RADIUS, recognizer.BALL_CHANNEL, item.device, w, h);
			see (l, seen, net1, recognizer.net1, true, 0.0, recognizer.NET1_CHANNEL, item.device, w, h);
			see (l, seen, net2, recognizer.net2, true, 0.0, recognizer.NET2_CHANNEL, item.device, w, h);
		}

		if (seen.isEmpty ())		return;
		ostore.set (seen.toArray (new VisionData[0]), System.currentTimeMillis ());
		linda.write (otuple);
	}

	/** The vision works on the LPS of the perception module of the robot (IndoorPerception), not on one of its own. */
	protected boolean lps_guest ()							{ return true; }

	/** Puts the LPOs of the vision (ball, nets, alignment) into an LPS, once. */
	protected void attach (LPS l)
	{
		if (l == attached)		return;
		for (LPO o : new LPO[] { ball, net1, net2, align })
			if ((o != null) && (l.find (o.label ()) == null))
				l.add (o);
		attached	= l;
	}

	/** An object seen in the frame, where it is and as what it goes out. */
	protected void see (LPS l, List<VisionData> seen, LPO lpo, SoccerRecognizer.Detection d, boolean onFloor, double height,
						int channel, int dev, int w, int h)
	{
		VisionData		vd;
		double[]		p;
		Color			c;

		if ((d == null) || (lpo == null))		return;
		// a net stands on the floor at the bottom of its blob; the ball's centre is the one of its circle
		// (the centre of its blob is not, when the ball is cut by the frame)
		p	= onFloor ? floor (dev, d.x, d.ymax, w, h, height) : floor (dev, d.cx, d.cy, w, h, height);
		if (p == null)							return;			// the ray does not reach that height in front of the camera

		c	= ((channel >= 0) && (channel < vconfig.channels.size ()) && (vconfig.channels.at (channel).color != null))
				? vconfig.channels.at (channel).color : Color.GRAY;
		vd	= new VisionData ();
		vd.set_blob (lpo.label (), d.x, d.y, d.xmax - d.xmin, d.ymax - d.ymin, c);
		vd.set_dev (dev);
		vd.rho	= p[0];
		vd.phi	= p[1];
		l.set_lpo (vd);
		seen.add (vd);
	}

	/**
	 * Where the ray through a pixel of the frame reaches a height, around the
	 * robot: {rho, phi} from its centre, or null when it does not (above the
	 * camera, or behind it). The camera is the pinhole the simulator renders
	 * with: its position, orientation and elevation in the robot, and its two
	 * fields of view spread over the frame.
	 */
	protected double[] floor (int dev, double u, double v, int w, int h, double height)
	{
		SensorPos		feat;
		double			hfov, vfov;
		double			yaw, pitch;
		double[]		f, r, up, d;
		double			xn, yn, t;
		double			x, y;

		if ((rdesc == null) || (rdesc.camfeat == null) || (dev < 0) || (dev >= rdesc.camfeat.length) || (rdesc.camfeat[dev] == null))
			return null;
		feat	= rdesc.camfeat[dev];
		hfov	= ((rdesc.camhfov != null) && (dev < rdesc.camhfov.length) && (rdesc.camhfov[dev] > 0.0)) ? rdesc.camhfov[dev] : rdesc.CONECAM;
		vfov	= ((rdesc.camvfov != null) && (dev < rdesc.camvfov.length) && (rdesc.camvfov[dev] > 0.0)) ? rdesc.camvfov[dev] : rdesc.VFOVCAM;
		if ((hfov <= 0.0) || (vfov <= 0.0))		return null;

		// the axes of the camera in the robot: forward, right and up
		yaw		= feat.orientation ();
		pitch	= feat.elevation ();
		f		= new double[] { Math.cos (yaw) * Math.cos (pitch), Math.sin (yaw) * Math.cos (pitch), Math.sin (pitch) };
		r		= new double[] { Math.sin (yaw), -Math.cos (yaw), 0.0 };
		up		= new double[] { r[1] * f[2] - r[2] * f[1], r[2] * f[0] - r[0] * f[2], r[0] * f[1] - r[1] * f[0] };

		// the ray through the pixel (its centre), and where it is at that height
		xn		= (2.0 * (u + 0.5) / w - 1.0) * Math.tan (hfov / 2.0);
		yn		= (1.0 - 2.0 * (v + 0.5) / h) * Math.tan (vfov / 2.0);
		d		= new double[] { f[0] + xn * r[0] + yn * up[0], f[1] + xn * r[1] + yn * up[1], f[2] + xn * r[2] + yn * up[2] };
		if (Math.abs (d[2]) < 1e-9)				return null;
		t		= (height - feat.z ()) / d[2];
		if (t <= 0.0)							return null;
		x		= feat.rho () * Math.cos (feat.theta ()) + t * d[0];		// the camera is where the simulator puts it
		y		= feat.rho () * Math.sin (feat.theta ()) + t * d[1];
		return new double[] { Math.sqrt (x * x + y * y), Math.atan2 (y, x) };
	}
}

