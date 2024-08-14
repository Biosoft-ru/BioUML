package biouml.plugins.ensembl.tracks;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MergedTrack.IntervalSet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Gene track
 */
public class GeneTrack extends SQLBasedEnsemblTrack
{
    private static final PropertyDescriptor ID_PD = StaticDescriptor.create("id");
    private static final PropertyDescriptor SYMBOL_PD = StaticDescriptor.create("symbol");
    private static final PropertyDescriptor TRANSCRIPTS_PD = StaticDescriptor.create("transcripts");
    private static final PropertyDescriptor EXONS_PD = StaticDescriptor.create("exons");

    private Index titleIndex;

    public GeneTrack(DataCollection<?> origin)
    {
        this("Genes", origin);
    }

    protected GeneTrack(String name, DataCollection<?> origin)
    {
        super(name, origin, 300000000, "gene");
    }

    protected void fillGeneProperties(final Site s, final String geneId)
    {
        if( titleIndex == null )
            titleIndex = DataElementPath.create(this).getRelativePath("../../Data/gene").getDataCollection().getInfo().getQuerySystem()
                    .getIndex("title");
        DynamicPropertySet properties = s.getProperties();
        try
        {
            SqlUtil.stream(
                    getConnection(),
                    "SELECT t.name,t.description,g.value FROM gene_attrib g,attrib_type t WHERE g.gene_id=" + geneId
                            + " AND g.attrib_type_id=t.attrib_type_id",
                    rs -> new DynamicProperty( rs.getString( 1 ), rs.getString( 2 ), String.class, rs.getString( 3 ) ) ).forEach(
                    properties::add );
            Object symbol = titleIndex.get(properties.getValue("id"));
            if( symbol != null )
                properties.add(new DynamicProperty(SYMBOL_PD, String.class, symbol.toString().replaceFirst("\\s\\(.+\\)$", "")));
            @SuppressWarnings ( "serial" )
            class ExonsProperty extends DynamicProperty
            {
                public ExonsProperty()
                {
                    super(EXONS_PD, String.class, null);
                }

                @Override
                public Object getValue()
                {
                    if( value == null )
                    {
                        try
                        {
                            IntervalSet intervals = new IntervalSet();
                            SqlUtil.stream(
                                    getConnection(),
                                    "select e.seq_region_start,e.seq_region_end from exon e join exon_transcript using(exon_id) join transcript t using(transcript_id) where gene_id="
                                            + geneId, rs -> new Interval( rs.getInt( 1 ), rs.getInt( 2 ) ).translateToSite( s ) ).forEach(
                                    intervals::add );
                            List<String> exons = new ArrayList<>();
                            for( Interval interval : intervals )
                            {
                                exons.add(interval.toString());
                            }
                            value = String.join(";", exons);
                        }
                        catch( BiosoftSQLException e )
                        {
                            new DataElementReadException(e, GeneTrack.this, "exons").log();
                            value = "";
                        }
                    }
                    return value;
                }

                @Override
                public String toString()
                {
                    return "name: " + getDescriptor().getName() + ", type:" + getType() + ", value: " + getValue();
                }
            }
            // Exon-intronic structure
            properties.add(new ExonsProperty());
            properties.add(new TranscriptsProperty(geneId));
        }
        catch( Exception e )
        {
            new DataElementReadException(e, this, "properties of gene#" + geneId).log();
        }
    }

    @Override
    protected Site createSite(ResultSet rs, Sequence sequence) throws SQLException
    {
        Site result;
        String name = rs.getString(1);
        int length = rs.getInt(3) - rs.getInt(2) + 1;
        int strand = rs.getInt(5) == 1 ? Site.STRAND_PLUS : Site.STRAND_MINUS;
        int start = rs.getInt(5) == 1 ? rs.getInt(2) : rs.getInt(3);
        result = new SiteImpl(null, name, rs.getString(4), Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY, strand, sequence,
                new DynamicPropertySetAsMap());
        result.getProperties().add(new DynamicProperty(ID_PD, String.class, rs.getString(6)));
        fillGeneProperties(result, name);
        return result;
    }

