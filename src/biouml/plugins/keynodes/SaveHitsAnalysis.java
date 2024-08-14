package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.table.exception.TableNoColumnException;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/save-hits.gif" )
public class SaveHitsAnalysis extends AnalysisMethodSupport<SaveHitsAnalysisParameters>
{
    public SaveHitsAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SaveHitsAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        SaveHitsAnalysisParameters params = getParameters();
        DataElementPath knResultPath = params.getKnResultPath();

        TableDataCollection knResult = knResultPath.getDataElement(TableDataCollection.class);
        AnalysisParameters parameters = AnalysisParametersFactory.read(knResult);
        if( ! ( parameters instanceof BasicKeyNodeAnalysisParameters ) )
            parameters = AnalysisParametersFactory.readPersistent(knResult);
        if( ! ( parameters instanceof BasicKeyNodeAnalysisParameters ) )
            throw new IllegalArgumentException("This table is not a result of master regulator node analysis");
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        SaveHitsAnalysisParameters params = getParameters();
        TableDataCollection knResult = params.getKnResultPath().getDataElement(TableDataCollection.class);
        DataElementPath destination = parameters.getOutputPath();
        List<DataElement> selectedItems = params.getSelectedItems();

        BasicKeyNodeAnalysisParameters parameters = null;
        AnalysisParameters analysisParams = AnalysisParametersFactory.read(knResult);
        if( ! ( analysisParams instanceof BasicKeyNodeAnalysisParameters ) )
            analysisParams = AnalysisParametersFactory.readPersistent(knResult);
        parameters = (BasicKeyNodeAnalysisParameters)analysisParams;
        TableDataCollection source = parameters.getSource();

        ColumnModel columnModel = knResult.getColumnModel();
        int hitsColumn = knResult.columns()
                .filter( column -> column.getValueClass() == StringSet.class )
                .mapToInt( column -> columnModel.optColumnIndex(column.getName()) )
                .findFirst().orElseThrow( () -> new TableNoColumnException(knResult, "Hits") );

        Iterator<? extends DataElement> iter = null;
        int totalNodes = 0;
        if( selectedItems != null )
        {
            iter = selectedItems.iterator();
            totalNodes = selectedItems.size();
        }
        else
        {
            int scoreColumn = columnModel.getColumnIndex(params.getRankColumn());
            knResult.sortTable(scoreColumn, false);
            iter = knResult.iterator();
            totalNodes = params.getNumTopRanking();
        }

        jobControl.setPreparedness(10);
        Set<String> hits = new HashSet<>();
        int cntNodes = 0;
        while( iter.hasNext() && !jobControl.isStopped() && cntNodes < totalNodes )
        {
            RowDataElement rde = (RowDataElement)iter.next();
            Object[] values = rde.getValues();

            Object value = values[hitsColumn];

            if( value instanceof StringSet )
            {
                hits.addAll((StringSet)value);
            }

            cntNodes++;
        }
        if(jobControl.isStopped()) return null;
        jobControl.setPreparedness(30);
        List<RowDataElement> hitsDE = new ArrayList<>();
        for( String hit : hits )
        {
            try
            {
                RowDataElement row = source.get(hit);
                if( row != null )
                    hitsDE.add( row );
            }
            catch( Exception e )
            {
            }
        }
        jobControl.setPreparedness(50);
        if(jobControl.isStopped()) return null;

        jobControl.pushProgress( 50, 90 );
        TableRowsExporter.exportTable(destination, source, hitsDE, jobControl);
        jobControl.popProgress();
        TableDataCollection result = destination.getDataElement(TableDataCollection.class);
        ReferenceTypeRegistry.copyCollectionReferenceType(result, source);
        destination.save(result);
        jobControl.setPreparedness( 100 );
        return result;
    }
}
