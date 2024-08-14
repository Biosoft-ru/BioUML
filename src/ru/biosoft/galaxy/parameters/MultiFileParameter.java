package ru.biosoft.galaxy.parameters;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.galaxy.FormatRegistry;
import ru.biosoft.galaxy.FormatRegistry.ExporterResult;
import ru.biosoft.galaxy.GalaxyDataCollection;

/**
 * @author lan
 *
 */
public class MultiFileParameter extends BaseFileParameter
{
    private static final Logger log = Logger.getLogger( MultiFileParameter.class.getName() );
    protected DataElementPathSet lastValue = null;
    protected boolean isExported = false;
    
    public MultiFileParameter(boolean output)
    {
        super(output);
        fields.put("id", 0);
    }
    
    @Override
    public boolean exportFile()
    {
        if(isExported && getFile().exists()) return true;
        if(lastValue == null) return false;
        Object galaxyFormat = getAttributes().get("format");
        ApplicationUtils.removeDir(getFile());
        getFile().mkdirs();
        DataElementPathSet value = lastValue;
        if(!value.isEmpty() && DataCollectionUtils.checkPrimaryElementType(value.first().optDataCollection(), FolderCollection.class))
        {
            value = value.first().getChildren();
        }
        int i = 0;
        for(DataElementPath path: value)
        {
            DataElement de = path.optDataElement();
            if( de == null )
            {
                log.warning("Data element not exists " + path);
                return false;
            }
            ExporterResult exporterInfo = FormatRegistry.getExporter(galaxyFormat.toString(), path, getFormatProperties());
            
            if( exporterInfo == null )
            {
                log.warning( "Exporter for " + galaxyFormat + " not found" );
                return false;
            }
            DataElementExporter exporter = exporterInfo.exporter;
            getAttributes().put("usedFormat", exporterInfo.galaxyFormat);
            File file = new File(getFile(), path.getName().replaceAll("\\W", "_") + (++i) + "." + getExtension());
            file.delete();
            if( exporter == null ) return false;
            try
            {
                exporter.doExport(de, file);
            }
            catch( Exception e )
            {
                log.log(Level.WARNING, "Can not export " + path + " to file " +  file.getAbsolutePath(), e);
                return false;
            }
            if(!file.exists()) return false;
        }
        isExported = true;
        return true;
    }

    public DataElementPathSet getDataElementPath()
    {
        return lastValue;
    }
    
    public String getExtension()
    {
        return getAttributes().get("format").toString();
    }
    
    public File getFile()
    {
        return name == null ? null : new File(path, name);
    }
    
    @Override
    public void setValue(String value)
    {
        if((this.name == null ^ value == null) || (this.name != null && !this.name.equals(value)))
        {
            this.name = value;
            isExported = false;
            updateParameterFields();
        }
    }
    
    @Override
    public void setValueFromTest(String value)
    {
        if( !isOutput() )
            this.path = GalaxyDataCollection.getGalaxyDistFiles().getTestDataFolder();
        super.setValueFromTest(value);
        isExported = true;
    }
    
    @Override
    protected void updateParameterFields()
    {
        fields.put("name", name);
        fields.put("file_name", toString());
        fields.put("dataset", toString());
        fields.put("extension", getExtension());
        fields.put("ext", getExtension());
        fields.put("extra_files_path", path);
        fields.put("value", toString());
    }

    @Override
    public String toString()
    {
        if(name == null)
          return "";
        return new File(new File(path, name), "*." + getExtension()).getAbsolutePath();
    }
    
    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        MultiFileParameter result = (MultiFileParameter)clone;
        result.setPath(getPath());
        result.setValue(getName());
    }

    @Override
    public Parameter cloneParameter()
    {
        MultiFileParameter result = new MultiFileParameter(output);
        doCloneParameter(result);
        return result;
    }

    /**
     * Set value by ru.biosoft.access.core.DataElementPath
     */
    public void setValue(DataElementPathSet path)
    {
        if((lastValue == null ^ path == null) || (this.lastValue != null && !lastValue.equals(path)))
        {
            lastValue = path;
            isExported = false;
            setValue(path == null ? null : "file_" + fileNum.incrementAndGet());
        }
    }
}
