package org.github.ypiel.jbudget.model;

import java.time.LocalDate;

public record Entry(Account account, LocalDate dateOperation, LocalDate dateValue, String label, String description, double debit, double credit, EntryCategory category) {

    public Entry {
        if(category == null) {
            category = EntryCategory.MISC;
        }

        if (account == null || dateOperation == null || dateValue == null || label == null || description == null || (debit < 0 && credit < 0)) {
            throw new IllegalArgumentException("Entry with wrong parameters: " + String.format("account: %s, dateOperation: %s, dateValue: %s, label: %s, description: %s, debit: %.2f, credit: %.2f",
                    account, dateOperation, dateValue, label, description, debit, credit));
        }

    }


}
