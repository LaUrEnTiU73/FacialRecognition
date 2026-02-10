/**
 * The <code>facedetection.Trainers</code> package contains classes responsible for training
 * models used in face detection and recognition. This package manages the process
 * of feature extraction, classifier training, and model saving.
 */
package facedetection.Trainers;

import facedetection.Kernels.KernelSigmoid;
import facedetection.SVM.SVMClassifier;
import facedetection.Utils.HOGExtractor;
import facedetection.Utils.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Class responsible for training SVM classifiers for facial recognition.
 * It processes images from a training set organized into directories for each person,
 * extracts HOG features, and trains an SVM classifier with a sigmoid kernel for each individual.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class FaceRecognitionTrainer {
    /**
     * Default constructor for the FaceRecognitionTrainer class.
     * Initializes a trainer responsible for the facial recognition process.
     */
    public FaceRecognitionTrainer() {
        // No explicit initialization required
    }

    /** Directory containing training images, organized into subdirectories for each person. */
    private static final String TRAIN_DIR = "data/train/recognition/trainPersons";
    /** Image processor for preprocessing and resizing. */
    private ImageProcessor processor = new ImageProcessor();
    /** HOG extractor for obtaining image features. */
    private HOGExtractor hogExtractor = new HOGExtractor();
    /** Callback for updating training progress. */
    private ProgressCallback progressCallback;

    /**
     * Interface for progress callback, used to notify about the training status.
     */
    public interface ProgressCallback {
        /**
         * Method called to update the current training progress status.
         *
         * @param nickname the name of the person for whom the classifier is being trained
         * @param status the current training status (e.g., "Training in progress...", "Completed")
         */
        void onProgressUpdate(String nickname, String status);
    }

    /**
     * Sets the callback for updating training progress.
     *
     * @param callback the progress callback
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * Trains SVM classifiers for facial recognition by processing images from each person's directory.
     * Extracts HOG features, trains an SVM classifier with a sigmoid kernel for each person,
     * and saves the trained model.
     *
     * @throws IOException if errors occur while reading images or saving classifiers
     */
    public void train() throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting classifier training...");

        File trainDir = new File(TRAIN_DIR);
        File[] personDirs = trainDir.listFiles(File::isDirectory);
        if (personDirs == null || personDirs.length == 0) {
            throw new IOException("Directory " + TRAIN_DIR + " is empty or does not exist.");
        }

        for (File personDir : personDirs) {
            String nickname = personDir.getName();
            System.out.println("Processing directory for " + nickname + "...");
            if (progressCallback != null) {
                progressCallback.onProgressUpdate(nickname, "Training in progress...");
            }

            List<double[]> features = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();
            int positiveCount = 0;
            int negativeCount = 0;

            // Process positive images (from the person's own directory)
            File[] posFiles = personDir.listFiles((dir, name) -> name.endsWith(".png"));
            if (posFiles == null || posFiles.length == 0) {
                System.out.println("No positive images found in " + personDir.getAbsolutePath());
                continue;
            }

            System.out.println("Number of positive images found: " + posFiles.length);
            for (File file : posFiles) {
                try {
                    long imgStartTime = System.currentTimeMillis();
                    BufferedImage image = ImageIO.read(file);
                    if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
                        System.out.println("Invalid image: " + file.getName());
                        continue;
                    }
                    if (image.getWidth() != 128 || image.getHeight() != 128) {
                        System.out.println("Image " + file.getName() + ": incorrect size=" + image.getWidth() + "x" + image.getHeight());
                    }
                    BufferedImage scaled = processor.scaleTo128x128(image);
                    long hogStartTime = System.currentTimeMillis();
                    double[] hog = hogExtractor.extractHOGFeatures(scaled);
                    long hogEndTime = System.currentTimeMillis();
                    System.out.println("HOG extraction took " + (hogEndTime - hogStartTime) / 1000.0 + " ms");
                    System.out.println("HOG for " + file.getName() + ": size=" + hog.length + 
                        ", first 5 elements=[" + hog[0] + ", " + hog[1] + ", " + hog[2] + ", " + hog[3] + ", " + hog[4] + "]" +
                        ", time=" + (System.currentTimeMillis() - imgStartTime) + "ms");
                    features.add(hog);
                    labels.add(1);
                    positiveCount++;
                } catch (Exception e) {
                    System.out.println("Error processing positive image " + file.getName() + ": " + e.getMessage());
                }
            }

            // Process negative images (from other persons' directories)
            for (File otherDir : personDirs) {
                if (otherDir.equals(personDir)) continue;
                File[] negFiles = otherDir.listFiles((dir, name) -> name.endsWith(".png"));
                if (negFiles == null) continue;

                for (File file : negFiles) {
                    try {
                        long imgStartTime = System.currentTimeMillis();
                        BufferedImage image = ImageIO.read(file);
                        if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
                            System.out.println("Invalid negative image: " + file.getName());
                            continue;
                        }
                        if (image.getWidth() != 128 || image.getHeight() != 128) {
                            System.out.println("Negative image " + file.getName() + ": incorrect size=" + image.getWidth() + "x" + image.getHeight());
                        }
                        BufferedImage scaled = processor.scaleTo128x128(image);
                        long hogStartTime = System.currentTimeMillis();
                        double[] hog = hogExtractor.extractHOGFeatures(scaled);
                        long hogEndTime = System.currentTimeMillis();
                        System.out.println("Negative HOG extraction took " + (hogEndTime - hogStartTime) / 1000.0 + " ms");
                        System.out.println("Negative HOG for " + file.getName() + ": size=" + hog.length + 
                            ", first 5 elements=[" + hog[0] + ", " + hog[1] + ", " + hog[2] + ", " + hog[3] + ", " + hog[4] + "]" +
                            ", time=" + (System.currentTimeMillis() - imgStartTime) + "ms");
                        features.add(hog);
                        labels.add(-1);
                        negativeCount++;
                    } catch (Exception e) {
                        System.out.println("Error processing negative image " + file.getName() + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("For " + nickname + ": " + positiveCount + " positive, " + negativeCount + " negative processed.");

            if (features.isEmpty()) {
                System.out.println("No valid images for training for " + nickname);
                continue;
            }

            // Train the classifier
            long svmStartTime = System.currentTimeMillis();
            SVMClassifier svm = new SVMClassifier(features, labels, 1.0, new KernelSigmoid()); // Increased C for flexibility
            svm.train();
            long svmEndTime = System.currentTimeMillis();
            System.out.println("SVM training for " + nickname + " took " + (svmEndTime - svmStartTime) + "ms");

            // Save the classifier
            File svmFile = new File(personDir, "svm_" + nickname + ".ser");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(svmFile))) {
                oos.writeObject(svm);
                System.out.println("Classifier saved: " + svmFile.getAbsolutePath());
                if (progressCallback != null) {
                    progressCallback.onProgressUpdate(nickname, "Completed");
                }
            } catch (IOException e) {
                System.out.println("Error saving classifier for " + nickname + ": " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total training completed in " + (endTime - startTime) / 1000.0 + " seconds.");
    }
}