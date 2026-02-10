package facedetection.App;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

/**
 * Class responsible for managing and displaying the training progress of facial recognition
 * classifiers in a graphical table. It loads person data from the training directory
 * and updates the status in real time.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class TrainingProgressManager {
    /** Directory where training images are stored. */
    private static final String TRAIN_DIR = "data/train/recognition/trainPersons";
    /** Table that displays training progress. */
    private JTable trainingTable;
    /** Table model that manages the displayed data. */
    private DefaultTableModel tableModel;

    /**
     * Class constructor that initializes the table and loads person data.
     */
    public TrainingProgressManager() {
        initializeTable();
        loadPersons();
    }

    /**
     * Initializes the table with specified columns and sets row and column dimensions.
     */
    private void initializeTable() {
        tableModel = new DefaultTableModel(new Object[]{"Photo", "Name", "Positive Photos", "Negative Photos", "Status"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? ImageIcon.class : String.class;
            }
        };
        trainingTable = new JTable(tableModel);
        trainingTable.setRowHeight(50); // Smaller row height
        trainingTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Smaller width for photo column
    }

    /**
     * Loads person data from the training directory and populates the table.
     * For each person, a representative image is displayed along with the count of positive
     * and negative photos, and the initial status.
     */
    private void loadPersons() {
        File trainDir = new File(TRAIN_DIR);
        File[] personDirs = trainDir.listFiles(File::isDirectory);
        if (personDirs == null) {
            System.out.println("Error: Directory " + TRAIN_DIR + " is empty or does not exist.");
            return;
        }
    
        for (File personDir : personDirs) {
            String nickname = personDir.getName();
            File[] posImages = personDir.listFiles((dir, name) -> name.endsWith(".png"));
            int posCount = (posImages != null) ? posImages.length : 0;
            int negCount = 0;
    
            // Calculate the number of negative images from other directories
            for (File otherDir : personDirs) {
                if (!otherDir.equals(personDir)) {
                    File[] negImages = otherDir.listFiles((dir, name) -> name.endsWith(".png"));
                    negCount += (negImages != null) ? negImages.length : 0;
                }
            }
    
            if (posImages != null && posImages.length > 0) {
                try {
                    BufferedImage img = ImageIO.read(posImages[0]); // First image as representative
                    if (img != null) {
                        Image scaledImg = img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                        tableModel.addRow(new Object[]{new ImageIcon(scaledImg), nickname, posCount, negCount, "Pending"});
                    }
                } catch (IOException e) {
                    System.out.println("Error loading image for " + nickname + ": " + e.getMessage());
                }
            }
        }
    }
      /**
     * Reloads person data from the training directory and resets the table with updated data.
     */
    public void refreshPersons() {
        tableModel.setRowCount(0); // Reset the table
        loadPersons(); // Reload the data
    }


    /**
     * Updates the status of a person in the table on the UI thread.
     *
     * @param nickname the nickname of the person whose status needs to be updated
     * @param status the new training status (e.g., "Pending", "Complete")
     */
    public void updateProgress(String nickname, String status) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 1).equals(nickname)) {
                    tableModel.setValueAt(status, i, 4);
                    break;
                }
            }
        });
    }

    /**
     * Returns the training progress table.
     *
     * @return JTable containing the progress data
     */
    public JTable getTrainingTable() {
        return trainingTable;
    }
}