package biouml.plugins.wdl;

import biouml.model.Diagram;
import biouml.model.Node;

public class WDLVelocityHelper extends WorkflowVelocityHelper
{
    public WDLVelocityHelper(Diagram diagram)
    {
        super( diagram );
    }
 
    @Override
    public String getDeclaration(Node n)
    {
        if( n == null )
            return "??";
        if( getExpression( n ) != null && !getExpression( n ).isEmpty() )
            return getType( n ) + " " + getName( n ) + " = " + getExpression( n );
        return getType( n ) + " " + getName( n );
    }

    @Override
    public String getShortDeclaration(Node n)
    {
        if( n == null )
            return "??";
        return getType( n ) + " " + getName( n );
    }
    
    public String getVersion()
    {
        return WorkflowUtil.getVersion( diagram );
    }

    public String getCallInput(Node inputNode)
    {
        String name = getName( inputNode );
        String expression = getExpression( inputNode );
        if( expression == null )
            return name;
        return name + " = " + expression;
    }

    public ImportProperties[] getImports()
    {
        return WorkflowUtil.getImports( diagram );
    }

}