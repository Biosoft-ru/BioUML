package biouml.plugins.wdl.model;

import java.util.HashMap;
import java.util.Map;

public class MetaInfo
{
    private String name = "meta";
    private Map<String, String> values = new HashMap<>();
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setProperty(String name, String value)
    {
        values.put(name, value);
    }
    
    public String getProperty(String name)
    {
        return values.get(name);
    }

    public void setValues(Map<String, String> values)
    {
        this.values = values;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Map<String, String> getValues()
    {
        return values;
    }
}