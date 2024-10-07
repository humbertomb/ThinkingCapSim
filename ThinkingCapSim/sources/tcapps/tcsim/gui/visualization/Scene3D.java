/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2004 Humberto Martinez
 */
 
package tcapps.tcsim.gui.visualization;

import java.util.*;
import java.awt.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import com.mnstarfire.loaders3d.*;
import com.sun.j3d.loaders.*;
import com.sun.j3d.utils.image.*;
import com.sun.j3d.utils.universe.*;

import devices.pos.*;

public class Scene3D extends Object
{
	// View modification modes
	static public final int			M_MOVE		= 0;
	static public final int			M_ROTATE		= 1;
	static public final int			M_ZOOM		= 2;
	
	static public final int			KEYMOVE		= 10;
	static public final int			KEYZOOM		= 10;
	static public final int			KEYROTATE	= 10;

	// Dafult lighting values
	static public final float			FRONT_INT	= 1.0f;
	static public final float			BACK_INT		= 0.8f; 
	static public final float			TOP_INT		= 0.65f; 
	static public final float			AMBIENT_INT	= 1.0f; 
	
	protected Canvas3D				canvas;
	protected SimpleUniverse			universe		= null;
	protected BranchGroup				root;
	protected TransformGroup			scene;
	
	// Viewpoint related stuff
	protected Transform3D				view;
	protected Point3d				eye;
	protected Point3d				focus;
	protected int					prevx;
	protected int					prevy;
	
	// Current point of view parameters
	protected double					len			= 20.0;
	protected double					rho			= 0.0;
	protected double					theta		= 0.0;

	// Lighting stuff
	protected DirectionalLight		lightFront;
	protected DirectionalLight		lightBack;
	protected DirectionalLight		lightTop;
	protected AmbientLight			lightAmbient;
	
	// Textures and coloring
	private Hashtable				texCache;		// Textures cache
	private Hashtable				objCache;		// 3D objects cache

	/* Constructors */
	public Scene3D (Canvas3D canvas) 
	{
		Background			bkg;
		BoundingSphere 		bounds;
		
		// Initialise some variables
		this.canvas	= canvas;
		
		// Caches
		texCache	= new Hashtable ();	
		objCache	= new Hashtable ();	
		
		view		= new Transform3D ();
		focus	= new Point3d (0.0, 0.0, 0.0);
		eye		= new Point3d (0.0, 0.0, 0.0);
		
		// Create a simple scene.
		root		= new BranchGroup ();
		scene	= new TransformGroup ();
		scene.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		scene.setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
		scene.setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		root.addChild (scene);
		
		// Set up the background color
		bounds	= new BoundingSphere (new Point3d (0.0,0.0,0.0), 100.0);
		bkg		= new Background (Color3D.ambientblue);
		bkg.setApplicationBounds (bounds);
		scene.addChild (bkg);

		// Create and setup lighting system
		lightFront	= new DirectionalLight(true, new Color3f(FRONT_INT,FRONT_INT,FRONT_INT), new Vector3f (100.0f, 100.0f, 20.0f));
		lightFront.setInfluencingBounds(new BoundingSphere(new Point3d(0.0f,0.0f,0.0f),300.0f));
		lightFront.setCapability(Light.ALLOW_STATE_WRITE);
		lightFront.setCapability(Light.ALLOW_COLOR_WRITE);
		lightFront.setCapability(Light.ALLOW_COLOR_READ);

		lightBack	= new DirectionalLight(true, new Color3f(BACK_INT,BACK_INT,BACK_INT), new Vector3f (-100.0f, -100.0f, 20.0f));
		lightBack.setInfluencingBounds(new BoundingSphere(new Point3d(0.0f,0.0f,0.0f),300.0f));
		lightBack.setCapability(Light.ALLOW_STATE_WRITE);
		lightBack.setCapability(Light.ALLOW_COLOR_WRITE);
		lightBack.setCapability(Light.ALLOW_COLOR_READ);

		lightTop		= new DirectionalLight(true, new Color3f(TOP_INT,TOP_INT,TOP_INT), new Vector3f (0.0f, 0.0f, -100.0f));
		lightTop.setInfluencingBounds(new BoundingSphere(new Point3d(0.0f,0.0f,0.0f),300.0f));
		lightTop.setCapability(Light.ALLOW_STATE_WRITE);
		lightTop.setCapability(Light.ALLOW_COLOR_WRITE);
		lightTop.setCapability(Light.ALLOW_COLOR_READ);

		lightAmbient = new AmbientLight(true,new Color3f(AMBIENT_INT,AMBIENT_INT,AMBIENT_INT));
		lightAmbient.setInfluencingBounds(new BoundingSphere(new Point3d(0.0f,0.0f,0.0f),300.0f));
		lightAmbient.setCapability(Light.ALLOW_STATE_WRITE);
		lightAmbient.setCapability(Light.ALLOW_COLOR_WRITE);
		lightAmbient.setCapability(Light.ALLOW_COLOR_READ);

		scene.addChild (lightFront);
		scene.addChild (lightBack);
		scene.addChild (lightTop);
		scene.addChild (lightAmbient);
		
		// Attach the scene to the virtual universe
		universe = new SimpleUniverse (canvas);
		universe.getViewingPlatform ().setNominalViewingTransform ();
		universe.addBranchGraph (root);
	}
  
