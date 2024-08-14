package ru.biosoft.galaxy.parameters;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import ru.biosoft.util.TempFiles;

/**
 * @author lan
 *
 */
public abstract class BaseFileParameter extends ParameterSupport
{
    protected File path = TempFiles.getTempDirectory();
    protected String name;
    protected static final AtomicInteger fileNum = new AtomicInteger();

    public BaseFileParameter(boolean output)
    {
        super(output);
    }

    abstract public boolean exportFile();
    abstract protected void updateParameterFields();
    
    public void setPath(File path)
    {
        this.path = path;
        updateParameterFields();
    }
   
    public File getPath()
    {
        return path;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Properties getFormatProperties()
    {
        Properties properties = new Properties();
        for(String name : getAttributes().keySet())
            if(name.startsWith( "format." ))
                properties.setProperty( name, getAttributes().get( name ).toString() );
        return properties;
    }
}
