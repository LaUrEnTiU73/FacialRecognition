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
import java.util.*;

/**
 * Class responsible for detecting faces in an image using an SVM classifier
 * and HOG features. It implements a sliding window approach to identify
 * regions containing faces, followed by filtering to eliminate overlaps.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class FaceDetector {
    /** Minimum score threshold to consider a region as a face. */
    public static final double SCORE_THRESHOLD = 0.035;
    /** Window sizes used for detection (in pixels). */
    private static final int[] WINDOW_SIZES = {112, 128};
    /** Sliding steps corresponding to each window size (in pixels). */
    private static final int[] STEPS = {40, 48};
    /** Minimum gradient magnitude threshold to process a window. */
    private static final double GRADIENT_THRESHOLD = 10.0;
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
    public FaceDetector(HOGExtractor hogExtractor, ImageProcessor imageProcessor) {
        this.hogExtractor = hogExtractor;
        this.imageProcessor = imageProcessor;
    }

    /**
     * Detects faces in an image using a sliding window approach.
     * Each window is evaluated with an SVM classifier, and results are filtered
     * to remove overlaps.
     *
     * @param image the image to process
     * @param svmClassifier the SVM classifier used for prediction
     * @return the list of rectangles bounding the detected faces
     */
    public List<Rectangle> detectFaces(BufferedImage image, SVMClassifier svmClassifier) {
        long startTime = System.currentTimeMillis();
        List<Rectangle> faces = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        // Check image variance
        double variance = imageProcessor.computeVariance(image);
        if (variance < 20.0) {
            System.out.println("Low-variance image (variance=" + variance + "), skipping detection.");
            return faces;
        }

        System.out.println("Starting face detection in image of size " + width + "x" + height);
        for (int i = 0; i < WINDOW_SIZES.length; i++) {
            int windowSize = WINDOW_SIZES[i];
            int step = STEPS[i];
            System.out.println("Checking window size " + windowSize + "x" + windowSize + " with step " + step);
            long windowStart = System.currentTimeMillis();
            int windowCount = 0;
            int skippedWindows = 0;
            for (int y = 0; y <= height - windowSize; y += step) {
                for (int x = 0; x <= width - windowSize; x += step) {
                    BufferedImage patch = image.getSubimage(x, y, windowSize, windowSize);
                    double patchVariance = imageProcessor.computeVariance(patch);
                    if (patchVariance < 20.0) {
                        skippedWindows++;
                        continue;
                    }
                    double gradientMag = hogExtractor.computeGradientMagnitude(patch);
                    if (gradientMag < GRADIENT_THRESHOLD) {
                        skippedWindows++;
                        continue;
                    }
                    BufferedImage resizedPatch = imageProcessor.scaleTo128x128(patch);
                    double[] hog = hogExtractor.extractHOGFeatures(resizedPatch);
                    double score = svmClassifier.predictScore(hog);
                    if (score > SCORE_THRESHOLD) {
                        faces.add(new Rectangle(x, y, windowSize, windowSize, score));
                        System.out.println("Face detected at x=" + x + ", y=" + y + ", size=" + windowSize + ", score=" + score);
                    } else {
                        System.out.println("Window at x=" + x + ", y=" + y + ", size=" + windowSize + ", score=" + score + " (below threshold)");
                    }
                    windowCount++;
                }
            }
            long windowEnd = System.currentTimeMillis();
            System.out.println("Processed " + windowCount + " windows (" + skippedWindows + " skipped) for size " + windowSize + " in " + (windowEnd - windowStart) / 1000.0 + " seconds");
        }

        List<Rectangle> filteredFaces = filterOverlappingFaces(faces);
        long endTime = System.currentTimeMillis();
        System.out.println("Total detection took " + (endTime - startTime) / 1000.0 + " seconds, detected faces: " + filteredFaces.size());
        return filteredFaces;
    }

    /**
     * Filters detected faces to eliminate overlaps using Non-Maximum Suppression (NMS).
     * Retains only the face with the highest score in overlapping regions.
     *
     * @param faces the initial list of detected faces
     * @return the filtered list of faces
     */
    private List<Rectangle> filterOverlappingFaces(List<Rectangle> faces) {
        long startTime = System.currentTimeMillis();
        List<Rectangle> filtered = new ArrayList<>();
        faces.sort((a, b) -> Double.compare(b.score, a.score));

        for (Rectangle face : faces) {
            boolean keep = true;
            for (Rectangle kept : filtered) {
                if (iou(face, kept) > 0.4) {
                    keep = false;
                    break;
                }
            }
            if (keep) filtered.add(face);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("NMS filtering took " + (endTime - startTime) / 1000.0 + " seconds");
        return filtered;
    }

    /**
     * Calculates the Intersection over Union (IoU) between two rectangles.
     *
     * @param a the first rectangle
     * @param b the second rectangle
     * @return the IoU value between the two rectangles
     */
    private double iou(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);
        int intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        int union = a.width * a.height + b.width * b.height - intersection;
        return (double) intersection / union;
    }
}