	// Instance methods
	public Appearance getCachedTexture (String name, boolean horiz)
	{
		Appearance			app;
		ColoringAttributes	col;
		TextureLoader		texl;
		Texture 				tex;
		TexCoordGeneration	txtc;		// Attributes to allow texture scale and repetition
		TextureAttributes	txta;		// Attribute to allow lightning textures
		Material				mat;
		
		if (texCache.containsKey(name))
			app = (Appearance) texCache.get(name);
		else
		{
			System.out.println ("  [Scene3D] Loading texture <"+name+">");
			
			app 		= new Appearance();
			texl		= new TextureLoader (name, canvas);
			txta		= new TextureAttributes();
			txta.setTextureMode(TextureAttributes.MODULATE);
			if (horiz)
				txtc 	= new TexCoordGeneration(TexCoordGeneration.TEXTURE_COORDINATE_2,TexCoordGeneration.OBJECT_LINEAR,new Vector4f(1.0f,0.0f,0.0f,0.0f),new Vector4f(0.0f,1.0f,0.0f,0.0f));
			else
				txtc 	= new TexCoordGeneration(TexCoordGeneration.TEXTURE_COORDINATE_2,TexCoordGeneration.OBJECT_LINEAR,new Vector4f(1.0f,0.0f,0.0f,0.0f),new Vector4f(0.0f,0.0f,1.0f,0.0f));
			col		= new ColoringAttributes (Color3D.gray, ColoringAttributes.SHADE_GOURAUD);
			mat		= new Material (Color3D.gray, Color3D.gray, Color3D.gray, Color3D.white, 100.0f);
			app.setColoringAttributes (col);
			tex = texl.getTexture();
			tex.setBoundaryModeS(Texture.WRAP);
			tex.setBoundaryModeT(Texture.WRAP);
			app.setTexture (tex);
			app.setMaterial (mat);
			app.setTexCoordGeneration(txtc);
			app.setTextureAttributes(txta);
			
			texCache.put(name, app);
		}
		
		return app;
	}

	public TransformGroup getCachedObject (String name, Color color)
	{
		TransformGroup	tgroup;
		TransformGroup	tbranch;
		Transform3D		trans;
		BranchGroup		branch;

		if (name == null)				return null;
		
		if (objCache.containsKey(name))
		{
			branch	= (BranchGroup) objCache.get (name);
			branch	= (BranchGroup) branch.cloneTree (true);
		}
		else
		{
			Scene 			group;
			Loader3DS		loader;

			System.out.println ("  [Scene3D] Loading 3D object <"+name+">");
						
			// Load 3D object
			branch	= new BranchGroup ();
			try 
			{					
				loader	= new Loader3DS ();
				group	= loader.load (name); 
				branch	= group.getSceneGroup ();
				branch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);					
			} catch (Exception e) { e.printStackTrace(); }		

			objCache.put (name, branch.cloneTree (true));
		}
		
		// Transform object references to Java3D
		trans 	= new Transform3D ();
		trans.rotX (Math.PI / 2.0f);
		tbranch 	= new TransformGroup ();
		tbranch.setTransform (trans);
		tbranch.addChild (branch);		

		// If color has to be overriden, apply new color
		if (color != null)
			traverse (branch, Color3D.toColor (color));

		// Create export group
		tgroup	= new TransformGroup ();
		tgroup.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		tgroup.addChild (tbranch);

