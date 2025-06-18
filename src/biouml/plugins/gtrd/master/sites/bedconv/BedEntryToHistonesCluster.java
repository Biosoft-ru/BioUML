package biouml.plugins.gtrd.master.sites.bedconv;

import java.nio.charset.StandardCharsets;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;

public abstract class BedEntryToHistonesCluster<T extends HistonesCluster> implements BedEntryConverter<T>
{
    private BigBedTrack<?> origin;
    private CellLine cell;
    private String target;
    
    public BedEntryToHistonesCluster(BigBedTrack<?> origin, CellLine cell, String target)
    {
        this.origin = origin;
        this.cell = cell;
        this.target = target;
    }
    
    protected abstract T createCluster();
    protected abstract void parseRestString(T cluster, String[] parts);

    @Override
    public T fromBedEntry(BedEntry e)
    {
        T c = createCluster();
        c.setOrigin( origin );
        c.setCell( cell );
        c.setTarget( StringPool.get( target ) );
        
        ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        String chrName = origin.internalToExternal(chrInfo.name);
        c.setChr( chrName );
        c.setFrom( e.start+1 );
        c.setTo( e.end );
        
        String[] parts = TextUtil2.split( e.getRest(), '\t' );
        parseRestString(c, parts);
        
        return c;
    }

    protected abstract String getRestString(T cluster);

    @Override
    public BedEntry toBedEntry(T c)
    {
    	 ChromInfo chrInfo = origin.getChromInfo(c.getChr());
         BedEntry e =  new BedEntry( chrInfo.id, c.getFrom() - 1, c.getTo());
         e.data = getRestString( c ).getBytes(StandardCharsets.UTF_8);
        return e;
    }

}
