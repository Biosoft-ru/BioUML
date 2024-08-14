package biouml.plugins.gtrd.analysis.maos;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.bsa.analysis.maos.IntervalData;

public class IntervalDataAdvanced extends IntervalData
{

    public final Map<String, GTRDDataForTFClass> gtrdDataByTFClass;
    public IntervalDataAdvanced(IntervalData main, Map<String, GTRDDataForTFClass> gtrdDataByTFClass)
    {
        super(main);
        this.gtrdDataByTFClass = gtrdDataByTFClass;
    }
    
    @Override
    public IntervalData getReverseComplement()
    {
        IntervalData mainRC = super.getReverseComplement();
        
        Map<String, GTRDDataForTFClass> gtrdRC = new HashMap<>();
        gtrdDataByTFClass.forEach( ( key, tfData) -> {
            GTRDDataForTFClass tfDataRC = tfData.getRevrseComplement( reference.getInterval(), reference.getStart() );
            gtrdRC.put( key, tfDataRC );
        });
        
        return new IntervalDataAdvanced( mainRC, gtrdRC );
    }
}
