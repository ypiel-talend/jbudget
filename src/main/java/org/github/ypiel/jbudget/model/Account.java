package org.github.ypiel.jbudget.model;

public record Account(String bank, String name, String code, double initialBalance, AccountCSVFormat csvFormat) {

    public Account {
        if (bank == null || bank.isBlank()) {
            throw new IllegalArgumentException("Bank cannot be null or empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        if (csvFormat == null) {
            throw new IllegalArgumentException("The account CSV format cannot be null");
        }
    }

    public String toLabel() {
        return String.format("%s (%s)", name, bank);
    }
}
