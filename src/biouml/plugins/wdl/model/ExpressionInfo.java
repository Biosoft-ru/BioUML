package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.List;

public class ExpressionInfo
{
    private String type;
    private String name;
    private String expression;
    
    public ExpressionInfo()
    {
        
    }
    
    public ExpressionInfo(String type, String name, String expression)
    {
        this.type = type;
        this.name = name;
        this.expression = expression;
    }
    
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public String getExpression()
    {
        return expression;
    }
}