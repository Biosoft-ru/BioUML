package biouml.plugins.riboseq.comparison_article;

import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointAdditionInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ComparisonSets;
import biouml.plugins.riboseq.comparison_article.util_data_structure.GeneralPointInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.TrackPointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.TrackPointInfo;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComparatorAnalysis extends AnalysisMethodSupport<ComparatorParameters>
{
    final static int DELTA_NUMBER = 10;
    final static int DELTA_VALUE = 10;

    private static final Comparator<GeneralPointInfo> POINT_INFO_COMPARATOR = Comparator.comparingInt( p -> p.point );

    public ComparatorAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ComparatorParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection articleCSV = getArticleCSV();
        final TableDataCollection geneInfo = getGeneInfo();
        final SqlTrack yesTrack = getYesTrack();

        final TrackPointCollection yesTrackPoints = calculateTrackPoints( yesTrack );
        final ArticleGenePointCollection articlePoints = calculateArticlePoints( articleCSV, geneInfo );

        articlePoints.checkContext( yesTrack );

        List<ComparisonSets> comparisonSetsList = fillComparisonSetses( yesTrackPoints, articlePoints );

        final TableDataCollection comparisonSetsTable = createComparisonTable( comparisonSetsList );
        final TableDataCollection uniqueArticlePointTable = createArticlePointTable( comparisonSetsList );

        final Object[] resultOutput = {comparisonSetsTable, uniqueArticlePointTable};

        return resultOutput;
    }

    public SqlTrack getYesTrack()
    {
        final DataElementPath yesTrackPath = parameters.getInputYesTrack();
        final SqlTrack yesTrack = yesTrackPath.getDataElement( SqlTrack.class );

        return yesTrack;
    }

    public TableDataCollection getArticleCSV()
    {
        final DataElementPath articleCSVPath = parameters.getInputArticleCSV();
        final TableDataCollection articleCSV = articleCSVPath.getDataElement( TableDataCollection.class );

        return articleCSV;
    }

    public TableDataCollection getGeneInfo()
    {
        final DataElementPath geneInfoPath = parameters.getInputGeneInfo();
        final TableDataCollection geneInfo = geneInfoPath.getDataElement( TableDataCollection.class );

        return geneInfo;
    }

    public ArticleGenePointCollection calculateArticlePoints(TableDataCollection articleCSV, TableDataCollection geneInfo)
    {
        final ArticleGenePointCollection articleGenePointCollection = getCollectionFromTables( articleCSV, geneInfo );
        articleGenePointCollection.computePoints();

        return articleGenePointCollection;
    }

    private ArticleGenePointCollection getCollectionFromTables(TableDataCollection articleCSV, TableDataCollection geneInfo)
    {
        final ArticleGenePointCollection articleGenePointCollection = new ArticleGenePointCollection();
        for( RowDataElement row : articleCSV )
        {
            final String geneName = (String)row.getValue( "knownGene" );

            final int initCodonPosition = (Integer)row.getValue( "Init Codon [nt]" );
            final String initContext = (String)row.getValue( "Init Context [-3 to +4]" );

            final ArticleGenePointInfo genePoint = new ArticleGenePointInfo( geneName, initCodonPosition, initContext );
            articleGenePointCollection.put( geneName, genePoint );
        }

        for( RowDataElement row : geneInfo )
        {
            final String geneName = row.getName();

            final String chr = (String)row.getValue( "chrom" );
            final boolean strandPlus = ( (String)row.getValue( "strand" ) ).charAt( 0 ) == '+';

            final int exonCount = (Integer)row.getValue( "exonCount" );
            final List<Integer> exonStarts = getValue( row, "exonStarts", exonCount );
            final List<Integer> exonEnds = getValue( row, "exonEnds", exonCount );

            final ArticleGenePointAdditionInfo additionInfo = new ArticleGenePointAdditionInfo( chr, strandPlus, exonCount, exonStarts,
                    exonEnds );
            articleGenePointCollection.addInformation( geneName, additionInfo );
        }
        articleGenePointCollection.restructureOnChr();

        return articleGenePointCollection;
    }

    private List<Integer> getValue(RowDataElement row, String exonStartsNameStr, int exonCount)
    {
        final List<Integer> exonList = new ArrayList<>( exonCount );

        final String exonStartsStr = (String)row.getValue( exonStartsNameStr );
        final String[] exonStartsStrArray = TextUtil.split( exonStartsStr, ',' );
        for( int i = 0; i < exonCount; i++ )
        {
            exonList.add( Integer.parseInt( exonStartsStrArray[i] ) );
        }

        return exonList;
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
            final int clusterLength = site.getLength();

            final TrackPointInfo pointInfo = new TrackPointInfo( chrName, initCodonPosition, strandPlus, clusterLength );
            genePointCollection.add( pointInfo );
        }

        return genePointCollection;
    }

    private ComparisonSets comparePointCollection(ArticleGenePointCollection articlePoints, TrackPointCollection trackPoints, int delta)
    {
        ComparisonSets comparisonSets = new ComparisonSets();

        final List<String> chrNameList = articlePoints.getChrList();
        chrNameList.retainAll( trackPoints.getChrList() );

        for( String chrName : chrNameList )
        {
            comparisonSets = fillComparisonSets( articlePoints, trackPoints, delta, comparisonSets, chrName, true );
            comparisonSets = fillComparisonSets( articlePoints, trackPoints, delta, comparisonSets, chrName, false );
        }

        return comparisonSets;
    }

    private ComparisonSets fillComparisonSets(ArticleGenePointCollection articlePoints, TrackPointCollection trackPoints, int delta,
            ComparisonSets comparisonSets, String chrName, boolean strandPlus)
    {
        List<GeneralPointInfo> articlePointList = articlePoints.getPointList( chrName, strandPlus );
        List<GeneralPointInfo> trackPointList = trackPoints.getPointList( chrName, strandPlus );

        comparisonSets = comparePointList( articlePointList, trackPointList, delta, comparisonSets );

        return comparisonSets;
    }

    private ComparisonSets comparePointList(List<GeneralPointInfo> articlePointList, List<GeneralPointInfo> trackPointList, int delta,
            ComparisonSets comparisonSets)
    {
        Collections.sort( articlePointList, POINT_INFO_COMPARATOR );
        Collections.sort( trackPointList, POINT_INFO_COMPARATOR );

        for( GeneralPointInfo trackPointInfo : trackPointList )
        {
            final Interval trackPointInterval = new Interval( trackPointInfo.point - delta / 2, trackPointInfo.point + delta / 2 );

            boolean isIntersected = false;
            for( GeneralPointInfo articlePointInfo : articlePointList )
            {
                final int articlePoint = articlePointInfo.point;
                if( trackPointInterval.inside( articlePoint ) )
                {
                    comparisonSets.addToIntersection( articlePointInfo, trackPointInfo );
                    isIntersected = true;
                }
            }

            if( !isIntersected && trackPointInfo.length > 10000 )
            {
                isIntersected = true;
            }
        }

        comparisonSets.summarize( articlePointList, trackPointList );

        return comparisonSets;
    }

    private TableDataCollection createComparisonTable(List<ComparisonSets> comparisonSetsList)
    {
        final DataElementPath outputDataElementPath = parameters.getOutputStatistic();
        final TableDataCollection statisticTable = TableDataCollectionUtils.createTableDataCollection( outputDataElementPath );

        final ColumnModel columnModel = statisticTable.getColumnModel();
        columnModel.addColumn( "intersectionArticle", Integer.class );
        columnModel.addColumn( "intersectionTrack", Integer.class );
        columnModel.addColumn( "uniqueArticle", Integer.class );
        columnModel.addColumn( "uniqueTrack", Integer.class );

        for( int i = 1; i <= DELTA_NUMBER; i++ )
        {
            final ComparisonSets comparisonSets = comparisonSetsList.get( i - 1 );
            final Object[] dataValues = {comparisonSets.getIntersectionArticlePointNumber(),
                    comparisonSets.getIntersectionTrackPointNumber(), comparisonSets.getUniqueArticlePointNumber(),
                    comparisonSets.getUniqueTrackPointNumber()};

            TableDataCollectionUtils.addRow( statisticTable, "" + i * DELTA_VALUE, dataValues );
        }

        return statisticTable;
    }

    private List<ComparisonSets> fillComparisonSetses(TrackPointCollection yesTrackPoints, ArticleGenePointCollection articlePoints)
    {
        List<ComparisonSets> comparisonSetsList = new ArrayList<>( DELTA_NUMBER );
        for( int i = 1; i <= DELTA_NUMBER; i++ )
        {
            final ComparisonSets comparisonSetsForSpecificDelta = comparePointCollection( articlePoints, yesTrackPoints, i * DELTA_VALUE );
            comparisonSetsList.add( comparisonSetsForSpecificDelta );
        }
        return comparisonSetsList;
    }

    private TableDataCollection createArticlePointTable(List<ComparisonSets> comparisonSetsList)
    {
        final ComparisonSets firstComparisonSets = comparisonSetsList.get( 0 );
        final ArrayList<GeneralPointInfo> uniqueArticlePointList = firstComparisonSets.getUniqueArticlePointList();
        return createUniqueArticlePointTable( uniqueArticlePointList );
    }

    private TableDataCollection createUniqueArticlePointTable(ArrayList<GeneralPointInfo> articlePointList)
    {
        final DataElementPath outputArticleTablePath = parameters.getOutputUniqueArticlePointTable();
        final TableDataCollection articlePointTable = TableDataCollectionUtils.createTableDataCollection( outputArticleTablePath );

        final ColumnModel columnModel = articlePointTable.getColumnModel();
        columnModel.addColumn( "point", Integer.class );
        columnModel.addColumn( "chr", String.class );
        columnModel.addColumn( "strand", String.class );

        int keyIndex = 0;
        for( GeneralPointInfo pointInfo : articlePointList )
        {
            final char strandChar = pointInfo.strandPlus ? '+' : '-';
            final Object[] dataValues = {pointInfo.point, pointInfo.chrName, strandChar};

            TableDataCollectionUtils.addRow( articlePointTable, String.valueOf( keyIndex ), dataValues );

            keyIndex++;
        }

        return articlePointTable;
    }
}
