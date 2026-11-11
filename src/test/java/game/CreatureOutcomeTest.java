package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CreatureOutcomeTest {
    private World world;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[5][5][2];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 2; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                }
            }
        }
        world = new World(tiles);
    }

    private Creature creature(char glyph, String name, int maxHp) {
        Creature creature = new Creature(world, glyph, Color.MAGENTA, name, maxHp, 10, 5);
        creature.x = 2;
        creature.y = 2;
        creature.z = 1;
        new CreatureAi(creature);
        world.add(creature);
        return creature;
    }

    private List<Item> worldItems() {
        List<Item> items = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 2; z++) {
                    Item item = world.item(x, y, z);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        }
        return items;
    }

    @ParameterizedTest
    @CsvSource({"@, hero, 100", "g, goblin, 50"})
    void startsAtFullHealthAndTheIntendedHungerLevel(char glyph, String name, int maxHp) {
        Creature creature = creature(glyph, name, maxHp);

        assertAll(
                () -> assertEquals(maxHp, creature.hp()),
                () -> assertEquals(maxHp, creature.maxHp()),
                () -> assertEquals(1000, creature.maxFood()),
                () -> assertEquals(666, creature.food()));
    }

    @Test
    void survivingAtOneHpPreservesInventoryAndCreatesNoCorpse() {
        Creature creature = creature('g', "goblin", 100);
        Item carried = new Item('*', Color.YELLOW, "gem", null);
        creature.inventory().add(carried);

        creature.modifyHp(-99, "test damage");

        assertEquals(1, creature.hp());
        assertEquals("test damage", creature.causeOfDeath());
        assertSame(creature, world.creature(2, 2, 1));
        assertTrue(creature.inventory().contains(carried));
        assertTrue(worldItems().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"50, -50, 0, 250", "100, -101, -1, 500"})
    void lethalDamageProducesTheCorrectCorpseAndRemovesTheCreature(
            int maxHp, int damage, int expectedHp, int expectedNutrition) {
        Creature creature = creature('g', "goblin", maxHp);

        creature.modifyHp(damage, "test damage");

        assertEquals(expectedHp, creature.hp());
        assertEquals(maxHp, creature.maxHp());
        assertEquals("test damage", creature.causeOfDeath());
        assertNull(world.creature(2, 2, 1));
        List<Item> items = worldItems();
        assertEquals(1, items.size());
        Item corpse = items.get(0);
        assertSame(corpse, world.item(2, 2, 1));
        assertAll(
                () -> assertEquals("goblin corpse", corpse.name()),
                () -> assertEquals("goblin corpse", corpse.appearance()),
                () -> assertEquals('%', corpse.glyph()),
                () -> assertEquals(Color.MAGENTA, corpse.color()),
                () -> assertEquals(expectedNutrition, corpse.foodValue()));
    }

    @Test
    void deathDropsEveryCarriedItemOnceAndClearsEquipmentOnTheCorrectDepth() {
        Creature creature = creature('g', "goblin", 100);
        Item sword = new Item('/', Color.WHITE, "sword", null);
        sword.modifyAttackValue(5);
        Item armor = new Item('[', Color.WHITE, "armor", null);
        armor.modifyDefenseValue(4);
        Item gem = new Item('*', Color.YELLOW, "gem", null);
        creature.inventory().add(sword);
        creature.inventory().add(armor);
        creature.inventory().add(gem);
        creature.equip(sword);
        creature.equip(armor);
        assertSame(sword, creature.weapon());
        assertSame(armor, creature.armor());

        creature.modifyHp(-100, "test damage");

        assertNull(world.creature(2, 2, 1));
        assertNull(creature.weapon());
        assertNull(creature.armor());
        for (Item item : creature.inventory().getItems()) {
            assertNull(item);
        }
        List<Item> items = worldItems();
        assertEquals(4, items.size());
        assertEquals(1L, items.stream().filter(item -> item == sword).count());
        assertEquals(1L, items.stream().filter(item -> item == armor).count());
        assertEquals(1L, items.stream().filter(item -> item == gem).count());
        assertEquals("goblin corpse", world.item(2, 2, 1).name());
        assertEquals(500, world.item(2, 2, 1).foodValue());
        // Neighbor order is shuffled, so verify conservation and depth, not exact drop slots.
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                assertNull(world.item(x, y, 0));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"10, 676", "-10, 656", "0, 666"})
    void ordinaryFoodAdjustmentsChangeFoodWithoutDamagingTheCreature(int amount, int expected) {
        Creature creature = creature('@', "hero", 100);

        creature.modifyFood(amount);

        assertEquals(expected, creature.food());
        assertEquals(1000, creature.maxFood());
        assertEquals(100, creature.hp());
        assertSame(creature, world.creature(2, 2, 1));
        assertTrue(worldItems().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "@, -665, 1, false", "@, -666, 0, true", "@, -667, -1, true",
            "g, -666, 0, false", "g, -667, -1, false"
    })
    void starvationAppliesAtZeroFoodOnlyToPlayers(char glyph, int amount, int food, boolean dies) {
        Creature creature = creature(glyph, "hungry creature", 100);

        creature.modifyFood(amount);

        assertEquals(food, creature.food());
        if (dies) {
            assertTrue(creature.hp() <= 0);
            assertEquals("Starved to death.", creature.causeOfDeath());
            assertNull(world.creature(2, 2, 1));
            assertEquals(1, worldItems().size());
        } else {
            assertEquals(100, creature.hp());
            assertSame(creature, world.creature(2, 2, 1));
            assertTrue(worldItems().isEmpty());
        }
    }

    @ParameterizedTest
    @CsvSource({"334, 1000, 1000, 100", "335, 1000, 1000, 99", "500, 1083, 1083, 99"})
    void capacityAndOvereatingBoundariesHaveTheIntendedFoodAndHealthOutcomes(
            int amount, int expectedFood, int expectedCapacity, int expectedHp) {
        Creature creature = creature('@', "hero", 100);

        creature.modifyFood(amount);

        assertEquals(expectedFood, creature.food());
        assertEquals(expectedCapacity, creature.maxFood());
        assertEquals(expectedHp, creature.hp());
        assertSame(creature, world.creature(2, 2, 1));
        assertTrue(worldItems().isEmpty());
    }
}
