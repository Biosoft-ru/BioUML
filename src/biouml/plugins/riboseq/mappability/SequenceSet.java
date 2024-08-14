package biouml.plugins.riboseq.mappability;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TByteArrayList;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;

public class SequenceSet
{
    byte[] data;
    List<String> names = new ArrayList<>();
    
    public void load(Track track, DataCollection<AnnotatedSequence> chromosomes)
    {
        TByteArrayList seq = new TByteArrayList();
        for(AnnotatedSequence as : chromosomes)
        {
            Sequence s = as.getSequence();
            int from = s.getStart();
            int to = s.getLength() + s.getStart();
            for(Site site : track.getSites( as.getCompletePath().toString(), from ,to ))
            {
                SequenceRegion siteSeq = new SequenceRegion( s, site.getInterval(), site.getStrand() == StrandType.STRAND_MINUS, false );
                for(int i = 0; i < siteSeq.getLength(); i++)
                {
                    byte letter = siteSeq.getLetterAt( i + siteSeq.getStart() );
                    letter = (byte)Character.toUpperCase( letter );
                    seq.add( letter );
                }
                seq.add( (byte)'\n' );
                names.add( site.getName() );
            }
        }
        
        data = seq.toArray();
    }
    
    public void load(DataCollection<AnnotatedSequence> chromosomes)
    {
        TByteArrayList seq = new TByteArrayList();
        for(AnnotatedSequence as : chromosomes)
        {
            Sequence s = as.getSequence();
            for(int i = 0; i < s.getLength(); i++)
            {
                byte letter = s.getLetterAt( i + s.getStart() );
                letter = (byte)Character.toUpperCase( letter );
                seq.add( letter );
            }
            seq.add( (byte)'\n' );
            names.add( as.getName() + "+" );
            
            int start = seq.size() - 2;
            for(int i = 0; i < s.getLength(); i++)
            {
                byte letter = seq.get( start - i );
                letter = s.getAlphabet().letterComplementMatrix()[letter];
                letter = (byte)Character.toUpperCase( letter );
                seq.add( letter );
            }
            seq.add( (byte)'\n' );
            names.add( as.getName() + "-" );
        }
        
        data = seq.toArray();        
    }
}
