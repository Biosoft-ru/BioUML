package biouml.plugins.riboseq.ingolia;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.riboseq.transcripts.Transcript;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;

/**
 * Convert alignments from genomic coordinates to transcript offsets
 */
public class AlignmentConverter
{
    private int transcriptOverhangs = 100;
    
    public int getTranscriptOverhangs()
    {
        return transcriptOverhangs;
    }

    public void setTranscriptOverhangs(int transcriptOverhangs)
    {
        this.transcriptOverhangs = transcriptOverhangs;
    }

    public List<AlignmentOnTranscript> getTranscriptAlignments(Transcript transcript, BAMTrack bamTrack)
    {
        List<AlignmentOnTranscript> result = new ArrayList<>();
        DataCollection<Site> sites = bamTrack.getSites( transcript.getChromosome(), transcript.getLocation().getFrom() + 1
                - transcriptOverhangs, transcript.getLocation().getTo() + 1 + transcriptOverhangs );
        for(Site site : sites)
        {
            AlignmentOnTranscript alignment = bamSiteToAlignment( (BAMSite)site, transcript, transcriptOverhangs );
            if( alignment != null )
                result.add( alignment );
        }
        return result;
    }
    
    public static AlignmentOnTranscript bamSiteToAlignment(BAMSite bamSite, Transcript transcript, int transcriptOverhangs)
    {
       return bamSiteToAlignment( bamSite.getFrom() - 1, bamSite.getStrand() == StrandType.STRAND_PLUS, bamSite.getCigar(), transcript, transcriptOverhangs );
    }
    
    public static AlignmentOnTranscript bamSiteToAlignment(int genomicFrom, boolean isOnPositiveStrand, Cigar cigar, Transcript transcript, int transcriptOverhangs)
    {
        int exonCount = transcript.getExonLocations().size();
        Interval[] exons = transcript.getExonLocations().toArray( new Interval[exonCount] );
        exons[0] = new Interval( exons[0].getFrom() - transcriptOverhangs, exons[0].getTo() );
        exons[exonCount - 1] = new Interval( exons[exonCount - 1].getFrom(), exons[exonCount -1].getTo() + transcriptOverhangs );
        
        int startOffset = 0;
        int curExon = 0;
        while(curExon < exonCount && genomicFrom > exons[curExon].getTo())
        {
            startOffset += exons[curExon].getLength();
            curExon++;
        }
        if(curExon == exonCount || genomicFrom < exons[curExon].getFrom())
            return null;
        startOffset += genomicFrom - exons[curExon].getFrom();
        startOffset -= transcriptOverhangs;
        
        int length = 0;
        genomicFrom--;
        for( CigarElement ce : cigar.getCigarElements() )
        {
            CigarOperator op = ce.getOperator();
            switch(op)
            {
                case M:
                case X:
                case EQ:
                case D:
                    genomicFrom += ce.getLength();
                    length += ce.getLength();
                    if( genomicFrom > exons[curExon].getTo() )
                        return null;
                    break;
                case N:
                    if( genomicFrom != exons[curExon].getTo() )
                        return null;
                    genomicFrom += ce.getLength();
                    curExon++;
                    if( curExon >= exonCount || genomicFrom != exons[curExon].getFrom() - 1 )
                        return null;
                    break;
                default:
                    break;
            }
        }
        if( transcript.isOnPositiveStrand() )
            return new AlignmentOnTranscript( startOffset, startOffset + length - 1, isOnPositiveStrand );
        startOffset = transcript.getLength() - startOffset - length;
        return new AlignmentOnTranscript( startOffset, startOffset + length - 1, !isOnPositiveStrand );
    }

}
