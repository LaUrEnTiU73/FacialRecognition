/**
 * The <code>facedetection.Utils</code> package contains utility classes that support image processing,
 * feature extraction, and configuration management within the facial detection application.
 */
package facedetection.Utils;

import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Class responsible for processing and manipulating images, including resizing,
 * variance computation, and drawing rectangles.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class ImageProcessor {
    /**
     * Default constructor for the ImageProcessor class.
     * Creates an object responsible for image processing (resizing, conversion, etc.).
     */
    public ImageProcessor() {
        // No special initialization required
    }

    /** Standard processed image size (in pixels). */
    private static final int IMAGE_SIZE = 128;
    /** RGB value for the green color used when drawing rectangles. */
    private static final int GREEN_RGB = 0x00FF00;
    /** Maximum allowed width for processed images (in pixels). */
    private static final int MAX_WIDTH = 640;
    /** Maximum allowed height for processed images (in pixels). */
    private static final int MAX_HEIGHT = 360;

    /**
     * Resizes an image to the standard size of 128x128 pixels.
     *
     * @param image the image to resize
     * @return the resized image
     */
    public BufferedImage scaleTo128x128(BufferedImage image) {
        Image scaledImage = image.getScaledInstance(IMAGE_SIZE, IMAGE_SIZE, Image.SCALE_SMOOTH);
        BufferedImage scaled = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        scaled.getGraphics().drawImage(scaledImage, 0, 0, null);
        return scaled;
    }

    /**
     * Resizes an image if it exceeds the maximum allowed dimensions (640x360).
     *
     * @param image the image to process
     * @return the resized image or the original if no resizing is needed
     */
    public BufferedImage resizeIfLarge(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return image;
        }
        double scale = Math.min((double) MAX_WIDTH / width, (double) MAX_HEIGHT / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        resized.getGraphics().drawImage(scaledImage, 0, 0, null);
        System.out.println("Image resized from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
        return resized;
    }

    /**
     * Computes the variance of pixel intensities in an image.
     *
     * @param image the image to process
     * @return the pixel variance
     */
    public double computeVariance(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double mean = 0;
        double variance = 0;
        int pixelCount = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                double intensity = 0.299 * r + 0.587 * g + 0.114 * b;
                mean += intensity;
            }
        }
        mean /= pixelCount;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                double intensity = 0.299 * r + 0.587 * g + 0.114 * b;
                variance += (intensity - mean) * (intensity - mean);
            }
        }
        variance /= pixelCount;
        return Math.sqrt(variance);
    }

    /**
     * Draws a green rectangle around a specified region in an image.
     *
     * @param image the image to process
     * @param rect the rectangle defining the region (may be null)
     * @return the image with the rectangle drawn
     */
    public BufferedImage drawSquare(BufferedImage image, Rectangle rect) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                output.setRGB(x, y, image.getRGB(x, y));
            }
        }

        if (rect != null) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                if (x >= 0 && x < output.getWidth()) {
                    if (rect.y >= 0 && rect.y < output.getHeight()) output.setRGB(x, rect.y, GREEN_RGB);
                    if (rect.y + rect.height - 1 >= 0 && rect.y + rect.height - 1 < output.getHeight()) output.setRGB(x, rect.y + rect.height - 1, GREEN_RGB);
                }
            }
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                if (y >= 0 && y < output.getHeight()) {
                    if (rect.x >= 0 && rect.x < output.getWidth()) output.setRGB(rect.x, y, GREEN_RGB);
                    if (rect.x + rect.width - 1 >= 0 && rect.x + rect.width - 1 < output.getWidth()) output.setRGB(rect.x + rect.width - 1, y, GREEN_RGB);
                }
            }
        }
        return output;
    }

    /**
     * Saves an image to a file based on the person's nickname and a sequence number.
     *
     * @param image the image to save
     * @param nickname the person's nickname associated with the image
     * @param number the image sequence number
     * @throws IOException if an error occurs while writing the file
     */
    public void saveImage(BufferedImage image, String nickname, int number) throws IOException {
        String path = String.format("data/train/recognition/trainPersons/%s/%s_%d.png", nickname, nickname, number);
        File outputFile = new File(path);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "png", outputFile);
    }
}