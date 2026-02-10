package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.List;

public class ConditionalInfo
{
    private String expression;
    private List<Object> objects = new ArrayList<>();

    public void addObject(Object obj)
    {
        objects.add(obj);
    }

    public Iterable<Object> getObjects()
    {
        return objects;
    }

    public String getExpression()
    {
        return expression;
    }
    
    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
