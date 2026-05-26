package biouml.plugins.wdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.wdl.diagram.WDLConstants;
import biouml.plugins.wdl.model.StructInfo;
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
    
    /**
     * Returns ordered list of structures so that structure used as types in other structure goes before it
     */
    public List<Node> orderStructs()
    {
        List<Node> structs = getStructs();
        List<Node> result = new ArrayList<Node>();
        Map<String, Node> structsToAdd = StreamEx.of( structs ).toMap( s -> s.getName(), s -> s );
        while( true )
        {
            Node info = findIndependent( structsToAdd );
            if( info == null )
                break;
            result.add( info );
            structsToAdd.remove( info.getName() );
        }
        return result;
    }

    private Node findIndependent(Map<String, Node> structs)
    {
        for( Entry<String, Node> e : structs.entrySet() )
        {
            if( getUsedStructs( structs.keySet(), e.getValue() ).isEmpty() )
            {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Retruns all structures that are used as types inside given structure
     */
    public Set<String> getUsedStructs(Set<String> allStructs, Node struct)
    {
        return StreamEx.of( getStructMembers( struct ) ).map( expr -> expr.getType() ).filter( s -> allStructs.contains( s ) ).toSet();
    }
}