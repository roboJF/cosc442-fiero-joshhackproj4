package ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.event.KeyEvent;

import game.Creature;
import game.CreatureAi;
import game.Item;
import game.Spell;
import game.StuffFactory;
import game.Tile;
import game.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReadSpellScreenTest {
    private Creature player;
    private Item book;

    @BeforeEach
    void setUp() {
        Tile[][][] tiles = new Tile[3][3][1];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                tiles[x][y][0] = Tile.FLOOR;
            }
        }
        World world = new World(tiles);
        player = new Creature(world, '@', Color.WHITE, "player", 100, 10, 5);
        player.x = 1;
        player.y = 1;
        new CreatureAi(player);
        world.add(player);
        player.modifyMaxMana(95);
        player.modifyMana(95);
        book = new StuffFactory(world).newWhiteMagesSpellbook(0);
        // Spell letters must not depend on the inventory slot containing the book.
        Item placeholder = new Item('?', Color.GRAY, "placeholder", null);
        for (int slot = 0; slot < 3; slot++) {
            player.inventory().add(placeholder);
        }
        player.inventory().add(book);
        for (int slot = 0; slot < 3; slot++) {
            player.inventory().remove(placeholder);
        }
    }

    private KeyEvent letter(char letter) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.getExtendedKeyCodeForChar(letter), letter);
    }

    private KeyEvent key(int code) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0,
                code, KeyEvent.CHAR_UNDEFINED);
    }

    @ParameterizedTest
    @CsvSource({"a, 0", "d, 3"})
    void selectsFirstAndLastSpellRegardlessOfInventorySlots(char letter, int index) {
        RecordingScreen screen = new RecordingScreen(player, book);

        assertSame(screen, screen.respondToUserInput(letter(letter)));

        assertSame(book.writtenSpells().get(index), screen.selected);
        assertEquals(100, player.mana());
    }

    @ParameterizedTest
    @ValueSource(chars = {'e', 't', 'z', '?'})
    void invalidSelectionStaysOpenEvenWhenInventorySlotsAreOccupied(char letter) {
        player.inventory().remove(book);
        int capacity = player.inventory().getItems().length;
        for (int slot = 0; slot < capacity; slot++) {
            player.inventory().add(book);
        }
        RecordingScreen screen = new RecordingScreen(player, book);

        assertSame(screen, screen.respondToUserInput(letter(letter)));

        assertNull(screen.selected);
        assertEquals(100, player.mana());
        assertFalse(player.hasActiveEffects());
    }

    @Test
    void emptySpellbookDoesNotSelectOrThrow() {
        Item emptyBook = new Item('+', Color.WHITE, "empty book", null);
        RecordingScreen screen = new RecordingScreen(player, emptyBook);
        player.inventory().add(book);

        assertSame(screen, screen.respondToUserInput(letter('a')));

        assertNull(screen.selected);
        assertEquals(100, player.mana());
    }

    @Test
    void escapeClosesMenuWithoutSelectingOrCasting() {
        RecordingScreen screen = new RecordingScreen(player, book);

        assertNull(screen.respondToUserInput(key(KeyEvent.VK_ESCAPE)));

        assertNull(screen.selected);
        assertEquals(100, player.mana());
        assertFalse(player.hasActiveEffects());
    }

    @Test
    void targetedSpellWaitsForConfirmationThenHealsAndChargesOnce() {
        player.modifyHp(-60, "test injury");
        ReadSpellScreen screen = new ReadSpellScreen(player, 1, 1, book);

        Screen targeting = screen.respondToUserInput(letter('a'));

        assertInstanceOf(CastSpellScreen.class, targeting);
        assertEquals(40, player.hp());
        assertEquals(100, player.mana());
        assertFalse(player.hasActiveEffects());

        assertNull(targeting.respondToUserInput(key(KeyEvent.VK_ENTER)));
        assertEquals(60, player.hp());
        assertEquals(96, player.mana());
    }

    @Test
    void cancellingTargetSelectionDoesNotCastOrSpendMana() {
        player.modifyHp(-60, "test injury");
        ReadSpellScreen screen = new ReadSpellScreen(player, 1, 1, book);
        Screen targeting = screen.respondToUserInput(letter('a'));
        assertInstanceOf(CastSpellScreen.class, targeting);

        assertNull(targeting.respondToUserInput(key(KeyEvent.VK_ESCAPE)));

        assertEquals(40, player.hp());
        assertEquals(100, player.mana());
        assertFalse(player.hasActiveEffects());
    }

    private static class RecordingScreen extends ReadSpellScreen {
        private Spell selected;

        RecordingScreen(Creature player, Item book) {
            super(player, 1, 1, book);
        }

        @Override
        protected Screen use(Spell spell) {
            selected = spell;
            return this;
        }
    }
}
