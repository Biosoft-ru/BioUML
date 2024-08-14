package ru.biosoft.galaxy.parameters;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.galaxy.FormatRegistry;
import ru.biosoft.galaxy.FormatRegistry.ExporterResult;
import ru.biosoft.galaxy.GalaxyDataCollection;

/**
 * Input/output file parameter
 */
public class FileParameter extends BaseFileParameter
{
    private static final Logger log = Logger.getLogger( FileParameter.class.getName() );
    protected String extension;
    protected DataElementPath lastValue = null;
    protected boolean isExported = false;

    public FileParameter(boolean output)
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
        DataElement de = lastValue.optDataElement();
        if(de == null) return false;
        ExporterResult exporterInfo = FormatRegistry.getExporter(galaxyFormat.toString(), lastValue, getFormatProperties());
        DataElementExporter exporter = exporterInfo.exporter;
        getAttributes().put("usedFormat", exporterInfo.galaxyFormat);
        getFile().delete();
        if( exporter != null )
        {
            try
            {
                exporter.doExport(de, getFile());
            }
            catch( Exception e )
            {
                log.log( Level.WARNING, "Can not export " + path + " to file " + getFile().getAbsolutePath(), e );
                return false;
            }
            if(getFile().exists())
            {
                isExported = true;
                return true;
            }
        }
        return false;
    }

    public DataElementPath getDataElementPath()
    {
        return lastValue;
    }

    public String getExtension()
    {
        if(extension != null)
            return extension;
        if(name == null)
            return "";
        int ind = name.lastIndexOf('.');
        if( ind != -1 )
        {
            return name.substring(ind + 1);
        }
        return "";
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
        updateParameterFields();
    }

    public boolean isExtensionSet()
    {
        return extension != null;
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
        return new File(path, name).getAbsolutePath();
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        FileParameter result = (FileParameter)clone;
        result.setPath(getPath());
        result.setValue(getName());
    }

    @Override
    public Parameter cloneParameter()
    {
        FileParameter result = new FileParameter(output);
        doCloneParameter(result);
        return result;
    }

    /**
     * Set value by ru.biosoft.access.core.DataElementPath
     */
    public void setValue(DataElementPath path)
    {
        Object galaxyFormat = getAttributes().get("format");
        if((lastValue == null ^ path == null) || (this.lastValue != null && !lastValue.equals(path)))
        {
            lastValue = path;
            isExported = false;
            setValue(path == null ? null : "file_" + fileNum.incrementAndGet() + "."
                    + FormatRegistry.getFileExtension((String)galaxyFormat));
        }
    }
}