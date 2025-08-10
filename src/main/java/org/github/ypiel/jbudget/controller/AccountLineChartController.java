package org.github.ypiel.jbudget.controller;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import org.github.ypiel.jbudget.model.Entry;

public class AccountLineChartController {

    private final Collection<Entry> entries;
    private final LineChart<String, Double> lineChart;

    public AccountLineChartController(final Collection<Entry> entries, final LineChart<String, Double> lineChart){
        this.entries = entries;
        this.lineChart = lineChart;
    }

    public void computeGraph(){
        XYChart.Series<String, Double> series = new XYChart.Series<>();

        Map<LocalDate, Double> soldesParMois = this.entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.dateValue().with(TemporalAdjusters.lastDayOfMonth()),
                        TreeMap::new,  // Pour garder l'ordre chronologique
                        Collectors.summingDouble(Entry::value)
                ));

        LinkedHashMap<String, Double> collect = soldesParMois.entrySet().stream()
                .collect(
                        LinkedHashMap<String, Double>::new,
                        (map, entry) -> {
                            double dernierSolde = map.isEmpty() ? 0.0 : map.values().stream().findFirst().get();
                            map.put(String.valueOf(entry.getKey()), dernierSolde + entry.getValue());
                        },
                        Map::putAll
                );

        collect.entrySet().stream().forEach(e -> series.getData().add(new XYChart.Data<>(String.valueOf(e.getKey()), e.getValue())));

        lineChart.getData().setAll(series);
    }

    private record EntryMonthValue(LocalDate month, Double value){}

}
