package biouml.plugins.ensembl.tracks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;

/**
 * Repeat track
 */
public class RepeatTrack extends SQLBasedEnsemblTrack
{
    public RepeatTrack(DataCollection origin)
    {
        super( "Repeats", origin, 1000000, "repeat_feature" );
    }

    public static final String REPEAT_CONSENSUS_TABLE = "repeat_consensus";

    @Override
    protected Site createSite(ResultSet rs, Sequence sequence) throws SQLException
    {
        Site result;
        String name = rs.getString(1);
        int start = rs.getInt(2);
        int length = rs.getInt(3) - start;
        result = new SiteImpl(null, name, rs.getString(4), Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                Site.STRAND_PLUS, sequence, null);
        result.getProperties().add(new DynamicProperty("Type", String.class, rs.getString(5)));
        result.getProperties().add(new DynamicProperty("Class", String.class, rs.getString(6)));
        return result;
    }

    @Override
    protected String getSliceQueryTemplate()
    {
        return "SELECT t.repeat_feature_id,t.seq_region_start,t.seq_region_end,c.repeat_name,c.repeat_type,c.repeat_class FROM {table} t,"
                + REPEAT_CONSENSUS_TABLE + " c WHERE {range} AND c.repeat_consensus_id=t.repeat_consensus_id";
    }

    @Override
    protected String getSiteQueryTemplate()
    {
        return getSliceQueryTemplate()+" AND t.repeat_feature_id={site}";
    }
    
    public String[] getAllRepTypes() throws SQLException
    {
    	String query = "SELECT DISTINCT(repeat_type) FROM " + REPEAT_CONSENSUS_TABLE;
    	List<String> result = new ArrayList<>();
    	 try (Statement st = SqlUtil.createStatement( getConnection() ); ResultSet rs = st.executeQuery( query ))
         {
             while( rs.next() )
                 result.add( rs.getString(1) );
             return result.toArray(new String[result.size()]);
         }
    }
    
}
