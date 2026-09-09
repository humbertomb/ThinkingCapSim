/*
 * Created on 07-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.gui.visualization.objects;

import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.geometry.*;

import tc.vrobot.*;

import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Range3D extends BranchGroup
{
	protected TransformGroup[]		rays;

	private Vector3d					pos = new Vector3d ();
	private Matrix3d					rot = new Matrix3d ();
	private Transform3D				mov = new Transform3D ();
	
	protected int					num;
	protected SensorPos[]				feat;
	protected double					cone;

	public Range3D (SensorPos[] feat, double cone, Color3f color, int num)
	{
		Appearance				app;
		ColoringAttributes		col;
		TransparencyAttributes	trs;
		Cone						ray;
		int						i;
		
		this.feat	= feat;
		this.cone	= cone;
		this.num		= num;

		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);

		rays		= new TransformGroup[num];
		app		= new Appearance ();
		col		= new ColoringAttributes (color, ColoringAttributes.SHADE_GOURAUD);
		//		trs		= new TransparencyAttributes (TransparencyAttributes.SCREEN_DOOR, 0.5f);
		trs		= new TransparencyAttributes (TransparencyAttributes.NICEST, 0.85f);
		app.setColoringAttributes (col);
		app.setTransparencyAttributes (trs);
		
		for (i = 0; i < num; i++)
		{			
			mov.setIdentity ();
			mov.setTranslation (new Vector3d (0.0, 0.0, -100.0));
			mov.setScale (0.0);

			ray		= new Cone (1.0f, 1.0f, Cone.GENERATE_NORMALS | Cone.GENERATE_TEXTURE_COORDS, app);

			rays[i]	= new TransformGroup ();
			rays[i].setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
			rays[i].setTransform (mov);			
			rays[i].addChild (ray);
			
			addChild (rays[i]);
		}
	}
	
	public void move (double[] data, Point3 pt, double a)
	{
		int					i;
		double				xo, yo;
		double				ap, as, rad;		
		
		for (i = 0; i < num; i++)
		{
			as	= feat[i].alpha () + a;
			ap	= feat[i].phi () + a;
			rad	= data[i] * Math.sin (cone * 0.5);
			
			xo	= feat[i].rho () * Math.cos (ap) + (data[i] * 0.5) * Math.cos (as);
			yo	= feat[i].rho () * Math.sin (ap) + (data[i] * 0.5) * Math.sin (as);
			pos.set (pt.x() + xo, pt.y() + yo, pt.z() + feat[i].z ());

			mov.setIdentity ();
			mov.rotZ (as + (Math.PI / 2.0));
			mov.setTranslation (pos);
			mov.setScale (new Vector3d (rad, data[i], rad));
			
			rays[i].setTransform (mov);
		}
	}
}
