package biouml.plugins.ensembl.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.ensembl.type.Transcript;
import one.util.streamex.StreamEx;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.util.LazyValue;

public class TranscriptTransformer extends SqlTransformerSupport<Transcript>
{
    private final static String trxColName     = "txid";
    private final static String geneColName    = "gnid";
    private final static String geneSymColName = "label";
    private final static String chrColName     = "chr";
    private final static String startColName   = "tstart";
    private final static String endColName     = "tend";
    private final static String strandColName  = "strand";
    private final static String typeColName    = "biotype";
    //private final static String statusColName  = "status";
    
    //private final static String transcriptQuery = "SELECT tsi.stable_id AS 'txid',gsi.stable_id AS 'gnid',xr.display_label as 'label',sqr.name AS 'chr',trx.seq_region_start AS 'tstart',trx.seq_region_end AS 'tend',trx.seq_region_strand AS 'strand',trx.biotype AS 'biotype',trx.status AS 'status' FROM transcript trx JOIN transcript_stable_id tsi using(transcript_id) JOIN gene gn ON trx.gene_id=gn.gene_id JOIN gene_stable_id gsi ON gn.gene_id=gsi.gene_id JOIN seq_region sqr ON trx.seq_region_id=sqr.seq_region_id JOIN xref xr ON gn.display_xref_id=xr.xref_id";
    private final static String transcriptQuery = "SELECT tsi.stable_id AS 'txid',gsi.stable_id AS 'gnid',xr.display_label as 'label',sqr.name AS 'chr',trx.seq_region_start AS 'tstart',trx.seq_region_end AS 'tend',trx.seq_region_strand AS 'strand',trx.biotype AS 'biotype' FROM transcript trx JOIN transcript_stable_id tsi using(transcript_id) JOIN gene gn ON trx.gene_id=gn.gene_id JOIN gene_stable_id gsi ON gn.gene_id=gsi.gene_id JOIN seq_region sqr ON trx.seq_region_id=sqr.seq_region_id JOIN xref xr ON gn.display_xref_id=xr.xref_id";
    private final static String exonQuery       = "SELECT esi.stable_id AS 'eid',exn.seq_region_start AS 'estart',exn.seq_region_end AS 'eend',ext.rank AS 'erank' FROM exon exn JOIN exon_stable_id esi using(exon_id) JOIN exon_transcript ext ON exn.exon_id=ext.exon_id JOIN transcript_stable_id ON ext.transcript_id=transcript_stable_id.transcript_id WHERE transcript_stable_id.stable_id=";
    private final static String cdsQuery        = "SELECT lesi.stable_id AS 'fexon',tr.seq_start AS 'fstart',resi.stable_id 'lexon',tr.seq_end AS 'lend' FROM translation tr JOIN exon fx ON tr.start_exon_id=fx.exon_id JOIN exon_stable_id lesi ON fx.exon_id=lesi.exon_id JOIN exon lx ON tr.end_exon_id=lx.exon_id JOIN exon_stable_id resi ON resi.exon_id=lx.exon_id  JOIN transcript_stable_id ON tr.transcript_id=transcript_stable_id.transcript_id WHERE transcript_stable_id.stable_id=";
    
