package ui;

import static org.slf4j.LoggerFactory.getLogger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

@Named
public class Ui {

    private static final Logger logger = getLogger(Ui.class);

    private final LinkedBlockingQueue<Image> images = new LinkedBlockingQueue<>();

    private ImagePanel imageCanvas;
    private JLabel fileNameLabel;
    private JLabel statusLabel;

    @Value("${kafka.group.id}")
    private String groupId;

    @Inject
    private MetricRegistry metrics;

    private Timer timer;

    @PostConstruct
    public void run() {
        timer = metrics.timer(MetricRegistry.name("inbound.kafka.images"));
        SwingUtilities.invokeLater(this::createAndShowGui);
        startUpdatingImages();
    }

    private void createAndShowGui() {
        JFrame frame = createMainWindow();
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        fileNameLabel = createAndInsertLabel(contentPane, "Waiting for images...", BorderLayout.NORTH);
        imageCanvas = createAndAddImageCanvas(contentPane);
        statusLabel = createAndInsertLabel(contentPane, "Status", BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }

    private JFrame createMainWindow() {
        JFrame frame = new JFrame(String.format("Kafka Picture Consumer - consumer group #%s", groupId));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(800, 600));
        return frame;
    }

    private JLabel createAndInsertLabel(Container contentPane, String text, String position) {
        JLabel result  = new JLabel(text);
        contentPane.add(result, position);
        return result;
    }

    private ImagePanel createAndAddImageCanvas(Container contentPane) {
        ImagePanel result = new ImagePanel();
        result.setBackground(Color.black);
        contentPane.add(result, BorderLayout.CENTER);
        result.setDoubleBuffered(true);
        return result;
    }

    private void startUpdatingImages() {
        new Thread(() -> {
            boolean killme = false;
            while (!killme) {
                try {
                    Image image = images.take();
                    refresh(image);
                } catch (InterruptedException e) {
                    logger.warn("update of images was interrupted");
                    killme = true;
                }
            }
        }).start();
    }

    private void refresh(Image image) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (image.name.contains("1520")) {
                    logger.info("update with images {}", image.name);
                }
                fileNameLabel.setText(image.name);
                try {
                    imageCanvas.updateImage(image.bufferedImage);
                    statusLabel.setText(String.format("Buffered images: %d", images.size()));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText(e.getLocalizedMessage()));
                }
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(e.getLocalizedMessage()));
        }
    }

    public void updateImage(String imageName, byte[] rawData) {
        Timer.Context context = timer.time();
        try (InputStream imageInput = new ByteArrayInputStream(rawData)) {
            images.offer(new Image(imageName, ImageIO.read(imageInput)));
            SwingUtilities.invokeLater(() -> statusLabel.setText(String.format("Buffered images: %d", images.size())));
        } catch (Exception e) {
            logger.info("Error reading image {}, reason {}", imageName, e.getLocalizedMessage());
        } finally {
            context.stop();
        }
    }

    private static class ImagePanel extends JPanel implements ImageObserver {

        private final Lock lock = new ReentrantLock();

        private BufferedImage image;

        public void updateImage(BufferedImage image) throws InterruptedException {
            lock.tryLock(5, TimeUnit.SECONDS);
            this.image = image;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (image != null) {
                g.drawImage(image, (getWidth() - image.getWidth()) / 2, (getHeight() - image.getHeight()) / 2, this);
            }
        }

        @Override
        public boolean imageUpdate(java.awt.Image img, int infoflags, int x, int y, int w, int h) {
            boolean result = super.imageUpdate(img, infoflags, x, y, w, h);
            if (result) {
                lock.unlock();
            }
            return result;
        }
    }

    private static class Image {
        private final BufferedImage bufferedImage;
        private final String name;

        public Image(String name, BufferedImage bufferedImage) {
            this.bufferedImage = bufferedImage;
            this.name = name;
        }
    }
}
