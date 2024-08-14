package ru.biosoft.galaxy.parameters;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.galaxy.GalaxyFactory;

/**
 * 'configfile' Galaxy tool parameter support
 */
public class ConfigParameter extends ParameterSupport
{
    protected static final Logger log = Logger.getLogger(ConfigParameter.class.getName());

    protected File configFile;
    protected String fileContent;
    protected String processedContent;

    public ConfigParameter(boolean output)
    {
        super(output);
    }

    public String getFileContent()
    {
        return fileContent;
    }


    public void setFileContent(String fileContent)
    {
        this.fileContent = fileContent;
    }
    
    public void processTemplate(Map<String, Parameter> parameters, File toolPath) throws Exception
    {
        processedContent = GalaxyFactory.fillTemplate(fileContent, parameters, toolPath);
    }


    public void setConfigFile(File configFile)
    {
        if(processedContent == null)
            throw new IllegalStateException("Template should be processed befort setting config file");
        this.configFile = configFile;
        try
        {
            ApplicationUtils.writeString(configFile, processedContent);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot create temporary config file", e);
        }
    }
    
    public File getConfigFile()
    {
        return configFile;
    }


    @Override
    public void setValue(String value)
    {
        throw new UnsupportedOperationException("setValue is not possible for ConfigParameter");
    }

    @Override
    public String toString()
    {
        return (configFile==null)?"":configFile.getAbsolutePath();
    }

    @Override
    protected void doCloneParameter(ParameterSupport clone)
    {
        super.doCloneParameter(clone);
        ConfigParameter result = (ConfigParameter)clone;
        result.setFileContent(fileContent);
    }

    @Override
    public Parameter cloneParameter()
    {
        ConfigParameter result = new ConfigParameter(output);
        doCloneParameter(result);
        return result;
    }
}
