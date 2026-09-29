/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

/**
 * What moves the joints of an articulated robot as it goes: given the
 * velocities the robot is told to have (m/s forward and to the left, rad/s
 * about the vertical) and where its camera is pointed (pan and tilt, rad), it
 * writes the angles of the joints into the kinematic model, a little further
 * along every {@link #step}. A walking model is named in the description of
 * the platform (WALKMODEL) by its class, which is built with the model of
 * the robot ({@link KineModel}) as its one argument.
 */
public interface WalkingModel
{
	/** The model whose joints it moves. */
	public KineModel model ();

	/** The speed asked of the robot: forward and to the left (m/s), about the vertical (rad/s). */
	public void setVelocities (double vlin, double vlat, double vrot);

	/** Where the camera is pointed: pan to the left and tilt up (rad). */
	public void setHead (double pan, double tilt);

	/** So much time goes by (s): the joints of the model move on. */
	public void step (double dt);

	/** Every joint where it is at rest. */
	public void stand ();

	/**
	 * How high the origin of the body is over the ground with this gait (m),
	 * the body pitched as {@link #pitch} says so that every foot is on it.
	 */
	public double height ();

	/**
	 * How the body pitches to stand on all its feet (rad, about y, positive nose
	 * down): a gait that carries the front feet higher under the body than the
	 * hind ones tilts the body forward. Zero when the feet are level.
	 */
	default public double pitch ()							{ return 0.0; }

	/**
	 * A walking model by the name of its class, built with a kinematic model;
	 * null when the class is not there or is not one.
	 */
	static public WalkingModel create (String className, KineModel model)
	{
		if ((className == null) || (className.trim ().length () == 0) || (model == null))		return null;
		try
		{
			Class<?>	c = Class.forName (className.trim ());

			return (WalkingModel) c.getConstructor (KineModel.class).newInstance (model);
		}
		catch (Throwable e)
		{
			System.out.println ("  [WalkingModel] Cannot build " + className + ": " + e);
			return null;
		}
	}
}
