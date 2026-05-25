package biouml.plugins.wdl;

import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import one.util.streamex.StreamEx;

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
    
    public String getWorkflowName(Compartment c)
    {
        if( c instanceof Diagram )
        {
            String name = diagram.getAttributes().getValueAsString( WDLConstants.WORKFLOW_NAME );
            if( name != null )
                return name;
        }
        return c.getName();
    }
    
    public String getVersion()
    {
        String version =  diagram.getAttributes().getValueAsString( WDLConstants.WDL_VERSION_ATTR );
        if (version == null)
            version = "1.2";
        return version;
    }

    /**
     * Returns list of call inputs in order but only ones having expressions
     */
    public static List<String> getCallInputs(Compartment c)
    {
        List<Node> preliminary = WorkflowUtil.getInputs( c );
        String[] result = new String[preliminary.size()];
        for( Node node : preliminary )
        {
            int position = WorkflowUtil.getPosition( node );
            String expression = WorkflowUtil.getExpression( node );
            String name = WorkflowUtil.getName( node );
            if( position >= 0  && expression != null)
                result[position] = name + " = " + expression;
        }
        return StreamEx.of( result ).nonNull().toList();
    }
    
    public List<ImportProperties> getImports()
    {
        return WorkflowUtil.getImports( diagram );
    }
    
    /**
     * Return all compartments which describe workflows including top level diagram
     */
    public List<Compartment> getWorkflows()
    {
        List<Compartment> result =  WorkflowUtil.getWorkflows( diagram );
        result.add( diagram );
        return result;
    }
}