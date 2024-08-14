package biouml.plugins.sedml._test;

import java.io.File;
import java.util.logging.Logger;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Preferences;

import biouml.model.Diagram;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.plugins.research.workflow.engine.WorkflowEngineListener;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.sedml.SedmlExporter;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.journal.JournalRegistry;

public class AbstractSedmlTest extends AbstractBioUMLTest
{
    protected static final Logger log = Logger.getLogger( AbstractSedmlTest.class.getName() );

    protected FolderVectorCollection root;

    public AbstractSedmlTest()
    {
    }

    public AbstractSedmlTest(String name)
    {
        super(name);
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        root = new FolderVectorCollection( "root", null );
        CollectionFactory.registerRoot( root );
        root.put( new FolderVectorCollection( "tmp", root ) );
        WorkflowEngine.setTmpCollection( DataElementPath.create( "root/tmp" ) );
        setupGraphicNotations();
    }

    public static void setupGraphicNotations() throws Exception
    {
        Preferences preferences = Application.getPreferences();
        if( preferences == null )
            Application.setPreferences( preferences = new Preferences() );
        Preferences globalPreferences = (Preferences)preferences.getValue( "Global" );
        if( globalPreferences == null )
            preferences.addValue( "Global", globalPreferences = new Preferences() );
        globalPreferences.addValue( XmlDiagramType.NOTATION_PATH_PROPERTY, "Diagrams/graphic notations" );
        DataCollection<?> diagrams = CollectionFactory.createRepository( "../data/Utils/Diagrams" );
        CollectionFactory.registerRoot( diagrams );
    }

    private boolean finished;
    private String error;

    protected void runWorkflow(Diagram workflow) throws Exception
    {
        DynamicPropertySet parameters = WorkflowItemFactory.getWorkflowParameters( workflow );
        parameters.getProperty( "Output folder" ).setValue( DataElementPath.create( "root/results" ) );
        runWorkflow( workflow, parameters );
    }

    protected void runWorkflow(Diagram workflow, DynamicPropertySet parameters) throws Exception, InterruptedException
    {
        JournalRegistry.setJournalUse( false );
        WorkflowEngine engine = new WorkflowEngine();
        engine.setWorkflow( workflow );
        engine.setParameters( parameters );
        engine.setLogger( log );
        engine.initWorkflow();
        finished = false;
        engine.addEngineListener( new WorkflowEngineListener()
        {
            @Override
            public void stateChanged()
            {
            }

            @Override
            public void started()
            {
            }

            @Override
            public void resultsReady(Object[] results)
            {
            }

            @Override
            public void parameterErrorDetected(String error)
            {
                AbstractSedmlTest.this.error = error;
            }

            @Override
            public void finished()
            {
                finished = true;
            }

            @Override
            public void errorDetected(String error)
            {
                AbstractSedmlTest.this.error = error;
                finished = true;
            }
        } );
        engine.start();
        while( !finished )
            Thread.sleep( 100 );
        if( error != null )
            fail( error );
    }

    protected void exportSedml(File exportedFile, Diagram workflow) throws Exception
    {
        SedmlExporter exporter = new SedmlExporter();
        exporter.doExport( workflow, exportedFile );
    }
}