    public final static Set<String> codingTypes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("IG_C_gene",
                                                                                    "IG_D_gene",
                                                                                    "IG_gene",
                                                                                    "IG_J_gene",
                                                                                    "IG_LV_gene",
                                                                                    "IG_M_gene",
                                                                                    "IG_V_gene",
                                                                                    "IG_Z_gene",
                                                                                    "nonsense_mediated_decay",
                                                                                    "nontranslating_CDS",
                                                                                    "non_stop_decay",
                                                                                    "polymorphic",
                                                                                    "polymorphic_pseudogene",
                                                                                    "protein_coding",
                                                                                    "TR_C_gene",
                                                                                    "TR_D_gene",
                                                                                    "TR_gene",
                                                                                    "TR_J_gene",
                                                                                    "TR_V_gene")));
    
    @Override
    public Class<Transcript> getTemplateClass()
    {
        return Transcript.class;
    }

    @Override
    public Transcript create(ResultSet resultSet, Connection connection)
            throws Exception
    {
        String biotype = resultSet.getString(typeColName);
        String trxName = resultSet.getString(trxColName);
        String chr = resultSet.getString( chrColName );
        int from = resultSet.getInt( startColName );
        int to = resultSet.getInt( endColName );
        int strand = resultSet.getInt( strandColName ) == 1 ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;

        Sequence sequence = getChromosomesPath().getChildPath( chr ).getDataElement( AnnotatedSequence.class ).getSequence();
        Site site = new SiteImpl( null, trxName, SiteType.TYPE_PRIM_TRANSCRIPT, Basis.BASIS_ANNOTATED,
                strand == StrandType.STRAND_PLUS ? from : to, to - from + 1, strand, sequence );
        
        Transcript transcript = new Transcript(owner, trxName, site);
        
        transcript.getAttributes().add(new DynamicProperty("gene", String.class, resultSet.getString(geneColName)));
        transcript.getAttributes().add(new DynamicProperty("gene_symbol", String.class, resultSet.getString(geneSymColName)));
        transcript.getAttributes().add(new DynamicProperty("chr", String.class, chr));
        transcript.getAttributes().add(new DynamicProperty("start", Integer.class, from));
        transcript.getAttributes().add(new DynamicProperty("end", Integer.class, to));
        transcript.getAttributes().add(new DynamicProperty("strand", String.class, strand == StrandType.STRAND_PLUS ? "+" : "-"));
        transcript.getAttributes().add(new DynamicProperty("biotype", String.class, biotype));
        //transcript.getAttributes().add(new DynamicProperty("status", String.class, resultSet.getString(statusColName)));
        try
        {
            String[] exons = getExons(trxName,connection);
            if (exons.length > 0)
            {
                transcript.getAttributes().add(new DynamicProperty("exons",String[].class,exons));
            }
        }
        catch (Exception e)
        {
        }
        if (codingTypes.contains(biotype))
        {
            try
            {
                String cdsInfo = getCDSInfo(trxName,connection);
                if (cdsInfo.length() > 0)
                {
                    transcript.getAttributes().add(new DynamicProperty("cds",String.class,cdsInfo));
                }
            }
            catch (Exception e)
            {
            }
        }
        return transcript;
    }
    
    
    private String getCDSInfo(String trxId, Connection con) throws Exception
    {
        String cdsInfo = "";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery( cdsQuery + validateValue( trxId ) ))
        {
            if (rs.next())
            {
                cdsInfo = rs.getString("fexon") + ":" + rs.getString("fstart") + " " + rs.getString("lexon") + ":" + rs.getString("lend");
            }
        }
        catch( SQLException e )
        {
            throw e;
        }
        return cdsInfo;
    }
    
    
    private String[] getExons(String trxId, Connection con) throws Exception
    {
        String[] exons = new String[0];
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery( exonQuery + validateValue( trxId ) ))
        {
            List<String> E = new ArrayList<>();
            while (rs.next())
            {
                E.add(rs.getString("eid") + ":" + rs.getString("estart") + "-" + rs.getString("eend") + ";" + rs.getString("erank"));
            }
            if (E.size() > 0)
            {
                exons = new String[E.size()];
                int e = 0;
                for (Object x : E)
                {
                    exons[e] = (String)x;
                    e++;
                }
            }
        }
        catch( SQLException e )
        {
            throw e;
        }
        return exons;
    }
    
    private DataElementPath getChromosomesPath()
    {
        DataElementPath ensemblPath = DataElementPath.create( owner ).getRelativePath( "../.." );
        return TrackUtils.getPrimarySequencesPath( ensemblPath );
    }
    
    private final LazyValue<String> chromosomeConstraints = new LazyValue<>( "Chromosome constraints",
            () -> StreamEx.of( getChromosomesPath().getDataCollection().names() ).joining( "','", "name IN ('", "')" ) );
    
    @Override
    public String getNameListQuery()
    {
        return "SELECT transcript_stable_id.stable_id FROM transcript_stable_id JOIN transcript using(transcript_id) JOIN seq_region using(seq_region_id) WHERE " + chromosomeConstraints.get()
                + " ORDER BY 1";
    }
    
    @Override
    public String getElementExistsQuery(String name)
    {
        return "SELECT transcript_stable_id.stable_id FROM transcript_stable_id JOIN transcript using(transcript_id) JOIN seq_region using(seq_region_id) WHERE " + chromosomeConstraints.get()
                + " AND transcript_stable_id.stable_id=" + validateValue( name );
    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT COUNT(*) FROM transcript JOIN seq_region using(seq_region_id) WHERE " + chromosomeConstraints.get();
    }
    
    @Override
    /*
     * This query is used to initialize the object with one row per transcript
     * 
     * @see ru.biosoft.access.SqlTransformerSupport#getSelectQuery()
     */
    public String getSelectQuery()
    {
        //return "SELECT * FROM " + table;
        return transcriptQuery;
    }
    
    @Override
    public String getElementQuery(String name)
    {
        String query = getSelectQuery();
        if( query.contains(" WHERE ") )
        {
            return query + " AND tsi.stable_id=" + validateValue(name);
        }
        return query + " WHERE tsi.stable_id=" + validateValue(name);
    }
    
    @Override
    public boolean init(SqlDataCollection<Transcript> owner)
    {
        table = "transcript_stable_id";
        idField = "stable_id";
        return super.init(owner);
    }

    @Override
    public void addInsertCommands(Statement statement, Transcript de) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

    @Override
    public void addUpdateCommands(Statement statement, Transcript de) throws Exception
    {
        throw new UnsupportedOperationException("Collection is readonly");
    }

}
