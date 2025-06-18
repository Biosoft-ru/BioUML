package biouml.plugins.riboseq.article_track;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TextUtil2;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointAdditionInfo;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointInfo;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class ArticleTrackCreatorAnalysis extends AnalysisMethodSupport<ArticleTrackCreatorParameters>
{
    public ArticleTrackCreatorAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new ArticleTrackCreatorParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkReferenceTrackSequence();
    }

    private void checkReferenceTrackSequence()
    {
        SqlTrack inputTrack = getReferenceTrackFromInputPath();
        DataCollection<AnnotatedSequence> seq = inputTrack.getGenomeSelector().getSequenceCollectionPath().optDataCollection(AnnotatedSequence.class);
        if( seq == null )
        {
            throw new IllegalArgumentException( "Invalid sequences collection specified" );
        }
    }

    private SqlTrack getReferenceTrackFromInputPath()
    {
        final DataElementPath referenceTrackPath = parameters.getInputReferenceTrack();

        return referenceTrackPath.getDataElement( SqlTrack.class );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection articleCSV = getArticleCSV();
        final TableDataCollection geneInfo = getGeneInfo();
        SqlTrack articleTrack = createArticleTrack();

        final ArticleGenePointCollection articlePoints = calculateArticlePoints( articleCSV, geneInfo );

        articleTrack = addArticlePoint( articleTrack, articlePoints );

        return articleTrack;
    }

    private SqlTrack createArticleTrack() throws Exception
    {
        final Track referenceTrack = getReferenceTrackFromInputPath();

        return SqlTrack.createTrack( parameters.getOutputTrack(), referenceTrack );
    }

    private TableDataCollection getArticleCSV()
    {
        final DataElementPath articleCSVPath = parameters.getInputArticleCSV();

        return articleCSVPath.getDataElement( TableDataCollection.class );
    }

    private TableDataCollection getGeneInfo()
    {
        final DataElementPath geneInfoPath = parameters.getInputGeneInfo();

        return geneInfoPath.getDataElement( TableDataCollection.class );
    }

    private ArticleGenePointCollection calculateArticlePoints(TableDataCollection articleCSV, TableDataCollection geneInfo)
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
        final String[] exonStartsStrArray = TextUtil2.split( exonStartsStr, ',' );
        for( int i = 0; i < exonCount; i++ )
        {
            exonList.add( Integer.parseInt( exonStartsStrArray[i] ) );
        }

        return exonList;
    }

    private SqlTrack addArticlePoint(SqlTrack track, ArticleGenePointCollection pointCollection) throws Exception
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

        track.finalizeAddition();

        final DataElementPath trackPath = track.getCompletePath();
        trackPath.save( track );

        return track;
    }
}