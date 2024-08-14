package biouml.plugins.ensembl.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import one.util.streamex.StreamEx;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil;
import biouml.plugins.ensembl.access.EnsemblSequenceTransformer;

public class GeneOverlapStatistics extends AnalysisMethodSupport<GeneOverlapStatisticsParameters>
{
    public GeneOverlapStatistics(DataCollection<?> origin, String name)
    {
        super( origin, name, new GeneOverlapStatisticsParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath ensemblPath = TrackUtils.getEnsemblPath( parameters.getSpecies(), parameters.getOutputFolder() );
        final DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath(ensemblPath);

        final Map<GeneElementType, Statistics> stats = new EnumMap<>( GeneElementType.class );
        for( GeneElementType t : GeneElementType.values() )
            stats.put( t, new Statistics() );

        jobControl.forCollection( sequencesPath.getChildren(), chromosomePath -> {

            try
            {
                Sequence chromosome = chromosomePath.getDataElement(AnnotatedSequence.class).getSequence();
                final IntervalMap<GeneElementType> geneElements = loadGeneElements( chromosomePath );

                Track track = parameters.getInputTrack().getDataElement(Track.class);
                DataCollection<Site> sites;

                sites = track.getSites( chromosomePath.toString(), chromosome.getStart(),
                        chromosome.getStart() + chromosome.getLength() );
                jobControl.forCollection( DataCollectionUtils.asCollection( sites, Site.class ), site -> {
                    Collection<GeneElementType> overlapping = geneElements.getIntervals( site.getFrom(), site.getTo() );
                    GeneElementType bestType = GeneElementType.INTERGENIC;
                    if( !overlapping.isEmpty() )
                        for( GeneElementType type : EnumSet.copyOf( overlapping ) )
                        {
                            stats.get( type ).overlapCount++;
                            if( type.ordinal() < bestType.ordinal() )
                                bestType = type;
                        }
                    stats.get( bestType ).belongToCount++;
                    return true;
                } );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not fetch sites from " + parameters.getInputTrack(), e );
                return false;
            }

            return true;
        } );

        DataCollectionUtils.createSubCollection( parameters.getOutputFolder() );

        DataElementPath overlappingSitesPath = parameters.getOutputFolder().getChildPath( "Overlapping sites" );
        DataElementPath classifiedSitesPath = parameters.getOutputFolder().getChildPath( "Classified sites" );

        TableDataCollection overlappinngSites = TableDataCollectionUtils.createTableDataCollection( overlappingSitesPath );
        overlappinngSites.getColumnModel().addColumn( "Count", Integer.class );

        TableDataCollection classifiedSites = TableDataCollectionUtils.createTableDataCollection( classifiedSitesPath );
        classifiedSites.getColumnModel().addColumn( "Count", Integer.class );

        for( GeneElementType t : GeneElementType.values() )
        {
            Statistics s = stats.get( t );
            TableDataCollectionUtils.addRow( overlappinngSites, t.title, new Object[] {s.overlapCount} );
            TableDataCollectionUtils.addRow( classifiedSites, t.title, new Object[] {s.belongToCount} );
        }

        overlappingSitesPath.save( overlappinngSites );
        classifiedSitesPath.save( classifiedSites );

        return new Object[] {overlappinngSites, createPlotForTable( overlappingSitesPath ), classifiedSites,
                createPlotForTable( classifiedSitesPath )};
    }

    private ImageDataElement createPlotForTable(DataElementPath tablePath) throws Exception
    {
        DataElementPath imagePath = tablePath.getSiblingPath( tablePath.getName() + " chart" );
        DefaultPieDataset dataset = new DefaultPieDataset();
        for( RowDataElement row : tablePath.getDataElement(TableDataCollection.class) )
            dataset.setValue( row.getName(), (Number)row.getValues()[0] );

        PiePlot plot = new PiePlot( dataset );
        plot.setLabelGenerator( new StandardPieSectionLabelGenerator( "{0} = {1}" ) );
        JFreeChart chart = new JFreeChart( plot );

        ImageDataElement image = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), chart.createBufferedImage(
                500, 500 ) );
        imagePath.save( image );
        return image;
    }

    static enum GeneElementType
    {
        FIVE_PRIME ( "5'" ), THREE_PRIME ( "3'" ), INTRON ( "Intron" ), EXON ( "Exon" ), INTERGENIC ( "Intergenic" );
        String title;
        private GeneElementType(String title)
        {
            this.title = title;
        }
    }

    static class Statistics
    {
        /** Number of sites overlapping elements of given type */
        int overlapCount;

        /** Number of sites that belong to given element type */
        int belongToCount;
    }

    private IntervalMap<GeneElementType> loadGeneElements(DataElementPath chromosomePath) throws Exception
    {
        IntervalMap<GeneElementType> result = new IntervalMap<>();

        DataCollection<?> chromosomeCollection = chromosomePath.getParentCollection();
        String coordSystemConstraints = EnsemblSequenceTransformer.getCoordSystemConstraints(chromosomeCollection);
        Connection con = SqlConnectionPool.getConnection( chromosomeCollection );
        SqlUtil.executeUpdate( con, "SET SESSION group_concat_max_len = 1000000" );

        Statement st = null;
        ResultSet rs = null;
        List<Interval> genes;
        try
        {
            st = con.createStatement();
            rs = st
                    .executeQuery( "SELECT transcript.seq_region_start, transcript.seq_region_end, transcript.seq_region_strand, "
                            + "group_concat(exon.seq_region_start), group_concat(exon.seq_region_end)"
                            + " FROM transcript LEFT JOIN exon_transcript using(transcript_id) JOIN exon using(exon_id) JOIN seq_region on(transcript.seq_region_id=seq_region.seq_region_id)"
                            + " WHERE name='" + chromosomePath.getName() + "' AND " + coordSystemConstraints + " GROUP BY transcript_id" );

            genes = new ArrayList<>();
            while( rs.next() )
            {
                int from = rs.getInt( 1 );
                int to = rs.getInt( 2 );

                boolean reverse = rs.getInt( 3 ) == StrandType.STRAND_MINUS;

                int left = from - ( reverse ? parameters.getThreePrimeFlankSize() : parameters.getFivePrimeFlankSize() );
                int right = to + ( reverse ? parameters.getFivePrimeFlankSize() : parameters.getThreePrimeFlankSize() );

                result.add( left, from - 1, reverse ? GeneElementType.THREE_PRIME : GeneElementType.FIVE_PRIME );
                result.add( to + 1, right, reverse ? GeneElementType.FIVE_PRIME : GeneElementType.THREE_PRIME );

                genes.add( new Interval( left, right ) );


                String[] exonStarts = TextUtil.split( rs.getString( 4 ), ',' );
                String[] exonEnds = TextUtil.split( rs.getString( 5 ), ',' );
                List<Interval> exons = StreamEx.zip( exonStarts, exonEnds,
                        (start, end) -> new Interval( Integer.parseInt( start ), Integer.parseInt( end ) ) )
                        .sorted().toList();
                for( Interval exon : exons )
                    result.add( exon.getFrom(), exon.getTo(), GeneElementType.EXON );
                for( Interval intron : complement( new Interval( from, to ), exons ) )
                    result.add( intron.getFrom(), intron.getTo(), GeneElementType.INTRON );
            }
        }
        finally
        {
            SqlUtil.close(st, rs);
        }


        Sequence seq = chromosomePath.getDataElement(AnnotatedSequence.class).getSequence();
        Collections.sort( genes );
        for( Interval intergenic : complement( new Interval( seq.getStart(), seq.getStart() + seq.getLength() - 1 ), genes ) )
            result.add( intergenic.getFrom(), intergenic.getTo(), GeneElementType.INTERGENIC );

        return result;
    }

    private List<Interval> complement(Interval parent, List<Interval> sortedIntervals)
    {
        return StreamEx.of( new Interval( parent.getFrom() ) ).append( sortedIntervals ).append( new Interval( parent.getTo() ) )
                .pairMap( (left, right) -> new Interval( left.getTo() + 1, right.getFrom() - 1 ) )
                .filter( interval -> interval.getLength() > 0 ).toList();
    }

}
