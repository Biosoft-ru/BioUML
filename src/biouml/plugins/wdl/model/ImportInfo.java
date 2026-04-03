package biouml.plugins.wdl.model;

public class ImportInfo
{
    private ScriptInfo imported;
    private String source;
    private String alias;
    private String task;
 
    public void ImportInfo()
    {
        
    }
    
    public ImportInfo(String alias, String source)
    {
        this.source = source;
        this.alias = alias;
    }
    
    public String getTask()
    {
        return task;
    }

    public void setTask(String task)
    {
        this.task = task;
    }
    
    public String getSource()
    {
        return source;
    }
    
    public void setSource(String source)
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
    
    public ScriptInfo getImported()
    {
        return imported;
    }
    
    public void setImported(ScriptInfo scriptInfo)
    {
        this.imported = scriptInfo;
    }
}
