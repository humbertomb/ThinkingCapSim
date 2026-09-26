/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.awt.image.*;
import javax.swing.*;

import tc.modules.*;
import tc.runtime.thread.ModuleConfig;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tc.shared.linda.ItemBehNeeds.ScanTypes;
import tclib.vision.chaos.blobs.BlobForming;
import tclib.vision.chaos.segment.LUT;
import tclib.vision.chaos.segment.Segmentation;
import tc.vrobot.*;
import tcrob.umu.quaky2.gui.SoccerVisionWindow;
import tcrob.umu.quaky2.lpo.*;

import wucore.utils.color.*;
import wucore.utils.math.Angles;

public class SoccerVision extends Perception
{
	static public final double			FACTOR		= 1.0;
	
	static public final double			BALL_RADIUS	= 0.11;			// Ball radius (m)
	static public final double			NET_SIZE	= 0.2;			// Net size (m)

	static public final double			BALL_FADING	= 8.0;
	static public final double			NET_FADING	= 15.0;

	static public final int				SCAN_STEPS	= 10;

	/** How sure the LPS has to be of an object (its anchor) for the camera to turn to where it was last seen. */
	static public final double			ANCHOR_MIN	= 0.2;
	/** Where the camera aims at a net when it turns to it: this high up it (m), so it does not look at the floor line. */
	static public final double			NET_AIM		= 0.15;
		
	// Application LPOs
	protected LPOBall					ball;
	protected LPONet					net1;
	protected LPONet					net2;
	protected LPOAlign					align;

	// Vision processing
	public SoccerVisionConfig			vconfig;
	public String						vfile;			// the file the configuration was read from (PARAMS); null if none
	
	public volatile LUT					lut;			// replaced from the window while frames arrive
	public Segmentation					segment;
	public BlobForming					blobbing;
	public SoccerRecognizer				recognizer;
	public BufferedImage				recognized;		// the last frame with what was recognised drawn on it

	// Camera control
	public ScanTypes					scan = ScanTypes.SCAN_HIGH;
	private int							scan_step = 0;		// where the scan is, 0 .. SCAN_STEPS (both ends of the pan included)
	private int							scan_dir = 1;		// which way it goes: +1 towards +pan max, -1 back towards -pan max
	private int							scan_full = 0;		// a full scan: which of LOW (0), MID (1) and HIGH (2) this sweep of the pan is
	private double						scan_max_pan = 0.0;
	private double						scan_max_tilt = 0.0;	
	protected CameraCtrl				camera_ctrl;

	protected Tuple						ctuple;
	protected ItemCameraCtrl			citem;

	// Attention: the object the behaviours need to keep seeing (BEH_NEEDS), which
	// the camera is turned to instead of scanning; null when nothing is needed
	protected String					attending;

	// Local graphics: configuration and monitoring of the vision
	protected SoccerVisionWindow		win;
	
	private volatile boolean			initialized = false;

	/**
	 * One frame is processed with one set of algorithms. The frames arrive on the
	 * thread of the robot while the algorithms are replaced on another one (a new
	 * configuration, which is what a restart of the execution sends, or the window
	 * of the vision), so segmenting with one and blobbing with the next one -- which
	 * has never seen a frame and knows of no channels -- was possible.
	 */
	protected final Object				vision = new Object ();

	// The LPS the LPOs of the vision are in: the one of the perception module of the robot (see lps_guest)
	protected LPS						attached;

