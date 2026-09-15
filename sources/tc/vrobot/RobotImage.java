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
 * plan view: over the box the drawing of the robot occupies, scaled to it on
 * each axis and turned with the robot.
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
	 * Box the image is drawn over, in the frame of the robot: the box the drawing
	 * of the robot occupies, as for the objects of a world, or the one its radius
	 * gives when it has no drawing. It is not necessarily centred on the robot (a
	 * fork lift is a long way in front of its axle), so the image is placed on the
	 * centre of the box and not on the centre of the robot.
	 *
	 * @return {minx, miny, maxx, maxy} in metres, or null when there is nothing to measure
	 */
	static public double[] box (Line2[] icon, double radius)
	{
		double[]	b = PlanImage.bounds (icon);

		if (b == null)		return (radius > 0.0) ? new double[] { -radius, -radius, radius, radius } : null;
		return b;
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
