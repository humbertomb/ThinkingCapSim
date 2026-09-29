/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.universe.SimpleUniverse;

import tc.vrobot.articulated.KineJson;
import tc.vrobot.articulated.KineModel;
import tc.vrobot.articulated.KineNode;

/**
 * A look at a kinematic model (.kine): the robot drawn from it over a floor
 * grid, a slider for every joint to turn it by hand, and the walking engine of
 * the Aibo (Walking) with sliders for the speeds asked of it and for where the
 * camera looks, to see it articulate. A tool to check a model with while the
 * articulated robots find their way into the simulator.
 *
 * <pre>
 *   java tcapps.tceditor.visualization.KineViewer [conf/robots/aibo.kine]
 * </pre>
 *
 * Left drag turns the view round the robot, the wheel zooms, right drag moves.
 */
public class KineViewer extends JFrame
{
	private static final long		serialVersionUID = 1L;

	protected KineModel				model;
	protected String				parts;						// the folder of its parts, if any
	protected Articulated3D			robot;
	protected Canvas3D				canvas;
	protected Scene3D				scene;
	protected Map<String, JSlider>	sliders = new HashMap<String, JSlider> ();
	protected JLabel				status;
	protected Timer					walk;
	protected tcrob.umu.soccer.walking.AiboWalking	engine;		// the walk of the Aibo, when the model has its legs
	protected double				vlinCmd, vlatCmd, vrotCmd, panCmd, tiltCmd;
	protected TransformGroup		zoom;
	protected double				scale;
	protected boolean				settingSliders;

	public KineViewer (KineModel model)
	{
		this (model, null);
	}

	/** The same, the robot drawn from its parts in a folder when it names them and they are all there. */
	public KineViewer (KineModel model, String parts)
	{
		super ("Kine Viewer - " + model.name);
		this.model	= model;
		this.parts	= parts;

		canvas	= new Canvas3D (SimpleUniverse.getPreferredConfiguration ());
		canvas.setPreferredSize (new Dimension (900, 650));
		scene	= new Scene3D (canvas);
		scene.universe.getViewer ().getView ().setFrontClipDistance (0.02);
		scene.universe.getViewer ().getView ().setBackClipDistance (50.0);
		scene.rho	= 0.35;
		scene.theta	= -2.5;
		scene.len	= 1.0;
		scene.setViewpoint ();

		// the view of Scene3D sits a fixed way off: the robot is scaled up to be seen, and the wheel scales it
		BranchGroup		bg = new BranchGroup ();

		zoom	= new TransformGroup ();
		zoom.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		zoom.addChild (grid (1.0, 0.1));
		robot	= new Articulated3D (model, parts);
		zoom.addChild (robot);
		bg.addChild (zoom);
		bg.compile ();
		scene.scene.addChild (bg);
		zoom (6.0);
		stand ();

		getContentPane ().setLayout (new BorderLayout ());
		getContentPane ().add (canvas, BorderLayout.CENTER);
		getContentPane ().add (new JScrollPane (controls ()), BorderLayout.EAST);
		status	= new JLabel (" ");
		status.setBorder (BorderFactory.createEmptyBorder (3, 8, 3, 8));
		getContentPane ().add (status, BorderLayout.SOUTH);
		mouse ();
		setDefaultCloseOperation (EXIT_ON_CLOSE);
		pack ();
		setLocationRelativeTo (null);
		said ();
	}

	/** How big the robot is drawn (the view cannot come closer), centred a little over the floor. */
	protected void zoom (double s)
	{
		Transform3D	t = new Transform3D ();

		scale	= Math.max (0.5, Math.min (40.0, s));
		t.setScale (scale);
		t.setTranslation (new javax.vecmath.Vector3d (0.0, 0.0, -0.12 * scale));
		zoom.setTransform (t);
	}

	/** The robot standing on the floor: its body as high over it as the lowest of what it is drawn with is below the body, level. */
	protected void stand ()
	{
		model.forward ();
		robot.update ();
		standing	= tcapps.tceditor.ShapeLines.standing (model, null, parts);
		robot.move (0.0, 0.0, standing[1], 0.0, 0.0);
	}

	/** The pose of the walk on the floor, {pitch, height}, worked out once the engine stands (the joints then move about it). */
	protected double[]			standing = { 0.0, 0.0 };

