/*
 * (c) 2026 Humberto Martinez Barbera
 */
package tc.shared.lps.lpo;

/**
 * Where what an LPO stands for comes from.
 */
public enum LpoSource
{
	/** The a priori map of the world (doors, rooms, home...). */
	MAP,
	/** A perception: the sensors (readings, groups, scans...) or what is recognised from them (ball, nets...). */
	PERCEPT,
	/** An object anchored from outside the robot (the other robots of a team). */
	ANCHOR,
	/** Something the robot itself puts there (goal, look-ahead point, alignment...). */
	ARTIFACT
}
