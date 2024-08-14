package biouml.standard.diagram._test;

import java.awt.Color;
import java.awt.Rectangle;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.SimplePath;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ArrowIntersectionTest extends TestCase
{
    /** Standart JUnit constructor */
    public ArrowIntersectionTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(ArrowIntersectionTest.class.getName());

        suite.addTest(new ArrowIntersectionTest("testArrowIntersection"));

        return suite;
    }

    public void testArrowIntersection() throws Exception
    {
        Rectangle rect = new Rectangle(191, 922, 10, 10);
        SimplePath path = new SimplePath(new int[]{1050, 698, 678, 678, 678}, new int[] {930, 930, 930, 950, 1018}, 5);
        ArrowView arrow = new ArrowView(new Pen(1.0f, Color.black), null, path, null, null);
        boolean intersects = arrow.intersects(rect);
        assertEquals(intersects, false);
    }
}
