package game;

import java.util.List;

public class Path {

	private static PathFinder pf = new PathFinder();
	
	private final List<Point> points;
	/** Returns copied coordinates, preserving null when no path was found. */
	public List<Point> points() {
		if (points == null)
			return null;
		return points.stream().map(point -> new Point(point.x, point.y, point.z)).toList();
	}
	
	public Path(Creature creature, int x, int y){
		points = pf.findPath(creature, new Point(creature.x, creature.y, creature.z), new Point(x, y, creature.z), 300);
	}
}
