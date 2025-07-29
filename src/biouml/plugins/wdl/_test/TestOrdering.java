package biouml.plugins.wdl._test;

import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.WDLUtil;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;

public class TestOrdering
{
    public static void main(String ... args) throws Exception
    {
        //        test( createSimpleBranches() );

        test( createCycleDiagram() );
    }

    private static void test(Diagram diagram) throws Exception
    {
        List<Node> result = WDLUtil.orderCallsScatters( diagram );

        for( Node c : result )
            System.out.println( c.getName() );
        
        for (Compartment cycle: WDLUtil.getCycles( diagram ))
        {
            List<Node> resultCycle = WDLUtil.orderCallsScatters( cycle );
            
            System.out.println( "" );
            System.out.println( "CYCLE "+cycle.getName() );
            System.out.println( "" );
            for( Node c : resultCycle )
                System.out.println( c.getName() );
        }
    }

    private static Diagram createSimpleBranches() throws Exception
    {
        Diagram diagram = new WDLDiagramType().createDiagram( null, "test", new DiagramInfo( null, "test" ) );

        createCall( diagram, "task1" );
        createCall( diagram, "task11", "task1" );
        createCall( diagram, "task12", "task1" );
        createCall( diagram, "task111", "task11" );
        createCall( diagram, "task112", "task11", "task12" );
        createCall( diagram, "task3" );
        createCall( diagram, "task31", "task3" );
        return diagram;
    }

    private static Diagram createCycleDiagram() throws Exception
    {
        Diagram diagram = new WDLDiagramType().createDiagram( null, "test", new DiagramInfo( null, "test" ) );
        createCall( diagram, "task1" );
        Compartment cycle1 = createCycle( diagram, "cycle1" );
        createCall( cycle1, "task11", "task1" );
        createCall( cycle1, "task12", "task11" );
        Compartment cycle2 = createCycle( diagram, "cycle2" );
        createCall( cycle2, "task21", "task12" );
        createCall( cycle2, "task22", "task21" );
        createCall( diagram, "task3", "task22" );
        return diagram;
    }

    public static Compartment createCycle(Compartment parent, String name)
    {
        Compartment c = new Compartment( parent, name, new Stub( null, name, WDLConstants.SCATTER_TYPE ) );
        parent.put( c );
        return c;
    }

    public static void createCall(Compartment parent, String name, String ... previous)
    {
        Compartment c = new Compartment( parent, name, new Stub( null, name, WDLConstants.CALL_TYPE ) );
        parent.put( c );

        if( previous.length == 0 )
            return;

        Diagram diagram = Diagram.getDiagram( parent );
        for( String pName : previous )
        {
            Compartment p = (Compartment)diagram.findNode( pName );
            String inputName = p.getName() + "_input";
            Node input = new Node( c, inputName, new Stub( null, inputName, WDLConstants.INPUT_TYPE ) );
            c.put( input );

            String outputName = c.getName() + "_output";
            Node output = new Node( p, outputName, new Stub( null, outputName, WDLConstants.OUTPUT_TYPE ) );
            p.put( input );

            WDLImporter.createLink( output, input, WDLConstants.LINK_TYPE );
        }

    }
}
