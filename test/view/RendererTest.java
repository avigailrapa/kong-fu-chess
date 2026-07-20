package view;

import org.junit.jupiter.api.Test;
import src.view.Renderer;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class RendererTest {

    @Test
    public void testDrawBannerPaintsABandNearTopOfBoard() {
        Renderer renderer = new Renderer("assets/pieces");
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);

        renderer.drawBanner(image, "GAME START!");

        int pixelInsideBand = image.getRGB(renderer.boardOffsetX() + 10, renderer.boardOffsetY() + 10);
        assertNotEquals(0, pixelInsideBand);
    }

    @Test
    public void testDrawBannerDoesNotResizeTheImage() {
        Renderer renderer = new Renderer("assets/pieces");
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);

        renderer.drawBanner(image, "GAME START!");

        assertEquals(800, image.getWidth());
        assertEquals(600, image.getHeight());
    }
}
