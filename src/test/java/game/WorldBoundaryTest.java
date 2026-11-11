package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class WorldBoundaryTest {
    private World world;

    @BeforeEach
    void setUp() {
        // Different axis lengths expose checks that use the wrong dimension.
        Tile[][][] tiles = new Tile[4][5][3];
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 3; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                }
            }
        }
        tiles[0][2][1] = Tile.WALL;
        tiles[3][2][1] = Tile.STAIRS_UP;
        tiles[2][0][1] = Tile.STAIRS_DOWN;
        tiles[2][4][1] = Tile.WALL;
        tiles[2][2][0] = Tile.STAIRS_UP;
        tiles[2][2][2] = Tile.STAIRS_DOWN;
        world = new World(tiles);
    }

    @Test
    void exposesTheDimensionsOfANonCubicWorld() {
        assertAll(
                () -> assertEquals(4, world.width()),
                () -> assertEquals(5, world.height()),
                () -> assertEquals(3, world.depth()));
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 2, 1", "4, 2, 1",
            "2, -1, 1", "2, 5, 1",
            "2, 2, -1", "2, 2, 3"
    })
    void returnsBoundsJustOutsideEachAxisWithOtherCoordinatesValid(int x, int y, int z) {
        assertSame(Tile.BOUNDS, world.tile(x, y, z));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 2, 1, WALL", "3, 2, 1, STAIRS_UP",
            "2, 0, 1, STAIRS_DOWN", "2, 4, 1, WALL",
            "2, 2, 0, STAIRS_UP", "2, 2, 2, STAIRS_DOWN"
    })
    void returnsTheActualTileAtBothEndsOfEachAxis(int x, int y, int z, Tile expected) {
        assertSame(expected, world.tile(x, y, z));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0", "0, 0, 2", "0, 4, 0", "0, 4, 2",
            "3, 0, 0", "3, 0, 2", "3, 4, 0", "3, 4, 2"
    })
    void allEightCornersAreInsideTheWorld(int x, int y, int z) {
        assertSame(Tile.FLOOR, world.tile(x, y, z));
    }

    @Test
    void returnsAnInteriorTile() {
        assertSame(Tile.FLOOR, world.tile(2, 2, 1));
    }
}
