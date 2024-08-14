package biouml.plugins.ensembl.access;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.ensembl.driver.CoreDriver;
import org.ensembl.driver.CoreDriverFactory;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.Connectors.ConnectionInfo;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.util.HashMapSoftValues;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.model.Module;
import biouml.plugins.ensembl.type.Gene;

import com.developmentontheedge.beans.DynamicProperty;

public class EnsemblGeneDataCollection extends SqlDataCollection<Gene>
{
    /**
     * Ensembl core driver that provides adaptors for accessing to ensembl core
     * databases.
     */
    protected CoreDriver driver;

    protected int size = -1;

    protected List<String> nameList;

    // /////////////////////////////////////////////////////////////////////////
    // Reused Ensembl CoreDriver properties
    //

    public static final String ATTR_STATUS_NAME = "status";
    public static final String ATTR_VERSION_NAME = "version";
    public static final String ATTR_CREATEDDATE_NAME = "createdDate";
    protected static final PropertyDescriptor STATUS_DESCRIPTOR = StaticDescriptor.create(ATTR_STATUS_NAME);
    protected static final PropertyDescriptor VERSION_DESCRIPTOR = StaticDescriptor.create(ATTR_VERSION_NAME);
    protected static final PropertyDescriptor CREATEDDATE_DESCRIPTOR = StaticDescriptor.create(ATTR_CREATEDDATE_NAME);

    private static final String GENE_INFO_QUERY = "select status,display_label,g.description,s.version,s.created_date,s.modified_date,r.name,"
            + "seq_region_start,seq_region_end,seq_region_strand from gene_stable_id s,seq_region r,coord_system c,gene g left join xref x"
            + " on(x.xref_id=g.display_xref_id) where g.gene_id=s.gene_id and r.seq_region_id=g.seq_region_id and c.coord_system_id=r.coord_system_id and c.rank=1 and s.stable_id=";

    protected String species;

    /**
     * Creates EnsemblGeneDataCollection using properties file.
     *
     * The property file also contains all settings for creating CoreDriver by
     * CoreDriverFactory.
     *
     * @see CoreDriverFactory.createCoreDriver
     */
    public EnsemblGeneDataCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);

        try
        {
            Properties moduleProperties = Module.getModule(this).getInfo().getProperties();
            ConnectionInfo connectionInfo = Connectors.getConnectionInfo( moduleProperties );
            driver = CoreDriverFactory.createCoreDriver( connectionInfo.getHost(), connectionInfo.getPort(), connectionInfo.getDb(),
                    connectionInfo.getUser(), connectionInfo.getPassword() );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create Ensembl core driver: " + e);
            throw e;
        }

        v_cache = new HashMapSoftValues();
    }

    @Override
    protected void init()
    {
        // Do not call super.init!
    }

    // //////////////////////////////////////////////////////////////////////////
    // Public methods for ru.biosoft.access.core.DataCollection
    //

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public @Nonnull Class<Gene> getDataElementType()
    {
        return Gene.class;
    }

    @Override
    public int getSize()
    {
        if( size > -1 )
            return size;

        synchronized( this )
        {
            if( nameList != null )
            {
                size = nameList.size();
                return size;
            }
            size = getNameList().size();
        }

        return size;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        synchronized( this )
        {
            if( nameList == null )
            {
                String table = "gene_stable_id";
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
                ResultSet rs = st.executeQuery( GENE_INFO_QUERY + SqlUtil.quoteString( name ) ))
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

    @Override
    public void close() throws Exception
    {
        driver.closeAllConnections();
        super.close();
    }

    @Override
    protected void finalize()
    {
        try
        {
            close();
        }
        catch( Throwable t )
        {
            ExceptionRegistry.log(t);
        }
    }

    @Override
    public boolean contains(String name)
    {
        return getNameList().contains(name);
    }

    @Override
    public Gene put(Gene obj)
    {
        throw new UnsupportedOperationException("You cannot put new Ensembl gene");
    }

    @Override
    public void remove(String name) throws Exception
    {
        throw new UnsupportedOperationException("You cannot remove Ensembl gene");
    }
}
