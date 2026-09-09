/*
 * Created on 07-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.gui.visualization.objects;

import javax.media.j3d.*;
import javax.vecmath.*;

import tc.vrobot.*;
import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Scan3D extends BranchGroup
{
	protected TransformGroup[]		scans;

	private Vector3d					pos = new Vector3d ();
	private Transform3D				mov = new Transform3D ();
	private Point3d					p1 = new Point3d ();
	private Point3d					p2 = new Point3d ();
	private Point3d					p3 = new Point3d (0.0, 0.0, 0.0);
	
	protected int					num;
	protected SensorPos[]				feat;
	protected double					cone;
	protected int					rays;

	public Scan3D (SensorPos[] feat, double cone, int rays, Color3f color, int num)
	{
		Appearance				app;
		ColoringAttributes		col;
		TransparencyAttributes	trs;
		Shape3D					scan;
		TriangleArray			pts;
		int						i, j;
		
		this.feat	= feat;
		this.cone	= cone;
		this.rays	= rays;
		this.num		= num;
		
		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);

		scans	= new TransformGroup[num];
		app		= new Appearance ();
		col		= new ColoringAttributes (color, ColoringAttributes.SHADE_GOURAUD);
		trs		= new TransparencyAttributes (TransparencyAttributes.NICEST, 0.75f);
		app.setColoringAttributes (col);
		app.setTransparencyAttributes (trs);
		
		for (i = 0; i < num; i++)
		{
			scans[i] = new TransformGroup ();
			scans[i].setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
			scans[i].setCapability (TransformGroup.ALLOW_CHILDREN_READ);
			
			mov.setIdentity ();
			mov.setTranslation (new Vector3d (0.0, 0.0, -100.0));
			mov.setScale (0.0);
			scans[i].setTransform (mov);
			
			pts		= new TriangleArray (rays*3, GeometryArray.COORDINATES | GeometryArray.NORMALS | GeometryArray.COLOR_3);
			pts.setCapability (TriangleArray.ALLOW_COORDINATE_WRITE);
			scan		= new Shape3D (pts, app);
			scan.setCapability (Shape3D.ALLOW_GEOMETRY_READ);
			
			for (j = 0; j < rays; j++)
			{
				pts.setNormal (j*3, new Vector3f (0.0f, 0.0f, 1.0f));
				pts.setNormal (j*3+1, new Vector3f (0.0f, 0.0f, 1.0f));
				pts.setNormal (j*3+2, new Vector3f (0.0f, 0.0f, 1.0f));
				
				pts.setColor (j*3, color);
				pts.setColor (j*3+1, color);
				pts.setColor (j*3+2, color);
			}
			
			scans[i].addChild (scan);
			addChild (scans[i]);
		}
	}
	
	public void move (double[][] data, Point3 pt, double a)
	{
		Shape3D				scan;
		TriangleArray		pts;
		int					i, j;
		double				hcone, delta, k;
		double				xo, yo;
		double				alpha, phi;
		double				xi, yi, xf, yf;
		
		for (i = 0; i < num; i++)
		{
			scan		= (Shape3D) scans[i].getChild (i);
			pts		= (TriangleArray) scan.getGeometry ();
			
			hcone	= cone * 0.5;		
			delta	= cone / (double) (rays - 1);
			
			xi		= 0.0;
			yi		= 0.0; 			
			for (j = 0, k = -hcone; j < rays; j++, k+=delta)
			{
				xf 	= data[i][j] * Math.cos (k);
				yf 	= data[i][j] * Math.sin (k); 
				
				p1.set (xi, yi, 0.0);
				p2.set (xf, yf, 0.0);
				
				pts.setCoordinate (j*3, p1);
				pts.setCoordinate (j*3+1, p2);
				pts.setCoordinate (j*3+2, p3);
				
				xi	= xf;
				yi	= yf;
			}
			
			alpha	= feat[i].alpha () + a;
			phi		= feat[i].phi () + a;
			
			xo		= feat[i].rho () * Math.cos (phi);
			yo		= feat[i].rho () * Math.sin (phi);
			pos.set (pt.x() + xo, pt.y() + yo, pt.z()+feat[i].z ());
			
			mov.setIdentity ();
			mov.rotZ (alpha);
			mov.setTranslation (pos);
			mov.setScale (new Vector3d (1.0, 1.0, 1.0));
			scans[i].setTransform (mov);
		}
	}
}
