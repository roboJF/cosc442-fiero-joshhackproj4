package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CreatureDropTest {
    private World world;
    private Creature player;
    private List<String> messages;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }
        world = new World(tiles);
        player = new Creature(world, '@', Color.WHITE, "player", 100, 10, 5);
        player.x = 1;
        player.y = 1;
        messages = new ArrayList<>();
        new CreatureAi(player) {
            @Override
            public void onNotify(String message) {
                messages.add(message);
            }
        };
        world.add(player);
    }

    private Item item(String name) {
        return new Item('!', Color.WHITE, name, null);
    }

    private void assertOccurrences(Item expected, int count) {
        int actual = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (world.item(x, y, 0) == expected) {
                    actual++;
                }
            }
        }
        assertEquals(count, actual);
    }

    private Item[] equipWeaponAndArmor() {
        Item weapon = item("sword");
        weapon.modifyAttackValue(5);
        Item armor = item("armor");
        armor.modifyDefenseValue(4);
        player.inventory().add(weapon);
        player.inventory().add(armor);
        player.equip(weapon);
        player.equip(armor);
        assertSame(weapon, player.weapon());
        assertSame(armor, player.armor());
        messages.clear();
        return new Item[] {weapon, armor};
    }

    @Test
    void droppingAnUnequippedItemTransfersItOnceAndPreservesOtherInventoryItems() {
        Item dropped = item("pebble");
        Item retained = item("retained");
        player.inventory().add(dropped);
        player.inventory().add(retained);

        player.drop(dropped);

        assertFalse(player.inventory().contains(dropped));
        assertTrue(player.inventory().contains(retained));
        assertSame(dropped, world.item(1, 1, 0));
        assertOccurrences(dropped, 1);
        assertOccurrences(retained, 0);
        assertTrue(messages.contains("You drop a pebble."));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void droppingEquippedGearClearsOnlyItsOwnEquipmentSlot(int slot) {
        Item[] equipment = equipWeaponAndArmor();
        Item dropped = equipment[slot];
        Item retained = equipment[1 - slot];

        player.drop(dropped);

        assertFalse(player.inventory().contains(dropped));
        assertTrue(player.inventory().contains(retained));
        assertSame(slot == 0 ? null : retained, player.weapon());
        assertSame(slot == 1 ? null : retained, player.armor());
        assertSame(dropped, world.item(1, 1, 0));
        assertOccurrences(dropped, 1);
        assertOccurrences(retained, 0);
    }

    @Test
    void droppingOnAnOccupiedTileFindsSpaceWithoutOverwritingTheExistingItem() {
        Item existing = item("existing");
        assertTrue(world.addAtEmptySpace(existing, 1, 1, 0));
        Item dropped = item("pebble");
        player.inventory().add(dropped);
        messages.clear();

        player.drop(dropped);

        assertSame(existing, world.item(1, 1, 0));
        assertFalse(player.inventory().contains(dropped));
        // Several neighbors are valid; the shuffled search may choose any one.
        assertOccurrences(dropped, 1);
        assertOccurrences(existing, 1);
        assertTrue(messages.contains("You drop a pebble."));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void failedDropPreservesInventoryEquipmentAndWorldItems(int slot) {
        Item[] equipment = equipWeaponAndArmor();
        Item dropped = equipment[slot];
        Item[][] before = new Item[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                before[x][y] = item("existing " + x + y);
                assertTrue(world.addAtEmptySpace(before[x][y], x, y, 0));
                assertSame(before[x][y], world.item(x, y, 0));
            }
        }
        Item[] inventoryBefore = player.inventory().getItems().clone();
        messages.clear();

        player.drop(dropped);

        assertArrayEquals(inventoryBefore, player.inventory().getItems());
        assertSame(equipment[0], player.weapon());
        assertSame(equipment[1], player.armor());
        assertOccurrences(dropped, 0);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                assertSame(before[x][y], world.item(x, y, 0));
            }
        }
        assertEquals(List.of("There's nowhere to drop the " + dropped.name() + "."), messages);
    }
}
