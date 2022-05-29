package de.opitz.sample.kafka.consumer.ui;

import java.awt.Graphics;
import java.awt.image.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

import javax.swing.JPanel;

class ImagePanel extends JPanel implements ImageObserver {

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
    public boolean imageUpdate(java.awt.Image img, int infoFlags, int x, int y, int w, int h) {
        var result = super.imageUpdate(img, infoFlags, x, y, w, h);
        if (result) {
            lock.unlock();
        }
        return result;
    }
}
