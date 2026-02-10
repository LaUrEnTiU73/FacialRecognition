/**
 * The <code>facedetection.SVM</code> package contains classes responsible for implementing
 * the SVM (Support Vector Machine) classifier used in face detection and recognition.
 * This package includes the logic for training, prediction, and managing support vectors.
 */
package facedetection.SVM;

import facedetection.Kernels.Kernel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Class that implements an SVM (Support Vector Machine) classifier for face detection and
 * recognition. It uses the SMO (Sequential Minimal Optimization) algorithm for training
 * and supports various kernel functions.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class SVMClassifier implements Serializable {
    /** List of support vectors used in classification. */
    private List<double[]> supportVectors;
    /** Labels associated with support vectors (1 for positive, -1 for negative). */
    private List<Integer> labels;
    /** Lagrange multipliers (alpha) for support vectors. */
    private double[] alphas;
    /** Bias term of the SVM model. */
    private double bias;
    /** Regularization parameter C of the SVM. */
    private double C;
    /** Kernel function used to compute similarity between vectors. */
    private Kernel kernel;
    /** Epsilon value for floating-point comparisons. */
    private static final double EPSILON = 1e-12;
    /** Maximum number of training iterations. */
    private static final int MAX_ITERATIONS = 10000;
    /** Minimum number of training iterations. */
    private static final int MIN_ITERATIONS = 5;
    /** Maximum allowed training time (in milliseconds, 15 minutes). */
    private static final long TIMEOUT_MS = 60 * 60 * 1000; // 15 minutes
    /** Maximum consecutive iterations without changes before stopping. */
    private static final int MAX_NO_CHANGE_ITERATIONS = 3;
    /** Estimated maximum iterations for progress calculation. */
    private static final int ESTIMATED_MAX_ITERATIONS = 1000;
    /** Expected maximum number of support vectors for progress calculation. */
    private static final int EXPECTED_MAX_SV = 200;

    /**
     * Class constructor that initializes the SVM classifier with training data.
     *
     * @param features list of feature vectors
     * @param labels list of labels associated with vectors (1 or -1)
     * @param C regularization parameter
     * @param kernel kernel function used for classification
     */
    public SVMClassifier(List<double[]> features, List<Integer> labels, double C, Kernel kernel) {
        this.supportVectors = new ArrayList<>(features);
        this.labels = new ArrayList<>(labels);
        this.alphas = new double[features.size()];
        this.bias = 0.0;
        this.C = C;
        this.kernel = kernel;
    }

    /**
     * Trains the SVM classifier using the SMO (Sequential Minimal Optimization) algorithm.
     * Optimizes Lagrange multipliers (alphas) and computes the model bias.
     */
    public void train() {
        int n = supportVectors.size();
        double[][] K = computeKernelMatrix();
        int numChanged = 0;
        boolean examineAll = true;
        int iteration = 0;
        long startTime = System.currentTimeMillis();
        int noChangeCount = 0;
        int prevSVCount = 0;

        System.out.println("Starting SVM training with " + n + " examples.");
        while (iteration < MAX_ITERATIONS && (numChanged > 0 || examineAll || iteration < MIN_ITERATIONS)) {
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                System.out.println("Timeout reached after " + (System.currentTimeMillis() - startTime) / 1000 + " seconds.");
                break;
            }

            numChanged = 0;
            if (examineAll) {
                for (int i = 0; i < n; i++) {
                    numChanged += examineExample(i, K);
                }
            } else {
                for (int i = 0; i < n; i++) {
                    if (alphas[i] > 0 && alphas[i] < C) {
                        numChanged += examineExample(i, K);
                    }
                }
            }
            iteration++;
            int svCount = countSupportVectors();
            double progressPercent = (double) iteration / ESTIMATED_MAX_ITERATIONS * 100;
            double svPercent = (double) svCount / EXPECTED_MAX_SV * 100;
            System.out.println("Iteration " + iteration + ": " + numChanged + " alphas modified, support vectors=" + svCount +
                               " (" + String.format("%.2f", svPercent) + "% of " + EXPECTED_MAX_SV + "), progress=" +
                               String.format("%.2f", progressPercent) + "%, time=" + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

            if (numChanged == 0) {
                noChangeCount++;
                if (noChangeCount >= MAX_NO_CHANGE_ITERATIONS && iteration >= MIN_ITERATIONS) {
                    System.out.println("Stopping: " + noChangeCount + " consecutive iterations with no changes.");
                    break;
                }
            } else {
                noChangeCount = 0;
            }

            if (iteration >= MIN_ITERATIONS && svCount == prevSVCount && numChanged == 0) {
                System.out.println("Stopping: Number of support vectors stabilized at " + svCount);
                break;
            }
            prevSVCount = svCount;

            if (examineAll && iteration >= MIN_ITERATIONS) {
                examineAll = false;
            } else if (numChanged == 0 && iteration >= MIN_ITERATIONS) {
                examineAll = true;
            }
        }

        double bSum = 0.0;
        int bCount = 0;
        for (int i = 0; i < n; i++) {
            if (alphas[i] > 0 && alphas[i] < C) {
                double sum = 0.0;
                for (int j = 0; j < n; j++) {
                    sum += alphas[j] * labels.get(j) * K[i][j];
                }
                bSum += labels.get(i) - sum;
                bCount++;
            }
        }
        bias = bCount > 0 ? bSum / bCount : 0.0;
        System.out.println("Training completed: " + iteration + " iterations performed.");
        System.out.println("Number of support vectors (alphas > 0): " + countSupportVectors());
        System.out.println("Bias: " + bias);
        System.out.println("Total time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
    }

    /**
     * Examines and optimizes a pair of Lagrange multipliers (alpha) for a given example.
     *
     * @param i1 index of the first example
     * @param K computed kernel matrix
     * @return 1 if alpha was modified, 0 otherwise
     */
    private int examineExample(int i1, double[][] K) {
        int y1 = labels.get(i1);
        double E1 = computeError(i1, K) - y1;
        int n = supportVectors.size();
        Random rand = new Random();

        int i2 = -1;
        double maxE = 0.0;
        for (int j = 0; j < n; j++) {
            if (j != i1) {
                double E2 = computeError(j, K) - labels.get(j);
                double absE = Math.abs(E1 - E2);
                if (absE > maxE) {
                    maxE = absE;
                    i2 = j;
                }
            }
        }
        if (i2 == -1) {
            i2 = i1;
            while (i2 == i1) {
                i2 = rand.nextInt(n);
            }
        }

        int y2 = labels.get(i2);
        double alpha1 = alphas[i1];
        double alpha2 = alphas[i2];
        double E2 = computeError(i2, K) - y2;

        double L, H;
        if (y1 != y2) {
            L = Math.max(0, alpha2 - alpha1);
            H = Math.min(C, C + alpha2 - alpha1);
        } else {
            L = Math.max(0, alpha1 + alpha2 - C);
            H = Math.min(C, alpha1 + alpha2);
        }

        if (L >= H) {
            return 0;
        }

        double k11 = K[i1][i1];
        double k22 = K[i2][i2];
        double k12 = K[i1][i2];
        double eta = 2 * k12 - k11 - k22;

        if (eta >= 0) {
            return 0;
        }

        double a2 = alpha2 - y2 * (E1 - E2) / eta;
        a2 = Math.max(L, Math.min(H, a2));

        if (Math.abs(a2 - alpha2) < EPSILON) {
            return 0;
        }

        double a1 = alpha1 + y1 * y2 * (alpha2 - a2);
        double b1 = bias - E1 - y1 * (a1 - alpha1) * k11 - y2 * (a2 - alpha2) * k12;
        double b2 = bias - E2 - y1 * (a1 - alpha1) * k12 - y2 * (a2 - alpha2) * k22;
        bias = (b1 + b2) / 2;

        alphas[i1] = a1;
        alphas[i2] = a2;

        return 1;
    }

    /**
     * Computes the prediction error for a given example.
     *
     * @param i index of the example
     * @param K computed kernel matrix
     * @return prediction error
     */
    private double computeError(int i, double[][] K) {
        double sum = bias;
        for (int j = 0; j < supportVectors.size(); j++) {
            if (alphas[j] > 0) {
                sum += alphas[j] * labels.get(j) * K[i][j];
            }
        }
        return sum;
    }

    /**
     * Computes the kernel matrix for all pairs of support vectors.
     *
     * @return kernel matrix
     */
    private double[][] computeKernelMatrix() {
        int n = supportVectors.size();
        double[][] K = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                K[i][j] = kernel.compute(supportVectors.get(i), supportVectors.get(j));
            }
        }
        return K;
    }

    /**
     * Computes the prediction score for a feature vector.
     *
     * @param x feature vector to evaluate
     * @return prediction score
     */
    public double predictScore(double[] x) {
        double sum = bias;
        for (int i = 0; i < supportVectors.size(); i++) {
            if (alphas[i] > 0) {
                sum += alphas[i] * labels.get(i) * kernel.compute(x, supportVectors.get(i));
            }
        }
        return sum;
    }

    /**
     * Predicts the class of a feature vector (1 for positive, -1 for negative).
     *
     * @param x feature vector to evaluate
     * @return predicted class (1 or -1)
     */
    public int predict(double[] x) {
        double score = predictScore(x);
        return score > 0 ? 1 : -1;
    }

    /**
     * Counts the number of support vectors (those with alpha > 0).
     *
     * @return number of support vectors
     */
    private int countSupportVectors() {
        int count = 0;
        for (double alpha : alphas) {
            if (alpha > 0) {
                count++;
            }
        }
        return count;
    }
}