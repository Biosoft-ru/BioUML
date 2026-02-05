package biouml.plugins.wdl.model;

public class ScatterInfo extends ContainerInfo
{
    private String variable;
    private String expression;

    public String getVariable()
    {
        return variable;
    }

    public void setVariable(String variable)
    {
        this.variable = variable;
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

