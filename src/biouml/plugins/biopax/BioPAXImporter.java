package biouml.plugins.biopax;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.semanticweb.owl.model.OWLOntology;

import biouml.model.Diagram;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection;
import biouml.plugins.biopax.reader.BioPAXReaderFactory;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.util.ExProperties;

public class BioPAXImporter implements DataElementImporter
{
    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if(parent != null && !parent.isAcceptable( FolderCollection.class ))
            return ACCEPT_UNSUPPORTED;
        if(file != null)
        {
            OWLOntology ontology = BioPAXReaderFactory.getOntology( file );
            if( ontology != null && BioPAXReaderFactory.getBioPAXVersion( ontology ) != null )
            {
                return ACCEPT_HIGH_PRIORITY;
            }
            return ACCEPT_UNSUPPORTED;
        }
        return ACCEPT_LOW_PRIORITY;
    }


    @Override
    public DataElement doImport(@Nonnull DataCollection<?> parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if(jobControl != null)
        {
            jobControl.functionStarted();
        }
        Properties properties = new ExProperties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "biopax" );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, file.getAbsolutePath() );
        BioPaxOwlDataCollection owl = new BioPaxOwlDataCollection( null, properties );
        FunctionJobControl fjc = jobControl == null ? new FunctionJobControl( null ) : new SubFunctionJobControl( jobControl, 0, 50 );
        final JobControlException[] ex = new JobControlException[1];
        fjc.addListener( new JobControlListenerAdapter() {
            @Override
            public void jobTerminated(JobControlEvent event)
            {
                ex[0] = event.getException();
            }
        });
        owl.initCollection( fjc );
        if(ex[0] != null)
        {
            throw ExceptionRegistry.translateException( ex[0].getError() );
        }
        @SuppressWarnings ( "unchecked" )
        DataCollection<Diagram> diagrams = (DataCollection<Diagram>)owl.get( "Diagrams" );
        if(diagrams == null)
        {
            throw new BiosoftParseException( new Exception("Error reading BioPAX"), elementName );
        }
        if(jobControl != null)
        {
            jobControl.setPreparedness( 50 );
            if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
            {
                return null;
            }
        }
        DataCollection<?> folder = DataCollectionUtils.createSubCollection( parent.getCompletePath().getChildPath( elementName ) );
        int i=0;
        for(Diagram diagram : diagrams)
        {
            CollectionFactoryUtils.save( diagram.clone( folder, diagram.getName() ) );
            if(jobControl != null)
            {
                i++;
                jobControl.setPreparedness( 50+50*i/diagrams.getSize() );
                if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                {
                    return folder;
                }
            }
        }
        if(jobControl != null)
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return folder;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        return null;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return FolderCollection.class;
    }
}
