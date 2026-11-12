package game;

import java.awt.Color;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {
    @Test
    void spellListSnapshotCannotBeChangedByCallers() {
        Item book = new Item('+', Color.WHITE, "book", null);
        book.addWrittenSpell("first", 1, () -> new Effect(1));
        List<Spell> spells = book.writtenSpells();
        Spell first = spells.get(0);

        assertThrows(UnsupportedOperationException.class, spells::clear);
        assertThrows(UnsupportedOperationException.class, () -> spells.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> spells.add(first));
        assertThrows(UnsupportedOperationException.class, () -> spells.set(0, first));

        assertEquals(List.of(first), book.writtenSpells());
    }

    @Test
    void addingSpellsDoesNotChangeAnEarlierSnapshot() {
        Item book = new Item('+', Color.WHITE, "book", null);
        book.addWrittenSpell("first", 1, () -> new Effect(1));
        List<Spell> snapshot = book.writtenSpells();
        Spell first = snapshot.get(0);

        book.addWrittenSpell("second", 2, () -> new Effect(2));

        assertEquals(List.of(first), snapshot);
        assertEquals(2, book.writtenSpells().size());
        assertSame(first, book.writtenSpells().get(0));
        assertEquals("second", book.writtenSpells().get(1).name());
    }

    @Test
    void preservesItsIdentityAndExplicitAppearance() {
        Item potion = new Item('!', Color.GREEN, "healing potion", "red potion");

        assertAll(
                () -> assertEquals('!', potion.glyph()),
                () -> assertEquals(Color.GREEN, potion.color()),
                () -> assertEquals("healing potion", potion.name()),
                () -> assertEquals("red potion", potion.appearance()));
    }

    @Test
    void missingAppearanceFallsBackToTheActualName() {
        Item item = new Item('*', Color.YELLOW, "gem", null);

        assertEquals("gem", item.name());
        assertEquals("gem", item.appearance());
    }

    @ParameterizedTest
    @CsvSource({"5, -2, 3", "-5, 2, -3", "5, -5, 0"})
    void foodAdjustmentsAccumulateInBothDirections(int first, int second, int expected) {
        Item food = new Item('%', Color.WHITE, "food", null);
        assertEquals(0, food.foodValue());

        food.modifyFoodValue(first);
        assertEquals(first, food.foodValue());
        food.modifyFoodValue(second);

        assertEquals(expected, food.foodValue());
    }

    @Test
    void thrownAttackAdjustmentsAppearInDetailsUntilRestoredToDefault() {
        Item rock = new Item('*', Color.WHITE, "rock", null);
        assertEquals(1, rock.thrownAttackValue());
        assertFalse(rock.details().contains("thrown:"));

        rock.modifyThrownAttackValue(3);

        assertEquals(4, rock.thrownAttackValue());
        assertTrue(rock.details().contains("thrown:4"));

        rock.modifyThrownAttackValue(-3);

        assertEquals(1, rock.thrownAttackValue());
        assertFalse(rock.details().contains("thrown:"));
    }

    @Test
    void rangedAttackValueTracksAdjustmentsAndAppearsInItemDetails() {
        Item bow = new Item(')', Color.WHITE, "bow", null);
        assertEquals(0, bow.rangedAttackValue());

        bow.modifyRangedAttackValue(7);
        assertEquals(7, bow.rangedAttackValue());
        bow.modifyRangedAttackValue(-2);

        assertEquals(5, bow.rangedAttackValue());
        assertTrue(bow.details().contains("ranged:5"));
    }
}
