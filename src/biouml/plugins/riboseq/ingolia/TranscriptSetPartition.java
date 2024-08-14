package biouml.plugins.riboseq.ingolia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import biouml.plugins.riboseq.transcripts.Transcript;

public class TranscriptSetPartition
{
    private final List<Transcript> learningSet;
    private final List<Transcript> testingSet;
    
    public TranscriptSetPartition(List<Transcript> transcripts, double learningFraction, long seed)
    {
        if(learningFraction < 0 || learningFraction > 1)
            throw new IllegalArgumentException();
        Random rnd = new Random( seed );
        int learningSetSize = (int)Math.round( transcripts.size() * learningFraction );
        transcripts = new ArrayList<>( transcripts );
        Collections.sort( transcripts, Comparator.comparing( Transcript::getName ) );
        Collections.shuffle( transcripts, rnd );
        learningSet = transcripts.subList( 0, learningSetSize );
        testingSet = transcripts.subList( learningSetSize, transcripts.size() );
    }
    
    public List<Transcript> getLearningSet()
    {
        return learningSet;
    }
    
    public List<Transcript> getTestingSet()
    {
        return testingSet;
    }
}
