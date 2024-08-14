package biouml.plugins.keynodes;

import java.util.List;

import biouml.model.Diagram;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.analysis.diagram.JoinDiagramAnalysis;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.exception.TableNoColumnException;

@SuppressWarnings ( "serial" )
public class JoinClustersAction extends BackgroundDynamicAction
{

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        TableDataCollection table = (TableDataCollection)model;
        int index = table.columns()
                .filter( column -> column.getValueClass() == ru.biosoft.access.core.DataElementPath.class )
                .mapToInt( column -> table.getColumnModel().optColumnIndex(column.getName()) )
                .findFirst().orElseThrow( () -> new TableNoColumnException(table, "Diagram") );
        StringBuilder resultName = new StringBuilder();
        DataElementPathSet inputs = new DataElementPathSet();
        for(DataElement item : selectedItems)
        {
            DataElementPath diagramPath = (DataElementPath)((RowDataElement)item).getValues()[index];
            String name = diagramPath.getName();
            if(name.startsWith("Cluster"))
                name = name.substring("Cluster".length());
            if(resultName.length() == 0)
                resultName.append("Cluster");
            else
                resultName.append(",");
            resultName.append(name);
            inputs.add(diagramPath);
        }
        final DataElementPath outputPath = inputs.getPath().getChildPath(resultName.toString());
        if(outputPath.exists())
        {
            return new AbstractJobControl(log)
            {
                @Override
                protected void doRun() throws JobControlException
                {
                    resultsAreReady(new Object[] {outputPath.getDataElement(Diagram.class)});
                }
            };
        }
        JoinDiagramAnalysis analysis = new JoinDiagramAnalysis(null, "");
        analysis.getParameters().setInputDiagrams(inputs);
        analysis.getParameters().setOutputDiagramPath(outputPath);
        return analysis.getJobControl();
    }

    @Override
    public boolean isApplicable(Object model)
    {
        if(model instanceof TableDataCollection)
        {
            Class<? extends AnalysisParameters> analysisClass = AnalysisParametersFactory.getAnalysisClass((DataElement)model);
            if( analysisClass != null && ShortestPathClusteringParameters.class.isAssignableFrom(analysisClass))
            {
                return true;
            }
        }
        return false;
    }
}
