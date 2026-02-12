package biouml.plugins.wdl.model;

public class InputInfo extends ExpressionInfo
{

    public InputInfo()
    {
        super();
    }

    public InputInfo(String type, String name, String expression)
    {
        super(type, name, expression);
    }
    
    public String toString()
    {
        return getType() + " " + getName() + " = " + getExpression();
    }
}