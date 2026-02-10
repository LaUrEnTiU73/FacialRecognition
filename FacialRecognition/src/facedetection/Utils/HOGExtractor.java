/**
 * The <code>facedetection.Utils</code> package contains utility classes that support image processing,
 * feature extraction, and configuration management within the facial detection application.
 */
package facedetection.Utils;

import java.awt.image.BufferedImage;

/**
 * Class responsible for extracting HOG (Histogram of Oriented Gradients) features
 * from an image, used for face detection and recognition.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class HOGExtractor {
    /**
     * Default constructor for the HOGExtractor class.
     * Creates an extractor for HOG features used in facial image analysis.
     */
    public HOGExtractor() {
        // Empty constructor
    }

    /** Standard processed image size (in pixels). */
    public static final int IMAGE_SIZE = 128;
    /** Cell size used for HOG histogram computation (in pixels). */
    private static final int HOG_CELL_SIZE = 8;
    /** Number of bins for gradient orientation histograms. */
    private static final int HOG_NBINS = 9;

    /**
     * Extracts HOG features from an image by computing gradients, histograms,
     * and block normalization.
     *
     * @param image the image to process
     * @return the HOG feature vector
     */
    public double[] extractHOGFeatures(BufferedImage image) {
        long startTime = System.nanoTime();
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert image to grayscale and normalize
        double[] gray = new double[width * height];
        double mean = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                double intensity = 0.299 * r + 0.587 * g + 0.114 * b;
                gray[y * width + x] = intensity;
                mean += intensity;
            }
        }
        mean /= (width * height);
        double variance = 0;
        for (int i = 0; i < gray.length; i++) {
            variance += (gray[i] - mean) * (gray[i] - mean);
        }
        variance = Math.sqrt(variance / gray.length + 1e-6);
        for (int i = 0; i < gray.length; i++) {
            gray[i] = Math.min(Math.max((gray[i] - mean) / variance, -3.0), 3.0);
        }

        // Compute gradients
        double[] gradX = new double[width * height];
        double[] gradY = new double[width * height];
        int index;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                index = y * width + x;
                gradX[index] = gray[index + 1] - gray[index - 1];
                gradY[index] = gray[index + width] - gray[index - width];
            }
        }

        // Create HOG histograms
        int cellsX = width / HOG_CELL_SIZE;
        int cellsY = height / HOG_CELL_SIZE;
        double[][] histograms = new double[cellsX * cellsY][HOG_NBINS];

        for (int cy = 0; cy < cellsY; cy++) {
            for (int cx = 0; cx < cellsX; cx++) {
                double[] hist = new double[HOG_NBINS];
                for (int i = cy * HOG_CELL_SIZE; i < (cy + 1) * HOG_CELL_SIZE && i < height; i++) {
                    for (int j = cx * HOG_CELL_SIZE; j < (cx + 1) * HOG_CELL_SIZE && j < width; j++) {
                        index = i * width + j;
                        double gx = gradX[index];
                        double gy = gradY[index];
                        double mag = Math.sqrt(gx * gx + gy * gy);
                        double angle = Math.atan2(gy, gx) * 180 / Math.PI;
                        if (angle < 0) angle += 180;
                        int bin = (int) (angle / (180.0 / HOG_NBINS));
                        if (bin >= HOG_NBINS) bin = HOG_NBINS - 1;
                        hist[bin] += mag;
                    }
                }
                histograms[cy * cellsX + cx] = hist;
            }
        }

        // Normalize blocks and generate feature vector
        double[] features = new double[(cellsX - 1) * (cellsY - 1) * HOG_NBINS * 4];
        int featureIndex = 0;
        for (int by = 0; by < cellsY - 1; by++) {
            for (int bx = 0; bx < cellsX - 1; bx++) {
                double[] block = new double[HOG_NBINS * 4];
                double norm = 0;
                int idx = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        double[] hist = histograms[(by + dy) * cellsX + (bx + dx)];
                        for (int i = 0; i < HOG_NBINS; i++) {
                            block[idx++] = hist[i];
                            norm += hist[i] * hist[i];
                        }
                    }
                }
                norm = Math.sqrt(norm + 1e-6);
                for (double v : block) {
                    features[featureIndex++] = v / norm;
                }
            }
        }

        long endTime = System.nanoTime();
        System.out.println("HOG extraction took " + (endTime - startTime) / 1_000_000.0 + " ms");
        return features;
    }

    /**
     * Computes the average gradient magnitude in an image.
     *
     * @param image the image to process
     * @return the average gradient magnitude
     */
    public double computeGradientMagnitude(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[] gray = new double[width * height];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[index++] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        double magSum = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                index = y * width + x;
                double gx = gray[index + 1] - gray[index - 1];
                double gy = gray[index + width] - gray[index - width];
                magSum += Math.sqrt(gx * gx + gy * gy);
            }
        }
        return magSum / ((width - 2) * (height - 2));
    }
}