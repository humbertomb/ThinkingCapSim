/*
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import tclib.utils.pos.Position;

/**
 * The team a soccer robot plays in, and the field as the team sees it.
 *
 * What a robot knows of the field is said from the side of the BLUE team: its
 * Net1 is the one it attacks, at the far end of the field (+y), its own net
 * (Net2) behind it (-y), its Landmark1 on its left (-x), and the robots of its
 * uniform are Robot1. That is the field of the localisation
 * ({@link tcrob.umu.soccer.gm.Localisation}) and of the scripts.
 *
 * A robot of the RED team plays the other way: its Net1 is the other net, its
 * Landmark1 the other landmark, and its mates wear the other uniform. The field
 * it knows is the same one turned half round about the centre: its coordinates
 * are the ones of the world (where the simulator puts the robots, the start
 * points of the world) with x and y the other way and the headings turned by
 * 180 degrees. For a BLUE robot the two are one and the same.
 *
 * The team is the one of the description of the robot (V3DTEAM, team in the
 * .robot file: BLUE or RED; none is BLUE), which every module is given with
 * CONFIG.
 */
public class SoccerTeam
{
	static public final String		BLUE	= "BLUE";
	static public final String		RED		= "RED";

	/** Whether the robot of some properties (the ones of CONFIG) plays in the RED team. */
	static public boolean red (java.util.Properties props)
	{
		String		t = (props != null) ? props.getProperty ("V3DTEAM") : null;

		return (t != null) && t.trim ().equalsIgnoreCase (RED);
	}

	/**
	 * A position of the world in the field of a team, or the other way round
	 * (the two are the same turn): as it is for the BLUE team, turned half
	 * round for the RED one -- x and y the other way, the heading turned by 180
	 * degrees, and the covariances of the heading with x and with y the other
	 * way too. A new position; the one given is left as it is.
	 */
	static public Position field (Position p, boolean red)
	{
		Position	q;

		if (p == null)				return null;
		q	= new Position (p);
		if (!red)					return q;
		q.set (-p.x (), -p.y (), p.z (), p.alpha + Math.PI, p.valid);
		if (p.uncert != null)
		{
			double[][]	u = new double[p.uncert.length][];

			for (int i = 0; i < u.length; i++)		u[i] = p.uncert[i].clone ();
			if ((u.length >= 3) && (u[0].length >= 3) && (u[1].length >= 3) && (u[2].length >= 3))
			{
				u[0][2]	= -u[0][2];		u[2][0]	= -u[2][0];
				u[1][2]	= -u[1][2];		u[2][1]	= -u[2][1];
			}
			q.set (u);
		}
		return q;
	}
}
