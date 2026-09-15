/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot;

import java.awt.Graphics2D;
import java.awt.Image;

import wucore.utils.geom.Line2;
import wucore.utils.image.PlanImage;

/**
 * The bitmap a robot description may carry (its <code>image</code>) drawn in a
 * plan view: centred on the centre of the robot, scaled to the size of its
 * bumpers and turned with it.
 *
 * The reading and the drawing are those of {@link PlanImage}; what belongs to a
 * robot is the size the image is given.
 */
public class RobotImage
{
	/** The image of a description, or null when it has none or it cannot be read. */
	static public Image get (String path)						{ return PlanImage.get (path); }

	/** Forgets what was read (the editor changing the image of a robot, for instance). */
	static public void flush ()									{ PlanImage.flush (); }
	static public void flush (String path)						{ PlanImage.flush (path); }

	/**
	 * Size the image is drawn at: the size of the box the bumpers occupy, or of
	 * the drawing of the robot when it has no bumpers, or its diameter when it
	 * has neither. The box is centred on the robot, so the image is too.
	 *
	 * @return {width, height} in metres, or null when there is nothing to size it with
	 */
	static public double[] size (Line2[] bumpers, Line2[] icon, double radius)
	{
		double[]	b = PlanImage.bounds (bumpers);

		if (b == null)		b = PlanImage.bounds (icon);
		if (b == null)		return (radius > 0.0) ? new double[] { 2 * radius, 2 * radius } : null;
		return new double[] { b[2] - b[0], b[3] - b[1] };
	}

	/**
	 * Draws the image of a robot on a plan view.
	 *
	 * @param g			where to draw
	 * @param img		the image ({@link #get(String)})
	 * @param px, py	centre of the robot, in pixels
	 * @param wpx, hpx	size the image takes, in pixels
	 * @param angle		heading of the robot (rad); the X axis of the robot is the width of the image
	 */
	static public void draw (Graphics2D g, Image img, double px, double py, double wpx, double hpx, double angle)
	{
		PlanImage.draw (g, img, px, py, wpx, hpx, angle);
	}
}