	/** A slider per joint, in degrees within its limits, and the walk. */
	protected JPanel controls ()
	{
		JPanel				p = new JPanel (new GridBagLayout ());
		GridBagConstraints	c = new GridBagConstraints ();
		int					row = 0;

		c.insets	= new Insets (1, 6, 1, 6);
		c.anchor	= GridBagConstraints.WEST;
		for (final KineNode n : model.joints ())
		{
			int		lo = (n.joint.min != null) ? (int) Math.round (Math.toDegrees (n.joint.min.doubleValue ())) : -180;
			int		hi = (n.joint.max != null) ? (int) Math.round (Math.toDegrees (n.joint.max.doubleValue ())) : 180;
			final JSlider	s = new JSlider (lo, hi, (int) Math.round (Math.toDegrees (n.angle ())));
			final JLabel	v = new JLabel (String.format ("%4d", s.getValue ()));

			s.setPreferredSize (new Dimension (170, 20));
			s.addChangeListener (new ChangeListener ()
			{
				public void stateChanged (ChangeEvent e)
				{
					v.setText (String.format ("%4d", s.getValue ()));
					if (settingSliders)		return;
					model.setAngle (n.name, Math.toRadians (s.getValue ()));
					stand ();
					said ();
				}
			});
			sliders.put (n.name, s);
			c.gridx = 0;	c.gridy = row;		p.add (new JLabel (n.name), c);
			c.gridx = 1;						p.add (s, c);
			c.gridx = 2;						p.add (v, c);
			row++;
		}

		// the walk: the engine of the Aibo when the model has its legs, with the speeds
		// asked of it and where the camera looks; the joints' sliders then only show
		final JCheckBox		w = new JCheckBox ("Walking", false);
		final JPanel		speeds = new JPanel (new GridBagLayout ());

		try			{ engine = new tcrob.umu.soccer.walking.AiboWalking (model); }
		catch (Exception e)		{ engine = null; }
		w.setEnabled (engine != null);
		w.setToolTipText ((engine != null) ? "The walking engine of the Aibo moves the legs and the head" : "The model has not the legs of the Aibo");
		w.addChangeListener (new ChangeListener ()
		{
			public void stateChanged (ChangeEvent e)
			{
				speeds.setVisible (w.isSelected ());
				for (JSlider s : sliders.values ())		s.setEnabled (!w.isSelected ());
				if (w.isSelected ())		{ engine.stand ();	standing = tcapps.tceditor.ShapeLines.standing (model, engine, parts);	walk.start (); }
				else						{ walk.stop ();	model.reset ();	refreshAll (); }
			}
		});
		c.gridx = 0;	c.gridy = row++;	c.gridwidth = 3;	p.add (w, c);

		GridBagConstraints	g = new GridBagConstraints ();
		int					r = 0;

		g.insets	= new Insets (1, 6, 1, 6);
		g.anchor	= GridBagConstraints.WEST;
		speed (speeds, g, r++, "vlin  [cm/s]", -35, 35, 0, new Setter () { public void set (double v) { vlinCmd = v / 100.0;	velocities (); } });
		speed (speeds, g, r++, "vlat  [cm/s]", -35, 35, 0, new Setter () { public void set (double v) { vlatCmd = v / 100.0;	velocities (); } });
		speed (speeds, g, r++, "vrot  [deg/s]", -160, 160, 0, new Setter () { public void set (double v) { vrotCmd = Math.toRadians (v);	velocities (); } });
		speed (speeds, g, r++, "pan   [deg]", -93, 93, 0, new Setter () { public void set (double v) { panCmd = Math.toRadians (v);	velocities (); } });
		speed (speeds, g, r++, "tilt  [deg]", -70, 50, 0, new Setter () { public void set (double v) { tiltCmd = Math.toRadians (v);	velocities (); } });
		speeds.setBorder (BorderFactory.createTitledBorder ("Walking"));
		speeds.setVisible (false);
		c.gridx = 0;	c.gridy = row;		c.gridwidth = 3;	p.add (speeds, c);

		JPanel	box = new JPanel ();

		box.setLayout (new BoxLayout (box, BoxLayout.Y_AXIS));
		box.add (p);
		box.add (Box.createVerticalGlue ());

		walk	= new Timer (40, new java.awt.event.ActionListener ()
		{
			public void actionPerformed (java.awt.event.ActionEvent e)		{ step (); }
		});
		return box;
	}

