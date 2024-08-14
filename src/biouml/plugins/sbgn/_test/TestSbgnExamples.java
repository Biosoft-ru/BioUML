package biouml.plugins.sbgn._test;

import java.io.File;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbml.SbmlEModel;
import biouml.plugins.sbml.SbmlExporter;
import biouml.plugins.sbml.SbmlExporter.SbmlExportProperties;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.sbml.validation.SBMLValidator;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class TestSbgnExamples extends AbstractBioUMLTest
{
    public TestSbgnExamples(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSbgnExamples.class.getName());
        suite.addTest(new TestSbgnExamples("test"));
        return suite;
    }

    public void test() throws Exception
    {
        DataCollection<?> collection = getExampleCollection();
        assertNotNull(collection);
        assertTrue(collection.getSize() == 6);
        
        for (Object object: collection)
        {
            assert(object instanceof Diagram);
            doTest((Diagram)object);
        }
    }
    public void doTest( Diagram diagram) throws Exception
    {
        assertNotNull(diagram);
        assertTrue ( diagram.getType() instanceof SbgnDiagramType );

        checkExport(diagram , true);
        
        CompositeModelPreprocessor preproccessor = new CompositeModelPreprocessor();
        Diagram plainDiagram = preproccessor.preprocess(diagram);

        assertNotNull(plainDiagram);
        assertTrue ( plainDiagram.getRole() instanceof SbmlEModel );

        checkDiagramConsistency(plainDiagram);

        //test restore
        Diagram sbmlDiagram = SBGNConverterNew.restore(plainDiagram);
        
        checkDiagramConsistency(sbmlDiagram);
    }
    
    public static void checkExport(Diagram diagram , boolean inlineSubModels) throws Exception
    {
        File testFile = AbstractBioUMLTest.getTestFile(diagram.getName());
        SbmlExporter exporter = new SbmlExporter();
        SbmlExportProperties properties = ((SbmlExportProperties)exporter.getProperties(diagram, testFile));
        properties.setInlineModels(true);
        properties.setSaveBioUMLAnnotation( false );
        exporter.doExport(diagram, testFile);
        String report = SBMLValidator.validateSBML(testFile);
        if (report != null)
        {
            System.out.println( "Error during "+diagram.getName()+" validating");
            System.out.println( report );            
        }
        assertTrue (report == null);
    }

    public static void checkDiagramConsistency(Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        for( VariableRole varRole : emodel.getVariableRoles() )
        {
            DiagramElement de = varRole.getDiagramElement();
            assert ( de instanceof Node );
            assert ( varRole.equals( de.getRole() ) );

            boolean mainDeFound = false;
            for( DiagramElement associated : varRole.getAssociatedElements() )
            {
                assert ( associated instanceof Node );
                assert ( varRole.equals( associated.getRole() ) );

                if( associated.equals(de) )
                    mainDeFound = true;
            }

            assert ( mainDeFound );
        }

        for( Node reactionNode : diagram.recursiveStream().select(Node.class).filter(node -> node.getKernel() instanceof Reaction) )
        {
            Reaction reaction = (Reaction)reactionNode.getKernel();
            for( SpecieReference sr : reaction.getSpecieReferences() )
            {
                String specie = sr.getSpecie();
                if( specie == null )
                    System.out.println("Specie Reference" + sr.getName());
                Node node = diagram.findNode(specie);
                if( node == null )
                    System.out.println("Can not found " + node.getName());
                assertNotNull(node);
            }
        }
    }


    public static Diagram getExampleDiagram(String name) throws Exception
    {
        return getDiagram(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION, name);
    }

    public static DataCollection<?> getExampleCollection() throws Exception
    {
        return getCollection(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION);
    }
    
    public static DataCollection<?> getCollection(String repositoryPath, String collectionName) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        return CollectionFactory.getDataCollection(collectionName);
    }

    public static Diagram getDiagram(String repositoryPath, String collectionName, String name) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> collection1 = CollectionFactory.getDataCollection( collectionName );
        DataElement de = collection1.get(name);
        return (Diagram)de;
    }

    public static final String DATA_RESOURCES_REPOSITORY = "../data_resources";
    public static final String EXAMPLE_DIAGRAMS_COLLECTION = "data/Examples/SBGN/Diagrams";
}
