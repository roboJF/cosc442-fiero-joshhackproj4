package game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldOwnershipTest {
    private Tile[][][] tiles() {
        return new Tile[][][] {
                {{Tile.WALL, Tile.WALL}, {Tile.WALL, Tile.WALL}},
                {{Tile.WALL, Tile.WALL}, {Tile.WALL, Tile.STAIRS_DOWN}}
        };
    }

    @Test
    void changingAnInputCellDoesNotChangeTheWorld() {
        Tile[][][] input = tiles();
        World world = new World(input);

        input[0][0][0] = Tile.FLOOR;

        assertSame(Tile.WALL, world.tile(0, 0, 0));
        assertSame(Tile.STAIRS_DOWN, world.tile(1, 1, 1));
    }

    @Test
    void replacingAnInputRowDoesNotChangeTheWorld() {
        Tile[][][] input = tiles();
        World world = new World(input);

        input[0][0] = new Tile[] {Tile.FLOOR, Tile.FLOOR};

        assertSame(Tile.WALL, world.tile(0, 0, 0));
        assertSame(Tile.WALL, world.tile(0, 0, 1));
    }

    @Test
    void replacingAnInputPlaneDoesNotChangeTheWorldOrItsDimensions() {
        Tile[][][] input = tiles();
        World world = new World(input);

        input[0] = new Tile[][] {{Tile.FLOOR}};

        assertSame(Tile.WALL, world.tile(0, 0, 0));
        assertSame(Tile.WALL, world.tile(0, 1, 1));
        assertEquals(2, world.width());
        assertEquals(2, world.height());
        assertEquals(2, world.depth());
    }

    @Test
    void diggingChangesOnlyTheWorldThatOwnsTheTiles() {
        Tile[][][] input = tiles();
        World first = new World(input);
        World second = new World(input);

        first.dig(0, 0, 0);

        assertSame(Tile.FLOOR, first.tile(0, 0, 0));
        assertSame(Tile.WALL, second.tile(0, 0, 0));
        assertSame(Tile.WALL, input[0][0][0]);
    }

    @Test
    void eachNewWorldUsesTheInputStateAtTheTimeItIsBuilt() {
        Tile[][][] input = tiles();
        World first = new World(input);
        input[0][0][0] = Tile.FLOOR;
        World second = new World(input);

        assertSame(Tile.WALL, first.tile(0, 0, 0));
        assertSame(Tile.FLOOR, second.tile(0, 0, 0));

        second.dig(1, 0, 0);
        assertSame(Tile.WALL, first.tile(1, 0, 0));
        assertSame(Tile.WALL, input[1][0][0]);
    }
}
