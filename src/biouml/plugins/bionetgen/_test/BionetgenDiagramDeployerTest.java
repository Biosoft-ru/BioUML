package biouml.plugins.bionetgen._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramDeployer;
import biouml.plugins.bionetgen.diagram.PermutationList;
import biouml.standard.type.Specie;
import biouml.standard.type.Type;

public class BionetgenDiagramDeployerTest extends BionetgenDiagramGeneratorTest
{
    private static final String REV_PREFIX = "rev";
    private static final String REACTION = "reaction";

    public BionetgenDiagramDeployerTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenDiagramDeployerTest.class.getName());

        suite.addTest(new BionetgenDiagramDeployerTest("testPermutationList"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_egfr_net"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_simple_system"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_egfr_path"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_toy_jim"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_blbr"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_tlbr"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_SHP2_base_model"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_CaOscillate_Func"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_CaOscillate_Sat"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_Haugh2b"));
        suite.addTest(new BionetgenDiagramDeployerTest("testDiagram_fceri_ji"));

        return suite;
    }

    public void testPermutationList() throws Exception
    {
        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("Big", "Small", "Medium"));
        data.add(Arrays.asList("Green", "Yellow", "Red"));
        data.add(Arrays.asList("Apple", "Pear"));
        String expected = "[[Big, Green, Apple], [Small, Green, Apple], [Medium, Green, Apple], "
                + "[Big, Yellow, Apple], [Small, Yellow, Apple], [Medium, Yellow, Apple], "
                + "[Big, Red, Apple], [Small, Red, Apple], [Medium, Red, Apple], "
                + "[Big, Green, Pear], [Small, Green, Pear], [Medium, Green, Pear], "
                + "[Big, Yellow, Pear], [Small, Yellow, Pear], [Medium, Yellow, Pear], "
                + "[Big, Red, Pear], [Small, Red, Pear], [Medium, Red, Pear]]";
        assertEquals( expected, new PermutationList<>( data ).toString() );
    }

    public void testDiagram_toy_jim() throws Exception
    {
        Diagram diagram = initDiagram( "toy-jim" );

        String expected = "[101, 9, 24, 46, 4(rev: 4), 10(rev: 10), 18(rev: 18), 14(rev: 14), 1(rev: 0), 1(rev: 0), 6(rev: 0), 1(rev: 0)]";
        List<String> data = getDetailedData( diagram, 8 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_tlbr() throws Exception
    {
        Diagram diagram = initDiagram( "tlbr" );

        String expected = "[99, 8, 22, 39, 9(rev: 9), 25(rev: 18), 26(rev: 12)]";
        List<String> data = getDetailedData( diagram, 3 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_simple_system() throws Exception
    {
        Diagram diagram = initDiagram( "simple_system" );

        String expected = "[4, 6, 4, 0, 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0)]";
        List<String> data = getDetailedData( diagram, 4 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_blbr() throws Exception
    {
        Diagram diagram = initDiagram( "blbr" );

        String expected = "[60, 0, 16, 30, 7(rev: 7), 19(rev: 19), 4(rev: 4)]";
        List<String> data = getDetailedData( diagram, 3 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_egfr_path() throws Exception
    {
        Diagram diagram = initDiagram( "egfr_path" );

        String expected = "[37, 11, 18, 18, 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), "
                + "1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), "
                + "1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 1), 1(rev: 0), 1(rev: 1), 1(rev: 1)]";
        List<String> data = getDetailedData( diagram, 19 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_SHP2_base_model() throws Exception
    {
        Diagram diagram = initDiagram( "SHP2_base_model" );

        String expected = "[1032, 1, 149, 413, 72(rev: 0), 49(rev: 49), 96(rev: 96), 48(rev: 48), "
                + "72(rev: 72), 72(rev: 0), 62(rev: 0), 9(rev: 9), 14(rev: 14), 14(rev: 14), 9(rev: 9), "
                + "14(rev: 14), 14(rev: 14), 14(rev: 14), 14(rev: 14), 14(rev: 14), 14(rev: 14), 3(rev: 3), "
                + "3(rev: 3), 3(rev: 3), 3(rev: 3), 3(rev: 3), 3(rev: 3)]";
        List<String> data = getDetailedData( diagram, 23 );
        assertEquals(expected, data.toString());
    }

    public void testDiagram_egfr_net() throws Exception
    {
        Diagram diagram = initDiagram( "egfr_net" );

        String expected = "[3749, 15, 356, 1524, 24(rev: 24), 300(rev: 300), 144(rev: 0),"
                + " 96(rev: 0), 156(rev: 0), 104(rev: 0), 96(rev: 0), 104(rev: 0), 156(rev: 156),"
                + " 156(rev: 156), 156(rev: 156), 104(rev: 104), 104(rev: 104), 104(rev: 104),"
                + " 104(rev: 104), 104(rev: 104), 104(rev: 104), 104(rev: 104), 1(rev: 1), 1(rev: 1), 1(rev: 0), 1(rev: 1), 1(rev: 1)]";
        List<String> data = getDetailedData( diagram, 23 );

        assertEquals(expected, data.toString());
    }

    public void testDiagram_CaOscillate_Func() throws Exception
    {
        Diagram diagram = initDiagram( "CaOscillate_Func" );

        String expected = "[8, 4, 4, 0, 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0)]";
        List<String> data = getDetailedData( diagram, 8 );
        assertEquals( expected, data.toString() );
    }

    public void testDiagram_Haugh2b() throws Exception
    {
        Diagram diagram = initDiagram( "Haugh2b" );

        String expected = "[29, 5, 12, 12, 2(rev: 2), 1(rev: 1), 3(rev: 3), 1(rev: 1), 1(rev: 1), 1(rev: 0), 1(rev: 1),"
                + " 1(rev: 1), 1(rev: 1), 1(rev: 1), 4(rev: 0)]";
        List<String> data = getDetailedData( diagram, 11 );
        assertEquals( expected, data.toString() );
    }

    public void testDiagram_fceri_ji() throws Exception
    {
        Diagram diagram = initDiagram( "fceri_ji" );

        String expected = "[3680, 10, 354, 1328, 24(rev: 24), 576(rev: 576), 156(rev: 156), 36(rev: 0), 24(rev: 0), "
                + "156(rev: 156), 36(rev: 0), 24(rev: 0), 416(rev: 416), 48(rev: 0), 48(rev: 0), 64(rev: 0), 64(rev: 0), "
                + "156(rev: 0), 104(rev: 0), 208(rev: 0), 208(rev: 0), 2(rev: 0), 2(rev: 0)]";
        List<String> data = getDetailedData( diagram, 19 );
        assertEquals( expected, data.toString() );
    }

    public void testDiagram_CaOscillate_Sat() throws Exception
    {
        Diagram diagram = initDiagram( "CaOscillate_Sat" );

        String expected = "[8, 4, 4, 0, 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0), 1(rev: 0)]";
        List<String> data = getDetailedData( diagram, 8 );
        assertEquals( expected, data.toString() );
    }

    private Diagram initDiagram(String diagramName) throws Exception
    {
        BionetgenTestUtility.initPreferences();
        return BionetgenDiagramDeployer.deployBNGDiagram( generateDiagram( diagramName, false ), false );
    }

    private List<String> getDetailedData(Diagram diagram, int templatesNumber)
    {
        int reactionCounter = 0;
        int observableCounter = 0;
        int moleculeCounter = 0;
        int revReactionCounter = 0;
        int[] reactionNumbers = new int[templatesNumber];
        int[] revReactionNumbers = new int[templatesNumber];
        int maxOneDigitIndex = Math.min( 9, templatesNumber );
        for( Node node : diagram.getNodes() )
        {
            if( isReaction( node ) )
            {
                reactionCounter++;
                if( node.getKernel().getName().startsWith(REV_PREFIX) )
                {
                    revReactionCounter++;
                    for( int i = 0; i < maxOneDigitIndex; i++ )
                    {
                        if( node.getKernel().getName().startsWith("revj0".concat(String.valueOf(i + 1))) )
                            revReactionNumbers[i]++;
                    }
                    if( templatesNumber < 10 )
                        continue;
                    for( int i = 9; i < templatesNumber; i++ )
                    {
                        if( node.getKernel().getName().startsWith( "revj".concat( String.valueOf( i + 1 ) ) ) )
                            revReactionNumbers[i]++;
                    }
                }
                else
                {
                    for( int i = 0; i < maxOneDigitIndex; i++ )
                    {
                        if( node.getKernel().getName().startsWith("j0".concat(String.valueOf(i + 1))) )
                            reactionNumbers[i]++;
                    }
                    if( templatesNumber < 10 )
                        continue;
                    for( int i = 9; i < templatesNumber; i++ )
                    {
                        if( node.getKernel().getName().startsWith("j".concat(String.valueOf(i + 1))) )
                            reactionNumbers[i]++;
                    }
                }
            }
            if( isObservable( node ) )
                observableCounter++;
            if( isMolecule( node ) )
                moleculeCounter++;
        }
        List<String> data = new ArrayList<>();
        data.add(String.valueOf(reactionCounter));
        data.add(String.valueOf(observableCounter));
        data.add(String.valueOf(moleculeCounter));
        data.add(String.valueOf(revReactionCounter));
        for( int i = 0; i < templatesNumber; i++ )
        {
            data.add(String.valueOf(reactionNumbers[i]).concat("(rev: ").concat(String.valueOf(revReactionNumbers[i])).concat(")"));
        }
        return data;
    }

    private boolean isReaction(Node node)
    {
        return REACTION.equals( node.getKernel().getType() );
    }

    private boolean isObservable(Node node)
    {
        return Type.MATH_EQUATION.equals( node.getKernel().getType() ) && node.getName().endsWith( BionetgenConstants.OBSERVABLE );
    }

    private boolean isMolecule(Node node)
    {
        return node.getKernel() instanceof Specie;
    }
}
