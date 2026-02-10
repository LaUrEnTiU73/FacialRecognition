/**
 * The <code>facedetection.Detection</code> package contains classes responsible for detecting
 * and evaluating the performance of face detection in images. This package integrates functionalities
 * for image processing, feature extraction, and classification using SVM to
 * analyze and measure the accuracy of facial detection.
 */
package facedetection.Detection;

import facedetection.SVM.SVMClassifier;
import facedetection.Trainers.FaceDetectionTrainer;
import facedetection.Utils.HOGExtractor;
import facedetection.Utils.ImageProcessor;

import java.io.*;

/**
 * The main class of the facial detection application, which coordinates training,
 * accuracy testing, and processing images with rectangles drawn around detected faces.
 * It accepts command-line arguments to control functionalities (--train, --accuracy, --drawsquare).
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class FaceDetectionApp {
    /** Output directory for saving results. */
    private static final String OUTPUT_DIR = "output/";
    /** Path to the saved SVM model file. */
    private static final String SVM_MODEL_PATH = "SvmFile/svm_model.ser";
    /** SVM classifier used for detection. */
    private SVMClassifier svmClassifier;
    /** HOG extractor for obtaining image features. */
    private HOGExtractor hogExtractor;
    /** Image processor for preprocessing. */
    private ImageProcessor imageProcessor;

    /**
     * Entry point of the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        FaceDetectionApp app = new FaceDetectionApp();
        app.run(args);
    }

    /**
     * Class constructor that initializes the HOG extractor and image processor.
     */
    public FaceDetectionApp() {
        hogExtractor = new HOGExtractor();
        imageProcessor = new ImageProcessor();
    }

    /**
     * Runs the application based on the provided command-line arguments.
     * Supports training the classifier, testing accuracy, and processing images with rectangles.
     *
     * @param args command-line arguments (--train, --accuracy, --drawsquare)
     */
    public void run(String[] args) {
        System.out.println("Initialization: Creating output directory...");
        new File(OUTPUT_DIR).mkdirs();

        // Check command-line arguments
        boolean doTrain = false;
        boolean doAccuracy = false;
        boolean doDrawSquare = false;

        for (String arg : args) {
            switch (arg) {
                case "--train":
                    doTrain = true;
                    break;
                case "--accuracy":
                    doAccuracy = true;
                    break;
                case "--drawsquare":
                    doDrawSquare = true;
                    break;
                default:
                    System.err.println("Unknown argument: " + arg);
            }
        }

        // If no valid arguments are provided, display a message and exit
        if (!doTrain && !doAccuracy && !doDrawSquare) {
            System.out.println("No valid arguments provided. Use: --train, --accuracy, --drawsquare");
            return;
        }

        // Perform training if requested
        if (doTrain) {
            FaceDetectionTrainer trainer = new FaceDetectionTrainer(hogExtractor, imageProcessor);
            svmClassifier = trainer.trainSVM();
        }

        // For accuracy testing and drawsquare processing, attempt to load the classifier if not trained
        if ((doAccuracy || doDrawSquare) && svmClassifier == null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SVM_MODEL_PATH))) {
                svmClassifier = (SVMClassifier) ois.readObject();
                System.out.println("SVM classifier loaded from " + SVM_MODEL_PATH);
            } catch (FileNotFoundException e) {
                System.err.println("Error: File " + SVM_MODEL_PATH + " does not exist. --train is required to create the classifier.");
                return;
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading classifier from " + SVM_MODEL_PATH + ": " + e.getMessage());
                return;
            }
        }

        // Perform accuracy testing if requested
        if (doAccuracy) {
            FaceDetectionAccuracy accuracyTester = new FaceDetectionAccuracy(svmClassifier, hogExtractor, imageProcessor);
            accuracyTester.testAccuracy();
        }

        // Perform drawsquare image processing if requested
        if (doDrawSquare) {
            StaticImageSquare imageProcessorSquare = new StaticImageSquare(svmClassifier, hogExtractor, imageProcessor);
            imageProcessorSquare.processDrawSquareImages();
        }
    }
}