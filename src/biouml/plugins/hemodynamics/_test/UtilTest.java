package biouml.plugins.hemodynamics._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import com.developmentontheedge.application.ApplicationUtils;

import Jama.Matrix;
import biouml.plugins.hemodynamics.HemodynamicsDiagramGenerator;
import biouml.plugins.hemodynamics.QRDecomposition;
import biouml.plugins.hemodynamics.Util;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class UtilTest extends TestCase
{
    public UtilTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(UtilTest.class.getName());
        //        suite.addTest(new UtilTest("test"));
        //        suite.addTest(new UtilTest("generateInputPressure"));
        suite.addTest(new UtilTest("generateDiagram"));
        return suite;
    }

    public void generateDiagram() throws Exception
    {
        String modelFileName = "..//data//test//biouml//plugins//hemodynamics//mynard.txt";//_correctAreasP40.txt";

        String repositoryPath = "../data";
        CollectionFactory.createRepository(repositoryPath);
        DataCollection collection = CollectionFactory.getDataCollection("databases/arterialTree/Diagrams");
        //        DataCollection collection = CollectionFactory.getDataCollection("databases/Virtual Human/Diagrams");
        //        DataCollection collection = CollectionFactory.getDataCollection("databases/Cardiovascular student/Diagrams");
        HemodynamicsDiagramGenerator generator;// = new HemodynamicsDiagramGenerator("3 vessels");
        //        generator.generatePorts = false;
        //        generator.createDiagram( collection, new File(modelFileName) );
        //        
        //        generator = new HemodynamicsDiagramGenerator("3 vessels with outlets");
        //        generator.addBranches = true;
        //        generator.generatePorts = false;
        //        generator.createDiagram( collection, new File(modelFileName) );
        //        
        //        generator = new HemodynamicsDiagramGenerator("3 vessels outlets const area");
        //        generator.addBranches = true;
        //        generator.generatePorts = false;
        //        generator.extentAreaFactor = 1;
        //        generator.createDiagram( collection, new File(modelFileName) );

        //        generator = new HemodynamicsDiagramGenerator("3 vessels outlets half area");
        //        generator.addBranches = true;
        //        generator.generatePorts = false;
        //        generator.extentAreaFactor = 0.5;
        //        generator.outletAreaFactor = 0.5;
        //        generator.createDiagram( collection, new File(modelFileName) );

        generator = new HemodynamicsDiagramGenerator("Arterial Tree Mynard");
        //      generator.addBranches = true;
        //      generator.generatePorts = false;
        generator.createDiagram(collection, new File(modelFileName));
    }

    public void generateInputPressure() throws Exception
    {
        double timeStart = 0;
        //        double timeFinish = 100;
        double increment = 0.001;

        File f = new File("../data/test/biouml/plugins/hemodynamics/input_profile.txt");

        try (BufferedWriter bw = ApplicationUtils.utfWriter(f))
        {
            int length = 10000;

            bw.write("time\tpressure\n");
            for( int i = 0; i < length; i++ )
            {
                double time = increment * i;
                double pressure = Util.generateInputPressure(time);

                bw.write(String.valueOf(time));
                bw.write("\t");
                bw.write(String.valueOf(pressure));
                bw.write("\n");
            }
        }
    }


    public void test() throws Exception
    {
        int dimension = 55;
        double[][] matrix = generateUpperTriangleMatrix(dimension);
        Matrix jamaMatrix = new Matrix(matrix);

        double time = System.currentTimeMillis();
        for( int i = 0; i < 10000; i++ )
        {

            double[][] invert = Util.invertUpper2(matrix);
            //            Matrix unity = new Matrix(Util.multiply(matrix, invert));

        }
        System.out.print("simple done:" + ( System.currentTimeMillis() - time ));
        //
        time = System.currentTimeMillis();
        for( int i = 0; i < 10000; i++ )
        {
            Matrix jamaInvert = jamaMatrix.inverse();
        }
        System.out.print("jama done:" + ( System.currentTimeMillis() - time ));
        //        Matrix jamaUnity = jamaMatrix.times(jamaInvert);
        //
        //
        //        Matrix identity = Matrix.identity(dimension, dimension);
        //
        //        Matrix zero = unity.minus(identity);
        //        Matrix jamaZero = jamaUnity.minus(identity);


        //        }
        //        System.out.print("jama done:" + ( System.currentTimeMillis() - time ));
        //
        //        double[][] unity = Util.multiply(matrix, Util.invertUpper(matrix));
    }


    private double[][] generateUpperTriangleMatrix(int n)
    {
        double[][] matrix = new double[n][n];
        Random r = new Random();
        for( int i = 0; i < n; i++ )
        {
            for( int j = 0; j < n; j++ )
            {
                matrix[i][j] = ( j < i ) ? 0 : r.nextInt(100) + 1;
            }
        }
        return matrix;
    }

    private double[][] generateTestMatrix()
    {
        double[][] matrix = new double[3][3];
        matrix[0][0] = 5;
        matrix[0][1] = 2;
        matrix[0][2] = 8;
        matrix[1][0] = 0;
        matrix[1][1] = 1;
        matrix[1][2] = 2;
        matrix[2][0] = 0;
        matrix[2][1] = 0;
        matrix[2][2] = 1;
        return matrix;
    }

    public static void main(String ... args) throws Exception
    {
        testQRDesomposition();
    }

    private static void testQRDesomposition() throws Exception
    {
        double[][] originalMatrix = new double[50][50];
        double[][] matrix = new double[50][50];
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < matrix.length; j++ )
                matrix[i][j] = originalMatrix[i][j] = Math.random();

        double time = System.currentTimeMillis();
        for (int i=0; i<10000; i++)
        {
        QRDecomposition qr = new QRDecomposition(matrix);
        double[][] Q = qr.getQFull();
        double[][] R = qr.getR();
        }
        System.out.println("Simple: " + ( System.currentTimeMillis() - time ));
