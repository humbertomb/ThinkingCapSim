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
 * grid, a slider for every joint to turn it by hand, and a walk that swings
 * the legs, to see it articulate. A tool to check a model with while the
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
	protected Articulated3D			robot;
	protected Canvas3D				canvas;
	protected Scene3D				scene;
	protected Map<String, JSlider>	sliders = new HashMap<String, JSlider> ();
	protected JLabel				status;
	protected Timer					walk;
	protected TransformGroup		zoom;
	protected double				scale;
	protected double				phase;
	protected boolean				settingSliders;

	public KineViewer (KineModel model)
	{
		super ("Kine Viewer - " + model.name);
		this.model	= model;

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
		robot	= new Articulated3D (model);
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

	/** The robot standing on the floor: its body as high over it as its lowest link is below the body. */
	protected void stand ()
	{
		model.forward ();
		robot.update ();
		robot.move (0.0, 0.0, -model.lowest () + 0.012, 0.0);		// the paws are drawn a little below their link
	}

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

		final JCheckBox		w = new JCheckBox ("Walk", false);

		w.addChangeListener (new ChangeListener ()
		{
			public void stateChanged (ChangeEvent e)		{ if (w.isSelected ()) walk.start (); else { walk.stop (); model.reset (); refreshAll (); } }
		});
		c.gridx = 0;	c.gridy = row;	c.gridwidth = 3;	p.add (w, c);

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

	/**
	 * One step of a trot: the diagonal pairs of legs swing in opposite phase, the
	 * knees bending as the leg comes forward, the head nodding a little with it.
	 * Whatever joints the model has with the names of the Aibo are moved; a model
	 * with other names just stands.
	 */
	protected void step ()
	{
		phase	+= 2 * Math.PI * 0.04 / 1.2;								// a stride every 1.2 s

		double	s = Math.sin (phase), c = Math.cos (phase);

		swing ("LEFT_FORELEG",   s,  c);
		swing ("RIGHT_HINDLEG",  s,  c);
		swing ("RIGHT_FORELEG", -s, -c);
		swing ("LEFT_HINDLEG",  -s, -c);
		model.setAngle ("HEAD_TILT", model.node ("HEAD_TILT") != null ? model.node ("HEAD_TILT").joint.def + 0.06 * Math.sin (2 * phase) : 0.0);
		model.setAngle ("TAIL_PAN", 0.5 * Math.sin (phase));
		refreshAll ();
	}

	private void swing (String leg, double s, double c)
	{
		KineNode	j1 = model.node (leg + "_J1"), j3 = model.node (leg + "_J3");

		if (j1 != null)			model.setAngle (j1.name, j1.joint.def + 0.35 * s);
		if (j3 != null)			model.setAngle (j3.name, j3.joint.def + 0.45 * Math.max (0.0, c));		// the knee lifts while the leg comes forward
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
						+ String.format ("   body %.3f m over the floor", -model.lowest () + 0.012)
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
		new KineViewer (m).setVisible (true);
	}
}
