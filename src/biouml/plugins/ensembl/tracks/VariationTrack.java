package biouml.plugins.ensembl.tracks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.VariationElement;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * Repeat track
 */
public class VariationTrack extends SQLBasedEnsemblTrack
{
    //protected boolean onlySNP;

    private String variationDb; 
    // TODO: support onlySNP mode
    public VariationTrack(DataCollection<?> origin, boolean onlySNP, String variationDb)
    {
        super("Variations", origin, 1000000, variationDb + ".variation_feature");
        this.variationDb = variationDb;
        this.maxSiteLength = 500;   // calculation of this value on the fly is quite slow
        //this.onlySNP = onlySNP;
    }
    
    @Override
    protected Site createSite(ResultSet rs, Sequence sequence) throws SQLException
    {
        String id = rs.getString(1);
        String name = rs.getString(2);
        int start = rs.getInt(5)==1?rs.getInt(3):rs.getInt(4);
        int length = rs.getInt(4) - rs.getInt(3) + 1;
        int strand = rs.getInt(5)==1?Site.STRAND_PLUS:Site.STRAND_MINUS;
        String alleleStr = rs.getString(6);
        return VariationElement.createVariationSite( sequence, id, name, start, length, strand, alleleStr );
    }


    @Override
    protected String getSliceQueryTemplate()
    {
        return "SELECT variation_feature_id,variation_name,seq_region_start,seq_region_end,"
                + "seq_region_strand,allele_string FROM {table} WHERE allele_string like '%/%' AND {range}";
    }

    @Override
    protected String getSiteQueryTemplate()
    {
        return getSliceQueryTemplate()+" AND variation_feature_id={site}";
    }
    
    public static final String INDEX_VARIATION_NAME = "Variation name";
    
    @Override
    public List<String> getIndexes()
    {
        List<String> result = new ArrayList<>();
        if(SqlUtil.isIndexExists( getConnection(), "variation_feature", "variation_name_idx", variationDb ))
            //Use following statement to build index: create index variation_name_idx on variation_feature(variation_name(12));
            result.add( INDEX_VARIATION_NAME );
        return result;
    }
    
    @Override
    public List<Site> queryIndex(String index, String query)
    {
        if(!getIndexes().contains( index ))
            throw new IllegalArgumentException("Unknown index: " + index);
        
        String sql =  "SELECT variation_feature_id,variation_name,seq_region_start,seq_region_end,"
                    + "seq_region_strand,allele_string,seq_region.name" 
                    + " FROM " + mainTable + " JOIN seq_region using(seq_region_id) JOIN coord_system using(coord_system_id)"
                    + " WHERE variation_name=" + SqlUtil.quoteString( query ) + " AND coord_system.name='chromosome' AND coord_system.rank=1";

        List<Site> result  = new ArrayList<>();
        try (Statement st = SqlUtil.createStatement( getConnection() ); ResultSet rs = st.executeQuery( sql ))
        {
            while( rs.next() )
            {
                String chrName = rs.getString( 7 );
                AnnotatedSequence chr = defaultSequencesPath.getChildPath( chrName ).getDataElement( AnnotatedSequence.class );
                result.add(createSite(rs, chr.getSequence()));
            }
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        return result;
    }
}
