package biouml.plugins.riboseq.comparison_article.util_data_structure;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;

public class ArticleGenePointInfo
{
    public int point;

    public final String geneName;

    public final int initCodonPosition;
    public final String initContext;

    public ArticleGenePointAdditionInfo additionInfo;

    public ArticleGenePointInfo(String geneName, int initCodonPosition, String initContext)
    {
        this.geneName = geneName;
        this.initCodonPosition = initCodonPosition;
        this.initContext = initContext.toUpperCase();
    }

    public void addInformation(ArticleGenePointAdditionInfo additionInfo)
    {
        this.additionInfo = additionInfo;
    }

    public void computePoint()
    {
        int remain = initCodonPosition;
        if( additionInfo.strandPlus )
        {
            int ind = 0;
            int start = additionInfo.exonStarts.get( ind );
            int end = additionInfo.exonEnds.get( ind );
            while( start + remain >= end )
            {
                remain -= end - start;

                ind++;
                start = additionInfo.exonStarts.get( ind );
                end = additionInfo.exonEnds.get( ind );
            }

            point = start + remain;
        }
        else
        {
            int ind = additionInfo.exonCount - 1;
            int start = additionInfo.exonStarts.get( ind );
            int end = additionInfo.exonEnds.get( ind ) - 1;
            while( end - remain < start )
            {
                remain -= ( end + 1 ) - start;

                ind--;
                start = additionInfo.exonStarts.get( ind );
                end = additionInfo.exonEnds.get( ind ) - 1;
            }

            point = end - remain;
        }
    }

    public boolean hasAdditionInfo()
    {
        return additionInfo != null;
    }

    public String getChrContext(Sequence chrSeq)
    {
        final boolean strandPlus = additionInfo.strandPlus;
        final int length = initContext.length();
        final StringBuilder seqBuilder = new StringBuilder( length );

        int startContext = point + chrSeq.getStart();
        int ind = 0;

        SequenceRegion chrSeqReg = new SequenceRegion( chrSeq, 0, chrSeq.getLength(), false, false );
        if( !strandPlus )
        {
            chrSeqReg = SequenceRegion.getReversedSequence( chrSeqReg );
        }

        int endExon = additionInfo.exonEnds.get( ind );
        while( startContext > endExon )
        {
            ind++;
            endExon = additionInfo.exonEnds.get( ind );
        }

        if( strandPlus )
        {
            startContext -= 2;
        }
        else
        {
            startContext -= 3;
        }
        // for Moves to the previous exon
        if( ind != 0 )
        {
            if( startContext < additionInfo.exonStarts.get( ind ) )
            {
                startContext = additionInfo.exonEnds.get( ind - 1 ) - ( additionInfo.exonStarts.get( ind ) - startContext );
                ind--;
                endExon = additionInfo.exonEnds.get( ind );
            }
        }

        int positionLetter = startContext;
        for( int i = 0; i < length; i++ )
        {
            if( positionLetter >= endExon )
            {
                ind++;
                endExon = additionInfo.exonEnds.get( ind );
                positionLetter = additionInfo.exonStarts.get( ind );
            }
            final char letter;
            if( strandPlus )
            {
                letter = (char)chrSeqReg.getLetterAt( positionLetter );
            }
            else
            {
                letter = (char)chrSeqReg.getLetterAt( chrSeq.getLength() - positionLetter );
            }
            seqBuilder.append( letter );

            positionLetter++;
        }
        if( !strandPlus )
        {
            seqBuilder.reverse();
        }

        final String chrContext = seqBuilder.toString().toUpperCase();

        return chrContext;
    }
}
