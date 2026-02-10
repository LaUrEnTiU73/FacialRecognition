/**
 * The <code>facedetection.Trainers</code> package contains classes responsible for training
 * models used in face detection and recognition. This package manages the process
 * of feature extraction, classifier training, and model saving.
 */
package facedetection.Trainers;

import facedetection.Kernels.KernelLinear;
import facedetection.SVM.SVMClassifier;
import facedetection.Utils.HOGExtractor;
import facedetection.Utils.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Class responsible for training an SVM classifier for face detection.
 * It processes positive and negative images from the training set, extracts
 * HOG features, and trains a linear SVM model.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class FaceDetectionTrainer {
    /** Directory containing positive training images (with faces). */
    private static final String POS_DIR = "data/train/detection/pos/";
    /** Directory containing negative training images (without faces). */
    private static final String NEG_DIR = "data/train/detection/neg/";
    /** HOG extractor for obtaining image features. */
    private HOGExtractor hogExtractor;
    /** Image processor for preprocessing and resizing. */
    private ImageProcessor imageProcessor;

    /**
     * Class constructor that initializes the HOG extractor and image processor.
     *
     * @param hogExtractor the HOG extractor used for image features
     * @param imageProcessor the image processor for preprocessing
     */
    public FaceDetectionTrainer(HOGExtractor hogExtractor, ImageProcessor imageProcessor) {
        this.hogExtractor = hogExtractor;
        this.imageProcessor = imageProcessor;
    }

    /**
     * Trains an SVM classifier for face detection using images from the training set.
     * Extracts HOG features from positive and negative images, creates a linear SVM model,
     * and saves the trained classifier.
     *
     * @return the trained SVM classifier, or null if training fails
     */
    public SVMClassifier trainSVM() {
        System.out.println("Starting SVM classifier training...");
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        int minPosWidth = Integer.MAX_VALUE, minPosHeight = Integer.MAX_VALUE;
        int maxPosWidth = 0, maxPosHeight = 0;
        int minNegWidth = Integer.MAX_VALUE, minNegHeight = Integer.MAX_VALUE;
        int maxNegWidth = 0, maxNegHeight = 0;

        // Process positive images
        File posDir = new File(POS_DIR);
        System.out.println("Loading positive images from " + POS_DIR + "...");
        File[] posFiles = posDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (posFiles == null || posFiles.length == 0) {
            System.err.println("Error: No positive images found in " + POS_DIR);
            return null;
        }
        for (File file : posFiles) {
            try {
                BufferedImage image = ImageIO.read(file);
                int w = image.getWidth();
                int h = image.getHeight();
                if (w == 0 || h == 0) {
                    System.err.println("Invalid positive image (zero size): " + file.getName());
                    continue;
                }
                minPosWidth = Math.min(minPosWidth, w);
                minPosHeight = Math.min(minPosHeight, h);
                maxPosWidth = Math.max(maxPosWidth, w);
                maxPosHeight = Math.max(maxPosHeight, h);
                System.out.println("Positive image: " + file.getName() + ", original size: " + w + "x" + h);
                BufferedImage resized = imageProcessor.scaleTo128x128(image);
                double[] hog = hogExtractor.extractHOGFeatures(resized);
                features.add(hog);
                labels.add(1);
            } catch (IOException e) {
                System.err.println("Error loading positive image: " + file.getName());
                e.printStackTrace();
            }
        }

        // Process negative images
        File negDir = new File(NEG_DIR);
        System.out.println("Loading negative images from " + NEG_DIR + "...");
        File[] negFiles = negDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (negFiles == null || negFiles.length == 0) {
            System.err.println("Error: No negative images found in " + NEG_DIR);
            return null;
        }
        for (File file : negFiles) {
            try {
                BufferedImage image = ImageIO.read(file);
                int w = image.getWidth();
                int h = image.getHeight();
                if (w == 0 || h == 0) {
                    System.err.println("Invalid negative image (zero size): " + file.getName());
                    continue;
                }
                minNegWidth = Math.min(minNegWidth, w);
                minNegHeight = Math.min(minNegHeight, h);
                maxNegWidth = Math.max(maxNegWidth, w);
                maxNegHeight = Math.max(maxNegHeight, h);
                System.out.println("Negative image: " + file.getName() + ", original size: " + w + "x" + h);
                BufferedImage resized = imageProcessor.scaleTo128x128(image);
                double[] hog = hogExtractor.extractHOGFeatures(resized);
                features.add(hog);
                labels.add(-1);
            } catch (IOException e) {
                System.err.println("Error loading negative image: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("Training set statistics:");
        System.out.println("Positive images: " + labels.stream().filter(l -> l == 1).count());
        System.out.println("Minimum positive size: " + minPosWidth + "x" + minPosHeight);
        System.out.println("Maximum positive size: " + maxPosWidth + "x" + maxPosHeight);
        System.out.println("Negative images: " + labels.stream().filter(l -> l == -1).count());
        System.out.println("Minimum negative size: " + minNegWidth + "x" + minNegHeight);
        System.out.println("Maximum negative size: " + maxNegWidth + "x" + maxNegHeight);
        System.out.println("Total images loaded: " + features.size());

        if (features.size() < 600) {
            System.err.println("Warning: Training set is small (" + features.size() + " images). Recommend at least 600 images (300 positive + 300 negative).");
        }

        if (features.isEmpty()) {
            System.err.println("Error: No valid images were loaded for training!");
            return null;
        }

        System.out.println("Training linear SVM...");
        SVMClassifier svmClassifier = new SVMClassifier(features, labels, 0.1, new KernelLinear());
        svmClassifier.train();
        System.out.println("SVM training completed.");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("SvmFile/svm_model.ser"))) {
            oos.writeObject(svmClassifier);
            System.out.println("Classifier saved to SvmFile/svm_model.ser");
        } catch (IOException e) {
            System.err.println("Error saving SVM classifier.");
            e.printStackTrace();
        }
        return svmClassifier;
    }
}