package biouml.plugins.sedml;

import java.util.Set;

import one.util.streamex.StreamEx;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;

import org.jlibsedml.DataGenerator;
import org.jlibsedml.DataSet;
import org.jlibsedml.Report;
import org.jlibsedml.Variable;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.sedml.analyses.Column;

public class ReportBuilder extends OutputBuilder
{
    private Report report;

    public ReportBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public void setReport(Report report)
    {
        this.report = report;
    }

    @Override
    public void build()
    {
        biouml.plugins.sedml.analyses.Report generateReportAnalysis = AnalysisMethodRegistry.getAnalysisMethod( biouml.plugins.sedml.analyses.Report.class );
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
        
        String title = getTitleForSedmlElement( report );
        outputNode = addWorkflowExpression( title, "$Output folder$/" + title, VariableType.getType( VariableType.TYPE_AUTOOPEN ) );
        
        addDirectedEdge( parent, (Node)generateReportNode.get( "outputTable" ), outputNode );
        
    }

    @Override
    protected Set<String> getTaskReferences()
    {
        return StreamEx.of( report.getListOfDataSets() )
                .map( DataSet::getDataReference ).map( sedml::getDataGeneratorWithId )
                .flatCollection( dg->dg.getListOfVariables() ).map( Variable::getReference ).toSet();
    }

    private void fillGenerateReportParameters(biouml.plugins.sedml.analyses.Report.Parameters parameters)
    {
        parameters.setColumns( StreamEx.of(report.getListOfDataSets()).map( this::convertToColumn ).toArray( Column[]::new ) );
    }

    private Column convertToColumn(DataSet dataSet)
    {
        Column c = new Column();
        String colName = dataSet.getLabel();
        if(colName == null)
            colName = getTitleForSedmlElement( dataSet );
        c.setName( colName );
        DataGenerator dataGenerator = sedml.getDataGeneratorWithId( dataSet.getDataReference() );
        c.setExpression( createExpression( dataGenerator ) );
        return c;
    }

}
