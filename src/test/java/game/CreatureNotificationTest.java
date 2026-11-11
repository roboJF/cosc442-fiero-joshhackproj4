package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CreatureNotificationTest {
    private World world;
    private RecordingAi actor;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[25][25][2];
        for (int x = 0; x < 25; x++) {
            for (int y = 0; y < 25; y++) {
                for (int z = 0; z < 2; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                }
            }
        }
        world = new World(tiles);
        actor = observerAt("goblin", 12, 12, 1);
    }

    private RecordingAi observerAt(String name, int x, int y, int z) {
        Creature creature = new Creature(world, 'g', Color.GREEN, name, 100, 10, 5);
        creature.x = x;
        creature.y = y;
        creature.z = z;
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        return ai;
    }

    private Item unknownPotion() {
        return new Item('!', Color.RED, "healing potion", "red potion");
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, 0, true", "-1, 0, 0, true", "0, 1, 0, true", "0, -1, 0, true",
            "9, 0, 0, true", "-9, 0, 0, true", "0, 9, 0, true", "0, -9, 0, true",
            "10, 0, 0, false", "-10, 0, 0, false", "0, 10, 0, false", "0, -10, 0, false",
            "6, 6, 0, true", "-6, 6, 0, true", "6, -6, 0, true", "-6, -6, 0, true",
            "7, 7, 0, false", "-7, 7, 0, false", "7, -7, 0, false", "-7, -7, 0, false",
            "1, 0, -1, false"
    })
    void formatsMessagesOnceForRecipientsWithinTheSameDepthRadius(
            int dx, int dy, int dz, boolean receivesMessage) {
        RecordingAi observer = observerAt("observer", 12 + dx, 12 + dy, 1 + dz);

        // Current action notifications use a fixed radius, independently of line of sight.
        actor.creature.doAction("pick up %s", "a stone");

        assertEquals(List.of("You pick up a stone."), actor.messages);
        assertEquals(receivesMessage ? List.of("The goblin picks up a stone.") : List.of(),
                observer.messages);
    }

    @Test
    void learningAnItemNameChangesOnlyThatCreaturesKnowledge() {
        RecordingAi other = observerAt("other", 13, 12, 1);
        Item potion = unknownPotion();
        assertEquals("red potion", actor.creature.nameOf(potion));
        assertEquals("red potion", other.creature.nameOf(potion));

        actor.creature.learnName(potion);

        assertEquals("healing potion", actor.creature.nameOf(potion));
        assertEquals("red potion", other.creature.nameOf(potion));
        assertEquals(List.of("The red potion is a healing potion!"), actor.messages);
        assertTrue(other.messages.isEmpty());
    }

    @Test
    void learningOneItemDoesNotIdentifyADifferentItemType() {
        Item healing = unknownPotion();
        Item poison = new Item('!', Color.GREEN, "poison potion", "green potion");

        actor.creature.learnName(healing);

        assertEquals("healing potion", actor.creature.nameOf(healing));
        assertEquals("green potion", actor.creature.nameOf(poison));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1})
    void itemActionsIdentifyItemsOnlyForCreaturesReceivingTheAction(int hp) {
        actor.creature.modifyHp(hp - 100, "test injury");
        RecordingAi nearby = observerAt("nearby", 13, 12, 1);
        RecordingAi distant = observerAt("distant", 22, 12, 1);
        RecordingAi otherDepth = observerAt("other depth", 13, 12, 0);
        Item potion = unknownPotion();

        actor.creature.doAction(potion, "drink %s", "a potion");

        assertEquals(List.of("You drink a potion.", "The red potion is a healing potion!"),
                actor.messages);
        assertEquals(List.of("The goblin drinks a potion.", "The red potion is a healing potion!"),
                nearby.messages);
        assertTrue(distant.messages.isEmpty());
        assertTrue(otherDepth.messages.isEmpty());
        assertEquals("healing potion", actor.creature.nameOf(potion));
        assertEquals("healing potion", nearby.creature.nameOf(potion));
        assertEquals("red potion", distant.creature.nameOf(potion));
        assertEquals("red potion", otherDepth.creature.nameOf(potion));
    }

    @Test
    void deadCreatureCannotPerformAnItemActionOrTeachItsName() {
        RecordingAi nearby = observerAt("nearby", 13, 12, 1);
        actor.creature.modifyHp(-100, "test damage");
        assertEquals(0, actor.creature.hp());
        actor.messages.clear();
        nearby.messages.clear();
        Item potion = unknownPotion();

        actor.creature.doAction(potion, "drink %s", "a potion");

        assertTrue(actor.messages.isEmpty());
        assertTrue(nearby.messages.isEmpty());
        assertEquals("red potion", actor.creature.nameOf(potion));
        assertEquals("red potion", nearby.creature.nameOf(potion));
    }

    private static class RecordingAi extends CreatureAi {
        private final List<String> messages = new ArrayList<>();

        RecordingAi(Creature creature) {
            super(creature);
        }

        @Override
        public void onNotify(String message) {
            messages.add(message);
        }
    }
}
