package biouml.plugins.riboseq._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.Interval;

import java.util.List;

public class SiteUtilTest extends AbstractBioUMLTest
{
    public void testIntersectRemain() throws Exception {
        final Interval interval1 = new Interval(4, 10);
        final Interval interval2 = new Interval(6, 8);

        final List<Interval> remainIntervalList = interval1.remainOfIntersect( interval2 );

        final Interval remainInterval1 = remainIntervalList.get(0);
        final Interval expectedInterval1 = new Interval(4, 5);
        assertEquals( expectedInterval1, remainInterval1 );

        final Interval remainInterval2 = remainIntervalList.get(1);
        final Interval expectedInterval2 = new Interval(9, 10);
        assertEquals( expectedInterval2, remainInterval2 );
    }
}
