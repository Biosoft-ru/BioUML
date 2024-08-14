package biouml.plugins.bionetgen._test;

import java.awt.Point;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import biouml.model.Compartment;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenUtils;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;

public class BasicBionetgenTest extends AbstractBionetgenTest
{
    protected static final String FILE_PATH_EX_1 = dir + "test_examples_1/bionetgen_ex_1.bngl";
    protected static final String FILE_PATH_EX_2 = dir + "test_examples_2/bionetgen_ex_2.bngl";

    protected static final String FILE_PATH_RES_1_1 = dir + "test_examples_1/bionetgen_res_1_1.bngl";
    protected static final String FILE_PATH_RES_1_2 = dir + "test_examples_1/bionetgen_res_1_2.bngl";
    protected static final String FILE_PATH_RES_1_3 = dir + "test_examples_1/bionetgen_res_1_3.bngl";
    protected static final String FILE_PATH_RES_1_4 = dir + "test_examples_1/bionetgen_res_1_4.bngl";
    //protected static final String FILE_PATH_RES_1_5 = dir + "test_examples_1/bionetgen_res_1_5.bngl";
    //protected static final String FILE_PATH_RES_1_6 = dir + "test_examples_1/bionetgen_res_1_6.bngl";
    protected static final String FILE_PATH_RES_1_7 = dir + "test_examples_1/bionetgen_res_1_7.bngl";
    protected static final String FILE_PATH_RES_1_8 = dir + "test_examples_1/bionetgen_res_1_8.bngl";
    protected static final String FILE_PATH_RES_1_9 = dir + "test_examples_1/bionetgen_res_1_9.bngl";
    protected static final String FILE_PATH_RES_1_10 = dir + "test_examples_1/bionetgen_res_1_10.bngl";
    protected static final String FILE_PATH_RES_1_11 = dir + "test_examples_1/bionetgen_res_1_11.bngl";
    protected static final String FILE_PATH_RES_1_12 = dir + "test_examples_1/bionetgen_res_1_12.bngl";
    protected static final String FILE_PATH_RES_1_13 = dir + "test_examples_1/bionetgen_res_1_13.bngl";
    protected static final String FILE_PATH_RES_1_14 = dir + "test_examples_1/bionetgen_res_1_14.bngl";
    protected static final String FILE_PATH_RES_1_15 = dir + "test_examples_1/bionetgen_res_1_15.bngl";
    protected static final String FILE_PATH_RES_1_16 = dir + "test_examples_1/bionetgen_res_1_16.bngl";
    protected static final String FILE_PATH_RES_1_17 = dir + "test_examples_1/bionetgen_res_1_17.bngl";
    protected static final String FILE_PATH_RES_1_18 = dir + "test_examples_1/bionetgen_res_1_18.bngl";
    protected static final String FILE_PATH_RES_1_19 = dir + "test_examples_1/bionetgen_res_1_19.bngl";

    protected static final String FILE_PATH_RES_2_1 = dir + "test_examples_2/bionetgen_res_2_1.bngl";
    protected static final String FILE_PATH_RES_2_2 = dir + "test_examples_2/bionetgen_res_2_2.bngl";
    protected static final String FILE_PATH_RES_2_3 = dir + "test_examples_2/bionetgen_res_2_3.bngl";
    protected static final String FILE_PATH_RES_2_4 = dir + "test_examples_2/bionetgen_res_2_4.bngl";
    protected static final String FILE_PATH_RES_2_5 = dir + "test_examples_2/bionetgen_res_2_5.bngl";
    protected static final String FILE_PATH_RES_2_6 = dir + "test_examples_2/bionetgen_res_2_6.bngl";
    protected static final String FILE_PATH_RES_2_7 = dir + "test_examples_2/bionetgen_res_2_7.bngl";
    protected static final String FILE_PATH_RES_2_8 = dir + "test_examples_2/bionetgen_res_2_8.bngl";
    protected static final String FILE_PATH_RES_2_9 = dir + "test_examples_2/bionetgen_res_2_9.bngl";

    public BasicBionetgenTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( BasicBionetgenTest.class.getName() );

