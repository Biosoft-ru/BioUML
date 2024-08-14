package biouml.plugins.sbgn;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;

import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.access.OptimizationWriter;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * Exports optimization into archive with all related data elements
 */
public class OptimizationExporter implements DataElementExporter
{
    protected static final Logger log = Logger.getLogger( OptimizationExporter.class.getName() );

    private Option emptyProperties;
    protected Map<String, String> newPaths = new HashMap<>();
    
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }
    
    @Override
    public int accept(DataElement de)
    {
        if( de instanceof Optimization )
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;

    }

    @Override
    public void doExport(@Nonnull DataElement dataElement, @Nonnull File file) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( file ))
        {
            OptimizationWriter writer = new OptimizationWriter( fos );
            writer.setNewPaths( newPaths );
            writer.write( (Optimization)dataElement );
        }
    }

    @Override
    public Object getProperties(DataElement de, File file)
    {
        emptyProperties = new OptimizationExporterProperties();
        return emptyProperties;
    }

    public static class OptimizationExporterProperties extends Option
    {
        
    }

    public static class OptimizationExporterPropertiesBeanInfo extends BeanInfoEx2<OptimizationExporterProperties>
    {
        public OptimizationExporterPropertiesBeanInfo()
        {
            super( OptimizationExporterProperties.class );
        }
    }

    @Override
    public void doExport(DataElement de, File file, FunctionJobControl jobControl) throws Exception
    {
        this.doExport( de, file );
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( DataCollection.class );
    }
}