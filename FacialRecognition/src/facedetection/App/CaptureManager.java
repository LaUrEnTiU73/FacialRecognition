package facedetection.App;

import facedetection.SVM.SVMClassifier;
import facedetection.Trainers.FaceRecognitionTrainer;
import facedetection.Utils.*;
import facedetection.Utils.Rectangle;
import facedetection.Detection.FaceDetector;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Class responsible for managing image capture, classifier training,
 * and facial recognition testing using the webcam. It coordinates the interaction
 * between the graphical user interface and the image-processing components.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class CaptureManager {
    /** Maximum number of photos captured in one session. */
    private static final int MAX_PHOTOS = 500;
    /** Maximum number of consecutive frames without a detected face before displaying a message. */
    private static final int NO_FACE_FRAME_THRESHOLD = 2;
    /** Capture frequency (frames per second). */
    private static final int FPS = 10;
    /** Delay between frames, calculated in milliseconds (1000/FPS). */
    private static final int FRAME_DELAY_MS = 1000 / FPS;
    /** Directory where SVM models are stored. */
    private static final String SVM_DIR = "SvmFile";
    /** Directory where training images are stored. */
    private static final String TRAIN_DIR = "data/train/recognition/trainPersons";

    /** Reference to the main application graphical user interface. */
    private WebcamCaptureApp ui;
    /** Object for capturing video stream from the webcam. */
    private VideoCapture capture;
    /** Processor for handling captured images. */
    private ImageProcessor processor;
    /** HOG feature extractor for face detection. */
    private HOGExtractor hogExtractor;
    /** Face detector that uses HOG and the image processor. */
    private FaceDetector faceDetector;
    /** SVM classifier used for initial face detection. */
    private SVMClassifier svmClassifier;
    /** Nickname of the current person for image capture. */
    private String nickname;
    /** Counter for the number of captured photos. */
    private int photoCount;
    /** Counter for consecutive frames without a detected face. */
    private int noFaceFrameCount;
    /** Timer for periodic frame capture. */
    private javax.swing.Timer captureTimer;
    /** List of loaded SVM classifiers for recognition. */
    private List<SVMClassifier> svmClassifiers;
    /** List of nicknames associated with the SVM classifiers. */
    private List<String> classifierNicknames;
    /** Manager for displaying training progress. */
    private TrainingProgressManager progressManager;

    /**
     * Static block for loading the OpenCV library.
     */
    static {
        System.loadLibrary("opencv_java<version>");
    }

    /**
     * Class constructor that initializes the components required for image capture and processing.
     *
     * @param ui the main application graphical user interface
     */
    public CaptureManager(WebcamCaptureApp ui) {
        this.ui = ui;
        processor = new ImageProcessor();
        hogExtractor = new HOGExtractor();
        faceDetector = new FaceDetector(hogExtractor, processor);
        svmClassifiers = new ArrayList<>();
        classifierNicknames = new ArrayList<>();
        progressManager = new TrainingProgressManager();
        loadSVMClassifier();
        initializeCamera();
    }

    /**
     * Initializes the webcam and sets the frame resolution.
     */
    private void initializeCamera() {
        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            JOptionPane.showMessageDialog(ui, "Error: Webcam cannot be opened.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        capture.set(3, ui.getFrameWidth());
        capture.set(4, ui.getFrameHeight());
    }

    /**
     * Loads the main SVM classifier from the specified file.
     */
    private void loadSVMClassifier() {
        File svmFile = new File(SVM_DIR + "/svm_model.ser");
        if (!svmFile.exists()) {
            JOptionPane.showMessageDialog(ui, "Error: File svm_model.ser does not exist in SvmFile.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(svmFile))) {
            svmClassifier = (SVMClassifier) ois.readObject();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ui, "Error loading SVM: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Loads all available SVM classifiers from the training directory.
     */
    private void loadAllSVMClassifiers() {
        svmClassifiers.clear();
        classifierNicknames.clear();
        File trainDir = new File(TRAIN_DIR);
        File[] personDirs = trainDir.listFiles(File::isDirectory);
        if (personDirs == null) {
            System.out.println("Error: Directory " + TRAIN_DIR + " is empty or does not exist.");
            return;
        }

        System.out.println("Loading classifiers from " + TRAIN_DIR + "...");
        for (File personDir : personDirs) {
            String nickname = personDir.getName();
            File svmFile = new File(personDir, "svm_" + nickname + ".ser");
            if (svmFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(svmFile))) {
                    SVMClassifier svm = (SVMClassifier) ois.readObject();
                    svmClassifiers.add(svm);
                    classifierNicknames.add(nickname);
                    System.out.println("Classifier loaded for " + nickname + ": " + svmFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("Error loading classifier for " + nickname + ": " + e.getMessage());
                }
            } else {
                System.out.println("SVM file does not exist for " + nickname + ": " + svmFile.getAbsolutePath());
            }
        }
        System.out.println("Total classifiers loaded: " + svmClassifiers.size());
    }
    /**
     * Checks whether all SVM classifiers are available in the training directories.
     */
     private boolean areAllSVMFilesPresent() {
        File trainDir = new File(TRAIN_DIR);
        File[] personDirs = trainDir.listFiles(File::isDirectory);
        if (personDirs == null || personDirs.length == 0) {
            System.out.println("Directory " + TRAIN_DIR + " is empty or does not exist.");
            return false;
        }

        for (File personDir : personDirs) {
            String nickname = personDir.getName();
            File svmFile = new File(personDir, "svm_" + nickname + ".ser");
            if (!svmFile.exists()) {
                System.out.println("SVM file missing for " + nickname + ": " + svmFile.getAbsolutePath());
                return false;
            }
        }
        System.out.println("All directories in " + TRAIN_DIR + " have the corresponding SVM files.");
        return true;
    }

    /**
     * Initiates the SVM classifier training and testing process.
     */
    public void startTrainingAndTesting() {
        for (Component comp : ui.getButtonPanel().getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }
          // Refresh the list of folders before displaying the table
        progressManager.refreshPersons();

        ui.getMainPanel().removeAll();
        ui.getMainPanel().add(ui.getButtonPanel(), BorderLayout.WEST);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(progressManager.getTrainingTable()), BorderLayout.CENTER);
        centerPanel.setBackground(new Color(176, 196, 222));
        ui.getMainPanel().add(centerPanel, BorderLayout.CENTER);
        ui.getMainPanel().revalidate();
        ui.getMainPanel().repaint();
    if (areAllSVMFilesPresent()) {
            // Skip training and proceed directly to testing
            SwingUtilities.invokeLater(() -> {
                ui.getProgressLabel().setText("");
                loadAllSVMClassifiers();
                if (svmClassifiers.isEmpty()) {
                    JOptionPane.showMessageDialog(ui, "Error: No SVM classifier available.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    ui.showInitialButtons();
                    for (Component comp : ui.getButtonPanel().getComponents()) {
                        if (comp instanceof JButton) {
                            comp.setEnabled(true);
                        }
                    }
                } else {
                    startTesting();
                }
            });
        } else {
        new Thread(() -> {
            try {
                FaceRecognitionTrainer trainer = new FaceRecognitionTrainer();
                trainer.setProgressCallback((nickname, status) -> {
                    SwingUtilities.invokeLater(() -> {
                        progressManager.updateProgress(nickname, status);
                    });
                });
                trainer.train();
                SwingUtilities.invokeLater(() -> {
                    ui.getProgressLabel().setText("");
                    loadAllSVMClassifiers();
                    if (svmClassifiers.isEmpty()) {
                        JOptionPane.showMessageDialog(ui, "Error: No SVM classifier available.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                        ui.showInitialButtons();
                        for (Component comp : ui.getButtonPanel().getComponents()) {
                            if (comp instanceof JButton) {
                                comp.setEnabled(true);
                            }
                        }
                    } else {
                        startTesting();
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(ui, "Training error: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    ui.getProgressLabel().setText("");
                    ui.showInitialButtons();
                    for (Component comp : ui.getButtonPanel().getComponents()) {
                        if (comp instanceof JButton) {
                            comp.setEnabled(true);
                        }
                    }
                });
            }
        }).start();
    }
    }
    /**
     * Initiates the facial recognition testing process using the webcam.
     */
    private void startTesting() {
        if (!capture.isOpened()) {
            initializeCamera();
        }

        ui.getCancelButton().setVisible(true);
        ui.getCancelButton().addActionListener(e -> cancelTesting());
        

        ui.getMainPanel().removeAll();
        ui.getMainPanel().add(ui.getButtonPanel(), BorderLayout.WEST);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(ui.getImageLabel(), BorderLayout.CENTER);
        centerPanel.add(ui.getCancelButton(), BorderLayout.SOUTH);
        ui.getMainPanel().add(centerPanel, BorderLayout.CENTER);
        ui.getMainPanel().revalidate();
        ui.getMainPanel().repaint();

        captureTimer = new javax.swing.Timer(FRAME_DELAY_MS, e -> captureAndProcessFrame());
        captureTimer.start();
    }

    /**
     * Cancels the testing process and restores the interface.
     */
    private void cancelTesting() {
        if (captureTimer != null) {
            captureTimer.stop();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        ui.getCancelButton().setVisible(false);
        ui.getImageContentLabel().setIcon(null);
        for (Component comp : ui.getButtonPanel().getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(true);
            }
        }
        ui.showInitialButtons();
    }

    /**
     * Captures a frame from the webcam, processes faces, and displays the results.
     */
    private void captureAndProcessFrame() {
        Mat frame = new Mat();
        if (!capture.read(frame)) {
            JOptionPane.showMessageDialog(ui, "Error capturing image.", "Error", JOptionPane.ERROR_MESSAGE);
            cancelTesting();
            return;
        }

        BufferedImage image = matToBufferedImage(frame);
        BufferedImage processedImage = processor.resizeIfLarge(image);
        List<Rectangle> faces = faceDetector.detectFaces(processedImage, svmClassifier);
        System.out.println("Detected faces: " + faces.size());



        BufferedImage displayImage = new BufferedImage(processedImage.getWidth(), processedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = displayImage.createGraphics();
        g2d.drawImage(processedImage, 0, 0, null);

        for (Rectangle face : faces) {
            g2d.setColor(Color.GREEN);
            g2d.drawRect(face.x, face.y, face.width, face.height);

            BufferedImage faceImg = processedImage.getSubimage(face.x, face.y, face.width, face.height);
            BufferedImage scaledFace = processor.scaleTo128x128(faceImg);
            
         

            long hogStartTime = System.currentTimeMillis();
            double[] hogFeatures = hogExtractor.extractHOGFeatures(scaledFace);
            long hogEndTime = System.currentTimeMillis();
            System.out.println("HOG extracted for face at x=" + face.x + ", y=" + face.y + ": size=" + hogFeatures.length + 
                ", time=" + (hogEndTime - hogStartTime) + "ms");
            System.out.println("First 5 HOG elements: [" + hogFeatures[0] + ", " + hogFeatures[1] + ", " + 
                hogFeatures[2] + ", " + hogFeatures[3] + ", " + hogFeatures[4] + "]");

            boolean recognized = false;
            String displayText = "Unknown";
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            for (int i = 0; i < svmClassifiers.size(); i++) {
                int prediction = svmClassifiers.get(i).predict(hogFeatures);
                System.out.println("Classifier " + classifierNicknames.get(i) + ": prediction=" + prediction);
                if (prediction == 1) {
                    displayText = classifierNicknames.get(i);
                    g2d.setColor(Color.GREEN);
                    recognized = true;
                    break;
                }
            }

            if (!recognized) {
                g2d.setColor(Color.RED);
            }

            g2d.drawString(displayText, face.x, face.y - 10);
        }
        g2d.dispose();

        ui.getImageContentLabel().setIcon(new ImageIcon(displayImage));
    }

    /**
     * Converts an OpenCV matrix (Mat) to a BufferedImage.
     *
     * @param mat the OpenCV matrix containing the image
     * @return the converted image in BufferedImage format
     */
    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);

        byte[] rgbData = new byte[width * height * 3];
        for (int i = 0; i < width * height; i++) {
            rgbData[i * 3] = data[i * 3 + 2];
            rgbData[i * 3 + 1] = data[i * 3 + 1];
            rgbData[i * 3 + 2] = data[i * 3];
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, width, height, rgbData);
        return image;
    }

    /**
     * Initiates the image capture process for training.
     */
    public void startCapture() {
        String input = JOptionPane.showInputDialog(ui, "Enter nickname:", "Nickname", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(ui, "Invalid nickname.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        nickname = input.trim();
        photoCount = 0;
        noFaceFrameCount = 0;
        ui.getProgressLabel().setText("0/" + MAX_PHOTOS);
        ui.getImageContentLabel().setIcon(null);

        for (Component comp : ui.getButtonPanel().getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }

        ui.getCancelButton().setVisible(true);
        ui.getCancelButton().addActionListener(e -> cancelCapture());

        if (!capture.isOpened()) {
            initializeCamera();
        }

        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel imageContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 20, 0);
        imageContainer.add(ui.getProgressLabel(), gbc);
        gbc.insets = new Insets(0, 0, 10, 0);
        imageContainer.add(ui.getImageLabel(), gbc);
        gbc.insets = new Insets(0, 0, 0, 0);
        imageContainer.add(ui.getCancelButton(), gbc);
        centerPanel.add(imageContainer, BorderLayout.CENTER);

        ui.getMainPanel().removeAll();
        ui.getMainPanel().add(ui.getButtonPanel(), BorderLayout.WEST);
        ui.getMainPanel().add(centerPanel, BorderLayout.CENTER);
        ui.getMainPanel().revalidate();
        ui.getMainPanel().repaint();

        captureTimer = new javax.swing.Timer(100, e -> captureAndDisplay());
        captureTimer.start();
    }

    /**
     * Cancels the capture process and restores the interface.
     */
    private void cancelCapture() {
        if (captureTimer != null) {
            captureTimer.stop();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        ui.getCancelButton().setVisible(false);
        ui.getProgressLabel().setText("");
        ui.getImageContentLabel().setIcon(null);

        for (Component comp : ui.getButtonPanel().getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(true);
            }
        }

        ui.showInitialButtons();
    }

    /**
     * Captures and displays images for training, saving detected faces.
     */
    private void captureAndDisplay() {
        Mat frame = new Mat();
        if (!capture.read(frame)) {
            JOptionPane.showMessageDialog(ui, "Error capturing image.", "Error", JOptionPane.ERROR_MESSAGE);
            cancelCapture();
            return;
        }

        BufferedImage image = matToBufferedImage(frame);
        BufferedImage processedImage = processor.resizeIfLarge(image);
        java.util.List<Rectangle> faces = faceDetector.detectFaces(processedImage, svmClassifier);

        if (!faces.isEmpty()) {
            noFaceFrameCount = 0;
            ui.getProgressLabel().setText(photoCount + "/" + MAX_PHOTOS);
            Rectangle face = faces.get(0);
            BufferedImage faceImg = processedImage.getSubimage(face.x, face.y, face.width, face.height);
            BufferedImage scaledFace = processor.scaleTo128x128(faceImg);
            photoCount++;

            try {
                processor.saveImage(scaledFace, nickname, photoCount);
                ui.getImageContentLabel().setIcon(new ImageIcon(scaledFace));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(ui, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                cancelCapture();
                return;
            }

            if (photoCount >= MAX_PHOTOS) {
                captureTimer.stop();
                capture.release();
                ui.getCancelButton().setVisible(false);
                for (Component comp : ui.getButtonPanel().getComponents()) {
                    if (comp instanceof JButton) {
                        comp.setEnabled(true);
                    }
                }
                JOptionPane.showMessageDialog(ui, "500 photos have been taken.", "Success", JOptionPane.INFORMATION_MESSAGE);
                ui.showInitialButtons();
                ui.getProgressLabel().setText("");
                ui.getImageContentLabel().setIcon(null);
            }
        } else {
            noFaceFrameCount++;
            if (noFaceFrameCount >= NO_FACE_FRAME_THRESHOLD) {
                ui.getProgressLabel().setText("Waiting: No face detected");
            }
        }
    }

    /**
     * Returns the current person's nickname.
     *
     * @return the set nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the person's nickname for capture.
     *
     * @param nickname the new nickname
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}