        suite.addTest( new BasicBionetgenTest( "testAddSeedSpecies1" ) );
        suite.addTest( new BasicBionetgenTest( "testAddSeedSpecies2" ) );
        suite.addTest( new BasicBionetgenTest( "testAddObservable1" ) );
        suite.addTest( new BasicBionetgenTest( "testAddObservable2" ) );
        suite.addTest( new BasicBionetgenTest( "testAddObservable3" ) );
        suite.addTest( new BasicBionetgenTest( "testAddObservable4" ) );
        suite.addTest( new BasicBionetgenTest( "testAddReactionRule1" ) );
        suite.addTest( new BasicBionetgenTest( "testAddReactionRule2" ) );
        suite.addTest( new BasicBionetgenTest( "testAddInitialAssignment" ) );
        suite.addTest( new BasicBionetgenTest( "testAddMolecule" ) );
        suite.addTest( new BasicBionetgenTest( "testAddMoleculeComponent" ) );
        suite.addTest( new BasicBionetgenTest( "testAddMoleculeType1" ) );
        suite.addTest( new BasicBionetgenTest( "testAddMoleculeType2" ) );
        suite.addTest( new BasicBionetgenTest( "testAddParameter1" ) );
        suite.addTest( new BasicBionetgenTest( "testAddParameter2" ) );
        suite.addTest( new BasicBionetgenTest( "testAddNotelink" ) );

        suite.addTest( new BasicBionetgenTest( "testRemoveSeedSpecies" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveObservable" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveReactionRule" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveInitialAssignment" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveMolecule" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveMoleculeComponent" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveMoleculeType" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveParameter" ) );
        suite.addTest( new BasicBionetgenTest( "testRemoveNotelink" ) );

        return suite;
    }

    //----------------------------------------------------------------
    //add seed species when block exists
    public void testAddSeedSpecies1() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        Node species = createNewNode( diagram, "A(b!1).B(a!1)", SPECIES_START_TYPE );
        diagram.put( species );

        compareResult( FILE_PATH_RES_1_1, 1 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        isConstant = true;
        species = createNewNode( diagram, "A(b!1).B(a!1)", SPECIES_START_TYPE );
        isConstant = false;
        diagram.put( species );

        compareResult( FILE_PATH_RES_1_11, 2 );
    }

    //add seed species when block doesn't exist
    public void testAddSeedSpecies2() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        Node species = createNewNode( diagram, "A(b!1).B(a!1)", SPECIES_START_TYPE );
        diagram.put( species );

        compareResult( FILE_PATH_RES_2_1, 1 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        isConstant = true;
        species = createNewNode( diagram, "A(b!1).B(a!1)", SPECIES_START_TYPE );
        isConstant = false;
        diagram.put( species );

        compareResult( FILE_PATH_RES_2_5, 2 );
    }

    //add observable when block exists
    public void testAddObservable1() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        Node observableNode = createNewNode( diagram, "newObservable", BionetgenConstants.TYPE_OBSERVABLE );
        observableNode.getAttributes().setValue( BionetgenConstants.MATCH_ONCE_ATTR, Boolean.FALSE );
        diagram.put( observableNode );

        compareResult( FILE_PATH_RES_1_2 );
    }

    //add observable when block exists
    public void testAddObservable2() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        Node observableNode = createNewNode( diagram, "newObservable", BionetgenConstants.TYPE_OBSERVABLE );
        observableNode.getAttributes().setValue( BionetgenConstants.MATCH_ONCE_ATTR, Boolean.TRUE );
        diagram.put( observableNode );

