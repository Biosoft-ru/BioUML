package biouml.plugins.riboseq.comparison_article.util_data_structure;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SqlTrack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleGenePointCollection
{
    private Map<String, List<ArticleGenePointInfo>> genePointMap = new HashMap<>();

    public void put(String geneName, ArticleGenePointInfo genePoint)
    {
        final List<ArticleGenePointInfo> specificGenePointList;
        if( genePointMap.containsKey( geneName ) )
        {
            specificGenePointList = genePointMap.get( geneName );
        }
        else
        {
            specificGenePointList = new ArrayList<>();
        }
        specificGenePointList.add( genePoint );
        genePointMap.put( geneName, specificGenePointList );
    }

    public void addInformation(String geneName, ArticleGenePointAdditionInfo additionInfo)
    {
        if( genePointMap.containsKey( geneName ) )
        {
            final List<ArticleGenePointInfo> genePointInfoList = genePointMap.get( geneName );
            for( ArticleGenePointInfo genePointInfo : genePointInfoList )
            {
                genePointInfo.addInformation( additionInfo );
            }
        }
    }

    public void restructureOnChr()
    {
        final Map<String, List<ArticleGenePointInfo>> chrGenePointMap = new HashMap<>();

        for( List<ArticleGenePointInfo> articleGenePointInfoList : genePointMap.values() )
        {
            for( ArticleGenePointInfo pointInfo : articleGenePointInfoList )
            {
                if( pointInfo.hasAdditionInfo() )
                {
                    final String chrName = pointInfo.additionInfo.chrName;

                    if( !chrGenePointMap.containsKey( chrName ) )
                    {
                        chrGenePointMap.put( chrName, new ArrayList<ArticleGenePointInfo>() );
                    }
                    final List<ArticleGenePointInfo> pointInfoList = chrGenePointMap.get( chrName );
                    pointInfoList.add( pointInfo );
                    chrGenePointMap.put( chrName, pointInfoList );
                }
            }
        }

        genePointMap = chrGenePointMap;
    }

    public void computePoints()
    {
        for( List<ArticleGenePointInfo> pointInfoList : genePointMap.values() )
        {
            for( ArticleGenePointInfo genePointInfo : pointInfoList )
            {
                if( genePointInfo.hasAdditionInfo() )
                {
                    genePointInfo.computePoint();
                }
            }
        }
    }

    public List<GeneralPointInfo> getPointList(String chrName, boolean strandPlus)
    {
        final List<GeneralPointInfo> pointList = new ArrayList<>();

        final List<ArticleGenePointInfo> chrPointList = getChrPointList( chrName );
        for( ArticleGenePointInfo pointInfo : chrPointList )
        {
            final boolean pointStrand = pointInfo.additionInfo.strandPlus;
            if( pointStrand == strandPlus )
            {
                final GeneralPointInfo generalPointInfo = new GeneralPointInfo( pointInfo.point, chrName, pointStrand );

                pointList.add( generalPointInfo );
            }
        }

        return pointList;
    }

    public List<String> getChrList()
    {
        return new ArrayList<>( genePointMap.keySet() );
    }

    public void checkContext(SqlTrack yesTrack)
    {
        final GenomeSelector genomeSelector = yesTrack.getGenomeSelector();
        final DataElementPathSet chrPathSet = genomeSelector.getSequenceCollectionPath().getChildren();
        for( DataElementPath chrPath : chrPathSet )
        {
            final Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
            final String chrName = "chr" + chrSeq.getName();

            final List<ArticleGenePointInfo> chrPointList = getChrPointList( chrName );
            if( chrPointList != null )
            {
                checkContext( chrPointList, chrSeq );
            }
        }
    }

    public List<ArticleGenePointInfo> getChrPointList(String chrName)
    {
        return genePointMap.get( chrName );
    }

    private void checkContext(List<ArticleGenePointInfo> chrPointList, Sequence chrSeq)
    {
        for( ArticleGenePointInfo pointInfo : chrPointList )
        {
            final String context = pointInfo.initContext;
            final String chrContext = pointInfo.getChrContext( chrSeq );

            if( !chrContext.equals( context ) )
            {
                int matchCounter = 0;
                final byte[] contextBytes = context.getBytes( StandardCharsets.ISO_8859_1 );
                final byte[] chrContextBytes = chrContext.getBytes( StandardCharsets.ISO_8859_1 );
                for( int i = 0; i < context.length(); i++ )
                {
                    if( contextBytes[i] == chrContextBytes[i] )
                    {
                        matchCounter++;
                    }
                }

                if( matchCounter < 3 )
                {
                    throw new RuntimeException( "mismatch context" );
                }
            }
        }
    }
}
