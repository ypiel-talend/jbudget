package org.github.ypiel.jbudget.controller;

import java.awt.Container;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.github.ypiel.jbudget.model.Entry;

public class SoldGraphController {

    private final LineChart<String, Number> balanceChart;

    private Collection<Entry> allEntries = new ArrayList<>();
    private Collection<Entry> currentEntries = new ArrayList<>();
    private Map<String, Double> monthlyBalances;
    private List<String> allMonths;
    private boolean isDragging = false;
    private String startMonth = null;
    private String endMonth = null;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private Rectangle selectionRectangle;

    public SoldGraphController(final LineChart<String, Number> balanceChart) {
        this.balanceChart = balanceChart;
        setupChart();
    }

    public void setEntries(Collection<Entry> entries) {
        this.allEntries = new ArrayList<>(entries);
        this.currentEntries = new ArrayList<>(entries);
        calculateMonthlyBalances();
        updateChart();
    }

    private void setupChart() {
        balanceChart.getXAxis().setLabel("Month");
        balanceChart.getYAxis().setLabel("Balance");
       /* balanceChart.setOnMousePressed(this::onMousePressed);
        balanceChart.setOnMouseDragged(this::onMouseDragged);
        balanceChart.setOnMouseReleased(this::onMouseReleased);*/

        balanceChart.getXAxis().setLabel("Month");
        balanceChart.getYAxis().setLabel("Balance (€)");

        // balanceChart.setTitle("Monthly Balance Evolution");
        balanceChart.setCreateSymbols(true);
        balanceChart.setLegendVisible(true);

        selectionRectangle = new Rectangle();
        selectionRectangle.setFill(Color.RED.deriveColor(0, 1, 1, 0.3)); // Semi-transparent red
        selectionRectangle.setStroke(Color.RED);
        selectionRectangle.setStrokeWidth(3);
        selectionRectangle.setX(0);
        selectionRectangle.setY(0);
        selectionRectangle.setWidth(0);
        selectionRectangle.setHeight(0);
        selectionRectangle.setVisible(false);
        selectionRectangle.setMouseTransparent(true);
        ((Pane)balanceChart.getParent()).getChildren().add(selectionRectangle);

        balanceChart.setOnMousePressed(event -> {
            System.out.println("balanceChart.setOnMousePressed");
            if (event.isPrimaryButtonDown()) {
                double startX = event.getX();
                double startY = event.getY();
                // Position and show rectangle
                selectionRectangle.setX(startX);
                selectionRectangle.setY(startY);
                selectionRectangle.setWidth(0);
                selectionRectangle.setHeight(0);
                selectionRectangle.setVisible(true);
            }
        });

        balanceChart.setOnMouseDragged(event -> {
            System.out.printf("balanceChart.setOnMouseDragged, visible : %s, %s x %s x %s x %s %n", selectionRectangle.isVisible(),
                    selectionRectangle.getX(), selectionRectangle.getY(), selectionRectangle.getWidth(), selectionRectangle.getHeight());
            if (event.isPrimaryButtonDown() && selectionRectangle.isVisible()) {
                double currentX = event.getX();
                double currentY = event.getY();

                double startX = selectionRectangle.getX();
                double startY = selectionRectangle.getY();

                // Update rectangle dimensions
                double rectX = Math.min(startX, currentX);
                double rectY = Math.min(startY, currentY);
                double rectWidth = Math.abs(currentX - startX);
                double rectHeight = Math.abs(currentY - startY);

                selectionRectangle.setX(rectX);
                selectionRectangle.setY(rectY);
                selectionRectangle.setWidth(rectWidth);
                selectionRectangle.setHeight(rectHeight);
            }
        });

        balanceChart.setOnMouseReleased(event -> {
            if (selectionRectangle.isVisible()) {
                double startX = selectionRectangle.getX();
                double endX = event.getX();

                System.out.println("balanceChart.setOnMouseReleased : " + Math.abs(endX - startX));
                if (Math.abs(endX - startX) > 10) { // Minimum selection width
                    System.out.println("filterEntriesBySelection ok!");
                    filterEntriesBySelection(startX, endX);
                }

                selectionRectangle.setVisible(false);
            }
        });

        balanceChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                resetZoom();
                event.consume();
            }
        });
    }

    private void calculateMonthlyBalances() {
        if (currentEntries == null || currentEntries.isEmpty()) {
            monthlyBalances = new LinkedHashMap<>();
            allMonths = new ArrayList<>();
            return;
        }

        // Group entries by month and calculate monthly totals
        Map<YearMonth, Double> monthlyTotals = currentEntries.stream()
                .collect(Collectors.groupingBy(
                        entry -> YearMonth.from(entry.dateOperation()),
                        TreeMap::new,
                        Collectors.summingDouble(Entry::value)
                ));

        // Calculate cumulative balances
        monthlyBalances = new LinkedHashMap<>();
        allMonths = new ArrayList<>();
        double cumulativeBalance = 0.0;

        for (Map.Entry<YearMonth, Double> monthEntry : monthlyTotals.entrySet()) {
            String monthKey = monthEntry.getKey().format(MONTH_FORMATTER);
            cumulativeBalance += monthEntry.getValue();
            monthlyBalances.put(monthKey, cumulativeBalance);
            allMonths.add(monthKey);
        }
    }

    private void updateChart() {
        balanceChart.getData().clear();

        if (monthlyBalances.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Balance");

        for (Map.Entry<String, Double> entry : monthlyBalances.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        balanceChart.getData().add(series);

        // Update x-axis categories
        ObservableList<String> categories = FXCollections.observableArrayList(monthlyBalances.keySet());
        ((CategoryAxis)balanceChart.getXAxis()).setCategories(categories);
    }

   /* private void onMousePressed(MouseEvent event) {
        startMonth = getMonthFromXPosition(event.getX());
        isDragging = false;
    }

    private void onMouseDragged(MouseEvent event) {
        isDragging = true;
        endMonth = getMonthFromXPosition(event.getX());
        // Could add visual feedback here (selection rectangle)

    }

    private void onMouseReleased(MouseEvent event) {
        filterEntriesBySelection(x);
    }*/

    private void filterEntriesBySelection(double startX, double endX){
        startMonth = getMonthFromXPosition(startX);
        endMonth = getMonthFromXPosition(endX);

        if (startMonth.equals(endMonth)) {
            return;
        }

        // Ensure start is before end
        if (compareMonths(startMonth, endMonth) > 0) {
            String temp = startMonth;
            startMonth = endMonth;
            endMonth = temp;
        }

        filterEntriesByMonthRange(startMonth, endMonth);
        calculateMonthlyBalances();
        updateChart();

        isDragging = false;
        startMonth = null;
        endMonth = null;
    }

    private String getMonthFromXPosition(double xPosition) {
        if (allMonths.isEmpty()) {
            return null;
        }

        // Get the chart area bounds
        double chartWidth = balanceChart.getWidth() - 100; // Approximate, accounting for margins
        double xOffset = 50; // Approximate left margin

        // Calculate relative position
        double relativeX = (xPosition - xOffset) / chartWidth;
        relativeX = Math.max(0, Math.min(1, relativeX));

        // Map to month index
        int monthIndex = (int) (relativeX * (allMonths.size() - 1));
        monthIndex = Math.max(0, Math.min(allMonths.size() - 1, monthIndex));

        return allMonths.get(monthIndex);
    }

    private int compareMonths(String month1, String month2) {
        YearMonth ym1 = YearMonth.parse(month1, MONTH_FORMATTER);
        YearMonth ym2 = YearMonth.parse(month2, MONTH_FORMATTER);
        return ym1.compareTo(ym2);
    }

    private void filterEntriesByMonthRange(String startMonth, String endMonth) {
        YearMonth start = YearMonth.parse(startMonth, MONTH_FORMATTER);
        YearMonth end = YearMonth.parse(endMonth, MONTH_FORMATTER);

        currentEntries = allEntries.stream()
                .filter(entry -> {
                    YearMonth entryMonth = YearMonth.from(entry.dateOperation());
                    return !entryMonth.isBefore(start) && !entryMonth.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    @FXML
    private void resetZoom() {
        currentEntries = new ArrayList<>(allEntries);
        calculateMonthlyBalances();
        updateChart();
    }

    public void refreshData() {
        if (allEntries != null) {
            setEntries(allEntries);
        }
    }
}
