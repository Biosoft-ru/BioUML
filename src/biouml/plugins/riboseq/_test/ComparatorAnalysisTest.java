package biouml.plugins.riboseq._test;

import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import biouml.plugins.riboseq.comparison_article.ComparatorAnalysis;
import biouml.plugins.riboseq.comparison_article.ComparatorParameters;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointCollection;
import biouml.plugins.riboseq.comparison_article.util_data_structure.ArticleGenePointInfo;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.TableDataCollection;

public class ComparatorAnalysisTest extends AbstractBioUMLTest
{
    private static final DataElementPath ARTICLE_DATA_PATH = DataElementPath.create( "data/Collaboration/testSql123/Data/Files" );
    private static final DataElementPath ARTICLE_CSV = ARTICLE_DATA_PATH.getChildPath( "mmc3" );
    private static final DataElementPath ARTICLE_GENE_INFO = ARTICLE_DATA_PATH.getChildPath( "ucsc_old_genes" );

    private static final DataElementPath TRACK = DataElementPath.create( "data/Collaboration/testSql123/Data/Tracks/clUnfiltered" );

    private static final String OUTPUT_STR = "output";
    private static final DataElementPath OUTPUT = DataElementPath.create( OUTPUT_STR );
    private static final DataElementPath OUTPUT_STATISTIC = OUTPUT.getChildPath( "stat" );
    private static final DataElementPath OUTPUT_TABLE = OUTPUT.getChildPath( "table" );

    private static final String MAIN_REPOSITORY_PATH = "../data_resources";
    private static final String REPOSITORY_SEQ_PATH = "../data/test/biouml/plugins/riboseq/data/";

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( MAIN_REPOSITORY_PATH );
        CollectionFactory.createRepository( REPOSITORY_SEQ_PATH );

        VectorDataCollection<DataElement> outputVdc = new VectorDataCollection<>( OUTPUT_STR, DataElement.class, null );
        CollectionFactory.registerRoot( outputVdc );
    }

    /*public void testJustRun() throws Exception
    {
        final ComparatorParameters parameters = getComparatorParameters();

        final ComparatorAnalysis analysis = new ComparatorAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        analysis.justAnalyzeAndPut();
    }*/

    public void testCreateArticleTrack() throws Exception
    {
        final DataElementPath TRACK = ru.biosoft.access.core.DataElementPath
                .create( "data/Collaboration/testSql123/Data/Tracks/testAddArticlePoint/clUnfilteredTestHeader" );

        final ComparatorParameters parameters = new ComparatorParameters();
        parameters.setInputArticleCSV( ARTICLE_CSV );
        parameters.setInputGeneInfo( ARTICLE_GENE_INFO );
        parameters.setInputYesTrack( TRACK );

        final ComparatorAnalysis analysis = new ComparatorAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        final TableDataCollection articleCSV = analysis.getArticleCSV();
        final TableDataCollection geneInfo = analysis.getGeneInfo();
        final ArticleGenePointCollection articleGenePointCollection = analysis.calculateArticlePoints( articleCSV, geneInfo );

        final SqlTrack track = analysis.getYesTrack();

        addArticlePoint( track, articleGenePointCollection );
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

        track.finalizeAddition();
        track.getCompletePath().save( track );
    }

    private ComparatorParameters getComparatorParameters()
    {
        final ComparatorParameters parameters = new ComparatorParameters();
        parameters.setInputArticleCSV( ARTICLE_CSV );
        parameters.setInputGeneInfo( ARTICLE_GENE_INFO );
        parameters.setInputYesTrack( TRACK );
        parameters.setOutputStatistic( OUTPUT_STATISTIC );
        parameters.setOutputUniqueArticlePointTable( OUTPUT_TABLE );

        return parameters;
    }
}
