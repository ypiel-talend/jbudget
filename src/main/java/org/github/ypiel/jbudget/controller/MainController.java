package org.github.ypiel.jbudget.controller;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
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
import org.github.ypiel.jbudget.model.AccountTotal;
import org.github.ypiel.jbudget.model.Entry;
import org.github.ypiel.jbudget.model.EntryCategory;

public class MainController implements Initializable {

    private static final Path baseDirectory = Path.of("C:", "YIE", "tmp", "jbudget");
    private static final int maxUpdateEntriesWithoutConfirmation = 5;
    private static final Path OUTPUT_FOLDER = Path.of("C:", "YIE", "tmp", "jbudget", "output");
    private static final Path OUTPUT_FILE = OUTPUT_FOLDER.resolve("jbudget.json");
    private static final double ZOOM_FACTOR = 1.1;

    private static final Account ALL_ACCOUNT = new Account("", "All accounts", "", 0);

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
    public ScrollPane barChartScrollPane;
    @FXML
    public LineChart balanceChart;
    @FXML
    public TableView<AccountTotal> totalTable;
    @FXML
    public TableColumn<AccountTotal, String> accountTotalColumn;
    @FXML
    public TableColumn<AccountTotal, Double> totalColumn;
    @FXML
    public ComboBox<Account> accountSearchComboBox;
    @FXML
    public TextField tfDelete;
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

    private boolean accountBarChartIsPanning = false;
    private double accountBarChartLastPanX;
    private double accountBarChartLastPanY;

    private SoldGraphController soldGraphController;

    @FXML
    private LineChart<String, Number> balance2Chart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableView();
        initializeAccounts();
        initializeAccountCombobox();
        initializeSearchPanel();
        initializeUpdatePanel();
        initializeBarChartTab();
        initializeTotalTable();

        statusLabel.setText("Ready - Select an account and load transactions");

        loadFromJson();

