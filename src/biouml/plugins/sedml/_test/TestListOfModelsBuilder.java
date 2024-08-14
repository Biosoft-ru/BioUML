package biouml.plugins.sedml._test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jlibsedml.Libsedml;
import org.jlibsedml.SEDMLDocument;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.research.workflow.WorkflowDiagramType;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.sedml.ListOfModelsBuilder;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;

public class TestListOfModelsBuilder extends AbstractBioUMLTest
{

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.registerRoot( new FolderVectorCollection( "live", null ) );
        TestSedMlImporter.setupGraphicNotations();
    }

    public void testSedml12() throws Exception
    {
        WorkflowDiagramType workflowDiagramType = new WorkflowDiagramType();
        Diagram workflow = workflowDiagramType.createDiagram( null, "test", null );
        WorkflowSemanticController controller = (WorkflowSemanticController)workflowDiagramType.getSemanticController();

        ListOfModelsBuilder builder = new ListOfModelsBuilder( workflow, controller );
        SEDMLDocument sedmlDoc = Libsedml.readDocument( getFile( "test_sedml12.xml" ) );
        builder.setSedml( sedmlDoc.getSedMLModel() );

        builder.build();

        Map<String, Diagram> models = builder.getResultingDiagrams();
        assertEquals( "Number of models", 2, models.size() );
        assertNotNull( "Changed model converted to Diagram", models.get( "model2" ) );
        assertEquals( "Original model value", 5e-4, models.get( "model1" ).getRole( EModel.class ).getVariableValue( "ps_0" ) );
        assertEquals( "Changed model value", 1.3e-5, models.get( "model2" ).getRole( EModel.class ).getVariableValue( "ps_0" ) );
    }

    private File getFile(String name) throws IOException
    {
        File file = getTestFile( name );
        InputStream is = getClass().getClassLoader().getResourceAsStream( "biouml/plugins/sedml/_test/resources/" + name );
        ApplicationUtils.copyStream( new FileOutputStream( file ), is );
        return file;
    }
}
