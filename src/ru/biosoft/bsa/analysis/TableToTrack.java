package ru.biosoft.bsa.analysis;

import java.util.Iterator;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
@ClassIcon("resources/table-to-track.gif")
public class TableToTrack extends AnalysisMethodSupport<TableToTrackParameters>
{
    public TableToTrack(DataCollection<?> origin, String name)
    {
        super(origin, name, new TableToTrackParameters());
    }

    public TableToTrack(DataCollection<?> origin, String name, TableToTrackParameters parameters)
    {
        super( origin, name, parameters );
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        log.info("Creating track...");
        final SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), null, null, getTrackClass() );
        if( parameters.getSequenceCollectionPath() != null )
            result.getInfo().getProperties().put(Track.SEQUENCES_COLLECTION_PROPERTY, parameters.getSequenceCollectionPath().toString());
        if( !TextUtil2.isEmpty(parameters.getGenomeId()) )
            result.getInfo().getProperties().put(Track.GENOME_ID_PROPERTY, parameters.getGenomeId());

        log.info("Writing the result...");
        jobControl.forCollection(DataCollectionUtils.asCollection(parameters.getInputTable(), RowDataElement.class), new Iteration<RowDataElement>()
        {
            @Override
            public boolean run(RowDataElement rde)
            {
                SiteImpl site = tableRowToSite( rde );
                if( site != null )
                    result.addSite( site );
                return true;
            }

        });
        if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            result.getOrigin().remove(result.getName());
            return null;
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        log.info("Track created ("+result.getAllSites().getSize()+" sites)");
        return result;
    }

    protected SiteImpl tableRowToSite(RowDataElement rde)
    {
        try
        {
            String chr = rde.getValue( parameters.getChromosomeColumn() ).toString().replaceFirst( "^chr[\\:\\.]?", "" );
            if( chr.equals( "M" ) )
                chr = "MT";
            int from = (Integer)DataType.Integer.convertValue( rde.getValue( parameters.getFromColumn() ) );
            int to = getToByRow( rde );
            int strand = StrandType.STRAND_NOT_APPLICABLE;
            if( !ColumnNameSelector.NONE_COLUMN.equals( parameters.getStrandColumn() ) )
            {
                Object strandObj = rde.getValue( parameters.getStrandColumn() );
                if( strandObj == null )
                    throw new RuntimeException( "Strand is null at row " + rde.getName() );
                String strandStr = strandObj.toString();
                switch( strandStr )
                {
                    case "+":
                        strand = StrandType.STRAND_PLUS;
                        break;
                    case "-":
                        strand = StrandType.STRAND_MINUS;
                        break;
                    default:
                        throw new RuntimeException( "Invalid strand specification '" + strandStr + "' at row " + rde.getName() );
                }
            }

            Iterator<DynamicProperty> iterator = rde.iterator();
            DynamicPropertySet dps = new DynamicPropertySetSupport();
            while( iterator.hasNext() )
            {
                DynamicProperty dynamicProperty = iterator.next();
                String name = dynamicProperty.getName();
                if( !isPropertyUsed( name ) )
                    dps.add( dynamicProperty );
            }
            return new SiteImpl( null, chr, SiteType.TYPE_UNSURE, Basis.BASIS_USER, strand == StrandType.STRAND_MINUS ? to : from,
                    getLengthByRow( rde ), Precision.PRECISION_EXACTLY, strand, null, dps );
        }
        catch( Exception e )
        {
            log.warning( "Error converting row " + rde.getName() + " to site: " + e.getMessage() );
            return null;
        }
    }

    protected int getLengthByRow(RowDataElement rde)
    {
        return (Integer)DataType.Integer.convertValue( rde.getValue( parameters.getToColumn() ) )
                - (Integer)DataType.Integer.convertValue( rde.getValue( parameters.getFromColumn() ) ) + 1;
    }

    protected int getToByRow(RowDataElement rde)
    {
        return (Integer)DataType.Integer.convertValue( rde.getValue( parameters.getToColumn() ) );
    }

    protected boolean isPropertyUsed(String name)
    {
        return name.equals( parameters.getChromosomeColumn() ) || name.equals( parameters.getFromColumn() )
                || name.equals( parameters.getToColumn() ) || name.equals( parameters.getStrandColumn() );
    }

    protected Class<? extends SqlTrack> getTrackClass()
    {
        return null;
    }
}
