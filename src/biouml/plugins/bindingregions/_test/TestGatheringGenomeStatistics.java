package biouml.plugins.bindingregions._test;

import java.util.List;

import biouml.plugins.bindingregions.analysis.GatheringGenomeStatistics;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa._test.BSATestUtils;

/**
 * @author lan
 *
 */
public class TestGatheringGenomeStatistics extends AbstractBioUMLTest
{
    public void testGetChromosomeGaps() throws Exception
    {
        BSATestUtils.createRepository();
        AnnotatedSequence aseq = DataElementPath.create("databases/Ensembl/Sequences/chromosomes NCBI36/19").getDataElement(AnnotatedSequence.class);
        List<Gap> gaps = GatheringGenomeStatistics.getChromosomeGaps(aseq.getSequence());
        assertNotNull(gaps);
        int[] positions = {1, 7_297_005, 8_593_199, 24_423_623, 63_806_652};
        int[] lengths = {11000, 5000, 5000, 8000000, 5000};
        
        assertEquals(lengths.length, gaps.size());
        for(int i=0; i<lengths.length; i++)
        {
            assertEquals("Gap#"+i, new Interval(positions[i], positions[i]+lengths[i]-1), gaps.get(i).getInterval());
        }
    }
}
