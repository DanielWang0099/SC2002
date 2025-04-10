package utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for reading and writing data in CSV format.
 * Handles basic comma separation and double quotes for fields containing commas.
 */
public class CsvUtil {

    private static final String CSV_SEPARATOR = ",";
    private static final String QUOTE = "\"";
    private static final String ESCAPED_QUOTE = "\"\"";
    private static final String[] EMPTY_ARRAY = new String[0];

    /**
     * Writes a list of objects to a CSV file.
     *
     * @param filename   The path to the CSV file.
     * @param data       The list of objects to write.
     * @param rowMapper  A function to convert an object T into a String array for a CSV row.
     * @param header     Optional header row (String array). Pass null or empty array for no header.
     * @param <T>        The type of the objects in the list.
     */
    public static <T> void writeCsv(String filename, List<T> data, Function<T, String[]> rowMapper, String[] header) {
        File file = new File(filename);
        // Ensure parent directory exists
        if (file.getParentFile() != null) {
             file.getParentFile().mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header if provided
            if (header != null && header.length > 0) {
                writer.println(convertToCsvRow(header));
            }

            // Write data rows
            for (T item : data) {
                String[] rowData = rowMapper.apply(item);
                writer.println(convertToCsvRow(rowData));
            }
            System.out.println("Data successfully written to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing CSV file '" + filename + "': " + e.getMessage());
            // Consider more robust error handling or re-throwing a custom exception
        }
    }

    /**
     * Reads objects from a CSV file.
     *
     * @param filename  The path to the CSV file.
     * @param rowMapper A function to convert a String array (from a CSV row) into an object T.
     * @param skipHeader If true, skips the first line of the file.
     * @param <T>       The type of the objects to create.
     * @return A List of objects created from the CSV data.
     */
    public static <T> List<T> readCsv(String filename, Function<String[], T> rowMapper, boolean skipHeader) {
        List<T> data = new ArrayList<>();
        File file = new File(filename);

        if (!file.exists()) {
             System.err.println("CSV file not found: " + filename + ". Returning empty list.");
             return data; // Return empty list if file doesn't exist
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            if (skipHeader && (reader.readLine() == null)) {
                // File is empty or only has a header
                return data;
            }

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] rowData = parseCsvRow(line);
                try {
                    T item = rowMapper.apply(rowData);
                    if (item != null) { // Mapper can return null to skip invalid rows
                       data.add(item);
                    }
                } catch (Exception e) {
                    // Catch errors during mapping (e.g., NumberFormatException, IllegalArgumentException)
                    System.err.println("Error mapping CSV row in file '" + filename + "': [" + line + "]. Error: " + e.getMessage());
                    // Continue to the next line
                }
            }
            System.out.println("Data successfully read from " + filename + ". Items loaded: " + data.size());
        } catch (IOException e) {
            System.err.println("Error reading CSV file '" + filename + "': " + e.getMessage());
            // Consider more robust error handling
        }
        return data;
    }


    // --- Helper methods for CSV formatting ---

    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // Escape quotes within the field
        String escaped = field.replace(QUOTE, ESCAPED_QUOTE);
        // Add quotes if the field contains comma, quote, or newline
        if (field.contains(CSV_SEPARATOR) || field.contains(QUOTE) || field.contains("\n") || field.contains("\r")) {
            escaped = QUOTE + escaped + QUOTE;
        }
        return escaped;
    }

    private static String convertToCsvRow(String[] fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        return List.of(fields).stream() // Use List.of for newer Java versions
                .map(CsvUtil::escapeCsvField)
                .collect(Collectors.joining(CSV_SEPARATOR));
    }

     // Basic CSV row parser (handles simple quoted fields)
    private static String[] parseCsvRow(String row) {
        if (row == null || row.trim().isEmpty()) {
            return EMPTY_ARRAY;
        }
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (inQuotes) {
                    // Check for escaped quote ("")
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++; // Skip the second quote
                    } else {
                        inQuotes = false; // End of quoted field
                    }
                } else {
                    inQuotes = true; // Start of quoted field
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0); // Reset for next field
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString()); // Add the last field
        return fields.toArray(String[]::new);
    }
}