	// Constructors
	public SoccerVision (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
		
		vconfig		= new SoccerVisionConfig ();
		camera_ctrl	= new CameraCtrl ();
	}
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);

		String			name = null;

		// Setup local stuff
		citem	= new ItemCameraCtrl ();
		ctuple	= new Tuple (Tuple.CAMERA_CTRL, citem);

		// Load and parse a BG program
		name = cfg.get ("PARAMS");
		if (name != null)
			try { vconfig.loadFromFilename(name); vfile = name; }	catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * A new LUT, of the method of the configuration. It is built before it takes
	 * the place of the one in use: the frames that arrive meanwhile (from another
	 * thread) are segmented with the old one, never with one not yet computed.
	 */
	public void instanceLUT ()
	{
		Class<?>	sclass;
		String		pack;
		LUT			nlut;
		
		try 
		{
			pack		= LUT.class.getPackage().getName();
			sclass		= Class.forName (pack + "." + SoccerVisionConfig.LUTMODES[vconfig.lutmode]);
			nlut		= (LUT) sclass.getDeclaredConstructor().newInstance();
			nlut.initialise (vconfig.channels);
			synchronized (vision)		{ lut = nlut; }				// never in the middle of a frame
		} catch (Exception ex) { ex.printStackTrace (); }		
	}

	public void instanceSegment ()
	{
		Class<?>	sclass;
		String		pack;
		
		try 
		{
			pack		= Segmentation.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.SEGMODES[vconfig.segmode]);

			Segmentation	nseg = (Segmentation) sclass.getDeclaredConstructor().newInstance();

			synchronized (vision)		{ segment = nseg; }			// never in the middle of a frame
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	public void instanceBlob ()
	{
		Class<?>	sclass;
		String		pack;
		
		try 
		{
			pack		= BlobForming.class.getPackage().getName();
			sclass		= Class.forName(pack + "." + SoccerVisionConfig.BLOBMODES[vconfig.blobmode]);

			BlobForming		nblob = (BlobForming) sclass.getDeclaredConstructor().newInstance();

			synchronized (vision)		{ blobbing = nblob; }		// never in the middle of a frame
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
		
		do_scan_pattern ();
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		// How far the camera can be turned when scanning: what the description of
		// the robot says of its first camera (pan max, tilt max, in radians), and
		// nothing when it has none or the camera is fixed
		scan_max_pan	= 0.0;
		scan_max_tilt	= 0.0;
		if ((rdesc != null) && (rdesc.MAXCAMERA > 0))
		{
			if ((rdesc.campanmax != null) && (rdesc.campanmax.length > 0))		scan_max_pan	= rdesc.campanmax[0];
			if ((rdesc.camtiltmax != null) && (rdesc.camtiltmax.length > 0))	scan_max_tilt	= rdesc.camtiltmax[0];
		}

		// Add domain specific LPOs to the LPS
		ball	= new LPOBall (BALL_RADIUS, "Ball", LPOSource.PERCEPT);
		ball.anchor_fade = BALL_FADING;
		ball.color (WColor.YELLOW.darker());
		
		net1	= new LPONet (NET_SIZE, "Net1", LPOSource.PERCEPT);
		net1.anchor_fade = NET_FADING;
		net1.color (WColor.RED);
		
		net2	= new LPONet (NET_SIZE, "Net2", LPOSource.PERCEPT);
		net2.anchor_fade = NET_FADING;
		net2.color (WColor.BLUE);
		
		align	= new LPOAlign ("Align", LPOSource.ARTIFACT);
		align.anchor_fade = NET_FADING;
		align.color (WColor.MAGENTA);
		
		attached	= null;								// they go into the LPS of the robot when there is something to put there
		
		// Instance vision processing algorithms. A configuration also arrives when an
		// execution is started again, with frames of the one before still on their
		// way, so no frame is made sense of while the algorithms are being replaced
		initialized	= false;
		synchronized (vision)		{ recognizer = new SoccerRecognizer (vconfig.recognizer); }

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
		// the frame is segmented, blobbed and recognised with the algorithms as they
		// are now: they may be replaced from another thread (a new configuration, the
		// window of the vision), but not while a frame is being made sense of
		synchronized (vision)
		{
			if (!initialized || (lut == null) || (segment == null) || (blobbing == null) || (recognizer == null))
				return;

			segment.process (item.image, lut, vconfig.channels);
			blobbing.process (segment);
			blobbing.postProcess ();
			recognized	= recognizer.process (item.image, segment.getSegmented(), blobbing.getBlobs (), vconfig.channels, vconfig);
		}

		// where what was recognised is: into the LPOs of the vision, and out (OBJECT) to the LPS of the robot
		located (item);

		// what the camera saw, and what came out of it, to the window. The window it
		// is shown in is the one there is now: the module may be stopped (close_gfx)
		// before the drawing is done, and a window that is gone is drawn on no more
		final SoccerVisionWindow	shown = win;

		if (shown != null)
			SwingUtilities.invokeLater (() -> shown.updateBufferedImage (item.image));
		
		// and where the camera goes next: to what is needed, or on with its scan
		attend (item);
	}

	/**
	 * What the behaviours need of the vision: how to scan with the camera, and
	 * which objects to keep seeing. Of those, the one needed most (the highest
	 * need, the first when several are needed as much) is the one the camera
	 * attends to; nothing needed, and the camera scans as told.
	 */
	public void notify_beh_neeeds (String space, ItemBehNeeds item)
	{
		ItemBehNeeds.BehNeeds	most = item.mostNeeded ();

		scan		= item.scanType;
		attending	= (most != null) ? most.object : null;
	}

	/** The object the camera is attending to (the LPS's name for it), or null when it is scanning. */
	public String attending ()								{ return attending; }

	/* ------------------------------------------------------------------ */
	/* Attention: where the camera goes next                               */
	/* ------------------------------------------------------------------ */

	/**
	 * Turns the camera for the next frame. With an object to attend to, the scan
	 * stops and the camera is turned to hold it in the fovea (the centre of the
	 * frame): when the object is in this frame, by what it is off the centre, so
	 * that the next frame has it there; when it is not in the frame but the LPS
	 * still knows where it is (it was seen, and its anchor has not faded below
	 * {@link #ANCHOR_MIN}), towards where the LPS has it; and when nothing is known
	 * of it, the camera scans for it as the behaviours asked (setScanType). With
	 * nothing to attend to, the camera scans.
	 */
	protected void attend (ItemCamera frame)
	{
		String						what = attending;
		SoccerRecognizer.Detection	d;
		LPO							o;

		if (what == null)					{ do_scan_pattern ();	return; }

		d	= detection (what);
		o	= object (what);
		if (d != null)
			foveate (frame, d, o == ball);
		else if ((o != null) && (o.anchor () >= ANCHOR_MIN))
			turnTo (frame.device, o, (o == ball) ? BALL_RADIUS : NET_AIM);
		else
			do_scan_pattern ();
	}

	/** What the recognizer found of an object in the frame just processed, by the LPS's name for it, or null. */
	protected SoccerRecognizer.Detection detection (String name)
	{
		if ((recognizer == null) || (name == null))		return null;
		if ((ball != null) && name.equals (ball.label ()))	return recognizer.ball;
		if ((net1 != null) && name.equals (net1.label ()))	return recognizer.net1;
		if ((net2 != null) && name.equals (net2.label ()))	return recognizer.net2;
		return null;
	}

	/** The object of the LPS of that name, as the LPS of the robot has it now, or null. */
	protected LPO object (String name)
	{
		LPS			l = lps_current ();

		if ((l == null) || (name == null))				return null;
		synchronized (l) { return l.find (name); }
	}

	/**
	 * Turns the camera by what an object seen in the frame is off its centre, so
	 * that the next frame has it in the centre: the pixel is turned into the angles
	 * of the ray through it (the two fields of view spread over the frame), and
	 * those go on top of the pan and tilt the frame was taken with. A ball is
	 * followed by the centre of its circle, which may be out of the frame when the
	 * ball is cut by it; a net by the centre of its blob.
	 */
	protected void foveate (ItemCamera frame, SoccerRecognizer.Detection d, boolean round)
	{
		double		hfov = camhfov (frame.device), vfov = camvfov (frame.device);
		int			w = frame.image.getWidth (), h = frame.image.getHeight ();
		double		u = round ? d.cx : d.x, v = round ? d.cy : d.y;
		double		ax, ay;

		if ((hfov <= 0.0) || (vfov <= 0.0) || (w <= 0) || (h <= 0))		return;
		ax	= Math.atan ((2.0 * (u + 0.5) / w - 1.0) * Math.tan (hfov / 2.0));		// to the right of the centre
		ay	= Math.atan ((1.0 - 2.0 * (v + 0.5) / h) * Math.tan (vfov / 2.0));		// above it
		aim (frame.pan - ax, frame.tilt + ay);
	}

	/**
	 * Turns the camera towards where the LPS has an object, at a height of it: the
	 * pan and tilt that point the camera, from where it sits on the robot, at that
	 * point.
	 */
	protected void turnTo (int dev, LPO o, double height)
	{
		SensorPos	feat = camfeat (dev);
		double		ox, oy, dx, dy, pan, tilt;

		if (feat == null)					return;
		ox		= o.rho () * Math.cos (o.theta ());
		oy		= o.rho () * Math.sin (o.theta ());
		dx		= ox - feat.rho () * Math.cos (feat.theta ());
		dy		= oy - feat.rho () * Math.sin (feat.theta ());
		pan		= Angles.radnorm_180 (Math.atan2 (dy, dx) - feat.orientation ());
		tilt	= Math.atan2 (height - feat.z (), Math.sqrt (dx * dx + dy * dy)) - feat.elevation ();
		aim (pan, tilt);
	}

	/**
	 * Sends the camera a pan and a tilt (CAMERA_CTRL), within what it can do: no
	 * further either way than the description of the robot says (pan max, tilt
	 * max), and nowhere at all when it says the camera is fixed.
	 */
	protected void aim (double pan, double tilt)
	{
		pan		= Math.max (-scan_max_pan, Math.min (scan_max_pan, pan));
		tilt	= Math.max (-scan_max_tilt, Math.min (scan_max_tilt, tilt));
		camera_ctrl.set (pan, tilt);
		citem.set (0, camera_ctrl, System.currentTimeMillis ());
		linda.write (ctuple);
	}

	/* ------------------------------------------------------------------ */
	/* The camera, as the description of the robot has it                  */
	/* ------------------------------------------------------------------ */

	/** Where a camera sits and where it looks, or null when the robot has no such camera. */
	protected SensorPos camfeat (int dev)
	{
		if ((rdesc == null) || (rdesc.camfeat == null) || (dev < 0) || (dev >= rdesc.camfeat.length))		return null;
		return rdesc.camfeat[dev];
	}

	/** How wide a camera sees (rad), that of the family when the camera says nothing. */
	protected double camhfov (int dev)
	{
		if (rdesc == null)					return 0.0;
		return ((rdesc.camhfov != null) && (dev >= 0) && (dev < rdesc.camhfov.length) && (rdesc.camhfov[dev] > 0.0)) ? rdesc.camhfov[dev] : rdesc.CONECAM;
	}

	/** How high a camera sees (rad), that of the family when the camera says nothing. */
	protected double camvfov (int dev)
	{
		if (rdesc == null)					return 0.0;
		return ((rdesc.camvfov != null) && (dev >= 0) && (dev < rdesc.camvfov.length) && (rdesc.camvfov[dev] > 0.0)) ? rdesc.camvfov[dev] : rdesc.VFOVCAM;
	}
	
	/* ------------------------------------------------------------------ */
	/* From the image to the floor                                         */
	/* ------------------------------------------------------------------ */

	/**
	 * Places the objects the recognizer found in the frame (ball, nets) around
	 * the robot, in their LPOs, and from them the point to align the ball with
	 * the net from. The LPOs are in the LPS of the robot (the one of its
	 * perception module, IndoorPerception), which keeps them (moves them with
	 * the robot and ages them) while they are not seen. The ball is put where the
	 * ray through the centre of its circle (fitted to the edge of its blob) is at
	 * the height of its centre; a net, where the ray through the bottom of its
	 * blob meets the floor.
	 */
	protected void located (ItemCamera item)
	{
		int					w = item.image.getWidth (), h = item.image.getHeight ();
		LPS					l = lps_current ();

		synchronized (l)
		{
			attach (l);
			if (l == lps)		l.update_anchors ();			// a shared LPS is aged by its owner
			see (ball, recognizer.ball, false, BALL_RADIUS, recognizer.params.ball_channel, item, w, h);
			see (net1, recognizer.net1, true, 0.0, recognizer.params.net1_channel, item, w, h);
			see (net2, recognizer.net2, true, 0.0, recognizer.params.net2_channel, item, w, h);
		}
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

	/**
	 * An object seen in the frame: its LPO goes where it is, around the robot now
	 * (the frame has just been taken), in the colour of the channel it was seen
	 * in, and the LPS is sure of it again (anchor 1, ageing 0).
	 */
	protected void see (LPO lpo, SoccerRecognizer.Detection d, boolean onFloor, double height, int channel, ItemCamera frame, int w, int h)
	{
		double[]		p;

		if ((d == null) || (lpo == null))		return;
		// a net cut by one side of the frame (the camera panned past it) shows only
		// part of itself, and the centre of that part is not where the net is: it is
		// left where it was last seen whole, and ages. One that fills the frame from
		// side to side is right in front, and its centre is as good as it gets.
		if (onFloor && ((d.xmin <= 0) != (d.xmax >= w - 1)))		return;
		// a net stands on the floor at the bottom of its blob; the ball's centre is the one of its circle
		// (the centre of its blob is not, when the ball is cut by the frame); the camera
		// was turned as the frame says when it took it (the scan), so the rays go from there
		p	= onFloor ? floor (frame.device, frame.pan, frame.tilt, d.x, d.ymax, w, h, height)
					  : floor (frame.device, frame.pan, frame.tilt, d.cx, d.cy, w, h, height);
		if (p == null)							return;			// the ray does not reach that height in front of the camera

		lpo.locate_polar (p[0], p[1], 0.0);
		if ((channel >= 0) && (channel < vconfig.channels.size ()) && (vconfig.channels.at (channel).color != null))
			lpo.color (ColorTool.fromColorToWColor (vconfig.channels.at (channel).color));
		lpo.active (true);
		lpo.anchor (1.0);
		lpo.ageing (0);
	}

	/**
	 * Where the ray through a pixel of the frame reaches a height, around the
	 * robot: {rho, phi} from its centre, or null when it does not (above the
	 * camera, or behind it). The camera is the pinhole the simulator renders
	 * with: its position, orientation and elevation in the robot, turned by the
	 * pan and tilt the frame was taken with, and its two fields of view spread
	 * over the frame.
	 */
	protected double[] floor (int dev, double pan, double tilt, double u, double v, int w, int h, double height)
	{
		SensorPos		feat;
		double			hfov, vfov;
		double			yaw, pitch;
		double[]		f, r, up, d;
		double			xn, yn, t;
		double			x, y;

		feat	= camfeat (dev);
		if (feat == null)						return null;
		hfov	= camhfov (dev);
		vfov	= camvfov (dev);
		if ((hfov <= 0.0) || (vfov <= 0.0))		return null;

		// the axes of the camera in the robot: forward, right and up, with the camera
		// turned on its mount as it was for the frame (pan, then tilt, as the simulator does it)
		yaw		= feat.orientation () + pan;
		pitch	= feat.elevation () + tilt;
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
	
	/**
	 * One step of the scan of the camera, sent to the robot (CAMERA_CTRL): the pan
	 * sweeps from -pan max to +pan max in SCAN_STEPS steps and then back the same
	 * way, end to end and over again, so the camera never jumps from one end to
	 * the other; the tilt is the one of the kind of scan. A full scan is the three
	 * of them in turn: one sweep of the pan low, the next in the middle, the next
	 * high, and low again -- the tilt changes at the ends of the pan.
	 */
	protected void do_scan_pattern ()
	{
		double		pan_step, pan, tilt;
		
		pan_step	= scan_max_pan * 2.0 / (double) SCAN_STEPS;
		pan			= -scan_max_pan + pan_step * scan_step;
		switch (scan)
		{
		case SCAN_LOW:		tilt = tiltOf (0);				break;
		case SCAN_MID:		tilt = tiltOf (1);				break;
		case SCAN_HIGH:		tilt = tiltOf (2);				break;
		case SCAN_FULL:		tilt = tiltOf (scan_full);		break;
		case SCAN_NONE:
		default:			pan = 0.0;	tilt = 0.0;			break;
		}
		camera_ctrl.set (pan, tilt);
		
		citem.set (0, camera_ctrl, System.currentTimeMillis ());
		linda.write (ctuple);
		
		// the next step: on to the end, and back from it -- and at the end of a
		// sweep, a full scan goes on to the next tilt
		scan_step	+= scan_dir;
		if (scan_step >= SCAN_STEPS)		{ scan_step = SCAN_STEPS;	scan_dir = -1;	scan_full = (scan_full + 1) % 3; }
		else if (scan_step <= 0)			{ scan_step = 0;			scan_dir = 1;	scan_full = (scan_full + 1) % 3; }
	}

	/** The tilt of a kind of scan: low (0), in the middle (1) or high (2), as shares of the tilt max. */
	protected double tiltOf (int kind)
	{
		switch (kind)
		{
		case 0:		return -scan_max_tilt * 0.6;
		case 2:		return  scan_max_tilt * 0.8;
		default:	return  0.0;
		}
	}
}

