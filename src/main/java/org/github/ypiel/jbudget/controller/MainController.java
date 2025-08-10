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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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

    private static final Path baseDirectory = Path.of("C:", "YIE", "tmp", "jbudget");
    private static final int maxUpdateEntriesWithoutConfirmation = 5;
    private static final Path OUTPUT_FOLDER = Path.of("C:", "YIE", "tmp", "jbudget", "output");
    private static final Path OUTPUT_FILE = OUTPUT_FOLDER.resolve("jbudget.json");

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
    public CheckBox cbOnlyNew;
    @FXML
    public CheckBox cbOnlyDuplicates;
    @FXML
    public ComboBox<Account> graphicsAccountComboBox;
    @FXML
    public BarChart<String, Double> accountBarChart;
    @FXML
    public LineChart<String, Double> accountLineChart;
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

    private final List<Entry> allEntries = new ArrayList<>();
    private final Set<Account> accounts = new TreeSet<>();
    private final Map<Account, AccountCSVFormat> csvFormatMap = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableView();
        initializeAccounts();
        initializeAccountCombobox();
        initializeSearchPanel();
        initializeUpdatePanel();
        initializeBarChartTab();

        statusLabel.setText("Ready - Select an account and load transactions");

        loadFromJson();
    }

    private void initializeBarChartTab() {
        graphicsAccountComboBox.setCellFactory(lv -> new ListCell<Account>() {
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

        graphicsAccountComboBox.setButtonCell(new ListCell<Account>() {
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
        graphicsAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
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

        transactionTable.setRowFactory(tv -> new TableRow<Entry>() {
            @Override
            protected void updateItem(Entry entry, boolean empty) {
                super.updateItem(entry, empty);

                if (entry == null || empty) {
                    setStyle(""); // Reset style if row is empty
                } else {
                    // Check if the label contains "toto"
                    if (entry.newEntry() && entry.duplicate()) {
                        setStyle("-fx-background-color: red;");
                    } else if (entry.newEntry()) {
                        setStyle("-fx-background-color: lightgreen;"); // Green background
                    } else if (entry.duplicate()) {
                        setStyle("-fx-background-color: orange;"); // Green background
                    } else {
                        setStyle(""); // Default style
                    }
                }
            }
        });
    }

    private void initializeAccounts() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#0.00", symbols);
        df.setParseBigDecimal(true);

        // Initialize with some default accounts

        Account ccfChequePerso = new Account("MyBank", "CCFChequePerso", "12345", 0.00);
        AccountCSVFormat ccfChequePersoFormat = new AccountCSVFormat(0, 1, 2,
                3, 4, "dd/MM/yyyy", "dd/MM/yyyy", df, ";");
        accounts.add(ccfChequePerso);
        csvFormatMap.put(ccfChequePerso, ccfChequePersoFormat);

        Account anotherAccount = new Account("MyBank", "Savings Account", "SAV456", 5000.00);
        AccountCSVFormat anotherAccountFormat = new AccountCSVFormat(0, 1, 2,
                3, 4, "dd/MM/yyyy", "dd/MM/yyyy", df, ";");
        accounts.add(anotherAccount);
        csvFormatMap.put(anotherAccount, anotherAccountFormat);
    }

    @FXML
    private void handleSwitchADuplicate() {
        ObservableList<Entry> entries = transactionTable.getSelectionModel().getSelectedItems();
        if (entries.size() > maxUpdateEntriesWithoutConfirmation) {
            boolean confirmation = askConfirmation("Confirmation", "You are about to switch duplicate tag for several entries at once: " + entries.size() + " . Do you want to proceed?");
            if (!confirmation) {
                return;
            }
        }

        entries.stream().forEach(e -> {
            allEntries.remove(e);
            allEntries.add(e.duplicate() ? e.isNotDuplicate() : e.isDuplicate());
        });

        updateEntriesInTableView(String.format("Validation of %d transactions", entries.size()));
    }

    @FXML
    private void handleUpdate() {
        final String description = tfDescriptionSetter.getText().trim();
        final boolean forceDescription = cbForceDescription.isSelected();
        final EntryCategory category = cbCategorySetter.getSelectionModel().getSelectedItem();

        Collection<Entry> updatedEntries = new ArrayList<>();
        ObservableList<Entry> entries = transactionTable.getSelectionModel().getSelectedItems();

        if (entries.size() <= 0) {
            // If no selection, we update all visible entries
            entries = transactionTable.getItems();
        }

        if (entries.size() > maxUpdateEntriesWithoutConfirmation) {
            boolean confirmation = askConfirmation("Confirmation", "You are about to update " + entries.size() + " transactions. Do you want to proceed?");
            if (!confirmation) {
                return;
            }
        }

        for (Entry e : entries) {

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
        boolean onlyNew = cbOnlyNew.isSelected();
        boolean onlyDuplicate = cbOnlyDuplicates.isSelected();

        Stream<Entry> entryStream = this.allEntries.stream()
                .filter(entry -> searchLabel.isEmpty() ||
                        entry.label().toLowerCase().contains(searchLabel.toLowerCase()))
                .filter(entry -> category == EntryCategory.ALL || category == entry.category())
                .filter(entry -> !cbDateRange.isSelected() || ((fromDate == null || !entry.dateOperation().isBefore(fromDate)) &&
                        (toDate == null || !entry.dateOperation().isAfter(toDate))));

        if (onlyNew) {
            entryStream = entryStream.filter(Entry::newEntry);
        }

        if (onlyDuplicate) {
            entryStream = entryStream.filter(Entry::duplicate);
        }

        List<Entry> filteredEntries = entryStream.toList();

        transactionTable.getItems().setAll(filteredEntries.stream().sorted().toList());
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

            AccountCSVFormat format = csvFormatMap.get(selectedAccount);
            if (format == null) {
                showAlert("Wrong configuration",
                        String.format("No CSV format for account %s.", selectedAccount.toLabel(), true));
            }

            //allEntries.clear();
            final AtomicInteger nbAdded = new AtomicInteger();
            //List<Entry> rejected = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(accountPath, "*.csv")) {
                for (Path file : directoryStream) {
                    List<Entry> tmpEntries = parseCSVFile(file, selectedAccount, format);
                    tmpEntries.stream().forEach(e -> {
                        allEntries.add(Entry.contains(allEntries, e) ? e.isDuplicate() : e);
                        nbAdded.incrementAndGet();
                    });
                }
            }

            updateEntriesInTableView(String.format("%d transactions loaded", nbAdded.get()));
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
                            "", debit, credit, EntryCategory.MISC, true, false));

                } catch (Exception e) {
                    System.err.println("Error parsing line: " + Arrays.toString(line));
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        return entries.stream().toList();
    }

    private void showAlert(String title, String message) {
        showAlert(title, message, false);
    }

    private void showAlert(String title, String message, boolean shutdown) {
        Alert alert = new Alert(shutdown ? AlertType.ERROR : AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

        if (shutdown) {
            System.exit(1);
        }
    }

    private boolean askConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == yesButton;
    }

    public MainController() {
        super();
    }

    public void handleSave() {
        try {
            if (!Files.isDirectory(OUTPUT_FOLDER)) {
                Files.createDirectories(OUTPUT_FOLDER);
            }
            EntryJsonController.saveEntriesToFile(
                    this.allEntries,
                    OUTPUT_FILE.toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadFromJson() {
        try {

            Map<Account, Account> accountMap = accounts.stream()
                    .collect(Collectors.toMap(e -> e, e -> e));

            List<Entry> entries = EntryJsonController.loadEntriesFromFile(OUTPUT_FILE.toFile().getAbsolutePath());
            allEntries.clear();
            allEntries.addAll(entries.stream()
                    .map(e -> e.withAccount(accountMap.get(e.account())).isNotNew()).sorted().toList()); // Only 1 instance for each account
            updateEntriesInTableView(String.format("Loaded %d transactions from file %s",
                    allEntries.size(), OUTPUT_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateEntriesInTableView(String message) {
        Collections.sort(allEntries);
        transactionTable.getItems().setAll(allEntries);
        statusLabel.setText(message);
        handleSearch();
    }


    public void handleGenerateAccountBarGraph() {
        Account selectedAccount = graphicsAccountComboBox.getSelectionModel().getSelectedItem();
        AccountBarChartController accountBarChartController = new AccountBarChartController(allEntries, this.accountBarChart);
        accountBarChartController.computeGraph();

        AccountLineChartController accountLineChartController = new AccountLineChartController(allEntries, this.accountLineChart);
        accountLineChartController.computeGraph();

    }
}
