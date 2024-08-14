package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.jlibsedml.DataGenerator;
import org.jlibsedml.DataSet;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Plot2D;
import org.jlibsedml.Report;
import org.jlibsedml.SedML;
import org.jlibsedml.Variable;
import org.jlibsedml.VariableSymbol;
import org.jmathml.ASTNode;

import ru.biosoft.util.Pair;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sedml.analyses.Column;
import biouml.plugins.sedml.analyses.Curve;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Utils;

public class ListOfOutputsParser extends WorkflowParser
{
    public ListOfOutputsParser(Diagram workflow, SedML sedml)
    {
        super( workflow, sedml );
    }
    
    @Override
    public void parse()
    {
        List<Runnable> runLater = new ArrayList<>();
        for( Compartment analysisNode : findAnalyses( biouml.plugins.sedml.analyses.Plot2D.class ) )
        {
            Node inPort = (Node)analysisNode.get( "simulationResultPath" );
            Set<String> referenceTasks = StreamEx.of( inPort.getEdges() ).filter( e -> e.getOutput() == inPort )
                    .map( e -> parseTitle( e.getInput().getName() ).id ).toSet();
            if( referenceTasks.isEmpty() )
                continue;
            biouml.plugins.sedml.analyses.Plot2D.Parameters parameters = (biouml.plugins.sedml.analyses.Plot2D.Parameters)AnalysisDPSUtils
                    .readParametersFromAttributes( analysisNode.getAttributes() );
            Node outputPort = (Node)analysisNode.get( "outputChart" );
            Optional<Edge> outgoingEdge = StreamEx.of( outputPort.getEdges() ).findAny( e -> e.getInput() == outputPort );
            if( outgoingEdge.isPresent() )
            {
                Node plotNode = outgoingEdge.get().getOutput();
                IdName res = parseTitle( plotNode.getName() );
                exportPlot( parameters, res.id, res.name, referenceTasks );
            }
            else
            {
                runLater.add( () -> {
                    String plotId = generatePlotId();
                    exportPlot( parameters, plotId, null, referenceTasks );
                } );
            }
        }
        for( Compartment analysisNode : findAnalyses( biouml.plugins.sedml.analyses.Report.class ) )
        {
            Node inPort = (Node)analysisNode.get( "simulationResultPath" );
            Set<String> referenceTasks = StreamEx.of( inPort.getEdges() ).filter( e -> e.getOutput() == inPort )
                    .map( e -> parseTitle( e.getInput().getName() ).id ).toSet();
            if( referenceTasks.isEmpty() )
                continue;
            biouml.plugins.sedml.analyses.Report.Parameters parameters = (biouml.plugins.sedml.analyses.Report.Parameters)AnalysisDPSUtils
                    .readParametersFromAttributes( analysisNode.getAttributes() );
            Node outputPort = (Node)analysisNode.get( "outputTable" );
            Optional<Edge> outgoingEdge = StreamEx.of( outputPort.getEdges() ).findAny( e -> e.getInput() == outputPort );
            if( outgoingEdge.isPresent() )
            {
                Node tableNode = outgoingEdge.get().getOutput();
                IdName res = parseTitle( tableNode.getName() );
                exportReport( parameters, res.id, res.name, referenceTasks );
            }
            else
            {
                runLater.add( () -> {
                    String tableId = generateTableId();
                    exportReport( parameters, tableId, null, referenceTasks );
                } );
            }
        }
        runLater.forEach( Runnable::run );
    }
    
    private int lastTableId = 0;
    private String generateTableId()
    {
        String tableId;
        do
        {
            tableId = "table_" + ( ++lastTableId );
        }
        while( sedml.getOutputWithId( tableId ) != null );
        return tableId;
    }

    private int lastPlotId = 0;;
    private String generatePlotId()
    {
        String plotId;
        do
        {
            plotId = "plot_" + ( ++lastPlotId );
        }
        while( sedml.getOutputWithId( plotId ) != null );
        return plotId;
    }
    
    private int lastDataSetId = 0;
    
