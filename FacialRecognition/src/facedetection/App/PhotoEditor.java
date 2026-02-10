package facedetection.App;

import facedetection.Utils.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Class responsible for editing and managing the photographs used in training
 * the facial recognition system. It allows displaying, deleting, and navigating
 * between the photographs associated with a specific nickname.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class PhotoEditor {
    /** Directory where training photographs are stored. */
    private static final String TRAIN_DIR = "data/train/recognition/trainPersons";

    /** Reference to the main application graphical user interface. */
    private WebcamCaptureApp ui;
    /** Processor for handling images. */
    private ImageProcessor processor;
    /** List of available photograph files for editing. */
    private List<File> photoFiles;
    /** Index of the current photograph in the list. */
    private int currentPhotoIndex;

    /**
     * Class constructor that initializes the components for photo editing.
     *
     * @param ui the main application graphical user interface
     */
    public PhotoEditor(WebcamCaptureApp ui) {
        this.ui = ui;
        this.processor = new ImageProcessor();
        this.photoFiles = new ArrayList<>();
        this.currentPhotoIndex = 0;
    }

    /**
     * Initiates the photo editing process, allowing the user to select
     * a nickname and access the associated photographs.
     */
    public void editPhotos() {
        File trainDir = new File(TRAIN_DIR);
        File[] personDirs = trainDir.listFiles(File::isDirectory);
        if (personDirs == null || personDirs.length == 0) {
            JOptionPane.showMessageDialog(ui, "No photos available for editing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> nicknames = new ArrayList<>();
        for (File dir : personDirs) {
            nicknames.add(dir.getName());
        }

        JComboBox<String> nicknameCombo = new JComboBox<>(nicknames.toArray(new String[0]));
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("Select nickname:");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        nicknameCombo.setMaximumSize(new Dimension(200, 30));
        nicknameCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(nicknameCombo);

        int result = JOptionPane.showConfirmDialog(
            ui,
            panel,
            "Nickname Selection",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String nickname = (String) nicknameCombo.getSelectedItem();
        if (nickname == null || nickname.trim().isEmpty()) {
            JOptionPane.showMessageDialog(ui, "Invalid nickname.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.getCaptureManager().setNickname(nickname);

        File personDir = new File(TRAIN_DIR + "/" + nickname);
        if (!personDir.exists() || !personDir.isDirectory()) {
            JOptionPane.showMessageDialog(ui, "No photos exist for " + nickname, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        photoFiles.clear();
        File[] files = personDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files != null) {
            for (File file : files) {
                photoFiles.add(file);
            }
        }

        if (photoFiles.isEmpty()) {
            JOptionPane.showMessageDialog(ui, "No photos available for editing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("Number of photos loaded: " + photoFiles.size());

        currentPhotoIndex = 0;
        ui.showEditPhotosUI(nickname);
        showCurrentPhoto();
    }

    /**
     * Displays the current photograph in the graphical interface.
     */
    public void showCurrentPhoto() {
        if (currentPhotoIndex >= 0 && currentPhotoIndex < photoFiles.size()) {
            try {
                BufferedImage img = ImageIO.read(photoFiles.get(currentPhotoIndex));
                ui.getEditImageContentLabel().setIcon(new ImageIcon(img));
                ui.getPhotoCountLabel().setText((currentPhotoIndex + 1) + "/" + photoFiles.size());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ui, "Error loading image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            ui.getEditImageContentLabel().setIcon(null);
            ui.getPhotoCountLabel().setText("");
            JOptionPane.showMessageDialog(ui, "No more photos available.", "Error", JOptionPane.ERROR_MESSAGE);
            ui.showPhotographButtons();
        }
    }

    /**
     * Deletes the current photograph from the list and disk.
     */
    public void deleteCurrentPhoto() {
        if (currentPhotoIndex >= 0 && currentPhotoIndex < photoFiles.size()) {
            File fileToDelete = photoFiles.get(currentPhotoIndex);
            if (fileToDelete.delete()) {
                photoFiles.remove(currentPhotoIndex);
                if (currentPhotoIndex >= photoFiles.size() && !photoFiles.isEmpty()) {
                    currentPhotoIndex = photoFiles.size() - 1;
                }
                if (photoFiles.isEmpty()) {
                    ui.getPhotoCountLabel().setText("");
                    JOptionPane.showMessageDialog(ui, "All photos have been deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    ui.showPhotographButtons();
                } else {
                    showCurrentPhoto();
                }
            } else {
                JOptionPane.showMessageDialog(ui, "Error deleting image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Displays the previous photograph in the list.
     */
    public void showPreviousPhoto() {
        if (currentPhotoIndex > 0) {
            currentPhotoIndex--;
            showCurrentPhoto();
        }
    }

    /**
     * Displays the next photograph in the list.
     */
    public void showNextPhoto() {
        if (currentPhotoIndex < photoFiles.size() - 1) {
            currentPhotoIndex++;
            showCurrentPhoto();
        }
    }

    /**
     * Deletes all directories and photographs from the training directory.
     */
    public void deleteAllPhotos() {
        int response = JOptionPane.showConfirmDialog(
            ui,
            "Are you sure you want to delete all directories in data/train/recognition/trainPersons?",
            "Deletion Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            File trainDir = new File(TRAIN_DIR);
            if (trainDir.exists() && trainDir.isDirectory()) {
                File[] subDirs = trainDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        deleteDirectory(subDir);
                    }
                }
                JOptionPane.showMessageDialog(ui, "All directories have been deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(ui, "The directory data/train/recognition/trainPersons does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        ui.showPhotographButtons();
    }

    /**
     * Recursively deletes a directory and all its files.
     *
     * @param directory the directory to delete
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Returns the index of the current photograph.
     *
     * @return the current photograph index
     */
    public int getCurrentPhotoIndex() {
        return currentPhotoIndex;
    }

    /**
     * Returns the total number of available photographs.
     *
     * @return the size of the photograph list
     */
    public int getPhotoFilesSize() {
        return photoFiles.size();
    }
}