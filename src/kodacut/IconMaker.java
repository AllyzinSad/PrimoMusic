package kodacut;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public final class IconMaker {
    private IconMaker() {}

    public static void main(String[] args) throws Exception {
        File png = new File(args.length > 0 ? args[0] : "assets/koda-cut-icon.png");
        File ico = new File(args.length > 1 ? args[1] : "assets/koda-cut.ico");
        if (png.getParentFile() != null) png.getParentFile().mkdirs();
        if (ico.getParentFile() != null) ico.getParentFile().mkdirs();

        BufferedImage image = KodaIcon.createIcon(256);
        ImageIO.write(image, "png", png);

        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", pngBytes);
        byte[] data = pngBytes.toByteArray();

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ico)))) {
            writeLEShort(out, 0);
            writeLEShort(out, 1);
            writeLEShort(out, 1);

            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0);
            writeLEShort(out, 1);
            writeLEShort(out, 32);
            writeLEInt(out, data.length);
            writeLEInt(out, 22);
            out.write(data);
        }

        System.out.println("Icones gerados:");
        System.out.println(png.getAbsolutePath());
        System.out.println(ico.getAbsolutePath());
    }

    private static void writeLEShort(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xff);
        out.writeByte((value >>> 8) & 0xff);
    }

    private static void writeLEInt(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xff);
        out.writeByte((value >>> 8) & 0xff);
        out.writeByte((value >>> 16) & 0xff);
        out.writeByte((value >>> 24) & 0xff);
    }
}
