package biouml.plugins.ensembl.tracks;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Repeat track
 */
public class KaryotypeTrack extends SQLBasedEnsemblTrack
{
    private static final PropertyDescriptor NAME_DESCRIPTOR = StaticDescriptor.create("name");
    private static final PropertyDescriptor STAIN_DESCRIPTOR = StaticDescriptor.create("stain");
    
    public KaryotypeTrack(DataCollection<?> origin)
    {
        super("Karyotype", origin, 2000000000, "karyotype");
        viewBuilder = new KaryotypeTrackViewBuilder();
    }

    @Override
    protected Site createSite(ResultSet rs, Sequence sequence) throws SQLException
    {
        String id = rs.getString(1);
        String name = rs.getString(2);
        String stain = rs.getString(3);
        int start = rs.getInt(4);
        int length = rs.getInt(5) - rs.getInt(4) + 1;
        DynamicPropertySet propertySet = new DynamicPropertySetAsMap();
        propertySet.add(new DynamicProperty(NAME_DESCRIPTOR, String.class, name));
        propertySet.add(new DynamicProperty(STAIN_DESCRIPTOR, String.class, stain));
        
        Site site = new SiteImpl(null, id, name, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                Site.STRAND_NOT_APPLICABLE, sequence, propertySet);
        return site;
    }

    @Override
    protected String getSliceQueryTemplate()
    {
        return "SELECT karyotype_id,band,stain,seq_region_start,seq_region_end FROM {table} WHERE {range}";
    }

    @Override
    protected String getSiteQueryTemplate()
    {
        return getSliceQueryTemplate()+" AND karyotype_id={site}";
    }
}
