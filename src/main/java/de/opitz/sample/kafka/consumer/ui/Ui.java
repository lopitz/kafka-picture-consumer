package de.opitz.sample.kafka.consumer.ui;

import java.awt.*;
import java.io.*;
import java.util.concurrent.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class Ui {

    private static final Logger logger = getLogger(Ui.class);

    private final LinkedBlockingQueue<Image> images = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String groupId;
    private final int left;
    private final int top;

    private ImagePanel imageCanvas;
    private JLabel fileNameLabel;
    private JLabel statusLabel;

    public Ui(@Value("${kafka.group.id}") String groupId, @Value("${ui.position.left:0}") int left, @Value("${ui.position.top:0}") int top) {
        this.groupId = groupId;
        this.left = left;
        this.top = top;
    }

    @PostConstruct
    public void run() {
        SwingUtilities.invokeLater(this::createAndShowGui);
        executorService.submit(this::startUpdatingImages);
    }

    private void createAndShowGui() {
        var frame = createMainWindow();
        var contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        fileNameLabel = createAndInsertLabel(contentPane, "Waiting for images...", BorderLayout.NORTH);
        imageCanvas = createAndAddImageCanvas(contentPane);
        statusLabel = createAndInsertLabel(contentPane, "Status", BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }

    private JFrame createMainWindow() {
        var frame = new JFrame(String.format("Kafka Picture Consumer - consumer group #%s", groupId));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.setLocation(left, top);
        return frame;
    }

    private JLabel createAndInsertLabel(Container contentPane, String text, String position) {
        var result = new JLabel(text);
        contentPane.add(result, position);
        return result;
    }

    private ImagePanel createAndAddImageCanvas(Container contentPane) {
        var result = new ImagePanel();
        result.setBackground(Color.black);
        contentPane.add(result, BorderLayout.CENTER);
        result.setDoubleBuffered(true);
        return result;
    }

    private void startUpdatingImages() {
        var stopRequested = false;
        while (!stopRequested) {
            try {
                var image = images.take();
                refresh(image);
            } catch (InterruptedException e) {
                logger.warn("update of images was interrupted");
                stopRequested = true;
            }
        }
    }

    private void refresh(Image image) {
        try {
            SwingUtilities.invokeAndWait(() -> refreshImage(image));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(e.getLocalizedMessage()));
        }
    }

    private void refreshImage(Image image) {
        fileNameLabel.setText(image.name());
        try {
            imageCanvas.updateImage(image.bufferedImage());
            statusLabel.setText(String.format("Buffered images: %d", images.size()));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(e.getLocalizedMessage()));
        }
    }

    public void updateImage(String imageName, byte[] rawData) {
        try (InputStream imageInput = new ByteArrayInputStream(rawData)) {
            images.offer(new Image(imageName, ImageIO.read(imageInput)));
            SwingUtilities.invokeLater(() -> statusLabel.setText(String.format("Buffered images: %d", images.size())));
        } catch (Exception e) {
            logger.info("Error reading image {}, reason {}", imageName, e.getLocalizedMessage());
        }
    }

}
