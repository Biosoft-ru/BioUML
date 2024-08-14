package biouml.plugins.simulation._test;

import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;

public class TestBIOMD404 extends TestCase implements ResultListener
{
    public TestBIOMD404(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestBIOMD404.class.getName() );
        suite.addTest( new TestBIOMD404( "test" ) );
        return suite;
    }

    BIOMD404 modelOriginal = new BIOMD404();
    BIOMD404_2 modelChanged = new BIOMD404_2();
    B404 modelSimple = new B404();
    
    private String mode_original = "ORIGINAL";
    private String mode_changed = "CHANGED";
    private String mode_simple = "SIMPLE";
    private String mode = mode_original;
    
    public void test() throws Exception
    {

//        JavaSimulationEngine engine = new JavaSimulationEngine();
        
//        engine.setSpan(  );
        
//        engine.simulate( model );
//        System.out.println("");
//        System.out.println("COMPlEX:");
//        System.out.println("");
//        mode = mode_original;
//        
//        EventLoopSimulator solver = new EventLoopSimulator();
//        JVodeSolver inner = (JVodeSolver)solver.getSolver();
//        inner.getOptions().setMethod( 1 );
////        inner.getOptions().setIterations( IterationType.FUNCTIONAL );
//        solver.start( modelOriginal, new UniformSpan(0, 0.001, 0.0001), new ResultListener[] {this}, null );
//        
//        System.out.println("");
//        System.out.println("CHANGED:");
//        System.out.println("");
//        mode = mode_changed;
//        
//        EventLoopSimulator solver2 = new EventLoopSimulator();
//        JVodeSolver inner2 = (JVodeSolver)solver.getSolver();
//        inner2.getOptions().setMethod( 1 );
////        inner.getOptions().setIterations( IterationType.FUNCTIONAL );
//        solver2.start( modelChanged, new UniformSpan(0, 0.001, 0.0001), new ResultListener[] {this}, null );
        
        System.out.println("");
        System.out.println("SIMPLE:");
        System.out.println("");
        mode = mode_simple;
        
        EventLoopSimulator solver3 = new EventLoopSimulator();
        JVodeSolver inner3 = (JVodeSolver)solver3.getSolver();
        inner3.getOptions().setMethod( 1 );
//        inner.getOptions().setIterations( IterationType.FUNCTIONAL );
        solver3.start( modelSimple, new UniformSpan(0, 0.001, 0.0001), new ResultListener[] {this}, null );
        
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        // TODO Auto-generated method stub
//        System.out.println( t+"\t"+DoubleStreamEx.of( y ).joining( "\t" ) );
//        System.out.println( t+"\t"+model.x_values[0]/model.cell+"\t"+model.x_values[1]/model.cell+"\t"+model.x_values[5]/model.cell+"\t"+model.rate_motor_r1+"\t"+model.rate_motor_r2 );
        
        if( mode == mode_original )
        {
            double r1 = modelOriginal.rate_motor_r1;
            double r2 = modelOriginal.rate_motor_r2;
            double r3 = modelOriginal.rate_motor_r3;
            double r4 = modelOriginal.rate_motor_r4;

            double M = modelOriginal.x_values[0] / modelOriginal.cell;
            double MYp = modelOriginal.x_values[1] / modelOriginal.cell;
            double MYYp = modelOriginal.x_values[2] / modelOriginal.cell;
            double MYYYp = modelOriginal.x_values[3] / modelOriginal.cell;
            double MYYYYp = modelOriginal.x_values[4] / modelOriginal.cell;
            double Yp = modelOriginal.x_values[5] / modelOriginal.cell;
            
            double check = modelOriginal.ka*(M*Yp - modelOriginal.kappa/4.0*MYp);
            
//                    System.out.println( t+"\t"+check+"\t"+r1+"\t"+r2+"\t"+r3+"\t"+r4 );
            System.out.println( t + "\t" + M + "\t" + MYp + "\t" + MYYp + "\t" + MYYYp + "\t" + MYYYYp + "\t" + Yp );
        }
        else if (mode == mode_simple)
        {
            double r1 = modelSimple.r1;
            double r2 = modelSimple.r2;
            double r3 = modelSimple.r3;
            double r4 = modelSimple.r4;

            double M = modelSimple.C;
            double MYp = modelSimple.C1;
            double MYYp = modelSimple.C2;
            double MYYYp = modelSimple.C3;
            double MYYYYp = modelSimple.C4;
            double Yp = modelSimple.Yc;
            
            double cell = modelSimple.cell;
            double check =modelSimple.ka*(M*Yp - modelSimple.kappa/4.0*MYp);
            
            System.out.println( t + "\t" + M + "\t" + Yp + "\t" + MYp + "\t" + r1);
//                        System.out.println( t+"\t"+check+"\t"+r1+"\t"+r2+"\t"+r3+"\t"+r4 );
//            System.out.println( t + "\t" + M + "\t" + MYp + "\t" + MYYp + "\t" + MYYYp + "\t" + MYYYYp + "\t" + Yp );
        }
        else if (mode == mode_changed)
        {
            double r1 = modelChanged.rate_motor_r1;
            double r2 = modelChanged.rate_motor_r2;
            double r3 = modelChanged.rate_motor_r3;
            double r4 = modelChanged.rate_motor_r4;

            double M = modelChanged.x_values[0] / modelChanged.cell;
            double MYp = modelChanged.x_values[1] / modelChanged.cell;
            double MYYp = modelChanged.x_values[2] / modelChanged.cell;
            double MYYYp = modelChanged.x_values[3] / modelChanged.cell;
            double MYYYYp = modelChanged.x_values[4] / modelChanged.cell;
            double Yp = modelChanged.x_values[5] / modelChanged.cell;
            
            double check =modelChanged.ka*(M*Yp - modelChanged.kappa/4.0*MYp);
//                    System.out.println( t+"\t"+check+"\t"+r1+"\t"+r2+"\t"+r3+"\t"+r4 );
//            System.out.println( t + "\t" + M + "\t" + MYp + "\t" + MYYp + "\t" + MYYYp + "\t" + MYYYYp + "\t" + Yp );
            
            System.out.println( t + "\t" + M + "\t" + Yp + "\t" + MYp + "\t" + r1);
        }
    }
}