    @SuppressWarnings ( "serial" )
    private class TranscriptsProperty extends DynamicProperty
    {
        String geneId;

        TranscriptsProperty(String geneId)
        {
            super(TRANSCRIPTS_PD, Interval[].class);
            this.geneId = geneId;
        }

        @Override
        public Object getValue()
        {
            if( value == null )
            {
                try
                {
                    value = SqlUtil.stream(
                            getConnection(),
                            "SELECT seq_region_start, seq_region_end, seq_region_strand FROM transcript WHERE gene_id=" + geneId,
                            rs -> rs.getInt( 3 ) == -1 ?
                                    new Interval( rs.getInt( 2 ), rs.getInt( 1 ) ) :
                                    new Interval( rs.getInt( 1 ), rs.getInt( 2 ) ) ).toArray( Interval[]::new );
                }
                catch( BiosoftSQLException e )
                {
                    new DataElementReadException(e, GeneTrack.this, "transcripts").log();
                    value = new Interval[0];
                }
            }
            return value;
        }
    }

    @Override
    protected String getSliceQueryTemplate()
    {
        return "SELECT g.gene_id,seq_region_start,seq_region_end,biotype,seq_region_strand,gs.stable_id "
                + "FROM {table} g JOIN gene_stable_id gs USING(gene_id) WHERE {range}";
    }

    @Override
    protected String getSiteQueryTemplate()
    {
        return getSliceQueryTemplate() + " AND gene_id={site}";
    }
    
    
    public static final String INDEX_ENSEMBL_ID = "Ensembl id";
    public static final String INDEX_GENE_SYMBOL = "Gene symbol";
    @Override
    public List<String> getIndexes()
    {
        List<String> result = new ArrayList<>();
        result.add( INDEX_ENSEMBL_ID );
        if(SqlUtil.isIndexExists( getConnection(), "xref", "display_label_idx" ))
            //Use following statement to build index: create index display_label_idx on xref(display_label);
            result.add( INDEX_GENE_SYMBOL );
        return result;
    }
    
    @Override
    public List<Site> queryIndex(String index, String query)
    {
        if(!getIndexes().contains( index ))
            throw new IllegalArgumentException( "Unknown index: " + index  );
        String sql;
        if(index.equals( INDEX_ENSEMBL_ID ))
        {
            sql = "SELECT g.gene_id,seq_region_start,seq_region_end,biotype,seq_region_strand,gs.stable_id,seq_region.name "
                + "FROM " + mainTable + " g JOIN gene_stable_id gs USING(gene_id) "
                + " JOIN seq_region using(seq_region_id) JOIN coord_system using(coord_system_id)"
                + " WHERE gs.stable_id=" + SqlUtil.quoteString( query )
                + " AND coord_system.name='chromosome' AND coord_system.rank=1";
            
            
        }else if(index.equals( INDEX_GENE_SYMBOL ))
        {
            sql = "SELECT g.gene_id,seq_region_start,seq_region_end,biotype,seq_region_strand,gs.stable_id,seq_region.name "
            + "FROM " + mainTable + " g JOIN gene_stable_id gs USING(gene_id) JOIN xref on(display_xref_id=xref_id)"
            + " JOIN seq_region using(seq_region_id) JOIN coord_system using(coord_system_id)"
            + " WHERE xref.display_label=" + SqlUtil.quoteString( query )
            + " AND coord_system.name='chromosome' AND coord_system.rank=1";
        }else
            throw new AssertionError();

        List<Site> result  = new ArrayList<>();
        try (Statement st = SqlUtil.createStatement( getConnection() ); ResultSet rs = st.executeQuery( sql ))
        {
            while( rs.next() )
            {
                String chrName = rs.getString( 7 );
                AnnotatedSequence chr = defaultSequencesPath.getChildPath( chrName ).optDataElement( AnnotatedSequence.class );
                if(chr != null)//skip noncanonical chromosomes missing in sequences collection
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
