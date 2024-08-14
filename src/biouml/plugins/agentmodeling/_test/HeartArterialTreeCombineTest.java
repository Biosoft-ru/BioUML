package biouml.plugins.agentmodeling._test;

import java.awt.BasicStroke;
import java.awt.Color;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.HemodynamicsSolver;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.Pen;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class HeartArterialTreeCombineTest// extends TestCase
{

    static Pen solid = new Pen( new BasicStroke( 1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0 ), Color.black );
    static Pen dashed = new Pen( new BasicStroke( 1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 6}, 0 ),
            Color.black );
    static Pen dot = new Pen( new BasicStroke( 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {2, 9}, 0 ), Color.black );
    static Pen dashdot = new Pen( new BasicStroke( 1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 4, 2, 4}, 0 ),
            Color.black );

    static Span span = new UniformSpan( 0, 200, 0.001 );

    //    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t.txt";
    //    public HeartAgentModelTest(String name)
    //    {
    //        super(name);
    //    }

    //
    //    public static TestSuite suite()
    //    {
    //        TestSuite suite = new TestSuite(HeartAgentModelTest.class.getName());
    //        suite.addTest(new HeartAgentModelTest("test"));
    //        return suite;
    //    }

    public static void main(String ... args) throws Exception
    {
        //                simulate( HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING, HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING );
        //                simulate( HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING, HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING );
        simulate( HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING, HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING );
        //                simulate( HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING, HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING );
        System.out.println( "done" );
    }

    public static void simulate(String inputCondition, String outputCondition) throws Exception
    {
        new AgentModelSimulationEngine().simulate( generateModel( inputCondition, outputCondition ) );
    }

    private static AgentBasedModel generateModel(String inputCondition, String outputCondition) throws Exception
    {
        Diagram arterialTree = getDiagram( "Arterial Tree" );
        arterialTree.getRole(EModel.class).getVariable( "inputFlow" ).setConstant( true );
        arterialTree.getRole(EModel.class).getVariable( "inputPressure" ).setConstant( true );
        HemodynamicsSimulationEngine hemodynamicEngine = new HemodynamicsSimulationEngine();
        HemodynamicsSolver hemodynamicSolver = new HemodynamicsModelSolver();///(HemodynamicsSolver)hemodynamicEngine.getSimulator();///new HemodynamicsModelSolver7();
        HemodynamicsOptions options = (HemodynamicsOptions)hemodynamicSolver.getOptions();
        options.setInputCondition( inputCondition );
        options.setOutputCondition( outputCondition );
        hemodynamicEngine.setSolver( hemodynamicSolver );

        hemodynamicEngine.setInitialTime( span.getTimeStart() );
        hemodynamicEngine.setCompletionTime( span.getTimeFinal() );
        hemodynamicEngine.setTimeIncrement( span.getTime( 1 ) - span.getTimeStart() );
        hemodynamicEngine.setDiagram( arterialTree );

        Diagram solodyannikovDiagram = getDiagram( "Solodyannikov 1994" );
        EModel heartEModel = solodyannikovDiagram.getRole(EModel.class);
        heartEModel.getVariable( "BloodFlow_Capillary" ).setConstant( true );
        heartEModel.getVariable( "Volume_Arterial" ).setConstant( true );
        heartEModel.getVariable( "Resistance_Arterial" ).setConstant( true );
        heartEModel.getVariable( "Pressure_Arterial" ).setConstant( true );
        
//        solodyannikovDiagram.setStateEditingMode( solodyannikovDiagram.getState( "Steady State" ) );
        solodyannikovDiagram.setStateEditingMode( solodyannikovDiagram.getState( "Cardiogenic shock" ) );
        JavaSimulationEngine javaEngine = new JavaSimulationEngine();
        javaEngine.setDiagram( solodyannikovDiagram );
        javaEngine.setInitialTime( span.getTimeStart() );
        javaEngine.setCompletionTime( span.getTimeFinal() );
        javaEngine.setTimeIncrement( span.getTime( 1 ) - span.getTimeStart() );

        AgentBasedModel agentModel = new AgentBasedModel();

        ModelAgent arterialTreeAgent = new ModelAgent( hemodynamicEngine );
        ModelAgent heartAgent = new ModelAgent( javaEngine );

        PlotAgent plot = new PlotAgent( "plot", span );
        PlotAgent plot2 = new PlotAgent( "plot2", span );

        agentModel.addAgent( heartAgent );
        agentModel.addAgent( plot );
        agentModel.addAgent( plot2 );
        agentModel.addAgent( arterialTreeAgent );

        if( inputCondition.equals( HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING ) )
            agentModel.addDirectedLink( heartAgent, "Pressure_Arterial", arterialTreeAgent, "inputPressure" );
        else if( inputCondition.equals( HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING ) )
            agentModel.addDirectedLink( heartAgent, "BloodFlow_VentricleToArteria", arterialTreeAgent, "inputFlow" );


        if( outputCondition.equals( HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING ) )
            agentModel.addDirectedLink( heartAgent, "BloodFlow_Capillary", arterialTreeAgent, "outputFlow" );
        else if( outputCondition.equals( HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING ) )
        {
            agentModel.addDirectedLink( heartAgent, "Pressure_Venous", arterialTreeAgent, "venousPressure" );
            agentModel.addDirectedLink( heartAgent, "Resistance_Capillary", arterialTreeAgent, "capillaryResistance" );
        }

        //        agentModel.addDirectedLink( heartAgent, "BloodFlow_VentricleToArteria", plot, "input Flow2" );
        //        agentModel.addDirectedLink( arterialTreeAgent, "inputFlow", plot, "input Flow" );
        agentModel.addDirectedLink( arterialTreeAgent, "$Common_Iliac", plot, "Common Iliac Pressure" );
        agentModel.addDirectedLink( arterialTreeAgent, "$Aorta", plot, "Aorta Pressure" );
        agentModel.addDirectedLink( arterialTreeAgent, "$Ulnar", plot, "Ulnar Pressure" );
        agentModel.addDirectedLink( arterialTreeAgent, "$Anterior_Tibial", plot, "Tibial Pressure" );

        plot.setSpec( "Common Iliac Pressure", dashed );
        plot.setSpec( "Ulnar Pressure", dashdot );
        plot.setSpec( "Aorta Pressure", solid );
        plot.setSpec( "Tibial Pressure", dot );

        //        agentModel.addDirectedLink(heartAgent, "BloodFlow_VentricularToArterial", plot, "inputFlow");
        agentModel.addDirectedLink( heartAgent, "BloodFlow_Capillary", plot2, "outputFlow" );
        agentModel.addDirectedLink( heartAgent, "Volume_Arterial", plot2, "totalVolume" );
        agentModel.addDirectedLink( heartAgent, "Pressure_Arterial", plot2, "Pressure_Arterial" );
        agentModel.addDirectedLink( heartAgent, "Resistance_Arterial", plot2, "Resistance_Arterial" );
        
        agentModel.addDirectedLink( arterialTreeAgent, "outputFlow", heartAgent, "BloodFlow_Capillary" );
        agentModel.addDirectedLink( arterialTreeAgent, "totalVolume", heartAgent, "Volume_Arterial" );
        agentModel.addDirectedLink( arterialTreeAgent, "averagePressure", heartAgent, "Pressure_Arterial" );
        agentModel.addDirectedLink( arterialTreeAgent, "arterialResistance", heartAgent, "Resistance_Arterial" );

        //                agentModel.addDirectedLink(arterialTreeAgent, "averagePressure", plot, "Pressure_Arterial");
        //        agentModel.addDirectedLink(arterialTree, "totalVolume", heart, "Volume_Arterial");
        //
        //        //        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plot, "Pressure_Arterial");
        //                agentModel.addDirectedLink(arterialTreeAgent, "outputFlow", plot, "outputFlow");
        //                agentModel.addDirectedLink(arterialTreeAgent, "totalVolume", plot, "totalVolume");


        return agentModel;
    }

    public static Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        return DataElementPath.create("databases/Cardiovascular student/Diagrams", name).getDataElement( Diagram.class );
    }
}