package src.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Img {

    private BufferedImage img;

    public Img() {
    }

    public Img(BufferedImage img) {
        this.img = img;
    }

    public Img read(String path,
                    Dimension targetSize,
                    boolean keepAspect,
                    Object interpolation ) {

    System.out.println("Searching for image at: " + new File(path).getAbsolutePath());
    try {
        img = ImageIO.read(new File(path));
    } catch (IOException e) {
        throw new IllegalArgumentException("Unsupported image: " + path, e);
    }

    if (img == null) throw new IllegalArgumentException("Unsupported image: " + path);

    if (targetSize != null) {
        img = resized(img, targetSize, keepAspect);
    }
    return this;
    }

    public Img read(String path) { return read(path, null, false, null); }

    public Img resize(Dimension targetSize, boolean keepAspect) {
        if (img == null) throw new IllegalStateException("Image not loaded.");
        img = resized(img, targetSize, keepAspect);
        return this;
    }

    private static BufferedImage resized(BufferedImage source, Dimension targetSize, boolean keepAspect) {
        int tw = targetSize.width, th = targetSize.height;
        int w = source.getWidth(), h = source.getHeight();

        int nw, nh;
        if (keepAspect) {
            double s = Math.min(tw / (double) w, th / (double) h);
            nw = (int) Math.round(w * s);
            nh = (int) Math.round(h * s);
        } else { nw = tw; nh = th; }

        BufferedImage dst = new BufferedImage(
                nw, nh,
                source.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB);

        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    public void drawOn(Img other, int x, int y) {
        if (img == null || other.img == null)
            throw new IllegalStateException("Both images must be loaded.");

        if (x + img.getWidth()  > other.img.getWidth()
         || y + img.getHeight() > other.img.getHeight())
            throw new IllegalArgumentException("Patch exceeds destination bounds.");

        Graphics2D g = other.img.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(img, x, y, null);
        g.dispose();
    }

    public void fillRect(int x, int y, int width, int height, Color color) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(x, y, width, height);
        g.dispose();
    }

    public void drawRect(int x, int y, int width, int height, Color color, int thickness) {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness));
        g.drawRect(x, y, width, height);
        g.dispose();
    }

    public void putText(String txt, int x, int y, float fontSize,
                        Color color, int thickness /*unused in Java2D*/) {

        if (img == null) throw new IllegalStateException("Image not loaded.");

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(color);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(fontSize * 12)));
        g.drawString(txt, x, y);
        g.dispose();
    }

    public void show() {
        if (img == null) throw new IllegalStateException("Image not loaded.");

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Image");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(new JLabel(new ImageIcon(img)));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    public BufferedImage get() { return img; }
}
