package org.github.ypiel.jbudget.controller;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import org.github.ypiel.jbudget.model.Entry;

public class AccountBarChartController {

    private final Collection<Entry> entries;
    private final BarChart<String, Double>
            barChart;

    public AccountBarChartController(final Collection<Entry> entries, final BarChart<String, Double> barChart){
        this.entries = entries;
        this.barChart = barChart;
    }

    public void computeGraph(){
        XYChart.Series<String, Double> series = new XYChart.Series<>();

        Map<LocalDate, Double> collect = this.entries.stream()
                .map(e -> new EntryMonthValue(e.dateValue().with(TemporalAdjusters.lastDayOfMonth()), e.value()))
                .collect(Collectors.groupingBy(EntryMonthValue::month, TreeMap::new, Collectors.summingDouble(EntryMonthValue::value)));

        collect.entrySet().stream().forEach(e -> series.getData().add(new XYChart.Data<>(String.valueOf(e.getKey()), e.getValue())));

        barChart.getData().setAll(series);
    }

    private record EntryMonthValue(LocalDate month, Double value){}

}
