package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateOwnershipTest {
    private List<Point> linePoints() {
        return List.of(new Point(0, 0, 0), new Point(1, 0, 0), new Point(2, 0, 0));
    }

    private List<Point> pathPoints() {
        return List.of(new Point(1, 0, 0), new Point(2, 0, 0));
    }

    private Path path(boolean blocked, int destination) {
        Tile[][][] tiles = {{{Tile.FLOOR}}, {{blocked ? Tile.WALL : Tile.FLOOR}}, {{Tile.FLOOR}}};
        World world = new World(tiles);
        Creature creature = new Creature(world, '@', Color.WHITE, "player", 100, 10, 5);
        world.add(creature);
        return new Path(creature, destination, 0);
    }

    @Test
    void lineSnapshotPreservesOrderAndRejectsStructuralChanges() {
        Line line = new Line(0, 0, 2, 0);
        List<Point> snapshot = line.getPoints();
        assertEquals(linePoints(), snapshot);

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Point(3, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, new Point(9, 9, 9)));
        assertEquals(linePoints(), line.getPoints());
    }

    @Test
    void changingReturnedCoordinatesDoesNotChangeTheLineOrAnotherSnapshot() {
        Line line = new Line(0, 0, 2, 0);
        List<Point> snapshot = line.getPoints();
        List<Point> anotherSnapshot = line.getPoints();

        snapshot.get(1).x = 90;
        snapshot.get(1).y = 91;
        snapshot.get(1).z = 92;

        assertEquals(linePoints(), line.getPoints());
        assertEquals(linePoints(), anotherSnapshot);
    }

    @Test
    void lineIteratorDoesNotExposeInternalPointsOrAllowRemoval() {
        Line line = new Line(0, 0, 2, 0);
        Iterator<Point> iterator = line.iterator();
        Point first = iterator.next();
        first.x = 90;
        first.y = 91;
        first.z = 92;

        assertThrows(UnsupportedOperationException.class, iterator::remove);

        List<Point> anotherIteration = new ArrayList<>();
        line.forEach(anotherIteration::add);
        assertEquals(linePoints(), anotherIteration);
        assertEquals(linePoints(), line.getPoints());
    }

    @Test
    void pathSnapshotPreservesOrderAndRejectsStructuralChanges() {
        Path path = path(false, 2);
        List<Point> snapshot = path.points();
        assertEquals(pathPoints(), snapshot);

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Point(3, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, new Point(9, 9, 9)));
        assertEquals(pathPoints(), path.points());
    }

    @Test
    void changingReturnedCoordinatesDoesNotChangeThePathOrAnotherSnapshot() {
        Path path = path(false, 2);
        List<Point> snapshot = path.points();
        List<Point> anotherSnapshot = path.points();

        snapshot.get(0).x = 90;
        snapshot.get(0).y = 91;
        snapshot.get(0).z = 92;

        assertEquals(pathPoints(), path.points());
        assertEquals(pathPoints(), anotherSnapshot);
    }

    @Test
    void unavailablePathStillReturnsNull() {
        assertNull(path(true, 2).points());
    }

    @Test
    void startingAtTheDestinationStillReturnsAnEmptyReadOnlyPath() {
        List<Point> points = path(false, 0).points();

        assertTrue(points.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> points.add(new Point(1, 0, 0)));
    }
}
