package biouml.plugins.gtrd.master.analyses.percellclusters;

import java.util.Properties;

import org.jetbrains.bio.big.BedEntry;

import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil;

public class BedEntryToPerCellCluster implements BedEntryConverter<PerCellCluster>
{
    private BigBedTrack<?> origin;
    public BedEntryToPerCellCluster(BigBedTrack<?> origin, Properties props)
    {
        this.origin = origin;
    }
    
    @Override
    public PerCellCluster fromBedEntry(BedEntry e)
    {
        PerCellCluster res = new PerCellCluster();
        res.setChr( e.getChrom() );
        res.setFrom( e.getStart()+1 );
        res.setTo( e.getEnd() );
        String[] parts = TextUtil.split( e.getRest(), '\t' );
        res.setMasterSiteId( parts[0] );
        res.setSummit( Integer.parseInt( parts[1] ) );
        res.chipSeqExpCount = Integer.parseInt( parts[2] );
        res.chipExoExpCount = Integer.parseInt( parts[3] );
        res.dnasePeakCount = Integer.parseInt( parts[4] );
        res.motifCount = Integer.parseInt( parts[5] );
        res.setOrigin( origin );
        return res;
    }

    @Override
    public BedEntry toBedEntry(PerCellCluster c)
    {
        StringBuilder rest = new StringBuilder();
        rest
            .append( c.getMasterSiteId() ).append( '\t' )
            .append( c.getSummit() ).append( '\t' )
            .append( c.chipSeqExpCount ).append( '\t' )
            .append( c.chipExoExpCount ).append( '\t' )
            .append( c.dnasePeakCount ).append( '\t' )
            .append( c.motifCount );
        return new BedEntry( c.getChr(), c.getFrom()-1, c.getTo(), rest.toString() );
    }
}