package com.bac.banking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Formats amounts as Botswana Pula, e.g. "BWP 1,250.00". */
public final class MoneyFormat {
    private MoneyFormat() {
    }

    public static String bwp(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return "BWP " + format.format(amount.setScale(2, RoundingMode.HALF_EVEN));
    }
}
