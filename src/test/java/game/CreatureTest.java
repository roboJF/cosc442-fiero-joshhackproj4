package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreatureTest {
    private World world;
    private Creature creature;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
            tiles[x][y][0] = Tile.FLOOR;
            }
        }

        world = new World(tiles);

        creature = new Creature(world, '@', Color.GRAY, "josh", 100, 10, 5);

        creature.x = 1;
        creature.y = 1;
        creature.z = 0;
    }

    @Test
    void testSurvivesDMG(){
        //im not entirely sure if you wanted us to add the creature to the world or not, but i did it anyways, just in case
        world.add(creature);

        creature.modifyHp(-99, "something awful");
        assertEquals(1, creature.hp());
        assertEquals("something awful", creature.causeOfDeath());
        //ik this arent needed for testing modifyHp(), but i spent way too much time reading through the world class to not include this
        //also it does check to make sure the creature is alive, and isnt a corpse, so maybe its not useless?
        assertSame(creature, world.creature(1, 1, 0));
    }

    @Test
    void testDiesAtZeroHp(){
        world.add(creature);
        creature.setCreatureAi(new CreatureAi(creature));
        creature.modifyHp(-100, "something more awful");
        
        assertEquals(0, creature.hp());
        assertEquals("something more awful", creature.causeOfDeath());
        assertNull(world.creature(1, 1, 0));
        assertNotNull(world.item(1, 1, 0));
    }

    @Test
    void testHealingCappedAtMax(){
        world.add(creature);
        creature.modifyHp(200, "something not awful");

        assertEquals(100, creature.hp());
        assertEquals("something not awful", creature.causeOfDeath());
        assertSame(creature, world.creature(1, 1, 0));
    }

    @Test
    void testDeathNotification(){
        List<String> notifications = new ArrayList<>();
        //writing this garbage was the only way i could get this to work
        //is there a better way to do this? probably, but im also very dumb so...
        new CreatureAi(creature) {
            @Override
            public void onNotify(String message){
                notifications.add(message);
            }
        };

        world.add(creature);
        creature.modifyHp(-100, "something pretty bad");

        assertEquals(0, creature.hp());
        assertTrue(notifications.contains("You die."));

    }

}
