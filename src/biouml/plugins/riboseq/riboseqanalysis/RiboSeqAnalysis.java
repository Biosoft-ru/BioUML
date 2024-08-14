package biouml.plugins.riboseq.riboseqanalysis;

import java.util.List;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import biouml.plugins.riboseq.datastructure.ChromosomeClusters;
import biouml.plugins.riboseq.datastructure.ClusterInfo;
import biouml.plugins.riboseq.datastructure.SiteCluster;
import biouml.plugins.riboseq.util.SiteUtil;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class RiboSeqAnalysis extends AnalysisMethodSupport<RiboSeqParameters>
{
    public RiboSeqAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new RiboSeqParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkInputTrackSequence();
        checkGreater( "minNumberSites", 0 );
        checkGreater( "maxLengthCluster", 0 );
    }

    private void checkInputTrackSequence()
    {
        BAMTrack inputTrack = getBAMTrackFromInputPath();

        final DataElementPath sequenceCollectionPath = inputTrack.getGenomeSelector().getSequenceCollectionPath();
        final IllegalArgumentException exception = new IllegalArgumentException( "Invalid sequences collection specified" );

        if( sequenceCollectionPath == null )
        {
            throw exception;
        }
        else
        {
            DataCollection<AnnotatedSequence> seq = sequenceCollectionPath.optDataCollection(AnnotatedSequence.class);
            if( seq == null )
            {
                throw exception;
            }
        }
    }

    @Override
    public WritableTrack justAnalyzeAndPut() throws Exception
    {
        final BAMTrack inputBamTrack = getBAMTrackFromInputPath();
        WritableTrack resultClusterTrack = getTrackFromOutputPath( inputBamTrack );

        resultClusterTrack = runAnalyze( inputBamTrack, resultClusterTrack );

        return resultClusterTrack;
    }

    private WritableTrack runAnalyze(BAMTrack bamTrack, WritableTrack resultTrack) throws Exception
    {
        final int minNumberSites = parameters.getMinNumberSites();
        final int maxLengthCluster = parameters.getMaxLengthCluster();

        BasicGenomeSelector genomeSelector = bamTrack.getGenomeSelector();
        for( AnnotatedSequence annotatedSequence : genomeSelector.getSequenceCollection() )
        {
            Sequence chr = annotatedSequence.getSequence();

            DataCollection<Site> sites = bamTrack.getSites( DataElementPath.create( annotatedSequence ).toString(), 0, chr.getLength() );

            ChromosomeClusters chromosomeClusters = new ChromosomeClusters( sites, chr, minNumberSites, maxLengthCluster );
            chromosomeClusters.runClustering();

            chromosomeClusters.filterClusters();

            resultTrack = appendChrClustersToTrack( resultTrack, chromosomeClusters );
        }

        resultTrack.finalizeAddition();
        parameters.getOutputPath().save( resultTrack );

        return resultTrack;
    }

    private WritableTrack appendChrClustersToTrack(WritableTrack track, ChromosomeClusters chrClusters)
    {
        if( chrClusters.empty() )
        {
            return track;
        }

        for( int idCluster = 0; idCluster < chrClusters.sizeClusters(); idCluster++ )
        {
            SiteCluster cluster = chrClusters.getCluster( idCluster );

            SiteImpl clusterSite = createClusterWithInfo( chrClusters, cluster );

            track.addSite( clusterSite );
        }

        return track;
    }

    private SiteImpl createClusterWithInfo(ChromosomeClusters chrClusters, SiteCluster cluster)
    {
        boolean isReversed = cluster.isReversed();
        Interval interval = cluster.getInterval();

        String placeDescription = isReversed ? "_rev" : "";
        int start = isReversed ? interval.getTo() : interval.getFrom();
        int length = interval.getLength();
        int strand = isReversed ? StrandType.STRAND_MINUS : StrandType.STRAND_PLUS;

        DynamicPropertySet properties = new DynamicPropertySetSupport();

        int numberSites = cluster.getNumberOfSites();
        ClusterInfo info = cluster.getInfo();
        int clusterStartModa = info.startModa;
        int clusterStartCodonShift = info.shiftStartCodon;
        int[] modasHistogrameArray = info.modasHistogram;
        int clusterInitCodonPosition = info.initCodonPosition;

        Sequence chrSeq = chrClusters.getChromosome();

        final String chrName = chrClusters.getChromosome().getName();
        final String idName = chrName + "chr_" + start + placeDescription;
        final int clusterLength = cluster.getInterval().getLength();
        final List<Interval> clusterExons = cluster.getExons();

        properties.add( new DynamicProperty( "idName", String.class, idName ) );
        properties.add( new DynamicProperty( "sites", int.class, numberSites ) );
        properties.add( new DynamicProperty( "startModaOffset", int.class, clusterStartModa ) );
        properties.add( new DynamicProperty( "startCodonOffset", int.class, clusterStartCodonShift ) );
        properties.add( new DynamicProperty( "lengthCluster", int.class, clusterLength ) );
        properties.add( new DynamicProperty( "initCodonPosition", int.class, clusterInitCodonPosition ) );
        properties.add( new DynamicProperty( "exons", String.class, SiteUtil.exonListToStr( clusterExons ) ) );

        final int maxInd = 11;
        int startPos = - ( maxInd - 1 ) / 2;
        for( int modaInd = 0; modaInd < maxInd; modaInd++ )
        {
            properties.add( new DynamicProperty( "modasHistogram" + startPos, int.class, modasHistogrameArray[modaInd] ) );
            startPos++;
        }

        return new SiteImpl( null, null, SiteType.TYPE_RBS, Site.BASIS_PREDICTED, start, length, Precision.PRECISION_EXACTLY, strand,
                chrSeq, properties );
    }

    private BAMTrack getBAMTrackFromInputPath()
    {
        DataElementPath inputPath = parameters.getInputPath();

        return inputPath.getDataElement( BAMTrack.class );
    }

    private WritableTrack getTrackFromOutputPath(BAMTrack inputBamTrack) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, parameters.getOutputPath().getName() );
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY,
                inputBamTrack.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
        return TrackUtils.createTrack( parameters.getOutputPath().getParentCollection(), properties );
    }
}
