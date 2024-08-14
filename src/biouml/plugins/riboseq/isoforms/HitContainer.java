package biouml.plugins.riboseq.isoforms;

import java.util.BitSet;

import gnu.trove.list.array.TIntArrayList;

public class HitContainer
{
    private TIntArrayList position = new TIntArrayList();
    private TIntArrayList transcript = new TIntArrayList();
    private BitSet forwardStrand = new BitSet();
    private TIntArrayList readBuckets = new TIntArrayList();
    
    { readBuckets.add( 0 ); }

    public int getHitCount()
    {
        return position.size();
    }
    public void getHitAt(int hitId, Hit result)
    {
        result.pos = position.get( hitId );
        result.transcriptId = transcript.get( hitId );
        result.forwardStrand = forwardStrand.get( hitId );
    }
    public int getTranscriptAt(int hitId) { return transcript.get( hitId ); }
    

    public int getReadCount()
    {
        return readBuckets.size() - 1;
    }
    public int getReadBucketFrom(int readId)
    {
        return readBuckets.get( readId );
    }
    public int getReadBucketTo(int readId)
    {
        return readBuckets.get( readId + 1 );
    }

    public void addHit(Hit hit)
    {
        forwardStrand.set( getHitCount(), hit.isForwardStrand() );
        position.add( hit.getPos() );
        transcript.add( hit.getTranscriptId() );
    }

    public void finishReadBucket()
    {
        if( readBuckets.get( readBuckets.size() - 1 ) < getHitCount() )
            readBuckets.add( getHitCount() );
    }
}
