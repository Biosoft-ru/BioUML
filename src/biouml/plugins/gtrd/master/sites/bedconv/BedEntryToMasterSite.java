package biouml.plugins.gtrd.master.sites.bedconv;

import java.io.IOException;
import java.util.Properties;

import org.jetbrains.bio.big.BedEntry;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.sites.json.MasterSiteSerializer;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.bsa.track.big.BedEntryConverter;

public class BedEntryToMasterSite implements BedEntryConverter<MasterSite>
{
    private MasterSiteSerializer jsonSerializer;
    
    public BedEntryToMasterSite(MasterTrack origin, Properties properties)
    {
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
        ms.setChr( StringPool.get(e.getChrom() ) );
        ms.setFrom( e.getStart() + 1 );
        ms.setTo( e.getEnd() );
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
        return new BedEntry( ms.getChr(), ms.getFrom() - 1, ms.getTo(), json );
    }
    
}
