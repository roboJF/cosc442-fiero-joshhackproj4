package game;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CombatRulesTest {
    
    @ParameterizedTest
    @CsvSource({
        "50, -51, -1, true",
        "50, -50, 0, true",
        "50, -49, 1, false",
        "50, 0, 50, false",
        "50, 49, 99, false",
        "50, 50, 100, false",
        "50, 51, 100, false",
    })
    void testHp(int currentHp, int amount, int expectedHp, boolean exIsDead){
        int hp = CombatRules.calculateNewHp(currentHp, 100, amount);
        assertEquals(expectedHp, hp);
        assertEquals(exIsDead, CombatRules.isDead(hp));
    }
}
