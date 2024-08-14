package biouml.plugins.riboseq.transcripts;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;

import com.developmentontheedge.beans.DynamicPropertySet;

public class BedTrackTranscriptsProvider extends TranscriptsProvider
{
    private Track bedTrack;
    public BedTrackTranscriptsProvider(Track bedTrack)
    {
        this.bedTrack = bedTrack;
    }

    @Override
    public List<Transcript> getTranscripts()
    {
        List<Transcript> result = new ArrayList<>();
        for(Site site : bedTrack.getAllSites())
        {
            Transcript transcript = transcriptFromSite(site);
            result.add( transcript );
        }
        return result;
    }

    public static Transcript transcriptFromSite(Site site)
    {
        DynamicPropertySet properties = site.getProperties();
        
        String transcriptName = properties.getValueAsString( "name" );
        String chromosome = site.getOriginalSequence().getName();
        Interval location = site.getInterval().shift( -1 ); //make it zero based
        boolean isOnPositiveStrand = site.getStrand() == StrandType.STRAND_PLUS;
        
        int exonCount = (Integer)properties.getValue( "blockCount" );

        String blockStartsStr = (String)properties.getValue( "blockStarts" );
        String blockSizesStr = (String)properties.getValue( "blockSizes" );
        
        String[] blockStartFields = blockStartsStr.split( "," );
        String[] blockSizesFields = blockSizesStr.split( "," );
        
        List<Interval> exonLocations = new ArrayList<>(exonCount);
        for(int i = 0; i < exonCount; i++)
        {
            int blockStart = Integer.parseInt( blockStartFields[i] );
            int blockSize = Integer.parseInt( blockSizesFields[i] );
            Interval exonLocation = new Interval( location.getFrom() + blockStart, location.getFrom() + blockStart + blockSize - 1 );
            exonLocations.add( exonLocation );
        }
        
        int thickStart = (Integer)properties.getValue( "thickStart" );
        int thickEnd = (Integer)properties.getValue( "thickEnd" );
        
        List<Interval> cdsLocations = new ArrayList<>();
        if( thickStart != thickEnd )
        {

            int cdsFrom = getTranscriptOffset( thickStart, exonLocations );
            int cdsTo = getTranscriptOffset( thickEnd - 1, exonLocations );
            if( isOnPositiveStrand )
                cdsLocations.add( new Interval( cdsFrom, cdsTo ) );
            else
            {
                int transcriptLength = 0;
                for( Interval exon : exonLocations )
                    transcriptLength += exon.getLength();
                cdsLocations.add( new Interval( transcriptLength - 1 - cdsTo, transcriptLength - 1 - cdsFrom ) );
            }
        }
        
        return new Transcript( transcriptName, chromosome, location, isOnPositiveStrand, exonLocations, cdsLocations );
    }
    
    private static int getTranscriptOffset(int genomicPos, List<Interval> exons)
    {
        int result = 0;
        int curExon = 0;
        while(curExon < exons.size() && genomicPos > exons.get( curExon ).getTo())
        {
            result += exons.get( curExon ).getLength();
            curExon++;
        }
        if(curExon == exons.size() || genomicPos < exons.get( curExon ).getFrom())
            throw new IllegalArgumentException();
        result += genomicPos - exons.get( curExon ).getFrom();
        return result;
    }

}
