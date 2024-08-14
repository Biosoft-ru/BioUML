package biouml.plugins.keynodes;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControl;

@SuppressWarnings ( "serial" )
public class KeyNodesResultVisualizer extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof DataCollection )
        {
            AnalysisMethod method = AnalysisParametersFactory.readAnalysis( (DataElement)object );
            if( isValid( method ) )
                return true;
            method = AnalysisParametersFactory.readAnalysisPersistent( (DataElement)object );
            if( isValid( method ) )
                return true;
        }
        return false;
    }
    private boolean isValid(AnalysisMethod method)
    {
        return method != null && PathGenerator.class.isAssignableFrom( method.getClass() )
                && method.getParameters() instanceof BasicKeyNodeAnalysisParameters;
    }

    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems) throws LoggedException
    {
        super.validateParameters(model, selectedItems);
        TableDataCollection table = ((DataElement)model).cast( TableDataCollection.class );

        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( table );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            method = AnalysisParametersFactory.readAnalysisPersistent( table );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            throw new ParameterNotAcceptableException( "Table", table.getCompletePath().toString() );
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath sourcePath = DataElementPath.create((DataElement)model);
        DataElementPath defaultPath = DataElementPath.create(sourcePath.optParentCollection(), sourcePath.getName() + " viz");
        return getTargetProperties(Diagram.class, defaultPath);
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        KeyNodeVisualization analysis = new KeyNodeVisualization(null, "");
        KeyNodeVisualizationParameters parameters = analysis.getParameters();
        parameters.setKnResultPath(DataElementPath.create((DataElement)model));
        parameters.setScore(-1);
        DataElementPath diagramPath = (DataElementPath) ( (DynamicPropertySet)properties ).getValue("target");
        parameters.setOutputPath(diagramPath);
        parameters.setSelectedItems(selectedItems);
        return analysis.getJobControl();
    }
}
