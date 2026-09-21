/**
 * A simple convenience class to produce 3D plots. This is a 
 * heavyweight object. Based on Joy Kyriakopulos' LegoPlot
 * 
 * @author Hu8mberto Martinez Barbera
 */

package tcrob.umu.quaky2.gui.ctables.colspace;

import javax.media.j3d.*;
import javax.vecmath.*;

import tclib.vision.chaos.channels.*;

import com.sun.j3d.utils.geometry.*;

public class SpaceCylindricalBuilder extends SpaceOrtoBuilder
{	
	static public final int				DIVS = 6;
	static public final double			SMOOTH_CREASE_ANGLE	= 22.0/7.0;
	
	protected Node drawPoint (int co1, int co2, int co3, int count, Color3f color)
	{
		double				x, y;
		double				rho, theta;
		float				side;
		Sphere				point;
		Appearance			app;
		TransformGroup		node;
		Transform3D			trans;
		ColoringAttributes	cattrs;
				
		cattrs	= new ColoringAttributes (color, ColoringAttributes.SHADE_FLAT);
		app		= new Appearance();
		app.setColoringAttributes (cattrs);
		
		side		= SIZE * (float) Math.log (Math.exp (1.0) + count * 3f);
		point	= new Sphere (side, app);	
		
		rho		= 0.5 * ((double) co2 / 255.0);
		theta	= ((double) co1 / 255.0) * Math.PI * 2.0;
		x		= rho * Math.cos (theta);
		y		= rho * Math.sin (theta);
		
		trans = new Transform3D();
		trans.setTranslation (new Vector3d (x, y, (double) co3 / 255.0));	
		
		node = new TransformGroup (trans);
		node.addChild (point);
		
		return node;
	}	
		
	protected Node drawCluster (Channel ch)
	{
		float					val0, val1;
		double					rho0, theta0;
		double					rho1, theta1;
		Color3f					color;
		ColorPrism				cprism;
		Appearance				app;
		BranchGroup				group;
		ColoringAttributes		cattrs;
		TransparencyAttributes	tattrs;
		
		cprism	= (ColorPrism) ch.getCluster ();
		
		rho0	= 0.5 * ((double) cprism.getMin1 () / 255.0);
		theta0	= ((double) cprism.getMin0 () / 255.0) * Math.PI * 2.0;
		val0	= (float) cprism.getMin2 () / 255.0f;

		rho1	= 0.5 * ((double) cprism.getMax1 () / 255.0);
		theta1	= ((double) cprism.getMax0 () / 255.0) * Math.PI * 2.0;
		val1	= (float) cprism.getMax2 () / 255.0f;

		color	= rgbToColor (ch.color.getRGB ());
		cattrs	= new ColoringAttributes (color, ColoringAttributes.SHADE_FLAT);
		tattrs	= new TransparencyAttributes (TransparencyAttributes.NICEST, 0.70f);
		app		= new Appearance();
		app.setColoringAttributes (cattrs);
		app.setTransparencyAttributes (tattrs);

		group = new BranchGroup ();	
		group.addChild (new Shape3D (drawPlaneR (rho0, theta0, theta1, val0, val1), app)); 		
		group.addChild (new Shape3D (drawPlaneR (rho1, theta1, theta0, val0, val1), app)); 		
		group.addChild (new Shape3D (drawPlaneV (rho0, rho1, theta1, theta0, val0), app)); 		
		group.addChild (new Shape3D (drawPlaneV (rho0, rho1, theta0, theta1, val1), app)); 		
		group.addChild (new Shape3D (drawPlaneT (rho0, rho1, theta0, val0, val1), app)); 		
		group.addChild (new Shape3D (drawPlaneT (rho1, rho0, theta1, val0, val1), app)); 		

		return group;
	}	
	
