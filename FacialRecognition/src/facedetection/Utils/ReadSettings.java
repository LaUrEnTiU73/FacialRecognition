/**
 * The <code>facedetection.Utils</code> package contains utility classes that support image processing,
 * feature extraction, and configuration management within the facial detection application.
 */
package facedetection.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

/**
 * Class responsible for reading and applying configuration settings from a file.
 * It loads numeric parameters from a text file and applies them to objects using reflection.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class ReadSettings {
    /** Map storing key-value pairs of parameters from the configuration file. */
    private Map<String, Double> parametri;

    /**
     * Class constructor that initializes the parameter map and loads the settings.
     */
    public ReadSettings() {
        parametri = new HashMap<>();
        incarcaSetari();
    }

    /**
     * Loads settings from the `settings.txt` file, where each line contains a key=value pair.
     * The value must be convertible to a double-precision floating-point number.
     */
    private void incarcaSetari() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("settings/settings.txt"));
            for (String line : lines) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue; // Invalid line, skip

                String key = parts[0].trim();
                String value = parts[1].trim();
                try {
                    double doubleValue = Double.parseDouble(value);
                    parametri.put(key, doubleValue);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid value for parameter " + key + ": " + value);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading settings from settings.txt: " + e.getMessage());
        }
    }

    /**
     * Applies the parameter values read from the configuration file to the fields of a specified object.
     * Uses reflection to set field values with names matching the parameter keys.
     *
     * @param obiect the object to which settings are applied
     */
    public void aplicaSetari(Object obiect) {
        try {
            Class<?> clasa = obiect.getClass();
            for (Map.Entry<String, Double> entry : parametri.entrySet()) {
                String numeParametru = entry.getKey();
                Double valoare = entry.getValue();

                try {
                    // Find the field with the same name as the parameter
                    Field field = clasa.getDeclaredField(numeParametru);
                    field.setAccessible(true); // Allow access to private fields
                    field.setDouble(obiect, valoare);
                    field.setAccessible(false);
                } catch (NoSuchFieldException e) {
                    System.err.println("Field " + numeParametru + " does not exist in " + clasa.getSimpleName());
                } catch (IllegalAccessException e) {
                    System.err.println("Error setting value for " + numeParametru + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error applying settings: " + e.getMessage());
        }
    }
}