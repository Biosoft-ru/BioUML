package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.exception.TableNoColumnException;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.JobControl;

@SuppressWarnings ( "serial" )
public class SaveHitsAction extends BackgroundDynamicAction
{
    public static final String KEY = "Subset hits table";

    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof TableDataCollection )
        {
            Class<? extends AnalysisParameters> analysisClass = AnalysisParametersFactory.getAnalysisClass((DataElement)object);
            if( analysisClass != null && BasicKeyNodeAnalysisParameters.class.isAssignableFrom(analysisClass))
            {
                return true;
            }
            analysisClass = AnalysisParametersFactory.getPersistentAnalysisClass((DataElement)object);
            if( analysisClass != null && BasicKeyNodeAnalysisParameters.class.isAssignableFrom(analysisClass))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath modelPath = DataElementPath.create((DataElement)model);
        DataElementPath defaultPath = DataElementPath.create(modelPath.optParentCollection(), modelPath.getName() + " hits");
        return getTargetProperties(TableDataCollection.class, defaultPath);
    }
    
    protected List<DataElement> getHits(TableDataCollection tdc, TableDataCollection source, List<DataElement> selectedItems)
    {
        ColumnModel model = tdc.getColumnModel();
        int hitsColumn = IntStreamEx.range( model.getColumnCount() )
                .findFirst( i -> model.getColumn( i ).getValueClass() == StringSet.class )
                .orElseThrow( () -> new TableNoColumnException( tdc, "Hits" ) );
        Set<String> hits = StreamEx.of( selectedItems ).select( RowDataElement.class ).map( row -> row.getValues()[hitsColumn] )
                .select( StringSet.class ).flatMap( Set::stream ).toSet();
        List<DataElement> hitsDE = new ArrayList<>();
        for(String hit: hits)
        {
            try
            {
                hitsDE.add(source.get(hit));
            }
            catch(Exception e)
            {
            }
        }
        if( hitsDE.isEmpty() )
            throw new BiosoftCustomException(null, "No hits found");
        return hitsDE;
    }

    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems) throws LoggedException
    {
        TableDataCollection table = ((DataElement)model).cast( TableDataCollection.class );
        AnalysisParameters parameters = AnalysisParametersFactory.read(table);
        if( ! (parameters instanceof BasicKeyNodeAnalysisParameters ))
            parameters = AnalysisParametersFactory.readPersistent(table);
        if( ! (parameters instanceof BasicKeyNodeAnalysisParameters ))
            throw new ParameterNotAcceptableException("Table", table.getCompletePath().toString());
        DataElementPath sourcePath = ((BasicKeyNodeAnalysisParameters)parameters).getSourcePath();
        if(sourcePath == null)
            throw new ParameterNotAcceptableException("Table", table.getCompletePath().toString());
        getHits(table, sourcePath.getDataElement(TableDataCollection.class), selectedItems);
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        SaveHitsAnalysis analysis = new SaveHitsAnalysis(null, "");
        SaveHitsAnalysisParameters parameters = analysis.getParameters();
        parameters.setKnResultPath(DataElementPath.create((DataElement)model));
        parameters.setScore(-1);
        DataElementPath outputPath = (DataElementPath) ( (DynamicPropertySet)properties ).getValue("target");
        parameters.setOutputPath(outputPath);
        parameters.setSelectedItems(selectedItems);
        return analysis.getJobControl();
    }
}
