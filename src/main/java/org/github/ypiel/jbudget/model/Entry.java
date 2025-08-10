package org.github.ypiel.jbudget.model;

import java.time.LocalDate;
import java.util.Collection;

public record Entry(Account account, LocalDate dateOperation, LocalDate dateValue, String label, String description,
                    double debit, double credit, EntryCategory category,
                    boolean newEntry, boolean duplicate) implements Comparable<Entry> {

    public Entry {
        if (category == null) {
            category = EntryCategory.MISC;
        }

        if (account == null || dateOperation == null || dateValue == null || label == null || description == null || (debit < 0 && credit < 0)) {
            throw new IllegalArgumentException("Entry with wrong parameters: " + String.format("account: %s, dateOperation: %s, dateValue: %s, label: %s, description: %s, debit: %.2f, credit: %.2f",
                    account, dateOperation, dateValue, label, description, debit, credit));
        }

    }

    public Entry(Account account, LocalDate dateOperation, LocalDate dateValue, String label, String description,
                 double debit, double credit, EntryCategory category) {
        this(account, dateOperation, dateValue, label, description, debit, credit, category, false, false);
    }

    public Entry withAccount(Account newAccount) {
        return new Entry(newAccount, dateOperation, dateValue, label, description, debit, credit, category, newEntry, duplicate);
    }

    public Entry withDescription(String newDescription) {
        return new Entry(account, dateOperation, dateValue, label, newDescription, debit, credit, category, newEntry, duplicate);
    }

    public Entry withCategory(EntryCategory newCategory) {
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, newCategory, newEntry, duplicate);
    }

    public Entry isDuplicate(){
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, category, newEntry, true);
    }

    public Entry isNotDuplicate(){
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, category, newEntry, false);
    }

    public Entry isNotNew(){
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, category, false, duplicate);
    }

    public double value(){
        if(debit > 0){
            return debit * -1;
        }

         return credit;
    }

    @Override
    public int compareTo(Entry e) {
        // Compare by dateOperation (ascending)
        int cmp = this.dateOperation().compareTo(e.dateOperation());
        if (cmp != 0) return cmp;

        // Compare by dateValue (ascending)
        cmp = this.dateValue().compareTo(e.dateValue());
        if (cmp != 0) return cmp;

        // Compare by label (case-insensitive)
        cmp = this.label().compareTo(e.label());
        if (cmp != 0) return cmp;

        // Compare by debit (ascending)
        cmp = Double.compare(this.debit(), e.debit());
        if (cmp != 0) return cmp;

        // Compare by credit (ascending)
        cmp = Double.compare(this.credit(), e.credit());
        if (cmp != 0) return cmp;

        // Compare by account (if Account implements Comparable)
        return this.account().compareTo(e.account());
    }

    /**
     *
     * @param collection The collection in which the given entry is search.
     * @param entry The entry to search in the collection.
     * @return true if an entry same princiapl attribute as those from in the given entry is found in the collection.
     */
    public static boolean contains(final Collection<Entry> collection, final Entry entry){
        return collection.stream().parallel().anyMatch(e -> e.compareTo(entry) == 0);
    }

}
