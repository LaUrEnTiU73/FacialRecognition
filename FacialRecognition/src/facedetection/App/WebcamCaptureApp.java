/**
 * The <code>facedetection.App</code> package contains the main classes of the facial recognition application.
 * These classes manage the graphical user interface (GUI), the logic for capturing images from the camera,
 * editing saved photographs, the training process of the facial recognition algorithm,
 * and displaying progress to the user.
 * The package serves as the central point for coordinating all application modules,
 * providing an intuitive interface and integrated functionalities for the user.
 */

package facedetection.App;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.*;

/**
 * The main class of the facial recognition application.
 * It extends JFrame and manages the creation and display of the application's main graphical interface.
 * The primary role of this class is to coordinate visual components and connect user actions
 * to application functionalities, such as:
 * - initiating video capture from the camera,
 * - training and testing the facial classifier,
 * - editing and deleting saved photographs,
 * - managing application settings.
 *
 * The class uses layouts such as BorderLayout and BoxLayout for visual organization,
 * and includes modern styling features for buttons and panels.
 *
 * @author Albu Laurențiu Cătălin
 * @version 1.0
 */
public class WebcamCaptureApp extends JFrame {
    /** Default width of the application window (in pixels). */
    private static final int FRAME_WIDTH = 1280;
    /** Default height of the application window (in pixels). */
    private static final int FRAME_HEIGHT = 720;
    /** Default size of the image display (in pixels). */
    private static final int IMAGE_DISPLAY_SIZE = 200;
    /** Border spacing around components (in pixels). */
    private static final int BORDER_SPACE = 10;
    /** Thickness of component borders (in pixels). */
    private static final int BORDER_THICKNESS = 1;
    /** Total size of the image panel, including borders (in pixels). */
    private static final int PANEL_SIZE = IMAGE_DISPLAY_SIZE + 2 * (BORDER_SPACE + BORDER_THICKNESS);
    /** Size of button icons (in pixels). */
    private static final int ICON_SIZE = 24;
    /** Size of the application logo (in pixels). */
    private static final int LOGO_SIZE = 60;

    /** Main panel containing all interface components. */
    private JPanel mainPanel;
    /** Panel containing the application buttons, positioned on the left. */
    private JPanel buttonPanel;
    /** Subpanel that organizes buttons in the main layout. */
    private JPanel buttonsSubPanel;
    /** Inner panel that vertically aligns buttons using BoxLayout. */
    private JPanel innerButtonsPanel;
    /** Panel displaying the camera video stream. */
    private JPanel imageLabel;
    /** Label displaying image content or status messages. */
    private JLabel imageContentLabel;
    /** Label displaying training or capture progress. */
    private JLabel progressLabel;
    /** Button for canceling ongoing operations. */
    private JButton cancelButton;
    /** Panel displaying the edited image. */
    private JPanel editImageLabel;
    /** Label displaying the content of the edited image. */
    private JLabel editImageContentLabel;
    /** Label displaying the number of captured photos. */
    private JLabel photoCountLabel;
    /** Label displaying the application version. */
    private JLabel versionLabel;
    /** Panel containing the version label. */
    private JPanel versionPanel;
    /** Manager handling data capture and training. */
    private CaptureManager captureManager;
    /** Editor handling photo editing. */
    private PhotoEditor photoEditor;
    

