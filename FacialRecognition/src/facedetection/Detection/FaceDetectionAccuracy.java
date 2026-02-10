/**
 * The <code>facedetection.Detection</code> package contains classes responsible for detecting
 * and evaluating the performance of face detection in images. This package integrates functionalities
 * for image processing, feature extraction, and classification using SVM to
 * analyze and measure the accuracy of facial detection.
 */
package facedetection.Detection;

import facedetection.SVM.SVMClassifier;
import facedetection.Utils.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Class responsible for testing and evaluating the accuracy of face detection using an
 * SVM classifier. It analyzes positive and negative images from the test set and generates
 * detailed statistics, including accuracy, precision, recall, and F1-score.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class FaceDetectionAccuracy {
    /** Directory containing positive test images (with faces). */
    private static final String TEST_POS_DIR = "data/test/pos/";
    /** Directory containing negative test images (without faces). */
    private static final String TEST_NEG_DIR = "data/test/neg/";
    /** Output directory where accuracy results are saved. */
    private static final String OUTPUT_DIR = "output/";
    /** SVM classifier used for face detection. */
    private SVMClassifier svmClassifier;
    /** HOG extractor for obtaining image features. */
    private HOGExtractor hogExtractor;
    /** Image processor for resizing and preprocessing. */
    private ImageProcessor imageProcessor;
    /** Face detector that uses HOG and SVM. */
    private FaceDetector faceDetector;

    /**
     * Class constructor that initializes the components required for accuracy evaluation.
     *
     * @param svmClassifier the SVM classifier used for detection
     * @param hogExtractor the HOG extractor for image features
     * @param imageProcessor the image processor for preprocessing
     */
    public FaceDetectionAccuracy(SVMClassifier svmClassifier, HOGExtractor hogExtractor, ImageProcessor imageProcessor) {
        this.svmClassifier = svmClassifier;
        this.hogExtractor = hogExtractor;
        this.imageProcessor = imageProcessor;
        this.faceDetector = new FaceDetector(hogExtractor, imageProcessor);
    }

    /**
     * Tests the accuracy of face detection on the test set, calculating metrics such as
     * True Positives, False Positives, True Negatives, False Negatives, accuracy, precision,
     * recall, and F1-score. Results are displayed in the console and saved to a file.
     */
    public void testAccuracy() {
        System.out.println("Starting accuracy testing on the test set...");
        int truePositives = 0, falsePositives = 0, trueNegatives = 0, falseNegatives = 0;
        int totalImages = 0;
        int minPosWidth = Integer.MAX_VALUE, minPosHeight = Integer.MAX_VALUE;
        int maxPosWidth = 0, maxPosHeight = 0;
        int minNegWidth = Integer.MAX_VALUE, minNegHeight = Integer.MAX_VALUE;
        int maxNegWidth = 0, maxNegHeight = 0;

        StringBuilder accuracyResults = new StringBuilder();
        accuracyResults.append("Accuracy Testing Results\n");
        accuracyResults.append("==========================\n\n");

        File posDir = new File(TEST_POS_DIR);
        System.out.println("Loading positive images from " + TEST_POS_DIR + "...");
        File[] posFiles = posDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (posFiles != null) {
            for (File file : posFiles) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w == 0 || h == 0) {
                        System.err.println("Invalid positive test image (zero size): " + file.getName());
                        continue;
                    }
                    minPosWidth = Math.min(minPosWidth, w);
                    minPosHeight = Math.min(minPosHeight, h);
                    maxPosWidth = Math.max(maxPosWidth, w);
                    maxPosHeight = Math.max(maxPosHeight, h);
                    System.out.println("Testing positive image: " + file.getName() + ", size: " + w + "x" + h);
                    long startTime = System.currentTimeMillis();
                    BufferedImage resized = imageProcessor.resizeIfLarge(image);
                    List<Rectangle> faces = detectFaces(resized);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Testing image " + file.getName() + " took " + (endTime - startTime) / 1000.0 + " seconds");
                    if (!faces.isEmpty()) {
                        truePositives++;
                        System.out.println("Face correctly detected in: " + file.getName() + " (detected faces: " + faces.size() + ")");
                    } else {
                        falseNegatives++;
                        System.out.println("Face not detected in: " + file.getName() + " (no region with score > " + FaceDetector.SCORE_THRESHOLD + ")");
                    }
                    totalImages++;
                } catch (IOException e) {
                    System.err.println("Error testing positive image: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        File negDir = new File(TEST_NEG_DIR);
        System.out.println("Loading negative images from " + TEST_NEG_DIR + "...");
        File[] negFiles = negDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (negFiles != null) {
            for (File file : negFiles) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    int w = image.getWidth();
                    int h = image.getHeight();
                    if (w == 0 || h == 0) {
                        System.err.println("Invalid negative test image (zero size): " + file.getName());
                        continue;
                    }
                    minNegWidth = Math.min(minNegWidth, w);
                    minNegHeight = Math.min(minNegHeight, h);
                    maxNegWidth = Math.max(maxNegWidth, w);
                    maxNegHeight = Math.max(maxNegHeight, h);
                    System.out.println("Testing negative image: " + file.getName() + ", size: " + w + "x" + h);
                    long startTime = System.currentTimeMillis();
                    BufferedImage resized = imageProcessor.resizeIfLarge(image);
                    List<Rectangle> faces = detectFaces(resized);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Testing image " + file.getName() + " took " + (endTime - startTime) / 1000.0 + " seconds");
                    if (faces.isEmpty()) {
                        trueNegatives++;
                        System.out.println("No face correctly detected in: " + file.getName());
                    } else {
                        falsePositives++;
                        System.out.println("Face incorrectly detected in: " + file.getName() + " (detected faces: " + faces.size() + ")");
                    }
                    totalImages++;
                } catch (IOException e) {
                    System.err.println("Error testing negative image: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        accuracyResults.append("Test set statistics:\n");
        accuracyResults.append("Positive images: ").append(truePositives + falseNegatives).append("\n");
        accuracyResults.append("Minimum positive size: ").append(minPosWidth).append("x").append(minPosHeight).append("\n");
        accuracyResults.append("Maximum positive size: ").append(maxPosWidth).append("x").append(maxPosHeight).append("\n");
        accuracyResults.append("Negative images: ").append(trueNegatives + falsePositives).append("\n");
        accuracyResults.append("Minimum negative size: ").append(minNegWidth).append("x").append(minNegHeight).append("\n");
        accuracyResults.append("Maximum negative size: ").append(maxNegWidth).append("x").append(maxNegHeight).append("\n");
        accuracyResults.append("Total images tested: ").append(totalImages).append("\n\n");

        double accuracy = totalImages > 0 ? (double) (truePositives + trueNegatives) / totalImages : 0;
        double precision = truePositives / (double) (truePositives + falsePositives + 1e-6);
        double recall = truePositives / (double) (truePositives + falseNegatives + 1e-6);
        double f1Score = 2 * (precision * recall) / (precision + recall + 1e-6);

        accuracyResults.append("Testing results:\n");
        accuracyResults.append("True Positives: ").append(truePositives).append("\n");
        accuracyResults.append("False Positives: ").append(falsePositives).append("\n");
        accuracyResults.append("True Negatives: ").append(trueNegatives).append("\n");
        accuracyResults.append("False Negatives: ").append(falseNegatives).append("\n");
        accuracyResults.append("Accuracy: ").append(String.format("%.2f", accuracy * 100)).append("%\n");
        accuracyResults.append("Precision: ").append(String.format("%.2f", precision * 100)).append("%\n");
        accuracyResults.append("Recall: ").append(String.format("%.2f", recall * 100)).append("%\n");
        accuracyResults.append("F1-Score: ").append(String.format("%.2f", f1Score * 100)).append("%\n");

        System.out.println("Test set statistics:");
        System.out.println("Positive images: " + (truePositives + falseNegatives));
        System.out.println("Minimum positive size: " + minPosWidth + "x" + minPosHeight);
        System.out.println("Maximum positive size: " + maxPosWidth + "x" + maxPosHeight);
        System.out.println("Negative images: " + (trueNegatives + falsePositives));
        System.out.println("Minimum negative size: " + minNegWidth + "x" + minNegHeight);
        System.out.println("Maximum negative size: " + maxNegWidth + "x" + maxNegHeight);
        System.out.println("Total images tested: " + totalImages);
        System.out.println("Testing results:");
        System.out.println("True Positives: " + truePositives);
        System.out.println("False Positives: " + falsePositives);
        System.out.println("True Negatives: " + trueNegatives);
        System.out.println("False Negatives: " + falseNegatives);
        System.out.println("Accuracy: " + String.format("%.2f", accuracy * 100) + "%");
        System.out.println("Precision: " + String.format("%.2f", precision * 100) + "%");
        System.out.println("Recall: " + String.format("%.2f", recall * 100) + "%");
        System.out.println("F1-Score: " + String.format("%.2f", f1Score * 100) + "%");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_DIR + "test_accuracy.txt"))) {
            writer.write(accuracyResults.toString());
            System.out.println("Accuracy results saved to " + OUTPUT_DIR + "test_accuracy.txt");
        } catch (IOException e) {
            System.err.println("Error saving accuracy results to file.");
            e.printStackTrace();
        }
    }

    /**
     * Detects faces in an image using the face detector.
     *
     * @param image the image to process
     * @return the list of rectangles bounding the detected faces
     */
    private List<Rectangle> detectFaces(BufferedImage image) {
        return faceDetector.detectFaces(image, svmClassifier);
    }
}