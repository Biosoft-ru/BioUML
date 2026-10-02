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

        // Verify species overlay is used: compare with non-species version
        ImageIcon iconNoSpecies = loader.loadImage(TEST_ICON);
        assertNotNull("Non-species icon should not be null", iconNoSpecies);
        BufferedImage biWith = toBufferedImage(icon);
        BufferedImage biWithout = toBufferedImage(iconNoSpecies);
        assertTrue("Species overlay should change the icon image",
                imagesDiffer(biWith, biWithout));
    }

    public void testLoadImage_withSpecies_musMusculus()
    {
        OmicsImageLoader loader = new OmicsImageLoader.G();
        String imageId = TEST_ICON + "|species:Mus musculus";
        ImageIcon icon = loader.loadImage(imageId);
        assertNotNull("Icon should not be null", icon);

        File out = getTestFile("genomics_mus_musculus.png");
        savePNG(icon, out);

        // Verify species overlay is used
        ImageIcon iconNoSpecies = loader.loadImage(TEST_ICON);
        BufferedImage biWith = toBufferedImage(icon);
        BufferedImage biWithout = toBufferedImage(iconNoSpecies);
        assertTrue("Species overlay should change the icon image",
                imagesDiffer(biWith, biWithout));
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
     * Check if two images differ in any pixel.
     */
    private boolean imagesDiffer(BufferedImage a, BufferedImage b)
    {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight())
            return true;
        for (int y = 0; y < a.getHeight(); y++)
        {
            for (int x = 0; x < a.getWidth(); x++)
            {
                if (a.getRGB(x, y) != b.getRGB(x, y))
                    return true;
            }
        }
        return false;
    }
}
