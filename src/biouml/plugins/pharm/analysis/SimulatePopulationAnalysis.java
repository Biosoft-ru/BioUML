package biouml.plugins.pharm.analysis;

import java.util.ArrayList;
import java.util.List;
import biouml.model.Diagram;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class SimulatePopulationAnalysis extends AnalysisMethodSupport<SimulatePopulationAnalysisParameters>
{
    private PatientCalculator calculator = null;
    private boolean debug;

    public SimulatePopulationAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SimulatePopulationAnalysisParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection resultTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputTablePath());
        int i = 0;
        for( Patient patient : justAnalyze() )
            TableDataCollectionUtils.addRow(resultTable, String.valueOf(i++), patient.getValues());
        resultTable.getOrigin().put(resultTable);
        return resultTable;
    }

    public List<Patient> justAnalyze() throws Exception
    {
        Diagram diagram = parameters.getInputDiagramPath().getDataElement(Diagram.class);
        TableDataCollection inputTable = parameters.getInputTablePath().getDataElement(TableDataCollection.class);
        String[] estimatedVariables = parameters.getEstimatedVariables();
        String[] observedVariables = parameters.getObservedVariables();

        return simulatePopulation(diagram, inputTable, observedVariables, estimatedVariables);
    }

    public List<Patient> simulatePopulation(Diagram diagram, TableDataCollection table, String[] input, String[] output) throws Exception
    {
        if( calculator == null )
            calculator = new SteadyStateCalculator(diagram, input, output);

        int[] indices = TableDataCollectionUtils.getColumnIndexes(table, input);
        List<Patient> result = new ArrayList<>();
        for( String rowName : table.getNameList() )
            result.add(createPatient(TableDataCollectionUtils.getDoubleRow(table, indices, rowName)));
        return result;
    }

    private Patient createPatient(double[] input) throws Exception
    {
        return calculator.calculate(input);
    }

    public void setCalculator(PatientCalculator calculator)
    {
        this.calculator = calculator;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }
}
