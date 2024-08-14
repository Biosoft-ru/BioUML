package biouml.plugins.hemodynamics._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TempFiles;

public class ArterialTreeFitTest extends TestCase
{
    public ArterialTreeFitTest(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( ArterialTreeFitTest.class.getName() );
        suite.addTest( new ArterialTreeFitTest( "testObserver" ) );
        return suite;
    }

    
    public void testObserver() throws Exception
    {
        simulate(1,1,1);
        
    }
    
    public void test() throws Exception
    {
        File f = AbstractBioUMLTest.getTestFile( "matrices-results.txt" );
        try(BufferedWriter bw = ApplicationUtils.utfWriter( f ))
        {
            bw.write("\nall beta = 1\t");
            write( simulate(1, 1, 1), bw );
            
            bw.write("\nBETA1");
            bw.write("\nbeta1 = 1.2\t");
            write( simulate(1.2, 1, 1), bw );
            bw.write("\nbeta1 = 2\t");
            write( simulate(2, 1, 1), bw );
            bw.write("\nbeta1 = 3\t");
            write( simulate(3, 1, 1), bw );
            bw.write("\nDECREASE\n");
            bw.write("\nbeta1 = 0.8\t");
            write( simulate(0.8, 1, 1), bw );
            bw.write("\nbeta1 = 1.2\t");
            write( simulate(0.5, 1, 1), bw );
            
            bw.write("\nBETA2");
            bw.write("\nbeta2 = 1.2\t");
            write( simulate(1, 1.2, 1), bw );
            bw.write("\nbeta2 = 2\t");
            write( simulate(1, 2, 1), bw );
            bw.write("\nbeta2 = 3\t");
            write( simulate(1, 3, 1), bw );
            bw.write("\nDECREASE\n");
            bw.write("\nbeta2 = 0.8\t");
            write( simulate(1, 0.8, 1), bw );
            bw.write("\nbeta2 = 0.5\t");
            write( simulate(1, 0.5, 1), bw );
            
            bw.write("\nBETA3");
            bw.write("\nbeta3 = 1.2\t");
            write( simulate(1, 1, 1.2), bw );
            bw.write("\nbeta3 = 2\t");
            write( simulate(1, 1, 2), bw );
            bw.write("\nbeta3 = 4\t");
            write( simulate(1, 1, 3), bw );
            bw.write("\nDECREASE\n");
            bw.write("\nbeta3 = 0.8\t");
            write( simulate(1, 1, 0.8), bw );
            bw.write("\nbeta3 = 0.5\t");
            write( simulate(1, 1, 0.5), bw );
            
            bw.write("\nALL BETA");
            bw.write("\nbeta = 1.2\t");
            write( simulate(1.2, 1.2, 1.2), bw );
            bw.write("\nbeta = 2\t");
            write( simulate(2, 2, 2), bw );
            bw.write("\nbeta = 3\t");
            write( simulate(3, 3, 3), bw );
            bw.write("\nDECREASE\n");
            bw.write("\nbeta = 0.8\t");
            write( simulate(0.8, 0.8, 0.8), bw );
            bw.write("\nbeta = 0.5\t");
            write( simulate(0.5, 0.5, 0.5), bw );
        }
    }
    
    public void write(Double[] vals, BufferedWriter bw) throws IOException
    {
        for (Double val: vals)
        {
           bw.write( String.valueOf(val) );
           bw.write("\t");
        }
    }
    
    public Double[] simulate(double betaFactor1, double betaFactor2, double betaFactor3) throws Exception
    {
        HemodynamicsModelSolver solver = new HemodynamicsModelSolver();
        solver.beta1 = betaFactor1;
        solver.beta2 = betaFactor2;
        solver.beta3 = betaFactor3;
        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();

        engine.setDiagram(  getDiagram() );
        engine.setSolver( solver );
        engine.setTimeIncrement( 0.001 );
        engine.setCompletionTime( 15 );
        ResultCollector collector = new ResultCollector(24);
        engine.addListeners( collector );
        engine.simulate();
        return collector.results.toArray(new Double[collector.results.size()]);
        
    }

    protected String outputDir = TempFiles.path( "simulation" ).getAbsolutePath();

    protected void writeFile(String source, boolean rewrite) throws Exception
    {
        File out = new File( outputDir, source );
        if( out.exists() && !rewrite )
            return;
        out.getParentFile().mkdirs();

        // load from srcDir
        File file = new File( source );
        if( !file.exists() )
            file = new File( ".", source );

        InputStream is;
        if( file.exists() )
            is = new FileInputStream( file );
        else
        {
            // try to load through plugin class loader
            ClassLoader cl = getClass().getClassLoader();
            URL url = cl.getResource( source );

            if( url == null ) // try to get resources as file
                throw new FileNotFoundException( "Can not find file: " + source );

            is = url.openStream();
        }

        com.developmentontheedge.application.ApplicationUtils.copyStream( new FileOutputStream( out, false ), is );
    }
    public Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        return DataElementPath.create("databases/arterialTree/Diagrams/Arterial Tree Brachial" ).getDataElement( Diagram.class );
    }
    
    public static class ResultCollector implements ResultListener
    {

        public double max = 0;
        public double min = 1000;
        int index;
        
        public ResultCollector(int index)
        {
            this.index = index;
        }
        
        @Override
        public void start(Object model)
        {
            // TODO Auto-generated method stub
            
        }
        public List<Double> results = new ArrayList<>();
        @Override
        public void add(double t, double[] y) throws Exception
        {
          
            if (t < 10)
            {
                return;
            }
            
            results.add( y[index] );
            
            max = Math.max( max, y[index]);
            min = Math.min( min, y[index] );
            
        }
        
    }
}
