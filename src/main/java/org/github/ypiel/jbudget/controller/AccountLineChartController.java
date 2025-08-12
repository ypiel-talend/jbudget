package org.github.ypiel.jbudget.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.KeyCode;

import org.github.ypiel.jbudget.model.Entry;

public class AccountLineChartController {

    private final Collection<Entry> entries;
    private final LineChart<String, Double> lineChart;
    private final CategoryAxis xAxis;
    private final Axis<Double> yAxis; // Changed to Axis<Double>

    // Store original data for zoom calculations
    private List<String> allCategories;
    private LinkedHashMap<String, Double> allData;
    private XYChart.Series<String, Double> originalSeries;

    // Zoom state
    private double minY = Double.MAX_VALUE;
    private double maxY = Double.MIN_VALUE;
    private int startCategoryIndex = 0;
    private int endCategoryIndex = 0;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    public AccountLineChartController(final Collection<Entry> entries, final LineChart<String, Double> lineChart) {
        this.entries = entries;
        this.lineChart = lineChart;
        this.xAxis = (CategoryAxis) lineChart.getXAxis();
        this.yAxis = lineChart.getYAxis(); // Removed cast
        setupZoomAndPan();
    }

    public void computeGraph() {
        Map<LocalDate, Double> soldesParMois = this.entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dateValue().with(TemporalAdjusters.lastDayOfMonth()),
                        TreeMap::new,  // Pour garder l'ordre chronologique
                        Collectors.summingDouble(Entry::value)
                ));

        // Create cumulative balance with formatted dates
        allData = new LinkedHashMap<>();
        double cumulativeBalance = 0.0;

        for (Map.Entry<LocalDate, Double> entry : soldesParMois.entrySet()) {
            cumulativeBalance += entry.getValue();
            String formattedDate = entry.getKey().format(MONTH_FORMATTER);
            allData.put(formattedDate, cumulativeBalance);

            // Track min/max values
            minY = Math.min(minY, cumulativeBalance);
            maxY = Math.max(maxY, cumulativeBalance);
        }

        // Store all categories in order
        allCategories = allData.keySet().stream().collect(Collectors.toList());

        // Create original series
        originalSeries = new XYChart.Series<>();
        allData.entrySet().forEach(e ->
                originalSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()))
        );

        // Initialize zoom state
        startCategoryIndex = 0;
        endCategoryIndex = allCategories.size() - 1;

        lineChart.getData().setAll(originalSeries);
        resetAxes();
    }

    private void setupZoomAndPan() {
        lineChart.setOnScroll(event -> {
            if (allCategories == null || allCategories.isEmpty()) return;

            // Get mouse position relative to the chart
            double mouseX = event.getX();
            double chartWidth = lineChart.getWidth() - lineChart.getPadding().getLeft() - lineChart.getPadding().getRight();

            // Calculate which category is under the mouse
            int visibleCategoryCount = endCategoryIndex - startCategoryIndex + 1;
            double categoryWidth = chartWidth / Math.max(1, visibleCategoryCount - 1); // -1 for line chart spacing
            int mouseCategory = (int) (mouseX / categoryWidth) + startCategoryIndex;
            mouseCategory = Math.max(startCategoryIndex, Math.min(endCategoryIndex, mouseCategory));

            double deltaY = event.getDeltaY();

            if (deltaY > 0) {
                // Zoom in
                zoomIn(mouseCategory);
            } else {
                // Zoom out
                zoomOut(mouseCategory);
            }

            event.consume();
        });

        // Double-click to reset zoom
        lineChart.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                resetZoom();
                event.consume();
            }
        });

        // Add panning functionality
        setupPanning();
    }

    private void zoomIn(int focusCategory) {
        int currentSpan = endCategoryIndex - startCategoryIndex + 1;
        if (currentSpan <= 3) return; // Minimum zoom level for line charts

        int newSpan = Math.max(3, (int) (currentSpan * 0.8)); // Zoom to 80% of current span
        int spanReduction = currentSpan - newSpan;

        // Calculate how much to move start/end based on focus point
        double focusRatio = (double) (focusCategory - startCategoryIndex) / currentSpan;
        int leftReduction = (int) (spanReduction * focusRatio);
        int rightReduction = spanReduction - leftReduction;

        startCategoryIndex = Math.max(0, startCategoryIndex + leftReduction);
        endCategoryIndex = Math.min(allCategories.size() - 1, endCategoryIndex - rightReduction);

        updateChart();
    }

    private void zoomOut(int focusCategory) {
        int currentSpan = endCategoryIndex - startCategoryIndex + 1;
        if (currentSpan >= allCategories.size()) return; // Already fully zoomed out

        int newSpan = Math.min(allCategories.size(), (int) (currentSpan * 1.25)); // Zoom out to 125%
        int spanIncrease = newSpan - currentSpan;

        // Calculate how much to expand start/end based on focus point
        double focusRatio = (double) (focusCategory - startCategoryIndex) / currentSpan;
        int leftExpansion = (int) (spanIncrease * focusRatio);
        int rightExpansion = spanIncrease - leftExpansion;

        startCategoryIndex = Math.max(0, startCategoryIndex - leftExpansion);
        endCategoryIndex = Math.min(allCategories.size() - 1, endCategoryIndex + rightExpansion);

        updateChart();
    }

    private void updateChart() {
        // Update X-axis categories
        List<String> visibleCategories = allCategories.subList(startCategoryIndex, endCategoryIndex + 1);
        xAxis.getCategories().setAll(visibleCategories);

        // Create filtered series maintaining line continuity
        XYChart.Series<String, Double> filteredSeries = new XYChart.Series<>();

        for (int i = startCategoryIndex; i <= endCategoryIndex; i++) {
            String category = allCategories.get(i);
            Double value = allData.get(category);
            filteredSeries.getData().add(new XYChart.Data<>(category, value));
        }

        // Update Y-axis range based on visible data (only if it's a NumberAxis)
        //if (yAxis instanceof NumberAxis numberAxis) {
            double visibleMinY = filteredSeries.getData().stream()
                    .mapToDouble(data -> data.getYValue())
                    .min().orElse(minY);
            double visibleMaxY = filteredSeries.getData().stream()
                    .mapToDouble(data -> data.getYValue())
                    .max().orElse(maxY);

            // Add some padding (10% of the range)
            double yRange = visibleMaxY - visibleMinY;
            double yPadding = yRange > 0 ? yRange * 0.1 : Math.abs(visibleMaxY) * 0.1;


          /*  numberAxis.setAutoRanging(false);
            numberAxis.setLowerBound(visibleMinY - yPadding);
            numberAxis.setUpperBound(visibleMaxY + yPadding);*/
        //}

        lineChart.getData().setAll(filteredSeries);
    }

    private void resetZoom() {
        startCategoryIndex = 0;
        endCategoryIndex = allCategories.size() - 1;
        resetAxes();
        lineChart.getData().setAll(originalSeries);
    }

    private void resetAxes() {
        // Reset X-axis
        xAxis.getCategories().setAll(allCategories);

        // Reset Y-axis (only if it's a NumberAxis)
       /* if (yAxis instanceof NumberAxis numberAxis) {
            numberAxis.setAutoRanging(false);
            double yRange = maxY - minY;
            double yPadding = yRange > 0 ? yRange * 0.1 : Math.abs(maxY) * 0.1;
            numberAxis.setLowerBound(minY - yPadding);
            numberAxis.setUpperBound(maxY + yPadding);
        }*/
    }

    private void setupPanning() {
        lineChart.setOnKeyPressed(event -> {
            int visibleSpan = endCategoryIndex - startCategoryIndex + 1;
            int panStep = Math.max(1, visibleSpan / 10);

            if (event.getCode() == KeyCode.LEFT) {
                if (startCategoryIndex > 0) {
                    startCategoryIndex = Math.max(0, startCategoryIndex - panStep);
                    endCategoryIndex = Math.min(allCategories.size() - 1, startCategoryIndex + visibleSpan - 1);
                    updateChart();
                }
            } else if (event.getCode() == KeyCode.RIGHT) {
                if (endCategoryIndex < allCategories.size() - 1) {
                    endCategoryIndex = Math.min(allCategories.size() - 1, endCategoryIndex + panStep);
                    startCategoryIndex = Math.max(0, endCategoryIndex - visibleSpan + 1);
                    updateChart();
                }
            }
            event.consume();
        });

        lineChart.setFocusTraversable(true); // Allow keyboard focus
    }

    private record EntryMonthValue(LocalDate month, Double value) {}
}