        soldGraphController = new SoldGraphController(balance2Chart);
        soldGraphController.setEntries(this.allEntries);
        //soldGraphController.refreshData();
    }

    private void initializeTotalTable() {
        accountTotalColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().account()));

        totalColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().total()));

        totalColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : "%.2f".formatted(item));
            }
        });
    }

    public void displayTotals(Map<String, Double> totalsMap) {
        List<AccountTotal> list = totalsMap.entrySet()
                .stream()
                .map(e -> new AccountTotal(e.getKey(), e.getValue()))
                .sorted(new Comparator<AccountTotal>() {
                    @Override
                    public int compare(AccountTotal at1, AccountTotal at2) {
                        return at1.account().compareTo(at2.account());
                    }
                })
                .toList();

        list = new ArrayList<>(list);
        double total = list.stream().mapToDouble(AccountTotal::total).sum();
        list.add(new AccountTotal("Total", total));
        totalTable.setItems(FXCollections.observableArrayList(new ArrayList<>(list)));
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


        accountBarChart.setOnScroll(event -> {
            double deltaY = event.getDeltaY();

            if (deltaY > 0) {
                // Zoom in
                accountBarChart.setScaleX(accountBarChart.getScaleX() * ZOOM_FACTOR);
                accountBarChart.setScaleY(accountBarChart.getScaleY() * ZOOM_FACTOR);
            } else {
                // Zoom out
                accountBarChart.setScaleX(accountBarChart.getScaleX() / ZOOM_FACTOR);
                accountBarChart.setScaleY(accountBarChart.getScaleY() / ZOOM_FACTOR);
            }

            event.consume();
        });

        accountBarChart.setOnMouseReleased(event -> {
            if (accountBarChartIsPanning) {
                accountBarChartIsPanning = false;
                accountBarChart.setCursor(Cursor.DEFAULT);
                event.consume();
            }
        });

        accountBarChart.setOnMousePressed(event -> {
            if (event.isMiddleButtonDown()) {
                accountBarChartIsPanning = true;
                accountBarChartLastPanX = event.getSceneX();
                accountBarChartLastPanY = event.getSceneY();
                accountBarChart.setCursor(Cursor.MOVE);
                event.consume();
            }
        });

        accountBarChart.setOnMouseDragged(event -> {
            if (accountBarChartIsPanning && event.isMiddleButtonDown()) {
                double deltaX = event.getSceneX() - accountBarChartLastPanX;
                double deltaY = event.getSceneY() - accountBarChartLastPanY;

                // Apply translation
                accountBarChart.setTranslateX(accountBarChart.getTranslateX() + deltaX);
                accountBarChart.setTranslateY(accountBarChart.getTranslateY() + deltaY);

                accountBarChartLastPanX = event.getSceneX();
                accountBarChartLastPanY = event.getSceneY();
                event.consume();
            }
        });

        // Optional: Reset on double-click
        accountBarChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                resetZoomAndPan(accountBarChart);
                event.consume();
            }
        });

        barChartScrollPane.setPannable(true);
        barChartScrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
        barChartScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        barChartScrollPane.setPrefHeight(400);


    }

    public void resetZoomAndPan(final XYChart chart) {
        chart.setScaleX(1.0);
        chart.setScaleY(1.0);
        chart.setTranslateX(0);
        chart.setTranslateY(0);
    }

    private void initializeSearchPanel() {
        dateRangeBox.managedProperty().bind(dateRangeBox.visibleProperty());
        dateRangeBox.visibleProperty().bind(cbDateRange.selectedProperty());

        cbCategory.getItems().setAll(EntryCategory.values());
        cbCategory.getSelectionModel().select(EntryCategory.ALL);

        accountSearchComboBox.setCellFactory(lv -> new ListCell<Account>() {
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

        accountSearchComboBox.setButtonCell(new ListCell<Account>() {
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

        List<Account> searchAccounts = new ArrayList<>(accounts);
        searchAccounts.add(0, ALL_ACCOUNT);
        accountSearchComboBox.setItems(FXCollections.observableArrayList(searchAccounts));
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

        AccountCSVFormat ccfFormat = new AccountCSVFormat(0, 1, 2,
                3, 4, "dd/MM/yyyy", "dd/MM/yyyy", df, ";");

        Account ccfCheque1Perso = new Account("CCF", "CCF_CHEQUE1_YVES", "FR7618079442560281578504008", 0.00);
        accounts.add(ccfCheque1Perso);
        csvFormatMap.put(ccfCheque1Perso, ccfFormat);

        Account ccfCheque2Commun = new Account("CCF", "CCF_CHEQUE2_COMMUN", "FR7618079442560281577954115", 0.00);
        accounts.add(ccfCheque2Commun);
        csvFormatMap.put(ccfCheque2Commun, ccfFormat);

        Account ccfLivDurableSolidaire = new Account("CCF", "CCF_LIV_DURABLE_SOLIDAIRE", "FR7618079442560281578505851", 0.00);
        accounts.add(ccfLivDurableSolidaire);
        csvFormatMap.put(ccfLivDurableSolidaire, ccfFormat);
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
        Account account = accountSearchComboBox.getSelectionModel().getSelectedItem();
        boolean onlyNew = cbOnlyNew.isSelected();
        boolean onlyDuplicate = cbOnlyDuplicates.isSelected();

        Stream<Entry> entryStream = this.allEntries.stream();

        if (account != null && account != ALL_ACCOUNT) {
            entryStream = entryStream.filter(e -> account.equals(e.account()));
        }

        if (!searchLabel.isEmpty()) {
            entryStream = entryStream.filter(e -> e.label().toLowerCase().contains(searchLabel.toLowerCase()));
        }

        if (category != EntryCategory.ALL) {
            entryStream = entryStream.filter(e -> category == e.category());
        }

        if (cbDateRange.isSelected()) {
            if (fromDate != null) {
                entryStream = entryStream.filter(e -> !e.dateOperation().isBefore(fromDate));
            }
            if (toDate != null) {
                entryStream = entryStream.filter(e -> !e.dateOperation().isAfter(toDate));
            }
        }

        if (onlyNew) {
            entryStream = entryStream.filter(Entry::newEntry);
        }

        if (onlyDuplicate) {
            entryStream = entryStream.filter(Entry::duplicate);
        }

        List<Entry> filteredEntries = entryStream.toList();

        transactionTable.getItems().setAll(filteredEntries.stream().sorted().toList());
        statusLabel.setText(String.format("Found %d / %d transactions matching criteria", filteredEntries.size(), allEntries.size()));

        updateTotals();
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
            Files.createDirectories(accountPath);

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

        if (file.getFileName().toString().startsWith("ok_")) {
            return Collections.emptyList();
        }

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

        Path doneFile = file.resolveSibling("ok_" + file.getFileName().toString());
        Files.move(file, doneFile);

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

            if (!Files.isRegularFile(OUTPUT_FILE)) {
                Files.writeString(OUTPUT_FILE, "[]", StandardOpenOption.CREATE_NEW);
            }

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

    private void updateTotals() {
        ObservableList<Entry> entries = transactionTable.getItems();
        Map<String, Double> collect = entries.stream()
                .filter(e -> !e.duplicate()) // Duplicates are ignored from totals
                .collect(
                        Collectors.groupingBy(Entry::account,
                                TreeMap::new,
                                Collectors.summingDouble(Entry::value)))
                .entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toLabel(), e -> e.getValue()));
        displayTotals(collect);
    }


    public void handleGenerateAccountBarGraph() {
        Account selectedAccount = graphicsAccountComboBox.getSelectionModel().getSelectedItem();
        AccountBarChartController accountBarChartController = new AccountBarChartController(allEntries, this.accountBarChart);
        accountBarChartController.computeGraph();

        AccountLineChartController accountLineChartController = new AccountLineChartController(allEntries, this.accountLineChart);
        accountLineChartController.computeGraph();

    }

    public void handleDelete() {
        if ("DELETE".equals(tfDelete.getText())) {
            List<Entry> toRemove = transactionTable.getSelectionModel().getSelectedItems().stream().toList();
            allEntries.removeAll(toRemove);
            handleSearch();
        }

        tfDelete.setText("");
    }
}
