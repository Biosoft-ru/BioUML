package biouml.standard.simulation;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;

import biouml.standard.simulation.access.SimulationResultTransformer;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class SimulationResultImporter implements DataElementImporter
{

    @Override
    public int accept(DataCollection parent, File file)
    {        
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() ) )
            return ACCEPT_UNSUPPORTED;
        return ( file != null )
                ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }

        String baseName = elementName == null || elementName.equals( "" ) ? file.getName().replaceFirst( "\\.[^\\.]+$", "" ) : elementName;
        String name = baseName;
        while( parent.get( name ) != null )
        {
            log.warning( "File with name "+elementName+" already exists. Imoport denied.");
            return null;
        }

        SimulationResultTransformer transformer = new SimulationResultTransformer();
        transformer.init(null, parent);
        SimulationResult result = transformer.load( file, elementName, parent );
        parent.put( result );
        
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return parent.get( name );
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
        return SimulationResult.class;
    }
}
