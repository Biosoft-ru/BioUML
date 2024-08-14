package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jlibsedml.ComputeChange;
import org.jlibsedml.Parameter;
import org.jlibsedml.SedML;
import org.jlibsedml.Variable;
import org.jlibsedml.XPathTarget;
import org.jmathml.ASTNode;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.analysiscore.AnalysisDPSUtils;

public class ComputeChangeParser extends WorkflowParser
{
    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile( "changes/\\[(\\d+)\\]/propertyValue" );

    private List<ComputeChange> changes = new ArrayList<>();
    private Compartment analysisNode;
    private Set<Integer> indices = new TreeSet<>();

    public ComputeChangeParser(Diagram workflow, SedML sedml)
    {
        super( workflow, sedml );
    }

    public List<ComputeChange> getChanges()
    {
        return changes;
    }
    
    public Set<Integer> getUsedIndices()
    {
        return indices;
    }

    public void setAnalysisNode(Compartment analysisNode)
    {
        this.analysisNode = analysisNode;
    }
    
    @Override
    public void parse()
    {
        ChangeDiagramParameters parameters = (ChangeDiagramParameters)AnalysisDPSUtils.readParametersFromAttributes( analysisNode
                .getAttributes() );
        for( Edge edge : StreamEx.of( analysisNode.getEdges() ).filter( e -> e.getOutput() == analysisNode ) )
        {
            String edgeVariable = (String)edge.getAttributes().getValue( WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY );
            if( edgeVariable == null )
                continue;

            Matcher matcher = PROPERTY_VALUE_PATTERN.matcher( edgeVariable );
            if( !matcher.matches() )
                continue;
            int i = Integer.parseInt( matcher.group( 1 ) );
            indices.add( i );
            StateChange stateChange = parameters.getChanges()[i];
            matcher = ListOfModelsParser.INITIAL_VALUE_PATTERN.matcher( stateChange.getElementProperty() );
            if( matcher.matches() && stateChange.getElementId().isEmpty() )
            {
                String varName = SedmlUtils.unresolveVariableName( matcher.group( 1 ) );
                XPathTarget xPath = new XPathTarget( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"" + varName
                        + "\"]/@value" );
                ComputeChange change = new ComputeChange( xPath );

                Node expr = edge.getInput();
                Optional<Node> scriptNodeOpt = expr.edges().filter( e -> e.getOutput() == expr ).map( Edge::getInput )
                        .findAny( n -> Type.ANALYSIS_SCRIPT.equals( n.getKernel().getType() ) );

                if( scriptNodeOpt.isPresent() )
                    parseChangeFromScript( change, scriptNodeOpt.get() );
                else
                    parseChangeFromVariable( change, expr );
                changes.add( change );
            }
        }
    }

    private void parseChangeFromVariable(ComputeChange change, Node node)
    {
        String id = parseVariableOrParameter( change, node );
        change.setMath( MathMLUtils.convertExpressionToMathML( id ) );
    }

    private void parseChangeFromScript(ComputeChange change, Node scriptNode)
    {
        if( !"math".equals( scriptNode.getAttributes().getValue( ScriptElement.SCRIPT_TYPE ) ) )
            return;
        String source = scriptNode.getAttributes().getValueAsString( ScriptElement.SCRIPT_SOURCE );
        ASTNode math = MathMLUtils.convertExpressionToMathML( source );
        change.setMath( math );
        for( Node varNode : scriptNode.edges().filter( e -> e.getOutput() == scriptNode ).map( Edge::getInput ) )
            parseVariableOrParameter( change, varNode );
    }

    private String parseVariableOrParameter(ComputeChange change, Node node)
    {
        Variable var = parseVariable( node );
        if( var != null )
        {
            change.addVariable( var );
            return var.getId();
        }
        Parameter par = parseParameter( node );
        if( par == null )
            throw new IllegalArgumentException( "Can not represent workflow expression " + node.getName() + " in sedml" );
        change.addParameter( par );
        return par.getId();
    }

    private Parameter parseParameter(Node varNode)
    {
        WorkflowExpression wfExpression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( varNode );
        double value;
        try
        {
            value = Double.parseDouble( wfExpression.getExpression() );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        IdName parseResult = parseTitle( varNode.getName() );
        return new Parameter( parseResult.id, parseResult.name, value );
    }

    private static Pattern VARIABLE_EXPRESSION_PATTERN = Pattern.compile("\\$([^/]+)/element/role/vars/([^/]+)/initialValue\\$");
    private Variable parseVariable(Node varNode)
    {
        WorkflowExpression wfExpression = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( varNode );
        Matcher matcher = VARIABLE_EXPRESSION_PATTERN.matcher( wfExpression.getExpression() );
        if(!matcher.matches())
            return null;
        String referenceModelNodeName = matcher.group( 1 );
        String modelId = parseTitle( referenceModelNodeName ).id;
        
        String nameInModel = SedmlUtils.unresolveVariableName( matcher.group( 2 ) );
        String xPath = "/sbml:sbml/sbml:model/sbml:listOfSpecies/sbml:species[@id='" + nameInModel + "']";
        
        IdName parseResult = parseTitle( varNode.getName() );
        return new Variable( parseResult.id, parseResult.name, modelId, xPath );
    }

}
