package ru.biosoft.bsa.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SitesTableCollection;
import ru.biosoft.bsa.access.SitesToTableTransformer;
import ru.biosoft.bsa.access.TransformedSite;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/table-to-track.gif" )
public class TrackToTable extends AnalysisMethodSupport<TrackToTable.TrackToTableParameters>
{
    public TrackToTable(DataCollection<?> origin, String name)
    {
        super( origin, name, new TrackToTableParameters() );
    }

    public TrackToTable(DataCollection<?> origin, String name, TrackToTableParameters parameters)
    {
        super( origin, name, parameters );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmptyCollection( "inputTrack" );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info( "Creating table..." );
        Track track = getParameters().getInputTrack().optDataElement( Track.class );
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( getParameters().getOutputTable() );
        SitesTableCollection stc = new SitesTableCollection( track, track.getAllSites() );
        Iterator<TransformedSite> iterator = stc.iterator();
        TransformedSite site = iterator.next();
        Map<String, Integer> col2index = new HashMap<>();
        log.info( "Initializing columns..." );
        ColumnModel model = result.getColumnModel();
        initColumns( site, model );
        int numCols = model.getColumnCount();
        for( int i = 0; i < numCols; i++ )
        {
            col2index.put( model.getColumn( i ).getShortDescription(), i );
        }

        log.info( "Filling table..." );
        jobControl.forCollection( DataCollectionUtils.asCollection( stc, TransformedSite.class ), new Iteration<TransformedSite>()
        {
            @Override
            public boolean run(TransformedSite site)
            {
                Iterator<String> nameIterator = site.nameIterator();
                Object[] values = new Object[numCols];
                while( nameIterator.hasNext() )
                {
                    String propName = nameIterator.next();
                    if( !col2index.containsKey( propName ) )
                        continue;
                    DynamicProperty prop = site.getProperty( propName );
                    Object value = prop.getValue();
                    if( value != null )
                    {
                        values[col2index.get( propName )] = value;
                    }
                }
                TableDataCollectionUtils.addRow( result, site.getName(), values );
                return true;
            }
        } );

        result.finalizeAddition();
        getParameters().getOutputTable().save( result );
        return result;
    }

    private void initColumns(TransformedSite site, ColumnModel resultModel)
    {
        Iterator<String> nameIterator = site.nameIterator();
        while( nameIterator.hasNext() )
        {
            String propName = nameIterator.next();
            if( propName.equals( "ID" ) )
                continue;

            DynamicProperty prop = site.getProperty( propName );

            //TODO: preprocess column names here
            String shortName = ( propName.equals( SitesToTableTransformer.PROPERTY_SEQUENCE ) ) ? "Chromosome" : propName;
            TableColumn tableColumn = new TableColumn( shortName, shortName, propName, DataType.fromClass( prop.getType() ), null );
            resultModel.addColumn( tableColumn );
        }
    }

    public static class TrackToTableParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack, outputTable;

        @PropertyName ( "Input track" )
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }

        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }

        @PropertyName ( "Output table" )
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }

        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
        }
    }

    public static class TrackToTableParametersBeanInfo extends BeanInfoEx2<TrackToTableParameters>
    {
        public TrackToTableParametersBeanInfo()
        {
            super( TrackToTableParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "inputTrack" ).inputElement( Track.class ).add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$inputTrack$ table" ).add();
        }
    }



}
