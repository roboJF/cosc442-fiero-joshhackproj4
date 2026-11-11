package game;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {
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