	/** One tick of the walk: the engine goes on so much time, and the scene and the sliders follow the model. */
	protected void step ()
	{
		if (engine == null)			return;
		engine.step (walk.getDelay () / 1000.0);
		robot.update ();
		robot.move (0.0, 0.0, standing[1], 0.0, standing[0]);				// on the spot: the world does not scroll under it yet
		settingSliders	= true;
		for (KineNode n : model.joints ())
		{
			JSlider	s = sliders.get (n.name);

			if (s != null)		s.setValue ((int) Math.round (Math.toDegrees (n.angle ())));
		}
		settingSliders	= false;
		status.setText (String.format ("%s: vlin %.2f m/s, vlat %.2f m/s, vrot %.2f rad/s, pan %.0f deg, tilt %.0f deg  --  phase %.2f, %s",
									   model.name, engine.vlin (), engine.vlat (), engine.vrot (), Math.toDegrees (panCmd), Math.toDegrees (tiltCmd),
									   engine.phase (), engine.walking () ? "walking" : "standing"));
	}

	/** What is asked of the engine, from the sliders. */
	protected void velocities ()
	{
		if (engine == null)			return;
		engine.setVelocities (vlinCmd, vlatCmd, vrotCmd);
		engine.setHead (panCmd, tiltCmd);
	}

	/** A slider of a speed, with its value beside it. */
	private interface Setter			{ void set (double v); }

	private void speed (JPanel p, GridBagConstraints g, int row, String name, int lo, int hi, int at, final Setter setter)
	{
		final JSlider	s = new JSlider (lo, hi, at);
		final JLabel	v = new JLabel (String.format ("%5d", at));

		s.setPreferredSize (new Dimension (170, 20));
		s.addChangeListener (new ChangeListener ()
		{
			public void stateChanged (ChangeEvent e)		{ v.setText (String.format ("%5d", s.getValue ()));	setter.set (s.getValue ()); }
		});
		g.gridx = 0;	g.gridy = row;		p.add (new JLabel (name), g);
		g.gridx = 1;						p.add (s, g);
		g.gridx = 2;						p.add (v, g);
	}

	/** The scene and the sliders as the model is now. */
	protected void refreshAll ()
	{
		stand ();
		settingSliders	= true;
		for (KineNode n : model.joints ())
		{
			JSlider	s = sliders.get (n.name);

			if (s != null)		s.setValue ((int) Math.round (Math.toDegrees (n.angle ())));
		}
		settingSliders	= false;
		said ();
	}

	protected void said ()
	{
		status.setText (model.name + ": " + model.nodes ().size () + " links, " + model.joints ().size () + " joints"
						+ String.format ("   body %.3f m over the floor", standing[1])
						+ "   |   left drag: turn, right drag: move, wheel: zoom");
	}

	/** A grid on the floor, so much across, with lines so far apart. */
	static protected Shape3D grid (double across, double step)
	{
		int			n = (int) Math.round (across / step);
		LineArray	la = new LineArray (4 * (n + 1), LineArray.COORDINATES);
		int			k = 0;

		for (int i = 0; i <= n; i++)
		{
			double	p = -across / 2.0 + i * step;

			la.setCoordinate (k++, new Point3d (p, -across / 2.0, 0.0));
			la.setCoordinate (k++, new Point3d (p, across / 2.0, 0.0));
			la.setCoordinate (k++, new Point3d (-across / 2.0, p, 0.0));
			la.setCoordinate (k++, new Point3d (across / 2.0, p, 0.0));
		}

		Appearance	app = new Appearance ();

		app.setColoringAttributes (new ColoringAttributes (new Color3f (0.45f, 0.55f, 0.45f), ColoringAttributes.SHADE_FLAT));
		return new Shape3D (la, app);
	}

	private void mouse ()
	{
		MouseAdapter	ma = new MouseAdapter ()
		{
			public void mousePressed (MouseEvent e)			{ scene.mouseDown (e.getX (), e.getY ()); }
			public void mouseDragged (MouseEvent e)
			{
				scene.mouseDrag (SwingUtilities.isRightMouseButton (e) ? Scene3D.M_MOVE : Scene3D.M_ROTATE, e.getX (), e.getY ());
			}
			public void mouseWheelMoved (MouseWheelEvent e)
			{
				zoom (scale * Math.pow (1.12, -e.getWheelRotation ()));
			}
		};
		canvas.addMouseListener (ma);
		canvas.addMouseMotionListener (ma);
		canvas.addMouseWheelListener (ma);
	}

	static public void main (String[] args) throws Exception
	{
		File		f = new File ((args.length > 0) ? args[0] : (KineJson.FOLDER + "/aibo" + KineJson.SUFFIX));
		KineModel	m = KineJson.read (f);

		System.out.println (m);
		new KineViewer (m, (args.length > 1) ? args[1] : null).setVisible (true);
	}
}
