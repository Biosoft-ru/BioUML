package biouml.plugins.pharm.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class GeneratePopulationAnalysis extends AnalysisMethodSupport<GeneratePopulationAnalysisParameters>
{
    private PatientCalculator calculator = null;
    private boolean debug;

    public GeneratePopulationAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new GeneratePopulationAnalysisParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection resultTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputTablePath());
        resultTable.getOrigin().put(resultTable);
        return resultTable;
    }

    public void fillTable(TableDataCollection table) throws Exception
    {
        for (Distribution d: parameters.getDistributions())
        {
//            d.get
//            table.add
        }
    }
}
