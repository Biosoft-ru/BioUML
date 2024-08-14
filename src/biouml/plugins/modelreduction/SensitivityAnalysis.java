package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.Util;

public class SensitivityAnalysis extends SteadyStateAnalysis
{
    protected static final Logger log = Logger.getLogger(SensitivityAnalysis.class.getName());

    public SensitivityAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SensitivityAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters params) throws IllegalArgumentException
    {
        try
        {
            parameters = (SensitivityAnalysisParameters)params;
        }
        catch( Exception ex )
        {
            throw new IllegalArgumentException("Wrong parameters");
        }
    }

    @Override
    public SensitivityAnalysisParameters getParameters()
    {
        return (SensitivityAnalysisParameters)parameters;
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        SensitivityAnalysisResults results = performAnalysis();

        if( results != null )
        {
            DataCollection<?> origin = DataCollectionUtils.createSubCollection(getParameters().getOutput());
            List<TableDataCollection> toPut = new ArrayList<>();
            toPut.add(getSensitivitiesToPut(origin, "Sensitivities", results.sensitivities, results.parameters, results.targets));
            toPut.add(getSensitivitiesToPut(origin, "Unscaled sensitivities", results.unscaledSensitivities, results.parameters,
                    results.targets));
            return saveResults(origin, toPut);
        }
        return null;
    }

    public void checkDiagram(Diagram diagram) throws IllegalArgumentException
    {
        if( diagram == null )
            throw new IllegalArgumentException("Diagram for sensitivity analysis was not found!");

        if( ! ( diagram.getRole() instanceof EModel ) )
            throw new IllegalArgumentException("Diagram " + diagram.getName() + " is not suitable for sensitivity analysis.");
    }


    public SensitivityAnalysisResults performAnalysis() throws Exception
    {
        Diagram diagram = parameters.getEngineWrapper().getDiagram();
        checkDiagram(diagram);
        diagram.getRole(EModel.class).detectVariableTypes();

        Model model = getModel(diagram); //init model and varindexmapping here
        SimulationEngine engine = getParameters().getEngineWrapper().getEngine();
        Map<String, Integer> mapping = engine.getVarPathIndexMapping();
    
        String[] targetVariables = VariableSet.getVariablePaths(getParameters().getTargetVariables());
        int[] targetIndices = StreamEx.of(targetVariables).mapToInt(v->mapping.get(v)).toArray();

        String[] targetVariablesTitles = StreamEx.of( targetVariables ).map( v -> Util.getVariable(diagram, v).getTitle() )
                .toArray( String[]::new );

        String[] inputVariables = VariableSet.getVariablePaths( getParameters().getInputVariables());
        int[] inputIndices = StreamEx.of(inputVariables).mapToInt(v->mapping.get(v)).toArray();

        double[][] difference = new double[targetIndices.length][inputVariables.length];
        double[][] unscaledSensitivities = new double[targetIndices.length][inputVariables.length];
        double[][] sensitivities = new double[targetIndices.length][inputVariables.length];

        model.init();
        double[] initialValues = model.getCurrentValues();
        Map<String, Double> steadyStateValues = findSteadyState(model);
        if( steadyStateValues == null )
            throw new Exception("Steady state not reached for unchanged parameters.");

        double[] steadyStateArray = model.getCurrentValues();

        for( int j = 0; j < inputVariables.length; ++j )
        {
            int inputIndex = inputIndices[j];
            double oldValue = initialValues[inputIndex];
            
            double step = initialValues[inputIndex] * getParameters().getRelativeStep() + getParameters().getAbsoluteStep();
            initialValues[inputIndex] += step;
            
            model.init();
            model.setCurrentValues(initialValues);
            
            steadyStateValues = findSteadyState(model);
            double[] changedSteadyState = null;
            if( steadyStateValues == null )
            {
                log.info("Steady state not reached");
                for( int i = 0; i < targetIndices.length; ++i )
                    unscaledSensitivities[i][j] = sensitivities[i][j] = Double.NaN;
            }
            else
            {
                changedSteadyState = model.getCurrentValues();

                for( int i = 0; i < targetIndices.length; ++i )
                {
                    int targetIndex = targetIndices[i];
//                    System.out.println(  changedSteadyState[targetIndex]+"\t"+steadyStateArray[targetIndex]+"\t"+step );                    
                    difference[i][j] = ( changedSteadyState[targetIndex] - steadyStateArray[targetIndex] );
                    unscaledSensitivities[i][j] = difference[i][j] / step;
                    sensitivities[i][j] = oldValue / steadyStateArray[targetIndex] * unscaledSensitivities[i][j]; }
            }
            initialValues[inputIndex] = oldValue;

            if( jobControl != null )
                jobControl.setPreparedness((int) ( ( (long)j * 100 ) / inputIndices.length ));
        }
        return new SensitivityAnalysisResults(targetVariablesTitles, inputVariables, difference, unscaledSensitivities, sensitivities);
    }

    private TableDataCollection getSensitivitiesToPut(@Nonnull DataCollection<?> origin, @Nonnull String name, double[][] sensitivities,
            String[] parameters, String[] targets) throws Exception
    {
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, name);
        if( sensitivities == null )
            return tdc;

        tdc.getColumnModel().addColumn( "Entities", DataType.Text );
        for( String element : parameters )
            tdc.getColumnModel().addColumn(element.replace( "/", "_" ), Double.class);

        for( int i = 0; i < targets.length; ++i )
        {
            Object[] row = generateRow( targets[i], sensitivities[i] );
            TableDataCollectionUtils.addRow(tdc, generateID( i ), row);
        }

        return tdc;
    }

    private TableDataCollection[] saveResults(DataCollection origin, List<TableDataCollection> results) throws Exception
    {
        return StreamEx.of(results).nonNull().peek(origin::put).toArray(TableDataCollection[]::new);
    }
    
    public static class SensitivityAnalysisResults
    {
        public String[] targets;
        public String[] parameters;
        public double[][] sensitivities;
        public double[][] unscaledSensitivities;
        public double[][] differences;

        public SensitivityAnalysisResults(String[] targets, String[] parameters, double[][] differences, double[][] unscaled, double[][] sensetivities)
        {
            this.targets = targets;
            this.parameters = parameters;
            this.differences = differences;
            this.sensitivities = sensetivities;
            this.unscaledSensitivities = unscaled;
        }
    }
}
