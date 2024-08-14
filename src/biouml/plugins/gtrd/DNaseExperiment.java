package biouml.plugins.gtrd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;

public class DNaseExperiment extends Experiment
{
    public static final String PEAK_CALLER_MACS2 = "macs2";
    public static final String PEAK_CALLER_HOTSPOT2 = "hotspot2";
    public static final String PEAK_CALLER_WELLINGTON_MACS2 = "wellington_macs2";
    public static final String PEAK_CALLER_WELLINGTON_HOTSPOT2 = "wellington_hotspot2";
    
    public static final String[] OPEN_CHROMATIN_PEAK_CALLERS = new String[] {PEAK_CALLER_MACS2, PEAK_CALLER_HOTSPOT2};
    public static final String[] FOOTPRINT_PEAK_CALLERS = new String[] {PEAK_CALLER_WELLINGTON_MACS2, PEAK_CALLER_WELLINGTON_HOTSPOT2};
    
    public static final String[] PEAK_CALLERS = new String[] {PEAK_CALLER_MACS2, PEAK_CALLER_HOTSPOT2, PEAK_CALLER_WELLINGTON_MACS2, PEAK_CALLER_WELLINGTON_HOTSPOT2};
    
    public static final String DESIGN = "DNase-seq";


    public DNaseExperiment(DataCollection<?> parent, String id)
    {
        super( parent, id );
    }
    
    public Set<String> getPeakRepIds()
    {
        Set<String> result = new HashSet<>();
        for(String repId : getRepIds())
            result.add( getPeakId() + "_" + repId );
        return result;
    }
    
    public Set<String> getRepIds()
    {
        Set<String> result = new HashSet<>();
        for(String readId : getReadsIds())
        {
            String rep = getElementProperties( readId ).get( "bio_rep_number" );
            if(rep != null)
                result.add( rep );
        }
        return result;
    }
    
    
    public String[] getPeakCallers()
    {
        return PEAK_CALLERS;
    }
    
    public DataElementPathSet getMacsPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_MACS2 );
    }
    
    public DataElementPathSet getHotspotPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_HOTSPOT2 );
    }
    
    public DataElementPathSet getMacsWelingtonPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_WELLINGTON_MACS2 );
    }
    
    public DataElementPathSet getHotspotWelingtonPeaks()
    {
        return getPeaksByPeakCaller( PEAK_CALLER_WELLINGTON_HOTSPOT2 );
    }
    
    public DataElementPathSet getWellingtonPeaks(String peakCaller)
    {
    	if(peakCaller.equals(PEAK_CALLER_MACS2)) return getPeaksByPeakCaller( PEAK_CALLER_WELLINGTON_MACS2 );
    	if(peakCaller.equals(PEAK_CALLER_HOTSPOT2)) return getPeaksByPeakCaller( PEAK_CALLER_WELLINGTON_HOTSPOT2 );
    	return null;
    }
    
    public DataElementPathSet getPeaksByPeakCaller(String peakCaller)
    {
        DataElementPathSet result = new DataElementPathSet();
        String parentDirName = peakCaller;
        if(peakCaller.contains("wellington"))
        	parentDirName = "footprints/" + peakCaller;
        for(String id : getPeakRepIds())
            result.add( DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_PEAKS + "/DNase-seq/" + parentDirName + "/" + id)  );
        return result;
        
    }
    
    @Override
    public String getDesign()
    {
        return DESIGN;
    }
    
    public static boolean isFootprints(String peakCaller)
    {
        return Arrays.asList( FOOTPRINT_PEAK_CALLERS ).contains( peakCaller );
    }
    
    public static String getOriginalPeakCaller(String peakCaller)
    {
        switch(peakCaller)
        {
            case PEAK_CALLER_WELLINGTON_MACS2: return PEAK_CALLER_MACS2;
            case PEAK_CALLER_WELLINGTON_HOTSPOT2: return PEAK_CALLER_HOTSPOT2;
            default:
                throw new IllegalArgumentException();
        }
    }
}
