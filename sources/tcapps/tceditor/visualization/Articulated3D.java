/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.util.HashMap;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;

import tc.vrobot.articulated.Frame;
import tc.vrobot.articulated.KineModel;
import tc.vrobot.articulated.KineNode;
import tc.vrobot.articulated.KineShape;

/**
 * An articulated robot in the 3D world, from its kinematic model
 * ({@link KineModel}): the tree of its links as a tree of transform groups,
 * each link placed on its parent as the model says and turned by its joint,
 * with the parts the robot is made of (a 3D Studio file per link, from the
 * folder of its parts) when they are all there, and with the solids the model
 * draws it with otherwise. {@link #update} takes the angles
 * the model has now to the scene, so that a robot that moves its legs in the
 * model moves them here; {@link #move} puts the whole robot at a pose in the
 * world. The frame of the robot is the simulator's: x forward, y left, z up.
 */
public class Articulated3D extends BranchGroup
{
	/** How finely the round solids are drawn. */
	static public final int			DIVISIONS	= 24;

	protected KineModel				model;
	protected String				parts;						// the folder of its parts, when it is drawn from them
	protected TransformGroup		pose;						// the robot in the world
	protected Map<String, TransformGroup>	joints = new HashMap<String, TransformGroup> ();	// the turning part of each link, by name
	private Transform3D				t = new Transform3D ();

	public Articulated3D (KineModel model)
	{
		this (model, null);
	}

	/** The same, drawn from the parts in a folder when the model names them and they are all there. */
	public Articulated3D (KineModel model, String parts)
	{
		this.model	= model;
		this.parts	= model.partsAvailable (parts) ? parts.trim () : null;
		setCapability (BranchGroup.ALLOW_DETACH);
		pose	= new TransformGroup ();
		pose.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild (pose);
		if (model.root != null)		pose.addChild (build (model.root));
		update ();
	}

	public KineModel model ()								{ return model; }

	/**
	 * A link and everything under it: a fixed group that places it on its parent,
	 * a turning group for its joint (kept, to be turned later), and in it the
	 * solids of the link and its children.
	 */
	protected TransformGroup build (KineNode n)
	{
		TransformGroup	fixed = new TransformGroup (transform (Frame.translation (n.translation).times (Frame.rotation (n.rotation))));
		TransformGroup	turn = new TransformGroup ();

		turn.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		fixed.addChild (turn);
		if (n.name != null)			joints.put (n.name, turn);

		javax.media.j3d.Node	part = (parts != null) ? part (n) : null;

		if (part != null)					turn.addChild (part);
		else								for (KineShape s : n.shapes)		turn.addChild (shape (s));
		for (KineNode c : n.children)		turn.addChild (build (c));
		return fixed;
	}

	/** Whether the robot is drawn from its parts (or from its solids). */
	public boolean fromParts ()								{ return parts != null; }

	/* The parts read once, by file: what a link is drawn with is a copy of them */
	static private final Map<String, BranchGroup>	PARTS = new HashMap<String, BranchGroup> ();

	/**
	 * The part of a link: its 3D Studio file, written Y up as every .3ds and
	 * turned a quarter turn about x here to stand in the frame of the link (z up),
	 * as the models of the robots are placed in the scene. Null when the link has
	 * none or it cannot be read.
	 */
	protected javax.media.j3d.Node part (KineNode n)
	{
		java.io.File	f = KineModel.part (parts, n);

		if (f == null)				return null;

		String			key = f.getPath ();
		BranchGroup		bg;

		synchronized (PARTS)
		{
			bg	= PARTS.get (key);
			if (bg == null)
			{
				try
				{
					com.sun.j3d.loaders.Scene	scene = new com.mnstarfire.loaders3d.Loader3DS ().load (key);

					bg	= scene.getSceneGroup ();
					PARTS.put (key, bg);
				}
				catch (Exception e)
				{
					System.out.println ("--[Articulated3D] Cannot read the part <" + key + ">: " + e);
					return null;
				}
			}
			bg	= (BranchGroup) bg.cloneTree (true);
		}

		Transform3D		r = new Transform3D ();

		r.rotX (Math.PI / 2.0);

		TransformGroup	tg = new TransformGroup (r);

		tg.addChild (bg);
		return tg;
	}

