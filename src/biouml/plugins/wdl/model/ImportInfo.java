package biouml.plugins.wdl.model;

public class ImportInfo
{
    private String source;
    private String alias;

    public ImportInfo(String alias, String source)
    {
        this.source = source;
        this.alias = alias;
    }
    
    public String getSource()
    {
        return source;
    }
    
    public String getAlias()
    {
        return alias;
    }
}
