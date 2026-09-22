/*
 * (c) 2026 Humberto Martinez Barbera
 */
package tc.shared.lps.lpo;

/**
 * Where what an LPO stands for comes from.
 */
public enum LPOSource
{
	/** The a priori map of the world (doors, rooms, home...). */
	MAP,
	/** A perception: the sensors (readings, groups, scans...) or what is recognised from them (ball, nets...). */
	PERCEPT,
	/** What comes from the coordination with other robots (the robots of a team). */
	COORDINATION,
	/** Something the robot itself puts there (goal, look-ahead point, alignment...). */
	ARTIFACT
}
