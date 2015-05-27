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
import java.lang.reflect.InvocationTargetException;
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

import org.slf4j.Logger;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

@Named
public class Ui {

    private static final Logger logger = getLogger(Ui.class);

    private ImagePanel imageCanvas;
    private JLabel fileNameLabel;
    private JLabel statusLabel;
    private LinkedBlockingQueue<Image> images = new LinkedBlockingQueue<>();

    @Inject
    private MetricRegistry metrics;

    private Timer timer;

    @PostConstruct
    public void run() {
        timer = metrics.timer(MetricRegistry.name("inbound.kafka.images"));
        SwingUtilities.invokeLater(this::createAndShowGui);
        new Thread(() -> {
            boolean killme = false;
            while (!killme) {
                try {
                    Image image = images.take();
                    refresh(image);
                } catch (InterruptedException e) {
                    killme = true;
                }
            }
        }).start();
    }

    private void refresh(Image image) {
        try {
            SwingUtilities.invokeAndWait(() -> {
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

    private void createAndShowGui() {
        JFrame frame = new JFrame("Kafka Picture Consumer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(800, 600));
        fileNameLabel = new JLabel("Hello World");
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(fileNameLabel, BorderLayout.NORTH);
        imageCanvas = new ImagePanel();
        imageCanvas.setBackground(Color.black);
        contentPane.add(imageCanvas, BorderLayout.CENTER);
        statusLabel = new JLabel("Status");
        contentPane.add(statusLabel, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
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

        private BufferedImage image;
        private Lock lock = new ReentrantLock();

        public void updateImage(BufferedImage image) throws InvocationTargetException, InterruptedException {
            lock.tryLock(5, TimeUnit.SECONDS);
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
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
