package biouml.plugins.seek;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class SeekSyncParameters extends AbstractAnalysisParameters
{
    protected String seekUrl = "http://test.genexplain.com/seek/";
    protected String login = "";
    protected String password = "";
    protected DataElementPath outputPath;
    protected String[] availableDataFiles;

    private Logger log;
    Logger getLogger()
    {
        return this.log;
    }
    void setLogger(@Nonnull Logger log)
    {
        this.log = log;
    }

    @PropertyName ( "List of available data files" )
    @PropertyDescription ( "SEEK Data files available to user" )
    public String[] getAvailableDataFiles()
    {
        return availableDataFiles;
    }

    public void setAvailableDataFiles(String[] availableDataFiles)
    {
        Object oldValue = this.availableDataFiles;
        this.availableDataFiles = availableDataFiles;
        firePropertyChange( "availableDataFiles", oldValue, availableDataFiles );
    }

    @PropertyName ( "Output SEEK folder" )
    @PropertyDescription ( "Storage for SEEK data" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        this.outputPath = outputPath;
    }

    @PropertyName ( "SEEK web address" )
    @PropertyDescription ( "example: http://test.genexplain.com/seek" )
    public String getSeekUrl()
    {
        return seekUrl;
    }

    public void setSeekUrl(String seekUrl)
    {
        this.seekUrl = seekUrl;
    }

    @PropertyName ( "SEEK user login" )
    @PropertyDescription ( "login" )
    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    @PropertyName ( "Password" )
    @PropertyDescription ( "Password" )
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

}