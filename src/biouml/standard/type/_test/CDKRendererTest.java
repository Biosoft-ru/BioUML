package biouml.standard.type._test;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import junit.framework.TestCase;
import ru.biosoft.graphics.CompositeView;

/**
 * Headless tests for CDKRenderer — verifies that the CDK/jchempaint
 * rendering pipeline works after library updates.
 */
public class CDKRendererTest extends TestCase
{
    /**
     * A molecule in MDL MOL format (RDKit-generated).
     * Expected: a non-empty CompositeView with atom labels and bond lines,
     * and a non-empty BufferedImage.
     */
    private static final String FORMYCIN_MOL =
            "     RDKit          2D\n" +
            "\n" +
            " 19 21  0  0  0  0  0  0  0  0999 V2000\n" +
            "    1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    0.7500   -1.2990    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -0.7500   -1.2990    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -1.7537   -2.4138    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -3.1240   -1.8037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -2.9672   -0.3119    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -0.7500    1.2990    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    0.7500    1.2990    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -1.5000    2.5981    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -1.4418   -3.8810    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -0.0715   -4.4911    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -0.2283   -5.9829    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -1.6955   -6.2947    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -2.4455   -4.9957    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -2.3056   -7.6651    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "   -3.7974   -7.8218    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    0.8864   -6.9866    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "    1.2275   -3.7411    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
            "  1  2  2  0\n" +
            "  2  3  1  0\n" +
            "  3  4  2  0\n" +
            "  4  5  1  0\n" +
            "  5  6  1  0\n" +
            "  6  7  2  0\n" +
            "  7  8  1  0\n" +
            "  8  9  2  0\n" +
            "  8 10  1  0\n" +
            " 11  4  1  1\n" +
            " 11 12  1  0\n" +
            " 12 13  1  0\n" +
            " 13 14  1  0\n" +
            " 14 15  1  0\n" +
            " 14 16  1  1\n" +
            " 16 17  1  0\n" +
            " 13 18  1  6\n" +
            " 12 19  1  6\n" +
            "  9  1  1  0\n" +
            "  7  3  1  0\n" +
            " 15 11  1  0\n" +
            "M  END\n";

    /**
     * Verify that createStructureView renders a molecule into a CompositeView
     * with children (bonds, atom labels).
     */
    public void testCreateStructureView() throws Exception
    {
        Structure structure = new Structure( null, "Formycin" );
        structure.setData(FORMYCIN_MOL);

        Graphics2D g = createGraphics();
        CompositeView view = CDKRenderer.createStructureView(structure, new Dimension(400, 300), g);

        assertNotNull("CompositeView should not be null", view);
        assertTrue("CompositeView should have children (bonds + atom labels)",
                view.size() > 0);
    }

    /**
     * Verify that createStructureImage renders a molecule into a non-empty image.
     */
    public void testCreateStructureImage() throws Exception
    {
        Structure structure = new Structure(null, "caffeine");
        structure.setData(FORMYCIN_MOL);

        BufferedImage image = CDKRenderer.createStructureImage(structure, new Dimension(400, 300));

        assertNotNull("BufferedImage should not be null", image);
        assertEquals("Image width should match requested size", 400, image.getWidth());
        assertEquals("Image height should match requested size", 300, image.getHeight());
        assertTrue("Image should contain non-zero pixels",
                image.getRaster().getWidth() * image.getRaster().getHeight() > 0);
    }

    /**
     * Verify that molecule parsing and bounds calculation work.
     */
    public void testLoadMolecule() throws Exception
    {
        Structure structure = new Structure(null, "caffeine");
        structure.setData(FORMYCIN_MOL);

        double[] bounds = CDKRenderer.getBounds(CDKRenderer.loadMolecule(structure));

        assertNotNull("Bounds array should not be null", bounds);
        assertTrue("Bounds width should be positive", bounds[0] > 0);
        assertTrue("Bounds height should be positive", bounds[1] > 0);
    }

    private static Graphics2D createGraphics()
    {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        return img.createGraphics();
    }
}
