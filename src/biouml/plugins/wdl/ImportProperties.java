package biouml.plugins.wdl;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataElementPath;

public class ImportProperties extends Option
{
    DataElementPath source;
    private String alias;

    public ImportProperties()
    {

    }

    public ImportProperties(DataElementPath source, String alias)
    {
        this.alias = alias;
        this.source = source;
    }
    public DataElementPath getSource()
    {
        return source;
    }
    public String getSourceName()
    {
        return source.getName();
    }
    public void setSource(DataElementPath source)
    {
        this.source = source;
    }
    public String getAlias()
    {
        return alias;
    }
    public void setAlias(String alias)
    {
        this.alias = alias;
    }
}