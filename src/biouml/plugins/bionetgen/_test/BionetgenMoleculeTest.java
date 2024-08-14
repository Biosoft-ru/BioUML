package biouml.plugins.bionetgen._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.bionetgen.diagram.BionetgenMap;
import biouml.plugins.bionetgen.diagram.BionetgenMolecule;
import biouml.plugins.bionetgen.diagram.BionetgenSpeciesGraph;

public class BionetgenMoleculeTest extends TestCase
{
    public BionetgenMoleculeTest(String name)
    {
        super(name);

        File configFile = new File( "./biouml/plugins/bionetgen/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenMoleculeTest.class.getName());

        suite.addTest(new BionetgenMoleculeTest("testMolecule"));
        for( int i = 0; i < 10; i++ )
        {
            suite.addTest(new BionetgenMoleculeTest("testSpeciesGraph"));
        }
        suite.addTest(new BionetgenMoleculeTest("testIsomorphicToSubgraph"));
        suite.addTest(new BionetgenMoleculeTest("testIsomorphicTo"));
        suite.addTest(new BionetgenMoleculeTest("testConnectedSubgraphs"));
        return suite;
    }

    public void testIsomorphicToSubgraph() throws Exception
    {
        String[][] tests = new String[][] {
                new String[] {"A(b!1,b!2).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4,e!?)",
                        "A(b!2,b!1).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4,e!5).E(d!5)",
                        "[[A(b!1,b!2)->A(b!1,b!2), B(a!1,c!3)->B(a!1,c!3), B(a!2,d!4)->B(a!2,d!4), C(b!3)->C(b!3), D(b!4,e!?)->D(b!4,e!5)]]"},
                new String[] {"egfr(r!+,Y1148~pU!1).Shc(PTB!1,Y317~Y)", "egfr(r!2,Y1148~U).egfr(r!2,Y1148~pU!1).Shc(PTB!1,Y317~Y)",
                        "[[Shc(PTB!1,Y317~Y)->Shc(PTB!1,Y317~Y), egfr(Y1148~pU!1,r!+)->egfr(Y1148~pU!1,r!2)]]"},
                new String[] {"A(b!+)", "C(b!1,d).B(c!1,a!2).A(b!2)", "[[A(b!+)->A(b!1)]]"},
                new String[] {"A(b)", "A(b!1).B(a!1,c)", "[]"},
                new String[] {"A(b)", "A.B(c!1).C(b!1)", "[]"},
                new String[] {"A(b)", "A(b~p)", "[[A(b)->A(b~p)]]"},
                new String[] {"A(b!1,c!?).B(a!1)", "A(b!1,c).B(a!1)", "[[A(b!1,c!?)->A(b!1,c), B(a!1)->B(a!1)]]"},
                new String[] {"A(b)", "A(b,c)", "[[A(b)->A(b,c)]]"},
                new String[] {"A.B", "A.B.C", "[[A->A, B->B]]"},
                new String[] {"X(y!1).Y(x!1)", "X(p~0,y!1).Y(x!1)", "[[X(y!1)->X(p~0,y!1), Y(x!1)->Y(x!1)]]"},
                new String[] {"A(b!1,b!2).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)", "A(b!2,b!1).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)",
                        "[[A(b!1,b!2)->A(b!1,b!2), B(a!1,c!3)->B(a!1,c!3), B(a!2,d!4)->B(a!2,d!4), C(b!3)->C(b!3), D(b!4)->D(b!4)]]"},};
        for( String[] test : tests )
        {
            BionetgenSpeciesGraph bsg1 = new BionetgenSpeciesGraph(test[0]);
            BionetgenSpeciesGraph bsg2 = new BionetgenSpeciesGraph(test[1]);
            List<String> result = new ArrayList<>();
            List<BionetgenMap> maps = bsg1.isomorphicToSubgraphOf(bsg2);
            for( BionetgenMap map : maps )
            {
                result.add(map.toString());
            }
            assertEquals(bsg1.toString() + " vs " + bsg2.toString(), test[2], result.toString());
        }
    }

    public void testIsomorphicTo()
    {
        String[][] tests = new String[][] {
                new String[] {"A(b!1,b!2).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)", "A(b!2,b!1).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)", "true"},
                new String[] {"A(b)", "A(b!1).B(a!1,c)", "false"},
                new String[] {"A.A(a).B(b)", "A.A(a).B", "false"},
                new String[] {"A(b~p)", "A(b~pU)", "false"},
                new String[] {"A(b~pU)", "A(b~pU)", "true"},
                new String[] {"A(b!1,b!2).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4,e!?)",
                        "A(b!2,b!1).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4,e!5).E(d!5)", "false"},
                new String[] {"A%1(a)", "A(a)", "true"},
                new String[] {
                        "L(l,l!16).R(r!16,r!1).L(l!1,l!2).R(r!2,r!7).L(l!7,l!8).R(r!8,r!6).L(l!6,l!5).R(r!5,r!13).L(l!13,l!14)."
                                + "R(r!14,r!12).L(l!12,l!11).R(r!11,r!3).L(l!3,l!4).R(r!4,r!9).L(l!9,l!10).R(r!10,r!15).L(l,l!15)",
                        "L(l,l!16).R(r!16,r!1).L(l!1,l!2).R(r!2,r!7).L(l!7,l!8).R(r!8,r!6).L(l!6,l!5).R(r!5,r!13).L(l!13,l!14)."
                                + "R(r!14,r!10).L(l!12,l!11).R(r!11,r!3).L(l!3,l!4).R(r!4,r!9).L(l!9,l!10).R(r!12,r!15).L(l,l!15)", "true"},
                new String[] {
                        "R(r!17,r).L(l!17,l!16).R(r!16,r!1).L(l!1,l!2).R(r!2,r!7).L(l!7,l!8).R(r!8,r!6).L(l!6,l!5).R(r!5,r!13).L(l!13,l!14)."
                                + "R(r!14,r!12).L(l!12,l!11).R(r!11,r!3).L(l!3,l!4).R(r!4,r!9).L(l!9,l!10).R(r!10,r!15).L(l,l!15)",
                        "L(l,l!16).R(r!16,r!1).L(l!1,l!2).R(r!2,r!7).L(l!7,l!8).R(r!8,r!6).L(l!6,l!5).R(r!5,r!13).L(l!13,l!14)."
                                + "R(r!14,r!10).L(l!12,l!11).R(r!11,r!3).L(l!3,l!4).R(r!4,r!9).L(l!9,l!10).R(r!12,r!15).L(l,l!15)", "false"},};

        for( String[] test : tests )
        {
            BionetgenSpeciesGraph bsg1 = new BionetgenSpeciesGraph(test[0]);
            BionetgenSpeciesGraph bsg2 = new BionetgenSpeciesGraph(test[1]);
            assertEquals(bsg1.toString() + " vs " + bsg2.toString(), test[2], String.valueOf(bsg1.isomorphicTo(bsg2)));
        }
    }

    public void testMolecule() throws Exception
    {
        String[][] tests = new String[][] {
                new String[] {"R3~P!+(a1~U!5!?,b2%G!1,d5!3!4)", "[R3, P, , 1, [a1~U!5!?, b2%G!1, d5!3!4], R3~P!+(a1~U!5!?,b2%G!1,d5!3!4)]"},
                new String[] {"Rds~P~U", "Invalid molecule format: Multiple state definition: 'Rds~P~U'"},
                new String[] {"A((b)", "Invalid molecule format (too many right parentheses): 'A((b)'"},
                new String[] {"A(b))", "Invalid molecule format (too many left parentheses): 'A(b))'"},
                new String[] {"A)", "Invalid molecule format (parentheses are not balanced): 'A)'"},
                new String[] {"A(", "Invalid molecule format (parentheses are not balanced): 'A('"},
                new String[] {"A(b", "Invalid molecule format (parentheses are not balanced): 'A(b'"},
                new String[] {"A%?", "Invalid molecule format: Invalid molecule format: 'A%?'"},
                new String[] {"A%+", "Invalid molecule format: Invalid molecule format: 'A%+'"},
                new String[] {"A~?", "Invalid molecule format: Invalid molecule format: 'A~?'"},
                new String[] {"A~+", "Invalid molecule format: Invalid molecule format: 'A~+'"},
                new String[] {"A(b~?)", "Invalid component format: 'b~?'"},
                new String[] {"A(b~+)", "Invalid component format: 'b~+'"},
                new String[] {"A(b%?)", "Invalid component format: 'b%?'"},
                new String[] {"A(b%+)", "Invalid component format: 'b%+'"},
                new String[] {"#()", "Invalid molecule format: '#()'"},//invalid molecule name
                new String[] {"A(#)", "Invalid component format: '#'"},//invalid component name
                new String[] {"A!+!1", "[A, , , 1, [], A!+!1]"},
                new String[] {"A%1%2", "Invalid molecule format: Multiple label definition: 'A%1%2'"},
                new String[] {"A!+!?", "Invalid molecule format: Multiple edge wildcard definition: 'A!+!?'"},
                new String[] {"A(b!+!?)", "Multiple edge wildcard definition: 'b!+!?'"},
                new String[] {"A(b%1%2)", "Multiple label definition: 'b%1%2'"},
                new String[] {"A(b~u~P)", "Multiple state definition: 'b~u~P'"},
                new String[] {"EGFR_4TLR", "[EGFR_4TLR, , , 0, [], EGFR_4TLR]"},
                new String[] {"EGFR_4TLR()", "[EGFR_4TLR, , , 0, [], EGFR_4TLR()]"},
                new String[] {"jkhd45h3(tp~U!1!2!+%E)!+~P", "[jkhd45h3, P, , 1, [tp~U%E!1!2!+], jkhd45h3~P!+(tp~U%E!1!2!+)]"},
                new String[] {"A(b,a,d,c!1,c)", "[A, , , 0, [a, b, c, c!1, d], A(a,b,c,c!1,d)]"},};

        BionetgenSpeciesGraph graph = new BionetgenSpeciesGraph("test");
        for( String[] test : tests )
        {
            try
            {
                BionetgenMolecule mol = new BionetgenMolecule(graph, test[0]);
                List<String> result = new ArrayList<>();
                result.add(mol.getName());
                result.add(mol.getState());
                result.add(mol.getLabel());
                result.add(String.valueOf(mol.getEdgeWildcard()));
                result.add(mol.getMoleculeComponents().toString());
                result.add(mol.toString());
                assertEquals(test[1], result.toString());
            }
            catch(IllegalArgumentException e)
            {
                assertEquals(test[1], e.getMessage());
            }
        }
    }

    public void testConnectedSubgraphs()
    {
        String[][] tests = new String[][] {
                new String[] {"A(b!1).B(a!1).C(c)", "[A(b!1).B(a!1), C(c)]"},
                new String[] {"R(l!8).L(r!8,w!6).W(l!6,z!3).Z(w!3).A(b!2,b!1).B(a!1).B(a!2)",
                        "[A(b!1,b!2).B(a!1).B(a!2), L(r!1,w!2).R(l!1).W(l!2,z!3).Z(w!3)]"},};

        for( String test[] : tests )
        {
            BionetgenSpeciesGraph bsg = new BionetgenSpeciesGraph(test[0]);
            List<BionetgenSpeciesGraph> temp = bsg.getConnectedSubgraphs(false);
            List<String> result = new ArrayList<>();
            for( BionetgenSpeciesGraph bsg1 : temp )
                result.add(bsg1.toString());
            assertEquals(test[1], result.toString());
        }

    }

    public void testSpeciesGraph() throws Exception
    {
        String[][] tests = new String[][] {
                new String[] {"A(a!1!+,b!3,c!+).L(r!1,d!?)~P.D()%T",
                        "[[A(a!+!1,b!+,c!+), D%T(), L~P(d!?,r!1)], A(a!+!1,b!+,c!+).D%T().L~P(d!?,r!1)]"},
                new String[] {"Dimer%D::R(l!1,l!?).L(r!1,r!2).R(l!2,l!3).L(r!3,r!?)",
                        "[[L(r!1,r!2), L(r!3,r!?), R(l!1,l!?), R(l!2,l!3)], Dimer%D::L(r!1,r!2).L(r!3,r!?).R(l!1,l!?).R(l!2,l!3)]"},
                new String[] {"Trimer:A(a!1!2).A(a!2!3).A(a!3!1)", "[[A(a!1!2), A(a!1!3), A(a!2!3)], Trimer:A(a!1!2).A(a!1!3).A(a!2!3)]"},
                new String[] {"%A::L(l,r)", "[[L(l,r)], %A::L(l,r)]"},
                new String[] {"Trimer:Dimer::A(a)", "Improper syntax of Species Graph header: 'Trimer:Dimer::A(a)'"},
                new String[] {":Dimer::A(a)", "Improper syntax of Species Graph header: ':Dimer::A(a)'"},
                new String[] {"Dimer:", "Improper syntax of Species Graph header: 'Dimer:'"},
                new String[] {"R(l!8,a!5)..L(r!8,w!3)",
                        "Improper syntax of Species Graph: two or more molecules separators ('.') in a row at 'R(l!8,a!5)..L(r!8,w!3)'"},
                new String[] {"R(l!8,a!5).L(r!8,w!3).A(r!5,w!7).W(l!3,a!7)",
                        "[[A(r!1,w!2), L(r!3,w!4), R(a!1,l!3), W(a!2,l!4)], A(r!1,w!2).L(r!3,w!4).R(a!1,l!3).W(a!2,l!4)]"},
                new String[] {"A(b!2,b!1).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)",
                        "[[A(b!1,b!2), B(a!1,c!3), B(a!2,d!4), C(b!3), D(b!4)], A(b!1,b!2).B(a!1,c!3).B(a!2,d!4).C(b!3).D(b!4)]"},};

        for( String[] test : tests )
        {
            try
            {
                BionetgenSpeciesGraph graph = new BionetgenSpeciesGraph(test[0]);
                List<String> result = new ArrayList<>();
                result.add(graph.getMoleculesListAsString());
                result.add(graph.toString());
                assertEquals(test[1], result.toString());
            }
            catch( IllegalArgumentException e )
            {
                assertEquals(test[1], e.getMessage());
            }
        }
    }

}
