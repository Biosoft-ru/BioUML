package biouml.plugins.sedml;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import org.jlibsedml.SEDMLDocument;
import org.jlibsedml.SedML;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import biouml.model.Diagram;
import biouml.plugins.research.workflow.WorkflowDiagramType;

import ru.biosoft.jobcontrol.FunctionJobControl;

public class SedmlExporter implements DataElementExporter
{

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof Diagram && ( (Diagram)de ).getType() instanceof WorkflowDiagramType )
        {
            DataCollection<?> parent = de.getOrigin();
            if( parent == null )
                return ACCEPT_LOW_PRIORITY;
            String plugins = parent.getInfo().getProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY );
            if( StreamEx.split( plugins, ';' ).has( "biouml.plugins.sedml" ) )
                return ACCEPT_HIGH_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( Diagram.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport( de, file, null );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        try
        {
            Diagram workflow = (Diagram)de;
            SEDMLDocument sedml = exportToSedml( workflow );
            sedml.writeDocument( file );
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            throw e;
        }
        if( jobControl != null )
            jobControl.functionFinished();
    }

    private SEDMLDocument exportToSedml(Diagram workflow) throws Exception
    {
        SEDMLDocument document = new SEDMLDocument();

        SedML sedml = document.getSedMLModel();

        ListOfModelsParser modelsParser = new ListOfModelsParser( workflow, sedml );
        modelsParser.parse();
        
        ListOfTasksParser tasksParser = new ListOfTasksParser( workflow, sedml );
        tasksParser.setModelNodes( modelsParser.getModelNodes() );
        tasksParser.parse();
        
        ListOfOutputsParser outputsParser = new ListOfOutputsParser( workflow, sedml );
        outputsParser.parse();

        /* Validator doesn't support repeatedTask
        document.validate();
        if( document.hasErrors() )
        {
            String message = StreamEx.of( document.getErrors() ).map( Object::toString ).joining( "\n" );
            throw new Exception( "Invalid sedml: " + message );
        }
        */
        return document;
    }

    

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

}
