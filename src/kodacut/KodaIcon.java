package kodacut;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class KodaIcon {
    private KodaIcon() {}

    public static BufferedImage createIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        configure(g);
        g.setColor(new Color(12, 12, 12));
        double r = size * 0.19;
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, r, r));
        drawMark(g, size * 0.16, size * 0.14, size * 0.68, Color.WHITE, new Color(12,12,12));
        g.dispose();
        return img;
    }

    public static void drawMark(Graphics2D g, double x, double y, double size, Color fg, Color cut) {
        configure(g);
        g.setColor(fg);

        double stemW = size * 0.22;
        double rr = stemW * 0.30;
        g.fill(new RoundRectangle2D.Double(x, y, stemW, size, rr, rr));

        Path2D upper = new Path2D.Double();
        upper.moveTo(x + stemW * 0.78, y + size * 0.48);
        upper.lineTo(x + size * 0.63, y);
        upper.lineTo(x + size, y);
        upper.lineTo(x + size * 0.52, y + size * 0.53);
        upper.closePath();
        g.fill(upper);

        Path2D lower = new Path2D.Double();
        lower.moveTo(x + stemW * 0.78, y + size * 0.50);
        lower.lineTo(x + size * 0.54, y + size * 0.44);
        lower.lineTo(x + size, y + size);
        lower.lineTo(x + size * 0.61, y + size);
        lower.closePath();
        g.fill(lower);

        g.setColor(cut);
        Path2D play = new Path2D.Double();
        play.moveTo(x + size * 0.21, y + size * 0.39);
        play.lineTo(x + size * 0.21, y + size * 0.63);
        play.lineTo(x + size * 0.42, y + size * 0.51);
        play.closePath();
        g.fill(play);

        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke((float)(size * 0.045), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int)(x + size * 0.13), (int)(y + size * 0.91),
                   (int)(x + size * 0.81), (int)(y + size * 0.31));
        g.setStroke(old);
    }

    public static void paintBrand(Graphics2D g, int width, int height, Color fg) {
        configure(g);
        int markSize = Math.min(height - 8, 58);
        drawMark(g, 4, (height - markSize) / 2.0, markSize, fg, new Color(11,11,11));

        g.setColor(fg);
        Font bold = new Font("SansSerif", Font.BOLD, Math.max(22, height / 2));
        Font plain = bold.deriveFont(Font.PLAIN);
        g.setFont(bold);
        int x = markSize + 18;
        int base = (height + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2 - 1;
        g.drawString("Koda", x, base);
        x += g.getFontMetrics().stringWidth("Koda") + 6;
        g.setFont(plain);
        g.drawString("Cut", x, base);
    }

    public static Image scaledIcon(int size) {
        return createIcon(size).getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    private static void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }
}
