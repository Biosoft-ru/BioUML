package biouml.plugins.riboseq.ingolia;

import java.beans.PropertyDescriptor;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptLoader;
import ru.biosoft.util.bean.StaticDescriptor;

public class ComputeTranscriptProfile extends AnalysisMethodSupport<ComputeTranscriptProfile.Parameters>
{
    private ProfileBuilder profileBuilder;
    private AlignmentConverter alignmentConverter;
    private SqlTrack resultingTrack;

    private static final PropertyDescriptor TRANSCRIPT_PD = StaticDescriptor.create( "transcript" );

    public ComputeTranscriptProfile(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 10 );
        TranscriptLoader loader = parameters.getTranscriptSet().createTranscriptLoader();
        List<Transcript> transcripts = loader.loadTranscripts( log );
        profileBuilder = parameters.createProfileBuilder();
        alignmentConverter = parameters.createAlignmentConverter();
        jobControl.popProgress();

        jobControl.pushProgress( 10, 90 );
        resultingTrack = SqlTrack.createTrack( parameters.getOutputTrack(), parameters.isProcessedTranscriptProfiles() ? null : parameters.getBAMTracks()[0] );
        jobControl.forCollection( transcripts, t -> {
            processTranscript( t );
            return true;
        } );
        jobControl.popProgress();

        jobControl.pushProgress( 90, 100 );
        resultingTrack.finalizeAddition();
        jobControl.popProgress();

        return resultingTrack;
    }

    private void processTranscript(Transcript t)
    {
        BAMTrack[] samples = parameters.getBAMTracks();

        int total = 0;
        double[] profile = new double[t.getLength()];
        for( int sampleIdx = 0; sampleIdx < samples.length; sampleIdx++ )
        {
            List<AlignmentOnTranscript> aligns = alignmentConverter.getTranscriptAlignments( t, samples[sampleIdx] );
            int[] sampleProfile = profileBuilder.computeProfile( aligns, t.getLength() );
            for( int i = 0; i < t.getLength(); i++ )
            {
                int v = sampleProfile[i];
                profile[i] += v;
                total += v;
            }
        }
        if( total < parameters.getMinTranscriptFootprints() )
            return;

        if( parameters.isProcessedTranscriptProfiles() )
        {
            DynamicPropertySet properties = new DynamicPropertySetAsMap();
            properties.add( new DynamicProperty( SqlTrack.PROFILE_PD, double[].class, profile ) );
            Site site = new SiteImpl( null, t.getName(), SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, 1,
                    t.getLength(), Precision.PRECISION_EXACTLY, StrandType.STRAND_BOTH, null, properties );
            resultingTrack.addSite( site );
        }
        else
        {
            int profilePos = 0;
            for( Interval exon : t.getExonLocations() )
            {
                exon = exon.shift( 1 );

                double[] exonProfile = new double[exon.getLength()];
                for( int i = 0; i < exon.getLength(); i++ )
                    exonProfile[i] = profile[t.isOnPositiveStrand() ? ( profilePos + i ) : ( t.getLength() - profilePos - i - 1 )];
                profilePos += exon.getLength();

                DynamicPropertySet properties = new DynamicPropertySetAsMap();
                properties.add( new DynamicProperty( SqlTrack.PROFILE_PD, double[].class, exonProfile ) );
                properties.add( new DynamicProperty( TRANSCRIPT_PD, String.class, t.getName() ) );
                int start = exon.getFrom();
                int strand = StrandType.STRAND_BOTH;
                Site site = new SiteImpl( null, t.getChromosome(), SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, start,
                        exon.getLength(), Precision.PRECISION_EXACTLY, strand, null, properties );
                resultingTrack.addSite( site );
            }
        }
    }

    public static class Parameters extends CoreParametersWithASiteTable
    {
        private int minTranscriptFootprints = 50;
        @PropertyName("Min transcript footprints")
        @PropertyDescription("Minimal number of footprints in transcript")
        public int getMinTranscriptFootprints()
        {
            return minTranscriptFootprints;
        }
        public void setMinTranscriptFootprints(int minTranscriptFootprints)
        {
            int oldValue = this.minTranscriptFootprints;
            this.minTranscriptFootprints = minTranscriptFootprints;
            firePropertyChange( "minTranscriptFootprints", oldValue, minTranscriptFootprints );
        }
        
        private boolean processedTranscriptProfiles = false;
        @PropertyName("Processed transcript")
        @PropertyDescription("Compute profile for processed transcript (without introns)")
        public boolean isProcessedTranscriptProfiles()
        {
            return processedTranscriptProfiles;
        }
        public void setProcessedTranscriptProfiles(boolean processedTranscriptProfiles)
        {
            boolean oldValue = this.processedTranscriptProfiles;
            this.processedTranscriptProfiles = processedTranscriptProfiles;
            firePropertyChange( "processedTranscriptProfiles", oldValue, processedTranscriptProfiles );
        }

        private DataElementPath outputTrack;
        @PropertyName ( "Output track" )
        @PropertyDescription ( "Resulting track with transcript profiles" )
        public DataElementPath getOutputTrack()
        {
            return outputTrack;
        }
        public void setOutputTrack(DataElementPath outputTrack)
        {
            DataElementPath oldValue = this.outputTrack;
            this.outputTrack = outputTrack;
            firePropertyChange( "outputTrack", oldValue, outputTrack );
        }
    }

    public static class ParametersBeanInfo extends CoreParametersWithASiteTableBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            property( "minTranscriptFootprints" ).add();
            property( "processedTranscriptProfiles" ).add();
            property( "outputTrack" ).outputElement( SqlTrack.class ).add();
        }
    }
}