    private void exportReport(biouml.plugins.sedml.analyses.Report.Parameters parameters, String id, String name, Set<String> referenceTasks)
    {
        Report report = new Report( id, name );
        if(parameters.getColumns() != null)
            for(Column c : parameters.getColumns())
            {
                if(c == null || c.getName() == null || c.getExpression() == null || c.getExpression().isEmpty() )
                    continue;
                
                String dataGeneratorId = addDataGenerator( c.getExpression(), referenceTasks );
                DataSet dataSet = new DataSet( "report_" + (++lastDataSetId), null, c.getName(), dataGeneratorId );
                report.addDataSet( dataSet  );
            }
        sedml.addOutput( report );
    }

   
    private int lastCurveId = 0;
    private void exportPlot(biouml.plugins.sedml.analyses.Plot2D.Parameters parameters, String plotId, String plotName, Set<String> referenceTasks)
    {
        Plot2D plot2d = new Plot2D( plotId, plotName );
        if( parameters.getCurves() != null )
            for( Curve curve : parameters.getCurves() )
            {
                if(curve == null)
                    continue;
                if(curve.getExpressionX() == null || curve.getExpressionX().isEmpty())
                    continue;
                if(curve.getExpressionY() == null || curve.getExpressionY().isEmpty())
                    continue;

                String xId = addDataGenerator( curve.getExpressionX(), referenceTasks );
                String yId = addDataGenerator( curve.getExpressionY(), referenceTasks );

                String id;
                String name = null;
                if(curve.getTitle() == null)
                    id = "curve_" + (++lastCurveId );
                else
                {
                    IdName res = parseTitle( curve.getTitle() );
                    id = res.id;
                    name = res.name;
                }
                org.jlibsedml.Curve sedmlCurve = new org.jlibsedml.Curve( id, name, curve.isLogX(),
                        curve.isLogY(), xId, yId );
                plot2d.addCurve( sedmlCurve );
            }
        sedml.addOutput( plot2d  );
    }
    
    private final Map<Pair<String, Set<String>>, DataGenerator> dataGenerators = new HashMap<>();
    private String addDataGenerator(String expression, Set<String> referenceTasks)
    {
        Pair<String, Set<String>> key = new Pair<>( expression, referenceTasks );
        DataGenerator result = dataGenerators.get( key );
        if( result == null )
        {
            result = createDataGenerator( expression, referenceTasks );
            dataGenerators.put( key, result );
            sedml.addDataGenerator( result );
        }
        return result.getId();
    }
    
    private int lastDataGeneratorId = 0;
    
    private DataGenerator createDataGenerator(String expression, Set<String> referenceTasks)
    {
        String id = "data_generator_" + (++lastDataGeneratorId);
        AstStart ast = Utils.parseExpression( expression );
        List<Variable> variables = new ArrayList<>();
        for(AstVarNode var : Utils.deepChildren( ast ).select( AstVarNode.class ))
        {
            String referenceInExpression = var.getName();
            String specieName = SedmlUtils.unresolveVariableName( referenceInExpression );
            String varId = specieName;

            String referenceTaskId = referenceTasks.iterator().next();
            if(referenceTasks.size() != 1)
            {
                referenceTaskId = StreamEx.of( referenceTasks ).findAny( x->referenceInExpression.startsWith( x + "." ) ).orElse( referenceTaskId );
                varId = referenceTaskId + "_" + specieName;
            }
            
            var.setName( varId );
            
            Variable sedmlVar;
            String varName = varId;
            if(specieName.equals( "time" ))
            {
                sedmlVar = new org.jlibsedml.Variable( varId, varName, referenceTaskId, VariableSymbol.TIME );
            }
            else
            {
                String targetXPath = "/sbml:sbml/sbml:model/sbml:listOfSpecies/sbml:species[@id='" + specieName + "']";
                sedmlVar = new org.jlibsedml.Variable( varId, varName, referenceTaskId, targetXPath );
            }
            variables.add( sedmlVar );
        }
        ASTNode math = Libsedml.parseFormulaString( MathMLUtils.mathMLToExpression( ast )  );
        org.jlibsedml.DataGenerator result = new org.jlibsedml.DataGenerator( id, id, math );
        for(Variable var : variables)
            result.addVariable( var );
        return result;
    }
}
