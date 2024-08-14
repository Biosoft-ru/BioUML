package biouml.plugins.gtrd.master.collections;

import java.io.IOException;
import java.util.Properties;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToMACS2HistonesCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class HistonesClustersCollection extends ClustersCollection<HistonesCluster>
{
    public HistonesClustersCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }

    @Override
    protected BedEntryConverter<HistonesCluster> getConverter(String expType, String peakCaller, CellLine cell,
            BigBedTrack<HistonesCluster> track)
    {
        int idx = expType.indexOf( '_' );//expType=H4K20me1_ChIP-seq_HM
        String target = expType.substring( 0, idx );
        BedEntryConverter<? extends HistonesCluster> result = new BedEntryToMACS2HistonesCluster( track, cell, target ); 
        return (BedEntryConverter<HistonesCluster>)result;
    }

}
