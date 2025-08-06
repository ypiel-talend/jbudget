package org.github.ypiel.jbudget.controller;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import org.github.ypiel.jbudget.model.Account;
import org.github.ypiel.jbudget.model.AccountCSVFormat;
import org.github.ypiel.jbudget.model.Entry;
import org.github.ypiel.jbudget.model.EntryCategory;

public class MainController implements Initializable {

    @FXML
    public TextField tfSearchLabel;
    @FXML
    public DatePicker dpFrom;
    @FXML
    public DatePicker dpTo;
    @FXML
    public CheckBox cbDateRange;
    @FXML
    public HBox dateRangeBox;
    @FXML
    public ComboBox<EntryCategory> cbCategory;
    @FXML
    public ComboBox<EntryCategory> cbCategorySetter;
    @FXML
    public TextField tfDescriptionSetter;
    @FXML
    public CheckBox cbForceDescription;
    @FXML
    private TableView<Entry> transactionTable;
    @FXML
    private ComboBox<Account> accountComboBox;
    @FXML
    private Label statusLabel;

    @FXML
    private TableColumn<Entry, Account> accountColumn;
    @FXML
    private TableColumn<Entry, LocalDate> dateOperationColumn;
    @FXML
    private TableColumn<Entry, LocalDate> dateValueColumn;
    @FXML
    private TableColumn<Entry, String> labelColumn;
    @FXML
    private TableColumn<Entry, String> descriptionColumn;
    @FXML
    private TableColumn<Entry, Double> debitColumn;
    @FXML
    private TableColumn<Entry, Double> creditColumn;
    @FXML
    private TableColumn<Entry, EntryCategory> categoryColumn;

    private final Set<Entry> allEntries = new TreeSet<>((e1, e2) -> {
        int cmp = e1.dateOperation().compareTo(e2.dateOperation());
        if (cmp == 0) {
            cmp = e1.label().compareToIgnoreCase(e2.label());
        }
        return cmp;
    });
    private final Set<Account> accounts = new TreeSet<>((a1, a2) -> a1.name().compareToIgnoreCase(a2.name()));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Path baseDirectory = Path.of("C:", "YIE", "tmp", "jbudget");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableView();
        initializeAccounts();
        initializeAccountCombobox();
        initializeSearchPanel();
        initializeUpdatePanel();

