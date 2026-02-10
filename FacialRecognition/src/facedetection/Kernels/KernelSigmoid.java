/**
 * The <code>facedetection.Kernels</code> package contains interfaces and implementations
 * of kernel functions used in SVM classification for facial recognition. These kernels define
 * how similarity is computed between feature vectors of images.
 */
package facedetection.Kernels;

/**
 * Class that implements a sigmoid kernel for SVM classification, used in facial recognition.
 * It computes the similarity between two feature vectors using the hyperbolic tangent function,
 * after normalizing the vectors.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class KernelSigmoid implements Kernel {
    /**
     * Default constructor for the KernelSigmoid class.
     * Creates an instance of a sigmoid kernel used in SVM classification.
     */
    public KernelSigmoid() {
        // No initialization required
    }

    /** Parameter A of the sigmoid function, controlling the slope of the hyperbolic tangent. */
    private static final double SIGMOID_A = 0.001;
    /** Parameter B of the sigmoid function, shifting the hyperbolic tangent. */
    private static final double SIGMOID_B = 0.0;
    /** Epsilon value used to avoid division by zero during normalization. */
    private static final double EPSILON = 1e-12;

    /**
     * Computes the sigmoid kernel value between two feature vectors.
     * The vectors are normalized before computation, and the result is obtained using
     * the hyperbolic tangent function.
     *
     * @param x1 the first feature vector
     * @param x2 the second feature vector
     * @return the sigmoid kernel value
     */
    @Override
    public double compute(double[] x1, double[] x2) {
        double[] normX1 = normalizeVector(x1);
        double[] normX2 = normalizeVector(x2);
        double dot = 0.0;
        for (int i = 0; i < normX1.length; i++) {
            dot += normX1[i] * normX2[i];
        }
        double result = Math.tanh(SIGMOID_A * dot + SIGMOID_B);
        return result;
    }

    /**
     * Normalizes a feature vector by dividing it by its Euclidean norm.
     * If the norm is less than epsilon, the original vector is returned to avoid
     * division by zero.
     *
     * @param vector the feature vector to normalize
     * @return the normalized vector
     */
    private double[] normalizeVector(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm < EPSILON) {
            return vector; // Avoid division by zero
        }
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}