package org.github.ypiel.jbudget.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.github.ypiel.jbudget.model.Entry;

public class EntryJsonController {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static{
        // Register JavaTimeModule for LocalDate support
        objectMapper.registerModule(new JavaTimeModule());

        // Disable timestamps for dates (use ISO format)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable pretty-printing (indented JSON)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // (Optional) Customize indentation (default is 2 spaces)
        objectMapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter() {
            @Override
            public DefaultPrettyPrinter createInstance() {
                return new DefaultPrettyPrinter() {
                    @Override
                    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
                        g.writeRaw(": "); // Adds a space after colon
                    }
                };
            }
        });

    }

    private EntryJsonController(){
        // Singleton using static
    }

    public static void saveEntriesToFile(List<Entry> entries, String filePath) throws IOException {
        objectMapper.writeValue(new File(filePath), entries);
    }

    public static List<Entry> loadEntriesFromFile(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Entry.class));
    }

}
