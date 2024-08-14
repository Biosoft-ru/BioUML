package biouml.plugins.riboseq.transcripts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;

public class TranscriptLoader
{
    private TranscriptsProvider provider;
    private List<String> subset;
    
    public TranscriptLoader(TranscriptsProvider provider, List<String> subset)
    {
        this.provider = provider;
        this.subset = subset;
    }
    
    public List<Transcript> loadTranscripts(Logger log)
    {
        log.info( "Loading transcript annotation" );
        if(subset != null)
            provider.setSubset( subset );
        List<Transcript> transcripts = provider.getTranscripts();
        log.info( transcripts.size() + " transcripts loaded" );

        if( subset != null )
        {
            log.info( "Filtering transcripts" );
            Set<String> index = new HashSet<>( subset );
            List<Transcript> transcriptSubset = new ArrayList<>();
            for( Transcript transcript : transcripts )
                if( index.contains( transcript.getName() ) )
                    transcriptSubset.add( transcript );
            log.info( transcriptSubset.size() + " transcripts remain" );
            if( transcriptSubset.size() < subset.size() )
                log.warning( ( subset.size() - transcriptSubset.size() ) + " transcripts were not found" );
            transcripts = transcriptSubset;
        }
        else
        {
            log.info( "Using all transcripts" );
        }
        return transcripts;
    }
}
