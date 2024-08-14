package biouml.plugins.sedml;

import java.util.HashSet;
import java.util.Set;

import one.util.streamex.StreamEx;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;

import org.jlibsedml.DataGenerator;
import org.jlibsedml.Plot2D;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.sedml.analyses.Curve;

public class Plot2DBuilder extends OutputBuilder
{
    private Plot2D plot2D;
    public Plot2DBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public void setPlot2D(Plot2D plot2D)
    {
        this.plot2D = plot2D;
    }
    
    @Override
    public void build()
    {
        biouml.plugins.sedml.analyses.Plot2D generateReportAnalysis = AnalysisMethodRegistry.getAnalysisMethod( biouml.plugins.sedml.analyses.Plot2D.class );
        fillGenerateReportParameters( generateReportAnalysis.getParameters() );
        Compartment generateReportNode = addAnalysis( generateReportAnalysis );
        
        for(String taskId : getTaskReferences())
        {
            if(!simulationResultNodes.containsKey( taskId ))
                throw new IllegalArgumentException("No simulation result for task " + taskId);
            Node simulationResultNode = simulationResultNodes.get(taskId);
            Node generateReportInputNode = (Node)generateReportNode.get( "simulationResultPath" );
            addDirectedEdge( parent, simulationResultNode, generateReportInputNode );
        }
        
        String title = getTitleForSedmlElement( plot2D );
        outputNode = addWorkflowExpression( title, "$Output folder$/" + title, VariableType.getType( VariableType.TYPE_AUTOOPEN ) );
        
        addDirectedEdge( parent, (Node)generateReportNode.get( "outputChart" ), outputNode );
    }
    
    @Override
    protected Set<String> getTaskReferences()
    {
        Set<String> taskReferences = new HashSet<>();
        for( org.jlibsedml.Curve curve : plot2D.getListOfCurves() )
        {
            org.jlibsedml.DataGenerator xDataGenerator = sedml.getDataGeneratorWithId( curve.getXDataReference() );
            for( org.jlibsedml.Variable var : xDataGenerator.getListOfVariables() )
                taskReferences.add( var.getReference() );
            org.jlibsedml.DataGenerator yDataGenerator = sedml.getDataGeneratorWithId( curve.getYDataReference() );
            for( org.jlibsedml.Variable var : yDataGenerator.getListOfVariables() )
                taskReferences.add( var.getReference() );
        }
        return taskReferences;
    }
    
    private void fillGenerateReportParameters(biouml.plugins.sedml.analyses.Plot2D.Parameters parameters)
    {
        parameters.setCurves( StreamEx.of(plot2D.getListOfCurves()).map( this::convertCurve ).toArray( Curve[]::new ) );
    }

    private Curve convertCurve(org.jlibsedml.Curve curve)
    {
        Curve result = new Curve();

        String title = getTitleForSedmlElement( curve );
        result.setTitle( title );

        DataGenerator xDataGenerator = sedml.getDataGeneratorWithId( curve.getXDataReference() );
        result.setExpressionX( createExpression( xDataGenerator ) );

        DataGenerator yDataGenerator = sedml.getDataGeneratorWithId( curve.getYDataReference() );
        result.setExpressionY( createExpression( yDataGenerator ) );

        result.setLogX( curve.getLogX() );
        result.setLogY( curve.getLogY() );
        return result;
    }

}
