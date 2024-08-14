package biouml.plugins.bionetgen._test;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramType;
import biouml.plugins.bionetgen.diagram.BionetgenEditor;
import biouml.plugins.bionetgen.diagram.BionetgenSemanticController;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SpecieReference;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public abstract class AbstractBionetgenTest extends AbstractBioUMLTest
{
    protected static final String dir = "../src/biouml/plugins/bionetgen/_test/test_suite/models/";

    public AbstractBionetgenTest(String name)
    {
        super( name );
    }
    protected static final String SPECIES_START_TYPE = "start";

    protected BionetgenEditor editor;
    protected @Nonnull Diagram diagram = new Diagram( null, new DiagramInfo( "diagramTest" ), new BionetgenDiagramType() );
    protected BionetgenSemanticController controller;
    protected String[] content = new String[] {"A(b!1).B(a!1)", "A(b)", "A(b!?)"};
    protected String[] addition = new String[] {"include_reactants(2,B)", "exclude_reactants(1,A)"};
    protected boolean isConstant = false;

    protected Node createNewNode(@Nonnull Compartment parent, String bngName, String type) throws Exception
    {
        Node node;
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        if( BionetgenConstants.TYPE_OBSERVABLE.equals( type ) )
        {
            node = controller.createNodeInstance( parent, type, bngName, new Point( 0, 0 ), dps );
            node.setTitle( bngName );
            node.getAttributes().setValue( BionetgenConstants.CONTENT_ATTR, content );
        }
        else if( BionetgenConstants.TYPE_MOLECULETYPE.equals( type ) )
        {
            dps.add( new DynamicProperty( BionetgenConstants.MOLECULE_TYPE_ATTR, String.class, bngName ) );
            node = controller.createNodeInstance( parent, type, "", new Point( 0, 0 ), dps );
        }
        else if( BionetgenConstants.TYPE_MOLECULE.equals( type ) )
        {
            dps.add( new DynamicProperty( BionetgenConstants.MOLECULE_ATTR, String.class, bngName ) );
            node = controller.createNodeInstance( parent, type, bngName, new Point( 0, 0 ), dps );
        }
        else if( BionetgenConstants.TYPE_MOLECULE_COMPONENT.equals( type ) )
        {
            node = controller.createNodeInstance( parent, type, bngName, new Point( 0, 0 ), dps );
        }
        else
        {
            boolean isStartType = SPECIES_START_TYPE.equals( type );
            dps.add( new DynamicProperty( BionetgenConstants.GRAPH_ATTR, String.class, bngName ) );
            dps.add( new DynamicProperty( BionetgenConstants.IS_SEED_SPECIES_PD, Boolean.class, isStartType ) );
            node = controller.createNodeInstance( parent, BionetgenConstants.TYPE_SPECIES, bngName, new Point( 0, 0 ), dps );
            VariableRole role = node.getRole( VariableRole.class );
            node.setRole( role );
            role.setConstant( isConstant );
            if( isStartType )
                role.setInitialValue( 25.0 );
        }
        return node;
    }

    protected void preprocess(String bionetgenText) throws Exception
    {
        BionetgenTestUtility.initPreferences();
        //create diagram
        diagram = new Diagram( null, new DiagramInfo( "diagramTest" ), new BionetgenDiagramType() );
        EModel emodel = new EModel( diagram );
        diagram.setRole( emodel );

        controller = (BionetgenSemanticController)diagram.getType().getSemanticController();

        //create editor
        editor = new BionetgenEditor();
        editor.explore( diagram, null );

        //add text and change diagram
        editor.setText( bionetgenText );
        Diagram generatedDiagram = editor.getBionetgen().generateDiagram( bionetgenText );
        assertNotNull( generatedDiagram );
        diagram = generatedDiagram;
        emodel = diagram.getRole( EModel.class );
        diagram.addDataCollectionListener( editor );
        diagram.addPropertyChangeListener( editor );
        emodel.getVariables().addDataCollectionListener( editor );
    }

    protected void compareResult(String fileName) throws Exception
    {
        compareResult( fileName, 0 );
    }

    protected void compareResult(String fileName, int number) throws Exception
    {
        String message = number != 0 ? "Subtest #" + number + " failed" : "";
        File testFile = BionetgenTestUtility.createTestFile( dir, editor.getText() );
        assertFileEquals( message, new File( fileName ), testFile );
        assertTrue( "Failed to delete temporary file", testFile.delete() );
    }

    protected Node createAndPutReactionNode(List<SpecieReference> components, String formula) throws Exception
    {
        List<DiagramElement> elements = controller.createReactionElements( diagram, "newReaction", formula, components, new Point( 0, 0 ) );
        Node reactionNode = StreamEx.of( elements ).select( Node.class ).peek( diagram::put ).findFirst().get();
        StreamEx.of( elements ).select( Edge.class ).forEach( diagram::put );
        return reactionNode;
    }

    protected List<SpecieReference> createReactionComponents()
    {
        List<SpecieReference> components = new ArrayList<>();
        SpecieReference sr = new SpecieReference( null, "newRef1", SpecieReference.PRODUCT );
        sr.setSpecie( "C(c~uP)" );
        components.add( sr );
        sr = new SpecieReference( null, "newRef2", SpecieReference.REACTANT );
        sr.setSpecie( "C(c~P)" );
        components.add( sr );
        sr = new SpecieReference( null, "newRef3", SpecieReference.PRODUCT );
        sr.setSpecie( "A(b)" );
        components.add( sr );
        return components;
    }

    protected void postprocessReaction(Node reactionNode, String expected, boolean isReversibleExpected)
    {
        DynamicPropertySet attributes = reactionNode.getAttributes();
        assertEquals( "Reaction was not validated", expected, attributes.getValueAsString( BionetgenConstants.RATE_LAW_TYPE_ATTR ) );
        assertEquals( isReversibleExpected, (boolean)attributes.getValue( BionetgenConstants.REVERSIBLE_ATTR ) );
        attributes.add( new DynamicProperty( BionetgenConstants.ADDITION_ATTR, String[].class, addition ) );
    }

    protected void createOtherTypeSpecies() throws Exception
    {
        Node species = createNewNode( diagram, "C(c~uP)", "" );
        diagram.put( species );
        species = createNewNode( diagram, "C(c~P)", "" );
        diagram.put( species );
        species = createNewNode( diagram, "A(b)", "" );
        diagram.put( species );
    }

}