package biouml.plugins.wdl.model;

public class CommandInfo
{
    private String type;
    private String script;
    
    public CommandInfo()
    {
        
    }
    
    public CommandInfo(String script)
    {
        this.script = script;
    }
    public String getScript()
    {
        return script;
    }
    public void setScript(String script)
    {
        this.script = script;
    }
}