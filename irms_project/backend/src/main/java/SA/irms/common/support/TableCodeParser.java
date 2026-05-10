package SA.irms.common.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TableCodeParser {
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    private TableCodeParser() {
    }

    public static int parseTableNumber(String tableCode) {
        if (tableCode == null || tableCode.isBlank()) {
            return 0;
        }
        Matcher matcher = DIGITS.matcher(tableCode);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
