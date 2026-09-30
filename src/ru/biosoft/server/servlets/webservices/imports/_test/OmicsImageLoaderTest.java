package ru.biosoft.server.servlets.webservices.imports._test;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.ImageIcon;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.server.servlets.webservices.imports.OmicsImageLoader;

/**
 * Tests for OmicsImageLoader icon rendering with optional species badge.
 */
public class OmicsImageLoaderTest extends AbstractBioUMLTest
{
    private static final String TEST_ICON = "default:ru/biosoft/bsa/resources/track.gif";
    public static final String REPOSITORY_PATH = "../data";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        DataCollection repository = CollectionFactory.createRepository( REPOSITORY_PATH );
    }

    // --- getSpecies parsing ---

    public void testGetSpecies_noSpecies()
    {
        OmicsImageLoader loader = new OmicsImageLoader.T();
        assertNull(loader.getSpecies(TEST_ICON));
    }

    public void testGetSpecies_withSpecies()
    {
        OmicsImageLoader loader = new OmicsImageLoader.T();
        assertEquals("Homo sapiens",
                loader.getSpecies(TEST_ICON + "|species:Homo sapiens"));
    }

    public void testGetSpecies_emptySpecies()
    {
        OmicsImageLoader loader = new OmicsImageLoader.T();
        assertEquals("",
                loader.getSpecies(TEST_ICON + "|species:"));
    }

    // --- loadImage without species ---

    public void testLoadImage_transcriptomics_noSpecies()
    {
        OmicsImageLoader loader = new OmicsImageLoader.T();
        ImageIcon icon = loader.loadImage(TEST_ICON);
        assertNotNull("Icon should not be null", icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());

        File out = getTestFile("transcriptomics_no_species.png");
        savePNG(icon, out);
        System.out.println("Saved: " + out.getAbsolutePath());
    }

    public void testLoadImage_proteomics_noSpecies()
    {
        OmicsImageLoader loader = new OmicsImageLoader.P();
        ImageIcon icon = loader.loadImage(TEST_ICON);
        assertNotNull("Icon should not be null", icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());

        File out = getTestFile("proteomics_no_species.png");
        savePNG(icon, out);
    }

    // --- loadImage with species ---

    public void testLoadImage_withSpecies_homoSapiens()
    {
        OmicsImageLoader loader = new OmicsImageLoader.T();
        String imageId = TEST_ICON + "|species:Homo sapiens";
        ImageIcon icon = loader.loadImage(imageId);
        assertNotNull("Icon should not be null", icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());

        File out = getTestFile("transcriptomics_homo_sapiens.png");
        savePNG(icon, out);
        System.out.println("Saved: " + out.getAbsolutePath());

        // Verify species abbreviation "Hs" is drawn as dark pixels in bottom-left
        BufferedImage bi = toBufferedImage(icon);
        assertTrue("Species abbreviation should be drawn (dark pixels in bottom-left)",
                hasDarkPixel(bi, 0, 10, 6, 6));
    }

    public void testLoadImage_withSpecies_musMusculus()
    {
        OmicsImageLoader loader = new OmicsImageLoader.G();
        String imageId = TEST_ICON + "|species:Mus musculus";
        ImageIcon icon = loader.loadImage(imageId);
        assertNotNull("Icon should not be null", icon);

        File out = getTestFile("genomics_mus_musculus.png");
        savePNG(icon, out);

        BufferedImage bi = toBufferedImage(icon);
        assertTrue("Species abbreviation 'Mm' should be drawn",
                hasDarkPixel(bi, 0, 10, 6, 6));
    }

    public void testLoadImage_withUnknownSpecies()
    {
        // Unknown species should not throw — just omics badge
        OmicsImageLoader loader = new OmicsImageLoader.M();
        String imageId = TEST_ICON + "|species:Unknown creature";
        ImageIcon icon = loader.loadImage(imageId);
        assertNotNull("Icon should not be null even for unknown species", icon);
        assertEquals(16, icon.getIconWidth());
        assertEquals(16, icon.getIconHeight());

        File out = getTestFile("metabolomics_unknown_species.png");
        savePNG(icon, out);
    }

    // --- all omics types with species ---

    public void testAllOmicsTypes_withSpecies()
    {
        String imageId = TEST_ICON + "|species:Homo sapiens";
        String[] names = { "Transcriptomics", "Proteomics", "Genomics",
                "Epigenomics", "Metabolomics" };
        OmicsImageLoader[] loaders = {
                new OmicsImageLoader.T(),
                new OmicsImageLoader.P(),
                new OmicsImageLoader.G(),
                new OmicsImageLoader.E(),
                new OmicsImageLoader.M(),
        };

        for (int i = 0; i < loaders.length; i++)
        {
            ImageIcon icon = loaders[i].loadImage(imageId);
            assertNotNull("Failed for " + names[i], icon);
            File out = getTestFile(names[i].toLowerCase() + "_homo_sapiens.png");
            savePNG(icon, out);
        }
    }

    // --- helpers ---

    private void savePNG(ImageIcon icon, File file)
    {
        try
        {
            BufferedImage bi = toBufferedImage(icon);
            javax.imageio.ImageIO.write(bi, "png", file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Failed to save icon: " + e);
        }
    }

    private BufferedImage toBufferedImage(ImageIcon icon)
    {
        Image img = icon.getImage();
        if (img instanceof BufferedImage)
            return (BufferedImage) img;

        BufferedImage bi = new BufferedImage(
                icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        bi.getGraphics().drawImage(img, 0, 0, null);
        return bi;
    }

    /**
     * Check if any pixel in the given rect is dark (not white, not fully transparent).
     */
    private boolean hasDarkPixel(BufferedImage bi, int x, int y, int w, int h)
    {
        for (int py = y; py < y + h; py++)
        {
            for (int px = x; px < x + w; px++)
            {
                if (px < 0 || px >= bi.getWidth() || py < 0 || py >= bi.getHeight())
                    continue;
                int rgb = bi.getRGB(px, py);
                int alpha = (rgb >> 24) & 0xFF;
                if (alpha == 0)
                    continue; // fully transparent
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // Dark pixel: not white, not light gray
                if (r < 180 && g < 180 && b < 180)
                    return true;
            }
        }
        return false;
    }
}
