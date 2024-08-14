package biouml.plugins.bionetgen._test;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenReactionRecord;
import biouml.plugins.bionetgen.diagram.BionetgenSpeciesGraph;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramDeployer;
import biouml.plugins.bionetgen.diagram.PermutationList;
import biouml.plugins.bionetgen.diagram.ReactionTemplate;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

import com.developmentontheedge.beans.DynamicProperty;

public class ReactionTemplateTest extends TestCase
{
    public ReactionTemplateTest(String name)
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
        TestSuite suite = new TestSuite(ReactionTemplateTest.class.getName());

        suite.addTest(new ReactionTemplateTest("testAddBind"));
        suite.addTest(new ReactionTemplateTest("testDelBind"));
        suite.addTest(new ReactionTemplateTest("testAddMol"));
        suite.addTest(new ReactionTemplateTest("testDelMol"));
        suite.addTest(new ReactionTemplateTest("testStateChange"));
        suite.addTest(new ReactionTemplateTest("testAddition"));
        suite.addTest(new ReactionTemplateTest("testMultiplier1"));
        suite.addTest(new ReactionTemplateTest("testMultiplier2"));
        suite.addTest(new ReactionTemplateTest("testMultiplier3"));
        suite.addTest(new ReactionTemplateTest("testMultiplier4"));
        suite.addTest(new ReactionTemplateTest("testRateFormula"));

