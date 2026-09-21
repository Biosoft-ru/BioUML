package biouml.plugins.wdl.model;

public class CommandInfo
{
    public static String TYPE_EXEC = "exec";
    public static String TYPE_SCRIPT = "script";
    public static String TYPE_SHELL = "shell";
    
    private String type = TYPE_SCRIPT;
   
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
    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    public String[] getAvailableTypes()
    {
        return new String[] {TYPE_SCRIPT, TYPE_SHELL, TYPE_EXEC};
    }
}