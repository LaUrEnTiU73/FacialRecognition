package facedetection.App;

import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class that implements a dialog window for configuring application settings.
 * It allows the user to modify parameters for various system components
 * (e.g., kernel, face detection, SVM classifier) and save these settings to a file.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class SettingsDialog extends JDialog {
    /** Map that stores setting fields and their associated components (JTextField or JComboBox). */
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    /**
     * Class constructor that initializes the settings dialog window.
     *
     * @param parent the parent window (JFrame) relative to which the dialog is positioned
     */
    public SettingsDialog(JFrame parent) {
        super(parent, "Settings", true);
        setSize(600, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        add(scrollPane, BorderLayout.CENTER);

        // KERNEL
        JPanel kernelPanel = createSectionPanel("Kernel");
        addComboBox(kernelPanel, "KernelDetection", new String[]{"Linear", "Sigmoid"});
        addComboBox(kernelPanel, "KernelRecognition", new String[]{"Sigmoid", "Linear"});
        contentPanel.add(kernelPanel);

        // FACE DETECTION
        JPanel facePanel = createSectionPanel("Face Detection");
        addTextField(facePanel, "SCORE_THRESHOLD", "0.035");
        addTextField(facePanel, "WINDOW_SIZES", "112,128");
        addTextField(facePanel, "STEPS", "40,48");
        addTextField(facePanel, "GRADIENT_THRESHOLD", "10.0");
        contentPanel.add(facePanel);

        // SVM CLASSIFIER
        JPanel svmPanel = createSectionPanel("SVM Classifier");
        addTextField(svmPanel, "EPSILON", "1e-12");
        addTextField(svmPanel, "MAX_ITERATIONS", "10000");
        addTextField(svmPanel, "MIN_ITERATIONS", "5");
        addTextField(svmPanel, "TIMEOUT_MS", String.valueOf(15 * 60 * 1000));
        addTextField(svmPanel, "MAX_NO_CHANGE_ITERATIONS", "3");
        addTextField(svmPanel, "ESTIMATED_MAX_ITERATIONS", "1000");
        addTextField(svmPanel, "EXPECTED_MAX_SV", "200");
        contentPanel.add(svmPanel);

        // HOG EXTRACTOR
        JPanel hogPanel = createSectionPanel("HOG Extractor");
        addTextField(hogPanel, "IMAGE_SIZE", "128");
        addTextField(hogPanel, "HOG_CELL_SIZE", "8");
        addTextField(hogPanel, "HOG_BLOCK_SIZE", "8");
        addTextField(hogPanel, "HOG_NBINS", "9");
        contentPanel.add(hogPanel);

        // CAPTURE MANAGER
        JPanel capturePanel = createSectionPanel("Capture Manager");
        addTextField(capturePanel, "MAX_PHOTOS", "500");
        contentPanel.add(capturePanel);

        // KERNEL SIGMOID
        JPanel sigmoidPanel = createSectionPanel("KernelSigmoid");
        addTextField(sigmoidPanel, "SIGMOID_A", "0.001");
        addTextField(sigmoidPanel, "SIGMOID_B", "0.0");
        addTextField(sigmoidPanel, "EPSILON", "1e-12");
        contentPanel.add(sigmoidPanel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> {
            File dir = new File("settings");
            if (!dir.exists()) {
                dir.mkdir();
            }
        
            File file = new File(dir, "settings.txt");
            try (PrintWriter writer = new PrintWriter(file)) {
                for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    JComponent comp = entry.getValue();
                    String value;
                    if (comp instanceof JTextField) {
                        value = ((JTextField) comp).getText();
                    } else if (comp instanceof JComboBox) {
                        value = ((JComboBox<?>) comp).getSelectedItem().toString();
                    } else {
                        continue;
                    }
                    writer.println(key + "=" + value);
                }
                JOptionPane.showMessageDialog(this, "Settings have been saved to settings/settings.txt.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage());
            }
        
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a panel for a settings section with a specified title.
     *
     * @param title the section title
     * @return the created panel for the respective section
     */
    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    /**
     * Adds a text field for a setting to the specified panel.
     *
     * @param panel the panel to which the field is added
     * @param label the setting label
     * @param defaultValue the default value of the field
     */
    private void addTextField(JPanel panel, String label, String defaultValue) {
        JLabel jLabel = new JLabel(label);
        JTextField field = new JTextField(defaultValue);
        fields.put(label, field);
        panel.add(jLabel);
        panel.add(field);
    }

    /**
     * Adds a dropdown menu (JComboBox) for a setting to the specified panel.
     *
     * @param panel the panel to which the dropdown is added
     * @param label the setting label
     * @param options the available options in the dropdown
     */
    private void addComboBox(JPanel panel, String label, String[] options) {
        JLabel jLabel = new JLabel(label);
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setSelectedIndex(0);
        fields.put(label, comboBox);
        panel.add(jLabel);
        panel.add(comboBox);
    }
}