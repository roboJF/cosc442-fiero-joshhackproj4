package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class WorldItemPlacementTest {
    private Tile[][][] tiles;
    private World world;

    @BeforeEach
    void setUp() {
        tiles = new Tile[3][3][2];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 2; z++) {
                    tiles[x][y][z] = Tile.WALL;
                }
            }
        }
        world = new World(tiles);
    }

    private Item item(String name) {
        return new Item('!', Color.WHITE, name, null);
    }

    private void placeExistingItem(Item item, int x, int y, int z) {
        assertTrue(world.addAtEmptySpace(item, x, y, z));
        assertSame(item, world.item(x, y, z));
    }

    private Item[][][] snapshot() {
        Item[][][] items = new Item[3][3][2];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 2; z++) {
                    items[x][y][z] = world.item(x, y, z);
                }
            }
        }
        return items;
    }

    private void assertItemsMatch(Item[][][] expected) {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 2; z++) {
                    assertSame(expected[x][y][z], world.item(x, y, z),
                            "Item at " + x + "," + y + "," + z);
                }
            }
        }
    }

    private void assertOccurrences(Item expected, int count) {
        int actual = 0;
        for (Item[][] column : snapshot()) {
            for (Item[] depths : column) {
                for (Item item : depths) {
                    if (item == expected) {
                        actual++;
                    }
                }
            }
        }
        assertEquals(count, actual);
    }

    private RecordingAi observerAt(String name, int x, int y, int z) {
        Creature creature = new Creature(world, '@', Color.WHITE, name, 100, 10, 5);
        creature.x = x;
        creature.y = y;
        creature.z = z;
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        return ai;
    }

    @ParameterizedTest
    @EnumSource(value = Tile.class, names = {"FLOOR", "STAIRS_UP", "STAIRS_DOWN"})
    void placesTheItemExactlyOnceOnAnEmptyUsableTile(Tile tile) {
        tiles[1][1][0] = tile;
        Item incoming = item("incoming");
        Item[][][] expected = snapshot();
        expected[1][1][0] = incoming;

        assertTrue(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertItemsMatch(expected);
        assertOccurrences(incoming, 1);
    }

    @Test
    void occupiedOriginUsesTheOnlyAvailableNeighborWithoutOverwriting() {
        tiles[1][1][0] = Tile.FLOOR;
        tiles[2][1][0] = Tile.FLOOR;
        Item existing = item("existing");
        placeExistingItem(existing, 1, 1, 0);
        Item incoming = item("incoming");
        Item[][][] expected = snapshot();
        expected[2][1][0] = incoming;

        assertTrue(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertItemsMatch(expected);
        assertOccurrences(existing, 1);
        assertOccurrences(incoming, 1);
    }

    @Test
    void searchesBeyondAnOccupiedNeighborToFindSpace() {
        for (int x = 0; x < 3; x++) {
            tiles[x][1][0] = Tile.FLOOR;
        }
        placeExistingItem(item("origin item"), 0, 1, 0);
        placeExistingItem(item("middle item"), 1, 1, 0);
        Item incoming = item("incoming");
        Item[][][] expected = snapshot();
        expected[2][1][0] = incoming;

        assertTrue(world.addAtEmptySpace(incoming, 0, 1, 0));

        assertItemsMatch(expected);
        assertOccurrences(incoming, 1);
    }

    @Test
    void doesNotUseEmptySpaceOnAnotherDepth() {
        tiles[1][1][0] = Tile.FLOOR;
        tiles[1][1][1] = Tile.FLOOR;
        placeExistingItem(item("existing"), 1, 1, 1);
        Item incoming = item("incoming");
        Item[][][] before = snapshot();

        assertFalse(world.addAtEmptySpace(incoming, 1, 1, 1));

        assertItemsMatch(before);
        assertOccurrences(incoming, 0);
    }

    @Test
    void fullMapReturnsFalseAndPreservesEveryExistingItem() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 2; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                    placeExistingItem(item("existing " + x + y + z), x, y, z);
                }
            }
        }
        Item incoming = item("incoming");
        Item[][][] before = snapshot();

        assertFalse(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertItemsMatch(before);
        assertOccurrences(incoming, 0);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 1, 0", "3, 1, 0", "1, -1, 0",
            "1, 3, 0", "1, 1, -1", "1, 1, 2"
    })
    void invalidOriginReturnsFalseWithoutChangingItems(int x, int y, int z) {
        tiles[1][1][0] = Tile.FLOOR;
        placeExistingItem(item("existing"), 1, 1, 0);
        Item[][][] before = snapshot();
        Item incoming = item("incoming");

        assertFalse(world.addAtEmptySpace(incoming, x, y, z));

        assertItemsMatch(before);
        assertOccurrences(incoming, 0);
    }

    @Test
    void wallOriginIsRejectedEvenWhenANeighborHasSpace() {
        tiles[2][1][0] = Tile.FLOOR;
        Item incoming = item("incoming");
        Item[][][] before = snapshot();

        assertFalse(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertItemsMatch(before);
        assertOccurrences(incoming, 0);
    }

    @ParameterizedTest
    @CsvSource({"1, 1, 0", "-1, 1, 0"})
    void nullItemIsASuccessfulNoOpEvenForAnInvalidOrigin(int x, int y, int z) {
        tiles[1][1][0] = Tile.FLOOR;
        placeExistingItem(item("existing"), 1, 1, 0);
        RecordingAi observer = observerAt("observer", 1, 1, 0);
        Item[][][] before = snapshot();

        assertTrue(world.addAtEmptySpace(null, x, y, z));

        assertItemsMatch(before);
        assertTrue(observer.messages.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"false, red potion", "true, healing potion"})
    void landingCreatureReceivesOneMessageUsingItsKnowledge(boolean identified, String name) {
        tiles[1][1][0] = Tile.FLOOR;
        tiles[2][1][0] = Tile.FLOOR;
        RecordingAi recipient = observerAt("recipient", 1, 1, 0);
        RecordingAi neighbor = observerAt("neighbor", 2, 1, 0);
        Item incoming = new Item('!', Color.WHITE, "healing potion", "red potion");
        if (identified) {
            recipient.setName(incoming, "healing potion");
        }

        assertTrue(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertEquals(List.of("A " + name + " lands between your feet."), recipient.messages);
        assertTrue(neighbor.messages.isEmpty());
        assertSame(incoming, world.item(1, 1, 0));
    }

    @Test
    void relocatedItemNotifiesOnlyTheCreatureAtItsActualDestination() {
        tiles[1][1][0] = Tile.FLOOR;
        tiles[2][1][0] = Tile.FLOOR;
        Item existing = item("existing");
        placeExistingItem(existing, 1, 1, 0);
        RecordingAi origin = observerAt("origin", 1, 1, 0);
        RecordingAi destination = observerAt("destination", 2, 1, 0);
        Item incoming = item("pebble");

        assertTrue(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertTrue(origin.messages.isEmpty());
        assertEquals(List.of("A pebble lands between your feet."), destination.messages);
        assertSame(existing, world.item(1, 1, 0));
        assertSame(incoming, world.item(2, 1, 0));
    }

    @Test
    void emptyLandingDoesNotNotifyANearbyCreature() {
        tiles[1][1][0] = Tile.FLOOR;
        tiles[2][1][0] = Tile.FLOOR;
        RecordingAi neighbor = observerAt("neighbor", 2, 1, 0);
        Item incoming = item("incoming");

        assertTrue(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertSame(incoming, world.item(1, 1, 0));
        assertTrue(neighbor.messages.isEmpty());
    }

    @Test
    void failedPlacementSendsNoArrivalNotification() {
        tiles[1][1][0] = Tile.FLOOR;
        placeExistingItem(item("existing"), 1, 1, 0);
        RecordingAi observer = observerAt("observer", 1, 1, 0);
        Item incoming = item("incoming");
        Item[][][] before = snapshot();

        assertFalse(world.addAtEmptySpace(incoming, 1, 1, 0));

        assertTrue(observer.messages.isEmpty());
        assertItemsMatch(before);
        assertOccurrences(incoming, 0);
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
