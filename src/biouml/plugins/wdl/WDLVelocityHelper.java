package biouml.plugins.wdl;

import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;

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
        String version =  diagram.getAttributes().getValueAsString( WDLConstants.WDL_VERSION_ATTR );
        if (version == null)
            version = "1.2";
        return version;
    }

    public String getCallInput(Node inputNode)
    {
        String name = getName( inputNode );
        String expression = getExpression( inputNode );
        if( expression == null )
            return name;
        return name + " = " + expression;
    }

    public List<ImportProperties> getImports()
    {
        return WorkflowUtil.getImports( diagram );
    }
    
    public List<Compartment> getWorkflows()
    {
        List<Compartment> result =  WorkflowUtil.getWorkflows( diagram );
        result.add( diagram );
        return result;
    }

}