package view;

import org.junit.jupiter.api.Test;
import src.view.Img;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImgTest {

    @Test
    public void testFillRectPaintsExactlyTheGivenRegion() {
        BufferedImage blank = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Img img = new Img(blank);

        img.fillRect(2, 3, 4, 5, Color.RED);

        assertEquals(Color.RED.getRGB(), blank.getRGB(2, 3));
        assertEquals(Color.RED.getRGB(), blank.getRGB(5, 7));
        assertEquals(0, blank.getRGB(0, 0));
        assertEquals(0, blank.getRGB(6, 3));
    }
}