        return suite;
    }

    public void testAddBind() throws Exception
    {
        Reaction reactionAddBind = new Reaction(null, "r1");

        addSR(reactionAddBind, "A", SpecieReference.REACTANT, "A(b,c!?)");
        addSR(reactionAddBind, "B", SpecieReference.REACTANT, "B(a)");
        addSR(reactionAddBind, "AB", SpecieReference.PRODUCT, "A(b!1,c!?).B(a!1)");

        String[][] testsAddBind = new String[][] {new String[] {"A(b,c)", "B(a)", "[A(b!1,c).B(a!1)]"},
                new String[] {"A(b,c!1).C(a!1)", "B(a)", "[A(b!1,c!2).B(a!1).C(a!2)]"},};

        ReactionTemplate brt = new ReactionTemplate(reactionAddBind);
        for( String[] testAddBind : testsAddBind )
        {
            testExecute(brt, testAddBind);
        }
    }

    public void testDelBind() throws Exception
    {
        Reaction reactionDelBind = new Reaction(null, "r2");

        addSR(reactionDelBind, "AB", SpecieReference.REACTANT, "A(b!1,c!?).B(a!1)");
        addSR(reactionDelBind, "A", SpecieReference.PRODUCT, "A(b,c!?)");
        addSR(reactionDelBind, "B", SpecieReference.PRODUCT, "B(a)");

        String[][] testsDelBind = new String[][] {new String[] {"A(b!1,c).B(a!1)", "[A(b,c), B(a)]"},
                new String[] {"A(b!1,c!2).B(a!1).C(a!2)", "[A(b,c!1).C(a!1), B(a)]"},};

        ReactionTemplate brt = new ReactionTemplate(reactionDelBind);
        for( String[] testDelBind : testsDelBind )
        {
            testExecute(brt, testDelBind);
        }
    }

    public void testDelMol() throws Exception
    {
        Reaction reactionDelMol = new Reaction(null, "r3");

        addSR(reactionDelMol, "AB", SpecieReference.REACTANT, "A(b!1,c!?).B(a!1)");
        addSR(reactionDelMol, "B", SpecieReference.REACTANT, "B(a)");
        addSR(reactionDelMol, "A", SpecieReference.PRODUCT, "A(b,c!?)");

        String[][] testsDelMol = new String[][] {new String[] {"A(b!1,c).B(a!1)", "B(a)", "[A(b,c)]"},
                new String[] {"A(b!1,c!2).B(a!1).C(a!2)", "B(a)", "[A(b,c!1).C(a!1)]"},};

        ReactionTemplate brt = new ReactionTemplate(reactionDelMol);
        for( String[] testDelMol : testsDelMol )
        {
            testExecute(brt, testDelMol);
        }

    }

    public void testAddMol() throws Exception
    {
        Reaction reactionAddMol = new Reaction(null, "r4");

        addSR(reactionAddMol, "A", SpecieReference.REACTANT, "A(b,c!?)");
        addSR(reactionAddMol, "AB", SpecieReference.PRODUCT, "A(b!1,c!?).B(a!1,d!2!3).D(b!2).D(b!3)");

        String[][] testsAddMol = new String[][] {new String[] {"A(b,c)", "[A(b!1,c).B(a!1,d!2!3).D(b!2).D(b!3)]"},
                new String[] {"A(b,c!2).C(a!2)", "[A(b!1,c!2).B(a!1,d!3!4).C(a!2).D(b!3).D(b!4)]"},};

        ReactionTemplate brt = new ReactionTemplate(reactionAddMol);
        for( String[] testAddMol : testsAddMol )
        {
            testExecute(brt, testAddMol);
        }

    }

    public void testStateChange() throws Exception
    {
        Reaction reactionStateChange = new Reaction(null, "r5");

        addSR(reactionStateChange, "Au", SpecieReference.REACTANT, "A(a~uP,c!?)");
        addSR(reactionStateChange, "Ap", SpecieReference.PRODUCT, "A(a~P,c!?)");

        String[][] testsStateChange = new String[][] {new String[] {"A(a~uP,c)", "[A(a~P,c)]"},
                new String[] {"A(a~uP,c!1).C(a!1)", "[A(a~P,c!1).C(a!1)]"},};

        ReactionTemplate brt = new ReactionTemplate(reactionStateChange);
        for( String[] testStateChange : testsStateChange )
        {
            testExecute(brt, testStateChange);
        }
    }

    public void testAddition() throws Exception
    {
        String[] addition = new String[] {"exclude_reactants(1,D)", "include_products(1,E,F)"};

        Reaction reaction = new Reaction(null, "r6");
        reaction.getAttributes().add(new DynamicProperty(BionetgenConstants.ADDITION_ATTR, String.class, addition));

        addSR(reaction, "A", SpecieReference.REACTANT, "A(b,c!?)");
        addSR(reaction, "B", SpecieReference.REACTANT, "B(a)");
        addSR(reaction, "AB", SpecieReference.PRODUCT, "A(b!1,c!?).B(a!1)");

        String[][] tests1 = new String[][] {new String[] {"A(b,c)", "B(a,e!1).E(b!1)", "[A(b!1,c).B(a!1,e!2).E(b!2)]"},
                new String[] {"A(b,c!1).C(a!1)", "B(a,e!1).E(b!1)", "[A(b!1,c!2).B(a!1,e!3).C(a!2).E(b!3)]"},
                new String[] {"A(b,c!1).C(a!1)", "B(a)", "[No products]"},
                new String[] {"A(b,c!1).C(a!1,d!2).D(c!2)", "B(a)", "[No reactants]"},
                new String[] {"A(b,c!1).C(a!1)", "B(a,f!1).F(b!1)", "[A(b!1,c!2).B(a!1,f!3).C(a!2).F(b!3)]"},};

        ReactionTemplate brt = new ReactionTemplate(reaction);
        for( String[] testAddition : tests1 )
        {
            testExecute(brt, testAddition);
        }
    }

    public void testMultiplier1() throws Exception
    {
        List<String> data = new ArrayList<>();
        //(toy-jim) j02 & revj02
        Reaction reaction = new Reaction(null, "j02");
        reaction.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "kpD"));
        //reversed reaction
        Reaction revReaction = new Reaction(null, "revj02");
        revReaction.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "kmD"));

        addSR(reaction, "LR", SpecieReference.REACTANT, "L(r!1).R(l!1,r)");
        addSR(reaction, "RL", SpecieReference.REACTANT, "R(l!1,r).L(r!1)");
        addSR(reaction, "LLRR", SpecieReference.PRODUCT, "L(r!1).R(l!1,r!3).L(r!2).R(l!2,r!3)");
        addSR(revReaction, "LLRR", SpecieReference.REACTANT, "L(r!1).R(l!1,r!3).L(r!2).R(l!2,r!3)");
        addSR(revReaction, "LR", SpecieReference.PRODUCT, "L(r!1).R(l!1,r)");
        addSR(revReaction, "RL", SpecieReference.PRODUCT, "R(l!1,r).L(r!1)");

        String[] speciesStrings = new String[] {"A(k,r)", "K(Y~U,a)", "L(r)", "R(a,l,r)", "L(r!4).R(a,l!4,r)",
                "L(r!4).L(r!5).R(a,l!4,r!6).R(a,l!5,r!6)", "A(k,r!1).R(a!1,l,r)", "A(k,r!1).L(r!2).R(a!1,l!2,r)",
                "A(k,r!1).L(r!2).L(r!3).R(a,l!3,r!4).R(a!1,l!2,r!4)", "A(k!1,r).K(Y~U,a!1)", "A(k!2,r!3).K(Y~U,a!2).R(a!3,l,r)",
                "A(k!8,r!9).K(Y~U,a!8).L(r!10).R(a!9,l!10,r)", "A(k!4,r!5).K(Y~U,a!4).L(r!6).L(r!7).R(a,l!7,r!8).R(a!5,l!6,r!8)",
                "A(k,r!1).A(k,r!2).L(r!3).L(r!4).R(a!1,l!3,r!5).R(a!2,l!4,r!5)",
                "A(k,r!1).A(k!2,r!3).K(Y~U,a!2).L(r!4).L(r!5).R(a!1,l!5,r!6).R(a!3,l!4,r!6)",
                "A(k!1,r!2).A(k!3,r!4).K(Y~U,a!1).K(Y~U,a!3).L(r!5).L(r!6).R(a!2,l!6,r!7).R(a!4,l!5,r!7)",
                "A(k!1,r!2).A(k!3,r!4).K(Y~P,a!1).K(Y~U,a!3).L(r!5).L(r!6).R(a!2,l!6,r!7).R(a!4,l!5,r!7)",
                "A(k!1,r!2).A(k!3,r!4).K(Y~P,a!1).K(Y~P,a!3).L(r!5).L(r!6).R(a!2,l!6,r!7).R(a!4,l!5,r!7)",
                "A(k!1,r!2).K(Y~P,a!1).L(r!3).R(a!2,l!3,r)", "A(k!1,r).K(Y~P,a!1)",
                "A(k!1,r!2).K(Y~P,a!1).L(r!3).L(r!4).R(a,l!3,r!5).R(a!2,l!4,r!5)", "K(Y~P,a)",
                "A(k,r!1).A(k!2,r!3).K(Y~P,a!2).L(r!4).L(r!5).R(a!1,l!4,r!6).R(a!3,l!5,r!6)", "A(k!2,r!3).K(Y~P,a!2).R(a!3,l,r)"};

        List<BionetgenSpeciesGraph> species = new ArrayList<>();
        for( String specieString : speciesStrings )
        {
            species.add(new BionetgenSpeciesGraph(specieString));
        }

        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();

        createReactionRecords(reaction, species, reactionRecords);
        createReactionRecords(revReaction, species, reactionRecords);

        data.add(String.valueOf(reactionRecords.size()));

        for( BionetgenReactionRecord rr : reactionRecords )
        {
            data.add(rr.getForwardRate());
        }
        String expected = "[20, 0.5*kpD, kpD, kpD, kpD, 0.5*kpD, kpD, kpD, 0.5*kpD, kpD,"
                + " 0.5*kpD, kmD, kmD, kmD, kmD, kmD, kmD, kmD, kmD, kmD, kmD]";
        assertEquals(expected, data.toString());
    }

    public void testMultiplier2() throws Exception
    {
        List<String> data = new ArrayList<>();
        //for blbr revj02
        //R(r) + L(l,l!+) <-> R(r!1).L(l!1,l!+)
        Reaction r2 = new Reaction(null, "revj02(blbr)");
        r2.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "km2"));

        addSR(r2, "LR", SpecieReference.REACTANT, "R(r!1).L(l!1,l!+)");
        addSR(r2, "R", SpecieReference.PRODUCT, "R(r)");
        addSR(r2, "L", SpecieReference.PRODUCT, "L(l,l!+)");

        String[] speciesStrings = new String[] {"L(l,l)", "R(r,r)", "L(l,l!1).R(r,r!1)", "L(l!1,l!2).R(r,r!1).R(r,r!2)",
                "L(l,l!1).L(l!2,l!3).R(r,r!3).R(r!2,r!1)", "L(l!1,l!2).R(r!1,r!2)", "L(l!1,l!2).L(l!3,l!4).R(r!1,r!4).R(r!2,r!3)",
                "L(l,l!1).L(l,l!2).R(r!1,r!2)", "L(l,l!1).L(l,l!2).L(l!3,l!4).R(r!1,r!4).R(r!2,r!3)",
                "L(l!1,l!2).L(l!3,l!4).R(r,r!2).R(r,r!4).R(r!1,r!3)", "L(l,l!1).L(l!2,l!3).L(l!4,l!5).R(r,r!5).R(r!2,r!1).R(r!3,r!4)",
                "L(l!1,l!2).L(l!3,l!4).L(l!5,l!6).R(r,r!4).R(r,r!6).R(r!1,r!3).R(r!2,r!5)",
                "L(l,l!1).L(l!2,l!3).L(l!4,l!5).L(l!6,l!7).R(r,r!7).R(r!1,r!5).R(r!2,r!4).R(r!3,r!6)",
                "L(l,l!1).L(l,l!2).L(l!3,l!4).L(l!5,l!6).R(r!1,r!5).R(r!3,r!6).R(r!4,r!2)",
                "L(l!1,l!2).L(l!3,l!4).L(l!5,l!6).R(r!1,r!6).R(r!2,r!3).R(r!4,r!5)",
                "L(l!1,l!2).L(l!3,l!4).L(l!5,l!6).L(l!7,l!8).R(r!1,r!8).R(r!2,r!6).R(r!3,r!5).R(r!4,r!7)"};
        List<BionetgenSpeciesGraph> species = new ArrayList<>();
        for( String specie : speciesStrings )
        {
            species.add(new BionetgenSpeciesGraph(specie));
        }
        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();
        createReactionRecords(r2, species, reactionRecords);

        data.add(String.valueOf(reactionRecords.size()));

        for( BionetgenReactionRecord rr : reactionRecords )
        {
            data.add(rr.getForwardRate());
        }
        String expected = "[19, 2.0*km2, km2, km2, 2.0*km2, 2.0*km2, 2.0*km2, 2.0*km2, km2, km2, 2.0*km2,"
                + " 2.0*km2, 2.0*km2, km2, km2, km2, 2.0*km2, km2, 2.0*km2, 2.0*km2]";
        assertEquals(expected, data.toString());
    }

    public void testMultiplier3() throws Exception
    {
        List<String> data = new ArrayList<>();

        Reaction reaction = new Reaction(null, "testReaction");
        reaction.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "kf"));

        addSR(reaction, "R1", SpecieReference.REACTANT, "A");
        addSR(reaction, "R2", SpecieReference.REACTANT, "A");
        addSR(reaction, "R3", SpecieReference.REACTANT, "A");
        addSR(reaction, "P1", SpecieReference.PRODUCT, "B");
        addSR(reaction, "P2", SpecieReference.PRODUCT, "B");
        addSR(reaction, "P3", SpecieReference.PRODUCT, "B");

        String[] speciesStrings = new String[] {"A", "B"};
        List<BionetgenSpeciesGraph> species = new ArrayList<>();
        for( String specie : speciesStrings )
        {
            species.add(new BionetgenSpeciesGraph(specie));
        }
        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();
        createReactionRecords(reaction, species, reactionRecords);

        data.add(String.valueOf(reactionRecords.size()));

        for( BionetgenReactionRecord rr : reactionRecords )
        {
            data.add(rr.getForwardRate());
        }
        String expected = "[1, 0.16666666666666666*kf]";
        assertEquals(expected, data.toString());
    }

    public void testMultiplier4() throws Exception
    {
        List<String> data = new ArrayList<>();

        Reaction reaction = new Reaction(null, "testReaction");
        reaction.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "kf"));

        addSR(reaction, "R1", SpecieReference.REACTANT, "L(l,l,l)");
        addSR(reaction, "R2", SpecieReference.REACTANT, "L(l,l,l)");
        addSR(reaction, "P1", SpecieReference.PRODUCT, "L(l!1,l,l).L(l!1,l,l)");

        String[] speciesStrings = new String[] {"L(l,l,l)", "L(l!1,l,l).L(l!1,l,l)"};
        List<BionetgenSpeciesGraph> species = new ArrayList<>();
        for( String specie : speciesStrings )
        {
            species.add(new BionetgenSpeciesGraph(specie));
        }
        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();
        createReactionRecords(reaction, species, reactionRecords);

        data.add(String.valueOf(reactionRecords.size()));

        for( BionetgenReactionRecord rr : reactionRecords )
        {
            data.add(rr.getForwardRate());
        }
        String expected = "[1, 4.5*kf]";
        assertEquals(expected, data.toString());
    }

    public void testRateFormula() throws Exception
    {
        String expected = "[Sat_1($Species1,k1*(k2+k3),((K2-K1)/K3)), Sat_2($Species0,$Species1,k1*(k2+k3),"
                + "((K2-K1)/K3)), MM($Species0,$Species1,k1*(k2+k3),((K2-K1)/K3))]";
        String[] test = new String[] {"S", "E", "P"};
        List<BionetgenSpeciesGraph> species = new ArrayList<>();
        for( String specie : test )
        {
            species.add(new BionetgenSpeciesGraph(specie));
        }
        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();

        Reaction reaction = new Reaction(null, "r1");
        //1st Saturation test
        reaction.getAttributes().add(
                new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "Sat(k1*(k2+k3),((K2-K1)/K3))"));
        reaction.getAttributes().add(
                new DynamicProperty( BionetgenConstants.RATE_LAW_TYPE_PD, String.class, BionetgenConstants.SATURATION ) );

        addSR(reaction, "E1", SpecieReference.REACTANT, "E", 1);
        addSR(reaction, "P", SpecieReference.PRODUCT, "P", 2);
        addSR(reaction, "E2", SpecieReference.PRODUCT, "E", 3);

        createReactionRecords(reaction, species, reactionRecords);

        //2nd Saturation test
        addSR(reaction, "S", SpecieReference.REACTANT, "S", 0);
        createReactionRecords(reaction, species, reactionRecords);

        //MM test
        reaction.getAttributes().remove(BionetgenConstants.FORWARD_RATE_ATTR);
        reaction.getAttributes().remove(BionetgenConstants.RATE_LAW_TYPE_ATTR);
        reaction.getAttributes().add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, "MM(k1*(k2+k3),((K2-K1)/K3))"));
        reaction.getAttributes().add( new DynamicProperty( BionetgenConstants.RATE_LAW_TYPE_PD, String.class, BionetgenConstants.MM ) );
        createReactionRecords(reaction, species, reactionRecords);

        List<String> data = new ArrayList<>();
        for( BionetgenReactionRecord reactionRecord : reactionRecords )
        {
            data.add(reactionRecord.generateFormula());
        }
        assertEquals(expected, data.toString());
    }

    private void createReactionRecords(Reaction reaction, List<BionetgenSpeciesGraph> species, List<BionetgenReactionRecord> reactionRecords)
            throws Exception
    {
        ReactionTemplate rt = new ReactionTemplate(reaction);
        List<List<BionetgenSpeciesGraph>> allReactants = rt.getReactantSets(species);
        int counter = 0;
        for( List<BionetgenSpeciesGraph> reactantSet : BionetgenDiagramDeployer.withoutPermutations( new PermutationList<>(
                allReactants), null) )
        {
            TIntList reactIndexes = BionetgenReactionRecord.getIndexes(reactantSet, species);
            List<List<BionetgenSpeciesGraph>> allProducts = rt.executeReaction(reactantSet, null);
            for( List<BionetgenSpeciesGraph> products : allProducts )
            {
                if( products == null )
                    continue;
                TIntList prodIndexes = new TIntArrayList();
                for( BionetgenSpeciesGraph product : products )
                {
                    int index = -1;
                    for( BionetgenSpeciesGraph node : species )
                    {
                        if( node.isomorphicTo(product) )
                        {
                            index = species.indexOf(node);
                            break;
                        }
                    }
                    prodIndexes.add(index);
                }
                BionetgenReactionRecord reactionRecord = new BionetgenReactionRecord(rt.getName() + "_" + ( ++counter ), reactIndexes,
                        prodIndexes, rt.getForwardRate(), rt.getRateLawType());
                if( reactionRecord.needMultipliers() )
                {
                    reactionRecord.addMultiplier(rt.getLastMultiplier(allProducts.indexOf(products)));
                }
                reactionRecords.add(reactionRecord);
            }
        }
    }

    private void testExecute(ReactionTemplate brt, String[] test) throws Exception
    {
        List<BionetgenSpeciesGraph> reactants = new ArrayList<>();
        for( String testStr : test )
        {
            if( testStr.endsWith("]") )
                break;
            reactants.add(new BionetgenSpeciesGraph(testStr));
        }
        List<List<BionetgenSpeciesGraph>> allReactants = new PermutationList<>( brt.getReactantSets( reactants ) );
        if( allReactants.size() == 0 )
        {
            assertEquals(test[test.length - 1], "[No reactants]");
            return;
        }
        List<List<BionetgenSpeciesGraph>> allProducts = brt.executeReaction( ( allReactants ).get(0), null);
        if( allProducts.size() == 0 )
        {
            assertEquals(test[test.length - 1], "[No products]");
            return;
        }
        List<String> products = new ArrayList<>();
        for( BionetgenSpeciesGraph bsg : allProducts.get(0) )
            products.add(bsg.toString());
        assertEquals(test[test.length - 1] + " vs " + products.toString(), test[test.length - 1], products.toString());
    }

    private void addSR(Reaction reaction, String name, String role, String specie, int reactantNumber) throws Exception
    {
        SpecieReference ref = new SpecieReference(null, name, role);
        ref.setTitle("");
        ref.setSpecie(specie);
        ref.getAttributes().add(new DynamicProperty(BionetgenConstants.REACTANT_NUMBER_ATTR, Integer.class, reactantNumber));
        reaction.put(ref);
    }

    private void addSR(Reaction reaction, String name, String role, String specie) throws Exception
    {
        addSR(reaction, name, role, specie, reaction.getSize());
    }
}