package biouml.workbench._test;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MathTest extends TestCase
{
    public static final int WIDTH = 400;
    public static final int HEIGHT = 400;

    public MathTest(String name)
    {
        super(name);
    }

    /** Make suite of tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite("MathTest");

        suite.addTest(new MathTest("testShow"));

        return suite;
    }

    public static void testShow()
    {
        JPanel panel = new DPanel();
        JFrame frame = new JFrame();
        frame.setSize(WIDTH, HEIGHT);
        frame.setContentPane(panel);
        frame.show();

    }

    static class DPanel extends JPanel
    {
        @Override
        public void paint(Graphics g)
        {
            super.paint(g);

            g.drawLine(0, MathTest.HEIGHT / 2, MathTest.WIDTH, MathTest.HEIGHT / 2);
            g.drawLine(MathTest.WIDTH / 2, 0, MathTest.WIDTH / 2, MathTest.HEIGHT);
            int nodesNum = 3;
            //sinVlaue * DY
            double degreeToNode = 180.0 / nodesNum;

            double r = MathTest.WIDTH / 2;
            int x = 0;
            int y = 0;
            double startDeg = degreeToNode / 2;
            for( int i = 0; i < nodesNum; i++ )
            {
                g.setColor(Color.blue);
                double value = Math.toRadians(startDeg + i * degreeToNode);
                double sinValue = Math.sin(value);
                double cosValue = Math.cos(value);
                x = (int) ( ( 1 - cosValue ) * r );
                y = (int) ( ( 1 - sinValue ) * r );
                g.fillRect(x, y, 40, 20);
                g.setColor(Color.yellow);
                g.drawString("" + i, x, y + 10);
            }

            for( int i = 0; i < nodesNum; i++ )
            {
                g.setColor(Color.blue);
                double value = -Math.toRadians(startDeg + i * degreeToNode);
                double sinValue = Math.sin(value);
                double cosValue = Math.cos(value);
                x = (int) ( ( 1 - cosValue ) * r );
                y = (int) ( ( 1 - sinValue ) * r );
                g.fillRect(x, y, 40, 20);
                g.setColor(Color.yellow);
                g.drawString("" + i, x, y + 10);
            }

        }
    }

}
