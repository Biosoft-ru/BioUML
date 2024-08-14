package biouml.plugins.graphml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;

/**
 * Exports {@link Diagram} to GraphML format
 */
public class GraphMLExporter extends DiagramExporter
{
    private GraphMLExporterProperties properties;
    
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram != null;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        boolean useYSchema = properties != null  &&  properties.isUseYSchema();
        try(OutputStream os = new FileOutputStream(file))
        {
            new GraphMLWriter().writeGraph(diagram, os, useYSchema);
        }
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }
    
    @Override
    public Object getProperties(DataElement de, File file)
    {
        properties = new GraphMLExporterProperties();
        return properties;
    }
    
    public static class GraphMLExporterProperties extends Option
    {
        private boolean useYSchema = true;
        
        public GraphMLExporterProperties()
        {
        }

        public boolean isUseYSchema()
        {
            return useYSchema;
        }

        public void setUseYSchema(boolean useYSchema)
        {
            this.useYSchema = useYSchema;
        }
    }

    public static class GraphMLExporterPropertiesBeanInfo extends BeanInfoEx
    {
        public GraphMLExporterPropertiesBeanInfo()
        {
            super(GraphMLExporterProperties.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_EXPORT_PROPERTIES"));
            beanDescriptor.setShortDescription(getResourceString("CD_EXPORT_PROPERTIES"));
        }
        @Override
        public void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("useYSchema", beanClass), getResourceString("PN_USE_YFILES"),
                    getResourceString("PD_USE_YFILES"));
        }
    }
}
