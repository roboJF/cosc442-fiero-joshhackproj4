package game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class InventoryTest {

    private Item item(String name) {
        return new Item('!', Color.WHITE, name, name);
    }

    // Creates a three-slot inventory and inspects its initial slots and full status.
    @Test
    void newInventoryHasRequestedCapacityAndEmptySlots() {
        Inventory inventory = new Inventory(3);

        assertAll(
                () -> assertEquals(3, inventory.getItems().length),
                () -> assertArrayEquals(new Item[3], inventory.getItems()),
                () -> assertFalse(inventory.isFull()));
    }

    // Tries to add an item to a zero-slot inventory, then inspects its capacity and contents.
    @Test
    void zeroCapacityInventoryIsFullAndCannotStoreAnItem() {
        Inventory inventory = new Inventory(0);
        Item item = item("potion");

        inventory.add(item);

        assertAll(
                () -> assertEquals(0, inventory.getItems().length),
                () -> assertTrue(inventory.isFull()),
                () -> assertFalse(inventory.contains(item)));
    }

    // Constructs an inventory with a negative capacity and captures the resulting exception.
    @Test
    void negativeCapacityIsRejected() {
        assertThrows(NegativeArraySizeException.class, () -> new Inventory(-1));
    }

    // Adds two items to three slots, then reads each occupied and empty index.
    @Test
    void getReturnsTheItemAtEachOccupiedIndexAndNullAtEmptyIndexes() {
        Inventory inventory = new Inventory(3);
        Item first = item("first");
        Item second = item("second");
        inventory.add(first);
        inventory.add(second);

        assertAll(
                () -> assertSame(first, inventory.get(0)),
                () -> assertSame(second, inventory.get(1)),
                () -> assertNull(inventory.get(2)));
    }

    // Reads indexes below zero and at the capacity boundary of a two-slot inventory.
    @Test
    void getRejectsIndexesOutsideTheInventory() {
        Inventory inventory = new Inventory(2);

        assertAll(
                () -> assertThrows(ArrayIndexOutOfBoundsException.class, () -> inventory.get(-1)),
                () -> assertThrows(ArrayIndexOutOfBoundsException.class, () -> inventory.get(2)));
    }

    // Adds two different items and inspects their order in the backing array.
    @Test
    void addFillsTheFirstAvailableSlot() {
        Inventory inventory = new Inventory(3);
        Item first = item("first");
        Item second = item("second");
        inventory.add(first);
        inventory.add(second);

        assertArrayEquals(new Item[] { first, second, null }, inventory.getItems());
    }

    // Fills every slot, tries to add one more item, and inspects the original contents.
    @Test
    void addingToAFullInventoryLeavesExistingItemsUntouched() {
        Inventory inventory = new Inventory(2);
        Item first = item("first");
        Item second = item("second");
        Item extra = item("extra");
        inventory.add(first);
        inventory.add(second);

        inventory.add(extra);

        assertAll(
                () -> assertArrayEquals(new Item[] { first, second }, inventory.getItems()),
                () -> assertFalse(inventory.contains(extra)),
                () -> assertTrue(inventory.isFull()));
    }

    // Adds null to a one-slot inventory and inspects the slot and full status afterward.
    @Test
    void addingNullLeavesAnEmptySlotAvailable() {
        Inventory inventory = new Inventory(1);

        inventory.add(null);

        assertAll(
                () -> assertNull(inventory.get(0)),
                () -> assertFalse(inventory.isFull()));
    }

    // Observes full status before additions, at capacity, and after removing an item.
    @Test
    void isFullChangesAtCapacityAndAfterRemoval() {
        Inventory inventory = new Inventory(2);
        Item first = item("first");
        Item second = item("second");

        assertFalse(inventory.isFull());
        inventory.add(first);
        assertFalse(inventory.isFull());
        inventory.add(second);
        assertTrue(inventory.isFull());
        inventory.remove(first);
        assertFalse(inventory.isFull());
    }

    // Removes the middle of three items and inspects the positions of the survivors.
    @Test
    void removeClearsOnlyTheMatchingSlotWithoutShiftingOtherItems() {
        Inventory inventory = new Inventory(3);
        Item first = item("first");
        Item middle = item("middle");
        Item last = item("last");
        inventory.add(first);
        inventory.add(middle);
        inventory.add(last);

        inventory.remove(middle);

        assertAll(
                () -> assertArrayEquals(new Item[] { first, null, last }, inventory.getItems()),
                () -> assertFalse(inventory.contains(middle)));
    }

    // Removes the middle item, adds a replacement, and inspects which slot it occupies.
    @Test
    void addReusesTheEarliestVacantSlot() {
        Inventory inventory = new Inventory(3);
        Item first = item("first");
        Item middle = item("middle");
        Item last = item("last");
        Item replacement = item("replacement");
        inventory.add(first);
        inventory.add(middle);
        inventory.add(last);
        inventory.remove(middle);

        inventory.add(replacement);

        assertArrayEquals(new Item[] { first, replacement, last }, inventory.getItems());
    }

    // Attempts to remove an item never added and inspects the existing slots afterward.
    @Test
    void removingAnAbsentItemLeavesTheInventoryUntouched() {
        Inventory inventory = new Inventory(2);
        Item present = item("present");
        inventory.add(present);

        inventory.remove(item("absent"));

        assertArrayEquals(new Item[] { present, null }, inventory.getItems());
    }

    // Adds one object twice, removes it once, and inspects both slots and lookup results.
    @Test
    void addingTheSameReferenceTwiceUsesTwoSlotsAndRemoveClearsOnlyFirstOccurrence() {
        Inventory inventory = new Inventory(2);
        Item duplicate = item("duplicate");
        inventory.add(duplicate);
        inventory.add(duplicate);

        assertArrayEquals(new Item[] { duplicate, duplicate }, inventory.getItems());
        inventory.remove(duplicate);

        assertAll(
                () -> assertArrayEquals(new Item[] { null, duplicate }, inventory.getItems()),
                () -> assertTrue(inventory.contains(duplicate)),
                () -> assertFalse(inventory.isFull()));
    }

    // Uses a distinct item whose equals method reports a match, then tries lookup and removal.
    @Test
    void containsAndRemoveUseObjectIdentityEvenWhenEqualsReportsAMatch() {
        Inventory inventory = new Inventory(1);
        Item stored = item("same");
        Item equalButDistinct = new Item('!', Color.WHITE, "same", "same") {
            @Override
            public boolean equals(Object other) {
                return other instanceof Item;
            }

            @Override
            public int hashCode() {
                return 1;
            }
        };
        inventory.add(stored);

        assertFalse(inventory.contains(equalButDistinct));
        inventory.remove(equalButDistinct);

        assertSame(stored, inventory.get(0));
    }

    // Queries contains(null) before filling a slot, while it is filled, and after removal.
    @Test
    void containsNullReflectsWhetherAnySlotIsEmpty() {
        Inventory inventory = new Inventory(1);
        Item item = item("item");

        assertTrue(inventory.contains(null));
        inventory.add(item);
        assertFalse(inventory.contains(null));
        inventory.remove(item);
        assertTrue(inventory.contains(null));
    }

    // Stores an item beside an empty slot, removes null, and inspects the remaining contents.
    @Test
    void removeNullDoesNotChangeAnyOccupiedSlot() {
        Inventory inventory = new Inventory(2);
        Item item = item("item");
        inventory.add(item);

        inventory.remove(null);

        assertArrayEquals(new Item[] { item, null }, inventory.getItems());
    }

    // Writes directly through getItems(), then reads the inventory through its other methods.
    @Test
    void getItemsExposesTheBackingArray() {
        Inventory inventory = new Inventory(1);
        Item item = item("item");

        inventory.getItems()[0] = item;

        assertAll(
                () -> assertSame(item, inventory.get(0)),
                () -> assertTrue(inventory.contains(item)),
                () -> assertTrue(inventory.isFull()));
    }
}