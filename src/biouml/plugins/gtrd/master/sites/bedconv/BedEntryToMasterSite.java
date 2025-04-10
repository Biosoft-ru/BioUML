package biouml.plugins.gtrd.master.sites.bedconv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.json.MasterSiteSerializer;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.track.big.BedEntryConverter;

public class BedEntryToMasterSite implements BedEntryConverter<MasterSite>
{
    private MasterSiteSerializer jsonSerializer;
    private MasterTrack origin;
    
    public BedEntryToMasterSite(MasterTrack origin, Properties properties)
    {
    	this.origin = origin;
        jsonSerializer = new MasterSiteSerializer( origin );
    }
    
    @Override
    public MasterSite fromBedEntry(BedEntry e)
    {
        MasterSite ms;
        try
        {
            ms = jsonSerializer.fromJSON( e.getRest() );
        }
        catch( IOException ex )
        {
            throw new RuntimeException(ex);
        }
        
        ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        String chrName = origin.internalToExternal(chrInfo.name);
        ms.setChr( StringPool.get( chrName ) );
        ms.setFrom( e.start + 1 );
        ms.setTo( e.end );
        return ms;
    }

    @Override
    public BedEntry toBedEntry(MasterSite ms)
    {
        String json;
        try
        {
            json = jsonSerializer.toJSON( ms );
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
        
        ChromInfo chrInfo = origin.getChromInfo(ms.getChr());
        BedEntry e =  new BedEntry( chrInfo.id, ms.getFrom() - 1, ms.getTo());
        e.data = json.getBytes(StandardCharsets.UTF_8);
        return e;
    }
    
}
