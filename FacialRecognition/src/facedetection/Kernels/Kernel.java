/**
 * The <code>facedetection.Kernels</code> package contains interfaces and implementations
 * of kernel functions used in SVM classification for facial recognition. These kernels define
 * how similarity is computed between feature vectors of images.
 */
package facedetection.Kernels;

/**
 * Interface for a kernel function used in SVM classification. It defines a method
 * for calculating the similarity between two feature vectors. The interface is serializable
 * to allow saving and loading of SVM models.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public interface Kernel extends java.io.Serializable {
    /**
     * Computes the kernel value between two feature vectors.
     *
     * @param x1 the first feature vector
     * @param x2 the second feature vector
     * @return the kernel value representing the similarity between the vectors
     */
    double compute(double[] x1, double[] x2);
}