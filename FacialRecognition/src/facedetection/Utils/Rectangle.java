/**
 * The <code>facedetection.Utils</code> package contains utility classes that support image processing,
 * feature extraction, and configuration management within the facial detection application.
 */
package facedetection.Utils;

/**
 * Class representing a rectangle with coordinates, dimensions, and an associated score,
 * used to bound regions detected as faces.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class Rectangle {
    /** X-coordinate of the top-left corner of the rectangle. */
    public int x;
    /** Y-coordinate of the top-left corner of the rectangle. */
    public int y;
    /** Width of the rectangle. */
    public int width;
    /** Height of the rectangle. */
    public int height;
    /** Score associated with the rectangle, indicating detection confidence. */
    public double score;

    /**
     * Class constructor that initializes a rectangle with coordinates, dimensions, and score.
     *
     * @param x x-coordinate of the top-left corner
     * @param y y-coordinate of the top-left corner
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param score detection confidence score
     */
    public Rectangle(int x, int y, int width, int height, double score) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.score = score;
    }
}