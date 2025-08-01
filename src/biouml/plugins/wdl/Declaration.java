package biouml.plugins.wdl;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.wdl.parser.AstDeclaration;

public class Declaration
{
    private String type;
    private String name;
    private String expression;

    public Declaration()
    {
    }

    public Declaration(AstDeclaration ast)
    {
        type = ast.getType();
        name = ast.getName();
        expression = ast.getExpression().toString();
    }

    public String toString()
    {
        return type + " " + name + " = " + expression;
    }


    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Expression" )
    public String getExpression()
    {
        return expression;
    }
    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
