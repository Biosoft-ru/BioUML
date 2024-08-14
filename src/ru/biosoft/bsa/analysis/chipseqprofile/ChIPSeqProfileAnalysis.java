package ru.biosoft.bsa.analysis.chipseqprofile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.macs14.MACSFWTrack;
import ru.biosoft.jobcontrol.Iteration;

@ClassIcon("resources/chipseqprofile.gif")
public class ChIPSeqProfileAnalysis extends AnalysisMethodSupport<ChIPSeqProfileParameters>
{
    public ChIPSeqProfileAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new ChIPSeqProfileParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        checkGreater("fragmentSize", 0);
        checkGreater("sigma", 0.0);
        checkRange("errorRate", 0.0, 1.0);
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        Track peaks = parameters.getPeakTrackPath().getDataElement(Track.class);

        parameters.getProfileTrackPath().remove();
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, parameters.getProfileTrackPath().getName());
        try
        {
            properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, TrackUtils.getTrackSequencesPath(peaks).toString());
        }
        catch( Exception ex )
        {
        }
        SqlTrack profileTrack = new SqlTrack(parameters.getProfileTrackPath().optParentCollection(), properties);

        Track tagTrack = parameters.getTagTrackPath().getDataElement(Track.class);

        PeakIteration peakIteration = new PeakIteration(tagTrack, profileTrack, parameters.getFragmentSize() / 2, parameters
                .getSigma(), parameters.getErrorRate());
        if( !jobControl.forCollection(DataCollectionUtils.asCollection(peaks.getAllSites(), Site.class), peakIteration) )
        {
            if( peakIteration.hasErrors() )
                throw peakIteration.getError();
            log.info("ChIP-seq profile analysis terminated by request");
            return null;
        }

        profileTrack.finalizeAddition();
        CollectionFactoryUtils.save(profileTrack);
        return profileTrack;
    }

    private static class PeakIteration implements Iteration<Site>
    {
        WritableTrack profileTrack;
        Map<String, MACSFWTrack> shiftedTags = new HashMap<>();
        Exception exception;

        double errorRate;

        DiscreteGaussian probFunc;

        public PeakIteration(Track tagTrack, WritableTrack profileTrack, int shift, double sigma, double errorRate)
        {
            this.profileTrack = profileTrack;
            this.errorRate = errorRate;
            probFunc = new DiscreteGaussian(sigma);
            for( Site tag: tagTrack.getAllSites() )
            {
                String chrom = tag.getOriginalSequence().getName();
                MACSFWTrack tags = shiftedTags.get(chrom);
                if( tags == null )
                    shiftedTags.put(chrom, tags = new MACSFWTrack());
                tags.addSite(tag);
            }
            for(MACSFWTrack tags : shiftedTags.values())
            {
                tags.finalizeAddition();
                tags.filterDup(1);
                tags.shift(shift);
                tags.mergePlusMinusLocationsNaive();
            }
        }
        
        

        @Override
        public boolean run(Site peak)
        {
            try
            {
                int[] tags = shiftedTags.get(peak.getOriginalSequence().getName()).getRangesByStrand(StrandType.STRAND_PLUS).data();
                
                int tagsStart = Arrays.binarySearch(tags, peak.getFrom());
                while(tagsStart > 0 && tags[tagsStart-1] == peak.getFrom())
                    tagsStart--;
                if(tagsStart < 0)
                    tagsStart = -tagsStart - 1;
                
                int tagsLength = 0;
                for(int i = tagsStart; i < tags.length && tags[i] <= peak.getTo(); i++)
                    tagsLength++;
                    
                double[] profile = computeProfile(peak.getFrom(), peak.getLength(), tags, tagsStart, tagsLength);

                DynamicPropertySet properties = (DynamicPropertySet)peak.getProperties().clone();
                properties.add(new DynamicProperty("profile", double[].class, profile));
                Site profiledPeak = new SiteImpl(null, peak.getName(), peak.getType(), peak.getBasis(), peak.getStart(), peak.getLength(),
                        peak.getPrecision(), peak.getStrand(), peak.getOriginalSequence(), peak.getComment(), properties);
                profileTrack.addSite(profiledPeak);
                return true;
            }
            catch( Exception e )
            {
                exception = e;
                return false;
            }
        }
        
        public boolean hasErrors()
        {
            return exception != null;
        }

        public Exception getError()
        {
            return exception;
        }

        private double[] computeProfile(int start, int length, int[] tags, int tagsStart, int tagsLength)
        {
            double[] result = new double[length];
            for( int x = start; x < start + length; x++ )
                result[x - start] = computeProfileAt(x, tags, tagsStart, tagsLength, length);
            potentiateAndNormalize(result);
            return result;
        }

        private void potentiateAndNormalize(double[] values)
        {
            double min = Double.MAX_VALUE;
            for( double v : values )
                if( v < min )
                    min = v;

            double sum = 0.0;
            for( int i = 0; i < values.length; i++ )
                sum += ( values[i] = Math.pow(10, - ( values[i] - min )) );

            for( int i = 0; i < values.length; i++ )
                values[i] /= sum;
        }

        private double computeProfileAt(int x, int[] tags, int tagsStart, int tagsLength, int domainSize)
        {
            double result = 0.0;
            for( int i = tagsStart; i < tagsStart + tagsLength; i++)
                result += -Math.log10(errorRate / domainSize + ( 1 - errorRate ) * probFunc.probability(x - tags[i]));
            return result;
        }

    }

    private static class DiscreteGaussian
    {
        double[] values;
        int lowerBound, upperBound;
        double sigma;

        public DiscreteGaussian(double sigma)
        {
            this.sigma = sigma;
            upperBound = (int)Math.ceil(sigma * 5);
            lowerBound = -upperBound;
            values = new double[upperBound - lowerBound + 1];
            double sum = 0.0;
            for( int x = lowerBound; x <= upperBound; x++ )
                sum += ( values[x - lowerBound] = Stat.standartNormalDistribution( ( x + 0.5 ) / sigma)
                        - Stat.standartNormalDistribution( ( x - 0.5 ) / sigma) );
            for( int i = 0; i < values.length; i++ )
                values[i] /= sum;
        }

        public double probability(int x)
        {
            if( ( x < lowerBound ) || ( x > upperBound ) )
                return 0;
            return values[x - lowerBound];
        }
    }
}
