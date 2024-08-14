package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.jlibsedml.AbstractIdentifiableElement;
import org.jlibsedml.FunctionalRange;
import org.jlibsedml.Parameter;
import org.jlibsedml.Range;
import org.jlibsedml.UniformRange;
import org.jlibsedml.UniformRange.UniformType;
import org.jlibsedml.Variable;
import org.jlibsedml.VectorRange;
import org.jmathml.ASTCi;
import org.jmathml.ASTNode;
import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.EnumCycleType;
import biouml.plugins.research.workflow.items.RangeCycleType;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.standard.type.Type;

public class RangesBuilder extends WorkflowBuilder
{
    private Map<String, Range> ranges;
    private Range mainRange;
    private Map<String, Node> modelNodes;
    private final Map<String, Node> nodes = new HashMap<>();
    
    private WorkflowCycleVariable cycleVariable;
    
    public RangesBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public void setRanges(Map<String, Range> ranges)
    {
        this.ranges = ranges;
    }

    public void setMainRange(Range mainRange)
    {
        this.mainRange = mainRange;
    }
    
    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }

    @Override
    public void build()
    {
        cycleVariable = (WorkflowCycleVariable)WorkflowItemFactory.getWorkflowItem( parent, Type.ANALYSIS_CYCLE_VARIABLE );
        cycleVariable.setType( VariableType.getType( Double.class ) );
        
        List<Range> baseRanges = StreamEx.ofValues(ranges).filter( x->!(x instanceof FunctionalRange) ).toList();
        if(baseRanges.isEmpty())
            throw new IllegalArgumentException("No base ranges found");
        if( baseRanges.size() == 1 && isSimpleRange( baseRanges.get( 0 )) )
        {
            buildSimpleRange( baseRanges.get( 0 ) );
        }
        else
        {
            String indexName = "index";
            {
                int suffix = 2;
                while(ranges.containsKey(indexName))
                    indexName = "index" + suffix++;
            }

            cycleVariable.setName( indexName );
            cycleVariable.setCycleType( new RangeCycleType() );
            
            Range baseRange = mainRange;
            if(baseRange instanceof FunctionalRange)
            {
                //TODO: analyze functional range dependencies
                baseRange = baseRanges.get( 0 );
            }
            cycleVariable.setExpression( "0.." + ( baseRange.getNumElements() - 1 ) );
            parent.put( cycleVariable.getNode() );
            
            for(Range r : baseRanges)
            {
                Node node = addWorkflowExpression( r.getId(), "", VariableType.getType( Double.class ) );
                nodes.put( r.getId(), node );
                Node scriptNode  = addScriptForRange(r, indexName);
                addDirectedEdge( parent, scriptNode, nodes.get( r.getId() ) );
                addDirectedEdge( parent, cycleVariable.getNode(), scriptNode );
            }
        }

        List<FunctionalRange> functionalRanges = StreamEx.of( ranges.values() ).select( FunctionalRange.class ).toList();
        for( FunctionalRange r : functionalRanges )
        {
            Node node = addWorkflowExpression( r.getId(), "", VariableType.getType( Double.class ) );
            nodes.put( r.getId(), node );
        }
        
        for( FunctionalRange r : functionalRanges )
        {
            Map<String, Node> variables = new HashMap<>();
            for( AbstractIdentifiableElement e : r.getParameters().values() )
            {
                Parameter p = (Parameter)e;
                Node node = addWorkflowExpression( p.getId(), String.valueOf(p.getValue()), VariableType.getType( Double.class ) );
                variables.put( p.getId(), node );
            }
            for( AbstractIdentifiableElement e : r.getVariables().values() )
            {
                Variable v = (Variable)e;
                String nameInModel = SedmlUtils.getIdFromXPath( v.getTarget() );
                String modelReference = v.getReference();
                if(modelReference == null)
                    throw new IllegalArgumentException("No modelReference in functional range variable");
                if(!modelNodes.containsKey( modelReference ))
                    throw new IllegalArgumentException("Unknown model " + modelReference);
                String modelNodeName = modelNodes.get( modelReference ).getName();
                Node node = addWorkflowExpression( v.getId(), "$" + modelNodeName + "/role/vars/" + nameInModel + "/initialValue$", VariableType.getType( Double.class ) );
                variables.put( v.getId(), node );
            }
            
            Node scriptNode = addScriptForRange( r, null );
            addDirectedEdge( parent, scriptNode, nodes.get( r.getId() ) );
            for(String identifier : r.getMath().getIdentifiers().stream().map( ASTCi::getName ).collect( Collectors.toSet() ))
            {
                Node rangeNode = variables.get( identifier );
                if(rangeNode == null)
                    rangeNode = nodes.get( identifier );
                if(rangeNode == null)
                    throw new IllegalArgumentException("Unknown identifier " + identifier);
                addDirectedEdge( parent, rangeNode, scriptNode );
            }
        }
       
    }
    
    /**
     * Range that can be represented as a single cycle variable
     */
    private boolean isSimpleRange(Range range)
    {
        return (range instanceof VectorRange) || (range instanceof UniformRange && ( (UniformRange)range ).getType().equals( UniformType.Linear ));
    }
    
    private void buildSimpleRange(Range range)
    {
        cycleVariable.setName( range.getId() );
        if( range instanceof UniformRange && ( (UniformRange)range ).getType().equals( UniformType.Linear ) )
        {
            UniformRange uniformRange = (UniformRange)range;
            double start = uniformRange.getStart();
            double end = uniformRange.getEnd();
            int nPoints = uniformRange.getNumberOfPoints();
            nPoints++;//according to SedML specification we should produce one more point
            double second = ( start * ( nPoints - 2 ) + end ) / ( nPoints - 1 );
            cycleVariable.setCycleType( new RangeCycleType() );
            cycleVariable.setExpression( start + "," + second + ".." + end );
        }
        else
        {
            VectorRange vectorRange = (VectorRange)range;
            cycleVariable.setCycleType( new EnumCycleType() );
            String expression = IntStreamEx.range( vectorRange.getNumElements() ).mapToDouble( vectorRange::getElementAt )
                    .mapToObj( String::valueOf ).joining( ";" );
            cycleVariable.setExpression( expression );
        }
        Node node = cycleVariable.getNode();
        nodes.put( range.getId(), node );
        parent.put( node );
    }

    private Node addScriptForRange(Range r, String indexName)
    {
        
        if(r instanceof UniformRange)
        {
            UniformRange ur = (UniformRange)r;
            double start = ur.getStart();
            double end = ur.getEnd();
            int nPoints = ur.getNumberOfPoints();
            nPoints++;//according to SedML specification we should produce one more point
            double step = ( end - start ) / ( nPoints - 1 );
            String script = start + "+" + step + "*" + indexName;
            return addScript( script, "math" );
        }
        else if(r instanceof VectorRange)
        {
            VectorRange vr = (VectorRange)r;
            List<String> values = new ArrayList<>();
            for( int i = 0; i < vr.getNumElements(); i++ )
                values.add( String.valueOf( vr.getElementAt( i ) ) );
            String prefix = "$['" + r.getId() + "']=";
            String script = prefix + "[" + String.join( ",", values ) + "][" + indexName + "]";
            return addScript( script, "js" );
        }
        else if(r instanceof FunctionalRange)
        {
            ASTNode math = ( (FunctionalRange)r ).getMath();
            //TODO: list of variables for funcitonal range
            String expression = MathMLUtils.mathMLToExpression( MathMLUtils.convertMathML( math ) );
            return addScript( expression , "math" );
        }
        else
            throw new IllegalArgumentException("Unknown range type: " + r.getClass());
    }

   
    
}