	/** One solid of a link, where the link has it. */
	protected TransformGroup shape (KineShape s)
	{
		TransformGroup	tg = new TransformGroup (transform (s.frame ()));
		Appearance		app = appearance (s.rgb ());
		int				flags = Primitive.GENERATE_NORMALS;
		char			along = s.along ();

		if (KineShape.BOX.equals (s.type) && (s.size != null) && (s.size.length >= 3))
			tg.addChild (new Box ((float) (s.size[0] / 2.0), (float) (s.size[1] / 2.0), (float) (s.size[2] / 2.0), flags, app));
		else if (KineShape.SPHERE.equals (s.type))
			tg.addChild (new Sphere ((float) s.radius, flags, DIVISIONS, app));
		else if (KineShape.CYLINDER.equals (s.type) || KineShape.CAPSULE.equals (s.type))
		{
			// a Cylinder of Java 3D stands along y: turned to lie along the axis asked for
			TransformGroup	up = new TransformGroup (upright (along));

			up.addChild (new Cylinder ((float) s.radius, (float) s.height, flags, DIVISIONS, 1, app));
			if (KineShape.CAPSULE.equals (s.type))
			{
				Transform3D		a = new Transform3D (), b = new Transform3D ();
				TransformGroup	ta, tb;

				a.setTranslation (new Vector3d (0.0, s.height / 2.0, 0.0));
				b.setTranslation (new Vector3d (0.0, -s.height / 2.0, 0.0));
				ta	= new TransformGroup (a);	ta.addChild (new Sphere ((float) s.radius, flags, DIVISIONS, app));
				tb	= new TransformGroup (b);	tb.addChild (new Sphere ((float) s.radius, flags, DIVISIONS, app));
				up.addChild (ta);
				up.addChild (tb);
			}
			tg.addChild (up);
		}
		return tg;
	}

	/** The turn that takes the y axis (a Cylinder's) to the axis asked for. */
	static protected Transform3D upright (char along)
	{
		Transform3D		r = new Transform3D ();

		if (along == 'x')			r.rotZ (-Math.PI / 2.0);
		else if (along == 'z')		r.rotX (Math.PI / 2.0);
		return r;
	}

	static protected Appearance appearance (double[] rgb)
	{
		Appearance	app = new Appearance ();
		Color3f		c = new Color3f ((float) rgb[0], (float) rgb[1], (float) rgb[2]);
		Material	m = new Material (new Color3f (0.35f * c.x, 0.35f * c.y, 0.35f * c.z), new Color3f (0f, 0f, 0f), c, new Color3f (0.5f, 0.5f, 0.5f), 48f);

		m.setLightingEnable (true);
		app.setMaterial (m);
		return app;
	}

	static protected Transform3D transform (Frame f)
	{
		return new Transform3D (new Matrix4d (f.m));
	}

	/** The joints of the scene turned as the model has them now. */
	public void update ()
	{
		for (KineNode n : model.nodes ())
		{
			TransformGroup	tg = joints.get (n.name);

			if ((tg == null) || (n.joint == null))		continue;
			tg.setTransform (transform (n.joint.turn (n.angle ())));
		}
	}

	/** The whole robot at a pose in the world: where its body is (m) and how it heads (rad, about z). */
	public void move (double x, double y, double z, double a)
	{
		move (x, y, z, a, 0.0);
	}

	/** The same, the body pitched as well (rad, about its own y, positive nose down): how it stands on its feet. */
	public void move (double x, double y, double z, double a, double pitch)
	{
		Transform3D		p = new Transform3D ();

		t.rotZ (a);
		p.rotY (pitch);
		t.mul (p);
		t.setTranslation (new Vector3d (x, y, z));
		pose.setTransform (t);
	}
}
