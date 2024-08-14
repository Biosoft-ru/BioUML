package ru.biosoft.bsa.importer;

import java.util.Properties;

import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class BowtieTrackImporter extends TrackImporter
{
    @Override
    protected Site parseLine(String line)
    {
        String[] fields = line.split("\t", -1);
        if( fields.length != 8 )
            return null;
        String readName = fields[0];
        int strand = fields[1].equals("+") ? StrandType.STRAND_PLUS : fields[1].equals("-") ? StrandType.STRAND_MINUS
                : StrandType.STRAND_NOT_KNOWN;
        String referenceSequenceName = normalizeChromosome(fields[2]);
        int zeroBasedOffset;
        try
        {
            zeroBasedOffset = Integer.parseInt(fields[3]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }

        String readSequence = fields[4];

        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        if( !importerProperties.isIgnoreSiteProperties() )
        {
            int numberOfSameAlignments;
            try
            {
                numberOfSameAlignments = Integer.parseInt(fields[6]);
            }
            catch( NumberFormatException e )
            {
                return null;
            }
            String readQualities = fields[5];
            String mismatchDescriptors = fields[7];

            properties.add(new DynamicProperty(getDescriptor("readName"), String.class, readName));
            properties.add(new DynamicProperty(getDescriptor("readSequence"), String.class, readSequence));
            properties.add(new DynamicProperty(getDescriptor("readQualities"), String.class, readQualities));
            properties.add(new DynamicProperty(getDescriptor("numberOfSameAlignments"), Integer.class, numberOfSameAlignments));
            properties.add(new DynamicProperty(getDescriptor("mismatchDescriptors"), String.class, mismatchDescriptors));
        }

        return new SiteImpl(null, referenceSequenceName, null, Basis.BASIS_USER, ( strand == StrandType.STRAND_MINUS )
                ? ( zeroBasedOffset + readSequence.length() ) : ( zeroBasedOffset + 1 ), readSequence.length(),
                Precision.PRECISION_EXACTLY, strand, null, properties);
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "bowtie";
        return true;
    }

}
