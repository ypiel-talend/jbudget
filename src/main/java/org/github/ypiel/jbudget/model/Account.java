package org.github.ypiel.jbudget.model;

public record Account(String bank, String name, String code, double initialBalance) implements Comparable<Account> {

    public Account {
        if (bank == null) {
            throw new IllegalArgumentException("Bank cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
    }

    public String toLabel() {
        return bank.isBlank() ? name : String.format("%s (%s)", name, bank);
    }

    @Override
    public int compareTo(Account o) {
        int bankComparison = this.bank.compareTo(o.bank);
        if (bankComparison != 0) {
            return bankComparison;
        }
        int nameComparison = this.name.compareTo(o.name);
        if (nameComparison != 0) {
            return nameComparison;
        }
        return this.code.compareTo(o.code);
    }
}