	private GeometryArray drawPlaneR (double rho, double theta0, double theta1, float val0, float val1)
	{
		float					x0, x1, x2, x3;
		float					y0, y1, y2, y3;
		double					th, thstep;
		GeometryInfo 			geom;
		Point3f[]				coordArray;

		coordArray	= new Point3f[DIVS*4];		

		thstep = (theta1 - theta0) / (double) DIVS;
		th = theta0;
		for (int i = 0; i < DIVS; th += thstep, i++)
		{
			x0 = (float) (rho * Math.cos (th));
			y0 = (float) (rho * Math.sin (th));
	
			x1 = (float) (rho * Math.cos (th+thstep));
			y1 = (float) (rho * Math.sin (th+thstep));
			
			x2 = (float) (rho * Math.cos (th+thstep));
			y2 = (float) (rho * Math.sin (th+thstep));
	
			x3 = (float) (rho * Math.cos (th));
			y3 = (float) (rho * Math.sin (th));
	
			coordArray[i*4+0] = new Point3f (x3, y3, val1);
			coordArray[i*4+1] = new Point3f (x2, y2, val1);
			coordArray[i*4+2] = new Point3f (x1, y1, val0);
			coordArray[i*4+3] = new Point3f (x0, y0, val0);
		}

		geom = new GeometryInfo (GeometryInfo.QUAD_ARRAY);
		geom.setCoordinates (coordArray);
		new NormalGenerator (SMOOTH_CREASE_ANGLE).generateNormals (geom);		

		return geom.getGeometryArray ();
	}

	private QuadArray drawPlaneT (double rho0, double rho1, double theta, float val0, float val1)
	{
		float					x0, x1, x2, x3;
		float					y0, y1, y2, y3;
		QuadArray				plane;

		x0 = (float) (rho1 * Math.cos (theta));
		y0 = (float) (rho1 * Math.sin (theta));

		x1 = (float) (rho0 * Math.cos (theta));
		y1 = (float) (rho0 * Math.sin (theta));
		
		x2 = (float) (rho0 * Math.cos (theta));
		y2 = (float) (rho0 * Math.sin (theta));

		x3 = (float) (rho1 * Math.cos (theta));
		y3 = (float) (rho1 * Math.sin (theta));

		plane = new QuadArray (4, QuadArray.COORDINATES);
		plane.setCoordinate (0, new Point3f (x3, y3, val1));
		plane.setCoordinate (1, new Point3f (x2, y2, val1));
		plane.setCoordinate (2, new Point3f (x1, y1, val0));
		plane.setCoordinate (3, new Point3f (x0, y0, val0));
		
		return plane;
	}
	
	private GeometryArray drawPlaneV (double rho0, double rho1, double theta0, double theta1, float val)
	{
		float					x0, x1, x2, x3;
		float					y0, y1, y2, y3;
		double					th, thstep;
		GeometryInfo 			geom;
		Point3f[]				coordArray;

		coordArray	= new Point3f[DIVS*4];		

		thstep = (theta1 - theta0) / (double) DIVS;
		th = theta0;
		for (int i = 0; i < DIVS; th += thstep, i++)
		{	
			x0 = (float) (rho0 * Math.cos (th));
			y0 = (float) (rho0 * Math.sin (th));
	
			x1 = (float) (rho0 * Math.cos (th+thstep));
			y1 = (float) (rho0 * Math.sin (th+thstep));
			
			x2 = (float) (rho1 * Math.cos (th+thstep));
			y2 = (float) (rho1 * Math.sin (th+thstep));
	
			x3 = (float) (rho1 * Math.cos (th));
			y3 = (float) (rho1 * Math.sin (th));
	
			coordArray[i*4+0] = new Point3f (x3, y3, val);
			coordArray[i*4+1] = new Point3f (x2, y2, val);
			coordArray[i*4+2] = new Point3f (x1, y1, val);
			coordArray[i*4+3] = new Point3f (x0, y0, val);		
		}

		geom = new GeometryInfo (GeometryInfo.QUAD_ARRAY);
		geom.setCoordinates (coordArray);
		new NormalGenerator (SMOOTH_CREASE_ANGLE).generateNormals (geom);		

		return geom.getGeometryArray ();
	}
}