    /**
     * Class constructor that initializes the graphical interface and components.
     */
    public WebcamCaptureApp() {
        setTitle("Facial Recognition - Artificial Intelligence");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 248, 255));
        try {
            Image logo = Toolkit.getDefaultToolkit().getImage("icons/logo.png");
            setIconImage(logo);
        } catch (Exception e) {
            System.err.println("Could not load application logo.");
        }

        captureManager = new CaptureManager(this);
        photoEditor = new PhotoEditor(this);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(new Color(50, 60, 80));
        buttonPanel.setPreferredSize(new Dimension(260, getHeight()));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        buttonsSubPanel = new JPanel(new BorderLayout());
        buttonsSubPanel.setOpaque(false);

        innerButtonsPanel = new JPanel();
        innerButtonsPanel.setLayout(new BoxLayout(innerButtonsPanel, BoxLayout.Y_AXIS));
        innerButtonsPanel.setOpaque(false);

        imageContentLabel = new JLabel("Waiting for camera stream", SwingConstants.CENTER);
        imageContentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageContentLabel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        imageContentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        imageContentLabel.setForeground(new Color(150, 150, 150));

        imageLabel = new JPanel(new BorderLayout());
        imageLabel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        imageLabel.setBackground(new Color(245, 245, 245));
        imageLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), BORDER_THICKNESS),
            BorderFactory.createEmptyBorder(BORDER_SPACE, BORDER_SPACE, BORDER_SPACE, BORDER_SPACE)));
        imageLabel.add(imageContentLabel, BorderLayout.CENTER);

        progressLabel = new JLabel("");
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        progressLabel.setForeground(new Color(50, 50, 50));

        cancelButton = createStyledButton("Cancel", new Color(220, 53, 69));
        cancelButton.setVisible(false);

        photoCountLabel = new JLabel("");
        photoCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        photoCountLabel.setForeground(new Color(50, 50, 50));

        editImageContentLabel = new JLabel();
        editImageContentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        editImageContentLabel.setPreferredSize(new Dimension(IMAGE_DISPLAY_SIZE, IMAGE_DISPLAY_SIZE));

        editImageLabel = new JPanel(new BorderLayout());
        editImageLabel.setPreferredSize(new Dimension(PANEL_SIZE, PANEL_SIZE));
        editImageLabel.setBackground(new Color(176, 196, 222));
        editImageLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), BORDER_THICKNESS),
            BorderFactory.createEmptyBorder(BORDER_SPACE, BORDER_SPACE, BORDER_SPACE, BORDER_SPACE)));
        editImageLabel.add(editImageContentLabel, BorderLayout.CENTER);

        versionLabel = new JLabel("Version 1.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(Color.WHITE);

        versionPanel = new JPanel();
        versionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        versionPanel.setOpaque(false);
        versionPanel.add(versionLabel);

        buttonsSubPanel.add(innerButtonsPanel, BorderLayout.CENTER);
        buttonPanel.add(buttonsSubPanel, BorderLayout.CENTER);
        buttonPanel.add(versionPanel, BorderLayout.SOUTH);

        mainPanel.add(buttonPanel, BorderLayout.WEST);
        add(mainPanel);

        showInitialButtons();
    }

    /**
     * Creates a stylized button with text, background color, and icon.
     *
     * @param text the text displayed on the button
     * @param backgroundColor the background color of the button
     * @return a configured JButton object
     */
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(280, 40));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 0, 0, 50)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(backgroundColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    /**
     * Creates a title label with logo and text.
     *
     * @return a configured JLabel for the title
     */
    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("<html><div style='text-align: center;'>Facial<br>Recognition</div></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        try {
            ImageIcon logo = new ImageIcon("icons/logo.png");
            Image scaledLogo = logo.getImage().getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH);
            titleLabel.setIcon(new ImageIcon(scaledLogo));
            titleLabel.setIconTextGap(10);
        } catch (Exception e) {
            System.err.println("Logo not found: icons/logo.png");
        }

        return titleLabel;
    }

    /**
     * Displays the initial application buttons.
     */
    public void showInitialButtons() {
        imageContentLabel.setText("Waiting for camera stream "); // or

        buttonsSubPanel.removeAll();
        innerButtonsPanel.removeAll();

        buttonsSubPanel.add(createTitleLabel(), BorderLayout.NORTH);

        JButton testButton = createStyledButton("Test", new Color(52, 152, 219));
        JButton photographButton = createStyledButton("Photograph", new Color(46, 204, 113));
        JButton settingsButton = createStyledButton("Settings", new Color(111, 66, 193));
        JButton aboutButton = createStyledButton("About", new Color(188, 143, 143));

        innerButtonsPanel.add(Box.createVerticalGlue());
        innerButtonsPanel.add(testButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(photographButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(settingsButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(aboutButton);
        innerButtonsPanel.add(Box.createVerticalGlue());

        buttonsSubPanel.add(innerButtonsPanel, BorderLayout.CENTER);

       testButton.addActionListener(e -> {
        
imageContentLabel.setText(""); 
    testButton.setEnabled(false);
    photographButton.setEnabled(false);
    
    // Invoke testing logic
    captureManager.startTrainingAndTesting();
});

        photographButton.addActionListener(e -> showPhotographButtons());
        aboutButton.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            "<html><b>Application Purpose:</b> Facial recognition for person identification.<br>" +
            "<b>Author:</b> Albu Laurențiu Cătălin<br>" +
            "<b>Group:</b> M533<br>" +
            "<b>Email:</b> laurentiualbu541@gmail.com</html>",
            "About the Application",
            JOptionPane.INFORMATION_MESSAGE
        ));
        settingsButton.addActionListener(e -> {
            new SettingsDialog(this).setVisible(true);
        });
        mainPanel.removeAll();
        mainPanel.setBackground(new Color(255, 255, 255));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        mainPanel.add(buttonPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 240, 240));
        JPanel imageContainer = new JPanel(new GridBagLayout());
        imageContainer.setBackground(new Color(176, 196, 222));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(20, 0, 0, 0);
        imageContainer.add(imageLabel, gbc);
        centerPanel.add(imageContainer, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Displays buttons for photography functionalities.
     */
    public void showPhotographButtons() {
        buttonsSubPanel.removeAll();
        innerButtonsPanel.removeAll();

        buttonsSubPanel.add(createTitleLabel(), BorderLayout.NORTH);

        JButton startButton = createStyledButton("Start", new Color(46, 204, 113));
        JButton editButton = createStyledButton("Edit Photos", new Color(255, 193, 7));
        JButton deleteButton = createStyledButton("Delete All", new Color(220, 53, 69));
        JButton backButton = createStyledButton("Back", new Color(108, 117, 125));

        innerButtonsPanel.add(Box.createVerticalGlue());
        innerButtonsPanel.add(startButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(editButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(deleteButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(backButton);
        innerButtonsPanel.add(Box.createVerticalGlue());

        buttonsSubPanel.add(innerButtonsPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> {
        imageContentLabel.setText(""); // or
        captureManager.startCapture();});
        
        editButton.addActionListener(e -> photoEditor.editPhotos());
        deleteButton.addActionListener(e -> photoEditor.deleteAllPhotos());
        backButton.addActionListener(e -> showInitialButtons());

        mainPanel.removeAll();
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        JPanel imageContainer = new JPanel(new GridBagLayout());
        imageContainer.setBackground(new Color(176, 196, 222));
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(imageContainer, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Displays the interface for editing photos associated with a specific nickname.
     *
     * @param nickname the nickname for which photos are being edited
     */
    public void showEditPhotosUI(String nickname) {
        buttonsSubPanel.removeAll();
        innerButtonsPanel.removeAll();

        buttonsSubPanel.add(createTitleLabel(), BorderLayout.NORTH);

        JButton deleteButton = createStyledButton("Delete", new Color(220, 53, 69));
        JButton prevButton = createStyledButton("Previous", new Color(108, 117, 125));
        JButton nextButton = createStyledButton("Next", new Color(108, 117, 125));
        JButton backButton = createStyledButton("Back to Menu", new Color(108, 117, 125));

        prevButton.setEnabled(photoEditor.getCurrentPhotoIndex() > 0);
        nextButton.setEnabled(photoEditor.getCurrentPhotoIndex() < photoEditor.getPhotoFilesSize() - 1);

        innerButtonsPanel.add(Box.createVerticalGlue());
        innerButtonsPanel.add(deleteButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(prevButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(nextButton);
        innerButtonsPanel.add(Box.createVerticalStrut(15));
        innerButtonsPanel.add(backButton);
        innerButtonsPanel.add(Box.createVerticalGlue());

        buttonsSubPanel.add(innerButtonsPanel, BorderLayout.CENTER);

        deleteButton.addActionListener(e -> {
            photoEditor.deleteCurrentPhoto();
            prevButton.setEnabled(photoEditor.getCurrentPhotoIndex() > 0);
            nextButton.setEnabled(photoEditor.getCurrentPhotoIndex() < photoEditor.getPhotoFilesSize() - 1);
        });
        prevButton.addActionListener(e -> {
            photoEditor.showPreviousPhoto();
            prevButton.setEnabled(photoEditor.getCurrentPhotoIndex() > 0);
            nextButton.setEnabled(photoEditor.getCurrentPhotoIndex() < photoEditor.getPhotoFilesSize() - 1);
        });
        nextButton.addActionListener(e -> {
            photoEditor.showNextPhoto();
            prevButton.setEnabled(photoEditor.getCurrentPhotoIndex() > 0);
            nextButton.setEnabled(photoEditor.getCurrentPhotoIndex() < photoEditor.getPhotoFilesSize() - 1);
        });
        backButton.addActionListener(e -> showInitialButtons());

        mainPanel.removeAll();
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(255, 255, 255));
        JPanel imageContainer = new JPanel(new GridBagLayout());
        imageContainer.setBackground(new Color(176, 196, 222));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(20, 0, 0, 0);
        imageContainer.add(photoCountLabel, gbc);
        gbc.insets = new Insets(10, 0, 0, 0);
        imageContainer.add(editImageLabel, gbc);
        centerPanel.add(imageContainer, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Returns the default width of the application window.
     *
     * @return the width in pixels
     */
    public int getFrameWidth() {
        return FRAME_WIDTH;
    }

    /**
     * Returns the default height of the application window.
     *
     * @return the height in pixels
     */
    public int getFrameHeight() {
        return FRAME_HEIGHT;
    }

    /**
     * Returns the main panel of the application.
     *
     * @return the main panel (JPanel)
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Returns the panel containing the application buttons.
     *
     * @return the button panel (JPanel)
     */
    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    /**
     * Returns the label displaying progress.
     *
     * @return the progress label (JLabel)
     */
    public JLabel getProgressLabel() {
        return progressLabel;
    }

    /**
     * Returns the cancel button.
     *
     * @return the cancel button (JButton)
     */
    public JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * Returns the panel displaying the camera stream.
     *
     * @return the image panel (JPanel)
     */
    public JPanel getImageLabel() {
        return imageLabel;
    }

    /**
     * Returns the label displaying image content.
     *
     * @return the image content label (JLabel)
     */
    public JLabel getImageContentLabel() {
        return imageContentLabel;
    }

    /**
     * Returns the label displaying the number of photos.
     *
     * @return the photo count label (JLabel)
     */
    public JLabel getPhotoCountLabel() {
        return photoCountLabel;
    }

    /**
     * Returns the panel displaying the edited image.
     *
     * @return the edited image panel (JPanel)
     */
    public JPanel getEditImageLabel() {
        return editImageLabel;
    }

    /**
     * Returns the label displaying the content of the edited image.
     *
     * @return the edited image content label (JLabel)
     */
    public JLabel getEditImageContentLabel() {
        return editImageContentLabel;
    }

    /**
     * Returns the application's capture manager.
     *
     * @return the CaptureManager object
     */
    public CaptureManager getCaptureManager() {
        return captureManager;
    }

    /**
     * Returns the application's photo editor.
     *
     * @return the PhotoEditor object
     */
    public PhotoEditor getPhotoEditor() {
        return photoEditor;
    }

    /**
     * The main method that launches the application on the UI thread.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WebcamCaptureApp app = new WebcamCaptureApp();
            app.setVisible(true);
        });
    }
}