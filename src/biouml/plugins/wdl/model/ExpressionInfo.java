package biouml.plugins.wdl.model;

import java.util.HashSet;
import java.util.Set;

import biouml.plugins.wdl.parser.AstDeclaration;

public class ExpressionInfo implements Cloneable
{
    private String type;
    private String name;
    private String expression;
    private Set<String> arguments = new HashSet<>();
    private AstDeclaration astDeclaration;
    
    public ExpressionInfo()
    {
        
    }
    
    public void setAST(AstDeclaration astDeclaration)
    {
        this.astDeclaration = astDeclaration;
    }
    public AstDeclaration getAST()
    {
        return astDeclaration;
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
    
    public void setArguments(Set<String> arguments)
    {
        this.arguments = arguments;
    }
    
    public Set<String> getArguments()
    {
        return arguments;
    }
    
    public ExpressionInfo clone()
    {
        ExpressionInfo result = new ExpressionInfo();
        result.setName( name );
        result.setExpression( expression );
        result.setType( type );
        result.setArguments( new HashSet<>(arguments) );
        
//        if (astDeclaration != null)
//        result.setAST( astDeclaration.clone() );
        return result;
    }
}