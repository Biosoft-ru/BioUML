package biouml.plugins.ensembl.access;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.ensembl.driver.CoreDriverFactory;

import biouml.model.Module;
import biouml.plugins.ensembl.type.Gene;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;

public class EnsemblGeneDataCollection2 extends EnsemblGeneDataCollection
{
    private static final String GENE_INFO_QUERY2 = "select g.status,ga.value as display_label,g.description,g.version,g.created_date,g.modified_date,r.name,g.seq_region_start,g.seq_region_end,g.seq_region_strand from seq_region r,coord_system c,gene_attrib ga, attrib_type at, gene g left join xref x on(x.xref_id=g.display_xref_id) where r.seq_region_id=g.seq_region_id and c.coord_system_id=r.coord_system_id and c.rank=1 and ga.gene_id=g.gene_id and ga.attrib_type_id=at.attrib_type_id and at.name='Name' and g.stable_id=";

    /**
     * Creates EnsemblGeneDataCollection2 using properties file.
     *
     * The property file also contains all settings for creating CoreDriver by
     * CoreDriverFactory.
     *
     * @see CoreDriverFactory.createCoreDriver
     */
    public EnsemblGeneDataCollection2(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        synchronized( this )
        {
            if( nameList == null )
            {
                String table = "gene";
                String idField = "stable_id";

                try
                {
                    String nameListQuery = "SELECT " + idField + " FROM " + table + " ORDER BY " + idField;
                    nameList = SqlUtil.queryStrings(getConnection(), nameListQuery);
                    sortNameList(nameList);
                }
                catch( BiosoftSQLException e )
                {
                    throw new DataElementReadException(e, this);
                }
            }
        }
        return nameList;
    }

    @Override
    protected Gene doGet(String name) throws Exception
    {
        if( species == null )
        {
            try
            {
                Module module = Module.optModule(this);
                if( module == null )
                {
                    log.warning("Cannot init species for " + DataElementPath.create(this, name));
                }
                else
                {
                    species = module.getInfo().getProperty(DataCollectionUtils.SPECIES_PROPERTY);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot init species for " + DataElementPath.create(this, name), e);
            }
        }

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery( GENE_INFO_QUERY2 + SqlUtil.quoteString( name ) ))
        {
            if( !rs.next() )
                return null;
            String chr = rs.getString( 7 );
            int from = rs.getInt( 8 );
            int to = rs.getInt( 9 );
            int strand = rs.getInt( 10 ) == 1 ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
            DataElementPath ensemblPath = DataElementPath.create( this ).getRelativePath( "../.." );
            Sequence sequence = TrackUtils.getPrimarySequencesPath( ensemblPath ).getChildPath( chr )
                    .getDataElement( AnnotatedSequence.class ).getSequence();
            Site site = new SiteImpl( null, name, SiteType.TYPE_GENE, Basis.BASIS_ANNOTATED, strand == StrandType.STRAND_PLUS ? from : to,
                    to - from + 1, strand, sequence );
            Gene gene = new Gene(this, name, site, driver);
            gene.setTitle(rs.getString(2));
            String description = rs.getString(3);
            if( description != null )
            {
                Matcher matcher = Pattern.compile("(.+)\\s+\\[(.+)\\]\\s*").matcher(description);
                if( matcher.matches() )
                {
                    gene.setDescription(matcher.group(1));
                    gene.setComment(matcher.group(2));
                }
                else
                {
                    gene.setDescription(description);
                }
            }
            gene.setDate( ( new SimpleDateFormat("dd-MMM-yyyy") ).format(rs.getDate(6)));
            if( species != null )
                gene.setSpecies(species);
            gene.getAttributes().add(new DynamicProperty(STATUS_DESCRIPTOR, String.class, rs.getString(1)));
            gene.getAttributes().add(new DynamicProperty(VERSION_DESCRIPTOR, Integer.class, rs.getInt(4)));
            gene.getAttributes()
                    .add(new DynamicProperty(CREATEDDATE_DESCRIPTOR, String.class, ( new SimpleDateFormat("dd-MMM-yyyy") ).format(rs
                            .getDate(5))));
            return gene;
        }
    }
}