		return tgroup;
	}
	
	/** Visit all the Shape3D objects in a Group node and 
	 *	applies them a material with the specified color
	 */
	protected void traverse (Group bg, Color3f objcolor) 
	{
		Enumeration e = bg.getAllChildren();		
		
		while (e.hasMoreElements())
		{
			Object o = e.nextElement();
			
			if (o instanceof Shape3D)
			{
				Appearance app = new Appearance();
				Material mat		= new Material (objcolor, objcolor, objcolor, Color3D.black, 64.0f);
				mat.setLightingEnable (true);
				app.setMaterial (mat);

				((Shape3D) o).setAppearance (app);					
			}
			else if (o instanceof Group) 
				traverse ((Group) o, objcolor);
		}
	}
	
	public void setViewpoint ()
	{
		double			x, y, z;
		
		x	= len * Math.cos (theta);
		y	= len * Math.sin (theta);
		z	= len * Math.sin (rho);

		eye.set (x, y, z);
		eye.add (focus);
		view.lookAt (eye, focus, new Vector3d (0, 0, 1.0f));
		scene.setTransform (view);
	}
	
	public void mouseDown (int x, int y) 
	{
		prevx = x;
		prevy = y;
	}

	public void keypress(int mode,int x,int y){
		mouseDrag(mode,prevx-x,prevy+y);
	}
	public void mouseDrag (int mode, int newx, int newy) 
	{
		int			dx, dy;
		double		angulo;
		Point3d		myneweye;
		Position	pos;
		
		dx	= newx - prevx;
		dy	= newy - prevy;
				
		
		switch (mode){
		case M_MOVE:
//			Angulo que forma la recta que pasa por los puntos eye y focus con respecto a la recta y=0
			angulo=Math.atan2(focus.y-eye.y,focus.x-eye.x)-Math.PI/2;
//			Nueva posicion de eye
			myneweye=new Point3d((-1*dx),dy,eye.z);
//			Coordenadas globales de la nueva posicion de eye
			pos=Transform2.toGlobal(myneweye.x,myneweye.y,0,eye.x,eye.y,angulo);
 
			focus.x	+= (pos.x()-eye.x) * 0.05;
			focus.y	+= (pos.y()-eye.y) * 0.05;
			
			break;
			
		case M_ROTATE:
			theta	+= dx * 0.002;
			rho		+= dy * 0.002;
			
			rho		= Math.max (Math.min (rho, Math.PI), -Math.PI);
			break;
			
		case M_ZOOM:
			len		+= dy * 0.05;
			len		= Math.max (len, 0.1);
			break;
			
		default:
		}
		
		setViewpoint ();

		prevx = newx;
		prevy = newy;
	}
	
/*	Point3d iorig, idest; // Initial and final points of drag movement in image plate coordinates
	Point3d vorig, vdest; // Initial and final points of drag movement in virtual universe coordinates
	Transform3D ipToVu; // Transformation from image plate coordinates to virtual universe coordinates

	iorig= new Point3d();
	idest = new Point3d();
	vorig = new Point3d();
	vdest = new Point3d();
	ipToVu = new Transform3D();

	canvas.getPixelLocationInImagePlate(prevx,prevy,iorig);
	canvas.getPixelLocationInImagePlate(newx,newy,idest);
	canvas.getImagePlateToVworld(ipToVu);
	ipToVu.transform(iorig,vorig);
	ipToVu.transform(idest,vdest);
	view.transform(vorig);
	view.transform(vdest);
	focus.add(new Vector3d(vdest.x-vorig.x,vdest.y-vorig.y,0.0));
*/
	/** Sets the front light intensity level. Must be between 0.0 and 1.0 */
	public void setFrontLightIntensity(float intensity)
	{
		lightFront.setColor(new Color3f(intensity,intensity,intensity));
	}
	
	/** Returns the front light intensity. Its value will be between 0.0 and 1.0 */
	public float getFrontLightIntensity()
	{
		Color3f color = new Color3f();
		lightFront.getColor(color);
		return color.x;
	}

	/** Sets the back light intensity level. Must be between 0.0 and 1.0 */
	public void setBackLightIntensity(float intensity)
	{
		lightBack.setColor(new Color3f(intensity,intensity,intensity));
	}

	/** Returns the back light intensity. Its value will be between 0.0 and 1.0 */
	public float getBackLightIntensity()
	{
		Color3f color = new Color3f();
		lightBack.getColor(color);
		return color.x;
	}
	
	/** Sets the top light intensity level. Must be between 0.0 and 1.0 */
	public void setTopLightIntensity(float intensity)
	{
		lightTop.setColor(new Color3f(intensity,intensity,intensity));
	}

	/** Returns the top light intensity. Its value will be between 0.0 and 1.0 */
	public float getTopLightIntensity()
	{
		Color3f color = new Color3f();
		lightTop.getColor(color);
		return color.x;
	}
	
	/** Sets the ambient light intensity level. Must be between 0.0 and 1.0 */
	public void setAmbientLightIntensity(float intensity)
	{
		lightAmbient.setColor(new Color3f(intensity,intensity,intensity));
	}

	/** Returns the ambient light intensity. Its value will be between 0.0 and 1.0 */
	public float getAmbientLightIntensity()
	{
		Color3f color = new Color3f();
		lightAmbient.getColor(color);
		return color.x;
	}
	
	/** Enables or disables the frontal light */
	public void enableFrontLight(boolean enable)
	{
		lightFront.setEnable(enable);
	}
	
	/** Enables or disables the backwards light */
	public void enableBackLight(boolean enable)
	{
		lightBack.setEnable(enable);
	}
	
	/** Enables or disables the backwards light */
	public void enableTopLight(boolean enable)
	{
		lightTop.setEnable(enable);
	}
	
	/** Enables or disables the ambient light */
	public void enableAmbientLight(boolean enable)
	{
		lightAmbient.setEnable(enable);
	}
}

