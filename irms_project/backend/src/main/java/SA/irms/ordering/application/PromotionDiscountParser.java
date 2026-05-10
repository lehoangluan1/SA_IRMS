package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import SA.irms.common.api.ApiErrorResponse;
import SA.irms.common.error.ValidationException;

@Component
class PromotionDiscountParser {
    private static final Pattern DISCOUNT_PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern DISCOUNT_AMOUNT_PATTERN = Pattern.compile("\\$?(\\d+(?:\\.\\d+)?)");

    ParsedDiscount parseDiscount(String discount) {
        Matcher percentageMatcher = DISCOUNT_PERCENT_PATTERN.matcher(discount);
        if (percentageMatcher.find()) {
            return new ParsedDiscount("percentage", new BigDecimal(percentageMatcher.group(1)));
        }
        Matcher amountMatcher = DISCOUNT_AMOUNT_PATTERN.matcher(discount);
        if (amountMatcher.find()) {
            return new ParsedDiscount("amount", new BigDecimal(amountMatcher.group(1)));
        }
        throw new ValidationException("Discount format is not supported.",
                List.of(new ApiErrorResponse.FieldError("discount", "Use a percentage such as 10% or an amount such as $5.")));
    }

    Instant parseDate(String validUntil) {
        if (validUntil == null || validUntil.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(validUntil).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            throw new ValidationException("Promotion date is invalid.",
                    List.of(new ApiErrorResponse.FieldError("validUntil", "Use the ISO date format YYYY-MM-DD.")));
        }
    }

    String formatDiscount(String type, BigDecimal value) {
        if ("percentage".equals(type)) {
            return value.stripTrailingZeros().toPlainString() + "% off";
        }
        return "$" + value.stripTrailingZeros().toPlainString() + " off";
    }

    record ParsedDiscount(String type, BigDecimal value) {
    }
}
