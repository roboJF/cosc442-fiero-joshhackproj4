package game;

import java.awt.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellCastingTest {
    private World world;
    private Creature caster;
    private Creature target;
    private StuffFactory factory;

    @BeforeEach
    void setUp() {
        createWorld(false);
    }

    private void createWorld(boolean blocked) {
        Tile[][][] tiles = new Tile[5][5][1];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }
        if (blocked) {
            tiles[2][1][0] = Tile.WALL;
        }
        world = new World(tiles);
        caster = creature("caster", 1, 1);
        target = creature("target", 3, 1);
        factory = new StuffFactory(world);
    }

    private Creature creature(String name, int x, int y) {
        Creature creature = new Creature(world, '@', Color.WHITE, name, 500, 10, 5);
        creature.x = x;
        creature.y = y;
        new CreatureAi(creature);
        creature.modifyMaxMana(95);
        creature.modifyMana(95);
        // Tick-based assertions should measure spell effects, not regeneration.
        creature.modifyRegenHpPer1000(-10);
        creature.modifyRegenManaPer1000(-20);
        world.add(creature);
        return creature;
    }

    private Spell whiteSpell(String name) {
        return spell(factory.newWhiteMagesSpellbook(0), name);
    }

    private Spell blueSpell(String name) {
        return spell(factory.newBlueMagesSpellbook(0), name);
    }

    private Spell spell(Item book, String name) {
        return book.writtenSpells().stream()
                .filter(spell -> spell.name().equals(name))
                .findFirst().orElseThrow();
    }

    private void tick(Creature creature, int turns) {
        for (int turn = 0; turn < turns; turn++) {
            creature.update();
        }
    }

    @Test
    void minorHealRestoresTargetHealthAndChargesCasterExactlyOnce() {
        target.modifyHp(-460, "test injury");

        caster.castSpell(whiteSpell("minor heal"), target.x, target.y);

        assertAll(
                () -> assertEquals(60, target.hp()),
                () -> assertEquals(96, caster.mana()),
                () -> assertEquals(500, caster.hp()),
                () -> assertEquals(100, target.mana()));
        assertTrue(target.hasActiveEffects());

        tick(target, 1);
        assertEquals(60, target.hp());
        assertFalse(target.hasActiveEffects());
        assertEquals(0, target.activeEffectCount());
        assertEquals(96, caster.mana());
    }

    @Test
    void majorHealDoesNotExceedMaximumHealth() {
        target.modifyHp(-10, "test injury");

        caster.castSpell(whiteSpell("major heal"), target.x, target.y);

        assertEquals(500, target.hp());
        assertEquals(92, caster.mana());
    }

    @Test
    void insufficientManaDoesNotHealOrCreateAnEffect() {
        target.modifyHp(-100, "test injury");
        caster.modifyMana(-97);

        caster.castSpell(whiteSpell("minor heal"), target.x, target.y);

        assertEquals(400, target.hp());
        assertEquals(3, caster.mana());
        assertFalse(target.hasActiveEffects());
    }

    @Test
    void exactlyEnoughManaStillAllowsTheSpellToBeCast() {
        target.modifyHp(-100, "test injury");
        caster.modifyMana(-96);

        caster.castSpell(whiteSpell("minor heal"), target.x, target.y);

        assertEquals(420, target.hp());
        assertEquals(0, caster.mana());
        assertEquals(1, target.activeEffectCount());
    }

    @Test
    void emptyTargetDoesNotConsumeManaOrCreateAnEffect() {
        caster.castSpell(whiteSpell("minor heal"), 4, 4);

        assertEquals(100, caster.mana());
        assertFalse(caster.hasActiveEffects());
        assertFalse(target.hasActiveEffects());
    }

    @Test
    void unseenTargetDoesNotReceiveAnEffectOrConsumeMana() {
        createWorld(true);
        target.modifyHp(-100, "test injury");
        assertFalse(caster.canSee(target.x, target.y, 0));

        caster.castSpell(whiteSpell("minor heal"), target.x, target.y);

        assertEquals(400, target.hp());
        assertEquals(100, caster.mana());
        assertFalse(target.hasActiveEffects());
    }

    @Test
    void slowHealAppliesOnEachTurnAndStopsAfterFiftyTurns() {
        target.modifyHp(-400, "test injury");
        caster.castSpell(whiteSpell("slow heal"), target.x, target.y);
        assertEquals(100, target.hp());
        assertEquals(88, caster.mana());

        tick(target, 49);
        assertEquals(198, target.hp());
        assertEquals(1, target.activeEffectCount());

        tick(target, 1);
        assertEquals(200, target.hp());
        assertFalse(target.hasActiveEffects());

        tick(target, 1);
        assertEquals(200, target.hp());
        assertEquals(88, caster.mana());
    }

    @Test
    void repeatedCastsHaveIndependentTimersAndDoNotReuseAnExpiredEffect() {
        Creature secondTarget = creature("second target", 3, 2);
        target.modifyHp(-400, "test injury");
        secondTarget.modifyHp(-400, "test injury");
        Spell slowHeal = whiteSpell("slow heal");

        caster.castSpell(slowHeal, target.x, target.y);
        tick(target, 10);
        caster.castSpell(slowHeal, secondTarget.x, secondTarget.y);
        // The different expiration times below verify independence through behavior.
        assertEquals(1, target.activeEffectCount());
        assertEquals(1, secondTarget.activeEffectCount());

        tick(target, 40);
        tick(secondTarget, 40);
        assertEquals(200, target.hp());
        assertFalse(target.hasActiveEffects());
        assertEquals(180, secondTarget.hp());
        assertEquals(1, secondTarget.activeEffectCount());

        tick(secondTarget, 10);
        assertEquals(200, secondTarget.hp());
        assertFalse(secondTarget.hasActiveEffects());
        assertEquals(76, caster.mana());

        caster.castSpell(slowHeal, target.x, target.y);
        tick(target, 1);
        assertEquals(202, target.hp());
        assertEquals(1, target.activeEffectCount());
        assertEquals(64, caster.mana());
    }

    @Test
    void innerStrengthBonusesExpireAndAreRemovedOnlyOnce() {
        caster.castSpell(whiteSpell("inner strength"), target.x, target.y);
        assertEquals(12, target.attackValue());
        assertEquals(7, target.defenseValue());
        assertEquals(10, target.visionRadius());
        assertEquals(84, caster.mana());

        tick(target, 49);
        assertEquals(12, target.attackValue());
        assertEquals(7, target.defenseValue());

        tick(target, 1);
        assertEquals(10, target.attackValue());
        assertEquals(5, target.defenseValue());
        assertEquals(9, target.visionRadius());
        assertFalse(target.hasActiveEffects());

        tick(target, 1);
        assertEquals(10, target.attackValue());
        assertEquals(5, target.defenseValue());
    }

    @Test
    void bloodToManaConvertsHealthAndThenChargesItsManaCost() {
        caster.modifyMana(-90);

        caster.castSpell(blueSpell("blood to mana"), caster.x, caster.y);

        assertEquals(410, caster.hp());
        assertEquals(99, caster.mana());
    }

    @Test
    void detectCreaturesSeesThroughWallsUntilItsDurationExpires() {
        createWorld(true);
        assertFalse(caster.canSee(target.x, target.y, 0));

        caster.castSpell(blueSpell("detect creatures"), caster.x, caster.y);
        assertTrue(caster.canSee(target.x, target.y, 0));
        assertEquals(84, caster.mana());

        tick(caster, 74);
        assertTrue(caster.canSee(target.x, target.y, 0));
        tick(caster, 1);
        assertFalse(caster.canSee(target.x, target.y, 0));
        assertFalse(caster.hasActiveEffects());
    }
}
