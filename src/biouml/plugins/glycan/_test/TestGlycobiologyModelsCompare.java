package biouml.plugins.glycan._test;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.glycan.DiagramNodeMap;
import biouml.plugins.glycan.DiagramNodeMap.LinkedNode;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class TestGlycobiologyModelsCompare extends TestCase
{

    public TestGlycobiologyModelsCompare(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestGlycobiologyModelsCompare.class.getName());
        suite.addTest(new TestGlycobiologyModelsCompare("test"));
        return suite;
    }

    private final static String diagramPath = "databases/Glycan_structures/Diagrams/";
    private final static DataElementPath DIAGRAM_PATH = DataElementPath.create(diagramPath + "glycobiology_bng(withSumms)");
    private final static DataElementPath DIAGRAM_PATH_FRED = DataElementPath.create(diagramPath + "glycobiology_Fred");
    private static final String SEARCHING_GLYCAN = "Ma2Ma2Ma3(Ma3(Ma2Ma6)Ma6)Mb4GNb4GN";

    public void test() throws Exception
    {
        CollectionFactory.createRepository("../data");
        Application.setPreferences(new Preferences());


        boolean foundDifference = false;
        int depth = 8;
        DiagramNodeMap nodeMap = new DiagramNodeMap(depth);

        nodeMap.initSecondChain( getGlycanNode( DIAGRAM_PATH_FRED.getDataElement( Diagram.class ), SEARCHING_GLYCAN ) );
        nodeMap.initFirstChain( getGlycanNode( DIAGRAM_PATH.getDataElement( Diagram.class ), SEARCHING_GLYCAN ) );

        try (PrintWriter pw = new PrintWriter( AbstractBioUMLTest.getTestFile( "comparingResults.txt" ), StandardCharsets.UTF_8.toString() ))
        {
            nodeMap.compare();

            Set<LinkedNode> notMapped1 = nodeMap.getNotMapped1();
            if( notMapped1.size() != 0 )
            {
                pw.println( "Contains " + notMapped1.size() + " element(s) in d1(BNG) with search depth = " + depth );
                foundDifference = true;
                for( LinkedNode lNode : notMapped1 )
                {
                    pw.print( lNode.getGlycanStructure() );
                    pw.print( "    " );
                    pw.println( "(on depth " + lNode.getCurrentDepth() + ")" );

                    saveRule( lNode );
                }
            }
            Set<LinkedNode> notMapped2 = nodeMap.getNotMapped2();
            if( notMapped2.size() != 0 )
            {
                pw.println( "Contains " + notMapped2.size() + " element(s) in d2(Fred) with search depth = " + depth );
                foundDifference = true;
                for( LinkedNode lNode : notMapped2 )
                {
                    pw.print( lNode.getGlycanStructure() );
                    pw.print( "    " );
                    pw.println( "(on depth " + lNode.getCurrentDepth() + ") from " + lNode.getPreviousNode().getGlycanStructure()
                            + " using reaction " + lNode.getReactionNodeName() );
                }
            }
            if( foundDifference )
                System.out.println( "Difference was found!" );
        }

        try (PrintWriter pw = new PrintWriter( AbstractBioUMLTest.getTestFile( "badRules.txt" ), StandardCharsets.UTF_8.toString() ))
        {
            for( String rule : rules )
                pw.println( rule );
        }
    }

    private @Nonnull Node getGlycanNode(Diagram diagram, String glycanName)
    {
        return diagram.stream( Node.class )
                .findAny( node -> glycanName.equals( node.getAttributes().getValueAsString( DiagramNodeMap.GLYCAN_ATTR ) ) )
                .orElseThrow( () -> new IllegalArgumentException( "Glycan " + glycanName + " is not found in diagram "
                                + diagram.getCompletePath() ) );
    }
    Set<String> rules = new LinkedHashSet<>();
    private void saveRule(LinkedNode missedNode)
    {
        String reactionName = missedNode.getReactionNodeName();
        String prevGlycan = missedNode.getPreviousNode() == null ? SEARCHING_GLYCAN : missedNode.getPreviousNode().getGlycanStructure();
        rules.add(prevGlycan + " ---(" + reactionName.substring(1, reactionName.lastIndexOf("_")) + ")--> "
                + missedNode.getGlycanStructure() + " (depth: " + missedNode.getCurrentDepth() + ")");
    }
}
