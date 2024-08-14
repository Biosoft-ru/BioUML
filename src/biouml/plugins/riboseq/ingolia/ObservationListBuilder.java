package biouml.plugins.riboseq.ingolia;

import java.util.List;

import biouml.plugins.riboseq.ingolia.Observation.Type;
import biouml.plugins.riboseq.transcripts.Transcript;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;

/**
 * Build observations using known CDS to create 'Yes' observations
 * and using near positions to build 'No' observations.   
 */
public class ObservationListBuilder
{
    private ObservationBuilder observationBuilder;
    private ProfileBuilder profileBuilder;
    private AlignmentConverter alignmentConverter;
    private int[] noSiteOffsets = {-6,-3,-2,-1,1,2,3,9,18,30,60,90,120,150};
    private int[][] sampleProfiles;

    public ObservationListBuilder(ObservationBuilder observationBuilder, ProfileBuilder profileBuilder, AlignmentConverter alignmentConverter)
    {
        this.observationBuilder = observationBuilder;
        this.profileBuilder = profileBuilder;
        this.alignmentConverter = alignmentConverter;
    }
    
    public int[] getNoSiteOffsets()
    {
        return noSiteOffsets;
    }
    public void setNoSiteOffsets(int[] noSiteOffsets)
    {
        this.noSiteOffsets = noSiteOffsets;
    }

    public ObservationList buildObservationListForTraining(List<Transcript> transcripts, BAMTrack[] samples) throws Exception
    {
        ObservationList result = createEmptyObservationList( samples );
        
        for( Transcript transcript : transcripts )
        {
            if( !transcript.isCoding() )
                continue;
            int[][] sampleProfiles = computeTranscriptProfile( samples, transcript );
            for( Interval cds : transcript.getCDSLocations() )
            {
                int yesPosition = cds.getFrom();
                Observation yesObservation = observationBuilder.buildObservation( Observation.Type.YES, yesPosition, sampleProfiles );
                if( yesObservation != null )
                {
                    yesObservation.setDescription( "+ " + transcript.getName() + " " + yesPosition + " " + 0 );
                    result.addObservation( yesObservation );
                }
                for( int offset : noSiteOffsets )
                {
                    int noPosition = yesPosition + offset;
                    Observation noObservation = observationBuilder.buildObservation( Observation.Type.NO, noPosition, sampleProfiles );
                    if( noObservation != null )
                    {
                        noObservation.setDescription( "- " + transcript.getName() + " " + noPosition + " " + offset );
                        result.addObservation( noObservation );
                    }
                }
            }
        }
        return result;
    }

    public ObservationList buildAllTranscriptObservations(Transcript transcript, BAMTrack[] samples) throws Exception
    {
        ObservationList result = createEmptyObservationList( samples );
        sampleProfiles = computeTranscriptProfile( samples, transcript );
        for(int position = 0; position < transcript.getLength(); position++)
        {
            Observation observation = observationBuilder.buildObservation( Type.UNKNOWN, position, sampleProfiles );
            if(observation != null)
            {
                observation.setDescription( String.valueOf(position) );
                result.addObservation( observation );
            }
        }
        return result;
    }

    public int[][] getSampleProfiles()
    {
        return sampleProfiles;
    }

    private int[][] computeTranscriptProfile(BAMTrack[] samples, Transcript transcript) throws Exception
    {
        int[][] sampleProfiles = new int[samples.length][];
        for( int sampleIdx = 0; sampleIdx < samples.length; sampleIdx++ )
        {
            List<AlignmentOnTranscript> aligns = alignmentConverter.getTranscriptAlignments( transcript, samples[sampleIdx] );
            sampleProfiles[sampleIdx] = profileBuilder.computeProfile( aligns, transcript.getLength() );
        }
        return sampleProfiles;
    }
    
    private ObservationList createEmptyObservationList(BAMTrack[] samples)
    {
        int windowWidth = observationBuilder.getWindowRight() - observationBuilder.getWindowLeft() + 1;
        String[] predictorNames = new String[samples.length * windowWidth];
        for( int i = 0; i < samples.length; i++ )
            for( int j = 0; j < windowWidth; j++ )
                predictorNames[i * windowWidth + j] = samples[i].getName() + "_" + (j + observationBuilder.getWindowLeft());
        return new ObservationList( predictorNames );
    }
}
