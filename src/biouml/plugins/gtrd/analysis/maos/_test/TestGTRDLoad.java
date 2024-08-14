package biouml.plugins.gtrd.analysis.maos._test;

import java.util.List;
import java.util.Map;

import biouml.plugins.gtrd.analysis.maos.GTRDDataForTFClass;
import biouml.plugins.gtrd.analysis.maos.GTRDPeak;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;

public class TestGTRDLoad extends AbstractBioUMLTest
{
    private DataCollection<?> repository;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        repository = CollectionFactory.createRepository( "/opt/BioUML_Server/repo" );
        assertNotNull( "Can't create repository", repository );
    }
    
    public void test1()
    {
        DataCollection<?> gtrd = DataElementPath.create( "databases/GTRD" ).getDataCollection();
        assertNotNull( gtrd );
        
        String chrPath = "databases/EnsemblHuman85_38/Sequences/chromosomes GRCh38/1";
        
        int from = 9500;
        int[] len = {1000,10000,100000,1000_000,10_000_000,100_000_000, 1000_000_000};
        for(int l : len)
        {
            Interval interval = new Interval(from, from + l);
            long start = System.currentTimeMillis();
            Map<String, GTRDDataForTFClass> data = GTRDDataForTFClass.load( chrPath, interval, 5 );
            int size = 0;
            for(GTRDDataForTFClass dataTF : data.values())
            {
                for(List<GTRDPeak> peakList : dataTF.groups.values())
                    size += peakList.size();
            }
            long time = System.currentTimeMillis() - start;
            System.out.println(l + " " + size + " " + time);
        }
        
    }
}
