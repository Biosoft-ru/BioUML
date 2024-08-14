package biouml.plugins.ensembl.access;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Module;
import biouml.plugins.ensembl.type.Gene;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;

public class EnsemblGeneDataCollection3 extends EnsemblGeneDataCollection {

	private static final String GENE_INFO_QUERY3 = "select display_label,g.description,s.version,s.created_date,s.modified_date,r.name,"
            + "seq_region_start,seq_region_end,seq_region_strand from gene_stable_id s,seq_region r,coord_system c,gene g left join xref x"
            + " on(x.xref_id=g.display_xref_id) where g.gene_id=s.gene_id and r.seq_region_id=g.seq_region_id and c.coord_system_id=r.coord_system_id and c.rank=1 and s.stable_id=";

	public EnsemblGeneDataCollection3(DataCollection parent, Properties properties) throws Exception
	{
		super(parent, properties);
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
                ResultSet rs = st.executeQuery( GENE_INFO_QUERY3 + SqlUtil.quoteString( name ) ))
        {
            if( !rs.next() )
                return null;
            String chr = rs.getString( 6 );
            int from = rs.getInt( 7 );
            int to = rs.getInt( 8 );
            int strand = rs.getInt( 9 ) == 1 ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
            DataElementPath ensemblPath = DataElementPath.create( this ).getRelativePath( "../.." );
            Sequence sequence = TrackUtils.getPrimarySequencesPath( ensemblPath ).getChildPath( chr )
                    .getDataElement( AnnotatedSequence.class ).getSequence();
            Site site = new SiteImpl( null, name, SiteType.TYPE_GENE, Basis.BASIS_ANNOTATED, strand == StrandType.STRAND_PLUS ? from : to,
                    to - from + 1, strand, sequence );
            Gene gene = new Gene(this, name, site, driver);
            gene.setTitle(rs.getString(1));
            String description = rs.getString(2);
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
            Date createdDate = rs.getDate(4);
            if(createdDate != null)
                gene.setDate( ( new SimpleDateFormat("dd-MMM-yyyy") ).format(createdDate));
            if( species != null )
                gene.setSpecies(species);
            //gene.getAttributes().add(new DynamicProperty(STATUS_DESCRIPTOR, String.class, rs.getString(1)));
            gene.getAttributes().add(new DynamicProperty(VERSION_DESCRIPTOR, Integer.class, rs.getInt(3)));
            if(createdDate != null)
                gene.getAttributes()
                    .add(new DynamicProperty(CREATEDDATE_DESCRIPTOR, String.class, ( new SimpleDateFormat("dd-MMM-yyyy") ).format(createdDate)));
            return gene;
        }
    }
}
