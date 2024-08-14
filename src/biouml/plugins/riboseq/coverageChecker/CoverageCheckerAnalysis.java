package biouml.plugins.riboseq.coverageChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
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
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointAdditionInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointInfo;
import biouml.plugins.riboseq.util.SiteUtil;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class CoverageCheckerAnalysis extends AnalysisMethodSupport<CoverageCheckerParameters>
{
    public static final int MATCH = 1;
    public static final int MISMATCH = 0;

    public static final int DELTA = 10;

    public CoverageCheckerAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new CoverageCheckerParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkInputTrackSequence();
    }

    private void checkInputTrackSequence()
    {
        BAMTrack inputTrack = getBamTrack();

        final DataElementPath sequenceCollectionPath = inputTrack.getGenomeSelector().getSequenceCollectionPath();
        if( sequenceCollectionPath == null )
        {
            throw new IllegalArgumentException( "Invalid sequences collection specified" );
        }
        else
        {
            DataCollection<AnnotatedSequence> seq = sequenceCollectionPath.optDataCollection(AnnotatedSequence.class);
            if( seq == null )
            {
                throw new IllegalArgumentException( "Invalid sequences collection specified" );
            }
        }
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final BAMTrack inputBamTrack = getBamTrack();

        final TableDataCollection articleCSV = getArticleCSV();
        final TableDataCollection geneInfo = getGeneInfo();
        final ArticleGenePointCollection articlePoints = calculateArticlePoints( articleCSV, geneInfo );

        // resultData[0] - mismatch; resultData[1] - match;
        int[] resultData = computeCoverage( inputBamTrack, articlePoints );

        TableDataCollection statisticTable = createStatisticTable( );
        statisticTable = fillStatisticTable( statisticTable, resultData );

        return statisticTable;
    }

    private int[] computeCoverage(BAMTrack track, ArticleGenePointCollection articlePoints) throws Exception
    {
        int[] resultData = new int[2];

        final List<String> chrNameList = articlePoints.getChrList();
        for( String chrName : chrNameList )
        {
            final List<ArticleGenePointInfo> chrArticlePointList = articlePoints.getChrPointList( chrName );
            resultData = computeCoverageForChr( track, chrArticlePointList, chrName, resultData );
        }

        return resultData;
    }

    private int[] computeCoverageForChr(BAMTrack track, List<ArticleGenePointInfo> pointList, String chrName, int[] resultData)
            throws Exception
    {
        for( ArticleGenePointInfo pointInfo : pointList )
        {
            final int point = pointInfo.point;
            final boolean strandPlus = pointInfo.additionInfo.strandPlus;

            final DataCollection<Site> siteIntersectedPointDC = track.getSites( chrName, point, point );
            int indexIncrement = MISMATCH;
            if( siteIntersectedPointDC.isEmpty() )
            {
                resultData[MISMATCH]++;
            }
            else
            {
                for( Site site : siteIntersectedPointDC )
                {
                    final boolean siteStrandPlus = site.getStrand() != StrandType.STRAND_MINUS;
                    if( strandPlus == siteStrandPlus )
                    {
                        indexIncrement = checkMatchSite( site, point );
                        if( indexIncrement == MATCH )
                        {
                            resultData[MATCH]++;
                            break;
                        }
                    }
                }
                if( indexIncrement == MISMATCH )
                {
                    resultData[MISMATCH]++;
                }
            }
        }

        return resultData;
    }

    private int checkMatchSite(Site site, int point)
    {
        final Cigar cigar = SiteUtil.getCigar( site );

        if( !cigarContainsNOperator( cigar ) )
        {
            return checkMatchCenterSolidSite( site, point );
        }
        else
        {
            return checkMatchIntronSite( site, cigar, point );
        }
    }

    private int checkMatchIntronSite(Site site, Cigar cigar, int point)
    {
        List<CigarElement> cigarElements = cigar.getCigarElements();

        int center = getCenterSiteWithIntron( site, cigarElements );
        final Interval centerInterval = new Interval( center - DELTA, center + DELTA );

        if( centerInterval.inside( point ) )
        {
            int pointOffset = point - site.getFrom();

            for( CigarElement element : cigarElements )
            {
                pointOffset -= element.getLength();
                if( pointOffset <= 0 )
                {
                    CigarOperator operator = element.getOperator();
                    if( operator == CigarOperator.N )
                    {
                        return MISMATCH;
                    }
                    else
                    {
                        return MATCH;
                    }
                }
            }
            return MISMATCH;
        }
        else
        {
            return MISMATCH;
        }
    }

    private int getCenterSiteWithIntron(Site site, List<CigarElement> cigarElements)
    {
        int lengthExons = 0;
        for( CigarElement element : cigarElements )
        {
            if( element.getOperator() != CigarOperator.N )
            {
                lengthExons += element.getLength();
            }
        }
        int halfLengthExon = lengthExons / 2;

        int center = site.getFrom();
        for( CigarElement curElement : cigarElements )
        {
            final int curElementLength = curElement.getLength();
            if( curElement.getOperator() == CigarOperator.N )
            {
                center += curElementLength;
            }
            else
            {
                if( curElementLength <= halfLengthExon )
                {
                    halfLengthExon -= curElementLength;
                    center += curElementLength;
                }
                else
                {
                    center += halfLengthExon;
                    break;
                }
            }
        }

        return center;
    }

    private boolean cigarContainsNOperator(Cigar cigar)
    {
        if( cigar.numCigarElements() == 1 )
        {
            return false;
        }
        else
        {
            final List<CigarElement> elementList = cigar.getCigarElements();
            for( CigarElement element : elementList )
            {
                final CigarOperator operator = element.getOperator();
                if( operator == CigarOperator.N )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private int checkMatchCenterSolidSite(Site site, int point)
    {
        final int center = site.getFrom() + site.getLength() / 2;
        final Interval centerInterval = new Interval( center - DELTA, center + DELTA );

        if( centerInterval.inside( point ) )
        {
            return MATCH;
        }
        else
        {
            return MISMATCH;
        }
    }
    private void addArticlePoint(SqlTrack track, ArticleGenePointCollection pointCollection) throws Exception
    {
        BasicGenomeSelector genomeSelector = track.getGenomeSelector();
        for( AnnotatedSequence annotatedSequence : genomeSelector.getSequenceCollection() )
        {
            final Sequence chr = annotatedSequence.getSequence();
            final String chrName = "chr" + chr.getName();

            final List<ArticleGenePointInfo> chrPointList = pointCollection.getChrPointList( chrName );
            if( chrPointList == null )
            {
                continue;
            }
            for( ArticleGenePointInfo point : chrPointList )
            {
                final int strand = point.additionInfo.strandPlus ? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
                final Integer txStart = point.additionInfo.exonStarts.get( 0 );
                final Integer txEnd = point.additionInfo.exonEnds.get( point.additionInfo.exonCount - 1 );

                final DynamicPropertySet properties = new DynamicPropertySetSupport();
                properties.add( new DynamicProperty( "geneName", String.class, point.geneName ) );
                properties.add( new DynamicProperty( "initCodonPosition", int.class, point.initCodonPosition ) );
                properties.add( new DynamicProperty( "initContext", String.class, point.initContext ) );
                properties.add( new DynamicProperty( "txStart", int.class, txStart ) );
                properties.add( new DynamicProperty( "txEnd", int.class, txEnd ) );

                final Site site = new SiteImpl( null, null, SiteType.TYPE_RBS, Site.BASIS_PREDICTED, point.point, 1,
                        Precision.PRECISION_EXACTLY, strand, chr, properties );

                track.addSite( site );
            }
        }
    }

    private void addCenterPoint(SqlTrack track, CenterPointCollection pointCollection) throws Exception
    {
        BasicGenomeSelector genomeSelector = track.getGenomeSelector();
        for( AnnotatedSequence annotatedSequence : genomeSelector.getSequenceCollection() )
        {
            final Sequence chr = annotatedSequence.getSequence();
            final String chrName = "chr" + chr.getName();

            final List<CenterPoint> chrPointList = pointCollection.getChrPointList( chrName );
            if( chrPointList == null )
            {
                continue;
            }
            for( CenterPoint point : chrPointList )
            {
                final int strand = point.getStrand();
                final int counter = point.getCounter();
                final int pointCoordinate = point.getPoint();

                final DynamicPropertySet properties = new DynamicPropertySetSupport();
                properties.add( new DynamicProperty( "counter", String.class, counter ) );

                final Site site = new SiteImpl( null, null, SiteType.TYPE_UNSURE, Site.BASIS_PREDICTED, pointCoordinate, 1,
                        Precision.PRECISION_EXACTLY, strand, chr, properties );

                track.addSite( site );
            }
        }
    }

    private WritableTrack fillCenterTrack(WritableTrack centerTrack, CenterPointCollection centerPoints,
            ArticleGenePointCollection articlePoints) throws Exception
    {
        final SqlTrack centerSqlTrack = (SqlTrack)centerTrack;

        addCenterPoint( centerSqlTrack, centerPoints );
        addArticlePoint( centerSqlTrack, articlePoints );

        centerSqlTrack.finalizeAddition();
        centerSqlTrack.getCompletePath().save( centerSqlTrack );

        return centerTrack;
    }

    private BAMTrack getBamTrack()
    {
        DataElementPath inputBamTrackPath = parameters.getInputBamTrackPath();

        return inputBamTrackPath.getDataElement( BAMTrack.class );
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
        final String[] exonStartsStrArray = exonStartsStr.split( "," );
        for( int i = 0; i < exonCount; i++ )
        {
            exonList.add( Integer.parseInt( exonStartsStrArray[i] ) );
        }

        return exonList;
    }

    private WritableTrack createCenterPointTrack(BAMTrack inputBamTrack) throws Exception
    {
        Properties properties = new Properties();
        final DataElementPath outputTrackPath = null;//parameters.getOutputTrackPath();

        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, outputTrackPath.getName() );
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY,
                inputBamTrack.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );

        return TrackUtils.createTrack( outputTrackPath.getParentCollection(), properties );
    }

    private TableDataCollection createStatisticTable()
    {
        final DataElementPath outputDataElementPath = parameters.getOutputStatisticTablePath();
        final TableDataCollection statisticTable = TableDataCollectionUtils.createTableDataCollection( outputDataElementPath );

        final ColumnModel columnModel = statisticTable.getColumnModel();
        columnModel.addColumn( "mismatch", Integer.class );
        columnModel.addColumn( "match", Integer.class );

        return statisticTable;
    }

    private TableDataCollection fillStatisticTable(TableDataCollection statisticTable, int[] statistic)
    {
        final Object[] dataValues = {statistic[MISMATCH], statistic[MATCH]};
        TableDataCollectionUtils.addRow( statisticTable, "result", dataValues );

        return statisticTable;
    }
}