//        double[][] error = Util.diff(Util.multiply(Q, R), originalMatrix, 1);
//        System.out.println("Error: " + new Matrix(error).norm2());
        
        
        time = System.currentTimeMillis();
        for (int i=0; i<10000; i++)
        {
            Array2DRowRealMatrix matrix2 = new Array2DRowRealMatrix(originalMatrix);
        org.apache.commons.math3.linear.QRDecomposition qrApache = new org.apache.commons.math3.linear.QRDecomposition(matrix2);
        double[][] QApache = qrApache.getQ().getData();
        double[][] RApache = qrApache.getR().getData();
        }
        System.out.println("Apache: " + ( System.currentTimeMillis() - time ));
//        double[][] errorApache = Util.diff(Util.multiply(QApache, RApache), originalMatrix, 1);
//        System.out.println("Error: " + new Matrix(errorApache).norm2());
        
        DenseDoubleMatrix2D matrix3 = new DenseDoubleMatrix2D(originalMatrix);
        time = System.currentTimeMillis();
        for (int i=0; i<10000; i++)
        {
        cern.colt.matrix.linalg.QRDecomposition qrCern = new cern.colt.matrix.linalg.QRDecomposition(matrix3);
        double[][] QCern = qrCern.getQ().toArray();
        double[][] RCern = qrCern.getR().toArray();
        }
        System.out.println("Cern: " + ( System.currentTimeMillis() - time ));
//        double[][] errorCern = Util.diff(Util.multiply(QCern, RCern), originalMatrix, 1);
//        System.out.println("Error: " + new Matrix(errorCern).norm2());
        
        Matrix matrix4 = new Matrix(originalMatrix);
        time = System.currentTimeMillis();
        for (int i=0; i<10000; i++)
        {
        Jama.QRDecomposition qrJama = new Jama.QRDecomposition(matrix4);
        double[][] QJama = qrJama.getQ().getArray();
        double[][] RJama = qrJama.getR().getArray();
        }
        System.out.println("Jama: " + ( System.currentTimeMillis() - time ));
//        double[][] errorJama = Util.diff(Util.multiply(QJama, RJama), originalMatrix, 1);
//        System.out.println("Error: " + new Matrix(errorJama).norm2());
    }

}
