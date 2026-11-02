package game;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WorldTest {

    //this was an interesting method to pick but man
    //i really had to learn a lot about the world generation to make this work lol

    Tile[][][] tiles;
    World world;

    @BeforeEach
    void setUp(){
        //i made it a 3x3 so that the normal case could be in the middle, and not on the edge of the generated world
        tiles = new Tile[3][3][1];
        tiles[1][1][0] = Tile.WALL;
        tiles[2][2][0] = Tile.FLOOR;
        world = new World(tiles);
    }
    @Test
    void testTile(){
        assertEquals(Tile.WALL, world.tile(1,1,0));
    }

    @Test
    void testBoundaryTile(){
        assertEquals(Tile.FLOOR, world.tile(2,2,0));
    }

    @Test 
    void testInvalidTile(){
        assertEquals(Tile.BOUNDS, world.tile(-5,-2,0));
    }
}
