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
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

/**
 * Class responsible for processing static images, detecting faces, and drawing a
 * rectangle around the face with the highest score. It saves both the processed image
 * and the resized face region.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class StaticImageSquare {
    /** Directory containing images to process for drawing rectangles. */
    private static final String DRAW_DIR = "data/drawsquare/";
    /** Output directory where processed images are saved. */
    private static final String OUTPUT_DIR = "output/";
    /** SVM classifier used for detection. */
    private SVMClassifier svmClassifier;
    /** HOG extractor for obtaining image features. */
    private HOGExtractor hogExtractor;
    /** Image processor for preprocessing and resizing. */
    private ImageProcessor imageProcessor;
    /** Face detector that uses HOG and SVM. */
    private FaceDetector faceDetector;

    /**
     * Class constructor that initializes the components required for image processing.
     *
     * @param svmClassifier the SVM classifier used for detection
     * @param hogExtractor the HOG extractor for image features
     * @param imageProcessor the image processor for preprocessing
     */
    public StaticImageSquare(SVMClassifier svmClassifier, HOGExtractor hogExtractor, ImageProcessor imageProcessor) {
        this.svmClassifier = svmClassifier;
        this.hogExtractor = hogExtractor;
        this.imageProcessor = imageProcessor;
        this.faceDetector = new FaceDetector(hogExtractor, imageProcessor);
    }

    /**
     * Processes images from the specified directory, detects faces, and draws a rectangle
     * around the face with the highest score. Saves the processed image and the resized face region.
     */
    public void processDrawSquareImages() {
        System.out.println("Starting image processing from " + DRAW_DIR + "...");
        File drawDir = new File(DRAW_DIR);
        File[] imageFiles = drawDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("No images found in " + DRAW_DIR);
            return;
        }

        System.out.println("Number of images to process: " + imageFiles.length);
        int processedCount = 0;

        for (File imageFile : imageFiles) {
            try {
                System.out.println("Processing image: " + imageFile.getName());
                long startTime = System.currentTimeMillis();
                BufferedImage image = ImageIO.read(imageFile);
                BufferedImage resized = imageProcessor.resizeIfLarge(image);
                System.out.println("Image resized to " + resized.getWidth() + "x" + resized.getHeight());
                long detectStart = System.currentTimeMillis();
                List<Rectangle> faces = faceDetector.detectFaces(resized, svmClassifier);
                long detectEnd = System.currentTimeMillis();
                System.out.println("Face detection took " + (detectEnd - detectStart) / 1000.0 + " seconds");
                if (!faces.isEmpty()) {
                    System.out.println("Detected faces: " + faces.size());
                    for (Rectangle face : faces) {
                        System.out.println("Face at x=" + face.x + ", y=" + face.y + ", size=" + face.width + ", score=" + face.score);
                    }
                    Rectangle bestFace = faces.stream()
                            .max(Comparator.comparingDouble(r -> r.score))
                            .orElse(null);

                    BufferedImage outputImage = imageProcessor.drawSquare(resized, bestFace);
                    BufferedImage faceRegion = resized.getSubimage(bestFace.x, bestFace.y, bestFace.width, bestFace.height);
                    BufferedImage scaledFace = imageProcessor.scaleTo128x128(faceRegion);

                    String outputName = imageFile.getName().replace(".jpg", ".png").replace(".png", ".png");
                    ImageIO.write(outputImage, "png", new File(OUTPUT_DIR + "processed_" + outputName));
                    ImageIO.write(scaledFace, "png", new File(OUTPUT_DIR + "face_" + outputName));
                    System.out.println("Processed image saved: processed_" + outputName + ", face_" + outputName);
                    processedCount++;
                } else {
                    System.out.println("No face detected in: " + imageFile.getName() + " (no region with score > " + FaceDetector.SCORE_THRESHOLD + ")");
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Processing image " + imageFile.getName() + " took " + (endTime - startTime) / 1000.0 + " seconds");
            } catch (IOException e) {
                System.err.println("Error processing image: " + imageFile.getName());
                e.printStackTrace();
            }
        }
        System.out.println("Processing completed. Images processed: " + processedCount + "/" + imageFiles.length);
    }
}