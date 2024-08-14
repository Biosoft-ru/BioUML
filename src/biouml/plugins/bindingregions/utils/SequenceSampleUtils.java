package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

/**
 * @author yura
 *
 */
public class SequenceSampleUtils
{
    public static final String SEQUENCE_TYPE_UNALTERED = "Unaltered sequences";
    public static final String SEQUENCE_TYPE_UNALTERED_CENTERS_AND_GIVEN_LENGTH = "Sequences have the specified length and original centers";
    public static final String SEQUENCE_TYPE_SHORT_PROLONGATED = "Short sequences are prolongated relative to original centers";
    public static final String SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH = "Sequences have the specified length and their centers are summits";

    public static String[] getAvailableSequenceSampleTypes()
    {
        return new String[]{SEQUENCE_TYPE_UNALTERED, SEQUENCE_TYPE_UNALTERED_CENTERS_AND_GIVEN_LENGTH, SEQUENCE_TYPE_SHORT_PROLONGATED, SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH};
    }
    
    public static Object[] removeMissingDataInSequenceSample(String[] sequenceNames, String[] sequenceSample)
    {
        List<String> newSequenceSample = new ArrayList<>(), newSequenceNames = new ArrayList<>();
        for( int i = 0; i < sequenceSample.length; i++ )
            if( sequenceSample[i] != null && ! sequenceSample[i].equals("") && sequenceNames[i] != null && ! sequenceNames[i].equals("") )
            {
                newSequenceSample.add(sequenceSample[i]);
                newSequenceNames.add(sequenceNames[i]);
            }
        if( newSequenceSample.isEmpty() ) return null;
        return new Object[]{newSequenceNames.toArray(new String[0]), newSequenceSample.toArray(new String[0])};
    }
    
    public static Sequence[] transformSequenceSample(String[] sequenceNames, String[] sequenceSample, Alphabet alphabet)
    {
        Sequence[] result = new Sequence[sequenceSample.length];
        for( int i = 0; i < sequenceSample.length; i++ )
            // result[i] = new LinearSequence(sequenceNames[i], sequenceSample[i], alphabet);
            result[i] = (sequenceSample[i] == null || sequenceSample[i] == "") ? null : new LinearSequence(sequenceNames[i], sequenceSample[i], alphabet);
        return result;
    }

    // TODO: it is copied to SiteUtils.
    public static String[] getAvailablePropertiesNames(Track track)
    {
        DataCollection<Site> sites = track.getAllSites();
        if( sites.getSize() <= 0 ) return null;
        for( Site site: sites )
        {
            DynamicPropertySet properties = site.getProperties();
            if( properties.isEmpty() ) return null;
            return properties.asMap().keySet().toArray(new String[0]);
        }
        return null;
    }
    
    public static Interval changeIntervalAppropriately(Interval interval, String sequenceSampleType, int specifiedLengthOfSequence, int summit)
    {
        switch( sequenceSampleType )
        {
            case SEQUENCE_TYPE_UNALTERED                          : return interval;
            case SEQUENCE_TYPE_UNALTERED_CENTERS_AND_GIVEN_LENGTH : return interval.zoomToLength(specifiedLengthOfSequence);
            case SEQUENCE_TYPE_SHORT_PROLONGATED                  : if( interval.getLength() >= specifiedLengthOfSequence ) return interval;
                                                                    return interval.zoomToLength(specifiedLengthOfSequence);
            case SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH     : return new Interval(interval.getFrom() + summit).zoomToLength(specifiedLengthOfSequence);
            default                                               : return null;
        }
    }
}
