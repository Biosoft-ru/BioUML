package biouml.plugins.riboseq.stc_analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.riboseq.datastructure.CenterArea;
import biouml.plugins.riboseq.util.SiteUtil;

public class SelectionTrustClustersAnalysis extends AnalysisMethodSupport<SelectionTrustClustersParameters>
{
    public static final String RELATIVE_PATH_TO_GENES = "../../Tracks/Genes";

    public SelectionTrustClustersAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new SelectionTrustClustersParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkInputTrackSequence();
    }

    private void checkInputTrackSequence()
    {
        final SqlTrack inputTrack = getInputTrack();
        final DataCollection<AnnotatedSequence> seq = inputTrack.getGenomeSelector().getSequenceCollectionPath().optDataCollection(AnnotatedSequence.class);
        if( seq == null )
        {
            throw new IllegalArgumentException( "Invalid sequences collection specified" );
        }
    }

    @Override
    public SqlTrack[] justAnalyzeAndPut() throws Exception
    {
        final SqlTrack inputTrack = getInputTrack();
        final SqlTrack yesTrack = createYesTrack( inputTrack );
        final SqlTrack noTrack = createNoTrack( inputTrack );
        final SqlTrack unTrack = createUndefinedTrack( inputTrack );

        final HashSet<String> houseKeepingGenesIdSet = getGenesId();
        final Track geneTrack = getGeneTrack( inputTrack );

        runAnalyze( inputTrack, geneTrack, houseKeepingGenesIdSet, yesTrack, noTrack, unTrack );

        return new SqlTrack[] {yesTrack, noTrack, unTrack};
    }

    private SqlTrack getInputTrack()
    {
        final DataElementPath inputPath = parameters.getInputPath();

        return inputPath.getDataElement( SqlTrack.class );
    }

    private SqlTrack createYesTrack(Track inputTrack) throws Exception
    {
        final DataElementPath outputPathYesTrack = parameters.getOutputPathYesTrack();

        return SqlTrack.createTrack( outputPathYesTrack, inputTrack );
    }

    private SqlTrack createNoTrack(Track inputTrack) throws Exception
    {
        final DataElementPath outputPathNoTrack = parameters.getOutputPathNoTrack();

        return SqlTrack.createTrack( outputPathNoTrack, inputTrack );
    }

    private SqlTrack createUndefinedTrack(Track inputTrack) throws Exception
    {
        final DataElementPath outputPathUndefinedTrack = parameters.getOutputPathUndefinedTrack();

        return SqlTrack.createTrack( outputPathUndefinedTrack, inputTrack );
    }

    private HashSet<String> getGenesId()
    {
        final HashSet<String> ensemblIdHashSet = new HashSet<>();

        final DataElementPath pathToHousekeepingGenes = parameters.getPathToHousekeepingGenes();
        final TableDataCollection tableDataCollection = pathToHousekeepingGenes.getDataElement( TableDataCollection.class );

        for( RowDataElement row : tableDataCollection )
        {
            final String ensemblId = row.getName();

            ensemblIdHashSet.add( ensemblId );
        }

        return ensemblIdHashSet;
    }

    private Track getGeneTrack(SqlTrack inputTrack)
    {
        final GenomeSelector genomeSelector = inputTrack.getGenomeSelector();
        final DataElementPath sequencePath = genomeSelector.getSequenceCollectionPath();

        final DataElementPath trackPath = sequencePath.getRelativePath( RELATIVE_PATH_TO_GENES );

        return trackPath.getDataElement( Track.class );
    }

    private void runAnalyze(SqlTrack inputTrack, Track geneTrack, HashSet<String> houseKeepingGenesIdSet, SqlTrack yesTrack,
            SqlTrack noTrack, SqlTrack unTrack) throws Exception
    {
        final BasicGenomeSelector genomeSelector = inputTrack.getGenomeSelector();

        final DataElementPathSet chrPathSet = genomeSelector.getSequenceCollectionPath().getChildren();
        for( DataElementPath chrPath : chrPathSet )
        {
            final Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();

            final DataCollection<Site> genes = geneTrack.getSites( chrPath.toString(), 0, chr.getLength() );
            final DataCollection<Site> cluster = inputTrack.getSites( chrPath.toString(), 0, chr.getLength() );

            final List<Site> pcGeneDirectedList = new ArrayList<>();
            final List<Site> pcGeneReversedList = new ArrayList<>();
            getProteinCodingGenes( genes, pcGeneDirectedList, pcGeneReversedList );

            final List<Site> clusterDirectedList = new ArrayList<>();
            final List<Site> clusterReversedList = new ArrayList<>();
            readClustersFromDataCollection( cluster, clusterDirectedList, clusterReversedList );

            SiteUtil.leftIntervalSort( pcGeneDirectedList );
            SiteUtil.leftIntervalSort( pcGeneReversedList );

            SiteUtil.leftIntervalSort( clusterDirectedList );
            SiteUtil.leftIntervalSort( clusterReversedList );

            trustSeparation( clusterDirectedList, pcGeneDirectedList, houseKeepingGenesIdSet, yesTrack, unTrack, noTrack );
            trustSeparation( clusterReversedList, pcGeneReversedList, houseKeepingGenesIdSet, yesTrack, unTrack, noTrack );
        }

        yesTrack.finalizeAddition();
        noTrack.finalizeAddition();
        unTrack.finalizeAddition();

        parameters.getOutputPathYesTrack().save( yesTrack );
        parameters.getOutputPathNoTrack().save( noTrack );
        parameters.getOutputPathUndefinedTrack().save( unTrack );
    }

    private void getProteinCodingGenes(DataCollection<Site> genes, List<Site> directedPCGeneList, List<Site> reversedPCGeneList)
    {
        final String PROTEIN_CODING_LABEL = "protein_coding";

        for( Site site : genes )
        {
            final String siteType = site.getType();
            if( siteType.equals( PROTEIN_CODING_LABEL ) )
            {
                if( SiteUtil.isSiteReversed( site ) )
                {
                    reversedPCGeneList.add( site );
                }
                else
                {
                    directedPCGeneList.add( site );
                }
            }
        }
    }

    private void readClustersFromDataCollection(DataCollection<Site> clusterList, List<Site> directedClusterList,
            List<Site> reversedClusterList)
    {
        for( Site cluster : clusterList )
        {
            if( SiteUtil.isSiteReversed( cluster ) )
            {
                reversedClusterList.add( cluster );
            }
            else
            {
                directedClusterList.add( cluster );
            }
        }
    }

    private void trustSeparation(List<Site> clusterList, List<Site> pcGeneList, HashSet<String> houseKeepingGenesIdSet, SqlTrack yesTrack,
            SqlTrack undefinedTrack, SqlTrack noTrack)
    {
        if( clusterList.isEmpty() )
        {
            return;
        }

        if( pcGeneList.isEmpty() )
        {
            for( Site cluster : clusterList )
            {
                noTrack.addSite( cluster );
            }
        }

        Set<Site> intersectedGeneSet = new HashSet<>();
        for( Site cluster : clusterList )
        {
            // getting all genes which start before current cluster end
            final int clusterTo = cluster.getTo();
            final Iterator<Site> pcGeneListIterator = pcGeneList.iterator();

            Site gene;
            if( pcGeneListIterator.hasNext() )
            {
                gene = pcGeneListIterator.next();
                int geneFrom = gene.getFrom();
                while( pcGeneListIterator.hasNext() && ( geneFrom <= clusterTo ) )
                {
                    intersectedGeneSet.add( gene );
                    gene = pcGeneListIterator.next();
                    geneFrom = gene.getFrom();
                }
            }

            final Interval clusterInterval = cluster.getInterval();
            final Iterator<Site> intersectedGeneIterator = intersectedGeneSet.iterator();
            while( intersectedGeneIterator.hasNext() )
            {
                gene = intersectedGeneIterator.next();
                final Interval geneInterval = gene.getInterval();
                if( !clusterInterval.intersects( geneInterval ) )
                {
                    intersectedGeneIterator.remove();
                }
            }

            if( intersectedGeneSet.isEmpty() )
            {
                noTrack.addSite( cluster );
            }
            else
            {
                boolean yesTrackFlag = false;
                for( Site pcGene : intersectedGeneSet )
                {
                    final String geneEnsemblId = String.valueOf( pcGene.getProperties().getValue( "id" ) );
                    if( houseKeepingGenesIdSet.contains( geneEnsemblId ) )
                    {
                        final List<Interval> geneExonIntervalList = SiteUtil.getExonsFromProperty( pcGene );
                        final List<Interval> clusterExonIntervalList = SiteUtil.getExonsFromProperty( cluster );

                        final CenterArea geneArea = new CenterArea( geneExonIntervalList );
                        final CenterArea clusterArea = new CenterArea( clusterExonIntervalList );

                        if( geneArea.inside( clusterArea ) )
                        {
                            yesTrack.addSite( cluster );
                            yesTrackFlag = true;
                            break;
                        }
                    }
                }
                if( !yesTrackFlag )
                {
                    undefinedTrack.addSite( cluster );
                }
            }
        }
    }
}
