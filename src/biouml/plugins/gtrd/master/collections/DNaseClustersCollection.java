package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Properties;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToHotspot2DNaseCluster;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMACS2DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster.Design;
import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.MACS2DNaseCluster;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class DNaseClustersCollection extends ClustersCollection<DNaseCluster>
{
    public DNaseClustersCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        
    }
    
    public BedEntryConverter<DNaseCluster> getConverter(String expType, String peakCaller, CellLine cell, BigBedTrack<DNaseCluster> result)
            throws AssertionError
    {
        Design design = Design.getByLabel( expType );
        BedEntryConverter<? extends DNaseCluster> converter;
        switch( peakCaller )
        {
            case MACS2DNaseCluster.PEAK_CALLER:
                converter = new BedEntryToMACS2DNaseCluster( result, cell, design );
                break;
            case Hotspot2DNaseCluster.PEAK_CALLER:
                converter = new BedEntryToHotspot2DNaseCluster( result, cell, design );
                break;
            default:
                throw new AssertionError( "Unsupported peak caller: " + peakCaller );
        }
        return (BedEntryConverter<DNaseCluster>)converter;
    }
}
