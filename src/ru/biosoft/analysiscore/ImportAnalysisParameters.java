package ru.biosoft.analysiscore;

import java.io.File;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TextUtil;

public class ImportAnalysisParameters extends AbstractAnalysisParameters
{
    private File file;
    private DataElementPath resultPath;
    private Object properties;
    private DataElementImporter importer;
    private ImporterInfo importerInfo;
    
    private DataElementImporter getImporter()
    {
        if(importer == null) importer = importerInfo.cloneImporter();
        return importer;
    }
    
    private void updateProperties()
    {
        if(resultPath == null)
            setProperties(getImporter().getProperties(null, file, null));
        else
            setProperties(getImporter().getProperties(resultPath.optParentCollection(), file, resultPath.getName()));
    }

    private String getFileName(File f)
    {
        String name = (f instanceof FileItem)?((FileItem)f).getOriginalName():f.getName();
        return name==null?null:ApplicationUtils.getFileNameWithoutExtension(name);
    }

    public ImportAnalysisParameters(ImporterInfo importerInfo)
    {
        super();
        this.importerInfo = importerInfo;
        resultPath = CollectionFactoryUtils.getUserProjectsPath();
        updateProperties();
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[]{"file"};
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        setResultPath(DataElementPath.create(properties.getProperty(prefix+"resultPath")));
        if(getProperties() != null)
        {
            ComponentModel model = ComponentFactory.getModel(getProperties());
            for( Property property : BeanUtil.properties( model ) )
            {
                if( !properties.containsKey(prefix + "importer." + property.getName()) )
                    continue;
                String valueStr = properties.getProperty(prefix + "importer." + property.getName());
                try
                {
                    property.setValue(TextUtil.fromString(property.getValueClass(), valueStr));
                }
                catch( NoSuchMethodException e )
                {
                }
            }
        }
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        if(getResultPath() != null)
            properties.setProperty(prefix+"resultPath", getResultPath().toString());
        if(getProperties() != null)
        {
            ComponentModel model = ComponentFactory.getModel(getProperties());
            BeanUtil.properties( model ).mapToEntry( prop -> prefix + "importer." + prop.getName(), Property::getValue )
                .nonNullValues().mapValues( TextUtil::toString ).forKeyValue( properties::put );
        }
    }

    public File getFile()
    {
        return file;
    }
    
    public void setFile(File file)
    {
        File oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, this.file);
        String itemName = getFileName(file);
        if( getResultPath() != null
                && ( getResultPath().getName().equals("") || ( oldValue != null && getResultPath().getName().equals(
                        getFileName(oldValue)) ) ) )
        {
            setResultPath(getResultPath().getSiblingPath(itemName));
        }
        updateProperties();
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

    public Object getProperties()
    {
        return properties;
    }

    public void setProperties(Object properties)
    {
        Object oldValue = this.properties;
        this.properties = properties;
        if(properties != null)
        {
            ComponentModel model = ComponentFactory.getModel(this);
            ComponentFactory.recreateChildProperties(model);
        }
        firePropertyChange("properties", oldValue, this.properties);
    }

    public ImporterInfo getImporterInfo()
    {
        return importerInfo;
    }
    
    public boolean isPropertiesHidden()
    {
        return properties == null;
    }
    
    public String getOutputIcon()
    {
        return IconFactory.getClassIconId(getImporter().getResultType());
    }
}