        statusLabel.setText("Ready - Select an account and load transactions");
    }

    private void initializeSearchPanel() {
        dateRangeBox.visibleProperty().bind(cbDateRange.selectedProperty());
        cbCategory.getItems().setAll(EntryCategory.values());
        cbCategory.getSelectionModel().select(EntryCategory.ALL);
    }

    private void initializeUpdatePanel() {
        cbCategorySetter.getItems().setAll(EntryCategory.values());
        cbCategorySetter.getSelectionModel().select(EntryCategory.ALL);
    }

    private void initializeAccountCombobox() {
        accountComboBox.setCellFactory(lv -> new ListCell<Account>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                } else {
                    setText(account.toLabel());
                }
            }
        });

        accountComboBox.setButtonCell(new ListCell<Account>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                } else {
                    setText(account.toLabel());
                }
            }
        });

        accountComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void initializeTableView() {
        transactionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        accountColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().account())
        );
        accountColumn.setCellFactory(column -> new TableCell<Entry, Account>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) {
                    setText(null);
                } else {
                    setText(account.toLabel());
                }
            }
        });

        dateOperationColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().dateOperation())
        );
        dateValueColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().dateValue())
        );
        labelColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().label())
        );
        descriptionColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().description())
        );
        debitColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().debit())
        );
        creditColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().credit())
        );
        categoryColumn.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().category())
        );

        // Make description column take remaining space
        descriptionColumn.prefWidthProperty().bind(
                transactionTable.widthProperty()
                        .subtract(accountColumn.widthProperty())
                        .subtract(dateOperationColumn.widthProperty())
                        .subtract(dateValueColumn.widthProperty())
                        .subtract(labelColumn.widthProperty())
                        .subtract(debitColumn.widthProperty())
                        .subtract(creditColumn.widthProperty())
                        .subtract(categoryColumn.widthProperty())
                        .subtract(20) // account for scrollbar and borders
        );
    }

    private void initializeAccounts() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#0.00", symbols);
        df.setParseBigDecimal(true);

        // Initialize with some default accounts
        accounts.add(new Account("MyBank", "CCFChequePerso", "12345", 0.00,
                new AccountCSVFormat(0, 1, 2, 3, 4, "dd/MM/yyyy", "dd/MM/yyyy", df, ";")));
        accounts.add(new Account("MyBank", "Savings Account", "SAV456", 5000.00, new AccountCSVFormat(0, 1, 2, 3, 4, "dd/MM/yyyy", "dd/MM/yyyy", df, ";")));
    }

    @FXML
    private void handleUpdate(){
        final String description = tfDescriptionSetter.getText().trim();
        final boolean forceDescription = cbForceDescription.isSelected();
        final EntryCategory category = cbCategorySetter.getSelectionModel().getSelectedItem();

        Collection<Entry> updatedEntries = new ArrayList<>();
        ObservableList<Entry> entries = transactionTable.getSelectionModel().getSelectedItems();
        for(Entry e : entries) {

            Entry initialEntry = e; // Keep a reference to the initial entry for debugging

            if (!description.isEmpty() && (forceDescription || e.description().isEmpty())) {
                e = e.withDescription(description);
            }
            if (category != EntryCategory.ALL) {
                e = e.withCategory(category);
            }

            updatedEntries.add(e);
        }

        allEntries.removeAll(entries);
        allEntries.addAll(updatedEntries);
        handleSearch();
    }

    @FXML
    private void handleSearch() {
        String searchLabel = tfSearchLabel.getText().trim();
        EntryCategory category = cbCategory.getSelectionModel().getSelectedItem();
        LocalDate fromDate = dpFrom.getValue();
        LocalDate toDate = dpTo.getValue();

        List<Entry> filteredEntries = this.allEntries.stream()
                .filter(entry -> searchLabel.isEmpty() ||
                        entry.label().toLowerCase().contains(searchLabel.toLowerCase()))
                .filter(entry -> category == EntryCategory.ALL || category == entry.category())
                .filter(entry -> !cbDateRange.isSelected() || ((fromDate == null || !entry.dateOperation().isBefore(fromDate)) &&
                        (toDate == null || !entry.dateOperation().isAfter(toDate))))
                .toList();

        transactionTable.getItems().setAll(filteredEntries);
        statusLabel.setText(String.format("Found %d / %d transactions matching criteria", filteredEntries.size(), allEntries.size()));

    }

    @FXML
    private void handleLoadTransactions() {
        Account selectedAccount = accountComboBox.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            showAlert("Warning", "Please select an account first");
            return;
        }

        try {
            Path accountPath = baseDirectory.resolve(selectedAccount.name());
            if (!Files.exists(accountPath)) {
                statusLabel.setText("No transaction directory found for this account");
                return;
            }


            AccountCSVFormat format = selectedAccount.csvFormat();

            allEntries.clear();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(accountPath, "*.csv")) {
                for (Path file : directoryStream) {
                    allEntries.addAll(parseCSVFile(file, selectedAccount, format));
                }
            }

            transactionTable.getItems().setAll(allEntries);
            statusLabel.setText(String.format("Loaded %d transactions for account %s",
                    allEntries.size(), selectedAccount.name()));
        } catch (IOException | CsvValidationException e) {
            showAlert("Error", "Failed to load transactions: " + e.getMessage());
        }
    }

    private List<Entry> parseCSVFile(Path file, Account account, AccountCSVFormat format)
            throws IOException, CsvValidationException {
        List<Entry> entries = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file.toFile()))
                .withSkipLines(1) // Skip header
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(format.delimiter().charAt(0))
                        .build())
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    LocalDate dateOperation = LocalDate.parse(line[format.dateOperationIndex()],
                            DateTimeFormatter.ofPattern(format.dateOperationFormat()));
                    LocalDate dateValue = LocalDate.parse(line[format.dateValueIndex()],
                            DateTimeFormatter.ofPattern(format.dateValueFormat()));

                    String label = line[format.labelIndex()].trim();

                    String sDebit = line[format.debitIndex()].trim();
                    double debit = 0;
                    if (!sDebit.isEmpty()) {
                        debit = format.decimalFormat().parse(sDebit).doubleValue();
                    }

                    String sCredit = line[format.creditIndex()].trim();
                    double credit = 0;
                    if (!sCredit.isEmpty()) {
                        credit = format.decimalFormat().parse(sCredit).doubleValue();
                    }

                    entries.add(new Entry(account, dateOperation, dateValue, label,
                            "", debit, credit, EntryCategory.MISC));
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + Arrays.toString(line));
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return entries;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public MainController() {
        super();
    }
}
