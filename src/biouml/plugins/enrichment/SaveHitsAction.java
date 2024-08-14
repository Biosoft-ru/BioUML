package biouml.plugins.enrichment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
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
import ru.biosoft.table.access.SubsetTableAction;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.table.exception.TableNoColumnException;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

@SuppressWarnings ( "serial" )
public class SaveHitsAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        if( object instanceof DataCollection )
        {
            Class<? extends AnalysisParameters> analysisClass = AnalysisParametersFactory.getAnalysisClass((DataElement)object);
            if( analysisClass != null && ( FunctionalClassificationParameters.class.isAssignableFrom( analysisClass )
                    || TreeMapAnalysisParameters.class.isAssignableFrom( analysisClass ) ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl( Logger.getLogger( SubsetTableAction.class.getName() ) )
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    DataElementPath destination = (DataElementPath)((DynamicPropertySet)properties).getValue("target");
                    AnalysisParameters parameters = AnalysisParametersFactory.read((DataElement)model);
                    if( parameters instanceof TreeMapAnalysisParameters )
                    {
                        DataElementPath functionalClassificationPath = ( (TreeMapAnalysisParameters)parameters ).getSourcePath();
                        parameters = AnalysisParametersFactory.read( functionalClassificationPath.getDataElement() );
                    }
                    TableDataCollection source = ( (FunctionalClassificationParameters)parameters ).getSource();
                    List<RowDataElement> hitsDE = getHits((TableDataCollection)model, source, selectedItems);
                    setPreparedness( 10 );
                    TableRowsExporter.exportTable(destination, source, hitsDE, new SubFunctionJobControl( this, 10, 100 ));
                    setPreparedness(100);
                    resultsAreReady(new Object[]{destination.optDataCollection()});
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        DataElementPath sourcePath = DataElementPath.create((DataElement)model);
        return getTargetProperties(TableDataCollection.class, DataElementPath.create(sourcePath.optParentCollection(), sourcePath.getName() + " hits").uniq());
    }

    protected List<RowDataElement> getHits(TableDataCollection tdc, TableDataCollection source, List<DataElement> selectedItems)
    {
        ColumnModel model = tdc.getColumnModel();
        int hitsColumn = IntStreamEx.range( model.getColumnCount() )
                .findFirst( i -> model.getColumn( i ).getValueClass() == StringSet.class )
                .orElseThrow( () -> new TableNoColumnException( tdc, "Hits" ) );
        Set<String> hits = StreamEx.of( selectedItems ).select( RowDataElement.class ).map( row -> row.getValues()[hitsColumn] )
                .select( StringSet.class ).flatMap( Set::stream ).toSet();
        List<RowDataElement> hitsDE = new ArrayList<>();
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
        if( parameters instanceof TreeMapAnalysisParameters )
        {
            DataElementPath functionalClassificationPath = ( (TreeMapAnalysisParameters)parameters ).getSourcePath();
            if( functionalClassificationPath == null || !functionalClassificationPath.exists() )
                throw new ParameterNotAcceptableException( "Table", table.getCompletePath().toString() );
            parameters = AnalysisParametersFactory.read( functionalClassificationPath.getDataElement() );
        }
        if( ! (parameters instanceof FunctionalClassificationParameters) )
            throw new ParameterNotAcceptableException("Table", table.getCompletePath().toString());
        DataElementPath sourcePath = ( (FunctionalClassificationParameters)parameters ).getSourcePath();
        if(sourcePath == null)
            throw new ParameterNotAcceptableException("Table", table.getCompletePath().toString());
        getHits(table, sourcePath.getDataElement(TableDataCollection.class), selectedItems);
    }
}
