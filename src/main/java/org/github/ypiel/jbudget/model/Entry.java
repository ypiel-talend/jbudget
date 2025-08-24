package org.github.ypiel.jbudget.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;

public record Entry(Account account, LocalDate dateOperation, LocalDate dateValue, String label, String description,
                    BigDecimal debit, BigDecimal credit, List<EntryCategory> category,
                    boolean newEntry, boolean duplicate) implements Comparable<Entry> {

    public Entry {
        if (category == null) {
            category = Collections.emptyList();
        }

        if (account == null || dateOperation == null || dateValue == null || label == null || description == null || (debit.compareTo(ZERO) < 0 && credit.compareTo(ZERO) < 0)) {
            throw new IllegalArgumentException("Entry with wrong parameters: " + String.format("account: %s, dateOperation: %s, dateValue: %s, label: %s, description: %s, debit: %.2f, credit: %.2f",
                    account, dateOperation, dateValue, label, description, debit, credit));
        }

    }

    public Entry(Account account, LocalDate dateOperation, LocalDate dateValue, String label, String description,
                 BigDecimal debit, BigDecimal credit, List<EntryCategory> category) {
        this(account, dateOperation, dateValue, label, description, debit, credit, category, false, false);
    }

    public Entry withAccount(Account newAccount) {
        return new Entry(newAccount, dateOperation, dateValue, label, description, debit, credit, category, newEntry, duplicate);
    }

    public Entry withDescription(String newDescription) {
        return new Entry(account, dateOperation, dateValue, label, newDescription, debit, credit, category, newEntry, duplicate);
    }

    public Entry addCategory(EntryCategory newCategory) {
        List<EntryCategory> categories = Stream.concat(this.category.stream(), Stream.of(newCategory)).toList();
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, categories, newEntry, duplicate);
    }

    public Entry removeCategory(EntryCategory category){
        List<EntryCategory> categories = this.category.stream().filter(c -> category != EntryCategory.ALL && c != category).toList();
        return new Entry(account, dateOperation, dateValue, label, description, debit, credit, categories, newEntry, duplicate);
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

    public BigDecimal value(){
        if(debit.compareTo(ZERO) > 0){
            return debit.negate();
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
        cmp = this.debit().compareTo(e.debit());
        if (cmp != 0) return cmp;

        // Compare by credit (ascending)
        cmp = this.credit().compareTo(e.credit());
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
