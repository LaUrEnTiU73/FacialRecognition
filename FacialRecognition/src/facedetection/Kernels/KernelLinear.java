/**
 * The <code>facedetection.Kernels</code> package contains interfaces and implementations
 * of kernel functions used in SVM classification for facial recognition. These kernels define
 * how similarity is computed between feature vectors of images.
 */
package facedetection.Kernels;

/**
 * Class that implements a linear kernel for SVM classification, used in face detection.
 * It computes the dot product between two feature vectors, representing a linear similarity measure.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class KernelLinear implements Kernel {
    /**
     * Default constructor for the KernelLinear class.
     * Initializes an object that implements the linear kernel function.
     */
    public KernelLinear() {
        // No additional initialization required
    }

    /**
     * Computes the dot product between two feature vectors, representing the linear kernel.
     * This method is used in the face detection process to evaluate similarity.
     *
     * @param x1 the first feature vector
     * @param x2 the second feature vector
     * @return the dot product value between the vectors
     */
    @Override
    public double compute(double[] x1, double[] x2) {
        double dot = 0.0;
        for (int i = 0; i < x1.length; i++) {
            dot += x1[i] * x2[i];
        }
        return dot;
    }
}