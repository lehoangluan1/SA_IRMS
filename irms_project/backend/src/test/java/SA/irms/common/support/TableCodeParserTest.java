package SA.irms.common.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TableCodeParserTest {
    @Test
    void extractsDigitsFromStandardTableCode() {
        Assertions.assertEquals(12, TableCodeParser.parseTableNumber("T12"));
    }

    @Test
    void extractsDigitsFromNonStandardTableCode() {
        Assertions.assertEquals(16, TableCodeParser.parseTableNumber("VIP-16"));
    }

    @Test
    void fallsBackWhenNoDigitsExist() {
        Assertions.assertEquals(0, TableCodeParser.parseTableNumber("ChefTable"));
    }
}
