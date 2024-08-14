package biouml.plugins.sbgn;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.access.OptimizationReader;
import biouml.plugins.sbml.MessageBundle;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

public class OptimizationImporter implements DataElementImporter
{
    protected static final Logger log = Logger.getLogger( OptimizationImporter.class.getName() );

    private OptimizationImportProperties properties;
    protected Map<String, String> newPaths;

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent != null && DataCollectionUtils.isAcceptable( parent, getResultType() ) && parent.isMutable() )
            return ACCEPT_LOW_PRIORITY;  
        return ACCEPT_UNSUPPORTED;
    }
   
    @Override
    public OptimizationImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new OptimizationImportProperties(  );
        return properties;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties." )
    public static class OptimizationImportProperties extends Option
    {
        
    }

    public static class OptimizationImportPropertiesBeanInfo extends BeanInfoEx2<OptimizationImportProperties>
    {
        public OptimizationImportPropertiesBeanInfo()
        {
            super( OptimizationImportProperties.class, MessageBundle.class.getName() );
        }
    }

    @Override
    public DataElement doImport(DataCollection parent, File file, String elementName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        DataElement result = null;
        try
        {
            try (FileInputStream fis = new FileInputStream( file ))
            {
                OptimizationReader reader = new OptimizationReader( elementName, fis );
                reader.setNewPaths( newPaths );
                result = reader.read( parent );
                parent.put( result );
            }
        }
        catch( Exception e )
        {
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError( e );
                return null;
            }
            throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return result;
    }


    @Override
    public boolean init(Properties properties)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    { 
        return Optimization.class;
    }
}
