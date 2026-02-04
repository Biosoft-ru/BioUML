package biouml.plugins.wdl.model;

import java.util.ArrayList;
import java.util.List;

public class StructInfo
{
    private String name;
    private List<ExpressionInfo> expressions = new ArrayList<>();
    
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public Iterable<ExpressionInfo> getExpressions()
    {
        return expressions;
    }
    public void addExpressions(ExpressionInfo expression)
    {
        this.expressions.add(expression);
    }
}
