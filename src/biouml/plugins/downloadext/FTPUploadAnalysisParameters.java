package biouml.plugins.downloadext;

import java.util.Objects;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.ImporterFormat;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.util.TextUtil2;

public class FTPUploadAnalysisParameters extends AbstractAnalysisParameters
{

    private String fileURL;
    private DataElementPath resultPath;
    private ImporterFormat importerFormat;
    private Object importerProperties;

    private void updateProperties()
    {
        setImporterFormat( new ImporterFormat( "Generic file" ) );
    }

    public FTPUploadAnalysisParameters()
    {
        super();
        resultPath = CollectionFactoryUtils.getUserProjectsPath();
        updateProperties();
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"fileURL"};
    }

   @Override
    public void read(Properties properties, String prefix)
    {
       ComponentModel model = ComponentFactory.getModel(this);
       for( int i = 0; i < model.getPropertyCount(); i++ )
       {
           Property property = model.getPropertyAt(i);
           String propertyName = property.getName();
           if(propertyName.equals("importerProperties") && getImporterProperties() != null)
           {
               ComponentModel imodel = ComponentFactory.getModel(getImporterProperties());
               for( int j = 0; j < imodel.getPropertyCount(); j++ )
               {
                   Property iproperty = imodel.getPropertyAt(j);
                   if( !properties.containsKey(prefix + "importer." + iproperty.getName()) )
                       continue;
                   String valueStr = properties.getProperty(prefix + "importer." + iproperty.getName());
                   try
                   {
                       Object value = TextUtil2.fromString(iproperty.getValueClass(), valueStr);
                       if(value != null)
                       {
                           iproperty.setValue(value);
                       }
                   }
                   catch( Exception e )
                   {
                   }
               }
           }
           else
           {
               if( !properties.containsKey(prefix + property.getName()) )
                   continue;
               String valueStr = properties.getProperty(prefix + property.getName());
               try
               {
                   Object value = TextUtil2.fromString(property.getValueClass(), valueStr);
                   if(value != null)
                   {
                       property.setValue(value);
                   }
               }
               catch( Exception e )
               {
               }
           }
       }
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        ComponentModel model = ComponentFactory.getModel(this);
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt(i);
            if( property.getValue() != null )
            {
                String propertyName = property.getName();
                if(propertyName.equals("importerProperties") && getImporterProperties() != null)
                {
                    ComponentModel ipmodel = ComponentFactory.getModel(getImporterProperties());
                    for( int j = 0; j < ipmodel.getPropertyCount(); j++ )
                    {
                        Property ipproperty = ipmodel.getPropertyAt(j);
                        if( ipproperty.getValue() != null )
                        {
                            Object ipvalue = ipproperty.getValue();
                            String ipvalueStr = ipvalue.toString();
                            try
                            {
                                ipvalueStr = (String)ipvalue.getClass().getMethod("getStringRepresentation").invoke(ipvalue);
                            }
                            catch( Exception e )
                            {
                            }
                            properties.put(prefix + "importer." + ipproperty.getName(), ipvalueStr);
                        }
                    }
                }
                else
                {
                    Object value = property.getValue();
                    String valueStr = value.toString();
                    try
                    {
                        valueStr = (String)value.getClass().getMethod("getStringRepresentation").invoke(value);
                    }
                    catch( Exception e )
                    {
                    }
                    properties.put(prefix + property.getName(), valueStr);
                }
            }
        }
    }

    public String getFileURL()
    {
        return fileURL;
    }

    public void setFileURL(String fileURL)
    {
        Object oldValue = this.fileURL;
        this.fileURL = fileURL;
        firePropertyChange("fileURL", oldValue, fileURL);
    }

    public String getImportFormat()
    {
        return importerFormat.getFormat();
    }

    public ImporterFormat getImporterFormat()
    {
        return importerFormat;
    }

    public void setImporterFormat(ImporterFormat importerFormat)
    {
        Object oldValue = this.importerFormat;
        this.importerFormat = importerFormat;
        firePropertyChange( "importerFormat", oldValue, this.importerFormat );
        if( !Objects.equals( oldValue, importerFormat ) )
            setImporterProperties( DataElementImporterRegistry.getImporterProperties( importerFormat.getFormat() ) );
    }

    public Object getImporterProperties()
    {
        return importerProperties;
    }

    public void setImporterProperties(Object importerProperties)
    {
        Object oldValue = this.importerProperties;
        this.importerProperties = importerProperties;
        if( importerProperties != null )
        {
            ComponentModel model = ComponentFactory.getModel(this);
            ComponentFactory.recreateChildProperties(model);
        }
        firePropertyChange("importerProperties", oldValue, this.importerProperties);
    }

    public boolean isPropertiesHidden()
    {
        return importerProperties == null;
    }

    public DataElementPath getResultPath()
    {
        return resultPath;
    }

    public void setResultPath(DataElementPath resultPath)
    {
        Object oldValue = this.resultPath;
        this.resultPath = resultPath;
        firePropertyChange("resultPath", oldValue, this.resultPath);
        updateProperties();
    }
    
    public String getOutputIcon()
    {
        ImporterInfo importerInfo = DataElementImporterRegistry.getImporterInfo( importerFormat.getFormat() );
        return importerInfo == null ? null : IconFactory.getClassIconId(importerInfo.getImporter().getResultType());
    }
}
