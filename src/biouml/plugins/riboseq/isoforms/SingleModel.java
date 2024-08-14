package biouml.plugins.riboseq.isoforms;

import biouml.plugins.riboseq.isoforms.Read.Type;

public class SingleModel
{
    private LenDist lenDist;
    private Profile profile;
    private NoiseProfile noiseProfile;
    
    private TranscriptSequence[] transcripts;
    private double forwardProb;
    
    private boolean needCalConProb = true;
    
    public SingleModel(ModelParameters modelParameters, boolean isMaster)
    {
        this.forwardProb = modelParameters.probF;
        this.transcripts = modelParameters.transcripts;
        if(isMaster)
        {
            lenDist = new LenDist(modelParameters.minL, modelParameters.maxL);
        }
        profile = new Profile( modelParameters.maxL );
        noiseProfile = new NoiseProfile();
    }
    
    public SingleModel(ModelParameters modelParameters)
    {
        this(modelParameters, true);
    }

    public boolean isNeedCalConProb()
    {
        return needCalConProb;
    }
    public void setNeedCalConProb(boolean needCalConProb)
    {
        this.needCalConProb = needCalConProb;
    }
    
    public void estimateFromReads(ReadContainer readContainer)
    {
        for( Type t : Type.values() )
        {
            ReadReader<Read> reader = readContainer.getReader( t );
            reader.reset();
            Read r;
            while((r = reader.read()) != null)
            {
                lenDist.update( r.seq.length );
                if(t == Type.UNMAPPED)
                    noiseProfile.updateC(r.seq);
            }
        }
        lenDist.finish();
        noiseProfile.calcInitParams();
    }

    
    public double getConProb(Read read, Hit hit)
    {
        TranscriptSequence t = transcripts[hit.getTranscriptId()];
        int effectiveLen = t.seq.length - read.seq.length + 1;
        double prob = 1.0 / effectiveLen;
        prob *= lenDist.getAdjustedProb( read.seq.length, t.seq.length );
        prob *= hit.isForwardStrand() ? forwardProb : 1-forwardProb;
        prob *= profile.getProb( read.seq, t.seq, hit.getPos(), hit.isForwardStrand() );
        if(prob < Const.EPSILON)
            prob = 0;
        return prob;
    }
    
    public double getNoiseConProb(Read read)
    {
        double prob = lenDist.getProb( read.seq.length );
        prob *= noiseProfile.getProb( read.seq );
        if(prob < Const.EPSILON)
            prob = 0;
        return prob;
    }
    
    public LenDist getLenDist() { return lenDist; }
    
    public void init()
    {
        profile.init();
        noiseProfile.init();
    }
    
    public void finish()
    {
        profile.finish();
        noiseProfile.finish();
        needCalConProb = true;
    }
    
    public void collect(SingleModel model)
    {
        profile.collect(model.profile);
        noiseProfile.collect(model.noiseProfile);
    }

    public void update(Read read, Hit hit, double frac)
    {
        if(frac < Const.EPSILON)
            return;
        TranscriptSequence t = transcripts[hit.getTranscriptId()];
        profile.update( read.seq, t.seq, hit.getPos(), hit.isForwardStrand(), frac );
    }

    public void updateNoise(Read read, double frac)
    {
        if(frac < Const.EPSILON)
            return;
        noiseProfile.update(read.seq, frac);
    }
    
}
