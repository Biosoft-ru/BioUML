package ru.biosoft.analysis._test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedWriter;
import java.util.Random;

import javax.swing.JFrame;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.graph.Node;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.editor.ViewPane;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FastGridLayoutTest extends TestCase
{
    public FastGridLayoutTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(FastGridLayoutTest.class.getName());
        suite.addTest(new FastGridLayoutTest("test"));
        return suite;
    }
    private static final int h = 15;

    private static final int w = 15;

    private static final int gridX = 70;

    private static final int gridY = 70;

    private final int[][] boundMatrix = new int[w + 1][h + 1];

    private final int[][] shadowMatrix = new int[w][h];
    
    private Point p1;
    private Point p2;
    boolean isUpOriented = false;
    boolean isDownOriented = false;
    boolean wideAngle = false;
    boolean edgeIsLower = false;

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {

        // Initial data
        Random r = new Random();

        Node n = new Node("n", r.nextInt(w), r.nextInt(h), 60, 60);
        Node n1 = new Node("n1", r.nextInt(w), r.nextInt(h), 44, 40);
        Node n2 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n3 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n4 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n5 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n6 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n7 = new Node("n2", r.nextInt(w), r.nextInt(h), 15, 15);
        Node n8 = new Node("n2", r.nextInt(w), r.nextInt(h), 20, 20);


        //        n.y = 9;
        //        n.x = 5;
        //        //
        //        n1.x = 0;
        //        n1.y = 0;
        //        ////
        //        n2.x = 0;
        //        n2.y = 0;

        //        n3.x = 14;
        //        n4.x = 14;
        //        n.x = 7;
        //
        //        n1.x = n.x;
        //        n2.x = n.x;

        //        Edge e1 = new Edge(n1, n2);
        //                Edge e2 = new Edge(n3, n4);
        //        Edge e3 = new Edge(n5, n6);
        //        Edge e4 = new Edge(n7, n8);
        //Processing
        //        n/.x/ = 10;
        //        n.y = 0;
        //
                n1.x = 5;
                n1.y = 5;
        //
                n2.x = 4;
                n2.y = 4;

        //        setNodeEdge(n, e1, 1);
        //                        setNodeEdge(n, e2, 1);
        //                setNodeEdge(n, e3, 1);
        //                setNodeEdge(n, e4, 1);
        //        setNodeEdge(n, e1, 1);
        //        setNodeNode(n1, n2, 1);
        //        setNodeNode(n1, n2, 1);
        //        setNodeNode(n2, n1, 1);

        //        setDistance(n1, 2);
        //        setEdgeNode(n, n1, 1);
        setEdgeNode(getCenter(n1), n2, 1);
        setEdgeNode(getCenter(n1), n3, 1);
        setEdgeNode(getCenter(n1), n4, 1);
        //        setEdgeNode(n, n3, 1);
        //        setEdgeNode(n, n4, 1);
        setEdgeNode(getCenter(n1), n5, 1);
        setEdgeNode(getCenter(n1), n6, 1);
        setEdgeNode(getCenter(n1), n7, 1);
        Point p = getCenter(n1);
        //        setEdgeNode(n, n8, 1);

        //        setEdgeEdge(n, e1, 1);
        //        setEdgeEdge(n, e2, 1);
        //        setEdgeEdge(n, e3, 1);
        //        setEdgeEdge(n, e4, 1);
        setShadowArea();

        // Painting
        JFrame frame = new JFrame();
        Brush brush;

        CompositeView view = new CompositeView();

        for( int i = 0; i < w; i++ )
        {
            for( int j = 0; j < h; j++ )
            {
                if( shadowMatrix[i][j] == 0 )
                {
                    brush = new Brush(Color.white);
                }
                else if( shadowMatrix[i][j] == 1 )
                {
                    brush = new Brush(Color.lightGray);
                }
                else if( shadowMatrix[i][j] == 2 )
                {
                    brush = new Brush(Color.gray);
                }
                else if( shadowMatrix[i][j] == 3 )
                {
                    brush = new Brush(Color.darkGray);
                }
                else
                {
                    brush = new Brush(Color.black);
                }
                view.add(new BoxView(new Pen(1, Color.gray), brush, i * gridX + gridX / 2, j * gridY + gridY / 2, gridX, gridY));
                //Node newNode = new Node("n_" + i + "_" + j, i, j, 20, 30);
                //                addNode(view, newNode, new Brush(Color.lightGray));
            }
        }

        //        view.add(new LineView(new Pen(1, Color.black), p1Up.x, p1Up.y, p2Up.x, p2Up.y));
        //        view.add(new LineView(new Pen(1, Color.black), p1Down.x, p1Down.y, p2Down.x, p2Down.y));

        view.add(new LineView(new Pen(1, Color.black), p.x, p.y, p2.x, p2.y));
        view.add(new LineView(new Pen(1, Color.black), p.x, p.y, p1.x, p1.y));

        //brush = new Brush(Color.magenta);
        //                addNode(view, n, brush);
        //brush = new Brush(Color.blue);
        addNode(view, n1);
        addNode(view, n2);
        addNode(view, n3);
        addNode(view, n4);
        addNode(view, n5);
        addNode(view, n6);
        addNode(view, n7);
        //        addNode(view, n8, brush);
        //        addEdge(view, e1);
        //                addEdge(view, e2);
        //                addEdge(view, e3);
        //                addEdge(view, e4);
        ViewPane pane = new ViewPane();
        pane.setView(view);

        Dimension dim = new Dimension(w * gridX + 100, h * gridY + 100);
        outDeltaMap();
        frame.add(pane, BorderLayout.CENTER);
        frame.doLayout();
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
        frame.setMinimumSize(dim);
        frame.setMaximumSize(dim);
        frame.setSize(dim);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setEnabled(true);
        frame.setFocusable(true);


    }
    private void setShadowArea()
    {
        for( int i = 0; i < w; i++ )
        {
            int sum = 0;
            for( int j = 0; j < h; j++ )
            {
                sum += boundMatrix[i][j];
                shadowMatrix[i][j] += sum;
            }
        }
    }

    private Point getCenter(Node n)
    {
        int x = ( n.x + 1 ) * gridX;
        int y = ( n.y + 1 ) * gridY;
        return new Point(x, y);
    }

    private void setEdgeNode(Point p, Node targetNode, int weight)
    {
        // center of targetNode
        int x = ( targetNode.x + 1 ) * gridX;
        int y = ( targetNode.y + 1 ) * gridY;
        if( p.x == x && p.y == y )
            return;

        //        Point p2;
        //        Point p1;

        int height = targetNode.height / 2;
        int width = targetNode.width / 2;

        if( p.x == x )
        {
            height *= (int)Math.signum(p.y - y);
            p1 = new Point(x - width, y + height);
            p2 = new Point(x + width, y + height);
        }
        else if( p.y == y )
        {
            width *= (int)Math.signum(p.x - x);
            p1 = new Point(x + width, y - height);
            p2 = new Point(x + width, y + height);
        }
        else if( ( p.x - x ) * ( p.y - y ) > 0 )
        {
            p1 = new Point(x - width, y + height);
            p2 = new Point(x + width, y - height);
        }
        else
        {
            p1 = new Point(x - width, y - height);
            p2 = new Point(x + width, y + height);
        }
        setCrossingZoneBoundary(p, p1, p2, weight);
    }

    //  Create Boundary for shadow zone sorrunded by edge e and lines connecting
    // node n with edge e nodes
    public void setCrossingZoneBoundary(int width, int height, Point p1, Point p2, int weight)
    {

        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;

        width /= 2;
        height /= 2;

        int iLeft = (int)Math.ceil( ( (double)Math.min(x1, x2) - width ) / gridX - 1);
        int iRight = (int)Math.floor( ( (double)Math.max(x1, x2) + width ) / gridX - 1);

        iLeft = Math.max(iLeft, 0);
        iRight = Math.min(iRight, w - 1);

        Point p1Up;
        Point p2Up;
        Point p1Down;
        Point p2Down;

        if( x1 == x2 || y1 == y2 )
        {
            int jUp = (int)Math.ceil( ( (double)Math.min(y1, y2) - height ) / gridY - 1);
            int jDown = (int)Math.floor( ( (double)Math.max(y1, y2) + height ) / gridY - 1);

            for( int i = iLeft; i <= iRight; i++ )
            {
                boundMatrix[i][jUp] += weight;
                boundMatrix[i][jDown + 1] -= weight;
            }
            return;
        }

        else if( ( x1 - x2 ) * ( y1 - y2 ) < 0 )
        {
            p1Up = new Point(x1 - width, y1 - height);
            p2Up = new Point(x2 - width, y2 - height);
            p1Down = new Point(x1 + width, y1 + height);
            p2Down = new Point(x2 + width, y2 + height);
        }
        else
        {
            p1Up = new Point(x1 + width, y1 - height);
            p2Up = new Point(x2 + width, y2 - height);
            p1Down = new Point(x1 - width, y1 + height);
            p2Down = new Point(x2 - width, y2 + height);
        }

        for( int i = iLeft; i <= iRight; i++ )
        {
            int x = ( i + 1 ) * gridX;
            double yUp = getYByX2(p1Up, p2Up, x);
            double yDown = getYByX2(p1Down, p2Down, x);

            int jUp = (int)Math.ceil(yUp / gridY - 1);
            int jDown = (int)Math.ceil(yDown / gridY - 1);

            if( yUp == -1 )
            {
                jUp = (int)Math.ceil(Math.min(y1, y2) / ((double)gridY) - 1);
            }

            if( yDown == -1 )
            {
                jDown = (int)Math.ceil(Math.max(y1, y2) / ((double)gridY));
            }

            if( jUp == (int) ( yUp / gridY - 1 ) )
                jUp++;

            jUp = Math.max(jUp, 0);
            jDown = Math.min(jDown, h);

            boundMatrix[i][jUp] += weight;
            boundMatrix[i][jDown] -= weight;
        }
    }
    // Utility functions
    private double getYByX2(Point p1, Point p2, int x)
    {
        if( p2.x == p1.x )
            return Math.min(p1.y, p2.y);
        double t = ( x - p1.x );
        t /= ( p2.x - p1.x );

        if( t < 0 || t > 1 )
            return -1;

        return p1.y + t * ( p2.y - p1.y );
    }
    //  Create Boundary for shadow zone sorrunded by edge e and lines connecting
    // node n with edge e nodes
    public void setCrossingZoneBoundary(Point p, Point p1, Point p2, Integer weight)
    {
        if( p.equals(p1) || p.equals(p2) )
            return;

        boolean edgeIsLower = getYByX(p1, p2, p.x, true) >= p.y / gridY - 1;
        boolean wideAngle = ( p1.x - p.x ) * ( p2.x - p.x ) <= 0;
        boolean isUpOriented = wideAngle && !edgeIsLower;
        boolean isDownOriented = wideAngle && edgeIsLower;

        for( int i = 0; i < w; i++ )
        {
            int x = ( i + 1 ) * gridX;
            int j1 = getYByX(p, p1, x, false);
            int j2 = getYByX(p, p2, x, false);
            int j3 = getYByX(p1, p2, x, true);

            int count = 0;
            if( j1 >= 0 )
                count++;

            if( j2 >= 0 )
                count++;

            if( j3 >= 0 )
                count++;

            if( count == 0 )
                continue;

            int jMax = Math.max(j1, Math.max(j2, j3));

            if( isDownOriented )
            {
                if( count == 3 )
                {
                    jMax = Math.min(Math.min(j1, j2), j3);
                }
                boundMatrix[i][jMax] += weight;
                continue;
            }

            if( isUpOriented && jMax != 0 )
            {
                boundMatrix[i][0] += weight;
                boundMatrix[i][jMax] -= weight;
                continue;
            }

            if( jMax == 0 )
            {
                continue;
            }

            switch( count )
            {
                case 1:
                {
                    boundMatrix[i][jMax] += weight;
                    break;
                }
                case 2:
                {
                    int jMed = j1 + j2 + j3 - jMax + 1;
                    if( jMed == h )
                    {
                        break;
                    }
                    boundMatrix[i][jMax] -= weight;
                    boundMatrix[i][jMed] += weight;
                    break;
                }
                case 3:
                {
                    int jLow = Math.min(Math.min(j1, j2), j3);
                    boundMatrix[i][jMax] -= weight;
                    boundMatrix[i][jLow] += weight;
                    break;
                }
            }
        }
    }
    //  Utility functions
    private int getYByX(Point p1, Point p2, int x, boolean inner)
    {
        if( p1.x == p2.x && p1.x != x )
        {
            return -1;
        }
        if( x == p2.x )
        {
            return p2.y / gridY - 1;
        }

        double t = ( x - p1.x );
        t /= ( p2.x - p1.x );

        if( ( inner != ( t < 1 ) ) || ( t < 0 ) )
            return -1;
        //        if( ( inner && t > 1 ) || ( !inner && t < 1 ) || t < 0 )
        //            return -1;

        double y = p1.y + t * ( p2.y - p1.y );

        y = Math.min(Math.max(y, gridY), ( h + 1 ) * gridY);

        return (int)Math.ceil(y / gridY - 1);

    }
    private void addNode(CompositeView view, Node node)
    {
        int x = (int) ( ( node.x + 1 ) * gridX - node.width / 2d );
        int y = (int) ( ( node.y + 1 ) * gridY - node.height / 2d );
        view.add(new BoxView(new Pen(1, Color.black), null, x, y, node.width, node.height));
    }

    private void outDeltaMap() throws Exception
    {
        try(BufferedWriter bw = ApplicationUtils.utfWriter( AbstractBioUMLTest.getTestFile( "fastgridlayout-delta.txt" ) ))
        {
            StringBuffer buf = new StringBuffer();

            buf.append("Bound:\n");

            for( int j = 0; j < boundMatrix[0].length; j++ )
            {
                for( int[] element : boundMatrix )
                {
                    String s = element[j] + "\t";
                    buf.append(s);
                }
                buf.append("\n");
            }
            buf.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
            buf.append("Shaow:\n");

            for( int j = 0; j < shadowMatrix[0].length; j++ )
            {
                for( int[] element : shadowMatrix )
                {
                    String s = element[j] + "\t";
                    buf.append(s);
                }
                buf.append("\n");
            }
            buf.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
            String s = buf.toString();
            bw.write(s);
        }
    }
}
