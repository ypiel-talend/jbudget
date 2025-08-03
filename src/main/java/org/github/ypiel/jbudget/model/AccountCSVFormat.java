package org.github.ypiel.jbudget.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;

// Record for defining CSV format for each account
public record AccountCSVFormat(
        int dateOperationIndex,
        int dateValueIndex,
        int labelIndex,
        int debitIndex,
        int creditIndex,
        String dateOperationFormat,
        String dateValueFormat,
        DecimalFormat decimalFormat,
        String delimiter) {
}