        compareResult( FILE_PATH_RES_1_3 );
    }

    //add observable when block doesn't exist
    public void testAddObservable3() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        Node observableNode = createNewNode( diagram, "newObservable", BionetgenConstants.TYPE_OBSERVABLE );
        observableNode.getAttributes().setValue( BionetgenConstants.MATCH_ONCE_ATTR, Boolean.FALSE );
        diagram.put( observableNode );

        compareResult( FILE_PATH_RES_2_2 );
    }

    //add observable when block doesn't exist
    public void testAddObservable4() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        Node observableNode = createNewNode( diagram, "newObservable", BionetgenConstants.TYPE_OBSERVABLE );
        observableNode.getAttributes().setValue( BionetgenConstants.MATCH_ONCE_ATTR, Boolean.TRUE );
        diagram.put( observableNode );

        compareResult( FILE_PATH_RES_2_3 );
    }

    public void testAddReactionRule1() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        List<SpecieReference> components = createReactionComponents();

        Node reactionNode = createAndPutReactionNode( components, "10" );
        reactionNode.getAttributes().setValue( BionetgenConstants.REVERSIBLE_ATTR, true );
        reactionNode.getAttributes().setValue( BionetgenConstants.BACKWARD_RATE_ATTR, "20" );
        postprocessReaction( reactionNode, BionetgenConstants.DEFAULT, true );

        compareResult( FILE_PATH_RES_1_4, 1 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        reactionNode = createAndPutReactionNode( components, BionetgenConstants.MM + "(10,20)" );
        postprocessReaction( reactionNode, BionetgenConstants.MM, false );

        compareResult( FILE_PATH_RES_1_12, 2 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        reactionNode = createAndPutReactionNode( components, BionetgenConstants.SATURATION + "(10,20)" );
        postprocessReaction( reactionNode, BionetgenConstants.SATURATION, false );

        compareResult( FILE_PATH_RES_1_13, 3 );
    }

    public void testAddReactionRule2() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );
        createOtherTypeSpecies();

        List<SpecieReference> components = createReactionComponents();

        Node reactionNode = createAndPutReactionNode( components, "10" );
        postprocessReaction( reactionNode, BionetgenConstants.DEFAULT, false );

        compareResult( FILE_PATH_RES_2_4, 1 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );
        createOtherTypeSpecies();

        reactionNode = createAndPutReactionNode( components, BionetgenConstants.MM + "(10,10)" );
        postprocessReaction( reactionNode, BionetgenConstants.MM, false );

        compareResult( FILE_PATH_RES_2_6, 2 );

        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );
        createOtherTypeSpecies();

        reactionNode = createAndPutReactionNode( components, BionetgenConstants.SATURATION + "(10,10)" );
        postprocessReaction( reactionNode, BionetgenConstants.SATURATION, false );

        compareResult( FILE_PATH_RES_2_7, 3 );
    }

    public void testAddInitialAssignment() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        diagram.setNotificationEnabled( false );
        Node eqNode = controller.createNodeInstance( diagram, BionetgenConstants.TYPE_EQUATION, BionetgenConstants.EQUATION_NAME,
                new Point( 0, 0 ), null );
        Equation eq = new Equation( eqNode, Equation.TYPE_INITIAL_ASSIGNMENT, "k2", "10*k1/5/2*(11+2-3)" );
        eqNode.setRole( eq );
        diagram.setNotificationEnabled( true );
        diagram.put( eqNode );

        compareResult( FILE_PATH_RES_1_7, 1 );

        diagram.setNotificationEnabled( false );
        eqNode = controller.createNodeInstance( diagram, BionetgenConstants.TYPE_EQUATION, BionetgenConstants.EQUATION_NAME, new Point( 0,
                0 ), null );
        eq = new Equation( eqNode, Equation.TYPE_INITIAL_ASSIGNMENT, "$\"B(a)\"", "k2/k1+1" );
        eqNode.setRole( eq );
        diagram.setNotificationEnabled( true );
        diagram.put( eqNode );

        compareResult( FILE_PATH_RES_1_18, 2 );
    }

    public void testAddMolecule() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        Compartment species = (Compartment)diagram.findNode( "A(b)" );
        assertNotNull( species );
        Node newMolecule = createNewNode( species, "Z", BionetgenConstants.TYPE_MOLECULE );
        species.put( newMolecule );

        compareResult( FILE_PATH_RES_1_8 );
    }

    public void testAddMoleculeComponent() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_8 ) );

        Compartment parent = (Compartment)diagram.findNode( "A(b)_Z" );
        Compartment molecule = (Compartment)parent.findNode( "Z" );
        assertNotNull( molecule );
        Node molCompNode = createNewNode( molecule, "a!+", BionetgenConstants.TYPE_MOLECULE_COMPONENT );
        molecule.put( molCompNode );

        compareResult( FILE_PATH_RES_1_9, 1 );

        molecule = (Compartment)parent.findNode( "A" );
        assertNotNull( molecule );
        molCompNode = createNewNode( molecule, "z!+", BionetgenConstants.TYPE_MOLECULE_COMPONENT );
        molecule.put( molCompNode );

        compareResult( FILE_PATH_RES_1_10, 2 );
    }

    public void testAddMoleculeType1() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        Node node = createNewNode( diagram, "A(b)", BionetgenConstants.TYPE_MOLECULETYPE );
        diagram.put( node );
        node = createNewNode( diagram, "B(a)", BionetgenConstants.TYPE_MOLECULETYPE );
        node.setComment( "molecule type comment" );
        diagram.put( node );
        node = createNewNode( diagram, "C(c~uP~P)", BionetgenConstants.TYPE_MOLECULETYPE );
        diagram.put( node );

        compareResult( FILE_PATH_RES_1_14, 1 );
    }

    public void testAddMoleculeType2() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        Node node = createNewNode( diagram, "B(a)", BionetgenConstants.TYPE_MOLECULETYPE );
        diagram.put( node );

        compareResult( FILE_PATH_RES_2_8 );
    }

    public void testAddParameter1() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_1 ) );

        EModel emodel = diagram.getRole( EModel.class );
        Variable var = new Variable( "k3", emodel, emodel.getVariables() );
        var.getAttributes().add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "P1" ) );
        var.setConstant( true );
        emodel.put( var );

        compareResult( FILE_PATH_RES_1_17 );
    }

    public void testAddParameter2() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_EX_2 ) );

        EModel emodel = diagram.getRole( EModel.class );
        Variable var = new Variable( "k3", emodel, emodel.getVariables() );
        var.getAttributes().add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "P1" ) );
        var.setConstant( true );
        emodel.put( var );

        compareResult( FILE_PATH_RES_2_9 );
    }

    public void testAddNotelink() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_19 ) );

        Compartment complex = (Compartment)diagram.findNode( "A(b)_B(a)" );
        Node in = complex.findNode( "b" );
        Node out = complex.findNode( "a" );
        assertNotNull( in );
        assertNotNull( out );
        Stub kernel = new Stub( null, BionetgenUtils.generateUniqueName( complex, BionetgenConstants.EDGE_NAME ), BionetgenConstants.TYPE_EDGE );
        Edge newEdge = new Edge( complex, kernel, in, out );
        in.addEdge( newEdge );
        out.addEdge( newEdge );
        complex.put( newEdge );

        compareResult( FILE_PATH_RES_1_1 );
    }

    //----------------------------------------------------------------
    public void testRemoveSeedSpecies() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_1 ) );

        diagram.remove( "A(b!1)_B(a!1)" );

        compareResult( FILE_PATH_EX_1 );
    }

    public void testRemoveObservable() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_2 ) );

        diagram.remove( "observable_1" );

        compareResult( FILE_PATH_EX_1 );
    }

    public void testRemoveReactionRule() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_4 ) );

        diagram.remove( "j02" );

        compareResult( FILE_PATH_EX_1 );
    }

    public void testRemoveInitialAssignment() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_18 ) );

        diagram.findNode( "B(a)" ).getRole( VariableRole.class ).setInitialValue( 5 );
        diagram.remove( BionetgenConstants.EQUATION_NAME + "_2" );

        compareResult( FILE_PATH_RES_1_7, 1 );

        diagram.remove( BionetgenConstants.EQUATION_NAME );

        compareResult( FILE_PATH_EX_1, 2 );
    }

    public void testRemoveMolecule() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_8 ) );

        Compartment parent = (Compartment)diagram.findNode( "A(b)_Z" );
        Compartment molecule = (Compartment)parent.findNode( "Z" );
        parent.remove( molecule.getName() );

        compareResult( FILE_PATH_EX_1 );
    }

    public void testRemoveMoleculeComponent() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_10 ) );

        Compartment parent = (Compartment)diagram.findNode( "A(b,z!+)_Z(a!+)" );
        Compartment molecule = (Compartment)parent.findNode( "A" );
        Node moleculeComponent = molecule.findNode( "z" );
        molecule.remove( moleculeComponent.getName() );

        compareResult( FILE_PATH_RES_1_9, 1 );

        molecule = (Compartment)parent.findNode( "Z" );
        moleculeComponent = molecule.findNode( "a" );
        molecule.remove( moleculeComponent.getName() );

        compareResult( FILE_PATH_RES_1_8, 2 );
    }

    public void testRemoveMoleculeType() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_14 ) );

        diagram.remove( BionetgenConstants.MOLECULE_TYPE_NAME + "_1" );

        compareResult( FILE_PATH_RES_1_15, 1 );

        diagram.remove( BionetgenConstants.MOLECULE_TYPE_NAME );

        compareResult( FILE_PATH_RES_1_16, 2 );
    }

    public void testRemoveParameter() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_17 ) );

        diagram.getRole( EModel.class ).getVariables().remove( "k3" );

        compareResult( FILE_PATH_EX_1 );
    }

    public void testRemoveNotelink() throws Exception
    {
        preprocess( BionetgenTestUtility.readFile( FILE_PATH_RES_1_1 ) );

        ( (Compartment)diagram.findNode( "A(b!1)_B(a!1)" ) ).remove( BionetgenConstants.EDGE_NAME );

        compareResult( FILE_PATH_RES_1_1 );
    }
}
