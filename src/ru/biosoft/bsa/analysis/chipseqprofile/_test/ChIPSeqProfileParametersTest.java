package ru.biosoft.bsa.analysis.chipseqprofile._test;

import java.util.Properties;
import junit.framework.TestCase;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.analysis.chipseqprofile.ChIPSeqProfileParameters;

public class ChIPSeqProfileParametersTest extends TestCase
{
    public void testWrite()
    {
        ChIPSeqProfileParameters parameters = new ChIPSeqProfileParameters();
        parameters.setPeakTrackPath(DataElementPath.create("tracks/peaks"));
        parameters.setTagTrackPath(DataElementPath.create("tracks/tags"));
        parameters.setProfileTrackPath(DataElementPath.create("tracks/peaks profile"));
        parameters.setFragmentSize(150);
        parameters.setSigma(75);
        parameters.setErrorRate(0.15);

        Properties properties = new Properties();
        String prefix = "some/path/";
        parameters.write(properties, prefix);

        assertEquals("tracks/peaks", properties.get(prefix + "peakTrackPath"));
        assertEquals("tracks/tags", properties.get(prefix + "tagTrackPath"));
        assertEquals("tracks/peaks profile", properties.get(prefix + "profileTrackPath"));
        assertEquals("150", properties.get(prefix + "fragmentSize"));
        assertEquals(String.valueOf(75.0), properties.get(prefix + "sigma"));
        assertEquals(String.valueOf(0.15), properties.get(prefix + "errorRate"));
    }

    public void testRead()
    {
        String prefix = "some/path/";
        Properties properties = new Properties();
        properties.put(prefix + "peakTrackPath", "tracks/peaks");
        properties.put(prefix + "tagTrackPath", "tracks/tags");
        properties.put(prefix + "profileTrackPath", "tracks/peaks profile");
        properties.put(prefix + "fragmentSize", "150");
        properties.put(prefix + "sigma", "75");
        properties.put(prefix + "errorRate", String.valueOf(0.15));

        ChIPSeqProfileParameters parameters = new ChIPSeqProfileParameters();
        parameters.read(properties, prefix);

        assertEquals("tracks/peaks", parameters.getPeakTrackPath().toString());
        assertEquals("tracks/tags", parameters.getTagTrackPath().toString());
        assertEquals("tracks/peaks profile", parameters.getProfileTrackPath().toString());
        assertEquals("150", String.valueOf(parameters.getFragmentSize()));
        assertEquals(String.valueOf(75.0), String.valueOf(parameters.getSigma()));
        assertEquals(String.valueOf(0.15), String.valueOf(parameters.getErrorRate()));
    }
}
