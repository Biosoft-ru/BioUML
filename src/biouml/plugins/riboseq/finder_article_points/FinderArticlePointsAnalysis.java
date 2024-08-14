package biouml.plugins.riboseq.finder_article_points;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ComparisonSets;
import biouml.plugins.riboseq.comparison_article.util_data_structure.GeneralPointInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.TrackPointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.TrackPointInfo;

import com.developmentontheedge.beans.DynamicPropertySet;

public class FinderArticlePointsAnalysis extends AnalysisMethodSupport<FinderArticlePointsParameters>
{
    public FinderArticlePointsAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new FinderArticlePointsParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkInputTracksSequence( parameters.getInputAllClusterTrack() );
        checkInputTracksSequence( parameters.getInputFilteredTrack() );
        checkInputTracksSequence( parameters.getInputSvmYesTrack() );
    }

    private void checkInputTracksSequence(DataElementPath trackPath)
    {
        SqlTrack track = getTrackFromPath( trackPath );
        DataCollection<AnnotatedSequence> seq = track.getGenomeSelector().getSequenceCollectionPath().optDataCollection(AnnotatedSequence.class);

        if( seq == null )
        {
            throw new IllegalArgumentException( "Invalid sequences collection specified" );
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection articlePointTable = getArticlePointTable();
        final SqlTrack allClusterTrack = getTrackFromPath( parameters.getInputAllClusterTrack() );
        final SqlTrack filteredTrack = getTrackFromPath( parameters.getInputFilteredTrack() );
        final SqlTrack svmTrack = getTrackFromPath( parameters.getInputSvmYesTrack() );

        final List<GeneralPointInfo> articlePointList = readPointsFromTable( articlePointTable );

        final ComparisonSets allClusterArticlePointStatistic = findArticlePointsInTrack( articlePointList, allClusterTrack );
        final ComparisonSets filteredTrackArticlePointStatistic = findArticlePointsInTrack( articlePointList, filteredTrack );
        final ComparisonSets svmTrackArticlePointStatistic = findArticlePointsInTrack( articlePointList, svmTrack );

        final TableDataCollection statisticTable = createStatisticTable( allClusterArticlePointStatistic,
                filteredTrackArticlePointStatistic, svmTrackArticlePointStatistic );

        return statisticTable;
    }

    private SqlTrack getTrackFromPath(DataElementPath trackPath)
    {
        return trackPath.getDataElement( SqlTrack.class );
    }

    private TableDataCollection getArticlePointTable()
    {
        final DataElementPath articlePointTable = parameters.getInputArticlePointTable();

        return articlePointTable.getDataElement( TableDataCollection.class );
    }

    private List<GeneralPointInfo> readPointsFromTable(TableDataCollection articlePointTable)
    {
        final List<GeneralPointInfo> pointInfoList = new ArrayList<>();

        for( RowDataElement row : articlePointTable )
        {
            final int point = (Integer)row.getValue( "point" );
            final String chr = (String)row.getValue( "chr" );
            final boolean strandPlus = ( (String)row.getValue( "strand" ) ).charAt( 0 ) == '+';

            final GeneralPointInfo pointInfo = new GeneralPointInfo( point, chr, strandPlus );
            pointInfoList.add( pointInfo );
        }

        return pointInfoList;
    }

    private ComparisonSets findArticlePointsInTrack(List<GeneralPointInfo> articlePointList, SqlTrack track)
    {
        final int delta = 20;
        ComparisonSets comparisonSets = new ComparisonSets();

        final TrackPointCollection trackPointCollection = calculateTrackPoints( track );

        final List<String> chrList = trackPointCollection.getChrList();
        for( String chrName : chrList )
        {
            List<GeneralPointInfo> articleFilteredPointList = getFilterPointList( articlePointList, chrName, true );
            List<GeneralPointInfo> trackPointList = trackPointCollection.getPointList( chrName, true );
            comparisonSets = comparePointList( articleFilteredPointList, trackPointList, delta, comparisonSets );

            articleFilteredPointList = getFilterPointList( articlePointList, chrName, false );
            trackPointList = trackPointCollection.getPointList( chrName, false );
            comparisonSets = comparePointList( articleFilteredPointList, trackPointList, delta, comparisonSets );
        }

        return comparisonSets;
    }

    private List<GeneralPointInfo> getFilterPointList(List<GeneralPointInfo> articlePointList, String chrName, boolean strandPlus)
    {
        final List<GeneralPointInfo> articleFilteredPointList = new ArrayList<>();

        for( GeneralPointInfo pointInfo : articlePointList )
        {
            if( pointInfo.chrName.equals( chrName ) && pointInfo.strandPlus == strandPlus )
            {
                articleFilteredPointList.add( pointInfo );
            }
        }

        return articleFilteredPointList;
    }

    private TrackPointCollection calculateTrackPoints(SqlTrack track)
    {
        final TrackPointCollection genePointCollection = new TrackPointCollection();

        final DataCollection<Site> siteDataCollection = track.getAllSites();
        for( Site site : siteDataCollection )
        {
            final DynamicPropertySet properties = site.getProperties();

            final String chrName = site.getSequence().getName();
            final int initCodonPosition = Integer.parseInt( (String)properties.getValue( "initCodonPosition" ) );
            final boolean strandPlus = site.getStrand() != StrandType.STRAND_MINUS;

            final TrackPointInfo pointInfo = new TrackPointInfo( chrName, initCodonPosition, strandPlus );
            genePointCollection.add( pointInfo );
        }

        return genePointCollection;
    }

    private ComparisonSets comparePointList(List<GeneralPointInfo> articlePointList, List<GeneralPointInfo> trackPointList, int delta,
            ComparisonSets comparisonSets)
    {
        for( GeneralPointInfo articlePointInfo : articlePointList )
        {
            final Interval pointInterval = new Interval( articlePointInfo.point - delta / 2, articlePointInfo.point + delta / 2 );

            for( GeneralPointInfo trackPointInfo : trackPointList )
            {
                final int trackPoint = trackPointInfo.point;
                if( pointInterval.inside( trackPoint ) )
                {
                    comparisonSets.addToIntersection( articlePointInfo, trackPointInfo );
                }
            }
        }
        comparisonSets.summarize( articlePointList, trackPointList );

        return comparisonSets;
    }

    private TableDataCollection createStatisticTable(ComparisonSets allClusterArticlePointStatistic, ComparisonSets filteredTrackStatistic,
            ComparisonSets svmTrackArticlePointStatistic)
    {
        final DataElementPath outputStatisticTablePath = parameters.getOutputStatisticTable();
        final TableDataCollection statisticTable = TableDataCollectionUtils.createTableDataCollection( outputStatisticTablePath );

        final ColumnModel columnModel = statisticTable.getColumnModel();
        columnModel.addColumn( "intersectionArticle", Integer.class );
        columnModel.addColumn( "intersectionTrack", Integer.class );
        columnModel.addColumn( "uniqueArticle", Integer.class );
        columnModel.addColumn( "uniqueTrack", Integer.class );

        ComparisonSets statistic = allClusterArticlePointStatistic;
        Object[] dateValues1 = {statistic.getIntersectionArticlePointNumber(), statistic.getIntersectionTrackPointNumber(),
                statistic.getUniqueArticlePointNumber(), statistic.getUniqueTrackPointNumber()};
        TableDataCollectionUtils.addRow( statisticTable, "allClusterArticlePointStatistic", dateValues1 );

        statistic = filteredTrackStatistic;
        Object[] dateValues2 = {statistic.getIntersectionArticlePointNumber(), statistic.getIntersectionTrackPointNumber(),
                statistic.getUniqueArticlePointNumber(), statistic.getUniqueTrackPointNumber()};
        TableDataCollectionUtils.addRow( statisticTable, "filteredTrackStatistic", dateValues2 );

        statistic = svmTrackArticlePointStatistic;
        Object[] dateValues3 = {statistic.getIntersectionArticlePointNumber(), statistic.getIntersectionTrackPointNumber(),
                statistic.getUniqueArticlePointNumber(), statistic.getUniqueTrackPointNumber()};
        TableDataCollectionUtils.addRow( statisticTable, "svmTrackArticlePointStatistic", dateValues3 );

        return statisticTable;
 }
}
